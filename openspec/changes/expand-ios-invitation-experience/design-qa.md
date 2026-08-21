# Design QA — iOS invitation experience

## Statut

- Date de clôture visuelle : 2026-08-21.
- Application : Wakeve 1.0, build 1.
- Verdict designer : **APPROUVÉ**.
- Findings ouverts après la passe finale : **P0 0 / P1 0 / P2 0**.
- Portée du verdict : hiérarchie, crop, densité, chrome natif, artwork, lisibilité, clarté d’état et priorité d’action des six surfaces sur les parcours repository-backed capturés.

Ce verdict ne remplace pas les audits manuels absents listés dans la section Limites.

## Sources canoniques

Les références Mobbin sont une autorité visuelle, jamais une source de logique ou de données :

- [Apple Invites — Library](https://mobbin.com/screens/d04f0876-4c6c-461e-bcba-ce6fb752ea95)
- [Apple Invites — Creation](https://mobbin.com/screens/e07fe9b7-1409-4345-9df0-56806b591ba6)
- [Apple Invites — Contextual modules](https://mobbin.com/screens/e5d7db23-34bd-4aa5-bbbf-6d74bd65e18e)
- [Apple Invites — Share and audience](https://mobbin.com/screens/d726cfff-2f7f-406c-b56a-b88fafcade02)
- [Apple Invites — Approvals](https://mobbin.com/screens/93e6c76c-c79b-4494-8cff-b61c72736251)
- [Apple Invites — Settings](https://mobbin.com/screens/c77b278e-3957-4b76-a1f7-60580a313347)
- [Apple Wallet — Stack](https://mobbin.com/screens/30c73314-7baa-46a1-9419-155aa6282488)
- [Apple Wallet — Detail](https://mobbin.com/screens/e1061c73-0be8-42ba-aaed-3bb1c3321a4e)
- [Apple Wallet — Information](https://mobbin.com/screens/f3ce79fa-8661-4833-a601-04c40cc95ef5)
- [Apple Wallet — Archive](https://mobbin.com/screens/4e2bd1e9-9a04-46ba-9bd9-db7093ace671)

Apple Invites inspire le workflow privé. Apple Wallet inspire uniquement l’architecture d’information. Aucun pass, paiement, admission ou QR local n’est adopté.

Sources Wakeve : [modèle](../../../models/ios-event-invitation-experience.md), [design](./design.md), [proposition](./proposal.md) et [guide d’architecture](../../../docs/guides/ios/invitation-experience.md).

## Environnement de capture

| Élément | Valeur |
|---|---|
| App | Wakeve 1.0 (1), configuration DEBUG |
| Outil | Xcode 27.0, build 27A5194q |
| Simulateur | iPhone 16 Pro |
| Runtime | iOS 26.5 |
| Capture native | 1206×2622 px |
| Données | Vrais repositories SQLDelight peuplés par le support QA DEBUG, pas des previews SwiftUI |
| Rollout | `iosInvitationExperienceV1` default-off ; opt-in explicite du hook DEBUG |

Le lancement QA utilise `--wakeve-qa-seed-invitation-experience` et une route typée fournie à `--wakeve-qa-open-invitation-route`. Les IDs `qa-invitation-*`, l’artwork total, les participants, les outcomes d’invitation et la préférence événementielle sont écrits dans les repositories réels avant montage de la surface.

## Captures finales repository-backed

Les artefacts de cette session sont conservés dans `/private/tmp/wakeve-ios-final/qa-seeded-assets/`. Ils ne sont pas des données de production.

| Capture | Surface / preuve |
|---|---|
| `40-studio-title-latest.png` | Studio EDIT_EXISTING : artwork réel, titre et métadonnées essentielles dans le premier viewport, action persistante accessible. |
| `41-detail-title-plate-latest.png` | Event Detail : artwork sunset, title plate opaque, badges de date/état, sync non portée par la couleur seule, safe chrome. |
| `42-detail-reduce-partial-artwork-latest.png` | Event Detail : branche Reduce Transparency, artwork conservé sous scrim partiel et surfaces locales opaques. |
| `43-library-wrapping-filters-latest.png` | Library : cinq projections visibles avec filtres adaptatifs sans rail horizontal coupé, cartes différenciées et une prochaine action. |
| `44-audience-composer-latest.png` | Audience : identités et axes séparés, composer destinataire placé avant les diagnostics, action directe honnêtement indisponible sans capability. |
| `45-information-realistic-copy-latest.png` | Information : copie domaine réelle, préférence événementielle au premier niveau, axes secondaires en disclosure, destinations et actions gardées. |
| `46-archive-distinct-labels-latest.png` | Archive : artwork, lecture seule, statut/date/organisateur, fraîcheur et synchronisation avec libellés distincts. |

Chaque fichier a été contrôlé avec `sips` à 1206×2622 px.

## Historique des corrections

| Sévérité | Finding observé pendant les passes | Correction et preuve finale |
|---|---|---|
| P0 | Aucun finding P0 n’a été retenu dans le cycle final. | Compteur final P0 : 0. |
| P1 | Studio repoussait titre/date/lieu/organisateur hors du premier viewport ; Detail dépendait trop du contraste de l’image et son chrome pouvait empiéter sur la safe area ; les filtres Library pouvaient être coupés horizontalement. | Preview Studio restructurée autour de l’artwork et des métadonnées essentielles ; title plate, badges et next-action opaques ; seul le paint layer ignore la safe area ; grille de filtres adaptative. Captures 40, 41 et 43. |
| P2 | Audience donnait la priorité aux axes diagnostiques avant la saisie ; Information exposait trop d’axes et une copie technique ; Archive répétait l’état lecture seule au lieu de distinguer fraîcheur/sync ; les presets et le chrome manquaient de différenciation. | Composer Audience remonté et encadré ; disclosure native des axes secondaires ; copie domaine ; libellés Archive distincts et localisés ; trois bitmaps preset et chrome compact. Captures 44, 45 et 46. |
| P2 accessibilité | Le fallback Reduce Transparency n’était pas visuellement distinct et le contraste sur artwork clair dépendait du verre. | Scrim partiel global, surfaces de lecture opaques, signal système conservé et override DEBUG combiné par OR. Captures 41 et 42. |

Les contrats automatisés `InvitationExperienceDesignerFindingsRedTests` figent ces corrections, notamment le premier viewport Studio, le contraste Detail, le wrap Library, le composer Audience, la disclosure Information et les libellés Archive.

## Accessibilité observée

- Dynamic Type et tailles accessibilité : contrats de layout SwiftUI et captures de session Studio aux tailles accessibilité ; aucun deuxième CTA primaire n’est introduit.
- Contraste accru : capture Audience dédiée dans la même session et surfaces Detail opaques sur artwork clair.
- Reduce Transparency : preuve finale par `42-detail-reduce-partial-artwork-latest.png`. Le chemin est activé par le signal OS ou, pour la capture DEBUG, par `--wakeve-qa-reduce-transparency` combiné par OR au signal OS.
- Reduce Motion : les contrats source interdisent une animation indispensable, mais aucun passage manuel complet n’est revendiqué.
- Cibles tactiles, libellés et actions : contrats XCTest sur les six surfaces.
- Localisations : catalogues en/fr/es/it/pt valides et tests de parité/copie longue passants.

## Rollout, owner sécurisé et rollback

Le rollout de production est `default-off` via `@AppStorage("iosInvitationExperienceV1")`. Le support QA peut l’activer uniquement dans une branche DEBUG avant de seed les repositories et résoudre les routes. Ce chemin n’est pas compilé en Release.

L’invitation directe ne dérive pas sa capability dans SwiftUI. `DirectInviteProductionOwner` fournit la capability ; la clé destinataire est un HMAC avec secret Keychain, l’enveloppe est scellée par AEAD, persistée avant `PENDING_SYNC`, puis transmise au backend authentifié. Aucun contact brut n’est une clé ou un diagnostic. Le partage public reste séparé et indisponible tant que `harden-event-invitation-links` ne fournit pas sa capability liée.

Le rollback coupe uniquement les routes de la nouvelle expérience. Les migrations, agrégats, artwork, receipts, enveloppes protégées, préférences, fences mixed-version, cascade et sync restent actifs. Il n’existe aucune down-migration destructive.

## Résultats de vérification

| Vérification | Résultat |
|---|---|
| XCTest iOS | 177 tests distincts sur deux actions réussies non chevauchantes : 153/153 + 24/24 |
| Shared ciblé invitation experience | 131/131 passants |
| Serveur | 32/32 passants |
| Shared complet | 1764/1767 passants ; 3 échecs APNs hors changement |
| Localisations `plutil -lint` | 5/5 catalogues valides |
| OpenSpec | `openspec validate expand-ios-invitation-experience --strict` valide |
| Diff | whitespace et `git diff --check` valides |

La preuve iOS est composée de deux result bundles distincts ; aucun xcresult unique ne prouve les 177 tests :

- 153/153 : `/private/tmp/wakeve-tests-latest-red/Logs/Test/Test-WakeveApp-2026.08.21_08-30-53-+0200.xcresult`.
- 24/24, soit `FindingsRegression` 21 + `Localization` 3 : `/private/tmp/wakeve-tests-audit-missing-24-20260821.xcresult`.

Les trois échecs Shared complets appartiennent au changement APNs, pas à cette expérience :

- `APNsProductionDeliveryRedTest.providerFailureIsNotAbsorbedAsSuccessfulAcceptance`
- `APNsProductionDeliveryRedTest.sentAtRemainsNullUntilProviderReturnsHttp200Acceptance`
- `APNsProductionDeliveryRedTest.registeringTwoIosInstallationsDoesNotOverwriteTheFirstDevice`

## Limites et tâches volontairement ouvertes

- Aucun audit VoiceOver manuel instrumenté n’a été exécuté sur les six surfaces.
- Le paysage n’est pas prouvé par une capture ou un parcours manuel.
- Reduce Transparency est prouvé par l’override DEBUG combiné par OR au signal OS et par les contrats source ; une activation manuelle du réglage système n’est pas revendiquée.
- Le comportement `AppStorage` du rollout n’a pas été validé sur une installation propre avec redémarrage réel. Le default-off et l’opt-in DEBUG sont couverts par source/tests.
- Les captures 40–46 sont des artefacts de session sous `/private/tmp`, pas des assets versionnés.
- En conséquence, seules les tâches OpenSpec 5.5 et 5.7 restent ouvertes.

Dans la portée visuelle et fonctionnelle repository-backed effectivement vérifiée, le designer approuve la livraison avec zéro P0, P1 ou P2 ouvert.

final result: passed
