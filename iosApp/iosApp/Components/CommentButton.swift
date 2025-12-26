import SwiftUI

struct CommentButton: View {
    let commentCount: Int
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            ZStack(alignment: .topTrailing) {
                Image(systemName: "bubble.right.fill")
                    .foregroundColor(.primary)
                    .font(.system(size: 20))

                if commentCount > 0 {
                    Text("\(commentCount)")
                        .font(.caption2.bold())
                        .foregroundColor(.white)
                        .padding(.horizontal, 6)
                        .padding(.vertical, 2)
                        .background(Color.red)
                        .clipShape(Capsule())
                        .offset(x: 8, y: -8)
                }
            }
        }
        .accessibilityLabel("Commentaires")
        .accessibilityHint(commentCount == 0 ? "Aucun commentaire" : "\(commentCount) commentaires")
    }
}