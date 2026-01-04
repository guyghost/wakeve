import SwiftUI
import Shared

/// Scenario Detail View - iOS
///
/// Displays detailed information about a single scenario.
/// Uses ScenarioDetailViewModel with State Machine pattern.
/// Organizers can edit and delete scenarios.
struct ScenarioDetailView: View {
    let scenarioId: String
    let eventId: String
    let isOrganizer: Bool
    let currentUserId: String
    let currentUserName: String
    let onBack: () -> Void
    let onDeleted: () -> Void
    
    @StateObject private var viewModel: ScenarioDetailViewModel
    
    // Local UI state
    @State private var showDeleteConfirm = false
    @State private var showComments = false
    
    // Edit fields
    @State private var editName = ""
    @State private var editLocation = ""
    @State private var editDateOrPeriod = ""
    @State private var editDuration = ""
    @State private var editParticipants = ""
    @State private var editBudget = ""
    @State private var editDescription = ""
    
    init(
        scenarioId: String,
        eventId: String,
        isOrganizer: Bool = false,
        currentUserId: String = "",
        currentUserName: String = "",
        onBack: @escaping () -> Void = {},
        onDeleted: @escaping () -> Void = {}
    ) {
        self.scenarioId = scenarioId
        self.eventId = eventId
        self.isOrganizer = isOrganizer
        self.currentUserId = currentUserId
        self.currentUserName = currentUserName
        self.onBack = onBack
        self.onDeleted = onDeleted
        self._viewModel = StateObject(wrappedValue: ScenarioDetailViewModel(scenarioId: scenarioId))
    }
    
