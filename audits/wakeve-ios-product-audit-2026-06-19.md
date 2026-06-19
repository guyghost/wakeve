# Audit produit et UX iOS Wakeve

Date: 2026-06-19
Plateforme auditee: iOS app, code SwiftUI dans `iosApp/src`, specs OpenSpec, design system iOS, build local.

## Limites de preuve

- Aucun simulateur iOS n'etait demarre. `xcrun simctl list devices booted` ne listait aucun device sous iOS 18.4, 26.5 ou 27.0. Je n'ai donc pas pu faire d'audit visuel runtime, animations, gestures, VoiceOver ou Dynamic Type sur screenshots.
- Build: `xcodebuild build -project iosApp/iosApp.xcodeproj -scheme WakeveApp -configuration Debug -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO` echoue car `Shared.framework` ne contient pas l'architecture `x86_64`. Le build arm64 simulator passe avec `ARCHS=arm64 ONLY_ACTIVE_ARCH=YES`. C'est acceptable pour un Mac Apple Silicon, mais fragile pour CI, previews et environnements Intel.
- Les constats ci-dessous sont bases sur lecture de code, specs, design system, structure produit et comparaison avec les standards actuels du marche.

## Verdict executif

Je ne montrerais pas Wakeve demain a une keynote Apple dans cet etat. Le produit a une ambition forte et des briques reelles: creation, sondage, votes, resultats, participants, scenarios, transport, budget, reunions, messages, auth, offline. Mais l'experience iOS ressemble encore a une collection de modules avances plus qu'a un produit qui fait gagner immediatement du temps a un organisateur.

Le probleme n'est pas le manque de fonctionnalites. C'est l'absence d'un chemin magique, fiable et continu: creer, inviter, voter, decider, annoncer, organiser. Pour quitter WhatsApp, Wakeve doit etre plus simple que WhatsApp sur les petits evenements et beaucoup plus rassurant sur les voyages. Aujourd'hui, il est parfois plus complet, mais pas assez direct.

Scores severes:

- Produit global: 58/100
- UX organisateur: 55/100
- UX invite: 61/100
- Proposition de valeur: 52/100
- Apple Quality percue: 64/100
- Wow effect: 38/100
- Retention probable: 48/100
- Readiness demo externe: 46/100

## Les 12 problemes critiques

1. L'invitation ne semble pas etre un objet social partageable en un geste. Le flux post-creation envoie vers la gestion des participants (`iosApp/src/Views/App/ContentView.swift:185`) et exige au moins un participant pour lancer le sondage (`iosApp/src/Views/Events/ParticipantManagementView.swift:534`). Il faut un lien partageable, une preview belle, une action "Envoyer sur WhatsApp/iMessage", et un etat clair "les invites ont recu ca".

2. Les deep links existent mais ne pilotent pas la navigation. `DeepLinkService` publie `navigationPath`, mais `iOSApp.swift` indique encore que `ContentView` devrait l'observer (`iosApp/src/iOSApp.swift:75`). C'est un bloqueur viral: une invitation recue doit ouvrir le bon evenement, le bon vote ou l'acceptation.

3. La creation est trop exigeante pour les petits evenements. Le titre et la description sont obligatoires (`CreateEventSheet.swift:1181`), puis au moins un creneau (`CreateEventSheet.swift:1186`). Pour un restaurant, cinema ou watch party, la description obligatoire cree de la friction inutile.

4. Le wizard iOS ne correspond pas a la promesse spec. Le projet decrit un wizard DRAFT en 4 etapes, mais l'app iOS expose seulement `name`, `date`, `confirm` (`CreateEventSheet.swift:1390`). Les participants, lieux, previews et options de type scenario sont enfouis ou partiels.

5. La destination saisie dans la creation n'est pas transmise a la creation de l'evenement. `selectedLocation` est validee pour le mode scenario, mais `createEvent()` n'envoie que title, description, slots, participants, planningMode et eventType (`CreateEventSheet.swift:1365`). Pour un week-end, voyage ou EVJF, c'est une perte de contexte majeure.

6. L'IA est une bonne idee, mais elle ne reduit pas encore assez le travail. `applySmartEventDraft()` remplit titre, description, lieu et participants, mais ne materialise pas les options de date ni les checklists dans le flux (`CreateEventSheet.swift:1315`). Cela ressemble plus a une aide de texte qu'a un assistant d'organisation.

7. Les scenarios sont beaux, mais le point de depart est vide. `ScenarioOrganizationView` affiche loading, empty state, ranking et comparaison (`ScenarioOrganizationView.swift:210`), mais l'etat vide organisateur ne propose aucune action de creation ou generation (`ScenarioOrganizationView.swift:316`). Un organisateur peut arriver sur "Options ouvertes" sans savoir quoi faire.

