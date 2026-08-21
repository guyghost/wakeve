import XCTest
import SwiftUI
import UIKit
import Shared
@testable import Wakeve

final class InvitationExperienceDesignerFindingsRedTests: XCTestCase {
    func testStudioPreviewKeepsArtworkAndEssentialMetadataInTheFirstViewport() throws {
        let root = try readProjectFile("iosApp/src/Views/App/ContentView.swift")
        let preview = sourceSlice(
            root,
            from: "private struct InvitationStudioPreviewSheet: View",
            to: "private struct AccessDenied:"
        )
        let artworkLedHeader = sourceSlice(
            preview,
            from: "InvitationArtworkView(",
            to: "eventStudioPreviewArtwork"
        )

        XCTAssertTrue(
            artworkLedHeader.contains("preview.snapshot.event.title"),
            "The exact draft title must remain attached to the artwork-led preview header."
        )
        for identifier in [
            "eventStudioPreviewDate",
            "eventStudioPreviewLocation",
            "eventStudioPreviewHost"
        ] {
            XCTAssertTrue(
                artworkLedHeader.contains(identifier),
                "The first preview viewport must keep \(identifier) with the artwork/title instead of pushing essential confirmation metadata below a long description."
            )
        }

        let description = sourceSlice(
            preview,
            from: "Text(preview.snapshot.event.description_)",
            to: "eventStudioPreviewDate"
        )
        XCTAssertTrue(description.contains(".lineLimit("))
        XCTAssertTrue(description.contains("eventStudioPreviewDescription"))
        XCTAssertFalse(
            description.contains(".fixedSize(horizontal: false, vertical: true)"),
            "A fully expanded description must not displace date, location, and host from the first preview viewport."
        )
    }

    func testEventDetailAdaptsScrimAndStateBadgesForHighContrastArtwork() throws {
        let canvas = try readProjectFile("iosApp/src/Views/Events/EventDetailInvitationCanvas.swift")
        let readableScrim = sourceSlice(
            canvas,
            from: "private var readableScrim",
            to: "private var titleAndState"
        )
        let titleAndState = sourceSlice(
            canvas,
            from: "private var titleAndState",
            to: "private var syncAndFreshnessDecoration"
        )
        let nextAction = sourceSlice(
            canvas,
            from: "private struct EventDetailInvitationNextActionSurface",
            to: "private struct EventDetailInvitationPrimaryActionButton"
        )

        XCTAssertTrue(readableScrim.contains("colorSchemeContrast"))
        XCTAssertTrue(
            readableScrim.contains("Color.black") || readableScrim.contains("midnightElevated"),
            "The sunset preset needs a stable contrast scrim independent of its bright focal point."
        )
        XCTAssertTrue(
            titleAndState.contains("Capsule") && titleAndState.contains(".background("),
            "Date/lifecycle state must sit on a contrast-stable badge rather than directly on bright artwork."
        )
        XCTAssertTrue(
            titleAndState.contains("eventDetailInvitationTitlePlate"),
            "The event title needs its own stable contrast plate over a bright sunset focal point."
        )
        XCTAssertTrue(
            titleAndState.contains("WakeveTheme.ColorToken.midnightElevated") ||
                titleAndState.contains("Color.black"),
            "The title plate must own a dark surface instead of relying on white foreground alone."
        )
        XCTAssertFalse(
            nextAction.contains("EventDetailInvitationGlassSurface"),
            "The standard sunset path needs one stable opaque next-action surface; glass cannot remain its default contrast owner."
        )
        XCTAssertTrue(
            nextAction.contains("WakeveTheme.ColorToken.midnightElevated"),
            "The next action must own an opaque midnight surface in the standard path, not only in accessibility fallbacks."
        )
        XCTAssertGreaterThanOrEqual(
            occurrences(of: ".foregroundStyle(.white", in: nextAction),
            3,
            "Caption, action title, and responsibility copy must all stay readable over the opaque surface."
        )
        XCTAssertTrue(canvas.contains("colorSchemeContrast == .increased"))
    }

