# OpenSpec - Specification-Driven Development

This directory contains the OpenSpec specifications and change proposals for Wakeve.

## Quick Links

- **[Process Guide](./PROCESS.md)** ← Start here for how to create specs
- **[Project Context](./project.md)** - Wakeve project conventions and architecture
- **[Agent Definitions](./AGENTS.md)** - System agents and responsibilities
- **[Specs](./specs/)** - Capability specifications
- **[Changes](./changes/)** - Change proposals

## What is OpenSpec?

OpenSpec is a lightweight specification-driven development process that:

1. **Clarifies requirements** before implementation (no surprises)
2. **Provides testable acceptance criteria** via scenarios
3. **Creates an approval gate** before coding begins
4. **Maintains traceability** from spec to code to tests
5. **Generates living documentation** that stays current

## Key Concepts

### Capability
A major feature or system component (e.g., "event-organization", "user-auth")
- Contains: Purpose, core concepts, 10+ requirements
- Lives in: `specs/<capability>/spec.md`
- Example: `openspec/specs/event-organization/spec.md`

### Change
A modification to one or more capabilities
- Types: ADD, UPDATE, REFACTOR, REMOVE
- Contains: Proposal and detailed requirements
- Lives in: `changes/<change-id>/`
- Example: `openspec/changes/add-event-organization/`

### Requirement
A specific, testable statement of what the system SHALL do
- Format: "The system SHALL [verb] [object]"
- Must include: At least one scenario with Given-When-Then
- Example: "The system SHALL enforce the voting deadline"

### Scenario
A concrete example demonstrating a requirement
- Format: Given-When-Then (what's true, what happens, what's expected)
- Purpose: Makes requirements unambiguous and testable

## Getting Started

### Create a New Specification

1. **Read**: [Process Guide](./PROCESS.md) (5 minutes)
2. **Create GitHub Issue** with change description
3. **Create proposal**: `openspec/changes/<change-id>/proposal.md`
4. **Create spec**: `openspec/specs/<capability>/spec.md`
5. **Write requirements** (minimum 10 for new features)
6. **Validate**: `openspec validate <change-id> --strict`
7. **Submit PR** with link to GitHub issue
8. **Get approval** before implementing

### Example Workflow

```bash
# 1. Create feature branch
git checkout -b change/add-event-organization

# 2. Create directories
mkdir -p openspec/changes/add-event-organization
mkdir -p openspec/specs/event-organization

# 3. Create proposal
cat > openspec/changes/add-event-organization/proposal.md << 'EOF'
# Proposal: Add Event Organization

## Change ID
`add-event-organization`

## Affected Spec
- `event-organization` (new)

## Why
Enable event organizers to create collaborative events...

## What Changes
- Event creation with time slots
- Participant invitation
- Availability polling
EOF

# 4. Create spec
cat > openspec/specs/event-organization/spec.md << 'EOF'
# Specification: Event Organization

## Purpose
Enable organizers to create events...

## Requirements
(write 10+ requirements with scenarios)
EOF

# 5. Validate
openspec validate add-event-organization --strict

# 6. Commit
git add openspec/
git commit -m "docs: add OpenSpec for add-event-organization"
git push origin change/add-event-organization

# 7. Create PR on GitHub
```

## File Structure

```
openspec/
├── README.md                          ← You are here
├── PROCESS.md                         ← How to create specs
├── AGENTS.md                          ← Agent definitions
├── project.md                         ← Project conventions
├── specs/
│   ├── event-organization/
│   │   ├── spec.md                   ← Main specification
│   │   ├── design.md                 ← Implementation design (optional)
│   │   └── changes-v1.0.0.md         ← Version-specific deltas (optional)
│   ├── user-authentication/
│   │   └── spec.md
│   └── destination-search/
│       └── spec.md
└── changes/
    ├── add-event-organization/
    │   └── proposal.md               ← Change proposal
    ├── update-rbac-permissions/
    │   └── proposal.md
    └── refactor-sync-logic/
        └── proposal.md
```

