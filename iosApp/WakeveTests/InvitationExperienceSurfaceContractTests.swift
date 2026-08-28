import XCTest
@testable import Wakeve

final class InvitationExperienceSurfaceContractTests: XCTestCase {
    private let surfacePaths = [
        "iosApp/src/Views/Invitations/EventLibraryView.swift",
        "iosApp/src/Views/Invitations/EventCreationStudioView.swift",
        "iosApp/src/Views/Invitations/EventAudienceView.swift",
        "iosApp/src/Views/Invitations/EventInformationView.swift",
        "iosApp/src/Views/Invitations/EventArchiveView.swift"
    ]

    func testSixRepositoryBackedSurfacesAreInstalled() throws {
        let expectedTypes = [
            "struct EventLibraryView",
            "struct EventCreationStudioView",
            "struct EventAudienceView",
            "struct EventInformationView",
            "struct EventArchiveView"
        ]

        for (path, type) in zip(surfacePaths, expectedTypes) {
            let source = readProjectFileIfPresent(path)
            XCTAssertTrue(source.contains(type), "Missing release-1 surface \(type) at \(path).")
            XCTAssertTrue(
                source.contains("ViewModel") || source.contains("repository"),
                "\(type) must render repository-backed state rather than fixture data."
            )
        }

        let detail = try readProjectFile("iosApp/src/Views/App/ContentView.swift")
        XCTAssertTrue(detail.contains("EventDetailInvitationCanvas("))
    }

    func testProductionRootActuallyRoutesToEveryInvitationSurfaceThroughTypedPreflight() throws {
        let root = try readProjectFile("iosApp/src/Views/App/ContentView.swift")

        for reachableSurface in [
            "EventLibraryView(",
            "EventCreationStudioView(",
            "EventAudienceView(",
            "EventInformationView(",
            "EventArchiveView("
        ] {
            XCTAssertTrue(
                root.contains(reachableSurface),
                "A declared invitation view is not a shipped surface until the authenticated production root can route to \(reachableSurface)."
            )
        }
        XCTAssertTrue(
            root.contains("InvitationExperienceRouter"),
            "Canvas actions and deep links must pass through the typed PAST/FINALIZED preflight before owner callbacks."
        )
    }

    func testProductionRootPreflightsEventAndPollDeepLinksBeforeAnyOwnerCallback() throws {
        let root = try readProjectFile("iosApp/src/Views/App/ContentView.swift")
        let deepLinkSwitch = sourceSlice(
            root,
            from: "private func handleDeepLinkNavigation",
            to: "private func navigateToEvent"
        )
        let protectedBranches = [
            sourceSlice(
                deepLinkSwitch,
                from: "case .event(.detail(let eventId)):",
                to: "case .event(.pollVoting(let eventId)):"
            ),
            sourceSlice(
                deepLinkSwitch,
                from: "case .event(.pollVoting(let eventId)):",
                to: "case .event(.pollResults(let eventId)):"
            ),
            sourceSlice(
                deepLinkSwitch,
                from: "case .event(.pollResults(let eventId)):",
                to: "case .event(.participants(let eventId)):"
            )
        ]

        for branch in protectedBranches {
            XCTAssertFalse(branch.isEmpty, "Unable to inspect an event/poll deep-link branch.")
            XCTAssertTrue(
                branch.contains("navigateInvitationDeepLink(") ||
                    branch.contains("routeInvitationExperience("),
                "Every event/poll deep link must derive membership and PAST/FINALIZED policy through the typed router."
            )
            XCTAssertFalse(
                branch.contains("navigateToEvent("),
                "A direct AppView assignment bypasses typed access and temporal preflight."
            )
        }
    }

    func testEveryShippedEventDeepLinkUsesTheTypedInvitationRouter() throws {
        let root = try readProjectFile("iosApp/src/Views/App/ContentView.swift")
        let deepLinkSwitch = sourceSlice(
            root,
            from: "private func handleDeepLinkNavigation",
            to: "private func navigateToEvent"
        )
        let eventBranches = sourceSlice(
            deepLinkSwitch,
            from: "case .event(.detail(let eventId)):",
            to: "case .meetingDetail(let meetingId):"
        )

        XCTAssertFalse(eventBranches.isEmpty, "Unable to inspect the global event deep-link branches.")
        XCTAssertFalse(
            eventBranches.contains("navigateToEvent(eventId:"),
            "Scenario, budget, meeting, invite, transport, accommodation, meal, equipment, activity, payment, photo, and comment links must not bypass temporal/membership preflight."
        )
    }