8. Le transport est structure, mais peut s'auto-bloquer. Quand aucune destination n'est selectionnee, le CTA affiche "Choisir une destination" (`TransportPlanningView.swift:557`) tout en etant desactive parce que `selectedDestination == nil` rend `primaryActionDisabled` vrai (`TransportPlanningView.swift:589`). Le libelle promet une action que l'utilisateur ne peut pas executer.

9. Le budget arrive trop tard pour les voyages. Les budgets partages sont plutot accessibles dans les phases organisation/finalisees, alors que le cout est une variable de decision pendant le choix de destination, logement et transport. Pour un voyage de groupe, le cout doit apparaitre avant le vote final.

10. La cagnotte et Tricount ne sont pas au niveau du reste de l'app. `PaymentPotView` est une `List` basique avec une cagnotte creee a `goalAmount: 0` et provider `TRICOUNT` (`ContentView.swift:883`). Cela donne une impression de placeholder, pas de fonctionnalite de confiance.

11. L'onglet "Groupes" affiche une experience Explore/templates. `WakeveTab.groups` s'appelle "Groupes" (`WakeveTab.swift:17`), mais `ContentView` y monte `ExploreTabView` (`ContentView.swift:642`). Le detail de template appelle bien `onCreateEvent(scenario)` (`ExploreScenarioDetailView.swift:159`), mais le callback ignore le scenario et ouvre une creation vierge (`ContentView.swift:643`). C'est une opportunite produit gaspillee.

12. La qualite visuelle est inegale. Les ecrans evenement/scenario/transport utilisent des composants premium, mais budget, cagnotte, Tricount et reunions retombent dans des listes natives utilitaires. Pour une demo Apple, cette rupture de densite et de langage visuel se voit.

## Audit par scenario

| Scenario | Ce qui marche | Ce qui manque ou bloque | Satisfaction |
| --- | --- | --- | --- |
| Watch party | Creation rapide possible, sondage utile si date ouverte | Trop lourd si la date est deja connue, description obligatoire, pas de vibe sociale forte | 5/10 |
| Restaurant | Creneaux et participants fonctionnent | Pas de lieu obligatoire, pas de reservation, pas de menu, pas de "qui vient vraiment" partageable en lien | 5/10 |
| Cinema | Creneaux OK | Pas de film/seance, pas de ticketing, pas de decision simple "on prend cette seance" | 4/10 |
| Bowling | Invite + RSVP possibles en theorie | Pas d'option lieu/heure pre-remplie, pas de confirmation fun ou partageable | 5/10 |
| Soiree amis | Partiful et Apple Invites sont plus emotionnels et plus rapides | Wakeve est trop gestionnaire, pas assez invitation/poster/photos/messages | 4/10 |
| Anniversaire | Templates Explore utiles en idee | Template non applique a la creation, pas d'album, playlist, cadeaux, relances simples | 5/10 |
| Barbecue | Participants, equipement et repas existent dans le code | Parcours pas assez integre, les responsabilites doivent etre automatiques et visibles | 6/10 |
| EVG | Scenarios, budget, transport pertinents | Creation des scenarios bloque, budget trop tardif, pas de roles, confidentialite ou votes anonymes | 5/10 |
| EVJF | Meme potentiel que EVG | Manque emotion, moodboard, choix logement, paiement clair, teasing et annonce | 5/10 |
| Week-end mer | Mode scenario pertinent | Destination mal persistee, pas de generation de scenarios visible, transport bloque sans scenario final | 5/10 |
| Week-end ski | Budget, logement, transport, equipement sont essentiels | Pas de parcours "ski" complet, pas de forfaits, voitures, materiel, chambres | 4/10 |
| Road trip | Transport pourrait devenir differentiant | Pas d'itineraire multi-etapes, points de depart et stops insuffisamment centraux | 4/10 |
| Voyage international | Fuseaux, participants, budget sont dans l'ambition | Pas de devises, vols, documents, passeports, contraintes pays, traduction, offline voyage | 3/10 |
| Voyage multi-logements | Scenarios/logement existent dans le domaine | UI de comparaison/logement pas assez operationnelle, pas de rooming robuste | 3/10 |
| Voyage budget partage | Budget + Tricount sont presents | Cagnotte placeholder, couts trop tard, pas de confiance paiement | 4/10 |
| Participants multi-pays | KMP et timezone sont des fondations | UX ne rend pas les fuseaux et disponibilites visibles, pas de localisation mature | 3/10 |

## Proposition de valeur

Pourquoi quitter WhatsApp aujourd'hui: pour un sondage de date propre et pour centraliser certains modules d'organisation.

