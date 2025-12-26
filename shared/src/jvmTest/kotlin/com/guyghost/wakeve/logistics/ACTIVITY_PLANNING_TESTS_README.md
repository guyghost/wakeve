# Activity Planning Integration Tests

## Overview

Comprehensive integration tests for activity planning and event organization features in Wakeve.

**Location**: `shared/src/jvmTest/kotlin/com/guyghost/wakeve/logistics/ActivityPlanningIntegrationTest.kt`

**Test Count**: 6 integration tests covering complete activity planning workflows

**Status**: ✅ All 6 tests passing

---

## Test Scenarios

### Scenario 1: Complete Activity Planning with Voting
**Test Method**: `testCompletActivityPlanningWithVoting_flows_from_suggestion_to_approval()`

**Objectives**:
- Verify activities can be suggested by participants
- Track voting on proposed activities
- Ensure voting comments are properly recorded

**Test Flow**:
1. Create event with confirmed date
2. Suggest multiple activities (hiking, kayaking, camping)
3. Add voting comments from all participants
4. Verify all activity suggestions and votes are recorded
5. Assert timestamps are properly recorded

**Assertions**:
- 6 total activity comments (3 suggestions + 3 votes)
- Contains hiking and camping references
- All comments have timestamps

**Related Sections**: ACTIVITY comments section

---

### Scenario 2: Activity Participant Registration
**Test Method**: `testActivityParticipantRegistration_tracks_who_joins_each_activity()`

**Objectives**:
- Track which participants register for each activity
- Build activity rosters
- Document participant commitments

**Test Flow**:
1. Create event and approve activities
2. Record activity approvals (Hiking, Kayaking)
3. Participants register for preferred activities
4. Verify registrations are recorded correctly

**Assertions**:
- 4 total registration comments (one per participant)
- 3 participants registered for hiking
- 2 participants registered for kayaking
- Registrations properly attributed to participants

