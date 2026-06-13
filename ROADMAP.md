# Roadmap Wakeve

Derniere mise a jour: 2026-06-13
Version: 2.0

## Etat verifie au 2026-06-13

Sources consultees:

- `openspec list`
- `openspec list --specs`
- `openspec/changes/*/tasks.md`
- `docs/APP_STORE_BLOCKER_REGISTER.md`
- XcodeBuildMCP iOS Simulator test/run artifacts du 2026-06-13
- `./gradlew :shared:jvmTest ...` et `./gradlew :shared:iosSimulatorArm64Test`

Changements OpenSpec actifs:

| Changement | Etat | Impact roadmap |
|---|---:|---|
| `add-in-app-account-deletion` | Complete | P0 App Store AS-09 implemente et verifie localement; `APP_STORE_ACCOUNT_DELETION_EVIDENCE_COMPLETE=false` reste bloque jusqu'aux preuves uploaded-build/TestFlight. |
| `add-ugc-moderation-controls` | 21/21 taches | P0 App Store, blocker AS-10. Filtering serveur, report/block/unblock, WebSocket block delivery, notifications, iOS states, moderation des champs UGC, notes App Review, docs evidence et gates audit poses; validation locale finale passee. Review-build evidence reste ouverte. |
| `add-on-device-wakeve-ai` | 40/41 taches | P1 produit iOS, client Foundation Models local finalise et fallback simulateur verifie; reste profiling device reel. |

Changements OpenSpec termines mais non archives:

| Changement | Etat | Prochaine action |
|---|---:|---|
| `add-web-microfrontends-landing` | Complete | Archiver quand la release branch est stabilisee. |
| `add-contact-participant-selection` | Complete | Archiver quand les preuves iOS release sont collectees. |
| `add-scenario-matrix-voting` | Complete | Archiver apres regression du parcours release. |
| `update-create-event-wizard-slots-preview` | Complete | Archiver apres regression du wizard iOS. |

## Position

Wakeve n'est plus dans l'etat de la roadmap initiale du 2026-02-10. Le projet a evolue vers un produit iOS-first proche d'une soumission App Store, avec une base KMP plus large, une surface web publique, des preuves de readiness, des workflows avancés d'organisation et plusieurs changements OpenSpec actifs.

La roadmap ne doit donc plus partir de "atteindre 60 % de couverture" ou "demarrer Advanced Push" comme si le produit etait encore en sortie de phase Ralph. Les priorites actuelles sont:

1. fermer les bloqueurs App Store reels;
2. finir les changements OpenSpec deja engages;
3. stabiliser les parcours produit critiques sur iOS;
4. garder Android/KMP et le web alignes sans rouvrir de grands chantiers non necessaires a la release.

## Ce qui change par rapport a l'ancienne roadmap

### Decisions challengees

| Ancienne hypothese | Verdict | Nouvelle decision |
|---|---|---|
| "Phase 1.1: atteindre 60 % coverage" est le premier P0 | Depasse | Les tests ont largement grandi. Le sujet P0 devient la fiabilite des gates release, pas un seuil generique. |
| "Advanced Push" est le prochain chantier P0 | Depriorise | Les notifications restent importantes, mais elles ne ferment pas les bloqueurs App Store actuels. |
| "Documentation API complete" est encore a creer | Partiellement obsolete | `docs/API/openapi.yaml`, `docs/API.md` et les docs App Store existent deja. Le besoin est la consolidation et la verification, pas une creation from scratch. |
| "Production Ready" se resume aux tests/perf/security | Incomplet | La readiness depend maintenant de DNS/live URLs, build signe, TestFlight, account deletion, UGC moderation, privacy evidence et App Store Connect. |
| "Ralph next session" guide encore le travail | Obsolete | La roadmap doit suivre OpenSpec et les blockers de release, pas une session historique. |

### Travaux recents a prendre en compte

- `add-web-microfrontends-landing` est complet: landing publique et dashboard separes, avec Vercel Microfrontends.
- `add-contact-participant-selection` est complet: selection multi-contact Android/iOS et logique partagee.
- `add-scenario-matrix-voting` est complet: generation, publication et selection de scenarios matriciels.
- `update-create-event-wizard-slots-preview` est complet: creation iOS avec plusieurs creneaux et preview avant creation.
- `add-on-device-wakeve-ai` est presque complet: le client Foundation Models local et le fallback simulateur sont verifies, et il reste surtout le profiling sur device compatible.
- Le parcours iOS creation -> poll -> scenarios -> organisation est verifie par tests de contrat iOS et tests KMP iOS/JVM; les preuves screenshots multi-ecran restent a collecter avec une automation UI ou une build TestFlight.
- `add-in-app-account-deletion` est complet cote implementation locale: backend, iOS, tests, wording public, docs App Store et validations locales sont passes. La readiness App Store reste bloquee par les preuves uploaded-build/TestFlight et le marker d'evidence final.
- `add-ugc-moderation-controls` est complet cote taches locales; il bloque encore la readiness App Store sur review-build, support/live URL et preuves App Review completes.

