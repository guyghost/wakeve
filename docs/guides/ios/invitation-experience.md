# Architecture de l’expérience d’invitation iOS

[← Retour aux guides iOS](./README.md)

Ce guide décrit l’architecture livrée par `expand-ios-invitation-experience`. Elle relie six surfaces SwiftUI à une source de vérité KMP locale, sans déplacer les décisions métier dans les vues.

## Sources de vérité

- [Modèle comportemental](../../../models/ios-event-invitation-experience.md) : états totaux, événements, gardes, propriétaires et invariants.
- [Proposition OpenSpec](../../../openspec/changes/expand-ios-invitation-experience/proposal.md) : périmètre produit et dépendances.
- [Design OpenSpec](../../../openspec/changes/expand-ios-invitation-experience/design.md) : décisions d’architecture, migration, rollout et rollback.
- [Delta organisation](../../../openspec/changes/expand-ios-invitation-experience/specs/event-organization/spec.md), [delta collaboration](../../../openspec/changes/expand-ios-invitation-experience/specs/collaboration-management/spec.md), [delta notifications](../../../openspec/changes/expand-ios-invitation-experience/specs/notification-management/spec.md) et [delta design iOS](../../../openspec/changes/expand-ios-invitation-experience/specs/ios-design-system/spec.md).
- [Dossier de QA](../../../openspec/changes/expand-ios-invitation-experience/design-qa.md) : environnement, captures, tests, verdict et limites.

`EventManagementStateMachine` reste l’unique propriétaire de `EventStatus`. Les vues projettent des données structurées et transmettent des callbacks typés ; elles ne déduisent jamais un droit ou une transition depuis un texte, une couleur, une image ou un résultat de LLM.

## Flux d’architecture

```text
SQLDelight + DatabaseEventRepository
  -> repositories InvitationExperience
  -> snapshots, états totaux et routeur KMP
  -> ViewModels et surfaces SwiftUI
  -> commande typée vers le propriétaire
  -> transaction + operation receipt + sync queue
  -> nouvelle projection observée
```

Les lectures utilisent les repositories de projection. Les écritures passent par un use case propriétaire et une identité d’opération stable. Un callback tardif doit correspondre à son événement, acteur, révision et opération ; sinon il est ignoré.

## Les six surfaces

| Surface | Vue iOS | Source et responsabilité |
|---|---|---|
| Library | `EventLibraryView.swift` | Projections `DRAFTS`, `HOSTING`, `ATTENDING`, `UPCOMING`, `PAST`, fraîcheur et prochaine action déterministe. |
| Creation Studio | `EventCreationStudioView.swift` | Brouillon transitoire, preview de la révision courante, puis `UpdateDraftAggregate` pour le commit atomique. |
| Audience | `EventAudienceView.swift` | Snapshot cohérent des axes invitation, approbation, membership, RSVP et validation de date ; invitation directe via son owner sécurisé. |
| Event Detail | `ContentView.swift` et `EventDetailInvitationCanvas.swift` | Réutilise le canvas approuvé et route ses actions vers Poll, Participants, Organization, Information ou Archive. |
| Information | `EventInformationView.swift` | Métadonnées, destinations typées, trois axes de notification et opérations destructives gardées. |
| Archive | `EventArchiveView.swift` | Lecture seule pour `PAST` ou `FINALIZED`, avec artwork, résumé, fraîcheur et état de synchronisation. |

Les contrats partagés vivent dans `shared/src/commonMain/kotlin/com/guyghost/wakeve/invitationexperience/`. `InvitationExperiencePublicContracts.kt` contient les unions totales, reducers, owners et le routeur ; `InvitationExperienceRepositories.kt` contient les repositories SQLDelight de projection, d’invitation directe, de préférence et d’Information.

## Artwork total et migration

La valeur persistée est toujours exactement l’une des suivantes :

```text
NONE
STRUCTURED(version, PRESET | SERVER_ASSET, alt, focalPoint, crop)
LEGACY_REMOTE(validatedHttpsUrl)
```

La sélection Studio de release 1 est `KEEP_EXISTING | NONE | PRESET(presetId) | EXISTING_SERVER_ASSET(assetId)`. `KEEP_EXISTING` n’existe qu’en édition. Pour un asset serveur, Studio transmet seulement l’identifiant opaque ; l’owner résout et valide la référence canonique autorisée. Le picker photo, les fichiers locaux et l’upload sont hors périmètre.

La migration SQLDelight conserve la donnée legacy : une URL HTTPS autorisée devient `LEGACY_REMOTE`, une valeur absente ou invalide devient `NONE`, et une anomalie produit uniquement un code assaini. Le rendu utilise le fallback d’ambiance déterministe pour `NONE`, l’indisponibilité réseau ou un échec d’image, sans changer l’état, l’accès ou l’action.

`UpdateDraftAggregate` relit l’événement, vérifie organisateur, `DRAFT`, révision attendue, artwork et opération, puis écrit atomiquement l’agrégat, la nouvelle révision, le receipt et l’opération de sync. Les writers mixed-version doivent préserver ces champs ou être bloqués par le fence de version. Un rollback UI ne réactive jamais un writer ancien non protégé.

## Invitation directe et partage public

