import SwiftUI

// Color scheme for onboarding using Wakev design system
struct OnboardingColors {
    // Using hex values from WakevColors design system
    static let primary = Color(red: 0x25/255.0, green: 0x63/255.0, blue: 0xEB/255.0)      // #2563EB (wakevPrimary)
    static let primaryLight = Color(red: 0x25/255.0, green: 0x63/255.0, blue: 0xEB/255.0).opacity(0.15)
    static let success = Color(red: 0x05/255.0, green: 0x96/255.0, blue: 0x69/255.0)      // #059669 (wakevSuccess)
    static let primaryText = Color.primary
    static let secondaryText = Color.secondary
}

struct OnboardingStep {
    let title: String
    let description: String
    let icon: String
    let features: [String]
}

struct OnboardingStepView: View {
    let step: OnboardingStep
    
    @State private var isAnimating = false
    
    var body: some View {
        VStack(spacing: 24) {
            Spacer()
            
            // Icon with spring animation
            ZStack {
                Circle()
                    .fill(.ultraThinMaterial)
                    .frame(width: 120, height: 120)
                    .overlay(
                        Circle()
                            .fill(OnboardingColors.primary.opacity(0.1))
                    )
                
                Image(systemName: step.icon)
                    .resizable()
                    .scaledToFit()
                    .frame(width: 60, height: 60)
                    .foregroundColor(OnboardingColors.primary)
                    .scaleEffect(isAnimating ? 1.1 : 1.0)
                    .animation(
                        Animation.spring(response: 1.0, dampingFraction: 0.7)
                            .repeatForever(autoreverses: true),
                        value: isAnimating
                    )
            }
            .clipShape(Circle())
            .shadow(color: OnboardingColors.primary.opacity(0.2), radius: 20, x: 0, y: 8)
            .onAppear { isAnimating = true }
            
            // Title
            Text(step.title)
                .font(.largeTitle.weight(.bold))
                .multilineTextAlignment(.center)
                .foregroundColor(.primary)
            
            // Description
            Text(step.description)
                .font(.body)
                .multilineTextAlignment(.center)
                .foregroundColor(.secondary)
                .padding(.horizontal)
                .lineSpacing(1.5)
            
            // Features
            VStack(spacing: 12) {
                ForEach(step.features, id: \.self) { feature in
                    HStack(spacing: 12) {
                        Image(systemName: "checkmark.circle.fill")
                            .font(.system(size: 16, weight: .semibold))
                            .foregroundColor(OnboardingColors.success)
                        
                        Text(feature)
                            .font(.body)
                            .foregroundColor(.secondary)
                            .lineLimit(2)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                }
            }
            .padding(.horizontal)
            
            Spacer()
        }
        .padding(24)
        .background(.regularMaterial)
        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
        .shadow(color: .black.opacity(0.08), radius: 12, x: 0, y: 6)
        .padding()
    }
}

struct OnboardingView: View {
    @State private var currentPage = 0
    let onOnboardingComplete: () -> Void
    
    private let onboardingSteps = [
        OnboardingStep(
            title: "Créez vos événements",
            description: "Organisez facilement des événements entre amis et collègues. Définissez des dates, proposez des créneaux horaires et laissez les participants voter.",
            icon: "calendar",
            features: [
                "Création rapide d'événements",
                "Sondage de disponibilité",
                "Calcul automatique du meilleur créneau"
            ]
        ),
        OnboardingStep(
            title: "Collaborez en équipe",
            description: "Travaillez ensemble sur l'organisation de l'événement. Partagez les responsabilités et suivez la progression en temps réel.",
            icon: "person.2",
            features: [
                "Gestion des participants",
                "Attribution des tâches",
                "Suivi en temps réel"
            ]
        ),
        OnboardingStep(
            title: "Organisez tout en un",
            description: "Gérez l'hébergement, les repas, les activités et le budget. Tout au même endroit pour une organisation sans faille.",
            icon: "target",
            features: [
                "Planification d'hébergement",
                "Organisation des repas",
                "Suivi du budget"
            ]
        ),
        OnboardingStep(
            title: "Profitez de vos événements",
            description: "Une fois l'organisation terminée, profitez de l'événement avec vos proches sans stress.",
            icon: "sparkles",
            features: [
                "Vue d'ensemble",
                "Rappels intégrés",
                "Calendrier natif"
            ]
        )
    ]
    
    var body: some View {
        ZStack {
            TabView(selection: $currentPage) {
                ForEach(0..<onboardingSteps.count, id: \.self) { index in
                    OnboardingStepView(step: onboardingSteps[index])
                        .tag(index)
                }
            }
            #if os(iOS)
            .tabViewStyle(.page(indexDisplayMode: .automatic))
            #endif
            .background(.ultraThinMaterial)
            
            // Bottom buttons
            VStack {
                Spacer()
                HStack(spacing: 12) {
                    Button(action: onOnboardingComplete) {
                        Text("Passer")
                            .font(.subheadline.weight(.semibold))
                            .foregroundColor(OnboardingColors.primary)
                            .frame(maxWidth: .infinity)
                            .frame(height: 44)
                            .background(.ultraThinMaterial)
                            .overlay(
                                RoundedRectangle(cornerRadius: 12, style: .continuous)
                                    .stroke(OnboardingColors.primary.opacity(0.3), lineWidth: 1)
                            )
                            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                            .shadow(color: .black.opacity(0.05), radius: 4, x: 0, y: 2)
                    }
                    
                    Button(action: {
                        if currentPage < onboardingSteps.count - 1 {
                            currentPage += 1
                        } else {
                            onOnboardingComplete()
                        }
                    }) {
                        HStack(spacing: 8) {
                            Text(currentPage < onboardingSteps.count - 1 ? "Suivant" : "Commencer")
                                .font(.subheadline.weight(.semibold))
                            
                            if currentPage < onboardingSteps.count - 1 {
                                Image(systemName: "arrow.right")
                                    .font(.system(size: 14, weight: .semibold))
                            }
                        }
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .frame(height: 44)
                        .background(OnboardingColors.primary)
                        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                        .shadow(color: OnboardingColors.primary.opacity(0.4), radius: 12, x: 0, y: 6)
                    }
                }
                .padding(.horizontal)
                .padding(.vertical, 16)
                .background(.regularMaterial)
                .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
                .padding(.horizontal)
                .padding(.bottom, 40)
            }
        }
    }
}

struct OnboardingView_Previews: PreviewProvider {
    static var previews: some View {
        OnboardingView(onOnboardingComplete: {})
    }
}