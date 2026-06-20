package com.guyghost.wakeve.ui.event

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Star
import androidx.compose.ui.graphics.vector.ImageVector
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.EventType
import com.guyghost.wakeve.presentation.state.EventManagementContract
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime

enum class EventListFilter(
    val label: String,
    val icon: ImageVector
) {
    Upcoming("À venir", Icons.Outlined.CalendarToday),
    Past("Passés", Icons.Outlined.History),
    Drafts("Brouillons", Icons.Outlined.Edit),
    OrganizedByMe("Organisés", Icons.Outlined.Star),
    Confirmed("Confirmés", Icons.Outlined.CheckCircle)
}

data class EventListItemUiState(
    val id: String,
    val title: String,
    val description: String,
    val statusLabel: String,
    val nextActionLabel: String,
    val deadlineLabel: String,
    val participantsLabel: String,
    val isOrganizer: Boolean
)

data class EventWorkspaceActionSummary(
    val eventId: String,
    val title: String,
    val body: String,
    val actionLabel: String,
    val action: EventWorkspaceSummaryAction,
    val template: EventWorkspaceCreationTemplate? = null
)

data class EventViralLoopSummary(
    val eventId: String?,
    val title: String,
    val headline: String,
    val inviteReasonLabel: String,
    val installReasonLabel: String,
    val returnReasonLabel: String,
    val actionLabel: String,
    val action: EventWorkspaceSummaryAction?,
    val template: EventWorkspaceCreationTemplate? = null
)

data class EventEmotionalSummary(
    val eventId: String?,
    val title: String,
    val headline: String,
    val scoreLabel: String,
    val excitementLabel: String,
    val anticipationLabel: String,
    val engagementLabel: String,
    val groupFeelingLabel: String,
    val serenityLabel: String,
    val controlLabel: String,
    val nextActionLabel: String,
    val actionLabel: String,
    val action: EventWorkspaceSummaryAction?,
    val template: EventWorkspaceCreationTemplate? = null
)

data class EventStrategicSummary(
    val eventId: String?,
    val title: String,
    val headline: String,
    val verdictLabel: String,
    val scorecardLabel: String,
    val honestAnswerLabel: String,
    val competitorLabel: String,
    val operatingSystemLabel: String,
    val criticalProblemLabel: String,
    val valueFeatureLabel: String,
    val missingCapabilityLabel: String,
    val nextActionLabel: String,
    val actionLabel: String,
    val action: EventWorkspaceSummaryAction?,
    val template: EventWorkspaceCreationTemplate? = null
)

data class EventRoadmapSummary(
    val eventId: String?,
    val title: String,
    val headline: String,
    val firstMonthLabel: String,
    val secondQuarterLabel: String,
    val sixthMonthLabel: String,
    val teamFocusLabel: String,
    val actionLabel: String,
    val action: EventWorkspaceSummaryAction?,
    val template: EventWorkspaceCreationTemplate? = null
)

enum class EventWorkspaceSummaryAction {
    OpenEvent,
    OpenPoll,
    RecreateFromTemplate
}

data class EventWorkspaceCreationTemplate(
    val title: String,
    val description: String,
    val eventType: EventType
)

data class EventReorganizationSummary(
    val title: String,
    val body: String,
    val actionLabel: String,
    val template: EventWorkspaceCreationTemplate
)

enum class EventWidgetKind {
    EventToday,
    Travel,
    Countdown,
    NextTask,
    Empty
}

data class EventWidgetSummary(
    val kind: EventWidgetKind,
    val eventId: String?,
    val title: String,
    val headline: String,
    val body: String,
    val userInterestLabel: String = "",
    val rationaleLabel: String = "",
    val actionLabel: String
)

data class EventWorkspaceUiState(
    val isLoading: Boolean,
    val error: String?,
    val selectedFilter: EventListFilter,
    val searchQuery: String,
    val actionSummary: EventWorkspaceActionSummary?,
    val viralLoopSummary: EventViralLoopSummary,
    val emotionalSummary: EventEmotionalSummary,
    val strategicSummary: EventStrategicSummary,
    val roadmapSummary: EventRoadmapSummary,
    val widgetSummary: EventWidgetSummary,
    val events: List<EventListItemUiState>,
    val selectedEvent: Event?,
    val participantCount: Int,
    val pollVoteCount: Int
)

fun EventManagementContract.State.toEventWorkspaceUiState(
    currentUserId: String,
    selectedFilter: EventListFilter,
    searchQuery: String,
    selectedEventId: String?,
    now: Instant = Clock.System.now(),
    timeZone: TimeZone = TimeZone.currentSystemDefault()
): EventWorkspaceUiState {
    val filtered = events
        .filter { event -> event.matchesFilter(selectedFilter, currentUserId) }
        .filter { event ->
            searchQuery.isBlank() ||
                event.title.contains(searchQuery, ignoreCase = true) ||
                event.description.contains(searchQuery, ignoreCase = true)
        }

    val selected = selectedEvent
        ?: selectedEventId?.let { id -> filtered.firstOrNull { it.id == id } }

    return EventWorkspaceUiState(
        isLoading = isLoading,
        error = error,
        selectedFilter = selectedFilter,
        searchQuery = searchQuery,
        actionSummary = filtered.toWorkspaceActionSummary(currentUserId, pollVotes),
        viralLoopSummary = events.toViralLoopSummary(currentUserId, pollVotes),
        emotionalSummary = events.toEmotionalSummary(currentUserId, pollVotes),
        strategicSummary = events.toStrategicSummary(currentUserId, pollVotes),
        roadmapSummary = events.toRoadmapSummary(currentUserId, pollVotes),
        widgetSummary = events.toEventWidgetSummary(
            now = now,
            timeZone = timeZone,
            currentUserId = currentUserId
        ),
        events = filtered.map { it.toListItem(currentUserId) },
        selectedEvent = selected,
        participantCount = if (selected?.id == selectedEvent?.id) participantIds.size else selected?.participants?.size ?: 0,
        pollVoteCount = selected?.let { pollVotes[it.id]?.size } ?: 0
    )
}

