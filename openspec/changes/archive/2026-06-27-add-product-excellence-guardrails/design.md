## Context
Wakeve already has detailed specs for event organization, workflow coordination, AI-assisted drafting, cross-platform UX, collaboration, transport, budget, notifications, and platform design systems. The missing layer is a single product-quality contract that future proposals must satisfy before adding capabilities.

This change adds that contract without changing runtime code. It is intentionally cross-cutting: it guides future feature proposals, UI work, AI surfaces, collaboration scope, and review criteria.

## Goals / Non-Goals
- Goals:
  - Make Wakeve's event-organization mission enforceable in OpenSpec.
  - Provide a repeatable gate for future proposals and reviews.
  - Prevent generic social, chat, calendar, notes, and task-management drift.
  - Keep AI assistance focused on reducing mental load through reviewable, user-controlled suggestions.
  - Align Android, iOS, and web product work around clarity, speed, state confidence, and premium mobile execution.
- Non-Goals:
  - Do not modify runtime code, APIs, database schema, or existing specs in this change.
  - Do not replace detailed platform design-system specs.
  - Do not define implementation details for future AI, transport, lodging, budget, or collaboration features.

## Decisions
- Decision: Create a new `product-excellence` capability rather than modifying every related spec now.
  - Rationale: A new cross-cutting capability avoids conflicts with active changes and gives future proposals one stable reference.
- Decision: Treat `product-excellence` as a proposal and review gate.
  - Rationale: The most important behavior is preventing low-value features from entering implementation, not adding a runtime branch.
- Decision: Keep wording normative but product-level.
  - Rationale: Requirements must be testable in reviews and acceptance criteria, while leaving feature-specific specs room to define concrete APIs and UI models.

## Risks / Trade-offs
- Risk: The spec could become aspirational if future proposals do not reference it.
  - Mitigation: Add an explicit proposal gate requirement and tasks requiring future significant changes to document product-excellence fit.
- Risk: Product doctrine can slow small fixes.
  - Mitigation: Scope the gate to significant OpenSpec changes and user-facing feature work, not bug fixes, typos, or minor maintenance.
- Risk: Premium UX can be interpreted as decorative complexity.
  - Mitigation: Requirements define premium as clarity, speed, confidence, mobile polish, and purposeful visual design.

## Migration Plan
No runtime migration is required. After approval and archive, future significant OpenSpec proposals should include a short `product-excellence` fit note or equivalent acceptance criteria.

## Open Questions
- None for this proposal. Future feature proposals may define feature-specific metrics for speed, completion time, or mental-load reduction.