    func testReduceTransparencySelectsAnOpaqueCanvasScrimThatDiffersFromGlass() throws {
        let canvas = try readProjectFile("iosApp/src/Views/Events/EventDetailInvitationCanvas.swift")
        let qaSupport = try readProjectFile("iosApp/src/Services/InvitationExperienceQALaunchSupport.swift")
        let root = try readProjectFile("iosApp/src/Views/App/ContentView.swift")
        let detailRoute = sourceSlice(
            root,
            from: "case .eventDetail:",
            to: "case .eventAudience:"
        )
        let readableScrim = sourceSlice(
            canvas,
            from: "private var readableScrim",
            to: "private var titleAndState"
        )
        let productionHero = sourceSlice(
            canvas,
            from: "private var productionHeroBackground",
            to: "private var readableScrim"
        )
        let glassSurface = sourceSlice(
            canvas,
            from: "private struct EventDetailInvitationGlassSurface",
            to: "private struct EventDetailInvitationCircleButton"
        )

        XCTAssertTrue(
            readableScrim.contains("reduceTransparency"),
            "Reduce Transparency must select a visibly different canvas scrim, not reuse the normal translucent artwork treatment."
        )
        let reducedArtworkScrimOpacity = numericLiteral(
            after: "reduceTransparency ? ",
            in: readableScrim
        )
        XCTAssertNotNil(reducedArtworkScrimOpacity)
        XCTAssertGreaterThan(reducedArtworkScrimOpacity ?? 0, 0)
        XCTAssertLessThan(
            reducedArtworkScrimOpacity ?? 1,
            1,
            "Reduce Transparency keeps the artwork visible under a partial global scrim; opacity belongs on local text/action surfaces."
        )
        XCTAssertTrue(
            productionHero.contains("InvitationArtworkView("),
            "The explicit accessibility branch must preserve the repository-backed artwork renderer."
        )
        XCTAssertTrue(glassSurface.contains("reduceTransparency || colorSchemeContrast == .increased"))
        XCTAssertTrue(glassSurface.contains("midnightElevated"))
        let opaqueBranch = sourceSlice(
            glassSurface,
            from: "else if reduceTransparency",
            to: "else {"
        )
        XCTAssertFalse(opaqueBranch.contains("Material"))
        XCTAssertFalse(opaqueBranch.contains("glassEffect"))

        XCTAssertTrue(
            qaSupport.contains(
                "static let reduceTransparencyArgument = \"--wakeve-qa-reduce-transparency\""
            ),
            "Repository-backed QA needs an explicit DEBUG-only override when Simulator accessibility preferences do not reach SwiftUI."
        )
        XCTAssertTrue(
            isDebugGuarded(
                "InvitationExperienceQALaunchSupport.reduceTransparencyArgument",
                in: root
            ),
            "The QA override argument must be consumed only inside a DEBUG compilation branch."
        )
        XCTAssertTrue(
            detailRoute.contains("\\.wakeveAccessibilityReduceTransparencyOverride") &&
                detailRoute.contains("invitationQAReduceTransparencyOverride"),
            "The repository-backed Detail route must receive the QA value through a writable custom environment key, because SwiftUI's system accessibility key is read-only."
        )
        XCTAssertTrue(
            isDebugGuarded("\\.wakeveAccessibilityReduceTransparencyOverride", in: detailRoute),
            "The custom environment injection must not be present in the Release Detail route."
        )
        XCTAssertGreaterThanOrEqual(
            occurrences(
                of: "@Environment(\\.accessibilityReduceTransparency)",
                in: canvas
            ),
            3,
            "Canvas surfaces must keep reading the real iOS accessibility signal."
        )
        XCTAssertGreaterThanOrEqual(
            occurrences(
                of: "@Environment(\\.wakeveAccessibilityReduceTransparencyOverride)",
                in: canvas
            ),
            3,
            "The explicit DEBUG capture override must reach the Canvas and its contrast-owning components."
        )
        XCTAssertGreaterThanOrEqual(
            occurrences(
                of: "systemReduceTransparency || reduceTransparencyOverride",
                in: canvas
            ),
            3,
            "The QA override must be OR-ed with, and never replace, the system Reduce Transparency signal."
        )
    }

    func testLibraryFiltersWrapWithoutHorizontalClippingAndKeepTheActiveChoiceVisible() throws {
        let library = try readProjectFile("iosApp/src/Views/Invitations/EventLibraryView.swift")
        let filterBar = sourceSlice(
            library,
            from: "private var filterBar",
            to: "private var cardBackground"
        )

        XCTAssertTrue(
            filterBar.contains(".contentMargins(.horizontal") ||
                filterBar.contains(".safeAreaPadding(.horizontal") ||
                filterBar.contains(".padding(.horizontal, WakeveTheme.Spacing.page"),
            "The first Library filter must retain its native leading inset instead of being clipped at the horizontal scroll edge."
        )
        XCTAssertFalse(filterBar.contains(".padding(.leading, -"))
        XCTAssertFalse(filterBar.contains(".offset(x: -"))
        XCTAssertFalse(
            filterBar.contains("ScrollView(.horizontal"),
            "A horizontally clipped rail cannot guarantee that the active Library projection remains visible."
        )
        XCTAssertFalse(filterBar.contains("ScrollViewReader"))
        XCTAssertFalse(filterBar.contains("scrollTo("))
        XCTAssertTrue(
            filterBar.contains("LazyVGrid") ||
                filterBar.contains("Grid(") ||
                filterBar.contains("Layout"),
            "Library filters need a native wrapping/grid layout that exposes every choice without auto-scroll conflicts."
        )
    }

    func testInformationProgressivelyDisclosesSecondaryNotificationAxes() throws {
        let information = try readProjectFile("iosApp/src/Views/Invitations/EventInformationView.swift")
        let notificationSection = sourceSlice(
            information,
            from: "String(localized: \"invitation.information.notifications\")",
            to: "destinationRow("
        )

        guard let disclosure = notificationSection.range(of: "DisclosureGroup") else {
            XCTFail("Account, iOS, and effective notification axes need one native progressive disclosure below the event preference row.")
            return
        }
        let eventAxis = try XCTUnwrap(
            notificationSection.range(of: "eventInformationNotificationEventAxis")
        )
        XCTAssertLessThan(eventAxis.lowerBound, disclosure.lowerBound)
        let disclosedAxes = String(notificationSection[disclosure.lowerBound...])
        for identifier in [
            "eventInformationNotificationAccountAxis",
            "eventInformationNotificationSystemAxis",
            "eventInformationNotificationEffectiveAxis"
        ] {
            XCTAssertTrue(disclosedAxes.contains(identifier))
        }
        XCTAssertTrue(notificationSection.contains("eventInformationPreferenceWrite"))
    }

    func testEventDetailMakesOnlyArtworkAndScrimFullBleedAboveSafeChrome() throws {
        let canvas = try readProjectFile("iosApp/src/Views/Events/EventDetailInvitationCanvas.swift")
        let body = sourceSlice(
            canvas,
            from: "var body: some View",
            to: "private var heroBackground"
        )
        let paintLayer = sourceSlice(
            body,
            from: "ZStack(alignment: .top)",
            to: "VStack(alignment: .leading"
        )
        let interactiveLayer = sourceSlice(
            body,
            from: "VStack(alignment: .leading",
            to: ".background(EventMoodPalette"
        )

        XCTAssertTrue(paintLayer.contains("heroBackground"))
        XCTAssertTrue(paintLayer.contains("readableScrim"))
        XCTAssertTrue(
            paintLayer.contains(".ignoresSafeArea(edges: .top)"),
            "Artwork and its scrim should paint under the status bar without moving content or controls there."
        )
        XCTAssertFalse(interactiveLayer.contains(".ignoresSafeArea"))
        XCTAssertTrue(interactiveLayer.contains("safeAreaTop + WakeveTheme.Spacing.sm"))
    }

