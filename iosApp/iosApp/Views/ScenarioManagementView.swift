import SwiftUI

/**
 * Scenario Management View for iOS (SwiftUI)
 *
 * Displays a list of scenarios for an event with voting capabilities.
 * Features:
 * - List of scenarios sorted by score
 * - Pull-to-refresh
 * - Vote on scenarios (PREFER/NEUTRAL/AGAINST)
 * - Compare scenarios side-by-side
 * - Create/update/delete scenarios
 * - Detailed voting breakdown
 * - Liquid Glass design with native iOS patterns
 */
struct ScenarioManagementView: View {
    @StateObject private var viewModel: ScenarioManagementViewModel
    @Environment(\.dismiss) var dismiss

    @State private var showCreateSheet = false
    @State private var showDeleteAlert = false
    @State private var scenarioToDelete: MockScenario? = nil
    @State private var editingScenario: MockScenario? = nil
    @State private var selectedForComparison = Set<String>()
    @State private var showComparisonMode = false
    @State private var isRefreshing = false

    init(eventId: String, participantId: String, isOrganizer: Bool = false) {
        _viewModel = StateObject(
            wrappedValue: ScenarioManagementViewModel(
                eventId: eventId,
                participantId: participantId,
                isOrganizer: isOrganizer
            )
        )
    }

