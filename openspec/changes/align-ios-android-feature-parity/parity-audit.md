# Android/iOS Feature Parity Audit

## Audit Basis
- Android route baseline: `composeApp/src/androidMain/kotlin/com/guyghost/wakeve/navigation/Screen.kt`
- Android route wiring: `composeApp/src/androidMain/kotlin/com/guyghost/wakeve/navigation/WakeveNavHost.kt`
- iOS top-level route state: `iosApp/src/Views/App/ContentView.swift`
- iOS deep links: `iosApp/src/Services/DeepLinkService.swift`
- iOS platform placeholders: `shared/src/iosMain/kotlin/com/guyghost/wakeve/**`

This audit records current-state gaps that must be closed, explicitly classified, or documented before the goal "iOS feature parity with Android" can be considered complete.

`verification-matrix.md` defines the post-implementation evidence required for each Android route and platform helper. During implementation, automated tests should replace this audit as the authoritative drift detector.

## Route Parity Snapshot

| Android capability | Android route/source | Current iOS state | Parity status |
| --- | --- | --- | --- |
| Home/upcoming events | `Screen.Home` | `WakeveTab.home`, `AppView.eventList` | Present |
| Explore/templates | `Screen.Explore` | `WakeveTab.groups`, `ExploreTabView` | Present, naming differs by platform |
| Inbox/messages | `Screen.Inbox` | `WakeveTab.messages`, `InboxView` | Present |
| Profile | `Screen.Profile` | `WakeveTab.profile`, `ProfileTabView` | Present |
| Auth/email/onboarding | `Screen.Auth`, `EmailAuth`, `Onboarding` | `LoginView`, `OnboardingView`, auth manager | Present but route model differs |
| Event creation | `Screen.EventCreation`, `DraftEventWizard` | `CreateEventSheet`; `AppView.eventCreation` still placeholder | Partial |
| Event AI assistant | `Screen.EventPlanningAssistant` | WakeveAI in create/detail/transport active change | Covered by active `add-on-device-wakeve-ai`; needs classification in parity tests |
| Event detail | `Screen.EventDetail` | `AppView.eventDetail`, `EventDetailView` | Present |
| Participant management | `Screen.ParticipantManagement` | `AppView.participantManagement`, `ParticipantManagementView` | Present |
| Poll voting/results | `Screen.PollVoting`, `PollResults` | `AppView.pollVoting`, `pollResults` | Present |
| Scenario list/comparison | `ScenarioList`, `ScenarioComparison` | `AppView.scenarioList`, `scenarioComparison` | Present |
| Scenario detail | `Screen.ScenarioDetail` | `AppView.scenarioDetail` placeholder | Missing |
| Scenario management | `Screen.ScenarioManagement` | folded into `ScenarioOrganizationView` | Partial, needs explicit route classification |
| Transport planning | `Screen.TransportPlanning` | `AppView.transportPlanning`, `TransportPlanningView` | Present |
| Accommodation | `Screen.Accommodation` | `AppView.accommodation` routes to `ScenarioOrganizationView` | Partial/wrong target |
| Meal planning | `Screen.MealPlanning` | `AppView.mealPlanning` placeholder; sheets exist | Missing full route |
| Equipment checklist | `Screen.EquipmentChecklist` | `AppView.equipmentChecklist` placeholder | Missing |
| Activity planning | `Screen.ActivityPlanning` | `AppView.activityPlanning` placeholder | Missing |
| Comments | `Screen.Comments` | `CommentListView` exists, no route-connected `AppView` case | Missing route |
| Event photos/follow-up | `Screen.EventPhotos` | no route-connected iOS equivalent found | Missing or needs documented deferral |
| Meeting list/detail | `MeetingList`, `MeetingDetail` | `AppView.meetingList`, `meetingDetail` | Present |
| Budget overview/detail | `BudgetOverview`, `BudgetDetail` | `AppView.budgetOverview`, `budgetDetail` | Present |
| Payment pot | `Screen.PaymentPot` | `AppView.paymentPot`, local `PaymentPotView` | Present |
| Tricount handoff | `Screen.Tricount` | `AppView.tricount`, local `TricountHandoffView` | Present |
| Invitation share | `Screen.InvitationShare` | `InvitationShareSheet` from participant flow; no route case | Partial |
| Notifications list | `Screen.Notifications` | `InboxView`; no equivalent notifications-list route | Partial |
| Notification preferences | `Screen.NotificationPreferences` | `NotificationPreferencesView` nested/sheet | Present but not route-classified |
| Settings/account | `Screen.Settings` | Profile nested settings/data management | Partial route parity |
| Leaderboard | `Screen.Leaderboard` | `LeaderboardView` exists, not route-connected | Missing route |
| Organizer dashboard | `Screen.OrganizerDashboard` | Profile utility copy only, no dashboard route | Missing route |

