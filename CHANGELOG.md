# CHANGELOG

All notable changes to the Wakeve project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added - 2025-12-31
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
| 0.1.0 | 2025-12-29 | Initial KMP release, event organization, polls, scenarios, meetings |
| Unreleased | 2025-12-31 | Workflow coordination, state machine communication, navigation automation |

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
