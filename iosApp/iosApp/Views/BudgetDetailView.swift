import SwiftUI
import Shared

/// Budget Detail View - iOS
///
/// Displays and manages budget items with filtering and CRUD operations.
/// Uses Liquid Glass design system with Material backgrounds.
struct BudgetDetailView: View {
    let budget: Budget_
    let eventId: String
    let repository: BudgetRepository
    let currentUserId: String
    let currentUserName: String
    let onBack: () -> Void
    
    @State private var items: [BudgetItem_] = []
    @State private var filteredItems: [BudgetItem_] = []
    @State private var selectedCategory: BudgetCategory? = nil
    @State private var showPaidOnly = false
    @State private var showUnpaidOnly = false
    @State private var isLoading = true
    @State private var errorMessage = ""
    @State private var showError = false
    
    // Comments state
    @State private var commentCount = 0
    @State private var showComments = false
    @State private var selectedItemForComments: BudgetItem_?
    
    // All budget categories
    private let allCategories: [BudgetCategory] = [
        BudgetCategory.transport, 
        BudgetCategory.accommodation, 
        BudgetCategory.meals, 
        BudgetCategory.activities, 
        BudgetCategory.equipment, 
        BudgetCategory.other
    ]
    
    // Dialog states
    @State private var showAddDialog = false
    @State private var showEditDialog = false
    @State private var showDeleteConfirmation = false
    @State private var itemToEdit: BudgetItem_?
    @State private var itemToDelete: BudgetItem_?
    
    // Form fields
    @State private var itemName = ""
    @State private var itemDescription = ""
    @State private var itemEstimatedCost = ""
    @State private var itemCategory: BudgetCategory = .other
    
