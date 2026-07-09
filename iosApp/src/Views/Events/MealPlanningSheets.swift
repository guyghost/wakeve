import SwiftUI

// MARK: - Meal Form Sheet

struct MealFormSheet: View {
    @Environment(\.colorScheme) private var colorScheme
    let eventId: String
    let meal: MealModel?
    let participants: [ParticipantModel]
    let onSave: (MealModel) -> Void
    
    @Environment(\.dismiss) private var dismiss
    
    @State private var name: String = ""
    @State private var selectedType: MealType = .breakfast
    @State private var selectedStatus: MealStatus = .planned
    @State private var date: Date = Date()
    @State private var time: Date = Date()
    @State private var location: String = ""
    @State private var estimatedCost: String = ""
    @State private var servings: String = ""
    @State private var notes: String = ""
    @State private var selectedParticipantIds: Set<String> = []
    
    @State private var showValidationError = false
    @State private var validationMessage = ""
    
    var isEditing: Bool {
        meal != nil
    }
    
    var body: some View {
        NavigationStack {
            Form {
                // Basic info section
                Section(String(localized: "meal.form.info")) {
                    TextField(String(localized: "meal.form.name"), text: $name)
                    
                     Picker(String(localized: "meal.form.type"), selection: $selectedType) {
                         ForEach(MealType.allCases, id: \.self) { type in
                             Text(displayName(for: type)).tag(type)
                         }
                     }
                    
                     Picker(String(localized: "meal.form.status"), selection: $selectedStatus) {
                         ForEach(MealStatus.allCases, id: \.self) { status in
                             Text(displayName(for: status)).tag(status)
                         }
                     }
                }
                
                // Date and time section
                Section(String(localized: "meal.form.date_time")) {
                    DatePicker(String(localized: "meal.form.date"), selection: $date, displayedComponents: .date)
                    DatePicker(String(localized: "meal.form.time"), selection: $time, displayedComponents: .hourAndMinute)
                }
                
                // Location section
                Section(String(localized: "meal.form.location_section")) {
                    TextField(String(localized: "meal.form.location_placeholder"), text: $location)
                }
                
                // Cost and servings section
                Section(String(localized: "meal.form.cost_servings")) {
                    HStack {
                        TextField(String(localized: "meal.form.estimated_cost"), text: $estimatedCost)
                        Text("€")
                            .foregroundColor(.secondary)
                    }
                    
                    HStack {
                        TextField(String(localized: "meal.form.servings"), text: $servings)
                        Text(String(localized: "meal.form.people_abbrev"))
                            .foregroundColor(.secondary)
                    }
                }
                
                // Responsible participants section
                Section(String(localized: "meal.form.responsibles")) {
                    ForEach(participants, id: \.id) { participant in
                        Button {
                            toggleParticipant(participant.id)
                        } label: {
                            HStack {
                                Text(participant.name)
                                    .foregroundColor(.primary)
                                Spacer()
                                if selectedParticipantIds.contains(participant.id) {
                                    Image(systemName: "checkmark.circle.fill")
                                        .foregroundColor(SemanticColor.accent(for: colorScheme))
                                } else {
                                    Image(systemName: "circle")
                                        .foregroundColor(.gray)
                                }
                            }
                        }
                    }
                }
                
                // Notes section
                Section(String(localized: "meal.form.notes")) {
                    TextEditor(text: $notes)
                        .frame(minHeight: 80)
                }
            }
            .navigationTitle(isEditing ? String(localized: "meal.form.edit_title") : String(localized: "meal.form.new_title"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(String(localized: "common.cancel")) {
                        dismiss()
                    }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button(String(localized: "common.save")) {
                        saveMeal()
                    }
                }
            }
            .alert(String(localized: "meal.validation.title"), isPresented: $showValidationError) {
                Button(String(localized: "common.ok"), role: .cancel) {}
            } message: {
                Text(validationMessage)
            }
        }
        .onAppear {
            loadMealData()
        }
    }
    