    func testCanvasActionsIncludingEditDraftReturnToTheTypedRootRouter() throws {
        let root = try readProjectFile("iosApp/src/Views/App/ContentView.swift")
        let detailInstallation = sourceSlice(
            root,
            from: "EventDetailView(",
            to: "case .eventAudience:"
        )
        let canvasDispatch = sourceSlice(
            root,
            from: "private func performCanvasAction(",
            to: "private func scrollToProgressiveDetails"
        )

        XCTAssertTrue(
            detailInstallation.contains("onCanvasAction:"),
            "The canvas must return its typed action to AuthenticatedView instead of choosing owners inside EventDetailView."
        )
        XCTAssertTrue(
            detailInstallation.contains("InvitationExperienceRouteRequestCanvasAction"),
            "The installed canvas callback must resolve through InvitationExperienceRouter before navigation."
        )
        XCTAssertFalse(
            canvasDispatch.contains("case .editDraft:\n            scrollToProgressiveDetails"),
            "EDIT_DRAFT currently scrolls local details instead of opening the guarded draft editor destination."
        )
    }

    func testProductionRootInstallsRealOwnersInsteadOfNoOpInvitationCallbacks() throws {
        let root = try readProjectFile("iosApp/src/Views/App/ContentView.swift")

        for noOp in [
            "onRequestPreview: { _, _ in }",
            "directInviteCapability: DirectInviteCapabilityHidden.shared",
            "onInvite: {}"
        ] {
            XCTAssertFalse(
                root.contains(noOp),
                "The production root still installs a visible invitation surface with a no-op owner: \(noOp)"
            )
        }

        guard let informationStart = root.range(of: "EventInformationView("),
              let archiveStart = root.range(
                of: "case .eventArchive:",
                range: informationStart.upperBound..<root.endIndex
              )
        else {
            XCTFail("Unable to locate the production Information route block.")
            return
        }
        let informationRoute = String(root[informationStart.lowerBound..<archiveStart.lowerBound])
        for owner in [
            "onOpenNotificationOwner:",
            "onOpenCalendar:",
            "onOpenMaps:",
            "onOpenWeather:",
            "onLeave:",
            "onDelete:"
        ] {
            XCTAssertTrue(
                informationRoute.contains(owner),
                "Event Information exposes \(owner) but the production root never installs that owner."
            )
        }
    }

    func testStudioPreviewConfirmationCommitsThroughTheTypedOwnerBeforeDismissing() throws {
        let root = try readProjectFile("iosApp/src/Views/App/ContentView.swift")
        let previewModel = sourceSlice(
            root,
            from: "private struct InvitationStudioPreview: Identifiable",
            to: "private struct InvitationStudioPreviewSheet: View"
        )
        let previewSheet = sourceSlice(
            root,
            from: "private struct InvitationStudioPreviewSheet: View",
            to: "private struct"
        )

        XCTAssertTrue(
            previewModel.contains("confirmCommit"),
            "The preview currently keeps only display copy, so the Studio commit owner becomes unreachable from the shipped sheet."
        )
        XCTAssertTrue(
            previewSheet.contains("confirmCommit"),
            "The preview confirmation must invoke the typed Studio commit owner."
        )
        XCTAssertFalse(
            previewSheet.contains("Button(String(localized: \"common.done\")) { dismiss() }"),
            "Done must not dismiss a NEW/EDIT preview while silently discarding the draft."
        )
    }

    func testEveryPrimarySurfaceExposesAtMostOneStablePrimaryAction() {
        let identifiers = [
            "eventLibraryPrimaryAction",
            "eventCreationStudioPrimaryAction",
            "eventAudiencePrimaryAction",
            "eventInformationPrimaryAction",
            "eventArchivePrimaryAction"
        ]

        for (path, identifier) in zip(surfacePaths, identifiers) {
            let source = readProjectFileIfPresent(path)
            XCTAssertEqual(
                occurrences(of: identifier, in: source),
                1,
                "\(path) must expose one stable primary action element, never duplicated by adaptive layout."
            )
        }
    }

    func testLibraryRendersTypedArtworkAndKeepsCancellableFailureState() throws {
        let library = try readProjectFile(
            "iosApp/src/Views/Invitations/EventLibraryView.swift"
        )

        XCTAssertTrue(
            library.contains("card.artwork"),
            "Repository projections carry total artwork, but the shipped Library card never renders it."
        )
        XCTAssertTrue(
            library.contains("LibraryLoadState"),
            "Library must retain the typed Idle/Loading/Ready/Empty/Failed state instead of collapsing every failure to an empty card array."
        )
        XCTAssertTrue(
            library.contains("cancelLoad"),
            "Cancelling a filter reload must restore PreviousStableState exactly."
        )
        XCTAssertTrue(
            library.contains("eventLibraryRetryAction"),
            "A repository failure needs an accessible retry action distinct from an honest empty Library."
        )
    }

