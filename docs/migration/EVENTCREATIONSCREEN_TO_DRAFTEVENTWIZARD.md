# Migration Guide: EventCreationScreen → DraftEventWizard

> **Version**: 1.0.0
> **Last Updated**: 2026-01-04
> **Status**: EventCreationScreen is @Deprecated

## Overview

`EventCreationScreen.kt` is **deprecated** and will be removed in a future major version. This guide explains why and how to migrate to `DraftEventWizard`.

## Why Migrate?

### Limitations of EventCreationScreen

1. **Single-Step Form**: All event details in one screen → overwhelming for users
2. **No Auto-Save**: Users must complete entire form before saving → data loss risk
3. **Limited Validation**: Only validates on final submission → poor UX
4. **No Progress Feedback**: Users don't know how much is left

### Benefits of DraftEventWizard

1. **Multi-Step Wizard**: 4 sequential steps → manageable, focused UX
2. **Auto-Save**: Data saved at each step → no data loss
3. **Real-Time Validation**: Validates at each step → immediate feedback
4. **Progress Indicator**: Visual progress bar → user knows status
5. **Better Accessibility**: Smaller screens, clearer navigation → improved TalkBack/VoiceOver
6. **Editing Support**: Can reuse for editing existing events

## Migration Timeline

| Version | Status |
|----------|--------|
| **Current** (v1.5.0) | `EventCreationScreen` marked `@Deprecated` |
| **Next Minor** (v1.6.0) | Warning logged when `EventCreationScreen` is used |
| **Next Major** (v2.0.0) | `EventCreationScreen` removed from codebase |

## Step-by-Step Migration

### Step 1: Identify Usage

Find all usages of `EventCreationScreen`:

```bash
# Search for EventCreationScreen usage
cd composeApp/src/commonMain/kotlin
grep -r "EventCreationScreen" .
```

**Current Usages:**
1. `WakevNavHost.kt:150` - Navigation route
2. `App.kt:248` - Screen entry point

### Step 2: Replace with DraftEventWizard

#### Before (Deprecated)

```kotlin
@Composable
fun CreateEventScreen(
    userId: String,
    onEventCreated: (Event) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Old implementation - single-step form
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var slots by remember { mutableStateOf(emptyList<TimeSlot>()) }

    // ... complex form in single screen
}
```

#### After (Recommended)

```kotlin
@Composable
fun CreateEventScreen(
    viewModel: EventManagementViewModel = koinViewModel(),
    navController: NavController,
    modifier: Modifier = Modifier
) {
    DraftEventWizard(
        initialEvent = null, // Set to existing event for editing
        onSaveStep = { event ->
            // Auto-save at each step transition
            viewModel.dispatch(Intent.UpdateDraftEvent(event))
        },
        onComplete = { event ->
            // Create event when wizard is complete
            viewModel.dispatch(Intent.CreateEvent(event))
        },
        onCancel = {
            // Cancel wizard
            navController.popBackStack()
        },
        modifier = modifier
    )
}
```

### Step 3: Update Navigation Routes

#### Before (Deprecated)

```kotlin
// WakevNavHost.kt
composable(
    route = "create-event"
) {
    CreateEventScreen(
        userId = userId,
        onEventCreated = { event ->
            navController.navigate("event-detail/${event.id}")
        },
        onBack = { navController.popBackStack() }
    )
}
```

#### After (Recommended)

```kotlin
// WakevNavHost.kt
composable(
    route = "create-event"
) {
    CreateEventScreen(
        viewModel = koinViewModel(),
        navController = navController
    )
}

// Also add edit route
composable(
    route = "edit-event/{eventId}"
) { backStackEntry ->
    val eventId = backStackEntry.arguments?.getString("eventId")
    EditEventScreen(
        eventId = eventId,
        viewModel = koinViewModel(),
        navController = navController
    )
}
```

### Step 4: Update App Entry Point

#### Before (Deprecated)

```kotlin
// App.kt
@Composable
fun App(
    userId: String,
    startDestination: String
) {
    // ... navigation setup
    
    // Create event route
    composable(
        route = "create-event"
    ) {
        CreateEventScreen(
            userId = userId,
            onEventCreated = { event ->
                navController.navigate("event-detail/${event.id}")
            },
            onBack = { navController.popBackStack() }
        )
    }
}
```

#### After (Recommended)

```kotlin
// App.kt
@Composable
fun App(
    startDestination: String
) {
    // ... navigation setup
    
    // Create event route
    composable(
        route = "create-event"
    ) {
        CreateEventScreen(
            viewModel = koinViewModel(),
            navController = navController
        )
    }
    
    // Edit event route
    composable(
        route = "edit-event/{eventId}"
    ) { backStackEntry ->
        val eventId = backStackEntry.arguments?.getString("eventId")
        EditEventScreen(
            eventId = eventId,
            viewModel = koinViewModel(),
            navController = navController
        )
    }
}
```

## Key Differences

