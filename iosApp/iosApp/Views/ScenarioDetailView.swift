import SwiftUI
import Shared

/// Scenario Detail View - iOS
///
/// Displays detailed information about a single scenario.
/// Organizers can edit and delete scenarios.
struct ScenarioDetailView: View {
    let scenarioId: String
    let eventId: String
    let repository: ScenarioRepository
    let isOrganizer: Bool
    let currentUserId: String
    let currentUserName: String
    let onBack: () -> Void
    let onDeleted: () -> Void
    
    @State private var scenario: Scenario_?
    @State private var isLoading = true
    @State private var isEditing = false
    @State private var isSaving = false
    @State private var showDeleteConfirm = false
    @State private var errorMessage = ""
    @State private var showError = false
    
    // Comments state
    @State private var commentCount = 0
    @State private var showComments = false
    
    // Edit fields
    @State private var editName = ""
    @State private var editLocation = ""
    @State private var editDateOrPeriod = ""
    @State private var editDuration = ""
    @State private var editParticipants = ""
    @State private var editBudget = ""
    @State private var editDescription = ""
    
    var body: some View {
        NavigationView {
            ZStack {
                Color(.systemGroupedBackground)
                    .ignoresSafeArea()
                
                VStack(spacing: 0) {
                    if isLoading {
                        loadingView
                    } else if let scenario = scenario {
                        ScrollView {
                            VStack(spacing: 16) {
                                if isEditing {
                                    editFormView
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
            .navigationTitle(scenario?.name ?? "Scenario Details")
            .navigationBarTitleDisplayMode(.large)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(action: {
                        if isEditing {
                            isEditing = false
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
                        CommentButton(commentCount: commentCount) {
                            showComments = true
                        }
                        
                        if isOrganizer {
                            if isEditing {
                                Button {
                                    Task {
                                        await saveChanges()
                                    }
                                } label: {
                                    if isSaving {
                                        ProgressView()
                                            .progressViewStyle(CircularProgressViewStyle())
                                    } else {
                                        Text("Save")
                                            .font(.system(size: 17, weight: .semibold))
                                            .foregroundColor(.blue)
                                    }
                                }
                                .disabled(isSaving)
                            } else {
                                Menu {
                                    Button {
                                        isEditing = true
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
    
    // MARK: - Comments
    
}
                    }
                }
            }
            .onAppear {
                loadScenario()
                
            }
            .alert("Error", isPresented: $showError) {
                Button("OK", role: .cancel) {}
            } message: {
                Text(errorMessage)
            }
            .alert("Delete Scenario", isPresented: $showDeleteConfirm) {
                Button("Cancel", role: .cancel) {}
                Button("Delete", role: .destructive) {
                    Task {
                        await deleteScenario()
                    }
                }
            } message: {
                Text("Are you sure you want to delete this scenario? This action cannot be undone.")
            }
            .sheet(isPresented: $showComments) {
                NavigationView {
                    CommentsView(
                        eventId: eventId,
                        section: .SCENARIO,
                        sectionItemId: scenario?.id,
                        currentUserId: currentUserId,
                        currentUserName: currentUserName
                    )
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
    
    // MARK: - Header View (removed - now in toolbar)
    
    // MARK: - Detail View
    
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
                    
                    ScenarioStatusBadge(status: ScenarioStatus(rawValue: scenario.status.name) ?? .proposed)
                }
                
                if !scenario.description.isEmpty {
                    Text(scenario.description)
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
        }
    }
    
    // MARK: - Edit Form View
    
    private var editFormView: some View {
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
    
    // MARK: - Data Loading & Actions
    
    private func loadScenario() {
        Task {
            let loadedScenario = repository.getScenarioById(id: scenarioId)
            
            await MainActor.run {
                if let loadedScenario = loadedScenario {
                    self.scenario = loadedScenario
                    self.editName = loadedScenario.name
                    self.editLocation = loadedScenario.location
                    self.editDateOrPeriod = loadedScenario.dateOrPeriod
                    self.editDuration = "\(Int(loadedScenario.duration))"
                    self.editParticipants = "\(loadedScenario.estimatedParticipants)"
                    self.editBudget = "\(loadedScenario.estimatedBudgetPerPerson)"
                    self.editDescription = loadedScenario.description_
                }
                self.isLoading = false
            }
        }
    }
    
    private func saveChanges() async {
        guard let scenario = scenario,
              let duration = Int(editDuration),
              let participants = Int(editParticipants),
              let budget = Double(editBudget) else {
            await MainActor.run {
                self.errorMessage = "Please enter valid numbers for duration, participants, and budget"
                self.showError = true
            }
            return
        }
        
        await MainActor.run {
            self.isSaving = true
        }
        
        do {
            let updatedScenario = Scenario_(
                id: scenario.id,
                eventId: scenario.eventId,
                name: editName,
                dateOrPeriod: editDateOrPeriod,
                location: editLocation,
                duration: Int32(duration),
                estimatedParticipants: Int32(participants),
                estimatedBudgetPerPerson: budget,
                description: editDescription,
                status: scenario.status,
                createdAt: scenario.createdAt,
                updatedAt: scenario.updatedAt
            )
            
            _ = try await repository.updateScenario(scenario: updatedScenario)
            
            await MainActor.run {
                self.scenario = updatedScenario
                self.isSaving = false
                self.isEditing = false
            }
        } catch {
            await MainActor.run {
                self.errorMessage = error.localizedDescription
                self.showError = true
                self.isSaving = false
            }
        }
    }
    
    private func deleteScenario() async {
        do {
            _ = try await repository.deleteScenario(scenarioId: scenarioId)
            
            await MainActor.run {
                onDeleted()
            }
        } catch {
            await MainActor.run {
                self.errorMessage = error.localizedDescription
                self.showError = true
            }
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
