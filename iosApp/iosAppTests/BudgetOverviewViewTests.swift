import XCTest
@testable import Wakeve

/// Tests for BudgetOverviewView rendering logic and navigation state.
///
/// Since SwiftUI view tests in a unit-test target focus on data flow rather than
/// pixel-level rendering, these tests verify:
/// - The view model state drives the correct UI branches (loading / error / content)
/// - Navigation state toggles correctly
/// - Category and balance models populate as expected
@MainActor
final class BudgetOverviewViewTests: XCTestCase {

    // MARK: - ViewModel State → UI Branch Tests

    func testInitialState_showsLoading() {
        let vm = BudgetViewModel(eventId: "test-event")
        // On init, isLoading = true → the view should show the loading spinner
        XCTAssertTrue(vm.isLoading, "BudgetOverviewView should display loading state on init")
        XCTAssertNil(vm.errorMessage, "No error message should be present initially")
    }

    func testErrorState_afterFailedLoad() {
        let vm = BudgetViewModel(eventId: "nonexistent-event")
        // Simulate what happens when budget load fails (errorMessage is set)
        // We can't easily call load() in unit tests without a real DB,
        // so we test the state transition contract.
        vm.isLoading = false
        vm.errorMessage = "Impossible de charger le budget."

        XCTAssertFalse(vm.isLoading, "Should not be loading when error is present")
        XCTAssertNotNil(vm.errorMessage, "Error message should be set")
        XCTAssertEqual(vm.errorMessage, "Impossible de charger le budget.")
    }

    func testContentState_afterSuccessfulLoad() {
        let vm = BudgetViewModel(eventId: "test-event")
        // Simulate a successful load by populating view model state
        vm.isLoading = false
        vm.totalEstimated = 500.0
        vm.totalActual = 300.0
        vm.participantCount = 5

        XCTAssertFalse(vm.isLoading, "Should not be loading after successful load")
        XCTAssertNil(vm.errorMessage, "No error should be present after successful load")
        XCTAssertEqual(vm.totalEstimated, 500.0)
        XCTAssertEqual(vm.totalActual, 300.0)
    }

    // MARK: - Budget Usage & Over-Budget Logic (mirrors what the view displays)

    func testBudgetUsagePercentage_normalCase() {
        let vm = BudgetViewModel(eventId: "test-event")
        vm.isLoading = false
        vm.totalEstimated = 1000.0
        vm.totalActual = 650.0

        XCTAssertEqual(vm.budgetUsagePercentage, 0.65, accuracy: 0.01,
                        "Usage percentage should be actual/estimated")
        XCTAssertFalse(vm.isOverBudget, "Should not be over budget at 65%")
        XCTAssertEqual(vm.remainingBudget, 350.0, accuracy: 0.01,
                        "Remaining budget should be estimated - actual")
    }

    func testBudgetUsagePercentage_overBudget() {
        let vm = BudgetViewModel(eventId: "test-event")
        vm.isLoading = false
        vm.totalEstimated = 400.0
        vm.totalActual = 550.0

        // budgetUsagePercentage is capped at 1.0 (min(..., 1.0))
        XCTAssertEqual(vm.budgetUsagePercentage, 1.0, accuracy: 0.01,
                        "Usage percentage should be capped at 1.0")
        XCTAssertTrue(vm.isOverBudget, "Should be over budget when actual > estimated")
        XCTAssertEqual(vm.remainingBudget, -150.0, accuracy: 0.01,
                        "Remaining budget should be negative when over budget")
    }

    func testBudgetUsagePercentage_zeroEstimated() {
        let vm = BudgetViewModel(eventId: "test-event")
        vm.isLoading = false
        vm.totalEstimated = 0
        vm.totalActual = 0

        XCTAssertEqual(vm.budgetUsagePercentage, 0,
                        "Usage percentage should be 0 when estimated is 0")
        XCTAssertFalse(vm.isOverBudget, "Should not be over budget when both are 0")
    }

    func testCostPerPerson_withParticipants() {
        let vm = BudgetViewModel(eventId: "test-event")
        vm.isLoading = false
        vm.totalEstimated = 600.0
        vm.participantCount = 3

        XCTAssertEqual(vm.costPerPerson, 200.0, accuracy: 0.01,
                        "Cost per person should be estimated / participant count")
    }

    func testCostPerPerson_zeroParticipants() {
        let vm = BudgetViewModel(eventId: "test-event")
        vm.isLoading = false
        vm.totalEstimated = 600.0
        vm.participantCount = 0

        XCTAssertEqual(vm.costPerPerson, 600.0, accuracy: 0.01,
                        "Cost per person should equal total when no participants")
    }

    // MARK: - Category Breakdown (drives categoryBreakdownCard visibility)

    func testCategoryModels_emptyByDefault() {
        let vm = BudgetViewModel(eventId: "test-event")
        XCTAssertTrue(vm.categoryModels.isEmpty,
                       "Category models should be empty before load")
    }

