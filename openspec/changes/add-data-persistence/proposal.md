# Proposal: Add Data Persistence Layer

## Change ID
`add-data-persistence`

## Affected Specs
- `event-organization` (modified - add persistence requirements)

## Why
Currently, event data is stored only in-memory, which means:
- Data is lost when the app closes
- No offline support
- Cannot test multi-session scenarios
- Not production-ready

This change adds persistent storage using SQLDelight (SQLite), enabling:
- Data survives app restart
- Offline-first capability
- Multi-device sync foundation
- Production readiness

## What Changes
- Integrate SQLDelight for local database (SQLite)
- Design event/poll database schema
- Implement SQLDelight queries (.sq files)
- Replace in-memory EventRepository with DB-backed implementation
- Add database migration support
- Update EventRepository to use DB as source of truth

## Impact
- Affected capabilities: `event-organization`
- Affected modules: shared/src/commonMain, shared/src/commonTest
- Breaking changes: None (internal refactoring)
- Data migrations: N/A (Phase 1 data is ephemeral)

## Related Issues
- GitHub issue #[TBD] - Phase 2 Planning
- Enables: iOS UI, backend sync, offline support

## Technical Decisions
- Use SQLDelight for shared database layer (KMP)
- Store all times as ISO 8601 UTC strings
- Last-write-wins conflict resolution (v1)
- Migrations via version-numbered .sq files
- Encrypt sensitive data (tokens, auth info)

## Success Criteria
- All event data persists across app restart
- All tests pass with DB backend
- No performance degradation vs in-memory
- Offline scenarios tested and working
- Migration from in-memory to DB transparent to upper layers
