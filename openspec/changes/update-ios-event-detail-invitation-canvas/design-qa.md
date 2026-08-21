# Design QA — iOS Event Detail Invitation Canvas

## Référence et environnement

- Composition approuvée : [`assets/option-1-invitation-canvas.png`](./assets/option-1-invitation-canvas.png), 853 × 1844 px.
- Device : iPhone 16 Pro.
- Runtime : iOS 26.5.
- Capture d’implémentation principale : [`assets/implementation-annecy-iphone16pro-ios26-final.png`](./assets/implementation-annecy-iphone16pro-ios26-final.png), 1206 × 2622 px natifs.
- Capture native : 1206 × 2622 px, normalisée directement à 853 × 1844 px pour la comparaison avec la référence.
- Fixture DEBUG : Annecy, événement confirmé, 4 participants confirmés, 2 réponses en attente, organisatrice Léa, action typée de comparaison des options.

Les données de la fixture diffèrent intentionnellement des noms, compteurs et textes de la référence graphique. Conformément à `design.md`, la QA compare la hiérarchie, le crop, la lisibilité et le comportement du canvas ; elle ne demande pas de reproduire des données de démonstration comme valeurs de production.

## Captures vérifiées

| Passage | Capture | Résultat |
|---|---|---|
| Référence | [`option-1-invitation-canvas.png`](./assets/option-1-invitation-canvas.png) | Direction visuelle approuvée. |
| Pass 1 | [`implementation-annecy-iphone16pro-ios26.png`](./assets/implementation-annecy-iphone16pro-ios26.png) | Overflow horizontal/vertical, crop du hero et accès aux actions classés P1. |
| Pass 2 | [`implementation-annecy-iphone16pro-ios26-pass2.png`](./assets/implementation-annecy-iphone16pro-ios26-pass2.png) | Amélioration confirmée, mais overflow résiduel encore visible. |
| Pass 3 | [`implementation-annecy-iphone16pro-ios26-pass3.png`](./assets/implementation-annecy-iphone16pro-ios26-pass3.png) | Comparaison du verdict designer : aucun P0, P1 ou P2. |
| Implémentation définitive | [`implementation-annecy-iphone16pro-ios26-final.png`](./assets/implementation-annecy-iphone16pro-ios26-final.png) | Capture principale après correctif final du scroll et de la safe area ; application laissée ouverte dans cet état. |
| Dynamic Type + contraste | [`implementation-annecy-accessibility-xxxl-contrast-pass2.png`](./assets/implementation-annecy-accessibility-xxxl-contrast-pass2.png) | Accessibility XXXL et contraste renforcé : contenu scrollable, action atteignable et statut non dépendant de la couleur. |
| Effets réduits | [`implementation-annecy-reduce-motion-transparency-pass2.png`](./assets/implementation-annecy-reduce-motion-transparency-pass2.png) | Reduce Motion et Reduce Transparency : fallback stable, hiérarchie et action conservées. |

## Historique des corrections

### Pass 1 — P1

- Le canvas dépassait la largeur utile du viewport, ce qui décalait le crop et rendait certaines actions difficiles à atteindre.
- Le cadrage du hero ne suivait pas correctement la géométrie réellement disponible.
- La distinction d’un badge reposait trop fortement sur la couleur.

Corrections appliquées : largeur finie bornée par le viewport, crop piloté par la géométrie disponible, maintien explicite des actions dans la zone utile et ajout d’un signal textuel/sémantique au badge.

### Pass 2 — P1 résiduel

- L’overflow persistait dans la composition normale après la première correction.
- Le comportement safe-area/scroll devait encore être stabilisé pour Dynamic Type.

Corrections appliquées : contraintes finales du conteneur, ajustement du scroll et de la safe area, puis conservation d’une seule action primaire atteignable pendant le reflow Dynamic Type.

### Pass 3 — validation visuelle

Le designer a comparé `implementation-annecy-iphone16pro-ios26-pass3.png` avec la référence sur la hiérarchie, le crop, la typographie, les espacements, les couleurs, le verre, la qualité d’image et la copie. Aucun P0, P1 ou P2 ne subsiste. Les différences mineures de titre, de carte et de rendu du Liquid Glass natif sont classées P3 et jugées acceptables dans le contexte d’une fixture structurée et du rendu système iOS 26.5.

### Capture définitive

Après le verdict designer, `implementation-annecy-iphone16pro-ios26-final.png` consigne l’état définitif avec le correctif scroll/safe-area appliqué. Cette capture remplace `pass3` comme évidence d’implémentation principale ; `pass3` reste conservée dans l’historique du verdict. L’application a été laissée ouverte sur cet état final.

## Vérifications techniques

| Vérification | Résultat |
|---|---|
| Build Debug pour le simulateur iPhone 16 Pro / iOS 26.5 | Réussi. |
| XCTest du canvas | 36/36 réussis. |
| XCTest Premium Event Detail | 13/13 réussis. |
| Total XCTest pertinent | 49/49 réussis. |
| Localisations en/fr/es/it/pt | 5/5 validées. |
| Accès, état terminal, offline/pending-sync, partage sécurisé et action unique | Couverts par les tests du canvas. |
| Dynamic Type XXXL et contraste renforcé | Capture finale validée. |
| Reduce Motion et Reduce Transparency | Capture finale validée. |
| `openspec validate update-ios-event-detail-invitation-canvas --strict` | Réussi. |
| `git diff --check` | Réussi. |

## Limite de vérification

Les contrats source couvrent l’ordre sémantique VoiceOver et la recomposition paysage. Cette session ne disposait toutefois pas d’un outil d’inspection de hiérarchie permettant d’exécuter une interaction UI instrumentée VoiceOver ou paysage. Le rapport ne revendique donc pas une validation interactive de ces deux parcours ; il atteste leur couverture contractuelle uniquement.

## Verdict designer

Pass 3 : aucun P0/P1/P2. P3 relatifs au titre, à la carte et au verre natif acceptés.

final result: passed