Pourquoi ne pas quitter WhatsApp: WhatsApp est deja le canal d'invitation, de relance et de decision. Wakeve ne gagne pas encore le premier geste social. Tant que l'invitation Wakeve n'est pas plus belle, plus rapide et plus fiable qu'un message WhatsApp, l'organisateur va revenir au chat.

Pourquoi revenir dans Wakeve: uniquement si l'evenement devient complexe et que les modules scenario, transport, budget et checklist sont vraiment connectes. Aujourd'hui, ces modules promettent une valeur forte mais leurs parcours iOS ne sont pas assez continus.

La fonctionnalite "impossible de revenir en arriere" n'existe pas encore. Le candidat le plus fort serait: "decris ton evenement en une phrase, Wakeve cree l'invitation, les options, le sondage, la checklist, le budget estime, le plan de transport et le message WhatsApp pret a envoyer". Le code a des premisses avec Wakeve AI, scenarios et transport, mais pas l'execution end-to-end.

## Audit emotionnel par ecran

| Ecran | Emotion actuelle | Score |
| --- | --- | --- |
| Onboarding | Clair, mais generique. Promet plus qu'il ne prouve. | 6.5/10 |
| Login | Fonctionnel, guest utile. Peu identitaire. | 5.5/10 |
| Home | Bon sentiment de controle quand il y a des evenements. Drafts trop caches. | 7/10 |
| Creation | Moderne, mais trop lourde pour petits evenements. IA cachee. | 6/10 |
| Preview invitation | Direction juste, mais doit devenir le coeur viral. | 6.5/10 |
| Participants | Rassurant pour un organisateur, mais trop email/contact-first. | 5/10 |
| Vote | Focalise et comprehensible. Peut devenir repetitif si beaucoup de creneaux. | 7/10 |
| Resultats | Clair, mais pas assez celebratoire ni orienté annonce. | 6/10 |
| Detail evenement | Riche, quelques bons signaux premium. Peut devenir bruyant. | 7/10 |
| Scenarios | Potentiellement excellent, mais empty state non actionnable. | 5/10 |
| Transport | Bonne ambition, mais CTA contradictoire en absence de destination. | 5/10 |
| Budget | Utile mais visuellement banal. | 5.5/10 |
| Paiement/Tricount | Placeholder percu. | 3.5/10 |
| Messages | In-box plus que chat. Ne remplace pas WhatsApp. | 5/10 |
| Profil | Trop de gamification non essentielle. | 5/10 |

Emotions demandees:

- Excitation: 4/10. Les templates et couvertures peuvent aider, mais l'invitation n'a pas encore un effet "j'ai envie d'y aller".
- Anticipation: 5/10. L'app montre les etapes, mais manque de moments futurs concrets: album, playlist, countdown, meteo, checklist partagee visible.
- Appartenance: 4/10. Les participants sont surtout des rows de statut, pas une presence sociale.
- Reduction du stress: 6/10 pour sondage simple, 4/10 pour voyage.
- Confiance: 5/10. Offline et architecture donnent confiance, mais paiement, deep links et modules vides l'abiment.
- Controle organisateur: 6/10. Beaucoup d'actions, mais trop de portes fermees et de modules dont la logique depend d'autres modules.

## Apple Quality

Score: 64/100.

Points forts:

- SwiftUI natif, composants design system, fonds et cards soignes sur les ecrans principaux.
- Plusieurs labels d'accessibilite et tailles de boutons correctes.
- KMP solide en ambition, services communs riches, offline pris en compte.
- Le build arm64 simulator passe.

Points faibles:

- Pas de verification runtime possible sans simulateur.
- Build generic simulator casse sur `x86_64`.
- Haptics presents seulement dans `LocationSelectionSheet` et `InvitationShareSheet`, pas systematiques dans les moments cles de vote, confirmation, selection finale.
- Incoherences visuelles entre ecrans premium et listes natives.
- Textes hardcodes et melange localisation/French strings.
- Manque de microinteractions de resolution: vote envoye, date gagnee, scenario choisi, transport pret.
- Perceived performance non auditee runtime, mais plusieurs chargements et refresh async risquent de flicker.

## Wow effect

Premieres 30 secondes: l'app semble serieuse et jolie, mais pas encore indispensable. On comprend "planification d'evenements", pas "je viens de gagner 3 heures et d'eviter 80 messages".

Moments wow existants:

- Wakeve AI dans la creation, meme s'il est sous-exploite.
- Scenario cards avec budget, duree, logement et votes.
- Transport readiness et generation de plans.
- Design event detail plus riche qu'une simple liste.

Moments wow manquants:

- Invitation/poster magnifique partageable en un tap.
- Resultat de sondage qui annonce automatiquement la date gagnante avec message partageable.
- Generation automatique de 3 options de voyage realistes.
- Budget estime avant decision.
- "Tout le monde est pret" avec transport, roles, paiements et checklist verrouilles.
- Album/photos/playlist/countdown pour donner envie de revenir.

