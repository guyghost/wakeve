import XCTest
@testable import Wakeve

final class InvitationExperienceArchitectureReviewRedTests: XCTestCase {
    func testRolloutFlagOwnsInvitationExperienceRoutingWithoutChangingPersistedWriters() throws {
        let root = try readProjectFile("iosApp/src/Views/App/ContentView.swift")
        let app = try readProjectFile("iosApp/src/iOSApp.swift")
        let combined = root + "\n" + app

        XCTAssertTrue(
            combined.contains("iosInvitationExperienceV1"),
            "The approved mixed-version rollout requires the named UI flag; rollback must hide routes without removing migrations or writer fences."
        )
        XCTAssertTrue(
            root.contains("@AppStorage(\"iosInvitationExperienceV1\") private var iosInvitationExperienceV1 = false"),
            "A mixed-version rollout must be default-off until configuration explicitly enables every invitation surface."
        )

        let gatedSurfaceSlices = [
            sourceSlice(root, from: "case .eventCreation:", to: "case .eventDetail:"),
            sourceSlice(root, from: "case .eventAudience:", to: "case .eventInformation:"),
            sourceSlice(root, from: "case .eventInformation:", to: "case .eventArchive:"),
            sourceSlice(root, from: "case .eventArchive:", to: "case .participantManagement:"),
            sourceSlice(root, from: "private var invitationExperienceRootContent", to: "// MARK: - Tab Content"),
            sourceSlice(root, from: "private func routeInvitationExperience", to: "private func invitationRouteContext")
        ]
        XCTAssertEqual(gatedSurfaceSlices.count, 6)
        for slice in gatedSurfaceSlices {
            XCTAssertFalse(slice.isEmpty, "The rollout contract must inspect every installed invitation surface.")
            XCTAssertTrue(
                slice.contains("invitationExperienceRolloutEnabled"),
                "Library, Studio, Audience, Information, Archive and typed routing must consume the same rollout decision."
            )
        }

        let qaLaunch = sourceSlice(
            root,
            from: "#if DEBUG\n    private func prepareInvitationExperienceQALaunch",
            to: "private func invitationQAArtwork"
        )
        XCTAssertTrue(qaLaunch.contains("InvitationExperienceQALaunchSupport.seedArgument"))
        XCTAssertTrue(
            qaLaunch.contains("iosInvitationExperienceV1 = true"),
            "Repository-backed QA may explicitly opt into the rollout, but only inside its DEBUG launch hook."
        )

        let typedRouter = sourceSlice(
            root,
            from: "private func routeInvitationExperience",
            to: "private func invitationRouteContext"
        )
        let gateOffset = typedRouter.range(of: "invitationExperienceRolloutEnabled")?.lowerBound
        let resolveOffset = typedRouter.range(of: "invitationExperienceRouter.resolve")?.lowerBound
        XCTAssertNotNil(gateOffset)
        XCTAssertNotNil(resolveOffset)
        if let gateOffset, let resolveOffset {
            XCTAssertLessThan(
                gateOffset,
                resolveOffset,
                "Disabled deep links must fall back before the new typed router can open a mutating invitation destination."
            )
        }
        XCTAssertTrue(
            typedRouter.contains("rolloutReadOnlyFallback") ||
                typedRouter.contains("invitationExperienceLegacyFallback"),
            "Rollback must preserve a legacy/read-only event destination rather than silently exposing the new route."
        )
    }

    func testProductionRootConsumesDirectInviteCapabilityFromItsOwner() throws {
        let root = try readProjectFile("iosApp/src/Views/App/ContentView.swift")
        let context = sourceSlice(
            root,
            from: "private func directInviteRecipientContext",
            to: "private func saveInformationNotificationPreference"
        )
        let composition = context.isEmpty ? root : context

        XCTAssertFalse(
            composition.contains("DirectInviteCapabilityReady("),
            "ContentView must not manufacture a business capability from raw rows."
        )
        XCTAssertFalse(
            composition.contains("DatabaseDirectInviteCapabilityOwner("),
            "The SwiftUI root must consume the backend/repository capability owner; it must not construct and configure authorization policy inside ContentView."
        )
        XCTAssertFalse(
            composition.contains("SECURE_DELIVERY_BACKEND_UNAVAILABLE"),
            "A permanently unavailable placeholder is not a production direct-invite owner."
        )
        XCTAssertTrue(
            composition.lowercased().contains("directinviteproductionowner") ||
                composition.lowercased().contains("directinvitecontextprovider"),
            "ContentView may request context from the installed production owner/provider, but must not derive the capability itself."
        )
    }

