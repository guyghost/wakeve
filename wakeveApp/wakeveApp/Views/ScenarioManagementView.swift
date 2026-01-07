import SwiftUI

/**
 * Scenario Management View for iOS (SwiftUI) - Liquid Glass Refactored
 *
 * Displays a list of scenarios for an event with voting capabilities.
 * Features:
 * - List of scenarios sorted by score
 * - Pull-to-refresh
 * - Vote on scenarios (PREFER/NEUTRAL/AGAINST)
 * - Compare scenarios side-by-side
 * - Create/update/delete scenarios
 * - Detailed voting breakdown
 * - Liquid Glass design system
 */

// MARK: - Main View

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
                    .padding(.top, 60)

                if viewModel.hasError {
                    errorBanner
                }
            }
            .navigationTitle("Scenarios")
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    if showComparisonMode {
                        cancelButton
                    } else if viewModel.isOrganizer {
                        addButton
                    }
                }
                
                if showComparisonMode {
                    ToolbarItem(placement: .primaryAction) {
                        compareButton
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

    // MARK: - Toolbar Buttons
    
    private var cancelButton: some View {
        Button(action: { clearComparison() }) {
            Text("Cancel")
                .fontWeight(.medium)
        }
    }
    
    private var addButton: some View {
        Button(action: { showCreateSheet = true }) {
            Image(systemName: "plus")
                .fontWeight(.semibold)
        }
    }
    
    private var compareButton: some View {
        Button(action: { startComparison() }) {
            Text("Compare")
                .fontWeight(.semibold)
        }
        .disabled(selectedForComparison.count < 2)
    }

    // MARK: - Error Banner

    private var errorBanner: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(alignment: .top, spacing: 12) {
                Image(systemName: "exclamationmark.circle.fill")
                    .foregroundColor(.wakevError)

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
        .padding(16)
        .background(Color.wakevError.opacity(0.1))
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
        .padding(16)
        .frame(maxHeight: .infinity, alignment: .top)
        .transition(.move(edge: .top))
    }

    @ViewBuilder
    private var mainContent: some View {
        if viewModel.isLoading && viewModel.scenarios.isEmpty {
            loadingView
        } else if viewModel.scenarios.isEmpty {
            scenarioEmptyState
        } else {
            scenarioList
        }
    }

    private var loadingView: some View {
        VStack(spacing: 16) {
            ProgressView()
                .scaleEffect(1.2)
                .tint(Color.wakevPrimary)
            Text("Loading scenarios...")
                .font(.body)
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color.wakevSurfaceDark)
    }

    @ViewBuilder
    private var scenarioList: some View {
        ScrollView {
            LazyVStack(spacing: 16) {
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
                    .id(scenario.id)
                }
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 100)
        }
        .background(Color.wakevSurfaceDark)
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
                .foregroundColor(Color.wakevTextSecondaryDark)

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
                createScenarioButton
                    .padding(.horizontal, 32)
            }

            Spacer()
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding(24)
        .background(Color.wakevSurfaceDark)
    }
    
    private var createScenarioButton: some View {
        Button(action: { showCreateSheet = true }) {
            HStack {
                Image(systemName: "plus.circle.fill")
                Text("Create Scenario")
            }
            .font(.headline.weight(.semibold))
            .foregroundColor(.white)
            .frame(maxWidth: .infinity)
            .frame(height: 52)
            .background(Color.wakevPrimary)
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
        }
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
        LiquidGlassCard(
            cornerRadius: 16,
            padding: 0
        ) {
            VStack(alignment: .leading, spacing: 12) {
                headerSection
                    .padding(16)

                LiquidGlassDivider(style: .subtle)

                detailsSection
                    .padding(.horizontal, 16)

                LiquidGlassDivider(style: .subtle)

                votingSection
                    .padding(.horizontal, 16)

                LiquidGlassDivider(style: .subtle)

                votingButtonsSection
                    .padding(.horizontal, 16)
                    .padding(.bottom, 8)

                if isLocked {
                    lockWarning
                        .padding(.horizontal, 16)
                }

                if isComparisonMode {
                    comparisonSection
                        .padding(.horizontal, 16)
                        .padding(.bottom, 12)
                }
            }
        }
        .overlay(
            RoundedRectangle(cornerRadius: 16)
                .stroke(
                    isSelected ? Color.wakevPrimary : Color.clear,
                    lineWidth: 2
                )
        )
    }

    // MARK: - Header Section

    private var headerSection: some View {
        HStack(alignment: .top, spacing: 12) {
            VStack(alignment: .leading, spacing: 6) {
                HStack(spacing: 8) {
                    Text(scenario.name)
                        .font(.headline)
                        .fontWeight(.bold)
                        .lineLimit(1)

                    if scenario.status == "SELECTED" {
                        LiquidGlassBadge(
                            text: "Selected",
                            style: .success
                        )
                    }
                }

                Text(scenario.description)
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .lineLimit(2)
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            if isOrganizer {
                HStack(spacing: 8) {
                    editButton
                    deleteButton
                }
            }
        }
    }
    
    private var editButton: some View {
        Button(action: onEdit) {
            Image(systemName: "pencil")
                .font(.body)
                .foregroundColor(Color.wakevPrimary)
                .frame(width: 32, height: 32)
                .background(Color.wakevPrimary.opacity(0.1))
                .clipShape(Circle())
        }
        .buttonStyle(.plain)
    }
    
    private var deleteButton: some View {
        Button(action: onDelete) {
            Image(systemName: "trash")
                .font(.body)
                .foregroundColor(Color.wakevError)
                .frame(width: 32, height: 32)
                .background(Color.wakevError.opacity(0.1))
                .clipShape(Circle())
        }
        .buttonStyle(.plain)
    }

    // MARK: - Details Section

    private var detailsSection: some View {
        VStack(spacing: 10) {
            HStack(spacing: 12) {
                LiquidGlassListItem(
                    title: scenario.dateOrPeriod,
                    icon: "calendar"
                ) {
                    Text("Date")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            }

            HStack(spacing: 12) {
                LiquidGlassListItem(
                    title: "\(scenario.estimatedParticipants) people",
                    icon: "person.2"
                ) {
                    Text("Participants")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            }

            LiquidGlassListItem(
                title: scenario.location,
                icon: "mappin.circle"
            ) {
                Text("Location")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }

            HStack(spacing: 12) {
                LiquidGlassListItem(
                    title: "\(scenario.duration) days",
                    icon: "calendar.badge.clock"
                )

                LiquidGlassListItem(
                    title: "₹\(Int(scenario.estimatedBudgetPerPerson))/person",
                    icon: "indianrupee"
                )
            }
        }
        .padding(.vertical, 8)
    }

    // MARK: - Voting Section

    private var votingSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text("Voting Results")
                    .font(.subheadline)
                    .fontWeight(.bold)

                Spacer()

                Text("Score: \(scenario.votingResult.score)")
                    .font(.subheadline)
                    .fontWeight(.bold)
                    .foregroundColor(scoreColor)
            }

            if scenario.votingResult.totalVotes == 0 {
                Text("No votes yet")
                    .font(.caption)
                    .foregroundColor(.secondary)
            } else {
                VStack(spacing: 8) {
                    VoteBreakdownRow(
                        label: "Prefer",
                        count: scenario.votingResult.preferCount,
                        percentage: scenario.votingResult.preferPercentage,
                        color: Color.wakevPrimary
                    )

                    VoteBreakdownRow(
                        label: "Neutral",
                        count: scenario.votingResult.neutralCount,
                        percentage: scenario.votingResult.neutralPercentage,
                        color: Color.wakevWarning
                    )

                    VoteBreakdownRow(
                        label: "Against",
                        count: scenario.votingResult.againstCount,
                        percentage: scenario.votingResult.againstPercentage,
                        color: Color.wakevError
                    )

                    Text("Total: \(scenario.votingResult.totalVotes) votes")
                        .font(.caption2)
                        .foregroundColor(.secondary)
                        .padding(.top, 4)
                }
            }
        }
        .padding(.vertical, 8)
    }

    private var scoreColor: Color {
        if scenario.votingResult.score > 0 {
            return Color.wakevSuccess
        } else if scenario.votingResult.score < 0 {
            return Color.wakevError
        } else {
            return .secondary
        }
    }

    // MARK: - Voting Buttons Section

    private var votingButtonsSection: some View {
        HStack(spacing: 12) {
            VotingButton(
                emoji: "👍",
                label: "Prefer",
                action: { onVote(scenario.id, "PREFER") },
                disabled: isLocked,
                color: Color.wakevSuccess
            )

            VotingButton(
                emoji: "😐",
                label: "Neutral",
                action: { onVote(scenario.id, "NEUTRAL") },
                disabled: isLocked,
                color: Color.wakevWarning
            )

            VotingButton(
                emoji: "👎",
                label: "Against",
                action: { onVote(scenario.id, "AGAINST") },
                disabled: isLocked,
                color: Color.wakevError
            )
        }
    }

    // MARK: - Lock Warning

    private var lockWarning: some View {
        HStack {
            Image(systemName: "lock.fill")
                .foregroundColor(Color.wakevWarning)
            Text("Voting locked")
                .font(.caption)
                .fontWeight(.medium)
                .foregroundColor(Color.wakevWarning)
        }
        .padding(.vertical, 4)
    }

    // MARK: - Comparison Section

    private var comparisonSection: some View {
        Button(action: { onSelect(scenario.id) }) {
            HStack(spacing: 12) {
                Image(systemName: isSelected ? "checkmark.circle.fill" : "circle")
                    .font(.title3)
                    .foregroundColor(isSelected ? Color.wakevPrimary : .secondary)

                Text("Select for comparison")
                    .font(.subheadline)
                    .foregroundColor(.primary)

                Spacer()
            }
            .padding(12)
            .background(isSelected ? Color.wakevPrimary.opacity(0.1) : Color.clear)
            .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
        }
        .buttonStyle(.plain)
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
                .foregroundColor(.secondary)
                .frame(width: 60, alignment: .leading)

            GeometryReader { geometry in
                ZStack(alignment: .leading) {
                    Capsule()
                        .fill(color.opacity(0.15))

                    Capsule()
                        .fill(color)
                        .frame(width: geometry.size.width * (percentage / 100))
                }
            }
            .frame(height: 8)

            Text("\(count) (\(String(format: "%.0f", percentage))%)")
                .font(.caption)
                .monospacedDigit()
                .frame(width: 60, alignment: .trailing)
                .foregroundColor(.secondary)
        }
    }
}

