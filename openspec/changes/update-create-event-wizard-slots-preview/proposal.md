# Change: Update Create Event Wizard Slots Preview

## Why
The iOS Create Event wizard currently lets the organizer choose a single date/time and then creates the event directly from the final confirmation step. This does not fully match Wakeve's event-organization specification, which expects one or more proposed time slots for polling, and it gives the organizer no required preview checkpoint immediately before persistence.

## What Changes
- Update the iOS Create Event date step so organizers can add, review, and remove multiple proposed time slots before continuing.
- Keep the simple one-slot creation path working while introducing a multi-slot ViewModel/API contract that persists all selected slots.
- Change the final wizard step to offer an event preview before creation, with the actual create action happening from the preview confirmation.
- Validate that title/description and at least one proposed slot are present before allowing preview or event creation.
- Update iOS regression/contract tests for multi-slot creation, final preview gating, and the preserved single-slot behavior.

## Impact
- Affected specs: `event-organization`
- Affected code:
  - `iosApp/src/Views/Events/CreateEventSheet.swift`
  - `iosApp/src/ViewModels/CreateEventViewModel.swift`
  - `iosApp/WakeveTests/PremiumCreateEventContractTests.swift`
  - `iosApp/WakeveTests/FindingsRegressionTests.swift`
