# Tasks: iOS Budget UI

## Implémentation

- [x] **@codegen** — `BudgetViewModel.swift` (ViewModel connecté au BudgetRepository KMP)
- [x] **@codegen** — `BudgetOverviewView.swift` (résumé, catégories, progression, soldes)
- [x] **@codegen** — `BudgetDetailView.swift` (liste items, groupement par catégorie)
- [x] **@codegen** — `BudgetItemRow.swift` (composant ligne)
- [x] **@codegen** — `AddBudgetItemSheet.swift` (sheet ajout dépense)
- [x] **@codegen** — Branchement `ContentView.swift` (remplacer les "Coming Soon")

## Tests

- [x] **@tests** — `BudgetViewModelTests.swift` (état chargement, calculs, actions)
- [ ] **@tests** — `BudgetOverviewViewTests.swift` (rendu, catégories, navigation)

## Intégration & Validation

- [x] **@integrator** — Vérification cohérence avec design system iOS (Liquid Glass)
- [x] **@validator** — Vérification FC&IS et patterns KMP interop
- [x] **@review** — Verdict final

## Verification

```bash
# Build iOS (vérifie compilation Swift)
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'platform=iOS Simulator,name=iPhone 16 Pro' build 2>&1 | tail -5

# Tests JVM (non-régression)
./gradlew shared:jvmTest --no-configuration-cache
```