    func testDirectInviteOwnerSealsPersistsAndDispatchesDeliveryWithoutRawRecipientLeakage() throws {
        let contracts = try readProjectFile(
            "shared/src/commonMain/kotlin/com/guyghost/wakeve/invitationexperience/InvitationExperiencePublicContracts.kt"
        )
        let repositories = try readProjectFile(
            "shared/src/commonMain/kotlin/com/guyghost/wakeve/invitationexperience/InvitationExperienceRepositories.kt"
        )
        let audience = try readProjectFile(
            "iosApp/src/Views/Invitations/EventAudienceView.swift"
        )
        let root = try readProjectFile("iosApp/src/Views/App/ContentView.swift")
        let keyOwnerProvider = try readProjectFile(
            "iosApp/src/Services/DirectInviteRecipientKeyOwnerProvider.swift"
        )
        let combined = contracts + "\n" + repositories
        let normalized = combined.lowercased()

        XCTAssertTrue(
            combined.contains("DirectInviteDeliveryEnvelope"),
            "The invitation owner must represent the separately encrypted, operation-scoped delivery value instead of trying to deliver from a one-way RecipientKey."
        )
        XCTAssertTrue(
            combined.contains("DirectInviteDeliveryTransport"),
            "A persisted local batch is not delivery; production needs an injected transport owner with typed acknowledgement."
        )
        XCTAssertTrue(
            normalized.contains("insertdirectinvitedeliveryenvelope"),
            "The owner must persist every sealed envelope before exposing PendingSync so retry survives relaunch."
        )
        XCTAssertTrue(
            normalized.contains("deliverytransport") &&
                (normalized.contains("dispatch(") || normalized.contains("deliver(")),
            "Submission/retry must invoke the production transport rather than stopping at the local queue."
        )
        XCTAssertFalse(
            normalized.contains("println(rawrecipient") ||
                normalized.contains("logger") && normalized.contains("rawrecipient"),
            "Transient recipient input is excluded from logs and diagnostics."
        )
        XCTAssertFalse(
            audience.contains("DirectInviteCapabilityReady("),
            "Audience may consume a revalidated capability, but must never manufacture one locally."
        )
        XCTAssertTrue(
            audience.contains("protectAndSeal(") && audience.contains("deliveryEnvelopes"),
            "Audience must hand transient input to the owner boundary and submit the sealed envelopes, not discard input after computing a digest."
        )
        XCTAssertTrue(
            (root + keyOwnerProvider).contains("DirectInviteDeliveryTransport") &&
                (root + keyOwnerProvider).contains("DirectInviteDeliverySealer"),
            "The production root must install both protected sealing and delivery transport owners; a shared seam alone is not a deliverable invitation path."
        )
        XCTAssertTrue(
            keyOwnerProvider.contains("URLSession") &&
                keyOwnerProvider.contains("appendingPathComponent(\"direct-invites\")") &&
                (keyOwnerProvider.contains("suffix: \"capability\"") ||
                    keyOwnerProvider.contains("appendingPathComponent(\"capability\")")) &&
                keyOwnerProvider.contains("appendingPathComponent(\"batches\")"),
            "The iOS production owner must load the bound capability and dispatch sealed batches through the authenticated backend endpoints."
        )
        XCTAssertTrue(
            keyOwnerProvider.contains("Authorization") && keyOwnerProvider.contains("Bearer"),
            "Capability and delivery requests must use the authenticated app session."
        )
        XCTAssertTrue(
            keyOwnerProvider.contains("ChaChaPoly") || keyOwnerProvider.contains("AES.GCM"),
            "The delivery sealer must encrypt the transient recipient; HMAC-only pseudonymization cannot deliver an invitation."
        )
        XCTAssertFalse(
            keyOwnerProvider.contains("rawRecipientInput") &&
                keyOwnerProvider.contains("debugLog("),
            "The production iOS owner must never log transient recipient input."
        )
        XCTAssertTrue(
            audience.contains("DatabaseDirectInviteBatchRepository(") &&
                audience.contains("deliveryTransport:"),
            "Audience must install the authenticated production transport into the repository owner rather than using the compatibility fail-closed constructor."
        )
    }

