# Runbook — Mesures device physique (post uniformisation UX)

Date : 2026-09-07
Contexte : clôture du programme d'uniformisation UX (DAO #27/#24/#25/#26).
Les cycles émulateur (iOS `Wakeve-QA-iPhone-16-Pro`, Android API 32) ont
verrouillé tokens, contrastes et flux. Ce runbook liste les mesures
**réservées au hardware réel**, à exécuter avant la prochaine release.

## Prérequis

- Builds debug + release sur devices physiques iOS et Android (1 OLED, 1 LCD
  si possible)
- Palette colorimétrique neutralisée (désactiver True Tone / mode couleurs
  vives / Night Light) pendant la mesure
- Application QA : iOS `--wakeve-qa-seed-invitation-experience` ;
  Android `--ez wakeve.dev.auth true --es wakeve.qa.route …` (seed livré)
- Capture : iOS `simctl io screenshot` équivaut à `adb exec-out screencap -p` ;
  échantillonnage pixel via ImageMagick (`%[pixel:p{x,y}]`)

## 1. Dark mode OLED (résidual risk revue 2026-09-07)

| Mesure | Écran(s) | Attendu | Méthode |
|---|---|---|---|
| Fond midnight exact | 7 écrans iOS | `#071421` ± artefacts OLED | Screenshot + pixel (marge x=8, y=1200) |
| Héro dorée lisible | Résultats | Bordure `#D97706`@30% perceptible sans éblouissement | Inspection visuelle à 3 niveaux de luminosité |
| Chip statut (D1 fix) | Scénarios | Texte paleBlue sur teinte accent, ≥ 4.5:1 perçu | Screenshot + ratio |
| Android Material You | 8 écrans Android | Palette wallpaper cohérente (arbitrage O3) | Captures + revue visuelle |

## 2. Dynamic Type XL / AAA (résidual risk)

| Mesure | Détail |
|---|---|
| iOS Dynamic Type AX3–AX5 | En-tête `WakeveScreenHeader` (titre display), carte héro, chips — pas de troncature destructrice, sous-titre toujours lisible |
| Android fontScale 1.3 / 2.0 | Mêmes écrans ; vérifier le report des actions `trailing` du header sur petite largeur |
| Touch targets | Boutons circulaires 44/46pt aux échelles agrandies |

## 3. Reduce Transparency réel

Le cycle émulateur (`--wakeve-qa-reduce-transparency`) a simulé le réglage.
Sur device : activer **Réduire la transparence** (iOS) et
**Supprimer les animations/Couleurs high-contrast** (Android), refaire les 3
écrans à matériaux (Résultats, Scénarios, Information) en light + dark, et
confirmer : fonds tokens conservés, cartes opaques lisibles, zéro zone
illisible.

## 4. Environnement lumineux réel (usage terrain)

| Scénario | Ce qu'on regarde |
|---|---|
| Plein soleil | Lisibilité ivoire/blanc des cartes, CTA `#2563EB`, bordure dorée perceptible |
| Lumière basse (soir) | Fatigue visuelle du dark midnight `#071421`, contraste héro |
| Lecture en mouvement | Hiérarchie titre/sous-titre (#24) identifiable en coup d'œil |

## 5. Critères de clôture

- [ ] 2 devices physiques (iOS + Android) validés sur les 4 sections
- [ ] Toute dérive de teinte > ΔE noticeable documentée et arbitrée
- [ ] Captures device jointes au dossier QA (`qa-screenshots/device-<date>/`)
- [ ] Sign-off revue accessibilité finale (docs/reviews/)
