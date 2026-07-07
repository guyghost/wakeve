import SwiftUI

// MARK: - BudgetDetailView

struct BudgetDetailView: View {
    @Environment(\.colorScheme) private var colorScheme

    let eventId: String

    @StateObject private var viewModel: BudgetViewModel
    @State private var showAddItem = false
    @State private var itemToDelete: BudgetItemModel? = nil
    @State private var showDeleteConfirm = false
    private let previewItems: [BudgetItemModel]?

    init(eventId: String) {
        self.eventId = eventId
        self._viewModel = StateObject(wrappedValue: BudgetViewModel(eventId: eventId))
        self.previewItems = nil
    }

#if DEBUG
    init(eventId: String, previewItems: [BudgetItemModel]) {
        self.eventId = eventId
        self._viewModel = StateObject(wrappedValue: BudgetViewModel(eventId: eventId))
        self.previewItems = previewItems
    }
#endif

    // All categories in display order
    private let categoryOrder = BudgetCategoryUI.allCases

    var body: some View {
        Group {
            if shouldShowLoading {
                loadingView
            } else if allItems.isEmpty {
                emptyView
            } else {
                listView
            }
        }
        .navigationTitle(String(localized: "budget.expenses_title"))
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button {
                    WakeveHaptics.selection()
                    showAddItem = true
                } label: {
                    Image(systemName: "plus")
                }
                .accessibilityLabel(String(localized: "budget.add_expense"))
            }
        }
        .sheet(isPresented: $showAddItem) {
            AddBudgetItemSheet { name, description, categoryUI, estimatedCost, sharedBy in
                guard !isPreviewing else { return }
                viewModel.addItem(
                    name: name,
                    description: description,
                    categoryUI: categoryUI,
                    estimatedCost: estimatedCost,
                    sharedBy: sharedBy
                )
            }
        }
        .confirmationDialog(
            String(localized: "budget.delete_confirmation.title"),
            isPresented: $showDeleteConfirm,
            titleVisibility: .visible
        ) {
            Button(String(localized: "common.delete"), role: .destructive) {
                if let item = itemToDelete, !isPreviewing {
                    WakeveHaptics.warning()
                    viewModel.deleteItem(itemId: item.id)
                }
            }
            Button(String(localized: "common.cancel"), role: .cancel) {}
        }
        .onAppear {
            guard !isPreviewing else { return }
            viewModel.load()
        }
    }

    // MARK: - Loading

    private var loadingView: some View {
        VStack(spacing: 16) {
            ProgressView()
                .scaleEffect(1.2)
                .accessibilityLabel(String(localized: "common.loading"))
            Text(String(localized: "budget.expenses_loading"))
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    // MARK: - Empty

    private var emptyView: some View {
        ZStack {
            WakeveScreenBackground(style: .grouped)

            WakeveContentCard(prominence: .prominent, cornerRadius: WakeveTheme.Radius.xl, padding: WakeveTheme.Spacing.xl) {
                VStack(spacing: WakeveTheme.Spacing.md) {
                    Image(systemName: "eurosign.circle.fill")
                        .font(.system(size: 56, weight: .semibold))
                        .foregroundStyle(SemanticColor.selectedState(for: colorScheme))
                        .frame(width: 72, height: 72)
                        .background(SemanticColor.badge(for: colorScheme), in: Circle())

                    VStack(spacing: WakeveTheme.Spacing.xs) {
                        Text(String(localized: "budget.empty_expenses_title"))
                            .font(WakeveTheme.Typography.section)
                            .foregroundStyle(SemanticColor.primaryText(for: colorScheme))

                        Text(String(localized: "budget.empty_expenses_body"))
                            .font(WakeveTheme.Typography.body)
                            .multilineTextAlignment(.center)
                            .foregroundStyle(SemanticColor.secondaryText(for: colorScheme))
                    }

                    WakeveActionButton(
                        String(localized: "budget.add_expense"),
                        systemImage: "plus.circle.fill",
                        variant: .primary
                    ) {
                        WakeveHaptics.selection()
                        showAddItem = true
                    }
                }
            }
            .padding(WakeveTheme.Spacing.page)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    // MARK: - List

    private var listView: some View {
        ScrollView {
            LazyVStack(spacing: WakeveTheme.Spacing.md) {
                BudgetSyncBanner(pendingSync: viewModel.pendingSync, isOnline: viewModel.isOnline)

                WakeveContentCard(prominence: .prominent, cornerRadius: WakeveTheme.Radius.xl, padding: WakeveTheme.Spacing.md) {
                    BudgetSummaryRow(
                        totalEstimated: totalEstimated,
                        totalActual: totalActual,
                        isOverBudget: isOverBudget
                    )
                }

                ForEach(categoryOrder, id: \.rawValue) { category in
                    let items = itemsByCategoryName[category.rawValue] ?? []
                    if !items.isEmpty {
                        WakeveContentCard(prominence: .regular, cornerRadius: WakeveTheme.Radius.xl, padding: WakeveTheme.Spacing.md) {
                            VStack(alignment: .leading, spacing: WakeveTheme.Spacing.sm) {
                                BudgetSectionHeader(category: category, itemCount: items.count)

                                ForEach(items) { item in
                                    BudgetDetailItemRow(
                                        item: item,
                                        onMarkAsPaid: {
                                            guard !isPreviewing else { return }
                                            viewModel.markItemAsPaid(
                                                itemId: item.id,
                                                actualCost: item.estimatedCost,
                                                paidBy: "current-user"
                                            )
                                            WakeveHaptics.success()
                                        },
                                        onDelete: {
                                            itemToDelete = item
                                            showDeleteConfirm = true
                                        }
                                    )

                                    if item.id != items.last?.id {
                                        Divider()
                                    }
                                }
                            }
                        }
                    }
                }

                Button {
                    WakeveHaptics.selection()
                    showAddItem = true
                } label: {
                    Label(String(localized: "budget.add_expense"), systemImage: "plus.circle.fill")
                        .font(.headline.weight(.semibold))
                        .frame(maxWidth: .infinity)
                        .frame(height: 50)
                }
                .buttonStyle(.borderedProminent)
                .padding(.top, WakeveTheme.Spacing.xs)
            }
            .padding(WakeveTheme.Spacing.md)
        }
        .background(WakeveScreenBackground(style: .grouped))
    }

    private var isPreviewing: Bool {
        previewItems != nil
    }

    private var shouldShowLoading: Bool {
        !isPreviewing && viewModel.isLoading
    }

    private var allItems: [BudgetItemModel] {
        previewItems ?? viewModel.allItems
    }

    private var itemsByCategoryName: [String: [BudgetItemModel]] {
        if let previewItems {
            Dictionary(grouping: previewItems, by: \.categoryName)
        } else {
            viewModel.itemsByCategoryName
        }
    }

    private var totalEstimated: Double {
        isPreviewing ? allItems.reduce(0) { $0 + $1.estimatedCost } : viewModel.totalEstimated
    }

    private var totalActual: Double {
        isPreviewing ? allItems.filter(\.isPaid).reduce(0) { $0 + $1.actualCost } : viewModel.totalActual
    }

    private var isOverBudget: Bool {
        totalActual > totalEstimated
    }
}

private struct BudgetSectionHeader: View {
    let category: BudgetCategoryUI
    let itemCount: Int

    var body: some View {
        HStack(spacing: 10) {
            Image(systemName: category.iconName)
                .font(.callout.weight(.bold))
                .foregroundStyle(.blue)
                .frame(width: 30, height: 30)
                .background(Color.blue.opacity(0.12), in: Circle())

            Text(category.displayName)
                .font(.headline)

            Spacer()

            Text("\(itemCount)")
                .font(.caption.weight(.bold))
                .foregroundStyle(.secondary)
                .padding(.horizontal, 8)
                .padding(.vertical, 4)
                .background(Color.secondary.opacity(0.12), in: Capsule())
        }
    }
}

private struct BudgetDetailItemRow: View {
    let item: BudgetItemModel
    let onMarkAsPaid: () -> Void
    let onDelete: () -> Void

    var body: some View {
        HStack(alignment: .center, spacing: 12) {
            BudgetItemRow(item: item, onMarkAsPaid: onMarkAsPaid)
                .frame(maxWidth: .infinity, alignment: .leading)

            Menu {
                if !item.isPaid {
                    Button {
                        onMarkAsPaid()
                    } label: {
                        Label(String(localized: "budget.mark_paid"), systemImage: "checkmark.circle.fill")
                    }
                }

                Button(role: .destructive) {
                    WakeveHaptics.warning()
                    onDelete()
                } label: {
                    Label(String(localized: "common.delete"), systemImage: "trash")
                }
            } label: {
                Image(systemName: "ellipsis.circle")
                    .font(.title3)
                    .foregroundStyle(.secondary)
                    .frame(width: 36, height: 36)
            }
            .accessibilityLabel(String(localized: "budget.expense_actions_accessibility"))
        }
    }
}

// MARK: - BudgetSummaryRow

struct BudgetSummaryRow: View {
    let totalEstimated: Double
    let totalActual: Double
    let isOverBudget: Bool

    var body: some View {
        VStack(spacing: 10) {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text(String(localized: "budget.total_spent"))
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    Text(String(format: "%.2f €", totalActual))
                        .font(.title3.bold())
                        .foregroundStyle(isOverBudget ? .red : .primary)
                }
                Spacer()
                VStack(alignment: .trailing, spacing: 2) {
                    Text(String(localized: "budget.total_estimated"))
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    Text(String(format: "%.2f €", totalEstimated))
                        .font(.title3)
                        .foregroundStyle(.secondary)
                }
            }
            ProgressView(value: totalEstimated > 0 ? min(totalActual / totalEstimated, 1.0) : 0)
                .tint(isOverBudget ? .red : .blue)
        }
        .padding(.vertical, 4)
    }
}

// MARK: - Preview

#if DEBUG
#Preview("Budget Detail - Light") {
    NavigationStack {
        BudgetDetailView(eventId: "preview-event-id", previewItems: BudgetFactory.items)
    }
    .preferredColorScheme(.light)
}

#Preview("Budget Detail - Dark") {
    NavigationStack {
        BudgetDetailView(eventId: "preview-event-id", previewItems: BudgetFactory.items)
    }
    .preferredColorScheme(.dark)
}
#endif