    func testSwiftSurfacesConsumeTotalSharedAccessInsteadOfParsingRsvpStrings() throws {
        let root = try readProjectFile("iosApp/src/Views/App/ContentView.swift")
        let audience = try readProjectFile(
            "iosApp/src/Views/Invitations/EventAudienceView.swift"
        )
        let routeContext = sourceSlice(
            root,
            from: "private func invitationRouteContext",
            to: "private func navigateToMeeting"
        )

        XCTAssertFalse(routeContext.contains("currentRecord?.rsvp"))
        XCTAssertFalse(routeContext.contains("!= \"DECLINED\""))
        XCTAssertFalse(audience.contains("switch record.rsvp"))
        XCTAssertFalse(
            audience.contains("switch record.role"),
            "A Swift participant-shaped row is not sufficient proof of ACTIVE_MEMBER."
        )
        XCTAssertTrue(
            (routeContext + audience).contains("ParticipantAccessMapper"),
            "UNAVAILABLE/NOT_APPLICABLE and ActiveMember proof must come from the total shared mapper."
        )
        XCTAssertTrue(
            audience.contains("switch access.role") &&
                audience.contains("case .organizer, .member: MembershipStateActiveMember"),
            "Audience may adapt an ACTIVE_MEMBER only after the total shared mapper proves organizer/member access."
        )
    }

    func testStudioObservesPersistedSyncCallbacksAndRetriesThroughTheSyncOwner() throws {
        let studio = try readProjectFile(
            "iosApp/src/Views/Invitations/EventCreationStudioView.swift"
        )

        XCTAssertTrue(
            studio.contains("CreationStudioEventSyncCompleted"),
            "PendingSync needs a production callback that dispatches the exact completed receipt."
        )
        XCTAssertTrue(
            studio.contains("CreationStudioEventSyncFailed"),
            "PendingSync needs a production callback that dispatches the exact failed receipt."
        )
        XCTAssertTrue(
            studio.lowercased().contains("syncowner"),
            "Post-commit retry must replay the persisted operation through its owner, not only transition local state."
        )

        let initializer = sourceSlice(
            studio,
            from: "init(\n        eventId:",
            to: "var currentDraftRevision"
        )
        XCTAssertTrue(
            initializer.contains("restorePersistedSync"),
            "Relaunch must recover the latest exact event/revision/operation binding instead of always rebuilding Editing."
        )

        let observation = sourceSlice(
            studio,
            from: "private func observeSync",
            to: "private func consumeSyncResult"
        )
        XCTAssertTrue(
            observation.contains("while") || observation.contains("for await"),
            "A single point-in-time Pending observation is not synchronization; Studio must continue until Completed or Failed."
        )
        XCTAssertTrue(
            observation.contains("CreationStudioSyncResultPending"),
            "Continuous observation must explicitly keep Pending non-terminal while preserving the exact binding."
        )

        let postCommitRetry = sourceSlice(
            studio,
            from: "else if let failed = studioState as? CreationStudioStateSyncFailed",
            to: "private func executeCommit"
        )
        XCTAssertTrue(postCommitRetry.contains("syncOwner.retry(binding: binding)"))
        XCTAssertTrue(
            postCommitRetry.contains("observeSync(binding)"),
            "RetrySync must resume the same terminal observation owner, not perform only one extra snapshot read."
        )

        let body = sourceSlice(studio, from: "var body: some View", to: "private func artworkButton")
        XCTAssertFalse(
            body.contains("Button(String(localized: \"common.retry\"))"),
            "The adaptive primary action already owns Retry; Studio must not render a second retry CTA in the hero card."
        )

        XCTAssertTrue(
            studio.contains("syncObservationTask") && studio.contains("syncObservationTask?.cancel()"),
            "The continuous sync observer must be owned by one stored Task so a closed Studio cannot keep polling or consume late callbacks."
        )
        XCTAssertTrue(
            studio.contains("onDisappear") || studio.contains("deinit"),
            "Studio must cancel its stored sync observation when the surface disappears or the owner is released."
        )
    }

