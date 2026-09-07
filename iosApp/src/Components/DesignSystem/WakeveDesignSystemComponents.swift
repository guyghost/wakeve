import SwiftUI

// MARK: - Screen Background

struct WakeveScreenBackground: View {
    enum Style {
        case app
        case utility
        case profile
        case event
        case grouped
    }

    @Environment(\.colorScheme) private var colorScheme

    let style: Style

    var body: some View {
        Group {
            switch style {
            case .app:
                SemanticColor.appBackground(for: colorScheme)
            case .utility:
                utilityBackground
            case .profile:
                WakeveTheme.EventGradient.profile
            case .event:
                eventBackground
            case .grouped:
                SemanticColor.appBackground(for: colorScheme)
            }
        }
        .ignoresSafeArea()
    }

    private var utilityBackground: LinearGradient {
        if colorScheme == .dark {
            return WakeveTheme.EventGradient.utility
        }

        return LinearGradient(
            colors: [
                WakeveTheme.ColorToken.softIvory,
                Color(hex: "EEF4F8"),
                WakeveTheme.ColorToken.paleBlue.opacity(0.36)
            ],
            startPoint: .topLeading,
            endPoint: .bottomTrailing
        )
    }

    private var eventBackground: LinearGradient {
        if colorScheme == .dark {
            return WakeveTheme.EventGradient.invitation
        }

        return LinearGradient(
            colors: [
                BrandColor.softIvory,
                Color(hex: "EEF0EC"),
                BrandColor.paleBlue.opacity(0.34),
                BrandColor.softIvory
            ],
            startPoint: .top,
            endPoint: .bottom
        )
    }
}

// MARK: - Content Surface Card

struct WakeveContentCard<Content: View>: View {
    enum Prominence {
        case subtle
        case regular
        case prominent
    }

    @Environment(\.colorScheme) private var colorScheme

    let prominence: Prominence
    let cornerRadius: CGFloat
    let padding: CGFloat
    let content: Content

    init(
        prominence: Prominence = .regular,
        cornerRadius: CGFloat = WakeveTheme.Radius.xl,
        padding: CGFloat = WakeveTheme.Spacing.lg,
        @ViewBuilder content: () -> Content
    ) {
        self.prominence = prominence
        self.cornerRadius = cornerRadius
        self.padding = padding
        self.content = content()
    }

    var body: some View {
        content
            .padding(padding)
            .background(fill)
            .overlay(
                RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                    .stroke(SemanticColor.border(for: colorScheme), lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: cornerRadius, style: .continuous))
            .shadow(
                color: prominence == .subtle ? .clear : WakeveTheme.Shadow.subtle.color,
                radius: prominence == .prominent ? 18 : 10,
                x: 0,
                y: prominence == .prominent ? 10 : 5
            )
    }

    private var fill: Color {
        switch prominence {
        case .subtle:
            return SemanticColor.contentSurface(for: colorScheme).opacity(colorScheme == .dark ? 0.68 : 0.76)
        case .regular:
            return SemanticColor.contentSurface(for: colorScheme)
        case .prominent:
            return colorScheme == .dark ? BrandColor.midnightBlueRaised : Color.white
        }
    }
}

struct WakeveGlassCard<Content: View>: View {
    enum Prominence {
        case subtle
        case regular
        case prominent
    }

    @Environment(\.colorScheme) private var colorScheme

    let prominence: Prominence
    let cornerRadius: CGFloat
    let padding: CGFloat
    let content: Content

    init(
        prominence: Prominence = .regular,
        cornerRadius: CGFloat = WakeveTheme.Radius.xl,
        padding: CGFloat = WakeveTheme.Spacing.lg,
        @ViewBuilder content: () -> Content
    ) {
        self.prominence = prominence
        self.cornerRadius = cornerRadius
        self.padding = padding
        self.content = content()
    }

    var body: some View {
        content
            .padding(padding)
            .background(fill)
            .overlay(
                RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                    .stroke(SemanticColor.border(for: colorScheme), lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: cornerRadius, style: .continuous))
            .shadow(
                color: prominence == .subtle ? .clear : WakeveTheme.Shadow.card.color,
                radius: prominence == .prominent ? 26 : 18,
                x: WakeveTheme.Shadow.card.x,
                y: prominence == .prominent ? 14 : 10
            )
    }

