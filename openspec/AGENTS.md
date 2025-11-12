# OpenSpec Instructions

Instructions for AI coding assistants using OpenSpec for spec-driven development with GitHub workflow.

## TL;DR Quick Checklist

- Search existing work: `openspec spec list --long`, `openspec list` (use `rg` only for full-text search)
- Decide scope: new capability vs modify existing capability
- Pick a unique `change-id`: kebab-case, verb-led (`add-`, `update-`, `remove-`, `refactor-`)
- Create GitHub Issue with change ID and tasks checklist
- Create GitHub Project item with design and specs in description
- Write spec deltas in project: use `## ADDED|MODIFIED|REMOVED|RENAMED Requirements`; include at least one `#### Scenario:` per requirement
- Validate: `openspec validate [change-id] --strict` and fix issues locally
- Create feature branch `change/<change-id>` and scaffold `proposal.md`
- Submit Pull Request linking to issue and project item
- Request approval: Do not start implementation until proposal PR is approved

## Three-Stage Workflow

### Stage 1: Creating Changes
Create proposal when you need to:
- Add features or functionality
- Make breaking changes (API, schema)
- Change architecture or patterns
- Optimize performance (changes behavior)
- Update security patterns

Triggers (examples):
- "Help me create a change proposal"
- "Help me plan a change"
- "Help me create a proposal"
- "I want to create a spec proposal"
- "I want to create a spec"

Loose matching guidance:
- Contains one of: `proposal`, `change`, `spec`
- With one of: `create`, `plan`, `make`, `start`, `help`

Skip proposal for:
- Bug fixes (restore intended behavior)
- Typos, formatting, comments
- Dependency updates (non-breaking)
- Configuration changes
- Tests for existing behavior

**GitHub Workflow**
1. Review `openspec/project.md`, `openspec list`, and `openspec list --specs` to understand current context.
2. Create a GitHub Issue with:
    - Title: `[Change] <Verb-led description>`
    - Change ID in body (kebab-case, verb-led)
    - Tasks checklist
    - Labels: `openspec-change`, `in-progress`
3. Create GitHub Project item with:
    - Title: `<change-id>` - `<Verb-led description>`
    - Description containing:
      - **Design section**: Technical design, architecture, decisions
      - **Specs section**: Spec deltas using `## ADDED|MODIFIED|REMOVED Requirements` with at least one `#### Scenario:` per requirement
4. Update Issue with link to project item
5. Run `openspec validate <id> --strict` locally and resolve any issues
6. Create feature branch: `git checkout -b change/<change-id>`
7. Create `openspec/changes/<change-id>/proposal.md` with:
    - Overview and rationale
    - Links to issue and project item
    - Summary of changes
8. Commit with message: `[#<issue-number>] Add proposal for <change-id>`
9. Create Pull Request:
    - Title: `[Proposal] <Verb-led description>`
    - Link to issue: `Relates to #<issue-number>`
    - Link to project item in description
    - Labels: `proposal`, `needs-review`
10. Wait for approval before implementation

### Stage 2: Implementing Changes
Track these steps as TODOs and complete them one by one.
1. **Approval gate** - Do not start until proposal PR is approved and merged
2. **Read proposal.md** - Understand what's being built
3. **Read project item design** - Review technical decisions from GitHub Project description
4. **Read issue tasks** - Get implementation checklist
5. **Implement tasks sequentially** - Complete in order, updating issue checklist
6. **Create implementation PRs** - Link back to original issue with `[#<issue-number>]`
7. **Confirm completion** - Ensure every item in issue checklist is finished
8. **Close issue** - When all implementation PRs are merged

### Stage 3: Archiving Changes
After deployment, create separate PR to:
- Move `changes/[name]/` → `changes/archive/YYYY-MM-DD-[name]/`
- Update `specs/` if capabilities changed
- Update project item status to mark as complete/archived
- Use `openspec archive [change] --skip-specs --yes` for tooling-only changes
- Run `openspec validate --strict` to confirm the archived change passes checks

## Before Any Task

**Context Checklist:**
- [ ] Read relevant specs in `specs/[capability]/spec.md`
- [ ] Check pending changes in `changes/` for conflicts
- [ ] Check active GitHub Issues for related work
- [ ] Read `openspec/project.md` for conventions
- [ ] Run `openspec list` to see active changes
- [ ] Run `openspec list --specs` to see existing capabilities

