package com.guyghost.wakeve.ui.meal

import android.content.Intent
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.guyghost.wakeve.R
import com.guyghost.wakeve.models.Meal
import com.guyghost.wakeve.models.MealStatus
import com.guyghost.wakeve.models.MealType
import com.guyghost.wakeve.ui.event.ComposeTestHostActivity
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertNotEquals

@RunWith(AndroidJUnit4::class)
class MealCardSemanticsTest {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private val scenarios = mutableListOf<ActivityScenario<ComposeTestHostActivity>>()

    @After
    fun tearDown() {
        scenarios.forEach { it.close() }
        scenarios.clear()
    }

    @Test
    fun mealCard_exposesOneLocalizedClickableActionForOpenAndDelete() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val mealName = "Dîner du samedi"
        val openLabel = context.getString(
            R.string.a11y_meal_open,
            mealName,
            context.getString(R.string.meal_status_planned)
        )
        val deleteLabel = context.getString(R.string.a11y_meal_delete, mealName)
        assertNotEquals(openLabel, deleteLabel)

        val intent = Intent().setClassName(
            "com.guyghost.wakeve",
            ComposeTestHostActivity::class.java.name
        )
        val scenario = ActivityScenario.launch<ComposeTestHostActivity>(intent)
        scenarios += scenario
        scenario.onActivity { activity ->
            activity.setContent {
                MaterialTheme {
                    MealCard(
                        meal = Meal(
                            id = "meal-1",
                            eventId = "event-1",
                            type = MealType.DINNER,
                            name = mealName,
                            date = "2026-07-11",
                            time = "19:00",
                            responsibleParticipantIds = emptyList(),
                            estimatedCost = 12_500,
                            servings = 8,
                            status = MealStatus.PLANNED,
                            createdAt = "2026-07-10T10:00:00Z",
                            updatedAt = "2026-07-10T10:00:00Z"
                        ),
                        onClick = {},
                        onDeleteClick = {}
                    )
                }
            }
        }

        composeTestRule
            .onAllNodes(hasClickAction() and hasContentDescription(openLabel), useUnmergedTree = true)
            .assertCountEquals(1)
        composeTestRule
            .onAllNodes(hasClickAction() and hasContentDescription(deleteLabel), useUnmergedTree = true)
            .assertCountEquals(1)
    }
}
