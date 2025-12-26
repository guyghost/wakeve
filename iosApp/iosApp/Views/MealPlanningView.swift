import SwiftUI

/**
 * Meal Planning View (iOS)
 * 
 * Features:
 * - List of meals grouped by date (daily schedule)
 * - Add/Edit/Delete meals with full form
 * - Auto-generate meal plan for date range
 * - Dietary restrictions management
 * - Liquid Glass design system
 * - Native iOS interactions (sheets, alerts, pickers)
 */
struct MealPlanningView: View {
    let eventId: String
    @Environment(\.dismiss) private var dismiss
    
    @State private var meals: [MealModel] = []
    @State private var participants: [ParticipantModel] = []
    @State private var dietaryRestrictions: [DietaryRestrictionModel] = []
    @State private var isLoading = false
    
    // Filters
    @State private var selectedTypeFilter: MealTypeFilter = .all
    @State private var selectedStatusFilter: MealStatusFilter = .all
    
    // Sheets
    @State private var showAddMealSheet = false
    @State private var showAutoGenerateSheet = false
    @State private var showRestrictionsSheet = false
    @State private var selectedMeal: MealModel?
    
    // Alerts
    @State private var showDeleteAlert = false
    @State private var mealToDelete: MealModel?
    
    var body: some View {
        NavigationView {
            ZStack {
                // Background gradient
                LinearGradient(
                    colors: [Color(.systemBackground), Color(.systemGray6)],
                    startPoint: .top,
                    endPoint: .bottom
                )
                .ignoresSafeArea()
                
                if isLoading {
                    ProgressView()
                } else {
                    mainContent
                }
            }
            .navigationTitle("Repas")
            .navigationBarTitleDisplayMode(.large)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Fermer") { dismiss() }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Menu {
                        Button {
                            selectedMeal = nil
                            showAddMealSheet = true
                        } label: {
                            Label("Ajouter un repas", systemImage: "plus.circle")
                        }
                        
                        Button {
                            showAutoGenerateSheet = true
                        } label: {
                            Label("Générer automatiquement", systemImage: "wand.and.stars")
                        }
                        
                        Button {
                            showRestrictionsSheet = true
                        } label: {
                            Label("Contraintes alimentaires", systemImage: "leaf.circle")
                        }
                    } label: {
                        Image(systemName: "ellipsis.circle.fill")
                            .font(.title2)
                    }
                }
            }
            .sheet(isPresented: $showAddMealSheet) {
                MealFormSheet(
                    eventId: eventId,
                    meal: selectedMeal,
                    participants: participants,
                    onSave: { meal in
                        if let index = meals.firstIndex(where: { $0.id == meal.id }) {
                            meals[index] = meal
                        } else {
                            meals.append(meal)
                        }
                        showAddMealSheet = false
                        selectedMeal = nil
                        sortMeals()
                    }
                )
            }
            .sheet(isPresented: $showAutoGenerateSheet) {
                AutoGenerateMealsSheet(
                    eventId: eventId,
                    participantCount: participants.count,
                    onGenerate: { generatedMeals in
                        meals.append(contentsOf: generatedMeals)
                        showAutoGenerateSheet = false
                        sortMeals()
                    }
                )
            }
            .sheet(isPresented: $showRestrictionsSheet) {
                DietaryRestrictionsSheet(
                    eventId: eventId,
                    participants: participants,
                    restrictions: $dietaryRestrictions
                )
            }
            .alert("Supprimer ce repas ?", isPresented: $showDeleteAlert, presenting: mealToDelete) { meal in
                Button("Annuler", role: .cancel) {
                    mealToDelete = nil
                }
                Button("Supprimer", role: .destructive) {
                    meals.removeAll { $0.id == meal.id }
                    mealToDelete = nil
                }
            } message: { _ in
                Text("Cette action est irréversible.")
            }
        }
        .onAppear {
            loadData()
        }
    }
    
    private var mainContent: some View {
        ScrollView {
            VStack(spacing: 20) {
                // Summary card
                summaryCard
                
                // Filters
                filterChips
                
                // Meals grouped by date
                if filteredMeals.isEmpty {
                    emptyStateView
                } else {
                    mealsListByDate
                }
            }
            .padding()
        }
    }
    
    // MARK: - Summary Card
    
    private var summaryCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Résumé")
                .font(.headline)
                .foregroundColor(.primary)
            
            HStack(spacing: 24) {
                summaryItem(
                    icon: "fork.knife.circle.fill",
                    value: "\(meals.count)",
                    label: "Repas",
                    color: .blue
                )
                
                summaryItem(
                    icon: "eurosign.circle.fill",
                    value: totalCost,
                    label: "Coût estimé",
                    color: .green
                )
                
                summaryItem(
                    icon: "checkmark.circle.fill",
                    value: "\(completedCount)",
                    label: "Terminés",
                    color: .purple
                )
            }
        }
        .padding()
        .glassCard()
    }
    
    private func summaryItem(icon: String, value: String, label: String, color: Color) -> some View {
        VStack(spacing: 4) {
            Image(systemName: icon)
                .font(.title2)
                .foregroundColor(color)
            
            Text(value)
                .font(.title3)
                .fontWeight(.bold)
            
            Text(label)
                .font(.caption)
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity)
    }
    
    private var totalCost: String {
        let total = meals.reduce(0) { $0 + $1.estimatedCost }
        return String(format: "%.2f€", Double(total) / 100.0)
    }
    
    private var completedCount: Int {
        meals.filter { $0.status == "COMPLETED" }.count
    }
    
    // MARK: - Filters
    
    private var filterChips: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Filtres")
                .font(.subheadline)
                .fontWeight(.medium)
                .foregroundColor(.secondary)
            
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 12) {
                    // Type filter
                    Menu {
                        ForEach(MealTypeFilter.allCases, id: \.self) { filter in
                            Button {
                                selectedTypeFilter = filter
                            } label: {
                                HStack {
                                    Text(filter.rawValue)
                                    if selectedTypeFilter == filter {
                                        Image(systemName: "checkmark")
                                    }
                                }
                            }
                        }
                    } label: {
                        FilterChip(
                            text: selectedTypeFilter.rawValue,
                            icon: "fork.knife",
                            isSelected: selectedTypeFilter != .all
                        )
                    }
                    
                    // Status filter
                    Menu {
                        ForEach(MealStatusFilter.allCases, id: \.self) { filter in
                            Button {
                                selectedStatusFilter = filter
                            } label: {
                                HStack {
                                    Text(filter.rawValue)
                                    if selectedStatusFilter == filter {
                                        Image(systemName: "checkmark")
                                    }
                                }
                            }
                        }
                    } label: {
                        FilterChip(
                            text: selectedStatusFilter.rawValue,
                            icon: "flag",
                            isSelected: selectedStatusFilter != .all
                        )
                    }
                }
            }
        }
    }
    
    // MARK: - Meals List by Date
    
    private var mealsListByDate: some View {
        VStack(alignment: .leading, spacing: 20) {
            ForEach(groupedMealsByDate, id: \.date) { group in
                VStack(alignment: .leading, spacing: 12) {
                    // Date header
                    Text(formatDate(group.date))
                        .font(.title3)
                        .fontWeight(.bold)
                        .foregroundColor(.primary)
                        .padding(.horizontal, 4)
                    
                    // Meals for this date
                    ForEach(group.meals, id: \.id) { meal in
                        MealCard(
                            meal: meal,
                            participants: participants,
                            onEdit: {
                                selectedMeal = meal
                                showAddMealSheet = true
                            },
                            onDelete: {
                                mealToDelete = meal
                                showDeleteAlert = true
                            }
                        )
                    }
                }
            }
        }
    }
    
    // MARK: - Empty State
    
    private var emptyStateView: some View {
        VStack(spacing: 24) {
            Image(systemName: "fork.knife.circle")
                .font(.system(size: 64))
                .foregroundColor(.blue)
            
            Text("Aucun repas planifié")
                .font(.title2)
                .fontWeight(.bold)
            
            Text("Ajoutez des repas manuellement ou générez-les automatiquement pour votre événement.")
                .font(.body)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal)
            
            HStack(spacing: 16) {
                Button {
                    showAddMealSheet = true
                } label: {
                    Label("Ajouter", systemImage: "plus")
                        .font(.headline)
                        .foregroundColor(.white)
                        .padding(.horizontal, 24)
                        .padding(.vertical, 12)
                        .background(Color.blue)
                        .continuousCornerRadius(12)
                }
                
                Button {
                    showAutoGenerateSheet = true
                } label: {
                    Label("Auto-générer", systemImage: "wand.and.stars")
                        .font(.headline)
                        .foregroundColor(.blue)
                        .padding(.horizontal, 24)
                        .padding(.vertical, 12)
                        .background(Color.blue.opacity(0.1))
                        .continuousCornerRadius(12)
                }
            }
        }
        .padding(40)
        .glassCard()
    }
    
    // MARK: - Helper Methods
    
    private var filteredMeals: [MealModel] {
        meals.filter { meal in
            let typeMatch = selectedTypeFilter == .all || meal.type == selectedTypeFilter.rawValue
            let statusMatch = selectedStatusFilter == .all || meal.status == selectedStatusFilter.rawValue
            return typeMatch && statusMatch
        }
    }
    
    private var groupedMealsByDate: [(date: String, meals: [MealModel])] {
        let grouped = Dictionary(grouping: filteredMeals) { $0.date }
        return grouped.map { (date: $0.key, meals: $0.value.sorted { $0.time < $1.time }) }
            .sorted { $0.date < $1.date }
    }
    
    private func sortMeals() {
        meals.sort { meal1, meal2 in
            if meal1.date == meal2.date {
                return meal1.time < meal2.time
            }
            return meal1.date < meal2.date
        }
    }
    
    private func formatDate(_ dateString: String) -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        guard let date = formatter.date(from: dateString) else {
            return dateString
        }
        
        formatter.dateFormat = "EEEE d MMMM"
        formatter.locale = Locale(identifier: "fr_FR")
        var result = formatter.string(from: date)
        result = result.prefix(1).uppercased() + result.dropFirst()
        return result
    }
    
    private func loadData() {
        isLoading = true
        // TODO: Load from repository
        // For now, use mock data
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
            isLoading = false
        }
    }
}