    private var fill: Color {
        switch prominence {
        case .subtle:
            return SemanticColor.contentSurface(for: colorScheme).opacity(colorScheme == .dark ? 0.72 : 0.78)
        case .regular:
            return SemanticColor.contentSurface(for: colorScheme)
        case .prominent:
            return colorScheme == .dark ? BrandColor.midnightBlueRaised : Color.white
        }
    }
}

// MARK: - Glass Control Surface

struct WakeveGlassControl<Content: View>: View {
    let cornerRadius: CGFloat
    let content: Content

    init(cornerRadius: CGFloat = WakeveTheme.Radius.full, @ViewBuilder content: () -> Content) {
        self.cornerRadius = cornerRadius
        self.content = content()
    }

    var body: some View {
        content
            .background(Color.white.opacity(0.14))
            .overlay(
                RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                    .stroke(Color.white.opacity(0.18), lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: cornerRadius, style: .continuous))
            .liquidGlass(cornerRadius: cornerRadius)
    }
}

// MARK: - Event Panel

struct WakeveEventPanel<Content: View>: View {
    @Environment(\.colorScheme) private var colorScheme

    let cornerRadius: CGFloat
    let padding: CGFloat
    let content: Content

    init(
        cornerRadius: CGFloat = WakeveTheme.Radius.xl,
        padding: CGFloat = WakeveTheme.Spacing.lg,
        @ViewBuilder content: () -> Content
    ) {
        self.cornerRadius = cornerRadius
        self.padding = padding
        self.content = content()
    }

    var body: some View {
        content
            .padding(padding)
            .background(panelFill)
            .overlay(
                RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                    .stroke(WakeveTheme.ColorToken.cardBorder(for: colorScheme), lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: cornerRadius, style: .continuous))
    }

    private var panelFill: Color {
        colorScheme == .dark
            ? WakeveTheme.ColorToken.eventNightElevated.opacity(0.82)
            : WakeveTheme.ColorToken.appLightElevated.opacity(0.94)
    }
}

// MARK: - Action Button

struct WakeveActionButton: View {
    enum Variant {
        case primary
        case secondary
        case neutral
        case eventNext
        case destructive
    }

    @Environment(\.colorScheme) private var colorScheme

    let title: String
    let systemImage: String?
    let variant: Variant
    let isDisabled: Bool
    let isLoading: Bool
    let action: () -> Void

    init(
        _ title: String,
        systemImage: String? = nil,
        variant: Variant = .primary,
        isDisabled: Bool = false,
        isLoading: Bool = false,
        action: @escaping () -> Void
    ) {
        self.title = title
        self.systemImage = systemImage
        self.variant = variant
        self.isDisabled = isDisabled
        self.isLoading = isLoading
        self.action = action
    }

    var body: some View {
        Button(action: action) {
            HStack(spacing: WakeveTheme.Spacing.xs) {
                if isLoading {
                    ProgressView()
                        .tint(foreground)
                        .accessibilityHidden(true)
                } else {
                    if let systemImage {
                        Image(systemName: systemImage)
                            .font(.system(size: 16, weight: .bold))
                    }

                    Text(title)
                        .font(WakeveTheme.Typography.bodySemibold)
                }
            }
            .foregroundColor(foreground)
            .frame(maxWidth: .infinity)
            .frame(height: 56)
            .background(background.opacity(isDisabled ? 0.42 : 1))
            .clipShape(Capsule())
            .shadow(
                color: isDisabled ? .clear : shadowColor,
                radius: 14,
                x: 0,
                y: 8
            )
        }
        .buttonStyle(.plain)
        .disabled(isDisabled || isLoading)
    }

    private var background: Color {
        switch variant {
        case .primary:
            // Token bleu canonique #2563EB — DAO #26 (l'ancien bleu dérivé #3F8FF2 est retiré)
            return colorScheme == .dark ? Color.white.opacity(0.92) : Color.wakevePrimary
        case .secondary:
            return Color.clear
        case .neutral:
            return colorScheme == .dark ? WakeveTheme.ColorToken.neutralCapsuleDark : Color.black.opacity(0.12)
        case .eventNext:
            return WakeveTheme.ColorToken.eventLilacAction
        case .destructive:
            return WakeveColors.error
        }
    }

