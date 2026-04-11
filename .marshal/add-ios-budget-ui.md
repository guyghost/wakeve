# iOS Budget UI — add-ios-budget-ui

Implémenter les vues SwiftUI de gestion budgétaire pour iOS, en parité
fonctionnelle avec l'Android existant (BudgetOverviewScreen + BudgetDetailScreen).

## Context
- Shared KMP: `BudgetRepository.kt`, `BudgetCalculator.kt`, `BudgetModels.kt` — prêts
- Android référence: `BudgetOverviewScreen.kt` (461 LOC), `BudgetDetailScreen.kt` (706 LOC)
- iOS cible: `iosApp/src/Views/`, `iosApp/src/ViewModels/`
- ContentView.swift: remplacer les `"Budget Overview - Coming Soon"` / `"Budget Detail - Coming Soon"`
- Design system iOS: Liquid Glass (iOS 18), look au dossier `iosApp/src/` pour patterns existants

## Goals
- BudgetViewModel.swift connecté au BudgetRepository KMP
- BudgetOverviewView.swift: résumé total, catégories, progression, soldes participants
- BudgetDetailView.swift: liste items groupés, swipe-to-delete, FAB ajout
- BudgetItemRow.swift: composant ligne réutilisable
- AddBudgetItemSheet.swift: sheet d'ajout dépense
- Branchement ContentView.swift (remplacer "Coming Soon")
- Tests XCTest pour ViewModel
- Non-régression: 1158 tests JVM passent toujours

## Pipeline Checklist
- [ ] @codegen — BudgetViewModel.swift
- [ ] @codegen — BudgetOverviewView.swift
- [ ] @codegen — BudgetDetailView.swift + BudgetItemRow.swift + AddBudgetItemSheet.swift
- [ ] @codegen — Branchement ContentView.swift
- [ ] @tests — BudgetViewModelTests.swift
- [ ] @integrator — Cohérence Liquid Glass + nettoyage
- [ ] @validator — FC&IS + KMP interop patterns
- [ ] @review — Verdict final

## Verification
```bash
# Build iOS
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp \
  -destination 'platform=iOS Simulator,name=iPhone 16 Pro' \
  build 2>&1 | grep -E "error:|warning:|BUILD"

# Non-régression JVM
./gradlew shared:jvmTest --no-configuration-cache 2>&1 | tail -5
```

## Notes
- Suivre les patterns SwiftUI existants (ex: `ScenarioDetailView.swift`, `MealPlanningSheets.swift`)
- Observer la navigation via `NavigationPath` / `WakeveTab` dans `ContentView.swift`
- `BudgetRepository` est un objet KMP — l'instancier via `SharedModule` ou DI existante
- Liquid Glass: utiliser `.glassEffect()` si disponible sur iOS 18, fallback sur `.ultraThinMaterial`