    var body: some View {
        NavigationStack {
            ZStack {
                mainContent

                if viewModel.hasError {
                    VStack(alignment: .leading, spacing: 8) {
                        HStack(alignment: .top, spacing: 12) {
                            Image(systemName: "exclamationmark.circle.fill")
                                .foregroundColor(.red)

                            VStack(alignment: .leading, spacing: 4) {
                                Text("Error")
                                    .fontWeight(.semibold)
                                Text(viewModel.errorMessage)
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }

                            Spacer()

                            Button(action: { viewModel.clearError() }) {
                                Image(systemName: "xmark.circle.fill")
                                    .foregroundColor(.secondary)
                            }
                        }
                    }
                    .padding(12)
                    .background(Color(red: 0.98, green: 0.89, blue: 0.89))
                    .cornerRadius(8)
                    .padding(16)
                    .frame(maxHeight: .infinity, alignment: .top)
                    .transition(.move(edge: .top))
                }
            }
            .navigationTitle("Scenarios")
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    if showComparisonMode {
                        Button(action: { clearComparison() }) {
                            Image(systemName: "xmark")
                                .fontWeight(.semibold)
                        }
                    }
                }

                ToolbarItem(placement: .topBarTrailing) {
                    if showComparisonMode {
                        Button(action: { startComparison() }) {
                            Text("Compare")
                                .fontWeight(.semibold)
                        }
                    } else if viewModel.isOrganizer {
                        Button(action: { showCreateSheet = true }) {
                            Image(systemName: "plus")
                                .fontWeight(.semibold)
                        }
                    }
                }
            }
            .sheet(isPresented: $showCreateSheet) {
                CreateScenarioSheet(
                    scenario: editingScenario,
                    eventId: viewModel.eventId,
                    onCreate: { scenario in
                        viewModel.createScenario(scenario)
                        editingScenario = nil
                    },
                    onUpdate: { scenario in
                        viewModel.updateScenario(scenario)
                        editingScenario = nil
                    }
                )
            }
            .alert("Delete Scenario", isPresented: $showDeleteAlert) {
                Button("Cancel", role: .cancel) { }
                Button("Delete", role: .destructive) {
                    if let id = scenarioToDelete?.id {
                        viewModel.deleteScenario(id)
                    }
                    scenarioToDelete = nil
                }
            } message: {
                Text("Are you sure you want to delete \"\(scenarioToDelete?.name ?? "this scenario")\"? This action cannot be undone.")
            }
        }
        .onAppear {
            viewModel.loadScenarios()
        }
    }

    @ViewBuilder
    private var mainContent: some View {
        if viewModel.isLoading && viewModel.scenarios.isEmpty {
            VStack(spacing: 16) {
                ProgressView()
                    .scaleEffect(1.2)
                Text("Loading scenarios...")
                    .font(.body)
                    .foregroundColor(.secondary)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(Color(.systemBackground))
        } else if viewModel.scenarios.isEmpty {
            scenarioEmptyState
        } else {
            scenarioList
        }
    }

    @ViewBuilder
    private var scenarioList: some View {
        List {
            ForEach(viewModel.getScenariosRanked()) { scenario in
                ScenarioRowView(
                    scenario: scenario,
                    isSelected: selectedForComparison.contains(scenario.id),
                    isComparisonMode: showComparisonMode,
                    onSelect: { id in handleScenarioSelect(id) },
                    onVote: { scenarioId, voteType in
                        viewModel.voteScenario(scenarioId, voteType: voteType)
                    },
                    onEdit: {
                        editingScenario = scenario
                        showCreateSheet = true
                    },
                    onDelete: {
                        scenarioToDelete = scenario
                        showDeleteAlert = true
                    },
                    isOrganizer: viewModel.isOrganizer,
                    isLocked: scenario.status == "SELECTED"
                )
                .listRowInsets(EdgeInsets(top: 8, leading: 16, bottom: 8, trailing: 16))
                .listRowSeparator(.hidden)
                .listRowBackground(Color.clear)
            }
        }
        .listStyle(.plain)
        .background(Color(.systemBackground))
        .refreshable {
            isRefreshing = true
            viewModel.loadScenarios()
            try? await Task.sleep(nanoseconds: 500_000_000)
            isRefreshing = false
        }
    }

    @ViewBuilder
    private var scenarioEmptyState: some View {
        VStack(spacing: 24) {
            Image(systemName: "calendar.badge.exclamationmark")
                .font(.system(size: 56))
                .foregroundColor(.secondary)

            VStack(spacing: 8) {
                Text("No Scenarios Yet")
                    .font(.system(.title2, design: .rounded))
                    .fontWeight(.bold)

                Text("Create a scenario to get started. Scenarios help participants vote on different planning options.")
                    .font(.body)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
            }

            if viewModel.isOrganizer {
                Button(action: { showCreateSheet = true }) {
                    Label("Create Scenario", systemImage: "plus.circle.fill")
                        .fontWeight(.semibold)
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .frame(height: 44)
                        .background(Color(red: 0.145, green: 0.386, blue: 0.932))
                        .cornerRadius(12)
                }
            }

            Spacer()
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding(24)
        .background(Color(.systemBackground))
    }

    private func handleScenarioSelect(_ scenarioId: String) {
        if showComparisonMode {
            if selectedForComparison.contains(scenarioId) {
                selectedForComparison.remove(scenarioId)
            } else {
                selectedForComparison.insert(scenarioId)
            }
        } else {
            viewModel.selectScenario(scenarioId)
        }
    }

    private func clearComparison() {
        showComparisonMode = false
        selectedForComparison.removeAll()
        viewModel.clearComparison()
    }

    private func startComparison() {
        if selectedForComparison.count >= 2 {
            viewModel.compareScenarios(Array(selectedForComparison))
            showComparisonMode = false
            selectedForComparison.removeAll()
        }
    }
}

// MARK: - Scenario Row View

struct ScenarioRowView: View {
    let scenario: MockScenario
    let isSelected: Bool
    let isComparisonMode: Bool
    let onSelect: (String) -> Void
    let onVote: (String, String) -> Void
    let onEdit: () -> Void
    let onDelete: () -> Void
    let isOrganizer: Bool
    let isLocked: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .top, spacing: 12) {
                VStack(alignment: .leading, spacing: 4) {
                    HStack(spacing: 8) {
                        Text(scenario.name)
                            .font(.headline)
                            .fontWeight(.bold)
                            .lineLimit(1)

                        if scenario.status == "SELECTED" {
                            Label("Selected", systemImage: "checkmark.circle.fill")
                                .font(.caption)
                                .fontWeight(.semibold)
                                .foregroundColor(.green)
                                .padding(.horizontal, 8)
                                .padding(.vertical, 2)
                                .background(Color.green.opacity(0.1))
                                .cornerRadius(4)
                        }
                    }

                    Text(scenario.description)
                        .font(.caption)
                        .foregroundColor(.secondary)
                        .lineLimit(2)
                }
                .frame(maxWidth: .infinity, alignment: .leading)

                if isOrganizer {
                    HStack(spacing: 0) {
                        Button(action: onEdit) {
                            Image(systemName: "pencil")
                                .font(.body)
                                .foregroundColor(.accentColor)
                                .frame(width: 32, height: 32)
                        }

                        Button(action: onDelete) {
                            Image(systemName: "trash")
                                .font(.body)
                                .foregroundColor(.red)
                                .frame(width: 32, height: 32)
                        }
                    }
                }
            }

            Divider()

            VStack(spacing: 8) {
                HStack(spacing: 12) {
                    DetailBadge(icon: "calendar", text: scenario.dateOrPeriod)
                    DetailBadge(icon: "person.2", text: "\(scenario.estimatedParticipants) people")
                }

                Text("📍 \(scenario.location)")
                    .font(.caption)
                    .foregroundColor(.secondary)

                HStack(spacing: 12) {
                    DetailBadge(icon: "calendar.badge.clock", text: "\(scenario.duration) days")
                    DetailBadge(icon: "indianrupee", text: "₹\(Int(scenario.estimatedBudgetPerPerson))/person")
                }
            }

            Divider()

            VotingBreakdownView(votingResult: scenario.votingResult)

            Divider()

            HStack(spacing: 8) {
                VotingButton(emoji: "👍", label: "Prefer", action: {
                    onVote(scenario.id, "PREFER")
                }, disabled: isLocked)
                VotingButton(emoji: "😐", label: "Neutral", action: {
                    onVote(scenario.id, "NEUTRAL")
                }, disabled: isLocked)
                VotingButton(emoji: "👎", label: "Against", action: {
                    onVote(scenario.id, "AGAINST")
                }, disabled: isLocked)
            }

            if isLocked {
                Label("Voting locked", systemImage: "lock.fill")
                    .font(.caption)
                    .foregroundColor(.orange)
                    .padding(.top, 4)
            }

            if isComparisonMode {
                HStack(spacing: 12) {
                    Button(action: { onSelect(scenario.id) }) {
                        HStack(spacing: 12) {
                            Image(systemName: isSelected ? "checkmark.circle.fill" : "circle")
                                .foregroundColor(isSelected ? .blue : .gray)
                            Text("Select for comparison")
                                .font(.body)
                                .foregroundColor(.primary)
                            Spacer()
                        }
                    }
                }
                .padding(.vertical, 4)
            }
        }
        .padding(16)
        .background(Color(.systemBackground))
        .cornerRadius(12)
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .stroke(
                    isSelected ? Color.blue.opacity(0.3) : Color.gray.opacity(0.1),
                    lineWidth: isSelected ? 2 : 1
                )
        )
    }
}

