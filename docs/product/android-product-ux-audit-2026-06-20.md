# Android Product and UX Audit - 2026-06-20

## Method

This audit answers the Android product brief from `/Users/guy/.codex/attachments/20819217-2b57-4eff-b84f-80227ade650a/pasted-text-1.txt`.

Evidence used:

- `ROADMAP.md`
- `openspec/specs/android-ui-system/spec.md`
- `openspec/specs/android-ai-workflows/spec.md`
- `composeApp/src/androidMain/kotlin/com/guyghost/wakeve/App.kt`
- `composeApp/src/androidMain/kotlin/com/guyghost/wakeve/navigation/WakeveNavHost.kt`
- `composeApp/src/androidMain/kotlin/com/guyghost/wakeve/navigation/WakeveAdaptiveNavigationScaffold.kt`
- `composeApp/src/androidMain/kotlin/com/guyghost/wakeve/navigation/Screen.kt`
- `composeApp/src/androidMain/AndroidManifest.xml`
- `composeApp/src/commonMain/kotlin/com/guyghost/wakeve/ui/event/EventWorkspaceScreen.kt`
- `composeApp/src/commonMain/kotlin/com/guyghost/wakeve/ui/event/EventWorkspaceModels.kt`
- `composeApp/src/commonMain/kotlin/com/guyghost/wakeve/ui/ai/EventPlanningAssistantScreen.kt`
- `composeApp/src/commonMain/kotlin/com/guyghost/wakeve/ui/transport/TransportPlanningScreen.kt`

Limits:

- This is a source and roadmap audit, not a hands-on Android device session.
- Cold start, scroll performance, memory, battery, TalkBack, font scaling, Play Store release signing, and app-link verification remain unproven until an Android device or emulator run is captured.
- The iOS App Store release remains the current P0 in `ROADMAP.md`; this Android audit should not be used to close iOS release blockers.

## Executive Verdict

Wakeve has more event-planning surface than a typical early consumer app, but the Android product still feels internally led instead of user-led. It has routes for events, inbox, explore, scenarios, budget, transport, activities, meals, equipment, comments, photos, meetings, notifications, AI drafting, and organizer analytics. The problem is not breadth. The problem is that the first 30 seconds still do not prove "this will save my group from chaos" with enough force.

The sharpest product issue in the inspected Android source is that the Home event workspace renders cards named around internal audit concepts: "Boucle de croissance", "Signal emotionnel", "Position strategique", and "Roadmap 6 mois". Those labels are useful to the product team, not to a normal organizer. In a consumer app, this risks making Wakeve look like a dashboard about Wakeve rather than a control center for my event.

Local follow-up on 2026-06-20: the Android Home workspace copy was updated in `EventWorkspaceModels.kt`, previews, and tests so those visible cards now use organizer-facing labels such as "Invitations et retours", "Ambiance du groupe", "Prochaine decision", and "Plan d'action". This closes the source-level copy issue, but it does not replace a device/emulator UX pass.

Launch verdict today: do not scale Android publicly yet. Use Android for internal testing and cohort feedback, but do not make it the main acquisition platform until the first event creation, invite, vote, and day-of coordination loop is measured on device.

## Scorecard

| Dimension | Score | Why |
|---|---:|---|
| Product | 58/100 | Strong ambition and broad modules, weak first-run proof and too much internal framing. |
| Android UX | 61/100 | Material 3, adaptive navigation, deep links, and Compose structure exist; actual flow clarity and device validation remain unproven. |
| Performance | 45/100 | Release build gates exist, but Android runtime profiling is still pending device traces. |
| Organizer Value | 64/100 | Polling, scenarios, budget, transport, activities, and comments point toward real coordination. The organizer cockpit needs tighter prioritization. |
| Virality | 50/100 | Invite and deep-link infrastructure exists, but the invited user install reason is not yet proven in the first screen. |
| Retention | 48/100 | Post-event recap, photos, reimbursements, and recreate loops are conceptually present but not dominant enough. |
| Wow Effect | 42/100 | The AI event draft can be a wow moment; the current Home strategy cards dilute it. |
| Material 3 Fit | 72/100 | Specs require Material 3, dynamic color fallback, adaptive rail/bottom bar, and reusable components; source supports that direction. |

## User Journey Audit