**Activity Registrations Documented**:
- Alice: Hiking + can provide transport
- Bob: Hiking + Kayaking + can sponsor extra spot
- Charlie: Hiking + time constraint (leave by 5pm)
- Diana: Kayaking only (knee injury, can't hike)

**Related Sections**: ACTIVITY comments section

---

### Scenario 3: Activity Status Progression
**Test Method**: `testActivityStatusProgression_tracks_proposal_approval_confirmation_completion()`

**Objectives**:
- Track activities through their lifecycle
- Document status changes over time
- Verify proper progression order

**Test Flow**:
1. Create event
2. Propose activity (Rock Climbing) - PROPOSED status
3. Approve activity - APPROVED status
4. Confirm activity details - CONFIRMED status
5. Mark activity complete - COMPLETED status
6. Verify status progression matches expected sequence

**Assertions**:
- 4 status update comments
- Comments sorted chronologically
- Status order: PROPOSED → APPROVED → CONFIRMED → COMPLETED
- Each status change includes relevant details

**Status Details Captured**:
- PROPOSED: Description of activity (Rock Climbing session)
- APPROVED: Requirements check list (date, cost, guide)
- CONFIRMED: Final details (specific time, number of spots, registrations)
- COMPLETED: Results (attendance, ratings, success metrics)

**Related Sections**: ACTIVITY comments section

---

### Scenario 4: Activity Cost Management
**Test Method**: `testActivityCostManagement_calculates_and_splits_costs_correctly()`

**Objectives**:
- Track activity costs
- Calculate per-person expenses
- Document cost splits among participants
- Verify total expenditure tracking

**Test Flow**:
1. Create event
2. Record activities with costs (Cooking Class $120, Spa Day $240)
3. Add participant cost registrations
4. Generate cost summary

**Assertions**:
- 7 total cost-related comments
- 4 participant cost breakdowns
- Cost summary recorded
- Individual costs clearly documented

**Cost Examples Documented**:
- Alice: Cooking ($30) + Spa ($40) = $70
- Bob: Cooking ($30) = $30
- Charlie: Spa ($40) = $40
- Diana: Cooking ($30) + Spa ($40) = $70
- Total: $240

**Related Sections**: ACTIVITY comments section

---

### Scenario 5: Activity Requirements and Logistics
**Test Method**: `testActivityRequirementsAndLogistics_documents_special_needs_and_equipment()`

**Objectives**:
- Document activity requirements
- Track accessibility considerations
- Record participant confirmations
- Document special equipment needs

**Test Flow**:
1. Create event
2. Document hiking activity requirements
3. Add accessibility notes
4. Participants confirm they can meet requirements
5. Document any limitations or accommodations

**Assertions**:
- 5 requirement-related comments
- Activity requirements clearly documented
- Accessibility considerations recorded
- 3 participant confirmations with status indicators

**Requirements Documented**:
- Hiking Requirements:
  - Good hiking boots (mandatory)
  - Weather-appropriate clothing
  - Water bottle (2L minimum)
  - Sunscreen
  - Duration: 4-5 hours (full body workout)

**Accessibility Notes**:
- No wheelchair access to summit
- Steep sections (challenging for mobility issues)
- Rest areas every 30 minutes
- Clear marked trail

**Participant Confirmations**:
- Alice: ✓ Has boots, ready to go
- Charlie: ⚠ Needs to borrow boots (size 10)
- Diana: ✗ Cannot attend (knee injury), will do water activities

**Related Sections**: ACTIVITY comments section

---

### Scenario 6: Activity Planning Statistics
**Test Method**: `testActivityPlanningStatistics_aggregates_participation_and_engagement_metrics()`

**Objectives**:
- Aggregate activity planning statistics
- Measure participation rates
- Track engagement metrics
- Calculate involvement per participant

**Test Flow**:
1. Create event
2. Create 5 activity suggestions
3. Create 12 voting comments (3 votes per participant × 4 participants)
4. Create 4 registration comments
5. Calculate statistics

**Assertions**:
- 21 total comments
- 5 activity suggestions
- 12 voting comments
- 4 registrations
- All 4 participants engaged
- Each participant has ≥3 comments

**Engagement Metrics**:
- Total Comments: 21
- Suggestion Density: 5 unique activities
- Voting Participation: 100% (4/4 participants)
- Registration Completion: 100% (4/4 participants)
- Average Comments per Participant: 5.25

**Participation Matrix**:
- Activity suggestions: All 1 organizer
- Voting: All 4 participants
- Registrations: All 4 participants
- Total contributions: Balanced engagement

**Related Sections**: ACTIVITY comments section

---

## Test Data Models

### Event
- ID: event-{scenario-number}
- Title: Activity-specific event names
- Status: CONFIRMED (all tests use confirmed events)
- Participants: Minimal 4 (Alice, Bob, Charlie, Diana)

### Comment Structure
All comments use:
- **Section**: CommentSection.ACTIVITY
- **Author**: Participant (with ID and name)
- **Content**: Free-form text describing activity planning

### Timeline
Comments are created with sequential timestamps to simulate realistic discussion flow

---

## Data Coverage

| Category | Count | Examples |
|----------|-------|----------|
| **Activities Suggested** | 8+ | Hiking, Kayaking, Camping, Rock Climbing, Cooking, Spa |
| **Activities Approved** | 6+ | Mountain Hiking, Lake Kayaking, Rock Climbing, etc. |
| **Participants** | 4 | Alice, Bob, Charlie, Diana |
| **Cost Scenarios** | 2 | Multiple paid activities with different price points |
| **Accessibility Considerations** | 5+ | Mobility, timing, equipment needs, transportation |
| **Status Transitions** | 4 | PROPOSED → APPROVED → CONFIRMED → COMPLETED |

---

## Running the Tests

### Run all Activity Planning tests
```bash
./gradlew shared:jvmTest --tests "*ActivityPlanningIntegrationTest*"
```

### Run specific test
```bash
./gradlew shared:jvmTest --tests "*testCompletActivityPlanningWithVoting*"
```

### Run with verbose output
```bash
./gradlew shared:jvmTest --tests "*ActivityPlanningIntegrationTest*" --info
```

### View test report
```bash
open shared/build/reports/tests/jvmTest/index.html
```

---

## Test Execution Summary

```
✅ testCompletActivityPlanningWithVoting_flows_from_suggestion_to_approval - PASSED
✅ testActivityParticipantRegistration_tracks_who_joins_each_activity - PASSED
✅ testActivityStatusProgression_tracks_proposal_approval_confirmation_completion - PASSED
✅ testActivityCostManagement_calculates_and_splits_costs_correctly - PASSED
✅ testActivityRequirementsAndLogistics_documents_special_needs_and_equipment - PASSED
✅ testActivityPlanningStatistics_aggregates_participation_and_engagement_metrics - PASSED

Total: 6/6 PASSED (100%)
```

---

## Integration Points Tested

### 1. CommentRepository Integration
- ✅ Creating comments via `createComment()`
- ✅ Retrieving comments by section via `getCommentsBySection()`
- ✅ Filtering comments by content patterns
- ✅ Grouping and aggregating comment data

### 2. EventRepository Integration
- ✅ Creating events
- ✅ Managing event status (CONFIRMED)
- ✅ Event lifecycle management

### 3. Database Integration
- ✅ Comment persistence in SQLDelight
- ✅ Query filtering by section (ACTIVITY)
- ✅ Timestamp storage and retrieval
- ✅ Multi-comment transactions

---

## Design Patterns Used

### 1. Arrange-Act-Assert (AAA)
Each test follows the AAA pattern:
- **Arrange**: Set up event and initial state
- **Act**: Create comments and interactions
- **Assert**: Verify expected outcomes

### 2. Helper Method Pattern
- `createTestEvent()`: Reusable event creation
- `now()`: Consistent timestamp generation

### 3. Comment-Driven Testing
All activity planning is tracked through the comment system:
- Suggestions as comments
- Voting as comment replies
- Status as comment content markers
- Cost tracking as comment text

---

## Future Extensions

### Potential Additional Tests
1. **Activity Conflicts**: Overlapping activities at same time
2. **Capacity Management**: More registrations than available spots
3. **Multi-section Comments**: Comments linking activities to other sections
4. **Reply Threading**: Nested discussion comments on specific activities
5. **Participant Availability**: Activity success based on available participants
6. **Dynamic Cost Calculation**: Automatic cost splitting across participants

### Enhanced Metrics
- Activity popularity scoring
- Participant engagement patterns
- Cost-effectiveness analysis
- Group availability optimization

---

## Related Test Suites

- **MealPlanningIntegrationTest**: Similar patterns for meal planning
- **AccommodationIntegrationTest**: Lodging organization patterns
- **EquipmentChecklistIntegrationTest**: Equipment management
- **CollaborationIntegrationTest**: Cross-section collaboration patterns

---

## Notes

- All tests use in-memory SQLDelight database for isolation
- Tests are independent and can run in any order
- Database is reset between tests via `@BeforeTest` setup
- Comments are properly timestamped for realistic chronological ordering
- All participant data is realistic and diverse (different participation levels)

---

**Last Updated**: December 26, 2025  
**Created by**: Test Agent  
**Status**: Production Ready ✅
