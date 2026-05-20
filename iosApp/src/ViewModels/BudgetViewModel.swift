import Foundation
import Shared

// MARK: - Swift-side wrappers for KMP types

struct BudgetItemModel: Identifiable, Equatable {
    let id: String
    let budgetId: String
    let categoryName: String       // KMP enum name: "TRANSPORT", "MEALS", etc.
    let name: String
    let description: String
    let estimatedCost: Double
    let actualCost: Double
    let isPaid: Bool
    let paidBy: String?
    let sharedBy: [String]
    let notes: String
    let createdAt: String
    let updatedAt: String

    var relevantCost: Double { isPaid && actualCost > 0 ? actualCost : estimatedCost }
    var costPerPerson: Double {
        let cost = relevantCost
        return sharedBy.isEmpty ? cost : cost / Double(sharedBy.count)
    }

    static func from(_ item: BudgetItem_) -> BudgetItemModel {
        BudgetItemModel(
            id: item.id,
            budgetId: item.budgetId,
            categoryName: item.category.name,
            name: item.name,
            description: item.description_,
            estimatedCost: item.estimatedCost,
            actualCost: item.actualCost,
            isPaid: item.isPaid,
            paidBy: item.paidBy,
            sharedBy: item.sharedBy as? [String] ?? [],
            notes: item.notes,
            createdAt: item.createdAt,
            updatedAt: item.updatedAt
        )
    }
}

struct BudgetCategoryModel: Identifiable {
    let id: String                  // category name
    let displayName: String
    let iconName: String
    let estimated: Double
    let actual: Double
    let itemCount: Int
    let paidItemCount: Int
    let kmpCategory: BudgetCategory

    var usagePercentage: Double {
        guard estimated > 0 else { return 0 }
        return min(actual / estimated, 1.0)
    }
    var isOverBudget: Bool { actual > estimated }
    var remaining: Double { estimated - actual }
}

struct ParticipantBalanceModel: Identifiable {
    let id: String           // participantId
    let name: String
    let balance: Double      // positive = owes, negative = is owed
    var owesMore: Bool { balance > 0.01 }
    var isOwed: Bool { balance < -0.01 }
}

// MARK: - Swift enum for UI (converts to KMP BudgetCategory)

enum BudgetCategoryUI: String, CaseIterable, Identifiable {
    case transport = "TRANSPORT"
    case accommodation = "ACCOMMODATION"
    case meals = "MEALS"
    case activities = "ACTIVITIES"
    case equipment = "EQUIPMENT"
    case other = "OTHER"

    var id: String { rawValue }

    var displayName: String {
        switch self {
        case .transport:     return "Transport"
        case .accommodation: return "Hébergement"
        case .meals:         return "Repas"
        case .activities:    return "Activités"
        case .equipment:     return "Équipement"
        case .other:         return "Autre"
        }
    }

    var iconName: String {
        switch self {
        case .transport:     return "car.fill"
        case .accommodation: return "house.fill"
        case .meals:         return "fork.knife"
        case .activities:    return "ticket.fill"
        case .equipment:     return "bag.fill"
        case .other:         return "ellipsis.circle.fill"
        }
    }

    var kmpCategory: BudgetCategory {
        switch self {
        case .transport:     return BudgetCategory.transport
        case .accommodation: return BudgetCategory.accommodation
        case .meals:         return BudgetCategory.meals
        case .activities:    return BudgetCategory.activities
        case .equipment:     return BudgetCategory.equipment
        case .other:         return BudgetCategory.other
        }
    }

    static func from(categoryName: String) -> BudgetCategoryUI {
        return BudgetCategoryUI(rawValue: categoryName) ?? .other
    }
}

// MARK: - BudgetViewModel

@MainActor
class BudgetViewModel: ObservableObject {

    // MARK: - Published state

    @Published var isLoading = true
    @Published var errorMessage: String? = nil

    @Published var totalEstimated: Double = 0
    @Published var totalActual: Double = 0
    @Published var participantCount: Int = 0

    @Published var categoryModels: [BudgetCategoryModel] = []
    @Published var itemsByCategoryName: [String: [BudgetItemModel]] = [:]
    @Published var allItems: [BudgetItemModel] = []
    @Published var participantBalances: [ParticipantBalanceModel] = []

