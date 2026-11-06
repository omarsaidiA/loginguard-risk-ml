package com.omarsaidi.loginguard.risk;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Logistic regression exported by ml/train.py. Scoring is a few lines of arithmetic, so no ML
 * library (and its dependency tree) runs in the backend, and each score decomposes exactly into
 * per-feature contributions that an analyst can read.
 */
public record RiskModel(String version, String algorithm, boolean syntheticTrainingData, List<Feature> features,
		double intercept, Thresholds thresholds, Map<String, Double> metrics, Map<String, Object> training) {

	public record Feature(String name, String label, double mean, double scale, double coefficient) {
	}

	public record Thresholds(double medium, double high) {
	}

	/** One feature's push on the score, in log-odds: positive raises the risk. */
	public record Contribution(String feature, String label, double value) {
	}

	public record Assessment(double probability, RiskLevel level, List<Contribution> contributions) {
	}

	public Assessment assess(LoginFeatures input) {
		double[] values = input.toVector();
		double logit = this.intercept;
		List<Contribution> contributions = new ArrayList<>(this.features.size());
		for (int i = 0; i < this.features.size(); i++) {
			Feature feature = this.features.get(i);
			double contribution = feature.coefficient() * (values[i] - feature.mean()) / feature.scale();
			logit += contribution;
			contributions.add(new Contribution(feature.name(), feature.label(), contribution));
		}
		contributions.sort(Comparator.comparingDouble(Contribution::value).reversed());
		double probability = 1.0 / (1.0 + Math.exp(-logit));
		return new Assessment(probability, levelOf(probability), List.copyOf(contributions));
	}

	RiskLevel levelOf(double probability) {
		if (probability >= this.thresholds.high()) {
			return RiskLevel.HIGH;
		}
		return (probability >= this.thresholds.medium()) ? RiskLevel.MEDIUM : RiskLevel.LOW;
	}

	/** Fails fast if the file does not describe the model this code was written for. */
	void validate() {
		List<String> names = this.features.stream().map(Feature::name).toList();
		if (!names.equals(LoginFeatures.NAMES)) {
			throw new IllegalStateException("Model features " + names + " do not match " + LoginFeatures.NAMES);
		}
		if (this.features.stream().anyMatch(f -> !(f.scale() > 0) || !Double.isFinite(f.coefficient()))
				|| !(this.thresholds.medium() < this.thresholds.high())) {
			throw new IllegalStateException("Model file contains invalid parameters");
		}
	}

}