private fun Event.matchesFilter(filter: EventListFilter, currentUserId: String): Boolean =
    when (filter) {
        EventListFilter.Upcoming -> status != EventStatus.FINALIZED
        EventListFilter.Past -> status == EventStatus.FINALIZED
        EventListFilter.Drafts -> status == EventStatus.DRAFT
        EventListFilter.OrganizedByMe -> organizerId == currentUserId
        EventListFilter.Confirmed -> status == EventStatus.CONFIRMED
    }

private fun Event.toListItem(currentUserId: String): EventListItemUiState =
    EventListItemUiState(
        id = id,
        title = title,
        description = description,
        statusLabel = status.workspaceStatusLabel(),
        nextActionLabel = workspaceNextActionLabel(isOrganizer = organizerId == currentUserId),
        deadlineLabel = "Deadline $deadline",
        participantsLabel = "${participants.size} participant${if (participants.size == 1) "" else "s"}",
        isOrganizer = organizerId == currentUserId
    )

internal fun EventStatus.workspaceStatusLabel(): String = when (this) {
    EventStatus.DRAFT -> "Brouillon"
    EventStatus.POLLING -> "Sondage"
    EventStatus.COMPARING -> "Comparaison"
    EventStatus.CONFIRMED -> "Confirmé"
    EventStatus.ORGANIZING -> "Organisation"
    EventStatus.FINALIZED -> "Finalisé"
}

internal fun Event.workspaceNextActionLabel(isOrganizer: Boolean): String = when (status) {
    EventStatus.DRAFT -> if (isOrganizer) "À reprendre" else "En préparation"
    EventStatus.POLLING -> "Vote attendu"
    EventStatus.COMPARING -> if (isOrganizer) "Option à choisir" else "Options à comparer"
    EventStatus.CONFIRMED -> "À préparer"
    EventStatus.ORGANIZING -> "À piloter"
    EventStatus.FINALIZED -> "Récapitulatif"
}

internal fun List<Event>.toWorkspaceActionSummary(
    currentUserId: String,
    pollVotes: Map<String, Map<String, com.guyghost.wakeve.models.Vote>>
): EventWorkspaceActionSummary? {
    val activeEvent = filterNot { it.status == EventStatus.FINALIZED }
        .minWithOrNull(compareBy<Event> { it.workspaceActionPriority(currentUserId) }.thenBy { it.updatedAt })
    val nextEvent = activeEvent ?: filter { it.status == EventStatus.FINALIZED }
        .maxByOrNull { it.updatedAt }
        ?: return null

    return EventWorkspaceActionSummary(
        eventId = nextEvent.id,
        title = nextEvent.workspaceActionTitle(currentUserId),
        body = nextEvent.workspaceActionBody(currentUserId, pollVotes[nextEvent.id]?.size ?: 0),
        actionLabel = nextEvent.workspaceActionButtonLabel(currentUserId),
        action = nextEvent.workspaceSummaryAction(),
        template = nextEvent.workspaceCreationTemplate()
    )
}

internal fun List<Event>.toViralLoopSummary(
    currentUserId: String,
    pollVotes: Map<String, Map<String, com.guyghost.wakeve.models.Vote>>
): EventViralLoopSummary {
    val event = maxWithOrNull(
        compareBy<Event> { it.viralLoopPriority(currentUserId, pollVotes[it.id]?.size ?: 0) }
            .thenBy { it.updatedAt }
    ) ?: return EventViralLoopSummary(
        eventId = null,
        title = "Boucle de croissance",
        headline = "Aucun événement à partager",
        inviteReasonLabel = "Pourquoi inviter : il manque un événement concret à proposer.",
        installReasonLabel = "Pourquoi installer : Wakeve doit d'abord montrer un groupe actif.",
        returnReasonLabel = "Pourquoi revenir : créez un premier événement réutilisable.",
        actionLabel = "Créer",
        action = null
    )

    val voteCount = pollVotes[event.id]?.size ?: 0
    val missingVotes = (event.participants.size - voteCount).coerceAtLeast(0)

    return when (event.status) {
        EventStatus.DRAFT -> EventViralLoopSummary(
            eventId = event.id,
            title = "Boucle de croissance",
            headline = "Invitation pas encore prête",
            inviteReasonLabel = "Pourquoi inviter : le groupe ne doit recevoir le lien qu'une fois le sondage clair.",
            installReasonLabel = "Pourquoi installer : voir les créneaux et répondre sans fouiller WhatsApp.",
            returnReasonLabel = "Pourquoi revenir : reprendre le brouillon pour lancer le vote.",
            actionLabel = if (event.organizerId == currentUserId) "Finaliser" else "Voir",
            action = EventWorkspaceSummaryAction.OpenEvent
        )
        EventStatus.POLLING -> EventViralLoopSummary(
            eventId = event.id,
            title = "Boucle de croissance",
            headline = if (missingVotes > 0) {
                "$missingVotes vote${if (missingVotes == 1) "" else "s"} à obtenir"
            } else {
                "Votes prêts à convertir"
            },
            inviteReasonLabel = "Pourquoi inviter : chaque invité débloque la décision collective.",
            installReasonLabel = "Pourquoi installer : voter, suivre la date limite et éviter les relances privées.",
            returnReasonLabel = "Pourquoi revenir : voir la date retenue et la suite du plan.",
            actionLabel = "Partager le vote",
            action = EventWorkspaceSummaryAction.OpenPoll
        )
        EventStatus.COMPARING -> EventViralLoopSummary(
            eventId = event.id,
            title = "Boucle de croissance",
            headline = "Décision à transformer en plan",
            inviteReasonLabel = "Pourquoi inviter : les retardataires voient les options avant la décision finale.",
            installReasonLabel = "Pourquoi installer : comparer destination, budget et contraintes au même endroit.",
            returnReasonLabel = "Pourquoi revenir : suivre le scénario choisi après la comparaison.",
            actionLabel = "Comparer",
            action = EventWorkspaceSummaryAction.OpenEvent
        )
        EventStatus.CONFIRMED -> EventViralLoopSummary(
            eventId = event.id,
            title = "Boucle de croissance",
            headline = "Date confirmée à diffuser",
            inviteReasonLabel = "Pourquoi inviter : l'événement a maintenant une date crédible à partager.",
            installReasonLabel = "Pourquoi installer : calendrier, budget, transport et programme restent centralisés.",
            returnReasonLabel = "Pourquoi revenir : préparer le départ et suivre les changements utiles.",
            actionLabel = "Préparer",
            action = EventWorkspaceSummaryAction.OpenEvent
        )
        EventStatus.ORGANIZING -> EventViralLoopSummary(
            eventId = event.id,
            title = "Boucle de croissance",
            headline = "Centre de contrôle actif",
            inviteReasonLabel = "Pourquoi inviter : les participants manquants ont besoin du plan à jour.",
            installReasonLabel = "Pourquoi installer : savoir où aller, qui vient, quoi payer et quoi faire ensuite.",
            returnReasonLabel = "Pourquoi revenir : suivre le jour J et les prochaines étapes.",
            actionLabel = "Piloter",
            action = EventWorkspaceSummaryAction.OpenEvent
        )
        EventStatus.FINALIZED -> EventViralLoopSummary(
            eventId = event.id,
            title = "Boucle de croissance",
            headline = "Réutilisation après événement",
            inviteReasonLabel = "Pourquoi inviter : partager le récap, les photos et les remboursements.",
            installReasonLabel = "Pourquoi installer : récupérer ce qui reste à solder sans refaire un groupe.",
            returnReasonLabel = "Pourquoi revenir : recréer une nouvelle édition en un geste.",
            actionLabel = "Réutiliser",
            action = EventWorkspaceSummaryAction.RecreateFromTemplate,
            template = event.workspaceCreationTemplate()
        )
    }
}