**Before Creating Specs:**
- Always check if capability already exists
- Prefer modifying existing specs over creating duplicates
- Use `openspec show [spec]` to review current state
- Check GitHub Projects for existing design docs
- If request is ambiguous, ask 1–2 clarifying questions before scaffolding

### Search Guidance
- Enumerate specs: `openspec spec list --long` (or `--json` for scripts)
- Enumerate changes: `openspec list` (or `openspec change list --json` - deprecated but available)
- Show details:
    - Spec: `openspec show <spec-id> --type spec` (use `--json` for filters)
    - Change: `openspec show <change-id> --json --deltas-only`
- Full-text search (use ripgrep): `rg -n "Requirement:|Scenario:" openspec/specs`
- Check GitHub: Search issues and wiki for related work

## Quick Start

### CLI Commands

```bash
# Essential commands
openspec list                  # List active changes
openspec list --specs          # List specifications
openspec show [item]           # Display change or spec
openspec diff [change]         # Show spec differences
openspec validate [item]       # Validate changes or specs
openspec archive [change] [--yes|-y]      # Archive after deployment (add --yes for non-interactive runs)

# Project management
openspec init [path]           # Initialize OpenSpec
openspec update [path]         # Update instruction files

# Interactive mode
openspec show                  # Prompts for selection
openspec validate              # Bulk validation mode

# Debugging
openspec show [change] --json --deltas-only
openspec validate [change] --strict
```

### Command Flags

- `--json` - Machine-readable output
- `--type change|spec` - Disambiguate items
- `--strict` - Comprehensive validation
- `--no-interactive` - Disable prompts
- `--skip-specs` - Archive without spec updates
- `--yes`/`-y` - Skip confirmation prompts (non-interactive archive)

## Directory Structure

```
openspec/
├── project.md              # Project conventions
├── specs/                  # Current truth - what IS built
│   └── [capability]/       # Single focused capability
│       ├── spec.md         # Requirements and scenarios
│       └── design.md       # Technical patterns
├── changes/                # Proposals - what SHOULD change
│   ├── [change-name]/
│   │   └── proposal.md     # Why, what, impact (links to issue & wiki)
│   └── archive/            # Completed changes

GitHub (separate from repository):
├── Issues/
│   └── #N [Change] <description>    # Task tracking with checklist
├── Projects/
│   └── Change: <change-id>
│       └── [Description with Design + Specs sections]
└── Pull Requests/
    └── #M [Proposal] <description>  # Proposal review
```

## Creating Change Proposals

### Decision Tree

```
New request?
├─ Bug fix restoring spec behavior? → Fix directly
├─ Typo/format/comment? → Fix directly
├─ New feature/capability? → Create GitHub Issue + Project + PR
├─ Breaking change? → Create GitHub Issue + Project + PR
├─ Architecture change? → Create GitHub Issue + Project + PR
└─ Unclear? → Create GitHub Issue + Project + PR (safer)
```

### GitHub Issue Structure

Create issue with template or manually:

```markdown
## Change ID
`<change-id>` (e.g., `add-user-auth`, `refactor-payment-flow`)

## Overview
[1-2 sentences on problem/opportunity]

## Documentation Links
- **Project Item**: [GitHub Project Item Link]
- **Proposal PR**: #<pr-number> (add after PR is created)

## Tasks
- [ ] Review current context
- [ ] Create GitHub Project item with design and specs
- [ ] Draft spec deltas with scenarios
- [ ] Run `openspec validate <id> --strict`
- [ ] Create proposal PR
- [ ] Address review feedback
- [ ] Update affected specifications
- [ ] Final validation passes
- [ ] Documentation updated
- [ ] PR merged

## Validation Status
```bash
# Run this command and paste results
openspec validate <id> --strict
```

## Notes
[Additional context, dependencies, or concerns]
```

### GitHub Project Item Structure

Create item in GitHub Project with the following structure in the **Description** field:

```markdown
# <change-id>: <Verb-led description>

> **Change ID**: `<change-id>`
> **Issue**: #<issue-number>
> **Status**: Draft | Under Review | Approved | Implemented

## Design

### Context
[Background, constraints, stakeholders]

### Goals / Non-Goals
- Goals: [...]
- Non-Goals: [...]

### Architecture
[Components, data flow, sequence diagrams]

### Technical Decisions
#### Decision 1: <Title>
**Context**: Why this decision is needed
**Options Considered**:
- Option A: Pros and cons
- Option B: Pros and cons
**Decision**: Which option was chosen
**Rationale**: Why this option is best

### Implementation Approach
[Phases, steps, milestones]

### Risks & Mitigation
| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| ...  | ...    | ...        | ...        |

### Migration Plan
[Steps, rollback strategy]

### Open Questions
- [ ] Question 1?
- [ ] Question 2?

---

## Specs

### Affected Specifications
- `openspec/specs/<spec-1>.md` - Brief description
- `openspec/specs/<spec-2>.md` - Brief description

### ADDED Requirements
#### REQ-<ID>: <Requirement Title>
**Priority**: High | Medium | Low
**Category**: Functional | Non-Functional | Security | Performance

**Description**:
The system SHALL provide...

##### Scenario: Success case
**Given**: Initial conditions
**When**: Action or trigger
**Then**: Expected outcome

**Acceptance Criteria**:
- [ ] Criterion 1
- [ ] Criterion 2

### MODIFIED Requirements
#### REQ-<ID>: <Existing Requirement>
**Original Spec**: `openspec/specs/<spec-name>.md`

**Previous Version**:
```
[Copy previous requirement text]
```

**Updated Version**:
```
[Complete modified requirement with all scenarios]
```

**Rationale**: Why this change is needed

##### Scenario: Updated behavior
**Given**: Initial conditions
**When**: Action
**Then**: Expected outcome

### REMOVED Requirements
#### REQ-<ID>: <Requirement Being Removed>
**Original Spec**: `openspec/specs/<spec-name>.md`

**Requirement Text**:
```
[Copy requirement being removed]
```

**Rationale**: Why no longer needed
**Impact Analysis**: Who/what is affected
**Migration Path**: How to handle existing implementations

### Validation Results
```bash
$ openspec validate <id> --strict
[Paste validation output]
```
```

### Local proposal.md Structure

Create in `openspec/changes/<change-id>/proposal.md`:

```markdown
# Proposal: <Change Description>

## Change ID
`<change-id>`

## Related Links
- **Issue**: #<issue-number>
- **GitHub Project**: [Project Item URL]

## Why
[1-2 sentences on problem/opportunity]

## What Changes
- [Bullet list of changes]
- [Mark breaking changes with **BREAKING**]

## Impact
- Affected specs: [list capabilities]
- Affected code: [key files/systems]
- Related issues: [link to related work]

## Next Steps
[What happens after proposal approval]
```

## Spec File Format

### Critical: Scenario Formatting