    // MARK: - Private

    private let eventId: String
    private let budgetRepository: BudgetRepository
    private var budgetId: String?

    // MARK: - Init

    init(eventId: String) {
        self.eventId = eventId
        self.budgetRepository = BudgetRepository(db: RepositoryProvider.shared.database)
    }

    // MARK: - Load

    func load() {
        Task {
            isLoading = true
            errorMessage = nil
            await loadBudget()
            isLoading = false
        }
    }

    private func loadBudget() async {
        // Fetch or create budget for this event
        var budget = budgetRepository.getBudgetByEventId(eventId: eventId)
        if budget == nil {
            budget = budgetRepository.createBudget(eventId: eventId)
        }
        guard let budget = budget else {
            errorMessage = "Impossible de charger le budget."
            return
        }

        self.budgetId = budget.id
        self.totalEstimated = budget.totalEstimated
        self.totalActual = budget.totalActual

        // Load items
        let rawItems = budgetRepository.getBudgetItems(budgetId: budget.id) as? [BudgetItem_] ?? []
        let items = rawItems.map(BudgetItemModel.from)
        self.allItems = items

        // Group by category name
        var grouped: [String: [BudgetItemModel]] = [:]
        for item in items {
            grouped[item.categoryName, default: []].append(item)
        }
        self.itemsByCategoryName = grouped

        // Build category models
        self.categoryModels = BudgetCategoryUI.allCases.compactMap { cat in
            let catItems = grouped[cat.rawValue] ?? []
            guard !catItems.isEmpty else { return nil }
            let estimated = catItems.reduce(0) { $0 + $1.estimatedCost }
            let actual = catItems.filter { $0.isPaid }.reduce(0) { $0 + $1.actualCost }
            let paidCount = catItems.filter { $0.isPaid }.count
            return BudgetCategoryModel(
                id: cat.rawValue,
                displayName: cat.displayName,
                iconName: cat.iconName,
                estimated: estimated,
                actual: actual,
                itemCount: catItems.count,
                paidItemCount: paidCount,
                kmpCategory: cat.kmpCategory
            )
        }

        // Participant balances — from KMP calculator
        let balancesMap = budgetRepository.getParticipantBalances(budgetId: budget.id)
        if let map = balancesMap as? [String: AnyObject] {
            self.participantBalances = map.compactMap { participantId, value -> ParticipantBalanceModel? in
                let balance: Double
                if let kotlinDouble = value as? Double {
                    balance = kotlinDouble
                } else if let nsNumber = value as? NSNumber {
                    balance = nsNumber.doubleValue
                } else {
                    return nil
                }
                return ParticipantBalanceModel(
                    id: participantId,
                    name: participantId,
                    balance: balance
                )
            }
        }
    }

    // MARK: - Budget helpers

    var budgetUsagePercentage: Double {
        guard totalEstimated > 0 else { return 0 }
        return min(totalActual / totalEstimated, 1.0)
    }

    var isOverBudget: Bool { totalActual > totalEstimated }
    var remainingBudget: Double { totalEstimated - totalActual }

    var costPerPerson: Double {
        guard participantCount > 0 else { return totalEstimated }
        return totalEstimated / Double(participantCount)
    }

    // MARK: - Actions

    func addItem(
        name: String,
        description: String,
        categoryUI: BudgetCategoryUI,
        estimatedCost: Double,
        sharedBy: [String] = []
    ) {
        guard let budgetId = budgetId else { return }
        Task {
            _ = budgetRepository.createBudgetItem(
                budgetId: budgetId,
                category: categoryUI.kmpCategory,
                name: name,
                description: description,
                estimatedCost: estimatedCost,
                sharedBy: sharedBy,
                notes: ""
            )
            await loadBudget()
        }
    }

    func markItemAsPaid(itemId: String, actualCost: Double, paidBy: String) {
        Task {
            _ = budgetRepository.markItemAsPaid(itemId: itemId, actualCost: actualCost, paidBy: paidBy)
            await loadBudget()
        }
    }

    func deleteItem(itemId: String) {
        Task {
            budgetRepository.deleteBudgetItem(itemId: itemId)
            await loadBudget()
        }
    }
}