internal fun List<Event>.toEmotionalSummary(
    currentUserId: String,
    pollVotes: Map<String, Map<String, com.guyghost.wakeve.models.Vote>>
): EventEmotionalSummary {
    val event = maxWithOrNull(
        compareBy<Event> { it.emotionalPriority(currentUserId, pollVotes[it.id]?.size ?: 0) }
            .thenBy { it.updatedAt }
    ) ?: return EventEmotionalSummary(
        eventId = null,
        title = "Signal émotionnel",
        headline = "Valeur encore abstraite",
        scoreLabel = "Score émotionnel : 20/100",
        excitementLabel = "Excitation : faible tant qu'aucun événement n'existe.",
        anticipationLabel = "Anticipation : aucune date à attendre.",
        engagementLabel = "Engagement : aucun groupe actif.",
        groupFeelingLabel = "Sentiment de groupe : absent.",
        serenityLabel = "Sérénité : l'utilisateur ne voit pas encore ce que Wakeve remplace.",
        controlLabel = "Contrôle : aucune décision centralisée.",
        nextActionLabel = "Créez un premier événement concret pour rendre la valeur visible.",
        actionLabel = "Créer",
        action = null
    )

    val voteCount = pollVotes[event.id]?.size ?: 0
    val missingVotes = (event.participants.size - voteCount).coerceAtLeast(0)

    return when (event.status) {
        EventStatus.DRAFT -> EventEmotionalSummary(
            eventId = event.id,
            title = "Signal émotionnel",
            headline = "Promesse encore fragile",
            scoreLabel = "Score émotionnel : 35/100",
            excitementLabel = "Excitation : basse, le groupe ne voit pas encore l'invitation.",
            anticipationLabel = "Anticipation : faible tant que la date reste brouillon.",
            engagementLabel = "Engagement : dépend de l'organisateur.",
            groupFeelingLabel = "Sentiment de groupe : pas encore déclenché.",
            serenityLabel = "Sérénité : moyenne, les détails restent modifiables.",
            controlLabel = "Contrôle : le brouillon évite déjà de repartir de zéro.",
            nextActionLabel = "Terminez le brouillon et lancez un vote clair.",
            actionLabel = if (event.organizerId == currentUserId) "Finaliser" else "Voir",
            action = EventWorkspaceSummaryAction.OpenEvent
        )
        EventStatus.POLLING -> EventEmotionalSummary(
            eventId = event.id,
            title = "Signal émotionnel",
            headline = if (missingVotes > 0) "Engagement à débloquer" else "Décision proche",
            scoreLabel = if (missingVotes > 0) "Score émotionnel : 58/100" else "Score émotionnel : 68/100",
            excitementLabel = "Excitation : moyenne, le groupe commence à se projeter.",
            anticipationLabel = "Anticipation : liée à la date qui va sortir du vote.",
            engagementLabel = if (missingVotes > 0) {
                "Engagement : $missingVotes participant${if (missingVotes == 1) "" else "s"} à relancer."
            } else {
                "Engagement : votes suffisants pour transformer le sondage en plan."
            },
            groupFeelingLabel = "Sentiment de groupe : visible grâce aux réponses partagées.",
            serenityLabel = "Sérénité : encore fragile tant que la date n'est pas retenue.",
            controlLabel = "Contrôle : meilleur que WhatsApp, mais la décision reste ouverte.",
            nextActionLabel = if (missingVotes > 0) {
                "Relancez les votes manquants avant de promettre une date."
            } else {
                "Confirmez la date pour créer le vrai moment wow."
            },
            actionLabel = "Ouvrir le vote",
            action = EventWorkspaceSummaryAction.OpenPoll
        )
        EventStatus.COMPARING -> EventEmotionalSummary(
            eventId = event.id,
            title = "Signal émotionnel",
            headline = "Choix collectif en cours",
            scoreLabel = "Score émotionnel : 64/100",
            excitementLabel = "Excitation : bonne, les options rendent l'événement tangible.",
            anticipationLabel = "Anticipation : bloquée tant que destination et scénario ne sont pas retenus.",
            engagementLabel = "Engagement : les participants peuvent comparer au lieu de débattre partout.",
            groupFeelingLabel = "Sentiment de groupe : renforcé par une décision commune.",
            serenityLabel = "Sérénité : moyenne, trop d'options peut recréer du bruit.",
            controlLabel = "Contrôle : élevé si l'organisateur tranche vite.",
            nextActionLabel = "Sélectionnez l'option qui réduit le plus les contraintes.",
            actionLabel = "Comparer",
            action = EventWorkspaceSummaryAction.OpenEvent
        )
        EventStatus.CONFIRMED -> EventEmotionalSummary(
            eventId = event.id,
            title = "Signal émotionnel",
            headline = "Moment wow à consolider",
            scoreLabel = "Score émotionnel : 76/100",
            excitementLabel = "Excitation : forte, l'événement a enfin une date crédible.",
            anticipationLabel = "Anticipation : forte grâce au compte à rebours et au calendrier.",
            engagementLabel = "Engagement : à convertir en préparation concrète.",
            groupFeelingLabel = "Sentiment de groupe : solide si l'invitation est partagée.",
            serenityLabel = "Sérénité : moyenne tant que budget, transport et programme restent ouverts.",
            controlLabel = "Contrôle : bon, la suite est centralisée dans Wakeve.",
            nextActionLabel = "Ajoutez budget, transport et programme pour éviter la rechute dans le chat.",
            actionLabel = "Préparer",
            action = EventWorkspaceSummaryAction.OpenEvent
        )
        EventStatus.ORGANIZING -> EventEmotionalSummary(
            eventId = event.id,
            title = "Signal émotionnel",
            headline = "Centre de contrôle crédible",
            scoreLabel = "Score émotionnel : 86/100",
            excitementLabel = "Excitation : utile, portée par un plan concret.",
            anticipationLabel = "Anticipation : forte, chacun sait quoi vérifier avant le départ.",
            engagementLabel = "Engagement : élevé si les tâches critiques restent visibles.",
            groupFeelingLabel = "Sentiment de groupe : fort, les décisions sont partagées.",
            serenityLabel = "Sérénité : haute, Wakeve remplace le chaos opérationnel.",
            controlLabel = "Contrôle : très fort, le jour J devient pilotable.",
            nextActionLabel = "Gardez uniquement les prochaines actions critiques visibles.",
            actionLabel = "Piloter",
            action = EventWorkspaceSummaryAction.OpenEvent
        )
        EventStatus.FINALIZED -> EventEmotionalSummary(
            eventId = event.id,
            title = "Signal émotionnel",
            headline = "Mémoire et rétention",
            scoreLabel = "Score émotionnel : 72/100",
            excitementLabel = "Excitation : transformée en souvenir partageable.",
            anticipationLabel = "Anticipation : à recréer via une nouvelle édition.",
            engagementLabel = "Engagement : dépend des photos, remboursements et recap.",
            groupFeelingLabel = "Sentiment de groupe : bon si le recap est renvoyé.",
            serenityLabel = "Sérénité : haute quand tout est soldé.",
            controlLabel = "Contrôle : utile pour retrouver décisions et relancer vite.",
            nextActionLabel = "Partagez le recap puis recréez l'événement s'il a fonctionné.",
            actionLabel = "Réutiliser",
            action = EventWorkspaceSummaryAction.RecreateFromTemplate,
            template = event.workspaceCreationTemplate()
        )
    }
}

