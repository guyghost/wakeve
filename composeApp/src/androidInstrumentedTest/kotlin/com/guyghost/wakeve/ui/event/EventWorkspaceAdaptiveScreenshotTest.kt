package com.guyghost.wakeve.ui.event

import android.content.Intent
import android.graphics.Bitmap
import android.content.ContentValues
import android.provider.MediaStore
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.TimeSlot
import com.guyghost.wakeve.theme.WakeveTheme
import com.guyghost.wakeve.ui.designsystem.calculateWakeveAdaptiveInfo
import org.junit.After
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertNotNull

class EventWorkspaceAdaptiveScreenshotTest {
    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private val scenarios = mutableListOf<ActivityScenario<ComposeTestHostActivity>>()

    @After
    fun tearDown() {
        scenarios.forEach { it.close() }
        scenarios.clear()
    }

    @Test
    fun capturesAdaptiveBreakpointsWithFakeData() {
        listOf(
            ScreenshotBreakpoint("phone-portrait", 393, 852, true),
            ScreenshotBreakpoint("phone-landscape", 852, 393, true),
            ScreenshotBreakpoint("foldable", 673, 841, true),
            ScreenshotBreakpoint("tablet", 1024, 768, true),
            ScreenshotBreakpoint("desktop", 1440, 960, false)
        ).forEach { breakpoint ->
            captureBreakpoint(breakpoint)
        }
    }

    private fun captureBreakpoint(breakpoint: ScreenshotBreakpoint) {
        val intent = Intent().setClassName(
            "com.guyghost.wakeve",
            ComposeTestHostActivity::class.java.name
        )
        val scenario = ActivityScenario.launch<ComposeTestHostActivity>(intent)
        scenarios += scenario
        lateinit var activityRef: ComposeTestHostActivity
        scenario.onActivity { activity ->
            activityRef = activity
            activity.setContent {
                WakeveTheme(dynamicColor = false) {
                    Box(
                        modifier = Modifier
                            .width(breakpoint.widthDp.dp)
                            .height(breakpoint.heightDp.dp)
                    ) {
                        EventWorkspaceScreen(
                            state = screenshotState(selected = breakpoint.selected),
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
                            adaptiveInfoOverride = calculateWakeveAdaptiveInfo(
                                widthDp = breakpoint.widthDp,
                                heightDp = breakpoint.heightDp
                            )
                        )
                    }
                }
            }
        }

        composeTestRule.waitForIdle()
        val bitmap = composeTestRule.onRoot().captureToImage().asAndroidBitmap()
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "${breakpoint.name}.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/WakeveAdaptiveScreenshots")
        }
        val uri = activityRef.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        assertNotNull(uri, "MediaStore screenshot URI should be created")
        activityRef.contentResolver.openOutputStream(uri).use { output ->
            assertNotNull(output, "MediaStore screenshot output stream should be opened")
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
    }

    private fun screenshotState(selected: Boolean): EventWorkspaceUiState {
        val events = listOf(
            screenshotEvent("event-1", "Lisbon team retreat", EventStatus.POLLING),
            screenshotEvent("event-2", "Birthday dinner", EventStatus.DRAFT),
            screenshotEvent("event-3", "Summer weekend", EventStatus.CONFIRMED),
            screenshotEvent("event-4", "Design offsite", EventStatus.POLLING),
            screenshotEvent("event-5", "Friends reunion", EventStatus.DRAFT),
            screenshotEvent("event-6", "Launch party", EventStatus.CONFIRMED)
        )

        return EventWorkspaceUiState(
            isLoading = false,
            error = null,
            selectedFilter = EventListFilter.Upcoming,
            searchQuery = "",
            actionSummary = EventWorkspaceActionSummary(
                eventId = events.first().id,
                title = "Faites avancer le sondage",
                body = "3 participants à relancer avant de bloquer une date.",
                actionLabel = "Ouvrir le vote",
                action = EventWorkspaceSummaryAction.OpenPoll
            ),
            widgetSummary = EventWidgetSummary(
                kind = EventWidgetKind.Countdown,
                eventId = events.first().id,
                title = "Lisbon team retreat",
                headline = "J-12",
                body = "Vote attendu",
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
                    isOrganizer = it.organizerId == "user-1"
                )
            },
            selectedEvent = events.first().takeIf { selected },
            participantCount = events.first().participants.size,
            pollVoteCount = 4
        )
    }

    private fun screenshotEvent(id: String, title: String, status: EventStatus): Event =
        Event(
            id = id,
            title = title,
            description = "Coordinate dates, attendance, and logistics with the group.",
            organizerId = "user-1",
            participants = listOf("user-1", "user-2", "user-3", "user-4"),
            proposedSlots = listOf(
                TimeSlot(
                    id = "$id-slot",
                    start = "2026-07-14T09:00:00Z",
                    end = "2026-07-14T18:00:00Z",
                    timezone = "Europe/Paris"
                )
            ),
            deadline = "2026-07-01T12:00:00Z",
            status = status,
            createdAt = "2026-06-01T08:00:00Z",
            updatedAt = "2026-06-01T08:00:00Z"
        )

    private data class ScreenshotBreakpoint(
        val name: String,
        val widthDp: Int,
        val heightDp: Int,
        val selected: Boolean
    )
}
