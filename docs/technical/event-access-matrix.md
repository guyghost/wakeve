# Matrice des droits des événements

T01 de la feuille de route technique : caractériser les accès existants avant le durcissement r3. Référence initiale : commit `4b8ba35fb2f8cbec5b3e85698eb4dfc85fc6e317`.

La politique est dans `shared/src/commonMain/kotlin/com/guyghost/wakeve/access/EventAccessPolicy.kt`. Les tests sont dans le même package sous `shared/src/commonTest/kotlin`. Les décisions sont pures et ne dépendent ni du réseau ni du stockage ; ces tests couvrent donc aussi leur utilisation hors ligne, sans prouver la protection des mutations ou de la synchronisation.

## Invariants attendus et caractérisés

Les lignes ci-dessous utilisent des identités cohérentes. « Métadonnées » signifie titre et identifiant de l’événement, sans détails d’organisation. L’autorisation de voter ci-dessous est celle de `canSubmitPollBallot`, pas la validation de l’état du sondage ou de son échéance.

| Acteur pour l’événement cible | Aperçu invitation | Soumission du bulletin | Sections d’organisation |
|---|---|---|---|
| Organisateur exact, absent de la liste des participants | Métadonnées, sans action rejoindre | Autorisée | Autorisées |
| Membre listé, RSVP accepté et date retenue validée | Métadonnées | Autorisée | Autorisées |
| Membre listé, RSVP accepté sans date validée | Métadonnées | Autorisée | Refus `RETAINED_DATE_NOT_VALIDATED` |
| Membre listé, RSVP accepté et date `NOT_APPLICABLE` ou `UNAVAILABLE` | Métadonnées | Autorisée | Refus `RETAINED_DATE_NOT_VALIDATED` |
| Membre listé, RSVP en attente | Métadonnées, action rejoindre | Refus `ATTENDANCE_NOT_CONFIRMED` | Refus `ATTENDANCE_NOT_CONFIRMED` |
| Membre listé, RSVP décliné | Métadonnées | Refus `PARTICIPATION_DECLINED` | Refus `PARTICIPATION_DECLINED` |
| Membre listé, RSVP `NOT_APPLICABLE` ou `UNAVAILABLE` | Métadonnées | Refus `ATTENDANCE_NOT_CONFIRMED` | Refus `ATTENDANCE_NOT_CONFIRMED` |
| Non membre, absent de la liste | Identifiant seul, titre absent, pas d’action rejoindre | Refus `NOT_EVENT_MEMBER` | Refus `NOT_EVENT_MEMBER` |
| Rôle membre mais absent de la liste, même avec RSVP accepté et date validée | Identifiant seul | Refus `NOT_EVENT_MEMBER` | Refus `NOT_EVENT_MEMBER` |

Les sections protégées sont `TRANSPORT`, `LODGING`, `BUDGET`, `PAYMENT` et `MEETING`. Les tests ajoutés vérifient leurs refus et l’accès de l’organisateur sur toutes les valeurs. L’aperçu ne révèle jamais les détails d’organisation. La matrice décrit aussi des combinaisons déduites de la lecture du code ; les tests de T01 ne prétendent pas épuiser le produit cartésien des états.

## Divergences observées à traiter dans T02

Ces constats proviennent de la lecture des branches de la politique, pas de tests exécutés de reproduction. Aucun test n’impose de conserver un accès indu.

| Entrée incohérente | Comportement actuel lu dans le code | Invariant r3 à établir |
|---|---|---|
| Rôle `ORGANIZER` avec un autre `userId` que `event.organizerId` | `invitationPreviewFor` considère le rôle seul comme connu et expose le titre | Lier le rôle organisateur à l’identité de l’organisateur de l’événement cible |
| Même rôle organisateur incohérent, mais identité présente dans `event.participants`, RSVP accepté et date validée | `canAccessOrganizationSection` poursuit vers le contrôle RSVP et peut autoriser | Refuser une identité et un rôle contradictoires avant les branches favorables |
| `event.organizerId` et `viewer.userId` vides, rôle `ORGANIZER` | La comparaison d’égalité de `canSubmitPollBallot` et `canAccessOrganizationSection` peut autoriser | Refuser les identifiants absents avant comparaison |
| Enregistrement de participation provenant d’un autre événement | `ParticipantAccessMapper` ne conserve pas `eventId` dans `ParticipantAccessState` | Vérifier la provenance de l’enregistrement à la frontière qui construit l’état ; inventorier les appelants avant de choisir le contrat |

Le bulletin possède déjà une vérification du rôle organisateur **et** de l’identité. La centralisation de T02 doit préserver cette protection, les droits légitimes ci-dessus et le refus avant effet de bord. T01 ne modifie aucun code de production et n’affirme pas qu’une entrée incohérente est exploitable depuis un appelant réel.

## Validation

Commande ciblée prévue par le projet :

```bash
./gradlew :shared:jvmTest --tests '*EventAccessPolicyTest*' --no-configuration-cache
```

La cible `jvm()` est déclarée dans `shared/build.gradle.kts`. La CI existante lance `./gradlew shared:jvmTest --no-configuration-cache` ; conserver cette suite après le contrôle ciblé.

État au 14 novembre 2026 : **validation d’exécution bloquée avant compilation, aucun test exécuté**. Le premier lancement du wrapper a échoué au téléchargement de Gradle 9.1.0 (`java.net.SocketException: Network is unreachable`). Une seconde tentative a utilisé la même distribution et le JDK JetBrains 21 demandés par le dépôt, téléchargés dans un répertoire temporaire, ainsi qu’un proxy Java temporaire hors dépôt. La configuration a ensuite échoué à `settings.gradle.kts:31` : le plugin `org.gradle.toolchains.foojay-resolver-convention:1.0.0` n’a pas pu être résolu depuis les dépôts configurés (`BUILD FAILED in 23s`, code de sortie 1).

La commande ciblée et la suite CI restent à exécuter dans un environnement disposant des dépendances. Les 7 tests ajoutés (13 tests dans `EventAccessPolicyTest`) ont fait l’objet d’une revue statique ; ils ne sont pas déclarés verts. `git diff --check` passe. Aucun code de production ni fichier de configuration du projet n’a été modifié.
