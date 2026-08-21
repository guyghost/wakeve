import XCTest
import SwiftUI
import CryptoKit
import Security
import Shared
@testable import Wakeve

@MainActor
final class InvitationExperienceRuntimeSurfaceTests: XCTestCase {
    private var retainedWindows: [UIWindow] = []

    override func tearDown() {
        retainedWindows.forEach { $0.isHidden = true }
        retainedWindows.removeAll()
        super.tearDown()
    }

    func testLibraryRendersAllFiveInteractiveRepositoryProjections() {
        let identifiers = renderIdentifiers(
            EventLibraryView(onOpenEvent: { _ in }, onCreateEvent: {})
        )

        XCTAssertTrue(identifiers.contains("eventLibraryPrimaryAction"), identifiers.description)
        assertContains(
            identifiers,
            [
                "eventLibraryFilterDrafts",
                "eventLibraryFilterHosting",
                "eventLibraryFilterAttending",
                "eventLibraryFilterUpcoming",
                "eventLibraryFilterPast"
            ],
            surface: "Library"
        )
    }

    func testLibraryFilterTitlesResolveLocalizedCopyInsteadOfShowingLocalizationKeys() {
        let rendered = render(
            EventLibraryView(onOpenEvent: { _ in }, onCreateEvent: {})
        )

        for filter in ["drafts", "hosting", "attending", "upcoming", "past"] {
            XCTAssertFalse(
                rendered.labels.contains("invitation.library.filter.\(filter)"),
                "The real Library filter must expose translated copy, not its localization key."
            )
        }
        let localizedUpcoming = String(localized: "invitation.library.filter.upcoming")
        XCTAssertNotEqual(
            localizedUpcoming,
            "invitation.library.filter.upcoming",
            "The active XCTest locale must resolve the selected Upcoming filter."
        )
        XCTAssertTrue(
            rendered.labels.contains(localizedUpcoming),
            "The selected Upcoming filter must expose the active catalog value. Visible labels: \(rendered.labels.sorted())"
        )
    }

    func testAuthenticatedRootInstallsTheInvitationLibraryInsteadOfOnlyLegacyHome() {
        let rolloutKey = "iosInvitationExperienceV1"
        let previous = UserDefaults.standard.object(forKey: rolloutKey)
        UserDefaults.standard.set(true, forKey: rolloutKey)
        defer {
            if let previous {
                UserDefaults.standard.set(previous, forKey: rolloutKey)
            } else {
                UserDefaults.standard.removeObject(forKey: rolloutKey)
            }
        }
        let authService = AuthenticationService()
        let authStateManager = AuthStateManager(authService: authService, enableOAuth: false)
        let deepLinkService = DeepLinkService()
        let identifiers = renderIdentifiers(
            AuthenticatedView(userId: "viewer-1")
                .environmentObject(authStateManager)
                .environmentObject(deepLinkService)
        )

        XCTAssertTrue(
            identifiers.contains("eventLibraryPrimaryAction"),
            "The authenticated production root must install the invitation Library; a standalone declaration is unreachable."
        )
    }

    func testInformationAndArchiveDeepLinksReachTypedInvitationPaths() throws {
        let service = DeepLinkService()
        let routes: [(String, DeepLinkType)] = [
            (
                "wakeve://event/event-1/information",
                .eventInformation(eventId: "event-1")
            ),
            (
                "wakeve://event/event-1/archive",
                .eventArchive(eventId: "event-1")
            )
        ]

        for (rawURL, expected) in routes {
            let url = try XCTUnwrap(URL(string: rawURL))
            XCTAssertEqual(
                service.parseDeepLink(url),
                expected,
                "Every shipped invitation surface needs a parsable global deep link before AuthenticatedView can preflight it through the typed router."
            )
        }
    }