## Roadmap priorisee

### P0 - Fermer les bloqueurs App Store

Objectif: pouvoir soumettre une build iOS verifiable, avec les exigences Apple critiques couvertes par le produit, les preuves et les scripts.

La definition globale du P0 est stricte: le projet n'est pret que lorsque `./scripts/app-store-submission-audit.sh --check-live-urls --run-submission-ready` sort avec le code 0 et que `docs/APP_STORE_FINAL_SIGNOFF.md` contient `APP_STORE_FINAL_SIGNOFF_COMPLETE=true`.

#### 0.1 Account deletion end-to-end

Source OpenSpec: `openspec/changes/add-in-app-account-deletion/`
Blocker App Store: AS-09

- [x] Backend: route authentifiee `DELETE /api/user/delete` exposee.
- [x] Tests first: succes authentifie, refus unauthentifie, idempotence, revocation tokens/sessions, suppression push tokens, audit log.
- [x] Backend: deletion orchestration, revocation sessions/tokens, suppression push tokens, audit log.
- [x] Backend: anonymisation ou suppression sure des references utilisateur dans events, comments, chat, notifications, sync metadata.
- [x] Backend: revocation Sign in with Apple si les materiaux d'autorisation sont disponibles, sans bloquer l'effacement Wakeve en cas d'echec provider transitoire.
- [x] iOS: `AuthenticationService.deleteAccount()` avec bearer token et gestion success/auth failure/offline/server error.
- [x] iOS: chemin Profile -> Data Management -> Delete Account.
- [x] iOS: suppression locale Keychain, cache profil, donnees sync et identifiants analytics.
- [x] iOS: retour vers onboarding/login non authentifie apres deletion.
- [x] Guest mode: Delete Guest Data local sans backend.
- [x] App Store gates: l'audit final doit echouer si route, flow Profile, cleanup local ou evidence manquent.
- [x] Docs: mettre a jour les preuves App Store uniquement apres verification.

Definition of done:

- `openspec validate add-in-app-account-deletion --strict` passe.
- Les tests backend et iOS pertinents passent.
- `docs/APP_STORE_ACCOUNT_DELETION_EVIDENCE.md`, `docs/APP_STORE_READINESS.md` et `docs/APP_STORE_LAUNCH_CHECKLIST.md` pointent vers des preuves reelles.
- Pour la soumission App Store, `APP_STORE_ACCOUNT_DELETION_EVIDENCE_COMPLETE=true` reste interdit tant que le meme flux n'est pas verifie sur la build uploaded/TestFlight.

#### 0.2 UGC moderation controls

Source OpenSpec: `openspec/changes/add-ugc-moderation-controls/`
Blocker App Store: AS-10

- [x] Tests first: rejet des contenus hard-policy comments/chat/event text avant persistence ou broadcast.
- [x] Tests first: contenus incertains caches des utilisateurs reguliers pendant pending review.
- [x] Modeles partages: moderation status, report target, report reason, block.
- [x] Persistence: reports, decisions de moderation, user blocks.
- [x] Filtrage serveur avant persistence ou broadcast pour comments, chat et textes d'evenement.
- [x] Quarantaine/pending review pour contenu incertain.
- [x] Report flows pour comments, chat messages, events et users.
- [x] Block/unblock avec filtrage des contenus et notifications.
- [x] Moderator/admin review avec audit records.
- [x] Moderation appliquee aux champs UGC: event title, description, custom event type, potential location, planning free-text.
- [x] iOS: report/block/unblock visibles sur les surfaces concernees.
- [x] iOS: etats hidden/pending/rejected pour eviter de presenter un contenu non visible comme publie.
- [x] iOS: support/help path visible pour contact abuse et verification reviewer.
- [x] App Store gates: l'audit final doit echouer si filtering, reporting, blocking, support contact, audit moderator ou evidence iOS manquent.
- [x] Docs: preuves App Review et support/moderation process.
- [x] Validation locale finale: OpenSpec strict, lint metadata, tests serveur, preflight iOS/web Fastlane et audit final local executes.

