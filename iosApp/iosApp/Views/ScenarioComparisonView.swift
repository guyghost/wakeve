import SwiftUI
import Shared

/// Scenario Comparison View - iOS
///
/// Side-by-side comparison of all scenarios for an event.
/// Shows key metrics in a scrollable table format.
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
            Color(.systemGroupedBackground)
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
                Button(action: onBack) {
                    Image(systemName: "arrow.left")
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundColor(.secondary)
                        .frame(width: 36, height: 36)
                        .background(Color(.tertiarySystemFill))
                        .clipShape(Circle())
                }
                
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
        .background(Color(.systemGroupedBackground))
    }
    
    // MARK: - Comparison Table
    
    private var comparisonTable: some View {
        VStack(spacing: 0) {
            // Header row with scenario names
            HStack(spacing: 0) {
                // Metric label column
                Text("Metric")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(.secondary)
                    .frame(width: 140, alignment: .leading)
                    .padding(12)
                
                // Scenario columns
                ForEach(scenarios, id: \.scenario.id) { swv in
                    VStack(spacing: 4) {
                        Text(swv.scenario.name)
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundColor(.primary)
                            .lineLimit(2)
                            .multilineTextAlignment(.center)
                        
                        if swv.scenario.id == bestScenarioId {
                            Text("★ Best Score")
                                .font(.system(size: 11, weight: .medium))
                                .foregroundColor(.green)
                        }
                    }
                    .frame(width: 140)
                    .padding(12)
                }
            }
            .background(Color(.secondarySystemGroupedBackground))
            
            Divider()
            
            // Data rows
            ComparisonRow(
                label: "Date/Period",
                values: scenarios.map { $0.scenario.dateOrPeriod }
            )
            
            Divider()
            
            ComparisonRow(
                label: "Location",
                values: scenarios.map { $0.scenario.location }
            )
            
            Divider()
            
            ComparisonRow(
                label: "Duration",
                values: scenarios.map { "\($0.scenario.duration) days" }
            )
            
            Divider()
            
            ComparisonRow(
                label: "Est. Participants",
                values: scenarios.map { "\($0.scenario.estimatedParticipants)" }
            )
            
            Divider()
            
            ComparisonRow(
                label: "Budget/Person",
                values: scenarios.map {
                    String(format: "$%.0f", $0.scenario.estimatedBudgetPerPerson)
                }
            )
            
            Divider()
            
            ComparisonRow(
                label: "Total Budget",
                values: scenarios.map {
                    let total = $0.scenario.estimatedBudgetPerPerson * Double($0.scenario.estimatedParticipants)
                    return String(format: "$%.0f", total)
                }
            )
            
            Divider()
            
            ComparisonRow(
                label: "Status",
                values: scenarios.map { statusText($0.scenario.status) }
            )
            
            Divider()
            
            // Voting results
            VotingResultsRow(
                label: "Prefer",
                values: scenarios.map { $0.votingResult.preferCount },
                color: .green
            )
            
            Divider()
            
            VotingResultsRow(
                label: "Neutral",
                values: scenarios.map { $0.votingResult.neutralCount },
                color: .orange
            )
            
            Divider()
            
            VotingResultsRow(
                label: "Against",
                values: scenarios.map { $0.votingResult.againstCount },
                color: .red
            )
            
            Divider()
            
            VotingResultsRow(
                label: "Total Votes",
                values: scenarios.map { $0.votingResult.totalVotes },
                color: .blue
            )
            
            Divider()
            
            VotingResultsRow(
                label: "Score",
                values: scenarios.map { $0.votingResult.score },
                color: .primary,
                bold: true
            )
        }
        .glassCard()
    }
    
    // MARK: - Loading View
    
    private var loadingView: some View {
        VStack(spacing: 20) {
            ProgressView()
                .progressViewStyle(CircularProgressViewStyle())
            
            Text("Loading scenarios...")
                .font(.system(size: 17))
                .foregroundColor(.secondary)
        }
        .frame(maxHeight: .infinity)
    }
    
    // MARK: - Empty State
    
    private var emptyStateView: some View {
        VStack(spacing: 24) {
            ZStack {
                Circle()
                    .fill(Color.blue.opacity(0.1))
                    .frame(width: 80, height: 80)
                
                Image(systemName: "arrow.left.arrow.right")
                    .font(.system(size: 36))
                    .foregroundColor(.blue)
            }
            
            VStack(spacing: 8) {
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
    
    // MARK: - Helpers
    
    private func statusText(_ status: ScenarioStatus) -> String {
        switch status {
        case .proposed: return "Proposed"
        case .selected: return "Selected"
        case .rejected: return "Rejected"
        }
    }
}

// MARK: - Comparison Row

struct ComparisonRow: View {
    let label: String
    let values: [String]
    
    var body: some View {
        HStack(spacing: 0) {
            // Label column
            Text(label)
                .font(.system(size: 14, weight: .medium))
                .foregroundColor(.primary)
                .frame(width: 140, alignment: .leading)
                .padding(12)
            
            // Value columns
            ForEach(Array(values.enumerated()), id: \.offset) { index, value in
                Text(value)
                    .font(.system(size: 14))
                    .foregroundColor(.secondary)
                    .frame(width: 140)
                    .padding(12)
                    .lineLimit(3)
                    .multilineTextAlignment(.center)
            }
        }
        .background(Color(.systemBackground))
    }
}

// MARK: - Voting Results Row

struct VotingResultsRow: View {
    let label: String
    let values: [Int]
    let color: Color
    var bold: Bool = false
    
    var body: some View {
        HStack(spacing: 0) {
            // Label column
            Text(label)
                .font(.system(size: 14, weight: bold ? .bold : .medium))
                .foregroundColor(color)
                .frame(width: 140, alignment: .leading)
                .padding(12)
            
            // Value columns
            ForEach(Array(values.enumerated()), id: \.offset) { index, value in
                Text("\(value)")
                    .font(.system(size: 14, weight: bold ? .bold : .regular))
                    .foregroundColor(color)
                    .frame(width: 140)
                    .padding(12)
            }
        }
        .background(Color(.systemBackground))
    }
}
