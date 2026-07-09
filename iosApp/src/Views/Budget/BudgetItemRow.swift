import SwiftUI

// MARK: - BudgetItemRow

struct BudgetItemRow: View {
    @Environment(\.colorScheme) private var colorScheme
    let item: BudgetItemModel
    let onMarkAsPaid: () -> Void

    private var categoryUI: BudgetCategoryUI {
        BudgetCategoryUI.from(categoryName: item.categoryName)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack(alignment: .top) {
                // Paid indicator
                Button {
                        if !item.isPaid { onMarkAsPaid() }
                } label: {
                    Image(systemName: item.isPaid ? "checkmark.circle.fill" : "circle")
                        .foregroundStyle(item.isPaid ? SemanticColor.confirmation(for: colorScheme) : SemanticColor.secondaryText(for: colorScheme))
                        .font(.system(size: 18))
                    }
                .buttonStyle(.plain)
                .frame(minWidth: 44, minHeight: 44)
                .disabled(item.isPaid)

                VStack(alignment: .leading, spacing: 2) {
                    Text(item.name)
                        .font(.subheadline)
                        .strikethrough(item.isPaid, color: .secondary)
                        .foregroundStyle(item.isPaid ? .secondary : .primary)

                    if !item.description.isEmpty {
                        Text(item.description)
                            .font(.caption)
                            .foregroundStyle(.secondary)
                            .lineLimit(1)
                            .minimumScaleFactor(0.78)
                    }
                }

                Spacer()

                VStack(alignment: .trailing, spacing: 2) {
                    // Actual cost (if paid) or estimated
                    if item.isPaid && item.actualCost > 0 {
                        Text(String(format: "%.2f €", item.actualCost))
                            .font(.subheadline.bold())
                            .foregroundStyle(SemanticColor.confirmation(for: colorScheme))
                        Text(String(localized: "budget.item.paid"))
                            .font(.caption2)
                            .foregroundStyle(.secondary)
                    } else {
                        Text(String(format: "%.2f €", item.estimatedCost))
                            .font(.subheadline.bold())
                        Text(String(localized: "budget.item.estimated"))
                            .font(.caption2)
                            .foregroundStyle(.secondary)
                    }
                }
            }

            // Shared cost info
            if !item.sharedBy.isEmpty {
                HStack(spacing: 4) {
                    Image(systemName: "person.2")
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                    Text(String(
                        format: String(localized: "budget.item.shared_cost_format"),
                        Int64(item.sharedBy.count),
                        String(format: "%.2f €", item.costPerPerson)
                    ))
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                }
                .padding(.leading, 26)
            }
        }
        .padding(.vertical, 2)
    }
}

// MARK: - Preview

#Preview("Budget Item Row - Light") {
    BudgetItemRowPreviewContent()
        .preferredColorScheme(.light)
}

#Preview("Budget Item Row - Dark") {
    BudgetItemRowPreviewContent()
        .preferredColorScheme(.dark)
}

private struct BudgetItemRowPreviewContent: View {
    var body: some View {
        VStack(spacing: WakeveTheme.Spacing.md) {
            WakeveContentCard(prominence: .regular, cornerRadius: WakeveTheme.Radius.xl, padding: WakeveTheme.Spacing.md) {
                BudgetItemRow(
                    item: BudgetItemModel(
                        id: "1",
                        budgetId: "b1",
                        categoryName: "TRANSPORT",
                        name: "Train Paris-Lyon",
                        description: "TGV aller-retour",
                        estimatedCost: 89.0,
                        actualCost: 0,
                        isPaid: false,
                        paidBy: nil,
                        sharedBy: ["u1", "u2", "u3"],
                        notes: "",
                        createdAt: "",
                        updatedAt: ""
                    ),
                    onMarkAsPaid: {}
                )
            }

            WakeveContentCard(prominence: .regular, cornerRadius: WakeveTheme.Radius.xl, padding: WakeveTheme.Spacing.md) {
                BudgetItemRow(
                    item: BudgetItemModel(
                        id: "2",
                        budgetId: "b1",
                        categoryName: "ACCOMMODATION",
                        name: "Airbnb Lyon",
                        description: "3 nuits",
                        estimatedCost: 450.0,
                        actualCost: 420.0,
                        isPaid: true,
                        paidBy: "u1",
                        sharedBy: ["u1", "u2"],
                        notes: "",
                        createdAt: "",
                        updatedAt: ""
                    ),
                    onMarkAsPaid: {}
                )
            }
        }
        .padding(WakeveTheme.Spacing.page)
        .background(WakeveScreenBackground(style: .grouped))
    }
}
