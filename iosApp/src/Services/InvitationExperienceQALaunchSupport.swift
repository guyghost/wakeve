#if DEBUG
import Foundation
import Shared

enum InvitationExperienceQALaunchRoute: Equatable {
    case library
    case detail(eventId: String)
    case studio(eventId: String)
    case audience(eventId: String)
    case information(eventId: String)
    case archive(eventId: String)
}

@MainActor
final class InvitationExperienceQALaunchSupport {
    static let seedArgument = "--wakeve-qa-seed-invitation-experience"
    static let openRouteArgument = "--wakeve-qa-open-invitation-route"
    static let reduceTransparencyArgument = "--wakeve-qa-reduce-transparency"

    private enum Seed {
        static let draft = "qa-invitation-draft"
        static let polling = "qa-invitation-polling"
        static let confirmed = "qa-invitation-confirmed"
        static let finalized = "qa-invitation-finalized"
        static let past = "qa-invitation-past"
        static let pendingParticipant = "qa-invitation-guest-pending"
        static let directInviteBatch = "qa-invitation-direct-batch"
        static let directInviteOperation = "qa-invitation-direct-operation"
        static let notificationOperation = "qa-invitation-notification-operation"
    }

    private let database: WakeveDb
    private let eventRepository: DatabaseEventRepository
    private let directInviteRepository: DatabaseDirectInviteBatchRepository
    private let notificationRepository: DatabaseEventNotificationPreferenceRepository
    private let router = InvitationExperienceRouter()

    init(
        database: WakeveDb,
        eventRepository: DatabaseEventRepository
    ) {
        self.database = database
        self.eventRepository = eventRepository
        directInviteRepository = DatabaseDirectInviteBatchRepository(database: database)
        notificationRepository = DatabaseEventNotificationPreferenceRepository(database: database)
    }

    func prepare(
        arguments: [String],
        viewerId: String
    ) async -> InvitationExperienceQALaunchRoute? {
        guard arguments.contains(Self.seedArgument), !viewerId.isEmpty else { return nil }
        // Direct DEBUG harnesses do not mount ContentView. Opt in here before
        // any seed/router work so the same explicit rollout decision applies
        // to direct support calls and first-launch root composition.
        UserDefaults.standard.set(true, forKey: "iosInvitationExperienceV1")
        guard await seedRepository(viewerId: viewerId) else { return nil }
        guard let rawRoute = argumentValue(
            after: Self.openRouteArgument,
            in: arguments
        ) else {
            return nil
        }

        switch rawRoute {
        case "library":
            return .library
        case "detail":
            return resolveLocalDetails(
                eventId: Seed.confirmed,
                viewerId: viewerId,
                request: InvitationExperienceRouteRequestCanvasAction(action: .showDetails)
            ) ? .detail(eventId: Seed.confirmed) : nil
        case "studio":
            return resolve(
                eventId: Seed.draft,
                viewerId: viewerId,
                request: InvitationExperienceRouteRequestCanvasAction(action: .editDraft),
                expected: .draftEditor
            ) ? .studio(eventId: Seed.draft) : nil
        case "audience":
            return resolve(
                eventId: Seed.draft,
                viewerId: viewerId,
                request: InvitationExperienceRouteRequestParticipants.shared,
                expected: .participants
            ) ? .audience(eventId: Seed.draft) : nil
        case "information":
            return resolve(
                eventId: Seed.confirmed,
                viewerId: viewerId,
                request: InvitationExperienceRouteRequestEventInformation.shared,
                expected: .eventInformation
            ) ? .information(eventId: Seed.confirmed) : nil
        case "archive":
            return resolve(
                eventId: Seed.finalized,
                viewerId: viewerId,
                request: InvitationExperienceRouteRequestDeepLink(
                    target: .archiveDetail,
                    intent: .read
                ),
                expected: .archiveDetail
            ) ? .archive(eventId: Seed.finalized) : nil
        default:
            return nil
        }
    }

