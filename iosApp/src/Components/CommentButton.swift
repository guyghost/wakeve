import SwiftUI

/// Bouton de commentaires avec design Liquid Glass
/// Suit le pattern Functional Core & Imperative Shell
struct CommentButton: View {
    let commentCount: Int
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            HStack(spacing: 8) {
                // Icône avec effet Liquid Glass
                ZStack {
                    // Fond de l'icône
                    Circle()
                        .fill(
                            LinearGradient(
                                colors: [
                                    Color.white.opacity(0.15),
                                    Color.white.opacity(0.08)
                                ],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            )
                        )
                        .frame(width: 40, height: 40)
                    
                    // Icône de commentaire
                    Image(systemName: "bubble.right.fill")
                        .font(.system(size: 18, weight: .medium))
                        .foregroundColor(.wakevePrimary)
                }
                .overlay(
                    Circle()
                        .stroke(
                            LinearGradient(
                                colors: [
                                    Color.white.opacity(0.3),
                                    Color.white.opacity(0.1)
                                ],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            ),
                            lineWidth: 1
                        )
                )
                
                // Badge du compteur de commentaires
                LiquidGlassBadge(text: "\(commentCount)", style: .info)
            }
            .padding(.horizontal, 4)
            .padding(.vertical, 4)
        }
        .accessibilityLabel("Commentaires")
        .accessibilityHint(commentCount == 0 ? "Aucun commentaire, appuyez pour commenter" : "\(commentCount) commentaires, appuyez pour voir")
    }
}

// MARK: - Preview

#Preview("CommentButton - With Comments") {
    VStack(spacing: 30) {
        CommentButton(commentCount: 5) {
            print("Comment button tapped")
        }
        
        CommentButton(commentCount: 0) {
            print("Comment button tapped")
        }
        
        CommentButton(commentCount: 99) {
            print("Comment button tapped")
        }
    }
    .padding()
    .background(Color.wakeveBackgroundDark)
}

#Preview("CommentButton - In Context") {
    VStack(spacing: 16) {
        // Simuler une carte d'événement
        VStack(alignment: .leading, spacing: 12) {
            Text("Réunion d'équipe")
                .font(.headline)
                .foregroundColor(.wakeveTextPrimaryLight)
            
            Text("Discussion sur les objectifs du trimestre")
                .font(.subheadline)
                .foregroundColor(.wakeveTextSecondaryLight)
            
            HStack {
                CommentButton(commentCount: 12) {}
                
                Spacer()
                
                // Autres actions
                HStack(spacing: 16) {
                    Image(systemName: "heart.fill")
                        .foregroundColor(.wakeveError)
                    
                    Image(systemName: "square.and.arrow.up")
                        .foregroundColor(.wakevePrimary)
                }
            }
        }
        .padding(20)
        .background(
            RoundedRectangle(cornerRadius: 20)
                .fill(Color.wakeveSurfaceDark)
        )
        .overlay(
            RoundedRectangle(cornerRadius: 20)
                .stroke(Color.wakeveBorderDark.opacity(0.3), lineWidth: 1)
        )
    }
    .padding()
    .background(Color.wakeveBackgroundDark)
}
