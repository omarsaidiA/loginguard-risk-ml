# Modèle de risque de connexion

Fiche du modèle (*model card*) utilisé par LoginGuard pour scorer chaque connexion.

## En bref

| | |
|---|---|
| Algorithme | Régression logistique (scikit-learn 1.9.1), variables standardisées |
| Sortie | Probabilité qu'une connexion soit une tentative de prise de compte, puis niveau LOW / MEDIUM (≥ 0,3) / HIGH (≥ 0,7) |
| Données | **Synthétiques** : 50 000 sessions générées par [`train.py`](train.py), 5 % d'attaques, 2 % d'étiquettes volontairement inversées |
| Exécution | Le backend Java recalcule le score à partir du JSON exporté : aucune bibliothèque de ML en production |

## Variables

Toutes sont calculées à partir de l'historique de l'utilisateur **avant** la connexion évaluée.

| Variable | Signal |
|---|---|
| `failed_attempts_1h` | Échecs de connexion dans l'heure précédente (devinette de mot de passe) |
| `new_device` | Appareil jamais vu pour cet utilisateur |
| `new_country` | Pays jamais vu pour cet utilisateur |
| `rapid_country_change` | Autre pays moins de 2 h après la connexion précédente (« voyage impossible ») |
| `night_time` | Connexion entre 0 h et 6 h UTC |
| `log_hours_since_last_login` | Compte resté longtemps inactif (échelle logarithmique) |
| `attempt_failed` | La tentative elle-même a échoué |

Une toute première connexion n'est pas « nouvelle » par rapport à un historique vide : `new_device` et
`new_country` valent alors 0.

## Résultats sur le jeu de test (20 %, jamais vu à l'entraînement)

| Mesure | Valeur |
|---|---|
| ROC AUC | 0,87 |
| Précision / rappel au seuil MEDIUM | 0,96 / 0,73 |
| Précision / rappel au seuil HIGH | 0,98 / 0,63 |

Le bruit d'étiquetage plafonne volontairement ces scores : un modèle parfait sur des données
synthétiques ne prouverait rien.

## Limites, à dire clairement

- Les données sont synthétiques : le modèle apprend les hypothèses écrites dans `generate()`, pas le
  comportement réel d'utilisateurs. Le projet démontre la chaîne complète (entraînement reproductible,
  export, contrôle d'intégrité, explicabilité, revue humaine), pas une précision réelle.
- L'heure est jugée en UTC, sans le fuseau de l'utilisateur.
- En production, il faudrait réentraîner sur des événements réels étiquetés par les analystes : c'est
  à quoi sert la revue `CONFIRMED_FRAUD` / `LEGITIMATE` dans l'application.

## Sécurité du modèle

- **Intégrité** : le backend calcule le SHA-256 du fichier au démarrage et refuse de démarrer s'il ne
  correspond pas à `loginguard.model.sha256`. Un fichier modifié (par exemple un poids mis à zéro pour
  ne plus signaler les nouveaux appareils) est rejeté.
- **Cohérence** : le backend refuse aussi un modèle dont les variables ne sont pas exactement celles du
  code, dans le même ordre.
- **Reproductibilité** : `train.py --check` réentraîne et vérifie que le fichier versionné est bien
  celui que produit le code. Le CI le vérifie à chaque push.

## Réentraîner

```bash
docker run --rm -v "$PWD:/work" -w /work/ml python:3.13.16-slim sh -c \
  'pip install -r requirements.txt && python train.py --output ../backend/src/main/resources/model/login-risk-model.json'
sha256sum backend/src/main/resources/model/login-risk-model.json   # puis mettre à jour loginguard.model.sha256
```