Definition of done:

- `openspec validate add-ugc-moderation-controls --strict` passe.
- Les tests serveur couvrent rejet, quarantine, report, block et autorisations moderator/admin.
- `bundle exec fastlane ios preflight` passe avec build Release unsigned, lint iOS, audit/check/build web et routes locales.
- `APP_REVIEW_PHONE_NUMBER='+33123456789' ./scripts/app-store-submission-audit.sh` execute la preflight locale et retourne `NOT READY` uniquement a cause des signoffs/env vars/live URLs/build signee externes manquants.
- La readiness App Store ne peut passer que si filtering, reporting, blocking, contact et review process sont verifies.

#### 0.3 Production live URLs, AASA et backend

Source documentaire: `docs/APP_STORE_READINESS.md`, `docs/APP_STORE_LIVE_URL_AASA_EVIDENCE.md`
Blocker App Store principal: AS-14

- [ ] DNS public pour `wakeve.app` et `api.wakeve.app`.
- [ ] HTTPS valide pour legal, support, privacy, terms, AASA et backend health.
- [ ] AASA configure avec le bon Apple Team ID de production.
- [ ] Backend review-accessible pendant la fenetre App Review.
- [ ] Smoke test public web, dashboard `/app`, redirects et endpoints critiques.

Definition of done:

- Les checks live URL/AASA passent depuis une machine externe.
- Les preuves ne se basent plus seulement sur des routes locales deployables.

Preuves locales du 2026-06-13:

- `./scripts/capture-app-store-live-url-aasa.sh --allow-failures --timeout 5` genere `docs/app-store-live-url-aasa/live-url-aasa-2026-06-13T13-02-14Z.md` avec DNS, HTTPS, AASA, backend health, dashboard `/app` et redirects legacy. Le rapport conclut `FAIL. 18 required live URL/AASA checks failed or could not be validated.`
- `dig +short wakeve.app` et `dig +short api.wakeve.app` ne retournent aucune entree DNS depuis l'environnement local; les `curl` publics vers privacy, support, terms, third-party notices, `/app`, `/app/login`, `/app/dashboard`, `/app/create`, `/app/events`, les redirects `/dashboard`/`/login`/`/create`/`/events`, les deux endpoints AASA et `https://api.wakeve.app/health` echouent avec `Could not resolve host`.
- `BASE_URL=http://127.0.0.1:3000 APPLE_TEAM_ID=A1B2C3D4E5 ./scripts/app-store-local-web-route-check.sh` passe contre la landing locale demarree avec `APPLE_TEAM_ID=A1B2C3D4E5 pnpm --dir apps/landing exec vite dev --host 127.0.0.1`; le check couvre les pages legales, les endpoints AASA, `microfrontends.json` pour `/app`, et les redirects legacy vers `/app/*`.
- Cette preuve rend le blocage AS-14 reproductible, mais ne coche aucun item P0.3 tant que DNS public, TLS, AASA avec vrai Apple Team ID, backend et smoke public ne passent pas depuis un reseau externe.

#### 0.4 Signed archive, TestFlight et release evidence

Blockers App Store principaux: AS-01, AS-02, AS-03, AS-04, AS-12, AS-13, AS-15, AS-16, AS-18, AS-19

- [ ] Produire une archive signee avec le profil App Store Connect correct.
- [ ] Uploader et selectionner une build traitee dans App Store Connect.
- [ ] Faire un smoke test TestFlight sur device reel.
- [ ] Collecter dSYM, hashes, version/build evidence et monitoring 24 h si requis.
- [ ] Completer review access, contact phone, demo/guest path et release control.

Definition of done:

- `bundle exec fastlane ios preflight` passe.
- Le final signoff reference une build uploadée, testee et selectionnable.

#### 0.5 App Store Connect decisions et preuves externes

Blockers App Store principaux: AS-05, AS-06, AS-07, AS-08, AS-11, AS-17, AS-20, AS-21, AS-22

- [ ] Privacy labels, privacy policy et `PrivacyInfo.xcprivacy` alignes.
- [ ] Accessibility labels decision: non publies ou preuves device completes.
- [ ] Availability: iPhone/iPad ciblee, Mac Apple silicon et Vision Pro confirmes selon decision de release.
- [ ] DSA trader status ou exclusion EU storefronts documente dans App Store Connect.
- [ ] Payment compliance: clarifier que les depenses partagees concernent des biens/services reels hors unlock digital.
- [ ] Pricing/storefront availability, EULA/terms et release control valides par le owner.
- [ ] Media/localization: screenshots, app preview decision, metadonnees EN/FR et limites App Store Connect.
- [ ] Third-party licenses/notices publies ou livres dans l'app.

