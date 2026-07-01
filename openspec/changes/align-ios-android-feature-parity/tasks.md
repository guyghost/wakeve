## 1. Inventory and Test Baseline
- [ ] 1.1 @tests Convert `parity-audit.md` and `verification-matrix.md` into a parity inventory test that lists Android `Screen` routes and iOS route/deep-link destinations, then fails for unclassified Android-only user-visible routes.
- [ ] 1.2 @tests Add iOS contract tests proving `ContentView` no longer renders placeholder text for Android-equivalent event organization states.
- [ ] 1.3 @tests Add localization parity checks for primary route labels, access-denied messages, pending-sync messages, and finalized/read-only labels.
- [ ] 1.4 @tests Add iOS deep-link parser tests for event create, event detail tabs, participants, poll, scenarios, budget, meetings, comments, invite, home, profile, settings, notification preferences, and notifications filters.
- [ ] 1.5 @tests Add fixture coverage proving every `verification-matrix.md` route row is classified as native, deferred, platform/provider exception, Android-only alias, or covered by another active OpenSpec change.

## 2. iOS Navigation and Route Model
- [ ] 2.1 @codegen Extract an iOS route model for top-level destinations and event-scoped secondary destinations.
- [ ] 2.2 @codegen Refactor `AuthenticatedView` route handling so event-scoped routes are reachable from buttons, sheets, and deep links without placeholder cases.
- [ ] 2.3 @codegen Add access-control wrappers for organization routes using existing organizer/participant confirmation/finalized rules.
- [ ] 2.4 @codegen Preserve native iOS tab behavior: Home, Explore, Messages, Profile remain destinations and one-off actions remain contextual.

## 3. Event Planning and Scenario Parity
- [ ] 3.1 @codegen Route the iOS event creation fallback state to the real `CreateEventSheet` or remove the unreachable placeholder state after tests prove no caller uses it.
- [ ] 3.2 @codegen Add a real iOS scenario detail route that mirrors Android capabilities: status, votes, organizer final selection, participant confirmation state, and navigation to meetings/transport.
- [ ] 3.3 @codegen Ensure scenario list, comparison, and final selection update the selected event and route state from the shared repository after mutations.

## 4. Logistics Parity
- [ ] 4.1 @codegen Add an iOS accommodation flow or wrapper using shared accommodation repository data instead of routing accommodation to generic scenario organization.
- [ ] 4.2 @codegen Replace the iOS meal planning placeholder with a full event-scoped meal planning screen using existing `MealPlanningSheets` and shared meal data.
- [ ] 4.3 @codegen Replace the iOS equipment checklist placeholder with a full event-scoped equipment checklist screen using shared equipment data and participant assignment state.
- [ ] 4.4 @codegen Replace the iOS activity planning placeholder with a full event-scoped activity planning screen using shared activity data and participant/organizer state.
- [ ] 4.5 @tests Add iOS contract/UI tests for accommodation, meals, equipment, and activities covering empty, editable, pending-sync, access-denied, and finalized read-only states.

## 5. Collaboration, Photos, and Utility Routes
- [ ] 5.1 @codegen Add a route-connected iOS comments screen using `CommentListView`, moderation actions, section/item context, and existing access rules.
- [ ] 5.2 @codegen Add an iOS event photos/follow-up route equivalent to Android's event photos follow-up surface or document it as deferred with a non-placeholder native unavailable state.
- [ ] 5.3 @codegen Connect iOS organizer dashboard access from Profile or Home with the same organizer event states Android exposes.
- [ ] 5.4 @codegen Connect iOS leaderboard access through an explicit route using existing leaderboard UI and shared gamification data.
- [ ] 5.5 @codegen Verify invitation share, notification preferences, settings/account, budget, payment, Tricount, meetings, and inbox routes are reachable through route model and not only incidental nested views.
- [ ] 5.6 @tests Add iOS comments route tests for event-level comments, section/item comments, access-denied state, moderation actions, and offline pending comments.

## 6. iOS Deep Links and Platform Helpers
- [ ] 6.1 @codegen Expand `DeepLinkService` to parse Android-equivalent supported routes and normalize unsafe path segments consistently.
- [ ] 6.2 @codegen Route iOS notification taps and universal links through the same route model and access gates.
- [ ] 6.3 @codegen Implement iOS meeting URL launch behavior in `IosMeetingProvider` while preserving provider-unavailable creation states.
- [ ] 6.4 @codegen Implement or honestly scope iOS text-to-speech availability using native AVFoundation when the shared service is user-visible.
- [ ] 6.5 @codegen Implement iOS badge notification permission/status behavior through `UNUserNotificationCenter` where it affects visible gamification or notification settings.
- [ ] 6.6 @tests Add iOS tests for safe meeting URL launch, provider mismatch warnings, notification tap routing, and notification preference route handling.

## 7. Review and Verification
- [ ] 7.1 @review Review iOS parity against product-excellence, access-control, offline transparency, and Liquid Glass/native iOS design constraints.
- [ ] 7.2 @tests Run `openspec validate align-ios-android-feature-parity --strict`.
- [ ] 7.3 @tests Run shared KMP tests affected by route/platform helper changes.
- [ ] 7.4 @tests Run Android route/deep-link tests to ensure the parity baseline remains stable.
- [ ] 7.5 @tests Run iOS unit/UI/contract tests for the new route, deep-link, logistics, and access-state coverage.
- [ ] 7.6 @docs Document any intentionally non-equivalent iOS behavior and the platform/provider reason.
- [ ] 7.7 @docs Update or remove `parity-audit.md` after automated drift tests become the authoritative parity evidence.
- [ ] 7.8 @review Verify the implemented evidence against every row in `verification-matrix.md` before the OpenSpec change is archived.
