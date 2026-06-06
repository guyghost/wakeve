## 1. Discovery
- [x] 1.1 Audit current iOS navigation, tabs, toolbar patterns, and screen hierarchy.
- [x] 1.2 Audit existing iOS design tokens, glass modifiers, reusable components, empty states, and loading states.
- [x] 1.3 Identify which current screens map to Home, Groups, Messages, Profile, Event Detail, Vote, Transport, Participants, and Create Event.
- [x] 1.4 Document the implementation audit and migration order in `implementation-audit.md`.
- [x] 1.5 Document implementation slices and completion evidence in `execution-plan.md`.
- [x] 1.6 Document final acceptance checks in `acceptance-checklist.md`.
- [x] 1.7 Document the explicit approval decision in `approval-summary.md`.

## 2. Design System
- [x] 2.1 Update iOS tokens for semantic colors, dark/light backgrounds, typography, spacing, radius, blur, opacity, elevation, and motion.
- [x] 2.2 Implement or consolidate reusable components: LiquidGlassCard, LiquidGlassButton, LiquidGlassToolbar, LiquidGlassTabBar, EventHeroCard, EventListRow, ParticipantAvatarStack, VoteOptionCard, BottomSheet, EmptyState, and LoadingSkeleton.
- [x] 2.3 Add Liquid Glass availability handling and readable fallbacks for older iOS versions, Reduce Transparency, Reduce Motion, increased contrast, and dynamic type.

## 3. Navigation and Core Screens
- [x] 3.1 Refactor the iOS tab bar to contain only Home, Groups, Messages, and Profile.
- [x] 3.2 Move Create Event into contextual actions instead of a permanent tab.
- [x] 3.3 Refactor Home into a calm upcoming-events screen with greeting, featured event, upcoming list, floating glass create action, and polished empty/loading states.
- [x] 3.4 Refactor Event Detail with immersive image header, glass toolbar, metadata, participants preview, urgent next action, progressive sections, and compact message preview.

## 4. Flow Screens
- [x] 4.1 Refactor Vote flow around one question, progress, options, selected state, confirmation feedback, and a glass capsule next action.
- [x] 4.2 Refactor Transport around route/meeting point preview, times, participants, and one primary action.
- [x] 4.3 Refactor Participants into accepted, pending, and declined groups with contextual invite action.
- [x] 4.4 Refactor Messages into event-based conversations with search, unread states, compact previews, and event context.
- [x] 4.5 Refactor Create Event into a lightweight multi-step flow: name, date, place, invite people, confirm.

## 5. Verification
- [x] 5.1 Add or update Swift tests for create-event step state, tab model, and presentation mappings.
- [x] 5.2 Add UI verification for dark mode, light mode, empty states, loading states, and major navigation flows.
- [x] 5.3 Run the relevant iOS test suite or document any local Xcode/simulator blocker.
- [x] 5.4 Update concise docs or implementation notes explaining major design decisions.
