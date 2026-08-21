import XCTest
import Shared
@testable import Wakeve

final class InvitationExperienceQALaunchRedTests: XCTestCase {
    func testDebugLaunchSupportSeedsRealInvitationRepositoriesAndUsesTypedRoutes() throws {
        let support = try readProjectFile("iosApp/src/Services/InvitationExperienceQALaunchSupport.swift")
        let root = try readProjectFile("iosApp/src/Views/App/ContentView.swift")

        XCTAssertTrue(support.contains("#if DEBUG"), "QA seed code must not compile into Release.")
        XCTAssertTrue(support.contains("--wakeve-qa-seed-invitation-experience"))
        XCTAssertTrue(support.contains("--wakeve-qa-open-invitation-route"))
        XCTAssertTrue(support.contains("WakeveDb"), "The QA launch must populate the real local source of truth.")
        XCTAssertTrue(support.contains("DatabaseEventRepository"))
        XCTAssertTrue(support.contains("DatabaseDirectInviteBatchRepository"))
        XCTAssertTrue(support.contains("DatabaseEventNotificationPreferenceRepository"))
        XCTAssertTrue(support.contains("InvitationExperienceRouter"))
        let prepare = sourceSlice(
            support,
            from: "func prepare(",
            to: "private func seedRepository"
        )
        let optInOffset = prepare.range(of: "UserDefaults.standard.set(true, forKey: \"iosInvitationExperienceV1\")")?.lowerBound
        let seedOffset = prepare.range(of: "seedRepository")?.lowerBound
        XCTAssertNotNil(optInOffset)
        XCTAssertNotNil(seedOffset)
        if let optInOffset, let seedOffset {
            XCTAssertLessThan(
                optInOffset,
                seedOffset,
                "The direct DEBUG support must explicitly enable the rollout before seeding or resolving routes; production remains default-off because this file is DEBUG-only."
            )
        }

        for seedId in [
            "qa-invitation-draft",
            "qa-invitation-polling",
            "qa-invitation-confirmed",
            "qa-invitation-finalized",
            "qa-invitation-past"
        ] {
            XCTAssertTrue(support.contains(seedId), "Missing deterministic repository seed \(seedId).")
        }
        for route in ["library", "studio", "audience", "information", "archive"] {
            XCTAssertTrue(support.contains("\"\(route)\""), "Missing DEBUG typed launch route \(route).")
        }

        XCTAssertFalse(support.localizedCaseInsensitiveContains("preview"))
        XCTAssertFalse(support.localizedCaseInsensitiveContains("fixture"))
        XCTAssertTrue(root.contains("InvitationExperienceQALaunchSupport"))
        XCTAssertTrue(root.contains("#if DEBUG"), "The production root hook must be erased from Release.")
        XCTAssertTrue(
            root.contains("invitationQALibraryReloadGeneration"),
            "Completing the asynchronous seed must invalidate the already-mounted Library."
        )
        XCTAssertTrue(
            root.contains(".id(invitationQALibraryReloadGeneration)"),
            "The repository-backed Library must remount/reload after the DEBUG seed commits."
        )
        XCTAssertTrue(
            root.contains("invitationQALibraryIsSeedReady"),
            "A clean first launch must gate the mounted Library until the asynchronous repository seed is complete; an after-the-fact identity bump still exposes a false empty state."
        )
        XCTAssertTrue(
            root.contains("if invitationQALibraryIsSeedReady"),
            "The real Library must only mount from the stable repository snapshot after seed completion, while ordinary non-QA launches remain immediately available."
        )
    }

#if DEBUG
    @MainActor
    func testSeedIsRepositoryBackedTotalProtectedAndIdempotentAcrossRelaunch() async throws {
        let database = RepositoryProvider.shared.database
        let repository = RepositoryProvider.shared.databaseRepository
        let viewerId = "wakeve-debug-user"
        let firstLaunch = InvitationExperienceQALaunchSupport(
            database: database,
            eventRepository: repository
        )

        let firstRoute = await firstLaunch.prepare(
            arguments: [
                InvitationExperienceQALaunchSupport.seedArgument,
                InvitationExperienceQALaunchSupport.openRouteArgument,
                "library"
            ],
            viewerId: viewerId
        )
        XCTAssertEqual(firstRoute, .library)

        let expectedStatuses: [(String, EventStatus)] = [
            ("qa-invitation-draft", .draft),
            ("qa-invitation-polling", .polling),
            ("qa-invitation-confirmed", .confirmed),
            ("qa-invitation-finalized", .finalized)
        ]
        for (eventId, status) in expectedStatuses {
            XCTAssertEqual(repository.getEvent(id: eventId)?.status, status, "Missing real seed \(eventId).")
        }

        let pastEvent = try XCTUnwrap(repository.getEvent(id: "qa-invitation-past"))
        let now = Kotlinx_datetimeInstant.companion.fromEpochMilliseconds(
            epochMilliseconds: Int64(Date().timeIntervalSince1970 * 1_000)
        )
        XCTAssertEqual(EventTemporalClassifier.shared.classify(event: pastEvent, now: now), .past)

        let allEventIds = expectedStatuses.map(\.0) + [pastEvent.id]
        for eventId in allEventIds {
            XCTAssertNotNil(
                database.invitationExperienceQueries
                    .selectArtworkByEventId(event_id: eventId)
                    .executeAsOneOrNull(),
                "Every seeded repository event must own exactly one total artwork projection."
            )
        }

        let audience = repository.getParticipantRecords(eventId: "qa-invitation-draft") ?? []
        XCTAssertGreaterThanOrEqual(audience.count, 2)
        XCTAssertTrue(audience.contains { $0.rsvp == "ACCEPTED" })
        XCTAssertTrue(audience.contains { $0.rsvp == "PENDING" })

        XCTAssertNotNil(
            database.invitationExperienceQueries
                .selectEventNotificationPreference(
                    event_id: "qa-invitation-confirmed",
                    user_id: viewerId
                )
                .executeAsOneOrNull(),
            "Information QA must read a persisted event-scoped preference."
        )

        let firstBatches = database.invitationExperienceQueries
            .selectDirectInviteBatchesByEventId(event_id: "qa-invitation-draft")
            .executeAsList()
        XCTAssertEqual(firstBatches.count, 1)
        let firstBatch = try XCTUnwrap(firstBatches.first)
        let firstOutcomes = database.invitationExperienceQueries
            .selectDirectInviteRecipientOutcomes(batch_id: firstBatch.batch_id)
            .executeAsList()
        XCTAssertGreaterThanOrEqual(firstOutcomes.count, 2)
        for outcome in firstOutcomes {
            XCTAssertTrue(outcome.recipient_key.hasPrefix("hmac-v1-"))
            XCTAssertFalse(outcome.recipient_key.localizedCaseInsensitiveContains("@"))
            XCTAssertFalse(outcome.recipient_key.localizedCaseInsensitiveContains("example"))
        }

        let revisionsBeforeRelaunch = Dictionary(uniqueKeysWithValues: allEventIds.map { eventId in
            (
                eventId,
                database.eventQueries.selectById(id: eventId).executeAsOneOrNull()?.aggregateRevision
            )
        })
        let restartedSupport = InvitationExperienceQALaunchSupport(
            database: database,
            eventRepository: repository
        )
        let relaunchedRoute = await restartedSupport.prepare(
            arguments: [
                InvitationExperienceQALaunchSupport.seedArgument,
                InvitationExperienceQALaunchSupport.openRouteArgument,
                "library"
            ],
            viewerId: viewerId
        )
        XCTAssertEqual(relaunchedRoute, .library)
        XCTAssertEqual(
            revisionsBeforeRelaunch,
            Dictionary(uniqueKeysWithValues: allEventIds.map { eventId in
                (
                    eventId,
                    database.eventQueries.selectById(id: eventId).executeAsOneOrNull()?.aggregateRevision
                )
            }),
            "Relaunch must not rewrite already-seeded aggregates."
        )
        XCTAssertEqual(
            database.invitationExperienceQueries
                .selectDirectInviteBatchesByEventId(event_id: "qa-invitation-draft")
                .executeAsList()
                .count,
            1,
            "Relaunch must not duplicate the direct-invite batch."
        )

        let mountedLibraryOwner = EventLibraryViewModel(
            viewerId: viewerId,
            projectionRepository: DatabaseInvitationExperienceProjectionRepository(
                database: database
            )
        )
        await mountedLibraryOwner.reload()
        let visibleEventIds = Set(mountedLibraryOwner.visibleCards.map(\.event.id))
        XCTAssertTrue(
            visibleEventIds.contains("qa-invitation-polling"),
            "After the asynchronous repository seed commits, the real mounted Library owner must consume the KMP Ready snapshot instead of rendering an empty shell. Visible IDs: \(visibleEventIds)"
        )
        XCTAssertTrue(
            visibleEventIds.contains("qa-invitation-confirmed"),
            "The default Upcoming projection must expose the repository-backed confirmed seed after relaunch. Visible IDs: \(visibleEventIds)"
        )
    }

