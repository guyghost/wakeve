# Proposal: Implement Event Organization

## Change ID
`add-event-organization`

## Affected Spec
- **Spec**: `event-organization` (New capability)

## Related Links
- **Issue**: https://github.com/guyghost/wakeve/issues/2
- **Design**: https://github.com/guyghost/wakeve/blob/change/add-event-organization/docs/changes/add-event-organization/design.md
- **Specs**: https://github.com/guyghost/wakeve/blob/change/add-event-organization/docs/changes/add-event-organization/specs.md

## Why
The Wakeve application requires core functionality for event organizers to create events, set up availability polls, define deadlines, and validate final dates, as described in the domain context and requirements in `openspec/project.md` (Domain Context section, lines 44-66).

## What Changes
- Add event data models and domain logic in the shared Kotlin module
- Implement event creation and management UI in Jetpack Compose for Android
- Add poll creation, voting, and deadline management features
- Integrate timezone handling for global event planning
- Add organizer controls for validating final event dates

## Impact
- Affected specs: New event-organization capability
- Affected code: shared/src/commonMain, composeApp/src/androidMain, iosApp (future)
- Related issues: None

## Next Steps
After proposal approval, implement the event organization features following the design and specs, then create implementation PRs linked to the issue.