    func testStudioKeepsDraftRevisionInternalWhileRenderingArtworkWithoutClaimingPendingSync() {
        let viewModel = EventCreationStudioViewModel()
        let rendered = render(
            EventCreationStudioView(
                previewAvailable: true,
                onCancel: {},
                onRequestPreview: { _, _ in }
            )
        )

        XCTAssertTrue(
            rendered.identifiers.contains("eventCreationStudioPrimaryAction"),
            rendered.identifiers.description
        )
        assertContains(
            rendered.identifiers,
            [
                "eventStudioArtworkNone",
                "eventStudioArtworkPreset"
            ],
            surface: "Creation Studio"
        )
        XCTAssertFalse(
            rendered.identifiers.contains("eventStudioRetryAction"),
            "Retry is an adaptive state of the single primary action, not a second CTA."
        )
        XCTAssertEqual(viewModel.currentDraftRevision, 0)
        XCTAssertFalse(
            rendered.identifiers.contains("eventStudioCurrentRevisionPreview"),
            "The CAS draft revision must stay in the typed owner and must not appear as user-facing copy."
        )
        XCTAssertFalse(
            rendered.identifiers.contains("eventStudioPendingSyncState"),
            "An untouched Editing draft must not claim it is already committed and pending synchronization."
        )
        XCTAssertFalse(
            viewModel.retryAvailable,
            "Retry must stay unavailable until a typed failure state exists."
        )
    }

    func testStudioRelaunchRestoresExactPersistedPendingSyncBinding() async throws {
        let event = EventFactory.make(
            id: "studio-relaunch-\(UUID().uuidString.lowercased())",
            title: "Persisted Studio draft",
            description: "A committed draft whose synchronization is still pending.",
            organizerId: "studio-owner",
            deadline: "2099-01-01T00:00:00Z",
            status: .draft
        )
        let repository = RepositoryProvider.shared.databaseRepository
        let database = RepositoryProvider.shared.database
        _ = try await repository.saveEvent(event: event)
        let aggregateOwner = DatabaseUpdateDraftAggregateUseCase(database: database)
        let operationId = "studio-relaunch-operation-\(UUID().uuidString.lowercased())"
        let result = try await aggregateOwner.execute(
            command: UpdateDraftAggregateCommand(
                eventId: event.id,
                actorId: event.organizerId,
                expectedBaseRevision: 1,
                eventDraft: StudioEventFields(
                    title: event.title,
                    description: event.description_,
                    deadline: event.deadline,
                    eventType: event.eventType,
                    eventTypeCustom: event.eventTypeCustom,
                    minParticipants: event.minParticipants,
                    maxParticipants: event.maxParticipants,
                    expectedParticipants: event.expectedParticipants,
                    proposedSlots: event.proposedSlots,
                    planningMode: event.planningMode
                ),
                artwork: ArtworkNone.shared,
                operationId: operationId,
                artworkCapability: ArtworkSelectionCapabilityHidden.shared
            )
        )
        let committed = try XCTUnwrap(result as? UpdateDraftAggregateResultCommitted)
        XCTAssertTrue(committed.pendingSync)

        let relaunched = EventCreationStudioViewModel(
            eventId: event.id,
            actorId: event.organizerId,
            baseRevision: committed.committedRevision,
            existingArtwork: ArtworkNone.shared,
            repository: repository,
            database: database,
            aggregateOwner: aggregateOwner,
            syncOwner: DatabaseCreationStudioSyncOwner(database: database)
        )
        for _ in 0..<20 where !(relaunched.studioState is CreationStudioStatePendingSync) {
            try await Task.sleep(nanoseconds: 10_000_000)
        }

        let pending = try XCTUnwrap(relaunched.studioState as? CreationStudioStatePendingSync)
        XCTAssertEqual(pending.eventId, event.id)
        XCTAssertEqual(pending.committedRevision, committed.committedRevision)
        XCTAssertEqual(pending.operationId, operationId)
    }

    func testStudioKeepsPreviewDisabledForAnInvalidEmptyDraft() {
        let rendered = render(
            EventCreationStudioView(
                previewAvailable: true,
                onCancel: {},
                onRequestPreview: { _, _ in }
            )
        )

        XCTAssertTrue(rendered.identifiers.contains("eventCreationStudioPrimaryAction"))
        XCTAssertTrue(
            rendered.traits["eventCreationStudioPrimaryAction"]?.contains(.notEnabled) == true,
            "Studio must derive validation from its current draft; a caller boolean cannot authorize preview of an empty draft."
        )
    }