    private var foreground: Color {
        switch variant {
        case .primary:
            return colorScheme == .dark ? WakeveTheme.ColorToken.appDark : .white
        case .secondary:
            // Texte bleu aligné sur le token canonique (DAO #26, AC 5)
            return Color.wakevePrimary
        case .neutral:
            return colorScheme == .dark ? .white : WakeveTheme.ColorToken.primaryText(for: colorScheme)
        case .eventNext:
            return WakeveTheme.ColorToken.eventLilacText
        case .destructive:
            return .white
        }
    }

    private var shadowColor: Color {
        switch variant {
        case .primary:
            return Color.wakevePrimary.opacity(0.28)
        case .eventNext:
            return WakeveTheme.ColorToken.eventLilacAction.opacity(0.32)
        case .destructive:
            return WakeveColors.error.opacity(0.28)
        case .secondary, .neutral:
            return .black.opacity(0.08)
        }
    }
}

// MARK: - Circular Icon Button

struct WakeveCircleButton: View {
    enum Variant {
        case glass
        case light
        case confirm
        case eventBack
    }

    let systemImage: String
    let accessibilityLabel: String
    let variant: Variant
    let size: CGFloat
    let action: () -> Void

    init(
        systemImage: String,
        accessibilityLabel: String,
        variant: Variant = .glass,
        size: CGFloat = 58,
        action: @escaping () -> Void
    ) {
        self.systemImage = systemImage
        self.accessibilityLabel = accessibilityLabel
        self.variant = variant
        self.size = size
        self.action = action
    }

    var body: some View {
        Button(action: action) {
            Image(systemName: systemImage)
                .font(.system(size: size * 0.38, weight: .bold))
                .foregroundColor(foreground)
                .frame(width: size, height: size)
                .background(background)
                .clipShape(Circle())
                .overlay(
                    Circle()
                        .stroke(border, lineWidth: 1)
                )
                .shadow(
                    color: WakeveTheme.Shadow.control.color,
                    radius: WakeveTheme.Shadow.control.radius,
                    x: WakeveTheme.Shadow.control.x,
                    y: WakeveTheme.Shadow.control.y
                )
        }
        .buttonStyle(.plain)
        .frame(minWidth: 44, minHeight: 44)
        .contentShape(Circle())
        .accessibilityLabel(accessibilityLabel)
    }

    private var background: Color {
        switch variant {
        case .glass:
            return Color.white.opacity(0.14)
        case .light:
            return Color.white.opacity(0.94)
        case .confirm:
            return Color(hex: "FFF169")
        case .eventBack:
            return Color(hex: "9427E8").opacity(0.86)
        }
    }

    private var foreground: Color {
        switch variant {
        case .glass, .eventBack:
            return .white
        case .light:
            return WakeveTheme.ColorToken.appDark
        case .confirm:
            return Color(hex: "5F2500")
        }
    }

    private var border: Color {
        switch variant {
        case .glass, .eventBack:
            return Color.white.opacity(0.18)
        case .light:
            return Color.black.opacity(0.06)
        case .confirm:
            return Color.white.opacity(0.46)
        }
    }
}

// MARK: - Screen Header

/// En-tête d'écran standard — Proposition Swarm DAO #24.
/// Pattern (modèle écran Résultats) : bouton circulaire 44pt en haut à gauche,
/// grand titre display aligné à gauche, sous-titre secondaire portant le contexte
/// (nom de l'événement). Accessoire optionnel en haut à droite (ex. refresh).
struct WakeveScreenHeader<Trailing: View>: View {
    private let title: String
    private let subtitle: String?
    private let closeButtonSystemImage: String
    private let closeButtonAccessibilityLabel: String
    private let showsTopControls: Bool
    private let onClose: (() -> Void)?
    private let trailing: Trailing