    private func toggleParticipant(_ id: String) {
        if selectedParticipantIds.contains(id) {
            selectedParticipantIds.remove(id)
        } else {
            selectedParticipantIds.insert(id)
        }
    }
    
    private func loadMealData() {
        guard let meal = meal else { return }
        
        name = meal.name
        selectedType = meal.type
        selectedStatus = meal.status
        location = meal.location ?? ""
        estimatedCost = String(format: "%.2f", Double(meal.estimatedCost) / 100.0)
        servings = String(meal.servings)
        notes = meal.notes ?? ""
        selectedParticipantIds = Set(meal.responsibleParticipantIds)
        
        // Parse date
        let dateFormatter = DateFormatter()
        dateFormatter.dateFormat = "yyyy-MM-dd"
        if let parsedDate = dateFormatter.date(from: meal.date) {
            date = parsedDate
        }
        
        // Parse time
        let timeFormatter = DateFormatter()
        timeFormatter.dateFormat = "HH:mm"
        if let parsedTime = timeFormatter.date(from: meal.time) {
            time = parsedTime
        }
    }
    
    private func saveMeal() {
        // Validate
        if name.isEmpty {
            validationMessage = String(localized: "meal.validation.name_required")
            showValidationError = true
            return
        }
        
        guard let cost = Double(estimatedCost.replacingOccurrences(of: ",", with: ".")), cost >= 0 else {
            validationMessage = String(localized: "meal.validation.cost_positive")
            showValidationError = true
            return
        }
        
        guard let servingsInt = Int(servings), servingsInt > 0 else {
            validationMessage = String(localized: "meal.validation.servings_positive")
            showValidationError = true
            return
        }
        
        // Format date and time
        let dateFormatter = DateFormatter()
        dateFormatter.dateFormat = "yyyy-MM-dd"
        let dateString = dateFormatter.string(from: date)
        
        let timeFormatter = DateFormatter()
        timeFormatter.dateFormat = "HH:mm"
        let timeString = timeFormatter.string(from: time)
        
        // Create meal
        let timestamp = ISO8601DateFormatter().string(from: Date())
        let costInCents = Int64(cost * 100)
        
        let newMeal = MealModel(
            id: meal?.id ?? UUID().uuidString,
            eventId: eventId,
            type: selectedType,
            name: name,
            date: dateString,
            time: timeString,
            location: location.isEmpty ? nil : location,
            responsibleParticipantIds: Array(selectedParticipantIds),
            estimatedCost: costInCents,
            actualCost: meal?.actualCost,
            servings: servingsInt,
            status: selectedStatus,
            notes: notes.isEmpty ? nil : notes,
            createdAt: meal?.createdAt ?? timestamp,
            updatedAt: timestamp
        )
        
        onSave(newMeal)
    }
    
    private func displayName(for type: MealType) -> String {
        switch type {
        case .breakfast: return String(localized: "meal.type.breakfast")
        case .lunch: return String(localized: "meal.type.lunch")
        case .dinner: return String(localized: "meal.type.dinner")
        case .snack: return String(localized: "meal.type.snack")
        case .aperitif: return String(localized: "meal.type.aperitif")
        }
    }
    
    private func displayName(for status: MealStatus) -> String {
        switch status {
        case .planned: return String(localized: "meal.status.planned")
        case .assigned: return String(localized: "meal.status.assigned")
        case .inProgress: return String(localized: "meal.status.in_progress")
        case .completed: return String(localized: "meal.status.completed")
        case .cancelled: return String(localized: "meal.status.cancelled")
        }
    }
}

// MARK: - Auto Generate Meals Sheet

struct AutoGenerateMealsSheet: View {
    @Environment(\.colorScheme) private var colorScheme
    let eventId: String
    let participantCount: Int
    let onGenerate: ([MealModel]) -> Void
    
    @Environment(\.dismiss) private var dismiss
    
