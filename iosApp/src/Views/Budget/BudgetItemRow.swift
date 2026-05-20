import SwiftUI

// MARK: - BudgetItemRow

struct BudgetItemRow: View {
    let item: BudgetItemModel
    let onMarkAsPaid: () -> Void

    private var categoryUI: BudgetCategoryUI {
        BudgetCategoryUI.from(categoryName: item.categoryName)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack(alignment: .top) {
                // Paid indicator
                Image(systemName: item.isPaid ? "checkmark.circle.fill" : "circle")
                    .foregroundStyle(item.isPaid ? .green : .secondary)
                    .font(.system(size: 18))
                    .onTapGesture {
                        if !item.isPaid { onMarkAsPaid() }
                    }

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
                    }
                }

                Spacer()

                VStack(alignment: .trailing, spacing: 2) {
                    // Actual cost (if paid) or estimated
                    if item.isPaid && item.actualCost > 0 {
                        Text(String(format: "%.2f €", item.actualCost))
                            .font(.subheadline.bold())
                            .foregroundStyle(.green)
                        Text("payé")
                            .font(.caption2)
                            .foregroundStyle(.secondary)
                    } else {
                        Text(String(format: "%.2f €", item.estimatedCost))
                            .font(.subheadline.bold())
                        Text("estimé")
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
                    Text("\(item.sharedBy.count) personnes · \(String(format: "%.2f €", item.costPerPerson))/pers.")
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

#Preview {
    List {
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