| Aspect | EventCreationScreen | DraftEventWizard |
|---------|-------------------|-----------------|
| **Structure** | Single-step form | Multi-step wizard (4 steps) |
| **Auto-Save** | No (manual save only) | Yes (automatic at each step) |
| **Validation** | On final submission | Real-time at each step |
| **UX** | Overwhelming form | Progressive, focused |
| **State Management** | Manual state handling | Integrated with ViewModel |
| **Editing** | Not supported | Supported (pass `initialEvent`) |
| **Accessibility** | TalkBack: Challenging | TalkBack: Improved |

## Migration Checklist

- [ ] Identify all usages of `EventCreationScreen`
- [ ] Replace with `DraftEventWizard` in navigation
- [ ] Update screen signatures (use `ViewModel` instead of callbacks)
- [ ] Test creation flow end-to-end
- [ ] Test editing flow (if applicable)
- [ ] Test auto-save functionality
- [ ] Test accessibility (TalkBack)
- [ ] Remove `EventCreationScreen` references (for v2.0.0)

## Common Migration Issues

### Issue 1: "Callback-based" vs "ViewModel-based"

**Problem**: Old screen uses callbacks, new uses ViewModel.

**Solution**: Integrate with `EventManagementViewModel`:

```kotlin
// Old
CreateEventScreen(
    userId = userId,
    onEventCreated = { event -> ... },
    onBack = { ... }
)

// New
CreateEventScreen(
    viewModel = koinViewModel(), // Koin DI
    navController = navController
)
```

### Issue 2: Navigation handling

**Problem**: Old screen manually navigates in callback.

**Solution**: Let side effects handle navigation:

```kotlin
// Old
onEventCreated = { event ->
    navController.navigate("event-detail/${event.id}")
}

// New - handled by side effect
LaunchedEffect(Unit) {
    viewModel.sideEffect.collect { effect ->
        when (effect) {
            is NavigateTo -> navController.navigate(effect.route)
        }
    }
}
```

### Issue 3: Missing auto-save

**Problem**: Old screen doesn't auto-save.

**Solution**: Use `onSaveStep` callback:

```kotlin
DraftEventWizard(
    onSaveStep = { event ->
        // Auto-save at each step
        viewModel.dispatch(Intent.UpdateDraftEvent(event))
    },
    // ...
)
```

## Testing After Migration

### Unit Tests

```kotlin
@Test
fun `create event flow should work with DraftEventWizard`() {
    // Arrange
    val viewModel = EventManagementViewModel(...)
    val event = createTestEvent()

    // Act
    viewModel.dispatch(Intent.CreateEvent(event))
    testScope.testScheduler.advanceUntilIdle()

    // Assert
    val sideEffects = viewModel.sideEffects.toList()
    assertTrue(sideEffects.any { it is NavigateTo })
    val navigateTo = sideEffects.filterIsInstance<NavigateTo>().first()
    assertTrue(navigateTo.route.contains("event-detail"))
}
```

### UI Tests

```kotlin
@Test
fun `user can complete wizard and create event`() {
    // Launch create event screen
    composeTestRule.setContent {
        CreateEventScreen(
            viewModel = viewModel,
            navController = TestNavController()
        )
    }

    // Step 1: Fill Basic Info
    composeTestRule.onNodeWithText("Title *")
        .performTextInput("Team Retreat")
    composeTestRule.onNodeWithText("Description *")
        .performTextInput("Annual team building")
    composeTestRule.onNodeWithText("Next")
        .performClick()

    // Step 2: Skip Participants (optional)
    composeTestRule.onNodeWithText("Next")
        .performClick()

    // Step 3: Skip Locations (optional)
    composeTestRule.onNodeWithText("Next")
        .performClick()

    // Step 4: Add Time Slot
    composeTestRule.onNodeWithText("Add Time Slot")
        .performClick()
    // Fill time slot dialog...
    composeTestRule.onNodeWithText("Create Event")
        .performClick()

    // Assert navigation to detail
    composeTestRule.onNodeWithText("Event Details")
        .assertIsDisplayed()
}
```

## Rollback Plan

If issues arise after migration:

1. **Temporary Rollback**: Keep `EventCreationScreen` but add deprecation warning
2. **Fix Issues**: Address issues while keeping both screens
3. **Complete Migration**: Remove `EventCreationScreen` after validation

## Support

For migration assistance:
1. Review [DRAFT Wizard Usage Guide](./DRAFT_WIZARD_USAGE.md)
2. Review [State Machine Integration Guide](./STATE_MACHINE_INTEGRATION_GUIDE.md)
3. Check [AGENTS.md](../../AGENTS.md) for agent documentation
4. Open an issue for specific problems

## Related Documentation

- [DRAFT Wizard Usage Guide](./DRAFT_WIZARD_USAGE.md)
- [State Machine Integration Guide](./STATE_MACHINE_INTEGRATION_GUIDE.md)
- [Workflow Coordination Spec](../../openspec/specs/workflow-coordination/spec.md)
- [AGENTS.md](../../AGENTS.md)
