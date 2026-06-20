package com.guyghost.wakeve.ui.event

import android.content.Intent
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import org.junit.After
import com.guyghost.wakeve.PollVotingScreen
import com.guyghost.wakeve.PollVotingState
import com.guyghost.wakeve.access.ParticipantRsvp
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.TimeSlot
import com.guyghost.wakeve.models.Vote
import com.guyghost.wakeve.theme.WakeveTheme
import com.guyghost.wakeve.ui.designsystem.calculateWakeveAdaptiveInfo
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class EventWorkspaceScreenTest {
    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private val scenarios = mutableListOf<ActivityScenario<ComposeTestHostActivity>>()

    @After
    fun tearDown() {
        scenarios.forEach { it.close() }
        scenarios.clear()
    }

    @Test
    fun compactLayoutShowsSingleListPane() {
        setContent {
            WakeveTheme(dynamicColor = false) {
                Box(
                    modifier = Modifier
                        .width(390.dp)
                        .height(820.dp)
                ) {
                    EventWorkspaceScreen(
                        state = workspaceState(),
                        onFilterChange = {},
                        onSearchChange = {},
                        onCreateEvent = {},
                        onCreateFromTemplate = {},
                        onOpenProfile = {},
                        onSelectEvent = { _, _ -> },
                        onOpenEvent = {},
                        onOpenPoll = {},
                        onRetry = {},
                        modifier = Modifier.fillMaxSize(),
                        adaptiveInfoOverride = calculateWakeveAdaptiveInfo(390, 820)
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag("event_list_pane").assertIsDisplayed()
        composeTestRule.onNodeWithText("Lisbon team retreat").assertIsDisplayed()
        composeTestRule.onNodeWithTag("event_grid_columns_1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("event_filter_scrollable").assertIsDisplayed()
    }

    @Test
    fun expandedLayoutShowsListAndDetailPanes() {
        setContent {
            WakeveTheme(dynamicColor = false) {
                Box(
                    modifier = Modifier
                        .width(1000.dp)
                        .height(760.dp)
                ) {
                    EventWorkspaceScreen(
                        state = workspaceState(),
                        onFilterChange = {},
                        onSearchChange = {},
                        onCreateEvent = {},
                        onCreateFromTemplate = {},
                        onOpenProfile = {},
                        onSelectEvent = { _, _ -> },
                        onOpenEvent = {},
                        onOpenPoll = {},
                        onRetry = {},
                        modifier = Modifier.fillMaxSize(),
                        adaptiveInfoOverride = calculateWakeveAdaptiveInfo(1000, 760)
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag("event_list_pane").assertIsDisplayed()
        composeTestRule.onAllNodesWithTag("event_detail_pane").assertCountEquals(1)
        composeTestRule.onNodeWithText("Participants").assertIsDisplayed()
    }

    @Test
    fun expandedLayoutWithoutSelectionShowsPlaceholder() {
        setContent {
            WakeveTheme(dynamicColor = false) {
                Box(
                    modifier = Modifier
                        .width(1000.dp)
                        .height(760.dp)
                ) {
                    EventWorkspaceScreen(
                        state = workspaceState(selected = false),
                        onFilterChange = {},
                        onSearchChange = {},
                        onCreateEvent = {},
                        onCreateFromTemplate = {},
                        onOpenProfile = {},
                        onSelectEvent = { _, _ -> },
                        onOpenEvent = {},
                        onOpenPoll = {},
                        onRetry = {},
                        modifier = Modifier.fillMaxSize(),
                        adaptiveInfoOverride = calculateWakeveAdaptiveInfo(1000, 760)
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag("event_list_pane").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Sélectionnez un événement").assertCountEquals(1)
    }

    @Test
    fun landscapeCompactHeightUsesReducedChromeAndAdaptiveGrid() {
        setContent {
            WakeveTheme(dynamicColor = false) {
                Box(
                    modifier = Modifier
                        .width(820.dp)
                        .height(390.dp)
                ) {
                    EventWorkspaceScreen(
                        state = workspaceState(),
                        onFilterChange = {},
                        onSearchChange = {},
                        onCreateEvent = {},
                        onCreateFromTemplate = {},
                        onOpenProfile = {},
                        onSelectEvent = { _, _ -> },
                        onOpenEvent = {},
                        onOpenPoll = {},
                        onRetry = {},
                        modifier = Modifier.fillMaxSize(),
                        adaptiveInfoOverride = calculateWakeveAdaptiveInfo(820, 390)
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag("event_list_pane").assertIsDisplayed()
        composeTestRule.onAllNodesWithTag("event_detail_pane").assertCountEquals(0)
        composeTestRule.onNodeWithTag("event_grid_columns_2").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Créer un événement").assertIsDisplayed()
    }

    @Test
    fun desktopWidthUsesWrappedFiltersAndThreeColumnGrid() {
        setContent {
            WakeveTheme(dynamicColor = false) {
                Box(
                    modifier = Modifier
                        .width(1440.dp)
                        .height(960.dp)
                ) {
                    EventWorkspaceScreen(
                        state = workspaceState(selected = false),
                        onFilterChange = {},
                        onSearchChange = {},
                        onCreateEvent = {},
                        onCreateFromTemplate = {},
                        onOpenProfile = {},
                        onSelectEvent = { _, _ -> },
                        onOpenEvent = {},
                        onOpenPoll = {},
                        onRetry = {},
                        modifier = Modifier.fillMaxSize(),
                        adaptiveInfoOverride = calculateWakeveAdaptiveInfo(1440, 960)
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag("event_filter_wrapped").assertIsDisplayed()
        composeTestRule.onNodeWithTag("event_grid_columns_2").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Sélectionnez un événement").assertCountEquals(1)
    }

    @Test
    fun compactEventSelectionRequestsNavigation() {
        var selectedEventId: String? = null
        var shouldNavigate: Boolean? = null

        setContent {
            WakeveTheme(dynamicColor = false) {
                Box(
                    modifier = Modifier
                        .width(390.dp)
                        .height(820.dp)
                ) {
                    EventWorkspaceScreen(
                        state = workspaceState(),
                        onFilterChange = {},
                        onSearchChange = {},
                        onCreateEvent = {},
                        onCreateFromTemplate = {},
                        onOpenProfile = {},
                        onSelectEvent = { eventId, navigate ->
                            selectedEventId = eventId
                            shouldNavigate = navigate
                        },
                        onOpenEvent = {},
                        onOpenPoll = {},
                        onRetry = {},
                        modifier = Modifier.fillMaxSize(),
                        adaptiveInfoOverride = calculateWakeveAdaptiveInfo(390, 820)
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Lisbon team retreat").performClick()

        assertEquals("event-1", selectedEventId)
        assertEquals(true, shouldNavigate)
    }

    @Test
    fun createEventFabIsAvailable() {
        var createClicked = false

        setContent {
            WakeveTheme(dynamicColor = false) {
                EventWorkspaceScreen(
                    state = workspaceState(),
                    onFilterChange = {},
                    onSearchChange = {},
                    onCreateEvent = { createClicked = true },
                    onCreateFromTemplate = {},
                    onOpenProfile = {},
                    onSelectEvent = { _, _ -> },
                    onOpenEvent = {},
                    onOpenPoll = {},
                    onRetry = {}
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription("Créer un événement")
            .performClick()

        assertEquals(true, createClicked)
    }

    @Test
    fun pollVotingEmitsVoteAndSubmitCallbacks() {
        val event = sampleEvent("event-1", "Lisbon team retreat")
        var selectedVote: Vote? = null
        var submitted = false

        setContent {
            WakeveTheme(dynamicColor = false) {
                PollVotingScreen(
                    event = event,
                    state = PollVotingState(
                        eventId = event.id,
                        participantId = "user-2",
                        votes = mapOf(event.proposedSlots.first().id to Vote.YES)
                    ),
                    onVoteChange = { _, vote -> selectedVote = vote },
                    onSubmitVotes = { submitted = true }
                )
            }
        }

        composeTestRule.onNodeWithTag("poll_voting_screen").assertIsDisplayed()
        composeTestRule.onNodeWithText("Peut-être").performClick()
        composeTestRule.onNodeWithText("Envoyer mes votes").assertIsEnabled().performClick()

        assertEquals(Vote.MAYBE, selectedVote)
        assertEquals(true, submitted)
    }

    @Test
    fun rsvpCardEmitsSelectedResponse() {
        var response: ParticipantRsvp? = null

        setContent {
            WakeveTheme(dynamicColor = false) {
                EventRsvpResponseCard(
                    state = EventRsvpUiState(
                        participantId = "user-2",
                        selectedResponse = ParticipantRsvp.PENDING,
                        isOrganizer = false,
                        isEnabled = true,
                        statusLabel = "Réponse en attente"
                    ),
                    onResponseSelected = { response = it }
                )
            }
        }

        composeTestRule.onNodeWithTag("event_rsvp_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("event_rsvp_accepted").performClick()

        assertEquals(ParticipantRsvp.ACCEPTED, response)
    }

    private fun setContent(content: @Composable () -> Unit) {
        val intent = Intent().setClassName(
            "com.guyghost.wakeve",
            ComposeTestHostActivity::class.java.name
        )
        val scenario = ActivityScenario.launch<ComposeTestHostActivity>(intent)
        scenarios += scenario
        scenario.onActivity { activity ->
            activity.setContent(content = content)
        }
        composeTestRule.waitForIdle()
    }

    private fun workspaceState(selected: Boolean = true): EventWorkspaceUiState {
        val events = listOf(sampleEvent("event-1", "Lisbon team retreat"), sampleEvent("event-2", "Birthday dinner"))
        return EventWorkspaceUiState(
            isLoading = false,
            error = null,
            selectedFilter = EventListFilter.Upcoming,
            searchQuery = "",
            actionSummary = EventWorkspaceActionSummary(
                eventId = events.first().id,
                title = "Faites avancer le sondage",
                body = "1 participant à relancer avant de bloquer une date.",
                actionLabel = "Ouvrir le vote",
                action = EventWorkspaceSummaryAction.OpenPoll
            ),
            viralLoopSummary = EventViralLoopSummary(
                eventId = events.first().id,
                title = "Boucle de croissance",
                headline = "1 vote à obtenir",
                inviteReasonLabel = "Pourquoi inviter : chaque invité débloque la décision collective.",
                installReasonLabel = "Pourquoi installer : voter, suivre la date limite et éviter les relances privées.",
                returnReasonLabel = "Pourquoi revenir : voir la date retenue et la suite du plan.",
                actionLabel = "Partager le vote",
                action = EventWorkspaceSummaryAction.OpenPoll
            ),
            emotionalSummary = EventEmotionalSummary(
                eventId = events.first().id,
                title = "Signal émotionnel",
                headline = "Engagement à débloquer",
                scoreLabel = "Score émotionnel : 58/100",
                excitementLabel = "Excitation : moyenne, le groupe commence à se projeter.",
                anticipationLabel = "Anticipation : liée à la date qui va sortir du vote.",
                engagementLabel = "Engagement : 1 participant à relancer.",
                groupFeelingLabel = "Sentiment de groupe : visible grâce aux réponses partagées.",
                serenityLabel = "Sérénité : encore fragile tant que la date n'est pas retenue.",
                controlLabel = "Contrôle : meilleur que WhatsApp, mais la décision reste ouverte.",
                nextActionLabel = "Relancez les votes manquants avant de promettre une date.",
                actionLabel = "Ouvrir le vote",
                action = EventWorkspaceSummaryAction.OpenPoll
            ),
            widgetSummary = EventWidgetSummary(
                kind = EventWidgetKind.Countdown,
                eventId = events.first().id,
                title = "Lisbon team retreat",
                headline = "J-12",
                body = "Vote attendu",
                userInterestLabel = "Interet utilisateur : 8/10",
                rationaleLabel = "Widget compte a rebours utile : il cree de l'anticipation sans spammer le groupe.",
                actionLabel = "Préparer"
            ),
            events = events.map {
                EventListItemUiState(
                    id = it.id,
                    title = it.title,
                    description = it.description,
                    statusLabel = it.status.name.lowercase().replaceFirstChar { char -> char.titlecase() },
                    nextActionLabel = "Vote attendu",
                    deadlineLabel = "Deadline ${it.deadline}",
                    participantsLabel = "${it.participants.size} participants",
                    isOrganizer = true
                )
            },
            selectedEvent = events.first().takeIf { selected },
            participantCount = events.first().participants.size,
            pollVoteCount = 2
        )
    }

    private fun sampleEvent(id: String, title: String): Event =
        Event(
            id = id,
            title = title,
            description = "Coordinate date, attendance, and logistics.",
            organizerId = "user-1",
            participants = listOf("user-1", "user-2"),
            proposedSlots = listOf(
                TimeSlot(
                    id = "$id-slot",
                    start = "2026-07-14T09:00:00Z",
                    end = "2026-07-14T18:00:00Z",
                    timezone = "Europe/Paris"
                )
            ),
            deadline = "2026-07-01T12:00:00Z",
            status = EventStatus.POLLING,
            createdAt = "2026-06-01T08:00:00Z",
            updatedAt = "2026-06-01T08:00:00Z"
        )
}
