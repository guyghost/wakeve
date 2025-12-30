# ScenarioDetailView: Migrating Callsites

**Version**: 1.0  
**Date**: 2025-12-29  
**Type**: Breaking Change (Repository Parameter Removed)

---

## Quick Summary

The `ScenarioDetailView` signature has changed:

```swift
// ❌ OLD SIGNATURE (repository-direct)
ScenarioDetailView(
    scenarioId: String,
    eventId: String,
    repository: ScenarioRepository,          // ❌ REMOVED
    isOrganizer: Bool,
    currentUserId: String,
    currentUserName: String,
    onBack: () -> Void,
    onDeleted: () -> Void
)

// ✅ NEW SIGNATURE (state machine)
ScenarioDetailView(
    scenarioId: String,
    eventId: String,
    // repository removed ✅
    isOrganizer: Bool = false,
    currentUserId: String = "",
    currentUserName: String = "",
    onBack: @escaping () -> Void = {},
    onDeleted: @escaping () -> Void = {}
)
```

---

## Finding All Callsites

### Search for Calls in Xcode

```bash
# Find all references to ScenarioDetailView initialization
grep -r "ScenarioDetailView(" iosApp --include="*.swift" | grep -v "struct ScenarioDetailView"

# Or use Xcode's Find Navigator:
# ⌘ + Shift + F → "ScenarioDetailView(" → Find All
```

### Expected Locations

```
├── iosApp/iosApp/Views/
│   ├── ScenarioManagementView.swift
│   ├── ScenarioListView.swift
│   └── (other navigation sources)
│
├── iosApp/iosApp/ViewModels/
│   └── (might have preview code)
│
└── iosApp/iosApp/Preview Content/
    └── (SwiftUI previews)
```

---

## Migration Patterns

### Pattern 1: Basic Callsite (No Repository)

#### BEFORE
```swift
NavigationLink(destination: 
    ScenarioDetailView(
        scenarioId: scenario.id,
        eventId: eventId,
        repository: repository,                      // ❌ Remove this line
        isOrganizer: isOrganizer,
        currentUserId: currentUserId,
        currentUserName: currentUserName,
        onBack: { navPath.removeLast() },
        onDeleted: { loadScenarios() }
    )
) {
    ScenarioCard(scenario: scenario)
}
```

#### AFTER
```swift
NavigationLink(destination: 
    ScenarioDetailView(
        scenarioId: scenario.id,
        eventId: eventId,
        // repository removed ✅
        isOrganizer: isOrganizer,
        currentUserId: currentUserId,
        currentUserName: currentUserName,
        onBack: { navPath.removeLast() },
        onDeleted: { loadScenarios() }
    )
) {
    ScenarioCard(scenario: scenario)
}
```

**Changes:**
- ❌ Delete: `repository: repository,`

---

### Pattern 2: Minimal Callsite (Using Defaults)

#### BEFORE
```swift
ScenarioDetailView(
    scenarioId: "scenario-1",
    eventId: "event-1",
    repository: ScenarioRepository(),        // ❌ Remove
    isOrganizer: true,
    currentUserId: "user-1",
    currentUserName: "John",
    onBack: { },
    onDeleted: { }
)
```

#### AFTER
```swift
ScenarioDetailView(
    scenarioId: "scenario-1",
    eventId: "event-1",
    // repository removed ✅
    isOrganizer: true,
    currentUserId: "user-1",
    currentUserName: "John",
    onBack: { },
    onDeleted: { }
)

// Or with defaults:
ScenarioDetailView(scenarioId: "scenario-1", eventId: "event-1")
```

**Changes:**
- ❌ Delete: `repository: ScenarioRepository(),`
- ✅ Add default values from init

---

### Pattern 3: SwiftUI Preview

#### BEFORE
```swift
#if DEBUG
struct ScenarioDetailViewPreview: PreviewProvider {
    static var previews: some View {
        ScenarioDetailView(
            scenarioId: "preview-1",
            eventId: "event-1",
            repository: ScenarioRepository(),        // ❌ Remove
            isOrganizer: true,
            currentUserId: "user-1",
            currentUserName: "Alice",
            onBack: { },
            onDeleted: { }
        )
    }
}
#endif
```

