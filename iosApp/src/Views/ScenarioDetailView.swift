import SwiftUI
import Shared

/// Scenario Detail View - iOS
///
/// Displays detailed information about a single scenario.
/// Uses ScenarioDetailViewModel with State Machine pattern.
/// Organizers can edit and delete scenarios.
///
/// Refactored to use Liquid Glass design system components:
/// - LiquidGlassCard for content sections
/// - LiquidGlassButton for actions
/// - LiquidGlassBadge for status indicators
/// - LiquidGlassDivider for separators
/// - LiquidGlassTextField for form inputs
/// - LiquidGlassListItem for information display
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
                    backButton
                }
                
                ToolbarItem(placement: .navigationBarTrailing) {
                    toolbarTrailingContent
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
                commentsSheetView
            }
        }
    }
    
    // MARK: - Toolbar Content
    
    private var backButton: some View {
        LiquidGlassButton(
            icon: "arrow.left",
            style: .text,
            size: .small,
            action: {
                if viewModel.isEditing {
                    viewModel.cancelEditing()
                } else {
                    onBack()
                }
            }
        )
        .accessibilityLabel("Back")
        .accessibilityHint("Return to previous screen")
    }
    
    @ViewBuilder
    private var toolbarTrailingContent: some View {
        HStack(spacing: 12) {
            CommentButton(commentCount: 0) {
                showComments = true
            }
            .accessibilityLabel("Comments")
            .accessibilityHint("View comments")
            
            if isOrganizer {
                if viewModel.isEditing {
                    saveButton
                } else {
                    organizerMenuButton
                }
            }
        }
    }
    
    private var saveButton: some View {
        LiquidGlassButton(
            title: "Save",
            style: .primary,
            size: .medium,
            isDisabled: viewModel.state.isLoading,
            action: saveChanges
        )
        .accessibilityLabel("Save changes")
        .accessibilityHint("Save the current edits")
    }
    
    private var organizerMenuButton: some View {
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
            LiquidGlassButton(
                icon: "ellipsis",
                style: .icon,
                size: .small,
                action: {}
            )
        }
        .accessibilityLabel("More options")
        .accessibilityHint("Edit or delete scenario")
    }
    
    // MARK: - Comments Sheet
    
    private var commentsSheetView: some View {
        NavigationView {
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
                    LiquidGlassButton(
                        title: "Close",
                        style: .text,
                        action: { showComments = false }
                    )
                }
            }
        }
    }
    
    // MARK: - Detail View
    
    private func detailView(scenario: Scenario_) -> some View {
        VStack(spacing: 16) {
            // Header card
            LiquidGlassCard {
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
            }
            
            // Details sections using LiquidGlassCard.thin
            DetailSection(title: "When", icon: "calendar") {
                LiquidGlassListItem(
                    title: "Date/Period",
                    subtitle: scenario.dateOrPeriod
                )
                
                LiquidGlassDivider(style: .subtle)
                
                LiquidGlassListItem(
                    title: "Duration",
                    subtitle: "\(Int(scenario.duration)) days"
                )
            }
            
            DetailSection(title: "Where", icon: "mappin.circle") {
                LiquidGlassListItem(
                    title: "Location",
                    subtitle: scenario.location
                )
            }
            
            DetailSection(title: "Group", icon: "person.2") {
                LiquidGlassListItem(
                    title: "Estimated Participants",
                    subtitle: "\(scenario.estimatedParticipants)"
                )
            }
            
            DetailSection(title: "Budget", icon: "dollarsign.circle") {
                LiquidGlassListItem(
                    title: "Per Person",
                    subtitle: String(format: "$%.2f", scenario.estimatedBudgetPerPerson)
                )
                
                LiquidGlassDivider(style: .subtle)
                
                LiquidGlassListItem(
                    title: "Total Estimated",
                    subtitle: String(
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
            
            LiquidGlassCard(cornerRadius: 12, padding: 20, opacity: 0.9, intensity: 0.8) {
                VStack(spacing: 16) {
                    LiquidGlassTextField(
                        title: "Name",
                        placeholder: "Scenario name",
                        text: $editName
                    )
                    
                    LiquidGlassTextField(
                        title: "Location",
                        placeholder: "Event location",
                        text: $editLocation
                    )
                    
                    LiquidGlassTextField(
                        title: "Date/Period",
                        placeholder: "Event date or period",
                        text: $editDateOrPeriod
                    )
                    
                    LiquidGlassTextField(
                        title: "Duration (days)",
                        placeholder: "Number of days",
                        text: $editDuration,
                        keyboardType: .numberPad
                    )
                    
                    LiquidGlassTextField(
                        title: "Est. Participants",
                        placeholder: "Number of participants",
                        text: $editParticipants,
                        keyboardType: .numberPad
                    )
                    
                    LiquidGlassTextField(
                        title: "Budget per Person",
                        placeholder: "Budget amount",
                        text: $editBudget,
                        keyboardType: .decimalPad
                    )
                    
                    // Description text editor
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
            }
        }
    }
    
    // MARK: - Loading View
    
    private var loadingView: some View {
        VStack(spacing: 20) {
            ProgressView()
                .progressViewStyle(CircularProgressViewStyle())
                .scaleEffect(1.5)
            
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
                    .fill(Color.wakeveError.opacity(0.1))
                    .frame(width: 80, height: 80)
                
                Image(systemName: "exclamationmark.triangle")
                    .font(.system(size: 36))
                    .foregroundColor(.wakeveError)
            }
            
            VStack(spacing: 8) {
                Text("Scenario Not Found")
                    .font(.system(size: 24, weight: .bold))
                    .foregroundColor(.primary)
                
                Text("This scenario may have been deleted.")
                    .font(.system(size: 17))
                    .foregroundColor(.secondary)
            }
            
            LiquidGlassButton(
                title: "Go Back",
                style: .secondary,
                action: onBack
            )
            .padding(.top, 16)
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

// MARK: - Scenario Status Badge

/// Maps scenario status to LiquidGlassBadge type
struct ScenarioStatusBadge: View {
    let status: String
    
    private var badgeStyle: LiquidGlassBadgeStyle {
        switch status.uppercased() {
        case "DRAFT":
            return .default
        case "POLLING":
            return .info
        case "CONFIRMED":
            return .success
        case "ACTIVE":
            return .success
        case "COMPLETED":
            return .accent
        case "CANCELLED", "CANCELED":
            return .warning
        case "ARCHIVED":
            return .warning
        default:
            return .default
        }
    }

    var body: some View {
        LiquidGlassBadge(
            text: status.capitalized,
            style: badgeStyle
        )
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
        LiquidGlassCard(cornerRadius: 12, padding: 20, opacity: 0.9, intensity: 0.8) {
            VStack(alignment: .leading, spacing: 16) {
                HStack(spacing: 8) {
                    Image(systemName: icon)
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundColor(.wakeveAccent)
                    
                    Text(title)
                        .font(.system(size: 17, weight: .semibold))
                        .foregroundColor(.primary)
                }
                
                content
            }
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
                    Text("Prefer")
                        .font(.system(size: 15, weight: .medium))
                        .foregroundColor(.primary)
                    
                    Text("\(result.preferCount)")
                        .font(.system(size: 13))
                        .foregroundColor(.secondary)
                }
                
                Spacer()
                
                VStack(alignment: .leading, spacing: 4) {
                    Text("Neutral")
                        .font(.system(size: 15, weight: .medium))
                        .foregroundColor(.primary)
                    
                    Text("\(result.neutralCount)")
                        .font(.system(size: 13))
                        .foregroundColor(.secondary)
                }
                
                Spacer()
                
                VStack(alignment: .leading, spacing: 4) {
                    Text("Against")
                        .font(.system(size: 15, weight: .medium))
                        .foregroundColor(.primary)
                    
                    Text("\(result.againstCount)")
                        .font(.system(size: 13))
                        .foregroundColor(.secondary)
                }
            }
            
            LiquidGlassDivider(style: .subtle)
            
            HStack {
                Text("Total Score")
                    .font(.system(size: 15))
                    .foregroundColor(.secondary)
                
                Spacer()
                
                Text("\(result.score)")
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundColor(.wakevePrimary)
            }
        }
    }
}

// MARK: - Preview

struct ScenarioDetailView_Previews: PreviewProvider {
    static var previews: some View {
        ScenarioDetailView(
            scenarioId: "preview-scenario",
            eventId: "preview-event",
            isOrganizer: true,
            currentUserId: "user-123",
            currentUserName: "John Doe"
        )
    }
}
