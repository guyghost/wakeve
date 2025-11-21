# OpenSpec Instructions for AI Assistants

**START HERE**: Read [PROCESS.md](./PROCESS.md) for the complete simplified workflow.

This page provides quick reference and implementation guidance.

## Quick Checklist

```
[ ] 1. Review existing specs: openspec list --specs
[ ] 2. Review existing changes: openspec list  
[ ] 3. Decide: new capability or modify existing?
[ ] 4. Create GitHub Issue with change-id
[ ] 5. Create feature branch: git checkout -b change/<change-id>
[ ] 6. Create proposal: openspec/changes/<change-id>/proposal.md
[ ] 7. Create/update spec: openspec/specs/<capability>/spec.md
[ ] 8. Write 10+ requirements with scenarios
[ ] 9. Validate: openspec validate <change-id> --strict
[ ] 10. Submit PR linking to GitHub Issue
[ ] 11. Get approval before implementing
```

## When to Create a Formal Spec

### ✅ Do Create Spec For:
- Adding new features or capabilities
- Making breaking API/schema changes
- Changing architecture or patterns
- Performance optimizations (behavior change)
- Security or RBAC updates
- Major refactoring

### ❌ Skip Formal Spec For:
- 🐛 Bug fixes (restore intended behavior)
- 📝 Documentation/comments only
- 🎨 Code style/formatting/refactoring (no behavior change)
- 📦 Non-breaking dependency updates
- ✅ Tests for existing behavior

## Stage 1: Proposal & Specification

### Step 1: Create GitHub Issue
```
Title: [Change] <Verb-led description>

Body:
## Change ID
`<change-id>`

## Description
Brief explanation of what and why.

## Key Requirements
- List main features

## Success Criteria
- How we'll know it's done
```

**Change ID Format**: `<verb>-<noun>` (kebab-case)
- ✅ `add-event-organization`
- ✅ `update-timezone-support`
- ✅ `refactor-sync-logic`

### Step 2: Create Feature Branch
```bash
git checkout -b change/<change-id>
```

### Step 3: Create Proposal
**File**: `openspec/changes/<change-id>/proposal.md`

```markdown
# Proposal: [Title]

## Change ID
`<change-id>`

## Affected Spec
- `<capability>` (new|modified)

## Why
Problem this solves / opportunity this enables.

## What Changes
- Bullet list of changes
- Keep high-level
- Focus on "what" not "how"

## Impact
- Affected capabilities/modules
- Breaking changes?
- Data migrations?

## Related Issues
- Link to GitHub issue
```

### Step 4: Create/Update Specification
**File**: `openspec/specs/<capability>/spec.md`

```markdown
# Specification: [Capability Name]

> **Capability**: `<capability>`
> **Version**: X.Y.Z
> **Status**: Draft|Approved|Implemented

## Purpose
Clear description of what this capability enables.

### Core Concepts
Define key terms. Example:
- **Event**: A collaborative event with multiple proposed time slots
- **Poll**: A collection of participant votes
- **Vote**: A participant's preference (YES, MAYBE, NO)

## Requirements

### Requirement 1: The system SHALL [verb] [object]

**Description**: Clear statement of what must happen.

#### Scenario: [Descriptive name]
**Given**: Initial state / precondition
**When**: Action taken / what triggers this
**Then**: Expected outcome / what should happen

### Requirement 2: The system SHALL [verb] [object]
...continue with 10+ requirements...
```

**Requirement Rules:**
1. Use `###` header (not `##` or `#`)
2. Include `SHALL` or `MUST` keyword (required for validation)
3. At least one `#### Scenario:` per requirement
4. Use Given-When-Then format in scenarios
5. Make each testable and unambiguous
6. **Minimum 10 requirements** for major features

### Step 5: Validate
```bash
# Validate the change
openspec validate <change-id> --strict

# Validate the spec
openspec validate <capability> --strict

# Fix any issues and re-run until passing
```

### Step 6: Commit & Create PR
```bash
git add openspec/
git commit -m "docs: add OpenSpec for <change-id>

- Define 10+ requirements for <capability>
- Document scenarios and acceptance criteria
- Relates to GitHub issue #<number>"

git push origin change/<change-id>
```

**Create Pull Request:**
- Link to GitHub Issue
- Label: `openspec`
- Await approval before implementing

## Stage 2: Implementation

### Prerequisites
- ✅ Spec proposal PR is approved and merged
- ✅ You've read the proposal.md
- ✅ You've read the full specification

### Key Practices

1. **Test-Driven Development (TDD)**
    - Write tests BEFORE implementing code
    - Tests must cover common code, Android, and iOS platforms
    - Use platform-specific test frameworks:
      - **Common**: Kotlin test (kotlin.test)
      - **Android**: JUnit + Robolectric for unit tests, Espresso for UI tests
      - **iOS**: XCTest for unit tests, XCUITest for UI tests
    - Test naming: `REQ_<ID>_<scenario>()` (e.g., `REQ_EVT_001_cannotVoteAfterDeadline()`)

2. **Reference requirements in code**
    ```kotlin
    // REQ-EVT-001: The system SHALL enforce voting deadline
    fun validateVote(eventId: String): Result<Boolean> { ... }
    ```