Preuves locales du 2026-06-13:

- `./scripts/app-store-license-inventory.sh --fetch-remote-metadata --fail-on-unknown` passe avec une classification de scope corrigee: Android/test/backend/web/tooling ne sont plus assimiles au scope iOS soumis.
- `docs/APP_STORE_LICENSE_INVENTORY_DRAFT.md`, `docs/APP_STORE_THIRD_PARTY_NOTICES.md` et `apps/landing/src/routes/third-party-notices/+page.svelte` ont ete regeneres; l'inventaire local contient 316 dependances, 3 licences inconnues hors scope iOS soumis, 1 mot-cle copyleft hors risque iOS soumis, et 0 risque unknown/copyleft en `submitted-ios`.
- Le profil iOS expose maintenant des liens actifs vers support, privacy, terms et `https://wakeve.app/third-party-notices`; `FindingsRegressionTests.testProfileExposesRequiredLegalAndNoticeLinks` et `./scripts/test-critical-release-gates.sh` gardent cette surface App Review.
- Cette preuve ne ferme pas AS-21 tant que le build signe, la route publique `https://wakeve.app/third-party-notices`, la revue legale/owner et le marker `APP_STORE_LICENSE_NOTICES_EVIDENCE_COMPLETE=true` ne sont pas disponibles.
- `./scripts/audit-app-store-media-localization.sh --fail-on-findings` genere `docs/app-store-media-localization/media-localization-2026-06-13T12-23-27Z.md` avec 0 finding: 8 screenshots locaux EN/FR inventories, dimensions iPhone `1320x2868`, dimensions iPad `2048x2732`, 0 app preview video, longueurs metadata EN/FR dans les limites et hash screenshot `e1d72a791111bc43b561e7b463043167e860a47f3c290443c0a015f64ef3effe`.
- Cette preuve ne ferme pas AS-20 tant que la page media App Store Connect, l'ordre des screenshots, le statut editable, la decision Media Manager/scaling et la concordance avec la build uploadee ne sont pas revus par le owner.
- `./scripts/audit-app-store-privacy-alignment.sh --fail-on-findings` genere `docs/app-store-privacy/privacy-alignment-2026-06-13T12-26-10Z.md` avec 0 finding local: `PrivacyInfo.xcprivacy`, `docs/APP_STORE_PRIVACY_LABELS.md` et `docs/PRIVACY_POLICY.md` sont hashes, le no-tracking est coherent, aucun IDFA/App Tracking Transparency n'est detecte dans les sources iOS/shared, et les 7 types collectes du manifest sont miroites par le draft privacy labels.
- Cette preuve ne ferme pas AS-05 tant que les privacy labels App Store Connect, la privacy policy live `https://wakeve.app/privacy`, la build uploadee et l'approbation legal/privacy owner ne sont pas verifies.

Definition of done:

- Chaque fichier d'evidence reference dans `docs/APP_STORE_BLOCKER_REGISTER.md` contient son marker `*_COMPLETE=true` uniquement apres preuve reelle.
- Les valeurs externes restent dans l'environnement release ou App Store Connect, pas dans le depot.

### P1 - Stabiliser le produit iOS

Objectif: reduire le risque de rejet et de regression sur les parcours visibles par les utilisateurs.

#### 1.1 Finaliser WakeveAI on-device

Source OpenSpec: `openspec/changes/add-on-device-wakeve-ai/`

- [x] Finaliser `WakeveAIClient` avec typed generation, streaming, tools, cancellation, timeout et logging safe.
- [x] Verifier le fallback sur simulateur ou device incompatible.
- [ ] Profiler latence, memoire et cancellation sur device compatible.
- [x] Documenter les limites: iOS-only, pas de server fallback, aucune persistence sans action explicite.

Definition of done:

- Les tests WakeveAI passent.
- Les notes de verification device/performance sont ajoutees au changement ou a `docs/implementation/`.

#### 1.2 Verrouiller le parcours creation -> poll -> scenarios -> organisation

