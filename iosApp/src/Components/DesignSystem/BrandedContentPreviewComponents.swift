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
