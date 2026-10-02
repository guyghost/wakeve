import MapKit
import SwiftUI

/// Jour J en mode immersif (couche 8, #47) : heure du rendez-vous en très grand, date, lieu (carte si les
/// coordonnées sont connues), pastilles logistiques, participants ; « Itinéraire » puis « Voir l'événement ».
/// Informations statiques : la présence en direct n'existe pas encore (spec §5.6).
struct EventDayView: View {
    static let accessibilityID = "immersive.eventDay"
    static let mapAccessibilityID = "immersive.eventDay.map"
    static let timeAccessibilityID = "immersive.eventDay.time"

    let model: EventDayModel
    let onDirections: () -> Void
    let onViewEvent: () -> Void
    let onClose: () -> Void

    var body: some View {
        let mood = WK.Mood(palette: model.palette)
        let hasDirections = model.directions != nil
        WKImmersiveScaffold(
            mood: mood,
            primary: WKImmersiveAction(
                title: model.primaryTitle,
                systemImage: hasDirections ? "arrow.triangle.turn.up.right.diamond" : "arrow.right",
                action: hasDirections ? onDirections : onViewEvent
            ),
            secondary: model.secondaryTitle.map { WKImmersiveAction(title: $0, action: onViewEvent) },
            onClose: onClose
        ) {
            Text(model.caption)
                .font(WK.Typo.headline)
                .foregroundStyle(mood.textSecondary)
            if let time = model.time {
                Text(time)
                    .font(WK.Typo.display)
                    .foregroundStyle(mood.textPrimary)
                    .fixedSize(horizontal: false, vertical: true)
                    .wkAccessibilityID(Self.timeAccessibilityID)
            }
            Text(model.title)
                .font(WK.Typo.title)
                .foregroundStyle(mood.textPrimary)
                .fixedSize(horizontal: false, vertical: true)
                .accessibilityAddTraits(.isHeader)
            if let date = model.date {
                Text([date, model.until].compactMap { $0 }.joined(separator: " · "))
                    .font(WK.Typo.body)
                    .foregroundStyle(mood.textSecondary)
                    .fixedSize(horizontal: false, vertical: true)
            }
            WKImmersiveCard(mood: mood) {
                if let coordinate = model.coordinate {
                    map(latitude: coordinate.latitude, longitude: coordinate.longitude)
                }
                Label {
                    Text(model.placeName)
                        .font(WK.Typo.headline)
                        .foregroundStyle(mood.textPrimary)
                        .fixedSize(horizontal: false, vertical: true)
                } icon: {
                    Image(systemName: "mappin.and.ellipse")
                        .foregroundStyle(mood.accent)
                        .accessibilityHidden(true)
                }
                if let address = model.placeAddress {
                    Text(address)
                        .font(WK.Typo.caption)
                        .foregroundStyle(mood.textSecondary)
                        .fixedSize(horizontal: false, vertical: true)
                }
            }
            if !model.pills.isEmpty {
                VStack(alignment: .leading, spacing: WK.Space.xs) {
                    ForEach(model.pills) { pill in
                        WKImmersivePill(text: pill.text, systemImage: pill.systemImage, mood: mood)
                    }
                }
            }
        }
        .wkAccessibilityID(Self.accessibilityID)
    }

    /// Carte compacte, non interactive (l'itinéraire passe par Plans).
    private func map(latitude: Double, longitude: Double) -> some View {
        let center = CLLocationCoordinate2D(latitude: latitude, longitude: longitude)
        return Map(
            initialPosition: .region(MKCoordinateRegion(center: center, latitudinalMeters: 1_500, longitudinalMeters: 1_500)),
            interactionModes: []
        ) {
            Marker(model.placeName, coordinate: center)
        }
        .frame(height: WK.Size.immersiveMedia)
        .clipShape(WK.shape(WK.Radius.md))
        .accessibilityElement()
        .accessibilityLabel(model.mapLabel ?? model.placeName)
        .wkAccessibilityID(Self.mapAccessibilityID)
    }
}