- [x] Verifier le wizard multi-creneaux et la preview sur iOS.
- [x] Verifier la selection de participants par contacts.
- [x] Verifier le vote par matrice de scenarios et la selection finale.
- [x] Verifier les transitions DRAFT -> POLLING -> CONFIRMED -> ORGANIZING -> FINALIZED.
- [x] Garder les tests E2E centres sur les parcours release, pas sur le volume de tests.

Preuves du 2026-06-13:

- iOS contrats release: XcodeBuildMCP `test_sim`, 57 tests passed, 0 failed. Artefacts:
  - `/Users/guy/Library/Developer/XcodeBuildMCP/workspaces/wakeve-cf467b3193b0/logs/test_sim_2026-06-13T10-58-35-016Z_pid17199_7dbe09c9.log`
  - `/Users/guy/Library/Developer/XcodeBuildMCP/workspaces/wakeve-cf467b3193b0/result-bundles/test_sim_2026-06-13T10-58-35-016Z_pid17199_5733f7f4.xcresult`
- Shared JVM parcours critiques: `./gradlew :shared:jvmTest --tests 'com.guyghost.wakeve.notification.NotificationSchedulerTest' --tests 'com.guyghost.wakeve.workflow.DraftWorkflowIntegrationTest' --tests 'com.guyghost.wakeve.scenario.ScenarioMatrixGenerationServiceTest' --tests 'com.guyghost.wakeve.contacts.ContactParticipantSelectionPolicyTest' --tests 'com.guyghost.wakeve.presentation.statemachine.EventManagementStateMachinePollingConfirmationTest'` passe.
- Shared iOS KMP gate: `./gradlew :shared:iosSimulatorArm64Test` passe apres durcissement du scheduler iOS de test.
- Lancement simulateur standard: `build_run_sim` passe. Screenshot accueil vide:
  - `/var/folders/1t/456kc0651bl7mgrc62_m43g80000gn/T/screenshot_optimized_619c494c-902e-4b14-81c6-e5f2ee9d1b24.jpg`
  - apres reset app: `/var/folders/1t/456kc0651bl7mgrc62_m43g80000gn/T/screenshot_optimized_4b84c20b-1411-4e5c-97c6-c2bedec4cb0b.jpg`
- `./scripts/audit-ios-release-screen-evidence.sh` genere `docs/ios-release-screen-evidence/release-screen-evidence-2026-06-13T13-32-47Z.md` et rend la couverture screenshot actuelle explicite: 3/5 ecrans requis couverts localement (onboarding, login/guest, creation event), event detail et organisation toujours manquants, avec cibles de capture TestFlight/App Review explicites.

Definition of done:

- [x] Les tests E2E critiques passent.
- [ ] Les screenshots/preuves iOS couvrent onboarding, login/guest, creation event, event detail et organisation. L'environnement actuel ne fournit pas encore d'automation UI fiable pour naviguer ces ecrans; `snapshot_ui` XcodeBuildMCP echoue avec un framework prive manquant dans la beta Xcode.

#### 1.3 Accessibility, localization et copy release

- [ ] Rejouer les validations Dynamic Type, high contrast et VoiceOver sur les ecrans release.
- [ ] Corriger les truncations ou labels manquants.
- [x] Stabiliser FR/EN pour les textes visibles App Store et onboarding.
- [x] Maintenir les preuves dans `docs/app-store-evidence/` et les docs App Store.

Preuves du 2026-06-13:

