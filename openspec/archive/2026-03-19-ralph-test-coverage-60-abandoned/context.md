# Context: Tests & Quality - Reach 60% Coverage (Ralph Mode)

## Ralph Mode Configuration
- **Enabled**: true
- **Max Iterations**: 10
- **Current Iteration**: 1
- **Mode**: TDD + Quality Focus

## Objective
Atteindre 60% de couverture de tests et corriger tous les tests qui échouent.

## Current State
- **Total tests**: 734
- **Passing**: ~730 (99.5%)
- **Failing**: 4-5 tests (meeting use cases)
- **Coverage**: ~17% (estimé)
- **Target**: 60% coverage

## Critical Gaps Identified
1. Repository layer tests (Event, Album, Comment, Scenario)
2. State machine edge cases (failure scenarios)
3. Offline sync scenarios
4. Meeting use cases (4 tests failing)
5. Integration tests (cross-state-machine flows)

## Test Strategy
1. Fix failing tests first (meeting use cases)
2. Add repository tests for uncovered repositories
3. Add state machine edge case tests
4. Add offline sync integration tests
5. Measure coverage with JaCoCo

## Files to Test
### Priority 1 (Critical Path)
- EventRepository
- PollService / PollLogic
- SyncService
- State Machines (EventManagement, Scenario)

### Priority 2 (Important)
- AlbumRepository
- CommentRepository
- ScenarioRepository
- MeetingRepository

### Priority 3 (Supporting)
- BudgetRepository
- VoteRepository
- ParticipantRepository

## Coverage Goals
| Layer | Current | Target | Tests Needed |
|-------|---------|--------|--------------|
| Repository | Low | 80% | +50 tests |
| Use Cases | Medium | 90% | +30 tests |
| State Machines | Medium | 85% | +20 tests |
| Services | Low | 70% | +25 tests |
| **Total** | **~17%** | **60%** | **+125 tests** |

## Ralph Iterations
1. Fix failing meeting tests
2. Add EventRepository tests
3. Add PollService/PollLogic tests
4. Add State Machine edge case tests
5. Add Offline sync tests
6. Measure and optimize coverage

## Acceptance Criteria
- [ ] All 734+ tests passing
- [ ] Coverage >= 60% (measured by JaCoCo)
- [ ] No critical gaps in repository layer
- [ ] State machine failure scenarios tested
- [ ] Offline/online transitions tested

## Next Phase (After 60%)
Phase 6 Analytics:
- P1.1: Analytics Provider Interface
- P1.2: Firebase Analytics
- P1.3: Analytics Integration
