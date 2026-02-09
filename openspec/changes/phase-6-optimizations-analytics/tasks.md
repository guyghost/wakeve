# Tasks: Phase 6 - Optimizations, Analytics & Advanced Push

## Overview

Phase 6 décomposée en 3 sous-changes OpenSpec recommandés pour gérer la complexité:
1. `phase-6-performance` (semaines 1-3)
2. `phase-6-analytics` (semaines 4-5)
3. `phase-6-advanced-push` (semaines 6-8)

**Estimation totale**: 8 semaines
**Dépendances minimales** entre sous-changes
**Approche**: Delivery incrémental avec des livrables mesurables

---

## Sous-Change 1: `phase-6-performance` (Weeks 1-3)

### P0.1 Database Indexes & Query Optimization
**Files**: `shared/src/commonMain/sqldelight/com/guyghost/wakeve/*.sq`
**Complexity**: Medium
**Time**: 2-3 days
**Priority**: P0
**Dependencies**: None
**Description**: Analyser et ajouter les indexes manquants pour toutes les tables SQLDelight

**Acceptance Criteria**:
- [ ] Analyse EXPLAIN QUERY PLAN pour toutes les requêtes fréquentes
- [ ] Indexes ajoutés pour: Event, Participant, Vote, TimeSlot, Scenario, Meeting
- [ ] Indexes composites pour les requêtes jointes (ex: `(eventId, status)`)
- [ ] Tests de performance avant/après avec benchmarks
- [ ] Aucune régression dans les tests existants
- [ ] Documentation des indexes dans chaque fichier `.sq`

