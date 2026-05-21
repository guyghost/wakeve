import Foundation
import Shared

#if DEBUG

enum BudgetFactory {
    static var items: [BudgetItemModel] {
        [
            item(
                id: "budget-item-train",
                category: .transport,
                name: "Train aller-retour",
                description: "Billets Paris - Marseille",
                estimatedCost: 420,
                actualCost: 398,
                isPaid: true,
                paidBy: "marie@example.com",
                sharedBy: ["marie@example.com", "lucas@example.com", "ines@example.com"]
            ),
            item(
                id: "budget-item-dinner",
                category: .meals,
                name: "Diner terrasse",
                description: "Reservation pour le groupe",
                estimatedCost: 280,
                actualCost: 0,
                isPaid: false,
                paidBy: nil,
                sharedBy: ["marie@example.com", "lucas@example.com", "ines@example.com"]
            ),
            item(
                id: "budget-item-activity",
                category: .activities,
                name: "Atelier cuisine",
                description: "Cours prive samedi apres-midi",
                estimatedCost: 180,
                actualCost: 210,
                isPaid: true,
                paidBy: "lucas@example.com",
                sharedBy: ["marie@example.com", "lucas@example.com"]
            ),
            item(
                id: "budget-item-house",
                category: .accommodation,
                name: "Appartement vieux port",
                description: "Deux nuits",
                estimatedCost: 640,
                actualCost: 640,
                isPaid: true,
                paidBy: "ines@example.com",
                sharedBy: ["marie@example.com", "lucas@example.com", "ines@example.com"]
            )
        ]
    }

    static var categoryModels: [BudgetCategoryModel] {
        BudgetCategoryUI.allCases.compactMap { category in
            let categoryItems = items.filter { $0.categoryName == category.rawValue }
            guard !categoryItems.isEmpty else { return nil }

            let estimated = categoryItems.reduce(0) { $0 + $1.estimatedCost }
            let actual = categoryItems.filter(\.isPaid).reduce(0) { $0 + $1.actualCost }

            return BudgetCategoryModel(
                id: category.rawValue,
                displayName: category.displayName,
                iconName: category.iconName,
                estimated: estimated,
                actual: actual,
                itemCount: categoryItems.count,
                paidItemCount: categoryItems.filter(\.isPaid).count,
                kmpCategory: category.kmpCategory
            )
        }
    }

    static var participantBalances: [ParticipantBalanceModel] {
        [
            ParticipantBalanceModel(id: "marie@example.com", name: "Marie", balance: 74.50),
            ParticipantBalanceModel(id: "lucas@example.com", name: "Lucas", balance: -28.00),
            ParticipantBalanceModel(id: "ines@example.com", name: "Ines", balance: -46.50)
        ]
    }

    private static func item(
        id: String,
        category: BudgetCategoryUI,
        name: String,
        description: String,
        estimatedCost: Double,
        actualCost: Double,
        isPaid: Bool,
        paidBy: String?,
        sharedBy: [String]
    ) -> BudgetItemModel {
        BudgetItemModel(
            id: id,
            budgetId: "budget-preview",
            categoryName: category.rawValue,
            name: name,
            description: description,
            estimatedCost: estimatedCost,
            actualCost: actualCost,
            isPaid: isPaid,
            paidBy: paidBy,
            sharedBy: sharedBy,
            notes: "",
            createdAt: "2026-05-21T09:00:00Z",
            updatedAt: "2026-05-21T09:00:00Z"
        )
    }
}

#endif
