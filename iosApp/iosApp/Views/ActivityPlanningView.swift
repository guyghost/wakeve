import SwiftUI

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
            .navigationTitle(activity == nil ? "Ajouter" : "Modifier")
            #if os(iOS)
            .navigationBarTitleDisplayMode(.inline)
            #endif
            .toolbar {
                #if os(iOS)
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Annuler") { dismiss() }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(activity == nil ? "Ajouter" : "Modifier") {
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
                            Text("Complet")
                                .font(.caption)
                                .padding(.horizontal, 8)
                                .padding(.vertical, 4)
                                .background(Color.red.opacity(0.2))
                                .foregroundColor(.red)
                                .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
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
                                    .foregroundColor(.blue)
                            } else {
                                Image(systemName: "circle")
                                    .foregroundColor(.gray)
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
                    Button("Fermer") {
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
 * Activity Planning View (iOS)
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
                    Button("Fermer") { dismiss() }
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
                    CommentsView(
                        eventId: eventId,
                        section: .ACTIVITY,
                        sectionItemId: nil,
                        currentUserId: currentUserId,
                        currentUserName: currentUserName
                    )
                }
            }
            .alert("Supprimer l'activité", isPresented: $showDeleteAlert, presenting: activityToDelete) { activity in
                Button("Supprimer", role: .destructive) {
                    activities.removeAll { $0.id == activity.id }
                    activityToDelete = nil
                }
                Button("Annuler", role: .cancel) {
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
    
    @ViewBuilder
    private var summaryCard: some View {
        LiquidGlassCard(cornerRadius: 16, padding: 16) {
            HStack(spacing: 40) {
                VStack {
                    Text("\(activities.count)")
                        .font(.system(size: 32, weight: .bold))
                    Text("Activités")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                
                Divider()
                    .frame(height: 48)
                
                VStack {
                    Text("\(totalCost / 100)€")
                        .font(.system(size: 32, weight: .bold))
                        .foregroundColor(.blue)
                    Text("Coût total")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            }
            .frame(maxWidth: .infinity)
        }
    }
    
    @ViewBuilder
    private var dateFilterRow: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                // All dates chip
                Button {
                    selectedDate = nil
                } label: {
                    Text("Toutes")
                        .font(.caption)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 6)
                        .background(
                            selectedDate == nil
                                ? Color.blue.opacity(0.2)
                                : Color.gray.opacity(0.2)
                        )
                        .foregroundColor(
                            selectedDate == nil ? .blue : .primary
                        )
                        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                }
                
                ForEach(uniqueDates, id: \.self) { date in
                    Button {
                        selectedDate = date
                    } label: {
                        Text(formatDateShort(date))
                            .font(.caption)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 6)
                            .background(
                                Calendar.current.isDate(date, inSameDayAs: selectedDate ?? Date.distantPast)
                                    ? Color.blue.opacity(0.2)
                                    : Color.gray.opacity(0.15)
                            )
                            .foregroundColor(
                                Calendar.current.isDate(date, inSameDayAs: selectedDate ?? Date.distantPast)
                                    ? .blue : .primary
                            )
                            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                    }
                }
            }
        }
    }
    
    @ViewBuilder
    private func dateSection(date: Date, activities: [ActivityModel]) -> some View {
        LiquidGlassCard(cornerRadius: 16, padding: 16) {
            VStack(alignment: .leading, spacing: 12) {
                HStack {
                    Text(formatDate(date))
                        .font(.headline)
                    
                    Spacer()
                    
                    Text("\(activities.count)")
                        .font(.caption)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 4)
                        .background(Color.blue.opacity(0.2))
                        .foregroundColor(.blue)
                        .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
                }
                
                ForEach(activities) { activity in
                    activityRow(activity: activity)
                    
                    if activity.id != activities.last?.id {
                        Divider()
                    }
                }
            }
        }
    }
    
    @ViewBuilder
    private func activityRow(activity: ActivityModel) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(activity.name)
                .font(.body)
                .fontWeight(.medium)
            
            if !activity.description.isEmpty {
                Text(activity.description)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            
            HStack(spacing: 12) {
                // Time and Duration
                if let time = activity.time {
                    HStack(spacing: 4) {
                        Image(systemName: "clock")
                            .font(.caption)
                            .foregroundColor(.secondary)
                        Text("\(time) (\(activity.durationMinutes)min)")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                }
                
                // Location
                if !activity.location.isEmpty {
                    HStack(spacing: 4) {
                        Image(systemName: "mappin.circle")
                            .font(.caption)
                            .foregroundColor(.secondary)
                        Text(activity.location)
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                }
                
                // Cost
                if activity.costPerPerson > 0 {
                    Text("\(activity.costPerPerson / 100)€/pers")
                        .font(.caption)
                        .foregroundColor(.blue)
                }
            }
            
            // Participants
            Button {
                activityForParticipants = activity
            } label: {
                HStack(spacing: 4) {
                    Image(systemName: "person.2.fill")
                        .font(.caption2)
                    
                    if let maxP = activity.maxParticipants {
                        Text("\(activity.registeredCount) / \(maxP) inscrits")
                            .font(.caption2)
                    } else {
                        Text("\(activity.registeredCount) inscrits")
                            .font(.caption2)
                    }
                }
                .padding(.horizontal, 8)
                .padding(.vertical, 4)
                .background(Color.blue.opacity(0.2))
                .foregroundColor(.blue)
                .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
            }
            
            // Full indicator
            if activity.isFull {
                HStack(spacing: 4) {
                    Image(systemName: "exclamationmark.triangle.fill")
                        .font(.caption2)
                    Text("Complet")
                        .font(.caption2)
                }
                .padding(.horizontal, 8)
                .padding(.vertical, 4)
                .background(Color.red.opacity(0.2))
                .foregroundColor(.red)
                .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
            }
            
            // Action Buttons
            HStack(spacing: 8) {
                Button {
                    selectedActivity = activity
                    showAddActivitySheet = true
                } label: {
                    HStack(spacing: 4) {
                        Image(systemName: "pencil")
                            .font(.caption2)
                        Text("Modifier")
                            .font(.caption2)
                    }
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(Color.gray.opacity(0.15))
                    .foregroundColor(.primary)
                    .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
                }
                
                Button {
                    activityToDelete = activity
                    showDeleteAlert = true
                } label: {
                    HStack(spacing: 4) {
                        Image(systemName: "trash")
                            .font(.caption2)
                        Text("Supprimer")
                            .font(.caption2)
                    }
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(Color.red.opacity(0.1))
                    .foregroundColor(.red)
                    .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
                }
            }
        }
    }
    
    @ViewBuilder
    private var emptyStateCard: some View {
        LiquidGlassCard(cornerRadius: 16, padding: 32) {
            VStack(spacing: 16) {
                Image(systemName: "calendar.badge.plus")
                    .font(.system(size: 64))
                    .foregroundColor(.secondary)
                
                Text(activities.isEmpty ? "Aucune activité planifiée" : "Aucune activité à cette date")
                    .font(.body)
                    .foregroundColor(.secondary)
            }
            .frame(maxWidth: .infinity)
        }
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
