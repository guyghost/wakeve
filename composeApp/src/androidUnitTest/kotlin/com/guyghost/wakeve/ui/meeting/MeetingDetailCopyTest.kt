package com.guyghost.wakeve.ui.meeting

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class MeetingDetailCopyTest {

    @Test
    fun meetingDetailCopyUsesFrenchLabels() {
        assertEquals("Détails de la réunion", meetingDetailScreenTitle())
        assertEquals("Retour", meetingDetailBackContentDescription())
        assertEquals("Enregistrer les modifications", meetingDetailSaveContentDescription())
        assertEquals("Modifier la réunion", meetingDetailEditContentDescription())
        assertEquals("Supprimer la réunion", meetingDetailDeleteContentDescription())
        assertEquals("Chargement des détails de la réunion...", meetingDetailLoadingMessage())
        assertEquals("Erreur", meetingDetailErrorTitle())
        assertEquals("Erreur inconnue", meetingDetailUnknownErrorMessage())
        assertEquals("Réessayer", meetingDetailRetryLabel())
    }

    @Test
    fun meetingDetailCopyClarifiesEditDeleteAndLinkStates() {
        assertEquals("Supprimer la réunion ?", meetingDetailDeleteDialogTitle())
        assertEquals(
            "Voulez-vous vraiment supprimer \"Point final\" ? Cette action est définitive.",
            meetingDetailDeleteDialogMessage("Point final")
        )
        assertEquals("Titre de la réunion", meetingDetailTitleFieldLabel())
        assertEquals("Description (optionnelle)", meetingDetailDescriptionFieldLabel())
        assertEquals("Plateforme", meetingDetailPlatformLabel())
        assertEquals("Date et heure", meetingDetailDateTimeLabel())
        assertEquals("Durée", meetingDetailDurationLabel())
        assertEquals("Lien de réunion", meetingDetailLinkTitle())
        assertEquals("Aucun lien généré pour le moment", meetingDetailNoLinkMessage())
        assertEquals("Générer un lien :", meetingDetailGenerateLinkLabel())
    }

    @Test
    fun meetingDetailCopyDoesNotUseOldEnglishDefaults() {
        val copy = listOf(
            meetingDetailScreenTitle(),
            meetingDetailBackContentDescription(),
            meetingDetailSaveContentDescription(),
            meetingDetailEditContentDescription(),
            meetingDetailDeleteContentDescription(),
            meetingDetailLoadingMessage(),
            meetingDetailErrorTitle(),
            meetingDetailUnknownErrorMessage(),
            meetingDetailRetryLabel(),
            meetingDetailDeleteDialogTitle(),
            meetingDetailDeleteDialogMessage("Point final"),
            meetingDetailTitleFieldLabel(),
            meetingDetailDescriptionFieldLabel(),
            meetingDetailPlatformLabel(),
            meetingDetailDateTimeLabel(),
            meetingDetailDurationLabel(),
            meetingDetailSaveChangesLabel(),
            meetingDetailDeleteMeetingLabel(),
            meetingDetailLinkTitle(),
            meetingDetailNoLinkMessage(),
            meetingDetailGenerateLinkLabel()
        )

        copy.forEach { label ->
            listOf(
                "Meeting Details",
                "Back",
                "Save",
                "Edit",
                "Delete",
                "Loading meeting details",
                "Unknown error",
                "Retry",
                "Delete Meeting",
                "Are you sure",
                "Meeting Title",
                "Description (optional)",
                "Platform",
                "Date & Time",
                "Duration",
                "Save Changes",
                "Meeting Link",
                "No link generated yet",
                "Generate link"
            ).forEach { oldCopy ->
                assertFalse(
                    label.contains(oldCopy, ignoreCase = true),
                    "Meeting detail copy should not contain `$oldCopy`: $label"
                )
            }
        }
    }
}
