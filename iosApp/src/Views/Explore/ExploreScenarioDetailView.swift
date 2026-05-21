import SwiftUI

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
                        scenarioHeader
                        scenarioContent
                        .padding(.bottom, 104)
                    }
                }

                backButton
            }
        }
        .ignoresSafeArea(edges: .top)
        .navigationBarBackButtonHidden(true)
        .navigationBarTitleDisplayMode(.inline)
        .toolbarBackground(.hidden, for: .navigationBar)
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

    private var scenarioHeader: some View {
        ZStack(alignment: .topLeading) {
            LinearGradient(
                colors: scenario.gradientColors,
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )

            VStack(alignment: .leading, spacing: 14) {
                Image(systemName: scenario.icon)
                    .font(.system(size: 40, weight: .medium))
                    .foregroundColor(.white.opacity(0.92))
                    .frame(width: 44, height: 46, alignment: .leading)

                Text(scenario.title)
                    .font(.system(size: 30, weight: .bold))
                    .foregroundColor(.white)
                    .lineSpacing(2)
                    .lineLimit(2)
                    .fixedSize(horizontal: false, vertical: true)

                Text(scenario.subtitle)
                    .font(.system(size: 16, weight: .medium))
                    .foregroundColor(.white.opacity(0.86))
                    .lineLimit(2)
                    .fixedSize(horizontal: false, vertical: true)
            }
            .padding(.horizontal, 28)
            .padding(.top, WakeveTheme.Navigation.currentSafeAreaTop + 64)
            .padding(.trailing, 20)
        }
        .frame(height: WakeveTheme.Navigation.currentSafeAreaTop + 250)
    }

    private var scenarioContent: some View {
        VStack(alignment: .leading, spacing: 26) {
            Text(scenario.description)
                .font(.system(size: 17, weight: .regular))
                .foregroundColor(.secondary)
                .lineSpacing(4)
                .fixedSize(horizontal: false, vertical: true)

            VStack(alignment: .leading, spacing: 18) {
                Text(String(localized: "scenario.checklist_title"))
                    .font(.system(size: 20, weight: .bold))
                    .foregroundColor(.primary)

                VStack(alignment: .leading, spacing: 14) {
                    ForEach(Array(scenario.checklistItems.enumerated()), id: \.offset) { index, item in
                        checklistRow(index: index, item: item)
                    }
                }
            }
        }
        .padding(.horizontal, 28)
        .padding(.top, 28)
    }

    private func checklistRow(index: Int, item: String) -> some View {
        HStack(alignment: .center, spacing: 14) {
            ZStack {
                Circle()
                    .fill(scenario.gradientColors.first ?? .orange)
                    .frame(width: 24, height: 24)

                Text("\(index + 1)")
                    .font(.system(size: 14, weight: .bold))
                    .foregroundColor(.white)
            }

            Text(item)
                .font(.system(size: 17, weight: .regular))
                .foregroundColor(.primary)
                .lineLimit(1)
                .minimumScaleFactor(0.82)
        }
    }

    private var backButton: some View {
        WakeveCircleButton(
            systemImage: "chevron.left",
            accessibilityLabel: "Retour",
            variant: .glass,
            size: 44,
            action: { dismiss() }
        )
        .padding(.leading, 24)
        .padding(.top, WakeveTheme.Navigation.controlTopPadding(safeAreaTop: WakeveTheme.Navigation.currentSafeAreaTop))
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

}