| Scenario | Current fit | Creation friction | Abandon risk | User score |
|---|---|---:|---:|---:|
| Watch party | Overbuilt; should be one screen, one invite, one vote. | Medium | Medium | 6/10 |
| Restaurant | Good fit if date, headcount, location, and RSVP stay simple. | Medium | Medium | 6/10 |
| Cinema | Needs showtime-like precision; likely too generic today. | Medium | High | 5/10 |
| Soiree | Good fit for invite/vote, weaker for vibe and attendance. | Medium | Medium | 6/10 |
| Birthday | Good fit; needs gifts, tasks, budget, photos, RSVP clarity. | Medium | Medium | 7/10 |
| Barbecue | Good fit if equipment/meal tasks are visible early. | Medium | Medium | 7/10 |
| EVG/EVJF | Strong conceptual fit; needs privacy, budget, transport, tasks. | High | Medium | 7/10 |
| Weekend | Strong fit; scenarios, budget, transport, activities matter. | High | Medium | 7/10 |
| Road trip | Potentially strong; needs route, cars, luggage, day-by-day plan. | High | High | 6/10 |
| Group stay | Strong if accommodation, rooming, budget, and tasks connect. | High | Medium | 7/10 |
| International trip | Not ready as primary promise; needs passports, currencies, flights, time zones, visas, insurance. | Very high | Very high | 4/10 |
| Multi-destination trip | Not ready; transport and itinerary model must become first-class. | Very high | Very high | 3/10 |
| Shared-budget trip | Partially covered by budget and Tricount handoff, not yet a complete settlement OS. | High | High | 6/10 |
| Multi-country participants | Not ready; requires time zones, currency, identity, language, and transport origin handling. | Very high | Very high | 3/10 |

## Need Coverage

### Before the Event

| User question | Current answer | Gap |
|---|---|---|
| Where are we going? | Scenarios, locations, accommodation, transport models exist. | The primary UI must make destination decision status obvious. |
| When are we going? | Polling and event status workflow exist. | The date decision must be the first visible group milestone. |
| Who is coming? | Participants, contacts, RSVP, invitation share exist. | Attendance confidence and missing invitees need stronger treatment. |
| What budget should I expect? | Budget and payment routes exist. | Per-person estimate and confidence range need to be shown before commitment. |
| What program should we follow? | Activities, meals, equipment routes exist. | Program should be a day timeline, not separated modules. |

### During the Event

| User question | Current answer | Gap |
|---|---|---|
| Who arrived? | Not visible as a first-class flow in inspected Android sources. | Add day-of presence/check-in. |
| Who is missing? | Not visible as a first-class flow. | Add missing participant status and safe reminders. |
| Where do we meet? | Transport meeting point summary exists. | Elevate meeting point to an event-day widget/surface. |
| What is next? | Activities exist. | Add "next step now" timeline and notification. |

### After the Event

| User question | Current answer | Gap |
|---|---|---|
| Who owes whom? | Budget, settlement, Tricount routes exist. | Settlement must become an explicit post-event checklist item. |
| Which photos should we share? | Event photos route exists. | Need shared album, recap, and invite-to-upload loop. |
| How do we reorganize quickly? | Recreate-from-template summaries exist. | Make "recreate this group/event" a clear post-event CTA. |

## Android Audit

### Navigation

Strengths:

- `WakeveAdaptiveNavigationScaffold` switches between bottom navigation and navigation rail from capability data.
- Top-level Android destinations are focused on Home, Notifications/Inbox, and Explore.
- `Screen.kt` exposes concrete routes for event creation, poll voting/results, scenarios, budget, transport, activities, meetings, photos, comments, notifications, and organizer dashboard.
- `AndroidManifest.xml` includes custom deep links and `https://wakeve.app/invite`.

Risks:

- The Home route carries too many roles: event list, event cockpit, internal strategy summaries, widget summary, viral summary, emotional summary, strategic summary, roadmap summary, and action summary.
- Bottom navigation labels do not yet communicate the end-to-end control center promise. "A venir", "Notifications", and "Explorer" are functional, but not enough to explain why Wakeve beats WhatsApp.
- App links depend on `wakeve.app`, while the roadmap still lists public DNS/AASA/live URL blockers. Android app-link verification should remain unclaimed until the domain is live and verified.
- Back navigation should be device-tested across deep links, auth redirects, event detail, poll, and nested logistics routes.

### Performance

Known source-level strengths:

- Release build gates exist in `ROADMAP.md`.
- The performance harness can build Android release and explicitly reports `PENDING_DEVICE_TRACE` when no device is connected.

Unproven:

- Cold start on a signed Android build.
- Scroll smoothness in event list, inbox, explore, budget, and transport.
- Memory during AI generation, image-heavy event photos, and long event lists.
- Battery impact of notifications, sync, and WorkManager token refresh.

Do not claim Android performance readiness until a physical-device or emulator run captures these traces.

### Notifications

