import SwiftUI

// MARK: - Premium Liquid Glass Components

struct LiquidGlassCard<Content: View>: View {
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
        cornerRadius: CGFloat = WakeveTheme.Glass.cardRadius,
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
            .background(fallbackFill)
            .overlay(
                RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                    .stroke(borderColor, lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: cornerRadius, style: .continuous))
            .liquidGlass(cornerRadius: cornerRadius)
            .shadow(
                color: prominence == .subtle ? .clear : WakeveTheme.Shadow.card.color,
                radius: prominence == .prominent ? 24 : 16,
                x: 0,
                y: prominence == .prominent ? 14 : 8
            )
    }

    private var fallbackFill: Color {
        switch prominence {
        case .subtle:
            return WakeveTheme.ColorToken.subtleCardFill(for: colorScheme)
        case .regular:
            return WakeveTheme.ColorToken.glassTint(for: colorScheme)
        case .prominent:
            return WakeveTheme.ColorToken.secondaryBackground(for: colorScheme)
        }
    }

    private var borderColor: Color {
        WakeveTheme.ColorToken.cardBorder(for: colorScheme)
    }
}

struct LiquidGlassButton: View {
    enum Variant {
        case primary
        case secondary
        case neutral
        case destructive
        case confirmation
    }

    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

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
                } else {
                    if let systemImage {
                        Image(systemName: systemImage)
                            .font(.body.weight(.semibold))
                    }

                    Text(title)
                        .font(WakeveTheme.Typography.bodySemibold)
                        .lineLimit(1)
                        .minimumScaleFactor(0.82)
                }
            }
            .foregroundColor(foreground)
            .frame(maxWidth: .infinity)
            .frame(height: 54)
            .padding(.horizontal, WakeveTheme.Spacing.md)
            .background(background.opacity(isDisabled ? WakeveTheme.Opacity.disabled : 1))
            .clipShape(Capsule())
            .liquidGlass(cornerRadius: WakeveTheme.Glass.buttonRadius)
            .shadow(color: shadowColor, radius: isDisabled ? 0 : 14, x: 0, y: isDisabled ? 0 : 8)
        }
        .buttonStyle(.plain)
        .disabled(isDisabled || isLoading)
        .animation(reduceMotion ? nil : WakeveTheme.Motion.standardSpring, value: isDisabled)
    }

    private var background: Color {
        switch variant {
        case .primary:
            return colorScheme == .dark ? Color.white.opacity(0.92) : WakeveTheme.ColorToken.accent(for: colorScheme)
        case .secondary:
            return WakeveTheme.ColorToken.glassTint(for: colorScheme)
        case .neutral:
            return WakeveTheme.ColorToken.controlFill(for: colorScheme)
        case .destructive:
            return WakeveTheme.ColorToken.destructive(for: colorScheme)
        case .confirmation:
            return WakeveTheme.ColorToken.confirmation(for: colorScheme)
        }
    }

    private var foreground: Color {
        switch variant {
        case .primary:
            return colorScheme == .dark ? WakeveTheme.ColorToken.midnight : .white
        case .secondary, .neutral:
            return WakeveTheme.ColorToken.primaryText(for: colorScheme)
        case .destructive, .confirmation:
            return .white
        }
    }

    private var shadowColor: Color {
        switch variant {
        case .primary:
            return WakeveTheme.ColorToken.accent(for: colorScheme).opacity(0.26)
        case .destructive:
            return WakeveTheme.ColorToken.destructive(for: colorScheme).opacity(0.24)
        case .confirmation:
            return WakeveTheme.ColorToken.confirmation(for: colorScheme).opacity(0.22)
        case .secondary, .neutral:
            return .black.opacity(0.08)
        }
    }
}

struct LiquidGlassToolbar<Leading: View, Trailing: View>: View {
    @Environment(\.colorScheme) private var colorScheme

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

            VStack(alignment: .leading, spacing: WakeveTheme.Spacing.xxs) {
                Text(title)
                    .font(WakeveTheme.Typography.rowTitle)
                    .foregroundColor(WakeveTheme.ColorToken.primaryText(for: colorScheme))
                    .lineLimit(1)

                if let subtitle {
                    Text(subtitle)
                        .font(WakeveTheme.Typography.caption)
                        .foregroundColor(WakeveTheme.ColorToken.secondaryText(for: colorScheme))
                        .lineLimit(1)
                }
            }

