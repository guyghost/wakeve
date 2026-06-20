import XCTest
@testable import Wakeve

final class PremiumEventDetailContractTests: XCTestCase {
    func testEventDetailUsesPremiumHierarchy() throws {
        let source = try readProjectFile("iosApp/src/Views/App/ContentView.swift")
        let content = slice(source, from: "struct EventDetailView", to: "private struct EventDetailHeroMetric")

        XCTAssertTrue(content.contains("EventHeroCard("))
        XCTAssertTrue(content.contains("metadataOverview"))
        XCTAssertTrue(content.contains("EventWeatherMapCard(state: eventWeatherViewModel.state)"))
        XCTAssertTrue(content.contains("anticipationPanel"))
        XCTAssertTrue(content.contains("urgentNextAction"))
        XCTAssertTrue(content.contains("groupReadinessPanel"))
        XCTAssertTrue(content.contains("participantsPreview"))
        XCTAssertTrue(content.contains("messagePreview"))
        XCTAssertTrue(content.contains("LiquidGlassToolbar(title: String(localized: \"event.detail.title\")"))
        XCTAssertTrue(content.contains("LiquidGlassButton("))
    }

    func testEventDetailUsesProgressiveSectionsAndCompactMessages() throws {
        let source = try readProjectFile("iosApp/src/Views/App/ContentView.swift")
        let content = slice(source, from: "struct EventDetailView", to: "private struct EventDetailHeroMetric")

        XCTAssertTrue(content.contains("EventDetailSectionCard(title: String(localized: \"event.detail.section.organization\")"))
        XCTAssertTrue(content.contains("EventDetailActionRow("))
        XCTAssertTrue(content.contains("EventDetailParticipantsPreview("))
        XCTAssertTrue(content.contains("EventDetailMessagePreview("))
        XCTAssertFalse(
            content.contains("EventPreviewDetailRow("),
            "Event Detail should use the premium progressive rows in its main hierarchy."
        )
    }

    func testEventDetailPreservesBusinessLogicBoundary() throws {
        let source = try readProjectFile("iosApp/src/Views/App/ContentView.swift")
        let content = slice(source, from: "struct EventDetailView", to: "private struct EventDetailHeroMetric")

        XCTAssertTrue(content.contains("canAccessScenarioPlanning"))
        XCTAssertTrue(content.contains("canAccessTransportPlanning"))
        XCTAssertTrue(content.contains("canShowOrganizationDashboard"))
        XCTAssertTrue(content.contains("ParticipantAccessMapper.shared.fromRepositoryRecord"))
        XCTAssertFalse(
            content.contains("StateMachine("),
            "Event Detail should keep presentation local and not instantiate shared state machines."
        )
    }

    func testEventDetailWeatherCardUsesWeatherKitAndMapKit() throws {
        let source = try readProjectFile("iosApp/src/Components/EventWeatherMapCard.swift")
        let provider = try readProjectFile("iosApp/src/Services/EventWeatherProvider.swift")

        XCTAssertTrue(provider.contains("import WeatherKit"))
        XCTAssertTrue(source.contains("import MapKit"))
        XCTAssertTrue(provider.contains("final class WeatherKitEventForecastProvider"))
        XCTAssertTrue(provider.contains("init(weatherService: WeatherService = .shared)"))
        XCTAssertTrue(source.contains("EventWeatherProviding"))
        XCTAssertTrue(source.contains("MKLocalSearch"))
        XCTAssertTrue(source.contains("potentialLocationQueries"))
        XCTAssertTrue(source.contains("weather.loading"))
        XCTAssertTrue(source.contains("weather.map_accessibility_format"))
        XCTAssertFalse(source.contains("Chargement météo"))
        XCTAssertFalse(source.contains("Météo bientôt disponible"))
    }

    func testEventInfoSheetUsesLocalizationKeys() throws {
        let source = try readProjectFile("iosApp/src/Components/EventInfoSheet.swift")

        XCTAssertTrue(source.contains("event_info.title"))
        XCTAssertTrue(source.contains("event_info.description_title"))
        XCTAssertTrue(source.contains("event_info.character_limit_format"))
        XCTAssertTrue(source.contains("event_info.profile_help"))
        XCTAssertFalse(source.contains("Informations sur l'évènement"))
        XCTAssertFalse(source.contains("Limite de caractères"))
    }

