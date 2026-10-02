import SwiftUI

/// Météo du jour retenu dans le hub (revue couche 9, #47).
struct EventHubWeatherCard: View {
    static let accessibilityID = "hub.weather"

    let state: EventWeatherState

    var body: some View {
        switch state {
        case .hidden:
            EmptyView()
        case .loading:
            WKCard(padding: WK.Space.md, radius: WK.Radius.lg) {
                HStack(spacing: WK.Space.xs) {
                    ProgressView()
                        .accessibilityHidden(true)
                    Text(String(localized: "weather.loading"))
                        .font(WK.Typo.caption)
                        .foregroundStyle(WK.Colors.textSecondary)
                }
                .accessibilityElement(children: .combine)
            }
            .wkAccessibilityID(Self.accessibilityID)
        case .unavailable:
            WKCard(padding: WK.Space.md, radius: WK.Radius.lg) {
                Label(String(localized: "hub.weather.unavailable"), systemImage: "cloud")
                    .font(WK.Typo.caption)
                    .foregroundStyle(WK.Colors.textSecondary)
                    .fixedSize(horizontal: false, vertical: true)
            }
            .wkAccessibilityID(Self.accessibilityID)
        case .available(let display):
            WKCard(padding: WK.Space.md, radius: WK.Radius.lg) {
                HStack(alignment: .top, spacing: WK.Space.sm) {
                    Image(systemName: display.symbolName)
                        .symbolRenderingMode(.multicolor)
                        .font(WK.Typo.title)
                        .accessibilityHidden(true)
                    VStack(alignment: .leading, spacing: WK.Space.xxs) {
                        Text(EventWeatherText.title(display))
                            .font(WK.Typo.headline)
                            .foregroundStyle(WK.Colors.textPrimary)
                        Text(display.condition)
                            .font(WK.Typo.body)
                            .foregroundStyle(WK.Colors.textPrimary)
                        Text("\(EventWeatherText.temperatures(display)) · \(EventWeatherText.rain(display))")
                            .font(WK.Typo.caption)
                            .foregroundStyle(WK.Colors.textSecondary)
                    }
                    .fixedSize(horizontal: false, vertical: true)
                }
                .accessibilityElement(children: .combine)
            }
            .wkAccessibilityID(Self.accessibilityID)
        }
    }
}