    func testAudienceRetryAndCancelUsePersistedBatchOwnersThenReloadProjection() throws {
        let audience = try readProjectFile(
            "iosApp/src/Views/Invitations/EventAudienceView.swift"
        )
        let retry = sourceSlice(
            audience,
            from: "func retryDirectInviteBatch",
            to: "func cancelDirectInviteBatch"
        )
        let cancel = sourceSlice(
            audience,
            from: "func cancelDirectInviteBatch",
            to: "struct EventAudienceView"
        )

        XCTAssertFalse(retry.isEmpty, "Audience must expose an owner-backed retry for unresolved persisted batches.")
        XCTAssertTrue(retry.contains("RetryDirectInviteBatchCommand"))
        XCTAssertTrue(retry.contains("directInviteRepository.retry"))
        XCTAssertTrue(retry.contains("await reload()"))
        XCTAssertTrue(
            retry.contains("DirectInviteCapabilityReady") && retry.contains("accessRevision"),
            "Retry must revalidate the exact actor/event/access revision before replaying unresolved recipients."
        )

        XCTAssertFalse(cancel.isEmpty, "Audience must expose an owner-backed cancellation for a pending persisted batch.")
        XCTAssertTrue(cancel.contains("CancelDirectInviteBatchCommand"))
        XCTAssertTrue(cancel.contains("directInviteRepository.cancel"))
        XCTAssertTrue(cancel.contains("await reload()"))
        XCTAssertTrue(
            cancel.contains("DirectInviteCapabilityReady") && cancel.contains("accessRevision"),
            "Cancel must fail closed when its capability binding is stale."
        )

        let view = sourceSlice(
            audience,
            from: "struct EventAudienceView: View",
            to: "private func recipientOutcomeTitle"
        )
        XCTAssertTrue(view.contains("eventAudienceCancelBatchAction"))
        XCTAssertFalse(
            view.contains("eventAudienceRetryBatchAction"),
            "Retry is a state of the single primary invitation action, not a second prominent CTA."
        )
        XCTAssertEqual(
            view.components(separatedBy: "\"eventAudiencePrimaryAction\"").count - 1,
            1,
            "Audience must expose one primary action identity across submit and retry states."
        )
        let composer = sourceSlice(
            audience,
            from: "private var recipientComposer",
            to: "private var audienceAxes"
        )
        XCTAssertTrue(
            composer.contains("canRetryDirectInviteBatch") &&
                composer.contains("retryDirectInviteBatch") &&
                composer.contains("submitRecipient"),
            "The one primary button must adapt between new-recipient submission and retrying the unresolved persisted batch."
        )
        XCTAssertTrue(
            view.contains("viewModel.retryDirectInviteBatch") &&
                view.contains("viewModel.cancelDirectInviteBatch"),
            "Visible actions must call the repository-backed owners; local row mutation is not a retry/cancel."
        )
    }

    func testEventDeepLinksUseOneTypedParserInsteadOfASecondStringSwitch() throws {
        let service = try readProjectFile("iosApp/src/Services/DeepLinkService.swift")
        let root = try readProjectFile("iosApp/src/Views/App/ContentView.swift")
        let eventParser = sourceSlice(
            service,
            from: "private func parseEventRoute",
            to: "func handleDeepLink"
        )

        XCTAssertFalse(eventParser.isEmpty)
        XCTAssertFalse(
            eventParser.contains("switch path[1]"),
            "The parser must resolve an IosEventRoute through the typed route inventory, not maintain a second manual string switch."
        )
        XCTAssertTrue(
            eventParser.contains("IosEventRoute"),
            "The URL boundary should produce the existing typed IosEventRoute before navigation preflight."
        )
        XCTAssertFalse(
            service.contains("var navigationPath: [String]"),
            "After the URL boundary, the published navigation owner must carry IosRoute/IosEventRoute rather than erase it back to string segments."
        )
        XCTAssertFalse(
            root.contains("private func handleDeepLinkNavigation(_ path: [String])"),
            "AuthenticatedView must consume the typed route directly; rebuilding an enum with a second string switch is not end-to-end typed routing."
        )
        XCTAssertTrue(
            root.contains("private func handleDeepLinkNavigation(_ route: IosRoute") ||
                root.contains("private func handleDeepLinkNavigation(_ route: DeepLinkType"),
            "The root deep-link preflight must accept the typed route emitted by DeepLinkService."
        )
    }

    func testCreateDeepLinkSelectsExactlyOneStudioOrLegacyOwnerFromTheRolloutGate() throws {
        let root = try readProjectFile("iosApp/src/Views/App/ContentView.swift")
        let createBranch = sourceSlice(
            root,
            from: "case .eventCreate:",
            to: "case .event(.detail"
        )

        XCTAssertFalse(createBranch.isEmpty)
        XCTAssertTrue(
            createBranch.contains("invitationExperienceRolloutEnabled"),
            "Create deep links must consult the same rollout decision as the visible Library action."
        )
        XCTAssertTrue(createBranch.contains("currentView = .eventCreation"))
        XCTAssertTrue(createBranch.contains("showEventCreationSheet = true"))
        XCTAssertTrue(
            createBranch.contains("else"),
            "The Studio route and legacy sheet must be mutually exclusive branches, never opened together."
        )
        let studioOffset = createBranch.range(of: "currentView = .eventCreation")?.lowerBound
        let legacyOffset = createBranch.range(of: "showEventCreationSheet = true")?.lowerBound
        XCTAssertNotEqual(
            studioOffset,
            legacyOffset,
            "Rollout ON opens only Studio; rollout OFF opens only the legacy sheet."
        )
    }