    func testStudioUsesLocalizedLabelsForItsRequiredFields() {
        let rendered = render(
            EventCreationStudioView(
                previewAvailable: true,
                onCancel: {},
                onRequestPreview: { _, _ in }
            )
        )

        XCTAssertFalse(
            rendered.labels.contains("create_event.title_label"),
            "The real Studio must not expose a missing localization key as its title label."
        )
        XCTAssertFalse(
            rendered.labels.contains("create_event.title_placeholder"),
            "The real Studio must not expose a missing localization key as its title placeholder."
        )
        XCTAssertNotEqual(
            String(localized: "common.retry"),
            "common.retry",
            "The real Studio must render translated retry copy instead of exposing its localization key."
        )
    }

    func testStudioArtworkChoicesUseAdaptiveWidthAtStandardAndAccessibilityDynamicType() async throws {
        let event = EventFactory.make(
            id: "studio-layout-\(UUID().uuidString)",
            title: "Adaptive artwork layout",
            organizerId: "organizer-1",
            deadline: "2099-01-01T00:00:00Z",
            status: .draft
        )
        let repository = RepositoryProvider.shared.databaseRepository
        _ = try await repository.saveEvent(event: event)
        defer {
            Task { try? await repository.deleteEvent(eventId: event.id) }
        }
        let viewModel = EventCreationStudioViewModel(
            eventId: event.id,
            actorId: "organizer-1",
            baseRevision: 1,
            existingArtwork: ArtworkNone.shared,
            repository: repository
        )
        let standard = render(
            EventCreationStudioView(
                viewModel: viewModel,
                previewAvailable: true,
                onCancel: {},
                onRequestPreview: { _, _ in }
            ),
            viewport: CGSize(width: 390, height: 2_000)
        )
        let accessibility = render(
            EventCreationStudioView(
                viewModel: viewModel,
                previewAvailable: true,
                onCancel: {},
                onRequestPreview: { _, _ in }
            )
            .dynamicTypeSize(.accessibility3),
            viewport: CGSize(width: 390, height: 2_000)
        )
        let choices = [
            "eventStudioArtworkKeepExisting",
            "eventStudioArtworkNone",
            "eventStudioArtworkPreset"
        ]

        let standardFrames = try choices.map { identifier in
            try XCTUnwrap(
                standard.frames[identifier],
                "Missing standard frame for \(identifier); available frames: \(standard.frames)"
            )
        }
        let accessibilityFrames = try choices.map { identifier in
            try XCTUnwrap(
                accessibility.frames[identifier],
                "Missing accessibility frame for \(identifier); available frames: \(accessibility.frames)"
            )
        }
        let standardRows = Set(standardFrames.map { Int($0.midY.rounded()) })
        let accessibilityRows = Set(accessibilityFrames.map { Int($0.midY.rounded()) })
        let accessibilityColumns = accessibilityFrames.map(\.midX)

        XCTAssertGreaterThanOrEqual(
            standardRows.count,
            2,
            "At standard Dynamic Type, three long localized artwork choices must leave the cramped single HStack row."
        )
        XCTAssertEqual(
            accessibilityRows.count,
            3,
            "Accessibility Dynamic Type must stack every artwork choice on its own row."
        )
        XCTAssertLessThanOrEqual(
            (accessibilityColumns.max() ?? 0) - (accessibilityColumns.min() ?? 0),
            2,
            "Accessibility Dynamic Type must align full-width artwork choices in one column."
        )
    }

    func testAudienceRendersIndependentAxesAndPerRecipientOutcomes() {
        let identifiers = renderIdentifiers(
            EventAudienceView(
                eventId: "missing-event",
                directInviteAvailable: true,
                onInvite: {}
            )
        )

        XCTAssertTrue(identifiers.contains("eventAudiencePrimaryAction"), identifiers.description)
        assertContains(
            identifiers,
            [
                "eventAudienceDeliveryAxis",
                "eventAudienceApprovalAxis",
                "eventAudienceMembershipAxis",
                "eventAudienceRsvpAxis",
                "eventAudienceDateValidationAxis",
                "eventAudienceRecipientOutcome"
            ],
            surface: "Audience"
        )
    }

    func testInformationReloadRefreshesEveryInjectedSystemAuthorizationWithoutPrompting() async {
        let statuses: [SystemNotificationAuthorization] = [
            .authorized,
            .denied,
            .notDetermined,
            .provisional,
            .ephemeral
        ]
        var nextStatusIndex = 0
        var readCount = 0
        let viewModel = EventInformationViewModel(
            eventId: "missing-event",
            systemAuthorizationReader: {
                let status = statuses[nextStatusIndex]
                nextStatusIndex += 1
                readCount += 1
                return status
            }
        )

        for expected in statuses {
            await viewModel.reload()
            XCTAssertEqual(viewModel.systemAuthorization, expected)
        }
        XCTAssertEqual(readCount, statuses.count)
    }