## Deep-Link Parity Gaps

Android parses and routes:
- `wakeve://event/create`
- `wakeve://event/{id}`
- `wakeve://event/{id}/details?tab=comments|budget|participants`
- `wakeve://event/{id}/poll`
- `wakeve://event/{id}/scenarios`
- `wakeve://event/{id}/meetings`
- `wakeve://poll/{eventId}`
- `wakeve://meeting/{meetingId}`
- `wakeve://invite/{token}`
- `wakeve://home`
- `wakeve://profile`
- `wakeve://settings`
- `wakeve://settings?category=notifications`
- `wakeve://notifications?filter=unread`
- calendar and vote-reminder utility links that resolve to Home

iOS currently parses:
- `wakeve://event/{id}`
- `wakeve://poll/{eventId}`
- `wakeve://meeting/{meetingId}`
- `wakeve://invite/{token}`
- matching universal-link variants for those four resource types

Required parity work:
- Add iOS parser cases for Android-equivalent supported destinations.
- Add unsafe path/fragment/user-info/port rejection equivalent to Android.
- Route private links through auth and event access gates before rendering.
- Preserve unauthenticated invite handling.

## Platform Placeholder Gaps

User-visible or likely parity-relevant placeholders:
- `shared/src/iosMain/kotlin/com/guyghost/wakeve/meeting/IosMeetingProvider.kt`
  - Android can launch existing meeting URLs. iOS currently delegates launch to `NoConfiguredMeetingProvider`.
- `shared/src/iosMain/kotlin/com/guyghost/wakeve/services/IosTextToSpeechService.kt`
  - Android has a real `TextToSpeech` wrapper. iOS currently delegates all behavior to `NoConfiguredTextToSpeechService`.
- `shared/src/iosMain/kotlin/com/guyghost/wakeve/gamification/IosBadgeNotificationService.kt`
  - Android exposes notification channel setup and permission/status checks. iOS currently delegates permission/status to `NoConfiguredBadgeNotificationService`.

Additional iOS placeholders found, requiring classification before implementation scope expands:
- `shared/src/iosMain/kotlin/com/guyghost/wakeve/file/IosDocumentPickerService.kt`
- `shared/src/iosMain/kotlin/com/guyghost/wakeve/image/IosImagePickerService.kt`
- `shared/src/iosMain/kotlin/com/guyghost/wakeve/ml/IosMemoryOptimizedImageProcessor.kt`

These additional placeholders should be included in drift detection, but they should only enter implementation scope when tied to an Android user-visible feature route or an existing iOS UI entry point.

## Highest-Risk Completion Criteria

The goal is not complete until current evidence proves all of the following:
- No Android user-visible event-organization route remains unclassified in the parity inventory.
- No Android-equivalent iOS route renders generic placeholder copy.
- iOS deep-link parsing covers Android-equivalent supported destinations and rejects unsafe links.
- iOS route handling preserves organizer/participant access, offline pending-sync, and finalized read-only semantics.
- iOS comments, notification taps, notification preferences, and meeting-link launch are verified against their domain specs, not only against design-level route presence.
- Platform placeholders are either replaced for user-visible parity or documented with a product-approved exception.
- iOS and Android parity tests fail when a new Android route is added without iOS classification.
