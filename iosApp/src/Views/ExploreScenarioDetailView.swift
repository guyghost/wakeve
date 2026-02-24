import SwiftUI

/// Detail view for an event scenario/template from the Explore tab.
/// Shows full description, planning checklist, and CTA to create event.
struct ExploreScenarioDetailView: View {
    let scenario: EventScenario
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        ScrollView {
            VStack(spacing: 0) {
                // Gradient header
                ZStack(alignment: .bottomLeading) {
                    LinearGradient(
                        colors: scenario.gradientColors,
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                    .frame(height: 220)

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

                    // CTA Button
                    Button(action: createEvent) {
                        HStack {
                            Image(systemName: "plus.circle.fill")
                            Text(String(localized: "scenario.create_event"))
                        }
                        .font(.headline)
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 16)
                        .background(
                            LinearGradient(
                                colors: scenario.gradientColors,
                                startPoint: .leading,
                                endPoint: .trailing
                            )
                        )
                        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                    }
                    .padding(.top, 8)
                }
                .padding(20)
            }
        }
        .ignoresSafeArea(edges: .top)
        .navigationBarTitleDisplayMode(.inline)
        .toolbarBackground(.hidden, for: .navigationBar)
    }

    private func createEvent() {
        // TODO: Navigate to event creation pre-filled with scenario data
        // scenario.eventType, scenario.suggestedTitle, scenario.description
        dismiss()
    }
}
