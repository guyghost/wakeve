package com.guyghost.wakeve.ui.scenario

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse

class ScenarioAccessUiPolicyContractTest {
    @Test
    fun managementScreenDoesNotTreatUnknownConfirmationAsUnlocked() {
        val source = projectFile(
            "composeApp/src/commonMain/kotlin/com/guyghost/wakeve/ui/scenario/ScenarioManagementScreen.kt"
        ).readText()

        assertFalse(
            source.contains("isParticipantConfirmed == null && !accessLockedByError"),
            "Unknown participant confirmation must lock scenario details unless the user is organizer."
        )
    }

    @Test
    fun comparisonScreenDoesNotTreatUnknownConfirmationAsUnlocked() {
        val source = projectFile(
            "composeApp/src/commonMain/kotlin/com/guyghost/wakeve/ui/scenario/ScenarioComparisonScreen.kt"
        ).readText()

        assertFalse(
            source.contains("isParticipantConfirmed != false"),
            "Unknown participant confirmation must not grant comparison/detail access."
        )
    }

    @Test
    fun detailScreenDoesNotTreatUnknownConfirmationAsUnlocked() {
        val source = projectFile(
            "composeApp/src/commonMain/kotlin/com/guyghost/wakeve/ui/scenario/ScenarioDetailScreen.kt"
        ).readText()

        assertFalse(
            source.contains("isParticipantConfirmed != false"),
            "Unknown participant confirmation must show the locked detail screen unless the user is organizer."
        )
    }

    @Test
    fun comparisonScreenDoesNotTreatUnknownEventStatusAsFinalSelectionAllowed() {
        val source = projectFile(
            "composeApp/src/commonMain/kotlin/com/guyghost/wakeve/ui/scenario/ScenarioComparisonScreen.kt"
        ).readText()

        assertFalse(
            source.contains("eventStatus == null || eventStatus == EventStatus.COMPARING"),
            "Unknown event status must not allow final scenario selection."
        )
    }

    private fun projectFile(relativePath: String): File {
        val root = generateSequence(File(System.getProperty("user.dir")).absoluteFile) { it.parentFile }
            .first { File(it, relativePath).exists() }
        return File(root, relativePath)
    }
}
