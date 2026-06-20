import SwiftUI
#if canImport(UIKit)
import UIKit
#endif

struct WakeveInvitationPreviewCard: View {
    @Environment(\.colorScheme) private var colorScheme

    let title: String
    let subtitle: String
    let inviteUrl: String
    let moodPalette: EventMoodPalette
    let qrImage: UIImage?

    var body: some View {
        WakeveContentCard(prominence: .prominent, cornerRadius: WakeveTheme.Radius.xl, padding: WakeveTheme.Spacing.lg) {
            VStack(alignment: .leading, spacing: WakeveTheme.Spacing.md) {
                HStack(alignment: .top, spacing: WakeveTheme.Spacing.md) {
                    ZStack {
                        RoundedRectangle(cornerRadius: WakeveTheme.Radius.lg, style: .continuous)
                            .fill(moodPalette.gradient(for: colorScheme))
                            .frame(width: 68, height: 68)

                        Image(systemName: moodPalette.symbolName)
                            .font(.title2.weight(.bold))
                            .foregroundColor(.white)
                    }
                    .accessibilityHidden(true)

                    VStack(alignment: .leading, spacing: WakeveTheme.Spacing.xxs) {
                        Text(title)
                            .font(TypographyTokens.cardTitle)
                            .foregroundColor(SemanticColor.primaryText(for: colorScheme))
                            .lineLimit(2)

                        Text(subtitle)
                            .font(TypographyTokens.callout)
                            .foregroundColor(SemanticColor.secondaryText(for: colorScheme))
                            .lineLimit(2)
                    }

                    Spacer(minLength: WakeveTheme.Spacing.xs)
                }

                HStack(spacing: WakeveTheme.Spacing.md) {
                    qrPreview

                    VStack(alignment: .leading, spacing: WakeveTheme.Spacing.xs) {
                        Text(moodPalette.microcopy)
                            .font(TypographyTokens.caption)
                            .foregroundColor(SemanticColor.secondaryText(for: colorScheme))
                            .lineLimit(2)

                        Text(inviteUrl)
                            .font(.system(.caption, design: .monospaced))
                            .foregroundColor(SemanticColor.tertiaryText(for: colorScheme))
                            .lineLimit(1)
                            .minimumScaleFactor(0.75)
                            .truncationMode(.middle)
                    }
                }
            }
        }
    }

    @ViewBuilder
    private var qrPreview: some View {
        if let qrImage {
            Image(uiImage: qrImage)
                .interpolation(.none)
                .resizable()
                .scaledToFit()
                .frame(width: 86, height: 86)
                .padding(WakeveTheme.Spacing.xs)
                .background(Color.white)
                .clipShape(RoundedRectangle(cornerRadius: WakeveTheme.Radius.md, style: .continuous))
        } else {
            RoundedRectangle(cornerRadius: WakeveTheme.Radius.md, style: .continuous)
                .fill(SemanticColor.badge(for: colorScheme))
                .frame(width: 102, height: 102)
                .overlay {
                    ProgressView()
                        .accessibilityLabel(String(localized: "common.loading"))
                }
        }
    }
}

struct WakeveGroupCard: View {
    @Environment(\.colorScheme) private var colorScheme

    let title: String
    let subtitle: String
    let count: Int
    let symbolName: String
    let content: AnyView

    init<Content: View>(
        title: String,
        subtitle: String,
        count: Int,
        symbolName: String = "person.2",
        @ViewBuilder content: () -> Content
    ) {
        self.title = title
        self.subtitle = subtitle
        self.count = count
        self.symbolName = symbolName
        self.content = AnyView(content())
    }

