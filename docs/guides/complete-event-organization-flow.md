# Workflow complet d'organisation d'événement

Ce guide décrit le parcours produit cible couvert par l'OpenSpec `complete-event-organization-flow`: organiser un événement Wakeve de sa création à sa finalisation, avec accès sécurisé, UX Android/iOS cohérente et fonctionnement offline-first.

[<- Retour](../README.md)

## Vue d'ensemble

Le cycle de vie canonique d'un événement est:

```text
DRAFT -> POLLING -> CONFIRMED -> COMPARING -> ORGANIZING -> FINALIZED
```

`Event.status` reste la source de vérité du workflow. Les modules de scénarios, transport, réunions, budget, paiement, calendrier, notifications et synchronisation ne forcent pas directement les transitions entre eux: ils lisent l'état et les données persistées via les repositories.

Les écritures critiques suivent le même modèle:

1. Valider le statut de l'événement et l'acteur.
2. Ecrire localement dans SQLDelight.
3. Enregistrer une opération de sync rejouable quand l'action doit atteindre le serveur.
4. Afficher un état local clair si la donnée n'est pas encore synchronisée.
5. Bloquer la finalisation tant qu'une opération critique rejouable est pending ou failed.

## Parcours organisateur

### 1. Création en DRAFT

L'organisateur crée l'événement avec les informations de base, les estimations de participants, les lieux potentiels et les créneaux de sondage. Le wizard DRAFT sauvegarde localement chaque étape pour permettre la reprise après fermeture de l'app ou perte réseau.

Critères de sortie:

- titre et description renseignés;
- au moins un créneau de sondage;
- données DRAFT persistées localement;
- passage explicite vers `POLLING` via `StartPoll`.

Voir aussi: [DraftEventWizard Usage Guide](./DRAFT_WORKFLOW_GUIDE.md).

### 2. Invitations et participants en POLLING

L'organisateur partage les invitations. Les invités peuvent consulter un aperçu limité, accepter l'invitation, voter, confirmer leur intention ou décliner.

Les participants qui ne sont pas encore confirmés pour la date retenue ne doivent pas voir les détails logistiques complets. L'invitation publique ou pré-confirmée reste limitée aux métadonnées nécessaires pour décider de participer.

### 3. Votes et confirmation de date

Pendant `POLLING`, les participants votent sur les créneaux avec les choix supportés par le domaine. L'organisateur confirme un créneau valide. La confirmation:

- conserve la date finale;
- passe l'événement à `CONFIRMED`;
- déverrouille les détails réservés aux participants confirmés;
- prépare les artefacts locaux de notification et de calendrier;
- reste rejouable après reconnexion si des effets externes ne peuvent pas partir immédiatement.

La confirmation reste une action organisateur. Un participant ne peut pas confirmer la date finale pour tout le groupe.

### 4. Scénarios, destination et logement

Après confirmation, les scénarios permettent de comparer des options de destination, logement, durée et budget estimé. Le workflow doit supporter:

- création et vote de scénarios;
- sélection d'un scénario final ou décision explicite que le scénario n'est pas nécessaire;
- destination et logement sélectionnés ou marqués non nécessaires;
- accès lecture/participation réservé à l'organisateur et aux participants confirmés.

`COMPARING` représente la phase de comparaison active. La transition vers `ORGANIZING` intervient quand l'organisation détaillée peut commencer.

### 5. Transport

Le transport est planifié depuis les lieux de départ des participants vers la destination et la date confirmées. Les règles critiques sont:

- l'organisateur peut gérer le plan transport global;
- un participant confirmé peut renseigner son propre lieu de départ;
- un participant non confirmé ou inconnu ne peut pas écrire de départ ni lire les détails;
- la génération, sélection, suppression ou décision "transport non nécessaire" reste réservée à l'organisateur;
- les états Android et iOS doivent venir des repositories persistants, pas seulement d'un état en mémoire.

Les opérations de sync transport doivent être rejouables quand elles sont comptées comme pending. Les lignes d'audit non rejouables, par exemple une résolution de conflit historique, ne doivent pas bloquer la readiness ni la finalisation.

### 6. Réunions et liens virtuels

En `ORGANIZING`, l'organisateur peut créer une réunion ou indiquer qu'elle n'est pas nécessaire. Les liens externes doivent être stockés avec des métadonnées de lien sûr: provider, label affichable, URL cible, créateur, date de création et statut de vérification.

Les rappels et entrées calendrier associés ne ciblent que les participants confirmés. Les créations de réunion côté backend restent organisateur-only et scoppées à l'événement.

### 7. Budget, dépenses, paiement et Tricount

Le budget partagé participe à la readiness d'organisation. Le système doit couvrir:

- baseline budget ou décision explicite que le budget n'est pas nécessaire;
- dépenses partagées, catégories, parts et obligations de règlement;
- visibilité des règlements limitée aux personnes concernées ou à l'organisateur;
- cagnotte ou paiement externe avec lifecycle local-first;
- lien ou handoff Tricount avec métadonnées sûres;
- mutations budget/paiement/Tricount protégées par le statut `ORGANIZING` quand elles modifient l'organisation.

Les vrais providers de paiement restent derrière des abstractions. Le produit ne doit pas considérer une intégration externe comme réussie si seule une URL brute ambiguë a été stockée.

### 8. Calendrier et notifications

Les transitions clés préparent des artefacts locaux:

- confirmation de date;
- invitation calendrier ou ICS;
- rappels de sondage ou d'organisation;
- notification de réunion;
- alertes liées aux changements logistiques importants.

La création locale d'un artefact ne garantit pas que le push, l'ICS ou le calendrier natif ont déjà été acceptés par le provider. L'UI doit distinguer clairement "préparé localement", "en attente d'envoi", "échoué" et "synchronisé".

### 9. Finalisation

`FINALIZED` est l'état read-only du parcours d'organisation. La finalisation est autorisée seulement si la readiness calculée prouve que les sections critiques sont complètes ou explicitement non nécessaires.

La readiness doit notamment vérifier:

- date finale confirmée;
- participants requis et accès cohérents;
- scénario, destination et logement prêts ou non nécessaires;
- transport prêt ou non nécessaire;
- réunion prête ou non nécessaire;
- calendrier et notifications prêts ou explicitement non bloquants;
- budget, paiement et Tricount prêts ou non nécessaires;
- absence d'opération critique pending ou failed qui doit encore être rejouée;
- absence de lien externe non sûr dans les sections critiques.

Après finalisation, les détails restent consultables par l'organisateur et les participants confirmés, mais les mutations d'organisation sont bloquées sauf action post-finalisation explicitement autorisée par une spec future.

## Règles d'accès

| Zone | Organisateur | Participant invité/non confirmé | Participant confirmé | Décliné ou non membre |
| --- | --- | --- | --- | --- |
| Aperçu invitation | Oui | Oui, limité | Oui | Métadonnées publiques minimales |
| Vote de sondage | Oui | Oui si invité | Oui | Non |
| Confirmation de date finale | Oui | Non | Non | Non |
| Scénarios et destination/logement | Oui | Non | Oui | Non |
| Transport détaillé | Oui | Non | Oui, avec écriture limitée à son départ | Non |
| Réunions, budget, paiement, Tricount | Oui | Non | Oui selon portée | Non |
| Finalisation | Oui | Non | Non | Non |

Les règles d'accès doivent être appliquées à trois niveaux:

- shared KMP: repositories, state machines et policies;
- backend Ktor: routes et contrôles anti-IDOR;
- Android/iOS: masquage des détails et actions non autorisées, sans utiliser l'UI comme seule barrière de sécurité.

Les détails organisationnels ne sont visibles qu'à l'organisateur et aux participants confirmés pour la date retenue.

## Offline-first et synchronisation

### Source locale

SQLDelight est la source locale de vérité. L'app doit rester utilisable sans réseau pour les actions critiques qui peuvent être capturées localement: votes, RSVP, départ transport, sélection logistique, dépenses, décisions "non nécessaire" et finalisation quand toutes les conditions locales et sync critiques sont satisfaites.

Les écrans Android Compose et iOS SwiftUI doivent se recharger depuis les repositories persistants après restart ou entrée directe dans une route. `UserDefaults`, mémoire de navigation ou paramètres transient ne doivent pas devenir la source de vérité des sections critiques.

### Pending sync et replay

Une ligne pending doit représenter une opération que le client sait rejouer. Pour les sections critiques, les opérations attendues sont les écritures métier comme `CREATE`, `UPDATE`, `DELETE` ou `UPSERT` selon le repository.

Les règles pratiques sont:

- ne pas créer de pending sync si la validation acteur/workflow échoue;
- marquer ou exclure les lignes d'audit non rejouables pour éviter un pending éternel;
- afficher une copie utilisateur claire pour les changements locaux en attente;
- garder les payloads suffisants pour rejouer l'opération sans dépendre uniquement d'un état UI;
- rejouer après reconnexion en conservant l'ordre nécessaire par événement et section.

### Conflits

La stratégie actuelle reste last-write-wins par timestamp, avec possibilité d'évoluer vers CRDT plus tard. Une résolution de conflit peut être conservée comme audit, mais elle ne doit pas être comptée comme opération critique bloquante si elle n'est pas rejouable.

### Finalization blocked

La finalisation doit être bloquée par:

- sync critique pending et rejouable;
- sync critique failed nécessitant retry ou résolution;
- conflit non résolu sur une section critique;
- section logistique obligatoire manquante;
- lien externe critique non vérifié ou dangereux.

La finalisation ne doit pas être bloquée par:

- lignes d'audit non rejouables déjà traitées;
- logs de refus d'accès;
- états informatifs non critiques;
- artefacts explicitement déclarés non nécessaires par l'organisateur.

## Cohérence Android/iOS

Les deux plateformes doivent présenter le même contrat produit:

- mêmes statuts utilisateur pour accès refusé, pending sync, failed sync, readiness incomplète et finalisation;
- actions de mutation visibles uniquement quand l'acteur et le statut le permettent;
- sections finalisées en lecture seule;
- texte utilisateur product-ready, localisé et sans marqueurs techniques visibles comme `pendingSync`, `AccessDenied`, `readOnly` ou `viewOnly`;
- Android avec composants Compose/Material 3;
- iOS avec vues SwiftUI et composants Liquid Glass du projet.

Les ancres de tests ou constantes techniques peuvent rester en source si elles ne sont pas affichées à l'utilisateur.

## Commandes de vérification de référence

Les résultats historiques et le détail RED/GREEN sont consignés dans `openspec/changes/complete-event-organization-flow/tasks.md`. Ne pas déduire un résultat actuel de cette liste sans relancer les commandes nécessaires.

Commandes shared principales:

```bash
./gradlew :shared:jvmTest --tests 'com.guyghost.wakeve.transport.TransportOfflineRepositoryPhase4Test' --no-daemon
./gradlew :shared:jvmTest --tests '*Phase5*' --no-daemon
./gradlew :shared:jvmTest --tests '*Phase6*' --no-daemon --no-configuration-cache --rerun-tasks --max-workers=1
./gradlew :shared:jvmTest --no-daemon
```

Commandes backend principales:

```bash
./gradlew :server:test --tests '*Phase5*' --no-daemon
./gradlew :server:test --tests com.guyghost.wakeve.routes.EventOrganizationPhase4TransportRoutesTest --no-daemon
./gradlew :server:test --no-daemon
```

Commandes Android principales:

```bash
./gradlew :composeApp:testDebugUnitTest --tests com.guyghost.wakeve.navigation.TransportNavigationContractTest --no-daemon --no-configuration-cache
./gradlew :composeApp:testDebugUnitTest --tests '*Phase5*' --no-daemon --no-configuration-cache
./gradlew :composeApp:testDebugUnitTest --tests '*Phase7*' --no-daemon --no-configuration-cache
./gradlew :composeApp:compileDebugKotlinAndroid --no-daemon --no-configuration-cache
```

Commandes iOS principales:

```bash
xcodebuild test -project iosApp/iosApp.xcodeproj -scheme WakeveApp -destination 'platform=iOS Simulator,name=iPhone 17,OS=26.5' -only-testing:WakeveTests/TransportPlanningContractTests CODE_SIGNING_ALLOWED=NO
xcodebuild test -project iosApp/iosApp.xcodeproj -scheme WakeveApp -destination 'platform=iOS Simulator,name=iPhone 17,OS=26.5' -only-testing:WakeveTests/OrganizationPhase5ContractTests CODE_SIGNING_ALLOWED=NO
xcodebuild test -project iosApp/iosApp.xcodeproj -scheme WakeveApp -destination 'platform=iOS Simulator,name=iPhone 17,OS=26.5' -only-testing:WakeveTests/OrganizationPhase7ContractTests CODE_SIGNING_ALLOWED=NO
```

Validation OpenSpec et hygiène diff:

```bash
git diff --check
openspec validate complete-event-organization-flow --strict
```

## Points d'attention pour les développeurs

- Ajouter les tests avant l'implémentation pour chaque nouvelle tranche verticale.
- Vérifier les guards local et backend pour toute route ou écriture sensible.
- Ne pas faire dépendre une section critique d'un état UI transient.
- Ne pas compter une ligne de sync comme pending si elle ne peut pas être rejouée.
- Garder les décisions "non nécessaire" explicites et persistées.
- Faire relire les changements UX par `@designer` quand des écrans Android/iOS changent.
- Faire relire les accès, IDOR, replay offline et finalization readiness par `@review` avant de cocher une phase.

## Voir aussi

- [State Machine Integration Guide](./STATE_MACHINE_INTEGRATION_GUIDE.md)
- [DraftEventWizard Usage Guide](./DRAFT_WORKFLOW_GUIDE.md)
- [Calendar Integration](../integrations/calendar/implementation.md)
- [Meeting Service Architecture](../architecture/meeting-service.md)
- [Testing Overview](../testing/README.md)
- [OpenSpec change tasks](../../openspec/changes/complete-event-organization-flow/tasks.md)