    func testAudiencePlacesRecipientInputBeforeDiagnosticAxesAndPrimaryAction() throws {
        let audience = try readProjectFile("iosApp/src/Views/Invitations/EventAudienceView.swift")
        let body = sourceSlice(
            audience,
            from: "var body: some View",
            to: "private var audienceAxes"
        )
        let input = try XCTUnwrap(body.range(of: "recipientComposer"))
        let axes = try XCTUnwrap(body.range(of: "audienceAxes"))

        XCTAssertLessThan(
            input.lowerBound,
            axes.lowerBound,
            "The direct recipient task must precede secondary diagnostic axes."
        )
        XCTAssertTrue(
            body.contains("recipientComposer"),
            "The recipient task needs one stable composer block before diagnostic axes."
        )
        let composer = sourceSlice(
            audience,
            from: "private var recipientComposer",
            to: "private var audienceAxes"
        )
        XCTAssertTrue(composer.contains("eventAudienceRecipientInput"))
        XCTAssertTrue(composer.contains("eventAudiencePrimaryAction"))
        XCTAssertTrue(
            composer.contains("Image(systemName:") || composer.contains("Label("),
            "The recipient field needs a recognizable leading contact/mail icon."
        )
        XCTAssertTrue(composer.contains(".background("))
        XCTAssertTrue(
            composer.contains(".overlay(") || composer.contains(".overlay {"),
            "The recipient field needs a visible stroked boundary, regardless of SwiftUI overlay syntax."
        )
        XCTAssertTrue(composer.contains("RoundedRectangle"))
        XCTAssertTrue(composer.contains("eventAudienceRecipientComposer"))
    }

    func testArchiveSeparatesFreshnessAndSyncCopyInsteadOfRepeatingReadOnlyState() throws {
        let archive = try readProjectFile("iosApp/src/Views/Invitations/EventArchiveView.swift")
        let rows = sourceSlice(
            archive,
            from: "private func archiveStateRows",
            to: "\n}"
        )
        let freshnessKey = "invitation.archive.freshness.current"
        let syncKey = "invitation.archive.sync.current"

        XCTAssertTrue(rows.contains(freshnessKey))
        XCTAssertTrue(rows.contains(syncKey))
        XCTAssertEqual(
            occurrences(of: "invitation.archive.read_only", in: rows),
            0,
            "Read-only policy, repository freshness, and sync success are distinct user-facing concepts."
        )
        for locale in ["en", "fr", "es", "it", "pt"] {
            let strings = try readProjectFile("iosApp/src/Resources/\(locale).lproj/Localizable.strings")
            XCTAssertTrue(strings.contains("\"\(freshnessKey)\""))
            XCTAssertTrue(strings.contains("\"\(syncKey)\""))
            let freshnessCopy = try XCTUnwrap(
                localizedStringValue(for: freshnessKey, in: strings)
            )
            let syncCopy = try XCTUnwrap(
                localizedStringValue(for: syncKey, in: strings)
            )
            let readOnlyCopy = try XCTUnwrap(
                localizedStringValue(for: "invitation.archive.read_only", in: strings)
            )
            XCTAssertNotEqual(freshnessCopy, syncCopy)
            XCTAssertNotEqual(freshnessCopy, readOnlyCopy)
            XCTAssertNotEqual(syncCopy, readOnlyCopy)
        }
    }

    func testEventDetailKeepsInteractiveChromeInsideTheTopSafeArea() throws {
        let root = try readProjectFile("iosApp/src/Views/App/ContentView.swift")
        let detail = sourceSlice(
            root,
            from: "struct EventDetailView: View",
            to: "private static let progressiveDetailsAnchor"
        )
        let background = sourceSlice(
            detail,
            from: "WakeveTheme.ColorToken.pageBackground",
            to: "ScrollViewReader"
        )

        XCTAssertTrue(
            background.contains(".ignoresSafeArea()"),
            "The decorative page background may continue behind the status bar."
        )
        let scrollContainer = sourceSlice(
            detail,
            from: "ScrollViewReader { scrollProxy in",
            to: ".toolbar(.hidden, for: .tabBar)"
        )
        XCTAssertTrue(
            scrollContainer.contains(".ignoresSafeArea(edges: .top)"),
            "The real EventDetail ScrollView/container must paint its first canvas under the status bar; ignoring top only inside the nested canvas leaves a visible page-background band."
        )
        XCTAssertTrue(
            detail.contains("safeAreaTop: viewport.safeAreaInsets.top"),
            "The canvas chrome must receive the real top safe-area inset."
        )
    }

    func testEventDetailInjectsTotalRepositoryArtworkIntoTheSharedCanvasRenderer() throws {
        let root = try readProjectFile("iosApp/src/Views/App/ContentView.swift")
        let canvas = try readProjectFile("iosApp/src/Views/Events/EventDetailInvitationCanvas.swift")
        let detailRoute = sourceSlice(
            root,
            from: "case .eventDetail:",
            to: "case .eventAudience:"
        )
        let detailView = sourceSlice(
            root,
            from: "struct EventDetailView: View",
            to: "private static let progressiveDetailsAnchor"
        )
        let canvasDefinition = sourceSlice(
            canvas,
            from: "struct EventDetailInvitationCanvas: View",
            to: "struct EventDetailInvitationCanvasPersistentAction"
        )
        let productionHero = sourceSlice(
            canvasDefinition,
            from: "private var productionHeroBackground",
            to: "private var readableScrim"
        )

        XCTAssertTrue(
            detailRoute.contains("artwork:"),
            "The root route must inject the repository artwork selected for this event."
        )
        XCTAssertFalse(detailRoute.contains("ArtworkNone.shared"))
        XCTAssertFalse(detailRoute.contains("invitationQAArtwork"))
        XCTAssertTrue(
            detailView.contains("let artwork: any Artwork"),
            "Event Detail must receive a total, non-optional repository artwork contract."
        )
        XCTAssertTrue(detailView.contains("artwork: artwork"))
        XCTAssertTrue(canvasDefinition.contains("let artwork: any Artwork"))
        XCTAssertTrue(
            productionHero.contains("InvitationArtworkView("),
            "Detail must reuse the same PRESET/SERVER_ASSET/LEGACY_REMOTE/NONE renderer as Library, Studio and Archive."
        )
        XCTAssertTrue(productionHero.contains("artwork: artwork"))
        XCTAssertTrue(productionHero.contains("event: event"))
        XCTAssertFalse(
            canvasDefinition.contains("previewHeroImageName"),
            "The production canvas cannot substitute a DEBUG fixture image for repository artwork."
        )
        XCTAssertFalse(
            productionHero.contains("event.heroImageUrl"),
            "Legacy event copy must not take precedence over the total artwork aggregate."
        )
    }