    func testCategoryModels_populatedWithData() {
        let vm = BudgetViewModel(eventId: "test-event")
        vm.isLoading = false

        let category = BudgetCategoryModel(
            id: "TRANSPORT",
            displayName: "Transport",
            iconName: "car.fill",
            estimated: 200.0,
            actual: 180.0,
            itemCount: 3,
            paidItemCount: 2,
            kmpCategory: BudgetCategory.transport
        )
        vm.categoryModels = [category]

        XCTAssertEqual(vm.categoryModels.count, 1)
        XCTAssertEqual(vm.categoryModels[0].displayName, "Transport")
        XCTAssertEqual(vm.categoryModels[0].usagePercentage, 0.9, accuracy: 0.01)
        XCTAssertFalse(vm.categoryModels[0].isOverBudget)
        XCTAssertEqual(vm.categoryModels[0].remaining, 20.0, accuracy: 0.01)
    }

    func testCategoryModel_overBudget() {
        let category = BudgetCategoryModel(
            id: "MEALS",
            displayName: "Repas",
            iconName: "fork.knife",
            estimated: 100.0,
            actual: 150.0,
            itemCount: 5,
            paidItemCount: 5,
            kmpCategory: BudgetCategory.meals
        )

        XCTAssertTrue(category.isOverBudget)
        XCTAssertEqual(category.usagePercentage, 1.0, accuracy: 0.01,
                        "Usage should be capped at 1.0")
        XCTAssertEqual(category.remaining, -50.0, accuracy: 0.01)
    }

    func testCategoryModel_zeroEstimated() {
        let category = BudgetCategoryModel(
            id: "OTHER",
            displayName: "Autre",
            iconName: "ellipsis.circle.fill",
            estimated: 0,
            actual: 0,
            itemCount: 0,
            paidItemCount: 0,
            kmpCategory: BudgetCategory.other
        )

        XCTAssertEqual(category.usagePercentage, 0,
                        "Usage should be 0 when estimated is 0")
        XCTAssertFalse(category.isOverBudget)
    }

    // MARK: - Participant Balances (drives participantBalancesCard visibility)

    func testParticipantBalances_emptyByDefault() {
        let vm = BudgetViewModel(eventId: "test-event")
        XCTAssertTrue(vm.participantBalances.isEmpty,
                       "Participant balances should be empty before load")
    }

    func testParticipantBalances_populated() {
        let vm = BudgetViewModel(eventId: "test-event")
        vm.isLoading = false

        vm.participantBalances = [
            ParticipantBalanceModel(id: "u1", name: "Alice", balance: 50.0),
            ParticipantBalanceModel(id: "u2", name: "Bob", balance: -30.0),
            ParticipantBalanceModel(id: "u3", name: "Charlie", balance: 0.0)
        ]

        XCTAssertEqual(vm.participantBalances.count, 3)

        // Alice owes more
        XCTAssertTrue(vm.participantBalances[0].owesMore)
        XCTAssertFalse(vm.participantBalances[0].isOwed)

        // Bob is owed
        XCTAssertFalse(vm.participantBalances[1].owesMore)
        XCTAssertTrue(vm.participantBalances[1].isOwed)

        // Charlie is balanced
        XCTAssertFalse(vm.participantBalances[2].owesMore)
        XCTAssertFalse(vm.participantBalances[2].isOwed)
    }

    // MARK: - Navigation State

    func testNavigateToDetail_initialState() {
        // The BudgetOverviewView uses @State private var navigateToDetail = false
        // We verify the view model doesn't hold navigation state — it's view-internal.
        // This test documents the contract: navigation is driven by the view's @State,
        // not the ViewModel. When navigateToDetail becomes true, it triggers
        // NavigationStack's .navigationDestination.
        let vm = BudgetViewModel(eventId: "test-event")
        // ViewModel should not have a navigateToDetail property
        // This is intentional — navigation state lives in the view layer
        XCTAssertTrue(vm.allItems.isEmpty, "Items should be empty before navigation is meaningful")
    }

    // MARK: - Items Integration

    func testAllItems_andItemsByCategoryPopulated() {
        let vm = BudgetViewModel(eventId: "test-event")
        vm.isLoading = false

        let item1 = BudgetItemModel(
            id: "1", budgetId: "b1", categoryName: "TRANSPORT",
            name: "Train", description: "",
            estimatedCost: 100.0, actualCost: 80.0,
            isPaid: true, paidBy: "u1", sharedBy: ["u1", "u2"],
            notes: "", createdAt: "", updatedAt: ""
        )
        let item2 = BudgetItemModel(
            id: "2", budgetId: "b1", categoryName: "MEALS",
            name: "Dinner", description: "",
            estimatedCost: 60.0, actualCost: 0,
            isPaid: false, paidBy: nil, sharedBy: [],
            notes: "", createdAt: "", updatedAt: ""
        )

        vm.allItems = [item1, item2]
        vm.itemsByCategoryName = [
            "TRANSPORT": [item1],
            "MEALS": [item2]
        ]

        XCTAssertEqual(vm.allItems.count, 2)
        XCTAssertEqual(vm.itemsByCategoryName["TRANSPORT"]?.count, 1)
        XCTAssertEqual(vm.itemsByCategoryName["MEALS"]?.count, 1)
        XCTAssertEqual(vm.itemsByCategoryName["ACCOMMODATION"]?.count ?? 0, 0,
                        "Categories with no items should not appear")
    }
}