    func testEventDetailSurfacesAnticipationAndCountdown() throws {
        let source = try readProjectFile("iosApp/src/Views/App/ContentView.swift")
        let content = slice(source, from: "struct EventDetailView", to: "private enum EventAIInvitationVariant")

        XCTAssertTrue(content.contains("private var anticipationPanel"))
        XCTAssertTrue(content.contains("eventMomentDate"))
        XCTAssertTrue(content.contains("countdownTitle"))
        XCTAssertTrue(content.contains("EventDetailAnticipationCard"))
        XCTAssertTrue(content.contains("photo.on.rectangle.angled"))
        XCTAssertTrue(content.contains("returnHookMessage"))
        XCTAssertTrue(content.contains("event.detail.return_hook.share_action"))
        XCTAssertTrue(content.contains("event.detail.return_hook.message_format"))
        XCTAssertTrue(content.contains("eventAnticipationReturnHookShare"))
        XCTAssertTrue(content.contains("ShareLink(item: returnHookMessage)"))
        XCTAssertTrue(content.contains("InvitationTokenCodec.invitationCode(forEventId: event.id)"))
        XCTAssertTrue(content.contains("WakeveHaptics.selection()"))
    }

    func testInviteDeepLinkShowsGuestLandingStateOnEventDetail() throws {
        let source = try readProjectFile("iosApp/src/Views/App/ContentView.swift")
        let routing = slice(source, from: "private var homeTabContent", to: "case .participantManagement")
        let handler = slice(source, from: "private func handleDeepLinkNavigation", to: "private func navigateToEvent")
        let detail = slice(source, from: "struct EventDetailView", to: "private struct EventDetailHeroMetric")

        XCTAssertTrue(source.contains("invitationLandingEventId"))
        XCTAssertTrue(routing.contains("isInvitationLanding:"))
        XCTAssertTrue(routing.contains("invitationLandingEventId == event.id"))
        XCTAssertTrue(routing.contains("onDismissInvitationLanding"))
        XCTAssertTrue(handler.contains("case (\"invite\", let token?, _)"))
        XCTAssertTrue(handler.contains("invitationLandingEventId = eventId"))
        XCTAssertTrue(handler.contains("invitationLandingEventId = nil"))
        XCTAssertTrue(detail.contains("if isInvitationLanding"))
        XCTAssertTrue(detail.contains("invitationLandingCard"))
        XCTAssertTrue(detail.contains("eventInvitationLandingCard"))
        XCTAssertTrue(detail.contains("eventInvitationLandingPrimaryAction"))
        XCTAssertTrue(detail.contains("eventInvitationLandingContinueAction"))
        XCTAssertTrue(detail.contains("triggerInvitationLandingHapticIfNeeded()"))
        XCTAssertTrue(detail.contains("WakeveHaptics.success()"))
        XCTAssertTrue(detail.contains("event.detail.invite_landing.polling_subtitle"))
        XCTAssertTrue(detail.contains("event.detail.invite_landing.vote_action"))

        for locale in ["en", "fr", "es", "it", "pt"] {
            let strings = try readProjectFile("iosApp/src/Resources/\(locale).lproj/Localizable.strings")
            for key in [
                "event.detail.invite_landing.title",
                "event.detail.invite_landing.default_subtitle",
                "event.detail.invite_landing.polling_subtitle",
                "event.detail.invite_landing.vote_action",
                "event.detail.invite_landing.view_invite_action",
                "event.detail.invite_landing.continue_action"
            ] {
                XCTAssertTrue(strings.contains("\"\(key)\""), "Missing invite landing key \(key) for \(locale).")
            }
        }
    }

    func testEventDetailSurfacesGroupReadinessBeforeFinalization() throws {
        let source = try readProjectFile("iosApp/src/Views/App/ContentView.swift")
        let content = slice(source, from: "struct EventDetailView", to: "private enum EventAIInvitationVariant")

        XCTAssertTrue(content.contains("private var groupReadinessPanel"))
        XCTAssertTrue(content.contains("private var canShowGroupReadiness"))
        XCTAssertTrue(content.contains("private var groupReadinessItems"))
        XCTAssertTrue(content.contains("EventDetailReadinessCard("))
        XCTAssertTrue(content.contains("selectedScenarioLocation"))
        XCTAssertTrue(content.contains("meetingQueries"))
        XCTAssertTrue(content.contains("PaymentPotRepository"))
        XCTAssertTrue(content.contains("TricountHandoffRepository"))
        XCTAssertTrue(content.contains("event.detail.readiness.title"))
        XCTAssertTrue(content.contains("event.detail.readiness.progress_format"))
        XCTAssertTrue(content.contains("event.detail.readiness.transport.missing"))
        XCTAssertTrue(content.contains("event.detail.readiness.checklist.missing"))
        XCTAssertFalse(content.contains("\"Tout le monde est prêt ?\""))
        XCTAssertFalse(content.contains("\"Transport, argent\""))
    }