    var body: some View {
        NavigationView {
            ZStack {
                Color(.systemGroupedBackground)
                    .ignoresSafeArea()
                
                VStack(spacing: 0) {
                    // Budget Summary Header
                    summaryHeader
                    
                    // Filters
                    filtersSection
                    
                    if isLoading {
                        loadingView
                    } else {
                        // Items List
                        itemsList
                    }
                }
                
                // FAB - Add Button
                VStack {
                    Spacer()
                    HStack {
                        Spacer()
                        Button(action: { showAddDialog = true }) {
                            Image(systemName: "plus")
                                .font(.system(size: 20, weight: .semibold))
                                .foregroundColor(.white)
                                .frame(width: 56, height: 56)
                                .background(Color.blue)
                                .clipShape(Circle())
                                .shadow(color: Color.black.opacity(0.2), radius: 8, x: 0, y: 4)
                        }
                        .padding(.trailing, 20)
                        .padding(.bottom, 20)
                    }
                }
            }
            .navigationTitle("Budget Details")
            .navigationBarTitleDisplayMode(.large)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(action: onBack) {
                        Image(systemName: "arrow.left")
                            .font(.system(size: 16, weight: .semibold))
                            .foregroundColor(.secondary)
                            .frame(width: 36, height: 36)
                            .background(Color(.tertiarySystemFill))
                            .clipShape(Circle())
                    }
                }
                
                ToolbarItem(placement: .navigationBarTrailing) {
                    CommentButton(commentCount: commentCount) {
                        showComments = true
                    }
                }
            }
            .onAppear {
                loadItems()
                loadCommentCount()
            }
            .sheet(isPresented: $showAddDialog) {
                addItemSheet
            }
            .sheet(isPresented: $showEditDialog) {
                editItemSheet
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
            .alert("Delete Item", isPresented: $showDeleteConfirmation) {
                Button("Cancel", role: .cancel) {}
                Button("Delete", role: .destructive) {
                    if let item = itemToDelete {
                        Task { await deleteItem(item) }
                    }
                }
            } message: {
                Text("Are you sure you want to delete this item?")
            }
            .alert("Error", isPresented: $showError) {
                Button("OK", role: .cancel) {}
            } message: {
                Text(errorMessage)
            }
        }
    }
    
    // MARK: - Header View (removed - now in toolbar)
    
    // MARK: - Summary Header
    
    // MARK: - Summary Header
    
    private var summaryHeader: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .top, spacing: 20) {
                VStack(alignment: .leading, spacing: 4) {
                    Text("Estimated")
                        .font(.system(size: 13))
                        .foregroundColor(.secondary)
                    
                    Text("$\(formatCost(budget.totalEstimated))")
                        .font(.system(size: 24, weight: .bold))
                        .foregroundColor(.blue)
                }
                
                VStack(alignment: .leading, spacing: 4) {
                    Text("Actual")
                        .font(.system(size: 13))
                        .foregroundColor(.secondary)
                    
                    Text("$\(formatCost(budget.totalActual))")
                        .font(.system(size: 24, weight: .bold))
                        .foregroundColor(actualCostColor)
                }
                
                Spacer()
                
                if budget.totalActual > budget.totalEstimated {
                    VStack(alignment: .trailing, spacing: 4) {
                        Image(systemName: "exclamationmark.triangle.fill")
                            .font(.system(size: 16))
                            .foregroundColor(.orange)
                        
                        Text("Over Budget")
                            .font(.system(size: 11, weight: .medium))
                            .foregroundColor(.orange)
                    }
                }
            }
        }
        .padding(16)
        .glassCard()
        .padding(.horizontal, 20)
        .padding(.bottom, 12)
    }
    
    // MARK: - Filters Section
    
    private var filtersSection: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                // All Categories
                FilterChip(
                    title: "All",
                    isSelected: selectedCategory == nil,
                    action: {
                        selectedCategory = nil
                        applyFilters()
                    }
                )
                
                // Category Chips
                ForEach(allCategories, id: \.self) { category in
                    FilterChip(
                        title: categoryName(category),
                        icon: categoryIcon(category),
                        isSelected: selectedCategory == category,
                        action: {
                            selectedCategory = category
                            applyFilters()
                        }
                    )
                }
                
                // Divider
                Rectangle()
                    .fill(Color(.separator))
                    .frame(width: 1, height: 24)
                    .padding(.horizontal, 4)
                
                // Payment Status
                FilterChip(
                    title: "Paid",
                    icon: "checkmark.circle.fill",
                    isSelected: showPaidOnly,
                    action: {
                        showPaidOnly.toggle()
                        if showPaidOnly { showUnpaidOnly = false }
                        applyFilters()
                    }
                )
                
                FilterChip(
                    title: "Unpaid",
                    icon: "circle",
                    isSelected: showUnpaidOnly,
                    action: {
                        showUnpaidOnly.toggle()
                        if showUnpaidOnly { showPaidOnly = false }
                        applyFilters()
                    }
                )
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 8)
        }
    }
    
    // MARK: - Items List
    
    private var itemsList: some View {
        ScrollView {
            if filteredItems.isEmpty {
                emptyStateView
            } else {
                LazyVStack(spacing: 12) {
                    ForEach(filteredItems, id: \.id) { item in
                        BudgetItemCard(
                            item: item,
                            onEdit: {
                                itemToEdit = item
                                itemName = item.name
                                 itemDescription = item.description
                                itemEstimatedCost = String(format: "%.2f", item.estimatedCost)
                                itemCategory = item.category
                                showEditDialog = true
                            },
                            onDelete: {
                                itemToDelete = item
                                showDeleteConfirmation = true
                            },
                            onMarkPaid: {
                                Task { await markItemAsPaid(item) }
                            }
                        )
                    }
                    
                    // Bottom padding for FAB
                    Spacer()
                        .frame(height: 80)
                }
                .padding(.horizontal, 20)
                .padding(.top, 8)
            }
        }
    }
    
    // MARK: - Loading View
    
    private var loadingView: some View {
        VStack(spacing: 20) {
            ProgressView()
                .progressViewStyle(CircularProgressViewStyle())
            
            Text("Loading items...")
                .font(.system(size: 17))
                .foregroundColor(.secondary)
        }
        .frame(maxHeight: .infinity)
    }
    
    // MARK: - Empty State
    
    private var emptyStateView: some View {
        VStack(spacing: 24) {
            Image(systemName: "tray")
                .font(.system(size: 50))
                .foregroundColor(.secondary)
            
            VStack(spacing: 8) {
                Text("No Items")
                    .font(.system(size: 20, weight: .semibold))
                    .foregroundColor(.primary)
                
                Text("Add budget items to track expenses")
                    .font(.system(size: 15))
                    .foregroundColor(.secondary)
            }
        }
        .frame(maxHeight: .infinity)
        .padding(.top, 60)
    }
    
    // MARK: - Add Item Sheet
    
    private var addItemSheet: some View {
        NavigationView {
            Form {
                Section {
                    TextField("Item Name", text: $itemName)
                    TextField("Description", text: $itemDescription)
                    TextField("Estimated Cost", text: $itemEstimatedCost)
                        .keyboardType(.decimalPad)
                }
                
                Section {
                    Picker("Category", selection: $itemCategory) {
                        ForEach(allCategories, id: \.self) { category in
                            Label(categoryName(category), systemImage: categoryIcon(category))
                                .tag(category)
                        }
                    }
                }
            }
            .navigationTitle("Add Item")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") {
                        resetForm()
                        showAddDialog = false
                    }
                }
                
                ToolbarItem(placement: .confirmationAction) {
                    Button("Add") {
                        Task {
                            await addItem()
                            showAddDialog = false
                        }
                    }
                    .disabled(itemName.isEmpty || itemEstimatedCost.isEmpty)
                }
            }
        }
    }
    
    // MARK: - Edit Item Sheet
    
    private var editItemSheet: some View {
        NavigationView {
            Form {
                Section {
                    TextField("Item Name", text: $itemName)
                    TextField("Description", text: $itemDescription)
                    TextField("Estimated Cost", text: $itemEstimatedCost)
                        .keyboardType(.decimalPad)
                }
                
                Section {
                    Picker("Category", selection: $itemCategory) {
                        ForEach(allCategories, id: \.self) { category in
                            Label(categoryName(category), systemImage: categoryIcon(category))
                                .tag(category)
                        }
                    }
                }
            }
            .navigationTitle("Edit Item")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") {
                        resetForm()
                        showEditDialog = false
                    }
                }
                
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") {
                        Task {
                            await updateItem()
                            showEditDialog = false
                        }
                    }
                    .disabled(itemName.isEmpty || itemEstimatedCost.isEmpty)
                }
            }
        }
    }
    
    // MARK: - Helper Properties
    
    private var actualCostColor: Color {
        let percentage = budget.totalEstimated > 0 
            ? (budget.totalActual / budget.totalEstimated) * 100 
            : 0
        
        if percentage <= 100 {
            return .green
        } else if percentage <= 120 {
            return .orange
        } else {
            return .red
        }
    }
    
    // MARK: - Helper Functions
    
    private func formatCost(_ cost: Double) -> String {
        String(format: "%.2f", cost)
    }
    
    private func categoryName(_ category: BudgetCategory) -> String {
        switch category {
        case .transport: return "Transport"
        case .accommodation: return "Accommodation"
        case .meals: return "Meals"
        case .activities: return "Activities"
        case .equipment: return "Equipment"
        case .other: return "Other"
        default: return "Other"
        }
    }
    
    private func categoryIcon(_ category: BudgetCategory) -> String {
        switch category {
        case .transport: return "car.fill"
        case .accommodation: return "house.fill"
        case .meals: return "fork.knife"
        case .activities: return "figure.walk"
        case .equipment: return "bag.fill"
        case .other: return "ellipsis.circle.fill"
        default: return "ellipsis.circle.fill"
        }
    }
    
    private func loadItems() {
        Task {
            isLoading = true
            do {
                items = try await repository.getBudgetItems(budgetId: budget.id)
                applyFilters()
                isLoading = false
            } catch {
                errorMessage = error.localizedDescription
                showError = true
                isLoading = false
            }
        }
    }
    
    private func applyFilters() {
        var filtered = items
        
        // Filter by category
        if let category = selectedCategory {
            filtered = filtered.filter { $0.category == category }
        }
        
        // Filter by payment status
        if showPaidOnly {
            filtered = filtered.filter { $0.isPaid }
        } else if showUnpaidOnly {
            filtered = filtered.filter { !$0.isPaid }
        }
        
        filteredItems = filtered
    }
    
    private func addItem() async {
        guard let estimatedCost = Double(itemEstimatedCost) else { return }
        
        do {
            let newItem = BudgetItem_(
                id: UUID().uuidString,
                budgetId: budget.id,
                category: itemCategory,
                name: itemName,
                description: itemDescription,
                estimatedCost: estimatedCost,
                actualCost: 0.0,
                isPaid: false,
                paidBy: nil,
                sharedBy: ["user-1"], // TODO: Get from event participants
                notes: "",
                createdAt: getCurrentIsoTimestamp(),
                updatedAt: getCurrentIsoTimestamp()
            )
            
            try await repository.createBudgetItem(
                budgetId: budget.id,
                category: itemCategory,
                name: itemName,
                description: itemDescription,
                estimatedCost: estimatedCost,
                sharedBy: ["user-1"],
                notes: ""
            )
            resetForm()
            loadItems()
        } catch {
            errorMessage = "Failed to add item: \(error.localizedDescription)"
            showError = true
        }
    }
    
    private func updateItem() async {
        guard let item = itemToEdit,
              let estimatedCost = Double(itemEstimatedCost) else { return }
        
        do {
            let updatedItem = BudgetItem_(
                id: item.id,
                budgetId: item.budgetId,
                category: itemCategory,
                name: itemName,
                description: itemDescription,
                estimatedCost: estimatedCost,
                actualCost: item.actualCost,
                isPaid: item.isPaid,
                paidBy: item.paidBy,
                sharedBy: item.sharedBy,
                notes: item.notes,
                createdAt: item.createdAt,
                updatedAt: getCurrentIsoTimestamp()
            )
            
            try await repository.updateBudgetItem(item: updatedItem)
            resetForm()
            loadItems()
        } catch {
            errorMessage = "Failed to update item: \(error.localizedDescription)"
            showError = true
        }
    }
    
    private func deleteItem(_ item: BudgetItem_) async {
        do {
            try await repository.deleteBudgetItem(itemId: item.id)
            loadItems()
        } catch {
            errorMessage = "Failed to delete item: \(error.localizedDescription)"
            showError = true
        }
    }
    
    private func markItemAsPaid(_ item: BudgetItem_) async {
        do {
            let updatedItem = BudgetItem_(
                id: item.id,
                budgetId: item.budgetId,
                category: item.category,
                name: item.name,
                description: item.description,
                estimatedCost: item.estimatedCost,
                actualCost: item.estimatedCost, // Use estimated as actual
                isPaid: true,
                paidBy: "user-1", // TODO: Get current user
                sharedBy: item.sharedBy,
                notes: item.notes,
                createdAt: item.createdAt,
                updatedAt: getCurrentIsoTimestamp()
            )
            
            try await repository.updateBudgetItem(item: updatedItem)
            loadItems()
        } catch {
            errorMessage = "Failed to mark as paid: \(error.localizedDescription)"
            showError = true
        }
    }
    
    private func resetForm() {
        itemName = ""
        itemDescription = ""
        itemEstimatedCost = ""
        itemCategory = .other
        itemToEdit = nil
    }
    
    private func getCurrentIsoTimestamp() -> String {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        return formatter.string(from: Date())
    }
    
    // MARK: - Comments
    
    private func loadCommentCount() {
        // TODO: Integrate with CommentRepository
        // For now, placeholder - should fetch count for section .BUDGET and sectionItemId = nil
        commentCount = 0
    }
}