    func testAudienceFailsClosedWhenDirectInviteHasNoBoundDraftCapability() {
        let rendered = render(
            EventAudienceView(
                eventId: "missing-event",
                directInviteAvailable: true,
                onInvite: {}
            )
        )

        XCTAssertTrue(rendered.identifiers.contains("eventAudiencePrimaryAction"))
        XCTAssertTrue(
            rendered.traits["eventAudiencePrimaryAction"]?.contains(.notEnabled) == true,
            "A raw caller boolean must not enable direct invite without a matching DRAFT event capability."
        )
    }

    func testAudienceFailsClosedWhenDirectInviteCapabilityRevisionIsStale() async throws {
        let event = EventFactory.make(
            id: "audience-stale-\(UUID().uuidString)",
            title: "Protected draft",
            organizerId: "organizer-1",
            deadline: "2099-01-01T00:00:00Z",
            status: .draft
        )
        let repository = RepositoryProvider.shared.databaseRepository
        _ = try await repository.saveEvent(event: event)
        defer {
            Task { try? await repository.deleteEvent(eventId: event.id) }
        }
        let rendered = render(
            EventAudienceView(
                eventId: event.id,
                directInviteAvailable: true,
                repository: repository,
                directInviteCapability: DirectInviteCapabilityReady(
                    eventId: event.id,
                    actorId: event.organizerId,
                    accessRevision: 999,
                    allowedEventStatuses: Set([EventStatus.draft])
                ),
                onInvite: {}
            )
        )

        XCTAssertTrue(rendered.identifiers.contains("eventAudiencePrimaryAction"))
        XCTAssertTrue(
            rendered.traits["eventAudiencePrimaryAction"]?.contains(.notEnabled) == true,
            "A stale access revision must never expand direct-invite permission in SwiftUI."
        )
    }

    func testAudienceFailsClosedWhenRecipientKeyOwnerIsUnavailable() async throws {
        let event = EventFactory.make(
            id: "audience-owner-unavailable-\(UUID().uuidString)",
            title: "Protected draft",
            organizerId: "organizer-1",
            deadline: "2099-01-01T00:00:00Z",
            status: .draft
        )
        let repository = RepositoryProvider.shared.databaseRepository
        _ = try await repository.saveEvent(event: event)
        defer {
            Task { try? await repository.deleteEvent(eventId: event.id) }
        }
        let revision = try XCTUnwrap(
            RepositoryProvider.shared.database.eventQueries
                .selectById(id: event.id)
                .executeAsOneOrNull()?
                .aggregateRevision
        )
        let capability = DirectInviteCapabilityReady(
            eventId: event.id,
            actorId: event.organizerId,
            accessRevision: revision,
            allowedEventStatuses: Set([EventStatus.draft])
        )
        let viewModel = EventAudienceViewModel(
            eventId: event.id,
            repository: repository,
            database: RepositoryProvider.shared.database,
            directInviteCapability: capability,
            recipientKeyOwner: nil
        )

        await viewModel.reload()

        XCTAssertFalse(
            viewModel.inviteEnabled,
            "A matching DRAFT capability must remain fail-closed when Keychain cannot supply the trusted recipient-key owner."
        )
        let unavailableSubmit = await viewModel.submitRecipient("alice@example.com")
        XCTAssertFalse(unavailableSubmit)
    }

