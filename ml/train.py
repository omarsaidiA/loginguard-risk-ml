"""Train the login risk model and export it as JSON for the Java backend.

The model is a logistic regression: the backend recomputes the score with a few lines of code
(standardise, weighted sum, sigmoid), needs no ML library at runtime, and can explain every
score as the sum of each feature's contribution.

The training data is SYNTHETIC: login sessions are generated from documented assumptions about
normal users and account takeover attempts (see generate()). The model therefore learns those
assumptions; the point of this project is the engineering around the model, not a claim about
real-world accuracy.

Usage:
    python train.py --output model.json   train and write the model
    python train.py --check model.json    retrain and verify the file matches (reproducibility)
"""

import argparse
import json
import sys

import numpy as np
import sklearn
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import average_precision_score, precision_score, recall_score, roc_auc_score
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler

MODEL_VERSION = "1.0.0"
SEED = 42
SAMPLES = 50_000
ATTACK_RATE = 0.05
LABEL_NOISE = 0.02
THRESHOLDS = {"medium": 0.3, "high": 0.7}

# Order matters: the backend builds its feature vector in exactly this order and refuses to
# start if the model lists different features.
FEATURES = [
    ("failed_attempts_1h", "Failed attempts in the previous hour"),
    ("new_device", "Device never seen for this user"),
    ("new_country", "Country never seen for this user"),
    ("rapid_country_change", "Country changed less than 2 hours after the previous login"),
    ("night_time", "Login between 00:00 and 05:59 UTC"),
    ("log_hours_since_last_login", "Long time since the previous login"),
    ("attempt_failed", "This attempt failed"),
]


def generate(rng, n):
    """Synthetic sessions. Each line states the assumption, (normal user, takeover attempt)."""
    attack = rng.random(n) < ATTACK_RATE

    def flag(p_normal, p_attack):
        return np.where(attack, rng.random(n) < p_attack, rng.random(n) < p_normal).astype(float)

    # Attackers guess passwords first: several failures shortly before
    failed_attempts = np.where(attack, rng.poisson(3.0, n), rng.poisson(0.2, n)).astype(float)
    # Stolen credentials are often used on dormant accounts: longer gap since the last login
    hours_since_last = np.where(attack, rng.lognormal(np.log(24 * 20), 1.2, n), rng.lognormal(np.log(18), 1.0, n))
    features = np.column_stack(
        [
            failed_attempts,
            flag(0.05, 0.80),  # new device: rare for a user, typical for an attacker
            flag(0.02, 0.60),  # new country
            flag(0.002, 0.35),  # "impossible travel": another country minutes after the last login
            flag(0.08, 0.40),  # night time
            np.log1p(hours_since_last),
            flag(0.05, 0.40),  # this attempt itself failed
        ]
    )
    # Real labels are imperfect: flip a small share so the model is not trained on a clean world
    flipped = rng.random(n) < LABEL_NOISE
    labels = np.where(flipped, ~attack, attack).astype(int)
    return features, labels


def evaluate(labels, probabilities):
    metrics = {
        "rocAuc": roc_auc_score(labels, probabilities),
        "averagePrecision": average_precision_score(labels, probabilities),
    }
    for level, threshold in THRESHOLDS.items():
        predicted = (probabilities >= threshold).astype(int)
        metrics[f"precisionAt{level.capitalize()}"] = precision_score(labels, predicted, zero_division=0)
        metrics[f"recallAt{level.capitalize()}"] = recall_score(labels, predicted)
    return {name: round(float(value), 4) for name, value in metrics.items()}


def train():
    rng = np.random.default_rng(SEED)
    features, labels = generate(rng, SAMPLES)
    x_train, x_test, y_train, y_test = train_test_split(
        features, labels, test_size=0.2, stratify=labels, random_state=SEED
    )
    scaler = StandardScaler().fit(x_train)
    model = LogisticRegression(max_iter=1000).fit(scaler.transform(x_train), y_train)
    probabilities = model.predict_proba(scaler.transform(x_test))[:, 1]

    def number(value):
        return float(f"{float(value):.10g}")  # stable text representation

    return {
        "version": MODEL_VERSION,
        "algorithm": "logistic-regression",
        "syntheticTrainingData": True,
        "features": [
            {
                "name": name,
                "label": label,
                "mean": number(mean),
                "scale": number(scale),
                "coefficient": number(coefficient),
            }
            for (name, label), mean, scale, coefficient in zip(
                FEATURES, scaler.mean_, scaler.scale_, model.coef_[0], strict=True
            )
        ],
        "intercept": number(model.intercept_[0]),
        "thresholds": THRESHOLDS,
        "metrics": evaluate(y_test, probabilities),
        "training": {
            "samples": SAMPLES,
            "attackRate": ATTACK_RATE,
            "labelNoise": LABEL_NOISE,
            "seed": SEED,
            "testShare": 0.2,
            "scikitLearn": sklearn.__version__,
        },
    }


def same(expected, actual, path="model"):
    """Numbers may differ in the last digits across CPUs; anything else must be identical."""
    if isinstance(expected, dict) and isinstance(actual, dict):
        return expected.keys() == actual.keys() and all(same(expected[k], actual[k], f"{path}.{k}") for k in expected)
    if isinstance(expected, list) and isinstance(actual, list):
        return len(expected) == len(actual) and all(same(e, a, path) for e, a in zip(expected, actual, strict=True))
    if isinstance(expected, float) or isinstance(actual, float):
        if abs(float(expected) - float(actual)) > 1e-6 * max(1.0, abs(float(expected))):
            print(f"mismatch at {path}: {expected} != {actual}", file=sys.stderr)
            return False
        return True
    if expected != actual:
        print(f"mismatch at {path}: {expected!r} != {actual!r}", file=sys.stderr)
    return expected == actual


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument("--output", help="write the trained model to this file")
    group.add_argument("--check", help="retrain and compare with this existing model file")
    args = parser.parse_args()

    model = train()
    if args.output:
        with open(args.output, "w", encoding="utf-8", newline="\n") as file:
            json.dump(model, file, indent=2)
            file.write("\n")
        print(json.dumps(model["metrics"], indent=2))
        return 0
    with open(args.check, encoding="utf-8") as file:
        committed = json.load(file)
    if not same(committed, model):
        print("The committed model does not match what train.py produces.", file=sys.stderr)
        return 1
    print("The committed model is reproducible from train.py.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
