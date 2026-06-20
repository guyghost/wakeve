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
        title = "Invitations et retours",
        headline = "Aucun événement à partager",
        inviteReasonLabel = "Invitation : il manque un événement concret à proposer.",
        installReasonLabel = "Pourquoi ouvrir Wakeve : Wakeve doit d'abord montrer un groupe actif.",
        returnReasonLabel = "À suivre : créez un premier événement réutilisable.",
        actionLabel = "Créer",
        action = null
    )

    val voteCount = pollVotes[event.id]?.size ?: 0
    val missingVotes = (event.participants.size - voteCount).coerceAtLeast(0)

    return when (event.status) {
        EventStatus.DRAFT -> EventViralLoopSummary(
            eventId = event.id,
            title = "Invitations et retours",
            headline = "Invitation pas encore prête",
            inviteReasonLabel = "Invitation : le groupe ne doit recevoir le lien qu'une fois le sondage clair.",
            installReasonLabel = "Pourquoi ouvrir Wakeve : voir les créneaux et répondre sans fouiller WhatsApp.",
            returnReasonLabel = "À suivre : reprendre le brouillon pour lancer le vote.",
            actionLabel = if (event.organizerId == currentUserId) "Finaliser" else "Voir",
            action = EventWorkspaceSummaryAction.OpenEvent
        )
        EventStatus.POLLING -> EventViralLoopSummary(
            eventId = event.id,
            title = "Invitations et retours",
            headline = if (missingVotes > 0) {
                "$missingVotes vote${if (missingVotes == 1) "" else "s"} à obtenir"
            } else {
                "Votes prêts à convertir"
            },
            inviteReasonLabel = "Invitation : chaque invité débloque la décision collective.",
            installReasonLabel = "Pourquoi ouvrir Wakeve : voter, suivre la date limite et éviter les relances privées.",
            returnReasonLabel = "À suivre : voir la date retenue et la suite du plan.",
            actionLabel = "Partager le vote",
            action = EventWorkspaceSummaryAction.OpenPoll
        )
        EventStatus.COMPARING -> EventViralLoopSummary(
            eventId = event.id,
            title = "Invitations et retours",
            headline = "Décision à transformer en plan",
            inviteReasonLabel = "Invitation : les retardataires voient les options avant la décision finale.",
            installReasonLabel = "Pourquoi ouvrir Wakeve : comparer destination, budget et contraintes au même endroit.",
            returnReasonLabel = "À suivre : suivre le scénario choisi après la comparaison.",
            actionLabel = "Comparer",
            action = EventWorkspaceSummaryAction.OpenEvent
        )
        EventStatus.CONFIRMED -> EventViralLoopSummary(
            eventId = event.id,
            title = "Invitations et retours",
            headline = "Date confirmée à diffuser",
            inviteReasonLabel = "Invitation : l'événement a maintenant une date crédible à partager.",
            installReasonLabel = "Pourquoi ouvrir Wakeve : calendrier, budget, transport et programme restent centralisés.",
            returnReasonLabel = "À suivre : préparer le départ et suivre les changements utiles.",
            actionLabel = "Préparer",
            action = EventWorkspaceSummaryAction.OpenEvent
        )
        EventStatus.ORGANIZING -> EventViralLoopSummary(
            eventId = event.id,
            title = "Invitations et retours",
            headline = "Centre de contrôle actif",
            inviteReasonLabel = "Invitation : les participants manquants ont besoin du plan à jour.",
            installReasonLabel = "Pourquoi ouvrir Wakeve : savoir où aller, qui vient, quoi payer et quoi faire ensuite.",
            returnReasonLabel = "À suivre : suivre le jour J et les prochaines étapes.",
            actionLabel = "Piloter",
            action = EventWorkspaceSummaryAction.OpenEvent
        )
        EventStatus.FINALIZED -> EventViralLoopSummary(
            eventId = event.id,
            title = "Invitations et retours",
            headline = "Réutilisation après événement",
            inviteReasonLabel = "Invitation : partager le récap, les photos et les remboursements.",
            installReasonLabel = "Pourquoi ouvrir Wakeve : récupérer ce qui reste à solder sans refaire un groupe.",
            returnReasonLabel = "À suivre : recréer une nouvelle édition en un geste.",
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
        title = "Ambiance du groupe",
        headline = "Valeur encore abstraite",
        scoreLabel = "Confiance du groupe : 20/100",
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
            title = "Ambiance du groupe",
            headline = "Promesse encore fragile",
            scoreLabel = "Confiance du groupe : 35/100",
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
            title = "Ambiance du groupe",
            headline = if (missingVotes > 0) "Engagement à débloquer" else "Décision proche",
            scoreLabel = if (missingVotes > 0) "Confiance du groupe : 58/100" else "Confiance du groupe : 68/100",
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
            title = "Ambiance du groupe",
            headline = "Choix collectif en cours",
            scoreLabel = "Confiance du groupe : 64/100",
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
            title = "Ambiance du groupe",
            headline = "Moment wow à consolider",
            scoreLabel = "Confiance du groupe : 76/100",
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
            title = "Ambiance du groupe",
            headline = "Centre de contrôle crédible",
            scoreLabel = "Confiance du groupe : 86/100",
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
            title = "Ambiance du groupe",
            headline = "Mémoire et rétention",
            scoreLabel = "Confiance du groupe : 72/100",
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
        title = "Plan d'action",
        headline = "Créer un premier groupe actif",
        firstMonthLabel = "Maintenant : rendre la création et l'invitation évidentes.",
        secondQuarterLabel = "Ensuite : mesurer activation, abandon et première réutilisation.",
        sixthMonthLabel = "Plus tard : connecter budget, transport, photos et recap.",
        teamFocusLabel = "Focus : créez un premier événement, publiez-le, puis partagez une invitation claire.",
        actionLabel = "Créer",
        action = null
    )

    val voteCount = pollVotes[event.id]?.size ?: 0
    val missingVotes = (event.participants.size - voteCount).coerceAtLeast(0)

    return when (event.status) {
        EventStatus.DRAFT -> EventRoadmapSummary(
            eventId = event.id,
            title = "Plan d'action",
            headline = "Réduire la friction de départ",
            firstMonthLabel = "Maintenant : transformer le brouillon en invitation partageable.",
            secondQuarterLabel = "Ensuite : guider les formats simples, intermédiaires et voyages.",
            sixthMonthLabel = "Plus tard : templates intelligents par type d'événement.",
            teamFocusLabel = "Focus : finalisez les champs utiles puis envoyez une invitation prête à voter.",
            actionLabel = if (event.organizerId == currentUserId) "Finaliser" else "Voir",
            action = EventWorkspaceSummaryAction.OpenEvent
        )
        EventStatus.POLLING -> EventRoadmapSummary(
            eventId = event.id,
            title = "Plan d'action",
            headline = if (missingVotes > 0) "Sortir du débat de date" else "Convertir le vote en plan",
            firstMonthLabel = "Maintenant : relances utiles, vote lisible et décision sans ambiguïté.",
            secondQuarterLabel = "Ensuite : convertir la date retenue en budget, transport et programme.",
            sixthMonthLabel = "Plus tard : recommandations qui anticipent les blocages du groupe.",
            teamFocusLabel = "Focus : obtenez les votes manquants, confirmez la date, puis ouvrez la préparation.",
            actionLabel = "Ouvrir le vote",
            action = EventWorkspaceSummaryAction.OpenPoll
        )
        EventStatus.COMPARING -> EventRoadmapSummary(
            eventId = event.id,
            title = "Plan d'action",
            headline = "Rendre les choix comparables",
            firstMonthLabel = "Maintenant : score clair pour destination, coût et contraintes.",
            secondQuarterLabel = "Ensuite : votes de scénario, arbitrage et justification partagée.",
            sixthMonthLabel = "Plus tard : moteur de recommandations multi-destinations.",
            teamFocusLabel = "Focus : gardez peu de critères et choisissez le scénario le plus simple à expliquer.",
            actionLabel = "Comparer",
            action = EventWorkspaceSummaryAction.OpenEvent
        )
        EventStatus.CONFIRMED -> EventRoadmapSummary(
            eventId = event.id,
            title = "Plan d'action",
            headline = "Éviter la rechute dans WhatsApp",
            firstMonthLabel = "Maintenant : checklist budget, transport, programme et rôles.",
            secondQuarterLabel = "Ensuite : assignations, rappels et changements de programme.",
            sixthMonthLabel = "Plus tard : coordination complète pour voyages et groupes nombreux.",
            teamFocusLabel = "Focus : regroupez budget, transport, programme et rôles dans un seul plan.",
            actionLabel = "Préparer",
            action = EventWorkspaceSummaryAction.OpenEvent
        )
        EventStatus.ORGANIZING -> EventRoadmapSummary(
            eventId = event.id,
            title = "Plan d'action",
            headline = "Jour J à piloter",
            firstMonthLabel = "Maintenant : fiabiliser jour J, présence, étapes et points de rendez-vous.",
            secondQuarterLabel = "Ensuite : rôles, alertes, offline et budget partagé.",
            sixthMonthLabel = "Plus tard : coordination pour 4 à 50 personnes avec transport et multi-destinations.",
            teamFocusLabel = "Focus : gardez le jour J lisible avec point de rendez-vous, rôles et alertes utiles.",
            actionLabel = "Piloter",
            action = EventWorkspaceSummaryAction.OpenEvent
        )
        EventStatus.FINALIZED -> EventRoadmapSummary(
            eventId = event.id,
            title = "Plan d'action",
            headline = "Recap et prochaine édition",
            firstMonthLabel = "Maintenant : recap, photos et remboursements visibles.",
            secondQuarterLabel = "Ensuite : partage post-event et recréation en un geste.",
            sixthMonthLabel = "Plus tard : mémoire de groupe et recommandations pour la prochaine édition.",
            teamFocusLabel = "Focus : partagez le recap, soldez les dépenses et proposez une nouvelle édition.",
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
        title = "Prochaine décision",
        headline = "Aucune action claire",
        verdictLabel = "État : agenda vide, aucune action de groupe visible.",
        scorecardLabel = "Priorité : concentrez le groupe sur la prochaine action utile.",
        honestAnswerLabel = "Confiance : trop faible tant qu'aucun événement n'est partagé.",
        competitorLabel = "À éviter : garder la décision dans un chat séparé.",
        operatingSystemLabel = "Coordination : aucun espace collectif actif.",
        criticalProblemLabel = "Blocage : aucune valeur démontrée en 30 secondes.",
        valueFeatureLabel = "Action utile : premier événement guidé avec invitation partageable.",
        missingCapabilityLabel = "Manque : créer un premier groupe coordonné.",
        nextActionLabel = "Créez un événement pour prouver la valeur de coordination.",
        actionLabel = "Créer",
        action = null
    )

    val voteCount = pollVotes[event.id]?.size ?: 0
    val missingVotes = (event.participants.size - voteCount).coerceAtLeast(0)

    return when (event.status) {
        EventStatus.DRAFT -> EventStrategicSummary(
            eventId = event.id,
            title = "Prochaine décision",
            headline = "Invitation à finaliser",
            verdictLabel = "État : l'invitation n'est pas encore prête pour le groupe.",
            scorecardLabel = "Priorité : concentrez le groupe sur la prochaine action utile.",
            honestAnswerLabel = "Confiance : attendez que l'invitation soit claire avant de partager.",
            competitorLabel = "À éviter : envoyer une invitation moins claire qu'un simple message.",
            operatingSystemLabel = "Coordination : embryon de décision, pas encore un espace collectif.",
            criticalProblemLabel = "Blocage : la promesse reste invisible avant partage.",
            valueFeatureLabel = "Action utile : checklist de publication avant invitation.",
            missingCapabilityLabel = "Manque : invitation claire et vote prêt.",
            nextActionLabel = "Finalisez le brouillon avant de partager.",
            actionLabel = if (event.organizerId == currentUserId) "Finaliser" else "Voir",
            action = EventWorkspaceSummaryAction.OpenEvent
        )
        EventStatus.POLLING -> EventStrategicSummary(
            eventId = event.id,
            title = "Prochaine décision",
            headline = if (missingVotes > 0) "Date à décider" else "Décision collective défendable",
            verdictLabel = "État : le vote avance, mais la préparation n'est pas encore ouverte.",
            scorecardLabel = "Priorité : concentrez le groupe sur la prochaine action utile.",
            honestAnswerLabel = "Confiance : adapté à un événement simple, encore fragile pour un grand groupe.",
            competitorLabel = "À éviter : laisser les votes et les relances se perdre dans le chat.",
            operatingSystemLabel = "Coordination : la première décision commune existe.",
            criticalProblemLabel = "Blocage : la valeur s'arrête si le vote ne débouche pas sur un plan.",
            valueFeatureLabel = "Action utile : conversion automatique du vote en plan de préparation.",
            missingCapabilityLabel = "Manque : transformer le vote en plan budget, transport et programme.",
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
            title = "Prochaine décision",
            headline = "Scénario à choisir",
            verdictLabel = "État : le groupe peut choisir si les options restent comparables.",
            scorecardLabel = "Priorité : concentrez le groupe sur la prochaine action utile.",
            honestAnswerLabel = "Confiance : bonne si les critères restent lisibles par tout le groupe.",
            competitorLabel = "À éviter : empiler des options sans aider le groupe à choisir.",
            operatingSystemLabel = "Coordination : le groupe arbitre destination, contraintes et scénario.",
            criticalProblemLabel = "Blocage : trop d'options recréent le chaos du chat.",
            valueFeatureLabel = "Action utile : scoring multicritère lisible par tout le groupe.",
            missingCapabilityLabel = "Manque : score lisible sur budget, logement et transport.",
            nextActionLabel = "Choisissez le scénario qui réduit le plus le chaos collectif.",
            actionLabel = "Comparer",
            action = EventWorkspaceSummaryAction.OpenEvent
        )
        EventStatus.CONFIRMED -> EventStrategicSummary(
            eventId = event.id,
            title = "Prochaine décision",
            headline = "Préparation à centraliser",
            verdictLabel = "État : la date est crédible, la préparation doit maintenant être centralisée.",
            scorecardLabel = "Priorité : concentrez le groupe sur la prochaine action utile.",
            honestAnswerLabel = "Confiance : bonne pour annoncer, à renforcer avec budget et transport.",
            competitorLabel = "À éviter : se limiter à annoncer la date sans préparer la suite.",
            operatingSystemLabel = "Coordination : date commune, préparation encore incomplète.",
            criticalProblemLabel = "Blocage : l'utilisateur peut repartir dans WhatsApp après la date.",
            valueFeatureLabel = "Action utile : tâches de préparation assignables.",
            missingCapabilityLabel = "Manque : budget, transport, rôles et programme actionnables.",
            nextActionLabel = "Centralisez les décisions qui retomberaient dans le chat.",
            actionLabel = "Préparer",
            action = EventWorkspaceSummaryAction.OpenEvent
        )
        EventStatus.ORGANIZING -> EventStrategicSummary(
            eventId = event.id,
            title = "Prochaine décision",
            headline = "Centre de contrôle actif",
            verdictLabel = "État : le groupe a besoin d'un seul endroit fiable pour le plan, le budget et le jour J.",
            scorecardLabel = "Priorité : concentrez le groupe sur la prochaine action utile.",
            honestAnswerLabel = "Confiance : forte si le cockpit reste fiable jusqu'au jour J.",
            competitorLabel = "À éviter : disperser plan, présences, budget et infos du jour J.",
            operatingSystemLabel = "Coordination : décisions, participants et prochaines actions convergent.",
            criticalProblemLabel = "Blocage : trop d'alertes ou de sections casserait la confiance.",
            valueFeatureLabel = "Action utile : cockpit jour J avec rôles, alertes et responsabilités.",
            missingCapabilityLabel = "Manque : automatiser rôles et alertes sans créer de bruit.",
            nextActionLabel = "Gardez le centre de contrôle comme écran principal jusqu'au jour J.",
            actionLabel = "Piloter",
            action = EventWorkspaceSummaryAction.OpenEvent
        )
        EventStatus.FINALIZED -> EventStrategicSummary(
            eventId = event.id,
            title = "Prochaine décision",
            headline = "Recap à partager",
            verdictLabel = "État : le recap, les photos et les remboursements doivent rester faciles à retrouver.",
            scorecardLabel = "Priorité : concentrez le groupe sur la prochaine action utile.",
            honestAnswerLabel = "Confiance : le retour dépend du recap, des photos et des soldes.",
            competitorLabel = "À éviter : laisser les remboursements hors du recap.",
            operatingSystemLabel = "Coordination : mémoire du groupe réutilisable.",
            criticalProblemLabel = "Blocage : sans boucle post-event, Wakeve redevient jetable.",
            valueFeatureLabel = "Action utile : recap partageable avec photos, soldes et recréation.",
            missingCapabilityLabel = "Manque : boucle photos, remboursements et nouvelle édition.",
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
            userInterestLabel = "Utilité : 10/10",
            rationaleLabel = "Priorité : il evite d'ouvrir l'app pour retrouver le rendez-vous du jour.",
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
                userInterestLabel = "Utilité : 9/10",
                rationaleLabel = "Voyage : il regroupe les points qui cassent les groupes longs.",
                actionLabel = if (event.organizerId == currentUserId) "Piloter" else "Préparer"
            )
        }
        return EventWidgetSummary(
            kind = EventWidgetKind.Countdown,
            eventId = event.id,
            title = event.title,
            headline = if (daysUntil == 1) "Demain" else "J-$daysUntil",
            body = event.workspaceNextActionLabel(isOrganizer = event.organizerId == currentUserId),
            userInterestLabel = "Utilité : 8/10",
            rationaleLabel = "Compte à rebours : il cree de l'anticipation sans spammer le groupe.",
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
            userInterestLabel = "Utilité : 7/10",
            rationaleLabel = "Prochaine tâche : il ramene l'organisateur vers le prochain blocage.",
            actionLabel = "Continuer"
        )
    }

    return EventWidgetSummary(
        kind = EventWidgetKind.Empty,
        eventId = null,
        title = "Wakeve",
        headline = "Aucun événement actif",
        body = "Créez un événement pour afficher le prochain rendez-vous ici.",
        userInterestLabel = "Utilité : a activer",
        rationaleLabel = "Aucune action rapide sans événement actif.",
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
