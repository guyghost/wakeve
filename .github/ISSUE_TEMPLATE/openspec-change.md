---
name: OpenSpec Change
about: Track implementation tasks for an OpenSpec change
title: '[Change] '
labels: 'openspec-change, in-progress'
assignees: ''
---

## Change ID
`<verb-led-id>` (e.g., `add-user-auth`, `refactor-payment-flow`)

## Overview
Brief description of what this change aims to accomplish.

## Documentation Links
- **GitHub Project Item**: [Project Item URL]
- **Proposal PR**: #<pr-number> (add after PR is created)

## Tasks
- [ ] Review current context (`openspec/project.md`, `openspec list`)
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
Any additional context, dependencies, or concerns.
