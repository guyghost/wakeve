import SwiftUI
#if canImport(UIKit)
import UIKit
#endif

/// Detail view for an event scenario/template from the Explore tab.
/// Shows full description, planning checklist, and CTA to create event.
struct ExploreScenarioDetailView: View {
    let scenario: EventScenario
    var onCreateEvent: (EventScenario) -> Void = { _ in }
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        GeometryReader { _ in
            ZStack(alignment: .topLeading) {
                ScrollView {
                    VStack(spacing: 0) {
                        // Gradient header
                        ZStack(alignment: .bottomLeading) {
                            LinearGradient(
                                colors: scenario.gradientColors,
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            )
                            .frame(height: 260)

                            VStack(alignment: .leading, spacing: 8) {
                                Image(systemName: scenario.icon)
                                    .font(.largeTitle)
                                    .foregroundColor(.white.opacity(0.9))

                                Text(scenario.title)
                                    .font(.title)
                                    .fontWeight(.bold)
                                    .foregroundColor(.white)

                                Text(scenario.subtitle)
                                    .font(.subheadline)
                                    .foregroundColor(.white.opacity(0.85))
                            }
                            .padding(20)
                            .padding(.bottom, 8)
                            .offset(y: 28)
                        }

                        VStack(alignment: .leading, spacing: 24) {
                            // Description
                            Text(scenario.description)
                                .font(.body)
                                .foregroundColor(.secondary)
                                .lineSpacing(4)

                            // Checklist
                            VStack(alignment: .leading, spacing: 16) {
                                Text(String(localized: "scenario.checklist_title"))
                                    .font(.headline)
                                    .foregroundColor(.primary)

                                ForEach(Array(scenario.checklistItems.enumerated()), id: \.offset) { index, item in
                                    HStack(alignment: .top, spacing: 12) {
                                        Image(systemName: "\(index + 1).circle.fill")
                                            .font(.title3)
                                            .foregroundColor(scenario.gradientColors.first ?? .blue)

                                        Text(item)
                                            .font(.body)
                                            .foregroundColor(.primary)
                                    }
                                }
                            }

                        }
                        .padding(20)
                        .padding(.bottom, 104)
                    }
                }

                WakeveCircleButton(
                    systemImage: "chevron.left",
                    accessibilityLabel: "Retour",
                    variant: .glass,
                    size: 44,
                    action: { dismiss() }
                )
                .padding(.leading, 16)
                .padding(.top, topSafeAreaInset + 8)
            }
        }
        .ignoresSafeArea(edges: .top)
        .navigationBarBackButtonHidden(true)
        .navigationBarTitleDisplayMode(.inline)
        .toolbarBackground(.hidden, for: .navigationBar)
        .toolbarColorScheme(.dark, for: .navigationBar)
        .safeAreaInset(edge: .bottom, spacing: 0) {
            VStack(spacing: 0) {
                LinearGradient(
                    colors: [Color.clear, Color(uiColor: .systemBackground)],
                    startPoint: .top,
                    endPoint: .bottom
                )
                .frame(height: 26)
                .allowsHitTesting(false)

                scenarioCTA
                    .padding(.horizontal, 20)
                    .padding(.top, 8)
                    .padding(.bottom, 12)
                    .background(Color(uiColor: .systemBackground))
            }
        }
    }

    private var scenarioCTA: some View {
        Button(action: createEvent) {
            HStack(spacing: 8) {
                Image(systemName: "plus.circle.fill")
                Text(String(localized: "scenario.create_event"))
            }
            .font(.headline)
            .foregroundColor(.white)
            .frame(maxWidth: .infinity)
            .frame(height: 54)
            .background(
                LinearGradient(
                    colors: scenario.gradientColors,
                    startPoint: .leading,
                    endPoint: .trailing
                )
            )
            .clipShape(RoundedRectangle(cornerRadius: WakeveTheme.Radius.md, style: .continuous))
        }
        .buttonStyle(.plain)
    }

    private func createEvent() {
        onCreateEvent(scenario)
        dismiss()
    }

    private var topSafeAreaInset: CGFloat {
        #if canImport(UIKit)
        let inset = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap(\.windows)
            .first(where: \.isKeyWindow)?
            .safeAreaInsets.top ?? 0
        return max(inset, 44)
        #else
        return 44
        #endif
    }
}