- Les decisions Accessibility/Availability sont realignees sur les build settings Release: iPhone/iPad restent cibles, Mac Apple silicon et Apple Vision Pro sont desactives cote Xcode pour la premiere release.
- `docs/APP_STORE_ACCESSIBILITY_LABELS.md` ne recommande plus de claims Mac/Vision pour la premiere release; toute reactivation future exige des smoke tests runtime specifiques.
- `APP_REVIEW_PHONE_NUMBER='+33123456789' ./scripts/lint-store-metadata.sh --ios-only` passe apres realignement: 3049 checks, 0 erreur, 1 warning.
- `docs/app-store-evidence/README.md` indexe les captures simulateur locales avec dimensions et SHA-256, et `docs/APP_STORE_ACCESSIBILITY_EVIDENCE.md` / `docs/APP_STORE_MEDIA_LOCALIZATION_EVIDENCE.md` referencent cet index comme preuve partielle non suffisante pour le signoff TestFlight.
- Les fichiers `iosApp/src/Resources/en.lproj/Localizable.strings`, `fr.lproj`, `es.lproj`, `it.lproj` et `pt.lproj` passent `plutil -lint`, ont chacun 848 cles, 0 doublon et aucune cle manquante entre langues apres localisation des labels d'accessibilite du login, des controles release supplementaires, des textes de chargement release, des hints VoiceOver detectes par audit source renforce, des boutons icone seule, des surfaces account deletion, des controles moderation et des notices tierces.
- `scripts/audit-ios-accessibility-source.sh` ajoute un audit source SwiftUI local. Le rapport `docs/a11y/ios-accessibility-source-audit-2026-06-13T12-16-54Z.md` indique 0 label/hint/value VoiceOver statique hardcode sous `iosApp/src`, apres localisation de labels release WakeveAI, scenarios, transport, sync, home, participants, calendrier et options organisateur.
- L'audit source est etendu aux arguments nommes `accessibilityLabel:`/`accessibilityHint:`/`accessibilityValue:` des controles partages; `docs/a11y/ios-accessibility-source-audit-2026-06-13T13-09-20Z.md` passe avec 0 finding apres localisation des labels back/close restants.
- L'audit source couvre aussi les `ProgressView()` indetermines: `docs/a11y/ios-accessibility-source-audit-2026-06-13T13-12-50Z.md` passe avec 0 loader sans label ou masquage explicite apres correction des loaders release visibles.
- L'audit source detecte maintenant les literals VoiceOver dans des expressions `accessibilityLabel`/`accessibilityHint` plus complexes, notamment les ternaires; `docs/a11y/ios-accessibility-source-audit-2026-06-13T13-24-22Z.md` passe avec 0 finding apres localisation des hints badge AI, champs texte/password, selection chips, actions recherche/dictee, details participants et comparaison de scenarios.
- L'audit source couvre maintenant les `Button` SwiftUI icone seule sans nom accessible ou masquage explicite; `docs/a11y/ios-accessibility-source-audit-2026-06-13T13-45-18Z.md` passe avec 0 finding apres ajout de labels localises aux actions fond, budget, commentaires, creation event, repas et reunions.
- Le meme audit indique 0 risque `lineLimit(1)` sans fallback proche (`minimumScaleFactor`, `fixedSize`, `allowsTightening` ou `dynamicTypeSize`) apres ajout de fallbacks Dynamic Type sur les textes variables. Les cases Dynamic Type/truncation restent ouvertes tant que les preuves device/signed build ne sont pas collectees.

### P2 - Durcir KMP, backend et web apres release gate

Objectif: consolider sans bloquer artificiellement la premiere soumission.

#### 2.1 Tests et qualite

- [x] Remplacer l'objectif brut "60 % coverage" par des gates utiles: tests critiques obligatoires, regression tests sur blockers, et coverage reporting si facile a maintenir.
- [x] Ajouter JaCoCo/Kover seulement si l'outillage s'integre proprement au build KMP.
- [x] Continuer a mesurer les tests par parcours critique: auth, deletion, moderation, sync, scenarios, calendar, meeting, transport.

Preuves du 2026-06-13:

- Nouveau gate local: `./scripts/test-critical-release-gates.sh`.
- Le gate valide OpenSpec strict pour account deletion, UGC moderation et WakeveAI, puis exécute les regressions App Store UGC, les tests serveur account deletion/auth/UGC, et les tests JVM partages guest auth, moderation, notification scheduler, draft workflow, scenario matrix, contacts, polling confirmation et sync conflict.
- `./scripts/test-critical-release-gates.sh` passe.
- Le gate iOS contractuel lourd reste optionnel via `RUN_IOS_CONTRACTS=1 ./scripts/test-critical-release-gates.sh`, pour ne pas rendre le gate local dépendant d'un simulateur Xcode disponible à chaque exécution.
- JaCoCo est integre au module `shared` via `jacocoJvmTestReport`, `jacocoCoverageVerification` et `testWithCoverage`; Kover n'est pas ajoute parce qu'un seul outil suffit pour le besoin actuel.
- Le seuil coverage est un baseline global maintenable a 30 %, pas l'ancien objectif arbitraire 60 % ni un seuil par classe bruyant.
- `./gradlew :shared:clean :shared:testWithCoverage :shared:jacocoCoverageVerification` passe; le rapport XML indique une couverture instructions globale de 33.32 % (`covered=76303`, `missed=152689`).
- `./gradlew :shared:jvmTest --tests com.guyghost.wakeve.logistics.ActivityPlanningIntegrationTest --tests com.guyghost.wakeve.workflow.CompleteWorkflowE2ETest` passe apres realignement des tests legacy sur la moderation UGC et les guards de finalisation Phase 6.

