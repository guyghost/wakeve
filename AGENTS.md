Ce document définit les agents (humains et logiciels) pour l’application mobile de planification d’événements “Plan Trip” en Kotlin Multiplatform. Il décrit leurs responsabilités, interfaces et handoffs, afin d’assurer un flux clair du sondage de disponibilités jusqu’à la planification complète et la synchronisation offline-first.

Vue d’ensemble des agents ￼

- Organisateur: crée l’événement, lance le sondage, fixe la date limite, valide la date finale.

- Participant: vote, propose des créneaux (si autorisé), co-construit les détails.

- Agent Sondage & Calendrier (logiciel): gère créneaux, votes, calcul du meilleur slot, fuseaux horaires.

- Agent Destination & Logement (logiciel): agrège destinations, hébergements, expériences locales.

- Agent Transport (logiciel): suggère options en fonction de la localisation des participants.

- Agent Réunions (logiciel): génère liens Zoom/Meet/FaceTime et invite les validés.

- Agent Paiement & Tricount (logiciel): cagnotte, suivi des coûts, liens Tricount.

- Agent Sync & Offline (logiciel): cache local, résolution de conflits, statut réseau.

- Agent Sécurité & Auth (logiciel): OAuth, permissions, conformité RGPD.

- Agent Notifications (logiciel): rappels de vote, changements d’état, confirmations.

Responsabilités détaillées ￼

- Organisateur

▫ Crée l’événement, description, règles de sondage.

▫ Définit la date limite de vote et autorisations d’ajout de dates.

▫ Valide la date retenue; déclenche accès aux détails pour les validés.

- Participant

▫ Vote sur les créneaux; peut proposer des dates si autorisé.

▫ Accède aux détails complets uniquement s’il a validé la date retenue.

▫ Contribue à la destination, logement, activités, transport.

- Agent Sondage & Calendrier

▫ Normalise fuseaux horaires; évite overlaps; calcule meilleur créneau.

▫ Politique de sélection: score = pondération YES/MAYBE – NO, avec pénalités conflit TZ.

▫ Verrouille le créneau retenu après échéance; notifie les agents dépendants.

- Agent Destination & Logement

▫ Fournit liste courte de destinations et hébergements via providers mockés puis réels.

▫ Score multi-critères: coût, accessibilité, préférences votées, saisonnalité.

- Agent Transport

▫ Suggestion par participant: vols/trains/covoiturage/voiture selon localisation.

▫ Optimise arrivées proches du créneau retenu; surface coût/durée/escales.

- Agent Réunions

▫ Génère liens des réunions virtuelles; place invites pour les validés.

▫ Ajoute rappels; respecte fuseaux horaires.

- Agent Paiement & Tricount

▫ Crée cagnotte (providers externes), objectifs et suivi.

▫ Intègre Tricount via liens; affiche répartition et avances.

- Agent Sync & Offline

▫ Source de vérité locale (SQLite via SQLDelight); sync incrémentale.

▫ Stratégie de conflits: last-write-wins + horodatage; évolutif vers CRDT.

▫ Signale clairement l’état offline/online.

- Agent Sécurité & Auth

▫ Auth via OAuth (Apple/Google), tokens stockés sécurisés.

▫ Permissions pour localisation; minimisation des données; droit à l’effacement.

- Agent Notifications

▫ Rappels de vote avant échéance; confirmation de créneau retenu.

▫ Changements de planning, réunions, paiements.

Handoffs et flux ￼

1. Organisateur → Agent Sondage: création de sondage, date limite, règles.

2. Participants → Agent Sondage: votes et propositions; calcul du meilleur slot.

3. Agent Sondage → Organisateur: recommandation; validation du créneau.

4. Agent Sondage → Agents Destination/Transport/Réunions: créneau verrouillé.

5. Agents Destination/Transport → Participants: suggestions classées; co-construction.

6. Agent Réunions → Participants validés: liens, rappels.

7. Agent Paiement → Tous: cagnotte, suivi des coûts; Tricount.

8. Agent Sync & Offline ↔ Tous: persistance, sync, conflits, statut réseau.

9. Agent Notifications → Tous: événements clés, échéances, confirmations.

Interfaces techniques (KMP) ￼

- Shared domain/services: Kotlin multiplatform (sondages, scoring, timezones, cache).

- Providers abstraits: Destination/Lodging/Transport/Expérience avec implémentations mock → réelles via backend.

- Backend proxy: Ktor server pour clés API et agrégation; client Ktor commun.

- Stockage: SQLDelight; chiffrement local selon plateforme.

- UI: Compose (Android), SwiftUI (iOS) interop avec framework KMP.

Règles d’accès et visibilité ￼

- Avant validation de la date finale: vue limitée aux infos générales.

- Après validation: détails complets accessibles uniquement aux participants ayant validé.

- Logs d’audit: actions critiques (validation, paiements, modifications) tracées.

Points d’attention ￼

- Cohérence fuseaux horaires et échéances de vote.

- Transparence offline: informer clairement quand une action est en file d’attente.

- Sécurité des liens externes (meetings, cagnotte, Tricount) et anti-phishing.

- Évolution des scores multi-critères avec feedback utilisateur.

- RGPD: consentements, export/suppression de données, minimisation.

Axes d’amélioration ￼

- Passer de last-write-wins à CRDT pour édition collaborative.

- Recommandations personnalisées basées sur historique et contraintes budget/temps.

- Intégration calendrier natif (iOS/Android) et invitations ICS.

- Optimisation multi-participants pour transports synchronisés.

- Observabilité: métriques, traces, alertes sur échecs de sync et providers.