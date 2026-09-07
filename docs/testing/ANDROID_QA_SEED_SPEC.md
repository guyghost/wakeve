# Spec — Seed QA Android (équivalent `InvitationExperienceQALaunchSupport`)

Date : 2026-09-07 · Statut : prêt à implémenter · Origine : audit Android
(`docs/reviews/android-compose-audit-2026-09-07.md`) + cycle QA 2026-09-07.

## Problème

Sur iOS, le lancement headless QA (`--wakeve-qa-seed-invitation-experience` +
`--wakeve-qa-open-invitation-route …`) seede le repository (5 événements de
cycle : draft, polling, confirmed, finalized, past) et ouvre directement
l'écran voulu. Sur Android, seul le bypass auth existe
(`wakeve.dev.auth`, livraison 2026-09-07) : aucun seed de données → les
écrans Vote/Résultats/Scénarios ne sont pas capturables avec données
réelles, et les DAO #24/#25/#26 ne peuvent pas être vérifiés en capture
Android sur ces flux.

## Solution proposée

Portage du seeder en Kotlin **debug-only** côté `composeApp/androidMain` :

```
composeApp/src/androidMain/kotlin/com/guyghost/wakeve/qa/AndroidQaSeeder.kt
```

- Point d'entrée : dans `App.kt` (ou `MainActivity`), le même LaunchedEffect
  que le bypass auth — si `wakeve.dev.auth` est actif, lancer
  `AndroidQaSeeder.seed(viewerId)` en coroutine IO avant la navigation.
- Données : réutiliser les mêmes ids de seed que iOS (`qa-invitation-draft`,
  `qa-invitation-polling`, `qa-invitation-confirmed`, `qa-invitation-finalized`,
  `qa-invitation-past`) pour des captures comparables entre plateformes.
- Accès données : `DatabaseEventRepository`, `WakeveDb` (SQLDelight) via Koin —
  mêmes chemins que l'app (aucun mock).
- Navigation : exposer `qaOpenRoute` (state/page dérivée) consommé par le
  `NavHost` pour l'ouverture déterministe (`poll`, `poll-results`,
  `scenarios`, `detail`, `event_creation`).
- Garde-fous : `BuildConfig.DEBUG`… remplacé par le check
  `ApplicationInfo.FLAG_DEBUGGABLE` (BuildConfig.DEBUG non généré, cf. AGP 9
  — même approche que le bypass auth) + idempotence (re-seed = no-op si
  présent, comme le contract iOS
  `testSeedIsRepositoryBackedTotalProtectedAndIdempotentAcrossRelaunch`).

## Inventaire à seed er (source de vérité : iOS, 682 lignes)

1. Utilisateurs QA (viewer `wakeve-debug-user` + participants invités).
2. 5 événements de cycle avec `TimeSlot` (Europe/Paris, `timeOfDay=.specific`),
   deadline J-7, `eventType=.other`, min/max/expected participants, copy
   description identique iOS.
3. Participants + axes d'accès (organisateur ACCEPTED/VALIDATED ; invité
   PENDING/NOT_VALIDATED) via `participantQueries` — nécessaire à
   `canAccessScenarioDetails`.
4. Votes du cycle polling (Oui/Peut-être) pour un `bestSlot` non trivial
   (scores YES=2/MAYBE=1/NO=-1 distincts).
5. Lieux potentiels du draft (`potentialLocationQueries`).
6. Invitations directes protégées : lot + résultats `hmac-v1`
   (`PENDING_SYNC`/`QUEUED_LOCAL`, rétention 29 jours) — **attention** :
   ne pas passer par `submit(command:)` (garde interdite, cf. defect iOS
   2026-09-05) ; écrire directement les tables comme le repository.
7. Préférences de notification de l'événement confirmed.

## Critères d'acceptation

1. `adb shell am start … --ez wakeve.dev.auth true` + route → l'écran visé
   s'ouvre avec données, sans intervention (zéro prompt SpringBoard/permissions).
2. Idempotent : relancer ne duplique rien et ne casse rien (test shared).
3. Les captures Android permettent de vérifier #24 (header des écrans
   vote/résultats), #25 (carte héro meilleur créneau), #26 (CTA/chips) avec
   le fond Material You (arbitrage O3).
4. Zéro code seed dans les builds release (garde debug + chemin inatteignable).

## Estimation

~600-800 lignes Kotlin + 1 test unitaire shared d'idempotence. Chantier
autonome, sans dépendance produit.