internal fun List<Event>.toRoadmapSummary(
    currentUserId: String,
    pollVotes: Map<String, Map<String, com.guyghost.wakeve.models.Vote>>
): EventRoadmapSummary {
    val event = maxWithOrNull(
        compareBy<Event> { it.roadmapPriority(currentUserId, pollVotes[it.id]?.size ?: 0) }
            .thenBy { it.updatedAt }
    ) ?: return EventRoadmapSummary(
        eventId = null,
        title = "Roadmap 6 mois",
        headline = "D'abord prouver un groupe actif",
        firstMonthLabel = "0-30 jours : rendre la création et l'invitation évidentes.",
        secondQuarterLabel = "31-90 jours : mesurer activation, abandon et première réutilisation.",
        sixthMonthLabel = "3-6 mois : connecter budget, transport, photos et recap.",
        teamFocusLabel = "Équipe : PM sur activation, designer sur premier wow, 3 devs sur création, partage et analytics.",
        actionLabel = "Créer",
        action = null
    )

    val voteCount = pollVotes[event.id]?.size ?: 0
    val missingVotes = (event.participants.size - voteCount).coerceAtLeast(0)

    return when (event.status) {
        EventStatus.DRAFT -> EventRoadmapSummary(
            eventId = event.id,
            title = "Roadmap 6 mois",
            headline = "Réduire la friction de départ",
            firstMonthLabel = "0-30 jours : transformer le brouillon en invitation partageable.",
            secondQuarterLabel = "31-90 jours : guider les formats simples, intermédiaires et voyages.",
            sixthMonthLabel = "3-6 mois : templates intelligents par type d'événement.",
            teamFocusLabel = "Équipe : PM sur taux de publication, designer sur clarté, devs sur templates et validation.",
            actionLabel = if (event.organizerId == currentUserId) "Finaliser" else "Voir",
            action = EventWorkspaceSummaryAction.OpenEvent
        )
        EventStatus.POLLING -> EventRoadmapSummary(
            eventId = event.id,
            title = "Roadmap 6 mois",
            headline = if (missingVotes > 0) "Sortir du débat de date" else "Convertir le vote en plan",
            firstMonthLabel = "0-30 jours : relances utiles, vote lisible et décision sans ambiguïté.",
            secondQuarterLabel = "31-90 jours : convertir la date retenue en budget, transport et programme.",
            sixthMonthLabel = "3-6 mois : recommandations qui anticipent les blocages du groupe.",
            teamFocusLabel = "Équipe : PM sur complétion du vote, designer sur relances, devs sur sondage, partage et suite.",
            actionLabel = "Ouvrir le vote",
            action = EventWorkspaceSummaryAction.OpenPoll
        )
        EventStatus.COMPARING -> EventRoadmapSummary(
            eventId = event.id,
            title = "Roadmap 6 mois",
            headline = "Rendre les choix comparables",
            firstMonthLabel = "0-30 jours : score clair pour destination, coût et contraintes.",
            secondQuarterLabel = "31-90 jours : votes de scénario, arbitrage et justification partagée.",
            sixthMonthLabel = "3-6 mois : moteur de recommandations multi-destinations.",
            teamFocusLabel = "Équipe : PM sur critères, designer sur matrice, devs sur scoring et décision finale.",
            actionLabel = "Comparer",
            action = EventWorkspaceSummaryAction.OpenEvent
        )
        EventStatus.CONFIRMED -> EventRoadmapSummary(
            eventId = event.id,
            title = "Roadmap 6 mois",
            headline = "Éviter la rechute dans WhatsApp",
            firstMonthLabel = "0-30 jours : checklist budget, transport, programme et rôles.",
            secondQuarterLabel = "31-90 jours : assignations, rappels et changements de programme.",
            sixthMonthLabel = "3-6 mois : coordination complète pour voyages et groupes nombreux.",
            teamFocusLabel = "Équipe : PM sur préparation, designer sur cockpit, devs sur tâches, calendrier et notifications.",
            actionLabel = "Préparer",
            action = EventWorkspaceSummaryAction.OpenEvent
        )
        EventStatus.ORGANIZING -> EventRoadmapSummary(
            eventId = event.id,
            title = "Roadmap 6 mois",
            headline = "Industrialiser le centre de contrôle",
            firstMonthLabel = "0-30 jours : fiabiliser jour J, présence, étapes et points de rendez-vous.",
            secondQuarterLabel = "31-90 jours : rôles, alertes, offline et budget partagé.",
            sixthMonthLabel = "3-6 mois : OS social pour 4 à 50 personnes avec transport et multi-destinations.",
            teamFocusLabel = "Équipe : PM sur sérénité, designer sur densité, devs sur offline, alertes et budget.",
            actionLabel = "Piloter",
            action = EventWorkspaceSummaryAction.OpenEvent
        )
        EventStatus.FINALIZED -> EventRoadmapSummary(
            eventId = event.id,
            title = "Roadmap 6 mois",
            headline = "Transformer la fin en rétention",
            firstMonthLabel = "0-30 jours : recap, photos et remboursements visibles.",
            secondQuarterLabel = "31-90 jours : partage post-event et recréation en un geste.",
            sixthMonthLabel = "3-6 mois : mémoire de groupe et recommandations pour la prochaine édition.",
            teamFocusLabel = "Équipe : PM sur retour, designer sur recap, devs sur photos, soldes et templates.",
            actionLabel = "Réutiliser",
            action = EventWorkspaceSummaryAction.RecreateFromTemplate,
            template = event.workspaceCreationTemplate()
        )
    }
}

