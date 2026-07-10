package com.guyghost.wakeve.ui.sync

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.guyghost.wakeve.sync.conflict.ConflictFieldSeverity
import com.guyghost.wakeve.sync.conflict.ConflictRecord
import org.junit.Rule
import org.junit.Test

class ConflictResolutionDialogSemanticsTest {
    @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun summaryIsSeparateAndEachChoiceIsActionableExactlyOnce() {
        composeRule.setContent {
            ConflictRowItem(
                conflict = ConflictRecord("event", "title", "Mine", "Theirs", "local-time", "remote-time", ConflictFieldSeverity.CRITICAL),
                currentDecision = null,
                onKeepLocal = {},
                onKeepRemote = {},
            )
        }
        composeRule.onNode(hasContentDescription("Conflict in Title. My version: Mine. Their version: Theirs."), useUnmergedTree = true).assertExists()
        composeRule.onAllNodes(hasClickAction(), useUnmergedTree = true).assertCountEquals(2)
        composeRule.onAllNodes(hasContentDescription("Keep Mine: Mine") and hasClickAction(), useUnmergedTree = true).assertCountEquals(1)
        composeRule.onAllNodes(hasContentDescription("Keep Theirs: Theirs") and hasClickAction(), useUnmergedTree = true).assertCountEquals(1)
    }
}
