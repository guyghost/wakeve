import SwiftUI

// Colors and LiquidGlass components are defined in Theme/WakeveColors.swift and UIComponents/

// MARK: - Mock Models (pour compilation - à remplacer par Shared module)

struct ActivityParticipant: Identifiable {
    let id: String
    let name: String
}

// MARK: - Models

struct ActivityModel: Identifiable {
    let id: String
    var eventId: String
    var scenarioId: String?
    var name: String
    var description: String
    var date: Date
    var time: String?
    var durationMinutes: Int
    var location: String
    var costPerPerson: Int64
    var maxParticipants: Int?
    var organizerId: String?
    var registeredCount: Int
    var isFull: Bool
    var createdAt: String
    var updatedAt: String
}

// CommentButton is defined in Components/CommentButton.swift

// MARK: - Form Sheets

struct ActivityFormSheet: View {
    let eventId: String
    let activity: ActivityModel?
    let participants: [ActivityParticipant]
    let onSave: (ActivityModel) -> Void
    
    @Environment(\.dismiss) private var dismiss
    
    @State private var name: String
    @State private var description: String
    @State private var date: Date
    @State private var time: String
    @State private var durationMinutes: String
    @State private var location: String
    @State private var costPerPerson: String
    @State private var maxParticipants: String
    
    init(eventId: String, activity: ActivityModel?, participants: [ActivityParticipant], onSave: @escaping (ActivityModel) -> Void) {
        self.eventId = eventId
        self.activity = activity
        self.participants = participants
        self.onSave = onSave
        
        _name = State(initialValue: activity?.name ?? "")
        _description = State(initialValue: activity?.description ?? "")
        _date = State(initialValue: activity?.date ?? Date())
        _time = State(initialValue: activity?.time ?? "")
        _durationMinutes = State(initialValue: activity != nil ? "\(activity!.durationMinutes)" : "60")
        _location = State(initialValue: activity?.location ?? "")
        _costPerPerson = State(initialValue: activity != nil ? "\(activity!.costPerPerson / 100)" : "")
        _maxParticipants = State(initialValue: activity?.maxParticipants != nil ? "\(activity!.maxParticipants!)" : "")
    }
    
    var isValid: Bool {
        !name.isEmpty && Int(durationMinutes) != nil && Int(durationMinutes)! > 0
    }
    
    var body: some View {
        NavigationView {
            Form {
                Section("Informations générales") {
                    TextField("Nom *", text: $name)
                    TextField("Description", text: $description, axis: .vertical)
                        .lineLimit(3...5)
                }
                
                Section("Date et heure") {
                    DatePicker("Date", selection: $date, displayedComponents: .date)
                    TextField("Heure (HH:MM)", text: $time, prompt: Text("14:30"))
                    HStack {
                        Text("Durée (minutes)")
                        Spacer()
                        TextField("60", text: $durationMinutes)
                            .multilineTextAlignment(.trailing)
                            .frame(width: 60)
                    }
                }
                
                Section("Lieu et coût") {
                    TextField("Lieu", text: $location)
                    HStack {
                        Text("Coût par personne (€)")
                        Spacer()
                        TextField("0", text: $costPerPerson)
                            .multilineTextAlignment(.trailing)
                            .frame(width: 80)
                    }
                }
                
                Section("Capacité") {
                    HStack {
                        Text("Places maximum")
                        Spacer()
                        TextField("Illimité", text: $maxParticipants)
                            .multilineTextAlignment(.trailing)
                            .frame(width: 80)
                    }
                }
            }
            .navigationTitle(activity == nil ? String(localized: "common.add") : String(localized: "common.edit"))
            #if os(iOS)
            .navigationBarTitleDisplayMode(.inline)
            #endif
            .toolbar {
                #if os(iOS)
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(String(localized: "common.cancel")) { dismiss() }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(activity == nil ? String(localized: "common.add") : String(localized: "common.edit")) {
                        saveActivity()
                    }
                    .disabled(!isValid)
                    .fontWeight(.semibold)
                }
                #endif
            }
        }
    }
    
    private func saveActivity() {
        let costInCents = Int64((Double(costPerPerson) ?? 0) * 100)
        let now = ISO8601DateFormatter().string(from: Date())
        
        let updatedActivity = ActivityModel(
            id: activity?.id ?? UUID().uuidString,
            eventId: eventId,
            scenarioId: activity?.scenarioId,
            name: name.trimmingCharacters(in: .whitespaces),
            description: description.trimmingCharacters(in: .whitespaces),
            date: date,
            time: time.isEmpty ? nil : time,
            durationMinutes: Int(durationMinutes) ?? 60,
            location: location.trimmingCharacters(in: .whitespaces),
            costPerPerson: costInCents,
            maxParticipants: maxParticipants.isEmpty ? nil : Int(maxParticipants),
            organizerId: activity?.organizerId,
            registeredCount: activity?.registeredCount ?? 0,
            isFull: false,
            createdAt: activity?.createdAt ?? now,
            updatedAt: now
        )
        
        onSave(updatedActivity)
    }
}

