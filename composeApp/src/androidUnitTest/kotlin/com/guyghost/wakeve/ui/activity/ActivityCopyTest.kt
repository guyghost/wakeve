package com.guyghost.wakeve.ui.activity

import com.guyghost.wakeve.models.ActivitiesByDate
import com.guyghost.wakeve.models.Activity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ActivityCopyTest {

    @Test
    fun activityPlanningCopyUsesSpecificFrenchLabels() {
        assertEquals("Planification des activités", activityPlanningTitle())
        assertEquals("Retour", activityBackContentDescription())
        assertEquals("Aucun commentaire activité", activityCommentContentDescription(0))
        assertEquals("1 commentaire activité", activityCommentContentDescription(1))
        assertEquals("4 commentaires activité", activityCommentContentDescription(4))
        assertEquals("Ajouter une activité", activityAddContentDescription())
        assertEquals("Aucune activité planifiée", activityEmptyPlanningMessage())
        assertEquals("Aucune activité à cette date", activityEmptyDateMessage())
        assertEquals("Toutes", activityAllDatesLabel())
    }

    @Test
    fun activitySummaryAndRowCopyAreReadable() {
        assertEquals("Activité", activitySummaryCountLabel(1))
        assertEquals("Activités", activitySummaryCountLabel(2))
        assertEquals("Coût total", activityTotalCostLabel())
        assertEquals("14:30 (90 min)", activityTimeDurationLabel("14:30", 90))
        assertEquals("Heure à définir (60 min)", activityTimeDurationLabel(null, 60))
        assertEquals("25 € / personne", activityCostPerPersonLabel(2500))
        assertEquals("1 inscrit", activityRegistrationLabel(1, null))
        assertEquals("3 inscrits", activityRegistrationLabel(3, null))
        assertEquals("3 / 8 inscrits", activityRegistrationLabel(3, 8))
        assertEquals("Complet", activityFullLabel())
    }

    @Test
    fun activityProgramSummaryHighlightsNextScheduledStep() {
        val summary = activityProgramSummary(
            listOf(
                activitiesByDate(
                    date = "2026-07-12",
                    activities = listOf(
                        activity(id = "late", name = "Dîner", date = "2026-07-12", time = "20:00"),
                        activity(id = "early", name = "Brunch", date = "2026-07-12", time = "10:30")
                    )
                )
            )
        )

        assertEquals("Programme à suivre", summary.title)
        assertEquals("Prochaine étape : 12 Juil à 10:30 - Brunch", summary.nextStepLabel)
        assertEquals("2 activités sur 1 jour.", summary.coverageLabel)
        assertEquals("Programme horaire prêt à partager.", summary.readinessLabel)
    }

    @Test
    fun activityProgramSummaryFlagsActivitiesWithoutTime() {
        val summary = activityProgramSummary(
            listOf(
                activitiesByDate(
                    date = "2026-07-12",
                    activities = listOf(
                        activity(id = "planned", name = "Randonnée", date = "2026-07-12", time = "09:00"),
                        activity(id = "missing-time", name = "Apéro", date = "2026-07-12", time = null)
                    )
                ),
                activitiesByDate(
                    date = "2026-07-13",
                    activities = listOf(
                        activity(id = "second-day", name = "Marché", date = "2026-07-13", time = null)
                    )
                )
            )
        )

        assertEquals("Prochaine étape : 12 Juil à 09:00 - Randonnée", summary.nextStepLabel)
        assertEquals("3 activités sur 2 jours.", summary.coverageLabel)
        assertEquals("2 activités restent sans heure.", summary.readinessLabel)
    }

    @Test
    fun activityProgramSummaryExplainsEmptyProgram() {
        val summary = activityProgramSummary(emptyList())

        assertEquals("Programme à suivre", summary.title)
        assertEquals(
            "Aucune prochaine étape : ajoutez une activité pour guider le groupe.",
            summary.nextStepLabel
        )
        assertEquals("Le programme n'a pas encore de journée planifiée.", summary.coverageLabel)
        assertEquals("Priorité : définir au moins une activité datée.", summary.readinessLabel)
    }

    @Test
    fun activityDialogCopyAvoidsTechnicalDateFormatsInLabels() {
        assertEquals("Ajouter une activité", activityDialogTitle(isNewActivity = true))
        assertEquals("Modifier l'activité", activityDialogTitle(isNewActivity = false))
        assertEquals("Nom requis", activityNameLabel())
        assertEquals("Description requise", activityDescriptionLabel())
        assertEquals("Date requise", activityDateLabel())
        assertEquals("AAAA-MM-JJ", activityDatePlaceholder())
        assertEquals("Heure", activityTimeLabel())
        assertEquals("HH:MM", activityTimePlaceholder())
        assertEquals("Durée requise (min)", activityDurationLabel())
        assertEquals("Places maximum", activityCapacityLabel())
        assertEquals("Illimité", activityUnlimitedCapacityPlaceholder())
        assertEquals("Coût par personne (€)", activityCostFieldLabel())

        assertFalse(activityDateLabel().contains("YYYY", ignoreCase = true))
        assertFalse(activityTimeLabel().contains("HH", ignoreCase = true))
    }

    @Test
    fun activityActionsAndDeletionCopyAreExplicit() {
        assertEquals("Supprimer l'activité ?", activityDeleteTitle())
        assertEquals("Supprimer « Brunch » du programme ?", activityDeleteMessage("Brunch"))
        assertEquals("Supprimer", activityDeleteActionLabel())
        assertEquals("Annuler", activityCancelActionLabel())
        assertEquals("Modifier", activityEditActionLabel())
        assertEquals("Participants", activityParticipantsActionLabel())
        assertEquals("Ajouter", activityDialogConfirmLabel(isNewActivity = true))
        assertEquals("Modifier", activityDialogConfirmLabel(isNewActivity = false))
        assertEquals("Participants - Brunch", activityParticipantsDialogTitle("Brunch"))
        assertEquals("Fermer", activityCloseActionLabel())
        assertEquals("Activité complète", activityFullDialogMessage())
        assertTrue(activityDeleteMessage("Brunch").contains("programme", ignoreCase = true))
    }

    private fun activitiesByDate(date: String, activities: List<Activity>): ActivitiesByDate {
        return ActivitiesByDate(
            date = date,
            activities = activities,
            totalActivities = activities.size,
            totalCost = activities.mapNotNull { it.cost }.sum()
        )
    }

    private fun activity(
        id: String,
        name: String,
        date: String,
        time: String?
    ): Activity {
        return Activity(
            id = id,
            eventId = "event-1",
            name = name,
            description = "Description",
            date = date,
            time = time,
            duration = 60,
            registeredParticipantIds = emptyList(),
            organizerId = "organizer",
            createdAt = "2026-06-20T10:00:00Z",
            updatedAt = "2026-06-20T10:00:00Z"
        )
    }
}
