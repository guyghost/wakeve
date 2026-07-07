## 1. OpenSpec
- [x] 1.1 Create dashboard UX proposal and public web presence delta
- [x] 1.2 Validate `update-web-dashboard-ux` with `openspec validate --strict`

## 2. Frontend Data Contract
- [x] 2.1 Add raw dashboard DTOs matching backend response fields
- [x] 2.2 Normalize dashboard overview, event list, and event analytics into UI models
- [x] 2.3 Update dashboard machine state to support drawer, offline, permission, and copy-link feedback

## 3. Dashboard UI
- [x] 3.1 Replace KPI/table presentation with action queue, lifecycle groups, and cards
- [x] 3.2 Add reusable MetricTile, StatePanel, Tooltip, overflow menu, event card, board, and drawer components
- [x] 3.3 Add loading, empty, error, offline, permission denied, archived/deleted, expired poll, vote closed, and pending participant states
- [x] 3.4 Add copy-link confirmation and progressive secondary actions

## 4. Verification
- [x] 4.1 Run `pnpm -C apps/dashboard check`
- [x] 4.2 Run `pnpm -C apps/dashboard build`
- [x] 4.3 Review responsive behavior at 320, 768, 1024, and 1440 px through responsive layout classes; authenticated screenshot automation was not available in this tool surface
