# Change: Update web dashboard UX

## Why
The authenticated web dashboard currently reads like an analytics report: summary cards, a status breakdown, and a table. Wakeve organizers need a clearer command center that highlights what to do next, adapts the visualization to event lifecycle data, and treats loading, empty, offline, permission, and partial-data states as first-class UX.

## What Changes
- Replace the dashboard table-first layout with an action-oriented dashboard using lifecycle groups, event cards, next actions, and an event detail drawer.
- Normalize the existing backend dashboard response in the web app so UI code uses stable dashboard models without changing backend endpoints.
- Add reusable web dashboard components for metrics, state panels, tooltips, overflow actions, lifecycle boards, and details.
- Surface primary actions immediately and hide secondary actions behind contextual controls.
- Add explicit handling for loading, empty, error, offline, permission denied, archived/deleted, expired poll, closed RSVP/vote, and pending participant states.

## Impact
- Affected specs: public-web-presence
- Affected code: apps/dashboard Svelte routes, dashboard API types/normalizers, dashboard UI components, dashboard XState machine
- Backend API: unchanged
