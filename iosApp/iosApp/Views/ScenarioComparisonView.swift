import SwiftUI
import Shared

/// Scenario Comparison View - iOS
///
/// Side-by-side comparison of all scenarios for an event.
/// Shows key metrics in a scrollable table format with Liquid Glass design.
struct ScenarioComparisonView: View {
    let event: Event
    let repository: ScenarioRepository
    let onBack: () -> Void

    @State private var scenarios: [ScenarioWithVotes] = []
    @State private var bestScenarioId: String?
    @State private var isLoading = true
    @State private var errorMessage = ""
    @State private var showError = false

    var body: some View {
        ZStack {
            // Liquid Glass background gradient
            LinearGradient(
                gradient: Gradient(colors: [
                    Color.wakevPrimary.opacity(0.1),
                    Color.wakevAccent.opacity(0.05),
                    Color.wakevPrimary.opacity(0.08)
                ]),
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            .ignoresSafeArea()

            VStack(spacing: 0) {
                // Header
                headerView

                if isLoading {
                    loadingView
                } else if scenarios.isEmpty {
                    emptyStateView
                } else {
                    ScrollView([.horizontal, .vertical]) {
                        comparisonTable
                            .padding(20)
                    }
                }
            }
        }
        .onAppear {
            loadScenarios()
        }
        .alert("Error", isPresented: $showError) {
            Button("OK", role: .cancel) {}
        } message: {
            Text(errorMessage)
        }
    }

    // MARK: - Header View

    private var headerView: some View {
        VStack(spacing: 16) {
            HStack {
                LiquidGlassIconButton(
                    icon: "arrow.left",
                    size: 40,
                    gradientColors: [.wakevPrimary.opacity(0.6), .wakevAccent.opacity(0.6)]
                ) {
                    onBack()
                }
                .accessibilityLabel("Retour")

                Spacer()
            }
            .padding(.horizontal, 20)
            .padding(.top, 60)

            VStack(spacing: 8) {
                Text("Compare Scenarios")
                    .font(.system(size: 34, weight: .bold))
                    .foregroundColor(.primary)
                    .frame(maxWidth: .infinity, alignment: .leading)

                Text(event.title)
                    .font(.system(size: 20, weight: .medium))
                    .foregroundColor(.secondary)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
            .padding(.horizontal, 20)
        }
        .padding(.bottom, 16)
    }

    // MARK: - Comparison Table

    private var comparisonTable: some View {
        LiquidGlassCard(cornerRadius: 20, padding: 0) {
            VStack(spacing: 0) {
                // Header row with scenario names
                headerRow

                LiquidGlassDivider(style: .prominent)

                // Data rows
                comparisonRow(
                    label: "Date/Period",
                    values: scenarios.map { $0.scenario.dateOrPeriod }
                )

                LiquidGlassDivider(style: .default)

                comparisonRow(
                    label: "Location",
                    values: scenarios.map { $0.scenario.location }
                )

                LiquidGlassDivider(style: .default)

                comparisonRow(
                    label: "Duration",
                    values: scenarios.map { "\($0.scenario.duration) days" }
                )

                LiquidGlassDivider(style: .default)

                comparisonRow(
                    label: "Est. Participants",
                    values: scenarios.map { "\($0.scenario.estimatedParticipants)" }
                )

                LiquidGlassDivider(style: .default)

                comparisonRow(
                    label: "Budget/Person",
                    values: scenarios.map {
                        String(format: "$%.0f", $0.scenario.estimatedBudgetPerPerson)
                    }
                )

                LiquidGlassDivider(style: .default)

                comparisonRow(
                    label: "Total Budget",
                    values: scenarios.map {
                        let total = $0.scenario.estimatedBudgetPerPerson * Double($0.scenario.estimatedParticipants)
                        return String(format: "$%.0f", total)
                    }
                )

                LiquidGlassDivider(style: .default)

                comparisonRow(
                    label: "Status",
                    values: scenarios.map { statusText($0.scenario.status.name) }
                )

                LiquidGlassDivider(style: .prominent)

                // Voting results
                votingResultsRow(
                    label: "Prefer",
                    values: scenarios.map { Int($0.votingResult.preferCount) },
                    color: .green,
                    badgeStyle: .success
                )

                LiquidGlassDivider(style: .default)

                votingResultsRow(
                    label: "Neutral",
                    values: scenarios.map { Int($0.votingResult.neutralCount) },
                    color: .orange,
                    badgeStyle: .warning
                )

                LiquidGlassDivider(style: .default)

                votingResultsRow(
                    label: "Against",
                    values: scenarios.map { Int($0.votingResult.againstCount) },
                    color: .red,
                    badgeStyle: .default
                )

                LiquidGlassDivider(style: .default)

                votingResultsRow(
                    label: "Total Votes",
                    values: scenarios.map { Int($0.votingResult.totalVotes) },
                    color: .blue,
                    badgeStyle: .info
                )

                LiquidGlassDivider(style: .prominent)

                // Score row with emphasis
                HStack(spacing: 0) {
                    // Label column
                    Text("Score")
                        .font(.system(size: 14, weight: .bold))
                        .foregroundColor(.wakevPrimary)
                        .frame(width: 140, alignment: .leading)
                        .padding(12)

                    // Value columns
                    ForEach(Array(scenarios.enumerated()), id: \.element.scenario.id) { index, swv in
                        VStack(spacing: 4) {
                            Text("\(Int(swv.votingResult.score))")
                                .font(.system(size: 18, weight: .bold))
                                .foregroundColor(scenarioScoreColor(swv))
                                .frame(width: 140)
                                .padding(12)

                            // Best score badge
                            if swv.scenario.id == bestScenarioId {
                                LiquidGlassBadge(
                                    text: "★ Best",
                                    icon: "star.fill",
                                    style: .accent
                                )
                            }
                        }
                        .frame(width: 140)
                    }
                }
                .background(Color(.systemBackground).opacity(0.5))
            }
        }
    }

    // MARK: - Header Row

    private var headerRow: some View {
        HStack(spacing: 0) {
            // Metric label column
            Text("Metric")
                .font(.system(size: 14, weight: .semibold))
                .foregroundColor(.secondary)
                .frame(width: 140, alignment: .leading)
                .padding(12)

            // Scenario columns
            ForEach(scenarios, id: \.scenario.id) { swv in
                VStack(spacing: 6) {
                    Text(swv.scenario.name)
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(.primary)
                        .lineLimit(2)
                        .multilineTextAlignment(.center)

                    if swv.scenario.id == bestScenarioId {
                        LiquidGlassBadge(
                            text: "Best Score",
                            icon: "star.fill",
                            style: .accent
                        )
                    }

                    // Status badge
                    statusBadge(for: swv.scenario.status.name)
                }
                .frame(width: 140)
                .padding(12)
            }
        }
        .background(Color.wakevPrimary.opacity(0.05))
    }

    // MARK: - Comparison Row

    private func comparisonRow(label: String, values: [String]) -> some View {
        HStack(spacing: 0) {
            // Label column
            Text(label)
                .font(.system(size: 14, weight: .medium))
                .foregroundColor(.primary)
                .frame(width: 140, alignment: .leading)
                .padding(12)

            // Value columns
            ForEach(Array(values.enumerated()), id: \.offset) { _, value in
                Text(value)
                    .font(.system(size: 14))
                    .foregroundColor(.secondary)
                    .frame(width: 140)
                    .padding(12)
                    .lineLimit(3)
                    .multilineTextAlignment(.center)
            }
        }
        .background(Color(.systemBackground).opacity(0.3))
    }

    // MARK: - Voting Results Row

    private func votingResultsRow(
        label: String,
        values: [Int],
        color: Color,
        badgeStyle: LiquidGlassBadgeStyle
    ) -> some View {
        HStack(spacing: 0) {
            // Label column
            Text(label)
                .font(.system(size: 14, weight: .medium))
                .foregroundColor(color)
                .frame(width: 140, alignment: .leading)
                .padding(12)

            // Value columns
            ForEach(Array(values.enumerated()), id: \.offset) { _, value in
                HStack(spacing: 4) {
                    Text("\(value)")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(color)

                    // Visual indicator
                    Circle()
                        .fill(color.opacity(0.2))
                        .frame(width: 8, height: 8)
                }
                .frame(width: 140)
                .padding(12)
            }
        }
        .background(Color(.systemBackground).opacity(0.3))
    }

    // MARK: - Status Badge

    @ViewBuilder
    private func statusBadge(for status: String) -> some View {
        let style: LiquidGlassBadgeStyle = {
            switch status.uppercased() {
            case "SELECTED":
                return .success
            case "REJECTED":
                return .warning
            default:
                return .info
            }
        }()

        LiquidGlassBadge(
            text: statusText(status),
            style: style
        )
    }

    // MARK: - Helper Functions

    private func scenarioScoreColor(_ swv: ScenarioWithVotes) -> Color {
        if swv.scenario.id == bestScenarioId {
            return .wakevAccent
        }
        return .primary
    }

    private func statusText(_ status: String) -> String {
        switch status.uppercased() {
        case "PROPOSED":
            return "Proposed"
        case "SELECTED":
            return "Selected"
        case "REJECTED":
            return "Rejected"
        default:
            return status
        }
    }

    // MARK: - Loading View

    private var loadingView: some View {
        VStack(spacing: 24) {
            ProgressView()
                .progressViewStyle(CircularProgressViewStyle())
                .scaleEffect(1.5)
                .liquidGlass(cornerRadius: 16, opacity: 0.8, intensity: 0.8)
                .frame(width: 60, height: 60)

            Text("Loading scenarios...")
                .font(.system(size: 17, weight: .medium))
                .foregroundColor(.secondary)
        }
        .frame(maxHeight: .infinity)
        .padding(40)
    }

    // MARK: - Empty State

    private var emptyStateView: some View {
        VStack(spacing: 32) {
            ZStack {
                Circle()
                    .fill(
                        LinearGradient(
                            gradient: Gradient(colors: [
                                Color.wakevPrimary.opacity(0.2),
                                Color.wakevAccent.opacity(0.1)
                            ]),
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
                    .frame(width: 100, height: 100)

                Image(systemName: "arrow.left.arrow.right")
                    .font(.system(size: 40, weight: .medium))
                    .foregroundColor(.wakevPrimary)
            }

            VStack(spacing: 12) {
                Text("No Scenarios to Compare")
                    .font(.system(size: 24, weight: .bold))
                    .foregroundColor(.primary)

                Text("At least 2 scenarios are needed for comparison.")
                    .font(.system(size: 17))
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 40)
            }
        }
        .frame(maxHeight: .infinity)
        .padding(40)
    }

    // MARK: - Data Loading

    private func loadScenarios() {
        Task {
            do {
                let scenariosWithVotes = try await repository.getScenariosWithVotes(eventId: event.id)

                // Rank scenarios by score
                let ranked = ScenarioLogic.shared.rankScenariosByScore(
                    scenarios: scenariosWithVotes.map { $0.scenario },
                    votes: scenariosWithVotes.flatMap { $0.votes }
                )

                await MainActor.run {
                    self.scenarios = scenariosWithVotes
                    self.bestScenarioId = ranked.first?.scenario.id
                    self.isLoading = false
                }
            } catch {
                await MainActor.run {
                    self.errorMessage = error.localizedDescription
                    self.showError = true
                    self.isLoading = false
                }
            }
        }
    }
}

// MARK: - Preview

#Preview("Scenario Comparison View") {
    ScenarioComparisonView(
        event: Event(
            id: "preview-event-1",
            title: "Summer Team Building",
            description: "Annual team building event",
            organizerId: "user-1",
            createdAt: Date(),
            updatedAt: Date(),
            status: EventStatus.comparing,
            eventType: EventType.teamBuilding,
            eventTypeCustom: nil,
            minParticipants: 5,
            maxParticipants: 20,
            expectedParticipants: 12
        ),
        repository: PreviewScenarioRepository(),
        onBack: {}
    )
}

// MARK: - Preview Repository

private class PreviewScenarioRepository: ScenarioRepository {
    func getScenariosWithVotes(eventId: String) async throws -> [ScenarioWithVotes] {
        [
            ScenarioWithVotes(
                scenario: Scenario(
                    id: "scenario-1",
                    eventId: "event-1",
                    name: "Beach Resort",
                    description: "Weekend at beach resort",
                    location: "Miami Beach, FL",
                    dateOrPeriod: "July 15-17",
                    duration: 3,
                    estimatedParticipants: 15,
                    estimatedBudgetPerPerson: 250,
                    status: ScenarioStatus.proposed,
                    votes: []
                ),
                votes: [],
                votingResult: VotingResult(preferCount: 8, neutralCount: 4, againstCount: 2, totalVotes: 14, score: 12)
            ),
            ScenarioWithVotes(
                scenario: Scenario(
                    id: "scenario-2",
                    eventId: "event-1",
                    name: "Mountain Cabin",
                    description: "Mountain retreat",
                    location: "Aspen, CO",
                    dateOrPeriod: "July 22-24",
                    duration: 3,
                    estimatedParticipants: 12,
                    estimatedBudgetPerPerson: 300,
                    status: ScenarioStatus.proposed,
                    votes: []
                ),
                votes: [],
                votingResult: VotingResult(preferCount: 5, neutralCount: 5, againstCount: 3, totalVotes: 13, score: 4)
            ),
            ScenarioWithVotes(
                scenario: Scenario(
                    id: "scenario-3",
                    eventId: "event-1",
                    name: "City Hotel",
                    description: "Downtown hotel event",
                    location: "New York, NY",
                    dateOrPeriod: "August 5-7",
                    duration: 3,
                    estimatedParticipants: 18,
                    estimatedBudgetPerPerson: 400,
                    status: ScenarioStatus.proposed,
                    votes: []
                ),
                votes: [],
                votingResult: VotingResult(preferCount: 3, neutralCount: 6, againstCount: 5, totalVotes: 14, score: -4)
            )
        ]
    }

    func getScenarios(eventId: String) async throws -> [Scenario] {
        []
    }

    func createScenario(_ scenario: Scenario) async throws -> Scenario {
        scenario
    }

    func updateScenario(_ scenario: Scenario) async throws -> Scenario {
        scenario
    }

    func deleteScenario(scenarioId: String) async throws {
        // No-op for preview
    }

    func vote(scenarioId: String, voteType: VoteType) async throws {
        // No-op for preview
    }
}
