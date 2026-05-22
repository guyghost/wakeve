package com.guyghost.wakeve.ui.transport

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TransportUiPolicyContractTest {
    @Test
    fun composeTransportPlanningScreenExists() {
        assertTrue(
            projectFileOrNull("composeApp/src/commonMain/kotlin/com/guyghost/wakeve/ui/transport/TransportPlanningScreen.kt")
                ?.exists() == true,
            "Compose must provide TransportPlanningScreen for event transport organization."
        )
    }

    @Test
    fun composeTransportScreenExposesCoreTransportStates() {
        val source = sourceOrEmpty(
            "composeApp/src/commonMain/kotlin/com/guyghost/wakeve/ui/transport/TransportPlanningScreen.kt"
        )

        listOf(
            "TransportReadiness",
            "missingDeparture",
            "OptimizationType",
            "COST_MINIMIZE",
            "TIME_MINIMIZE",
            "BALANCED",
            "generate",
            "selectFinal",
            "pending"
        ).forEach { requiredToken ->
            assertTrue(
                source.contains(requiredToken, ignoreCase = true),
                "Transport screen must expose UI state/action for $requiredToken."
            )
        }
    }

    @Test
    fun composeTransportAccessPathsAreExplicitAndUnknownParticipantsStayLocked() {
        val source = sourceOrEmpty(
            "composeApp/src/commonMain/kotlin/com/guyghost/wakeve/ui/transport/TransportPlanningScreen.kt"
        )

        assertTrue(
            source.contains("isOrganizer") && source.contains("isParticipantConfirmed == true"),
            "Transport details must be accessible only to organizers or explicitly confirmed participants."
        )
        assertFalse(
            source.contains("isParticipantConfirmed != false") ||
                source.contains("isParticipantConfirmed == null"),
            "Unknown participant confirmation must not unlock transport details."
        )
    }

    @Test
    fun composeTransportReferencesConfirmedDateAndDestinationNotPollDeadline() {
        val source = sourceOrEmpty(
            "composeApp/src/commonMain/kotlin/com/guyghost/wakeve/ui/transport/TransportPlanningScreen.kt"
        )

        assertTrue(
            source.contains("finalDate", ignoreCase = true) ||
                source.contains("confirmedDate", ignoreCase = true),
            "Transport UI must use the confirmed event date as the travel anchor."
        )
        assertTrue(
            source.contains("destination", ignoreCase = true),
            "Transport UI must anchor plans to the confirmed destination."
        )
        assertFalse(
            source.contains("deadline", ignoreCase = true),
            "Transport UI must not use the poll deadline as the travel date."
        )
    }

    @Test
    fun composeTransportScreenExposesTransportNotNeededDecision() {
        val source = sourceOrEmpty(
            "composeApp/src/commonMain/kotlin/com/guyghost/wakeve/ui/transport/TransportPlanningScreen.kt"
        )
        val navHostSource = sourceOrEmpty(
            "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/navigation/WakeveNavHost.kt"
        )

        assertTrue(
            source.contains("onMarkTransportNotNeeded") &&
                source.contains("transportNotNeeded"),
            "Transport UI must expose an explicit mark-not-needed action and state, not only display missing departures."
        )
        assertTrue(
            navHostSource.contains("onMarkTransportNotNeeded"),
            "Android navigation must wire the transport-not-needed action to repository/state."
        )
    }

    @Test
    fun composeTransportPlanningScreenRequiresWorkflowReadOnlyAndSelectedDestinationContracts() {
        val source = sourceOrEmpty(
            "composeApp/src/commonMain/kotlin/com/guyghost/wakeve/ui/transport/TransportPlanningScreen.kt"
        )
        val parameters = transportPlanningScreenParameters(source)

        assertTrue(
            Regex("""\b(isReadOnly|readOnly)\s*:\s*Boolean\b""").containsMatchIn(parameters),
            "TransportPlanningScreen must receive an explicit read-only mode so FINALIZED events cannot mutate transport state."
        )
        assertTrue(
            Regex("""\b(eventStatus|workflowStatus)\s*:\s*EventStatus\b""").containsMatchIn(parameters),
            "TransportPlanningScreen must receive workflow status instead of deriving mutability from organizer/readiness alone."
        )
        assertTrue(
            Regex("""\b(selectedDestination|destination)\s*:\s*TransportLocation\?""").containsMatchIn(parameters),
            "TransportPlanningScreen must receive the selected scenario destination as a nullable TransportLocation contract."
        )
        assertFalse(
            Regex("""\bdestinationLabel\s*:\s*String\b""").containsMatchIn(parameters),
            "A non-null destination label is not enough: transport generation must be impossible without a real selected destination."
        )
    }

    @Test
    fun composeTransportMutatingButtonsAreDisabledWhenFinalizedOrDestinationMissing() {
        val source = sourceOrEmpty(
            "composeApp/src/commonMain/kotlin/com/guyghost/wakeve/ui/transport/TransportPlanningScreen.kt"
        )
        val canGenerate = assignedExpression(source, "canGenerate")
        val canSelectFinal = assignedExpression(source, "canSelectFinal")
        val canMarkNotNeeded = assignedExpression(source, "canMarkTransportNotNeeded")
        val canSaveDeparture = assignedExpression(source, "canSaveDeparture")

        listOf(canGenerate, canSelectFinal, canMarkNotNeeded, canSaveDeparture).forEach { expression ->
            assertTrue(
                expression.contains("!isReadOnly") ||
                    expression.contains("!readOnly") ||
                    expression.contains("isReadOnly == false") ||
                    expression.contains("readOnly == false"),
                "Mutating transport actions must be disabled in read-only mode; expression was: $expression"
            )
            assertTrue(
                expression.contains("selectedDestination != null") ||
                    expression.contains("destination != null"),
                "Mutating transport actions must be disabled until a selected destination exists; expression was: $expression"
            )
        }

        assertTrue(
            canGenerate.contains("EventStatus.ORGANIZING") ||
                canGenerate.contains("EventStatus.CONFIRMED"),
            "Generate must be gated to the transport workflow state, not only readiness."
        )
        assertTrue(
            canSelectFinal.contains("EventStatus.ORGANIZING") ||
                canSelectFinal.contains("EventStatus.CONFIRMED"),
            "Select-final must be gated to the transport workflow state."
        )
        assertEquals(
            false,
            Regex("""Button\([\s\S]{0,120}onSaveDepartureLocation[\s\S]{0,180}enabled\s*=\s*value\.isNotBlank\(\)""")
                .containsMatchIn(source),
            "Save departure must use the same workflow/read-only/destination policy as the other transport mutations."
        )
    }

    @Test
    fun iosTransportPlanningViewExistsAndExposesEquivalentStates() {
        val source = sourceOrEmpty("iosApp/src/Views/Events/TransportPlanningView.swift")

        assertTrue(source.isNotBlank(), "iOS must provide TransportPlanningView.swift.")
        listOf(
            "missingDeparture",
            "OptimizationType",
            "COST_MINIMIZE",
            "TIME_MINIMIZE",
            "BALANCED",
            "generate",
            "selected",
            "pending"
        ).forEach { requiredToken ->
            assertTrue(
                source.contains(requiredToken, ignoreCase = true),
                "iOS transport view must expose UI state/action for $requiredToken."
            )
        }
        assertTrue(
            source.contains("finalDate", ignoreCase = true) ||
                source.contains("confirmedDate", ignoreCase = true),
            "iOS transport view must use the confirmed event date."
        )
        assertFalse(
            source.contains("deadline", ignoreCase = true),
            "iOS transport view must not use the poll deadline as the travel date."
        )
    }

    @Test
    fun iosNavigationWiresTransportFromEventAndScenarioOrganizationFlow() {
        val contentViewSource = projectFile("iosApp/src/Views/App/ContentView.swift").readText()
        val scenarioSource = projectFile("iosApp/src/Views/Events/ScenarioOrganizationView.swift").readText()

        assertTrue(
            contentViewSource.contains("transportPlanning") &&
                contentViewSource.contains("TransportPlanningView("),
            "iOS AppView must include transportPlanning and render TransportPlanningView."
        )
        assertTrue(
            contentViewSource.contains("case .confirmed") &&
                contentViewSource.contains("case .organizing") &&
                contentViewSource.contains("Transport"),
            "iOS event detail must expose transport only from confirmed/organizing flows."
        )
        assertTrue(
            scenarioSource.contains("onOpenTransport") ||
                contentViewSource.contains("currentView = .transportPlanning"),
            "iOS scenario organizing flow must wire a transport planning action after destination selection."
        )
    }

    @Test
    fun iosTransportAccessUsesConfirmedParticipantStateForNonOrganizer() {
        val contentViewSource = projectFile("iosApp/src/Views/App/ContentView.swift").readText()

        assertFalse(
            contentViewSource.contains("isParticipantConfirmed: event.organizerId == userId ? true : nil"),
            "iOS transport access must not lock every non-organizer behind nil participant confirmation."
        )
        assertTrue(
            contentViewSource.contains("ParticipantManagementPresentationMapper") ||
                contentViewSource.contains("canAccessOrganizationDetails") ||
                contentViewSource.contains("DateValidationState") ||
                contentViewSource.contains("participantAccess"),
            "iOS transport navigation must derive non-organizer access from confirmed participant state."
        )
    }

    @Test
    fun iosTransportScreenDoesNotUseHardcodedLocalPlanningState() {
        val source = sourceOrEmpty("iosApp/src/Views/Events/TransportPlanningView.swift")

        assertFalse(
            source.contains("@State private var missingDeparture = [\"Participant 1\", \"Participant 2\"]"),
            "iOS transport missing departures must come from shared/repository state, not hardcoded local sample names."
        )
        assertFalse(
            source.contains("generatedPlan = \"\\(selectedOptimization.rawValue)-plan\""),
            "iOS transport plan generation must call shared/repository state, not synthesize local plan IDs."
        )
        assertTrue(
            source.contains("TransportReadiness") ||
                source.contains("TransportRepository") ||
                source.contains("TransportPlanningViewModel"),
            "iOS transport screen must consume real transport readiness/plans from shared or view-model state."
        )
    }

    private fun sourceOrEmpty(relativePath: String): String {
        return projectFileOrNull(relativePath)?.readText().orEmpty()
    }

    private fun projectFile(relativePath: String): File {
        return projectFileOrNull(relativePath)
            ?: error("Missing required project file: $relativePath")
    }

    private fun projectFileOrNull(relativePath: String): File? {
        val root = generateSequence(File(System.getProperty("user.dir")).absoluteFile) { it.parentFile }
            .firstOrNull { File(it, relativePath).exists() }
        return root?.let { File(it, relativePath) }
    }

    private fun transportPlanningScreenParameters(source: String): String {
        val start = source.indexOf("fun TransportPlanningScreen(")
        require(start >= 0) { "TransportPlanningScreen function is missing" }
        val openParen = source.indexOf('(', start)
        var depth = 0
        for (index in openParen until source.length) {
            when (source[index]) {
                '(' -> depth += 1
                ')' -> {
                    depth -= 1
                    if (depth == 0) {
                        return source.substring(openParen + 1, index)
                    }
                }
            }
        }
        error("Could not parse TransportPlanningScreen parameters")
    }

    private fun assignedExpression(source: String, name: String): String {
        val match = Regex("""val\s+$name\s*=\s*([^\n]+)""").find(source)
            ?: error("Missing policy expression val $name")
        return match.groupValues[1].trim()
    }
}