// MARK: - Meal Card

struct MealCard: View {
    let meal: MealModel
    let participants: [ParticipantModel]
    let onEdit: () -> Void
    let onDelete: () -> Void
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            // Header with type icon and time
            HStack {
                Image(systemName: mealTypeIcon(meal.type))
                    .font(.title3)
                    .foregroundColor(mealTypeColor(meal.type))
                
                Text(meal.name)
                    .font(.headline)
                
                Spacer()
                
                Text(meal.time)
                    .font(.subheadline)
                    .foregroundColor(.secondary)
            }
            
            // Status badge
            HStack {
                StatusBadge(status: meal.status)
                Spacer()
            }
            
            // Location (if set)
            if let location = meal.location, !location.isEmpty {
                Label(location, systemImage: "mappin.circle.fill")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
            }
            
            // Cost and servings
            HStack(spacing: 16) {
                Label(
                    String(format: "%.2f€", Double(meal.estimatedCost) / 100.0),
                    systemImage: "eurosign.circle"
                )
                .font(.subheadline)
                .foregroundColor(.green)
                
                Label("\(meal.servings) pers.", systemImage: "person.2.fill")
                    .font(.subheadline)
                    .foregroundColor(.blue)
            }
            
            // Responsible participants
            if !meal.responsibleParticipantIds.isEmpty {
                HStack(spacing: 8) {
                    Image(systemName: "person.fill.checkmark")
                        .font(.caption)
                        .foregroundColor(.purple)
                    
                    Text(responsibleNames)
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            }
            
            // Actions
            HStack(spacing: 12) {
                Button {
                    onEdit()
                } label: {
                    Label("Modifier", systemImage: "pencil")
                        .font(.subheadline)
                        .foregroundColor(.blue)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 8)
                        .background(Color.blue.opacity(0.1))
                        .continuousCornerRadius(8)
                }
                
                Button {
                    onDelete()
                } label: {
                    Label("Supprimer", systemImage: "trash")
                        .font(.subheadline)
                        .foregroundColor(.red)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 8)
                        .background(Color.red.opacity(0.1))
                        .continuousCornerRadius(8)
                }
                