| Notification | Useful? | Priority | Spam risk | Product decision |
|---|---|---:|---:|---|
| Invitation received | Yes | High | Low | Keep. This is the viral entry point. |
| Vote deadline approaching | Yes | High | Medium | Keep, but batch and suppress if already voted. |
| Vote completed/date validated | Yes | High | Low | Keep. This is the core moment. |
| Program changed | Yes | Medium | High | Keep only for material changes. |
| Departure reminder | Yes | High | Medium | Keep for confirmed attendees only. |
| Budget exceeded | Yes | Medium | High | Keep with thresholds and organizer controls. |
| Generic engagement reminder | No | Low | Very high | Avoid until retention loops are proven. |

### Widgets

| Widget | Interest | Why |
|---|---:|---|
| Event today | Very high | Turns Wakeve into the event-day control surface. |
| Countdown | Medium | Creates anticipation, but can become decorative. |
| Next tasks | High | Strong organizer utility before complex events. |
| Travel | High | Strong for weekends, road trips, and international trips. |

Android widgets should be driven by event status and next critical action, not marketing copy.

## Material Design 3 Audit

Score: 72/100.

What works:

- The Android UI system spec requires centralized Material 3 design tokens, dynamic color fallback, reusable components, adaptive navigation, previews, and UI tests.
- `WakeveAdaptiveNavigationScaffold` uses Material 3 `NavigationBar`, `NavigationRail`, `Scaffold`, badges, and color schemes.
- Event workspace uses Wakeve design-system wrappers such as `WakeveScaffold`, `WakeveCard`, `WakeveSearchBar`, `WakeveSegmentedOptions`, and `WakeveStatusChip`.

What still needs proof:

- TalkBack order on the real event workspace, especially with many summary cards before the event list.
- 200% font scaling for dense cards and grids.
- Touch target consistency across nested routes.
- Contrast under dynamic color.
- Landscape and large-screen behavior on real hardware or emulator screenshots.

## Emotional Audit

| Surface | Current emotional score | Problem | Opportunity |
|---|---:|---|---|
| Get started/auth | 45/100 | Value must be proven before account friction. | Show "create an event in 30 seconds" before heavy auth. |
| Home empty state | 35/100 | An empty dashboard does not beat WhatsApp. | Offer one-tap templates for watch party, restaurant, birthday, weekend. |
| Event workspace with events | 58/100 | Internal audit cards dilute user intent. | Replace with "Your next group decision", "People to nudge", "Today/next step". |
| Polling | 70/100 | Strong core use case. | Make missing votes and deadline the central tension. |
| Confirmed event | 76/100 | Good moment, but preparation can fragment. | Turn budget, transport, program, and tasks into one cockpit. |
| Organizing | 82/100 | Closest to the social OS vision. | Add day-of presence, next step, meeting point, and offline reliability. |
| Finalized | 55/100 | Retention loop is underdeveloped. | Recap, photos, settlements, and "do it again" need a strong post-event flow. |

## Wow Effect

Existing wow candidates:

- Natural-language AI event draft.
- Adaptive event workspace for larger screens.
- Scenario comparison and planning modules.
- Transport meeting point/readiness.
- Recreate-from-template post-event loop.

Missing wow moments:

- First event created and shareable in under 60 seconds.
- Invited participant can vote without confusion or account panic.
- The app says "3 people still need to answer; send one clean reminder".
- Confirmed date instantly becomes a preparation cockpit.
- Event-day surface shows "where, when, who is missing, what next".
- Post-event recap closes money, photos, and next edition.

## Virality

Why users would invite friends:

- To stop repeating date, place, and budget decisions across chat.
- To get votes and confirmations in one place.
- To make the final plan visible to everyone.

Why guests would install:

- Only if the invite link immediately answers what the event is, what action is needed, and what value remains after voting.
- If install is required too early, many guests will stay in WhatsApp.

Why users would return:

- Before: reminders, votes, budget, transport, next task.
- During: meeting point, who is missing, schedule changes.
- After: photos, settlements, recap, recreate.

The current Android surface has pieces of this, but the first-run and invited-user proof must be measured.

## Competitive Audit

| Competitor | Wakeve strength | Wakeve weakness | Feature to steal | Differentiation path |
|---|---|---|---|---|
| Partiful | Broader planning beyond invite. | Lower social polish and less immediate wow. | Fast invite creation and delightful RSVP. | Coordination after RSVP. |
| Apple Invites | Cross-platform ambition and deeper logistics. | Weaker native trust and polish. | Clean invitation artifact. | Works for Android groups and complex logistics. |
| Splitwise | Event context around money. | Settlement is not yet the dominant post-event loop. | Simple balance clarity. | Money plus plan plus group memory. |
| TripIt | Group planning context. | Travel itinerary depth is not ready for international trips. | Timeline and travel document clarity. | Social travel coordination, not solo itinerary. |
| Eventbrite | Private group use case. | Discovery/ticketing network effect absent. | Clear event page and attendee status. | Personal group events, not public events. |