## Audit organisateur

L'organisateur peut creer un evenement, ajouter des participants, lancer un sondage, voir des resultats et explorer des modules. Mais le mental model est fragile: il doit comprendre les statuts DRAFT/POLLING/CONFIRMED/ORGANIZING, savoir ou sont les scenarios, savoir pourquoi le transport est bloque, savoir que le budget arrive plus tard, et savoir comment partager l'invitation.

Les meilleurs produits d'organisation guident l'organisateur par "la prochaine action evidente". Wakeve a cette intention, mais pas encore assez de resolution. Apres chaque action majeure, il faut un etat qui dit: qui est bloque, qui doit faire quoi, quel message envoyer, et ce qui se passe ensuite.

## Comparaison concurrentielle

Sources officielles consultees:

- Apple Invites: `https://apps.apple.com/us/app/apple-invites/id6472498645` et `https://www.icloud.com/invites`
- Partiful: `https://partiful.com/` et `https://partiful.com/free-online-party-invitations`
- Splitwise: `https://www.splitwise.com/`
- TripIt: `https://www.tripit.com/web`
- Eventbrite: `https://www.eventbrite.com/organizer/features/event-management-software/`

Partiful gagne sur les evenements sociaux simples: invite belle, lien partageable, RSVP, messages, photos, personnalisation, vitesse. Wakeve ne doit pas essayer de battre Partiful sur "faire une jolie invite" uniquement, mais il doit au moins atteindre ce niveau de plaisir pour les premiers evenements.

Apple Invites gagne sur l'integration ecosysteme: RSVP, shared album, playlist collaborative, messages d'evenement, iCloud, Photos, Music, Maps/Weather selon les surfaces. Wakeve doit justifier son existence par la coordination multi-participants et multi-decisions, pas par une invite basique.

Splitwise gagne sur une promesse simple: qui doit quoi, sans gene. Wakeve ne peut pas afficher une cagnotte a 0 et un Tricount placeholder si le budget est cense etre un pilier.

TripIt gagne sur la magie d'importer des confirmations et de construire un itineraire. Pour les voyages, Wakeve devrait apprendre de cette logique: absorber les infos existantes et produire une timeline partagee.

Eventbrite n'est pas le concurrent direct des petits groupes, mais il est fort sur la confiance organisateur: inscription, paiement, communication, reporting. Wakeve doit apprendre la clarte de statut et de suivi.

## Reponses franches

Est-ce que Wakeve est utile aujourd'hui ? Oui, pour des sondages de dates et pour montrer une architecture produit ambitieuse.

Est-ce que Wakeve donne envie de quitter WhatsApp ? Non, pas encore. Le premier geste social et le canal invite ne sont pas assez forts.

Est-ce que Wakeve donne envie de revenir ? Pas de maniere fiable. Il manque le moment recurrent: suivi vivant, album, countdown, todo, paiement, decision.

Est-ce que les scenarios complexes sont vraiment couverts ? Non. Ils sont representes dans le domaine et les specs, mais les parcours iOS bloquent trop tot ou trop souvent.

Est-ce que l'app est trop ambitieuse ? Non. Elle est trop modulaire avant d'etre fluide.

## Priorite produit recommandee

1. Faire du flux create -> invite -> vote -> result -> announce une experience parfaite pour watch party, restaurant, anniversaire et barbecue.
2. Rendre l'invitation virale: preview poster, lien unique, deep link fonctionnel, partage WhatsApp/iMessage, RSVP invite sans friction.
3. Transformer Wakeve AI en createur de plan complet, pas seulement en pre-remplissage de texte.
4. Relier templates Explore a la creation pre-remplie.
5. Ajouter une vraie creation/generation de scenarios depuis l'empty state scenario.
6. Corriger le transport: si la destination manque, le CTA doit ouvrir le choix ou renvoyer vers les scenarios.
7. Faire apparaitre le budget estime avant la selection finale de scenario.
8. Refaire cagnotte/Tricount en produit de confiance ou les cacher de la demo.
9. Uniformiser les ecrans basiques avec le design system iOS.
10. Ajouter haptics et microinteractions aux moments de decision.

## Positionnement conseille

Ne pas positionner Wakeve comme "un autre outil d'invitation". Positionner Wakeve comme:

"Le copilote qui transforme un chat de groupe chaotique en evenement decide, partage et pret."

Mais cette promesse n'est credible que si l'app fait en un tap ce que le chat ne sait pas faire: synthetiser les options, obtenir une decision, annoncer la suite, repartir les responsabilites et garder tout le monde aligne.
