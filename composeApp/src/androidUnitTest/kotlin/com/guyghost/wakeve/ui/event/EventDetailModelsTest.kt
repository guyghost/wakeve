package com.guyghost.wakeve.ui.event

import com.guyghost.wakeve.access.DateValidationState
import com.guyghost.wakeve.access.ParticipantAccessState
import com.guyghost.wakeve.access.ParticipantRsvp
import com.guyghost.wakeve.models.Album
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.EventType
import com.guyghost.wakeve.models.LocationType
import com.guyghost.wakeve.models.PotentialLocation
import com.guyghost.wakeve.models.TimeSlot
import com.guyghost.wakeve.models.Vote
import com.guyghost.wakeve.payment.SettlementRecord
import com.guyghost.wakeve.postevent.PostEventItemStatus
import com.guyghost.wakeve.postevent.PostEventPrimaryAction
import com.guyghost.wakeve.presentation.state.EventManagementContract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EventDetailModelsTest {
    @Test
    fun eventDetailStatusLabelsAreUserFacingFrenchCopy() {
        assertEquals("Brouillon", eventDetailStatusLabel(EventStatus.DRAFT))
        assertEquals("Sondage", eventDetailStatusLabel(EventStatus.POLLING))
        assertEquals("Comparaison", eventDetailStatusLabel(EventStatus.COMPARING))
        assertEquals("Date confirmee", eventDetailStatusLabel(EventStatus.CONFIRMED))
        assertEquals("Organisation", eventDetailStatusLabel(EventStatus.ORGANIZING))
        assertEquals("Finalise", eventDetailStatusLabel(EventStatus.FINALIZED))
    }

    @Test
    fun eventDetailNextStepsAnswerWhatToDoNowForEveryStatus() {
        val statuses = EventStatus.entries

        assertEquals(statuses.size, statuses.map(::eventDetailNextStepTitle).toSet().size)
        assertEquals(statuses.size, statuses.map(::eventDetailNextStepBody).toSet().size)

        assertEquals("Terminer la creation", eventDetailNextStepTitle(EventStatus.DRAFT))
        assertEquals("Obtenir les votes", eventDetailNextStepTitle(EventStatus.POLLING))
        assertEquals("Choisir la meilleure option", eventDetailNextStepTitle(EventStatus.COMPARING))
        assertEquals("Inviter et preparer", eventDetailNextStepTitle(EventStatus.CONFIRMED))
        assertEquals("Piloter l'evenement", eventDetailNextStepTitle(EventStatus.ORGANIZING))
        assertEquals("Consulter le recapitulatif", eventDetailNextStepTitle(EventStatus.FINALIZED))

        assertTrue(eventDetailNextStepBody(EventStatus.POLLING).contains("participants"))
        assertTrue(eventDetailNextStepBody(EventStatus.CONFIRMED).contains("invitation"))
        assertTrue(eventDetailNextStepBody(EventStatus.ORGANIZING).contains("centre de controle"))
        assertTrue(eventDetailNextStepBody(EventStatus.FINALIZED).contains("verrouille"))
    }

    @Test
    fun detailStateFallsBackToRouteEventWhenSelectedEventBelongsToAnotherRoute() {
        val routeEvent = event(id = "route-event", status = EventStatus.POLLING)
        val otherSelectedEvent = event(id = "other-event", status = EventStatus.CONFIRMED)

        val uiState = EventManagementContract.State(
            events = listOf(routeEvent, otherSelectedEvent),
            selectedEvent = otherSelectedEvent,
            participantIds = listOf("other-participant"),
            pollVotes = mapOf("route-event" to mapOf("alice" to Vote.YES))
        ).toEventDetailUiState(eventId = "route-event", currentUserId = "organizer")

        assertEquals("route-event", uiState.event?.id)
        assertEquals(routeEvent.participants, uiState.participants)
        assertEquals(mapOf("alice" to Vote.YES), uiState.pollVotes)
    }

    @Test
    fun organizerCanEditButCannotDeleteFinalizedEvent() {
        val uiState = EventManagementContract.State(
            selectedEvent = event(status = EventStatus.FINALIZED)
        ).toEventDetailUiState(eventId = eventId, currentUserId = "organizer")

        assertTrue(uiState.isOrganizer)
        assertFalse(uiState.canDelete)
        assertTrue(uiState.canAccessOrganizationDetails)
        assertTrue(uiState.showOrganizationTools)
    }

    @Test
    fun finalizedEventExposesPostEventSummaryWithMissingFollowUpByDefault() {
        val uiState = EventManagementContract.State(
            selectedEvent = event(status = EventStatus.FINALIZED)
        ).toEventDetailUiState(eventId = eventId, currentUserId = "organizer")

        assertEquals(PostEventItemStatus.MISSING, uiState.postEventSummary?.settlementStatus)
        assertEquals(PostEventItemStatus.MISSING, uiState.postEventSummary?.photoStatus)
        assertEquals(PostEventItemStatus.COMPLETE, uiState.postEventSummary?.reorganizationStatus)
        assertEquals(PostEventPrimaryAction.OPEN_SETTLEMENTS, uiState.postEventSummary?.primaryAction)
    }

    @Test
    fun finalizedEventPostEventSummaryUsesInjectedSettlementsAndAlbums() {
        val uiState = EventManagementContract.State(
            selectedEvent = event(status = EventStatus.FINALIZED)
        ).toEventDetailUiState(
            eventId = eventId,
            currentUserId = "organizer",
            settlements = listOf(settlement(eventId = eventId, status = "PAID")),
            albums = listOf(album(eventId = eventId, photoIds = listOf("photo-1", "photo-2")))
        )

        assertEquals(PostEventItemStatus.COMPLETE, uiState.postEventSummary?.settlementStatus)
        assertEquals(PostEventItemStatus.COMPLETE, uiState.postEventSummary?.photoStatus)
        assertEquals(2, uiState.postEventSummary?.sharedPhotoCount)
        assertEquals(PostEventPrimaryAction.RECREATE_EVENT, uiState.postEventSummary?.primaryAction)
    }

    @Test
    fun postEventSummaryIsHiddenBeforeEventIsFinalized() {
        val uiState = EventManagementContract.State(
            selectedEvent = event(status = EventStatus.ORGANIZING)
        ).toEventDetailUiState(
            eventId = eventId,
            currentUserId = "organizer",
            settlements = listOf(settlement(eventId = eventId)),
            albums = listOf(album(eventId = eventId, photoIds = listOf("photo-1")))
        )

        assertEquals(null, uiState.postEventSummary)
    }

    @Test
    fun finalizedEventSettlementSummaryAnswersWhoMustReimburseWhom() {
        val uiState = EventManagementContract.State(
            selectedEvent = event(status = EventStatus.FINALIZED)
        ).toEventDetailUiState(
            eventId = eventId,
            currentUserId = "organizer",
            settlements = listOf(
                settlement(
                    eventId = eventId,
                    fromParticipantId = "participant-2",
                    toParticipantId = "organizer",
                    amount = 44.0
                ),
                settlement(
                    eventId = eventId,
                    fromParticipantId = "participant-1",
                    toParticipantId = "participant-2",
                    amount = 12.0
                ),
                settlement(
                    eventId = eventId,
                    fromParticipantId = "participant-3",
                    toParticipantId = "organizer",
                    amount = 99.0,
                    status = "PAID"
                ),
                settlement(
                    eventId = "other-event",
                    fromParticipantId = "participant-4",
                    toParticipantId = "organizer",
                    amount = 200.0
                )
            )
        )

        assertEquals("Remboursements", uiState.settlementSummary?.title)
        assertEquals("2 remboursements a regler", uiState.settlementSummary?.statusLabel)
        assertEquals("Total restant : 56 EUR", uiState.settlementSummary?.totalLabel)
        assertEquals(
            "Liste claire de qui doit rembourser qui apres l'evenement.",
            uiState.settlementSummary?.body
        )
        assertEquals("Regler les remboursements", uiState.settlementSummary?.actionLabel)
        assertTrue(uiState.settlementSummary?.canOpenBudget == true)
        assertEquals(
            listOf(
                "Participant 2 doit 44 EUR a Organizer",
                "Participant 1 doit 12 EUR a Participant 2"
            ),
            uiState.settlementSummary?.lines?.map { it.sentence }
        )
    }

    @Test
    fun finalizedEventSettlementSummaryPromptsCalculationWhenNoSettlementsExist() {
        val uiState = EventManagementContract.State(
            selectedEvent = event(status = EventStatus.FINALIZED)
        ).toEventDetailUiState(eventId = eventId, currentUserId = "organizer")

        assertEquals("A calculer", uiState.settlementSummary?.statusLabel)
        assertEquals(
            "Aucun remboursement n'est encore renseigne pour cet evenement.",
            uiState.settlementSummary?.body
        )
        assertEquals("Total a regler : non disponible", uiState.settlementSummary?.totalLabel)
        assertEquals(emptyList(), uiState.settlementSummary?.lines)
    }

    @Test
    fun finalizedEventSettlementSummaryMarksSettledRecordsAsDone() {
        val uiState = EventManagementContract.State(
            selectedEvent = event(status = EventStatus.FINALIZED)
        ).toEventDetailUiState(
            eventId = eventId,
            currentUserId = "organizer",
            settlements = listOf(settlement(eventId = eventId, status = "SETTLED"))
        )

        assertEquals("Soldes", uiState.settlementSummary?.statusLabel)
        assertEquals("Tous les remboursements connus sont marques comme regles.", uiState.settlementSummary?.body)
        assertEquals("Total restant : 0 EUR", uiState.settlementSummary?.totalLabel)
        assertEquals(emptyList(), uiState.settlementSummary?.lines)
    }

    @Test
    fun finalizedEventSettlementSummaryIsHiddenForUnconfirmedParticipant() {
        val uiState = EventManagementContract.State(
            selectedEvent = event(status = EventStatus.FINALIZED)
        ).toEventDetailUiState(
            eventId = eventId,
            currentUserId = "participant-1",
            settlements = listOf(settlement(eventId = eventId))
        )

        assertEquals(null, uiState.settlementSummary)
    }

    @Test
    fun confirmedParticipantCanAccessTransportButNotOrganizationToolsBeforeOrganizing() {
        val uiState = EventManagementContract.State(
            selectedEvent = event(status = EventStatus.CONFIRMED),
            participantAccessStates = listOf(
                ParticipantAccessState.member(
                    userId = "participant-1",
                    rsvp = ParticipantRsvp.ACCEPTED,
                    dateValidation = DateValidationState.VALIDATED_RETAINED_DATE
                )
            )
        ).toEventDetailUiState(eventId = eventId, currentUserId = "participant-1")

        assertFalse(uiState.isOrganizer)
        assertFalse(uiState.canDelete)
        assertTrue(uiState.canAccessOrganizationDetails)
        assertTrue(uiState.showTransportPlanning)
        assertFalse(uiState.showOrganizationTools)
        assertEquals(ParticipantRsvp.ACCEPTED, uiState.rsvp?.selectedResponse)
        assertTrue(uiState.rsvp?.isEnabled == true)
        assertEquals("Participation confirmée", uiState.rsvp?.statusLabel)
    }

    @Test
    fun organizerRsvpStateIsLockedAccepted() {
        val uiState = EventManagementContract.State(
            selectedEvent = event(status = EventStatus.CONFIRMED),
            participantAccessStates = listOf(ParticipantAccessState.organizer("organizer"))
        ).toEventDetailUiState(eventId = eventId, currentUserId = "organizer")

        assertEquals(ParticipantRsvp.ACCEPTED, uiState.rsvp?.selectedResponse)
        assertTrue(uiState.rsvp?.isOrganizer == true)
        assertFalse(uiState.rsvp?.isEnabled ?: true)
    }

    @Test
    fun scheduleSummaryShowsConfirmedDepartureDate() {
        val finalDate = "2026-07-14T18:30:00Z"
        val uiState = EventManagementContract.State(
            selectedEvent = event(
                status = EventStatus.CONFIRMED,
                finalDate = finalDate,
                proposedSlots = listOf(timeSlot("slot-1", "2026-07-13T08:00:00Z"))
            )
        ).toEventDetailUiState(eventId = eventId, currentUserId = "organizer")

        assertEquals("Date et depart", uiState.scheduleSummary?.title)
        assertEquals("Date retenue", uiState.scheduleSummary?.statusLabel)
        assertEquals(
            "Depart confirme : ${eventDayOfFormatFinalDate(finalDate)}",
            uiState.scheduleSummary?.primaryLabel
        )
        assertEquals(
            "Decision prise; gardez l'horaire visible pour le groupe.",
            uiState.scheduleSummary?.deadlineLabel
        )
        assertEquals(
            "Partagez la date retenue et preparez les rappels de depart.",
            uiState.scheduleSummary?.nextActionLabel
        )
        assertTrue(uiState.scheduleSummary?.hasConfirmedDate == true)
    }

    @Test
    fun scheduleSummaryShowsPollingSlotsBeforeConfirmation() {
        val earliestSlot = "2026-07-12T08:00:00Z"
        val uiState = EventManagementContract.State(
            selectedEvent = event(
                status = EventStatus.POLLING,
                proposedSlots = listOf(
                    timeSlot("slot-2", "2026-07-14T18:30:00Z"),
                    timeSlot("slot-1", earliestSlot)
                )
            )
        ).toEventDetailUiState(eventId = eventId, currentUserId = "organizer")

        assertEquals("Vote en cours", uiState.scheduleSummary?.statusLabel)
        assertEquals(
            "2 creneaux proposes - premier depart possible : ${eventDayOfFormatFinalDate(earliestSlot)}",
            uiState.scheduleSummary?.primaryLabel
        )
        assertTrue(uiState.scheduleSummary?.deadlineLabel.orEmpty().contains("Votes ouverts jusqu'au"))
        assertEquals(
            "Relancez les votes avant de confirmer la date.",
            uiState.scheduleSummary?.nextActionLabel
        )
        assertFalse(uiState.scheduleSummary?.hasConfirmedDate ?: true)
    }

    @Test
    fun scheduleSummaryWarnsWhenNoSlotIsProposed() {
        val uiState = EventManagementContract.State(
            selectedEvent = event(status = EventStatus.DRAFT)
        ).toEventDetailUiState(eventId = eventId, currentUserId = "organizer")

        assertEquals("A planifier", uiState.scheduleSummary?.statusLabel)
        assertEquals("Aucun creneau propose", uiState.scheduleSummary?.primaryLabel)
        assertEquals(
            "Ajoutez au moins un creneau pour obtenir une decision.",
            uiState.scheduleSummary?.nextActionLabel
        )
    }

    @Test
    fun programSummaryGuidesConfirmedOrganizerToSharedProgram() {
        val uiState = EventManagementContract.State(
            selectedEvent = event(
                status = EventStatus.CONFIRMED,
                eventType = EventType.OUTDOOR_ACTIVITY
            ),
            participantAccessStates = listOf(ParticipantAccessState.organizer("organizer"))
        ).toEventDetailUiState(eventId = eventId, currentUserId = "organizer")

        assertEquals("Programme", uiState.programSummary?.title)
        assertEquals("A construire", uiState.programSummary?.statusLabel)
        assertEquals(
            "A cadrer : activites, niveaux, equipement, pauses et repli.",
            uiState.programSummary?.scopeLabel
        )
        assertEquals(
            "Creez le programme partage pour que le groupe sache quoi suivre.",
            uiState.programSummary?.nextActionLabel
        )
        assertTrue(uiState.programSummary?.canOpenProgram == true)
    }

    @Test
    fun programSummaryBlocksProgramActionBeforeDateIsConfirmed() {
        val uiState = EventManagementContract.State(
            selectedEvent = event(status = EventStatus.POLLING)
        ).toEventDetailUiState(eventId = eventId, currentUserId = "organizer")

        assertEquals("En attente de date", uiState.programSummary?.statusLabel)
        assertEquals(
            "Attendez les votes ou preparez des idees de programme.",
            uiState.programSummary?.nextActionLabel
        )
        assertFalse(uiState.programSummary?.canOpenProgram ?: true)
    }

    @Test
    fun programSummaryKeepsProgramReadOnlyForFinalizedEvents() {
        val uiState = EventManagementContract.State(
            selectedEvent = event(
                status = EventStatus.FINALIZED,
                eventType = EventType.WEDDING
            ),
            participantAccessStates = listOf(ParticipantAccessState.organizer("organizer"))
        ).toEventDetailUiState(eventId = eventId, currentUserId = "organizer")

        assertEquals("Verrouille", uiState.programSummary?.statusLabel)
        assertEquals(
            "A cadrer : ceremonie, repas, transitions et temps forts.",
            uiState.programSummary?.scopeLabel
        )
        assertEquals("Conservez le programme comme reference du recap.", uiState.programSummary?.nextActionLabel)
        assertTrue(uiState.programSummary?.canOpenProgram == true)
    }

    @Test
    fun attendanceSummaryAnswersWhoComesWhenRsvpAreSynced() {
        val uiState = EventManagementContract.State(
            selectedEvent = event(
                status = EventStatus.POLLING,
                participants = listOf("organizer", "participant-1", "participant-2", "participant-3")
            ),
            participantAccessStates = listOf(
                ParticipantAccessState.organizer("organizer"),
                ParticipantAccessState.member(
                    userId = "participant-1",
                    rsvp = ParticipantRsvp.ACCEPTED,
                    dateValidation = DateValidationState.NOT_VALIDATED
                ),
                ParticipantAccessState.invitedPending("participant-2"),
                ParticipantAccessState.declined("participant-3")
            )
        ).toEventDetailUiState(eventId = eventId, currentUserId = "organizer")

        assertEquals("Presence", uiState.attendanceSummary?.title)
        assertEquals("Reponses a relancer", uiState.attendanceSummary?.statusLabel)
        assertEquals("Confirmes : 2", uiState.attendanceSummary?.confirmedLabel)
        assertEquals("En attente : 1", uiState.attendanceSummary?.pendingLabel)
        assertEquals("Ne viennent pas : 1", uiState.attendanceSummary?.declinedLabel)
        assertEquals(
            "Relancez les invites en attente avant de figer le budget et le programme.",
            uiState.attendanceSummary?.nextActionLabel
        )
        assertTrue(uiState.attendanceSummary?.hasSyncedRsvp == true)
    }

    @Test
    fun attendanceSummaryFallsBackToInvitedCountWhenRsvpAreNotSynced() {
        val uiState = EventManagementContract.State(
            selectedEvent = event(
                status = EventStatus.POLLING,
                participants = listOf("organizer", "participant-1", "participant-1", "participant-2")
            )
        ).toEventDetailUiState(eventId = eventId, currentUserId = "organizer")

        assertEquals("RSVP non synchronises", uiState.attendanceSummary?.statusLabel)
        assertEquals("Confirmes : non disponible", uiState.attendanceSummary?.confirmedLabel)
        assertEquals("Invites : 3", uiState.attendanceSummary?.pendingLabel)
        assertEquals("Refus : non disponible", uiState.attendanceSummary?.declinedLabel)
        assertEquals(
            "Synchronisez les RSVP pour savoir qui vient vraiment.",
            uiState.attendanceSummary?.nextActionLabel
        )
        assertFalse(uiState.attendanceSummary?.hasSyncedRsvp ?: true)
    }

    @Test
    fun notificationSummaryRecommendsVoteRemindersDuringPolling() {
        val uiState = EventManagementContract.State(
            selectedEvent = event(status = EventStatus.POLLING),
            participantAccessStates = listOf(
                ParticipantAccessState.organizer("organizer"),
                ParticipantAccessState.invitedPending("participant-1")
            )
        ).toEventDetailUiState(eventId = eventId, currentUserId = "organizer")

        assertEquals("Notifications", uiState.notificationSummary?.title)
        assertEquals("Votes a relancer", uiState.notificationSummary?.statusLabel)
        assertEquals("Priorite moyenne", uiState.notificationSummary?.priorityLabel)
        assertEquals("Risque spam modere", uiState.notificationSummary?.spamRiskLabel)
        assertEquals(
            "Relancez les votes manquants et gardez les autres mises a jour dans l'app.",
            uiState.notificationSummary?.nextActionLabel
        )
        assertEquals("Relance de vote", uiState.notificationSummary?.reminders?.first()?.title)
        assertEquals(
            "1 invite a relancer avant la date limite.",
            uiState.notificationSummary?.reminders?.first()?.body
        )
        assertTrue(uiState.notificationSummary?.reminders?.all { it.isRecommended } == true)
    }

    @Test
    fun notificationSummaryHighlightsDateAndDepartureAfterConfirmation() {
        val uiState = EventManagementContract.State(
            selectedEvent = event(
                status = EventStatus.CONFIRMED,
                finalDate = "2026-07-14T18:30:00Z"
            )
        ).toEventDetailUiState(eventId = eventId, currentUserId = "organizer")

        assertEquals("Date a diffuser", uiState.notificationSummary?.statusLabel)
        assertEquals("Priorite urgente", uiState.notificationSummary?.priorityLabel)
        assertEquals("Risque spam faible", uiState.notificationSummary?.spamRiskLabel)
        assertEquals(
            "Envoyez la date validee, puis programmez le rappel de depart.",
            uiState.notificationSummary?.nextActionLabel
        )
        assertEquals(
            listOf("Date validee", "Rappel de depart"),
            uiState.notificationSummary?.reminders?.map { it.title }
        )
        assertTrue(uiState.notificationSummary?.reminders?.last()?.isRecommended == true)
    }

    @Test
    fun notificationSummaryDoesNotRecommendDepartureReminderWithoutFinalDate() {
        val uiState = EventManagementContract.State(
            selectedEvent = event(status = EventStatus.CONFIRMED)
        ).toEventDetailUiState(eventId = eventId, currentUserId = "organizer")

        val departureReminder = uiState.notificationSummary?.reminders?.last()

        assertEquals("Rappel de depart", departureReminder?.title)
        assertEquals(
            "Confirmez l'horaire avant de programmer le rappel de depart.",
            departureReminder?.body
        )
        assertFalse(departureReminder?.isRecommended ?: true)
    }

    @Test
    fun notificationSummaryCallsOutProgramUpdateSpamRiskDuringOrganizing() {
        val uiState = EventManagementContract.State(
            selectedEvent = event(status = EventStatus.ORGANIZING)
        ).toEventDetailUiState(eventId = eventId, currentUserId = "organizer")

        assertEquals("Rappels actifs", uiState.notificationSummary?.statusLabel)
        assertEquals("Priorite urgente", uiState.notificationSummary?.priorityLabel)
        assertEquals("Risque spam eleve", uiState.notificationSummary?.spamRiskLabel)
        assertTrue(
            uiState.notificationSummary?.reminders
                ?.first { it.title == "Changement de programme" }
                ?.body
                .orEmpty()
                .contains("grouper")
        )
        assertEquals(
            "Gardez les rappels critiques; regroupez les changements de programme.",
            uiState.notificationSummary?.nextActionLabel
        )
    }

    @Test
    fun notificationSummaryLimitsFinalizedEventToTargetedFollowUps() {
        val uiState = EventManagementContract.State(
            selectedEvent = event(status = EventStatus.FINALIZED)
        ).toEventDetailUiState(eventId = eventId, currentUserId = "organizer")

        assertEquals("Suivi final", uiState.notificationSummary?.statusLabel)
        assertEquals("Risque spam eleve", uiState.notificationSummary?.spamRiskLabel)
        assertEquals(
            listOf("Remboursements restants", "Photos et recap"),
            uiState.notificationSummary?.reminders?.map { it.title }
        )
        assertEquals(
            "Limitez l'apres-evenement aux remboursements, photos et recap groupe.",
            uiState.notificationSummary?.nextActionLabel
        )
    }

    @Test
    fun attendanceSummaryGuidesOrganizerWhenNobodyConfirmed() {
        val uiState = EventManagementContract.State(
            selectedEvent = event(status = EventStatus.CONFIRMED),
            participantAccessStates = listOf(
                ParticipantAccessState.invitedPending("participant-1"),
                ParticipantAccessState.declined("participant-2")
            )
        ).toEventDetailUiState(eventId = eventId, currentUserId = "organizer")

        assertEquals("Aucun confirme", uiState.attendanceSummary?.statusLabel)
        assertEquals(
            "Obtenez au moins une confirmation avant de poursuivre l'organisation.",
            uiState.attendanceSummary?.nextActionLabel
        )
    }

    @Test
    fun budgetSummaryAnswersBudgetBasisForConfirmedOrganizer() {
        val uiState = EventManagementContract.State(
            selectedEvent = event(
                status = EventStatus.CONFIRMED,
                eventType = EventType.OUTDOOR_ACTIVITY,
                expectedParticipants = 8
            ),
            participantAccessStates = listOf(
                ParticipantAccessState.organizer("organizer"),
                ParticipantAccessState.member(
                    userId = "participant-1",
                    rsvp = ParticipantRsvp.ACCEPTED,
                    dateValidation = DateValidationState.VALIDATED_RETAINED_DATE
                )
            )
        ).toEventDetailUiState(eventId = eventId, currentUserId = "organizer")

        assertEquals("Budget", uiState.budgetSummary?.title)
        assertEquals("A cadrer", uiState.budgetSummary?.statusLabel)
        assertEquals("Base budget : 2 participants confirmes", uiState.budgetSummary?.participantBasisLabel)
        assertEquals(
            "A chiffrer : transport, logement, repas, activites et extras.",
            uiState.budgetSummary?.scopeLabel
        )
        assertEquals(
            "Creez une estimation par personne avant de lancer les depenses.",
            uiState.budgetSummary?.nextActionLabel
        )
        assertTrue(uiState.budgetSummary?.canOpenBudget == true)
    }

    @Test
    fun budgetSummaryFallsBackToExpectedParticipantsWhenRsvpIsNotSynced() {
        val uiState = EventManagementContract.State(
            selectedEvent = event(
                status = EventStatus.CONFIRMED,
                expectedParticipants = 6
            )
        ).toEventDetailUiState(eventId = eventId, currentUserId = "organizer")

        assertEquals("Base budget : 6 participants prevus", uiState.budgetSummary?.participantBasisLabel)
        assertTrue(uiState.budgetSummary?.canOpenBudget == true)
    }

    @Test
    fun budgetSummaryBlocksBudgetActionForUnconfirmedInvitee() {
        val uiState = EventManagementContract.State(
            selectedEvent = event(status = EventStatus.CONFIRMED),
            participantAccessStates = listOf(ParticipantAccessState.invitedPending("participant-1"))
        ).toEventDetailUiState(eventId = eventId, currentUserId = "participant-1")

        assertEquals("A cadrer", uiState.budgetSummary?.statusLabel)
        assertFalse(uiState.budgetSummary?.canOpenBudget ?: true)
    }

    @Test
    fun budgetSummaryIsHiddenBeforeDateIsConfirmed() {
        val uiState = EventManagementContract.State(
            selectedEvent = event(status = EventStatus.POLLING)
        ).toEventDetailUiState(eventId = eventId, currentUserId = "organizer")

        assertEquals(null, uiState.budgetSummary)
    }

    @Test
    fun dayOfSummaryExplainsExpectedPendingAndDeclinedParticipants() {
        val uiState = EventManagementContract.State(
            selectedEvent = event(
                status = EventStatus.ORGANIZING,
                participants = listOf("organizer", "participant-1", "participant-2", "participant-3")
            ),
            participantAccessStates = listOf(
                ParticipantAccessState.organizer("organizer"),
                ParticipantAccessState.member(
                    userId = "participant-1",
                    rsvp = ParticipantRsvp.ACCEPTED,
                    dateValidation = DateValidationState.VALIDATED_RETAINED_DATE
                ),
                ParticipantAccessState.invitedPending("participant-2"),
                ParticipantAccessState.declined("participant-3")
            )
        ).toEventDetailUiState(eventId = eventId, currentUserId = "organizer")

        assertEquals("Jour J", uiState.dayOfSummary?.title)
        assertEquals(EventDayOfStatus.NeedsAttention, uiState.dayOfSummary?.status)
        assertEquals(
            "Controle incomplet : 2 RSVP a verifier avant depart.",
            uiState.dayOfSummary?.controlLabel
        )
        assertEquals("2 participants attendus", uiState.dayOfSummary?.attendanceLabel)
        assertEquals("1 sans reponse · 1 ne vient pas", uiState.dayOfSummary?.missingLabel)
        assertEquals(
            "Presence a pointer : organizer, participant-1.",
            uiState.dayOfSummary?.arrivalTrackingLabel
        )
        assertEquals(
            "A verifier : participant-2 · Ne viennent pas : participant-3",
            uiState.dayOfSummary?.missingPeopleLabel
        )
        assertEquals(
            "Suivre les arrivees et traiter les absents avant la prochaine etape.",
            uiState.dayOfSummary?.nextActionLabel
        )
        assertEquals(4, uiState.dayOfSummary?.checklist?.size)
        assertEquals("Point de rendez-vous", uiState.dayOfSummary?.checklist?.get(0)?.title)
        assertEquals("Presents a pointer", uiState.dayOfSummary?.checklist?.get(1)?.title)
        assertEquals("Pret", uiState.dayOfSummary?.checklist?.get(1)?.statusLabel)
        assertEquals("Reponses a relancer", uiState.dayOfSummary?.checklist?.get(2)?.title)
        assertEquals("A relancer", uiState.dayOfSummary?.checklist?.get(2)?.statusLabel)
        assertTrue(uiState.dayOfSummary?.checklist?.get(2)?.isBlocking == true)
        assertEquals("Absents a traiter", uiState.dayOfSummary?.checklist?.get(3)?.title)
        assertEquals("A traiter", uiState.dayOfSummary?.checklist?.get(3)?.statusLabel)
    }

    @Test
    fun dayOfSummarySurfacesConcreteMeetingPointAndDepartureTime() {
        val finalDate = "2026-07-14T18:30:00Z"
        val uiState = EventManagementContract.State(
            selectedEvent = event(
                status = EventStatus.ORGANIZING,
                finalDate = finalDate
            ),
            participantAccessStates = listOf(
                ParticipantAccessState.organizer("organizer"),
                ParticipantAccessState.member(
                    userId = "participant-1",
                    rsvp = ParticipantRsvp.ACCEPTED,
                    dateValidation = DateValidationState.VALIDATED_RETAINED_DATE
                )
            ),
            potentialLocations = listOf(
                potentialLocation(
                    eventId = eventId,
                    name = "Gare de Lyon",
                    address = "Place Louis-Armand"
                ),
                potentialLocation(
                    eventId = "other-event",
                    name = "Mauvais lieu",
                    address = "Ne doit pas apparaitre"
                )
            )
        ).toEventDetailUiState(eventId = eventId, currentUserId = "organizer")

        assertEquals("Gare de Lyon (Place Louis-Armand)", uiState.dayOfSummary?.meetingPointLabel)
        assertEquals(eventDayOfFormatFinalDate(finalDate), uiState.dayOfSummary?.meetingTimeLabel)
        assertTrue(uiState.dayOfSummary?.checklist?.first()?.body.orEmpty().contains("Gare de Lyon"))
        assertTrue(uiState.dayOfSummary?.checklist?.first()?.body.orEmpty().contains("juillet 2026"))
    }

    @Test
    fun dayOfSummaryKeepsMeetingPointFallbacksWhenLogisticsAreMissing() {
        val uiState = EventManagementContract.State(
            selectedEvent = event(status = EventStatus.CONFIRMED),
            participantAccessStates = listOf(ParticipantAccessState.organizer("organizer"))
        ).toEventDetailUiState(eventId = eventId, currentUserId = "organizer")

        assertEquals("Lieu de rendez-vous a confirmer", uiState.dayOfSummary?.meetingPointLabel)
        assertEquals("Horaire a confirmer", uiState.dayOfSummary?.meetingTimeLabel)
        assertTrue(
            uiState.dayOfSummary?.checklist?.first()?.body.orEmpty()
                .contains("Lieu de rendez-vous a confirmer")
        )
    }

    @Test
    fun dayOfSummaryUsesFallbackWhenRsvpDetailsAreNotLoaded() {
        val uiState = EventManagementContract.State(
            selectedEvent = event(status = EventStatus.CONFIRMED)
        ).toEventDetailUiState(eventId = eventId, currentUserId = "organizer")

        assertEquals(EventDayOfStatus.NeedsAttention, uiState.dayOfSummary?.status)
        assertEquals(
            "Controle incomplet : synchronisez les RSVP avant de pointer les presents.",
            uiState.dayOfSummary?.controlLabel
        )
        assertEquals("Participants invites : 2", uiState.dayOfSummary?.attendanceLabel)
        assertEquals("RSVP detailles non synchronises", uiState.dayOfSummary?.missingLabel)
        assertEquals(
            "Presence a pointer des que les participants se presentent.",
            uiState.dayOfSummary?.arrivalTrackingLabel
        )
        assertEquals(
            "Liste des presents non disponible tant que les RSVP ne sont pas synchronises.",
            uiState.dayOfSummary?.missingPeopleLabel
        )
        assertEquals(
            "Verifier le lieu de rendez-vous et envoyer le rappel de depart.",
            uiState.dayOfSummary?.nextActionLabel
        )
        assertEquals("A synchroniser", uiState.dayOfSummary?.checklist?.get(1)?.statusLabel)
        assertEquals("A synchroniser", uiState.dayOfSummary?.checklist?.get(2)?.statusLabel)
        assertTrue(uiState.dayOfSummary?.checklist?.get(2)?.isBlocking == true)
    }

    @Test
    fun dayOfSummaryCompactsLongArrivalAndMissingLists() {
        val uiState = EventManagementContract.State(
            selectedEvent = event(
                status = EventStatus.ORGANIZING,
                participants = listOf(
                    "organizer",
                    "participant-1",
                    "participant-2",
                    "participant-3",
                    "participant-4",
                    "participant-5",
                    "participant-6"
                )
            ),
            participantAccessStates = listOf(
                ParticipantAccessState.organizer("organizer"),
                ParticipantAccessState.member(
                    userId = "participant-1",
                    rsvp = ParticipantRsvp.ACCEPTED,
                    dateValidation = DateValidationState.VALIDATED_RETAINED_DATE
                ),
                ParticipantAccessState.member(
                    userId = "participant-2",
                    rsvp = ParticipantRsvp.ACCEPTED,
                    dateValidation = DateValidationState.VALIDATED_RETAINED_DATE
                ),
                ParticipantAccessState.member(
                    userId = "participant-3",
                    rsvp = ParticipantRsvp.ACCEPTED,
                    dateValidation = DateValidationState.VALIDATED_RETAINED_DATE
                ),
                ParticipantAccessState.invitedPending("participant-4"),
                ParticipantAccessState.invitedPending("participant-5"),
                ParticipantAccessState.invitedPending("participant-6")
            )
        ).toEventDetailUiState(eventId = eventId, currentUserId = "organizer")

        assertEquals(
            "Presence a pointer : organizer, participant-1, participant-2 + 1 autre.",
            uiState.dayOfSummary?.arrivalTrackingLabel
        )
        assertEquals(
            "A verifier : participant-4, participant-5, participant-6",
            uiState.dayOfSummary?.missingPeopleLabel
        )
    }

    @Test
    fun dayOfSummaryDoesNotClaimRealArrivalWithoutCheckInState() {
        val summary = event(status = EventStatus.ORGANIZING)
            .toDayOfSummary(listOf(ParticipantAccessState.organizer("organizer")))

        assertEquals(EventDayOfStatus.Ready, summary?.status)
        assertEquals("Controle pret : tous les participants attendus sont confirmes.", summary?.controlLabel)
        assertFalse(summary?.arrivalTrackingLabel.orEmpty().contains("arrive", ignoreCase = true))
        assertTrue(summary?.arrivalTrackingLabel.orEmpty().contains("pointer"))
    }

    @Test
    fun dayOfSummaryIsBlockedWhenNobodyHasConfirmed() {
        val uiState = EventManagementContract.State(
            selectedEvent = event(
                status = EventStatus.ORGANIZING,
                participants = listOf("participant-1", "participant-2")
            ),
            participantAccessStates = listOf(
                ParticipantAccessState.invitedPending("participant-1"),
                ParticipantAccessState.declined("participant-2")
            )
        ).toEventDetailUiState(eventId = eventId, currentUserId = "organizer")

        assertEquals(EventDayOfStatus.Blocked, uiState.dayOfSummary?.status)
        assertEquals(
            "Controle bloque : aucun participant confirme pour l'instant.",
            uiState.dayOfSummary?.controlLabel
        )
        assertEquals("0 participants attendus", uiState.dayOfSummary?.attendanceLabel)
        assertEquals("Bloque", uiState.dayOfSummary?.checklist?.get(1)?.statusLabel)
        assertTrue(uiState.dayOfSummary?.checklist?.get(1)?.isBlocking == true)
    }

    @Test
    fun dayOfSummaryIsHiddenBeforeDateIsConfirmed() {
        val uiState = EventManagementContract.State(
            selectedEvent = event(status = EventStatus.POLLING),
            participantAccessStates = listOf(ParticipantAccessState.organizer("organizer"))
        ).toEventDetailUiState(eventId = eventId, currentUserId = "organizer")

        assertEquals(null, uiState.dayOfSummary)
    }

    private fun event(
        id: String = eventId,
        status: EventStatus,
        participants: List<String> = listOf("organizer", "participant-1"),
        finalDate: String? = null,
        eventType: EventType = EventType.OTHER,
        expectedParticipants: Int? = null,
        proposedSlots: List<TimeSlot> = emptyList()
    ): Event =
        Event(
            id = id,
            title = "Detail event",
            description = "Detail description",
            organizerId = "organizer",
            participants = participants,
            proposedSlots = proposedSlots,
            deadline = "2026-07-01T12:00:00Z",
            status = status,
            finalDate = finalDate,
            createdAt = "2026-06-01T08:00:00Z",
            updatedAt = "2026-06-01T08:00:00Z",
            eventType = eventType,
            expectedParticipants = expectedParticipants
        )

    private fun timeSlot(id: String, start: String): TimeSlot =
        TimeSlot(
            id = id,
            start = start,
            end = null,
            timezone = "Europe/Paris"
        )

    private fun potentialLocation(
        eventId: String,
        name: String,
        address: String? = null
    ): PotentialLocation =
        PotentialLocation(
            id = "location-$name",
            eventId = eventId,
            name = name,
            locationType = LocationType.SPECIFIC_VENUE,
            address = address,
            createdAt = "2026-06-01T08:00:00Z"
        )

    private fun settlement(
        eventId: String,
        amount: Double = 25.0,
        status: String = "PERSISTED",
        fromParticipantId: String = "participant-1",
        toParticipantId: String = "organizer"
    ): SettlementRecord =
        SettlementRecord(
            settlementId = "settlement-$status-$fromParticipantId-$toParticipantId",
            eventId = eventId,
            budgetId = "budget-1",
            fromParticipantId = fromParticipantId,
            toParticipantId = toParticipantId,
            amount = amount,
            status = status,
            createdAt = "2026-06-02T08:00:00Z",
            updatedAt = "2026-06-02T08:00:00Z"
        )

    private fun album(eventId: String, photoIds: List<String>): Album =
        Album(
            id = "album-${photoIds.size}",
            eventId = eventId,
            name = "Photos",
            coverPhotoId = photoIds.firstOrNull(),
            photoIds = photoIds,
            createdAt = "2026-06-02T08:00:00Z",
            isAutoGenerated = true,
            updatedAt = "2026-06-02T08:00:00Z"
        )

    private companion object {
        const val eventId = "event-1"
    }
}
