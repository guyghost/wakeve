# Context: Phase 6 Performance - Pagination (Ralph Mode)

## Ralph Mode Configuration
- **Enabled**: true
- **Max Iterations**: 10
- **Current Iteration**: 2
- **Mode**: TDD (Test-Driven Development)

## Objective
Implémenter la pagination pour toutes les listes d'événements (Android/iOS) avec:
- LazyColumn/LazyVStack avec pagination
- Taille de page: 50 items
- Keys stables pour recyclage efficace
- Loading skeletons
- Gestion des erreurs

## Current State
- ✅ Database indexes completed (15 indexes added)
- ✅ Security fixes committed
- ✅ 730/734 tests passing
- 🔄 P0.2: Pagination Implementation - GREEN Phase Complete

## Technical Stack
- **Android**: Jetpack Compose + LazyColumn
- **iOS**: SwiftUI + LazyVStack
- **Backend**: Already supports pagination (SQLDelight)
- **Shared**: Kotlin Multiplatform

## Acceptance Criteria (from Phase 6 specs)
- [x] Pagination avec load incremental (scroll → load next page)
- [x] Taille de page configurable (default: 50 items)
- [x] Keys stables pour LazyColumn/LazyVStack (item.id)
- [x] État de chargement affiché (Loading skeletons)
- [x] Gestion des erreurs de chargement
- [ ] Tests UI pour le scroll infini (blocked by Compose test dependency issues)
- [ ] Performance: <100ms pour charger 50 items (to be validated)

## Files to Modify
1. ~~`wakeveApp/src/commonMain/kotlin/com/guyghost/wakeve/ui/event/EventListScreen.kt`~~ (PaginatedEventList.kt created instead)
2. [x] `wakeveApp/src/commonMain/kotlin/com/guyghost/wakeve/repository/EventRepository.kt` (add pagination)
3. `wakeveApp/wakeveApp/Views/EventListView.swift` (not yet implemented)

## Artifacts Produced

### Core Layer
- `shared/src/commonMain/kotlin/com/guyghost/wakeve/repository/OrderBy.kt` - NEW
- `shared/src/commonMain/kotlin/com/guyghost/wakeve/EventRepository.kt` - MODIFIED (added getEventsPaginated)
- `shared/src/commonMain/kotlin/com/guyghost/wakeve/DatabaseEventRepository.kt` - MODIFIED (added getEventsPaginated implementation)
- `shared/src/commonMain/sqldelight/com/guyghost/wakeve/Event.sq` - MODIFIED (added selectPaginated query)

### Android Presentation Layer
- `wakeveApp/src/commonMain/kotlin/com/guyghost/wakeve/ui/event/PaginatedEventViewModel.kt` - NEW
- `wakeveApp/src/commonMain/kotlin/com/guyghost/wakeve/ui/event/PaginatedEventList.kt` - NEW (includes EventCard, EmptyState, ErrorMessage)
- `wakeveApp/src/androidUnitTest/kotlin/com/guyghost/wakeve/ui/event/PaginatedEventListTest.kt` - MODIFIED (helper functions implemented)

## Technical Decisions

| Decision | Reason |
|-----------|---------|
| Use Flow<List<Event>> return type | Test contract requires reactive updates and proper async handling |
| OrderBy enum in separate file | Clean separation of concerns, reusable across repository implementations |
| IPaginatedEventViewModel interface | Enables easy testing with fake implementations |
| LazyColumn with derivedStateOf for scroll detection | Efficient infinite scroll triggering when user is 5 items from end |
| pageSize default of 50 | Matches spec requirements for reasonable chunk size |
| SQL ORDER BY with CASE statements | Flexible sorting based on orderBy parameter |
| GoogleSignInProvider fix | Added missing currentTime parameter to User.createAuthenticated |

## Inter-Agent Notes

### To @tests
7/7 repository tests passing. Android UI tests (PaginatedEventListTest.kt) have Compose testing import issues - `androidx.compose.ui.test.junit4` package not resolving. May need to move to androidInstrumentedTest or check dependencies. Shared code compiles successfully.

## TDD Approach
1. RED: Write failing tests for pagination
2. GREEN: Implement to pass tests
3. REFACTOR: Optimize and clean up

## Constraints
- Offline-first: Pagination must work offline
- FC&IS: Core logic pure, Shell handles pagination state
- Performance: <100ms per page load
