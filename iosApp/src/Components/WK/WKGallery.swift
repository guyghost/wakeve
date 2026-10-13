import SwiftUI

#if DEBUG
/// Galerie de composants WK — à vérifier en clair, sombre, AX5, Reduce Transparency et Increase Contrast.
struct WKGallery: View {
    @State private var zone: AppZone = .events

    private let people = [
        WKAvatar(id: "1", name: "Léa Martin"), WKAvatar(id: "2", name: "Tom Durand"),
        WKAvatar(id: "3", name: "Max"), WKAvatar(id: "4", name: "Sam"), WKAvatar(id: "5", name: "Zoé"),
        WKAvatar(id: "6", name: "Ana")
    ]

    var body: some View {
        ZStack(alignment: .bottom) {
            ScrollView {
                VStack(spacing: WK.Space.sm) {
                    HStack {
                        WKCircleButton(systemImage: "chevron.left", accessibilityLabel: "Retour") {}
                        Spacer()
                        WKCircleButton(systemImage: "gearshape", accessibilityLabel: "Réglages") {}
                    }
                    WKHeroMetric(caption: "Prochaine étape · Week-end Annecy", value: "5", unit: "/8",
                                 subtitle: "votes reçus · clôture dans 2 j", actionTitle: "Relancer les 3") {}
                    ViewThatFits {
                        HStack { statusPills }
                        VStack(alignment: .leading) { statusPills }
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    WKAvatarStack(avatars: people)
                        .frame(maxWidth: .infinity, alignment: .leading)
                    LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: WK.Space.xs) {
                        WKModuleTile(systemImage: "calendar", title: "Date", summary: "Sam. 18 · 6 oui", isHighlighted: true) {}
                        WKModuleTile(systemImage: "mappin", title: "Lieu", summary: "2 options") {}
                        WKModuleTile(systemImage: "car", title: "Transport", summary: "2 sans place", status: .pending) {}
                        WKModuleTile(systemImage: "eurosign", title: "Budget", summary: "~120 € / pers.") {}
                    }
                    WKCard(style: .inset) {
                        Text("Carte inset").font(WK.Typo.body).foregroundStyle(WK.Colors.textPrimary)
                    }
                    ViewThatFits {
                        HStack { chips }
                        VStack(alignment: .leading) { chips }
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    WKActionBar(primaryTitle: "Proposer", primaryAction: {}, secondary: [
                        .init(systemImage: "person.badge.plus", label: "Assigner") {},
                        .init(systemImage: "bubble.left", label: "Commenter") {},
                        .init(systemImage: "square.and.arrow.up", label: "Partager") {}
                    ])
                    WKPrimaryButton(title: "Confirmer le 18 oct") {}
                }
                .padding(WK.Space.screen)
                // Dégage la barre flottante (cible tactile + anneau) en fin de défilement.
                .padding(.bottom, WK.Size.minTapTarget * 2 + WK.Space.xs)
            }
            WKFloatingNavBar(selection: $zone, activityBadge: 3) {}
                .padding(.bottom, WK.Space.xs)
        }
        .background(WK.Colors.canvas)
    }

    @ViewBuilder private var statusPills: some View {
        ForEach(WK.Status.allCases, id: \.self) { status in
            WKStatusPill(text: status.localizedName, status: status)
        }
    }

    @ViewBuilder private var chips: some View {
        WKChip(title: "Oui", isSelected: true) {}
        WKChip(title: "Peut-être") {}
        WKChip(title: "Non") {}
    }
}

#Preview("Clair") { WKGallery() }
#Preview("Sombre") { WKGallery().preferredColorScheme(.dark) }
#Preview("AX5") { WKGallery().dynamicTypeSize(.accessibility5) }
#Preview("Reduce Transparency") {
    // Le setter public est en lecture seule ; la variante soulignée est réservée aux previews.
    WKGallery().environment(\._accessibilityReduceTransparency, true)
}
#Preview("Increase Contrast") {
    // Même contrainte : `colorSchemeContrast` est en lecture seule, la variante soulignée est réservée aux previews.
    WKGallery().environment(\._colorSchemeContrast, .increased)
}

