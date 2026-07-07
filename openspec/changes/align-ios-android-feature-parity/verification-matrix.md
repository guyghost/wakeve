# iOS/Android Parity Verification Matrix

This matrix defines the evidence required before the goal "iOS feature parity with Android" can be considered complete. It complements `parity-audit.md`: the audit records current gaps, while this file records the proof expected after implementation.

## Route Classification

| Android route | Required iOS outcome | Completion evidence |
| --- | --- | --- |
| `home` | Native iOS Home route remains reachable. | iOS route inventory test classifies Home as present and UI navigation test opens it after auth. |
| `events` | Classified as Android-only alias or mapped to iOS Home/event list. | Parity inventory documents whether Android `Events` is actively routed; if active, iOS route model has an equivalent event-list destination. |
| `explore` | Native iOS Explore/Groups/Templates route remains reachable with approved naming. | Localization parity test accepts the platform naming decision and UI test opens the route. |
| `profile` | Native iOS Profile route remains reachable. | Route inventory and UI navigation test prove Profile opens without nested placeholder fallback. |
| `splash`, `get_started`, `auth`, `auth/email`, `login`, `onboarding` | iOS auth/onboarding model is classified as equivalent or intentionally different. | Auth route classification documents platform difference and tests cover post-auth navigation from deep links. |
| `event_creation` | iOS creation route opens `CreateEventSheet` or the obsolete placeholder state is removed. | Contract test proves `AppView.eventCreation` no longer renders `navigation.placeholder.event_creation`; UI test opens create flow from Home and a deep link/route entry if supported. |
| `event_planning_assistant` | Classified as covered by `add-on-device-wakeve-ai` or routed to the completed iOS WakeveAI surface. | Parity inventory references the active AI change and fails if the route is neither implemented nor classified as pending external change. |
| `event/{eventId}` | Native iOS Event Detail route remains reachable. | Deep-link test and manual navigation UI test open the same event by id. |
| `event/{eventId}/participants` | Native iOS participant management route remains reachable. | Route/deep-link tests cover allowed, access-denied, and missing-event states. |
| `event/{eventId}/poll/vote` | Native iOS poll voting route remains reachable. | Deep-link and UI tests cover voting when allowed and blocked/finalized states. |
| `event/{eventId}/poll/results` | Native iOS poll results route remains reachable. | Route test covers organizer and participant result visibility states. |
| `event/{eventId}/scenarios` | Native iOS scenario list route remains reachable. | Route test opens scenarios and verifies repository refresh preserves the selected event. |
| `event/{eventId}/scenario/{scenarioId}` | Native iOS scenario detail route is implemented. | Contract/UI tests cover status, votes, final selection, missing scenario, access-denied, and navigation to meetings/transport. |
| `event/{eventId}/scenarios/compare` | Native iOS scenario comparison route remains reachable. | Route test opens comparison and verifies final selection updates shared state. |
| `event/{eventId}/scenarios/manage` | iOS has an explicit route classification for scenario management. | Inventory test classifies it as a dedicated iOS route or a documented native consolidation into scenario organization. |
| `event/{eventId}/budget` | Native iOS budget overview route remains reachable. | Route and access tests cover organizing/finalized/access-denied states. |
| `event/{eventId}/budget/{budgetItemId}` | Native iOS budget detail route remains reachable, including comments context where applicable. | Route test opens a budget item and comment context test opens item comments without generic inbox fallback. |
| `event/{eventId}/payment` | Native iOS payment pot route remains reachable. | Route test covers organizer editable, participant visible, finalized read-only, and access-denied states. |
| `event/{eventId}/tricount` | Native iOS Tricount route remains reachable. | Route test covers configured, unavailable, finalized read-only, and access-denied states. |
| `event/{eventId}/accommodation` | Dedicated iOS accommodation route replaces generic scenario routing or gets a product-approved unavailable state. | Contract/UI tests prove the route is not `ScenarioOrganizationView` unless documented as a deliberate consolidation with accommodation-specific state and actions. |
| `event/{eventId}/meals` | Native iOS meal planning route replaces placeholder. | UI tests cover empty, editable, pending-sync, finalized read-only, and access-denied states. |
| `event/{eventId}/equipment` | Native iOS equipment checklist route replaces placeholder. | UI tests cover item assignment, pending-sync, finalized read-only, and access-denied states. |
| `event/{eventId}/activities` | Native iOS activity planning route replaces placeholder. | UI tests cover planned activity list, organizer edits, participant visibility, finalized read-only, and access-denied states. |
| `event/{eventId}/transport` | Native iOS transport route remains reachable. | Route test proves selected destination/readiness state survives event refresh and finalized state is read-only. |
| `event/{eventId}/comments` | Native iOS comments route is connected. | Tests cover event-level comments, section/item comments, moderation/reporting, offline pending comments, and access-denied states. |
| `event/{eventId}/photos` | Native iOS photos/follow-up route is implemented or explicitly deferred with non-placeholder unavailable state. | UI test proves the route never renders generic construction copy and the documented state gives an event-scoped next action. |
| `inbox` | Native iOS Inbox/Messages route remains reachable. | Route test opens inbox and notification tests prove notification-list route is not silently redirected unless classified. |
| `event/{eventId}/meetings` | Native iOS meeting list route remains reachable. | Route/access tests cover organizer creation affordance, participant visibility, finalized read-only, and provider-unavailable states. |
| `meeting/{meetingId}` | Native iOS meeting detail route remains reachable and can launch safe stored links. | Meeting provider tests cover safe URL launch, unsafe URL warning/blocking, provider mismatch, and missing meeting. |
| `event/{eventId}/invite` | iOS invitation share route is explicit, not only incidental sheet state. | Route/UI tests open invitation sharing from route model and verify unauthenticated invite links still work. |
| `settings` | Native iOS settings/account route is explicit or documented as profile-contained with equivalent deep-link handling. | Deep-link and route tests open settings or profile-contained settings without losing the requested destination. |
| `notifications` | Native iOS notification list route or documented Inbox mapping exists. | Notification route tests cover all/unread filters and ensure event-targeting taps do not fall back to a generic inbox. |
| `notifications/preferences` | Native iOS notification preferences route is explicit. | Tests verify Wakeve preference state and iOS system permission state are both visible. |
| `leaderboard` | Existing iOS leaderboard view is route-connected. | Route/UI test opens leaderboard from explicit route and verifies shared gamification state. |
| `organizer_dashboard` | iOS organizer dashboard route is implemented or deliberately folded into Home/Profile with route-preserving behavior. | Route/UI test opens organizer event states and verifies confirmed/pending/action summaries. |

