import SwiftUI

// TODO: Replace with WakevColors.swift when available
// Using temporary color constants for now
struct OnboardingColors {
    static let primary = Color.blue
    static let primaryLight = Color.blue.opacity(0.1)
    static let success = Color.green
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
    
    var body: some View {
        VStack(spacing: 24) {
            Spacer()
            
            // Icon
            ZStack {
                Circle()
                    .fill(OnboardingColors.primaryLight)
                    .frame(width: 120, height: 120)
                
                Image(systemName: step.icon)
                    .resizable()
                    .scaledToFit()
                    .frame(width: 60, height: 60)
                    .foregroundColor(OnboardingColors.primary)
            }
            .clipShape(Circle())
            
            // Title
            Text(step.title)
                .font(.largeTitle)
                .fontWeight(.bold)
                .multilineTextAlignment(.center)
                .foregroundColor(Color.primary)
            
            // Description
            Text(step.description)
                .font(.body)
                .multilineTextAlignment(.center)
                .foregroundColor(Color.secondary)
                .padding(.horizontal)
            
            // Features
            VStack(spacing: 12) {
                ForEach(step.features, id: \.self) { feature in
                    HStack {
                        Image(systemName: "checkmark.circle.fill")
                            .foregroundColor(OnboardingColors.success)
                        Text(feature)
                            .font(.body)
                            .foregroundColor(Color.secondary)
                    }
                }
            }
            .padding(.horizontal)
            
            Spacer()
        }
        .padding()
        .background(.ultraThinMaterial)
        .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
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
            .tabViewStyle(.page(indexDisplayMode: .automatic))
            .background(.ultraThinMaterial)
            
            // Bottom buttons
            VStack {
                Spacer()
                HStack {
                    Button(action: onOnboardingComplete) {
                        Text("Passer")
                            .font(.body)
                            .foregroundColor(OnboardingColors.primary)
                    }
                    .padding()
                    
                    Spacer()
                    
                    Button(action: {
                        if currentPage < onboardingSteps.count - 1 {
                            currentPage += 1
                        } else {
                            onOnboardingComplete()
                        }
                    }) {
                        Text(currentPage < onboardingSteps.count - 1 ? "Suivant" : "Commencer")
                            .font(.body)
                            .foregroundColor(.white)
                            .padding(.horizontal, 24)
                            .padding(.vertical, 12)
                            .background(OnboardingColors.primary)
                            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                    }
                    .padding()
                }
                .background(.ultraThinMaterial)
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