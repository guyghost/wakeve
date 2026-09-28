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

    /// Teinte stable dérivée de l'identifiant (même personne = même couleur partout).
    var tint: Color {
        let palette: [UInt32] = [0xDCD6F7, 0xBDE8CF, 0xF7C9C4, 0xFBE3B8, 0xCFE3F7, 0xF3D1E6]
        let index = abs(id.unicodeScalars.reduce(0) { $0 &+ Int($1.value) }) % palette.count
        return Color(uiColor: WK.uiColor(palette[index]))
    }
}

struct WKAvatarStack: View {
    let avatars: [WKAvatar]
    var maxVisible: Int = 4
    var size: CGFloat = WK.Size.avatar

    static func layout(count: Int, maxVisible: Int) -> (visible: Int, overflow: Int) {
        let visible = min(count, maxVisible)
        return (visible, max(0, count - visible))
    }

    static func accessibilityLabel(for avatars: [WKAvatar], locale: Locale = .current) -> String {
        let formatter = ListFormatter()
        formatter.locale = locale
        let names = avatars.map(\.name)
        let parts: [String]
        if names.count <= 3 {
            parts = names
        } else {
            let others = String(format: String(localized: "wk.avatars.others_format"), names.count - 2)
            parts = Array(names.prefix(2)) + [others]
        }
        return formatter.string(from: parts) ?? parts.joined(separator: ", ")
    }

    var body: some View {
        let layout = Self.layout(count: avatars.count, maxVisible: maxVisible)
        HStack(spacing: -size * 0.3) {
            ForEach(avatars.prefix(layout.visible)) { avatar in
                avatarView(avatar)
            }
            if layout.overflow > 0 {
                Text("+\(layout.overflow)")
                    .font(WK.Typo.micro.weight(.medium))
                    .foregroundStyle(WK.Colors.textSecondary)
                    .padding(.horizontal, WK.Space.xs)
                    .frame(minWidth: size, minHeight: size)
                    .background(WK.Colors.cardInset, in: Capsule())
                    .overlay(Capsule().stroke(WK.Colors.card, lineWidth: 1.5))
            }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(Self.accessibilityLabel(for: avatars))
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
        .frame(width: size, height: size)
        .overlay(Circle().stroke(WK.Colors.card, lineWidth: 1.5))
    }

    private func initialsView(_ avatar: WKAvatar) -> some View {
        Text(avatar.initials)
            .font(WK.Typo.micro.weight(.semibold))
            .foregroundStyle(Color(uiColor: WK.uiColor(0x1C1C1E)))
            .minimumScaleFactor(0.6)
    }
}