    func testAudienceLoadsPersistedBatchOutcomesAndNamesEveryAxisDistinctly() throws {
        let audience = try readProjectFile(
            "iosApp/src/Views/Invitations/EventAudienceView.swift"
        )

        XCTAssertTrue(
            audience.contains("DatabaseDirectInviteBatchRepository"),
            "Audience delivery/outcome axes must come from the persisted direct-invite owner, not participant rows alone."
        )
        XCTAssertTrue(
            audience.contains("DirectInviteRecipientOutcome"),
            "Per-recipient accepted/invalid/failed/cancelled outcomes are not projected by the shipped Audience."
        )
        for key in [
            "invitation.audience.axis.delivery",
            "invitation.audience.axis.approval",
            "invitation.audience.axis.membership",
            "invitation.audience.axis.rsvp",
            "invitation.audience.axis.date_validation"
        ] {
            XCTAssertTrue(
                audience.contains(key),
                "Each independent Audience axis needs distinct visible and VoiceOver copy: \(key)."
            )
        }
        XCTAssertLessThanOrEqual(
            occurrences(
                of: "audienceAxis(\n                String(localized: \"invitation.audience.title\")",
                in: audience
            ),
            1,
            "All five axes currently announce the same generic title."
        )
    }

    func testAudienceSubmitsProtectedRecipientsThroughTheDirectInviteOwner() throws {
        let audience = try readProjectFile(
            "iosApp/src/Views/Invitations/EventAudienceView.swift"
        )
        let root = try readProjectFile("iosApp/src/Views/App/ContentView.swift")
        let installation = sourceSlice(
            root,
            from: "case .eventAudience:",
            to: "case .eventInformation:"
        )

        XCTAssertTrue(
            audience.contains("DirectInviteRecipientKeyOwner"),
            "Audience must pass transient recipient input through the trusted protected-key owner."
        )
        XCTAssertTrue(
            audience.contains("SubmitDirectInviteBatchCommand") &&
                audience.contains("directInviteRepository.submit"),
            "The visible direct-invite action must submit one typed owner command and render its persisted outcomes."
        )
        XCTAssertFalse(
            installation.contains("directInviteAvailable: false"),
            "The production root currently disables direct invite even when it installs a matching ready capability."
        )
        XCTAssertTrue(
            installation.contains("recipientKeyOwner:"),
            "The production Audience route must inject its trusted recipient-key owner; the ViewModel default is nil and otherwise every visible submit fails closed."
        )
        XCTAssertFalse(
            installation.contains("currentView = .participantManagement"),
            "Audience direct invite must not bypass batch persistence by routing to the legacy participant writer."
        )
    }

    func testAudienceRecipientDigestUsesRandomKeychainBackedHmacWithoutFallbackMaterial() throws {
        let adapter = try readProjectFile(
            "iosApp/src/Services/DirectInviteRecipientKeyOwnerProvider.swift"
        )

        for contract in [
            "import CryptoKit",
            "import Security",
            "SecRandomCopyBytes",
            "SecItemCopyMatching",
            "SecItemAdd",
            "HMAC<SHA256>",
            "kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly"
        ] {
            XCTAssertTrue(
                adapter.contains(contract),
                "The iOS recipient digest boundary must use a random device-protected Keychain secret and keyed HMAC: missing \(contract)."
            )
        }
        let digestImplementation = sourceSlice(
            adapter,
            from: "func hmacSha256",
            to: "private static func loadOrCreateSecret"
        )
        XCTAssertFalse(
            digestImplementation.contains("SHA256.hash(data:"),
            "A plain, non-keyed SHA-256 digest does not protect low-entropy recipient identities."
        )
        XCTAssertFalse(
            adapter.contains("String(repeating:"),
            "Production must never fall back to deterministic test digest or hardcoded key material."
        )
    }

    func testInformationUsesDistinctProviderLabelsAndWaitsForDeleteOwnerResult() throws {
        let information = try readProjectFile(
            "iosApp/src/Views/Invitations/EventInformationView.swift"
        )
        let root = try readProjectFile("iosApp/src/Views/App/ContentView.swift")
        let deleteOwner = sourceSlice(
            root,
            from: "private func deleteInformationEventThroughOwner",
            to: "private func handleDeepLinkNavigation"
        )

        for key in [
            "invitation.information.destination.calendar",
            "invitation.information.destination.maps",
            "invitation.information.destination.weather"
        ] {
            XCTAssertTrue(
                information.contains(key),
                "Calendar, Maps, and Weather must have distinct visible and VoiceOver labels: \(key)."
            )
        }
        XCTAssertTrue(
            deleteOwner.contains("await"),
            "Information delete must wait for the guarded owner result before changing navigation state."
        )
        XCTAssertFalse(
            deleteOwner.contains("owner.deleteEvent()\n        selectedEvent = nil"),
            "The UI currently reports deletion success before the owner completes or fails."
        )
    }