// MARK: - Budget Item Card

private struct BudgetItemCard: View {
    let item: BudgetItem_
    let onEdit: () -> Void
    let onDelete: () -> Void
    let onMarkPaid: () -> Void
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 12) {
                // Category Icon
                Image(systemName: categoryIcon)
                    .font(.system(size: 20))
                    .foregroundColor(categoryColor)
                    .frame(width: 36, height: 36)
                    .background(categoryColor.opacity(0.15))
                    .continuousCornerRadius(8)
                
                VStack(alignment: .leading, spacing: 4) {
                    HStack {
                        Text(item.name)
                            .font(.system(size: 16, weight: .semibold))
                            .foregroundColor(.primary)
                        
                        Spacer()
                        
                        if item.isPaid {
                            Image(systemName: "checkmark.circle.fill")
                                .font(.system(size: 16))
                                .foregroundColor(.green)
                        }
                    }
                    
                    if !item.description.isEmpty {
                        Text(item.description)
                            .font(.system(size: 13))
                            .foregroundColor(.secondary)
                            .lineLimit(2)
                    }
                }
            }
            
            // Cost Info
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Estimated")
                        .font(.system(size: 11))
                        .foregroundColor(.secondary)
                    
                    Text("$\(formatCost(item.estimatedCost))")
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundColor(.blue)
                }
                
                if item.isPaid {
                    Divider()
                        .frame(height: 30)
                    
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Actual")
                            .font(.system(size: 11))
                            .foregroundColor(.secondary)
                        
                        Text("$\(formatCost(item.actualCost))")
                            .font(.system(size: 15, weight: .semibold))
                            .foregroundColor(.green)
                    }
                }
                
                Spacer()
                
                // Actions
                Menu {
                    if !item.isPaid {
                        Button {
                            onMarkPaid()
                        } label: {
                            Label("Mark as Paid", systemImage: "checkmark.circle")
                        }
                    }
                    
                    Button {
                        onEdit()
                    } label: {
                        Label("Edit", systemImage: "pencil")
                    }
                    
                    Button(role: .destructive) {
                        onDelete()
                    } label: {
                        Label("Delete", systemImage: "trash")
                    }
                } label: {
                    Image(systemName: "ellipsis.circle")
                        .font(.system(size: 20))
                        .foregroundColor(.secondary)
                }
            }
        }
        .padding(16)
        .glassCard()
    }
    
    private var categoryIcon: String {
        switch item.category {
        case .transport: return "car.fill"
        case .accommodation: return "house.fill"
        case .meals: return "fork.knife"
        case .activities: return "figure.walk"
        case .equipment: return "bag.fill"
        case .other: return "ellipsis.circle.fill"
        default: return "ellipsis.circle.fill"
        }
    }
    
    private var categoryColor: Color {
        switch item.category {
        case .transport: return .blue
        case .accommodation: return .purple
        case .meals: return .orange
        case .activities: return .green
        case .equipment: return .pink
        case .other: return .gray
        default: return .gray
        }
    }
    
    private func formatCost(_ cost: Double) -> String {
        String(format: "%.2f", cost)
    }
}

// MARK: - Preview
// Note: FilterChip is already defined in SharedComponents.swift

struct BudgetDetailView_Previews: PreviewProvider {
    static var previews: some View {
        // Note: Preview uses placeholder repository
        // Real implementation requires proper database initialization
        BudgetDetailView(
            budget: Budget_(
                id: "budget-1",
                eventId: "event-1",
                totalEstimated: 1000.0,
                totalActual: 850.0,
                transportEstimated: 300.0,
                transportActual: 280.0,
                accommodationEstimated: 400.0,
                accommodationActual: 380.0,
                mealsEstimated: 200.0,
                mealsActual: 150.0,
                activitiesEstimated: 100.0,
                activitiesActual: 40.0,
                equipmentEstimated: 0.0,
                equipmentActual: 0.0,
                otherEstimated: 0.0,
                otherActual: 0.0,
                createdAt: "2025-12-01T00:00:00Z",
                updatedAt: "2025-12-01T00:00:00Z"
            ),
            eventId: "event-1",
            repository: BudgetRepository(db: DatabaseProvider.shared.getDatabase(factory: IosDatabaseFactory())),
            currentUserId: "user-1",
            currentUserName: "Test User",
            onBack: {}
        )
    }
}