    init(
        title: String,
        subtitle: String? = nil,
        closeButtonSystemImage: String = "xmark",
        closeButtonAccessibilityLabel: String,
        showsTopControls: Bool = true,
        onClose: (() -> Void)? = nil,
        @ViewBuilder trailing: () -> Trailing = { EmptyView() }
    ) {
        self.title = title
        self.subtitle = subtitle
        self.closeButtonSystemImage = closeButtonSystemImage
        self.closeButtonAccessibilityLabel = closeButtonAccessibilityLabel
        self.showsTopControls = showsTopControls
        self.onClose = onClose
        self.trailing = trailing()
    }

    var body: some View {
        VStack(spacing: 16) {
            if showsTopControls, onClose != nil {
                HStack(spacing: 12) {
                    WakeveCircleButton(
                        systemImage: closeButtonSystemImage,
                        accessibilityLabel: closeButtonAccessibilityLabel,
                        variant: .light,
                        size: 44,
                        action: onClose ?? {}
                    )

                    Spacer()

                    trailing
                }
                .padding(.horizontal, 20)
                .padding(.top, WakeveTheme.Navigation.controlTopSpacing)
            }

            VStack(spacing: 8) {
                Text(title)
                    .font(WakeveTheme.Typography.display)
                    .foregroundColor(.primary)
                    .frame(maxWidth: .infinity, alignment: .leading)

                if let subtitle {
                    Text(subtitle)
                        .font(WakeveTheme.Typography.title2)
                        .foregroundColor(.secondary)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
            }
            .padding(.horizontal, 20)
        }
        .accessibilityElement(children: .contain)
    }
}

// MARK: - Secondary Icon Button

/// Action secondaire circulaire — Proposition Swarm DAO #26.
/// Cercle teinté bleu primaire (≈ #D8E3FB sur blanc), icône bleu #2563EB.
/// Usage : copier, partager, actions utilitaires des cartes.
struct WakeveSecondaryIconButton: View {
    let systemImage: String
    let accessibilityLabel: String
    let action: () -> Void
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        Button(action: action) {
            Image(systemName: systemImage)
                .font(.system(size: 17, weight: .bold))
                .foregroundColor(Color.wakevePrimary)
                .frame(width: 46, height: 46)
                .background(Color.wakevePrimary.opacity(colorScheme == .dark ? 0.22 : 0.12))
                .clipShape(Circle())
        }
        .buttonStyle(.plain)
        .frame(minWidth: 44, minHeight: 44)
        .contentShape(Circle())
        .accessibilityLabel(accessibilityLabel)
    }
}

// MARK: - Meta Chip

/// Chip de métadonnées standard — Proposition Swarm DAO #26.

/// Chip de métadonnées standard — Proposition Swarm DAO #26.
/// Icône 16pt + texte caption sur fond subtil capsule (fuseau, invités, échéance…).
struct WakeveMetaChip: View {
    let systemImage: String
    let label: String
    let foregroundColor: Color
    let backgroundColor: Color

    init(
        systemImage: String = "globe",
        label: String,
        foregroundColor: Color = .secondary,
        backgroundColor: Color = Color(.tertiarySystemFill)
    ) {
        self.systemImage = systemImage
        self.label = label
        self.foregroundColor = foregroundColor
        self.backgroundColor = backgroundColor
    }

    var body: some View {
        Label(label, systemImage: systemImage)
            .font(TypographyTokens.caption)
            .foregroundColor(foregroundColor)
            .lineLimit(1)
            .minimumScaleFactor(0.78)
            .padding(.horizontal, WakeveTheme.Spacing.sm)
            .frame(height: 30)
            .background(backgroundColor)
            .clipShape(Capsule())
    }
}

// MARK: - Hero Card

/// Carte héro « info clé confirmée » — Proposition Swarm DAO #25.
/// Bordure dorée standard (modèle Meilleur créneau) : réservée à l'information
/// confirmée d'un écran (meilleur créneau, date retenue, info finale, jour J).
/// Ne pas multiplier : la dorure doit rester un signal univoque.
struct WakeveHeroCard<Content: View>: View {
    private let content: Content
    @Environment(\.colorScheme) private var colorScheme

    init(@ViewBuilder content: () -> Content) {
        self.content = content()
    }

