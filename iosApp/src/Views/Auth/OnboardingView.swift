import SwiftUI

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
        VStack(spacing: WakeveTheme.Spacing.xl) {
            Spacer()
            
            ZStack {
                Circle()
                    .fill(Color.white.opacity(0.08))
                    .frame(width: 132, height: 132)
                    .overlay(Circle().stroke(Color.white.opacity(0.12), lineWidth: 1))
                
                Image(systemName: step.icon)
                    .resizable()
                    .scaledToFit()
                    .frame(width: 62, height: 62)
                    .foregroundColor(WakeveTheme.ColorToken.permissionBlue)
                    .scaleEffect(isAnimating ? 1.1 : 1.0)
                    .animation(
                        Animation.spring(response: 1.0, dampingFraction: 0.7)
                            .repeatForever(autoreverses: true),
                        value: isAnimating
                    )
            }
            .clipShape(Circle())
            .shadow(color: WakeveTheme.ColorToken.permissionBlue.opacity(0.22), radius: 22, x: 0, y: 10)
            .onAppear { isAnimating = true }
            
            Text(step.title)
                .font(WakeveTheme.Typography.largeTitle)
                .multilineTextAlignment(.center)
                .foregroundColor(.white)
                .lineLimit(3)
                .minimumScaleFactor(0.82)
            
            Text(step.description)
                .font(WakeveTheme.Typography.body)
                .multilineTextAlignment(.center)
                .foregroundColor(Color.white.opacity(0.78))
                .padding(.horizontal, WakeveTheme.Spacing.md)
                .lineSpacing(3)
            
            VStack(spacing: WakeveTheme.Spacing.sm) {
                ForEach(step.features, id: \.self) { feature in
                    HStack(spacing: WakeveTheme.Spacing.sm) {
                        Image(systemName: "checkmark.circle.fill")
                            .font(.system(size: 16, weight: .semibold))
                            .foregroundColor(WakeveColors.success)
                        
                        Text(feature)
                            .font(WakeveTheme.Typography.metadata)
                            .foregroundColor(Color.white.opacity(0.72))
                            .lineLimit(2)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                }
            }
            .padding(.horizontal, WakeveTheme.Spacing.md)
            
            Spacer()
        }
        .padding(WakeveTheme.Spacing.xl)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color.white.opacity(0.035))
        .clipShape(RoundedRectangle(cornerRadius: WakeveTheme.Radius.panel, style: .continuous))
        .padding(WakeveTheme.Spacing.page)
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
            WakeveScreenBackground(style: .utility)

            TabView(selection: $currentPage) {
                ForEach(0..<onboardingSteps.count, id: \.self) { index in
                    OnboardingStepView(step: onboardingSteps[index])
                        .tag(index)
                }
            }
            #if os(iOS)
            .tabViewStyle(.page(indexDisplayMode: .automatic))
            #endif
            
            VStack {
                Spacer()

                VStack(spacing: WakeveTheme.Spacing.md) {
                    WakeveActionButton(
                        currentPage < onboardingSteps.count - 1 ? String(localized: "onboarding.next") : String(localized: "onboarding.get_started"),
                        systemImage: currentPage < onboardingSteps.count - 1 ? "arrow.right" : "checkmark",
                        variant: .primary
                    ) {
                        if currentPage < onboardingSteps.count - 1 {
                            currentPage += 1
                        } else {
                            onOnboardingComplete()
                        }
                    }

                    Button(action: onOnboardingComplete) {
                        Text(String(localized: "onboarding.skip"))
                            .font(WakeveTheme.Typography.bodySemibold)
                            .foregroundColor(WakeveTheme.ColorToken.permissionBlue)
                            .frame(height: 44)
                    }
                    .buttonStyle(.plain)
                }
                .padding(WakeveTheme.Spacing.page)
                .background(
                    LinearGradient(
                        colors: [Color.clear, WakeveTheme.ColorToken.appDark.opacity(0.98)],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                    .frame(height: 168)
                    .allowsHitTesting(false),
                    alignment: .bottom
                )
            }
            .ignoresSafeArea(edges: .bottom)
        }
    }
}

struct OnboardingView_Previews: PreviewProvider {
    static var previews: some View {
        OnboardingView(onOnboardingComplete: {})
            .preferredColorScheme(.light)
    }
}

#Preview("Onboarding - Dark") {
    OnboardingView(onOnboardingComplete: {})
        .preferredColorScheme(.dark)
}
