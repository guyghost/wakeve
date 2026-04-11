import SwiftUI
import Shared

// MARK: - BudgetOverviewView

struct BudgetOverviewView: View {
    let eventId: String

    @StateObject private var viewModel: BudgetViewModel
    @State private var navigateToDetail = false

    init(eventId: String) {
        self.eventId = eventId
        self._viewModel = StateObject(wrappedValue: BudgetViewModel(eventId: eventId))
    }

    var body: some View {
        NavigationStack {
            Group {
                if viewModel.isLoading {
                    loadingView
                } else if let error = viewModel.errorMessage {
                    errorView(message: error)
                } else {
                    contentView
                }
            }
            .navigationTitle("Budget")
            .navigationBarTitleDisplayMode(.large)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button {
                        navigateToDetail = true
                    } label: {
                        Image(systemName: "list.bullet")
                    }
                }
            }
            .navigationDestination(isPresented: $navigateToDetail) {
                BudgetDetailView(eventId: eventId)
            }
        }
        .onAppear { viewModel.load() }
    }

    // MARK: - Loading

    private var loadingView: some View {
        VStack(spacing: 16) {
            ProgressView()
                .scaleEffect(1.2)
            Text("Chargement du budget…")
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    // MARK: - Error

    private func errorView(message: String) -> some View {
        VStack(spacing: 16) {
            Image(systemName: "exclamationmark.triangle.fill")
                .font(.largeTitle)
                .foregroundStyle(.orange)
            Text(message)
                .multilineTextAlignment(.center)
                .foregroundStyle(.secondary)
            Button("Réessayer") { viewModel.load() }
                .buttonStyle(.bordered)
        }
        .padding()
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    // MARK: - Main content

    private var contentView: some View {
        ScrollView {
            LazyVStack(spacing: 16) {
                summaryCard
                if !viewModel.categoryModels.isEmpty {
                    categoryBreakdownCard
                }
                if !viewModel.participantBalances.isEmpty {
                    participantBalancesCard
                }
                detailButton
            }
            .padding()
        }
    }

    // MARK: - Summary card

    private var summaryCard: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack {
                Image(systemName: "eurosign.circle.fill")
                    .foregroundStyle(.blue)
                Text("Résumé")
                    .font(.headline)
                Spacer()
                if viewModel.isOverBudget {
                    Label("Dépassé", systemImage: "exclamationmark.circle.fill")
                        .font(.caption)
                        .foregroundStyle(.red)
                }
            }

            // Progress bar
            VStack(alignment: .leading, spacing: 6) {
                HStack {
                    Text("Budget utilisé")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    Spacer()
                    Text(String(format: "%.0f%%", viewModel.budgetUsagePercentage * 100))
                        .font(.caption.bold())
                        .foregroundStyle(viewModel.isOverBudget ? .red : .primary)
                }
                ProgressView(value: viewModel.budgetUsagePercentage)
                    .tint(viewModel.isOverBudget ? .red : .blue)
            }

            Divider()

            // Amounts grid
            HStack(spacing: 0) {
                amountCell(
                    title: "Estimé",
                    amount: viewModel.totalEstimated,
                    color: .secondary
                )
                Divider().frame(height: 44)
                amountCell(
                    title: "Réel",
                    amount: viewModel.totalActual,
                    color: viewModel.isOverBudget ? .red : .green
                )
                Divider().frame(height: 44)
                amountCell(
                    title: "Restant",
                    amount: viewModel.remainingBudget,
                    color: viewModel.isOverBudget ? .red : .blue
                )
            }

            if viewModel.participantCount > 0 {
                Divider()
                HStack {
                    Image(systemName: "person.2.fill")
                        .foregroundStyle(.secondary)
                        .font(.caption)
                    Text("Coût estimé par personne")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    Spacer()
                    Text(formatAmount(viewModel.costPerPerson))
                        .font(.caption.bold())
                }
            }
        }
        .padding()
        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 16))
    }

    private func amountCell(title: String, amount: Double, color: Color) -> some View {
        VStack(spacing: 4) {
            Text(title)
                .font(.caption)
                .foregroundStyle(.secondary)
            Text(formatAmount(amount))
                .font(.subheadline.bold())
                .foregroundStyle(color)
        }
        .frame(maxWidth: .infinity)
    }

    // MARK: - Category breakdown card

    private var categoryBreakdownCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Image(systemName: "chart.bar.fill")
                    .foregroundStyle(.purple)
                Text("Par catégorie")
                    .font(.headline)
            }

            ForEach(viewModel.categoryModels) { categoryModel in
                BudgetCategoryRow(model: categoryModel)
                if categoryModel.id != viewModel.categoryModels.last?.id {
                    Divider()
                }
            }
        }
        .padding()
        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 16))
    }

    // MARK: - Participant balances card

    private var participantBalancesCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Image(systemName: "person.2.fill")
                    .foregroundStyle(.teal)
                Text("Soldes participants")
                    .font(.headline)
            }

            ForEach(viewModel.participantBalances) { balance in
                HStack {
                    Circle()
                        .fill(balance.owesMore ? Color.orange.opacity(0.2) : Color.green.opacity(0.2))
                        .frame(width: 36, height: 36)
                        .overlay {
                            Image(systemName: balance.owesMore ? "arrow.up.circle.fill" : "arrow.down.circle.fill")
                                .foregroundStyle(balance.owesMore ? .orange : .green)
                        }
                    Text(balance.name)
                        .font(.subheadline)
                    Spacer()
                    VStack(alignment: .trailing, spacing: 2) {
                        Text(formatAmount(abs(balance.balance)))
                            .font(.subheadline.bold())
                            .foregroundStyle(balance.owesMore ? .orange : .green)
                        Text(balance.owesMore ? "doit" : "à recevoir")
                            .font(.caption2)
                            .foregroundStyle(.secondary)
                    }
                }
            }
        }
        .padding()
        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 16))
    }

    // MARK: - Detail button

    private var detailButton: some View {
        Button {
            navigateToDetail = true
        } label: {
            HStack {
                Image(systemName: "list.bullet.rectangle.fill")
                Text("Voir toutes les dépenses")
                    .fontWeight(.semibold)
                Spacer()
                Image(systemName: "chevron.right")
                    .font(.caption)
            }
            .padding()
            .frame(maxWidth: .infinity)
            .background(.blue.opacity(0.1), in: RoundedRectangle(cornerRadius: 12))
            .foregroundStyle(.blue)
        }
    }

    // MARK: - Helpers

    private func formatAmount(_ amount: Double) -> String {
        String(format: "%.2f €", amount)
    }
}

// MARK: - BudgetCategoryRow

struct BudgetCategoryRow: View {
    let model: BudgetCategoryModel

    var body: some View {
        VStack(spacing: 6) {
            HStack {
                Image(systemName: model.iconName)
                    .foregroundStyle(.blue)
                    .frame(width: 20)
                Text(model.displayName)
                    .font(.subheadline)
                Spacer()
                VStack(alignment: .trailing, spacing: 2) {
                    Text(String(format: "%.2f €", model.actual))
                        .font(.subheadline.bold())
                        .foregroundStyle(model.isOverBudget ? .red : .primary)
                    Text("/ \(String(format: "%.2f €", model.estimated))")
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                }
            }
            ProgressView(value: model.usagePercentage)
                .tint(model.isOverBudget ? .red : .blue)
        }
    }
}

// MARK: - Preview

#Preview {
    BudgetOverviewView(eventId: "preview-event-id")
}