private fun Event.roadmapPriority(currentUserId: String, voteCount: Int): Int =
    when (status) {
        EventStatus.ORGANIZING -> 80
        EventStatus.CONFIRMED -> 70
        EventStatus.POLLING -> 65 + (participants.size - voteCount).coerceAtLeast(0)
        EventStatus.COMPARING -> 62
        EventStatus.FINALIZED -> 55
        EventStatus.DRAFT -> if (organizerId == currentUserId) 45 else 20
    }

internal fun List<Event>.toStrategicSummary(
    currentUserId: String,
    pollVotes: Map<String, Map<String, com.guyghost.wakeve.models.Vote>>
): EventStrategicSummary {
    val event = maxWithOrNull(
        compareBy<Event> { it.strategicPriority(currentUserId, pollVotes[it.id]?.size ?: 0) }
            .thenBy { it.updatedAt }
    ) ?: return EventStrategicSummary(
        eventId = null,
        title = "Position stratégique",
        headline = "Pas encore défendable",
        verdictLabel = "Verdict : agenda vide, aucun moat visible.",
        scorecardLabel = "Scores : produit 20/100 · UX 35/100 · rétention 10/100.",
        honestAnswerLabel = "Honnête : je ne lancerais pas, ne paierais pas et ne recommanderais pas encore.",
        competitorLabel = "Face aux concurrents : WhatsApp suffit encore.",
        operatingSystemLabel = "OS social : aucun espace collectif actif.",
        criticalProblemLabel = "Problème critique : aucune valeur démontrée en 30 secondes.",
        valueFeatureLabel = "Fonction à créer : premier événement guidé avec invitation partageable.",
        missingCapabilityLabel = "Capacité manquante : créer un premier groupe coordonné.",
        nextActionLabel = "Créez un événement pour prouver la valeur de coordination.",
        actionLabel = "Créer",
        action = null
    )

    val voteCount = pollVotes[event.id]?.size ?: 0
    val missingVotes = (event.participants.size - voteCount).coerceAtLeast(0)

    return when (event.status) {
        EventStatus.DRAFT -> EventStrategicSummary(
            eventId = event.id,
            title = "Position stratégique",
            headline = "Encore plus faible qu'un chat",
            verdictLabel = "Verdict : proposition non lançable tant que le groupe ne voit rien.",
            scorecardLabel = "Scores : produit 35/100 · UX 55/100 · rétention 20/100.",
            honestAnswerLabel = "Honnête : je ne lancerais pas ce brouillon à un groupe.",
            competitorLabel = "Face aux concurrents : Partiful gagne si l'invitation reste plus simple.",
            operatingSystemLabel = "OS social : embryon de décision, pas encore un espace collectif.",
            criticalProblemLabel = "Problème critique : la promesse reste invisible avant partage.",
            valueFeatureLabel = "Fonction à créer : checklist de publication avant invitation.",
            missingCapabilityLabel = "Capacité manquante : invitation claire et vote prêt.",
            nextActionLabel = "Finalisez le brouillon avant de partager.",
            actionLabel = if (event.organizerId == currentUserId) "Finaliser" else "Voir",
            action = EventWorkspaceSummaryAction.OpenEvent
        )
        EventStatus.POLLING -> EventStrategicSummary(
            eventId = event.id,
            title = "Position stratégique",
            headline = if (missingVotes > 0) "Meilleur que WhatsApp, pas encore un OS" else "Décision collective défendable",
            verdictLabel = "Verdict : utile pour choisir une date, pas encore pour organiser tout le séjour.",
            scorecardLabel = "Scores : produit 52/100 · UX 66/100 · rétention 35/100.",
            honestAnswerLabel = "Honnête : je l'utiliserais pour un dîner, pas encore pour 15 voyageurs.",
            competitorLabel = "Face aux concurrents : WhatsApp perd la trace des votes et des relances.",
            operatingSystemLabel = "OS social : la première décision commune existe.",
            criticalProblemLabel = "Problème critique : la valeur s'arrête si le vote ne débouche pas sur un plan.",
            valueFeatureLabel = "Fonction à créer : conversion automatique du vote en plan de préparation.",
            missingCapabilityLabel = "Capacité manquante : transformer le vote en plan budget, transport et programme.",
            nextActionLabel = if (missingVotes > 0) {
                "Obtenez les votes manquants pour sortir du débat."
            } else {
                "Confirmez la date et ouvrez la préparation."
            },
            actionLabel = "Ouvrir le vote",
            action = EventWorkspaceSummaryAction.OpenPoll
        )
        EventStatus.COMPARING -> EventStrategicSummary(
            eventId = event.id,
            title = "Position stratégique",
            headline = "Différenciation à portée",
            verdictLabel = "Verdict : Wakeve peut dépasser le simple agenda si la comparaison tranche vraiment.",
            scorecardLabel = "Scores : produit 60/100 · UX 68/100 · rétention 42/100.",
            honestAnswerLabel = "Honnête : je recommanderais seulement si les choix sont vraiment comparables.",
            competitorLabel = "Face aux concurrents : TripIt suit un plan, Wakeve peut décider le plan.",
            operatingSystemLabel = "OS social : le groupe arbitre destination, contraintes et scénario.",
            criticalProblemLabel = "Problème critique : trop d'options recréent le chaos du chat.",
            valueFeatureLabel = "Fonction à créer : scoring multicritère lisible par tout le groupe.",
            missingCapabilityLabel = "Capacité manquante : score lisible sur budget, logement et transport.",
            nextActionLabel = "Choisissez le scénario qui réduit le plus le chaos collectif.",
            actionLabel = "Comparer",
            action = EventWorkspaceSummaryAction.OpenEvent
        )
        EventStatus.CONFIRMED -> EventStrategicSummary(
            eventId = event.id,
            title = "Position stratégique",
            headline = "Raison d'exister visible",
            verdictLabel = "Verdict : l'événement devient crédible, mais la promesse OS social reste à prouver.",
            scorecardLabel = "Scores : produit 66/100 · UX 72/100 · rétention 50/100.",
            honestAnswerLabel = "Honnête : je lancerais en bêta, mais je ne paierais pas sans budget et transport.",
            competitorLabel = "Face aux concurrents : Apple Invites couvre l'annonce, Wakeve doit couvrir l'organisation.",
            operatingSystemLabel = "OS social : date commune, préparation encore incomplète.",
            criticalProblemLabel = "Problème critique : l'utilisateur peut repartir dans WhatsApp après la date.",
            valueFeatureLabel = "Fonction à créer : tâches de préparation assignables.",
            missingCapabilityLabel = "Capacité manquante : budget, transport, rôles et programme actionnables.",
            nextActionLabel = "Centralisez les décisions qui retomberaient dans le chat.",
            actionLabel = "Préparer",
            action = EventWorkspaceSummaryAction.OpenEvent
        )
        EventStatus.ORGANIZING -> EventStrategicSummary(
            eventId = event.id,
            title = "Position stratégique",
            headline = "OS social crédible",
            verdictLabel = "Verdict : Wakeve garde une raison d'exister même si WhatsApp, Splitwise et TripIt fusionnent.",
            scorecardLabel = "Scores : produit 78/100 · UX 78/100 · rétention 66/100.",
            honestAnswerLabel = "Honnête : je l'utiliserais pour un voyage à 15 si ce cockpit reste fiable.",
            competitorLabel = "Face aux concurrents : plan, présences, budget et jour J vivent dans un seul espace.",
            operatingSystemLabel = "OS social : décisions, participants et prochaines actions convergent.",
            criticalProblemLabel = "Problème critique : trop d'alertes ou de sections casserait la confiance.",
            valueFeatureLabel = "Fonction à créer : cockpit jour J avec rôles, alertes et responsabilités.",
            missingCapabilityLabel = "Capacité manquante : automatiser rôles et alertes sans créer de bruit.",
            nextActionLabel = "Gardez le centre de contrôle comme écran principal jusqu'au jour J.",
            actionLabel = "Piloter",
            action = EventWorkspaceSummaryAction.OpenEvent
        )
        EventStatus.FINALIZED -> EventStrategicSummary(
            eventId = event.id,
            title = "Position stratégique",
            headline = "Rétention à prouver",
            verdictLabel = "Verdict : utile après l'événement seulement si recap, photos et dettes ressortent vite.",
            scorecardLabel = "Scores : produit 64/100 · UX 70/100 · rétention 72/100.",
            honestAnswerLabel = "Honnête : je reviendrais si le recap relance une nouvelle édition.",
            competitorLabel = "Face aux concurrents : Splitwise gagne si les remboursements restent séparés.",
            operatingSystemLabel = "OS social : mémoire du groupe réutilisable.",
            criticalProblemLabel = "Problème critique : sans boucle post-event, Wakeve redevient jetable.",
            valueFeatureLabel = "Fonction à créer : recap partageable avec photos, soldes et recréation.",
            missingCapabilityLabel = "Capacité manquante : boucle photos, remboursements et nouvelle édition.",
            nextActionLabel = "Transformez le recap en invitation pour la prochaine édition.",
            actionLabel = "Réutiliser",
            action = EventWorkspaceSummaryAction.RecreateFromTemplate,
            template = event.workspaceCreationTemplate()
        )
    }
}