// MARK: - Detail Badge

struct DetailBadge: View {
    let icon: String
    let text: String

    var body: some View {
        HStack(spacing: 4) {
            Image(systemName: icon)
                .font(.caption)
            Text(text)
                .font(.caption)
        }
        .padding(.horizontal, 8)
        .padding(.vertical, 4)
        .background(Color(.systemGray6))
        .cornerRadius(6)
    }
}

// MARK: - Voting Breakdown

struct VotingBreakdownView: View {
    let votingResult: MockVotingResult

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text("Voting Results")
                    .font(.caption)
                    .fontWeight(.bold)

                Spacer()

                Text("Score: \(votingResult.score)")
                    .fontWeight(.bold)
                    .foregroundColor(scoreColor)
            }

            if votingResult.totalVotes == 0 {
                Text("No votes yet")
                    .font(.caption)
                    .foregroundColor(.secondary)
            } else {
                VStack(spacing: 6) {
                    VoteBreakdownRow(
                        label: "👍 Prefer",
                        count: votingResult.preferCount,
                        percentage: votingResult.preferPercentage,
                        color: Color(red: 0.145, green: 0.386, blue: 0.932)
                    )

                    VoteBreakdownRow(
                        label: "😐 Neutral",
                        count: votingResult.neutralCount,
                        percentage: votingResult.neutralPercentage,
                        color: Color.orange
                    )

                    VoteBreakdownRow(
                        label: "👎 Against",
                        count: votingResult.againstCount,
                        percentage: votingResult.againstPercentage,
                        color: Color.red
                    )

                    Text("Total: \(votingResult.totalVotes) votes")
                        .font(.caption)
                        .foregroundColor(.secondary)
                        .padding(.top, 4)
                }
            }
        }
    }

    private var scoreColor: Color {
        if votingResult.score > 0 {
            return Color(red: 0.145, green: 0.386, blue: 0.932)
        } else if votingResult.score < 0 {
            return .red
        } else {
            return .gray
        }
    }
}