## Capabilities

### Current Capabilities

| Capability | Version | Status | Files |
|-----------|---------|--------|-------|
| event-organization | 1.0.0 | Draft | [spec.md](specs/event-organization/spec.md) |

### Planned Capabilities

These are part of Wakeve's roadmap but not yet specified:
- `user-authentication` - OAuth, permission management
- `destination-search` - Find and recommend destinations
- `lodging-recommendations` - Hotel and accommodation suggestions
- `transport-suggestions` - Flight/train/ride options
- `meeting-integration` - Zoom, Meet, FaceTime links
- `payment-tracking` - Cost splitting and Tricount integration
- `offline-sync` - Local caching and conflict resolution

## Validation

All specs are validated using OpenSpec:

```bash
# Validate a specific spec
openspec validate event-organization --strict

# Validate a specific change
openspec validate add-event-organization --strict

# Validate all specs
openspec validate --specs --strict

# Validate all changes
openspec validate --changes --strict
```

**Validation checks:**
- ✅ Spec has `## Purpose` section
- ✅ Spec has `## Requirements` section
- ✅ Requirements contain `SHALL` or `MUST`
- ✅ Each requirement has `#### Scenario:`
- ✅ Scenarios use Given-When-Then format

## Best Practices

### Writing Requirements

**Good ✅**
```markdown
### The system SHALL allow organizers to create events with multiple time slots

#### Scenario: Organizer creates event
**Given**: Organizer is authenticated
**When**: Organizer fills in title, description, and selects 3 time slots
**Then**: Event is created with DRAFT status and assigned a unique ID
```

**Bad ❌**
```markdown
### Event creation

Should allow creating events with time slots.
```

### Requirement Count

- **10+ requirements**: Major new capability
- **5-9 requirements**: Update to existing capability
- **1-4 requirements**: Small enhancement or bug fix

### Naming

**Change IDs**: verb + object (kebab-case)
- ✅ `add-event-organization`
- ✅ `update-timezone-support`
- ✅ `refactor-sync-logic`

**Capabilities**: domain + feature (kebab-case)
- ✅ `event-organization`
- ✅ `user-authentication`
- ✅ `payment-tracking`

## Integration with Code

Once a spec is approved and implemented:

1. **Reference requirements in code**
   ```kotlin
   // REQ-EVT-001: Organizers SHALL be able to create events
   fun createEvent(event: Event): Result<Event> { ... }
   ```

2. **Write tests per requirement scenario**
   ```kotlin
   @Test
   fun REQ_EVT_001_organizerCanCreateEvent() { ... }
   ```

3. **Update spec for clarifications only**
   - Non-breaking clarifications are acceptable
   - Major changes require a new change proposal

## Resources

- **[OpenSpec Official Docs](https://openspec.ai/docs)** - External documentation
- **[Proposal Template](./changes/add-event-organization/proposal.md)** - Example proposal
- **[Spec Template](./specs/event-organization/spec.md)** - Example specification

## FAQ

**Q: Do I need OpenSpec for bug fixes?**
A: No. Use git commits with clear messages instead.

**Q: Can I implement before the spec is approved?**
A: No. The approval of the spec is a gate before implementation.

**Q: What if requirements change during implementation?**
A: Update the spec and create a new change proposal. Document the reason.

**Q: How detailed should requirements be?**
A: Detailed enough to be testable and unambiguous. Scenarios help clarify.

**Q: Can I have fewer than 10 requirements?**
A: Yes, but only for small features. Major new capabilities should have 10+.

**Q: What format should scenarios use?**
A: Given-When-Then format is most readable and testable.

## Contributing

When adding a new capability or change:

1. Follow the [Process Guide](./PROCESS.md)
2. Write clear requirements with scenarios
3. Run validation before submitting PR
4. Link PR to GitHub issue
5. Get approval before implementing

---

**Last Updated**: 2025-11-12
**Version**: 1.0 (Simplified - no GitHub Projects required)