    var body: some View {
        WakeveContentCard(prominence: .regular, cornerRadius: WakeveTheme.Radius.xl, padding: WakeveTheme.Spacing.md) {
            VStack(alignment: .leading, spacing: WakeveTheme.Spacing.md) {
                HStack(alignment: .top, spacing: WakeveTheme.Spacing.sm) {
                    Image(systemName: symbolName)
                        .font(.headline.weight(.bold))
                        .foregroundColor(SemanticColor.selectedState(for: colorScheme))
                        .frame(width: 36, height: 36)
                        .background(SemanticColor.badge(for: colorScheme))
                        .clipShape(Circle())

                    VStack(alignment: .leading, spacing: WakeveTheme.Spacing.xxs) {
                        Text(title)
                            .font(TypographyTokens.cardTitle)
                            .foregroundColor(SemanticColor.primaryText(for: colorScheme))

                        Text(subtitle)
                            .font(TypographyTokens.callout)
                            .foregroundColor(SemanticColor.secondaryText(for: colorScheme))
                            .lineLimit(2)
                    }

                    Spacer()

                    Text("\(count)")
                        .font(TypographyTokens.caption)
                        .foregroundColor(SemanticColor.selectedState(for: colorScheme))
                        .frame(width: 32, height: 32)
                        .background(SemanticColor.badge(for: colorScheme))
                        .clipShape(Circle())
                }

                content
            }
        }
    }
}

struct WakeveWidgetPreviewCard: View {
    @Environment(\.colorScheme) private var colorScheme

    enum Kind: String, CaseIterable, Identifiable {
        case nextEvent
        case activeVote
        case guestList
        case notification
        case liveActivity

        var id: String { rawValue }
    }

    let kind: Kind
    let title: String
    let subtitle: String
    let moodPalette: EventMoodPalette
    let progress: Double?

    var body: some View {
        VStack(alignment: .leading, spacing: WakeveTheme.Spacing.sm) {
            HStack {
                Image(systemName: iconName)
                    .font(.headline.weight(.bold))
                    .foregroundColor(.white)
                    .frame(width: 34, height: 34)
                    .background(moodPalette.accent(for: colorScheme))
                    .clipShape(Circle())

                Spacer()

                Text("Wakeve")
                    .font(TypographyTokens.badge)
                    .foregroundColor(SemanticColor.secondaryText(for: colorScheme))
            }

            Text(title)
                .font(TypographyTokens.headline)
                .foregroundColor(SemanticColor.primaryText(for: colorScheme))
                .lineLimit(2)

            Text(subtitle)
                .font(TypographyTokens.caption)
                .foregroundColor(SemanticColor.secondaryText(for: colorScheme))
                .lineLimit(2)

            if let progress {
                ProgressView(value: min(max(progress, 0), 1))
                    .tint(SemanticColor.progress(for: colorScheme))
                    .accessibilityLabel(String(localized: "common.progress"))
            }
        }
        .padding(WakeveTheme.Spacing.md)
        .frame(width: 168, height: 168, alignment: .topLeading)
        .background(SemanticColor.contentSurface(for: colorScheme))
        .overlay(
            RoundedRectangle(cornerRadius: WakeveTheme.Radius.xl, style: .continuous)
                .stroke(SemanticColor.border(for: colorScheme), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: WakeveTheme.Radius.xl, style: .continuous))
    }

    private var iconName: String {
        switch kind {
        case .nextEvent:
            return "calendar"
        case .activeVote:
            return "chart.bar"
        case .guestList:
            return "person.3"
        case .notification:
            return "bell"
        case .liveActivity:
            return "livephoto"
        }
    }
}

struct WakeveWidgetPreviewGallery: View {
    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: WakeveTheme.Spacing.md) {
                WakeveWidgetPreviewCard(
                    kind: .nextEvent,
                    title: "Diner terrasse",
                    subtitle: "Vendredi, 8 invites",
                    moodPalette: .dinner,
                    progress: nil
                )

                WakeveWidgetPreviewCard(
                    kind: .activeVote,
                    title: "Vote en cours",
                    subtitle: "3 creneaux a departager",
                    moodPalette: .weekend,
                    progress: 0.62
                )

                WakeveWidgetPreviewCard(
                    kind: .guestList,
                    title: "Liste invites",
                    subtitle: "5 confirmes, 2 en attente",
                    moodPalette: .family,
                    progress: 0.71
                )
            }
            .padding(WakeveTheme.Spacing.page)
        }
    }
}