3. **Write tests per scenario**
    ```kotlin
    @Test
    fun REQ_EVT_001_cannotVoteAfterDeadline() { ... }
    ```

4. **Track requirement coverage**
    - Ensure each requirement is tested
    - Use REQ-ID in test names
    - Document implementation in PR

5. **Update spec if clarifications needed**
    - Only non-breaking clarifications
    - Major changes require new spec proposal

6. **Update GitHub issues regularly**
    - Check off completed actions in issue descriptions
    - Close issues when work is fully completed
    - Add comments documenting progress and completion
    - Remember to update issues after each major step

7. **Conventional Commits**
    - Use conventional commit format for all commits
    - Format: `<type>[optional scope]: <description>`
    - Types: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`
    - Example: `feat(sync): add CRDT-based conflict resolution`
    - Commit at the end of each task, not during implementation

## Reference: OpenSpec Commands

```bash
# List all specs
openspec list --specs

# List all active changes
openspec list

# Show spec details
openspec show <capability>

# Show change details  
openspec show <change-id>

# Validate spec
openspec validate <capability> --strict

# Validate change
openspec validate <change-id> --strict

# Validate all
openspec validate --specs --strict
openspec validate --changes --strict
```

## Examples

### Example 1: Creating a New Capability

```
Issue: #5 - Add event organization foundations
Change ID: add-event-organization
Branch: change/add-event-organization

Files created:
- openspec/changes/add-event-organization/proposal.md
- openspec/specs/event-organization/spec.md

Requirements: 10 (all validated ✓)
```

### Example 2: Updating an Existing Capability

```
Issue: #12 - Add timezone support
Change ID: update-timezone-support  
Branch: change/update-timezone-support

Files modified:
- openspec/changes/update-timezone-support/proposal.md
- openspec/specs/event-organization/spec.md

Requirements added: 3 (validated ✓)
```

## Common Issues

### Issue: "Requirement must contain SHALL or MUST keyword"
**Solution**: Add `SHALL` or `MUST` to requirement title
```markdown
### The system SHALL calculate the best time slot
```

### Issue: "Each requirement MUST include at least one #### Scenario:"
**Solution**: Add scenario with Given-When-Then format
```markdown
#### Scenario: Participant votes on slots
**Given**: Poll is active
**When**: Participant selects votes
**Then**: Votes are recorded
```

### Issue: "Spec has 0 requirements"
**Solution**: Ensure requirements are under `## Requirements` header with proper format
```markdown
## Requirements

### The system SHALL [description]
...
```

### Issue: "Cannot locate openspec validate"
**Solution**: Ensure you're in project root and openspec CLI is installed
```bash
cd /path/to/wakeve
which openspec  # Check if installed
openspec validate <name> --strict
```

## Best Practices

### Writing Clear Requirements

**✅ Good**
```markdown
### The system SHALL enforce the voting deadline and prevent votes after the deadline

#### Scenario: Participant cannot vote after deadline
**Given**: Voting deadline is 2025-12-25 18:00:00Z
**When**: Participant tries to vote at 2025-12-25 18:00:01Z
**Then**: System rejects the vote with error message
```

**❌ Bad**
```markdown
### Deadline enforcement
Should prevent voting after deadline.
```

### Requirement Count

| Scope | Requirements | Examples |
|-------|--------------|----------|
| New capability | 10+ | `add-event-organization` |
| Feature update | 5-9 | `update-timezone-support` |
| Enhancement | 1-4 | `add-email-notifications` |
| Bug fix | 0 (skip spec) | Use git commits instead |

### Naming Conventions

**Change IDs**: `<verb>-<object>` (kebab-case)
- ✅ `add-event-organization`
- ✅ `update-rbac-permissions`  
- ✅ `refactor-sync-logic`
- ❌ `event-org` (too vague)
- ❌ `feature-123` (not specific)

**Capabilities**: `<domain>-<feature>` (kebab-case)
- ✅ `event-organization`
- ✅ `user-authentication`
- ✅ `destination-search`
- ❌ `feature1` (not descriptive)

## File Structure Reference

```
openspec/
├── README.md                    ← Overview and getting started
├── PROCESS.md                   ← Detailed workflow guide  
├── AGENTS.md                    ← This file
├── project.md                   ← Project conventions
├── specs/
│   ├── event-organization/
│   │   ├── spec.md             ← Main specification
│   │   ├── design.md           ← Implementation design (optional)
│   │   └── changes-v1.0.0.md   ← Version deltas (optional)
│   └── <capability>/
│       └── spec.md
└── changes/
    ├── add-event-organization/
    │   └── proposal.md         ← Change proposal
    └── <change-id>/
        └── proposal.md
```

## Summary

OpenSpec ensures:
1. Clear requirements before implementation
2. Testable acceptance criteria via scenarios
3. Approval gate before coding
4. Traceability from spec to code
5. Living documentation

**Follow [PROCESS.md](./PROCESS.md) for step-by-step guidance.**

---

**Last Updated**: 2025-11-12
**Version**: 2.0 (Simplified - no GitHub Projects required)