                Spacer()
            }
        }
        .padding()
        .glassCard()
    }
    
    private var responsibleNames: String {
        let names = meal.responsibleParticipantIds.compactMap { id in
            participants.first(where: { $0.id == id })?.name
        }
        return names.joined(separator: ", ")
    }
    
    private func mealTypeIcon(_ type: String) -> String {
        switch type {
        case "BREAKFAST": return "cup.and.saucer.fill"
        case "LUNCH": return "fork.knife"
        case "DINNER": return "moon.stars.fill"
        case "SNACK": return "birthday.cake.fill"
        case "APERITIF": return "wineglass.fill"
        default: return "fork.knife"
        }
    }
    
    private func mealTypeColor(_ type: String) -> Color {
        switch type {
        case "BREAKFAST": return .orange
        case "LUNCH": return .blue
        case "DINNER": return .purple
        case "SNACK": return .pink
        case "APERITIF": return .red
        default: return .gray
        }
    }
}

// MARK: - Filter Enums

enum MealTypeFilter: String, CaseIterable {
    case all = "Tous"
    case breakfast = "BREAKFAST"
    case lunch = "LUNCH"
    case dinner = "DINNER"
    case snack = "SNACK"
    case aperitif = "APERITIF"
}

enum MealStatusFilter: String, CaseIterable {
    case all = "Tous"
    case planned = "PLANNED"
    case assigned = "ASSIGNED"
    case inProgress = "IN_PROGRESS"
    case completed = "COMPLETED"
    case cancelled = "CANCELLED"
}

// MARK: - Models

struct MealModel {
    let id: String
    let eventId: String
    let type: String
    let name: String
    let date: String
    let time: String
    let location: String?
    let responsibleParticipantIds: [String]
    let estimatedCost: Int64
    let actualCost: Int64?
    let servings: Int
    let status: String
    let notes: String?
    let createdAt: String
    let updatedAt: String
}

struct DietaryRestrictionModel {
    let id: String
    let participantId: String
    let eventId: String
    let restriction: String
    let notes: String?
    let createdAt: String
}

// MARK: - Preview

#Preview {
    MealPlanningView(eventId: "event-1")
}