private fun Event.strategicPriority(currentUserId: String, voteCount: Int): Int =
    when (status) {
        EventStatus.ORGANIZING -> 90
        EventStatus.CONFIRMED -> 75
        EventStatus.COMPARING -> 70
        EventStatus.POLLING -> 65 + (participants.size - voteCount).coerceAtLeast(0)
        EventStatus.FINALIZED -> 60
        EventStatus.DRAFT -> if (organizerId == currentUserId) 40 else 20
    }

private fun Event.emotionalPriority(currentUserId: String, voteCount: Int): Int =
    when (status) {
        EventStatus.ORGANIZING -> 70
        EventStatus.CONFIRMED -> 65
        EventStatus.POLLING -> 55 + (participants.size - voteCount).coerceAtLeast(0)
        EventStatus.COMPARING -> 52
        EventStatus.FINALIZED -> 45
        EventStatus.DRAFT -> if (organizerId == currentUserId) 35 else 15
    }

private fun Event.viralLoopPriority(currentUserId: String, voteCount: Int): Int =
    when (status) {
        EventStatus.ORGANIZING -> 60
        EventStatus.CONFIRMED -> 55
        EventStatus.POLLING -> 50 + (participants.size - voteCount).coerceAtLeast(0)
        EventStatus.COMPARING -> 45
        EventStatus.FINALIZED -> 35
        EventStatus.DRAFT -> if (organizerId == currentUserId) 25 else 10
    }

