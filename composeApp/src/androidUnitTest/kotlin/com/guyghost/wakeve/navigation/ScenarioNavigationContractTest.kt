package com.guyghost.wakeve.navigation

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScenarioNavigationContractTest {
    @Test
    fun screenDeclaresRegisteredEventScopedScenarioRoutes() {
        val source = projectFile(
            "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/navigation/Screen.kt"
        ).readText()

        assertTrue(
            source.contains("""data object ScenarioList : Screen("event/{eventId}/scenarios")"""),
            "Scenario list route must be registered as event-scoped."
        )
        assertTrue(
            source.contains("fun createRoute(eventId: String) = \"event/\${routePathSegment(eventId)}/scenarios\""),
            "Scenario list helper must create encoded event-scoped routes."
        )
        assertTrue(
            source.contains("""data object ScenarioComparison : Screen("event/{eventId}/scenarios/compare")"""),
            "Scenario comparison route must be registered as event-scoped."
        )
        assertTrue(
            source.contains("fun createRoute(eventId: String) = \"event/\${routePathSegment(eventId)}/scenarios/compare\""),
            "Scenario comparison helper must create encoded event-scoped routes."
        )
        assertTrue(
            source.contains("""data object ScenarioDetail : Screen("event/{eventId}/scenario/{scenarioId}")"""),
            "Scenario detail route must be registered as event-scoped."
        )
        assertTrue(
            source.contains("\"event/\${routePathSegment(eventId)}/scenario/\${routePathSegment(scenarioId)}\""),
            "Scenario detail helper must create encoded event-scoped routes."
        )
    }

    @Test
    fun scenarioManagementScreenDoesNotNavigateWithRawUnregisteredScenarioRoutes() {
        val source = projectFile(
            "composeApp/src/commonMain/kotlin/com/guyghost/wakeve/ui/scenario/ScenarioManagementScreen.kt"
        ).readText()

        assertFalse(
            source.contains("""onNavigate("scenario/compare")"""),
            "Compare navigation must use the registered event-scoped route event/{eventId}/scenarios/compare."
        )
        assertFalse(
            source.contains("onNavigate(\"scenario/detail/\$it\")"),
            "Detail navigation must use the registered event-scoped route event/{eventId}/scenario/{scenarioId}."
        )
    }

    @Test
    fun navHostDoesNotNavigateToMeetingsImmediatelyAfterFinalScenarioDispatch() {
        val source = projectFile(
            "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/navigation/WakeveNavHost.kt"
        ).readText()
        val immediateMeetingsNavigation = Regex(
            pattern = """onSelectWinner = \{ scenarioId ->[\s\S]*?SelectScenarioAsFinal[\s\S]*?navController\.navigate\(Screen\.MeetingList\.createRoute\(eventId\)\)""",
            options = setOf(RegexOption.MULTILINE)
        )

        assertFalse(
            immediateMeetingsNavigation.containsMatchIn(source),
            "Meetings navigation must be driven by a successful state-machine side effect, not immediately after dispatch."
        )
    }

    @Test
    fun comparisonScreenDoesNotExposeUngatedDirectMeetingsNavigation() {
        val source = projectFile(
            "composeApp/src/commonMain/kotlin/com/guyghost/wakeve/ui/scenario/ScenarioComparisonScreen.kt"
        ).readText()

        assertFalse(
            source.contains("onViewMeetings = { onNavigateToMeetings(eventId) }"),
            "Comparison winner actions must not expose direct meetings navigation before ORGANIZING or FINALIZED."
        )
        assertFalse(
            source.contains("onClick = { onNavigateToMeetings(eventId) }"),
            "Comparison bottom actions must not expose direct meetings navigation before ORGANIZING or FINALIZED."
        )
    }

    @Test
    fun detailScreenRequiresWorkflowStatusBeforeDirectMeetingsNavigation() {
        val source = projectFile(
            "composeApp/src/commonMain/kotlin/com/guyghost/wakeve/ui/scenario/ScenarioDetailScreen.kt"
        ).readText()

        assertTrue(
            source.contains("eventStatus: EventStatus?"),
            "Scenario detail must receive event workflow status before it can decide whether meetings navigation is allowed."
        )
        assertFalse(
            source.contains("onClick = onNavigateToMeetings"),
            "Scenario detail must not expose direct meetings navigation without an ORGANIZING or FINALIZED status guard."
        )
    }

    @Test
    fun navHostDoesNotProvideAlwaysDirectMeetingsCallbacksFromScenarioScreens() {
        val source = projectFile(
            "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/navigation/WakeveNavHost.kt"
        ).readText()
        val directDetailMeetingsCallback = Regex(
            pattern = """ScenarioDetailScreen\([\s\S]*?onNavigateToMeetings = \{[\s\S]*?navController\.navigate\(Screen\.MeetingList\.createRoute\(eventId\)\)[\s\S]*?\}""",
            options = setOf(RegexOption.MULTILINE)
        )
        val directComparisonMeetingsCallback = Regex(
            pattern = """ScenarioComparisonScreen\([\s\S]*?onNavigateToMeetings = \{ id ->[\s\S]*?navController\.navigate\(Screen\.MeetingList\.createRoute\(id\)\)[\s\S]*?\}""",
            options = setOf(RegexOption.MULTILINE)
        )

        assertFalse(
            directDetailMeetingsCallback.containsMatchIn(source),
            "Detail route must not provide an always-direct meetings callback for pre-ORGANIZING scenario states."
        )
        assertFalse(
            directComparisonMeetingsCallback.containsMatchIn(source),
            "Comparison route must not provide an always-direct meetings callback for pre-ORGANIZING scenario states."
        )
    }

    private fun projectFile(relativePath: String): File {
        var root = File(System.getProperty("user.dir") ?: error("Missing user.dir")).absoluteFile
        while (!File(root, relativePath).exists()) {
            root = root.parentFile ?: error("Could not find project root for $relativePath")
        }
        return File(root, relativePath)
    }
}
