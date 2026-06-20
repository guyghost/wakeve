package com.guyghost.wakeve.ui.activity

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
}
