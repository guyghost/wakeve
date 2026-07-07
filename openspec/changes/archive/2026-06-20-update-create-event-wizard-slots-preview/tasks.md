## 1. Tests First
- [x] 1.1 Add ViewModel tests proving multiple ISO slot starts are converted into multiple `TimeSlot` values.
- [x] 1.2 Preserve the existing single selected-date regression test or adapt it to the new multi-slot API compatibility path.
- [x] 1.3 Add iOS contract tests proving the date step exposes add/remove multi-slot controls and the confirm step opens preview instead of directly creating the event.
- [x] 1.4 Add tests or source-level assertions that creation is disabled until at least one proposed slot exists.

## 2. iOS Wizard Implementation
- [x] 2.1 Introduce local value state for proposed slots in `CreateEventSheet`, including formatted display labels and stable identity.
- [x] 2.2 Update the date/time picker save path so each save appends a slot rather than replacing the only selected date.
- [x] 2.3 Add remove/edit affordances for proposed slots while keeping the date step focused and lightweight.
- [x] 2.4 Update the final confirm step and bottom action so it opens `EventPreviewSheet` before event creation.
- [x] 2.5 Update `EventPreviewSheet` to display multiple proposed slots clearly.

## 3. ViewModel and Persistence Contract
- [x] 3.1 Extend `CreateEventViewModel.createEvent` to accept multiple proposed slot start values or slot drafts.
- [x] 3.2 Update `EventTimeSlotFactory` to build multiple timezone-aware `TimeSlot` instances with default end times.
- [x] 3.3 Keep backward compatibility for callers that still pass a single `selectedDate`.

## 4. Verification
- [x] 4.1 Run OpenSpec validation with `openspec validate update-create-event-wizard-slots-preview --strict`.
- [x] 4.2 Run the relevant iOS unit/contract tests.
- [x] 4.3 Build or test the iOS app on simulator, and manually verify the wizard path through preview and creation.