    func testArchiveLocalizesEverySettledSummaryValueInsteadOfShowingISORepositoryCopy() throws {
        let archive = try readProjectFile("iosApp/src/Views/Invitations/EventArchiveView.swift")
        let information = try readProjectFile("iosApp/src/Views/Invitations/EventInformationView.swift")
        let rows = sourceSlice(
            archive,
            from: "private func archiveStateRows",
            to: "\n}"
        )

        XCTAssertTrue(
            information.contains("static func localizedSettledSummary("),
            "The shared metadata projection needs a total localized formatter for settled date ranges."
        )
        XCTAssertTrue(
            rows.contains("InvitationEventMetadataProjection.localizedSettledSummary"),
            "Archive must project repository settledSummary before rendering it."
        )
        XCTAssertFalse(
            rows.contains("settledSummary.joined(separator:"),
            "Joining raw repository timestamps exposes ISO persistence values to users."
        )
        XCTAssertFalse(rows.contains("Text(snapshot.settledSummary"))
    }

    func testLibraryAndArchiveUseCompactNativeActionChrome() throws {
        let library = try readProjectFile("iosApp/src/Views/Invitations/EventLibraryView.swift")
        let archive = try readProjectFile("iosApp/src/Views/Invitations/EventArchiveView.swift")
        let libraryBody = sourceSlice(
            library,
            from: "var body: some View",
            to: "private var filterBar"
        )
        let archiveBody = sourceSlice(
            archive,
            from: "var body: some View",
            to: "private func archiveStateRows"
        )

        XCTAssertFalse(
            libraryBody.contains(".safeAreaInset(edge: .bottom)"),
            "Library Create must use compact native toolbar chrome rather than a second full-width bottom bar above the app tab bar."
        )
        XCTAssertTrue(libraryBody.contains(".toolbar"))
        XCTAssertTrue(libraryBody.contains("eventLibraryPrimaryAction"))
        XCTAssertEqual(occurrences(of: "eventLibraryPrimaryAction", in: libraryBody), 1)

        XCTAssertFalse(
            archiveBody.contains(".safeAreaInset(edge: .bottom)"),
            "Archive refresh must not create an oversized full-width action bar above the app tab bar."
        )
        XCTAssertTrue(
            archiveBody.contains(".refreshable") || archiveBody.contains("ToolbarItem"),
            "Archive refresh should use the native pull-to-refresh or compact toolbar affordance."
        )
        XCTAssertEqual(occurrences(of: "eventArchivePrimaryAction", in: archiveBody), 1)
    }

    func testInformationUsesCompactTypedNotificationRowsWithOnlyEventPreferenceEditable() throws {
        let information = try readProjectFile("iosApp/src/Views/Invitations/EventInformationView.swift")
        let notificationRow = sourceSlice(
            information,
            from: "private func notificationRow",
            to: "private func informationCard"
        )

        XCTAssertTrue(
            notificationRow.contains("LabeledContent"),
            "Event/account/iOS/effective axes must be compact key-value rows rather than stacked caption blocks."
        )
        for identifier in [
            "eventInformationNotificationEventAxis",
            "eventInformationNotificationAccountAxis",
            "eventInformationNotificationSystemAxis",
            "eventInformationNotificationEffectiveAxis"
        ] {
            XCTAssertTrue(
                information.contains(identifier),
                "Information is missing the distinct typed axis \(identifier)."
            )
        }
        XCTAssertEqual(
            occurrences(of: "eventInformationPreferenceWrite", in: information),
            1,
            "Only the event preference axis owns an edit action; account and OS axes are read-only projections."
        )
        XCTAssertFalse(information.contains("showNotificationPreferencesSheet"))
        XCTAssertFalse(information.contains("requestAuthorization"))
    }

    func testInformationAudienceAndArchiveOwnSingleChromeWithAudienceReasonAdjacentToCTA() throws {
        let information = try readProjectFile("iosApp/src/Views/Invitations/EventInformationView.swift")
        let audience = try readProjectFile("iosApp/src/Views/Invitations/EventAudienceView.swift")
        let archive = try readProjectFile("iosApp/src/Views/Invitations/EventArchiveView.swift")

        for (surface, source) in [
            ("Information", information),
            ("Audience", audience),
            ("Archive", archive)
        ] {
            XCTAssertTrue(
                source.contains(".toolbar(.hidden, for: .tabBar)"),
                "\(surface) must hide the app tab bar so the route owns one navigation/action chrome layer."
            )
        }

        let actionContainer = sourceSlice(
            audience,
            from: "private var recipientComposer",
            to: "private var audienceAxes"
        )
        XCTAssertTrue(actionContainer.contains("eventAudienceRecipientInput"))
        XCTAssertTrue(actionContainer.contains("eventAudienceDisabledReason"))
        XCTAssertTrue(actionContainer.contains("eventAudiencePrimaryAction"))
        XCTAssertLessThan(
            abs(
                (actionContainer.range(of: "eventAudienceDisabledReason")?.lowerBound.utf16Offset(in: actionContainer) ?? 0) -
                    (actionContainer.range(of: "eventAudiencePrimaryAction")?.lowerBound.utf16Offset(in: actionContainer) ?? 10_000)
            ),
            2_000,
            "The fail-closed reason must remain visibly adjacent to its disabled CTA, not disappear higher in the scroll."
        )
    }

