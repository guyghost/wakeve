# Approval Summary

## Decision Requested
Approve `refactor-ios-premium-liquid-glass-ux` so implementation can begin.

## Scope Being Approved
- iOS-only UI/UX refactor.
- Premium Liquid Glass visual system.
- Destination-only tabs: Home, Groups, Messages, Profile.
- Contextual Create Event action.
- Refactored Home, Event Detail, Vote, Transport, Participants, Messages, Create Event, empty states, and loading states.
- Shared reusable iOS components and semantic design tokens.

## Scope Not Being Approved
- No shared KMP business logic rewrite.
- No Android redesign.
- No backend API changes.
- No event lifecycle or repository behavior change except presentation mapping where needed.

## Implementation Start Point
After approval, start with Slice 1 from `execution-plan.md`: design tokens, Liquid Glass components, fallbacks, motion tokens, and shared empty/loading components.

## Approval Gate
Implementation should not start until this change is explicitly approved.
