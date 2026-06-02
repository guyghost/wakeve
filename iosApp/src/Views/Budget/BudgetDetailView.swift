import SwiftUI

// MARK: - BudgetDetailView

struct BudgetDetailView: View {
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
        .navigationTitle("Dépenses")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button {
                    showAddItem = true
                } label: {
                    Image(systemName: "plus")
                }
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
            "Supprimer cette dépense ?",
            isPresented: $showDeleteConfirm,
            titleVisibility: .visible
        ) {
            Button("Supprimer", role: .destructive) {
                if let item = itemToDelete, !isPreviewing {
                    viewModel.deleteItem(itemId: item.id)
                }
            }
            Button("Annuler", role: .cancel) {}
        }
        .onAppear {
            guard !isPreviewing else { return }
            viewModel.load()
        }
    }

    // MARK: - Loading

    private var loadingView: some View {
        VStack(spacing: 16) {
            ProgressView().scaleEffect(1.2)
            Text("Chargement des dépenses…")
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    // MARK: - Empty

    private var emptyView: some View {
        VStack(spacing: 20) {
            Image(systemName: "eurosign.circle")
                .font(.system(size: 56))
                .foregroundStyle(.secondary.opacity(0.5))
            Text("Aucune dépense")
                .font(.title3.bold())
            Text("Ajoutez des dépenses pour suivre\nle budget de votre événement.")
                .multilineTextAlignment(.center)
                .foregroundStyle(.secondary)
            Button {
                showAddItem = true
            } label: {
                Label("Ajouter une dépense", systemImage: "plus.circle.fill")
                    .fontWeight(.semibold)
                    .padding(.horizontal, 24)
                    .padding(.vertical, 12)
                    .background(.blue, in: RoundedRectangle(cornerRadius: 12))
                    .foregroundStyle(.white)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding()
    }

    // MARK: - List

    private var listView: some View {
        List {
            // Summary header
            BudgetSyncBanner(pendingSync: viewModel.pendingSync, isOnline: viewModel.isOnline)

            Section {
                BudgetSummaryRow(
                    totalEstimated: totalEstimated,
                    totalActual: totalActual,
                    isOverBudget: isOverBudget
                )
            }

            // Items grouped by category
            ForEach(categoryOrder, id: \.rawValue) { category in
                let items = itemsByCategoryName[category.rawValue] ?? []
                if !items.isEmpty {
                    Section(category.displayName) {
                        ForEach(items) { item in
                            BudgetItemRow(item: item) {
                                guard !isPreviewing else { return }
                                viewModel.markItemAsPaid(
                                    itemId: item.id,
                                    actualCost: item.estimatedCost,
                                    paidBy: "current-user"
                                )
                            }
                            .swipeActions(edge: .trailing, allowsFullSwipe: false) {
                                Button(role: .destructive) {
                                    itemToDelete = item
                                    showDeleteConfirm = true
                                } label: {
                                    Label("Supprimer", systemImage: "trash")
                                }
                            }
                        }
                    }
                }
            }

            // Add item button at the bottom
            Section {
                Button {
                    showAddItem = true
                } label: {
                    Label("Ajouter une dépense", systemImage: "plus.circle.fill")
                        .foregroundStyle(.blue)
                }
            }
        }
        .listStyle(.insetGrouped)
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

// MARK: - BudgetSummaryRow

struct BudgetSummaryRow: View {
    let totalEstimated: Double
    let totalActual: Double
    let isOverBudget: Bool

    var body: some View {
        VStack(spacing: 10) {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Total dépensé")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    Text(String(format: "%.2f €", totalActual))
                        .font(.title3.bold())
                        .foregroundStyle(isOverBudget ? .red : .primary)
                }
                Spacer()
                VStack(alignment: .trailing, spacing: 2) {
                    Text("Budget estimé")
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