            Spacer(minLength: WakeveTheme.Spacing.sm)
            trailing
        }
        .padding(.horizontal, WakeveTheme.Spacing.md)
        .frame(minHeight: 58)
        .background(WakeveTheme.ColorToken.glassTint(for: colorScheme))
        .clipShape(RoundedRectangle(cornerRadius: WakeveTheme.Glass.toolbarRadius, style: .continuous))
        .liquidGlass(cornerRadius: WakeveTheme.Glass.toolbarRadius)
        .accessibilityElement(children: .contain)
    }
}

struct LiquidGlassTabItem: Identifiable, Hashable {
    let id: String
    let title: String
    let systemImage: String
}

struct LiquidGlassTabBar: View {
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    let items: [LiquidGlassTabItem]
    @Binding var selection: String

    var body: some View {
        HStack(spacing: WakeveTheme.Spacing.xs) {
            ForEach(items) { item in
                Button {
                    selection = item.id
                } label: {
                    VStack(spacing: WakeveTheme.Spacing.xxs) {
                        Image(systemName: item.systemImage)
                            .font(.system(size: 19, weight: .semibold))
                        Text(item.title)
                            .font(WakeveTheme.Typography.tiny)
                            .lineLimit(1)
                            .minimumScaleFactor(0.78)
                    }
                    .foregroundColor(selection == item.id ? selectedColor : secondaryColor)
                    .frame(maxWidth: .infinity)
                    .frame(height: 54)
                    .background(selection == item.id ? selectedFill : Color.clear)
                    .clipShape(Capsule())
                }
                .buttonStyle(.plain)
                .accessibilityLabel(item.title)
            }
        }
        .padding(WakeveTheme.Spacing.xs)
        .background(WakeveTheme.ColorToken.glassTint(for: colorScheme))
        .clipShape(RoundedRectangle(cornerRadius: WakeveTheme.Glass.tabBarRadius, style: .continuous))
        .liquidGlass(cornerRadius: WakeveTheme.Glass.tabBarRadius)
        .animation(reduceMotion ? nil : WakeveTheme.Motion.standardSpring, value: selection)
    }

    private var selectedColor: Color {
        colorScheme == .dark ? WakeveTheme.ColorToken.midnight : .white
    }

    private var secondaryColor: Color {
        WakeveTheme.ColorToken.secondaryText(for: colorScheme)
    }

    private var selectedFill: Color {
        colorScheme == .dark ? Color.white.opacity(0.9) : WakeveTheme.ColorToken.accent(for: colorScheme)
    }
}

struct EventHeroCard<Content: View>: View {
    @Environment(\.colorScheme) private var colorScheme

    let title: String
    let subtitle: String
    let metadata: String?
    let gradient: LinearGradient
    let content: Content

    init(
        title: String,
        subtitle: String,
        metadata: String? = nil,
        gradient: LinearGradient = WakeveTheme.EventGradient.invitation,
        @ViewBuilder content: () -> Content = { EmptyView() }
    ) {
        self.title = title
        self.subtitle = subtitle
        self.metadata = metadata
        self.gradient = gradient
        self.content = content()
    }

    var body: some View {
        ZStack(alignment: .bottomLeading) {
            gradient

            LinearGradient(
                colors: [.clear, .black.opacity(colorScheme == .dark ? 0.42 : 0.28)],
                startPoint: .center,
                endPoint: .bottom
            )

            VStack(alignment: .leading, spacing: WakeveTheme.Spacing.sm) {
                if let metadata {
                    Text(metadata)
                        .font(WakeveTheme.Typography.caption)
                        .foregroundColor(.white.opacity(0.78))
                        .textCase(.uppercase)
                }

                Text(title)
                    .font(WakeveTheme.Typography.hero)
                    .foregroundColor(.white)
                    .lineLimit(3)
                    .minimumScaleFactor(0.78)

                Text(subtitle)
                    .font(WakeveTheme.Typography.metadata)
                    .foregroundColor(.white.opacity(0.82))
                    .lineLimit(2)

                content
                    .padding(.top, WakeveTheme.Spacing.xs)
            }
            .padding(WakeveTheme.Spacing.xl)
        }
        .frame(maxWidth: .infinity)
        .frame(minHeight: 280)
        .clipShape(RoundedRectangle(cornerRadius: WakeveTheme.Radius.panel, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: WakeveTheme.Radius.panel, style: .continuous)
                .stroke(Color.white.opacity(WakeveTheme.Opacity.border), lineWidth: 1)
        )
    }
}