    var body: some View {
        NavigationView {
            ZStack {
                Color(.systemGroupedBackground)
                    .ignoresSafeArea()
                
                VStack(spacing: 0) {
                    if viewModel.isLoading {
                        loadingView
                    } else if let scenario = viewModel.scenario {
                        ScrollView {
                            VStack(spacing: 16) {
                                if viewModel.isEditing {
                                    editFormView(scenario: scenario)
                                } else {
                                    detailView(scenario: scenario)
                                }
                                
                                Spacer()
                                    .frame(height: 40)
                            }
                            .padding(.horizontal, 20)
                            .padding(.top, 16)
                        }
                    } else {
                        errorStateView
                    }
                }
            }
            .navigationTitle(viewModel.scenario?.name ?? "Scenario Details")
            .navigationBarTitleDisplayMode(.large)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(action: {
                        if viewModel.isEditing {
                            viewModel.cancelEditing()
                        } else {
                            onBack()
                        }
                    }) {
                        Image(systemName: "arrow.left")
                            .font(.system(size: 16, weight: .semibold))
                            .foregroundColor(.secondary)
                            .frame(width: 36, height: 36)
                            .background(Color(.tertiarySystemFill))
                            .clipShape(Circle())
                    }
                }
                
                ToolbarItem(placement: .navigationBarTrailing) {
                    HStack(spacing: 12) {
                        CommentButton(commentCount: 0) {
                            showComments = true
                        }
                        
                        if isOrganizer {
                            if viewModel.isEditing {
                                Button {
                                    saveChanges()
                                } label: {
                                    if viewModel.state.isLoading {
                                        ProgressView()
                                            .progressViewStyle(CircularProgressViewStyle())
                                    } else {
                                        Text("Save")
                                            .font(.system(size: 17, weight: .semibold))
                                            .foregroundColor(.blue)
                                    }
                                }
                                .disabled(viewModel.state.isLoading)
                            } else {
                                Menu {
                                    Button {
                                        viewModel.startEditing()
                                        initializeEditFields()
                                    } label: {
                                        Label("Edit", systemImage: "pencil")
                                    }
                                    
                                    Button(role: .destructive) {
                                        showDeleteConfirm = true
                                    } label: {
                                        Label("Delete", systemImage: "trash")
                                    }
                                } label: {
                                    Image(systemName: "ellipsis")
                                        .font(.system(size: 16, weight: .semibold))
                                        .foregroundColor(.secondary)
                                        .frame(width: 36, height: 36)
                                        .background(Color(.tertiarySystemFill))
                                        .clipShape(Circle())
                                }
                            }
                        }
                    }
                }
            }
            .onAppear {
                viewModel.reload()
            }
            .alert("Error", isPresented: Binding(
                get: { viewModel.hasError },
                set: { if !$0 { viewModel.clearError() } }
            )) {
                Button("OK", role: .cancel) { viewModel.clearError() }
            } message: {
                Text(viewModel.errorMessage ?? "An error occurred")
            }
            .alert("Delete Scenario", isPresented: $showDeleteConfirm) {
                Button("Cancel", role: .cancel) {}
                Button("Delete", role: .destructive) {
                    deleteScenario()
                }
            } message: {
                Text("Are you sure you want to delete this scenario? This action cannot be undone.")
            }
            .sheet(isPresented: $showComments) {
                NavigationView {
                    // TODO: Re-enable CommentsView when Shared types are properly integrated
                    VStack(spacing: 16) {
                        Image(systemName: "bubble.left.and.bubble.right")
                            .font(.system(size: 48))
                            .foregroundColor(.secondary)
                        Text("Comments - Coming Soon")
                            .font(.title2)
                            .foregroundColor(.secondary)
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .navigationBarTitleDisplayMode(.inline)
                    .toolbar {
                        ToolbarItem(placement: .navigationBarLeading) {
                            Button("Fermer") {
                                showComments = false
                            }
                        }
                    }
                }
            }
        }
    }
    
    // MARK: - Detail View
    
    private func detailView(scenario: Scenario_) -> some View {
        VStack(spacing: 16) {
            // Header card
            VStack(alignment: .leading, spacing: 12) {
                HStack {
                    Text(scenario.name)
                        .font(.system(size: 28, weight: .bold))
                        .foregroundColor(.primary)
                    
                    Spacer()
                    
                    ScenarioStatusBadge(status: scenario.status.name)
                }
                
                if !scenario.description_.isEmpty {
                    Text(scenario.description_)
                        .font(.system(size: 15))
                        .foregroundColor(.secondary)
                        .fixedSize(horizontal: false, vertical: true)
                }
            }
            .padding(20)
            .glassCard()
            
            // Details sections
            DetailSection(title: "When", icon: "calendar") {
                DetailItem(label: "Date/Period", value: scenario.dateOrPeriod)
                DetailItem(label: "Duration", value: "\(Int(scenario.duration)) days")
            }
            
            DetailSection(title: "Where", icon: "mappin.circle") {
                DetailItem(label: "Location", value: scenario.location)
            }
            
            DetailSection(title: "Group", icon: "person.2") {
                DetailItem(label: "Estimated Participants", value: "\(scenario.estimatedParticipants)")
            }
            
            DetailSection(title: "Budget", icon: "dollarsign.circle") {
                DetailItem(
                    label: "Per Person",
                    value: String(format: "$%.2f", scenario.estimatedBudgetPerPerson)
                )
                DetailItem(
                    label: "Total Estimated",
                    value: String(
                        format: "$%.2f",
                        scenario.estimatedBudgetPerPerson * Double(scenario.estimatedParticipants)
                    )
                )
            }
            
            // Voting Results
            if let votingResult = viewModel.votingResult, votingResult.totalVotes > 0 {
                DetailSection(title: "Voting Results", icon: "hand.thumbsup") {
                    VotingResultsView(result: votingResult)
                }
            }
        }
    }
    
    // MARK: - Edit Form View
    
    private func editFormView(scenario: Scenario_) -> some View {
        VStack(spacing: 16) {
            Text("Edit Scenario")
                .font(.system(size: 28, weight: .bold))
                .frame(maxWidth: .infinity, alignment: .leading)
            
            VStack(spacing: 16) {
                FormField(label: "Name", text: $editName)
                FormField(label: "Location", text: $editLocation)
                FormField(label: "Date/Period", text: $editDateOrPeriod)
                FormField(label: "Duration (days)", text: $editDuration, keyboardType: .numberPad)
                FormField(label: "Est. Participants", text: $editParticipants, keyboardType: .numberPad)
                FormField(label: "Budget per Person", text: $editBudget, keyboardType: .decimalPad)
                
                VStack(alignment: .leading, spacing: 8) {
                    Text("Description")
                        .font(.system(size: 15, weight: .medium))
                        .foregroundColor(.primary)
                    
                    TextEditor(text: $editDescription)
                        .font(.system(size: 15))
                        .frame(minHeight: 100)
                        .padding(12)
                        .background(Color(.tertiarySystemFill))
                        .continuousCornerRadius(12)
                }
            }
            .padding(20)
            .glassCard()
        }
    }
    
    // MARK: - Loading View
    
    private var loadingView: some View {
        VStack(spacing: 20) {
            ProgressView()
                .progressViewStyle(CircularProgressViewStyle())
            
            Text("Loading scenario...")
                .font(.system(size: 17))
                .foregroundColor(.secondary)
        }
        .frame(maxHeight: .infinity)
    }
    
    // MARK: - Error State
    
    private var errorStateView: some View {
        VStack(spacing: 24) {
            ZStack {
                Circle()
                    .fill(Color.red.opacity(0.1))
                    .frame(width: 80, height: 80)
                
                Image(systemName: "exclamationmark.triangle")
                    .font(.system(size: 36))
                    .foregroundColor(.red)
            }
            
            VStack(spacing: 8) {
                Text("Scenario Not Found")
                    .font(.system(size: 24, weight: .bold))
                    .foregroundColor(.primary)
                
                Text("This scenario may have been deleted.")
                    .font(.system(size: 17))
                    .foregroundColor(.secondary)
            }
        }
        .frame(maxHeight: .infinity)
    }
    
    // MARK: - Data Actions
    
    /// Initialize edit fields from the current scenario
    private func initializeEditFields() {
        guard let scenario = viewModel.scenario else { return }
        
        editName = scenario.name
        editLocation = scenario.location
        editDateOrPeriod = scenario.dateOrPeriod
        editDuration = "\(Int(scenario.duration))"
        editParticipants = "\(scenario.estimatedParticipants)"
        editBudget = "\(scenario.estimatedBudgetPerPerson)"
        editDescription = scenario.description_
    }
    
    /// Save changes to the scenario
    private func saveChanges() {
        guard let _ = viewModel.scenario else { return }
        
        // Validate input
        guard let duration = Int32(editDuration),
              let participants = Int32(editParticipants),
              let budget = Double(editBudget) else {
            // Error handling could be improved with a dedicated error state
            print("Invalid input for duration, participants, or budget")
            return
        }
        
        // Dispatch update intent to the state machine
        viewModel.updateScenario(
            name: editName,
            dateOrPeriod: editDateOrPeriod,
            location: editLocation,
            duration: duration,
            estimatedParticipants: participants,
            estimatedBudgetPerPerson: budget,
            description: editDescription
        )
    }
    
    /// Delete the scenario
    private func deleteScenario() {
        viewModel.deleteScenario()
        // Trigger onDeleted callback after deletion
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
            onDeleted()
        }
    }
}

