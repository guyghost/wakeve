package com.guyghost.wakeve.navigation

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TransportNavigationContractTest {
    @Test
    fun screenDefinesEventScopedTransportPlanningRoute() {
        val source = projectFile(
            "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/navigation/Screen.kt"
        ).readText()

        assertTrue(
            source.contains("TransportPlanning") &&
                source.contains("""Screen("event/{eventId}/transport")""") &&
                source.contains("fun createRoute(eventId: String) = \"event/\$eventId/transport\""),
            "Android navigation must expose Screen.TransportPlanning as event/{eventId}/transport."
        )
    }

    @Test
    fun navHostRegistersTransportPlanningDestination() {
        val source = projectFile(
            "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/navigation/WakeveNavHost.kt"
        ).readText()

        assertTrue(
            source.contains("import com.guyghost.wakeve.ui.transport.TransportPlanningScreen") &&
                source.contains("route = Screen.TransportPlanning.route") &&
                source.contains("""navArgument("eventId")""") &&
                source.contains("TransportPlanningScreen("),
            "WakeveNavHost must register the transport planning destination and pass the eventId to the screen."
        )
    }

    @Test
    fun navHostWiresTransportScreenToRealRepositoryStateInsteadOfStubDefaults() {
        val source = projectFile(
            "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/navigation/WakeveNavHost.kt"
        ).readText()
        val transportBlock = source.substringAfter("route = Screen.TransportPlanning.route")
            .substringBefore("// ========================================\n        // COMMUNICATION")

        assertTrue(
            transportBlock.contains("TransportRepository") ||
                transportBlock.contains("TransportViewModel") ||
                transportBlock.contains("transportRepository"),
            "Transport navigation must obtain transport state from a repository/view-model, not only event/scenario state."
        )
        assertTrue(
            transportBlock.contains("readiness =") && !transportBlock.contains("readiness = null"),
            "WakeveNavHost must pass real TransportReadiness to TransportPlanningScreen."
        )
        assertTrue(
            transportBlock.contains("plans =") && !transportBlock.contains("plans = emptyList()"),
            "WakeveNavHost must pass persisted/generated transport plans to TransportPlanningScreen."
        )
        assertTrue(
            transportBlock.contains("pendingSync =") && !transportBlock.contains("pendingSync = false"),
            "WakeveNavHost must pass actual pending sync state for transport operations."
        )
        assertFalse(
            transportBlock.contains("Toast.makeText"),
            "Transport generate/select callbacks must mutate repository/state and must not be Toast-only stubs."
        )
    }

    @Test
    fun navHostRequiresARealSelectedDestinationBeforeTransportStateAndMutations() {
        val source = projectFile(
            "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/navigation/WakeveNavHost.kt"
        ).readText()
        val transportBlock = source.substringAfter("route = Screen.TransportPlanning.route")
            .substringBefore("// ========================================\n        // COMMUNICATION")
        val generateCallback = transportBlock.substringAfter("onGeneratePlan =")
            .substringBefore("onSelectFinalPlan =")

        assertFalse(
            transportBlock.contains("\"Destination confirmed\"") ||
                transportBlock.contains("\"Destination confirmee\""),
            "Android transport planning must not fabricate a destination fallback; generation requires a selected scenario destination."
        )
        assertTrue(
            transportBlock.contains("selectedScenario") &&
                transportBlock.contains("ScenarioStatus.SELECTED"),
            "Transport destination must be derived from the selected scenario, not from a display-only label fallback."
        )
        assertTrue(
            Regex("""val\s+(selectedDestination|destination)\s*[:=][\s\S]{0,160}\?""")
                .containsMatchIn(transportBlock) ||
                transportBlock.contains("selectedDestination == null") ||
                transportBlock.contains("destination == null"),
            "Navigation must model the selected destination as nullable until a real selected scenario exists."
        )
        assertTrue(
            generateCallback.contains("selectedDestination") ||
                generateCallback.contains("destination != null") ||
                generateCallback.contains("return@launch"),
            "Generate callback must guard against missing selected destination before calling TransportRepository.generatePlan."
        )
    }

    @Test
    fun navHostLoadsSelectedTransportDestinationFromPersistedScenarioRepositoryOnDirectEntry() {
        val source = projectFile(
            "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/navigation/WakeveNavHost.kt"
        ).readText()
        val transportBlock = source.substringAfter("route = Screen.TransportPlanning.route")
            .substringBefore("// ========================================\n        // COMMUNICATION")

        assertTrue(
            source.contains("import com.guyghost.wakeve.repository.ScenarioRepository") &&
                Regex("""val\s+scenarioRepository\s*:\s*ScenarioRepository\s*=\s*koinInject\(""")
                    .containsMatchIn(transportBlock),
            "Transport navigation must inject ScenarioRepository so direct route entry/restart can read persisted selected scenario state."
        )
        assertTrue(
            containsAny(
                transportBlock,
                "scenarioRepository.getSelectedScenario(eventId)",
                "scenarioRepository.getScenariosByEventIdAndStatus(eventId, ScenarioStatus.SELECTED)"
            ),
            "Transport selectedDestination must be loaded from ScenarioRepository selected scenario state, not only ScenarioManagementViewModel.state."
        )
        assertFalse(
            Regex("""val\s+selectedScenario\s*=[\s\S]{0,360}scenarioState\.selectedScenario""")
                .containsMatchIn(transportBlock) &&
                !transportBlock.contains("scenarioRepository.getSelectedScenario") &&
                !transportBlock.contains("scenarioRepository.getScenariosByEventIdAndStatus"),
            "Transport selectedScenario must not be derived only from transient ScenarioManagementViewModel.state; that state is empty on direct access or process restart."
        )
        assertTrue(
            transportBlock.contains("LaunchedEffect(eventId") &&
                containsAny(transportBlock, "loadSelectedScenario", "selectedScenarioFromRepository", "persistedSelectedScenario"),
            "Transport screen setup must explicitly reload the persisted selected scenario before transport readiness/actions are evaluated."
        )
    }

    @Test
    fun navHostReloadsTransportEventAndParticipantAccessFromPersistedRepositoryOnDirectEntry() {
        val source = projectFile(
            "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/navigation/WakeveNavHost.kt"
        ).readText()
        val transportBlock = source.substringAfter("route = Screen.TransportPlanning.route")
            .substringBefore("// ========================================\n        // COMMUNICATION")

        assertTrue(
            Regex("""val\s+eventRepository\s*:\s*EventRepository\s*=\s*koinInject\(""")
                .containsMatchIn(transportBlock),
            "Transport direct-route setup must inject EventRepository so selectedEvent can be rebuilt after process restart."
        )
        assertTrue(
            containsAny(
                transportBlock,
                "eventRepository.getEvent(eventId)",
                "eventRepository.getEvent(eventId = eventId)"
            ),
            "Transport route must load the persisted event by eventId instead of relying only on EventManagementViewModel.state.selectedEvent."
        )
        assertTrue(
            containsAny(
                transportBlock,
                "eventRepository.getParticipantRecords(eventId)",
                "eventRepository.getParticipants(eventId)",
                "participantQueries.selectByEventId(eventId)"
            ),
            "Transport route must reload persisted participants/access state for direct entry when EventManagementViewModel state is empty."
        )
        assertFalse(
            transportBlock.contains("val selectedEvent = eventState.selectedEvent") &&
                !transportBlock.contains("persistedEvent") &&
                !transportBlock.contains("eventFromRepository"),
            "Transport selectedEvent must fall back to persisted repository state, not only transient EventManagementViewModel.state."
        )
        assertFalse(
            Regex("""val\s+eventStatus\s*=\s*selectedEvent\?\.status\s*\?:\s*EventStatus\.DRAFT""")
                .containsMatchIn(transportBlock) &&
                !transportBlock.contains("persistedEvent") &&
                !transportBlock.contains("eventFromRepository"),
            "Transport eventStatus must not degrade to DRAFT on direct route when a persisted confirmed/organizing event exists."
        )
        assertFalse(
            Regex("""val\s+isOrganizer\s*=\s*userId\s*==\s*selectedEvent\?\.organizerId""")
                .containsMatchIn(transportBlock) &&
                !transportBlock.contains("persistedEvent") &&
                !transportBlock.contains("eventFromRepository"),
            "Transport organizer permissions must be derived from persisted event state when selectedEvent is absent."
        )
        assertFalse(
            transportBlock.contains("eventState.participantAccessStates") &&
                !transportBlock.contains("participantRecords") &&
                !transportBlock.contains("persistedParticipants"),
            "Transport participant confirmation must use persisted participant records on direct route, not only transient access state."
        )
        assertTrue(
            transportBlock.contains("LaunchedEffect(eventId") &&
                containsAny(transportBlock, "loadTransportEvent", "loadEventAccess", "persistedEvent", "eventFromRepository"),
            "Transport screen setup must explicitly reload persisted event and participant access before evaluating transport permissions."
        )
    }

    @Test
    fun navHostDoesNotDowngradeDirectTransportEntryToDraftBeforeRepositoryEventLoads() {
        val source = projectFile(
            "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/navigation/WakeveNavHost.kt"
        ).readText()
        val transportBlock = source.substringAfter("route = Screen.TransportPlanning.route")
            .substringBefore("// ========================================\n        // COMMUNICATION")

        assertFalse(
            Regex("""val\s+eventStatus\s*=\s*selectedEvent\?\.status\s*\?:\s*EventStatus\.DRAFT""")
                .containsMatchIn(transportBlock),
            "Direct transport route entry must not temporarily downgrade a persisted confirmed/organizing event to DRAFT while repository state is loading."
        )
        assertTrue(
            containsAny(
                transportBlock,
                "val eventStatus = selectedEvent?.status",
                "val eventStatus: EventStatus? = selectedEvent?.status",
                "eventStatus = selectedEvent?.status"
            ),
            "Transport route should model missing selectedEvent as unresolved repository-backed state, not as a DRAFT event."
        )
    }

    @Test
    fun navHostUsesTransientSelectedEventOnlyWhenItMatchesTransportRouteEventId() {
        val source = projectFile(
            "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/navigation/WakeveNavHost.kt"
        ).readText()
        val transportBlock = source.substringAfter("route = Screen.TransportPlanning.route")
            .substringBefore("// ========================================\n        // COMMUNICATION")

        assertFalse(
            Regex("""val\s+selectedEvent\s*=\s*eventState\.selectedEvent\s*\?:\s*eventFromRepository""")
                .containsMatchIn(transportBlock),
            "Transport route must not prefer EventManagementViewModel.selectedEvent unless that event belongs to the route eventId."
        )
        assertTrue(
            Regex("""eventState\.selectedEvent\?\.takeIf\s*\{[\s\S]{0,160}\.id\s*==\s*eventId""")
                .containsMatchIn(transportBlock) ||
                containsAny(
                    transportBlock,
                    "eventState.selectedEvent?.takeIf { it.id == eventId }",
                    "eventState.selectedEvent.takeIf { it.id == eventId }"
                ),
            "Transient selectedEvent must be route-scoped with selectedEvent.id == eventId before falling back to the persisted event."
        )
        assertTrue(
            containsAny(
                transportBlock,
                "?: eventFromRepository",
                "?: persistedEvent",
                "?: eventRepository.getEvent(eventId)"
            ),
            "When transient selectedEvent belongs to another event, transport must fall back to the repository-backed event for the route."
        )
    }

    @Test
    fun navHostUsesTransientParticipantAccessOnlyForRouteScopedInMemoryEvent() {
        val source = projectFile(
            "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/navigation/WakeveNavHost.kt"
        ).readText()
        val transportBlock = source.substringAfter("route = Screen.TransportPlanning.route")
            .substringBefore("// ========================================\n        // COMMUNICATION")
        val participantAccessBlock = transportBlock.substringAfter("val isParticipantConfirmed")
            .substringBefore("var persistedSelectedScenario")

        assertFalse(
            Regex("""val\s+isParticipantConfirmed\s*=\s*eventState\.participantAccessStates[\s\S]{0,320}\|\|[\s\S]{0,220}participantRecords""")
                .containsMatchIn(participantAccessBlock),
            "Transport participant confirmation must not OR stale EventManagementViewModel access state with route participant records."
        )
        assertTrue(
            containsAny(
                participantAccessBlock,
                "eventState.selectedEvent?.id == eventId",
                "transientEventMatchesRoute",
                "routeScopedSelectedEvent",
                "selectedEventFromState"
            ),
            "Transient participantAccessStates may be used only behind the same route-scoped selectedEvent.id == eventId guard."
        )
        assertTrue(
            containsAny(
                participantAccessBlock,
                "participantRecords.firstOrNull",
                "persistedParticipantRecords.firstOrNull"
            ),
            "If in-memory event state is absent or stale, persisted participant records must drive transport confirmation."
        )
    }

    @Test
    fun navHostFiltersTransientScenariosByTransportRouteEventIdBeforeSelectedDestination() {
        val source = projectFile(
            "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/navigation/WakeveNavHost.kt"
        ).readText()
        val transportBlock = source.substringAfter("route = Screen.TransportPlanning.route")
            .substringBefore("// ========================================\n        // COMMUNICATION")
        val selectedScenarioBlock = transportBlock.substringAfter("val selectedScenario =")
            .substringBefore("val selectedDestination")

        assertFalse(
            Regex("""scenarioState\.scenarios\s*\.\s*firstOrNull\s*\{\s*it\.scenario\.status\s*==\s*ScenarioStatus\.SELECTED""")
                .containsMatchIn(selectedScenarioBlock),
            "Transient scenarioState.scenarios must not provide a selected destination without first matching scenario.eventId to the route eventId."
        )
        assertTrue(
            Regex("""scenarioState\.scenarios[\s\S]{0,260}it\.scenario\.eventId\s*==\s*eventId""")
                .containsMatchIn(selectedScenarioBlock),
            "Transient scenario lists must be filtered with scenario.eventId == eventId before selected destination is derived."
        )
        assertTrue(
            Regex("""scenarioState\.selectedScenario\?\.takeIf\s*\{[\s\S]{0,220}it\.eventId\s*==\s*eventId""")
                .containsMatchIn(selectedScenarioBlock),
            "Transient selectedScenario must be ignored unless selectedScenario.eventId == eventId."
        )
        assertTrue(
            selectedScenarioBlock.contains("selectedScenarioFromRepository?.takeIf { it.status == ScenarioStatus.SELECTED }"),
            "When transient scenarios belong to another event, repository selected scenario must remain the fallback source."
        )
    }

    @Test
    fun navHostPassesWorkflowReadOnlyAndSelectedDestinationContractsToTransportScreen() {
        val source = projectFile(
            "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/navigation/WakeveNavHost.kt"
        ).readText()
        val transportScreenCall = source.substringAfter("TransportPlanningScreen(")
            .substringBefore("\n            )")

        assertTrue(
            transportScreenCall.contains("eventStatus =") ||
                transportScreenCall.contains("workflowStatus ="),
            "WakeveNavHost must pass the event workflow status into TransportPlanningScreen."
        )
        assertTrue(
            transportScreenCall.contains("isReadOnly =") ||
                transportScreenCall.contains("readOnly ="),
            "WakeveNavHost must pass read-only mode so FINALIZED transport screens cannot mutate local state."
        )
        assertTrue(
            transportScreenCall.contains("selectedDestination =") ||
                Regex("""\bdestination\s*=\s*[^,\n]+""").containsMatchIn(transportScreenCall),
            "WakeveNavHost must pass the nullable selected destination object, not only a display label."
        )
    }

    @Test
    fun eventDetailOffersTransportOnlyFromConfirmedOrganizationFlow() {
        val eventDetailSource = projectFile(
            "composeApp/src/commonMain/kotlin/com/guyghost/wakeve/EventDetailScreen.kt"
        ).readText()
        val navHostSource = projectFile(
            "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/navigation/WakeveNavHost.kt"
        ).readText()

        assertTrue(
            eventDetailSource.contains("Transport") ||
                navHostSource.contains("Screen.TransportPlanning.createRoute(eventId)"),
            "Event detail must expose a transport entry point once date and destination are confirmed."
        )
        assertTrue(
            eventDetailSource.contains("EventStatus.CONFIRMED") &&
                eventDetailSource.contains("EventStatus.ORGANIZING"),
            "The event detail transport entry point must be gated to confirmed/organizing workflow states."
        )
        assertFalse(
            Regex("""EventStatus\.(DRAFT|POLLING)[\s\S]{0,180}Transport""").containsMatchIn(eventDetailSource),
            "Transport planning must not be exposed from draft or polling states."
        )
    }

    @Test
    fun scenarioOrganizingSurfacesCanNavigateToTransportAfterScenarioSelection() {
        val scenarioSources = listOf(
            "composeApp/src/commonMain/kotlin/com/guyghost/wakeve/ui/scenario/ScenarioManagementScreen.kt",
            "composeApp/src/commonMain/kotlin/com/guyghost/wakeve/ui/scenario/ScenarioDetailScreen.kt",
            "composeApp/src/commonMain/kotlin/com/guyghost/wakeve/ui/scenario/ScenarioComparisonScreen.kt",
            "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/navigation/WakeveNavHost.kt"
        ).joinToString("\n") { projectFile(it).readText() }

        assertTrue(
            scenarioSources.contains("Screen.TransportPlanning.createRoute(eventId)") ||
                scenarioSources.contains("onNavigateToTransport"),
            "Scenario organizing surfaces must provide a transport planning path after a destination scenario is selected."
        )
        assertTrue(
            scenarioSources.contains("EventStatus.ORGANIZING") &&
                scenarioSources.contains("Transport"),
            "Scenario transport navigation must be gated to the organizing workflow, not the comparison vote step."
        )
    }

    @Test
    fun navHostPendingTransportSyncExcludesAuditOnlyConflictResolutionRows() {
        val source = projectFile(
            "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/navigation/WakeveNavHost.kt"
        ).readText()
        val transportBlock = source.substringAfter("route = Screen.TransportPlanning.route")
            .substringBefore("// ========================================\n        // COMMUNICATION")
        val pendingSyncBlock = transportBlock.substringAfter("pendingTransportSync =")
            .substringBefore("}\n            }")

        assertFalse(
            pendingSyncBlock.contains("selectPending()") &&
                !pendingSyncBlock.contains("CONFLICT_RESOLVED") &&
                !pendingSyncBlock.contains("hasPendingTransportSync(eventId"),
            "Android transport pending sync must not count raw selectPending() rows without excluding audit-only CONFLICT_RESOLVED metadata or delegating to TransportRepository.hasPendingTransportSync."
        )
        assertTrue(
            containsAny(
                pendingSyncBlock,
                "operation != \"CONFLICT_RESOLVED\"",
                "operation == \"CREATE\"",
                "operation == \"UPDATE\"",
                "operation == \"DELETE\"",
                "hasPendingTransportSync(eventId"
            ),
            "Android transport pending sync displayed in WakeveNavHost must be derived from replayable operations only."
        )
    }

    private fun projectFile(relativePath: String): File {
        val root = generateSequence(File(System.getProperty("user.dir")).absoluteFile) { it.parentFile }
            .first { File(it, relativePath).exists() }
        return File(root, relativePath)
    }

    private fun containsAny(source: String, vararg candidates: String): Boolean {
        return candidates.any { source.contains(it) }
    }
}