    private func seedRepository(viewerId: String) async -> Bool {
        let now = Date()
        let futureStart = now.addingTimeInterval(30 * 24 * 60 * 60)
        let futureEnd = futureStart.addingTimeInterval(3 * 60 * 60)
        let pastStart = now.addingTimeInterval(-30 * 24 * 60 * 60)
        let pastEnd = pastStart.addingTimeInterval(3 * 60 * 60)

        guard ensureQAUsers(viewerId: viewerId) else { return false }

        let seeds = [
            makeEvent(
                id: Seed.draft,
                title: "Escapade à Annecy",
                status: .draft,
                viewerId: viewerId,
                start: futureStart,
                end: futureEnd,
                confirmed: false,
                presetId: "wakeve-lake"
            ),
            makeEvent(
                id: Seed.polling,
                title: "Vote pour le week-end",
                status: .polling,
                viewerId: viewerId,
                start: futureStart.addingTimeInterval(7 * 24 * 60 * 60),
                end: futureEnd.addingTimeInterval(7 * 24 * 60 * 60),
                confirmed: false,
                presetId: "wakeve-celebration"
            ),
            makeEvent(
                id: Seed.confirmed,
                title: "Week-end confirmé",
                status: .confirmed,
                viewerId: viewerId,
                start: futureStart.addingTimeInterval(14 * 24 * 60 * 60),
                end: futureEnd.addingTimeInterval(14 * 24 * 60 * 60),
                confirmed: true,
                presetId: "wakeve-sunset"
            ),
            makeEvent(
                id: Seed.finalized,
                title: "Séjour finalisé",
                status: .finalized,
                viewerId: viewerId,
                start: futureStart.addingTimeInterval(21 * 24 * 60 * 60),
                end: futureEnd.addingTimeInterval(21 * 24 * 60 * 60),
                confirmed: true,
                presetId: "wakeve-lake"
            ),
            makeEvent(
                id: Seed.past,
                title: "Souvenir du lac",
                status: .confirmed,
                viewerId: viewerId,
                start: pastStart,
                end: pastEnd,
                confirmed: true,
                presetId: "wakeve-celebration"
            )
        ]

        for seed in seeds where !(await ensureEvent(seed)) {
            return false
        }

        guard await ensureDraftAudience(viewerId: viewerId),
              ensureDraftLocation(),
              await ensureConfirmedNotification(viewerId: viewerId),
              await ensureProtectedDirectInvite(viewerId: viewerId)
        else {
            return false
        }
        return true
    }

    private func ensureEvent(_ seed: QASeedEvent) async -> Bool {
        if let existing = eventRepository.getEvent(id: seed.event.id) {
            guard existing.organizerId == seed.event.organizerId,
                  existing.status == seed.event.status,
                  !existing.proposedSlots.isEmpty
            else {
                return false
            }
            if existing.description_ != seed.event.description_ {
                do {
                    _ = try await eventRepository.updateEvent(event: seed.event)
                } catch {
                    return false
                }
            }
        } else {
            do {
                _ = try await eventRepository.createEvent(event: seed.event)
            } catch {
                return false
            }
        }

        let now = iso8601(Date())
        let currentArtwork = database.invitationExperienceQueries
            .selectArtworkByEventId(event_id: seed.event.id)
            .executeAsOneOrNull()
        if currentArtwork?.kind != "STRUCTURED" ||
            currentArtwork?.source_kind != "PRESET" ||
            currentArtwork?.preset_id != seed.presetId {
            database.invitationExperienceQueries.upsertEventArtwork(
                event_id: seed.event.id,
                kind: "STRUCTURED",
                structured_version: KotlinLong(value: 1),
                source_kind: "PRESET",
                preset_id: seed.presetId,
                server_asset_id: nil,
                canonical_https_url: nil,
                asset_revision: nil,
                alt_kind: "DECORATIVE",
                alt_text: nil,
                focal_x: KotlinDouble(value: seed.focalX),
                focal_y: KotlinDouble(value: seed.focalY),
                crop: "FILL",
                legacy_remote_url: nil,
                updated_at: now
            )
        }

        if seed.confirmed,
           database.confirmedDateQueries
            .existsByEventId(eventId: seed.event.id)
            .executeAsOneOrNull() == nil,
           let slot = seed.event.proposedSlots.first {
            database.confirmedDateQueries.insertConfirmedDate(
                id: "qa-confirmed-\(seed.event.id)",
                eventId: seed.event.id,
                timeslotId: slot.id,
                confirmedByOrganizerId: seed.event.organizerId,
                confirmedAt: now,
                updatedAt: now
            )
        }

        return eventRepository.getEvent(id: seed.event.id) != nil
    }