    func testAudienceSubmitPersistsOnlyProtectedRecipientOutcome() async throws {
        let event = EventFactory.make(
            id: "audience-protected-submit-\(UUID().uuidString)",
            title: "Protected draft",
            organizerId: "organizer-1",
            deadline: "2099-01-01T00:00:00Z",
            status: .draft
        )
        let repository = RepositoryProvider.shared.databaseRepository
        let database = RepositoryProvider.shared.database
        _ = try await repository.saveEvent(event: event)
        defer {
            Task { try? await repository.deleteEvent(eventId: event.id) }
        }
        let revision = try XCTUnwrap(
            database.eventQueries.selectById(id: event.id).executeAsOneOrNull()?.aggregateRevision
        )
        let capability = DirectInviteCapabilityReady(
            eventId: event.id,
            actorId: event.organizerId,
            accessRevision: revision,
            allowedEventStatuses: Set([EventStatus.draft])
        )
        let viewModel = EventAudienceViewModel(
            eventId: event.id,
            repository: repository,
            database: database,
            directInviteCapability: capability,
            recipientKeyOwner: DirectInviteRecipientKeyOwner(
                digestPort: TestDirectInviteRecipientDigestPort(),
                keyVersion: 1
            ),
            deliverySealer: TestDirectInviteDeliverySealer(),
            deliveryTransport: TestDirectInviteDeliveryTransport()
        )

        await viewModel.reload()
        XCTAssertTrue(viewModel.inviteEnabled)
        let submitAccepted = await viewModel.submitRecipient("  Alice@Example.COM  ")
        XCTAssertTrue(submitAccepted)

        let batches = database.invitationExperienceQueries
            .selectDirectInviteBatchesByEventId(event_id: event.id)
            .executeAsList()
        XCTAssertEqual(batches.count, 1)
        let outcomes = database.invitationExperienceQueries
            .selectDirectInviteRecipientOutcomes(batch_id: try XCTUnwrap(batches.first?.batch_id))
            .executeAsList()
        XCTAssertEqual(outcomes.count, 1)
        let protectedKey = try XCTUnwrap(outcomes.first?.recipient_key)
        XCTAssertTrue(protectedKey.hasPrefix("hmac-v1-"))
        XCTAssertFalse(protectedKey.localizedCaseInsensitiveContains("alice"))
        XCTAssertFalse(protectedKey.localizedCaseInsensitiveContains("example.com"))
        XCTAssertFalse(
            viewModel.displayNamesByIdentityKey.values.contains {
                $0.localizedCaseInsensitiveContains("alice@example.com")
            },
            "The transient recipient input must never reappear in the visible Audience identity projection."
        )
    }

    func testRecipientDigestKeyIsRandomKeychainBackedAndStableAcrossRelaunch() throws {
        let service = "com.guyghost.wakeve.tests.direct-invite.\(UUID().uuidString)"
        let otherService = "\(service).other"
        let account = "recipient-hmac-v1"
        deleteKeychainItem(service: service, account: account)
        deleteKeychainItem(service: otherService, account: account)
        defer {
            deleteKeychainItem(service: service, account: account)
            deleteKeychainItem(service: otherService, account: account)
        }

        let firstLaunch: KeychainDirectInviteRecipientDigestPort
        if let provider = KeychainDirectInviteRecipientDigestPort(service: service, account: account) {
            firstLaunch = provider
        } else {
            let probeStatus = keychainWriteProbe(
                service: "\(service).probe",
                account: account
            )
            XCTFail("Recipient Keychain provider was unavailable; equivalent SecItemAdd probe status: \(probeStatus).")
            return
        }
        let firstDigest = try XCTUnwrap(
            firstLaunch.hmacSha256(normalizedRecipient: "alice@example.com")
        )
        let persistedSecret = try XCTUnwrap(
            keychainData(service: service, account: account)
        )
        XCTAssertEqual(persistedSecret.count, 32, "The device-protected HMAC secret must be 256 random bits.")
        XCTAssertNotEqual(persistedSecret, Data(repeating: 0, count: 32))

        let relaunched = try XCTUnwrap(
            KeychainDirectInviteRecipientDigestPort(service: service, account: account)
        )
        XCTAssertEqual(
            firstDigest,
            relaunched.hmacSha256(normalizedRecipient: "alice@example.com"),
            "A new app/provider instance must reload the same Keychain secret after relaunch."
        )

        let independentInstall = try XCTUnwrap(
            KeychainDirectInviteRecipientDigestPort(service: otherService, account: account)
        )
        let independentDigest = try XCTUnwrap(
            independentInstall.hmacSha256(normalizedRecipient: "alice@example.com")
        )
        XCTAssertNotEqual(
            firstDigest,
            independentDigest,
            "Separate Keychain namespaces must generate independent random secrets, never reuse hardcoded key material."
        )
        let plainDigest = SHA256.hash(data: Data("alice@example.com".utf8))
            .map { String(format: "%02x", $0) }
            .joined()
        XCTAssertNotEqual(firstDigest, plainDigest, "Recipient identities require keyed HMAC, not plain SHA-256.")
        XCTAssertFalse(firstDigest.localizedCaseInsensitiveContains("alice"))
        XCTAssertFalse(firstDigest.localizedCaseInsensitiveContains("example.com"))
    }