    func testRepositoryBackedQADetailRouteUsesTypedPreflightWithoutADeepLinkPrompt() throws {
        let support = try readProjectFile("iosApp/src/Services/InvitationExperienceQALaunchSupport.swift")
        let root = try readProjectFile("iosApp/src/Views/App/ContentView.swift")

        XCTAssertTrue(
            support.contains("case detail(eventId: String)"),
            "Repository-backed visual QA needs a DEBUG-only Event Detail route that does not depend on an interactive system URL prompt."
        )
        XCTAssertTrue(support.contains("case \"detail\":"))
        let detailRoute = sourceSlice(
            support,
            from: "case \"detail\":" ,
            to: "case \"studio\":"
        )
        XCTAssertTrue(
            detailRoute.contains("InvitationExperienceRouteRequestCanvasAction(action: .showDetails)"),
            "The DEBUG route must exercise the same typed SHOW_DETAILS preflight as production."
        )
        XCTAssertTrue(
            detailRoute.contains("Seed.confirmed"),
            "Event Detail QA must open a real repository-backed confirmed seed."
        )
        XCTAssertTrue(
            root.contains("case .detail(let eventId):"),
            "The root must apply the resolved DEBUG route only after seeding/preflight completes."
        )
        XCTAssertTrue(root.contains("repository.getEvent(id: eventId)"))
    }

    func testStudioEditorRendersTheCurrentTotalArtworkAsAnAdaptiveSelectedHero() throws {
        let studio = try readProjectFile("iosApp/src/Views/Invitations/EventCreationStudioView.swift")
        let editor = sourceSlice(
            studio,
            from: "struct EventCreationStudioView: View",
            to: "private func artworkButton("
        )
        let artworkButton = sourceSlice(
            studio,
            from: "private func artworkButton(",
            to: "private var artworkColumns"
        )

        XCTAssertTrue(
            studio.contains("var currentPreviewArtwork: any Artwork"),
            "Studio ViewModel must expose the total KEEP_EXISTING/NONE/PRESET artwork currently selected."
        )
        XCTAssertTrue(
            studio.contains("var currentPreviewEvent: Event"),
            "The artwork renderer needs a total draft event projection even before the local commit."
        )
        XCTAssertTrue(
            editor.contains("InvitationArtworkView("),
            "The editor must show the same real artwork renderer used by Library, final preview and Archive."
        )
        XCTAssertTrue(editor.contains("viewModel.currentPreviewArtwork"))
        XCTAssertTrue(editor.contains("viewModel.currentPreviewEvent"))
        XCTAssertTrue(
            editor.contains("eventStudioArtworkHero"),
            "The editable hero needs a stable accessibility/layout identifier."
        )
        XCTAssertTrue(
            editor.contains("aspectRatio(4.0 / 3.0") || editor.contains("aspectRatio(4 / 3"),
            "A 4:3 crop-safe hero occupies roughly 30–40% of the standard viewport without a fixed-height Dynamic Type trap."
        )
        XCTAssertFalse(
            editor.contains(".frame(height:"),
            "The artwork hero must remain scrollable/adaptive rather than claim a fixed height."
        )
        XCTAssertTrue(
            artworkButton.contains("checkmark.circle.fill") || artworkButton.contains("eventStudioArtworkSelectionIndicator"),
            "The current artwork choice must remain visually identifiable, not only expose an accessibility selected trait."
        )

        let hero = try XCTUnwrap(editor.range(of: "eventStudioArtworkHero"))
        let titleEditor = try XCTUnwrap(editor.range(of: "create_event.title_label"))
        let descriptionEditor = try XCTUnwrap(editor.range(of: "TextEditor("))
        XCTAssertLessThan(
            hero.lowerBound,
            titleEditor.lowerBound,
            "The real Studio first viewport must lead with artwork and its exact snapshot before title editing."
        )
        XCTAssertLessThan(
            hero.lowerBound,
            descriptionEditor.lowerBound,
            "A long description editor cannot displace the invitation artwork from the first viewport."
        )

        let heroSnapshot = sourceSlice(
            editor,
            from: "InvitationArtworkView(",
            to: "if viewModel.isPendingSync"
        )
        for identifier in [
            "eventStudioHeroDate",
            "eventStudioHeroLocation",
            "eventStudioHeroHost"
        ] {
            XCTAssertTrue(
                heroSnapshot.contains(identifier),
                "The artwork-led editor snapshot must keep \(identifier) attached to the hero before field editors."
            )
        }
        let heroTitle = try XCTUnwrap(heroSnapshot.range(of: "eventStudioHeroTitle"))
        let heroDate = try XCTUnwrap(heroSnapshot.range(of: "eventStudioHeroDate"))
        XCTAssertTrue(
            heroSnapshot.contains("Text(viewModel.title)"),
            "Studio must identify the real draft in the artwork card, not only label the generic Studio surface."
        )
        XCTAssertLessThan(
            heroTitle.lowerBound,
            heroDate.lowerBound,
            "The exact event title must be visible with the artwork before date/location/host metadata."
        )

        let disclosure = try XCTUnwrap(editor.range(of: "DisclosureGroup"))
        XCTAssertLessThan(disclosure.lowerBound, descriptionEditor.lowerBound)
        XCTAssertTrue(editor.contains("eventStudioDescriptionDisclosure"))
        XCTAssertFalse(
            editor.contains("isExpanded: .constant(true)"),
            "The long description disclosure must start collapsed."
        )
    }