    func testInformationReadsSystemNotificationAuthorizationThroughANonPromptingInjectedPort() throws {
        let adapter = try readProjectFile(
            "iosApp/src/Services/EventInformationSystemAuthorizationAdapter.swift"
        )
        let information = try readProjectFile(
            "iosApp/src/Views/Invitations/EventInformationView.swift"
        )

        XCTAssertTrue(
            adapter.contains("UNUserNotificationCenter"),
            "The iOS Information axis must read the real system notification settings."
        )
        XCTAssertTrue(
            adapter.contains("getNotificationSettings") || adapter.contains("notificationSettings()"),
            "Reading the Information axis must use UNUserNotificationCenter settings rather than a cached account flag."
        )
        for status in [".authorized", ".denied", ".notDetermined", ".provisional", ".ephemeral"] {
            XCTAssertTrue(
                adapter.contains(status),
                "The adapter must map the complete iOS authorization model: \(status)."
            )
        }
        for forbiddenEffect in ["requestAuthorization", "registerForRemoteNotifications", "openSettings"] {
            XCTAssertFalse(
                adapter.contains(forbiddenEffect),
                "Information reload is read-only and must never perform \(forbiddenEffect)."
            )
        }
        XCTAssertTrue(
            information.contains("systemAuthorizationReader"),
            "EventInformationViewModel must receive the system reader as an injected port."
        )
        XCTAssertTrue(
            information.contains("await systemAuthorizationReader"),
            "Every Information reload must refresh the OS axis through the injected reader."
        )
        XCTAssertFalse(
            information.contains("requestAuthorization"),
            "Reloading Information must not turn a read into a permission prompt."
        )
        let reload = sourceSlice(
            information,
            from: "func reload() async",
            to: "private func updateOrganizerDisplayName"
        )
        for forbiddenWrite in ["save(", "upsert", "requestAuthorization", "openSettings"] {
            XCTAssertFalse(
                reload.contains(forbiddenWrite),
                "Refreshing the OS axis must not mutate account preferences or system permission: \(forbiddenWrite)."
            )
        }
    }

    func testArchiveRendersTheTotalRepositoryArtworkProjection() throws {
        let archive = try readProjectFile(
            "iosApp/src/Views/Invitations/EventArchiveView.swift"
        )

        XCTAssertTrue(
            archive.contains("snapshot.artwork"),
            "Archive receives NONE/STRUCTURED/LEGACY_REMOTE artwork but replaces it with an event-type gradient."
        )
    }

    func testSurfacesExposeAccessibilityAndAdaptiveLayoutContract() {
        let combined = surfacePaths.map(readProjectFileIfPresent).joined(separator: "\n")

        for contract in [
            "dynamicTypeSize",
            "verticalSizeClass",
            "accessibilityReduceMotion",
            "accessibilityReduceTransparency",
            "colorSchemeContrast",
            "accessibilitySortPriority",
            "frame(minWidth: 44, minHeight: 44)"
        ] {
            XCTAssertTrue(combined.contains(contract), "Missing invitation-experience accessibility contract: \(contract)")
        }
    }

    func testNewSurfacesContainNoLocalCredentialPassOrRedeemSubstitute() throws {
        let canvas = try readProjectFile("iosApp/src/Views/Events/EventDetailInvitationCanvas.swift")
        let combined = (surfacePaths.map(readProjectFileIfPresent) + [canvas]).joined(separator: "\n")

        for forbidden in [
            "import PassKit",
            "InvitationTokenCodec",
            "wakeve.app/invite/",
            "CIQRCodeGenerator",
            "QRCode",
            "PhotosPicker",
            "PHPickerViewController",
            "UIImagePickerController",
            "fileImporter(",
            "uploadArtwork",
            "redeemInvitation(",
            "acceptInvitation("
        ] {
            XCTAssertFalse(
                combined.contains(forbidden),
                "The six new surfaces must not own local invitation credentials, passes, QR, or redeem: \(forbidden)"
            )
        }
    }

    func testCanvasReadyShareCapabilityRetainsAndRevalidatesEveryBindingDimension() throws {
        let canvas = try readProjectFile("iosApp/src/Views/Events/EventDetailInvitationCanvas.swift")
        let capability = sourceSlice(
            canvas,
            from: "enum EventDetailInvitationShareCapability",
            to: "enum EventDetailInvitationCanvasLifecycleTone"
        )
        let filter = sourceSlice(
            canvas,
            from: "private func filteredShareCapability",
            to: "private func syncDecoration"
        )

        XCTAssertTrue(
            capability.contains("EventDetailInvitationShareBinding"),
            "A ready server payload must retain its event/actor/revision/capability binding even while production sharing is hidden."
        )
        for dimension in ["eventId", "actorId", "accessRevision", "capabilityId"] {
            XCTAssertTrue(
                canvas.contains("let \(dimension)"),
                "The Swift share binding drops \(dimension)."
            )
            XCTAssertTrue(
                filter.contains(dimension),
                "Share-time preflight never revalidates \(dimension)."
            )
        }
        XCTAssertTrue(capability.contains("serverIssuedPayload"))
    }

