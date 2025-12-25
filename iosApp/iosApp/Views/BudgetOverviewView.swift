import SwiftUI
import Shared

/// Budget Overview View - iOS
///
/// Displays budget summary with category breakdown and per-person costs.
/// Uses Liquid Glass design system with Material backgrounds.
struct BudgetOverviewView: View {
    let event: Event
    let repository: BudgetRepository
    let onBack: () -> Void
    let onViewDetails: () -> Void
    
    @State private var budget: Budget?
    @State private var items: [BudgetItem] = []
    @State private var categoryBreakdown: [BudgetCategory: BudgetCategoryDetails] = [:]
    @State private var isLoading = true
    @State private var errorMessage = ""
    @State private var showError = false
    
    // Hardcoded participant count (TODO: get from event)
    private let participantCount: Int32 = 3
    
    var body: some View {
        ZStack {
            Color(.systemGroupedBackground)
                .ignoresSafeArea()
            
            VStack(spacing: 0) {
                // Header
                headerView
                
                if isLoading {
                    loadingView
                } else if budget == nil {
                    emptyStateView
                } else {
                    ScrollView {
                        VStack(spacing: 16) {
                            // Summary Card
                            summaryCard
                            
                            // Per Person Card
                            perPersonCard
                            
                            // Status Card
                            statusCard
                            
                            // Category Breakdown
                            categoryBreakdownSection
                            
                            // View Details Button
                            viewDetailsButton
                            
                            Spacer()
                                .frame(height: 40)
                        }
                        .padding(.horizontal, 20)
                        .padding(.top, 16)
                    }
                }
            }
        }
        .onAppear {
            loadBudget()
        }
        .alert("Error", isPresented: $showError) {
            Button("OK", role: .cancel) {}
        } message: {
            Text(errorMessage)
        }
    }
    
    // MARK: - Header View
    