    func testAudienceDoesNotExposeRepositoryParticipantIdentifiersAsVisibleCopy() async throws {
        let event = EventFactory.make(
            id: "audience-copy-\(UUID().uuidString)",
            title: "Audience copy",
            organizerId: "organizer-\(UUID().uuidString)",
            deadline: "2099-01-01T00:00:00Z",
            status: .draft
        )
        let repository = RepositoryProvider.shared.databaseRepository
        _ = try await repository.saveEvent(event: event)
        defer {
            Task { try? await repository.deleteEvent(eventId: event.id) }
        }
        let rendered = render(
            EventAudienceView(
                eventId: event.id,
                directInviteAvailable: false,
                repository: repository,
                onInvite: {}
            )
        )
        let participantRecordId = "part_\(event.id)_\(event.organizerId)"

        XCTAssertFalse(
            rendered.labels.contains(where: {
                $0.contains(participantRecordId) || $0.contains(event.organizerId)
            }),
            "Audience visible/accessibility copy must use a user-facing identity projection, never repository ids: \(rendered.labels.sorted())"
        )
    }

    func testInformationRendersTypedProvidersNotificationWriteAndGuardedOperations() {
        let identifiers = renderIdentifiers(
            EventInformationView(eventId: "missing-event", onDone: {})
        )

        XCTAssertTrue(identifiers.contains("eventInformationPrimaryAction"), identifiers.description)
        assertContains(
            identifiers,
            [
                "eventInformationCalendarDestination",
                "eventInformationMapsDestination",
                "eventInformationWeatherDestination",
                "eventInformationPreferenceWrite",
                "eventInformationLeaveConfirmation",
                "eventInformationDeleteConfirmation"
            ],
            surface: "Information"
        )
    }

    func testInformationRendersTheDomainDescriptionWithoutKotlinDebugIdentityDump() async throws {
        let event = EventFactory.make(
            id: "information-copy-\(UUID().uuidString)",
            title: "Week-end confirmé",
            description: "Description sûre destinée aux invités.",
            organizerId: "private-organizer-id",
            participants: ["private-participant-id"],
            status: .confirmed,
            finalDate: "2099-01-02T10:00:00Z"
        )
        let repository = RepositoryProvider.shared.databaseRepository
        _ = try await repository.saveEvent(event: event)
        defer {
            Task { try? await repository.deleteEvent(eventId: event.id) }
        }

        let viewModel = EventInformationViewModel(
            eventId: event.id,
            viewerId: event.organizerId,
            repository: repository
        )
        XCTAssertEqual(
            viewModel.event?.description_,
            "Description sûre destinée aux invités.",
            "The real repository owner must retain the domain description independently of Kotlin CustomStringConvertible."
        )
        XCTAssertFalse(viewModel.event?.description_.contains(event.id) == true)
        XCTAssertFalse(viewModel.event?.description_.contains(event.organizerId) == true)

        let informationSource = try readProjectFile(
            "iosApp/src/Views/Invitations/EventInformationView.swift"
        )
        XCTAssertTrue(
            informationSource.contains("Text(event.description_)"),
            "Event Information must bind the visible Text to the exported KMP domain field."
        )
        XCTAssertFalse(
            informationSource.contains("Text(event.description)"),
            "`Event.description` is Kotlin CustomStringConvertible/toString and leaks repository identities."
        )
    }

    func testArchiveRendersRepositoryFreshnessSyncWarningsAndNoMutationControls() {
        let identifiers = renderIdentifiers(
            EventArchiveView(eventId: "missing-event")
        )

        XCTAssertTrue(identifiers.contains("eventArchivePrimaryAction"), identifiers.description)
        assertContains(
            identifiers,
            [
                "eventArchiveFreshness",
                "eventArchiveSyncWarning",
                "eventArchiveSettledSummary"
            ],
            surface: "Archive"
        )
        XCTAssertTrue(
            identifiers.isDisjoint(with: [
                "eventArchiveVote",
                "eventArchiveInvite",
                "eventArchiveEdit",
                "eventArchiveDelete",
                "eventArchiveNotificationWrite"
            ])
        )
    }

