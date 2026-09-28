import SwiftUI

struct WKAvatar: Identifiable, Equatable {
    let id: String
    let name: String
    var imageURL: URL? = nil

    var initials: String {
        let letters = name
            .split(separator: " ")
            .prefix(2)
            .compactMap { $0.first.map { String($0).uppercased() } }
            .joined()
        return letters.isEmpty ? "?" : letters
    }

    private static let tintPalette: [UInt32] = [0xDCD6F7, 0xBDE8CF, 0xF7C9C4, 0xFBE3B8, 0xCFE3F7, 0xF3D1E6]

    /// Index de palette stable et sans risque de dépassement, dérivé de l'identifiant.
    static func tintIndex(for id: String, paletteCount: Int) -> Int {
        let sum = id.unicodeScalars.reduce(UInt(0)) { $0 &+ UInt($1.value) }
        return Int(sum % UInt(paletteCount))
    }

    /// Teinte stable dérivée de l'identifiant (même personne = même couleur partout).
    var tint: Color {
        let index = Self.tintIndex(for: id, paletteCount: Self.tintPalette.count)
        return Color(uiColor: WK.uiColor(Self.tintPalette[index]))
    }
}

struct WKAvatarStack: View {
    let avatars: [WKAvatar]
    var maxVisible: Int = 4
    var size: CGFloat? = nil

    @ScaledMetric(relativeTo: .caption) private var scaledSize: CGFloat = WK.Size.avatar

    private var side: CGFloat { size ?? scaledSize }

    static func layout(count: Int, maxVisible: Int) -> (visible: Int, overflow: Int) {
        let visible = min(count, maxVisible)
        return (visible, max(0, count - visible))
    }

    /// Texte localisé "N autres" pour la locale demandée, indépendant de la langue de l'app.
    static func othersText(_ count: Int, locale: Locale) -> String {
        let bundle = locale.language.languageCode
            .flatMap { Bundle.main.path(forResource: $0.identifier, ofType: "lproj") }
            .flatMap(Bundle.init(path:)) ?? .main
        let format = bundle.localizedString(forKey: "wk.avatars.others_format", value: nil, table: nil)
        return String(format: format, locale: locale, count)
    }

    static func accessibilityLabel(for avatars: [WKAvatar], locale: Locale = .current) -> String {
        let formatter = ListFormatter()
        formatter.locale = locale
        let names = avatars
            .map { $0.name.trimmingCharacters(in: .whitespacesAndNewlines) }
            .filter { !$0.isEmpty }
        let parts: [String]
        if names.count <= 3 {
            parts = names
        } else {
            let others = othersText(names.count - 2, locale: locale)
            parts = Array(names.prefix(2)) + [others]
        }
        return formatter.string(from: parts) ?? parts.joined(separator: ", ")
    }

    var body: some View {
        let layout = Self.layout(count: avatars.count, maxVisible: maxVisible)
        HStack(spacing: -side * 0.3) {
            ForEach(avatars.prefix(layout.visible)) { avatar in
                avatarView(avatar)
            }
            if layout.overflow > 0 {
                Text("+\(layout.overflow)")
                    .font(WK.Typo.micro.weight(.medium))
                    .foregroundStyle(WK.Colors.onCardInset)
                    .padding(.horizontal, WK.Space.xs)
                    .frame(minWidth: side, minHeight: side)
                    .background(WK.Colors.cardInset, in: Capsule())
                    .overlay(Capsule().stroke(WK.Colors.card, lineWidth: 1.5))
            }
        }
        .dynamicTypeSize(...DynamicTypeSize.accessibility1)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(Self.accessibilityLabel(for: avatars))
        .accessibilityHidden(avatars.isEmpty)
    }

    @ViewBuilder
    private func avatarView(_ avatar: WKAvatar) -> some View {
        ZStack {
            Circle().fill(avatar.tint)
            if let url = avatar.imageURL {
                AsyncImage(url: url) { image in
                    image.resizable().scaledToFill()
                } placeholder: {
                    initialsView(avatar)
                }
                .clipShape(Circle())
            } else {
                initialsView(avatar)
            }
        }
        .frame(width: side, height: side)
        .overlay(Circle().stroke(WK.Colors.card, lineWidth: 1.5))
    }

    private func initialsView(_ avatar: WKAvatar) -> some View {
        Text(avatar.initials)
            .font(WK.Typo.micro.weight(.semibold))
            .foregroundStyle(Color(uiColor: WK.uiColor(0x1C1C1E)))
            .minimumScaleFactor(0.6)
    }
}