    @State private var startDate: Date = Date()
    @State private var endDate: Date = Date().addingTimeInterval(86400 * 3) // 3 days
    @State private var estimatedCostPerMeal: String = "10.00"
    @State private var selectedMealTypes: Set<MealType> = [.breakfast, .lunch, .dinner]
    
    @State private var showValidationError = false
    @State private var validationMessage = ""
    
    private let mealTypes = [
        (MealType.breakfast, String(localized: "meal.type.breakfast"), "cup.and.saucer.fill"),
        (MealType.lunch, String(localized: "meal.type.lunch"), "fork.knife"),
        (MealType.dinner, String(localized: "meal.type.dinner"), "moon.stars.fill"),
        (MealType.snack, String(localized: "meal.type.snack"), "birthday.cake.fill"),
        (MealType.aperitif, String(localized: "meal.type.aperitif"), "wineglass.fill")
    ]
    
    var body: some View {
        NavigationStack {
            Form {
                Section(String(localized: "meal.auto.period")) {
                    DatePicker(String(localized: "meal.auto.start_date"), selection: $startDate, displayedComponents: .date)
                    DatePicker(String(localized: "meal.auto.end_date"), selection: $endDate, displayedComponents: .date)
                }
                
                Section(String(localized: "meal.auto.meal_types")) {
                     ForEach(mealTypes, id: \.0) { type in
                         Button {
                             toggleMealType(type.0)
                         } label: {
                             HStack {
                                 Image(systemName: type.2)
                                     .foregroundColor(SemanticColor.accent(for: colorScheme))
                                 Text(type.1)
                                     .foregroundColor(.primary)
                                 Spacer()
                                 if selectedMealTypes.contains(type.0) {
                                     Image(systemName: "checkmark.circle.fill")
                                         .foregroundColor(SemanticColor.accent(for: colorScheme))
                                 } else {
                                     Image(systemName: "circle")
                                         .foregroundColor(.gray)
                                 }
                             }
                         }
                     }
                 }
                
                Section(String(localized: "meal.auto.budget")) {
                    HStack {
                        TextField(String(localized: "meal.auto.estimated_cost_per_meal"), text: $estimatedCostPerMeal)
                        Text("€")
                            .foregroundColor(.secondary)
                    }
                    
                    Text(String(format: String(localized: "meal.auto.participant_count_format"), Int64(participantCount)))
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
                
                Section {
                    Text(String(format: String(localized: "meal.auto.estimated_count_format"), Int64(estimatedMealCount)))
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
            }
            .navigationTitle(String(localized: "meal.auto.title"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(String(localized: "common.cancel")) {
                        dismiss()
                    }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button(String(localized: "meal.auto.prepare")) {
                        generateMeals()
                    }
                }
            }
            .alert(String(localized: "meal.validation.title"), isPresented: $showValidationError) {
                Button(String(localized: "common.ok"), role: .cancel) {}
            } message: {
                Text(validationMessage)
            }
        }
    }
    
    private var estimatedMealCount: Int {
        let days = Calendar.current.dateComponents([.day], from: startDate, to: endDate).day ?? 0
        return (days + 1) * selectedMealTypes.count
    }
    
    private func toggleMealType(_ type: MealType) {
        if selectedMealTypes.contains(type) {
            selectedMealTypes.remove(type)
        } else {
            selectedMealTypes.insert(type)
        }
    }
    
    private func generateMeals() {
        // Validate
        if startDate > endDate {
            validationMessage = String(localized: "meal.validation.start_before_end")
            showValidationError = true
            return
        }
        
        if selectedMealTypes.isEmpty {
            validationMessage = String(localized: "meal.validation.select_type")
            showValidationError = true
            return
        }
        
        guard let costPerMeal = Double(estimatedCostPerMeal.replacingOccurrences(of: ",", with: ".")), costPerMeal >= 0 else {
            validationMessage = String(localized: "meal.validation.cost_positive")
            showValidationError = true
            return
        }
        
        // Generate meals
        var meals: [MealModel] = []
        let dateFormatter = DateFormatter()
        dateFormatter.dateFormat = "yyyy-MM-dd"
        
        var currentDate = startDate
        let calendar = Calendar.current
        
        while currentDate <= endDate {
            let dateString = dateFormatter.string(from: currentDate)
            
            for mealType in selectedMealTypes {
                 let time = defaultTime(for: mealType)
                 let name = defaultName(for: mealType, date: currentDate)
                 let timestamp = ISO8601DateFormatter().string(from: Date())
                 let costInCents = Int64(costPerMeal * Double(participantCount) * 100)
                 
                 let meal = MealModel(
                     id: UUID().uuidString,
                     eventId: eventId,
                     type: mealType,
                     name: name,
                     date: dateString,
                     time: time,
                     location: nil,
                     responsibleParticipantIds: [],
                     estimatedCost: costInCents,
                     actualCost: nil,
                     servings: participantCount,
                     status: .planned,
                     notes: nil,
                     createdAt: timestamp,
                     updatedAt: timestamp
                 )
                
                meals.append(meal)
            }
            
            guard let nextDate = calendar.date(byAdding: .day, value: 1, to: currentDate) else {
                break
            }
            currentDate = nextDate
        }
        
        onGenerate(meals)
    }
    
    private func defaultTime(for type: MealType) -> String {
        switch type {
        case .breakfast: return "08:00"
        case .lunch: return "12:30"
        case .dinner: return "19:30"
        case .snack: return "16:00"
        case .aperitif: return "18:30"
        }
    }
    
    private func defaultName(for type: MealType, date: Date) -> String {
        let dayFormatter = DateFormatter()
        dayFormatter.dateFormat = "EEEE"
        dayFormatter.locale = .current
        let dayOfWeek = dayFormatter.string(from: date).capitalized
        
        switch type {
        case .breakfast: return String(localized: "meal.type.breakfast")
        case .lunch: return String(localized: "meal.type.lunch")
        case .dinner: return String(format: String(localized: "meal.default_name.dinner_format"), dayOfWeek)
        case .snack: return String(localized: "meal.type.snack")
        case .aperitif: return String(localized: "meal.type.aperitif")
        }
    }
}

// MARK: - Dietary Restrictions Sheet

struct DietaryRestrictionsSheet: View {
    @Environment(\.colorScheme) private var colorScheme
    let eventId: String
    let participants: [ParticipantModel]
    @Binding var restrictions: [DietaryRestrictionModel]
    
    @Environment(\.dismiss) private var dismiss
    
    @State private var showAddSheet = false
    @State private var showDeleteAlert = false
    @State private var restrictionToDelete: DietaryRestrictionModel?
    
    var body: some View {
        NavigationStack {
            ZStack {
                if restrictions.isEmpty {
                    emptyStateView
                } else {
                    restrictionsList
                }
            }
            .navigationTitle(String(localized: "meal.dietary.title"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(String(localized: "common.close")) {
                        dismiss()
                    }
                }
                ToolbarItem(placement: .primaryAction) {
                    Button {
                        showAddSheet = true
                    } label: {
                        Image(systemName: "plus.circle.fill")
                    }
                    .accessibilityLabel(String(localized: "meal.add"))
                }
            }
            .sheet(isPresented: $showAddSheet) {
                AddDietaryRestrictionSheet(
                    eventId: eventId,
                    participants: participants,
                    onSave: { restriction in
                        restrictions.append(restriction)
                        showAddSheet = false
                    }
                )
            }
            .alert(String(localized: "meal.delete_constraint"), isPresented: $showDeleteAlert, presenting: restrictionToDelete) { restriction in
                Button(String(localized: "common.cancel"), role: .cancel) {
                    restrictionToDelete = nil
                }
                Button(String(localized: "common.delete"), role: .destructive) {
                    restrictions.removeAll { $0.id == restriction.id }
                    restrictionToDelete = nil
                }
            }
        }
    }
    
    private var restrictionsList: some View {
        List {
            ForEach(groupedRestrictions, id: \.restriction) { group in
                Section {
                    ForEach(group.items, id: \.id) { item in
                        HStack {
                            VStack(alignment: .leading, spacing: 4) {
                                if let participant = participants.first(where: { $0.id == item.participantId }) {
                                    Text(participant.name)
                                        .font(.headline)
                                }
                                
                                if let notes = item.notes, !notes.isEmpty {
                                    Text(notes)
                                        .font(.subheadline)
                                        .foregroundColor(.secondary)
                                }
                            }
                            
                            Spacer()
                            
                            Button {
                                restrictionToDelete = item
                                showDeleteAlert = true
                            } label: {
                                Image(systemName: "trash")
                                    .foregroundColor(SemanticColor.destructive(for: colorScheme))
                            }
                            .accessibilityLabel(String(localized: "common.delete"))
                        }
                    }
                } header: {
                    HStack {
                        Image(systemName: "leaf.circle.fill")
                            .foregroundColor(SemanticColor.confirmation(for: colorScheme))
                        Text(formatRestriction(group.restriction))
                        Text("(\(group.items.count))")
                            .foregroundColor(.secondary)
                    }
                }
            }
        }
    }
    
    private var emptyStateView: some View {
        VStack(spacing: 20) {
            Image(systemName: "leaf.circle")
                .font(.system(size: 64))
                .foregroundColor(SemanticColor.confirmation(for: colorScheme))
            
            Text(String(localized: "meal.dietary.empty_title"))
                .font(.title2)
                .fontWeight(.bold)
            
            Text(String(localized: "meal.dietary.empty_body"))
                .font(.body)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal)
            
            Button {
                showAddSheet = true
            } label: {
                Label(String(localized: "meal.dietary.add"), systemImage: "plus")
                    .font(.headline)
                    .foregroundColor(.white)
                    .padding(.horizontal, 24)
                    .padding(.vertical, 12)
                    .background(SemanticColor.confirmation(for: colorScheme))
                    .continuousCornerRadius(12)
            }
        }
        .padding()
    }
    
    private var groupedRestrictions: [(restriction: DietaryRestriction, items: [DietaryRestrictionModel])] {
        let grouped = Dictionary(grouping: restrictions) { $0.restriction }
        return grouped.map { (restriction: $0.key, items: $0.value) }
            .sorted { $0.restriction.rawValue < $1.restriction.rawValue }
    }
    
    private func formatRestriction(_ restriction: DietaryRestriction) -> String {
        switch restriction {
        case .vegetarian: return String(localized: "meal.dietary.vegetarian")
        case .vegan: return String(localized: "meal.dietary.vegan")
        case .glutenFree: return String(localized: "meal.dietary.gluten_free")
        case .lactoseIntolerant: return String(localized: "meal.dietary.lactose_intolerant")
        case .nutAllergy: return String(localized: "meal.dietary.nut_allergy")
        case .shellfishAllergy: return String(localized: "meal.dietary.shellfish_allergy")
        case .kosher: return String(localized: "meal.dietary.kosher")
        case .halal: return String(localized: "meal.dietary.halal")
        case .diabetic: return String(localized: "meal.dietary.diabetic")
        case .other: return String(localized: "meal.dietary.other")
        }
    }
}

// MARK: - Add Dietary Restriction Sheet

struct AddDietaryRestrictionSheet: View {
    let eventId: String
    let participants: [ParticipantModel]
    let onSave: (DietaryRestrictionModel) -> Void
    