    func testArchiveNeverRendersRepositoryIdentityOrKotlinDebugDump() async throws {
        let event = EventFactory.make(
            id: "private-archive-event-id",
            title: "Séjour finalisé",
            description: "Souvenir destiné aux invités.",
            organizerId: "private-archive-organizer-id",
            participants: ["private-archive-participant-id"],
            status: .finalized,
            finalDate: "2099-01-02T10:00:00Z"
        )
        let repository = RepositoryProvider.shared.databaseRepository
        _ = try await repository.saveEvent(event: event)
        defer {
            Task { try? await repository.deleteEvent(eventId: event.id) }
        }

        let viewModel = EventArchiveViewModel(
            eventId: event.id,
            viewerId: event.organizerId,
            repository: repository
        )
        XCTAssertEqual(viewModel.event?.title, event.title)
        XCTAssertNotEqual(viewModel.organizerDisplayName, event.organizerId)

        let archiveSource = try readProjectFile(
            "iosApp/src/Views/Invitations/EventArchiveView.swift"
        )
        XCTAssertFalse(archiveSource.contains("Text(event.description)"))
        XCTAssertFalse(archiveSource.contains("Text(event)"))
        XCTAssertFalse(archiveSource.contains("value: event.organizerId"))
        XCTAssertFalse(archiveSource.contains("value: event.id"))
        XCTAssertTrue(
            archiveSource.contains("InvitationEventMetadataProjection.localizedDate(for: finalDate)"),
            "Archive must project the settled date as user-facing localized copy."
        )
        XCTAssertFalse(
            archiveSource.contains("value: finalDate"),
            "Archive must never expose the raw ISO-8601 persistence value."
        )
    }

    func testArchiveRejectsAnInteractiveFutureDraftFromRepository() async throws {
        let event = EventFactory.make(
            id: "archive-reject-\(UUID().uuidString)",
            title: "Interactive draft",
            organizerId: "organizer-1",
            deadline: "2099-01-01T00:00:00Z",
            status: .draft
        )
        let repository = RepositoryProvider.shared.databaseRepository
        _ = try await repository.saveEvent(event: event)
        defer {
            Task { try? await repository.deleteEvent(eventId: event.id) }
        }

        let viewModel = EventArchiveViewModel(eventId: event.id, repository: repository)
        XCTAssertNil(
            viewModel.event,
            "Archive must accept only temporal PAST or lifecycle FINALIZED repository projections."
        )
    }

    private func assertContains(
        _ actual: Set<String>,
        _ expected: Set<String>,
        surface: String,
        file: StaticString = #filePath,
        line: UInt = #line
    ) {
        XCTAssertEqual(
            [],
            expected.subtracting(actual).sorted(),
            "\(surface) is missing executable accessibility contracts.",
            file: file,
            line: line
        )
    }

    private func renderIdentifiers<Content: View>(_ content: Content) -> Set<String> {
        render(content).identifiers
    }

    private func render<Content: View>(
        _ content: Content,
        viewport: CGSize = CGSize(width: 390, height: 844)
    ) -> RenderedAccessibility {
        let host = UIHostingController(rootView: content)
        let window = UIWindow(frame: CGRect(origin: .zero, size: viewport))
        window.rootViewController = host
        window.makeKeyAndVisible()
        window.setNeedsLayout()
        window.layoutIfNeeded()
        host.view.setNeedsLayout()
        host.view.layoutIfNeeded()
        RunLoop.main.run(until: Date().addingTimeInterval(0.15))
        retainedWindows.append(window)
        return accessibility(in: host.view)
    }