    func testTypedIosRoutesHaveNoDeadStringArraySerializersAfterTheNavigationCutover() throws {
        let routes = try readProjectFile("iosApp/src/Models/IosRoute.swift")
        let service = try readProjectFile("iosApp/src/Services/DeepLinkService.swift")
        let root = try readProjectFile("iosApp/src/Views/App/ContentView.swift")

        XCTAssertFalse(
            routes.contains("navigationPath: [String]") || routes.contains("var navigationPath"),
            "IosRoute/IosEventRoute are the end-to-end navigation value; dead [String] serializers invite a second untyped switch."
        )
        XCTAssertFalse(service.contains("navigationPath"))
        XCTAssertFalse(root.contains("navigationPath"))
    }

    func testInformationStartsUnavailableUntilItsInjectedSystemAuthorizationPortReplies() throws {
        let information = try readProjectFile(
            "iosApp/src/Views/Invitations/EventInformationView.swift"
        )
        let adapter = try readProjectFile(
            "iosApp/src/Services/EventInformationSystemAuthorizationAdapter.swift"
        )

        XCTAssertTrue(
            information.contains("systemAuthorization: SystemNotificationAuthorization = .unavailable"),
            "Before the injected OS reader replies, Information must not misrepresent an unknown platform axis as a user decision NOT_DETERMINED."
        )
        XCTAssertFalse(
            information.contains("systemAuthorization: SystemNotificationAuthorization = .notDetermined")
        )
        XCTAssertTrue(information.contains("systemAuthorizationReader"))
        XCTAssertTrue(information.contains("await systemAuthorizationReader()"))
        XCTAssertTrue(adapter.contains("getNotificationSettings"))
        XCTAssertFalse(
            adapter.contains("requestAuthorization"),
            "Reading the system axis must remain side-effect free."
        )
    }

    func testEveryProductionDatabaseFactoryExplicitlyEnablesForeignKeys() throws {
        for path in [
            "shared/src/jvmMain/kotlin/com/guyghost/wakeve/JvmDatabaseFactory.kt",
            "shared/src/iosMain/kotlin/com/guyghost/wakeve/IosDatabaseFactory.kt",
            "shared/src/androidMain/kotlin/com/guyghost/wakeve/AndroidDatabaseFactory.kt"
        ] {
            let factory = try readProjectFile(path)
            XCTAssertTrue(
                factory.contains("PRAGMA foreign_keys = ON"),
                "Exclusive invitation rows cannot rely on test-only FK setup; missing explicit enforcement in \(path)."
            )
        }
    }

    func testServerArtworkReferencesHaveOnePersistentReleaseOwner() throws {
        let contracts = try readProjectFile(
            "shared/src/commonMain/kotlin/com/guyghost/wakeve/invitationexperience/InvitationExperiencePublicContracts.kt"
        )
        let invitationRepositories = try readProjectFile(
            "shared/src/commonMain/kotlin/com/guyghost/wakeve/invitationexperience/InvitationExperienceRepositories.kt"
        )
        let eventRepository = try readProjectFile(
            "shared/src/commonMain/kotlin/com/guyghost/wakeve/repository/DatabaseEventRepository.kt"
        )
        let combined = contracts + invitationRepositories + eventRepository
        let normalized = combined.lowercased()

        XCTAssertTrue(
            normalized.contains("serverartworkreference"),
            "SERVER_ASSET needs one persistent reference owner shared by replacement, deletion, and erasure."
        )
        XCTAssertTrue(
            normalized.contains("suspend fun release(") &&
                normalized.contains("releaseintransaction(reference"),
            "The final reference must schedule/reconcile physical release instead of disappearing as an unowned row count."
        )
    }

    private func sourceSlice(_ source: String, from start: String, to end: String) -> String {
        guard let startRange = source.range(of: start),
              let endRange = source.range(of: end, range: startRange.upperBound..<source.endIndex)
        else {
            return ""
        }
        return String(source[startRange.lowerBound..<endRange.lowerBound])
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