struct EventListRow: View {
    @Environment(\.colorScheme) private var colorScheme

    let title: String
    let subtitle: String
    let dateLabel: String
    let participantInitials: [String]
    let nextActionHint: String?
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: WakeveTheme.Spacing.md) {
                VStack(spacing: 2) {
                    Text(dateLabel)
                        .font(WakeveTheme.Typography.caption)
                        .foregroundColor(WakeveTheme.ColorToken.eventHighlight(for: colorScheme))
                        .multilineTextAlignment(.center)
                        .lineLimit(2)
                }
                .frame(width: 58, height: 58)
                .background(WakeveTheme.ColorToken.controlFill(for: colorScheme))
                .clipShape(RoundedRectangle(cornerRadius: WakeveTheme.Radius.md, style: .continuous))

                VStack(alignment: .leading, spacing: WakeveTheme.Spacing.xxs) {
                    Text(title)
                        .font(WakeveTheme.Typography.rowTitle)
                        .foregroundColor(WakeveTheme.ColorToken.primaryText(for: colorScheme))
                        .lineLimit(2)

                    Text(subtitle)
                        .font(WakeveTheme.Typography.callout)
                        .foregroundColor(WakeveTheme.ColorToken.secondaryText(for: colorScheme))
                        .lineLimit(1)

                    if let nextActionHint {
                        Text(nextActionHint)
                            .font(WakeveTheme.Typography.caption)
                            .foregroundColor(WakeveTheme.ColorToken.accent(for: colorScheme))
                            .lineLimit(1)
                    }
                }

                Spacer(minLength: WakeveTheme.Spacing.sm)

                ParticipantAvatarStack(initials: participantInitials, size: 30, maxVisible: 3)
            }
            .padding(WakeveTheme.Spacing.md)
            .background(WakeveTheme.ColorToken.secondaryBackground(for: colorScheme))
            .clipShape(RoundedRectangle(cornerRadius: WakeveTheme.Radius.xl, style: .continuous))
        }
        .buttonStyle(.plain)
        .accessibilityElement(children: .combine)
    }
}

struct ParticipantAvatarStack: View {
    let initials: [String]
    let size: CGFloat
    let maxVisible: Int

    init(initials: [String], size: CGFloat = 34, maxVisible: Int = 4) {
        self.initials = initials
        self.size = size
        self.maxVisible = maxVisible
    }

    var body: some View {
        HStack(spacing: -size * 0.32) {
            ForEach(Array(initials.prefix(maxVisible).enumerated()), id: \.offset) { index, value in
                WakeveAvatar(initials: value, size: size)
                    .overlay(Circle().stroke(Color.white.opacity(0.34), lineWidth: 1.5))
                    .zIndex(Double(maxVisible - index))
            }

            if initials.count > maxVisible {
                Text("+\(initials.count - maxVisible)")
                    .font(WakeveTheme.Typography.tiny)
                    .foregroundColor(.white)
                    .frame(width: size, height: size)
                    .background(Color.black.opacity(0.34))
                    .clipShape(Circle())
            }
        }
        .accessibilityLabel("\(initials.count) participants")
    }
}