// MARK: - Detail Section

struct DetailSection<Content: View>: View {
    let title: String
    let icon: String
    let content: Content
    
    init(title: String, icon: String, @ViewBuilder content: () -> Content) {
        self.title = title
        self.icon = icon
        self.content = content()
    }
    
    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack(spacing: 8) {
                Image(systemName: icon)
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundColor(.blue)
                
                Text(title)
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundColor(.primary)
            }
            
            content
        }
        .padding(20)
        .glassCard()
    }
}

// MARK: - Detail Item

struct DetailItem: View {
    let label: String
    let value: String
    
    var body: some View {
        HStack {
            Text(label)
                .font(.system(size: 15))
                .foregroundColor(.secondary)
            
            Spacer()
            
            Text(value)
                .font(.system(size: 15, weight: .medium))
                .foregroundColor(.primary)
        }
    }
}

// MARK: - Form Field

struct FormField: View {
    let label: String
    @Binding var text: String
    var keyboardType: UIKeyboardType = .default
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(label)
                .font(.system(size: 15, weight: .medium))
                .foregroundColor(.primary)
            
            TextField("", text: $text)
                .font(.system(size: 15))
                .keyboardType(keyboardType)
                .padding(12)
                .background(Color(.tertiarySystemFill))
                .continuousCornerRadius(12)
        }
    }
}

// MARK: - Voting Results View

struct VotingResultsView: View {
    let result: ScenarioVotingResult
    
    var body: some View {
        VStack(spacing: 12) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text("👍 Prefer")
                        .font(.system(size: 15, weight: .medium))
                        .foregroundColor(.primary)
                    
                    Text("\(result.preferCount)")
                        .font(.system(size: 13))
                        .foregroundColor(.secondary)
                }
                
                Spacer()
                
                VStack(alignment: .leading, spacing: 4) {
                    Text("😐 Neutral")
                        .font(.system(size: 15, weight: .medium))
                        .foregroundColor(.primary)
                    
                    Text("\(result.neutralCount)")
                        .font(.system(size: 13))
                        .foregroundColor(.secondary)
                }
                
                Spacer()
                
                VStack(alignment: .leading, spacing: 4) {
                    Text("👎 Against")
                        .font(.system(size: 15, weight: .medium))
                        .foregroundColor(.primary)
                    
                    Text("\(result.againstCount)")
                        .font(.system(size: 13))
                        .foregroundColor(.secondary)
                }
            }
            
            Divider()
            
            HStack {
                Text("Total Score")
                    .font(.system(size: 15))
                    .foregroundColor(.secondary)
                
                Spacer()
                
                Text("\(result.score)")
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundColor(.blue)
            }
        }
    }
}