**CORRECT** (use #### headers):
```markdown
#### Scenario: User login success
**Given**: User has valid credentials
**When**: User submits login form
**Then**: System returns JWT token and redirects to dashboard
```

**WRONG** (don't use bullets or bold for header):
```markdown
- **Scenario: User login**  ❌
**Scenario**: User login     ❌
### Scenario: User login      ❌
```

Every requirement MUST have at least one scenario with Given-When-Then format.

### Requirement Wording
- Use SHALL/MUST for normative requirements (avoid should/may unless intentionally non-normative)

### Delta Operations

- `## ADDED Requirements` - New capabilities
- `## MODIFIED Requirements` - Changed behavior (must include full updated requirement)
- `## REMOVED Requirements` - Deprecated features
- `## RENAMED Requirements` - Name changes

Headers matched with `trim(header)` - whitespace ignored.

#### When to use ADDED vs MODIFIED
- **ADDED**: Introduces a new capability or sub-capability that can stand alone as a requirement. Prefer ADDED when the change is orthogonal (e.g., adding "Slash Command Configuration") rather than altering the semantics of an existing requirement.
- **MODIFIED**: Changes the behavior, scope, or acceptance criteria of an existing requirement. Always paste the full, updated requirement content (header + all scenarios). The archiver will replace the entire requirement with what you provide here; partial deltas will drop previous details.
- **RENAMED**: Use when only the name changes. If you also change behavior, use RENAMED (name) plus MODIFIED (content) referencing the new name.

Common pitfall: Using MODIFIED to add a new concern without including the previous text. This causes loss of detail at archive time. If you aren't explicitly changing the existing requirement, add a new requirement under ADDED instead.

Authoring a MODIFIED requirement correctly:
1. Locate the existing requirement in `openspec/specs/<capability>/spec.md`.
2. Copy the entire requirement block (from `### Requirement: ...` through its scenarios).
3. Paste it in the wiki specs page under `## MODIFIED Requirements` and edit to reflect the new behavior.
4. Ensure the header text matches exactly (whitespace-insensitive) and keep at least one `#### Scenario:`.

Example for RENAMED:
```markdown
## RENAMED Requirements
- FROM: `### Requirement: Login`
- TO: `### Requirement: User Authentication`
```

## Troubleshooting

### Common Errors

**"Change must have at least one delta"**
- Check wiki specs page exists and has content
- Verify page has operation prefixes (## ADDED Requirements)
- Ensure proposal.md references wiki correctly

**"Requirement must have at least one scenario"**
- Check scenarios use `#### Scenario:` format (4 hashtags)
- Verify Given-When-Then structure
- Don't use bullet points or bold for scenario headers

**"Cannot find issue or wiki pages"**
- Ensure GitHub Wiki is enabled in repository settings
- Check wiki page paths match pattern: `changes/<id>/design` and `changes/<id>/specs`
- Verify issue exists and is properly linked

**Silent scenario parsing failures**
- Exact format required: `#### Scenario: Name`
- Debug with: `openspec show [change] --json --deltas-only`

### Validation Tips

```bash
# Always use strict mode for comprehensive checks
openspec validate [change] --strict

# Debug delta parsing
openspec show [change] --json | jq '.deltas'

# Check specific requirement
openspec show [spec] --json -r 1
```

## Happy Path Script

```bash
# 1) Explore current state
openspec spec list --long
openspec list
# Optional full-text search:
# rg -n "Requirement:|Scenario:" openspec/specs

# 2) Create GitHub Issue
# - Use issue template or create manually
# - Title: [Change] Add two-factor authentication
# - Change ID: add-two-factor-auth
# - Add tasks checklist
# Note the issue number (e.g., #42)

# 3) Create GitHub Project item
# Navigate to GitHub Project
# Create new item: add-two-factor-auth
# In Description add Design + Specs sections:
# - Add ADDED/MODIFIED/REMOVED requirements
# - Include scenarios with Given-When-Then

# 4) Update issue with project item link

# 5) Validate locally
CHANGE=add-two-factor-auth
openspec validate $CHANGE --strict

# 6) Create feature branch and proposal
git checkout -b change/$CHANGE
mkdir -p openspec/changes/$CHANGE
cat > openspec/changes/$CHANGE/proposal.md << 'EOF'
# Proposal: Add Two-Factor Authentication

## Change ID
`add-two-factor-auth`

## Related Links
- **Issue**: #42
- **GitHub Project**: [Link to project item]

## Why
Enhance security by requiring second factor during login.

## What Changes
- Add OTP verification step to authentication flow
- Integrate with authenticator apps
- Add backup codes for recovery

## Impact
- Affected specs: auth
- Affected code: AuthService, LoginController
EOF

# 7) Commit and push
git add openspec/changes/$CHANGE/proposal.md
git commit -m "[#42] Add proposal for two-factor authentication"
git push -u origin change/$CHANGE

# 8) Create Pull Request
# - Title: [Proposal] Add two-factor authentication
# - Body: Links to issue #42 and wiki pages
# - Labels: proposal, needs-review

# 9) Wait for approval, then implement
```

## Multi-Capability Example

```
GitHub Issue #42: [Change] Add 2FA with notifications

GitHub Project Item: add-2fa-notify
├── Design section (architecture across auth and notifications)
└── Specs section (combined spec deltas with ADDED/MODIFIED/REMOVED)

Repository:
openspec/changes/add-2fa-notify/
└── proposal.md       # Links to issue #42 and project item
```

In project item description (Specs section):
```markdown
## ADDED Requirements

### REQ-AUTH-010: Two-Factor Authentication
The system SHALL require a second factor during login.

#### Scenario: OTP required
**Given**: User has valid credentials
**When**: User provides username and password
**Then**: System prompts for OTP code

### REQ-NOTIF-005: OTP Email Notification
The system SHALL send OTP codes via email.

#### Scenario: OTP delivery
**Given**: User requires OTP
**When**: System generates OTP
**Then**: Email is sent to user's registered address
```

## Best Practices

### Simplicity First
- Default to <100 lines of new code
- Single-file implementations until proven insufficient
- Avoid frameworks without clear justification
- Choose boring, proven patterns

### Complexity Triggers
Only add complexity with:
- Performance data showing current solution too slow
- Concrete scale requirements (>1000 users, >100MB data)
- Multiple proven use cases requiring abstraction

### Clear References
- Use `file.ts:42` format for code locations
- Reference specs as `specs/auth/spec.md`
- Link GitHub issues with `#42` format
- Link PRs in commit messages: `[#42]`
- Link wiki pages in proposals and PRs

### Capability Naming
- Use verb-noun: `user-auth`, `payment-capture`
- Single purpose per capability
- 10-minute understandability rule
- Split if description needs "AND"

### Change ID Naming
- Use kebab-case, short and descriptive: `add-two-factor-auth`
- Prefer verb-led prefixes: `add-`, `update-`, `remove-`, `refactor-`
- Ensure uniqueness; if taken, append `-2`, `-3`, etc.
- Match GitHub branch name: `change/<change-id>`

### GitHub Integration
- Always link issues, PRs, and wiki pages bidirectionally
- Use commit message format: `[#<issue>] <description>`
- Update issue checklist as tasks complete
- Keep wiki pages up-to-date with latest decisions
- Archive wiki pages when change is complete

## Tool Selection Guide

| Task | Tool | Why |
|------|------|-----|
| Find files by pattern | Glob | Fast pattern matching |
| Search code content | Grep | Optimized regex search |
| Read specific files | Read | Direct file access |
| Explore unknown scope | Task | Multi-step investigation |
| Check related work | GitHub Search | Issues, PRs, wiki |

## Error Recovery

### Change Conflicts
1. Run `openspec list` to see active changes
2. Check GitHub issues for related work
3. Check for overlapping specs in wiki
4. Coordinate with change owners
5. Consider combining proposals

### Validation Failures
1. Run with `--strict` flag
2. Check JSON output for details
3. Verify spec file format in wiki
4. Ensure scenarios properly formatted with Given-When-Then
5. Update wiki and re-validate

### Missing Context
1. Read project.md first
2. Check related specs
3. Review GitHub wiki for design docs
4. Check related issues and PRs
5. Review recent archives
6. Ask for clarification

### GitHub Integration Issues
- **Project not accessible**: Ensure Project is enabled and visible
- **Cannot find issue**: Check issue number and status
- **PR not linking**: Use `Relates to #N` or `Closes #N` in description
- **Project item not found**: Verify item exists and has correct title format

## Quick Reference

### Stage Indicators
- `GitHub Issue` - Tasks to complete
- `GitHub Project` - Design and spec deltas
- `Pull Request` - Proposal review
- `openspec/changes/` - Proposal files (links to issue/project)
- `openspec/specs/` - Built and deployed
- `changes/archive/` - Completed changes

### File Purposes
- `proposal.md` (local) - Why and what, links to issue/project
- GitHub Issue - Task tracking and checklist
- Project Item Description - Technical decisions and architecture with ADDED/MODIFIED/REMOVED requirements

### CLI Essentials
```bash
openspec list              # What's in progress?
openspec show [item]       # View details
openspec diff [change]     # What's changing?
openspec validate --strict # Is it correct?
openspec archive [change] [--yes|-y]  # Mark complete (add --yes for automation)
```

### GitHub Essentials
```bash
# Create issue (GitHub UI or CLI)
gh issue create --title "[Change] Add feature" --label "openspec-change"

# Create branch
git checkout -b change/<change-id>

# Commit with issue reference
git commit -m "[#42] Add proposal for <change>"

# Create PR
gh pr create --title "[Proposal] Add feature" --body "Relates to #42"

# Update issue
gh issue edit 42 --add-label "approved"

# Close issue
gh issue close 42 --comment "Implementation complete"
```

Remember:
- **Issues** track the work
- **Wiki** documents the details
- **PRs** propose the changes
- **Specs** are the truth

Keep them all in sync and linked together.
