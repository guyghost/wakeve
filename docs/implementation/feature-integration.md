# PRD Features Integration for iOS Navigation

## Overview

This document describes the changes needed to integrate the new PRD features into the iOS navigation system for the Wakeve application.

## Changes Required

### 1. Update AppView Enum

The `AppView` enum in `ContentView.swift` needs to be extended with the following cases:

```swift
// New PRD features
case scenarioList
case scenarioDetail
case scenarioComparison
case budgetOverview
case budgetDetail
case accommodation
case mealPlanning
case equipmentChecklist
case activityPlanning
```

### 2. Add State Variables

Add the following state variables to the `AuthenticatedView` struct in `ContentView.swift`:

```swift
// New state variables for PRD features
@State private var showScenarioList = false
@State private var showBudgetDetail = false
@State private var showAccommodation = false
@State private var showMealPlanning = false
@State private var showEquipmentChecklist = false
@State private var showActivityPlanning = false
@State private var selectedScenario: Scenario_?
```

### 3. Navigation Cases

Add the following cases to the `homeTabContent` switch statement in `ContentView.swift`:

```swift
// MARK: - New PRD Features Navigation Cases

case .scenarioList:
    if let event = selectedEvent {
        ScenarioListView(
            event: event,
            repository: ScenarioRepository(db: DatabaseProvider.shared.getDatabase(factory: IosDatabaseFactory())),
            participantId: userId,
            onScenarioTap: { scenario in
                selectedScenario = scenario
                // TODO: Navigate to scenario detail
            },
            onCompareTap: {
                // TODO: Navigate to scenario comparison
            },
            onBack: {
                currentView = .eventDetail
            }
        )
    }

case .scenarioComparison:
    if let event = selectedEvent {
        ScenarioComparisonView(
            event: event,
            repository: ScenarioRepository(db: DatabaseProvider.shared.getDatabase(factory: IosDatabaseFactory())),
            participantId: userId,
            onBack: {
                currentView = .scenarioList
            }
        )
    }

case .budgetOverview:
    if let event = selectedEvent {
        BudgetOverviewView(
            event: event,
            repository: BudgetRepository(db: DatabaseProvider.shared.getDatabase(factory: IosDatabaseFactory())),
            onBack: {
                currentView = .eventDetail
            },
            onViewDetails: {
                // TODO: Navigate to budget detail
            }
        )
    }

case .accommodation:
    if let event = selectedEvent {
        AccommodationView(
            eventId: event.id,
            currentUserId: userId,
            currentUserName: "Current User" // TODO: Get actual user name
        )
        .navigationBarTitleDisplayMode(.inline)
    }

case .mealPlanning:
    if let event = selectedEvent {
        MealPlanningView(
            eventId: event.id,
            userId: userId,
            repository: MealRepository(db: DatabaseProvider.shared.getDatabase(factory: IosDatabaseFactory())),
            onBack: {
                currentView = .eventDetail
            }
        )
    }

case .equipmentChecklist:
    if let event = selectedEvent {
        EquipmentChecklistView(
            eventId: event.id,
            userId: userId,
            repository: EquipmentRepository(db: DatabaseProvider.shared.getDatabase(factory: IosDatabaseFactory())),
            onBack: {
                currentView = .eventDetail
            }
        )
    }

case .activityPlanning:
    if let event = selectedEvent {
        ActivityPlanningView(
            eventId: event.id,
            userId: userId,
            repository: ActivityRepository(db: DatabaseProvider.shared.getDatabase(factory: IosDatabaseFactory())),
            onBack: {
                currentView = .eventDetail
            }
        )
    }
```

### 4. Add PRD Feature Buttons to Event Detail View

Add a new section to the `ModernEventDetailView.swift` file with buttons that appear based on event status:

```swift
// MARK: - PRD Feature Buttons Section

struct PRDFeatureButtonsSection: View {
    let event: Event
    
    var body: some View {
        VStack(spacing: 12) {
            // Scenario Planning - Available in COMPARING and CONFIRMED states
            if event.status == .comparing || event.status == .confirmed {
                HostActionButton(
                    title: "Scenario Planning",
                    icon: "list.bullet.rectangle.portrait",
                    color: .blue,
                    action: {
                        // Navigate to scenario list
                        // TODO: Implement navigation
                    }
                )
            }
            
            // Budget Overview - Available in CONFIRMED and ORGANIZING states
            if event.status == .confirmed || event.status == .organizing {
                HostActionButton(
                    title: "Budget Overview",
                    icon: "dollarsign.circle",
                    color: .green,
                    action: {
                        // Navigate to budget overview
                        // TODO: Implement navigation
                    }
                )
            }
            
            // Accommodation - Available in ORGANIZING state
            if event.status == .organizing {
                HostActionButton(
                    title: "Accommodation",
                    icon: "house.fill",
                    color: .purple,
                    action: {
                        // Navigate to accommodation
                        // TODO: Implement navigation
                    }
                )
                
                HostActionButton(
                    title: "Meal Planning",
                    icon: "fork.knife",
                    color: .orange,
                    action: {
                        // Navigate to meal planning
                        // TODO: Implement navigation
                    }
                )
                
                HostActionButton(
                    title: "Equipment Checklist",
                    icon: "bag.fill",
                    color: .pink,
                    action: {
                        // Navigate to equipment checklist
                        // TODO: Implement navigation
                    }
                )
                
                HostActionButton(
                    title: "Activity Planning",
                    icon: "figure.walk",
                    color: .red,
                    action: {
                        // Navigate to activity planning
                        // TODO: Implement navigation
                    }
                )
            }
        }
        .padding(.vertical, 8)
    }
}
```

Then add this section to the ModernEventDetailView body:

```swift
// PRD Feature Buttons based on event status
PRDFeatureButtonsSection(event: event)
```

## Event Status Mapping

The new features should be accessible based on the event status:

- **DRAFT**: No additional features
- **POLLING**: No additional features
- **COMPARING**: Scenario Planning
- **CONFIRMED**: Scenario Planning, Budget Overview
- **ORGANIZING**: Scenario Planning, Budget Overview, Accommodation, Meal Planning, Equipment Checklist, Activity Planning
- **FINALIZED**: All organizing features plus any finalization features

## Implementation Notes

1. All views should follow the Liquid Glass design system
2. Navigation should respect the offline-first principle
3. Error handling should be implemented for all repository interactions
4. User permissions should be checked before allowing access to features
5. All new views should be integrated with the existing repository pattern