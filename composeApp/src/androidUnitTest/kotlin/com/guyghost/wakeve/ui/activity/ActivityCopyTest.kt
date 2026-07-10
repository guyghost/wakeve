package com.guyghost.wakeve.ui.activity

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ActivityCopyTest {
    @Test
    fun activitySurfacesUseAndroidResourcesInsteadOfFrenchCopyHelpers() {
        val dialogs = projectFile("composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/activity/ActivityDialogs.kt")
        val planning = projectFile("composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/activity/ActivityPlanningScreen.kt")

        assertTrue(dialogs.contains("R.string.activity_add_title"))
        assertTrue(planning.contains("R.string.activity_planning_title"))
        assertTrue(planning.contains("R.plurals.activity_registration_count"))
        assertFalse(dialogs.contains("internal fun activityNameLabel"))
        assertFalse(planning.contains("internal fun activityPlanningTitle"))
    }

    @Test
    fun activityActionsExposeTargetSpecificAccessibilityResources() {
        val planning = projectFile("composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/activity/ActivityPlanningScreen.kt")

        assertTrue(planning.contains("R.string.a11y_activity_edit, activity.activity.name"))
        assertTrue(planning.contains("R.string.a11y_activity_manage_participants, activity.activity.name"))
        assertTrue(planning.contains("R.string.a11y_activity_delete, activity.activity.name"))
    }

    private fun projectFile(path: String): String = root.resolve(path).readText()

    private companion object {
        val root: File by lazy {
            var file = File(requireNotNull(System.getProperty("user.dir"))).absoluteFile
            while (!File(file, "settings.gradle.kts").isFile) file = requireNotNull(file.parentFile)
            file
        }
    }
}
