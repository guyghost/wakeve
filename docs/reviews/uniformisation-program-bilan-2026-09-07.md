# Bilan consolidé — Programme Uniformisation UX

Date : 2026-09-07 · Clôture du programme Swarm DAO #24/#25/#26/#27
(+ chantiers QA tooling, revues et audits associés)

## 1. Objectif & référence

Uniformiser l'expérience utilisateur Wakeve en prenant pour référence
l'écran Résultats du sondage
(`qa-screenshots/cycle-2026-09-04/28-poll-results.png`) : fond ivoire chaud
`#F6F1EA`, en-têtes grand titre + sous-titre contexte, carte héro dorée pour
l'information confirmée, un seul bleu d'action `#2563EB`.

## 2. Gouvernance Swarm DAO

| # | Proposition | Vote | Gates | Ship | Note |
|---|---|---|---|---|---|
| #27 | Fond ivoire #F6F1EA (tokens) | 82% | ✅ (dry-run requis, zone rouge) | ✅ | 4/5 |
| #24 | En-têtes unifiés | 100% | ✅ 0 warning | ✅ | 4/5 |
| #25 | Cartes + carte héro dorée | 100% | ✅ | ✅ | 4/5 |
| #26 | Boutons + chips | 100% | ✅ | ✅ | 4/5 |

(Proposition #23, première itération de #27, close en échec terminal pour
mauvais séquencement dry-run/check — leçon enregistrée.)

Santé DAO au 2026-09-07 : **90/100** (participation 27/27, profondeur de
délibération 100 %, note moyenne 4.3/5).

## 3. Implémentation

- iOS : `WakeveScreenHeader`, `WakeveHeroCard`, `WakeveSecondaryIconButton`,
  `WakeveMetaChip`, tokens `wakeveWarmIvory`/`wakeveWarmIvoryDark`,
  unification des 3 ivoires dispersés, migrés sur Résultats + Scénarios +
  les cartes confirmées ; 16 tests contract (pattern source-contract)
- Android/Compose : `WakeveScreenHeader`, `WakeveHeroCard` (+onClick),
  `WakevePrimaryPillButton`, `WakeveSecondaryIconButton`, `WakeveMetaChip`,
  `WakeveCard` blanche hairline ; migrés sur ScenarioManagement +
  EventDetail + PollResults ; tokens `WarmIvory`/`WarmIvoryDark`
- QA tooling : bypass auth iOS/Android, seed repository idempotent des deux
  côtés, 13 routes déterministes (9 iOS + 4 Android)

## 4. Vérification (cycles QA 2026-09-07)

| Cycle | Périmètre | Résultat clé |
|---|---|---|
| Light iOS | 8 écrans | Ivoire `#F6F1EA` exact sur 6 écrans + 3 finitions |
| Dark iOS | 8 écrans | Midnight `#071421` exact sur 7 écrans ; D1 corrigé |
| Reduce Transparency iOS | 3 écrans × light/dark | Cartes opaques lisibles, fonds conservés |
| Android seeded | 7 captures + flux vote E2E | Score 2 sur héro dorée ; bordure pixel-confirmée |
| Contrastes WCAG 2.1 | light + dark | 10/12 pass ; A2 (4.94) et D1 (8.03) corrigés ; A1 qualifié décoratif |

Évidences : `qa-screenshots/cycle-2026-09-07{,-dark,-reduce-transparency}/`,
`qa-screenshots/cycle-2026-09-07-android/`.

## 5. Décisions enregistrées

| Décision | Contenu |
|---|---|
| O3 (Android) | Material You dynamic color conservé ; AC #27 rescopée iOS ; tokens ivoire = schéma fixe/fallback |
| A1 | Bordure dorée du héro = décorative ; signal porté par étoile + libellé (obligatoires) |
| A3 | Studio d'invitation = écran plein écran → fond ivoire (pas un sheet) ; natif réservé aux vrais sheets |
| F1/O3 suite | Les cycles Android auditent la cohérence interne Material You, pas la parité de teinte |

## 6. Défauts connus & risques résiduels

- `AndroidProductLanguageContractTest` : 4 échecs préexistants sur develop
  (identiques avant/après le programme) — à traiter séparément
- Gradle configuration-cache : échec préexistant `processDebugGoogleServices`
  (contournement `-Dorg.gradle.configuration-cache=false`)
- Sous-titre événement de l'écran Scénarios Android : nécessite de remonter
  `event.title` dans `ScenarioManagementContract`
- Dark mode / device physique : voir `docs/testing/DEVICE_MEASUREMENT_RUNBOOK.md`
- sheet Studio : fond natif assumé ; information/archive Android : voir P1

## 7. Backlog ouvert

| Chantier | Ref |
|---|---|
| Surfaces invitation Android (P1→P3 recommandé) | `docs/product/android-invitation-surfaces-gap-analysis.md` |
| Mesures device physique | `docs/testing/DEVICE_MEASUREMENT_RUNBOOK.md` |
| Sous-titre événement Scénarios Android | dépendance `ScenarioManagementContract` |
| 4 échecs contract langage produit Android | défaut préexistant |