    @Environment(\.dismiss) private var dismiss
    
    @State private var selectedParticipantId: String = ""
    @State private var selectedRestriction: DietaryRestriction = .vegetarian
    @State private var notes: String = ""
    
    private let restrictions = [
        (DietaryRestriction.vegetarian, String(localized: "meal.dietary.vegetarian")),
        (DietaryRestriction.vegan, String(localized: "meal.dietary.vegan")),
        (DietaryRestriction.glutenFree, String(localized: "meal.dietary.gluten_free")),
        (DietaryRestriction.lactoseIntolerant, String(localized: "meal.dietary.lactose_intolerant")),
        (DietaryRestriction.nutAllergy, String(localized: "meal.dietary.nut_allergy")),
        (DietaryRestriction.shellfishAllergy, String(localized: "meal.dietary.shellfish_allergy")),
        (DietaryRestriction.kosher, String(localized: "meal.dietary.kosher")),
        (DietaryRestriction.halal, String(localized: "meal.dietary.halal")),
        (DietaryRestriction.diabetic, String(localized: "meal.dietary.diabetic")),
        (DietaryRestriction.other, String(localized: "meal.dietary.other"))
    ]
    
    var body: some View {
        NavigationStack {
            Form {
                Section(String(localized: "meal.dietary.participant")) {
                    Picker(String(localized: "meal.dietary.select"), selection: $selectedParticipantId) {
                        Text(String(localized: "meal.dietary.choose_participant")).tag("")
                        ForEach(participants, id: \.id) { participant in
                            Text(participant.name).tag(participant.id)
                        }
                    }
                }
                
                Section(String(localized: "meal.dietary.constraint_type")) {
                    Picker(String(localized: "meal.form.type"), selection: $selectedRestriction) {
                        ForEach(restrictions, id: \.0) { restriction in
                            Text(restriction.1).tag(restriction.0)
                        }
                    }
                }
                
                Section(String(localized: "meal.form.notes")) {
                    TextEditor(text: $notes)
                        .frame(minHeight: 80)
                }
            }
            .navigationTitle(String(localized: "meal.dietary.add_title"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(String(localized: "common.cancel")) {
                        dismiss()
                    }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button(String(localized: "common.save")) {
                        saveRestriction()
                    }
                    .disabled(selectedParticipantId.isEmpty)
                }
            }
        }
        .onAppear {
            if !participants.isEmpty && selectedParticipantId.isEmpty {
                selectedParticipantId = participants[0].id
            }
        }
    }
    
