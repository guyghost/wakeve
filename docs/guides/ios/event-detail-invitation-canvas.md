# Migration de l’Event Detail vers l’Invitation Canvas

[← Retour aux guides iOS](./README.md)

Ce guide décrit la migration interne de l’écran Event Detail iOS vers l’option graphique « Affiche vivante ». Il complète le [design system iOS](./design-system.md) sans modifier le workflow métier : le canvas projette un état structuré, puis émet uniquement des navigations typées.

## Sources de vérité

- [Modèle de présentation](../../../models/ios-event-detail-invitation-canvas.md) : matrice état × accès, entrées et invariants.
- [Revue du modèle](../../../models/ios-event-detail-invitation-canvas.review.md) : cas nominaux, erreurs, permissions, offline, états terminaux et accessibilité.

La vue ne devient pas une nouvelle machine à états. `EventStatus`, les permissions du repository et les modèles métier existants restent autoritatifs. Aucun libellé, contenu généré ou résultat de LLM ne peut déterminer un état, un droit, un acteur responsable ou une destination.

## Architecture du canvas

Le premier viewport forme une composition continue :

1. image de l’événement ou gradient d’ambiance ;
2. contrôles natifs retour, partage et menu ;
3. titre, date et état du cycle de vie ;
4. organisateur et synthèse structurée des participants ;
5. acteur responsable et prochaine action utile ;
6. exactement une action primaire visible.

Les modules météo, anticipation, suggestions IA révisables, readiness, participants, organisation, invitations et messages restent dans leur ordre existant sous le canvas. Ils conservent leurs gardes d’accès et leur propriétaire métier.

### Frontière native et identité Wakeve

| Couche | Responsabilité |
|---|---|
| Chrome iOS | Navigation, partage, menu, symboles SF, cibles tactiles de 44 points et comportements d’accessibilité. |
| Identité Wakeve | Image, palette d’ambiance, scrim, hiérarchie typographique, statut, participants et tonalité localisée. |
| Projection | Sélection déterministe du mode, de l’acteur, des sections visibles et de l’action typée. |
| Flux propriétaires | Mutation métier, permissions, synchronisation, émission d’invitation et retries réseau. |

La vue peut demander une navigation. Elle ne confirme pas une date, n’enregistre pas un vote et ne fait avancer aucun statut elle-même.

## Mapper déterministe état × accès

Le mapper couvre chaque combinaison de `DRAFT`, `POLLING`, `CONFIRMED`, `COMPARING`, `ORGANIZING` et `FINALIZED` avec :

- `ORGANIZER` ;
- `ELIGIBLE_PARTICIPANT` ;
- `RESTRICTED_PARTICIPANT` ;
- `NON_PARTICIPANT`.

Les conditions supplémentaires sont elles aussi structurées : disponibilité des créneaux en DRAFT, responsabilité de vote en POLLING, readiness, fraîcheur et synchronisation du sujet affiché. Une seule ligne de la matrice doit correspondre et produire un seul `canvasMode`.

| Cycle | Projection principale |
|---|---|
| DRAFT | L’organisateur modifie le brouillon ; les autres accès reçoivent un état d’accès en lecture seule. |
| POLLING | L’organisateur voit les résultats ; un participant éligible vote ou consulte les résultats selon son état de vote. |
| CONFIRMED | Les accès autorisés comparent les options ou poursuivent l’organisation selon la priorité typée. |
| COMPARING | Les accès autorisés ouvrent la comparaison. |
| ORGANIZING | Les accès autorisés ouvrent le prochain élément incomplet de readiness. |
| FINALIZED | Tous les parcours sont en lecture seule ; aucune action mutante n’est permise. |

Toute donnée auxiliaire manquante ou en erreur supprime la revendication qu’elle ne permet pas de prouver. Elle ne doit jamais fabriquer un nombre de participants, une confirmation, une responsabilité ou une readiness.

## Actions et placement adaptatif

Les seules destinations du canvas sont :

- `EDIT_DRAFT` ;
- `SUBMIT_VOTE` ;
- `VIEW_POLL_RESULTS` ;
- `COMPARE_OPTIONS` ;
- `CONTINUE_ORGANIZATION` ;
- `VIEW_FINAL_DETAILS` ;
- `SHOW_ACCESS_STATE` ;
- `SHOW_DETAILS`, repli local toujours sûr.

Une action émet le callback du flux propriétaire ; son libellé localisé n’est jamais interprété comme une commande. En état `FINALIZED`, seules `VIEW_FINAL_DETAILS`, `SHOW_ACCESS_STATE` et `SHOW_DETAILS` sont autorisées.

