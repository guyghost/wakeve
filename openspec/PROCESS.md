# OpenSpec Process - Simplified (No GitHub Projects Required)

## Overview
OpenSpec is a specification-driven development process for Wakeve that ensures clear requirements before implementation. This simplified version uses only:
- **GitHub Issues** for tracking
- **Local markdown files** for specifications
- **Structured naming conventions** for organization

## Quick Start Checklist

```
[ ] 1. Create GitHub Issue with change-id and description
[ ] 2. Create feature branch: git checkout -b change/<change-id>
[ ] 3. Create proposal.md in openspec/changes/<change-id>/
[ ] 4. Create/update spec.md in openspec/specs/<capability-name>/
[ ] 5. Write 10+ requirements with scenarios
[ ] 6. Run: openspec validate <change-id> --strict
[ ] 7. Commit and create PR (link to issue)
[ ] 8. Get approval before starting implementation
```

## Terminology

**Capability**: A major feature or system component (e.g., "event-organization", "user-auth")
- Lives in: `openspec/specs/<capability-name>/`
- Contains: `spec.md` with Purpose, Requirements, Scenarios

**Change**: A modification to one or more capabilities
- Lives in: `openspec/changes/<change-id>/`
- Types: ADD, UPDATE, REFACTOR, REMOVE
- Examples: `add-event-organization`, `update-rbac`, `refactor-sync-logic`

## Step-by-Step Process

### Step 1: Plan the Change

**Ask yourself:**
- What capability does this affect?
- Is it new (add-) or modifying existing (update-)?
- What are the key requirements?
- How will users interact with this?

**Create GitHub Issue:**
```
Title: [Change] Add event organization foundations
Body:
## Change ID
`add-event-organization`

## Description
Brief description of what this change does.

## Affected Capabilities
- `event-organization` (new)

## Key Requirements
- Event creation with multiple time slots
- Participant invitation and management
- Availability polling with voting
- Automatic best slot calculation
- Organizer date confirmation

## Success Criteria
- All requirements implemented and tested
- OpenSpec validation passes
- Code review approved
```

### Step 2: Create Feature Branch

```bash
git checkout -b change/add-event-organization
```

### Step 3: Create Proposal

**File**: `openspec/changes/<change-id>/proposal.md`

```markdown
# Proposal: [Title]

## Change ID
`add-event-organization`

## Affected Spec
- `event-organization` (new capability)

## Why
Clear explanation of the problem or opportunity this addresses.

## What Changes
- Bullet point list of changes
- Keep it high-level
- Focus on "what" not "how"

## Impact
- Affected capabilities
- Affected code modules
- Breaking changes?
- Data migrations needed?

## Related Issues
- GitHub issue number
```

### Step 4: Create/Update Specification

**File**: `openspec/specs/<capability>/spec.md`

```markdown
# Specification: Event Organization

> **Capability**: `event-organization`
> **Version**: 1.0.0
> **Status**: Draft

## Purpose
Clear description of what this capability enables.

### Core Concepts
Define key terms used throughout the spec.

## Requirements

### Requirement 1: [Verb] [Object]

**Description**: Clear statement of what the system SHALL/MUST do.

#### Scenario: [Scenario Name]
**Given**: Initial state
**When**: Action taken
**Then**: Expected outcome

### Requirement 2: [Verb] [Object]
...continue for all requirements (minimum 10 for major features)
```

**Requirements Format:**
- Use `SHALL` or `MUST` keyword (required for validation)
- Include at least one `#### Scenario:` per requirement
- Use Given-When-Then format
- Make them testable and unambiguous
- Aim for 10+ requirements per major feature

### Step 5: Validate Specification

```bash
# Validate the change
openspec validate <change-id> --strict

# Or validate just the spec
openspec validate <capability> --strict
```

**Expected output**: ✓ All validations pass

If validation fails:
1. Check error message carefully
2. Ensure all requirements have `### Requirement:` format with SHALL/MUST
3. Ensure all requirements have at least one `#### Scenario:`
4. Run again until it passes

### Step 6: Commit & Create PR

```bash
git add openspec/
git commit -m "docs: add OpenSpec for [change-id]

- Define [number] requirements for [capability]
- Document scenarios and acceptance criteria
- Link to GitHub issue #[number]"

git push origin change/<change-id>
```

**Create Pull Request:**
- Title: Same as commit message
- Body: Links to:
  - GitHub Issue (for tracking)
  - Specification file (for review)
- Label: `openspec` (if using labels)

### Step 7: Get Approval

**Before implementing:**
1. Wait for code review
2. Address feedback on spec
3. Get approval from maintainer
4. Only then start implementation

## File Structure

