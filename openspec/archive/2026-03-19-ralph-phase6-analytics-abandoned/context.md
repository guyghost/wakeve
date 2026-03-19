# Context: Phase 6 Analytics (Ralph Mode)

## Ralph Mode Configuration
- **Enabled**: true
- **Max Iterations**: 10
- **Current Iteration**: 1
- **Mode**: Standard Implementation

## Objective
Implémenter le système d'analytics complet pour Wakeve:
1. Interface AnalyticsProvider
2. Firebase Analytics Provider (Android/iOS)
3. Events tracking dans ViewModels
4. Dashboard backend
5. RGPD Consent Management

## Current State
- ✅ Phase 6 Performance: Indexes, Pagination, Image Optimization
- ✅ Test Coverage: Améliorée (+33 tests)
- ✅ Security Fixes: Complétés
- ✅ Phase 6 Analytics P1.1: AnalyticsProvider Interface Created
- 🔄 P1.2: Firebase Provider (next iteration)

## Technical Stack
- **Analytics**: Firebase Analytics (primary)
- **Backend**: Ktor with analytics endpoints
- **Mobile**: Kotlin Multiplatform with platform-specific providers
- **Storage**: Local queue for offline events

## Implementation Plan (Ralph Iterations)

### Iteration 1: Analytics Interface (P1.1) ✅ Complete
- Create AnalyticsProvider interface
- Create AnalyticsEvent sealed class
- Mock implementation for tests

**Artifacts Produced:**
- `shared/src/commonMain/kotlin/com/guyghost/wakeve/analytics/AnalyticsProvider.kt`
- `shared/src/commonMain/kotlin/com/guyghost/wakeve/analytics/AnalyticsEvent.kt`
- `shared/src/commonTest/kotlin/com/guyghost/wakeve/analytics/MockAnalyticsProvider.kt`
- `shared/src/commonTest/kotlin/com/guyghost/wakeve/analytics/AnalyticsProviderTest.kt`

**Tests:** 12/12 passing

### Iteration 2: Firebase Provider (P1.2)
- Android: Firebase Analytics integration
- iOS: Firebase Analytics integration
- Offline queue with batch sync

### Iteration 3: ViewModel Integration (P1.3)
- Add analytics to EventViewModel
- Add analytics to PollViewModel
- Add analytics to ScenarioViewModel
- Add screen view tracking

### Iteration 4: Dashboard Backend (P1.4)
- Create AnalyticsDashboard service
- Add endpoints: /mau, /dau, /retention, /funnel
- Funnel conversion tracking
- Export functionality

### Iteration 5: RGPD Consent (P1.5)
- Consent dialog on first launch
- Settings to revoke consent
- Data deletion on revoke
- Secure storage of consent

## Files to Create

### Shared (Common)
1. `shared/src/commonMain/kotlin/com/guyghost/wakeve/analytics/AnalyticsProvider.kt`
2. `shared/src/commonMain/kotlin/com/guyghost/wakeve/analytics/AnalyticsEvent.kt`
3. `shared/src/commonMain/kotlin/com/guyghost/wakeve/analytics/AnalyticsQueue.kt`

### Android
4. `shared/src/androidMain/kotlin/com/guyghost/wakeve/analytics/FirebaseAnalyticsProvider.kt`

### iOS
5. `shared/src/iosMain/kotlin/com/guyghost/wakeve/analytics/FirebaseAnalyticsProvider.kt`

### Backend
6. `server/src/main/kotlin/com/guyghost/wakeve/analytics/AnalyticsDashboard.kt`
7. `server/src/main/kotlin/com/guyghost/wakeve/routes/AnalyticsRoutes.kt`

### UI
8. `wakeveApp/src/commonMain/kotlin/com/guyghost/wakeve/ui/settings/AnalyticsConsentDialog.kt`

## Acceptance Criteria
- [x] AnalyticsProvider interface with trackEvent, setUserProperty
- [x] 20+ events tracked (app lifecycle, user actions, errors)
- [ ] Firebase Analytics working on Android and iOS
- [ ] Offline queue with automatic sync
- [ ] Dashboard with MAU, DAU, retention, funnel
- [ ] RGPD compliant with explicit consent
- [x] Tests for analytics provider (12/12 passing)

## Events to Track

### App Lifecycle
- app_start
- app_foreground
- app_background
- screen_view

### Event Actions
- event_created (eventType)
- event_joined
- event_viewed
- event_shared

### Poll Actions
- poll_voted (response: yes/no/maybe)
- poll_viewed
- poll_closed

### Scenario Actions
- scenario_created
- scenario_viewed
- scenario_selected

### Meeting Actions
- meeting_created (platform)
- meeting_joined

### Errors
- error_occurred (type, context)

## Next Tasks
1. ✅ P1.1: Analytics Interface (1 day) - Complete
2. P1.2: Firebase Provider (2-3 days) - Next iteration
3. P1.3: ViewModel Integration (3-4 days)
4. P1.4: Dashboard (4-5 days)
5. P1.5: RGPD Consent (2-3 days)
