package com.guyghost.wakeve.invitationexperience

import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.TimeSlot
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class EventLibraryProjectorContractTest {
    private val projector = EventLibraryProjector()
    private val now = Instant.parse("2030-06-15T12:00:00Z")

    @Test
    fun `library projections overlap but attendance and undated draft membership stay exact`() {
        val undatedDraft = assertNotNull(
            projector.project(
                input(
                    event = event("draft", EventStatus.DRAFT, organizerId = "viewer"),
                    role = ViewerRole.ORGANIZER
                ),
                now
            )
        )
        assertEquals(
            setOf(LibraryProjection.DRAFTS, LibraryProjection.HOSTING),
            undatedDraft.memberships
        )
        assertEquals(TemporalClass.UNDATED_DRAFT, undatedDraft.temporalClass)
        assertEquals(LibraryNextAction.CONTINUE_DRAFT, undatedDraft.nextAction)

        val hostedAndAttending = assertNotNull(
            projector.project(
                input(
                    event = event(
                        "hosted",
                        EventStatus.CONFIRMED,
                        organizerId = "viewer",
                        start = "2030-06-20T10:00:00Z",
                        end = "2030-06-20T12:00:00Z"
                    ),
                    role = ViewerRole.ORGANIZER,
                    membership = MembershipState.ActiveMember("member-viewer"),
                    rsvp = RsvpState.ACCEPTED,
                    action = LibraryNextAction.VIEW_EVENT
                ),
                now
            )
        )
        assertEquals(
            setOf(
                LibraryProjection.HOSTING,
                LibraryProjection.ATTENDING,
                LibraryProjection.UPCOMING
            ),
            hostedAndAttending.memberships
        )

        val pendingMember = assertNotNull(
            projector.project(
                input(
                    event = event(
                        "pending",
                        EventStatus.POLLING,
                        start = "2030-06-20T10:00:00Z",
                        end = "2030-06-20T12:00:00Z"
                    ),
                    role = ViewerRole.MEMBER,
                    membership = MembershipState.ActiveMember("member-viewer"),
                    rsvp = RsvpState.PENDING,
                    action = LibraryNextAction.SUBMIT_VOTE
                ),
                now
            )
        )
        assertEquals(setOf(LibraryProjection.UPCOMING), pendingMember.memberships)
    }

    @Test
    fun `missing contradictory or inactive access fails closed`() {
        val accessibleEvent = event(
            "event-1",
            EventStatus.POLLING,
            start = "2030-06-20T10:00:00Z",
            end = "2030-06-20T12:00:00Z"
        )
        val cases = listOf(
            input(
                event = accessibleEvent,
                role = ViewerRole.NON_MEMBER,
                membership = MembershipState.NonMember
            ),
            input(
                event = accessibleEvent,
                role = ViewerRole.MEMBER,
                membership = MembershipState.NonMember
            ),
            input(
                event = accessibleEvent,
                role = ViewerRole.MEMBER,
                membership = MembershipState.Left("member-viewer")
            ),
            input(
                event = accessibleEvent,
                role = ViewerRole.MEMBER,
                membership = MembershipState.Removed("member-viewer")
            ),
            input(
                event = accessibleEvent,
                role = ViewerRole.ORGANIZER,
                membership = MembershipState.NonMember
            )
        )

        cases.forEach { inaccessible ->
            assertNull(projector.project(inaccessible, now), inaccessible.toString())
        }
    }

    @Test
    fun `next action mapping is total with global archive override and interactive fallback`() {
        val cases = listOf(
            ActionCase(EventStatus.DRAFT, ViewerRole.ORGANIZER, null, null, LibraryNextAction.CONTINUE_DRAFT),
            ActionCase(EventStatus.POLLING, ViewerRole.MEMBER, LibraryNextAction.SUBMIT_VOTE, null, LibraryNextAction.SUBMIT_VOTE),
            ActionCase(EventStatus.POLLING, ViewerRole.ORGANIZER, LibraryNextAction.VIEW_POLL_RESULTS, null, LibraryNextAction.VIEW_POLL_RESULTS),
            ActionCase(EventStatus.POLLING, ViewerRole.MEMBER, null, null, LibraryNextAction.VIEW_EVENT),
            ActionCase(EventStatus.COMPARING, ViewerRole.ORGANIZER, LibraryNextAction.COMPARE_OPTIONS, null, LibraryNextAction.COMPARE_OPTIONS),
            ActionCase(EventStatus.CONFIRMED, ViewerRole.ORGANIZER, LibraryNextAction.CONTINUE_ORGANIZATION, null, LibraryNextAction.CONTINUE_ORGANIZATION),
            ActionCase(EventStatus.ORGANIZING, ViewerRole.MEMBER, LibraryNextAction.VIEW_EVENT, null, LibraryNextAction.VIEW_EVENT),
            ActionCase(EventStatus.FINALIZED, ViewerRole.MEMBER, LibraryNextAction.CONTINUE_ORGANIZATION, null, LibraryNextAction.VIEW_ARCHIVE),
            ActionCase(EventStatus.POLLING, ViewerRole.MEMBER, LibraryNextAction.SUBMIT_VOTE, "2030-06-10T12:00:00Z", LibraryNextAction.VIEW_ARCHIVE)
        )

        cases.forEachIndexed { index, case ->
            val isOrganizer = case.role == ViewerRole.ORGANIZER
            val projection = assertNotNull(
                projector.project(
                    input(
                        event = event(
                            "case-$index",
                            case.status,
                            organizerId = if (isOrganizer) "viewer" else "other-organizer",
                            start = case.end?.let { "2030-06-10T10:00:00Z" }
                                ?: "2030-06-20T10:00:00Z",
                            end = case.end ?: "2030-06-20T12:00:00Z"
                        ),
                        role = case.role,
                        membership = if (isOrganizer) {
                            MembershipState.NonMember
                        } else {
                            MembershipState.ActiveMember("member-viewer")
                        },
                        rsvp = if (isOrganizer) RsvpState.NOT_APPLICABLE else RsvpState.ACCEPTED,
                        action = case.inputAction
                    ),
                    now
                )
            )

            assertEquals(case.expected, projection.nextAction, "case #$index")
            if (case.expected == LibraryNextAction.VIEW_ARCHIVE) {
                assertEquals(InteractionPolicy.READ_ONLY, projection.interactionPolicy, "case #$index")
            }
        }
    }

    @Test
    fun `filter retains only requested membership and orders by structured date then event id`() {
        val cards = listOf(
            projectedCard("b", "2030-06-22T10:00:00Z"),
            projectedCard("c", "2030-06-20T10:00:00Z"),
            projectedCard("a", "2030-06-20T10:00:00Z")
        )

        assertEquals(
            listOf("a", "c", "b"),
            projector.filter(cards, LibraryProjection.UPCOMING).map { it.event.id }
        )
        assertEquals(emptyList(), projector.filter(cards, LibraryProjection.PAST))
    }

    @Test
    fun `conflict and permanent failure add reload warning without replacing next action`() {
        val cases = listOf(
            LibrarySyncState.Conflict("operation-conflict") to LibrarySyncWarning.CONFLICT,
            LibrarySyncState.PermanentFailure("operation-failed") to LibrarySyncWarning.PERMANENT_FAILURE
        )

        cases.forEach { (syncState, expectedWarning) ->
            val projection = assertNotNull(
                projector.project(
                    input(
                        event = event(
                            "event-${expectedWarning.name.lowercase()}",
                            EventStatus.POLLING,
                            start = "2030-06-20T10:00:00Z",
                            end = "2030-06-20T12:00:00Z"
                        ),
                        role = ViewerRole.MEMBER,
                        membership = MembershipState.ActiveMember("member-viewer"),
                        rsvp = RsvpState.ACCEPTED,
                        action = LibraryNextAction.SUBMIT_VOTE,
                        syncState = syncState
                    ),
                    now
                )
            )

            assertEquals(LibraryNextAction.SUBMIT_VOTE, projection.nextAction)
            assertEquals(syncState, projection.syncState)
            assertEquals(expectedWarning, projection.warning)
            assertEquals(true, projection.reloadAvailable)
        }
    }

    @Test
    fun `cancel load restores the exact captured stable state`() {
        val snapshot = listOf(projectedCard("event-1", "2030-06-20T10:00:00Z"))
        val capturedStates: List<Pair<PreviousStableState<List<LibraryCardProjection>>, LibraryLoadState<List<LibraryCardProjection>>>> =
            listOf(
                PreviousStableState.Idle to LibraryLoadState.Idle,
                PreviousStableState.Ready(snapshot, Freshness.Stale(now)) to
                    LibraryLoadState.Ready(snapshot, Freshness.Stale(now)),
                PreviousStableState.Empty("PAST") to LibraryLoadState.Empty(LibraryProjection.PAST)
            )

        capturedStates.forEach { (captured, expected) ->
            assertEquals(
                expected,
                projector.cancelLoad(LibraryLoadState.Loading(captured)),
                captured.toString()
            )
        }
    }

    private fun projectedCard(id: String, end: String): LibraryCardProjection = assertNotNull(
        projector.project(
            input(
                event = event(
                    id,
                    EventStatus.CONFIRMED,
                    start = end,
                    end = end
                ),
                role = ViewerRole.MEMBER,
                membership = MembershipState.ActiveMember("member-viewer"),
                rsvp = RsvpState.ACCEPTED,
                action = LibraryNextAction.VIEW_EVENT
            ),
            now
        )
    )

    private fun input(
        event: Event,
        role: ViewerRole,
        membership: MembershipState = MembershipState.NonMember,
        rsvp: RsvpState = RsvpState.NOT_APPLICABLE,
        action: LibraryNextAction? = null,
        syncState: LibrarySyncState = LibrarySyncState.Unavailable
    ) = LibraryEventInput(
        event = event,
        viewerId = "viewer",
        viewerRole = role,
        membershipState = membership,
        rsvpState = rsvp,
        interactiveNextAction = action,
        syncState = syncState
    )

    private fun event(
        id: String,
        status: EventStatus,
        organizerId: String = "other-organizer",
        start: String? = null,
        end: String? = null
    ) = Event(
        id = id,
        title = "Event $id",
        description = "Description",
        organizerId = organizerId,
        proposedSlots = if (start == null && end == null) {
            emptyList()
        } else {
            listOf(TimeSlot("slot-$id", start, end, "UTC"))
        },
        deadline = "2030-06-01T00:00:00Z",
        status = status,
        finalDate = start,
        createdAt = "2030-05-01T00:00:00Z",
        updatedAt = "2030-05-01T00:00:00Z"
    )

    private data class ActionCase(
        val status: EventStatus,
        val role: ViewerRole,
        val inputAction: LibraryNextAction?,
        val end: String?,
        val expected: LibraryNextAction
    )
}
