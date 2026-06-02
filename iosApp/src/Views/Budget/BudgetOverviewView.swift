import SwiftUI
import Shared

// MARK: - BudgetOverviewView

struct BudgetOverviewView: View {
    let eventId: String

    @StateObject private var viewModel: BudgetViewModel
    @State private var navigateToDetail = false
    private let previewCategories: [BudgetCategoryModel]?
    private let previewBalances: [ParticipantBalanceModel]?
    private let previewParticipantCount: Int?

    init(eventId: String) {
        self.eventId = eventId
        self._viewModel = StateObject(wrappedValue: BudgetViewModel(eventId: eventId))
        self.previewCategories = nil
        self.previewBalances = nil
        self.previewParticipantCount = nil
    }

#if DEBUG
    init(
        eventId: String,
        previewCategories: [BudgetCategoryModel],
        previewBalances: [ParticipantBalanceModel],
        previewParticipantCount: Int
    ) {
        self.eventId = eventId
        self._viewModel = StateObject(wrappedValue: BudgetViewModel(eventId: eventId))
        self.previewCategories = previewCategories
        self.previewBalances = previewBalances
        self.previewParticipantCount = previewParticipantCount
    }
#endif

    var body: some View {
        NavigationStack {
            Group {
                if shouldShowLoading {
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
                BudgetSyncBanner(pendingSync: viewModel.pendingSync, isOnline: viewModel.isOnline)
                summaryCard
                if !categoryModels.isEmpty {
                    categoryBreakdownCard
                }
                if !participantBalances.isEmpty {
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
                if isOverBudget {
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
                    Text(String(format: "%.0f%%", budgetUsagePercentage * 100))
                        .font(.caption.bold())
                        .foregroundStyle(isOverBudget ? .red : .primary)
                }
                ProgressView(value: budgetUsagePercentage)
                    .tint(isOverBudget ? .red : .blue)
            }

            Divider()

            // Amounts grid
            HStack(spacing: 0) {
                amountCell(
                    title: "Estimé",
                    amount: totalEstimated,
                    color: .secondary
                )
                Divider().frame(height: 44)
                amountCell(
                    title: "Réel",
                    amount: totalActual,
                    color: isOverBudget ? .red : .green
                )
                Divider().frame(height: 44)
                amountCell(
                    title: "Restant",
                    amount: remainingBudget,
                    color: isOverBudget ? .red : .blue
                )
            }

            if participantCount > 0 {
                Divider()
                HStack {
                    Image(systemName: "person.2.fill")
                        .foregroundStyle(.secondary)
                        .font(.caption)
                    Text("Coût estimé par personne")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    Spacer()
                    Text(formatAmount(costPerPerson))
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

            ForEach(categoryModels) { categoryModel in
                BudgetCategoryRow(model: categoryModel)
                if categoryModel.id != categoryModels.last?.id {
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

            ForEach(participantBalances) { balance in
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

    private var isPreviewing: Bool {
        previewCategories != nil
    }

    private var shouldShowLoading: Bool {
        !isPreviewing && viewModel.isLoading
    }

    private var categoryModels: [BudgetCategoryModel] {
        previewCategories ?? viewModel.categoryModels
    }

    private var participantBalances: [ParticipantBalanceModel] {
        previewBalances ?? viewModel.participantBalances
    }

    private var participantCount: Int {
        previewParticipantCount ?? viewModel.participantCount
    }

    private var totalEstimated: Double {
        isPreviewing ? categoryModels.reduce(0) { $0 + $1.estimated } : viewModel.totalEstimated
    }

    private var totalActual: Double {
        isPreviewing ? categoryModels.reduce(0) { $0 + $1.actual } : viewModel.totalActual
    }

    private var budgetUsagePercentage: Double {
        guard totalEstimated > 0 else { return 0 }
        return min(totalActual / totalEstimated, 1.0)
    }

    private var isOverBudget: Bool {
        totalActual > totalEstimated
    }

    private var remainingBudget: Double {
        totalEstimated - totalActual
    }

    private var costPerPerson: Double {
        guard participantCount > 0 else { return totalEstimated }
        return totalEstimated / Double(participantCount)
    }
}

struct BudgetSyncBanner: View {
    let pendingSync: Bool
    let isOnline: Bool

    var body: some View {
        if pendingSync || !isOnline {
            Label(
                pendingSync ? "Modifications locales en attente d'envoi" : "Données locales disponibles hors ligne",
                systemImage: "arrow.triangle.2.circlepath"
            )
            .font(.footnote.weight(.semibold))
            .padding(12)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(.yellow.opacity(0.16), in: RoundedRectangle(cornerRadius: 12))
        }
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

#if DEBUG
#Preview("Budget Overview - Light") {
    BudgetOverviewView(
        eventId: "preview-event-id",
        previewCategories: BudgetFactory.categoryModels,
        previewBalances: BudgetFactory.participantBalances,
        previewParticipantCount: 3
    )
    .preferredColorScheme(.light)
}

#Preview("Budget Overview - Dark") {
    BudgetOverviewView(
        eventId: "preview-event-id",
        previewCategories: BudgetFactory.categoryModels,
        previewBalances: BudgetFactory.participantBalances,
        previewParticipantCount: 3
    )
    .preferredColorScheme(.dark)
}
#endif