// MARK: - Voting Button

struct VotingButton: View {
    let emoji: String
    let label: String
    let action: () -> Void
    let disabled: Bool
    let color: Color

    var body: some View {
        Button(action: action) {
            VStack(spacing: 4) {
                Text(emoji)
                    .font(.title3)
                Text(label)
                    .font(.caption2)
                    .fontWeight(.semibold)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 12)
            .background(disabled ? Color.wakevTextSecondaryDark.opacity(0.1) : color.opacity(0.1))
            .foregroundColor(disabled ? Color.wakevTextSecondaryDark.opacity(0.5) : color)
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(disabled ? Color.clear : color.opacity(0.3), lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
        .disabled(disabled)
        .opacity(disabled ? 0.6 : 1.0)
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
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }

                ToolbarItem(placement: .confirmationAction) {
                    Button(scenario == nil ? "Create" : "Update") {
                        saveScenario()
                    }
                    .fontWeight(.semibold)
                    .disabled(!isValid)
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

    private var isValid: Bool {
        !name.trimmingCharacters(in: .whitespaces).isEmpty &&
        !description.trimmingCharacters(in: .whitespaces).isEmpty
    }

    private func saveScenario() {
        let newScenario = MockScenario(
            id: scenario?.id ?? "scenario-\(UUID().uuidString)",
            eventId: eventId,
            name: name.trimmingCharacters(in: .whitespaces),
            dateOrPeriod: dateOrPeriod,
            location: location,
            duration: Int(duration) ?? 3,
            estimatedParticipants: Int(estimatedParticipants) ?? 5,
            estimatedBudgetPerPerson: Double(budget) ?? 1000.0,
            description: description.trimmingCharacters(in: .whitespaces),
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