#### 2.2 Observabilite et performance

- [ ] Mesurer cold start iOS et Android.
- [ ] Profiler les ecrans de liste, creation event, scenario matrix et WakeveAI.
- [x] Confirmer absence de logs personnels sur les chemins release auth, notification et push serveur.
- [x] Garder Crashlytics/analytics conforme aux privacy labels.

Preuves du 2026-06-13:

- `OtpManager`, `PushNotificationSender`, `EventNotificationTrigger` et `NotificationScheduler` ne loggent plus les emails, OTP, tokens push, payloads de notification, user IDs, participant IDs, event IDs ni previews de contenu sur les chemins serveur critiques.
- `./scripts/test-critical-release-gates.sh` inclut maintenant `assert_no_sensitive_server_logs` sur ces fichiers et echoue si ces valeurs reapparaissent dans les logs.
- `./scripts/test-critical-release-gates.sh` passe apres ce durcissement.
- `APP_REVIEW_PHONE_NUMBER='+33123456789' ./scripts/lint-store-metadata.sh --ios-only` passe: 3049 checks, 0 erreur, 1 warning.
- `docs/analytics/FIREBASE_ANALYTICS_PROVIDER.md` reflete l'etat reel de la premiere release iOS: provider local-only, pas de bridge Firebase Analytics iOS actif, pas d'emission third-party cote iOS.
- `docs/APP_STORE_OBSERVABILITY_EVIDENCE.md` documente l'audit local analytics/crash: `PrivacyInfo.xcprivacy` declare `NSPrivacyTracking=false`, product interaction reste declare pour analytics de facon conservative, et la recherche locale ne trouve pas d'integration active Crashlytics/Sentry/Bugsnag/AppCenter/IDFA/ATT sur les chemins iOS release verifies.
- `scripts/profile-release-performance.sh` ajoute un harnais local pour mesurer les cold starts iOS/Android quand un simulateur/emulateur ou device est disponible. Il produit des rapports Markdown dans `docs/performance/`, borne les builds release via `IOS_BUILD_TIMEOUT_SECONDS` / `ANDROID_BUILD_TIMEOUT_SECONDS`, cible le simulateur booté demande via `IOS_SIMULATOR`, resume les samples cold start et separe explicitement les preuves locales des traces device necessaires.
- `docs/performance/release-profiling-runbook.md` documente les captures restantes: cold start, home/list, creation event, scenario matrix et WakeveAI generation/cancellation/memoire, avec une matrice de traces device attendues.
- Rapport local genere: `docs/performance/release-performance-2026-06-13T12-08-21Z.md`. Il mesure un cold start iOS simulateur existant a 308.9 ms via `simctl launch`, saute Android faute de device/emulateur connecte, et ne ferme pas les items device/release.
- Rapport local actualise: `docs/performance/release-performance-2026-06-13T13-29-50Z.md`. Il mesure un cold start iPhone 17 simulateur a 292.2 ms via `simctl launch`, ajoute une table min/mediane/p95/max/moyenne et garde les flows cold start, home/list, creation event, scenario matrix et WakeveAI en `PENDING_DEVICE_TRACE`.

#### 2.3 Notifications et deep links

- [x] Reprendre rich notifications seulement apres fermeture des blockers App Store.
- [x] Prioriser les deep links utiles au review flow: event, invite, poll, legal/support.
- [x] Documenter les categories/actions avant implementation si elles changent les contrats.

Preuves du 2026-06-13:

- `docs/deep-linking.md` fixe le perimetre review: rich notifications reportees apres fermeture P0 App Store, deep links prioritaires limites a event, invite, poll et legal/support.
- Le meme document inventorie les categories/actions existantes (`event_invite`, `poll_reminder`, `meeting_starting`, `scenario_vote`, `general`) et interdit l'ajout de nouveaux contrats notification/deep link sans proposition OpenSpec.
- Les liens meeting restent supportes par les parseurs existants, mais ne sont pas promus comme surface App Review critique pour la premiere soumission.

#### 2.4 Sync avancée et CRDT

- [x] Garder last-write-wins pour la release si les conflits sont comprehensibles.
- [x] Evaluer CRDT seulement sur preuve de besoin produit ou bug recurrent.
- [x] Documenter les scenarios offline critiques dans `docs/testing/`.

Preuves du 2026-06-13:

