# Design: Create Event Wizard Slots Preview

## Current State
`CreateEventSheet` owns one `selectedDate` and passes one ISO string to `CreateEventViewModel.createEvent(selectedDate:)`. The ViewModel converts that optional string into zero or one `TimeSlot`.

The sheet already has an `EventPreviewSheet`, but the wizard final step calls `createEvent()` through the bottom action. The preview can be opened from the toolbar, so it is optional rather than the final checkpoint.

## Proposed Model
Introduce an iOS-local draft slot value for the wizard, for example:

```swift
private struct EventSlotDraft: Identifiable, Equatable {
    let id: UUID
    var startDate: Date
    var startTime: Date
    var isAllDay: Bool
    var hasEndTime: Bool
    var endTime: Date
}
```

The date picker keeps its focused job: choosing one date/time. Saving appends or updates one draft slot. The date step lists the current draft slots with clear remove controls and an add button.

## Creation Contract
`CreateEventViewModel` should expose a multi-slot path, such as `selectedSlotStarts: [String]` or a richer slot draft DTO. A string-list path is the smallest change because `TimeSlot` already stores ISO `start`/`end` values and the current factory creates default one-hour end times.

The existing `selectedDate: String?` path should remain available during this change to avoid breaking older call sites and regression tests.

## Preview Gate
The confirm step should summarize the event and expose a preview action. The create operation should happen from `EventPreviewSheet.onNext`, after the organizer has seen the generated invitation/event preview.

Validation should require:
- non-empty trimmed title,
- non-empty trimmed description,
- at least one proposed slot.

## Test Strategy
Use source-level iOS contract tests for UI structure and ViewModel unit tests for slot conversion. This matches the current test style in `PremiumCreateEventContractTests` and `FindingsRegressionTests` while leaving room for simulator verification after implementation.
