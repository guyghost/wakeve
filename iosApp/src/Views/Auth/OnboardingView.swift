import SwiftUI

struct OnboardingStep {
    let title: String
    let description: String
    let icon: String
    let features: [String]
    let proofTitle: String
    let proofDetail: String
}

struct OnboardingStepView: View {
    let step: OnboardingStep
    
    @State private var isAnimating = false
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

    private var visibleFeatures: [String] {
        dynamicTypeSize.isAccessibilitySize ? Array(step.features.prefix(1)) : step.features
    }
    
    var body: some View {
        ScrollView(showsIndicators: false) {
            VStack(spacing: dynamicTypeSize.isAccessibilitySize ? WakeveTheme.Spacing.md : WakeveTheme.Spacing.xl) {
                ZStack {
                    Circle()
                        .fill(Color.white.opacity(0.08))
                        .frame(width: dynamicTypeSize.isAccessibilitySize ? 88 : 132, height: dynamicTypeSize.isAccessibilitySize ? 88 : 132)
                        .overlay(Circle().stroke(Color.white.opacity(0.12), lineWidth: 1))
                    
                    Image(systemName: step.icon)
                        .resizable()
                        .scaledToFit()
                        .frame(width: dynamicTypeSize.isAccessibilitySize ? 42 : 62, height: dynamicTypeSize.isAccessibilitySize ? 42 : 62)
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
                    .font(dynamicTypeSize.isAccessibilitySize ? WakeveTheme.Typography.title : WakeveTheme.Typography.largeTitle)
                    .multilineTextAlignment(.center)
                    .foregroundColor(.white)
                    .fixedSize(horizontal: false, vertical: true)
                
                Text(step.description)
                    .font(dynamicTypeSize.isAccessibilitySize ? WakeveTheme.Typography.callout : WakeveTheme.Typography.body)
                    .multilineTextAlignment(.center)
                    .foregroundColor(Color.white.opacity(0.78))
                    .padding(.horizontal, WakeveTheme.Spacing.sm)
                    .lineSpacing(3)
                    .fixedSize(horizontal: false, vertical: true)

                OnboardingProofCard(
                    title: step.proofTitle,
                    detail: step.proofDetail,
                    icon: step.icon
                )
                
                VStack(spacing: WakeveTheme.Spacing.sm) {
                    ForEach(visibleFeatures, id: \.self) { feature in
                        HStack(spacing: WakeveTheme.Spacing.sm) {
                            Image(systemName: "checkmark.circle.fill")
                                .font(.system(size: 16, weight: .semibold))
                                .foregroundColor(WakeveColors.success)
                            
                            Text(feature)
                                .font(WakeveTheme.Typography.metadata)
                                .foregroundColor(Color.white.opacity(0.72))
                                .fixedSize(horizontal: false, vertical: true)
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                    }
                }
                .padding(.horizontal, WakeveTheme.Spacing.md)
            }
            .padding(dynamicTypeSize.isAccessibilitySize ? WakeveTheme.Spacing.md : WakeveTheme.Spacing.xl)
            .frame(maxWidth: .infinity, minHeight: dynamicTypeSize.isAccessibilitySize ? nil : 520)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color.white.opacity(0.035))
        .clipShape(RoundedRectangle(cornerRadius: WakeveTheme.Radius.panel, style: .continuous))
        .padding(WakeveTheme.Spacing.page)
    }
}

private struct OnboardingProofCard: View {
    let title: String
    let detail: String
    let icon: String

    var body: some View {
        HStack(alignment: .top, spacing: WakeveTheme.Spacing.sm) {
            Image(systemName: icon)
                .font(.system(size: 16, weight: .semibold))
                .foregroundColor(WakeveTheme.ColorToken.permissionBlue)
                .frame(width: 32, height: 32)
                .background(Color.white.opacity(0.08))
                .clipShape(Circle())

            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .font(WakeveTheme.Typography.metadata.weight(.semibold))
                    .foregroundColor(.white)

                Text(detail)
                    .font(WakeveTheme.Typography.metadata)
                    .foregroundColor(Color.white.opacity(0.74))
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(WakeveTheme.Spacing.md)
        .background(Color.white.opacity(0.06))
        .overlay(
            RoundedRectangle(cornerRadius: WakeveTheme.Radius.lg, style: .continuous)
                .stroke(Color.white.opacity(0.12), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: WakeveTheme.Radius.lg, style: .continuous))
        .accessibilityIdentifier("onboardingProofCard")
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
            ],
            proofTitle: String(localized: "onboarding.proof.create_title"),
            proofDetail: String(localized: "onboarding.proof.create_detail")
        ),
        OnboardingStep(
            title: String(localized: "onboarding.collaborate_title"),
            description: String(localized: "onboarding.collaborate_desc"),
            icon: "person.2",
            features: [
                String(localized: "onboarding.feature.participant_management"),
                String(localized: "onboarding.feature.task_assignment"),
                String(localized: "onboarding.feature.realtime_tracking")
            ],
            proofTitle: String(localized: "onboarding.proof.collaborate_title"),
            proofDetail: String(localized: "onboarding.proof.collaborate_detail")
        ),
        OnboardingStep(
            title: String(localized: "onboarding.organize_title"),
            description: String(localized: "onboarding.organize_desc"),
            icon: "target",
            features: [
                String(localized: "onboarding.feature.accommodation_planning"),
                String(localized: "onboarding.feature.meal_organization"),
                String(localized: "onboarding.feature.budget_tracking")
            ],
            proofTitle: String(localized: "onboarding.proof.organize_title"),
            proofDetail: String(localized: "onboarding.proof.organize_detail")
        ),
        OnboardingStep(
            title: String(localized: "onboarding.enjoy_title"),
            description: String(localized: "onboarding.enjoy_desc"),
            icon: "sparkles",
            features: [
                String(localized: "onboarding.feature.overview"),
                String(localized: "onboarding.feature.built_in_reminders"),
                String(localized: "onboarding.feature.native_calendar")
            ],
            proofTitle: String(localized: "onboarding.proof.enjoy_title"),
            proofDetail: String(localized: "onboarding.proof.enjoy_detail")
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
            .padding(.bottom, 160)
            
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