#Preview("Sheet de module") {
    Color.clear.sheet(isPresented: .constant(true)) {
        WKModuleSheet(
            title: "Repas",
            status: .init(text: "2/5 prêts", status: .pending),
            missing: "3 repas sans responsable",
            primary: (title: "Ajouter un repas", action: {}),
            secondary: [
                .init(systemImage: "arrow.up.left.and.arrow.down.right", label: "Plein écran", accessibilityID: "full") {},
                .init(systemImage: "bubble.left", label: "Commentaires", accessibilityID: "comments") {}
            ],
            onClose: {}
        ) {
            WKCard(style: .inset) {
                Text("Barbecue du samedi soir").font(WK.Typo.headline).foregroundStyle(WK.Colors.textPrimary)
                Text("sam. 3 oct. · 19:30").font(WK.Typo.caption).foregroundStyle(WK.Colors.textMuted)
            }
        }
    }
}

/// Galerie du mode immersif (couche 8) : chaque ambiance, avec carte, pastilles et CTA.
struct WKImmersiveGallery: View {
    var mood: EventMoodPalette.Mood = .evening

    var body: some View {
        let palette = EventMoodPalette.palette(for: mood)
        let tokens = WK.Mood(palette: palette)
        WKImmersiveScaffold(
            mood: tokens,
            primary: .init(title: "Itinéraire", systemImage: "arrow.triangle.turn.up.right.diamond") {},
            secondary: .init(title: "Voir l'événement") {},
            onClose: {}
        ) {
            Text("C'est aujourd'hui · \(palette.name)").font(WK.Typo.caption).foregroundStyle(tokens.textSecondary)
            Text("19:30").font(WK.Typo.display).foregroundStyle(tokens.textPrimary)
            Text("Week-end à Annecy").font(WK.Typo.title).foregroundStyle(tokens.textPrimary)
            WKImmersiveCard(mood: tokens) {
                Text("Gare d'Annecy").font(WK.Typo.headline).foregroundStyle(tokens.textPrimary)
                Text("Place de la Gare, 74000 Annecy").font(WK.Typo.caption).foregroundStyle(tokens.textSecondary)
            }
            ViewThatFits {
                HStack { pills(tokens) }
                VStack(alignment: .leading) { pills(tokens) }
            }
        }
    }

    @ViewBuilder private func pills(_ tokens: WK.Mood) -> some View {
        WKImmersivePill(text: "Plan choisi", systemImage: "car", mood: tokens)
        WKImmersivePill(text: "Dîner · 20:00", systemImage: "fork.knife", mood: tokens)
        WKImmersivePill(text: "6 confirmés", systemImage: "person.2", mood: tokens)
    }
}

#Preview("Immersif") {
    TabView {
        ForEach(EventMoodPalette.Mood.allCases) { mood in
            WKImmersiveGallery(mood: mood)
        }
    }
    .tabViewStyle(.page)
}
#Preview("Immersif AX5") { WKImmersiveGallery(mood: .beach).dynamicTypeSize(.accessibility5) }
#Preview("Immersif Reduce Transparency") {
    WKImmersiveGallery(mood: .birthday).environment(\._accessibilityReduceTransparency, true)
}
#Preview("Immersif Increase Contrast") {
    WKImmersiveGallery(mood: .family).environment(\._colorSchemeContrast, .increased)
}

#Preview("Mood immersif") {
    let mood = WK.Mood(palette: .palette(for: .evening))
    return VStack(alignment: .leading, spacing: WK.Space.xs) {
        Text("19:30").font(WK.Typo.display).foregroundStyle(mood.textPrimary)
        Text("Rendez-vous gare d'Annecy").font(WK.Typo.body).foregroundStyle(mood.textPrimary)
        Text("4 arrivés · Léa à 12 min").font(WK.Typo.caption).foregroundStyle(mood.textSecondary)
    }
    .padding(WK.Space.lg)
    .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottomLeading)
    .background(mood.background)
}
#endif