L’invitation directe DRAFT est détenue par `DirectInviteProductionOwner` et les contrats KMP :

1. l’entrée destinataire reste transitoire ;
2. `KeychainDirectInviteRecipientDigestPort` produit une clé pseudonyme par HMAC avec matériel aléatoire protégé ;
3. `DirectInviteAEADSealer` scelle l’enveloppe de livraison liée à l’opération ;
4. le repository persiste batch, enveloppe protégée et outcomes idempotents ;
5. `DirectInviteAuthenticatedTransport` appelle la route serveur détenue par `DirectInviteDeliveryRoutes.kt` ;
6. retry et annulation ne rejouent que les destinataires non résolus et revalident la capability.

Aucun email brut n’est une clé persistée, un identifiant UI, un log ou une donnée analytics. La rétention, la suppression de l’événement et l’effacement de compte suppriment ou anonymisent les valeurs protégées.

Le partage public est une capacité distincte. `harden-event-invitation-links` reste le seul owner de l’émission, de la rotation, de la révocation et du redeem. Tant qu’il ne fournit pas une capability opaque liée à `(eventId, actorId, accessRevision, capabilityId)`, le partage reste masqué ou honnêtement indisponible. Les six nouvelles surfaces ne construisent ni token, ni URL redeemable, ni QR local.

## Notifications : trois axes séparés

Event Information affiche sans les fusionner :

- `EventNotificationPreference`, seul axe éditable ici ;
- `AccountNotificationPreference`, lu depuis le propriétaire de compte ;
- `SystemNotificationAuthorization`, lu par `EventInformationSystemAuthorizationAdapter`.

L’autorisation système est totale : `UNAVAILABLE | NOT_DETERMINED | PROVISIONAL | AUTHORIZED | EPHEMERAL | DENIED | RESTRICTED`. `UNAVAILABLE` signifie qu’aucun snapshot OS n’est encore disponible. Cet état bloque la livraison, n’affiche aucun faux consentement, ne déclenche aucun prompt et ne modifie ni préférence événementielle ni préférence de compte. Seul un callback typé du port iOS peut le remplacer.

L’ordre effectif est : gate OS, politique de sécurité critique, types activés au compte, intersection événementielle, puis quiet hours. `ALL_EVENT_UPDATES` ne réactive jamais un type désactivé au compte.

## Routage global en lecture seule

Avant toute route, menu, deep link ou callback, le routeur calcule la classe temporelle depuis les bornes structurées et une horloge fournie. `PAST` ou `FINALIZED` force `READ_ONLY` et `VIEW_ARCHIVE`. Si Archive n’est pas disponible, le repli est un résumé local en lecture seule.

La règle s’applique aussi à `DeleteEvent`. Un événement historique non finalisé ne propose plus la suppression UI ; le propriétaire relit l’événement et rejette une action obsolète avec une horloge de confiance. Une éventuelle remédiation support/privacy passe par l’autorité existante et ne mute pas le cycle de vie.

## Rollout et rollback

Le flag `iosInvitationExperienceV1` est `false` par défaut dans `ContentView`. En production, seule une configuration explicite ouvre les six nouvelles surfaces. Le support QA DEBUG peut l’activer avec :

```text
--wakeve-qa-seed-invitation-experience
--wakeve-qa-open-invitation-route <library|studio|audience|information|archive>
```

Ce hook remplit les vrais repositories locaux et utilise les routes typées ; il ne compile pas en Release. Le flag DEBUG `--wakeve-qa-reduce-transparency` sert seulement à capturer le fallback lorsqu’un réglage Simulator n’atteint pas SwiftUI. Il est combiné par OR au signal iOS et ne le remplace pas.

Le rollback désactive seulement le routage des nouvelles surfaces. Les migrations, données, receipts, fences mixed-version, règles de cascade, rétention et sync restent actifs. Aucune down-migration destructive n’est autorisée et le canvas Event Detail existant demeure le repli stable.

## Carte des fichiers

- `shared/src/commonMain/kotlin/com/guyghost/wakeve/invitationexperience/InvitationExperienceContracts.kt`
- `shared/src/commonMain/kotlin/com/guyghost/wakeve/invitationexperience/InvitationExperiencePublicContracts.kt`
- `shared/src/commonMain/kotlin/com/guyghost/wakeve/invitationexperience/InvitationExperienceRepositories.kt`
- `shared/src/commonMain/sqldelight/com/guyghost/wakeve/InvitationExperience.sq`
- `shared/src/commonMain/sqldelight/com/guyghost/wakeve/migrations/8.sqm` à `11.sqm`
- `iosApp/src/Views/Invitations/`
- `iosApp/src/Services/DirectInviteRecipientKeyOwnerProvider.swift`
- `iosApp/src/Services/EventInformationSystemAuthorizationAdapter.swift`
- `iosApp/src/Services/InvitationDeepLinkResolver.swift`
- `iosApp/src/Services/InvitationExperienceQALaunchSupport.swift`
- `server/src/main/kotlin/com/guyghost/wakeve/routes/DirectInviteDeliveryRoutes.kt`

## Voir aussi

- [Migration du canvas Event Detail](./event-detail-invitation-canvas.md)
- [Design system iOS](./design-system.md)
- [Tests](../../testing/README.md)