    private func ensureQAUsers(viewerId: String) -> Bool {
        let now = iso8601(Date())
        if database.userQueries.selectUserById(id: viewerId).executeAsOneOrNull() == nil {
            database.userQueries.insertUser(
                id: viewerId,
                provider_id: "qa-provider-organizer",
                email: "organizer@qa.wakeve.invalid",
                name: "Léa Martin",
                avatar_url: nil,
                provider: "qa",
                role: "ORGANIZER",
                created_at: now,
                updated_at: now
            )
        }
        if database.userQueries
            .selectUserById(id: Seed.pendingParticipant)
            .executeAsOneOrNull() == nil {
            database.userQueries.insertUser(
                id: Seed.pendingParticipant,
                provider_id: "qa-provider-guest",
                email: "guest@qa.wakeve.invalid",
                name: "Noé Bernard",
                avatar_url: nil,
                provider: "qa",
                role: "USER",
                created_at: now,
                updated_at: now
            )
        }
        return database.userQueries.selectUserById(id: viewerId).executeAsOneOrNull() != nil &&
            database.userQueries
                .selectUserById(id: Seed.pendingParticipant)
                .executeAsOneOrNull() != nil
    }

    private func ensureDraftLocation() -> Bool {
        if database.potentialLocationQueries
            .selectFirstLocationByEventId(eventId: Seed.draft)
            .executeAsOneOrNull() == nil {
            database.potentialLocationQueries.insertLocation(
                id: "qa-location-annecy",
                eventId: Seed.draft,
                name: "Annecy",
                locationType: "CITY",
                address: nil,
                coordinates: nil,
                createdAt: iso8601(Date())
            )
        }
        return database.potentialLocationQueries
            .selectFirstLocationByEventId(eventId: Seed.draft)
            .executeAsOneOrNull() != nil
    }

    private func ensureDraftAudience(viewerId: String) async -> Bool {
        let records = eventRepository.getParticipantRecords(eventId: Seed.draft) ?? []
        if !records.contains(where: { $0.userId == Seed.pendingParticipant }) {
            do {
                _ = try await eventRepository.addParticipant(
                    eventId: Seed.draft,
                    participantId: Seed.pendingParticipant
                )
            } catch {
                return false
            }
        }

        guard let organizer = database.participantQueries
            .selectByEventIdAndUserId(eventId: Seed.draft, userId: viewerId)
            .executeAsOneOrNull()
        else {
            return false
        }
        if organizer.hasValidatedDate != 1 {
            database.participantQueries.updateValidation(
                hasValidatedDate: 1,
                updatedAt: iso8601(Date()),
                id: organizer.id
            )
        }

        database.participantQueries.updateAccessAxes(
            rsvpState: "ACCEPTED",
            dateValidationState: "VALIDATED_RETAINED_DATE",
            updatedAt: iso8601(Date()),
            id: organizer.id
        )
        if let pending = database.participantQueries
            .selectByEventIdAndUserId(
                eventId: Seed.draft,
                userId: Seed.pendingParticipant
            )
            .executeAsOneOrNull() {
            database.participantQueries.updateAccessAxes(
                rsvpState: "PENDING",
                dateValidationState: "NOT_VALIDATED",
                updatedAt: iso8601(Date()),
                id: pending.id
            )
        }

        let updated = eventRepository.getParticipantRecords(eventId: Seed.draft) ?? []
        return updated.count >= 2 &&
            updated.contains(where: { $0.rsvp == "ACCEPTED" }) &&
            updated.contains(where: { $0.rsvp == "PENDING" })
    }