    var body: some View {
        content
            .padding(20)
            .frame(maxWidth: .infinity)
            .glassCard(cornerRadius: 20)
            .overlay(
                RoundedRectangle(cornerRadius: 20, style: .continuous)
                    .stroke(SemanticColor.warning(for: colorScheme).opacity(0.3), lineWidth: 2)
            )
    }
}

// MARK: - Search Field

struct WakeveSearchField: View {
    @Environment(\.colorScheme) private var colorScheme

    let placeholder: String
    @Binding var text: String
    let trailingSystemImage: String?
    let trailingAction: (() -> Void)?

    init(
        placeholder: String,
        text: Binding<String>,
        trailingSystemImage: String? = nil,
        trailingAction: (() -> Void)? = nil
    ) {
        self.placeholder = placeholder
        self._text = text
        self.trailingSystemImage = trailingSystemImage
        self.trailingAction = trailingAction
    }

    var body: some View {
        HStack(spacing: WakeveTheme.Spacing.sm) {
            Image(systemName: "magnifyingglass")
                .font(.system(size: 22, weight: .semibold))
                .foregroundColor(.white.opacity(colorScheme == .dark ? 0.86 : 0.58))

            TextField(placeholder, text: $text)
                .font(WakeveTheme.Typography.rowTitle)
                .foregroundColor(WakeveTheme.ColorToken.primaryText(for: colorScheme))
                .textFieldStyle(.plain)

            if let trailingSystemImage {
                Button {
                    trailingAction?()
                } label: {
                    Image(systemName: trailingSystemImage)
                        .font(.system(size: 24, weight: .medium))
                        .foregroundColor(.white.opacity(colorScheme == .dark ? 0.9 : 0.62))
                }
                .buttonStyle(.plain)
                .accessibilityLabel(trailingSystemImage == "mic" ? String(localized: "common.dictation") : String(localized: "common.search_action_accessibility"))
            }
        }
        .padding(.horizontal, WakeveTheme.Spacing.lg)
        .frame(height: 56)
        .background(colorScheme == .dark ? WakeveTheme.ColorToken.searchFieldDark : WakeveTheme.ColorToken.searchFieldLight)
        .clipShape(Capsule())
    }
}

// MARK: - Avatar

struct WakeveAvatar: View {
    let initials: String
    let imageName: String?
    let size: CGFloat
    let badgeSystemImage: String?

    init(
        initials: String,
        imageName: String? = nil,
        size: CGFloat = 64,
        badgeSystemImage: String? = nil
    ) {
        self.initials = initials
        self.imageName = imageName
        self.size = size
        self.badgeSystemImage = badgeSystemImage
    }

    var body: some View {
        ZStack(alignment: .bottomTrailing) {
            Group {
                if let imageName {
                    Image(imageName)
                        .resizable()
                        .scaledToFill()
                } else {
                    Circle()
                        .fill(avatarGradient)
                        .overlay(
                            Text(initials)
                                .font(.system(size: size * 0.38, weight: .bold))
                                .foregroundColor(.white)
                        )
                }
            }
            .frame(width: size, height: size)
            .clipShape(Circle())

            if let badgeSystemImage {
                Circle()
                    .fill(Color(hex: "F05560"))
                    .frame(width: size * 0.36, height: size * 0.36)
                    .overlay(
                        Image(systemName: badgeSystemImage)
                            .font(.system(size: size * 0.16, weight: .bold))
                            .foregroundColor(.white)
                    )
                    .offset(x: size * 0.04, y: size * 0.04)
            }
        }
        .accessibilityHidden(true)
    }

    private var avatarGradient: LinearGradient {
        LinearGradient(
            colors: [Color(hex: "B9CBF5"), Color(hex: "8795D8")],
            startPoint: .topLeading,
            endPoint: .bottomTrailing
        )
    }
}

struct WakeveStackedAvatars: View {
    let initials: [String]