- `docs/testing/offline-critical-scenarios.md` documente la strategie offline-first, le maintien de last-write-wins pour la release, les conditions de reouverture CRDT et la matrice des scenarios critiques.
- `docs/testing/README.md` reference la matrice offline critique.
- `./gradlew :shared:jvmTest --tests com.guyghost.wakeve.auth.offline.GuestModeOfflineTest --tests com.guyghost.wakeve.offline.DeleteEventOfflineTest --tests com.guyghost.wakeve.offline.InvitationParticipantOfflineRepositoryTest --tests com.guyghost.wakeve.sync.conflict.ConflictResolutionIntegrationTest --tests com.guyghost.wakeve.scenario.ScenarioOfflineRepositoryPhase3Test --tests com.guyghost.wakeve.transport.TransportOfflineRepositoryPhase4Test --tests com.guyghost.wakeve.organization.EventOrganizationPhase5ReadinessTest --tests com.guyghost.wakeve.organization.EventOrganizationPhase56SharedOfflineRedTest --tests com.guyghost.wakeve.workflow.EventOrganizationPhase6EndToEndSyncTest --tests com.guyghost.wakeve.sync.SyncManagerTest --tests com.guyghost.wakeve.analytics.AnalyticsQueueTest` passe.

## Backlog non bloquant

- Android parity pour les features iOS-first recentes.
- Payment/Tricount avance au-dela du handoff conforme App Store.
- Cache LRU applicatif si profiling le justifie.
- Internationalisation complete FR/EN/ES/DE.
- Recommendation engine externe et providers reels transport/destination/lodging.
- Web dashboard post-release.

## Ordre recommande des prochaines sessions

1. Live release gate: DNS, AASA, backend, signed archive, TestFlight.
2. App Store Connect decisions: privacy, accessibility, availability, DSA, payment, pricing, EULA, media/localization, licenses.
3. Review-build evidence pour `add-in-app-account-deletion` et `add-ugc-moderation-controls`: verifier les flux sur la build signee/TestFlight avant de lever les markers d'evidence.
4. `add-on-device-wakeve-ai`: fermer le profiling device.
5. Regression release flow: creation, contacts, matrix scenarios, organisation, accessibility.

## Gates de sortie

### Sortie P0

- `openspec validate add-in-app-account-deletion --strict` passe.
- `openspec validate add-ugc-moderation-controls --strict` passe.
- Les blockers AS-01 a AS-22 du registre App Store sont fermes ou explicitement exclus par decision documentee.
- `./scripts/app-store-submission-audit.sh --check-live-urls --run-submission-ready` passe.
- Une build App Store Connect/TestFlight signee est uploadée, installee sur device reel, smoke testee et selectionnable pour review.

### Sortie P1

- `openspec validate add-on-device-wakeve-ai --strict` passe.
- Les validations device WakeveAI sont documentees.
- Les parcours onboarding/login/guest, creation event, contacts, poll, scenario matrix, organisation et event detail ont des preuves iOS release.
- Les checks accessibility/localization/copy des ecrans release sont rejoues.

### Sortie P2

- Les gates de tests critiques remplacent les objectifs de couverture generiques.
- Les mesures performance et observabilite sont documentees sur iOS et Android.
- Les notifications/deep links et la sync avancee ne changent pas de contrat sans proposition OpenSpec.

## Commandes utiles

```bash
openspec list
openspec list --specs
openspec validate add-in-app-account-deletion --strict
openspec validate add-ugc-moderation-controls --strict
openspec validate add-on-device-wakeve-ai --strict
./scripts/test-critical-release-gates.sh
./gradlew :shared:testWithCoverage :shared:jacocoCoverageVerification
APP_REVIEW_PHONE_NUMBER='+33123456789' ./scripts/app-store-submission-audit.sh --skip-preflight
APPLE_TEAM_ID='<TEAMID>' ./scripts/lint-store-metadata.sh --ios-only --check-live-urls
bundle exec fastlane ios preflight
./gradlew test
```

## References

- `openspec/changes/add-in-app-account-deletion/`
- `openspec/changes/add-ugc-moderation-controls/`
- `openspec/changes/add-on-device-wakeve-ai/`
- `openspec/changes/add-web-microfrontends-landing/`
- `openspec/changes/add-contact-participant-selection/`
- `openspec/changes/add-scenario-matrix-voting/`
- `openspec/changes/update-create-event-wizard-slots-preview/`
- `docs/APP_STORE_READINESS.md`
- `docs/APP_STORE_LAUNCH_CHECKLIST.md`
- `docs/APP_STORE_BLOCKER_REGISTER.md`
- `docs/implementation/prd-status.md`
