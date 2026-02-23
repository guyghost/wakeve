import SwiftUI

// Color scheme for onboarding using Wakeve design system
struct OnboardingColors {
    // Using hex values from WakeveColors design system
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
            title: String(localized: "onboarding.create_events_title"),
            description: String(localized: "onboarding.create_events_desc"),
            icon: "calendar",
            features: [
                String(localized: "onboarding.feature.quick_creation"),
                String(localized: "onboarding.feature.availability_poll"),
                String(localized: "onboarding.feature.auto_calculate")
            ]
        ),
        OnboardingStep(
            title: String(localized: "onboarding.collaborate_title"),
            description: String(localized: "onboarding.collaborate_desc"),
            icon: "person.2",
            features: [
                String(localized: "onboarding.feature.participant_management"),
                String(localized: "onboarding.feature.task_assignment"),
                String(localized: "onboarding.feature.realtime_tracking")
            ]
        ),
        OnboardingStep(
            title: String(localized: "onboarding.organize_title"),
            description: String(localized: "onboarding.organize_desc"),
            icon: "target",
            features: [
                String(localized: "onboarding.feature.accommodation_planning"),
                String(localized: "onboarding.feature.meal_organization"),
                String(localized: "onboarding.feature.budget_tracking")
            ]
        ),
        OnboardingStep(
            title: String(localized: "onboarding.enjoy_title"),
            description: String(localized: "onboarding.enjoy_desc"),
            icon: "sparkles",
            features: [
                String(localized: "onboarding.feature.overview"),
                String(localized: "onboarding.feature.built_in_reminders"),
                String(localized: "onboarding.feature.native_calendar")
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
                        Text(String(localized: "onboarding.skip"))
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
                            Text(currentPage < onboardingSteps.count - 1 ? String(localized: "onboarding.next") : String(localized: "onboarding.get_started"))
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