    func testInformationAndArchiveDoNotExposeTechnicalMetadataOrOpenAccountSettingsForEventWrites() throws {
        let information = try readProjectFile(
            "iosApp/src/Views/Invitations/EventInformationView.swift"
        )
        let archive = try readProjectFile(
            "iosApp/src/Views/Invitations/EventArchiveView.swift"
        )
        let root = try readProjectFile("iosApp/src/Views/App/ContentView.swift")
        let informationRoute = sourceSlice(
            root,
            from: "case .eventInformation:",
            to: "case .eventArchive:"
        )
        let visibleMetadata = information + "\n" + archive

        for technicalCopy in [
            "value: event.status.name",
            "value: event.organizerId"
        ] {
            XCTAssertFalse(
                visibleMetadata.contains(technicalCopy),
                "Invitation surfaces must localize/project user-facing metadata instead of exposing \(technicalCopy)."
            )
        }
        XCTAssertFalse(
            informationRoute.contains("showNotificationPreferencesSheet = true"),
            "The event-preference action must not silently open account-wide notification settings."
        )
        XCTAssertTrue(
            root.contains("DatabaseEventNotificationPreferenceRepository"),
            "The production Information route must install the event-scoped notification preference owner."
        )
        XCTAssertTrue(
            information.contains("eventPreferenceRecord"),
            "Visible event notification state must come from the typed Information snapshot."
        )
    }

    func testStudioCloseDetachesInFlightCommitAndKeepsUnknownOutcomesDistinct() throws {
        let studio = try readProjectFile(
            "iosApp/src/Views/Invitations/EventCreationStudioView.swift"
        )
        let viewModel = sourceSlice(
            studio,
            from: "final class EventCreationStudioViewModel",
            to: "struct EventCreationStudioView"
        )
        let view = sourceSlice(
            studio,
            from: "struct EventCreationStudioView",
            to: "private func artworkButton"
        )
        let shared = try readProjectFile(
            "shared/src/commonMain/kotlin/com/guyghost/wakeve/invitationexperience/InvitationExperiencePublicContracts.kt"
        )

        XCTAssertTrue(viewModel.contains("CreationStudioEventClose"))
        XCTAssertTrue(
            viewModel.contains("CreationStudioStateDetachedCommitting") &&
                viewModel.contains("CreationStudioStateDetachedResolving"),
            "Closing during a commit must detach presentation without cancelling durable work."
        )
        XCTAssertTrue(
            viewModel.contains("CreationStudioEventOutcomeUnknown") &&
                viewModel.contains("CreationStudioEventResolutionResult"),
            "Initial commit uncertainty and resolution-attempt uncertainty are disjoint modeled events."
        )
        XCTAssertTrue(
            viewModel.contains("CreationStudioEventLateLocalCommit") &&
                shared.contains("data class DetachedPendingSync") &&
                shared.contains("val binding: CreationStudioSyncBinding"),
            "A late local PENDING_SYNC commit must preserve its durable binding after the sheet closes."
        )
        XCTAssertTrue(
            view.contains("viewModel.close") || view.contains("viewModel.consumeClose"),
            "The Cancel control must be inert outside valid states and consumed by the reducer while work is in flight."
        )
        XCTAssertFalse(
            view.contains("Button(String(localized: \"common.cancel\"), action: onCancel)"),
            "The presentation must not bypass the commit reducer when closing."
        )
    }

    func testStudioCarriesDurableCommitEnvelopeAndBoundsCorrelatedUnknownResolution() throws {
        let studio = try readProjectFile(
            "iosApp/src/Views/Invitations/EventCreationStudioView.swift"
        )
        let shared = try readProjectFile(
            "shared/src/commonMain/kotlin/com/guyghost/wakeve/invitationexperience/InvitationExperiencePublicContracts.kt"
        )
        let combined = shared + "\n" + studio

        for field in [
            "durableOperationRef",
            "requestFingerprint",
            "resolutionRetryBudget",
            "attemptId",
            "fence"
        ] {
            XCTAssertTrue(
                combined.contains(field),
                "Studio commit/resolution is missing the durable correlation field \(field)."
            )
        }
        XCTAssertTrue(
            combined.contains("MAX_RESOLUTION_ATTEMPTS") || combined.contains("resolutionRetryBudget > 0"),
            "COMMIT_OUTCOME_UNKNOWN may start only a bounded number of resolution attempts."
        )
        XCTAssertTrue(
            studio.contains("CreationStudioEventOutcomeUnknown") &&
                studio.contains("CreationStudioEventResolutionResult"),
            "A thrown database call must remain UNKNOWN until a correlated attempt proves its outcome."
        )
        let executeCommit = sourceSlice(
            studio,
            from: "private func executeCommit() async -> Bool",
            to: "private func beginResolutionAttempt"
        )
        let commitCatch = executeCommit.components(separatedBy: "} catch {").last ?? ""
        XCTAssertTrue(
            commitCatch.contains("CreationStudioEventOutcomeUnknown"),
            "The database exception path must enter unknown-outcome resolution."
        )
        XCTAssertFalse(
            commitCatch.contains("CreationStudioEventFailBeforeLocalCommit"),
            "An unclassified database exception is not proof that the local transaction did not commit."
        )
    }