    var body: some View {
        HStack(spacing: -14) {
            ForEach(Array(initials.prefix(4).enumerated()), id: \.offset) { index, value in
                WakeveAvatar(initials: value, size: 54)
                    .overlay(Circle().stroke(Color.white.opacity(0.18), lineWidth: 3))
                    .zIndex(Double(initials.count - index))
            }
        }
        .accessibilityLabel(String.localizedStringWithFormat(String(localized: "participants.count_accessibility"), initials.count))
    }
}

// MARK: - Rows and Sections

struct WakeveSectionHeader: View {
    let title: String

    var body: some View {
        Text(title)
            .font(WakeveTheme.Typography.section)
            .foregroundColor(.primary)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, WakeveTheme.Spacing.page)
            .accessibilityAddTraits(.isHeader)
    }
}

struct WakeveListRow<Leading: View, Trailing: View>: View {
    let title: String
    let subtitle: String?
    let leading: Leading
    let trailing: Trailing

    init(
        title: String,
        subtitle: String? = nil,
        @ViewBuilder leading: () -> Leading,
        @ViewBuilder trailing: () -> Trailing
    ) {
        self.title = title
        self.subtitle = subtitle
        self.leading = leading()
        self.trailing = trailing()
    }

    var body: some View {
        HStack(spacing: WakeveTheme.Spacing.md) {
            leading
                .frame(width: 68, height: 68)

            VStack(alignment: .leading, spacing: WakeveTheme.Spacing.xxs) {
                Text(title)
                    .font(WakeveTheme.Typography.rowTitle)
                    .foregroundColor(.primary)
                    .lineLimit(2)
                    .minimumScaleFactor(0.82)

                if let subtitle {
                    Text(subtitle)
                        .font(WakeveTheme.Typography.metadata)
                        .foregroundColor(.secondary)
                        .lineLimit(2)
                }
            }

            Spacer(minLength: WakeveTheme.Spacing.sm)

            trailing
        }
        .padding(.vertical, WakeveTheme.Spacing.sm)
        .contentShape(Rectangle())
    }
}

// MARK: - Vote Control

struct WakeveSegmentedVoteControl: View {
    let selectedVote: PollVote?
    let onVoteSelected: (PollVote) -> Void

    var body: some View {
        HStack(spacing: 0) {
            voteSegment(vote: .yes, icon: "checkmark.circle", label: "Oui")
            Divider().background(Color.white.opacity(0.16))
            voteSegment(vote: .no, icon: "xmark.circle", label: "Non")
            Divider().background(Color.white.opacity(0.16))
            voteSegment(vote: .maybe, icon: "questionmark.circle", label: "Peut-être")
        }
        .frame(height: 86)
        .background(Color.black.opacity(0.12))
        .overlay(
            Capsule()
                .stroke(Color.white.opacity(0.16), lineWidth: 1)
        )
        .clipShape(Capsule())
    }

    private func voteSegment(vote: PollVote, icon: String, label: String) -> some View {
        Button {
            onVoteSelected(vote)
        } label: {
            VStack(spacing: WakeveTheme.Spacing.xs) {
                Image(systemName: icon)
                    .font(.system(size: 22, weight: .bold))
                Text(label)
                    .font(WakeveTheme.Typography.bodySemibold)
            }
            .foregroundColor(selectedVote == vote ? .white : Color.white.opacity(0.58))
            .frame(maxWidth: .infinity)
        }
        .buttonStyle(.plain)
        .accessibilityLabel(label)
    }
}

#Preview("Wakeve Design System") {
    ZStack {
        WakeveScreenBackground(style: .event)
        VStack(spacing: 20) {
            WakeveSearchField(placeholder: "Trouver des contacts", text: .constant(""), trailingSystemImage: "mic")
            WakeveGlassCard {
                WakeveListRow(
                    title: "Tous les amis",
                    subtitle: "Total : 23",
                    leading: {
                        Image(systemName: "person.2.fill")
                            .font(.system(size: 34, weight: .bold))
                            .foregroundColor(.white.opacity(0.82))
                    },
                    trailing: {
                        WakeveStackedAvatars(initials: ["A", "J", "M"])
                    }
                )
            }
            WakeveActionButton("Continuer") {}
            WakeveSegmentedVoteControl(selectedVote: .yes) { _ in }
        }
        .padding()
    }
    .preferredColorScheme(.dark)
}
