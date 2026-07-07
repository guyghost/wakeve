package com.guyghost.wakeve.ui.accommodation

import com.guyghost.wakeve.models.AccommodationType
import com.guyghost.wakeve.models.BookingStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AccommodationScreenCopyTest {

    @Test
    fun accommodationScreenCopyExplainsPurposeAndActions() {
        assertEquals("Hébergement", accommodationScreenTitle())
        assertEquals("Retour", accommodationBackContentDescription())
        assertEquals("Aucun commentaire hébergement", accommodationCommentContentDescription(0))
        assertEquals("1 commentaire hébergement", accommodationCommentContentDescription(1))
        assertEquals("5 commentaires hébergement", accommodationCommentContentDescription(5))
        assertEquals("Ajouter un hébergement", accommodationAddContentDescription())
        assertEquals("Aucun hébergement", accommodationEmptyTitle())
        assertEquals("Ajouter", accommodationAddActionLabel())
        assertEquals("Modifier", accommodationEditActionLabel())
        assertEquals("Supprimer", accommodationDeleteActionLabel())
        assertEquals("Annuler", accommodationCancelActionLabel())
        assertTrue(accommodationEmptyDescription().contains("adresse", ignoreCase = true))
        assertTrue(accommodationEmptyDescription().contains("statut de réservation", ignoreCase = true))
    }

    @Test
    fun accommodationDeleteCopyMentionsRoomAssignments() {
        assertEquals("Supprimer l'hébergement ?", accommodationDeleteTitle())
        assertTrue(accommodationDeleteMessage().contains("affectations de chambres", ignoreCase = true))
        assertFalse(accommodationDeleteMessage().contains("irréversible", ignoreCase = true))
    }

    @Test
    fun accommodationDetailCopyUsesFrenchTripLabels() {
        assertEquals("Du 2026-07-10 au 2026-07-12 (2 nuits)", accommodationDateRangeLabel("2026-07-10", "2026-07-12", 2))
        assertEquals("1 nuit", accommodationNightsLabel(1))
        assertEquals("3 nuits", accommodationNightsLabel(3))
        assertEquals("Capacité : 1 personne", accommodationCapacityLabel(1))
        assertEquals("Capacité : 6 personnes", accommodationCapacityLabel(6))

        val nightPrice = accommodationNightPriceLabel(4200)

        assertTrue(nightPrice.startsWith("42"))
        assertTrue(nightPrice.endsWith("€ / nuit"))
    }

    @Test
    fun accommodationDialogCopyAvoidsEnglishCheckInOut() {
        assertEquals("Ajouter un hébergement", accommodationDialogTitle(isNewAccommodation = true))
        assertEquals("Modifier l'hébergement", accommodationDialogTitle(isNewAccommodation = false))
        assertEquals("Prix par nuit (€)", accommodationPricePerNightLabel())
        assertEquals("Arrivée", accommodationCheckInLabel())
        assertEquals("Départ", accommodationCheckOutLabel())
        assertEquals("AAAA-MM-JJ", accommodationDatePlaceholder())
        assertEquals("Lien de réservation (optionnel)", accommodationBookingUrlLabel())
        assertEquals("Enregistrer", accommodationSaveActionLabel())

        listOf(accommodationCheckInLabel(), accommodationCheckOutLabel()).forEach { label ->
            assertFalse(label.contains("check", ignoreCase = true))
        }
    }

    @Test
    fun accommodationTypeAndStatusCopyHideEnumNames() {
        assertEquals("Hôtel", getAccommodationTypeLabel(AccommodationType.HOTEL))
        assertEquals("Airbnb", getAccommodationTypeLabel(AccommodationType.AIRBNB))
        assertEquals("Camping", getAccommodationTypeLabel(AccommodationType.CAMPING))
        assertEquals("Auberge", getAccommodationTypeLabel(AccommodationType.HOSTEL))
        assertEquals("Location de vacances", getAccommodationTypeLabel(AccommodationType.VACATION_RENTAL))
        assertEquals("Autre", getAccommodationTypeLabel(AccommodationType.OTHER))

        assertEquals("Recherche", getBookingStatusLabel(BookingStatus.SEARCHING))
        assertEquals("Réservé", getBookingStatusLabel(BookingStatus.RESERVED))
        assertEquals("Confirmé", getBookingStatusLabel(BookingStatus.CONFIRMED))
        assertEquals("Annulé", getBookingStatusLabel(BookingStatus.CANCELLED))

        AccommodationType.values().forEach { type ->
            assertFalse(getAccommodationTypeLabel(type).contains(type.name))
        }
        BookingStatus.values().forEach { status ->
            assertFalse(getBookingStatusLabel(status).contains(status.name))
        }
    }
}