    private var headerView: some View {
        VStack(spacing: 16) {
            HStack {
                Button(action: onBack) {
                    Image(systemName: "arrow.left")
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundColor(.secondary)
                        .frame(width: 36, height: 36)
                        .background(Color(.tertiarySystemFill))
                        .clipShape(Circle())
                }
                
                Spacer()
            }
            .padding(.horizontal, 20)
            .padding(.top, 60)
            
            VStack(spacing: 8) {
                Text("Budget")
                    .font(.system(size: 34, weight: .bold))
                    .foregroundColor(.primary)
                    .frame(maxWidth: .infinity, alignment: .leading)
                
                Text(event.title)
                    .font(.system(size: 20, weight: .medium))
                    .foregroundColor(.secondary)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
            .padding(.horizontal, 20)
        }
        .background(Color(.systemGroupedBackground))
    }
    
    // MARK: - Summary Card
    
    private var summaryCard: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack {
                Image(systemName: "chart.bar.fill")
                    .font(.system(size: 20))
                    .foregroundColor(.blue)
                
                Text("Budget Summary")
                    .font(.system(size: 20, weight: .semibold))
                    .foregroundColor(.primary)
            }
            
            if let budget = budget {
                // Estimated vs Actual
                HStack(alignment: .top, spacing: 20) {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("Estimated")
                            .font(.system(size: 13))
                            .foregroundColor(.secondary)
                        
                        Text("$\(formatCost(budget.totalEstimatedCost))")
                            .font(.system(size: 28, weight: .bold))
                            .foregroundColor(.blue)
                    }
                    
                    VStack(alignment: .leading, spacing: 4) {
                        Text("Actual")
                            .font(.system(size: 13))
                            .foregroundColor(.secondary)
                        
                        Text("$\(formatCost(budget.totalActualCost))")
                            .font(.system(size: 28, weight: .bold))
                            .foregroundColor(actualCostColor)
                    }
                    
                    Spacer()
                }
                
                // Progress Bar
                if budget.totalEstimatedCost > 0 {
                    VStack(alignment: .leading, spacing: 8) {
                        HStack {
                            Text("Usage")
                                .font(.system(size: 13))
                                .foregroundColor(.secondary)
                            
                            Spacer()
                            
                            Text("\(Int(usagePercentage))%")
                                .font(.system(size: 13, weight: .semibold))
                                .foregroundColor(usagePercentage > 100 ? .red : .secondary)
                        }
                        
                        GeometryReader { geometry in
                            ZStack(alignment: .leading) {
                                // Background
                                RoundedRectangle(cornerRadius: 4, style: .continuous)
                                    .fill(Color(.tertiarySystemFill))
                                    .frame(height: 8)
                                
                                // Progress
                                RoundedRectangle(cornerRadius: 4, style: .continuous)
                                    .fill(progressBarColor)
                                    .frame(
                                        width: min(geometry.size.width * CGFloat(usagePercentage / 100.0), geometry.size.width),
                                        height: 8
                                    )
                            }
                        }
                        .frame(height: 8)
                    }
                }
            }
        }
        .padding(20)
        .glassCard()
    }
    
    // MARK: - Per Person Card
    
    private var perPersonCard: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack {
                Image(systemName: "person.2.fill")
                    .font(.system(size: 20))
                    .foregroundColor(.purple)
                
                Text("Cost Per Person")
                    .font(.system(size: 20, weight: .semibold))
                    .foregroundColor(.primary)
            }
            
            if let budget = budget {
                HStack(alignment: .top, spacing: 20) {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("Estimated")
                            .font(.system(size: 13))
                            .foregroundColor(.secondary)
                        
                        Text("$\(formatCost(budget.totalEstimatedCost / Double(participantCount)))")
                            .font(.system(size: 24, weight: .bold))
                            .foregroundColor(.blue)
                    }
                    
                    VStack(alignment: .leading, spacing: 4) {
                        Text("Actual")
                            .font(.system(size: 13))
                            .foregroundColor(.secondary)
                        
                        Text("$\(formatCost(budget.totalActualCost / Double(participantCount)))")
                            .font(.system(size: 24, weight: .bold))
                            .foregroundColor(actualCostColor)
                    }
                    
                    Spacer()
                }
                
                HStack(spacing: 8) {
                    Image(systemName: "info.circle")
                        .font(.system(size: 12))
                        .foregroundColor(.secondary)
                    
                    Text("Based on \(participantCount) participants")
                        .font(.system(size: 13))
                        .foregroundColor(.secondary)
                }
            }
        }
        .padding(20)
        .glassCard()
    }
    
    // MARK: - Status Card
    
    private var statusCard: some View {
        HStack(spacing: 12) {
            statusIcon
            
            VStack(alignment: .leading, spacing: 4) {
                Text(statusTitle)
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundColor(.primary)
                
                Text(statusMessage)
                    .font(.system(size: 14))
                    .foregroundColor(.secondary)
            }
            
            Spacer()
        }
        .padding(16)
        .glassCard()
    }
    
    private var statusIcon: some View {
        Group {
            if usagePercentage <= 100 {
                Image(systemName: "checkmark.circle.fill")
                    .font(.system(size: 28))
                    .foregroundColor(.green)
            } else if usagePercentage <= 120 {
                Image(systemName: "exclamationmark.triangle.fill")
                    .font(.system(size: 28))
                    .foregroundColor(.orange)
            } else {
                Image(systemName: "xmark.octagon.fill")
                    .font(.system(size: 28))
                    .foregroundColor(.red)
            }
        }
    }
    
    private var statusTitle: String {
        if usagePercentage <= 100 {
            return "Within Budget"
        } else if usagePercentage <= 120 {
            return "Budget Alert"
        } else {
            return "Over Budget"
        }
    }
    
    private var statusMessage: String {
        if usagePercentage <= 100 {
            return "You're on track with your budget"
        } else if usagePercentage <= 120 {
            let diff = (budget?.totalActualCost ?? 0) - (budget?.totalEstimatedCost ?? 0)
            return "$\(formatCost(diff)) over estimated budget"
        } else {
            let diff = (budget?.totalActualCost ?? 0) - (budget?.totalEstimatedCost ?? 0)
            return "$\(formatCost(diff)) significantly over budget"
        }
    }
    
    // MARK: - Category Breakdown
    
    private var categoryBreakdownSection: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack {
                Image(systemName: "square.grid.2x2.fill")
                    .font(.system(size: 20))
                    .foregroundColor(.orange)
                
                Text("Category Breakdown")
                    .font(.system(size: 20, weight: .semibold))
                    .foregroundColor(.primary)
            }
            
            VStack(spacing: 12) {
                ForEach(BudgetCategory.allCases, id: \.self) { category in
                    if let details = categoryBreakdown[category],
                       details.actualCost > 0 || details.estimatedCost > 0 {
                        CategoryRow(category: category, details: details)
                    }
                }
            }
        }
        .padding(20)
        .glassCard()
    }
    
    // MARK: - View Details Button
    
    private var viewDetailsButton: some View {
        Button(action: onViewDetails) {
            HStack {
                Text("View All Items")
                    .font(.system(size: 17, weight: .semibold))
                
                Spacer()
                
                Image(systemName: "chevron.right")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(.secondary)
            }
            .foregroundColor(.blue)
            .padding(16)
            .glassCard()
        }
    }
    
    // MARK: - Loading View
    
    private var loadingView: some View {
        VStack(spacing: 20) {
            ProgressView()
                .progressViewStyle(CircularProgressViewStyle())
            
            Text("Loading budget...")
                .font(.system(size: 17))
                .foregroundColor(.secondary)
        }
        .frame(maxHeight: .infinity)
    }
    
    // MARK: - Empty State
    
    private var emptyStateView: some View {
        VStack(spacing: 24) {
            Image(systemName: "chart.bar.doc.horizontal")
                .font(.system(size: 60))
                .foregroundColor(.secondary)
            
            VStack(spacing: 12) {
                Text("No Budget Yet")
                    .font(.system(size: 24, weight: .bold))
                    .foregroundColor(.primary)
                
                Text("Create a budget to track expenses and manage costs for this event.")
                    .font(.system(size: 17))
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 40)
            }
            
            Button(action: {
                Task { await createBudget() }
            }) {
                Text("Create Budget")
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 16)
                    .background(Color.blue)
                    .continuousCornerRadius(12)
            }
            .padding(.horizontal, 40)
        }
        .frame(maxHeight: .infinity)
    }
    
    // MARK: - Helper Properties
    
    private var usagePercentage: Double {
        guard let budget = budget, budget.totalEstimatedCost > 0 else { return 0 }
        return (budget.totalActualCost / budget.totalEstimatedCost) * 100
    }
    
    private var actualCostColor: Color {
        if usagePercentage <= 100 {
            return .green
        } else if usagePercentage <= 120 {
            return .orange
        } else {
            return .red
        }
    }
    
    private var progressBarColor: Color {
        if usagePercentage <= 80 {
            return .green
        } else if usagePercentage <= 100 {
            return .blue
        } else if usagePercentage <= 120 {
            return .orange
        } else {
            return .red
        }
    }
    
    // MARK: - Helper Functions
    
    private func formatCost(_ cost: Double) -> String {
        String(format: "%.2f", cost)
    }
    
    private func loadBudget() {
        Task {
            isLoading = true
            do {
                // Get budget for event
                budget = try await repository.getBudgetByEventId(eventId: event.id)
                
                if let budget = budget {
                    // Get all items
                    items = try await repository.getItemsByBudgetId(budgetId: budget.id)
                    
                    // Calculate category breakdown
                    for category in BudgetCategory.allCases {
                        let categoryItems = items.filter { $0.category == category }
                        let estimated = categoryItems.reduce(0.0) { $0 + $1.estimatedCost }
                        let actual = categoryItems.reduce(0.0) { $0 + $1.actualCost }
                        
                        categoryBreakdown[category] = BudgetCategoryDetails(
                            category: category,
                            estimatedCost: estimated,
                            actualCost: actual
                        )
                    }
                }
                
                isLoading = false
            } catch {
                errorMessage = error.localizedDescription
                showError = true
                isLoading = false
            }
        }
    }
    
    private func createBudget() async {
        do {
            let newBudget = Budget(
                id: UUID().uuidString,
                eventId: event.id,
                totalEstimatedCost: 0.0,
                totalActualCost: 0.0,
                estimatedTransportCost: 0.0,
                actualTransportCost: 0.0,
                estimatedAccommodationCost: 0.0,
                actualAccommodationCost: 0.0,
                estimatedMealsCost: 0.0,
                actualMealsCost: 0.0,
                estimatedActivitiesCost: 0.0,
                actualActivitiesCost: 0.0,
                estimatedEquipmentCost: 0.0,
                actualEquipmentCost: 0.0,
                estimatedOtherCost: 0.0,
                actualOtherCost: 0.0,
                createdAt: getCurrentIsoTimestamp(),
                updatedAt: getCurrentIsoTimestamp()
            )
            
            try await repository.createBudget(budget: newBudget)
            loadBudget()
        } catch {
            errorMessage = "Failed to create budget: \(error.localizedDescription)"
            showError = true
        }
    }
    
    private func getCurrentIsoTimestamp() -> String {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        return formatter.string(from: Date())
    }
}