**Requêtes à optimiser (basé sur l'analyse actuelle)**:

```sql
-- Event.sq - Indexes manquants
CREATE INDEX IF NOT EXISTS idx_event_organizer ON event(organizerId, createdAt DESC);
CREATE INDEX IF NOT EXISTS idx_event_status ON event(status, createdAt DESC);
CREATE INDEX IF NOT EXISTS idx_event_updated ON event(updatedAt);

-- Participant.sq - Indexes manquants
CREATE INDEX IF NOT EXISTS idx_participant_event ON participant(eventId, joinedAt ASC);
CREATE INDEX IF NOT EXISTS idx_participant_user ON participant(userId);
CREATE INDEX IF NOT EXISTS idx_participant_role ON participant(eventId, role);
CREATE INDEX IF NOT EXISTS idx_participant_validated ON participant(eventId, hasValidatedDate);

-- Vote.sq - Indexes manquants
CREATE INDEX IF NOT EXISTS idx_vote_event ON vote(eventId, createdAt DESC);
CREATE INDEX IF NOT EXISTS idx_vote_timeslot ON vote(timeslotId, vote);
CREATE INDEX IF NOT EXISTS idx_vote_participant ON vote(participantId, createdAt DESC);
CREATE INDEX IF NOT EXISTS idx_vote_timeslot_participant ON vote(timeslotId, participantId);
```

**Risques**:
- Augmentation de la taille de la DB (10-20%)
- Impact sur les INSERT/UPDATE (négligeable si indexes bien choisis)

---

### P0.2 Pagination Implementation for Lists
**Files**:
- `wakeveApp/src/commonMain/kotlin/com/guyghost/wakeve/ui/event/EventListScreen.kt`
- `wakeveApp/src/commonMain/kotlin/com/guyghost/wakeve/ui/scenario/ScenarioListScreen.kt`
- `wakeveApp/wakeveApp/Views/EventListView.swift`

**Complexity**: Medium
**Time**: 3-4 days
**Priority**: P0
**Dependencies**: P0.1 (Database Indexes)
**Description**: Implémenter la pagination pour toutes les listes avec LazyColumn/LazyVStack

**Acceptance Criteria**:
- [ ] Pagination avec load incremental (scroll → load next page)
- [ ] Taille de page configurable (default: 50 items)
- [ ] Keys stables pour LazyColumn/LazyVStack (item.id)
- [ ] État de chargement affiché (Loading skeletons)
- [ ] Gestion des erreurs de chargement
- [ ] Tests UI pour le scroll infini
- [ ] Performance: <100ms pour charger 50 items

**Implémentation Android (Jetpack Compose)**:
```kotlin
@Composable
fun PaginatedEventList(
    events: LazyPagingItems<Event>,
    onItemClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        items(
            count = events.itemCount,
            key = { index -> events[index]?.id ?: index }
        ) { index ->
            events[index]?.let { event ->
                EventCard(
                    event = event,
                    onClick = { onItemClick(event.id) }
                )
            }
        }

        if (events.loadState.append == LoadState.Loading) {
            item { LoadingIndicator() }
        }
    }
}
```

**Implémentation iOS (SwiftUI)**:
```swift
struct PaginatedEventList: View {
    @State private var events: [Event] = []
    @State private var isLoading = false

    var body: some View {
        ScrollView {
            LazyVStack(spacing: 12) {
                ForEach(events, id: \.id) { event in
                    EventCard(event: event)
                        .onAppear {
                            if event.id == events.last?.id {
                                loadNextPage()
                            }
                        }
                }

                if isLoading {
                    ProgressView()
                        .padding()
                }
            }
        }
    }
}
```

---

### P0.3 Cache LRU Implementation
**Files**:
- `shared/src/commonMain/kotlin/com/guyghost/wakeve/cache/LRUCache.kt` (nouveau)
- `shared/src/commonMain/kotlin/com/guyghost/wakeve/repository/EventRepository.kt` (modification)

**Complexity**: Low
**Time**: 1-2 days
**Priority**: P0
**Dependencies**: None
**Description**: Implémenter un cache LRU pour les données fréquemment accédées

**Acceptance Criteria**:
- [ ] Implémentation LRU cache thread-safe
- [ ] Capacité max configurable (default: 100 items)
- [ ] TTL par défaut (5 minutes)
- [ ] Intégration dans EventRepository, ParticipantRepository
- [ ] Cache invalidation sur mutations
- [ ] Tests unitaires (99% coverage)

**Implémentation**:
```kotlin
// shared/src/commonMain/kotlin/com/guyghost/wakeve/cache/LRUCache.kt
class LRUCache<K, V>(
    private val maxSize: Int = 100,
    private val ttlMillis: Long = 5 * 60 * 1000 // 5 minutes
) {
    private val cache = LinkedHashMap<K, CacheEntry<V>>(maxSize, 0.75f, true)
    private val lock = Mutex()

    data class CacheEntry<V>(
        val value: V,
        val timestamp: Long = System.currentTimeMillis()
    )

    suspend fun get(key: K): V? = lock.withLock {
        val entry = cache[key]
        if (entry == null) return null
        if (isExpired(entry)) {
            cache.remove(key)
            return null
        }
        return entry.value
    }

    suspend fun put(key: K, value: V) = lock.withLock {
        if (cache.size >= maxSize) {
            cache.remove(cache.keys.first())
        }
        cache[key] = CacheEntry(value)
    }

    suspend fun remove(key: K) = lock.withLock {
        cache.remove(key)
    }

    suspend fun clear() = lock.withLock {
        cache.clear()
    }

    private fun isExpired(entry: CacheEntry<V>): Boolean =
        System.currentTimeMillis() - entry.timestamp > ttlMillis
}
```

---

### P0.4 Image Optimization with Coil
**Files**:
- `wakeveApp/build.gradle.kts` (ajouter Coil)
- `wakeveApp/src/commonMain/kotlin/com/guyghost/wakeve/ui/components/ImageLoader.kt` (nouveau)

**Complexity**: Low
**Time**: 1 day
**Priority**: P0
**Dependencies**: None
**Description**: Configurer Coil pour optimiser le chargement des images

**Acceptance Criteria**:
- [ ] Coil intégré avec Memory cache, Disk cache
- [ ] Placeholders gris/borders pour images en chargement
- [ ] Error placeholders pour images échouées
- [ ] Crossfade animation (200ms)
- [ ] Transformation: Circle, Rounded corners
- [ ] Tests d'intégration

**Configuration Coil**:
```kotlin
// wakeveApp/src/commonMain/kotlin/com/guyghost/wakeve/ui/components/ImageLoader.kt
object ImageLoader {
    val imageLoader = Coil.imageLoader {
        memoryCache {
            MemoryCache.Builder(WakeveApp.instance)
                .maxSizePercent(0.25) // 25% de la mémoire disponible
                .build()
        }
        diskCache {
            DiskCache.Builder()
                .directory(WakeveApp.instance.cacheDir.resolve("image_cache"))
                .maxSizeBytes(50 * 1024 * 1024) // 50MB
                .build()
        }
        crossfade(true)
        placeholder(R.drawable.placeholder)
        error(R.drawable.error_placeholder)
    }
}
```

---

### P0.5 Memory Profiling & Optimization
**Files**: Tous les fichiers Repository, ViewModel, UI
**Complexity**: Medium
**Time**: 2-3 days
**Priority**: P0
**Dependencies**: P0.2, P0.3
**Description**: Profiler et optimiser l'usage mémoire

**Acceptance Criteria**:
- [ ] Profiler avec Android Studio Profiler
- [ ] Identifier memory leaks (ViewModels, coroutines)
- [ ] Optimiser: Lazy loading, coroutines scopes, disposable
- [ ] Tests de charge: 1000+ events sans crash
- [ ] Documentation des optimisations

**Checklist**:
- [ ] Vérifier que les ViewModels sont scoped correctement
- [ ] S'assurer que les coroutines sont annulées (viewModelScope)
- [ ] Éviter les captures d'activité dans les callbacks
- [ ] Utiliser `rememberLazyListState()` au lieu de `rememberScrollState()`
- [ ] Libérer les listeners dans `onDispose()`

---

## Sous-Change 2: `phase-6-analytics` (Weeks 4-5)

### P1.1 Analytics Provider Interface
**Files**:
- `shared/src/commonMain/kotlin/com/guyghost/wakeve/analytics/AnalyticsProvider.kt` (nouveau)
- `shared/src/commonMain/kotlin/com/guyghost/wakeve/analytics/AnalyticsEvent.kt` (nouveau)

**Complexity**: Low
**Time**: 1 day
**Priority**: P1
**Dependencies**: None
**Description**: Créer l'interface abstraite pour analytics

**Acceptance Criteria**:
- [ ] Interface `AnalyticsProvider` avec méthodes: `trackEvent()`, `setUserProperty()`
- [ ] Enum/Sealed class `AnalyticsEvent` pour 20+ events
- [ ] Support pour properties personnalisées
- [ ] Mock implementation pour tests
- [ ] Documentation complète

**Interface**:
```kotlin
// shared/src/commonMain/kotlin/com/guyghost/wakeve/analytics/AnalyticsProvider.kt
interface AnalyticsProvider {
    fun trackEvent(event: AnalyticsEvent, properties: Map<String, Any?> = emptyMap())
    fun setUserProperty(name: String, value: String)
    fun setUserId(userId: String?)
    fun setEnabled(enabled: Boolean) // RGPD consent
    fun clearUserData()
}

// shared/src/commonMain/kotlin/com/guyghost/wakeve/analytics/AnalyticsEvent.kt
sealed class AnalyticsEvent(val eventName: String) {
    // Events Lifecycle
    data object AppStart : AnalyticsEvent("app_start")
    data object AppForeground : AnalyticsEvent("app_foreground")
    data object AppBackground : AnalyticsEvent("app_background")

    // Events Événement
    data class EventCreated(val eventType: String) : AnalyticsEvent("event_created")
    data class EventJoined(val eventId: String) : AnalyticsEvent("event_joined")
    data class EventViewed(val eventId: String) : AnalyticsEvent("event_viewed")

    // Events Poll
    data class PollVoted(val eventId: String, val response: String) : AnalyticsEvent("poll_voted")
    data class PollViewed(val eventId: String) : AnalyticsEvent("poll_viewed")

    // Events Scénario
    data class ScenarioCreated(val eventId: String) : AnalyticsEvent("scenario_created")
    data class ScenarioViewed(val eventId: String) : AnalyticsEvent("scenario_viewed")
    data class ScenarioSelected(val scenarioId: String) : AnalyticsEvent("scenario_selected")

    // Events Réunion
    data class MeetingCreated(val eventId: String) : AnalyticsEvent("meeting_created")
    data class MeetingJoined(val meetingId: String) : AnalyticsEvent("meeting_joined")

    // Events Navigation
    data class ScreenView(val screenName: String) : AnalyticsEvent("screen_view")

    // Events Erreur
    data class ErrorOccurred(val errorType: String, val errorContext: String?) : AnalyticsEvent("error_occurred")
}
```

---

### P1.2 FirebaseAnalyticsProvider Implementation
**Files**:
- `shared/src/androidMain/kotlin/com/guyghost/wakeve/analytics/FirebaseAnalyticsProvider.android.kt` (nouveau)
- `shared/src/iosMain/kotlin/com/guyghost/wakeve/analytics/FirebaseAnalyticsProvider.ios.kt` (nouveau)

**Complexity**: Medium
**Time**: 2-3 days
**Priority**: P1
**Dependencies**: P1.1
**Description**: Implémenter Firebase Analytics pour Android et iOS

**Acceptance Criteria**:
- [ ] Firebase Analytics intégré (Android + iOS)
- [ ] Respect des paramètres RGPD (consentement)
- [ ] Batch events offline (file d'attente locale)
- [ ] Sync automatique quand online
- [ ] Tests unitaires (mock Firebase)

**Dépendances build.gradle.kts**:
```kotlin
// shared/build.gradle.kts
androidMain.dependencies {
    implementation("com.google.firebase:firebase-analytics-ktx:22.0.2")
}

iosMain.dependencies {
    implementation("com.google.firebase:firebase-analytics:11.1.0")
}
```

---

### P1.3 Analytics Integration in ViewModels
**Files**: Tous les ViewModels du projet
**Complexity**: Medium
**Time**: 3-4 days
**Priority**: P1
**Dependencies**: P1.2
**Description**: Intégrer le tracking analytics dans tous les ViewModels

**Acceptance Criteria**:
- [ ] Events tracking dans: EventViewModel, PollViewModel, ScenarioViewModel, MeetingViewModel
- [ ] Screen view events sur chaque screen
- [ ] Error events avec contexte
- [ ] User properties (role, event type count)
- [ ] Tests pour vérifier que tous les events sont émis

**Exemple d'intégration**:
```kotlin
// wakeveApp/src/commonMain/kotlin/com/guyghost/wakeve/ui/event/EventViewModel.kt
@HiltViewModel
class EventViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val analyticsProvider: AnalyticsProvider
) : ViewModel() {

    fun createEvent(title: String, description: String) {
        viewModelScope.launch {
            try {
                val event = eventRepository.createEvent(title, description)
                analyticsProvider.trackEvent(
                    AnalyticsEvent.EventCreated(event.eventType.toString()),
                    mapOf("event_id" to event.id)
                )
                _uiState.value = EventUiState.Success(event)
            } catch (e: Exception) {
                analyticsProvider.trackEvent(
                    AnalyticsEvent.ErrorOccurred("create_event", e.message)
                )
                _uiState.value = EventUiState.Error(e)
            }
        }
    }
}
```

---

### P1.4 Analytics Dashboard (Backend)
**Files**:
- `server/src/main/kotlin/com/guyghost/wakeve/analytics/AnalyticsDashboard.kt` (nouveau)
- `server/src/main/kotlin/com/guyghost/wakeve/routes/AnalyticsRoutes.kt` (nouveau)

**Complexity**: High
**Time**: 4-5 days
**Priority**: P1
**Dependencies**: P1.2
**Description**: Créer un dashboard backend pour visualiser les métriques

**Acceptance Criteria**:
- [ ] Endpoints: `/api/analytics/mau`, `/api/analytics/dau`, `/api/analytics/funnel`
- [ ] Funnel conversion: event_created → poll_voted → event_finalized
- [ ] Rétention: Day 1, Day 7, Day 30
- [ ] Top events par volume
- [ ] Export CSV/JSON
- [ ] Authentification requise (admin only)

**Endpoints**:
```kotlin
// server/src/main/kotlin/com/guyghost/wakeve/routes/AnalyticsRoutes.kt
routing {
    authenticate("admin") {
        route("/api/analytics") {
            get("/mau") { getMonthlyActiveUsers(call) }
            get("/dau") { getDailyActiveUsers(call) }
            get("/retention") { getRetentionMetrics(call) }
            get("/funnel") { getConversionFunnel(call) }
            get("/events") { getTopEvents(call) }
            get("/export") { exportAnalytics(call) }
        }
    }
}
```

---

### P1.5 RGPD Consent Management
**Files**:
- `wakeveApp/src/commonMain/kotlin/com/guyghost/wakeve/ui/settings/AnalyticsConsentDialog.kt` (nouveau)
- `wakeveApp/wakeveApp/Views/AnalyticsConsentView.swift` (nouveau)

**Complexity**: Medium
**Time**: 2-3 days
**Priority**: P1
**Dependencies**: P1.1
**Description**: Implémenter la gestion du consentement RGPD

**Acceptance Criteria**:
- [ ] Dialog de consentement au premier lancement
- [ ] Option dans Settings pour révoquer le consentement
- [ ] Suppression des données analytics sur revoke
- [ ] Storage du consentement (EncryptedSharedPreferences/Keychain)
- [ ] Tests d'intégration

**Dialog Consentement**:
```kotlin
@Composable
fun AnalyticsConsentDialog(
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("Analytics & Privacy") },
        text = {
            Text("""
                Wakeve collecte des données anonymes pour améliorer l'application.

                • Quels types de données: Événements créés, sondages votés, erreurs
                • À quoi ça sert: Optimiser les performances, corriger les bugs
                • RGPD: Vous pouvez révoquer votre consentement à tout moment

                En acceptant, vous nous permettez de collecter ces données.
            """.trimIndent())
        },
        confirmButton = {
            Button(onClick = onAccept) {
                Text("Accepter")
            }
        },
        dismissButton = {
            TextButton(onClick = onDecline) {
                Text("Refuser")
            }
        }
    )
}
```

---

## Sous-Change 3: `phase-6-advanced-push` (Weeks 6-8)

### P0.6 Rich Notifications Implementation
**Files**:
- `wakeveApp/src/commonMain/kotlin/com/guyghost/wakeve/notification/RichNotificationService.kt` (nouveau)
- `wakeveApp/src/androidMain/kotlin/com/guyghost/wakeve/notification/AndroidRichNotificationService.android.kt` (nouveau)
- `wakeveApp/wakeveApp/Services/RichNotificationService.swift` (nouveau)

**Complexity**: High
**Time**: 4-5 days
**Priority**: P0
**Dependencies**: None
**Description**: Implémenter les notifications riches avec images et actions

**Acceptance Criteria**:
- [ ] Notifications avec images (thumbnail)
- [ ] Actions rapides: "Oui/Non/Peut-être" pour sondage
- [ ] Notifications groupées par événement
- [ ] Priorité configurable (high, default, low)
- [ ] Interruption level (critical, active, passive)
- [ ] Tests d'intégration Android/iOS

**Android Implementation**:
```kotlin
// wakeveApp/src/androidMain/kotlin/com/guyghost/wakeve/notification/AndroidRichNotificationService.android.kt
class AndroidRichNotificationService(
    private val context: Context
) : RichNotificationService {

    override fun showRichNotification(notification: RichNotification) {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(notification.title)
            .setContentText(notification.body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)

        // Image
        notification.imageUrl?.let { url ->
            val bitmap = loadBitmap(url)
            builder.setLargeIcon(bitmap)
            builder.setStyle(NotificationCompat.BigPictureStyle().bigPicture(bitmap))
        }

        // Actions rapides
        notification.actions.forEach { action ->
            val pendingIntent = createPendingIntent(action)
            builder.addAction(
                NotificationCompat.Action.Builder(0, action.label, pendingIntent)
                    .build()
            )
        }

        NotificationManagerCompat.from(context).notify(notification.id, builder.build())
    }
}
```

**iOS Implementation**:
```swift
// wakeveApp/wakeveApp/Services/RichNotificationService.swift
class RichNotificationService {
    func showRichNotification(_ notification: RichNotification) {
        let content = UNMutableNotificationContent()
        content.title = notification.title
        content.body = notification.body
        content.sound = .default
        content.categoryIdentifier = notification.category.rawValue

        // Image
        if let imageUrl = notification.imageUrl,
           let attachment = try? UNNotificationAttachment(
               identifier: "image",
               url: imageUrl,
               options: nil
           ) {
            content.attachments = [attachment]
        }

        // Actions
        let actions = notification.actions.map { action in
            UNNotificationAction(
                identifier: action.identifier,
                title: action.label,
                options: []
            )
        }
        let category = UNNotificationCategory(
            identifier: notification.category.rawValue,
            actions: actions,
            intentIdentifiers: []
        )
        UNUserNotificationCenter.current().setNotificationCategories([category])

        let request = UNNotificationRequest(
            identifier: notification.id,
            content: content,
            trigger: nil
        )
        UNUserNotificationCenter.current().add(request)
    }
}
```

---

### P0.7 Notification Categories & Actions
**Files**:
- `shared/src/commonMain/kotlin/com/guyghost/wakeve/notification/NotificationCategory.kt` (nouveau)
- `shared/src/commonMain/kotlin/com/guyghost/wakeve/notification/NotificationAction.kt` (nouveau)

**Complexity**: Low
**Time**: 1 day
**Priority**: P0
**Dependencies**: P0.6
**Description**: Définir les catégories et actions de notifications

**Acceptance Criteria**:
- [ ] 5 catégories: Poll, Scenario, Meeting, Comment, System
- [ ] Actions par catégorie: Vote (Yes/No/Maybe), Join, Reply
- [ ] Enum/Sealed classes typesafe
- [ ] Documentation

**Implémentation**:
```kotlin
// shared/src/commonMain/kotlin/com/guyghost/wakeve/notification/NotificationCategory.kt
enum class NotificationCategory(val identifier: String) {
    POLL("poll"),
    SCENARIO("scenario"),
    MEETING("meeting"),
    COMMENT("comment"),
    SYSTEM("system");

    companion object {
        fun fromString(identifier: String): NotificationCategory? =
            values().find { it.identifier == identifier }
    }
}

data class NotificationAction(
    val identifier: String,
    val label: String,
    val type: ActionType
)

enum class ActionType {
    VOTE_YES,
    VOTE_NO,
    VOTE_MAYBE,
    JOIN_EVENT,
    JOIN_MEETING,
    REPLY_COMMENT
}
```

---

### P0.8 NotificationScheduler (WorkManager)
**Files**:
- `shared/src/commonMain/kotlin/com/guyghost/wakeve/notification/NotificationScheduler.kt` (nouveau)
- `shared/src/androidMain/kotlin/com/guyghost/wakeve/notification/AndroidNotificationScheduler.android.kt` (nouveau)
- `shared/src/iosMain/kotlin/com/guyghost/wakeve/notification/IosNotificationScheduler.ios.kt` (nouveau)

**Complexity**: High
**Time**: 4-5 days
**Priority**: P0
**Dependencies**: P0.6, P0.7
**Description**: Scheduler de notifications programmées avec WorkManager

**Acceptance Criteria**:
- [ ] Scheduler pour rappels: 15min, 1h, 24h avant réunion
- [ ] Batch processing pour réduire les appels FCM
- [ ] Rate limiting: max 5 notifications/heure par utilisateur
- [ ] Cancellation de notifications obsolètes
- [ ] Tests unitaires + instrumented

**Implémentation Android**:
```kotlin
// shared/src/androidMain/kotlin/com/guyghost/wakeve/notification/AndroidNotificationScheduler.android.kt
class AndroidNotificationScheduler(
    private val context: Context
) : NotificationScheduler {

    override fun scheduleNotification(
        notification: RichNotification,
        delayMillis: Long
    ) {
        val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setInputData(
                workDataOf(
                    "notification_id" to notification.id,
                    "title" to notification.title,
                    "body" to notification.body
                )
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "notification-${notification.id}",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    override fun cancelNotification(notificationId: String) {
        WorkManager.getInstance(context).cancelUniqueWork("notification-$notificationId")
    }
}
```

---

### P1.9 Deep Linking from Notifications
**Files**:
- `wakeveApp/src/commonMain/kotlin/com/guyghost/wakeve/navigation/DeepLinkHandler.kt` (nouveau)
- `wakeveApp/wakeveApp/Navigation/DeepLinkHandler.swift` (nouveau)

**Complexity**: Medium
**Time**: 2-3 days
**Priority**: P1
**Dependencies**: P0.6
**Description**: Navigation automatique depuis les notifications

**Acceptance Criteria**:
- [ ] Deep links format: `wakeve://events/{id}`, `wakeve://meetings/{id}`
- [ ] Navigation vers l'écran correct
- [ ] Passage des parameters (scroll, highlighted item)
- [ ] Gestion quand app est fermée/ouverte
- [ ] Tracking conversion (notification → action)

**Implémentation Android**:
```kotlin
// wakeveApp/src/commonMain/kotlin/com/guyghost/wakeve/navigation/DeepLinkHandler.kt
class DeepLinkHandler(
    private val navController: NavController
) {

    fun handleDeepLink(uri: Uri): Boolean {
        return when {
            uri.path?.startsWith("/events/") == true -> {
                val eventId = uri.lastPathSegment
                navController.navigate("event_detail/$eventId")
                true
            }
            uri.path?.startsWith("/meetings/") == true -> {
                val meetingId = uri.lastPathSegment
                navController.navigate("meeting_detail/$meetingId")
                true
            }
            else -> false
        }
    }
}
```

---

## Monitoring & Observability (Weeks 7-8)

### P1.10 Crashlytics Integration
**Files**:
- `wakeveApp/build.gradle.kts` (ajouter Firebase Crashlytics)
- `wakeveApp/src/main/kotlin/com/guyghost/wakeve/WakeveApp.kt` (init)

**Complexity**: Low
**Time**: 1-2 days
**Priority**: P1
**Dependencies**: None
**Description**: Intégrer Firebase Crashlytics pour le crash reporting

**Acceptance Criteria**:
- [ ] Crashlytics configuré (Android + iOS)
- [ ] Crash reporting automatique
- [ ] Custom logs pour contexte (userId, eventId)
- [ ] Non-fatal errors capturés
- [ ] Test de crash (bouton "Crash App")

**Dépendances build.gradle.kts**:
```kotlin
// wakeveApp/build.gradle.kts
dependencies {
    implementation("com.google.firebase:firebase-crashlytics-ktx:19.0.3")
}

apply plugin: "com.google.firebase.crashlytics"
```

---

### P1.11 Performance Monitoring
**Files**:
- `wakeveApp/build.gradle.kts` (ajouter Firebase Performance)
- `shared/src/commonMain/kotlin/com/guyghost/wakeve/performance/PerformanceMonitor.kt` (nouveau)

**Complexity**: Medium
**Time**: 2-3 days
**Priority**: P1
**Dependencies**: None
**Description**: Configurer Firebase Performance Monitoring

**Acceptance Criteria**:
- [ ] Performance monitoring configuré
- [ ] Custom traces: "event_list_load", "event_detail_load"
- [ ] Network traces: API calls (GET /api/events, POST /api/events/{id}/poll/votes)
- [ ] Startup time monitoring
- [ ] Screen rendering time (Compose, SwiftUI)

**Implémentation**:
```kotlin
// shared/src/commonMain/kotlin/com/guyghost/wakeve/performance/PerformanceMonitor.kt
class PerformanceMonitor {
    fun startTrace(name: String): Trace {
        return Firebase.performance.newTrace(name).start()
    }

    inline fun <T> trace(name: String, block: () -> T): T {
        val trace = startTrace(name)
        return try {
            block()
        } finally {
            trace.stop()
        }
    }
}

// Utilisation
val event = performanceMonitor.trace("load_events") {
    eventRepository.getAllEvents()
}
```

---

### P1.12 Structured Logging (WakevLogger)
**Files**:
- `shared/src/commonMain/kotlin/com/guyghost/wakeve/logging/WakevLogger.kt` (nouveau)
- `shared/src/commonMain/kotlin/com/guyghost/wakeve/logging/LogLevel.kt` (nouveau)

**Complexity**: Low
**Time**: 1-2 days
**Priority**: P1
**Dependencies**: None
**Description**: Créer un logger structuré JSON

**Acceptance Criteria**:
- [ ] Niveaux: DEBUG, INFO, WARN, ERROR
- [ ] Format JSON (pour parsing)
- [ ] Rotation des logs (7 jours, max 50MB)
- [ ] Filtres par log level (configurable)
- [ ] Tests unitaires

**Implémentation**:
```kotlin
// shared/src/commonMain/kotlin/com/guyghost/wakeve/logging/WakevLogger.kt
object WakevLogger {
    private val logLevel = LogLevel.INFO

    fun d(tag: String, message: String, metadata: Map<String, Any?> = emptyMap()) {
        log(LogLevel.DEBUG, tag, message, metadata)
    }

    fun i(tag: String, message: String, metadata: Map<String, Any?> = emptyMap()) {
        log(LogLevel.INFO, tag, message, metadata)
    }

    fun w(tag: String, message: String, metadata: Map<String, Any?> = emptyMap()) {
        log(LogLevel.WARN, tag, message, metadata)
    }

    fun e(tag: String, message: String, error: Throwable? = null, metadata: Map<String, Any?> = emptyMap()) {
        log(LogLevel.ERROR, tag, message, metadata + ("error" to error?.stackTraceToString()))
    }

    private fun log(level: LogLevel, tag: String, message: String, metadata: Map<String, Any?>) {
        if (level.ordinal >= logLevel.ordinal) {
            val logEntry = mapOf(
                "timestamp" to System.currentTimeMillis(),
                "level" to level.name,
                "tag" to tag,
                "message" to message,
                "metadata" to metadata
            )
            println(Json.encodeToString(logEntry))
        }
    }
}
```

---

## Summary Timeline

| Week | Tasks | Output |
|-------|-------|--------|
| **1** | P0.1 (Database Indexes), P0.3 (Cache LRU) | DB optimisée, Cache LRU |
| **2** | P0.2 (Pagination), P0.4 (Coil), P0.5 (Memory Profiling) | Listes paginées, Images optimisées |
| **3** | Tests Performance, Bug fixes | Performance mesurée et validée |
| **4** | P1.1 (Analytics Interface), P1.2 (Firebase Provider) | Analytics setup |
| **5** | P1.3 (Analytics Integration), P1.4 (Dashboard), P1.5 (RGPD) | Analytics intégré et RGPD compliant |
| **6** | P0.6 (Rich Notifications), P0.7 (Categories) | Notifications riches |
| **7** | P0.8 (NotificationScheduler), P1.9 (Deep Linking) | Scheduler complet |
| **8** | P1.10 (Crashlytics), P1.11 (Performance Monitoring), P1.12 (Logger) | Monitoring complet |

---

## Risques & Mitigations

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| **P0.6/P0.8: Rich Notifications bugs sur iOS** | Medium | High | Tests instrumentés iOS, fallback aux notifications basiques |
| **P1.4: Dashboard performance avec gros volumes** | Medium | Medium | Indexes DB, pagination API, cache Redis |
| **P0.1: Indexes augmente la taille de la DB** | Low | Low | Surveiller la taille DB, monitorer performance INSERT/UPDATE |
| **P1.5: RGPD consentement complexe** | Low | Medium | UX simple, documentation claire, storage chiffré |
| **Cross-platform: Différences Android/iOS** | High | Medium | Abstraction commune, tests par plateforme, fallback si nécessaire |

---

## Non-Goals (Phase 7+)

- Machine Learning pour recommandations personnalisées
- A/B testing framework
- Feature flags (remote config)
- Real-time analytics (websockets)
- Advanced segmentation utilisateur

---

## Dependencies Graph

```
phase-6-performance
├── P0.1 (DB Indexes) ─┐
├── P0.3 (Cache) ────────┤
├── P0.2 (Pagination) ────┤
├── P0.4 (Coil) ──────────┤
└── P0.5 (Memory Profiling)─┘
                            │
                            ▼
phase-6-analytics
├── P1.1 (Interface) ─────┐
├── P1.2 (Firebase) ────────┤
├── P1.3 (Integration) ────┤
├── P1.4 (Dashboard) ───────┤
└── P1.5 (RGPD) ───────────┘
                            │
                            ▼
phase-6-advanced-push
├── P0.6 (Rich Notifications) ──┐
├── P0.7 (Categories) ─────────┤
├── P0.8 (Scheduler) ───────────┤
└── P1.9 (Deep Linking) ────────┘
                                │
                                ▼
Monitoring
├── P1.10 (Crashlytics)
├── P1.11 (Performance)
└── P1.12 (Logger)
```

---

## Success Metrics

### Performance
- [ ] Query time réduite de 50%+ sur les requêtes fréquentes
- [ ] Scroll 60fps sur les listes paginées
- [ ] Memory stable (pas de leaks) avec 1000+ events

### Analytics
- [ ] 20+ events tracked dans tous les ViewModels
- [ ] Dashboard accessible avec métriques MAU/DAU/funnel
- [ ] RGPD compliant (consentement, suppression)

### Push Notifications
- [ ] Rich notifications (images, actions) fonctionnelles
- [ ] Scheduler pour rappels (15min, 1h, 24h)
- [ ] Deep linking vers l'écran correct
- [ ] Rate limiting (max 5 notifications/heure)

### Monitoring
- [ ] Crashlytics intégré (Android + iOS)
- [ ] Performance traces pour 10+ opérations
- [ ] Structured logging (JSON) avec rotation

---

## Questions & Recommendations

### 1. Tâche la plus risquée ?

**Réponse**: **P0.6/P0.8: Rich Notifications + Scheduler**

**Pourquoi**:
- Complexité cross-platform élevée (Android/iOS diffèrent significativement)
- iOS notifications avec actions nécessitent UNUserNotificationCenter complexe
- WorkManager Android peut avoir des bugs sur certains devices
- Deep linking depuis notifications peut être instable

**Mitigation**:
- Tests instrumentés étendus
- Fallback aux notifications basiques si crash
- Monitoring des crashs via Crashlytics
- Feature flag pour désactiver rapidement si problèmes

---

### 2. Quelles tâches peuvent être parallélisées ?

**Parallélisation possible**:

1. **Performance**: P0.1 (DB Indexes) et P0.3 (Cache LRU) peuvent être développés en parallèle (aucune dépendance)
2. **Analytics**: P1.1 (Interface) et P1.10 (Crashlytics) sont indépendants
3. **Monitoring**: P1.10, P1.11, P1.12 peuvent être développés en parallèle après P1.2 (Firebase setup)

**Recommandation**: Assigner 2-3 développeurs sur les tâches parallèles pour réduire la durée totale de 8 semaines à ~5-6 semaines.

---

### 3. Estimation totale de temps pour la Phase 6 ?

**Réponse**: **8 semaines** avec 1 développeur, **5-6 semaines** avec 2-3 développeurs

**Breakdown**:
- Performance: 3 semaines
- Analytics: 2 semaines
- Push: 2 semaines
- Monitoring: 1 semaine

**Estimation détaillée par tâche**:
- P0.1: 2-3 days
- P0.2: 3-4 days
- P0.3: 1-2 days
- P0.4: 1 day
- P0.5: 2-3 days
- P1.1: 1 day
- P1.2: 2-3 days
- P1.3: 3-4 days
- P1.4: 4-5 days
- P1.5: 2-3 days
- P0.6: 4-5 days
- P0.7: 1 day
- P0.8: 4-5 days
- P1.9: 2-3 days
- P1.10: 1-2 days
- P1.11: 2-3 days
- P1.12: 1-2 days

**Total**: ~40 working days (8 semaines @ 5 days/week)

---

### 4. Faut-il scinder Phase 6 en plusieurs changements plus petits ?

**Réponse**: **OUI, fortement recommandé**

**Pourquoi**:
1. **Complexité**: Phase 6 contient 3 domaines distincts (Performance, Analytics, Push) qui n'ont que peu de dépendances
2. **Delivery incrémental**: Chaque domaine peut être livré indépendamment avec de la valeur
3. **Reviews**: PRs plus petits = reviews plus rapides et plus approfondies
4. **Testing**: Tests isolés par domaine plus faciles à maintenir
5. **Risk**: Si un domaine a des problèmes, les autres ne sont pas bloqués

**Recommandation**: Scinder en 3 sous-changes OpenSpec:

1. **`phase-6-performance`** (Weeks 1-3)
   - P0.1, P0.2, P0.3, P0.4, P0.5
   - Livrable: DB optimisée, listes paginées, cache LRU

2. **`phase-6-analytics`** (Weeks 4-5)
   - P1.1, P1.2, P1.3, P1.4, P1.5, P1.10, P1.11, P1.12
   - Livrable: Analytics complet, dashboard, monitoring

3. **`phase-6-advanced-push`** (Weeks 6-8)
   - P0.6, P0.7, P0.8, P1.9
   - Livrable: Rich notifications, scheduler, deep linking

**Avantages**:
- Chaque sous-change peut être archivé indépendamment
- Feedback utilisateur plus rapide (après 3, 5, 8 semaines)
- Risque réparti sur 3 PRs plus petits
- Plus facile de prioriser/cut scope si nécessaire

---

## Checklist de Validation

### Avant de commencer chaque sous-change:
- [ ] Proposal.md créé et validé (`openspec validate <change-id> --strict`)
- [ ] Design.md rédigé si nécessaire (pour changes complexes)
- [ ] Spécifications deltas créées pour chaque capability affectée
- [ ] Review et approval obtenue avant l'implémentation

### Avant d'archiver chaque sous-change:
- [ ] Toutes les tâches cochées dans tasks.md
- [ ] Tests passants (100%)
- [ ] Documentation mise à jour
- [ ] Performance benchmarks exécutés et documentés
- [ ] Code review approuvée
- [ ] `openspec archive <change-id> --yes` exécuté

---

## Documentation à mettre à jour

- [ ] `AGENTS.md` - Ajouter descriptions des nouveaux agents (AnalyticsAgent, MonitoringAgent)
- [ ] `CONTRIBUTING.md` - Ajouter conventions pour analytics et performance
- [ ] `.opencode/context.md` - Mettre à jour avec les livrables Phase 6
- [ ] `QUICK_START.md` - Ajouter instructions pour analytics/push
- [ ] API Docs - Documenter les nouveaux endpoints analytics

---

## Prochaine étape

Une fois ce `tasks.md` validé:

1. Créer les 3 sous-changes OpenSpec:
   - `openspec/changes/phase-6-performance/`
   - `openspec/changes/phase-6-analytics/`
   - `openspec/changes/phase-6-advanced-push/`

2. Pour chaque sous-change:
   - Créer `proposal.md`
   - Copier les tâches correspondantes dans `tasks.md`
   - Créer `specs/` deltas si nécessaire

3. Valider chaque sous-change avec `openspec validate <change-id> --strict`

4. Obtenir l'approbation avant de commencer l'implémentation

---

**Fin du document tasks.md**