    func testStudioArtworkChoicesUseOneAdaptiveRowPerLabelWithoutFragmentingCopy() throws {
        let studio = try readProjectFile("iosApp/src/Views/Invitations/EventCreationStudioView.swift")
        let artworkSection = sourceSlice(
            studio,
            from: ".invitationAccessibilityIdentifier(\"eventStudioHeroHost\")",
            to: "if viewModel.isPendingSync"
        )
        let artworkButton = sourceSlice(
            studio,
            from: "private func artworkButton(",
            to: "private var artworkColumns"
        )
        let artworkColumns = sourceSlice(
            studio,
            from: "private var artworkColumns",
            to: "\n}"
        )

        XCTAssertFalse(
            artworkColumns.contains("dynamicTypeSize.isAccessibilitySize ? 1 : 2"),
            "Standard size must not squeeze the long Keep Existing choice into a two-column chip."
        )
        XCTAssertTrue(
            artworkSection.contains("VStack") ||
                (
                    artworkColumns.contains("[GridItem(.flexible()") &&
                        !artworkColumns.contains("Array(repeating:")
                ),
            "Each artwork choice needs a full-width native row at standard and accessibility sizes."
        )
        XCTAssertTrue(
            artworkButton.contains(".lineLimit(2)"),
            "Localized artwork labels may wrap once, but must not fracture into four narrow lines."
        )
        XCTAssertTrue(artworkButton.contains(".fixedSize(horizontal: false, vertical: true)"))
        XCTAssertFalse(artworkButton.contains(".lineLimit(1)"))
        XCTAssertFalse(artworkButton.contains(".frame(height:"))
        XCTAssertTrue(artworkButton.contains("eventStudioArtworkSelectionIndicator"))
        XCTAssertTrue(artworkButton.contains(".accessibilityAddTraits"))
    }

    func testDetailSyncStateOwnsAnOpaqueContrastPlateInsteadOfUsingColorAlone() throws {
        let canvas = try readProjectFile("iosApp/src/Views/Events/EventDetailInvitationCanvas.swift")
        let decoration = sourceSlice(
            canvas,
            from: "private var syncAndFreshnessDecoration",
            to: "private var organizerRow"
        )

        XCTAssertTrue(decoration.contains("event.detail.canvas.sync_pending"))
        XCTAssertTrue(decoration.contains("event.detail.canvas.sync_confirmed"))
        XCTAssertTrue(
            decoration.contains("WakeveTheme.ColorToken.midnightElevated") ||
                decoration.contains("Color.black"),
            "Pending and synced states need a dark opaque owner over bright artwork."
        )
        XCTAssertTrue(
            decoration.contains("Capsule"),
            "The entire icon-and-copy sync state must sit on one contrast-stable plate."
        )
        XCTAssertTrue(
            decoration.contains(".foregroundStyle(.white"),
            "Sync icon and copy need a contrast-stable foreground independent of amber/green hue."
        )
        XCTAssertFalse(decoration.contains(".foregroundStyle(WakeveTheme.ColorToken.warmAmber)"))
        XCTAssertFalse(decoration.contains(".foregroundStyle(WakeveTheme.ColorToken.confirmationBase)"))
        XCTAssertTrue(decoration.contains(".accessibilityElement(children: .combine)"))
    }

    func testPresetArtworkUsesThreeRealBitmapAssetsInsteadOfGeneratedGradientSymbols() throws {
        let assets: [(imageset: String, assetName: String)] = [
            ("InvitationPresetLake.imageset", "InvitationPresetLake"),
            ("InvitationPresetSunset.imageset", "InvitationPresetSunset"),
            ("InvitationPresetCelebration.imageset", "InvitationPresetCelebration")
        ]
        let root = repositoryRoot()
        for asset in assets {
            let imageset = root
                .appendingPathComponent("iosApp/src/Assets.xcassets")
                .appendingPathComponent(asset.imageset)
            let contentsURL = imageset.appendingPathComponent("Contents.json")
            let contentsData = try Data(contentsOf: contentsURL)
            let contents = try XCTUnwrap(
                JSONSerialization.jsonObject(with: contentsData) as? [String: Any]
            )
            let images = try XCTUnwrap(contents["images"] as? [[String: Any]])
            let filenames = images.compactMap { $0["filename"] as? String }
            XCTAssertFalse(filenames.isEmpty, "\(asset.imageset) must reference a real bitmap file.")
            for filename in filenames {
                let bitmapURL = imageset.appendingPathComponent(filename)
                let bitmapData = try Data(contentsOf: bitmapURL)
                XCTAssertGreaterThan(bitmapData.count, 50_000, "\(filename) is not a production artwork bitmap.")
                let image = try XCTUnwrap(UIImage(data: bitmapData), "\(filename) must decode as an image.")
                XCTAssertGreaterThanOrEqual(image.size.width, 1_000)
                XCTAssertGreaterThanOrEqual(image.size.height, 1_000)
            }
        }

        let library = try readProjectFile("iosApp/src/Views/Invitations/EventLibraryView.swift")
        let presetRenderer = sourceSlice(
            library,
            from: "private func presetArtwork(",
            to: "private func focalAlignment"
        )
        for asset in assets {
            XCTAssertTrue(
                presetRenderer.contains("Image(\"\(asset.assetName)\")"),
                "presetId must map to the real \(asset.assetName) catalogue asset."
            )
        }
        XCTAssertTrue(presetRenderer.contains("scaledToFill"))
        XCTAssertTrue(presetRenderer.contains("focalAlignment"))
        XCTAssertFalse(
            presetRenderer.contains("LinearGradient"),
            "PRESET artwork cannot be synthesized as a coded gradient."
        )
        XCTAssertFalse(
            presetRenderer.contains("Image(systemName:"),
            "PRESET artwork cannot substitute an SF Symbol for the approved bitmap."
        )
        XCTAssertFalse(presetRenderer.contains("presetAppearance"))
    }