```
openspec/
├── AGENTS.md              # Agent definitions (don't change)
├── PROCESS.md            # This file
├── project.md            # Project context
├── specs/
│   └── <capability>/
│       ├── spec.md       # Main specification
│       └── design.md     # Implementation design (optional)
└── changes/
    └── <change-id>/
        ├── proposal.md   # Change proposal
        └── <version>/
            └── deltas.md # (Optional: version-specific deltas)
```

## Best Practices

### Writing Good Requirements

**Good ✅**
```markdown
### The system SHALL store time slots in UTC and convert to participant timezone

#### Scenario: Participant views slots in local timezone
**Given**: Event has slots in UTC (15:00 UTC - 17:00 UTC)
**When**: Participant in US/Eastern timezone opens the event
**Then**: Slots display as 10:00 AM - 12:00 PM EST
```

**Bad ❌**
```markdown
### Timezone support

The system will handle timezones properly.
```

### Naming Conventions

**Change IDs**: `<verb>-<object>`
- ✅ `add-event-organization`
- ✅ `update-rbac-permissions`
- ✅ `refactor-sync-logic`
- ❌ `event-org` (unclear)
- ❌ `new-feature-123` (not specific)

**Capabilities**: `<domain>-<feature>`
- ✅ `event-organization`
- ✅ `user-authentication`
- ✅ `destination-search`
- ❌ `feature1` (not descriptive)

### Requirement Levels

**10+ requirements**: Major features (new capability)
**5-9 requirements**: Minor features (enhance existing)
**1-4 requirements**: Bug fixes, small improvements (may skip spec)

## Validation Rules

OpenSpec validates:
1. ✅ Spec has `## Purpose` section
2. ✅ Spec has `## Requirements` section
3. ✅ Each requirement uses `###` (not `#` or `##`)
4. ✅ Each requirement contains `SHALL` or `MUST`
5. ✅ Each requirement has at least one `#### Scenario:`
6. ✅ Each scenario uses `**Given**`/`**When**`/`**Then**` format

## Examples

### Example 1: Adding a new feature

```
GitHub Issue: #5
Title: [Change] Add event organization foundations

Change ID: add-event-organization
Feature branch: change/add-event-organization

Files:
- openspec/changes/add-event-organization/proposal.md
- openspec/specs/event-organization/spec.md (NEW)

Result: 10 requirements, all validated ✓
```

### Example 2: Updating existing feature

```
GitHub Issue: #8
Title: [Change] Add timezone support to events

Change ID: update-timezone-support
Feature branch: change/update-timezone-support

Files:
- openspec/changes/update-timezone-support/proposal.md
- openspec/specs/event-organization/spec.md (MODIFIED)

Result: 3 new requirements added, all validated ✓
```

## Common Issues & Solutions

**Problem**: `Requirement must contain SHALL or MUST keyword`
**Solution**: Add `SHALL` to requirement title or description
```markdown
### The system SHALL calculate the best time slot
```

**Problem**: `Each requirement MUST include at least one #### Scenario: block`
**Solution**: Add scenario block with Given-When-Then
```markdown
#### Scenario: Participant votes on slots
**Given**: Poll is active
**When**: Participant selects votes
**Then**: Votes are recorded
```

**Problem**: `openspec validate` not working
**Solution**: Check:
1. File is in `openspec/specs/<name>/spec.md`
2. Run from project root: `openspec validate <capability-name>`
3. Check that capability name matches directory

## Integration with Implementation

Once spec is approved:

1. **Create implementation branch**: `git checkout -b feat/<change-id>`
2. **Reference requirements**: Comment code with `REQ-ID` references
3. **Write tests**: One test per requirement scenario
4. **Validate coverage**: Ensure each requirement is tested
5. **Update spec if needed**: Non-breaking clarifications only

Example test name:
```kotlin
@Test
fun REQ_EVT_001_organizerCanCreateEvent() { ... }
```

## OpenSpec Commands

```bash
# List all capabilities
openspec list --specs

# List all changes
openspec list

# Validate a spec
openspec validate <capability-name> --strict

# Validate a change
openspec validate <change-id> --strict

# Show change details
openspec show <change-id>

# Show spec details
openspec show <capability-name>
```

## When NOT to Create a Spec

You can skip the formal spec for:
- 🐛 Bug fixes (restoring intended behavior)
- 📝 Documentation and comments
- 🎨 Formatting, refactoring (no behavior change)
- 📦 Dependency updates (non-breaking)
- ✅ Tests for existing behavior

Still use git commits with good messages for these.

## Summary

OpenSpec ensures:
1. **Clear requirements** before implementation
2. **Testable acceptance criteria** via scenarios
3. **Approval gate** before coding
4. **Traceability** from idea to code
5. **Documentation** that stays in sync

By following this process, the codebase stays organized, requirements are clear, and implementation is focused and traceable.
