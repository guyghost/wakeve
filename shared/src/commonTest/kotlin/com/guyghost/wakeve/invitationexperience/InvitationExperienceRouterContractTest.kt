package com.guyghost.wakeve.invitationexperience

import com.guyghost.wakeve.models.EventStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class InvitationExperienceRouterContractTest {
    private val router = InvitationExperienceRouter()
    private val allRoutes = InvitationExperienceRouteCapability.entries.toSet()
    private val allAccess = InvitationExperienceRouteAccess(
        canEditDraft = true,
        canUsePoll = true,
        canReadPollResults = true,
        canOpenParticipants = true,
        canOpenOrganization = true
    )

    @Test
    fun `every canvas action resolves to its installed typed owner or safe local details`() {
        val cases = listOf(
            CanvasCase(
                InvitationExperienceCanvasAction.EDIT_DRAFT,
                context(EventStatus.DRAFT),
                destination(InvitationExperienceRouteCapability.DRAFT_EDITOR)
            ),
            CanvasCase(
                InvitationExperienceCanvasAction.SUBMIT_VOTE,
                context(EventStatus.POLLING, role = ViewerRole.MEMBER),
                destination(InvitationExperienceRouteCapability.POLL)
            ),
            CanvasCase(
                InvitationExperienceCanvasAction.VIEW_POLL_RESULTS,
                context(EventStatus.POLLING, role = ViewerRole.MEMBER),
                destination(InvitationExperienceRouteCapability.POLL)
            ),
            CanvasCase(
                InvitationExperienceCanvasAction.COMPARE_OPTIONS,
                context(EventStatus.COMPARING),
                destination(InvitationExperienceRouteCapability.ORGANIZATION)
            ),
            CanvasCase(
                InvitationExperienceCanvasAction.CONTINUE_ORGANIZATION,
                context(EventStatus.CONFIRMED),
                destination(InvitationExperienceRouteCapability.ORGANIZATION)
            ),
            CanvasCase(
                InvitationExperienceCanvasAction.SHOW_ACCESS_STATE,
                context(EventStatus.CONFIRMED, role = ViewerRole.MEMBER),
                InvitationExperienceRouteResolution.LocalDetails(readOnly = false)
            ),
            CanvasCase(
                InvitationExperienceCanvasAction.SHOW_DETAILS,
                context(EventStatus.ORGANIZING, role = ViewerRole.MEMBER),
                InvitationExperienceRouteResolution.LocalDetails(readOnly = false)
            ),
            CanvasCase(
                InvitationExperienceCanvasAction.VIEW_FINAL_DETAILS,
                context(EventStatus.CONFIRMED, role = ViewerRole.MEMBER),
                InvitationExperienceRouteResolution.LocalDetails(readOnly = false)
            )
        )

        cases.forEach { case ->
            assertEquals(
                case.expected,
                router.resolve(
                    InvitationExperienceRouteRequest.CanvasAction(case.action),
                    case.context
                ),
                case.action.name
            )
        }
    }

    @Test
    fun `missing route falls back to details while denied access never dispatches an adjacent route`() {
        val missingPoll = context(
            status = EventStatus.POLLING,
            role = ViewerRole.MEMBER,
            installedRoutes = allRoutes - InvitationExperienceRouteCapability.POLL
        )
        assertEquals(
            InvitationExperienceRouteResolution.LocalDetails(readOnly = false),
            router.resolve(
                InvitationExperienceRouteRequest.CanvasAction(
                    InvitationExperienceCanvasAction.SUBMIT_VOTE
                ),
                missingPoll
            )
        )

        val denied = context(
            status = EventStatus.POLLING,
            role = ViewerRole.MEMBER,
            access = allAccess.copy(
                canUsePoll = false,
                canReadPollResults = false,
                canOpenParticipants = false
            )
        )
        assertIs<InvitationExperienceRouteResolution.AccessDenied>(
            router.resolve(
                InvitationExperienceRouteRequest.CanvasAction(
                    InvitationExperienceCanvasAction.SUBMIT_VOTE
                ),
                denied
            )
        )
        assertIs<InvitationExperienceRouteResolution.AccessDenied>(
            router.resolve(InvitationExperienceRouteRequest.Participants, denied)
        )
    }

    @Test
    fun `past or finalized preflight redirects every stale route to archive before owner routing`() {
        val staleRequests = listOf(
            InvitationExperienceRouteRequest.CanvasAction(InvitationExperienceCanvasAction.EDIT_DRAFT),
            InvitationExperienceRouteRequest.CanvasAction(InvitationExperienceCanvasAction.SUBMIT_VOTE),
            InvitationExperienceRouteRequest.CanvasAction(InvitationExperienceCanvasAction.COMPARE_OPTIONS),
            InvitationExperienceRouteRequest.Participants,
            InvitationExperienceRouteRequest.EventInformation,
            InvitationExperienceRouteRequest.DeepLink(
                InvitationExperienceRouteCapability.DRAFT_EDITOR,
                InvitationExperienceDeepLinkIntent.MUTATE
            ),
            InvitationExperienceRouteRequest.DeepLink(
                InvitationExperienceRouteCapability.ORGANIZATION,
                InvitationExperienceDeepLinkIntent.READ
            )
        )
        val archivedContexts = listOf(
            context(EventStatus.ORGANIZING, temporal = TemporalClass.PAST),
            context(EventStatus.FINALIZED, temporal = TemporalClass.UPCOMING)
        )

        archivedContexts.forEach { archived ->
            staleRequests.forEach { request ->
                assertEquals(
                    destination(InvitationExperienceRouteCapability.ARCHIVE_DETAIL),
                    router.resolve(request, archived),
                    "$archived / $request"
                )
            }
        }

        val archiveUnavailable = context(
            status = EventStatus.ORGANIZING,
            temporal = TemporalClass.PAST,
            installedRoutes = allRoutes - InvitationExperienceRouteCapability.ARCHIVE_DETAIL
        )
        assertEquals(
            InvitationExperienceRouteResolution.LocalDetails(readOnly = true),
            router.resolve(
                InvitationExperienceRouteRequest.CanvasAction(
                    InvitationExperienceCanvasAction.CONTINUE_ORGANIZATION
                ),
                archiveUnavailable
            )
        )
    }

    @Test
    fun `deep links reuse the same installed route and typed access guards`() {
        val cases = listOf(
            InvitationExperienceRouteRequest.DeepLink(
                InvitationExperienceRouteCapability.POLL,
                InvitationExperienceDeepLinkIntent.READ
            ) to destination(InvitationExperienceRouteCapability.POLL),
            InvitationExperienceRouteRequest.DeepLink(
                InvitationExperienceRouteCapability.PARTICIPANTS,
                InvitationExperienceDeepLinkIntent.READ
            ) to destination(InvitationExperienceRouteCapability.PARTICIPANTS),
            InvitationExperienceRouteRequest.DeepLink(
                InvitationExperienceRouteCapability.EVENT_INFORMATION,
                InvitationExperienceDeepLinkIntent.READ
            ) to destination(InvitationExperienceRouteCapability.EVENT_INFORMATION),
            InvitationExperienceRouteRequest.DeepLink(
                InvitationExperienceRouteCapability.ORGANIZATION,
                InvitationExperienceDeepLinkIntent.MUTATE
            ) to destination(InvitationExperienceRouteCapability.ORGANIZATION)
        )
        val interactive = context(EventStatus.ORGANIZING)

        cases.forEach { (request, expected) ->
            assertEquals(expected, router.resolve(request, interactive), request.toString())
        }

        assertIs<InvitationExperienceRouteResolution.AccessDenied>(
            router.resolve(
                InvitationExperienceRouteRequest.DeepLink(
                    InvitationExperienceRouteCapability.PARTICIPANTS,
                    InvitationExperienceDeepLinkIntent.READ
                ),
                interactive.copy(access = allAccess.copy(canOpenParticipants = false))
            )
        )
        assertEquals(
            InvitationExperienceRouteResolution.LocalDetails(readOnly = false),
            router.resolve(
                InvitationExperienceRouteRequest.DeepLink(
                    InvitationExperienceRouteCapability.ORGANIZATION,
                    InvitationExperienceDeepLinkIntent.READ
                ),
                interactive.copy(
                    installedRoutes = allRoutes - InvitationExperienceRouteCapability.ORGANIZATION
                )
            )
        )
    }

    private fun context(
        status: EventStatus,
        temporal: TemporalClass = TemporalClass.UPCOMING,
        role: ViewerRole = ViewerRole.ORGANIZER,
        access: InvitationExperienceRouteAccess = allAccess,
        installedRoutes: Set<InvitationExperienceRouteCapability> = allRoutes
    ) = InvitationExperienceRouteContext(
        eventStatus = status,
        temporalClass = temporal,
        viewerRole = role,
        access = access,
        installedRoutes = installedRoutes
    )

    private fun destination(route: InvitationExperienceRouteCapability) =
        InvitationExperienceRouteResolution.Destination(route)

    private data class CanvasCase(
        val action: InvitationExperienceCanvasAction,
        val context: InvitationExperienceRouteContext,
        val expected: InvitationExperienceRouteResolution
    )
}