struct ManageParticipantsSheet: View {
    let activity: ActivityModel
    let allParticipants: [ActivityParticipant]
    let onUpdate: (ActivityModel) -> Void
    
    @Environment(\.dismiss) private var dismiss
    @State private var registeredIds: Set<String> = []
    
    init(activity: ActivityModel, allParticipants: [ActivityParticipant], onUpdate: @escaping (ActivityModel) -> Void) {
        self.activity = activity
        self.allParticipants = allParticipants
        self.onUpdate = onUpdate
        _registeredIds = State(initialValue: [])
    }
    
    var canRegisterMore: Bool {
        if let max = activity.maxParticipants {
            return registeredIds.count < max
        }
        return true
    }
    
    var body: some View {
        NavigationView {
            VStack {
                if let max = activity.maxParticipants {
                    HStack {
                        Text("\(registeredIds.count) / \(max) inscrits")
                            .font(.headline)
                        Spacer()
                        if !canRegisterMore {
                            LiquidGlassBadge(
                                text: "Complet",
                                style: .warning
                            )
                        }
                    }
                    .padding()
                }
                
                List(allParticipants) { participant in
                    Button {
                        toggleParticipant(participant.id)
                    } label: {
                        HStack {
                            Text(participant.name)
                                .foregroundColor(.primary)
                            Spacer()
                            if registeredIds.contains(participant.id) {
                                Image(systemName: "checkmark.circle.fill")
                                    .foregroundColor(.wakeveSuccess)
                            } else {
                                Image(systemName: "circle")
                                    .foregroundColor(.secondary)
                            }
                        }
                    }
                    .disabled(!canRegisterMore && !registeredIds.contains(participant.id))
                }
            }
            .navigationTitle("Participants")
            #if os(iOS)
            .navigationBarTitleDisplayMode(.inline)
            #endif
            .toolbar {
                #if os(iOS)
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(String(localized: "common.close")) {
                        var updatedActivity = activity
                        updatedActivity.registeredCount = registeredIds.count
                        updatedActivity.isFull = activity.maxParticipants != nil && registeredIds.count >= activity.maxParticipants!
                        onUpdate(updatedActivity)
                        dismiss()
                    }
                    .fontWeight(.semibold)
                }
                #endif
            }
        }
    }
    
    private func toggleParticipant(_ id: String) {
        if registeredIds.contains(id) {
            registeredIds.remove(id)
        } else if canRegisterMore {
            registeredIds.insert(id)
        }
    }
}

// MARK: - Main View

/**
 * Activity Planning View (iOS) - Refactorisé avec Liquid Glass Design System
 *
 * Features:
 * - List activities grouped by date
 * - Create/Edit/Delete activities
 * - Participant registration with capacity management
 * - Display costs and registration count
 * - Filter by date
 * - Liquid Glass design system
 */
struct ActivityPlanningView: View {
    let eventId: String
    let currentUserId: String
    let currentUserName: String
    @Environment(\.dismiss) private var dismiss
    
    @State private var activities: [ActivityModel] = []
    @State private var participants: [ActivityParticipant] = []
    @State private var isLoading = false
    
    // Filters
    @State private var selectedDate: Date?
    
    // Sheets
    @State private var showAddActivitySheet = false
    @State private var showParticipantsSheet = false
    @State private var selectedActivity: ActivityModel?
    @State private var activityForParticipants: ActivityModel?
    
    // Alerts
    @State private var showDeleteAlert = false
    @State private var activityToDelete: ActivityModel?
    
    // Comments state
    @State private var commentCount = 0
    @State private var showComments = false
    
    var filteredActivities: [ActivityModel] {
        if let date = selectedDate {
            let calendar = Calendar.current
            return activities.filter { activity in
                calendar.isDate(activity.date, inSameDayAs: date)
            }
        }
        return activities
    }
    
