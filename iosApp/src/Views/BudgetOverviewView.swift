import SwiftUI
import Shared

// MARK: - Helper Structs

/// Helper struct for category breakdown
struct BudgetCategoryDetails {
    let category: BudgetCategory
    let estimatedCost: Double
    let actualCost: Double
}

// MARK: - Budget Overview View

/// Budget Overview View - iOS
///
/// Displays budget summary with category breakdown and per-person costs.
/// Uses Liquid Glass design system with standardized components.
struct BudgetOverviewView: View {
    let event: Event
    let repository: BudgetRepository
    let onBack: () -> Void
    let onViewDetails: () -> Void
    
    @State private var budget: Budget_?
    @State private var items: [BudgetItem_] = []
    @State private var categoryBreakdown: [BudgetCategory: BudgetCategoryDetails] = [:]
    @State private var isLoading = true
    @State private var errorMessage = ""
    @State private var showError = false
    
    // Hardcoded participant count (TODO: get from event)
    private let participantCount: Int32 = 3
    
    // All budget categories
    private let allCategories: [BudgetCategory] = [
        .transport, .accommodation, .meals, .activities, .equipment, .other
    ]
    
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
                backButton
                
                Spacer()
            }
            .padding(.horizontal, 20)
            .padding(.top, 60)
            
            VStack(spacing: 8) {
                Text("Budget")
                    .font(.system(size: 34, weight: .bold))
                    .foregroundColor(.primary)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .accessibilityAddTraits(.isHeader)
                
                Text(event.title)
                    .font(.system(size: 20, weight: .medium))
                    .foregroundColor(.secondary)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
            .padding(.horizontal, 20)
        }
        .background(Color(.systemGroupedBackground))
    }
    
    private var backButton: some View {
        Button(action: onBack) {
            Image(systemName: "arrow.left")
                .font(.system(size: 16, weight: .semibold))
                .foregroundColor(.secondary)
                .frame(width: 36, height: 36)
                .background(Color(.tertiarySystemFill))
                .clipShape(Circle())
        }
        .accessibilityLabel("Go back")
        .accessibilityHint("Returns to the previous screen")
    }
    
    // MARK: - Summary Card
    
    private var summaryCard: some View {
        LiquidGlassCard(cornerRadius: 16, padding: 20) {
            VStack(alignment: .leading, spacing: 16) {
                // Header with icon
                HStack {
                    Image(systemName: "chart.bar.fill")
                        .font(.system(size: 20))
                        .foregroundColor(.wakevPrimary)
                    
                    Text("Budget Summary")
                        .font(.system(size: 20, weight: .semibold))
                        .foregroundColor(.primary)
                    
                    Spacer()
                }
                
                if let budget = budget {
                    // Estimated vs Actual
                    HStack(alignment: .top, spacing: 20) {
                        estimatedColumn(budget: budget)
                        actualColumn(budget: budget)
                        Spacer()
                    }
                    
                    // Progress Bar
                    if budget.totalEstimated > 0 {
                        progressSection(budget: budget)
                    }
                }
            }
        }
    }
    
    private func estimatedColumn(budget: Budget_) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text("Estimated")
                .font(.system(size: 13))
                .foregroundColor(.secondary)
            
            HStack(spacing: 4) {
                Text("$")
                    .font(.system(size: 20, weight: .bold))
                    .foregroundColor(.wakevPrimary)
                Text(formatCost(budget.totalEstimated))
                    .font(.system(size: 28, weight: .bold))
                    .foregroundColor(.wakevPrimary)
            }
        }
        .accessibilityLabel("Estimated budget: \(formatCost(budget.totalEstimated)) dollars")
    }
    
    private func actualColumn(budget: Budget_) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text("Actual")
                .font(.system(size: 13))
                .foregroundColor(.secondary)
            
            HStack(spacing: 4) {
                Text("$")
                    .font(.system(size: 20, weight: .bold))
                    .foregroundColor(actualCostColor)
                Text(formatCost(budget.totalActual))
                    .font(.system(size: 28, weight: .bold))
                    .foregroundColor(actualCostColor)
            }
        }
        .accessibilityLabel("Actual spending: \(formatCost(budget.totalActual)) dollars")
    }
    
    private func progressSection(budget: Budget_) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text("Usage")
                    .font(.system(size: 13))
                    .foregroundColor(.secondary)
                
                Spacer()
                
                LiquidGlassBadge(
                    text: "\(Int(usagePercentage))%",
                    style: usagePercentageBadgeStyle
                )
            }
            
            GeometryReader { geometry in
                ZStack(alignment: .leading) {
                    // Background
                    RoundedRectangle(cornerRadius: 4, style: .continuous)
                        .fill(Color(.tertiarySystemFill))
                        .frame(height: 8)
                    
                    // Progress
                    RoundedRectangle(cornerRadius: 4, style: .continuous)
                        .fill(progressBarGradient)
                        .frame(
                            width: min(geometry.size.width * CGFloat(usagePercentage / 100.0), geometry.size.width),
                            height: 8
                        )
                }
            }
            .frame(height: 8)
        }
        .accessibilityValue("\(Int(usagePercentage))% of budget used")
    }
    
    private var usagePercentageBadgeStyle: LiquidGlassBadgeStyle {
        if usagePercentage <= 80 {
            return .success
        } else if usagePercentage <= 100 {
            return .info
        } else if usagePercentage <= 120 {
            return .warning
        } else {
            return .default
        }
    }
    
    private var progressBarGradient: LinearGradient {
        LinearGradient(
            gradient: Gradient(colors: progressBarColors),
            startPoint: .leading,
            endPoint: .trailing
        )
    }
    
    private var progressBarColors: [Color] {
        if usagePercentage <= 80 {
            return [.wakevSuccess, .wakevSuccessLight]
        } else if usagePercentage <= 100 {
            return [.wakevPrimary, .wakevAccent]
        } else if usagePercentage <= 120 {
            return [.wakevWarning, .wakevWarningLight]
        } else {
            return [.wakevError, .wakevErrorLight]
        }
    }
    
    // MARK: - Per Person Card
    
    private var perPersonCard: some View {
        LiquidGlassCard(cornerRadius: 16, padding: 20) {
            VStack(alignment: .leading, spacing: 16) {
                HStack {
                    Image(systemName: "person.2.fill")
                        .font(.system(size: 20))
                        .foregroundColor(.wakevAccent)
                    
                    Text("Cost Per Person")
                        .font(.system(size: 20, weight: .semibold))
                        .foregroundColor(.primary)
                    
                    Spacer()
                }
                
                if let budget = budget {
                    HStack(alignment: .top, spacing: 20) {
                        perPersonEstimatedColumn(budget: budget)
                        perPersonActualColumn(budget: budget)
                        Spacer()
                    }
                    
                    LiquidGlassDivider(style: .subtle)
                    
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
        }
    }
    
    private func perPersonEstimatedColumn(budget: Budget_) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text("Estimated")
                .font(.system(size: 13))
                .foregroundColor(.secondary)
            
            HStack(spacing: 4) {
                Text("$")
                    .font(.system(size: 16, weight: .bold))
                    .foregroundColor(.wakevPrimary)
                Text(formatCost(budget.totalEstimated / Double(participantCount)))
                    .font(.system(size: 24, weight: .bold))
                    .foregroundColor(.wakevPrimary)
            }
        }
        .accessibilityLabel("Estimated cost per person: \(formatCost(budget.totalEstimated / Double(participantCount))) dollars")
    }
    
    private func perPersonActualColumn(budget: Budget_) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text("Actual")
                .font(.system(size: 13))
                .foregroundColor(.secondary)
            
            HStack(spacing: 4) {
                Text("$")
                    .font(.system(size: 16, weight: .bold))
                    .foregroundColor(actualCostColor)
                Text(formatCost(budget.totalActual / Double(participantCount)))
                    .font(.system(size: 24, weight: .bold))
                    .foregroundColor(actualCostColor)
            }
        }
        .accessibilityLabel("Actual cost per person: \(formatCost(budget.totalActual / Double(participantCount))) dollars")
    }
    
    // MARK: - Status Card
    
    private var statusCard: some View {
        LiquidGlassCard(cornerRadius: 16, padding: 16) {
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
        }
    }
    
    private var statusIcon: some View {
        Group {
            if usagePercentage <= 100 {
                Image(systemName: "checkmark.circle.fill")
                    .font(.system(size: 28))
                    .foregroundColor(.wakevSuccess)
            } else if usagePercentage <= 120 {
                Image(systemName: "exclamationmark.triangle.fill")
                    .font(.system(size: 28))
                    .foregroundColor(.wakevWarning)
            } else {
                Image(systemName: "xmark.octagon.fill")
                    .font(.system(size: 28))
                    .foregroundColor(.wakevError)
            }
        }
        .accessibilityHidden(true)
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
            let diff = (budget?.totalActual ?? 0) - (budget?.totalEstimated ?? 0)
            return "$\(formatCost(diff)) over estimated budget"
        } else {
            let diff = (budget?.totalActual ?? 0) - (budget?.totalEstimated ?? 0)
            return "$\(formatCost(diff)) significantly over budget"
        }
    }
    
    // MARK: - Category Breakdown
    
    private var categoryBreakdownSection: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack {
                Image(systemName: "square.grid.2x2.fill")
                    .font(.system(size: 20))
                    .foregroundColor(.wakevWarning)
                
                Text("Category Breakdown")
                    .font(.system(size: 20, weight: .semibold))
                    .foregroundColor(.primary)
            }
            
            VStack(spacing: 12) {
                ForEach(allCategories, id: \.self) { category in
                    if let details = categoryBreakdown[category],
                       details.actualCost > 0 || details.estimatedCost > 0 {
                        CategoryRow(category: category, details: details)
                    }
                }
            }
        }
        .padding(20)
        .liquidGlass(cornerRadius: 16, opacity: 0.85, intensity: 0.9)
    }
    
    // MARK: - View Details Button
    
    private var viewDetailsButton: some View {
        LiquidGlassButton(
            title: "View All Items",
            style: .secondary
        ) {
            onViewDetails()
        }
        .accessibilityLabel("View all budget items")
        .accessibilityHint("Shows complete list of budget items")
    }
    
    // MARK: - Loading View
    
    private var loadingView: some View {
        VStack(spacing: 20) {
            ProgressView()
                .progressViewStyle(CircularProgressViewStyle())
                .scaleEffect(1.2)
            
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
                .opacity(0.6)
            
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
            
            LiquidGlassButton(
                title: "Create Budget",
                style: .primary
            ) {
                Task { createBudget() }
            }
            .padding(.horizontal, 40)
            .accessibilityLabel("Create a new budget")
        }
        .frame(maxHeight: .infinity)
    }
    
    // MARK: - Helper Properties
    
    private var usagePercentage: Double {
        guard let budget = budget, budget.totalEstimated > 0 else { return 0 }
        return (budget.totalActual / budget.totalEstimated) * 100
    }
    
    private var actualCostColor: Color {
        if usagePercentage <= 100 {
            return .wakevSuccess
        } else if usagePercentage <= 120 {
            return .wakevWarning
        } else {
            return .wakevError
        }
    }
    
    // MARK: - Helper Functions
    
    private func formatCost(_ cost: Double) -> String {
        String(format: "%.2f", cost)
    }
    
    private func loadBudget() {
        Task {
            isLoading = true
            // Get budget for event
            budget = repository.getBudgetByEventId(eventId: event.id)
            
            if let budget = budget {
                // Get all items
                items = repository.getBudgetItems(budgetId: budget.id)
                
                // Calculate category breakdown
                for category in allCategories {
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
        }
    }
    
    private func createBudget() {
        repository.createBudget(eventId: event.id)
        loadBudget()
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
        LiquidGlassListItem(
            title: categoryName,
            subtitle: "$\(formatCost(details.actualCost)) of $\(formatCost(details.estimatedCost))",
            icon: categoryIcon,
            iconColor: categoryColor,
            style: .compact
        ) {
            if details.estimatedCost > 0 {
                let percentage = (details.actualCost / details.estimatedCost) * 100
                LiquidGlassBadge(
                    text: "\(Int(percentage))%",
                    style: percentageBadgeStyle(percentage: percentage)
                )
            }
        }
        .accessibilityLabel("\(categoryName): \(formatCost(details.actualCost)) of \(formatCost(details.estimatedCost))")
    }
    
    private var categoryIcon: String {
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
    
    private var categoryColor: Color {
        switch category {
        case .transport: return .wakevPrimary
        case .accommodation: return .wakevAccent
        case .meals: return .wakevWarning
        case .activities: return .wakevSuccess
        case .equipment: return .pink
        case .other: return .secondary
        default: return .secondary
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
        default: return "Other"
        }
    }
    
    private func percentageBadgeStyle(percentage: Double) -> LiquidGlassBadgeStyle {
        if percentage <= 80 {
            return .success
        } else if percentage <= 100 {
            return .info
        } else if percentage <= 120 {
            return .warning
        } else {
            return .default
        }
    }
    
    private func formatCost(_ cost: Double) -> String {
        String(format: "%.2f", cost)
    }
}

// MARK: - Preview

#Preview("Budget Overview View - With Data") {
    BudgetOverviewView(
        event: Event(
            id: "event-1",
            title: "Team Retreat",
            description: "Annual team building",
            organizerId: "user-1",
            participants: [],
            proposedSlots: [],
            deadline: "2025-12-31T23:59:59Z",
            status: .organizing,
            finalDate: nil,
            createdAt: "2025-12-01T00:00:00Z",
            updatedAt: "2025-12-01T00:00:00Z",
            eventType: .teamBuilding,
            eventTypeCustom: nil,
            minParticipants: nil,
            maxParticipants: nil,
            expectedParticipants: nil,
            heroImageUrl: nil
        ),
        repository: BudgetRepository(db: DatabaseProvider.shared.getDatabase(factory: IosDatabaseFactory())),
        onBack: {},
        onViewDetails: {}
    )
}

#Preview("Budget Overview View - Empty State") {
    BudgetOverviewView(
        event: Event(
            id: "event-2",
            title: "Birthday Party",
            description: "John's birthday",
            organizerId: "user-1",
            participants: [],
            proposedSlots: [],
            deadline: "2025-12-31T23:59:59Z",
            status: .draft,
            finalDate: nil,
            createdAt: "2025-12-01T00:00:00Z",
            updatedAt: "2025-12-01T00:00:00Z",
            eventType: .birthday,
            eventTypeCustom: nil,
            minParticipants: nil,
            maxParticipants: nil,
            expectedParticipants: nil,
            heroImageUrl: nil
        ),
        repository: BudgetRepository(db: DatabaseProvider.shared.getDatabase(factory: IosDatabaseFactory())),
        onBack: {},
        onViewDetails: {}
    )
}

#Preview("Budget Overview View - Over Budget") {
    BudgetOverviewView(
        event: Event(
            id: "event-3",
            title: "Conference",
            description: "Annual conference",
            organizerId: "user-1",
            participants: [],
            proposedSlots: [],
            deadline: "2025-12-31T23:59:59Z",
            status: .organizing,
            finalDate: nil,
            createdAt: "2025-12-01T00:00:00Z",
            updatedAt: "2025-12-01T00:00:00Z",
            eventType: .conference,
            eventTypeCustom: nil,
            minParticipants: nil,
            maxParticipants: nil,
            expectedParticipants: nil,
            heroImageUrl: nil
        ),
        repository: BudgetRepository(db: DatabaseProvider.shared.getDatabase(factory: IosDatabaseFactory())),
        onBack: {},
        onViewDetails: {}
    )
}