    func testEventDetailControlCopyUsesLocalizationKeys() throws {
        let source = try readProjectFile("iosApp/src/Views/App/ContentView.swift")
        let metadataContent = slice(source, from: "private var metadataOverview", to: "private var primaryActionTitle")
        let detailContent = slice(source, from: "struct EventDetailView", to: "private struct EventDetailHeroMetric")

        XCTAssertTrue(metadataContent.contains("event.detail.summary_title"))
        XCTAssertTrue(metadataContent.contains("event.detail.anticipation.invite.title"))
        XCTAssertTrue(metadataContent.contains("event.detail.participant_summary.you"))
        XCTAssertTrue(metadataContent.contains("event.detail.footer.draft"))
        XCTAssertTrue(metadataContent.contains("event.detail.message_preview.default"))
        XCTAssertTrue(metadataContent.contains("event.detail.messages_title"))
        XCTAssertTrue(metadataContent.contains("event.detail.organizer.invite_title"))
        XCTAssertTrue(metadataContent.contains("event.detail.organizer.poll_title"))
        XCTAssertTrue(metadataContent.contains("event.detail.menu.configure_poll"))
        XCTAssertTrue(metadataContent.contains("event.detail.menu.tricount"))
        XCTAssertTrue(metadataContent.contains("event.detail.organization.transport_value"))
        XCTAssertTrue(detailContent.contains("event.detail.readiness.title"))
        XCTAssertTrue(detailContent.contains("event.detail.readiness.payment.missing"))
        XCTAssertTrue(metadataContent.contains("event.detail.subtitle.empty"))
        XCTAssertFalse(detailContent.contains("Text(\"Résumé\")"))
        XCTAssertFalse(detailContent.contains("title: \"Messages\""))
        XCTAssertFalse(detailContent.contains("title: \"Inviter\""))
        XCTAssertFalse(detailContent.contains("title: \"Sondage\""))
        XCTAssertFalse(detailContent.contains("label: \"Tricount\""))
        XCTAssertFalse(detailContent.contains("\"Inviter le groupe\""))
        XCTAssertFalse(detailContent.contains("\"Aucun invité ajouté pour le moment.\""))
        XCTAssertFalse(detailContent.contains("\"Créneaux proposés\""))
        XCTAssertFalse(detailContent.contains("\"Régler le sondage\""))
    }

    func testLegacyEventCardCopyUsesLocalizationKeys() throws {
        let source = try readProjectFile("iosApp/src/Views/App/ContentView.swift")
        let eventList = slice(source, from: "struct EventListView", to: "struct EventCard")
        let eventCard = slice(source, from: "struct EventCard", to: "struct EventDetailView")

        XCTAssertTrue(eventList.contains("events.legacy_list.subtitle"))
        XCTAssertTrue(eventList.contains("events.legacy_list.create_title"))
        XCTAssertTrue(eventList.contains("events.legacy_list.create_subtitle"))
        XCTAssertTrue(eventList.contains("events.empty.title"))
        XCTAssertTrue(eventList.contains("events.empty.subtitle"))
        XCTAssertTrue(eventCard.contains("events.status.draft_preview"))
        XCTAssertTrue(eventCard.contains("events.status.organizing"))
        XCTAssertTrue(eventCard.contains("participants.count.singular_format"))
        XCTAssertTrue(eventCard.contains("participants.count.plural_format"))
        XCTAssertTrue(eventCard.contains("event.detail.slot_option_singular_format"))
        XCTAssertTrue(eventCard.contains("event.detail.slot_options_plural_format"))
        XCTAssertTrue(eventCard.contains("formatter.locale = .autoupdatingCurrent"))
        XCTAssertFalse(eventList.contains("\"No events yet\""))
        XCTAssertFalse(eventList.contains("\"Create your first event to get started\""))
        XCTAssertFalse(eventList.contains("\"Collaborative Event Planning\""))
        XCTAssertFalse(eventList.contains("\"Create New Event\""))
        XCTAssertFalse(eventList.contains("\"Start planning your collaborative event\""))
        XCTAssertFalse(eventCard.contains("return \"Draft\""))
        XCTAssertFalse(eventCard.contains("return \"Polling\""))
        XCTAssertFalse(eventCard.contains("return \"Confirmed\""))
        XCTAssertFalse(eventCard.contains("return \"Unknown\""))
        XCTAssertFalse(eventCard.contains("Text(\"\\(event.participants.count) participants\")"))
        XCTAssertFalse(eventCard.contains("Text(\"\\(event.proposedSlots.count) time slots\")"))
    }

