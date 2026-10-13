import SwiftUI
import Shared

/// Total artwork renderer shared by the studio, Archive and the invitation landing. Missing or unavailable
/// remote imagery falls back to the event mood without rewriting persisted state.
struct InvitationArtworkView: View {
    let artwork: any Artwork
    let event: Event

    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        Group {
            if let legacy = artwork as? ArtworkLegacyRemote,
               let url = URL(string: legacy.validatedHttpsUrl) {
                remote(url: url, crop: .fill, focalPoint: nil)
            } else if let structured = artwork as? ArtworkStructured,
                      let server = structured.ref.source as? ArtworkSourceServerAsset,
                      let url = URL(string: server.canonicalHttpsUrl) {
                remote(
                    url: url,
                    crop: structured.ref.crop,
                    focalPoint: structured.ref.focalPoint
                )
            } else if let structured = artwork as? ArtworkStructured,
                      let preset = structured.ref.source as? ArtworkSourcePreset {
                presetArtwork(
                    presetId: preset.presetId,
                    crop: structured.ref.crop,
                    focalPoint: structured.ref.focalPoint
                )
            } else {
                fallback
            }
        }
        .accessibilityHidden(true)
    }

    @ViewBuilder
    private func remote(
        url: URL,
        crop: ArtworkCrop,
        focalPoint: ArtworkFocalPoint?
    ) -> some View {
        AsyncImage(url: url) { phase in
            if let image = phase.image {
                if crop == .fit {
                    image
                        .resizable()
                        .scaledToFit()
                        .frame(
                            maxWidth: .infinity,
                            maxHeight: .infinity,
                            alignment: focalAlignment(focalPoint)
                        )
                } else {
                    image
                        .resizable()
                        .scaledToFill()
                        .frame(
                            maxWidth: .infinity,
                            maxHeight: .infinity,
                            alignment: focalAlignment(focalPoint)
                        )
                        .clipped()
                }
            } else {
                fallback
            }
        }
    }

    @ViewBuilder
    private func presetArtwork(
        presetId: String,
        crop: ArtworkCrop,
        focalPoint: ArtworkFocalPoint
    ) -> some View {
        if crop == .fit {
            presetImage(presetId: presetId)
                .scaledToFit()
                .frame(
                    maxWidth: .infinity,
                    maxHeight: .infinity,
                    alignment: focalAlignment(focalPoint)
                )
        } else {
            presetImage(presetId: presetId)
                .scaledToFill()
                .frame(
                    maxWidth: .infinity,
                    maxHeight: .infinity,
                    alignment: focalAlignment(focalPoint)
                )
                .clipped()
        }
    }

    @ViewBuilder
    private func presetImage(presetId: String) -> some View {
        switch presetId {
        case "wakeve-lake":
            Image("InvitationPresetLake")
                .resizable()
        case "wakeve-sunset":
            Image("InvitationPresetSunset")
                .resizable()
        case "wakeve-celebration":
            Image("InvitationPresetCelebration")
                .resizable()
        default:
            fallback
        }
    }

    private func focalAlignment(_ focalPoint: ArtworkFocalPoint?) -> Alignment {
        guard let focalPoint else { return .center }
        let horizontal: HorizontalAlignment = focalPoint.x < 0.34
            ? .leading
            : (focalPoint.x > 0.66 ? .trailing : .center)
        let vertical: VerticalAlignment = focalPoint.y < 0.34
            ? .top
            : (focalPoint.y > 0.66 ? .bottom : .center)
        return Alignment(horizontal: horizontal, vertical: vertical)
    }

    private var fallback: some View {
        let palette = EventMoodPalette.palette(for: event.eventType.name)
        return ZStack {
            palette.gradient(for: colorScheme)
            Image(systemName: palette.symbolName)
                .font(.title2)
                .foregroundStyle(.white)
        }
    }
}