    func testStudioCorruptRehydrationBecomesRepositoryInconsistentUnknownAndNeverRestoresEditing() throws {
        let studio = try readProjectFile(
            "iosApp/src/Views/Invitations/EventCreationStudioView.swift"
        )
        let restore = sourceSlice(
            studio,
            from: "private func restorePersistedSync() async",
            to: "func cancelSyncObservation()"
        )

        XCTAssertTrue(
            restore.contains("REPOSITORY_INCONSISTENT") ||
                restore.contains("repositoryInconsistent"),
            "Missing or divergent commitEnvelope/pendingSubject data must surface the stable repository inconsistency code."
        )
        XCTAssertTrue(
            restore.contains("COMMIT_OUTCOME_UNKNOWN") ||
                restore.contains("commitOutcomeUnknown") ||
                restore.contains("CreationStudioEventOutcomeUnknown"),
            "Corrupt durable Studio state has UNKNOWN commit outcome and cannot fall back to an editable draft."
        )
        XCTAssertTrue(
            restore.contains("consumeSyncFailure") || restore.contains("stateMachine.transition"),
            "Every corrupt rehydration branch must explicitly enter the blocked terminal projection."
        )
        XCTAssertFalse(
            restore.contains(
                "operationId: receipt.operation_id\n        ) else { return }"
            ),
            "A failed durable binding/envelope load must not silently return and leave the editor enabled."
        )
    }

    func testStudioUnknownResolutionCorrelatesInnerEnvelopeAndPendingSubjectAndForbidsBlankReplay() throws {
        let studio = try readProjectFile(
            "iosApp/src/Views/Invitations/EventCreationStudioView.swift"
        )
        let shared = try readProjectFile(
            "shared/src/commonMain/kotlin/com/guyghost/wakeve/invitationexperience/InvitationExperiencePublicContracts.kt"
        )
        let resolve = sourceSlice(
            studio,
            from: "private func resolveUnknownCommit",
            to: "private func observeSync"
        )
        let replay = sourceSlice(
            shared,
            from: "if (receipt != null)",
            to: "var event = database.eventQueries"
        )

        XCTAssertTrue(
            resolve.contains("commit_envelope") &&
                (resolve.contains("StudioCommitEnvelope") || resolve.contains("loadCommitEnvelope")),
            "UNKNOWN resolution must decode the persisted inner commit envelope before proving local commit."
        )
        XCTAssertTrue(
            resolve.contains("syncMetadataQueries") &&
                resolve.contains("StudioPendingSyncSubject"),
            "UNKNOWN resolution must decode and correlate the exact pending Studio subject."
        )
        XCTAssertTrue(
            resolve.contains("repositoryInconsistent") ||
                resolve.contains("REPOSITORY_INCONSISTENT"),
            "Missing, malformed, or divergent inner records are repository inconsistency, never localCommitted."
        )
        XCTAssertFalse(
            replay.contains("receipt.commit_envelope.isBlank()") ||
                replay.contains("receipt.durable_operation_ref.isBlank()"),
            "A blank legacy inner record cannot be accepted as a valid idempotent Studio replay."
        )
    }

    func testStudioTerminalRetryabilityCrossesClientStateAndBlocksRetrySync() throws {
        let studio = try readProjectFile(
            "iosApp/src/Views/Invitations/EventCreationStudioView.swift"
        )
        let contracts = try readProjectFile(
            "shared/src/commonMain/kotlin/com/guyghost/wakeve/invitationexperience/InvitationExperiencePublicContracts.kt"
        )
        let syncManager = try readProjectFile(
            "shared/src/commonMain/kotlin/com/guyghost/wakeve/sync/SyncManager.kt"
        )
        let terminalCodes = sourceSlice(
            syncManager,
            from: "STUDIO_TERMINAL_REJECTION_CODES = setOf(",
            to: ")\n\ninternal fun syncFailureMessage"
        )
        let failedState = sourceSlice(
            contracts,
            from: "data class SyncFailed(",
            to: "data class Completed("
        )
        let retryReducer = sourceSlice(
            contracts,
            from: "private fun retrySync(",
            to: "private fun retryBeforeCommit("
        )
        let retryProjection = sourceSlice(
            studio,
            from: "var retryAvailable: Bool",
            to: "var isPendingSync: Bool"
        )

        for code in ["IDEMPOTENCY_CONFLICT", "REPOSITORY_INCONSISTENT"] {
            XCTAssertTrue(terminalCodes.contains("\"\(code)\""), "Missing terminal Studio code \(code).")
        }
        XCTAssertTrue(
            failedState.contains("retryable"),
            "The correlated server retryability flag must survive in CreationStudioState.SyncFailed."
        )
        XCTAssertTrue(
            retryReducer.contains("failed.retryable") &&
                (retryReducer.contains("!failed.retryable") || retryReducer.contains("failed.retryable == false")),
            "RetrySync must be rejected by the reducer for a terminal non-retryable Studio failure."
        )
        XCTAssertTrue(
            retryProjection.contains("retryable"),
            "The Swift retry affordance must project the modeled retryability flag, not every SyncFailed state."
        )
    }

