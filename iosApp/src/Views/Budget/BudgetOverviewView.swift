import SwiftUI
import Shared

// MARK: - BudgetOverviewView

struct BudgetOverviewView: View {
    @Environment(\.colorScheme) private var colorScheme

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
            .navigationTitle(String(localized: "budget.overview_title"))
            .navigationBarTitleDisplayMode(.large)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button {
                        navigateToDetail = true
                    } label: {
                        Image(systemName: "list.bullet")
                    }
                    .accessibilityLabel(String(localized: "budget.open_expenses"))
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
                .accessibilityLabel(String(localized: "common.loading"))
            Text(String(localized: "budget.loading"))
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    // MARK: - Error

    private func errorView(message: String) -> some View {
        VStack(spacing: 16) {
            Image(systemName: "exclamationmark.triangle.fill")
                .font(.largeTitle)
                .foregroundStyle(SemanticColor.warning(for: colorScheme))
            Text(message)
                .multilineTextAlignment(.center)
                .foregroundStyle(.secondary)
            WakeveActionButton(
                String(localized: "common.try_again"),
                systemImage: "arrow.clockwise",
                variant: .secondary
            ) {
                viewModel.load()
            }
        }
        .padding()
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(WakeveScreenBackground(style: .grouped))
    }

    // MARK: - Main content

    private var contentView: some View {
        ScrollView {
            LazyVStack(spacing: WakeveTheme.Spacing.md) {
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
            .padding(WakeveTheme.Spacing.md)
        }
        .background(WakeveScreenBackground(style: .grouped))
    }

    // MARK: - Summary card

    private var summaryCard: some View {
        WakeveContentCard(prominence: .prominent, cornerRadius: WakeveTheme.Radius.xl, padding: WakeveTheme.Spacing.md) {
        VStack(alignment: .leading, spacing: WakeveTheme.Spacing.md) {
            HStack {
                Image(systemName: "eurosign.circle.fill")
                    .foregroundStyle(WakeveTheme.ColorToken.accent(for: colorScheme))
                Text(String(localized: "budget.overview.summary"))
                    .font(WakeveTheme.Typography.section)
                Spacer()
                if isOverBudget {
                    Label(String(localized: "budget.overview.over_budget"), systemImage: "exclamationmark.circle.fill")
                        .font(WakeveTheme.Typography.caption)
                        .foregroundStyle(WakeveTheme.ColorToken.destructive(for: colorScheme))
                }
            }

            // Progress bar
            VStack(alignment: .leading, spacing: 6) {
                HStack {
                    Text(String(localized: "budget.overview.used"))
                        .font(WakeveTheme.Typography.caption)
                        .foregroundStyle(.secondary)
                    Spacer()
                    Text(String(format: "%.0f%%", budgetUsagePercentage * 100))
                        .font(.caption.bold())
                        .foregroundStyle(isOverBudget ? WakeveTheme.ColorToken.destructive(for: colorScheme) : WakeveTheme.ColorToken.primaryText(for: colorScheme))
                }
                ProgressView(value: budgetUsagePercentage)
                    .tint(isOverBudget ? WakeveTheme.ColorToken.destructive(for: colorScheme) : WakeveTheme.ColorToken.accent(for: colorScheme))
            }

            Divider()

            // Amounts grid
            HStack(spacing: 0) {
                amountCell(
                    title: String(localized: "budget.overview.estimated"),
                    amount: totalEstimated,
                    color: .secondary
                )
                Divider().frame(height: 44)
                amountCell(
                    title: String(localized: "budget.overview.actual"),
                    amount: totalActual,
                    color: isOverBudget ? WakeveTheme.ColorToken.destructive(for: colorScheme) : WakeveTheme.ColorToken.confirmation(for: colorScheme)
                )
                Divider().frame(height: 44)
                amountCell(
                    title: String(localized: "budget.overview.remaining"),
                    amount: remainingBudget,
                    color: isOverBudget ? WakeveTheme.ColorToken.destructive(for: colorScheme) : WakeveTheme.ColorToken.accent(for: colorScheme)
                )
            }

            if participantCount > 0 {
                Divider()
                HStack {
                    Image(systemName: "person.2.fill")
                        .foregroundStyle(.secondary)
                        .font(.caption)
                    Text(String(localized: "budget.overview.per_person"))
                        .font(WakeveTheme.Typography.caption)
                        .foregroundStyle(.secondary)
                    Spacer()
                    Text(formatAmount(costPerPerson))
                        .font(.caption.bold())
                }
            }
        }
        }
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
        WakeveContentCard(prominence: .regular, cornerRadius: WakeveTheme.Radius.xl, padding: WakeveTheme.Spacing.md) {
        VStack(alignment: .leading, spacing: WakeveTheme.Spacing.sm) {
            HStack {
                Image(systemName: "chart.bar.fill")
                    .foregroundStyle(WakeveTheme.ColorToken.accent(for: colorScheme))
                Text(String(localized: "budget.overview.by_category"))
                    .font(WakeveTheme.Typography.section)
            }

            ForEach(categoryModels) { categoryModel in
                BudgetCategoryRow(model: categoryModel)
                if categoryModel.id != categoryModels.last?.id {
                    Divider()
                }
            }
        }
        }
    }

    // MARK: - Participant balances card

    private var participantBalancesCard: some View {
        WakeveContentCard(prominence: .regular, cornerRadius: WakeveTheme.Radius.xl, padding: WakeveTheme.Spacing.md) {
        VStack(alignment: .leading, spacing: WakeveTheme.Spacing.sm) {
            HStack {
                Image(systemName: "person.2.fill")
                    .foregroundStyle(WakeveTheme.ColorToken.confirmation(for: colorScheme))
                Text(String(localized: "budget.overview.participant_balances"))
                    .font(WakeveTheme.Typography.section)
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
                        Text(balance.owesMore ? String(localized: "budget.overview.owes") : String(localized: "budget.overview.to_receive"))
                            .font(.caption2)
                            .foregroundStyle(.secondary)
                    }
                }
            }
        }
        }
    }

    // MARK: - Detail button

    private var detailButton: some View {
        WakeveContentCard(prominence: .subtle, cornerRadius: WakeveTheme.Radius.lg, padding: 0) {
        Button {
            navigateToDetail = true
        } label: {
            HStack {
                Image(systemName: "list.bullet.rectangle.fill")
                Text(String(localized: "budget.overview.view_all_expenses"))
                    .fontWeight(.semibold)
                Spacer()
                Image(systemName: "chevron.right")
                    .font(.caption)
            }
            .padding(WakeveTheme.Spacing.md)
            .frame(maxWidth: .infinity)
            .foregroundStyle(WakeveTheme.ColorToken.accent(for: colorScheme))
        }
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
                pendingSync ? String(localized: "sync.pending_changes") : String(localized: "sync.offline_available"),
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