## Deep-Link Evidence

The iOS deep-link parser and route handler must have tests for:

- `wakeve://event/create`
- `wakeve://event/{eventId}`
- `wakeve://event/{eventId}/details?tab=comments|budget|participants`
- `wakeve://event/{eventId}/participants`
- `wakeve://event/{eventId}/poll`
- `wakeve://event/{eventId}/poll/vote`
- `wakeve://event/{eventId}/poll/results`
- `wakeve://event/{eventId}/scenarios`
- `wakeve://event/{eventId}/scenarios/compare`
- `wakeve://event/{eventId}/scenarios/manage`
- `wakeve://event/{eventId}/scenario/{scenarioId}`
- `wakeve://event/{eventId}/budget`
- `wakeve://event/{eventId}/budget/{budgetItemId}`
- `wakeve://event/{eventId}/meetings`
- `wakeve://event/{eventId}/comments`
- `wakeve://event/{eventId}/invite`
- `wakeve://poll/{eventId}`
- `wakeve://meeting/{meetingId}`
- `wakeve://invite/{token}`
- `wakeve://home`
- `wakeve://profile`
- `wakeve://settings`
- `wakeve://settings?category=notifications`
- `wakeve://notifications`
- `wakeve://notifications?filter=unread`

Universal-link equivalents under `https://wakeve.app/` must be tested for every public or shareable route that Android supports. Private event routes must require authentication and event access after parsing.

Unsafe URL tests must reject encoded traversal, encoded separators that change route meaning, fragments for private destinations, user-info, unsupported ports, unknown schemes, unknown hosts, and overlong path components.

## Platform Helper Evidence

| iOS helper | Required proof |
| --- | --- |
| `IosMeetingProvider` | Stored safe meeting links launch through native iOS URL handling without provider creation credentials; unsafe/provider-mismatched links warn or block. |
| `IosTextToSpeechService` | If any current iOS user-facing surface calls the shared TTS service, AVFoundation-backed availability/speak/stop behavior is implemented and tested; otherwise the service is classified as non-user-visible for this change. |
| `IosBadgeNotificationService` | Visible badge/notification permission state uses `UNUserNotificationCenter` where surfaced; if no visible iOS surface depends on it, the exception is documented. |
| `IosDocumentPickerService`, `IosImagePickerService`, `IosMemoryOptimizedImageProcessor` | Classified in drift detection and implemented only if tied to an Android-equivalent user-visible route in this change. |

## Release Gate

This change is not complete until:

- The parity inventory test fails for an unclassified new Android user-visible route.
- iOS no longer renders generic placeholder copy for Android-equivalent event organization routes.
- Deep-link tests cover supported Android-equivalent destinations and unsafe rejection.
- iOS UI/contract tests cover empty, editable, pending-sync, access-denied, unavailable, and finalized read-only states for new parity surfaces.
- Any intentionally non-equivalent behavior is documented with a platform/provider/product reason and a non-placeholder user-facing state.
- `openspec validate align-ios-android-feature-parity --strict` passes after implementation tasks are complete.
