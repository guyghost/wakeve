import XCTest
@testable import Wakeve

/// Tests for BudgetViewModel
/// Verifies budget calculations, state management, and UI model conversions.
@MainActor
final class BudgetViewModelTests: XCTestCase {

    // MARK: - BudgetCategoryUI Tests

    func testBudgetCategoryUIAllCasesCount() {
        // There must be exactly 6 categories (matching KMP BudgetCategory enum)
        XCTAssertEqual(BudgetCategoryUI.allCases.count, 6)
    }

    func testBudgetCategoryUIRawValues() {
        XCTAssertEqual(BudgetCategoryUI.transport.rawValue, "TRANSPORT")
        XCTAssertEqual(BudgetCategoryUI.accommodation.rawValue, "ACCOMMODATION")
        XCTAssertEqual(BudgetCategoryUI.meals.rawValue, "MEALS")
        XCTAssertEqual(BudgetCategoryUI.activities.rawValue, "ACTIVITIES")
        XCTAssertEqual(BudgetCategoryUI.equipment.rawValue, "EQUIPMENT")
        XCTAssertEqual(BudgetCategoryUI.other.rawValue, "OTHER")
    }

    func testBudgetCategoryUIDisplayNames() {
        XCTAssertEqual(BudgetCategoryUI.transport.displayName, "Transport")
        XCTAssertEqual(BudgetCategoryUI.accommodation.displayName, "Hébergement")
        XCTAssertEqual(BudgetCategoryUI.meals.displayName, "Repas")
        XCTAssertEqual(BudgetCategoryUI.activities.displayName, "Activités")
        XCTAssertEqual(BudgetCategoryUI.equipment.displayName, "Équipement")
        XCTAssertEqual(BudgetCategoryUI.other.displayName, "Autre")
    }

    func testBudgetCategoryUIIconNames() {
        XCTAssertFalse(BudgetCategoryUI.transport.iconName.isEmpty)
        XCTAssertFalse(BudgetCategoryUI.accommodation.iconName.isEmpty)
        XCTAssertFalse(BudgetCategoryUI.meals.iconName.isEmpty)
        XCTAssertFalse(BudgetCategoryUI.activities.iconName.isEmpty)
        XCTAssertFalse(BudgetCategoryUI.equipment.iconName.isEmpty)
        XCTAssertFalse(BudgetCategoryUI.other.iconName.isEmpty)
    }

    func testBudgetCategoryUIFromCategoryName_validNames() {
        XCTAssertEqual(BudgetCategoryUI.from(categoryName: "TRANSPORT"), .transport)
        XCTAssertEqual(BudgetCategoryUI.from(categoryName: "ACCOMMODATION"), .accommodation)
        XCTAssertEqual(BudgetCategoryUI.from(categoryName: "MEALS"), .meals)
        XCTAssertEqual(BudgetCategoryUI.from(categoryName: "ACTIVITIES"), .activities)
        XCTAssertEqual(BudgetCategoryUI.from(categoryName: "EQUIPMENT"), .equipment)
        XCTAssertEqual(BudgetCategoryUI.from(categoryName: "OTHER"), .other)
    }

    func testBudgetCategoryUIFromCategoryName_unknownFallsToOther() {
        XCTAssertEqual(BudgetCategoryUI.from(categoryName: "UNKNOWN"), .other)
        XCTAssertEqual(BudgetCategoryUI.from(categoryName: ""), .other)
        XCTAssertEqual(BudgetCategoryUI.from(categoryName: "random"), .other)
    }

    // MARK: - BudgetItemModel Tests

    func testBudgetItemModel_relevantCost_whenNotPaid() {
        let item = BudgetItemModel(
            id: "1", budgetId: "b1", categoryName: "TRANSPORT",
            name: "Train", description: "",
            estimatedCost: 100.0, actualCost: 80.0,
            isPaid: false, paidBy: nil, sharedBy: [],
            notes: "", createdAt: "", updatedAt: ""
        )
        // When not paid, relevant cost = estimated
        XCTAssertEqual(item.relevantCost, 100.0)
    }

    func testBudgetItemModel_relevantCost_whenPaid() {
        let item = BudgetItemModel(
            id: "1", budgetId: "b1", categoryName: "TRANSPORT",
            name: "Train", description: "",
            estimatedCost: 100.0, actualCost: 80.0,
            isPaid: true, paidBy: "u1", sharedBy: [],
            notes: "", createdAt: "", updatedAt: ""
        )
        // When paid with actualCost > 0, relevant cost = actual
        XCTAssertEqual(item.relevantCost, 80.0)
    }

    func testBudgetItemModel_costPerPerson_noSharedBy() {
        let item = BudgetItemModel(
            id: "1", budgetId: "b1", categoryName: "MEALS",
            name: "Dinner", description: "",
            estimatedCost: 120.0, actualCost: 0,
            isPaid: false, paidBy: nil, sharedBy: [],
            notes: "", createdAt: "", updatedAt: ""
        )
        // No sharing: full cost
        XCTAssertEqual(item.costPerPerson, 120.0)
    }

    func testBudgetItemModel_costPerPerson_withSharedBy() {
        let item = BudgetItemModel(
            id: "1", budgetId: "b1", categoryName: "MEALS",
            name: "Dinner", description: "",
            estimatedCost: 120.0, actualCost: 0,
            isPaid: false, paidBy: nil, sharedBy: ["u1", "u2", "u3"],
            notes: "", createdAt: "", updatedAt: ""
        )
        // 3 people sharing 120€ = 40€ each
        XCTAssertEqual(item.costPerPerson, 40.0)
    }

    // MARK: - BudgetViewModel init

    func testBudgetViewModelInitialState() {
        let vm = BudgetViewModel(eventId: "test-event")
        XCTAssertTrue(vm.isLoading)
        XCTAssertNil(vm.errorMessage)
        XCTAssertEqual(vm.totalEstimated, 0)
        XCTAssertEqual(vm.totalActual, 0)
        XCTAssertEqual(vm.participantCount, 0)
        XCTAssertTrue(vm.allItems.isEmpty)
        XCTAssertTrue(vm.categoryModels.isEmpty)
        XCTAssertTrue(vm.participantBalances.isEmpty)
    }

    func testBudgetViewModelBudgetHelpers_empty() {
        let vm = BudgetViewModel(eventId: "test-event")
        XCTAssertEqual(vm.budgetUsagePercentage, 0)
        XCTAssertFalse(vm.isOverBudget)
        XCTAssertEqual(vm.remainingBudget, 0)
    }

    // MARK: - ParticipantBalanceModel Tests

    func testParticipantBalanceModel_owesMore() {
        let balance = ParticipantBalanceModel(id: "u1", name: "Alice", balance: 50.0)
        XCTAssertTrue(balance.owesMore)
        XCTAssertFalse(balance.isOwed)
    }

    func testParticipantBalanceModel_isOwed() {
        let balance = ParticipantBalanceModel(id: "u2", name: "Bob", balance: -30.0)
        XCTAssertFalse(balance.owesMore)
        XCTAssertTrue(balance.isOwed)
    }

    func testParticipantBalanceModel_balanced() {
        let balance = ParticipantBalanceModel(id: "u3", name: "Charlie", balance: 0.005)
        XCTAssertFalse(balance.owesMore)  // < 0.01 threshold
        XCTAssertFalse(balance.isOwed)
    }
}
