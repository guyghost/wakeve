# Surfaces Invitation Android — Analyse d'écart & Backlog produit

Date : 2026-09-07
Origine : audit Android (`docs/reviews/android-compose-audit-2026-09-07.md`),
bloc F2. Décision produit requise — ce document fournit la cartographie et les
options, sans trancher le périmètre.

## Cartographie iOS ↔ Android

Les surfaces « invitation experience » iOS (navigation déterministe QA,
`InvitationExperienceQALaunchSupport`) confrontées aux routes Android
(`Screen.kt` / `WakeveNavHost.kt`) :

| Surface iOS | Vue iOS | Équivalent Android | Écart |
|---|---|---|---|
| `library` — bibliothèque d'invitations | `EventLibraryView` | `Home`/`Events` + `EventPhotos` (filtre) | **Partiel** : pas de vue bibliothèque dédiée (tri, recherche, filtres invitations) |
| `detail` — canvas d'invitation plein écran | `EventDetailInvitationCanvas` | `EventDetail` (workspace de planification : cartes summary destination/jour J/budget/transport) | **Divergence de design** : iOS = canvas artistique, Android = workspace fonctionnel. Deux langages pour le même objet |
| `studio` — studio de création/d'édition | `EventCreationStudioView` (prévisualisation artwork 4:3) | `EventCreation` (wizard 4 étapes + assistant de planification) | **Partiel** : pas de prévisualisation d'artwork dans le wizard Android |
| `audience` — gestion des participants | `EventAudienceView` + invitations protégées | `ParticipantManagement` + `InvitationShare` | **Partiel** : partage basique ; pas de lot d'invitations directes protégées (hmac-v1) |
| `information` — information confirmée | `EventInformationView` | — (résumés dispersés dans `EventDetail`) | **Manquant** |
| `archive` — jour J / archivé | `EventArchiveView` (read-only) | — (statut FINALIZED listé sur Home, pas d'écran dédié) | **Manquant** |

Le cycle décisionnel (poll → résultats → scénarios → organisation) est
couvert des deux côtés.

## Options produit

| Option | Contenu | Effort estimé | Impact |
|---|---|---|---|
| **P1 — Parité minimale** | Écrans `Archive` (jour J read-only) et `Information` (info confirmée) Android, consommant les composants #24/#25/#26 déjà livrés (header, héro dorée, chips) | M (2 écrans, données déjà en base) | Ferme les 2 surfaces « Manquant » du cycle de vie |
| **P2 — Unification canvas** | Porter le canvas d'invitation sur Android (artwork structuré + presets déjà en base via `invitationExperienceQueries`) | L (rendu artwork + édition) | Alignement visuel majeur, mais refonte du `EventDetail` Android |
| **P3 — Audience avancée** | Invitations directes protégées sur Android (le schéma et le repository shared sont prêts — le seeder QA écrit déjà ces tables) | M | Parité du flux d'invitation privée |
| **P4 — Bibliothèque** | Vue bibliothèque dédiée | S–M | Confort, non bloquant |

## Recommandation

Séquencer **P1 → P3**, P2/P4 plus tard. P1 ferme l'écart visible du cycle de
vie (archive + information) à coût maîtrisé puisque les composants
d'uniformisation (#24/#25/#26) sont déjà en place ; P3 capitalise sur le
travail de schéma/validations déjà démontré par le harnais QA.

## Dépendances & prérequis

- Composants d'uniformisation livrés (#24/#25/#26) ✅
- Fond Material You assumé (arbitrage O3) — l'ivoire ne s'applique pas
  aux nouveaux écrans Android (voir audit F1) ⚠️ cohérence à arbitrer si P1
  veut reprendre les visuels iOS
- Seed QA Android livré ✅ (captures de recette immédiates)