private fun Event.workspaceActionPriority(currentUserId: String): Int =
    when (status) {
        EventStatus.DRAFT -> if (organizerId == currentUserId) 0 else 5
        EventStatus.POLLING -> 1
        EventStatus.COMPARING -> 2
        EventStatus.CONFIRMED -> 3
        EventStatus.ORGANIZING -> 4
        EventStatus.FINALIZED -> 6
    }

private fun Event.workspaceActionTitle(currentUserId: String): String =
    when (status) {
        EventStatus.DRAFT -> if (organizerId == currentUserId) {
            "Reprenez ${title}"
        } else {
            "${title} est en préparation"
        }
        EventStatus.POLLING -> "Faites avancer le sondage"
        EventStatus.COMPARING -> "Choisissez la meilleure option"
        EventStatus.CONFIRMED -> "Préparez ${title}"
        EventStatus.ORGANIZING -> "Pilotez ${title}"
        EventStatus.FINALIZED -> "Réorganisez ${title}"
    }

private fun Event.workspaceActionBody(currentUserId: String, voteCount: Int): String =
    when (status) {
        EventStatus.DRAFT -> if (organizerId == currentUserId) {
            "Terminez les infos manquantes pour lancer le vote sans repartir de zéro."
        } else {
            "L'organisateur finalise encore les détails avant d'inviter le groupe."
        }
        EventStatus.POLLING -> {
            val missingVotes = (participants.size - voteCount).coerceAtLeast(0)
            if (missingVotes == 0) {
                "Tous les votes connus sont enregistrés; ouvrez l'événement pour confirmer la suite."
            } else {
                "$missingVotes participant${if (missingVotes > 1) "s" else ""} à relancer avant de bloquer une date."
            }
        }
        EventStatus.COMPARING -> "Comparez destination, budget et contraintes avant de sélectionner le scénario final."
        EventStatus.CONFIRMED -> "Partagez l'invitation, ajoutez le calendrier et préparez budget, transport et activités."
        EventStatus.ORGANIZING -> "Suivez les prochaines décisions et les tâches critiques depuis le centre de contrôle."
        EventStatus.FINALIZED -> "Repartez de ce qui a marché: titre, description et type sont préremplis pour créer une nouvelle édition."
    }

private fun Event.workspaceActionButtonLabel(currentUserId: String): String =
    when (status) {
        EventStatus.DRAFT -> if (organizerId == currentUserId) "Continuer" else "Voir"
        EventStatus.POLLING -> "Ouvrir le vote"
        EventStatus.COMPARING -> "Comparer"
        EventStatus.CONFIRMED -> "Préparer"
        EventStatus.ORGANIZING -> "Piloter"
        EventStatus.FINALIZED -> "Réutiliser"
    }

