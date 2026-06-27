# Change: Add Product Excellence Guardrails

## Why
Wakeve's long-term ambition is broader than feature delivery: the product must become the default way close groups organize private events while reducing the coordination work currently spread across chat, calendars, notes, maps, payments, and reminders.

Existing specs define many capabilities, but they do not yet provide one cross-cutting product gate for deciding whether a new feature genuinely helps a group prepare an event. This change makes that gate explicit and enforceable before runtime implementation.

## What Changes
- Add a new `product-excellence` capability as the canonical product doctrine for Wakeve.
- Define requirements that keep Wakeve focused on event organization rather than becoming a social network, chat app, task manager, calendar clone, or generic workspace.
- Require future event screens and features to expose the current decision state, pending work, responsible actors, and next useful action.
- Require AI-assisted experiences to reduce organizer effort through reviewable suggestions without automatically creating, sending, inviting, modifying, or persisting user-visible work.
- Require mobile-first speed, clarity, premium feel, and one clear intention per screen.
- Require collaboration features to stay scoped to event decisions, logistics, and preparation rather than generic messaging.
- Add a proposal review gate so future significant OpenSpec changes state how they satisfy `product-excellence`.

## Product Excellence Fit
This change is the product-excellence gate itself. It codifies Wakeve's event-organization boundary, mental-load reduction standard, state-clarity standard, AI reviewability rule, mobile-first premium interaction standard, and generic-product-drift rejection rule.

## Impact
- Affected specs: `product-excellence` (new)
- Related specs: `event-organization`, `cross-platform-organization-ux`, `wakeve-ai`, `collaboration-management`, `ios-design-system`, `android-ui-system`
- Affected code: none in this change
- Public API: unchanged
- Database schema: unchanged
- Runtime behavior: unchanged until future feature-specific proposals implement these requirements
