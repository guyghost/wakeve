# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added (Phase 4: DRAFT Workflow Tests & Phase 5: Documentation)

- **DRAFT Workflow Integration Tests**
  - Comprehensive test suite (`DraftWorkflowIntegrationTest`) with 8 passing tests
  - Tests cover: repository operations, use cases, state machine dispatch, event persistence
  - Tests verify auto-save, validation gates, location management, time slot management
  - 100% test coverage for DRAFT workflow phase
  
- **Developer Documentation**
  - `DraftEventWizard Usage Guide` - Complete integration guide for Android & iOS
  - `State Machine Integration Guide` - MVI pattern and intent dispatch documentation
  - Enhanced `AGENTS.md` with detailed DRAFT phase workflow documentation
  - Examples for composable patterns, custom validation, offline support
  
- **Documentation Updates**
  - Added DRAFT Phase section to AGENTS.md with workflow diagram
  - Added validation rules by step
  - Added side effects documentation
  - Added integration patterns and best practices

## [0.3.0] - 2026-01-01

### Added

- **Event Type Classification**
  - New `EventType` enum with 11 predefined types (BIRTHDAY, WEDDING, TEAM_BUILDING, CONFERENCE, WORKSHOP, PARTY, SPORTS_EVENT, CULTURAL_EVENT, FAMILY_GATHERING, OTHER, CUSTOM)
  - Support for custom event types with `eventTypeCustom` field
  - `SuggestEventTypeUseCase` for preset event type suggestions
  
- **Participant Estimation**
  - New fields in Event model: `minParticipants`, `maxParticipants`, `expectedParticipants`
  - `EstimateParticipantsUseCase` for participant count calculations
  - Validation: max >= min, all counts >= 0
  
- **Potential Locations**
  - New `PotentialLocation` model for event location suggestions
  - `LocationType` enum: CITY, REGION, SPECIFIC_VENUE, ONLINE
  - CRUD operations for potential locations (add/remove in DRAFT only)
  - New API endpoints: `GET/POST/DELETE /api/events/{id}/potential-locations`
  
- **Flexible Time Slots**
  - New `TimeOfDay` enum: ALL_DAY, MORNING, AFTERNOON, EVENING, SPECIFIC
  - `timeOfDay` field in TimeSlot model
  - Support for flexible slots without exact times (e.g., "afternoon in June")
  - Migration: existing slots default to SPECIFIC
  
- **Draft Event Wizard UI**
  - 4-step wizard for event creation (Basics, Participants, Locations, Time Slots)
  - Android: Material You design with `DraftEventWizard` composable
  - iOS: Liquid Glass design with `DraftEventWizardView` SwiftUI
  - Auto-save behavior for each step
  - Real-time validation feedback
  
- **State Machine Updates**
  - New Intents: `UpdateDraftEvent`, `AddPotentialLocation`, `RemovePotentialLocation`
  - Validation guards for DRAFT status only operations
  - Enhanced `EventManagementStateMachine` with new handlers
  
- **Use Cases**
  - `ValidateEventDraftUseCase` for multi-field event validation
  - `SuggestEventTypeUseCase` for preset type suggestions
  - `EstimateParticipantsUseCase` for participant count estimations
  
- **Testing**
  - 14 unit tests for State Machine new Intents
  - 12 unit tests for Draft Event Use Cases
  - 10 migration tests for SQLDelight schema updates
  - 12 integration tests for complete DRAFT workflow
  - Total: 48 new tests (100% passing)
  
- **Documentation**
  - Updated `event-organization` specification with new requirements
  - Updated `AGENTS.md` with new models and agent capabilities
  - Updated `API.md` with new endpoints and data models
  - New `draft-event-wizard-guide.md` with UX documentation

### Changed

- **Event Model**
  - Enhanced with 5 new optional fields (eventType, eventTypeCustom, minParticipants, maxParticipants, expectedParticipants)
  - Updated serialization/deserialization for new fields
  
- **TimeSlot Model**
  - Added `timeOfDay` field (nullable, default SPECIFIC)
  - `start` and `end` can now be null if `timeOfDay != SPECIFIC`
  
- **Event Creation Flow**
  - Split into multi-step wizard (previously single screen)
  - Reduced cognitive load with progressive disclosure
  - All new fields are optional for backward compatibility
  
- **API POST /api/events**
  - Request body now accepts new optional fields
  - Validation enhanced with new rules (participant counts, custom type)
  
- **Database Schema**
  - Migrated Event table with 5 new columns
  - Migrated TimeSlot table with 1 new column
  - New PotentialLocation table with FK cascade delete

