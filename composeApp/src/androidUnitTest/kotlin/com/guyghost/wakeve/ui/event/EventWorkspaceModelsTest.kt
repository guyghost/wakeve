package com.guyghost.wakeve.ui.event

import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.EventType
import com.guyghost.wakeve.models.TimeSlot
import com.guyghost.wakeve.models.Vote
import com.guyghost.wakeve.presentation.state.EventManagementContract
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class EventWorkspaceModelsTest {
    @Test
    fun `maps event state to filtered immutable workspace state`() {
        val state = EventManagementContract.State(
            events = listOf(
                event(id = "draft", title = "Draft dinner", status = EventStatus.DRAFT, organizerId = "me"),
                event(id = "final", title = "Finished trip", status = EventStatus.FINALIZED, organizerId = "friend")
            )
        )

        val uiState = state.toEventWorkspaceUiState(
            currentUserId = "me",
            selectedFilter = EventListFilter.Upcoming,
            searchQuery = "dinner",
            selectedEventId = null
        )

        assertFalse(uiState.isLoading)
        assertEquals(null, uiState.error)
        assertEquals(listOf("draft"), uiState.events.map { it.id })
        assertNull(uiState.selectedEvent)
        assertEquals(true, uiState.events.single().isOrganizer)
        assertEquals("Brouillon", uiState.events.single().statusLabel)
        assertEquals("À reprendre", uiState.events.single().nextActionLabel)
    }

    @Test
    fun `uses selected event id for expanded list detail state`() {
        val state = EventManagementContract.State(
            events = listOf(
                event(id = "first", title = "First event", status = EventStatus.POLLING),
                event(id = "second", title = "Second event", status = EventStatus.CONFIRMED)
            )
        )

        val uiState = state.toEventWorkspaceUiState(
            currentUserId = "me",
            selectedFilter = EventListFilter.Upcoming,
            searchQuery = "",
            selectedEventId = "second"
        )

        assertEquals("second", uiState.selectedEvent?.id)
    }

    @Test
    fun `workspace state exposes widget summary across filters`() {
        val state = EventManagementContract.State(
            events = listOf(
                event(
                    id = "draft",
                    title = "Draft dinner",
                    status = EventStatus.DRAFT,
                    proposedSlots = emptyList()
                ),
                event(
                    id = "today",
                    title = "Today launch",
                    status = EventStatus.CONFIRMED,
                    finalDate = "2026-06-20T18:00:00Z"
                )
            )
        )

        val uiState = state.toEventWorkspaceUiState(
            currentUserId = "me",
            selectedFilter = EventListFilter.Drafts,
            searchQuery = "dinner",
            selectedEventId = null,
            now = Instant.parse("2026-06-20T08:00:00Z"),
            timeZone = TimeZone.UTC
        )

        assertEquals(listOf("draft"), uiState.events.map { it.id })
        assertEquals(EventWidgetKind.EventToday, uiState.widgetSummary.kind)
        assertEquals("today", uiState.widgetSummary.eventId)
        assertEquals("Today launch", uiState.widgetSummary.headline)
    }

    @Test
    fun `workspace summary prioritizes organizer draft recovery`() {
        val state = EventManagementContract.State(
            events = listOf(
                event(id = "poll", title = "Vote dinner", status = EventStatus.POLLING, participants = listOf("me", "alice")),
                event(id = "draft", title = "Draft trip", status = EventStatus.DRAFT, organizerId = "me")
            ),
            pollVotes = mapOf("poll" to mapOf("me" to Vote.YES))
        )

        val uiState = state.toEventWorkspaceUiState(
            currentUserId = "me",
            selectedFilter = EventListFilter.Upcoming,
            searchQuery = "",
            selectedEventId = null
        )

        assertEquals("draft", uiState.actionSummary?.eventId)
        assertEquals("Reprenez Draft trip", uiState.actionSummary?.title)
        assertEquals("Continuer", uiState.actionSummary?.actionLabel)
        assertEquals(EventWorkspaceSummaryAction.OpenEvent, uiState.actionSummary?.action)
    }

    @Test
    fun `workspace summary opens poll directly when votes are the next blocker`() {
        val state = EventManagementContract.State(
            events = listOf(
                event(
                    id = "poll",
                    title = "Vote dinner",
                    status = EventStatus.POLLING,
                    participants = listOf("me", "alice", "sam")
                )
            ),
            pollVotes = mapOf("poll" to mapOf("me" to Vote.YES))
        )

        val uiState = state.toEventWorkspaceUiState(
            currentUserId = "me",
            selectedFilter = EventListFilter.Upcoming,
            searchQuery = "",
            selectedEventId = null
        )

        assertEquals("poll", uiState.actionSummary?.eventId)
        assertEquals("Faites avancer le sondage", uiState.actionSummary?.title)
        assertEquals("2 participants à relancer avant de bloquer une date.", uiState.actionSummary?.body)
        assertEquals("Ouvrir le vote", uiState.actionSummary?.actionLabel)
        assertEquals(EventWorkspaceSummaryAction.OpenPoll, uiState.actionSummary?.action)
    }

    @Test
    fun `workspace summary recreates from most recent finalized event`() {
        val state = EventManagementContract.State(
            events = listOf(
                event(
                    id = "older",
                    title = "Spring dinner",
                    status = EventStatus.FINALIZED,
                    updatedAt = "2026-05-01T08:00:00Z"
                ),
                event(
                    id = "newer",
                    title = "Summer retreat",
                    description = "A weekend by the sea",
                    status = EventStatus.FINALIZED,
                    eventType = EventType.OUTDOOR_ACTIVITY,
                    updatedAt = "2026-06-15T08:00:00Z"
                )
            )
        )

        val uiState = state.toEventWorkspaceUiState(
            currentUserId = "me",
            selectedFilter = EventListFilter.Past,
            searchQuery = "",
            selectedEventId = null
        )

        assertEquals("newer", uiState.actionSummary?.eventId)
        assertEquals("Réorganisez Summer retreat", uiState.actionSummary?.title)
        assertEquals("Réutiliser", uiState.actionSummary?.actionLabel)
        assertEquals(EventWorkspaceSummaryAction.RecreateFromTemplate, uiState.actionSummary?.action)
        assertEquals(
            EventWorkspaceCreationTemplate(
                title = "Summer retreat",
                description = "A weekend by the sea",
                eventType = EventType.OUTDOOR_ACTIVITY
            ),
            uiState.actionSummary?.template
        )
    }

    @Test
    fun `viral loop summary answers why invite install and return during poll`() {
        val state = EventManagementContract.State(
            events = listOf(
                event(
                    id = "poll",
                    title = "Vote dinner",
                    status = EventStatus.POLLING,
                    participants = listOf("me", "alice", "sam")
                )
            ),
            pollVotes = mapOf("poll" to mapOf("me" to Vote.YES))
        )

        val summary = state.toEventWorkspaceUiState(
            currentUserId = "me",
            selectedFilter = EventListFilter.Upcoming,
            searchQuery = "",
            selectedEventId = null
        ).viralLoopSummary

        assertEquals("poll", summary.eventId)
        assertEquals("Boucle de croissance", summary.title)
        assertEquals("2 votes à obtenir", summary.headline)
        assertEquals("Pourquoi inviter : chaque invité débloque la décision collective.", summary.inviteReasonLabel)
        assertEquals(
            "Pourquoi installer : voter, suivre la date limite et éviter les relances privées.",
            summary.installReasonLabel
        )
        assertEquals("Pourquoi revenir : voir la date retenue et la suite du plan.", summary.returnReasonLabel)
        assertEquals("Partager le vote", summary.actionLabel)
        assertEquals(EventWorkspaceSummaryAction.OpenPoll, summary.action)
    }

    @Test
    fun `viral loop summary prioritizes active control center over draft`() {
        val summary = listOf(
            event(
                id = "draft",
                title = "Draft dinner",
                status = EventStatus.DRAFT,
                organizerId = "me"
            ),
            event(
                id = "organizing",
                title = "Team offsite",
                status = EventStatus.ORGANIZING,
                participants = listOf("me", "alice", "sam")
            )
        ).toViralLoopSummary(currentUserId = "me", pollVotes = emptyMap())

        assertEquals("organizing", summary.eventId)
        assertEquals("Centre de contrôle actif", summary.headline)
        assertEquals(
            "Pourquoi installer : savoir où aller, qui vient, quoi payer et quoi faire ensuite.",
            summary.installReasonLabel
        )
        assertEquals("Pourquoi revenir : suivre le jour J et les prochaines étapes.", summary.returnReasonLabel)
        assertEquals("Piloter", summary.actionLabel)
        assertEquals(EventWorkspaceSummaryAction.OpenEvent, summary.action)
    }

    @Test
    fun `viral loop summary turns finalized event into retention loop`() {
        val summary = listOf(
            event(
                id = "final",
                title = "Summer retreat",
                description = "A weekend by the sea",
                status = EventStatus.FINALIZED,
                eventType = EventType.OUTDOOR_ACTIVITY
            )
        ).toViralLoopSummary(currentUserId = "me", pollVotes = emptyMap())

        assertEquals("final", summary.eventId)
        assertEquals("Réutilisation après événement", summary.headline)
        assertEquals(
            "Pourquoi inviter : partager le récap, les photos et les remboursements.",
            summary.inviteReasonLabel
        )
        assertEquals(
            "Pourquoi revenir : recréer une nouvelle édition en un geste.",
            summary.returnReasonLabel
        )
        assertEquals("Réutiliser", summary.actionLabel)
        assertEquals(EventWorkspaceSummaryAction.RecreateFromTemplate, summary.action)
        assertEquals(
            EventWorkspaceCreationTemplate(
                title = "Summer retreat",
                description = "A weekend by the sea",
                eventType = EventType.OUTDOOR_ACTIVITY
            ),
            summary.template
        )
    }

    @Test
    fun `viral loop summary explains missing growth loop when workspace is empty`() {
        val summary = emptyList<Event>().toViralLoopSummary(currentUserId = "me", pollVotes = emptyMap())

        assertEquals(null, summary.eventId)
        assertEquals("Aucun événement à partager", summary.headline)
        assertEquals("Pourquoi inviter : il manque un événement concret à proposer.", summary.inviteReasonLabel)
        assertEquals("Pourquoi installer : Wakeve doit d'abord montrer un groupe actif.", summary.installReasonLabel)
        assertEquals("Pourquoi revenir : créez un premier événement réutilisable.", summary.returnReasonLabel)
        assertEquals("Créer", summary.actionLabel)
        assertEquals(null, summary.action)
    }

    @Test
    fun `emotional summary explains empty workspace weakness`() {
        val summary = emptyList<Event>().toEmotionalSummary(currentUserId = "me", pollVotes = emptyMap())

        assertEquals(null, summary.eventId)
        assertEquals("Signal émotionnel", summary.title)
        assertEquals("Valeur encore abstraite", summary.headline)
        assertEquals("Score émotionnel : 20/100", summary.scoreLabel)
        assertEquals("Excitation : faible tant qu'aucun événement n'existe.", summary.excitementLabel)
        assertEquals("Sentiment de groupe : absent.", summary.groupFeelingLabel)
        assertEquals("Créez un premier événement concret pour rendre la valeur visible.", summary.nextActionLabel)
        assertEquals("Créer", summary.actionLabel)
        assertEquals(null, summary.action)
    }

    @Test
    fun `emotional summary surfaces polling engagement blocker`() {
        val summary = listOf(
            event(
                id = "poll",
                title = "Vote dinner",
                status = EventStatus.POLLING,
                participants = listOf("me", "alice", "sam")
            )
        ).toEmotionalSummary(
            currentUserId = "me",
            pollVotes = mapOf("poll" to mapOf("me" to Vote.YES))
        )

        assertEquals("poll", summary.eventId)
        assertEquals("Engagement à débloquer", summary.headline)
        assertEquals("Score émotionnel : 58/100", summary.scoreLabel)
        assertEquals("Excitation : moyenne, le groupe commence à se projeter.", summary.excitementLabel)
        assertEquals("Engagement : 2 participants à relancer.", summary.engagementLabel)
        assertEquals("Sérénité : encore fragile tant que la date n'est pas retenue.", summary.serenityLabel)
        assertEquals("Relancez les votes manquants avant de promettre une date.", summary.nextActionLabel)
        assertEquals("Ouvrir le vote", summary.actionLabel)
        assertEquals(EventWorkspaceSummaryAction.OpenPoll, summary.action)
    }

    @Test
    fun `emotional summary prioritizes organizing control center`() {
        val summary = listOf(
            event(
                id = "confirmed",
                title = "Confirmed dinner",
                status = EventStatus.CONFIRMED
            ),
            event(
                id = "organizing",
                title = "Team offsite",
                status = EventStatus.ORGANIZING,
                participants = listOf("me", "alice", "sam")
            )
        ).toEmotionalSummary(currentUserId = "me", pollVotes = emptyMap())

        assertEquals("organizing", summary.eventId)
        assertEquals("Centre de contrôle crédible", summary.headline)
        assertEquals("Score émotionnel : 86/100", summary.scoreLabel)
        assertEquals("Sérénité : haute, Wakeve remplace le chaos opérationnel.", summary.serenityLabel)
        assertEquals("Contrôle : très fort, le jour J devient pilotable.", summary.controlLabel)
        assertEquals("Gardez uniquement les prochaines actions critiques visibles.", summary.nextActionLabel)
        assertEquals("Piloter", summary.actionLabel)
        assertEquals(EventWorkspaceSummaryAction.OpenEvent, summary.action)
    }

    @Test
    fun `emotional summary turns finalized event into memory and retention`() {
        val summary = listOf(
            event(
                id = "final",
                title = "Summer retreat",
                description = "A weekend by the sea",
                status = EventStatus.FINALIZED,
                eventType = EventType.OUTDOOR_ACTIVITY
            )
        ).toEmotionalSummary(currentUserId = "me", pollVotes = emptyMap())

        assertEquals("final", summary.eventId)
        assertEquals("Mémoire et rétention", summary.headline)
        assertEquals("Score émotionnel : 72/100", summary.scoreLabel)
        assertEquals("Excitation : transformée en souvenir partageable.", summary.excitementLabel)
        assertEquals("Engagement : dépend des photos, remboursements et recap.", summary.engagementLabel)
        assertEquals("Partagez le recap puis recréez l'événement s'il a fonctionné.", summary.nextActionLabel)
        assertEquals("Réutiliser", summary.actionLabel)
        assertEquals(EventWorkspaceSummaryAction.RecreateFromTemplate, summary.action)
        assertEquals(
            EventWorkspaceCreationTemplate(
                title = "Summer retreat",
                description = "A weekend by the sea",
                eventType = EventType.OUTDOOR_ACTIVITY
            ),
            summary.template
        )
    }

    @Test
    fun `strategic summary explains empty workspace has no moat`() {
        val summary = emptyList<Event>().toStrategicSummary(currentUserId = "me", pollVotes = emptyMap())

        assertEquals(null, summary.eventId)
        assertEquals("Position stratégique", summary.title)
        assertEquals("Pas encore défendable", summary.headline)
        assertEquals("Verdict : agenda vide, aucun moat visible.", summary.verdictLabel)
        assertEquals("Scores : produit 20/100 · UX 35/100 · rétention 10/100.", summary.scorecardLabel)
        assertEquals(
            "Honnête : je ne lancerais pas, ne paierais pas et ne recommanderais pas encore.",
            summary.honestAnswerLabel
        )
        assertEquals("Face aux concurrents : WhatsApp suffit encore.", summary.competitorLabel)
        assertEquals("OS social : aucun espace collectif actif.", summary.operatingSystemLabel)
        assertEquals("Problème critique : aucune valeur démontrée en 30 secondes.", summary.criticalProblemLabel)
        assertEquals(
            "Fonction à créer : premier événement guidé avec invitation partageable.",
            summary.valueFeatureLabel
        )
        assertEquals("Capacité manquante : créer un premier groupe coordonné.", summary.missingCapabilityLabel)
        assertEquals("Créez un événement pour prouver la valeur de coordination.", summary.nextActionLabel)
        assertEquals("Créer", summary.actionLabel)
        assertEquals(null, summary.action)
    }

    @Test
    fun `strategic summary frames polling as useful but incomplete coordination`() {
        val summary = listOf(
            event(
                id = "poll",
                title = "Vote dinner",
                status = EventStatus.POLLING,
                participants = listOf("me", "alice", "sam")
            )
        ).toStrategicSummary(
            currentUserId = "me",
            pollVotes = mapOf("poll" to mapOf("me" to Vote.YES))
        )

        assertEquals("poll", summary.eventId)
        assertEquals("Meilleur que WhatsApp, pas encore un OS", summary.headline)
        assertEquals(
            "Verdict : utile pour choisir une date, pas encore pour organiser tout le séjour.",
            summary.verdictLabel
        )
        assertEquals("Scores : produit 52/100 · UX 66/100 · rétention 35/100.", summary.scorecardLabel)
        assertEquals(
            "Honnête : je l'utiliserais pour un dîner, pas encore pour 15 voyageurs.",
            summary.honestAnswerLabel
        )
        assertEquals(
            "Face aux concurrents : WhatsApp perd la trace des votes et des relances.",
            summary.competitorLabel
        )
        assertEquals("OS social : la première décision commune existe.", summary.operatingSystemLabel)
        assertEquals(
            "Problème critique : la valeur s'arrête si le vote ne débouche pas sur un plan.",
            summary.criticalProblemLabel
        )
        assertEquals(
            "Fonction à créer : conversion automatique du vote en plan de préparation.",
            summary.valueFeatureLabel
        )
        assertEquals(
            "Capacité manquante : transformer le vote en plan budget, transport et programme.",
            summary.missingCapabilityLabel
        )
        assertEquals("Obtenez les votes manquants pour sortir du débat.", summary.nextActionLabel)
        assertEquals("Ouvrir le vote", summary.actionLabel)
        assertEquals(EventWorkspaceSummaryAction.OpenPoll, summary.action)
    }

    @Test
    fun `strategic summary prioritizes organizing as social operating system proof`() {
        val summary = listOf(
            event(
                id = "final",
                title = "Finished dinner",
                status = EventStatus.FINALIZED
            ),
            event(
                id = "organizing",
                title = "Team offsite",
                status = EventStatus.ORGANIZING,
                participants = listOf("me", "alice", "sam")
            )
        ).toStrategicSummary(currentUserId = "me", pollVotes = emptyMap())

        assertEquals("organizing", summary.eventId)
        assertEquals("OS social crédible", summary.headline)
        assertEquals(
            "Verdict : Wakeve garde une raison d'exister même si WhatsApp, Splitwise et TripIt fusionnent.",
            summary.verdictLabel
        )
        assertEquals("Scores : produit 78/100 · UX 78/100 · rétention 66/100.", summary.scorecardLabel)
        assertEquals(
            "Honnête : je l'utiliserais pour un voyage à 15 si ce cockpit reste fiable.",
            summary.honestAnswerLabel
        )
        assertEquals(
            "Face aux concurrents : plan, présences, budget et jour J vivent dans un seul espace.",
            summary.competitorLabel
        )
        assertEquals("OS social : décisions, participants et prochaines actions convergent.", summary.operatingSystemLabel)
        assertEquals(
            "Problème critique : trop d'alertes ou de sections casserait la confiance.",
            summary.criticalProblemLabel
        )
        assertEquals(
            "Fonction à créer : cockpit jour J avec rôles, alertes et responsabilités.",
            summary.valueFeatureLabel
        )
        assertEquals("Gardez le centre de contrôle comme écran principal jusqu'au jour J.", summary.nextActionLabel)
        assertEquals("Piloter", summary.actionLabel)
        assertEquals(EventWorkspaceSummaryAction.OpenEvent, summary.action)
    }

    @Test
    fun `strategic summary turns finalized event into retention challenge`() {
        val summary = listOf(
            event(
                id = "final",
                title = "Summer retreat",
                description = "A weekend by the sea",
                status = EventStatus.FINALIZED,
                eventType = EventType.OUTDOOR_ACTIVITY
            )
        ).toStrategicSummary(currentUserId = "me", pollVotes = emptyMap())

        assertEquals("final", summary.eventId)
        assertEquals("Rétention à prouver", summary.headline)
        assertEquals(
            "Verdict : utile après l'événement seulement si recap, photos et dettes ressortent vite.",
            summary.verdictLabel
        )
        assertEquals("Scores : produit 64/100 · UX 70/100 · rétention 72/100.", summary.scorecardLabel)
        assertEquals(
            "Honnête : je reviendrais si le recap relance une nouvelle édition.",
            summary.honestAnswerLabel
        )
        assertEquals(
            "Face aux concurrents : Splitwise gagne si les remboursements restent séparés.",
            summary.competitorLabel
        )
        assertEquals("OS social : mémoire du groupe réutilisable.", summary.operatingSystemLabel)
        assertEquals("Problème critique : sans boucle post-event, Wakeve redevient jetable.", summary.criticalProblemLabel)
        assertEquals(
            "Fonction à créer : recap partageable avec photos, soldes et recréation.",
            summary.valueFeatureLabel
        )
        assertEquals("Transformez le recap en invitation pour la prochaine édition.", summary.nextActionLabel)
        assertEquals("Réutiliser", summary.actionLabel)
        assertEquals(EventWorkspaceSummaryAction.RecreateFromTemplate, summary.action)
        assertEquals(
            EventWorkspaceCreationTemplate(
                title = "Summer retreat",
                description = "A weekend by the sea",
                eventType = EventType.OUTDOOR_ACTIVITY
            ),
            summary.template
        )
    }

    @Test
    fun `roadmap summary starts with activation when workspace is empty`() {
        val summary = emptyList<Event>().toRoadmapSummary(currentUserId = "me", pollVotes = emptyMap())

        assertEquals(null, summary.eventId)
        assertEquals("Roadmap 6 mois", summary.title)
        assertEquals("D'abord prouver un groupe actif", summary.headline)
        assertEquals("0-30 jours : rendre la création et l'invitation évidentes.", summary.firstMonthLabel)
        assertEquals("31-90 jours : mesurer activation, abandon et première réutilisation.", summary.secondQuarterLabel)
        assertEquals("3-6 mois : connecter budget, transport, photos et recap.", summary.sixthMonthLabel)
        assertEquals(
            "Équipe : PM sur activation, designer sur premier wow, 3 devs sur création, partage et analytics.",
            summary.teamFocusLabel
        )
        assertEquals("Créer", summary.actionLabel)
        assertEquals(null, summary.action)
    }

    @Test
    fun `roadmap summary turns polling into vote conversion plan`() {
        val summary = listOf(
            event(
                id = "poll",
                title = "Vote dinner",
                status = EventStatus.POLLING,
                participants = listOf("me", "alice", "sam")
            )
        ).toRoadmapSummary(
            currentUserId = "me",
            pollVotes = mapOf("poll" to mapOf("me" to Vote.YES))
        )

        assertEquals("poll", summary.eventId)
        assertEquals("Sortir du débat de date", summary.headline)
        assertEquals("0-30 jours : relances utiles, vote lisible et décision sans ambiguïté.", summary.firstMonthLabel)
        assertEquals(
            "31-90 jours : convertir la date retenue en budget, transport et programme.",
            summary.secondQuarterLabel
        )
        assertEquals("3-6 mois : recommandations qui anticipent les blocages du groupe.", summary.sixthMonthLabel)
        assertEquals("Ouvrir le vote", summary.actionLabel)
        assertEquals(EventWorkspaceSummaryAction.OpenPoll, summary.action)
    }

    @Test
    fun `roadmap summary prioritizes organizing control center work`() {
        val summary = listOf(
            event(
                id = "poll",
                title = "Vote dinner",
                status = EventStatus.POLLING,
                participants = listOf("me", "alice", "sam")
            ),
            event(
                id = "organizing",
                title = "Team offsite",
                status = EventStatus.ORGANIZING,
                participants = listOf("me", "alice", "sam", "lee")
            )
        ).toRoadmapSummary(currentUserId = "me", pollVotes = emptyMap())

        assertEquals("organizing", summary.eventId)
        assertEquals("Industrialiser le centre de contrôle", summary.headline)
        assertEquals(
            "0-30 jours : fiabiliser jour J, présence, étapes et points de rendez-vous.",
            summary.firstMonthLabel
        )
        assertEquals("31-90 jours : rôles, alertes, offline et budget partagé.", summary.secondQuarterLabel)
        assertEquals(
            "3-6 mois : OS social pour 4 à 50 personnes avec transport et multi-destinations.",
            summary.sixthMonthLabel
        )
        assertEquals("Piloter", summary.actionLabel)
        assertEquals(EventWorkspaceSummaryAction.OpenEvent, summary.action)
    }

    @Test
    fun `roadmap summary turns finalized event into retention roadmap`() {
        val summary = listOf(
            event(
                id = "final",
                title = "Summer retreat",
                description = "A weekend by the sea",
                status = EventStatus.FINALIZED,
                eventType = EventType.OUTDOOR_ACTIVITY
            )
        ).toRoadmapSummary(currentUserId = "me", pollVotes = emptyMap())

        assertEquals("final", summary.eventId)
        assertEquals("Transformer la fin en rétention", summary.headline)
        assertEquals("0-30 jours : recap, photos et remboursements visibles.", summary.firstMonthLabel)
        assertEquals("31-90 jours : partage post-event et recréation en un geste.", summary.secondQuarterLabel)
        assertEquals(
            "3-6 mois : mémoire de groupe et recommandations pour la prochaine édition.",
            summary.sixthMonthLabel
        )
        assertEquals("Réutiliser", summary.actionLabel)
        assertEquals(EventWorkspaceSummaryAction.RecreateFromTemplate, summary.action)
        assertEquals(
            EventWorkspaceCreationTemplate(
                title = "Summer retreat",
                description = "A weekend by the sea",
                eventType = EventType.OUTDOOR_ACTIVITY
            ),
            summary.template
        )
    }

    @Test
    fun `finalized event builds quick reorganization summary`() {
        val event = event(
            id = "finalized",
            title = "Summer retreat",
            description = "A weekend by the sea",
            status = EventStatus.FINALIZED,
            eventType = EventType.OUTDOOR_ACTIVITY
        )

        val summary = event.toReorganizationSummary()

        assertEquals("Réorganiser rapidement", summary?.title)
        assertEquals(
            "Créez une nouvelle édition de Summer retreat avec le titre, la description et le type déjà repris.",
            summary?.body
        )
        assertEquals("Créer une nouvelle édition", summary?.actionLabel)
        assertEquals(
            EventWorkspaceCreationTemplate(
                title = "Summer retreat",
                description = "A weekend by the sea",
                eventType = EventType.OUTDOOR_ACTIVITY
            ),
            summary?.template
        )
    }

    @Test
    fun `active event does not build reorganization summary`() {
        assertEquals(
            null,
            event(
                id = "active",
                title = "Summer retreat",
                status = EventStatus.ORGANIZING
            ).toReorganizationSummary()
        )
    }

    @Test
    fun `event widget summary promotes event happening today`() {
        val summary = listOf(
            event(
                id = "tomorrow",
                title = "Tomorrow dinner",
                status = EventStatus.CONFIRMED,
                finalDate = "2026-06-21T18:00:00Z"
            ),
            event(
                id = "today",
                title = "Today barbecue",
                status = EventStatus.ORGANIZING,
                participants = listOf("me", "alice", "sam"),
                finalDate = "2026-06-20T12:00:00Z"
            )
        ).toEventWidgetSummary(
            now = Instant.parse("2026-06-20T08:00:00Z"),
            timeZone = TimeZone.UTC,
            currentUserId = "me"
        )

        assertEquals(EventWidgetKind.EventToday, summary.kind)
        assertEquals("today", summary.eventId)
        assertEquals("Aujourd'hui", summary.title)
        assertEquals("Today barbecue", summary.headline)
        assertEquals("3 participants attendus", summary.body)
        assertEquals("Interet utilisateur : 10/10", summary.userInterestLabel)
        assertEquals(
            "Widget prioritaire : il evite d'ouvrir l'app pour retrouver le rendez-vous du jour.",
            summary.rationaleLabel
        )
        assertEquals("Ouvrir", summary.actionLabel)
    }

    @Test
    fun `event widget summary shows countdown for next dated event`() {
        val summary = listOf(
            event(
                id = "future",
                title = "Board game night",
                status = EventStatus.CONFIRMED,
                finalDate = "2026-06-24T09:00:00Z"
            )
        ).toEventWidgetSummary(
            now = Instant.parse("2026-06-20T08:00:00Z"),
            timeZone = TimeZone.UTC,
            currentUserId = "me"
        )

        assertEquals(EventWidgetKind.Countdown, summary.kind)
        assertEquals("future", summary.eventId)
        assertEquals("Board game night", summary.title)
        assertEquals("J-4", summary.headline)
        assertEquals("À préparer", summary.body)
        assertEquals("Interet utilisateur : 8/10", summary.userInterestLabel)
        assertEquals(
            "Widget compte a rebours utile : il cree de l'anticipation sans spammer le groupe.",
            summary.rationaleLabel
        )
        assertEquals("Préparer", summary.actionLabel)
    }

    @Test
    fun `event widget summary promotes travel events`() {
        val summary = listOf(
            event(
                id = "travel",
                title = "Road trip Portugal",
                status = EventStatus.CONFIRMED,
                participants = listOf("me", "alice", "sam", "lea"),
                eventType = EventType.OUTDOOR_ACTIVITY,
                finalDate = "2026-06-24T09:00:00Z"
            )
        ).toEventWidgetSummary(
            now = Instant.parse("2026-06-20T08:00:00Z"),
            timeZone = TimeZone.UTC,
            currentUserId = "me"
        )

        assertEquals(EventWidgetKind.Travel, summary.kind)
        assertEquals("travel", summary.eventId)
        assertEquals("Voyage à préparer", summary.title)
        assertEquals("Road trip Portugal", summary.headline)
        assertEquals("4 participants - transport, budget et programme à vérifier", summary.body)
        assertEquals("Interet utilisateur : 9/10", summary.userInterestLabel)
        assertEquals(
            "Widget voyage pertinent : il regroupe les points qui cassent les groupes longs.",
            summary.rationaleLabel
        )
        assertEquals("Piloter", summary.actionLabel)
    }

    @Test
    fun `event widget summary falls back to next task when no date is known`() {
        val summary = listOf(
            event(
                id = "draft",
                title = "Draft dinner",
                status = EventStatus.DRAFT,
                proposedSlots = emptyList()
            )
        ).toEventWidgetSummary(
            now = Instant.parse("2026-06-20T08:00:00Z"),
            timeZone = TimeZone.UTC,
            currentUserId = "me"
        )

        assertEquals(EventWidgetKind.NextTask, summary.kind)
        assertEquals("draft", summary.eventId)
        assertEquals("Prochaine tâche", summary.title)
        assertEquals("Reprenez Draft dinner", summary.headline)
        assertEquals("Continuer", summary.body)
        assertEquals("Interet utilisateur : 7/10", summary.userInterestLabel)
        assertEquals(
            "Widget tache utile : il ramene l'organisateur vers le prochain blocage.",
            summary.rationaleLabel
        )
        assertEquals("Continuer", summary.actionLabel)
    }

    @Test
    fun `event widget summary explains empty state`() {
        val summary = emptyList<Event>().toEventWidgetSummary(
            now = Instant.parse("2026-06-20T08:00:00Z"),
            timeZone = TimeZone.UTC,
            currentUserId = "me"
        )

        assertEquals(EventWidgetKind.Empty, summary.kind)
        assertEquals(null, summary.eventId)
        assertEquals("Wakeve", summary.title)
        assertEquals("Aucun événement actif", summary.headline)
        assertEquals("Créez un événement pour afficher le prochain rendez-vous ici.", summary.body)
        assertEquals("Interet utilisateur : a activer", summary.userInterestLabel)
        assertEquals("Aucun widget utile sans evenement actif.", summary.rationaleLabel)
        assertEquals("Créer", summary.actionLabel)
    }

    private fun event(
        id: String,
        title: String,
        status: EventStatus,
        organizerId: String = "me",
        participants: List<String> = listOf("me"),
        description: String = "Description",
        eventType: EventType = EventType.OTHER,
        updatedAt: String = "2026-06-01T08:00:00Z",
        finalDate: String? = null,
        proposedSlots: List<TimeSlot> = listOf(
            TimeSlot(
                id = "$id-slot",
                start = "2026-07-14T09:00:00Z",
                end = "2026-07-14T18:00:00Z",
                timezone = "Europe/Paris"
            )
        )
    ): Event =
        Event(
            id = id,
            title = title,
            description = description,
            organizerId = organizerId,
            participants = participants,
            proposedSlots = proposedSlots,
            deadline = "2026-07-01T12:00:00Z",
            status = status,
            finalDate = finalDate,
            createdAt = "2026-06-01T08:00:00Z",
            updatedAt = updatedAt,
            eventType = eventType
        )
}