// MARK: - Vote Breakdown Row

struct VoteBreakdownRow: View {
    let label: String
    let count: Int
    let percentage: Double
    let color: Color

    var body: some View {
        HStack(spacing: 8) {
            Text(label)
                .font(.caption)

            GeometryReader { geometry in
                ZStack(alignment: .leading) {
                    Capsule()
                        .fill(color.opacity(0.2))

                    Capsule()
                        .fill(color)
                        .frame(width: geometry.size.width * (percentage / 100))
                }
            }
            .frame(height: 6)

            Text("\(count) (\(String(format: "%.0f", percentage))%)")
                .font(.caption)
                .monospacedDigit()
                .frame(minWidth: 60, alignment: .trailing)
        }
    }
}

// MARK: - Voting Button

struct VotingButton: View {
    let emoji: String
    let label: String
    let action: () -> Void
    let disabled: Bool

    var body: some View {
        Button(action: action) {
            VStack(spacing: 2) {
                Text(emoji)
                    .font(.headline)
                Text(label)
                    .font(.caption2)
                    .fontWeight(.semibold)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 8)
            .background(Color(.systemGray6))
            .cornerRadius(8)
            .overlay(
                RoundedRectangle(cornerRadius: 8)
                    .stroke(Color.gray.opacity(0.2), lineWidth: 1)
            )
        }
        .disabled(disabled)
        .opacity(disabled ? 0.5 : 1.0)
    }
}

// MARK: - Create Scenario Sheet

struct CreateScenarioSheet: View {
    @Environment(\.dismiss) var dismiss

    let scenario: MockScenario?
    let eventId: String
    let onCreate: (MockScenario) -> Void
    let onUpdate: (MockScenario) -> Void

    @State private var name = ""
    @State private var description = ""
    @State private var dateOrPeriod = ""
    @State private var location = ""
    @State private var duration = "3"
    @State private var estimatedParticipants = "5"
    @State private var budget = "1000"

    var body: some View {
        NavigationStack {
            Form {
                Section("Scenario Details") {
                    TextField("Name", text: $name)
                    TextField("Description", text: $description, axis: .vertical)
                        .lineLimit(3...5)
                }

                Section("Location & Time") {
                    TextField("Date or Period", text: $dateOrPeriod)
                    TextField("Location", text: $location)
                }

                Section("Logistics") {
                    HStack {
                        Text("Duration")
                        Spacer()
                        TextField("Days", text: $duration)
                            .multilineTextAlignment(.trailing)
                            .frame(width: 60)
                    }

                    HStack {
                        Text("Estimated People")
                        Spacer()
                        TextField("Count", text: $estimatedParticipants)
                            .multilineTextAlignment(.trailing)
                            .frame(width: 60)
                    }

                    HStack {
                        Text("Budget per Person")
                        Spacer()
                        TextField("₹", text: $budget)
                            .multilineTextAlignment(.trailing)
                            .frame(width: 100)
                    }
                }
            }
            .navigationTitle(scenario == nil ? "Create Scenario" : "Edit Scenario")
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("Cancel") { dismiss() }
                }

                ToolbarItem(placement: .topBarTrailing) {
                    Button(scenario == nil ? "Create" : "Update") {
                        saveScenario()
                    }
                    .fontWeight(.semibold)
                }
            }
        }
        .onAppear {
            if let scenario = scenario {
                name = scenario.name
                description = scenario.description
                dateOrPeriod = scenario.dateOrPeriod
                location = scenario.location
                duration = String(scenario.duration)
                estimatedParticipants = String(scenario.estimatedParticipants)
                budget = String(Int(scenario.estimatedBudgetPerPerson))
            }
        }
    }

    private func saveScenario() {
        let newScenario = MockScenario(
            id: scenario?.id ?? "scenario-\(UUID().uuidString)",
            eventId: eventId,
            name: name,
            dateOrPeriod: dateOrPeriod,
            location: location,
            duration: Int(duration) ?? 3,
            estimatedParticipants: Int(estimatedParticipants) ?? 5,
            estimatedBudgetPerPerson: Double(budget) ?? 1000.0,
            description: description,
            status: scenario?.status ?? "PROPOSED",
            votingResult: scenario?.votingResult ?? MockVotingResult(
                scenarioId: "scenario-\(UUID().uuidString)",
                preferCount: 0,
                neutralCount: 0,
                againstCount: 0,
                totalVotes: 0,
                score: 0
            ),
            createdAt: scenario?.createdAt ?? ISO8601DateFormatter().string(from: Date()),
            updatedAt: ISO8601DateFormatter().string(from: Date())
        )

        if scenario == nil {
            onCreate(newScenario)
        } else {
            onUpdate(newScenario)
        }

        dismiss()
    }
}