### Deprecated

- Single-screen event creation UI (replaced by 4-step wizard)

### Removed

- None

### Fixed

- **Compatibility**: Fixed EventRepository to work with new Event fields
- **Migration**: Ensured existing events work correctly with default values
- **UI**: Fixed PollResultsScreen to handle nullable TimeSlot start/end times

### Security

- None

## [0.2.0] - 2025-12-31

### Added
- **Workflow Coordination System** (`verify-statemachine-workflow`) - Complete event lifecycle coordination between state machines
  - Implemented repository-mediated communication pattern for loose coupling between EventManagement, ScenarioManagement, and MeetingService state machines
  - Added 5 new Intent handlers enabling complete DRAFT → FINALIZED workflow:
    - `StartPoll`: Transitions event from DRAFT to POLLING
    - `ConfirmDate`: Transitions from POLLING to CONFIRMED, unlocks scenarios, navigates automatically
    - `TransitionToOrganizing`: Transitions from CONFIRMED to ORGANIZING, unlocks meetings
    - `MarkAsFinalized`: Transitions from ORGANIZING to FINALIZED, locks event
    - `SelectScenarioAsFinal`: Enables optional scenario selection with automatic navigation
  - Implemented guard pattern with status-based validation to prevent invalid state transitions
  - Added automatic navigation side effects (NavigateTo) to guide users through workflow phases
  - Implemented status-based feature unlocking (scenarios at CONFIRMED, meetings at ORGANIZING)
  - Created comprehensive test suite: 29 tests (23 unit + 6 integration) with 100% pass rate
  - Added extensive documentation:
    - Permanent specification: `openspec/specs/workflow-coordination/spec.md` (11 requirements)
    - 7 Mermaid workflow diagrams in WORKFLOW_DIAGRAMS.md
    - Troubleshooting guide with 8 common issues and 3 validation tools
    - Updated AGENTS.md with 200+ lines on workflow coordination patterns
  - Modified 3 Contract files and 2 StateMachine files (~680 lines production code)
  - All changes validated with 100% test coverage and successful JVM + Android compilation
  - **Archive Location**: `openspec/archive/2025-12-31-verify-statemachine-workflow/`
  - **Specification**: `openspec/specs/workflow-coordination/spec.md`

## [0.1.0] - 2025-12-29

### Added
- Initial Kotlin Multiplatform implementation with Android and iOS support
- Event organization with status-based lifecycle (DRAFT, POLLING, CONFIRMED, COMPARING, ORGANIZING, FINALIZED)
- Weighted poll system with YES=2, MAYBE=1, NO=-1 voting
- Scenario management for destination and lodging comparison
- Meeting service for virtual meeting link generation (Zoom, Google Meet, FaceTime)
- Calendar integration with ICS generation (RFC 5545 compliant)
- Offline-first architecture with SQLDelight (SQLite) and sync service
- MVI (Model-View-Intent) architecture with state machines
- Jetpack Compose UI for Android with Material You design system
- SwiftUI UI for iOS with Liquid Glass design language
- Ktor backend REST API with 8 endpoints
- Comprehensive test suite: 36 tests with offline scenario coverage
- First-time onboarding flow (4 screens) for Android and iOS
- OAuth authentication preparation (Phase 3 ready)

---

## Version History

| Version | Date | Key Features |
|---------|------|--------------|
| 0.3.0 | 2026-01-01 | Enhanced DRAFT phase, wizard UI, event types, participants estimation, potential locations |
| 0.2.0 | 2025-12-31 | Workflow coordination, state machine communication, navigation automation |
| 0.1.0 | 2025-12-29 | Initial KMP release, event organization, polls, scenarios, meetings |

---

## Categories

### Added
New features and capabilities

### Changed
Changes to existing functionality

### Deprecated
Features marked for removal in future versions

### Removed
Features removed in this version

### Fixed
Bug fixes

### Security
Security-related changes and fixes

---

## Contributors

- AI Assistant (Orchestrator) - Workflow coordination system, state machine architecture, comprehensive documentation
- Development Team - Core KMP implementation, UI design systems, offline-first architecture

---

## Related Documentation

- **OpenSpec Changes**: `openspec/archive/` - Detailed change documentation
- **Specifications**: `openspec/specs/` - Permanent capability specifications
- **Architecture**: `docs/architecture/` - System architecture documentation
- **Testing**: `docs/testing/` - Test strategies and guides
- **Quick Start**: `QUICK_START.md` - 5-minute setup guide
- **Contributing**: `CONTRIBUTING.md` - Development guidelines