    func testStudioCorruptInnerResolutionIsTerminalRepositoryInconsistentAndNeverDetachedCommitting() throws {
        let studio = try readProjectFile(
            "iosApp/src/Views/Invitations/EventCreationStudioView.swift"
        )
        let resolve = sourceSlice(
            studio,
            from: "private func resolveUnknownCommit",
            to: "private func observeSync"
        )
        let corruptInnerBranch = resolve.components(separatedBy: "} else {").last ?? ""

        XCTAssertTrue(
            corruptInnerBranch.contains("repositoryInconsistent") ||
                corruptInnerBranch.contains("REPOSITORY_INCONSISTENT")
        )
        XCTAssertTrue(
            corruptInnerBranch.contains("retryable: false") &&
                (corruptInnerBranch.contains("CreationStudioStateDetachedResolutionFailed") ||
                    corruptInnerBranch.contains("CreationStudioStateSyncFailed") ||
                    corruptInnerBranch.contains("CreationStudioEventRepositoryInconsistent")),
            "Malformed or divergent inner Studio data must enter a terminal non-retryable inconsistency state."
        )
        XCTAssertFalse(
            corruptInnerBranch.contains("outcome: .unknown") ||
                corruptInnerBranch.contains("CreationStudioEventOutcomeUnknown"),
            "Repository corruption is not an unresolved transport outcome and must never recreate DetachedCommitting."
        )
    }

    func testDetachedPendingStudioRestoreRetainsBindingProjectsPendingAndTriggersImmediateSync() throws {
        let studio = try readProjectFile(
            "iosApp/src/Views/Invitations/EventCreationStudioView.swift"
        )
        let restore = sourceSlice(
            studio,
            from: "private func restorePersistedSync() async",
            to: "func cancelSyncObservation()"
        )
        let pendingProjection = sourceSlice(
            studio,
            from: "var isPendingSync: Bool",
            to: "var primaryActionAvailable: Bool"
        )

        XCTAssertTrue(
            restore.contains("CreationStudioStateDetachedPendingSync"),
            "A detached local PENDING_SYNC receipt must restore as DETACHED_PENDING_SYNC, not as attached terminal commit."
        )
        XCTAssertTrue(
            restore.contains("binding:") || restore.contains("syncBinding:"),
            "Detached restoration must retain the exact durable binding that owns retries and ACK correlation."
        )
        XCTAssertTrue(
            pendingProjection.contains("CreationStudioStateDetachedPendingSync"),
            "DETACHED_PENDING_SYNC must remain visible through isPendingSync."
        )
        XCTAssertTrue(
            restore.contains("observeSync(binding)") ||
                restore.contains("triggerSyncImmediately(binding)") ||
                restore.contains("syncOwner.retry(binding: binding)"),
            "Restoring the detached receipt must immediately and idempotently trigger its typed sync subject."
        )
    }

    func testPendingStudioBindingCorruptionBecomesTerminalRepositoryInconsistentUnknown() throws {
        let studio = try readProjectFile(
            "iosApp/src/Views/Invitations/EventCreationStudioView.swift"
        )
        let execute = sourceSlice(
            studio,
            from: "private func executeCommit() async",
            to: "private func beginResolutionAttempt"
        )
        let pendingBindingLoad = sourceSlice(
            execute,
            from: "if committed.pendingSync {",
            to: "return studioState is"
        )
        let retryBindingLoad = sourceSlice(
            studio,
            from: "else if let failed = studioState as? CreationStudioStateSyncFailed",
            to: "} else if let detached = studioState as? CreationStudioStateDetachedCommitting"
        )
        let resultConsumption = sourceSlice(
            studio,
            from: "private func consumeSyncResult",
            to: "private func consumeSyncFailure"
        )

        XCTAssertFalse(
            pendingBindingLoad.contains("else { return false }"),
            "A committed PendingSync with a missing/malformed binding cannot silently return and remain pending."
        )
        XCTAssertTrue(
            pendingBindingLoad.contains("repositoryInconsistent") ||
                (pendingBindingLoad.contains("commitOutcomeUnknown") &&
                    pendingBindingLoad.contains("retryable: false")),
            "The first failed binding load must project terminal REPOSITORY_INCONSISTENT / UNKNOWN."
        )
        XCTAssertFalse(
            retryBindingLoad.contains("else { return }"),
            "Retry must not silently abandon a missing or corrupt durable binding."
        )
        XCTAssertTrue(
            retryBindingLoad.contains("repositoryInconsistent") ||
                (retryBindingLoad.contains("commitOutcomeUnknown") &&
                    retryBindingLoad.contains("retryable: false")),
            "A retry-time binding failure must become the same terminal inconsistency."
        )
        XCTAssertTrue(
            resultConsumption.contains("failed.code") &&
                resultConsumption.contains("failed.commitOutcome"),
            "DetachedPendingSync must preserve the typed REPOSITORY_INCONSISTENT and UNKNOWN proof returned by KMP."
        )
    }

