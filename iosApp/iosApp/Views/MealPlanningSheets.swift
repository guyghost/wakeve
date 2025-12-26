import SwiftUI

// MARK: - Meal Form Sheet

struct MealFormSheet: View {
    let eventId: String
    let meal: MealModel?
    let participants: [ParticipantModel]
    let onSave: (MealModel) -> Void
    
    @Environment(\.dismiss) private var dismiss
    
    @State private var name: String = ""
    @State private var selectedType: String = "BREAKFAST"
    @State private var selectedStatus: String = "PLANNED"
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
        NavigationView {
            Form {
                // Basic info section
                Section("Informations") {
                    TextField("Nom du repas", text: $name)
                    
                    Picker("Type", selection: $selectedType) {
                        Text("Petit-déjeuner").tag("BREAKFAST")
                        Text("Déjeuner").tag("LUNCH")
                        Text("Dîner").tag("DINNER")
                        Text("Goûter").tag("SNACK")
                        Text("Apéritif").tag("APERITIF")
                    }
                    
                    Picker("Statut", selection: $selectedStatus) {
                        Text("Planifié").tag("PLANNED")
                        Text("Assigné").tag("ASSIGNED")
                        Text("En cours").tag("IN_PROGRESS")
                        Text("Terminé").tag("COMPLETED")
                        Text("Annulé").tag("CANCELLED")
                    }
                }
                
                // Date and time section
                Section("Date et heure") {
                    DatePicker("Date", selection: $date, displayedComponents: .date)
                    DatePicker("Heure", selection: $time, displayedComponents: .hourAndMinute)
                }
                
                // Location section
                Section("Lieu") {
                    TextField("Lieu (optionnel)", text: $location)
                }
                
                // Cost and servings section
                Section("Coût et portions") {
                    HStack {
                        TextField("Coût estimé", text: $estimatedCost)
                        Text("€")
                            .foregroundColor(.secondary)
                    }
                    
                    HStack {
                        TextField("Nombre de personnes", text: $servings)
                        Text("pers.")
                            .foregroundColor(.secondary)
                    }
                }
                
                // Responsible participants section
                Section("Responsables") {
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
                                        .foregroundColor(.blue)
                                } else {
                                    Image(systemName: "circle")
                                        .foregroundColor(.gray)
                                }
                            }
                        }
                    }
                }
                
                // Notes section
                Section("Notes") {
                    TextEditor(text: $notes)
                        .frame(minHeight: 80)
                }
            }
            .navigationTitle(isEditing ? "Modifier le repas" : "Nouveau repas")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Annuler") {
                        dismiss()
                    }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Enregistrer") {
                        saveMeal()
                    }
                }
            }
            .alert("Erreur de validation", isPresented: $showValidationError) {
                Button("OK", role: .cancel) {}
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
            validationMessage = "Le nom du repas est requis"
            showValidationError = true
            return
        }
        
        guard let cost = Double(estimatedCost.replacingOccurrences(of: ",", with: ".")), cost >= 0 else {
            validationMessage = "Le coût doit être un nombre positif"
            showValidationError = true
            return
        }
        
        guard let servingsInt = Int(servings), servingsInt > 0 else {
            validationMessage = "Le nombre de personnes doit être supérieur à 0"
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
}

// MARK: - Auto Generate Meals Sheet

struct AutoGenerateMealsSheet: View {
    let eventId: String
    let participantCount: Int
    let onGenerate: ([MealModel]) -> Void
    
    @Environment(\.dismiss) private var dismiss
    
    @State private var startDate: Date = Date()
    @State private var endDate: Date = Date().addingTimeInterval(86400 * 3) // 3 days
    @State private var estimatedCostPerMeal: String = "10.00"
    @State private var selectedMealTypes: Set<String> = ["BREAKFAST", "LUNCH", "DINNER"]
    
    @State private var showValidationError = false
    @State private var validationMessage = ""
    