    @MainActor
    func testDebugLaunchRoutesResolveEverySeededSurfaceThroughTypedDestinations() async {
        let support = InvitationExperienceQALaunchSupport(
            database: RepositoryProvider.shared.database,
            eventRepository: RepositoryProvider.shared.databaseRepository
        )
        let routes: [(String, InvitationExperienceQALaunchRoute)] = [
            ("library", .library),
            ("studio", .studio(eventId: "qa-invitation-draft")),
            ("audience", .audience(eventId: "qa-invitation-draft")),
            ("information", .information(eventId: "qa-invitation-confirmed")),
            ("archive", .archive(eventId: "qa-invitation-finalized"))
        ]

        for (rawRoute, expected) in routes {
            let actual = await support.prepare(
                arguments: [
                    InvitationExperienceQALaunchSupport.seedArgument,
                    InvitationExperienceQALaunchSupport.openRouteArgument,
                    rawRoute
                ],
                viewerId: "wakeve-debug-user"
            )
            XCTAssertEqual(actual, expected, "DEBUG route \(rawRoute) must resolve through its typed destination.")
        }
    }
#endif

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

    private func sourceSlice(_ source: String, from start: String, to end: String) -> String {
        guard let startRange = source.range(of: start),
              let endRange = source.range(of: end, range: startRange.upperBound..<source.endIndex)
        else { return "" }
        return String(source[startRange.lowerBound..<endRange.lowerBound])
    }
}