struct VoteOptionCard: View {
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    let vote: PollVote
    let title: String
    let subtitle: String
    let isSelected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: WakeveTheme.Spacing.md) {
                Image(systemName: icon)
                    .font(.system(size: 20, weight: .bold))
                    .foregroundColor(iconForeground)
                    .frame(width: 44, height: 44)
                    .background(iconBackground)
                    .clipShape(Circle())

                VStack(alignment: .leading, spacing: WakeveTheme.Spacing.xxs) {
                    Text(title)
                        .font(WakeveTheme.Typography.bodySemibold)
                        .foregroundColor(WakeveTheme.ColorToken.primaryText(for: colorScheme))

                    Text(subtitle)
                        .font(WakeveTheme.Typography.callout)
                        .foregroundColor(WakeveTheme.ColorToken.secondaryText(for: colorScheme))
                }

                Spacer()

                if isSelected {
                    Image(systemName: "checkmark.circle.fill")
                        .font(.system(size: 22, weight: .semibold))
                        .foregroundColor(WakeveTheme.ColorToken.confirmation(for: colorScheme))
                }
            }
            .padding(WakeveTheme.Spacing.md)
            .background(isSelected ? selectedFill : WakeveTheme.ColorToken.secondaryBackground(for: colorScheme))
            .overlay(
                RoundedRectangle(cornerRadius: WakeveTheme.Radius.xl, style: .continuous)
                    .stroke(isSelected ? WakeveTheme.ColorToken.accent(for: colorScheme).opacity(0.58) : WakeveTheme.ColorToken.cardBorder(for: colorScheme), lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: WakeveTheme.Radius.xl, style: .continuous))
        }
        .buttonStyle(.plain)
        .scaleEffect(isSelected && !reduceMotion ? 1.018 : 1)
        .animation(reduceMotion ? nil : WakeveTheme.Motion.confirmationSpring, value: isSelected)
        .accessibilityElement(children: .combine)
    }

    private var icon: String {
        switch vote {
        case .yes: return "checkmark"
        case .maybe: return "questionmark"
        case .no: return "xmark"
        }
    }

    private var iconForeground: Color {
        isSelected ? .white : voteColor
    }

    private var iconBackground: Color {
        isSelected ? voteColor : voteColor.opacity(0.16)
    }

    private var selectedFill: Color {
        WakeveTheme.ColorToken.accent(for: colorScheme).opacity(colorScheme == .dark ? 0.18 : 0.12)
    }

    private var voteColor: Color {
        switch vote {
        case .yes: return WakeveTheme.ColorToken.confirmation(for: colorScheme)
        case .maybe: return WakeveTheme.ColorToken.eventHighlight(for: colorScheme)
        case .no: return WakeveTheme.ColorToken.destructive(for: colorScheme)
        }
    }
}

struct BottomSheet<Content: View>: View {
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    let isPresented: Bool
    let onDismiss: () -> Void
    let content: Content

    init(
        isPresented: Bool,
        onDismiss: @escaping () -> Void,
        @ViewBuilder content: () -> Content
    ) {
        self.isPresented = isPresented
        self.onDismiss = onDismiss
        self.content = content()
    }

    var body: some View {
        if isPresented {
            ZStack(alignment: .bottom) {
                Color.black.opacity(WakeveTheme.Opacity.scrim)
                    .ignoresSafeArea()
                    .onTapGesture(perform: onDismiss)

                VStack(spacing: WakeveTheme.Spacing.lg) {
                    Capsule()
                        .fill(WakeveTheme.ColorToken.secondaryText(for: colorScheme).opacity(0.34))
                        .frame(width: 42, height: 5)
                        .padding(.top, WakeveTheme.Spacing.sm)

                    content
                }
                .padding(WakeveTheme.Spacing.lg)
                .frame(maxWidth: .infinity)
                .background(WakeveTheme.ColorToken.secondaryBackground(for: colorScheme))
                .clipShape(
                    UnevenRoundedRectangle(
                        topLeadingRadius: WakeveTheme.Glass.bottomSheetRadius,
                        topTrailingRadius: WakeveTheme.Glass.bottomSheetRadius,
                        style: .continuous
                    )
                )
                .liquidGlass(cornerRadius: WakeveTheme.Glass.bottomSheetRadius)
            }
            .transition(reduceMotion ? .opacity : .move(edge: .bottom).combined(with: .opacity))
            .animation(reduceMotion ? .easeInOut(duration: WakeveTheme.Motion.quick) : WakeveTheme.Motion.sheetSpring, value: isPresented)
        }
    }
}

struct EmptyState: View {
    @Environment(\.colorScheme) private var colorScheme

    let systemImage: String
    let title: String
    let subtitle: String
    let actionTitle: String?
    let action: (() -> Void)?

    init(
        systemImage: String = "calendar",
        title: String,
        subtitle: String,
        actionTitle: String? = nil,
        action: (() -> Void)? = nil
    ) {
        self.systemImage = systemImage
        self.title = title
        self.subtitle = subtitle
        self.actionTitle = actionTitle
        self.action = action
    }