    private let mealTypes = [
        ("BREAKFAST", "Petit-déjeuner", "cup.and.saucer.fill"),
        ("LUNCH", "Déjeuner", "fork.knife"),
        ("DINNER", "Dîner", "moon.stars.fill"),
        ("SNACK", "Goûter", "birthday.cake.fill"),
        ("APERITIF", "Apéritif", "wineglass.fill")
    ]
    
    var body: some View {
        NavigationView {
            Form {
                Section("Période") {
                    DatePicker("Date de début", selection: $startDate, displayedComponents: .date)
                    DatePicker("Date de fin", selection: $endDate, displayedComponents: .date)
                }
                
                Section("Types de repas") {
                    ForEach(mealTypes, id: \.0) { type in
                        Button {
                            toggleMealType(type.0)
                        } label: {
                            HStack {
                                Image(systemName: type.2)
                                    .foregroundColor(.blue)
                                Text(type.1)
                                    .foregroundColor(.primary)
                                Spacer()
                                if selectedMealTypes.contains(type.0) {
                                    Image(systemName: "checkmark.circle.fill")
                                        .foregroundColor(.blue)
                                } else {
                                    Image(systemName: "circle")
                                        .foregroundColor(.gray)
                                }
                            }
                        }
                    }
                }
                
                Section("Budget") {
                    HStack {
                        TextField("Coût estimé par repas", text: $estimatedCostPerMeal)
                        Text("€")
                            .foregroundColor(.secondary)
                    }
                    
                    Text("\(participantCount) participants")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
                
                Section {
                    Text("Un total de \(estimatedMealCount) repas sera généré.")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
            }
            .navigationTitle("Génération automatique")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Annuler") {
                        dismiss()
                    }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Générer") {
                        generateMeals()
                    }
                }
            }
            .alert("Erreur de validation", isPresented: $showValidationError) {
                Button("OK", role: .cancel) {}
            } message: {
                Text(validationMessage)
            }
        }
    }
    
    private var estimatedMealCount: Int {
        let days = Calendar.current.dateComponents([.day], from: startDate, to: endDate).day ?? 0
        return (days + 1) * selectedMealTypes.count
    }
    
    private func toggleMealType(_ type: String) {
        if selectedMealTypes.contains(type) {
            selectedMealTypes.remove(type)
        } else {
            selectedMealTypes.insert(type)
        }
    }
    
    private func generateMeals() {
        // Validate
        if startDate > endDate {
            validationMessage = "La date de début doit être avant la date de fin"
            showValidationError = true
            return
        }
        
        if selectedMealTypes.isEmpty {
            validationMessage = "Sélectionnez au moins un type de repas"
            showValidationError = true
            return
        }
        
        guard let costPerMeal = Double(estimatedCostPerMeal.replacingOccurrences(of: ",", with: ".")), costPerMeal >= 0 else {
            validationMessage = "Le coût doit être un nombre positif"
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
                    status: "PLANNED",
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
    
    private func defaultTime(for type: String) -> String {
        switch type {
        case "BREAKFAST": return "08:00"
        case "LUNCH": return "12:30"
        case "DINNER": return "19:30"
        case "SNACK": return "16:00"
        case "APERITIF": return "18:30"
        default: return "12:00"
        }
    }
    
    private func defaultName(for type: String, date: Date) -> String {
        let dayFormatter = DateFormatter()
        dayFormatter.dateFormat = "EEEE"
        dayFormatter.locale = Locale(identifier: "fr_FR")
        let dayOfWeek = dayFormatter.string(from: date).capitalized
        
        switch type {
        case "BREAKFAST": return "Petit-déjeuner"
        case "LUNCH": return "Déjeuner"
        case "DINNER": return "Dîner du \(dayOfWeek)"
        case "SNACK": return "Goûter"
        case "APERITIF": return "Apéritif"
        default: return "Repas"
        }
    }
}

// MARK: - Dietary Restrictions Sheet

struct DietaryRestrictionsSheet: View {
    let eventId: String
    let participants: [ParticipantModel]
    @Binding var restrictions: [DietaryRestrictionModel]
    
    @Environment(\.dismiss) private var dismiss
    
    @State private var showAddSheet = false
    @State private var showDeleteAlert = false
    @State private var restrictionToDelete: DietaryRestrictionModel?
    
    var body: some View {
        NavigationView {
            ZStack {
                if restrictions.isEmpty {
                    emptyStateView
                } else {
                    restrictionsList
                }
            }
            .navigationTitle("Contraintes alimentaires")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Fermer") {
                        dismiss()
                    }
                }
                ToolbarItem(placement: .primaryAction) {
                    Button {
                        showAddSheet = true
                    } label: {
                        Image(systemName: "plus.circle.fill")
                    }
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
            .alert("Supprimer cette contrainte ?", isPresented: $showDeleteAlert, presenting: restrictionToDelete) { restriction in
                Button("Annuler", role: .cancel) {
                    restrictionToDelete = nil
                }
                Button("Supprimer", role: .destructive) {
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
                                    .foregroundColor(.red)
                            }
                        }
                    }
                } header: {
                    HStack {
                        Image(systemName: "leaf.circle.fill")
                            .foregroundColor(.green)
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
                .foregroundColor(.green)
            
            Text("Aucune contrainte")
                .font(.title2)
                .fontWeight(.bold)
            
            Text("Ajoutez les contraintes alimentaires des participants.")
                .font(.body)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal)
            
            Button {
                showAddSheet = true
            } label: {
                Label("Ajouter", systemImage: "plus")
                    .font(.headline)
                    .foregroundColor(.white)
                    .padding(.horizontal, 24)
                    .padding(.vertical, 12)
                    .background(Color.green)
                    .continuousCornerRadius(12)
            }
        }
        .padding()
    }
    
    private var groupedRestrictions: [(restriction: String, items: [DietaryRestrictionModel])] {
        let grouped = Dictionary(grouping: restrictions) { $0.restriction }
        return grouped.map { (restriction: $0.key, items: $0.value) }
            .sorted { $0.restriction < $1.restriction }
    }
    
    private func formatRestriction(_ restriction: String) -> String {
        switch restriction {
        case "VEGETARIAN": return "Végétarien"
        case "VEGAN": return "Végétalien"
        case "GLUTEN_FREE": return "Sans gluten"
        case "LACTOSE_INTOLERANT": return "Intolérant au lactose"
        case "NUT_ALLERGY": return "Allergie aux noix"
        case "SHELLFISH_ALLERGY": return "Allergie aux fruits de mer"
        case "KOSHER": return "Casher"
        case "HALAL": return "Halal"
        case "DIABETIC": return "Diabétique"
        case "OTHER": return "Autre"
        default: return restriction
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
    @State private var selectedRestriction: String = "VEGETARIAN"
    @State private var notes: String = ""
    
    private let restrictions = [
        ("VEGETARIAN", "Végétarien"),
        ("VEGAN", "Végétalien"),
        ("GLUTEN_FREE", "Sans gluten"),
        ("LACTOSE_INTOLERANT", "Intolérant au lactose"),
        ("NUT_ALLERGY", "Allergie aux noix"),
        ("SHELLFISH_ALLERGY", "Allergie aux fruits de mer"),
        ("KOSHER", "Casher"),
        ("HALAL", "Halal"),
        ("DIABETIC", "Diabétique"),
        ("OTHER", "Autre")
    ]
    
    var body: some View {
        NavigationView {
            Form {
                Section("Participant") {
                    Picker("Sélectionner", selection: $selectedParticipantId) {
                        Text("Choisir un participant").tag("")
                        ForEach(participants, id: \.id) { participant in
                            Text(participant.name).tag(participant.id)
                        }
                    }
                }
                
                Section("Type de contrainte") {
                    Picker("Type", selection: $selectedRestriction) {
                        ForEach(restrictions, id: \.0) { restriction in
                            Text(restriction.1).tag(restriction.0)
                        }
                    }
                }
                
                Section("Notes") {
                    TextEditor(text: $notes)
                        .frame(minHeight: 80)
                }
            }
            .navigationTitle("Ajouter une contrainte")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Annuler") {
                        dismiss()
                    }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Enregistrer") {
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