    private func accessibility(in root: UIView) -> RenderedAccessibility {
        var identifiers = Set<String>()
        var traits: [String: UIAccessibilityTraits] = [:]
        var frames: [String: CGRect] = [:]
        var labels = Set<String>()
        func retainLargestFrame(_ candidate: CGRect, for identifier: String) {
            let currentArea = frames[identifier].map { $0.width * $0.height } ?? 0
            let candidateArea = candidate.width * candidate.height
            if candidateArea > currentArea {
                frames[identifier] = candidate
            }
        }
        var stack = [root]
        while let view = stack.popLast() {
            if let identifier = view.accessibilityIdentifier, !identifier.isEmpty {
                identifiers.insert(identifier)
                traits[identifier] = view.accessibilityTraits
                retainLargestFrame(view.accessibilityFrame, for: identifier)
            }
            if let label = view.accessibilityLabel, !label.isEmpty {
                labels.insert(label)
            }
            if let elements = view.accessibilityElements {
                for element in elements {
                    if let identified = element as? UIAccessibilityIdentification,
                       let identifier = identified.accessibilityIdentifier,
                       !identifier.isEmpty {
                        identifiers.insert(identifier)
                        if let view = element as? UIView {
                            traits[identifier] = view.accessibilityTraits
                            retainLargestFrame(view.accessibilityFrame, for: identifier)
                        } else if let accessibilityElement = element as? UIAccessibilityElement {
                            traits[identifier] = accessibilityElement.accessibilityTraits
                            retainLargestFrame(accessibilityElement.accessibilityFrame, for: identifier)
                        }
                    }
                    if let accessibilityElement = element as? UIAccessibilityElement,
                       let label = accessibilityElement.accessibilityLabel,
                       !label.isEmpty {
                        labels.insert(label)
                    } else if let view = element as? UIView,
                              let label = view.accessibilityLabel,
                              !label.isEmpty {
                        labels.insert(label)
                    }
                }
            }
            stack.append(contentsOf: view.subviews)
        }
        return RenderedAccessibility(
            identifiers: identifiers,
            traits: traits,
            frames: frames,
            labels: labels
        )
    }

    private struct RenderedAccessibility {
        let identifiers: Set<String>
        let traits: [String: UIAccessibilityTraits]
        let frames: [String: CGRect]
        let labels: Set<String>
    }

    private func deleteKeychainItem(service: String, account: String) {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account
        ]
        SecItemDelete(query as CFDictionary)
    }

    private func keychainData(service: String, account: String) -> Data? {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account,
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne
        ]
        var result: CFTypeRef?
        guard SecItemCopyMatching(query as CFDictionary, &result) == errSecSuccess else {
            return nil
        }
        return result as? Data
    }

    private func keychainWriteProbe(service: String, account: String) -> OSStatus {
        deleteKeychainItem(service: service, account: account)
        defer { deleteKeychainItem(service: service, account: account) }
        let item: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account,
            kSecValueData as String: Data(repeating: 7, count: 32),
            kSecAttrAccessible as String: kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
        ]
        return SecItemAdd(item as CFDictionary, nil)
    }

    private func readProjectFile(_ path: String) throws -> String {
        let repositoryRoot = URL(fileURLWithPath: #filePath)
            .deletingLastPathComponent()
            .deletingLastPathComponent()
            .deletingLastPathComponent()
        return try String(
            contentsOf: repositoryRoot.appendingPathComponent(path),
            encoding: .utf8
        )
    }
}

private final class TestDirectInviteRecipientDigestPort: NSObject, DirectInviteRecipientDigestPort {
    func hmacSha256(normalizedRecipient: String) -> String? {
        String(repeating: "a", count: 64)
    }
}

private final class TestDirectInviteDeliverySealer: NSObject, DirectInviteDeliverySealer {
    func seal(
        binding: DirectInviteDeliveryBinding,
        recipientKey: RecipientKey,
        normalizedRecipient: String,
        expiresAt: String
    ) -> DirectInviteDeliveryEnvelope? {
        DirectInviteDeliveryEnvelope(
            binding: binding,
            recipientKey: recipientKey,
            ciphertext: "test-ciphertext-\(recipientKey.value)",
            keyVersion: 1,
            expiresAt: expiresAt
        )
    }
}

private final class TestDirectInviteDeliveryTransport: NSObject, DirectInviteDeliveryTransport {
    func dispatch(
        request: DirectInviteDeliveryRequest
    ) async throws -> any DirectInviteDeliveryResult {
        var outcomes: [RecipientKey: any DirectInviteRecipientOutcome] = [:]
        for envelope in request.envelopes {
            outcomes[envelope.recipientKey] = DirectInviteRecipientOutcomeServerAccepted(
                invitationId: "test-invitation-\(envelope.recipientKey.value)"
            )
        }
        return DirectInviteDeliveryResultAcknowledged(
            batchId: request.binding.batchId,
            operationId: request.binding.operationId,
            outcomesByRecipientKey: outcomes
        )
    }
}
