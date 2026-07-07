# Acceptance Checklist: Premium iOS Liquid Glass UX

Use this checklist before considering the change complete.

## Product Requirements

- [ ] Home answers where the user is, what is next, and how to create an event.
- [ ] Event Detail answers event identity, date/place, participant context, urgent next action, and next sections.
- [ ] Vote has one decision focus, progress, options, selected state, and confirmation feedback.
- [ ] Transport prioritizes route or meeting point, departure/arrival, participants, and one primary action.
- [ ] Participants are grouped by accepted, pending, and declined.
- [ ] Messages are event-conversation oriented with unread state and compact previews.
- [ ] Create Event uses five lightweight steps: name, date, place, invite people, confirm.
- [ ] Secondary actions are progressively disclosed through menus, bottom sheets, expandable rows, or contextual controls.

## Navigation Requirements

- [ ] Tab bar contains only Home, Groups, Messages, and Profile.
- [ ] Create Event is not a tab.
- [ ] Create Event appears contextually on Home and where relevant.
- [ ] Event-specific actions remain inside event screens.
- [ ] Labels are explicit and understandable without guessing.

## Visual System Requirements

- [ ] Dark mode is the primary polished target.
- [ ] Light mode is coherent and readable.
- [ ] Purple is not visually dominant.
- [ ] Semantic colors are used for backgrounds, labels, separators, accent, destructive, progress, and confirmation.
- [ ] Liquid Glass is native on iOS 26 where available.
- [ ] Fallbacks preserve readability when native glass is unavailable or transparency is reduced.
- [ ] Dynamic type does not cause overlapping or clipped text in core flows.
- [ ] Increased contrast and reduced motion are respected.

## Component Requirements

- [ ] Shared Liquid Glass card/button/toolbar/tab bar components exist or are consolidated.
- [ ] Shared Event Hero Card exists or the existing event card is refactored into that role.
- [ ] Shared Event List Row exists.
- [ ] Shared Participant Avatar Stack exists.
- [ ] Shared Vote Option Card exists.
- [ ] Shared Bottom Sheet pattern exists.
- [ ] Shared Empty State exists.
- [ ] Shared Loading Skeleton exists.
- [ ] Components use design tokens instead of local palettes and arbitrary styling.

## Business Logic Preservation

- [ ] Shared KMP state machines are not rewritten for presentation-only changes.
- [ ] Event creation still saves through the existing repository path.
- [ ] Poll voting still submits through the existing repository path.
- [ ] Transport actions still use existing `TransportPlanningViewModel` behavior.
- [ ] Access-control checks for organization details remain intact.
- [ ] Offline/pending-sync states remain visible where they existed.

## Verification Evidence

- [ ] `openspec validate refactor-ios-premium-liquid-glass-ux --strict` passes.
- [ ] Swift tests cover `WakeveTab` cases and labels.
- [ ] Swift tests cover create-event step progression and required validations.
- [ ] Existing transport planning contract tests pass.
- [ ] Existing organization access/phase tests pass.
- [ ] Existing inbox/messages model tests pass or are intentionally updated.
- [ ] iOS build/test command output is captured, or an exact local Xcode/simulator blocker is documented.
- [ ] Dark and light mode screenshots or manual verification notes exist for Home, Event Detail, Create Event, Vote, Transport, Messages, and Profile.