    private func ensureConfirmedNotification(viewerId: String) async -> Bool {
        do {
            if try await notificationRepository.get(
                eventId: Seed.confirmed,
                userId: viewerId
            ) != nil {
                return true
            }
            _ = try await notificationRepository.save(
                operationKey: OperationKey(
                    subject: OperationSubjectEventNotification(
                        eventId: Seed.confirmed,
                        userId: viewerId
                    ),
                    action: .saveEventPreference,
                    target: OperationTargetUser(userId: viewerId),
                    operationId: Seed.notificationOperation
                ),
                preference: .allEventUpdates
            )
            return try await notificationRepository.get(
                eventId: Seed.confirmed,
                userId: viewerId
            ) != nil
        } catch {
            return false
        }
    }

    private func ensureProtectedDirectInvite(viewerId: String) async -> Bool {
        let existingBatches = database.invitationExperienceQueries
            .selectDirectInviteBatchesByEventId(event_id: Seed.draft)
            .executeAsList()
        if let batch = existingBatches.first {
            let outcomes = database.invitationExperienceQueries
                .selectDirectInviteRecipientOutcomes(batch_id: batch.batch_id)
                .executeAsList()
            return existingBatches.count == 1 && outcomes.count >= 2
        }

        guard let event = eventRepository.getEvent(id: Seed.draft),
              event.organizerId == viewerId,
              event.status == .draft,
              let aggregate = database.eventQueries
                .selectById(id: Seed.draft)
                .executeAsOneOrNull(),
              aggregate.aggregateSchemaVersion == 1,
              let digestPort = KeychainDirectInviteRecipientDigestPort()
        else {
            return false
        }

        let keyOwner = DirectInviteRecipientKeyOwner(
            digestPort: digestPort,
            keyVersion: 1
        )
        guard let firstKey = keyOwner.protect(rawRecipientInput: "+33 6 12 34 56 70"),
              let secondKey = keyOwner.protect(rawRecipientInput: "+33 6 12 34 56 71")
        else {
            return false
        }

        let capability = DirectInviteCapabilityReady(
            eventId: event.id,
            actorId: viewerId,
            accessRevision: aggregate.aggregateRevision,
            allowedEventStatuses: Set([EventStatus.draft])
        )
        do {
            let operation = try await directInviteRepository.submit(
                command: SubmitDirectInviteBatchCommand(
                    eventId: event.id,
                    actorId: viewerId,
                    eventStatus: event.status,
                    batchId: Seed.directInviteBatch,
                    operationId: Seed.directInviteOperation,
                    recipientKeys: Set([firstKey, secondKey]),
                    capability: capability
                )
            )
            guard operation is DirectInviteOperationPendingSync else { return false }
        } catch {
            return false
        }

        let batches = database.invitationExperienceQueries
            .selectDirectInviteBatchesByEventId(event_id: Seed.draft)
            .executeAsList()
        guard batches.count == 1, let batch = batches.first else { return false }
        return database.invitationExperienceQueries
            .selectDirectInviteRecipientOutcomes(batch_id: batch.batch_id)
            .executeAsList()
            .count >= 2
    }