#### AFTER
```swift
#if DEBUG
struct ScenarioDetailViewPreview: PreviewProvider {
    static var previews: some View {
        Group {
            ScenarioDetailView(
                scenarioId: "preview-1",
                eventId: "event-1",
                // repository removed ✅
                isOrganizer: true,
                currentUserId: "user-1",
                currentUserName: "Alice",
                onBack: { },
                onDeleted: { }
            )
            .preferredColorScheme(.light)
            .previewDisplayName("Light Mode")
            
            ScenarioDetailView(
                scenarioId: "preview-1",
                eventId: "event-1",
                isOrganizer: false
            )
            .preferredColorScheme(.dark)
            .previewDisplayName("Dark Mode - Participant")
        }
    }
}
#endif
```

**Changes:**
- ❌ Delete: `repository: ScenarioRepository(),`
- ✅ Add default parameters
- ✅ Group multiple previews

---

### Pattern 4: Navigation with State

#### BEFORE
```swift
@State private var selectedScenario: Scenario? = nil

var body: some View {
    NavigationStack {
        List(scenarios) { scenario in
            NavigationLink(
                destination: ScenarioDetailView(
                    scenarioId: scenario.id,
                    eventId: eventId,
                    repository: repository,          // ❌ Remove
                    isOrganizer: isOrganizer,
                    currentUserId: currentUserId,
                    currentUserName: currentUserName,
                    onBack: { selectedScenario = nil },
                    onDeleted: { reload() }
                ),
                tag: scenario,
                selection: $selectedScenario
            ) {
                ScenarioRow(scenario: scenario)
            }
        }
    }
}
```

#### AFTER
```swift
@State private var selectedScenario: Scenario? = nil

var body: some View {
    NavigationStack {
        List(scenarios) { scenario in
            NavigationLink(
                destination: ScenarioDetailView(
                    scenarioId: scenario.id,
                    eventId: eventId,
                    // repository removed ✅
                    isOrganizer: isOrganizer,
                    currentUserId: currentUserId,
                    currentUserName: currentUserName,
                    onBack: { selectedScenario = nil },
                    onDeleted: { reload() }
                ),
                tag: scenario,
                selection: $selectedScenario
            ) {
                ScenarioRow(scenario: scenario)
            }
        }
    }
}
```

**Changes:**
- ❌ Delete: `repository: repository,`
- No other changes needed

---

### Pattern 5: If You Were Storing Repository Reference

#### BEFORE
```swift
class ScenarioListViewModel: ObservableObject {
    @Published var scenarios: [Scenario] = []
    let repository: ScenarioRepository       // ❌ No longer needed
    
    init(repository: ScenarioRepository) {
        self.repository = repository
    }
    
    func loadScenarios() {
        Task {
            self.scenarios = try await repository.getScenarios()  // ❌ Direct call
        }
    }
}

struct ScenarioListView: View {
    @StateObject var viewModel: ScenarioListViewModel
    
    var body: some View {
        NavigationLink(destination: 
            ScenarioDetailView(
                scenarioId: scenario.id,
                eventId: eventId,
                repository: viewModel.repository,    // ❌ Remove
                isOrganizer: isOrganizer,
                currentUserId: currentUserId,
                currentUserName: currentUserName,
                onBack: { },
                onDeleted: { viewModel.loadScenarios() }
            )
        ) {
            ScenarioRow(scenario: scenario)
        }
    }
}
```

#### AFTER
```swift
class ScenarioListViewModel: ObservableObject {
    @Published var scenarios: [Scenario] = []
    // repository removed ✅ - now handled by ScenarioDetailViewModel
    
    init() {  // ✅ No repository parameter needed
        loadScenarios()
    }
    
    func loadScenarios() {
        // Repository access is now via RepositoryProvider in ViewModels
        // See RepositoryProvider.swift
    }
}

struct ScenarioListView: View {
    @StateObject var viewModel: ScenarioListViewModel
    
    var body: some View {
        NavigationLink(destination: 
            ScenarioDetailView(
                scenarioId: scenario.id,
                eventId: eventId,
                // repository removed ✅
                isOrganizer: isOrganizer,
                currentUserId: currentUserId,
                currentUserName: currentUserName,
                onBack: { },
                onDeleted: { viewModel.loadScenarios() }
            )
        ) {
            ScenarioRow(scenario: scenario)
        }
    }
}
```

