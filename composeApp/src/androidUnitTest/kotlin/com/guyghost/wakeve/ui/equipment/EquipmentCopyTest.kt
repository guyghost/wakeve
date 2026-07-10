package com.guyghost.wakeve.ui.equipment

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EquipmentCopyTest {
    @Test
    fun equipmentSurfacesUseAndroidResourcesInsteadOfFrenchCopyHelpers() {
        val checklist = projectFile("composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/equipment/EquipmentChecklistScreen.kt")
        val dialogs = projectFile("composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/equipment/EquipmentDialogs.kt")

        assertTrue(checklist.contains("R.string.equipment_screen_title"))
        assertTrue(checklist.contains("R.string.equipment_progress_ratio"))
        assertTrue(dialogs.contains("R.string.equipment_add_title"))
        assertTrue(dialogs.contains("R.string.equipment_event_type_beach"))
        assertFalse(checklist.contains("internal fun equipmentScreenTitle"))
        assertFalse(dialogs.contains("internal fun equipmentItemDialogTitle"))
    }

    @Test
    fun equipmentActionsExposeTargetAndStateSpecificAccessibilityResources() {
        val checklist = projectFile("composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/equipment/EquipmentChecklistScreen.kt")

        assertTrue(checklist.contains("R.string.a11y_equipment_assign, item.name"))
        assertTrue(checklist.contains("R.string.a11y_equipment_edit, item.name"))
        assertTrue(checklist.contains("R.string.a11y_equipment_delete, item.name"))
        assertTrue(checklist.contains("R.string.a11y_equipment_set_packed, item.name, packedState"))
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
