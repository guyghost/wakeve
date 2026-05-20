import SwiftUI

// MARK: - BudgetDetailView

struct BudgetDetailView: View {
    let eventId: String

    @StateObject private var viewModel: BudgetViewModel
    @State private var showAddItem = false
    @State private var itemToDelete: BudgetItemModel? = nil
    @State private var showDeleteConfirm = false

    init(eventId: String) {
        self.eventId = eventId
        self._viewModel = StateObject(wrappedValue: BudgetViewModel(eventId: eventId))
    }

    // All categories in display order
    private let categoryOrder = BudgetCategoryUI.allCases

    var body: some View {
        Group {
            if viewModel.isLoading {
                loadingView
            } else if viewModel.allItems.isEmpty {
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
                if let item = itemToDelete {
                    viewModel.deleteItem(itemId: item.id)
                }
            }
            Button("Annuler", role: .cancel) {}
        }
        .onAppear { viewModel.load() }
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
            Section {
                BudgetSummaryRow(
                    totalEstimated: viewModel.totalEstimated,
                    totalActual: viewModel.totalActual,
                    isOverBudget: viewModel.isOverBudget
                )
            }

            // Items grouped by category
            ForEach(categoryOrder, id: \.rawValue) { category in
                let items = viewModel.itemsByCategoryName[category.rawValue] ?? []
                if !items.isEmpty {
                    Section(category.displayName) {
                        ForEach(items) { item in
                            BudgetItemRow(item: item) {
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

#Preview {
    NavigationStack {
        BudgetDetailView(eventId: "preview-event-id")
    }
}