**Changes:**
- ❌ Delete: `let repository: ScenarioRepository`
- ❌ Delete: Repository from init
- ✅ ViewModels now access repository via `RepositoryProvider.shared.database`

---

## Automated Migration Script

### Bash Script to Update Callsites

```bash
#!/bin/bash
# migrate-scenariodetailview.sh

FILE_PATTERN="*.swift"
SEARCH_PATTERN="repository: "
EXCLUDE_PATTERN="ScenarioRepository"

echo "Searching for ScenarioDetailView callsites..."

# Find all files with ScenarioDetailView calls
find iosApp -name "$FILE_PATTERN" -type f | while read file; do
    if grep -q "ScenarioDetailView(" "$file"; then
        echo "Processing: $file"
        
        # Count repository parameters
        repo_count=$(grep -o "repository: " "$file" | wc -l)
        
        if [ $repo_count -gt 0 ]; then
            echo "  Found $repo_count 'repository:' parameter(s)"
            echo "  Action: Remove 'repository: <value>,' line"
        fi
    fi
done

echo ""
echo "Manual review required in these files:"
grep -r "ScenarioDetailView(" iosApp --include="*.swift" | grep "repository:" | cut -d: -f1 | sort -u
```

### Run the script:
```bash
chmod +x migrate-scenariodetailview.sh
./migrate-scenariodetailview.sh
```

---

## Checklist for Each Callsite

For each location where `ScenarioDetailView` is called, verify:

- [ ] `repository:` parameter line **removed**
- [ ] All other parameters **remain the same**
- [ ] File compiles without errors
- [ ] Navigation flow still works
- [ ] Callbacks (`onBack`, `onDeleted`) still execute
- [ ] UI rendering unchanged

---

## Known Callsites

### 1. ScenarioManagementView.swift

**Location**: Tab or detail view that displays scenarios

```swift
NavigationLink(destination:
    ScenarioDetailView(
        scenarioId: scenario.id,
        eventId: eventId,
        repository: repository,           // ❌ REMOVE THIS LINE
        isOrganizer: organizer.id == currentUserId,
        currentUserId: currentUserId,
        currentUserName: currentUserName,
        onBack: { navPath.removeLast() },
        onDeleted: { loadScenarios() }
    )
)
```

**Action**:
```swift
NavigationLink(destination:
    ScenarioDetailView(
        scenarioId: scenario.id,
        eventId: eventId,
        // repository removed
        isOrganizer: organizer.id == currentUserId,
        currentUserId: currentUserId,
        currentUserName: currentUserName,
        onBack: { navPath.removeLast() },
        onDeleted: { loadScenarios() }
    )
)
```

---

### 2. ScenarioListView.swift (if exists)

**Location**: List of scenarios view

**Action**: Same as above - remove `repository:` parameter

---

### 3. Preview Files

**Location**: `+Preview.swift` or preview within struct

**Before:**
```swift
ScenarioDetailView(
    scenarioId: "preview-1",
    eventId: "event-1",
    repository: ScenarioRepository(),     // ❌ REMOVE
    isOrganizer: true,
    currentUserId: "user-1",
    currentUserName: "Alice",
    onBack: { },
    onDeleted: { }
)
```

**After:**
```swift
ScenarioDetailView(
    scenarioId: "preview-1",
    eventId: "event-1",
    isOrganizer: true,
    currentUserId: "user-1",
    currentUserName: "Alice",
    onBack: { },
    onDeleted: { }
)
```

---

## Testing After Migration

### 1. Build Verification

```bash
# Build the iOS app
xcodebuild build -scheme iosApp -destination 'generic/platform=iOS'

# Or in Xcode:
⌘ + B
```

### 2. Runtime Testing

For each callsite, test:

```swift
// Test 1: View loads
✓ Scenario detail screen displays

// Test 2: Navigation
✓ Back button returns to previous screen

// Test 3: Edit (organizer only)
✓ Edit button appears
✓ Edit form shows correctly
✓ Save updates scenario

// Test 4: Delete (organizer only)
✓ Delete button appears
✓ Confirmation dialog shows
✓ Deletion callback fires

// Test 5: Error handling
✓ Invalid scenario ID shows error state
```

### 3. Regression Testing

Run existing tests:
```bash
xcodebuild test -scheme iosApp -destination 'generic/platform=iOS'
```

---

