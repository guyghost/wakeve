import SwiftUI
import Shared

// MARK: - AddBudgetItemSheet

struct AddBudgetItemSheet: View {
    let onSave: (String, String, BudgetCategoryUI, Double, [String]) -> Void

    @Environment(\.dismiss) private var dismiss

    @State private var name = ""
    @State private var description = ""
    @State private var selectedCategory: BudgetCategoryUI = .other
    @State private var estimatedCostText = ""

    @State private var showValidationError = false
    @State private var validationMessage = ""

    var body: some View {
        NavigationView {
            Form {
                // Basic info
                Section("Informations") {
                    TextField("Nom de la dépense *", text: $name)
                        .autocorrectionDisabled()

                    TextField("Description (optionnel)", text: $description)
                        .autocorrectionDisabled()
                }

                // Category
                Section("Catégorie") {
                    Picker("Catégorie", selection: $selectedCategory) {
                        ForEach(BudgetCategoryUI.allCases) { category in
                            Label(category.displayName, systemImage: category.iconName).tag(category)
                        }
                    }
                    .pickerStyle(.menu)
                }

                // Cost
                Section("Montant") {
                    HStack {
                        TextField("Coût estimé *", text: $estimatedCostText)
                            .keyboardType(.decimalPad)
                        Text("€")
                            .foregroundStyle(.secondary)
                    }
                }

                // Validation error
                if showValidationError {
                    Section {
                        Label(validationMessage, systemImage: "exclamationmark.triangle.fill")
                            .foregroundStyle(.red)
                            .font(.caption)
                    }
                }
            }
            .navigationTitle("Nouvelle dépense")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Annuler") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Ajouter") { save() }
                        .fontWeight(.semibold)
                }
            }
        }
    }

    // MARK: - Validation & Save

    private func save() {
        guard !name.trimmingCharacters(in: .whitespaces).isEmpty else {
            validationMessage = "Le nom est obligatoire."
            showValidationError = true
            return
        }

        guard let cost = Double(estimatedCostText.replacingOccurrences(of: ",", with: ".")),
              cost > 0 else {
            validationMessage = "Veuillez entrer un montant valide."
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

// MARK: - Preview

#Preview {
    AddBudgetItemSheet { name, description, category, cost, sharedBy in
        print("Save: \(name) - \(cost)€ - \(category.displayName)")
    }
}
