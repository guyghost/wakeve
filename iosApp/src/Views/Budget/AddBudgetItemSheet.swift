import SwiftUI
import Shared

// MARK: - AddBudgetItemSheet

struct AddBudgetItemSheet: View {
    let onSave: (String, String, BudgetCategoryUI, Double, [String]) -> Void

    @Environment(\.dismiss) private var dismiss
    @Environment(\.colorScheme) private var colorScheme

    @State private var name = ""
    @State private var description = ""
    @State private var selectedCategory: BudgetCategoryUI = .other
    @State private var estimatedCostText = ""

    @State private var showValidationError = false
    @State private var validationMessage = ""

    var body: some View {
        NavigationStack {
            ScrollView(showsIndicators: false) {
                VStack(alignment: .leading, spacing: WakeveTheme.Spacing.lg) {
                    WakeveContentCard(prominence: .prominent, cornerRadius: WakeveTheme.Radius.xl, padding: WakeveTheme.Spacing.lg) {
                        VStack(alignment: .leading, spacing: WakeveTheme.Spacing.xs) {
                            Label(String(localized: "budget.add_sheet.title"), systemImage: "eurosign.circle.fill")
                                .font(WakeveTheme.Typography.title2)
                                .foregroundStyle(primaryText)

                            Text(String(localized: "budget.add_sheet.subtitle"))
                                .font(WakeveTheme.Typography.callout)
                                .foregroundStyle(secondaryText)
                                .fixedSize(horizontal: false, vertical: true)
                        }
                    }

                    WakeveContentCard(prominence: .regular, cornerRadius: WakeveTheme.Radius.xl, padding: WakeveTheme.Spacing.lg) {
                        VStack(alignment: .leading, spacing: WakeveTheme.Spacing.md) {
                            budgetField(
                                title: String(localized: "budget.add_sheet.name"),
                                text: $name,
                                systemImage: "textformat",
                                placeholder: String(localized: "budget.add_sheet.name_placeholder")
                            )

                            budgetField(
                                title: String(localized: "budget.add_sheet.description"),
                                text: $description,
                                systemImage: "text.alignleft",
                                placeholder: String(localized: "budget.add_sheet.description_placeholder"),
                                axis: .vertical
                            )
                        }
                    }

                    WakeveContentCard(prominence: .regular, cornerRadius: WakeveTheme.Radius.xl, padding: WakeveTheme.Spacing.lg) {
                        VStack(alignment: .leading, spacing: WakeveTheme.Spacing.md) {
                            Text(String(localized: "budget.add_sheet.category"))
                                .font(WakeveTheme.Typography.bodySemibold)
                                .foregroundStyle(primaryText)

                            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: WakeveTheme.Spacing.sm) {
                                ForEach(BudgetCategoryUI.allCases) { category in
                                    BudgetCategoryOptionCard(
                                        category: category,
                                        isSelected: selectedCategory == category,
                                        action: {
                                            selectedCategory = category
                                            WakeveHaptics.selection()
                                        }
                                    )
                                }
                            }
                        }
                    }

                    WakeveContentCard(prominence: .regular, cornerRadius: WakeveTheme.Radius.xl, padding: WakeveTheme.Spacing.lg) {
                        VStack(alignment: .leading, spacing: WakeveTheme.Spacing.md) {
                            budgetField(
                                title: String(localized: "budget.add_sheet.estimated_amount"),
                                text: $estimatedCostText,
                                systemImage: "eurosign",
                                placeholder: "0",
                                keyboardType: .decimalPad,
                                trailingText: "EUR"
                            )

                            if showValidationError {
                                Label(validationMessage, systemImage: "exclamationmark.triangle.fill")
                                    .font(WakeveTheme.Typography.caption)
                                    .foregroundStyle(WakeveTheme.ColorToken.destructive(for: colorScheme))
                                    .fixedSize(horizontal: false, vertical: true)
                            }
                        }
                    }

                    WakeveActionButton(
                        String(localized: "budget.add_sheet.add_action"),
                        systemImage: "plus.circle.fill",
                        variant: .primary,
                        action: save
                    )
                }
                .padding(WakeveTheme.Spacing.page)
            }
            .background(WakeveScreenBackground(style: .grouped))
            .navigationTitle(String(localized: "budget.add_sheet.title"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(String(localized: "common.cancel")) { dismiss() }
                }
            }
        }
    }

    private func budgetField(
        title: String,
        text: Binding<String>,
        systemImage: String,
        placeholder: String,
        keyboardType: UIKeyboardType = .default,
        axis: Axis = .horizontal,
        trailingText: String? = nil
    ) -> some View {
        VStack(alignment: .leading, spacing: WakeveTheme.Spacing.xs) {
            Text(title)
                .font(WakeveTheme.Typography.caption)
                .foregroundStyle(secondaryText)
                .textCase(.uppercase)

            HStack(alignment: axis == .vertical ? .top : .center, spacing: WakeveTheme.Spacing.sm) {
                Image(systemName: systemImage)
                    .font(.headline.weight(.bold))
                    .foregroundStyle(WakeveTheme.ColorToken.accent(for: colorScheme))
                    .frame(width: 34, height: 34)
                    .background(WakeveTheme.ColorToken.controlFill(for: colorScheme))
                    .clipShape(Circle())

                TextField(placeholder, text: text, axis: axis)
                    .font(WakeveTheme.Typography.body)
                    .foregroundStyle(primaryText)
                    .keyboardType(keyboardType)
                    .textFieldStyle(.plain)
                    .lineLimit(axis == .vertical ? 3...5 : 1...1)
                    .autocorrectionDisabled()

                if let trailingText {
                    Text(trailingText)
                        .font(WakeveTheme.Typography.bodySemibold)
                        .foregroundStyle(secondaryText)
                }
            }
            .padding(WakeveTheme.Spacing.md)
            .background(.thinMaterial, in: RoundedRectangle(cornerRadius: WakeveTheme.Radius.md, style: .continuous))
        }
    }

    private var primaryText: Color {
        WakeveTheme.ColorToken.primaryText(for: colorScheme)
    }

    private var secondaryText: Color {
        WakeveTheme.ColorToken.secondaryText(for: colorScheme)
    }

    // MARK: - Validation & Save

    private func save() {
        guard !name.trimmingCharacters(in: .whitespaces).isEmpty else {
            validationMessage = String(localized: "budget.add_sheet.validation.name_required")
            showValidationError = true
            return
        }

        guard let cost = Double(estimatedCostText.replacingOccurrences(of: ",", with: ".")),
              cost > 0 else {
            validationMessage = String(localized: "budget.add_sheet.validation.amount_required")
            showValidationError = true
            return
        }

        onSave(
            name.trimmingCharacters(in: .whitespaces),
            description.trimmingCharacters(in: .whitespaces),
            selectedCategory,
            cost,
            []  // sharedBy — future enhancement with participant picker
        )

        dismiss()
    }
}