// MARK: - Category Row

private struct CategoryRow: View {
    let category: BudgetCategory
    let details: BudgetCategoryDetails
    
    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: categoryIcon)
                .font(.system(size: 20))
                .foregroundColor(categoryColor)
                .frame(width: 28, height: 28)
            
            VStack(alignment: .leading, spacing: 4) {
                Text(categoryName)
                    .font(.system(size: 15, weight: .medium))
                    .foregroundColor(.primary)
                
                Text("$\(formatCost(details.actualCost)) of $\(formatCost(details.estimatedCost))")
                    .font(.system(size: 13))
                    .foregroundColor(.secondary)
            }
            
            Spacer()
            
            // Progress indicator
            if details.estimatedCost > 0 {
                let percentage = (details.actualCost / details.estimatedCost) * 100
                Text("\(Int(percentage))%")
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundColor(percentage > 100 ? .red : .secondary)
            }
        }
        .padding(.vertical, 8)
    }
    
    private var categoryIcon: String {
        switch category {
        case .transport: return "car.fill"
        case .accommodation: return "house.fill"
        case .meals: return "fork.knife"
        case .activities: return "figure.walk"
        case .equipment: return "bag.fill"
        case .other: return "ellipsis.circle.fill"
        }
    }
    
    private var categoryColor: Color {
        switch category {
        case .transport: return .blue
        case .accommodation: return .purple
        case .meals: return .orange
        case .activities: return .green
        case .equipment: return .pink
        case .other: return .gray
        }
    }
    
    private var categoryName: String {
        switch category {
        case .transport: return "Transport"
        case .accommodation: return "Accommodation"
        case .meals: return "Meals"
        case .activities: return "Activities"
        case .equipment: return "Equipment"
        case .other: return "Other"
        }
    }
    
    private func formatCost(_ cost: Double) -> String {
        String(format: "%.2f", cost)
    }
}

// MARK: - Preview

struct BudgetOverviewView_Previews: PreviewProvider {
    static var previews: some View {
        BudgetOverviewView(
            event: Event(
                id: "event-1",
                title: "Team Retreat",
                description: "Annual team building",
                organizerId: "user-1",
                status: .organizing,
                deadline: "2025-12-31T23:59:59Z",
                createdAt: "2025-12-01T00:00:00Z",
                updatedAt: "2025-12-01T00:00:00Z"
            ),
            repository: BudgetRepository(database: DatabaseFactory().createDatabase()),
            onBack: {},
            onViewDetails: {}
        )
    }
}