    func testEventActionCopyIsLocalizedInCoreLocales() throws {
        let localeFiles = [
            "iosApp/src/Resources/en.lproj/Localizable.strings",
            "iosApp/src/Resources/fr.lproj/Localizable.strings",
            "iosApp/src/Resources/es.lproj/Localizable.strings",
            "iosApp/src/Resources/it.lproj/Localizable.strings",
            "iosApp/src/Resources/pt.lproj/Localizable.strings"
        ]
        let requiredKeys = [
            "events.empty.title",
            "events.empty.subtitle",
            "events.status.draft_preview",
            "events.status.date_confirmed",
            "events.status.organizing",
            "events.status.finalized",
            "events.status.event",
            "events.next_action.label",
            "events.next_action.blocked_label",
            "events.next_action.draft.title",
            "events.next_action.draft.short",
            "events.next_action.draft.subtitle",
            "events.next_action.draft.blocked.participants",
            "events.next_action.draft.blocked.slots",
            "events.next_action.draft.blocked.participants_and_slots",
            "events.next_action.polling.title",
            "events.next_action.polling.short",
            "events.next_action.polling.subtitle",
            "events.next_action.confirmed.title",
            "events.next_action.confirmed.short",
            "events.next_action.confirmed.subtitle",
            "events.next_action.organizing.title",
            "events.next_action.organizing.short",
            "events.next_action.organizing.subtitle",
            "events.next_action.finalized.title",
            "events.next_action.finalized.short",
            "events.next_action.finalized.subtitle",
            "events.next_action.default.title",
            "events.next_action.default.short",
            "events.next_action.default.subtitle"
        ]

        for localeFile in localeFiles {
            let content = try readProjectFile(localeFile)
            for key in requiredKeys {
                XCTAssertTrue(content.contains("\"\(key)\""), "\(localeFile) is missing \(key)")
            }
        }
    }

    func testEventDetailDatesUseUserLocale() throws {
        let source = try readProjectFile("iosApp/src/Views/App/ContentView.swift")
        let formatSlot = slice(source, from: "private func formatSlot", to: "private func formatEventDate")
        let formatEventDate = slice(source, from: "private func formatEventDate", to: "private func formatLongEventDate")
        let formatLongEventDate = slice(source, from: "private func formatLongEventDate", to: "private enum EventAIInvitationVariant")

        XCTAssertTrue(formatSlot.contains("dateFormatter.locale = .autoupdatingCurrent"))
        XCTAssertTrue(formatSlot.contains("timeFormatter.locale = .autoupdatingCurrent"))
        XCTAssertTrue(formatSlot.contains("timeFormatter.timeStyle = .short"))
        XCTAssertTrue(formatEventDate.contains("formatter.locale = .autoupdatingCurrent"))
        XCTAssertTrue(formatLongEventDate.contains("formatter.locale = .autoupdatingCurrent"))
        XCTAssertFalse(formatSlot.contains("Locale(identifier: \"fr_FR\")"), "Event detail slot display should respect the user's locale.")
        XCTAssertFalse(formatEventDate.contains("Locale(identifier: \"fr_FR\")"), "Event detail compact dates should respect the user's locale.")
        XCTAssertFalse(formatLongEventDate.contains("Locale(identifier: \"fr_FR\")"), "Event detail long dates should respect the user's locale.")
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

    private func slice(_ source: String, from startMarker: String, to endMarker: String) -> String {
        guard let start = source.range(of: startMarker)?.lowerBound else {
            return source
        }

        let tail = source[start...]
        guard let end = tail.range(of: endMarker)?.lowerBound else {
            return String(tail)
        }

        return String(tail[..<end])
    }
}