## Social OS Verdict

If WhatsApp, Splitwise, TripIt, and Apple Invites merged tomorrow, Wakeve would only survive if it owns the shared state of a group event better than any chat can.

That means Wakeve's defensible product is not "event planning". It is a group coordination operating system:

- shared decisions;
- participant commitments;
- roles and tasks;
- money;
- movement;
- itinerary;
- reminders;
- day-of presence;
- memories;
- reusable group context.

Missing elements for that vision:

- A single event cockpit that shows the next critical group decision.
- Day-of mode with arrivals, missing people, meeting point, next step, and urgent changes.
- Post-event mode with photos, settlements, recap, and recreate.
- Trust mechanics for permissions, privacy, moderation, and guest access.
- Group memory across events: recurring people, preferences, constraints, budgets, and successful templates.
- Android widgets and notifications that expose the current group state without opening the app.

## Ten Critical Problems

1. Home still needs device validation after the source-level replacement of internal product strategy copy.
2. First 30 seconds do not yet prove enough value for a cold user.
3. Complex event breadth risks overwhelming simple events.
4. Android performance and TalkBack readiness are not device-proven.
5. App-link trust depends on live `wakeve.app` readiness, still blocked in the roadmap.
6. Invited-user install motivation is not yet validated.
7. Day-of coordination is not a first-class mode.
8. Post-event retention is underpowered.
9. Budget, transport, activity, meal, equipment, and comments can feel like separate modules instead of one plan.
10. AI draft is promising but not positioned as the shortest path to a shareable event.

## Ten Highest-Value Features

1. One-minute Android event creation with templates for watch party, restaurant, birthday, weekend, and trip.
2. Invite landing flow that lets guests understand and vote before heavy account friction.
3. Event cockpit with next decision, missing participants, date confidence, budget estimate, and next task.
4. Day-of mode: who arrived, who is missing, meeting point, next step, urgent update.
5. Post-event recap: photos, settlements, memories, and recreate.
6. Android event-day widget and next-task widget.
7. Smart reminders that suppress spam and target only people who need action.
8. Travel readiness checklist for weekends, road trips, and international trips.
9. Group memory: recurring participants, preferences, constraints, and favorite templates.
10. AI event draft as the default fast path, with explicit privacy and local/cloud routing.

## Six-Month Android Roadmap

### Month 1 - Prove Activation

- Replace internal strategy cards on Home with user-facing coordination cards.
- Make "create shareable event" the primary Android success metric.
- Add analytics for creation started, published, invite shared, invite opened, vote submitted, date confirmed.
- Run Android device smoke tests for create, invite, vote, notification click, and deep link.
- Capture baseline performance: cold start, event list scroll, event creation, poll, event detail.

### Month 2 - Fix the Guest Loop

- Optimize invite landing and guest voting.
- Delay account friction until the user understands the event.
- Add targeted reminder states for missing votes.
- Verify Android app links against live `wakeve.app`.
- Add TalkBack and 200% font-scale validation for the invite and poll flows.

### Month 3 - Build the Event Cockpit

- Consolidate budget, transport, program, comments, and tasks into one event cockpit.
- Show one "next critical action" at the top of every active event.
- Add organizer controls for useful reminders.
- Make confirmed event preparation measurable.

### Month 4 - Day-Of Mode

- Add event-day surface with meeting point, current time block, arrivals, missing people, and urgent updates.
- Add event-day Android widget.
- Add high-priority notification policy for departure and program changes.
- Test offline read behavior for day-of essentials.

### Month 5 - Post-Event Retention

- Add recap, photos, settlement checklist, and recreate CTA.
- Measure first-event-to-second-event conversion.
- Add group memory primitives for recurring groups.

### Month 6 - Complex Event Differentiation

- Harden travel readiness, multi-origin transport, budget ranges, rooming/accommodation, and multi-day itinerary.
- Add product gates for international travel claims: currencies, time zones, documents, insurance, and multi-destination support.
- Decide whether Android should lead growth or follow iOS after App Store P0 closure.

## Immediate Roadmap Changes

- Treat Android public growth as P2 until device evidence exists.
- Add a P2 Android product activation stream focused on first event, invite, vote, and day-of cockpit.
- Do not start broad new Android features before replacing internal strategy cards with user-facing coordination cards.
- Keep `migrate-android-google-auth-credential-manager` pending until approved; it is important platform hygiene, but it does not solve activation by itself.