Le placement est exclusif :

- `IN_CANVAS` aux tailles standard ;
- `PERSISTENT_SAFE_AREA` lorsque la hauteur compacte ou Dynamic Type accessibilité empêche de garder le contenu sémantique et l’action dans le premier viewport.

Les deux branches réutilisent la même identité d’action et le même callback. Elles ne doivent jamais rendre deux boutons simultanément. L’ordre VoiceOver reste : état, titre et date, organisateur, participants, acteur/prochaine action, action primaire, détails.

## Image et mode dégradé

`event.heroImageUrl` est utilisé lorsqu’une image valide est disponible. Le chargement, l’absence, l’échec et l’indisponibilité hors ligne utilisent immédiatement `EventMoodPalette` avec le même scrim, les mêmes informations et la même action.

Le fallback est non bloquant : un échec d’image ne change ni `canvasMode`, ni permissions, ni destination. Les noms, portraits et données « Annecy » de la référence restent limités aux previews et snapshots ; ils ne sont jamais injectés comme valeurs par défaut d’un événement réel.

L’image est décorative si elle ne porte aucune information distincte du texte. Sinon, sa description d’accessibilité vient d’une métadonnée structurée et localisée.

## Partage sécurisé

Le canvas consomme la capacité typée détenue par le changement `harden-event-invitation-links` :

- `HIDDEN` ;
- `REQUESTING` ;
- `READY(serverIssuedPayload)` ;
- `UNAVAILABLE(reason)` ;
- `FAILED(reason)`.

Le contrôle n’est actif que pour un utilisateur autorisé et un payload serveur prêt. Au tap, la vue transmet ce payload opaque au callback sécurisé. Elle ne doit pas appeler `InvitationTokenCodec`, construire une URL locale, dériver un token, le journaliser ou le persister.

Tant que le backend ne fournit pas cette capacité, le comportement de production correct est de masquer le contrôle ou d’afficher son indisponibilité réelle. Il n’existe aucun repli vers un lien prévisible.

## Liquid Glass et compatibilité

Sur iOS 26 et versions ultérieures, les contrôles circulaires et la surface compacte de prochaine action peuvent utiliser les API Liquid Glass natives, regroupées lorsque nécessaire. Le contenu long du hero et les sections secondaires restent sur des surfaces opaques ou teintées afin de préserver la lecture.

Pour les versions antérieures, Reduce Transparency ou un contraste renforcé, utiliser les matériaux et fonds opaques existants. Le fallback doit conserver :

- les mêmes libellés et la même hiérarchie sémantique ;
- une cible d’au moins 44 points pour chaque contrôle ;
- un contraste stable en clair et sombre ;
- une mise en page prévisible avec Dynamic Type ;
- aucun mouvement indispensable lorsque Reduce Motion est actif.

## Séquence de migration

1. Introduire le mapper pur et vérifier la totalité de la matrice.
2. Construire le hero et son fallback sans donnée de fixture en production.
3. Rendre organisateur et participants depuis un même snapshot structuré.
4. Remplacer les anciens blocs concurrents du premier viewport par un seul canvas et une seule action primaire.
5. Conserver les sections secondaires et leurs gardes sous le canvas.
6. Brancher le partage uniquement sur la capacité serveur autorisée.
7. Ajouter les clés localisées et vérifier les chaînes longues.
8. Valider contrats, accessibilité, build et fidélité visuelle avant de retirer l’ancienne composition.

## Vérification et limite sans simulateur

Sans simulateur configuré et démarré, la vérification peut couvrir la matrice du mapper, les contrats source, les localisations, l’absence de génération locale d’invitation, la compilation pour une destination générique et les invariants statiques d’accessibilité. Elle ne peut pas valider de façon fiable le crop réel, la hiérarchie au viewport cible, le rendu Liquid Glass, les tailles tactiles, l’ordre VoiceOver effectif, le paysage ou les réglages système d’accessibilité.

Avant de déclarer la migration visuellement terminée, il faut donc :

1. configurer et démarrer un simulateur iPhone pris en charge ;
2. capturer la fixture Event Detail au viewport cible ;
3. comparer la capture à la direction graphique approuvée ;
4. tester hauteur compacte, paysage, Dynamic Type accessibilité, contraste accru, Reduce Motion, Reduce Transparency et VoiceOver ;
5. consigner la comparaison et obtenir `final result: passed` dans le rapport de design QA.

## Voir aussi

- [Design system iOS](./design-system.md)
- [Guides iOS](./README.md)
- [Documentation des tests](../../testing/README.md)