/// Ouvre Plans : itinéraire vers les coordonnées, ou recherche de l'adresse par son nom.
enum EventDayDirectionsLauncher {
    @MainActor
    static func open(_ directions: EventDayDirections, openURL: OpenURLAction) {
        switch directions {
        case .coordinate(let latitude, let longitude, let name):
            let item: MKMapItem
            if #available(iOS 26.0, *) {
                item = MKMapItem(location: CLLocation(latitude: latitude, longitude: longitude), address: nil)
            } else {
                item = MKMapItem(placemark: MKPlacemark(coordinate: CLLocationCoordinate2D(latitude: latitude, longitude: longitude)))
            }
            item.name = name
            item.openInMaps(launchOptions: [MKLaunchOptionsDirectionsModeKey: MKLaunchOptionsDirectionsModeDefault])
        case .search:
            if let url = directions.searchURL { openURL(url) }
        }
    }
}

/// Charge les faits du jour J (source injectable) et publie leur présentation.
@MainActor
final class EventDayViewModel: ObservableObject {
    enum State: Equatable { case loading, loaded, failed }

    @Published private(set) var state: State = .loading
    @Published private(set) var model: EventDayModel?

    private let eventId: String
    private let viewerId: String
    private let source: EventDaySource
    private var generation = 0

    init(eventId: String, viewerId: String, source: EventDaySource) {
        self.eventId = eventId
        self.viewerId = viewerId
        self.source = source
    }

    func reload() async {
        generation += 1
        let token = generation
        do {
            let facts = try await source.loadEventDay(eventId: eventId, viewerId: viewerId, now: Date())
            guard token == generation else { return }
            model = EventDayModel(facts: facts)
            state = .loaded
        } catch {
            guard token == generation, !(error is CancellationError) else { return }
            state = model == nil ? .failed : .loaded
        }
    }
}

/// Présenté en plein écran depuis le hub (« C'est aujourd'hui ») ou l'accueil (« Voir le jour J »).
struct EventDayContainer: View {
    @StateObject private var viewModel: EventDayViewModel
    let onViewEvent: () -> Void
    let onClose: () -> Void

    @Environment(\.openURL) private var openURL

    /// L'écran s'ouvre seulement quand `isEventDay` l'a autorisé (hub, accueil) ; il ne revérifie pas la règle.
    init(eventId: String, userId: String, onViewEvent: @escaping () -> Void, onClose: @escaping () -> Void) {
        _viewModel = StateObject(wrappedValue: EventDayViewModel(eventId: eventId, viewerId: userId, source: SharedEventDaySource()))
        self.onViewEvent = onViewEvent
        self.onClose = onClose
    }

    var body: some View {
        Group {
            if viewModel.state == .loaded, let model = viewModel.model {
                EventDayView(
                    model: model,
                    onDirections: {
                        if let directions = model.directions {
                            EventDayDirectionsLauncher.open(directions, openURL: openURL)
                        }
                    },
                    onViewEvent: onViewEvent,
                    onClose: onClose
                )
            } else {
                placeholder
            }
        }
        // Mode immersif toujours sombre : barre d'état claire sur le fond teinté.
        .preferredColorScheme(.dark)
        .task { await viewModel.reload() }
    }

    private var placeholder: some View {
        let mood = WK.Mood(palette: .weekend)
        let failed = viewModel.state == .failed
        return WKImmersiveScaffold(
            mood: mood,
            primary: failed ? WKImmersiveAction(title: String(localized: "immersive.view_event"), action: onViewEvent) : nil,
            onClose: onClose
        ) {
            if failed {
                Text(String(localized: "common.error_generic"))
                    .font(WK.Typo.body)
                    .foregroundStyle(mood.textPrimary)
            } else {
                ProgressView()
                    .tint(mood.textPrimary)
                    .frame(maxWidth: .infinity)
                    .padding(.top, WK.Space.xl)
                    .accessibilityLabel(String(localized: "common.loading"))
            }
        }
    }
}