    var body: some View {
        VStack(spacing: WakeveTheme.Spacing.lg) {
            Image(systemName: systemImage)
                .font(.system(size: 44, weight: .semibold))
                .foregroundColor(WakeveTheme.ColorToken.accent(for: colorScheme))
                .frame(width: 82, height: 82)
                .background(WakeveTheme.ColorToken.glassTint(for: colorScheme))
                .clipShape(RoundedRectangle(cornerRadius: WakeveTheme.Radius.xl, style: .continuous))
                .liquidGlass(cornerRadius: WakeveTheme.Radius.xl)
                .accessibilityHidden(true)

            VStack(spacing: WakeveTheme.Spacing.xs) {
                Text(title)
                    .font(WakeveTheme.Typography.section)
                    .foregroundColor(WakeveTheme.ColorToken.primaryText(for: colorScheme))
                    .multilineTextAlignment(.center)

                Text(subtitle)
                    .font(WakeveTheme.Typography.body)
                    .foregroundColor(WakeveTheme.ColorToken.secondaryText(for: colorScheme))
                    .multilineTextAlignment(.center)
                    .lineSpacing(3)
            }

            if let actionTitle, let action {
                LiquidGlassButton(actionTitle, systemImage: "plus", variant: .primary, action: action)
                    .frame(maxWidth: 260)
            }
        }
        .padding(WakeveTheme.Spacing.xl)
        .frame(maxWidth: .infinity)
    }
}

struct LoadingSkeleton: View {
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    let rows: Int
    let showsHero: Bool

    init(rows: Int = 3, showsHero: Bool = true) {
        self.rows = rows
        self.showsHero = showsHero
    }

    var body: some View {
        VStack(spacing: WakeveTheme.Spacing.md) {
            if showsHero {
                skeletonBlock(height: 220, radius: WakeveTheme.Radius.panel)
            }

            ForEach(0..<rows, id: \.self) { _ in
                HStack(spacing: WakeveTheme.Spacing.md) {
                    skeletonBlock(width: 58, height: 58, radius: WakeveTheme.Radius.md)

                    VStack(alignment: .leading, spacing: WakeveTheme.Spacing.xs) {
                        skeletonBlock(height: 16, radius: WakeveTheme.Radius.sm)
                        skeletonBlock(width: 180, height: 12, radius: WakeveTheme.Radius.sm)
                    }

                    Spacer()
                }
                .padding(WakeveTheme.Spacing.md)
                .background(WakeveTheme.ColorToken.secondaryBackground(for: colorScheme))
                .clipShape(RoundedRectangle(cornerRadius: WakeveTheme.Radius.xl, style: .continuous))
            }
        }
        .redacted(reason: .placeholder)
        .modifier(SkeletonMotionModifier(isEnabled: !reduceMotion))
    }

    private func skeletonBlock(width: CGFloat? = nil, height: CGFloat, radius: CGFloat) -> some View {
        RoundedRectangle(cornerRadius: radius, style: .continuous)
            .fill(WakeveTheme.ColorToken.skeletonFill(for: colorScheme))
            .frame(width: width, height: height)
    }
}

private struct SkeletonMotionModifier: ViewModifier {
    let isEnabled: Bool

    func body(content: Content) -> some View {
        if isEnabled {
            content.shimmerEffect()
        } else {
            content
        }
    }
}

#Preview("Premium Liquid Glass Components") {
    ZStack {
        WakeveScreenBackground(style: .event)

        ScrollView {
            VStack(spacing: WakeveTheme.Spacing.lg) {
                LiquidGlassToolbar(title: "A venir", subtitle: "3 moments") {
                    Image(systemName: "chevron.left")
                } trailing: {
                    Image(systemName: "plus")
                }

                EventHeroCard(title: "Diner a Lyon", subtitle: "Vendredi soir", metadata: "Prochain")

                EventListRow(
                    title: "Week-end amis",
                    subtitle: "Annecy",
                    dateLabel: "12 Jun",
                    participantInitials: ["A", "J", "M"],
                    nextActionHint: "Vote en cours",
                    action: {}
                )

                VoteOptionCard(vote: .yes, title: "Oui", subtitle: "Ce creneau me convient", isSelected: true, action: {})

                EmptyState(title: "Aucun evenement", subtitle: "Cree ton premier moment partage.", actionTitle: "Creer", action: {})

                LoadingSkeleton(rows: 2)
            }
            .padding()
        }
    }
    .preferredColorScheme(.dark)
}