private fun Event.workspaceSummaryAction(): EventWorkspaceSummaryAction =
    when (status) {
        EventStatus.POLLING -> EventWorkspaceSummaryAction.OpenPoll
        EventStatus.FINALIZED -> EventWorkspaceSummaryAction.RecreateFromTemplate
        else -> EventWorkspaceSummaryAction.OpenEvent
    }

internal fun Event.workspaceCreationTemplate(): EventWorkspaceCreationTemplate? =
    if (status == EventStatus.FINALIZED) {
        EventWorkspaceCreationTemplate(
            title = title,
            description = description,
            eventType = eventType
        )
    } else {
        null
    }

internal fun Event.toReorganizationSummary(): EventReorganizationSummary? {
    val template = workspaceCreationTemplate() ?: return null

    return EventReorganizationSummary(
        title = "Réorganiser rapidement",
        body = "Créez une nouvelle édition de $title avec le titre, la description et le type déjà repris.",
        actionLabel = "Créer une nouvelle édition",
        template = template
    )
}

internal fun List<Event>.toEventWidgetSummary(
    now: Instant,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
    currentUserId: String
): EventWidgetSummary {
    val today = now.toLocalDateTime(timeZone).date

    val eventsWithDate = mapNotNull { event ->
        event.widgetStartInstant()?.let { start -> event to start }
    }

    val todayEvent = eventsWithDate
        .filter { (event, start) ->
            event.status in listOf(EventStatus.CONFIRMED, EventStatus.ORGANIZING) &&
                start.toLocalDateTime(timeZone).date == today
        }
        .minByOrNull { (_, start) -> start.toEpochMilliseconds() }
        ?.first

    if (todayEvent != null) {
        return EventWidgetSummary(
            kind = EventWidgetKind.EventToday,
            eventId = todayEvent.id,
            title = "Aujourd'hui",
            headline = todayEvent.title,
            body = "${todayEvent.participants.size} participant${if (todayEvent.participants.size > 1) "s" else ""} attendus",
            userInterestLabel = "Interet utilisateur : 10/10",
            rationaleLabel = "Widget prioritaire : il evite d'ouvrir l'app pour retrouver le rendez-vous du jour.",
            actionLabel = "Ouvrir"
        )
    }

    val nextDatedEvent = eventsWithDate
        .filter { (event, start) ->
            event.status != EventStatus.FINALIZED &&
                start.toLocalDateTime(timeZone).date >= today
        }
        .minByOrNull { (_, start) -> start.toEpochMilliseconds() }

    if (nextDatedEvent != null) {
        val (event, start) = nextDatedEvent
        val eventDate = start.toLocalDateTime(timeZone).date
        val daysUntil = today.daysUntil(eventDate)
        if (event.isTravelWidgetCandidate()) {
            return EventWidgetSummary(
                kind = EventWidgetKind.Travel,
                eventId = event.id,
                title = "Voyage à préparer",
                headline = event.title,
                body = "${event.participants.size} participant${if (event.participants.size > 1) "s" else ""} - transport, budget et programme à vérifier",
                userInterestLabel = "Interet utilisateur : 9/10",
                rationaleLabel = "Widget voyage pertinent : il regroupe les points qui cassent les groupes longs.",
                actionLabel = if (event.organizerId == currentUserId) "Piloter" else "Préparer"
            )
        }
        return EventWidgetSummary(
            kind = EventWidgetKind.Countdown,
            eventId = event.id,
            title = event.title,
            headline = if (daysUntil == 1) "Demain" else "J-$daysUntil",
            body = event.workspaceNextActionLabel(isOrganizer = event.organizerId == currentUserId),
            userInterestLabel = "Interet utilisateur : 8/10",
            rationaleLabel = "Widget compte a rebours utile : il cree de l'anticipation sans spammer le groupe.",
            actionLabel = "Préparer"
        )
    }

    val nextTask = filterNot { it.status == EventStatus.FINALIZED }
        .minWithOrNull(compareBy<Event> { it.workspaceActionPriority(currentUserId) }.thenBy { it.updatedAt })

    if (nextTask != null) {
        return EventWidgetSummary(
            kind = EventWidgetKind.NextTask,
            eventId = nextTask.id,
            title = "Prochaine tâche",
            headline = nextTask.workspaceActionTitle(currentUserId),
            body = nextTask.workspaceActionButtonLabel(currentUserId),
            userInterestLabel = "Interet utilisateur : 7/10",
            rationaleLabel = "Widget tache utile : il ramene l'organisateur vers le prochain blocage.",
            actionLabel = "Continuer"
        )
    }

    return EventWidgetSummary(
        kind = EventWidgetKind.Empty,
        eventId = null,
        title = "Wakeve",
        headline = "Aucun événement actif",
        body = "Créez un événement pour afficher le prochain rendez-vous ici.",
        userInterestLabel = "Interet utilisateur : a activer",
        rationaleLabel = "Aucun widget utile sans evenement actif.",
        actionLabel = "Créer"
    )
}

private fun Event.widgetStartInstant(): Instant? {
    val final = finalDate?.let { raw -> runCatching { Instant.parse(raw) }.getOrNull() }
    if (final != null) return final

    return proposedSlots
        .mapNotNull { slot -> slot.start?.let { raw -> runCatching { Instant.parse(raw) }.getOrNull() } }
        .minByOrNull { it.toEpochMilliseconds() }
}

private fun Event.isTravelWidgetCandidate(): Boolean {
    if (eventType == EventType.OUTDOOR_ACTIVITY) return true

    val searchableText = "$title $description ${eventType.name}".lowercase()
    return listOf(
        "trip",
        "travel",
        "voyage",
        "weekend",
        "week-end",
        "road trip",
        "retreat",
        "séjour",
        "sejour"
    ).any { keyword -> keyword in searchableText }
}