private struct BudgetCategoryOptionCard: View {
    @Environment(\.colorScheme) private var colorScheme

    let category: BudgetCategoryUI
    let isSelected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: WakeveTheme.Spacing.sm) {
                Image(systemName: category.iconName)
                    .font(.headline.weight(.bold))
                    .foregroundStyle(isSelected ? .white : WakeveTheme.ColorToken.accent(for: colorScheme))
                    .frame(width: 34, height: 34)
                    .background(isSelected ? WakeveTheme.ColorToken.accent(for: colorScheme) : WakeveTheme.ColorToken.controlFill(for: colorScheme))
                    .clipShape(Circle())

                Text(category.displayName)
                    .font(WakeveTheme.Typography.callout.weight(.semibold))
                    .foregroundStyle(WakeveTheme.ColorToken.primaryText(for: colorScheme))
                    .lineLimit(1)
                    .minimumScaleFactor(0.78)

                Spacer(minLength: 0)
            }
            .padding(WakeveTheme.Spacing.sm)
            .background(
                isSelected
                    ? WakeveTheme.ColorToken.accent(for: colorScheme).opacity(0.14)
                    : WakeveTheme.ColorToken.controlFill(for: colorScheme).opacity(0.72),
                in: RoundedRectangle(cornerRadius: WakeveTheme.Radius.lg, style: .continuous)
            )
            .overlay {
                RoundedRectangle(cornerRadius: WakeveTheme.Radius.lg, style: .continuous)
                    .stroke(isSelected ? WakeveTheme.ColorToken.accent(for: colorScheme) : Color.clear, lineWidth: 1.5)
            }
        }
        .buttonStyle(.plain)
        .accessibilityLabel(category.displayName)
    }
}

// MARK: - Preview

#Preview("Add Budget Item - Light") {
    AddBudgetItemSheet { name, description, category, cost, sharedBy in
        debugLog("Save: \(name) - \(cost)€ - \(category.displayName)")
    }
    .preferredColorScheme(.light)
}

#Preview("Add Budget Item - Dark") {
    AddBudgetItemSheet { name, description, category, cost, sharedBy in
        debugLog("Save: \(name) - \(cost)€ - \(category.displayName)")
    }
    .preferredColorScheme(.dark)
}