    func testPresetArtworkUsesOneDeterministicRendererAcrossLibraryStudioAndArchive() throws {
        let library = try readProjectFile("iosApp/src/Views/Invitations/EventLibraryView.swift")
        let root = try readProjectFile("iosApp/src/Views/App/ContentView.swift")
        let archive = try readProjectFile("iosApp/src/Views/Invitations/EventArchiveView.swift")
        let renderer = sourceSlice(
            library,
            from: "struct InvitationArtworkView: View",
            to: "private var fallback"
        )
        let preview = sourceSlice(
            root,
            from: "private struct InvitationStudioPreview:",
            to: "private struct AccessDenied:"
        )

        XCTAssertTrue(
            renderer.contains("ArtworkSourcePreset") && renderer.contains("presetId"),
            "A STRUCTURED/PRESET artwork must render its deterministic preset, not collapse to the event-type fallback."
        )
        XCTAssertTrue(library.contains("InvitationArtworkView(\n                                        artwork: card.artwork"))
        XCTAssertTrue(archive.contains("InvitationArtworkView(\n                                    artwork: snapshot.artwork"))
        XCTAssertTrue(
            preview.contains("InvitationArtworkView("),
            "Studio preview must consume the same total artwork renderer and crop semantics as Library and Archive."
        )
        XCTAssertTrue(
            preview.contains("eventStudioPreviewArtwork"),
            "The preview artwork needs a stable accessibility/layout contract for repository-backed QA."
        )
    }

    func testStudioPreviewConsumesATypedSnapshotWithArtworkDateLocationAndHost() throws {
        let studio = try readProjectFile("iosApp/src/Views/Invitations/EventCreationStudioView.swift")
        let root = try readProjectFile("iosApp/src/Views/App/ContentView.swift")
        let preview = sourceSlice(
            root,
            from: "private struct InvitationStudioPreview:",
            to: "private struct AccessDenied:"
        )

        XCTAssertTrue(
            studio.contains("InvitationStudioPreviewSnapshot"),
            "Preview must receive one typed draft snapshot instead of a lossy title/description callback."
        )
        XCTAssertTrue(
            preview.contains("InvitationStudioPreviewSnapshot"),
            "The root must retain the exact typed preview snapshot through confirmation."
        )
        for identifier in [
            "eventStudioPreviewArtwork",
            "eventStudioPreviewDate",
            "eventStudioPreviewLocation",
            "eventStudioPreviewHost"
        ] {
            XCTAssertTrue(
                preview.contains(identifier),
                "Studio preview is missing the user-facing snapshot field \(identifier)."
            )
        }
        XCTAssertTrue(
            preview.contains("InvitationEventMetadataProjection.localizedDate"),
            "Preview must localize its proposed date/slot rather than expose repository timestamps."
        )
    }

    func testLibraryPrimaryActionKeepsExplicitForegroundInDarkAndIncreasedContrast() throws {
        let library = try readProjectFile("iosApp/src/Views/Invitations/EventLibraryView.swift")

        XCTAssertTrue(
            library.contains(".foregroundStyle(libraryPrimaryForeground)"),
            "The prominent Create label needs an explicit adaptive foreground; it disappeared against the dark glass capture."
        )
        XCTAssertTrue(
            library.contains(".tint(libraryPrimaryTint)"),
            "The prominent Create control needs an explicit tint contract under dark and increased-contrast appearances."
        )
        XCTAssertTrue(
            library.contains("@Environment(\\.colorScheme)"),
            "The Library primary action cannot adapt its foreground without the active color scheme."
        )
        XCTAssertTrue(library.contains("colorSchemeContrast"))
    }

    func testStudioRouteOwnsSingleChromeAndKeepsPrimaryActionInsideAccessibilitySafeArea() throws {
        let studio = try readProjectFile("iosApp/src/Views/Invitations/EventCreationStudioView.swift")
        let root = try readProjectFile("iosApp/src/Views/App/ContentView.swift")
        let creationRoute = sourceSlice(
            root,
            from: "case .eventCreation:",
            to: "case .eventDetail:"
        )

        XCTAssertTrue(
            studio.contains(".toolbar(.hidden, for: .tabBar)"),
            "Studio must explicitly hide the app tab bar so its own bottom action does not create double chrome."
        )
        XCTAssertTrue(
            studio.contains("dynamicTypeSize.isAccessibilitySize"),
            "Studio must derive bottom spacing from accessibility Dynamic Type."
        )
        XCTAssertTrue(
            studio.contains("safeAreaInset(edge: .bottom"),
            "The primary action must remain above the device safe area at accessibility3/5."
        )
        XCTAssertFalse(
            creationRoute.contains(".toolbar(.visible, for: .tabBar)"),
            "The production Studio route must not re-enable app chrome around the editor."
        )
    }

    func testLibraryKeepsTheSelectedFilterVisibleAndArchiveUsesDataRefreshCopy() throws {
        let library = try readProjectFile("iosApp/src/Views/Invitations/EventLibraryView.swift")
        let filterBar = sourceSlice(
            library,
            from: "private var filterBar",
            to: "private var cardBackground"
        )
        let french = try readProjectFile("iosApp/src/Resources/fr.lproj/Localizable.strings")

        XCTAssertFalse(filterBar.contains("ScrollViewReader"))
        XCTAssertFalse(filterBar.contains("ScrollView(.horizontal"))
        XCTAssertFalse(filterBar.contains("scrollTo("))
        XCTAssertTrue(filterBar.contains(".isSelected"))
        XCTAssertTrue(
            french.contains("\"invitation.action.reload_projection\" = \"Actualiser les données\";"),
            "Archive refreshes its repository projection/data, not the immutable finalized event itself."
        )
    }