    func testStudioEnvelopeAndPendingSubjectFingerprintExpectedResultingArtworkForKeepExisting() throws {
        let shared = try readProjectFile(
            "shared/src/commonMain/kotlin/com/guyghost/wakeve/invitationexperience/InvitationExperiencePublicContracts.kt"
        )
        let studio = try readProjectFile(
            "iosApp/src/Views/Invitations/EventCreationStudioView.swift"
        )
        let envelope = sourceSlice(
            shared,
            from: "data class StudioCommitEnvelope(",
            to: "data class StudioPendingSyncSubject("
        )
        let pendingSubject = sourceSlice(
            shared,
            from: "data class StudioPendingSyncSubject(",
            to: "enum class StudioSyncOutcome"
        )
        let commandConstruction = sourceSlice(
            studio,
            from: "private func makeCommitCommand",
            to: "private func executeCommit"
        )

        XCTAssertTrue(
            envelope.contains("expectedResultingArtwork") &&
                pendingSubject.contains("expectedResultingArtwork"),
            "The KEEP_EXISTING snapshot result must be repeated in the fingerprinted envelope and pending subject."
        )
        XCTAssertTrue(
            commandConstruction.contains("expectedResultingArtwork") &&
                (commandConstruction.contains("existingArtwork") ||
                    commandConstruction.contains("resolvedArtwork")),
            "Swift must bind KEEP_EXISTING to the resolved pre-commit artwork snapshot, not a later mutable aggregate."
        )
    }

    func testNewSurfacesUseTypedOwnersAndNoConstructionPlaceholder() {
        let combined = surfacePaths.map(readProjectFileIfPresent).joined(separator: "\n")

        for expectedOwner in [
            "EventLibraryProjector",
            "CreationStudioStateMachine",
            "AudienceProjector",
            "EventNotificationPolicy",
            "InvitationExperienceRouter"
        ] {
            XCTAssertTrue(combined.contains(expectedOwner), "Missing typed owner bridge: \(expectedOwner)")
        }

        for forbidden in [
            "Coming soon",
            "Bientôt disponible",
            "navigation.placeholder",
            "updateEventStatus(",
            "InvitationTokenCodec",
            "Text(identity.identityKey)"
        ] {
            XCTAssertFalse(combined.contains(forbidden), "New surfaces must not contain placeholder or direct workflow logic: \(forbidden)")
        }
    }

    private func occurrences(of needle: String, in haystack: String) -> Int {
        guard !needle.isEmpty else { return 0 }
        return haystack.components(separatedBy: needle).count - 1
    }

    private func sourceSlice(_ source: String, from start: String, to end: String) -> String {
        guard let startRange = source.range(of: start),
              let endRange = source.range(of: end, range: startRange.upperBound..<source.endIndex)
        else {
            return ""
        }
        return String(source[startRange.lowerBound..<endRange.lowerBound])
    }

    private func readProjectFileIfPresent(_ relativePath: String) -> String {
        (try? readProjectFile(relativePath)) ?? ""
    }

    private func readProjectFile(_ relativePath: String) throws -> String {
        let fileURL = URL(fileURLWithPath: #filePath)
        let runtimeURL = URL(fileURLWithPath: FileManager.default.currentDirectoryPath)
        for startURL in [fileURL.deletingLastPathComponent(), runtimeURL] {
            var candidateRoot = startURL
            for _ in 0..<8 {
                let targetURL = candidateRoot.appendingPathComponent(relativePath)
                if FileManager.default.fileExists(atPath: targetURL.path) {
                    return try String(contentsOf: targetURL, encoding: .utf8)
                }
                let parentURL = candidateRoot.deletingLastPathComponent()
                guard parentURL.path != candidateRoot.path else { break }
                candidateRoot = parentURL
            }
        }
        throw CocoaError(.fileNoSuchFile)
    }
}