    private func saveRestriction() {
        let timestamp = ISO8601DateFormatter().string(from: Date())
        
        let restriction = DietaryRestrictionModel(
            id: UUID().uuidString,
            participantId: selectedParticipantId,
            eventId: eventId,
            restriction: selectedRestriction,
            notes: notes.isEmpty ? nil : notes,
            createdAt: timestamp
        )
        
        onSave(restriction)
    }
}

// MARK: - Previews

#if DEBUG
#Preview("Meal Form - Create Light") {
    MealFormSheet(
        eventId: EventFactory.polling.id,
        meal: nil,
        participants: ParticipantModel.samples,
        onSave: { _ in }
    )
    .preferredColorScheme(.light)
}

#Preview("Meal Form - Edit Dark") {
    MealFormSheet(
        eventId: EventFactory.polling.id,
        meal: MealModel.sample,
        participants: ParticipantModel.samples,
        onSave: { _ in }
    )
    .preferredColorScheme(.dark)
}

#Preview("Auto Generate Meals") {
    AutoGenerateMealsSheet(
        eventId: EventFactory.polling.id,
        participantCount: ParticipantModel.samples.count,
        onGenerate: { _ in }
    )
}

#Preview("Dietary Restrictions") {
    DietaryRestrictionsPreviewHarness()
}

private struct DietaryRestrictionsPreviewHarness: View {
    @State private var restrictions = DietaryRestrictionModel.samples

    var body: some View {
        DietaryRestrictionsSheet(
            eventId: EventFactory.polling.id,
            participants: ParticipantModel.samples,
            restrictions: $restrictions
        )
    }
}
#endif