// MARK: - Mock Models for iOS

struct MockScenario: Identifiable {
    let id: String
    let eventId: String
    let name: String
    let dateOrPeriod: String
    let location: String
    let duration: Int
    let estimatedParticipants: Int
    let estimatedBudgetPerPerson: Double
    let description: String
    let status: String
    let votingResult: MockVotingResult
    let createdAt: String
    let updatedAt: String
}

struct MockVotingResult {
    let scenarioId: String
    let preferCount: Int
    let neutralCount: Int
    let againstCount: Int
    let totalVotes: Int
    let score: Int

    var preferPercentage: Double {
        totalVotes > 0 ? Double(preferCount) / Double(totalVotes) * 100 : 0
    }

    var neutralPercentage: Double {
        totalVotes > 0 ? Double(neutralCount) / Double(totalVotes) * 100 : 0
    }

    var againstPercentage: Double {
        totalVotes > 0 ? Double(againstCount) / Double(totalVotes) * 100 : 0
    }
}

// MARK: - ViewModel for iOS

@MainActor
class ScenarioManagementViewModel: ObservableObject {
    @Published var isLoading = false
    @Published var scenarios: [MockScenario] = []
    @Published var errorMessage = ""

    let eventId: String
    let participantId: String
    let isOrganizer: Bool

    init(eventId: String, participantId: String, isOrganizer: Bool = false) {
        self.eventId = eventId
        self.participantId = participantId
        self.isOrganizer = isOrganizer
    }

    var hasError: Bool { !errorMessage.isEmpty }

    func loadScenarios() {
        isLoading = true
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) { [weak self] in
            self?.isLoading = false
        }
    }

    func createScenario(_ scenario: MockScenario) {
        scenarios.append(scenario)
    }

    func updateScenario(_ scenario: MockScenario) {
        if let index = scenarios.firstIndex(where: { $0.id == scenario.id }) {
            scenarios[index] = scenario
        }
    }

    func deleteScenario(_ scenarioId: String) {
        scenarios.removeAll { $0.id == scenarioId }
    }

    func selectScenario(_ scenarioId: String) {
        // Navigation would happen here
    }

    func voteScenario(_ scenarioId: String, voteType: String) {
        // Vote submission would happen here
    }

    func compareScenarios(_ scenarioIds: [String]) {
        // Comparison logic would happen here
    }

    func clearComparison() {
        // Clear comparison state
    }

    func clearError() {
        errorMessage = ""
    }

    func getScenariosRanked() -> [MockScenario] {
        scenarios.sorted { $0.votingResult.score > $1.votingResult.score }
    }
}

#Preview {
    ScenarioManagementView(
        eventId: "event-1",
        participantId: "participant-1",
        isOrganizer: true
    )
}