    private func resolve(
        eventId: String,
        viewerId: String,
        request: any InvitationExperienceRouteRequest,
        expected: InvitationExperienceRouteCapability
    ) -> Bool {
        guard let event = eventRepository.getEvent(id: eventId),
              event.organizerId == viewerId
        else {
            return false
        }
        let now = Kotlinx_datetimeInstant.companion.fromEpochMilliseconds(
            epochMilliseconds: Int64(Date().timeIntervalSince1970 * 1_000)
        )
        let resolution = router.resolve(
            request: request,
            context: InvitationExperienceRouteContext(
                eventStatus: event.status,
                temporalClass: EventTemporalClassifier.shared.classify(event: event, now: now),
                viewerRole: .organizer,
                access: InvitationExperienceRouteAccess(
                    canEditDraft: event.status == .draft,
                    canUsePoll: true,
                    canReadPollResults: true,
                    canOpenParticipants: true,
                    canOpenOrganization: true
                ),
                installedRoutes: Set([
                    .draftEditor,
                    .poll,
                    .participants,
                    .organization,
                    .eventInformation,
                    .archiveDetail
                ])
            )
        )
        return (resolution as? InvitationExperienceRouteResolutionDestination)?.route == expected
    }

    private func resolveLocalDetails(
        eventId: String,
        viewerId: String,
        request: any InvitationExperienceRouteRequest
    ) -> Bool {
        guard let event = eventRepository.getEvent(id: eventId),
              event.organizerId == viewerId
        else {
            return false
        }
        let now = Kotlinx_datetimeInstant.companion.fromEpochMilliseconds(
            epochMilliseconds: Int64(Date().timeIntervalSince1970 * 1_000)
        )
        let resolution = router.resolve(
            request: request,
            context: InvitationExperienceRouteContext(
                eventStatus: event.status,
                temporalClass: EventTemporalClassifier.shared.classify(event: event, now: now),
                viewerRole: .organizer,
                access: InvitationExperienceRouteAccess(
                    canEditDraft: false,
                    canUsePoll: false,
                    canReadPollResults: true,
                    canOpenParticipants: true,
                    canOpenOrganization: true
                ),
                installedRoutes: Set([
                    .draftEditor,
                    .poll,
                    .participants,
                    .organization,
                    .eventInformation,
                    .archiveDetail
                ])
            )
        )
        return resolution is InvitationExperienceRouteResolutionLocalDetails
    }

    private func argumentValue(after flag: String, in arguments: [String]) -> String? {
        guard let index = arguments.firstIndex(of: flag),
              arguments.indices.contains(index + 1)
        else {
            return nil
        }
        return arguments[index + 1]
    }

    private func makeEvent(
        id: String,
        title: String,
        status: EventStatus,
        viewerId: String,
        start: Date,
        end: Date,
        confirmed: Bool,
        presetId: String
    ) -> QASeedEvent {
        let startValue = iso8601(start)
        let endValue = iso8601(end)
        let slot = TimeSlot(
            id: "qa-slot-\(id)",
            start: startValue,
            end: endValue,
            timezone: "Europe/Paris",
            timeOfDay: .specific
        )
        let now = iso8601(Date())
        return QASeedEvent(
            event: Event(
                id: id,
                title: title,
                description: "Un week-end au bord du lac d’Annecy pour retrouver le groupe et profiter du château.",
                organizerId: viewerId,
                participants: [viewerId],
                proposedSlots: [slot],
                deadline: iso8601(start.addingTimeInterval(-7 * 24 * 60 * 60)),
                status: status,
                finalDate: confirmed ? startValue : nil,
                createdAt: now,
                updatedAt: now,
                eventType: .other,
                eventTypeCustom: nil,
                minParticipants: KotlinInt(value: 2),
                maxParticipants: KotlinInt(value: 8),
                expectedParticipants: KotlinInt(value: 4),
                heroImageUrl: nil,
                planningMode: .timeSlotPoll,
                aggregateRevision: 1,
                aggregateSchemaVersion: 1
            ),
            confirmed: confirmed,
            presetId: presetId,
            focalX: id == Seed.draft ? 0.72 : 0.5,
            focalY: id == Seed.draft ? 0.42 : 0.5
        )
    }

    private func iso8601(_ date: Date) -> String {
        ISO8601DateFormatter().string(from: date)
    }
}

private struct QASeedEvent {
    let event: Event
    let confirmed: Bool
    let presetId: String
    let focalX: Double
    let focalY: Double
}
#endif