## Common Issues & Solutions

### Issue 1: "Cannot find 'ScenarioDetailView' in scope"

**Cause**: Missing import

**Solution**:
```swift
import SwiftUI  // ✓ Required
import Shared   // ✓ Required for Scenario type
```

---

### Issue 2: "'ScenarioDetailView' requires that 'SomeType' conform to 'ObservableObject'"

**Cause**: Wrong parameter type

**Solution**:
Verify parameter types match:
```swift
scenarioId: String              // ✓ Not Int or UUID
eventId: String                 // ✓ Not Int or UUID
isOrganizer: Bool              // ✓ Not Int or String
currentUserId: String          // ✓ Not Int
currentUserName: String        // ✓ Not Int
onBack: () -> Void            // ✓ Not optional
onDeleted: () -> Void         // ✓ Not optional
```

---

### Issue 3: ViewModel Not Initializing

**Cause**: Shared module not available at runtime

**Solution**: Ensure `Shared` framework is linked:
1. Target Settings → Build Phases → Link Binary With Libraries
2. Verify `Shared` is checked

---

### Issue 4: "Repository not found in scope"

**Cause**: You forgot to remove the repository parameter

**Solution**:
```swift
// ❌ Wrong - this will fail
ScenarioDetailView(
    scenarioId: scenario.id,
    repository: repository,     // ❌ Remove this
    ...
)

// ✅ Correct
ScenarioDetailView(
    scenarioId: scenario.id,
    ...
)
```

---

## Verification Steps

### After Each Migration

```swift
// ✓ Step 1: Syntax check
// Xcode should show no red errors

// ✓ Step 2: Build check
xcodebuild build -scheme iosApp

// ✓ Step 3: Test the flow
// - Navigate to scenario detail
// - Go back
// - If organizer: edit and save
// - If organizer: delete with confirmation

// ✓ Step 4: Check callbacks
// - onBack is called when back button tapped
// - onDeleted is called after deletion

// ✓ Step 5: Verify state consistency
// - Scenario data loads
// - Voting results display
// - Comments section works
```

---

## Summary of Changes

| Item | Change |
|------|--------|
| **Removed Parameter** | `repository: ScenarioRepository` |
| **New State Management** | `@StateObject private var viewModel: ScenarioDetailViewModel` |
| **Repository Access** | Now internal to ViewModel |
| **Offline Support** | Built-in via state machine |
| **Breaking Change** | YES - parameter removed |
| **Migration Effort** | Low (remove 1 parameter per callsite) |
| **Testing Required** | YES - test each callsite |

---

## Rollback (If Needed)

If there are serious issues, you can revert:

```bash
# Revert specific file
git checkout iosApp/iosApp/Views/ScenarioDetailView.swift

# Or revert all changes
git reset --hard HEAD~1
```

Then re-apply the refactoring with fixes.

---

## Timeline

```
Phase 1: Preparation (5 min)
  ✓ Identify all callsites
  ✓ Plan migration strategy
  ✓ Create backup/branch

Phase 2: Migration (15-30 min)
  ✓ Remove repository parameters
  ✓ Run builds
  ✓ Fix any compilation errors

Phase 3: Testing (30-60 min)
  ✓ Test each callsite
  ✓ Test all user flows
  ✓ Run test suite

Phase 4: Review & Merge (10 min)
  ✓ Code review
  ✓ Final build
  ✓ Commit & merge to main
```

---

## Questions?

| Question | Answer |
|----------|--------|
| Do I need to update the repository? | No, it's handled internally by ViewModel |
| Will my tests break? | Only if they directly access ScenarioDetailView with old signature |
| Is this compatible with Android? | Yes, ViewModel uses shared state machine |
| Can I use the old signature? | No, repository parameter is removed |
| When should I migrate? | Before next feature build |

---

## Next Steps

1. **Locate all callsites** - Use grep command above
2. **Update each one** - Remove `repository:` parameter
3. **Build and test** - Verify compilation and runtime
4. **Commit** - `git commit -m "refactor: migrate ScenarioDetailView callsites to state machine"`
5. **Review** - Code review before merge

---

**Status**: ✅ Migration Guide Complete  
**Date**: 2025-12-29  
**Effort**: Low (< 1 hour total)  
**Risk**: Low (simple parameter removal)  
**Testing**: Required for each callsite