    var activitiesByDate: [(date: Date, activities: [ActivityModel])] {
        let grouped = Dictionary(grouping: filteredActivities, by: { Calendar.current.startOfDay(for: $0.date) })
        return grouped.sorted { $0.key < $1.key }
            .map { ($0.key, $0.value.sorted { $0.time ?? "" < $1.time ?? "" }) }
    }
    
    var uniqueDates: [Date] {
        let dates = activities.map { Calendar.current.startOfDay(for: $0.date) }
        return Array(Set(dates)).sorted()
    }
    
    var totalCost: Int64 {
        activities.reduce(0) { $0 + $1.costPerPerson }
    }
    
    var body: some View {
        NavigationView {
            ZStack {
                // Background
                Color.primary.opacity(0.05)
                    .ignoresSafeArea()
                
                if isLoading {
                    ProgressView()
                } else {
                    mainContent
                }
            }
            .navigationTitle("Activités")
            #if os(iOS)
            .navigationBarTitleDisplayMode(.large)
            #endif
            .toolbar {
                #if os(iOS)
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(String(localized: "common.close")) { dismiss() }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    HStack(spacing: 12) {
                        CommentButton(commentCount: commentCount) {
                            showComments = true
                        }
                        
                        Button {
                            selectedActivity = nil
                            showAddActivitySheet = true
                        } label: {
                            Image(systemName: "plus.circle.fill")
                                .font(.title2)
                        }
                    }
                }
                #endif
            }
            .sheet(isPresented: $showAddActivitySheet) {
                ActivityFormSheet(
                    eventId: eventId,
                    activity: selectedActivity,
                    participants: participants,
                    onSave: { activity in
                        if let index = activities.firstIndex(where: { $0.id == activity.id }) {
                            activities[index] = activity
                        } else {
                            activities.append(activity)
                        }
                        showAddActivitySheet = false
                        selectedActivity = nil
                        sortActivities()
                    }
                )
            }
            .sheet(item: $activityForParticipants) { activity in
                ManageParticipantsSheet(
                    activity: activity,
                    allParticipants: participants,
                    onUpdate: { updatedActivity in
                        if let index = activities.firstIndex(where: { $0.id == updatedActivity.id }) {
                            activities[index] = updatedActivity
                        }
                        activityForParticipants = nil
                    }
                )
            }
            .sheet(isPresented: $showComments) {
                NavigationView {
                    // TODO: Re-enable CommentsView when Shared types are properly integrated
                    Text("Comments - Coming Soon")
                        .font(.title2)
                        .foregroundColor(.secondary)
                }
            }
            .alert(String(localized: "activity.delete_title"), isPresented: $showDeleteAlert, presenting: activityToDelete) { activity in
                Button(String(localized: "common.delete"), role: .destructive) {
                    activities.removeAll { $0.id == activity.id }
                    activityToDelete = nil
                }
                Button(String(localized: "common.cancel"), role: .cancel) {
                    activityToDelete = nil
                }
            } message: { activity in
                Text("Voulez-vous vraiment supprimer « \(activity.name) » ?")
            }
            .onAppear {
                loadData()
                loadCommentCount()
            }
        }
    }
    
    @ViewBuilder
    private var mainContent: some View {
        ScrollView {
            VStack(spacing: 16) {
                // Summary Card
                summaryCard
                
                // Create Activity Button
                createActivityButton
                
                // Date Filter
                if !uniqueDates.isEmpty {
                    dateFilterRow
                }
                
                // Activities List
                if filteredActivities.isEmpty {
                    emptyStateCard
                } else {
                    ForEach(activitiesByDate, id: \.date) { dateGroup in
                        dateSection(date: dateGroup.date, activities: dateGroup.activities)
                    }
                }
            }
            .padding()
        }
    }
    
    // MARK: - Summary Card
    
    @ViewBuilder
    private var summaryCard: some View {
        LiquidGlassCard(cornerRadius: 16, padding: 16) {
            HStack(spacing: 40) {
                // Activities Count
                VStack(spacing: 4) {
                    Text("\(activities.count)")
                        .font(.system(size: 32, weight: .bold))
                        .foregroundColor(.wakevePrimary)
                    Text("Activités")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                
                LiquidGlassDivider(style: .default, orientation: .vertical)
                    .frame(height: 48)
                
                // Total Cost
                VStack(spacing: 4) {
                    Text("\(totalCost / 100)€")
                        .font(.system(size: 32, weight: .bold))
                        .foregroundColor(.wakeveSuccess)
                    Text("Coût total")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            }
            .frame(maxWidth: .infinity)
        }
        .accessibilityElement(children: .combine)
        .accessibilityLabel("\(activities.count) activités, \(totalCost / 100) euros de coût total")
    }
    
    // MARK: - Create Activity Button
    
    @ViewBuilder
    private var createActivityButton: some View {
        LiquidGlassButton(
            title: String(localized: "activity.create"),
            style: .primary,
            size: .medium
        ) {
            selectedActivity = nil
            showAddActivitySheet = true
        }
        .accessibilityLabel(String(localized: "activity.create_new"))
        .accessibilityHint(String(localized: "activity.create_new_hint"))
    }
    
    // MARK: - Date Filter Row
    
    @ViewBuilder
    private var dateFilterRow: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                // All dates chip
                dateFilterChip(
                    title: "Toutes",
                    isSelected: selectedDate == nil
                ) {
                    selectedDate = nil
                }
                
                ForEach(uniqueDates, id: \.self) { date in
                    dateFilterChip(
                        title: formatDateShort(date),
                        isSelected: Calendar.current.isDate(date, inSameDayAs: selectedDate ?? Date.distantPast)
                    ) {
                        selectedDate = date
                    }
                }
            }
        }
    }
    
    @ViewBuilder
    private func dateFilterChip(title: String, isSelected: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(title)
                .font(.caption)
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(
                    isSelected
                        ? Color.wakevePrimary.opacity(0.2)
                        : Color.gray.opacity(0.1)
                )
                .foregroundColor(
                    isSelected ? .wakevePrimary : .primary
                )
                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
        }
        .buttonStyle(PlainButtonStyle())
        .accessibilityLabel(isSelected ? "\(title) (sélectionné)" : title)
    }
    
    // MARK: - Date Section
    
    @ViewBuilder
    private func dateSection(date: Date, activities: [ActivityModel]) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            // Date Header with Badge
            HStack {
                Text(formatDate(date))
                    .font(.headline)
                    .foregroundColor(.primary)
                
                Spacer()
                
                LiquidGlassBadge(
                    text: "\(activities.count)",
                    style: .success
                )
            }
            
            // Activities Cards
            ForEach(activities) { activity in
                activityCard(activity: activity)
                
                if activity.id != activities.last?.id {
                    LiquidGlassDivider(style: .subtle)
                }
            }
        }
    }
    
    // MARK: - Activity Card
    
    @ViewBuilder
    private func activityCard(activity: ActivityModel) -> some View {
        LiquidGlassCard(cornerRadius: 12, padding: 12) {
            VStack(alignment: .leading, spacing: 8) {
                // Activity Header
                HStack(alignment: .top) {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(activity.name)
                            .font(.body)
                            .fontWeight(.medium)
                            .foregroundColor(.primary)
                        
                        if !activity.description.isEmpty {
                            Text(activity.description)
                                .font(.caption)
                                .foregroundColor(.secondary)
                                .lineLimit(2)
                        }
                    }
                    
                    Spacer()
                    
                    // Full indicator
                    if activity.isFull {
                        LiquidGlassBadge(
                            text: "Complet",
                            style: .warning
                        )
                    }
                }
                
                LiquidGlassDivider(style: .subtle)
                
                // Activity Details
                VStack(alignment: .leading, spacing: 8) {
                    // Time, Duration, Location
                    HStack(spacing: 16) {
                        if let time = activity.time {
                            detailItem(
                                icon: "clock",
                                text: "\(time) (\(activity.durationMinutes)min)"
                            )
                        }
                        
                        if !activity.location.isEmpty {
                            detailItem(
                                icon: "mappin.circle",
                                text: activity.location
                            )
                        }
                        
                        if activity.costPerPerson > 0 {
                            detailItem(
                                icon: "creditcard",
                                text: "\(activity.costPerPerson / 100)€/pers",
                                color: .wakeveSuccess
                            )
                        }
                    }
                    
                    // Participants and Actions
                    HStack(spacing: 12) {
                        // Participants Button
                        Button {
                            activityForParticipants = activity
                        } label: {
                            HStack(spacing: 4) {
                                Image(systemName: "person.2.fill")
                                    .font(.caption2)
                                
                                if let maxP = activity.maxParticipants {
                                    Text("\(activity.registeredCount)/\(maxP)")
                                        .font(.caption2)
                                } else {
                                    Text("\(activity.registeredCount)")
                                        .font(.caption2)
                                }
                            }
                            .padding(.horizontal, 8)
                            .padding(.vertical, 4)
                            .background(Color.wakevePrimary.opacity(0.15))
                            .foregroundColor(.wakevePrimary)
                            .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
                        }
                        .buttonStyle(PlainButtonStyle())
                        .accessibilityLabel("Gérer les participants")
                        
                        Spacer()
                        
                        // Action Buttons
                        HStack(spacing: 8) {
                            // Edit Button
                            Button {
                                selectedActivity = activity
                                showAddActivitySheet = true
                            } label: {
                                HStack(spacing: 4) {
                                    Image(systemName: "pencil")
                                        .font(.caption2)
                                    Text(String(localized: "common.edit"))
                                        .font(.caption2)
                                }
                                .padding(.horizontal, 8)
                                .padding(.vertical, 4)
                                .background(Color.gray.opacity(0.1))
                                .foregroundColor(.primary)
                                .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
                            }
                            .buttonStyle(PlainButtonStyle())
                            .accessibilityLabel("Modifier l'activité")
                            
                            // Delete Button
                            Button {
                                activityToDelete = activity
                                showDeleteAlert = true
                            } label: {
                                HStack(spacing: 4) {
                                    Image(systemName: "trash")
                                        .font(.caption2)
                                    Text(String(localized: "common.delete"))
                                        .font(.caption2)
                                }
                                .padding(.horizontal, 8)
                                .padding(.vertical, 4)
                                .background(Color.wakeveError.opacity(0.1))
                                .foregroundColor(.wakeveError)
                                .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
                            }
                            .buttonStyle(PlainButtonStyle())
                            .accessibilityLabel("Supprimer l'activité")
                        }
                    }
                }
            }
        }
        .accessibilityElement(children: .contain)
        .accessibilityLabel("\(activity.name), \(activity.registeredCount) participants")
    }
    
    @ViewBuilder
    private func detailItem(icon: String, text: String, color: Color = .secondary) -> some View {
        HStack(spacing: 4) {
            Image(systemName: icon)
                .font(.caption)
                .foregroundColor(color)
            Text(text)
                .font(.caption)
                .foregroundColor(color)
        }
    }
    
    // MARK: - Empty State Card
    
    @ViewBuilder
    private var emptyStateCard: some View {
        LiquidGlassCard(cornerRadius: 16, padding: 32) {
            VStack(spacing: 16) {
                Image(systemName: "calendar.badge.plus")
                    .font(.system(size: 64))
                    .foregroundColor(.wakevePrimary.opacity(0.6))
                
                Text(activities.isEmpty ? "Aucune activité planifiée" : "Aucune activité à cette date")
                    .font(.body)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
                
                if activities.isEmpty {
                    LiquidGlassButton(
                        title: String(localized: "activity.create_first"),
                        style: .primary,
                        size: .medium
                    ) {
                        selectedActivity = nil
                        showAddActivitySheet = true
                    }
                    .padding(.top, 8)
                }
            }
            .frame(maxWidth: .infinity)
        }
        .accessibilityLabel(activities.isEmpty ? "Aucune activité" : "Aucune activité pour cette date")
    }
    
    // MARK: - Helper Methods
    
    private func loadData() {
        // TODO: Load from repository
        isLoading = true
        // Simulate loading
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
            isLoading = false
        }
    }
    
    // MARK: - Comments
    
    private func loadCommentCount() {
        // TODO: Integrate with CommentRepository
        // For now, placeholder - should fetch count for section .ACTIVITY and sectionItemId = nil
        commentCount = 0
    }
    
    private func sortActivities() {
        activities.sort { a1, a2 in
            if a1.date != a2.date {
                return a1.date < a2.date
            }
            return (a1.time ?? "") < (a2.time ?? "")
        }
    }
    
    private func formatDate(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "d MMM"
        formatter.locale = Locale(identifier: "fr_FR")
        return formatter.string(from: date)
    }
    
    private func formatDateShort(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "d MMM"
        formatter.locale = Locale(identifier: "fr_FR")
        return formatter.string(from: date)
    }
}

// MARK: - Preview

struct ActivityPlanningView_Previews: PreviewProvider {
    static var previews: some View {
        ActivityPlanningView(
            eventId: "event-1",
            currentUserId: "user-1",
            currentUserName: "Test User"
        )
    }
}