    func testAudienceShowsSpecificDisabledReasonAndIdentityGroupStatusWithoutKeys() throws {
        let audience = try readProjectFile("iosApp/src/Views/Invitations/EventAudienceView.swift")

        XCTAssertTrue(
            audience.contains("eventAudienceIdentityGroup"),
            "Every audience row must expose a privacy-safe group such as member or invitation."
        )
        XCTAssertTrue(
            audience.contains("eventAudienceIdentityStatus"),
            "Every audience row must expose a user-facing status without showing its identity key."
        )
        XCTAssertTrue(
            audience.contains("invitation.audience.error.security_unavailable"),
            "A fail-closed invite action must explain the specific unavailable security owner inline."
        )
        let visibleRows = sourceSlice(
            audience,
            from: "ForEach(viewModel.projection.identities",
            to: "audienceAxes"
        )
        XCTAssertFalse(
            visibleRows.contains("Text(identity.identityKey)"),
            "Audience must never render a repository or protected recipient key."
        )
    }

#if DEBUG
    @MainActor
    func testRepositoryBackedQASeedProvidesDistinctPresetsAndPrivacySafeAudienceNames() async throws {
        let supportSource = try readProjectFile(
            "iosApp/src/Services/InvitationExperienceQALaunchSupport.swift"
        )
        XCTAssertFalse(supportSource.localizedCaseInsensitiveContains("Données QA"))
        let database = RepositoryProvider.shared.database
        let repository = RepositoryProvider.shared.databaseRepository
        let viewerId = "wakeve-debug-user"
        let support = InvitationExperienceQALaunchSupport(
            database: database,
            eventRepository: repository
        )

        let route = await support.prepare(
            arguments: [
                InvitationExperienceQALaunchSupport.seedArgument,
                InvitationExperienceQALaunchSupport.openRouteArgument,
                "audience"
            ],
            viewerId: viewerId
        )
        XCTAssertEqual(
            route,
            .audience(eventId: "qa-invitation-draft")
        )

        let eventIds = [
            "qa-invitation-draft",
            "qa-invitation-polling",
            "qa-invitation-confirmed",
            "qa-invitation-finalized",
            "qa-invitation-past"
        ]
        let presetIds = Set(eventIds.compactMap { eventId -> String? in
            guard let row = database.invitationExperienceQueries
                .selectArtworkByEventId(event_id: eventId)
                .executeAsOneOrNull(),
                  row.kind == "STRUCTURED",
                  row.source_kind == "PRESET"
            else { return nil }
            return row.preset_id
        })
        XCTAssertGreaterThanOrEqual(
            presetIds.count,
            2,
            "Repository-backed QA must render at least two distinct PRESET artworks across real routes. Found \(presetIds)."
        )

        let seededDraft = try XCTUnwrap(repository.getEvent(id: "qa-invitation-draft"))
        let seededDescription = seededDraft.description_
            .trimmingCharacters(in: .whitespacesAndNewlines)
        XCTAssertGreaterThan(seededDescription.count, 24)
        XCTAssertTrue(
            seededDescription.localizedCaseInsensitiveContains("Annecy"),
            "Event Information must show realistic event copy tied to the seeded route."
        )
        XCTAssertFalse(
            seededDescription.localizedCaseInsensitiveContains("QA"),
            "No DEBUG/test vocabulary may leak into a repository-backed Information capture."
        )

        let participantRecords = repository.getParticipantRecords(eventId: "qa-invitation-draft") ?? []
        XCTAssertGreaterThanOrEqual(participantRecords.count, 2)
        let viewModel = EventAudienceViewModel(
            eventId: "qa-invitation-draft",
            repository: repository,
            database: database
        )
        await viewModel.reload()
        let visibleNames = participantRecords.map { viewModel.displayName(for: $0.id) }
        XCTAssertEqual(
            Set(visibleNames).count,
            participantRecords.count,
            "Seeded participants need distinct display names so Audience grouping/status is reviewable. Names: \(visibleNames)"
        )
        XCTAssertFalse(visibleNames.contains(String(localized: "participants.role.member")))
        for (record, visibleName) in zip(participantRecords, visibleNames) {
            XCTAssertNotEqual(visibleName, record.id)
            XCTAssertNotEqual(visibleName, record.userId)
            XCTAssertFalse(visibleName.contains("hmac-v"))
        }
    }
#endif

    private func sourceSlice(_ source: String, from start: String, to end: String) -> String {
        guard let startRange = source.range(of: start),
              let endRange = source.range(of: end, range: startRange.upperBound..<source.endIndex)
        else {
            return ""
        }
        return String(source[startRange.lowerBound..<endRange.lowerBound])
    }

    private func occurrences(of needle: String, in haystack: String) -> Int {
        guard !needle.isEmpty else { return 0 }
        return haystack.components(separatedBy: needle).count - 1
    }

    private func isDebugGuarded(_ needle: String, in source: String) -> Bool {
        guard let occurrence = source.range(of: needle) else { return false }
        let prefix = source[..<occurrence.lowerBound]
        guard let debugStart = prefix.range(of: "#if DEBUG", options: .backwards),
              let debugEnd = source.range(
                  of: "#endif",
                  range: debugStart.upperBound..<source.endIndex
              )
        else {
            return false
        }
        return occurrence.lowerBound < debugEnd.lowerBound
    }

    private func numericLiteral(after marker: String, in source: String) -> Double? {
        guard let markerRange = source.range(of: marker) else { return nil }
        let suffix = source[markerRange.upperBound...]
        let literal = suffix.prefix { character in
            character.isNumber || character == "."
        }
        return Double(String(literal))
    }

    private func localizedStringValue(for key: String, in source: String) -> String? {
        guard let line = source.split(separator: "\n").first(where: {
            $0.contains("\"\(key)\"")
        }),
              let equals = line.firstIndex(of: "=")
        else {
            return nil
        }
        return line[line.index(after: equals)...]
            .trimmingCharacters(in: .whitespacesAndNewlines)
            .trimmingCharacters(in: CharacterSet(charactersIn: ";\""))
    }

    private func readProjectFile(_ path: String) throws -> String {
        let repositoryRoot = repositoryRoot()
        return try String(
            contentsOf: repositoryRoot.appendingPathComponent(path),
            encoding: .utf8
        )
    }

    private func repositoryRoot() -> URL {
        URL(fileURLWithPath: #filePath)
            .deletingLastPathComponent()
            .deletingLastPathComponent()
            .deletingLastPathComponent()
    }
}
