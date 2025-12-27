# Transport Optimization Specification

## Version
**Version**: 1.0.0
**Status**: ✅ Implémenté
**Date de création**: 26 décembre 2025
**Auteur**: Équipe Wakeve

## Overview

Le système de Transport de Wakeve calcule des routes optimisées pour des groupes multi-participants en tenant compte des contraintes de coût, de temps et d'équilibre entre participants.

## Domain Model

### Core Concepts

- **Multi-Participant Routing**: Calcul de routes optimales depuis plusieurs points de départ
- **Departure Locations**: Lieux de départ de chaque participant
- **Meeting Points**: Points de rencontre pour réduire les trajets
- **Optimization Modes**: Coût, Temps, Équilibré
- **Transport Providers**: Intégration avec APIs externes (vols, trains, bus, location de voiture)

### DepartureLocation

```kotlin
@Serializable
data class DepartureLocation(
    val id: String,
    val participantId: String,
    val eventId: String,
    val city: String,
    val country: String,
    val latitude: Double?,
    val longitude: Double?,
    val preferredDepartureTime: Instant?,
    val transportMode: TransportMode
)
```

### TransportMode

```kotlin
enum class TransportMode {
    PLANE,           // Avion (vols)
    TRAIN,           // Train
    BUS,             // Bus
    CAR,             // Voiture (location ou partage)
    CARPOOL,         // Covoiturage
    FERRY,           // Ferry
    MIXED            // Combiné
}
```

### TransportRoute

```kotlin
@Serializable
data class TransportRoute(
    val id: String,
    val eventId: String,
    val optimizationMode: OptimizationMode,
    val meetingPoint: String?,
    val totalCost: Double,
    val totalDuration: Duration,
    val participantRoutes: List<ParticipantRoute>,
    val createdAt: Instant,
    val isValid: Boolean
)
```

### ParticipantRoute

```kotlin
@Serializable
data class ParticipantRoute(
    val participantId: String,
    val departureLocation: DepartureLocation,
    val destination: String,
    val segments: List<RouteSegment>,
    val totalCost: Double,
    val totalDuration: Duration,
    val departureTime: Instant,
    val arrivalTime: Instant
)
```

### RouteSegment

```kotlin
@Serializable
data class RouteSegment(
    val id: String,
    val transportMode: TransportMode,
    val from: String,
    val to: String,
    val departureTime: Instant,
    val arrivalTime: Instant,
    val cost: Double,
    val duration: Duration,
    val carrier: String?,         // Compagnie (Air France, SNCF, etc.)
    val flightNumber: String?,      // Pour avion
    val trainNumber: String?,      // Pour train
    val bookingUrl: String?,       // URL de réservation
    val isDirect: Boolean          // Vol/Train direct ou avec correspondance
)
```

### OptimizationMode

```kotlin
enum class OptimizationMode {
    COST_OPTIMIZED,       // Priorité au moindre coût
    TIME_OPTIMIZED,       // Priorité au temps le plus court
    BALANCED              // Équilibre coût/temps
}
```

### MeetingPointSuggestion

```kotlin
@Serializable
data class MeetingPointSuggestion(
    val city: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val avgDistanceFromParticipants: Double, // km
    val accessibilityScore: Double,       // 0.0-1.0
    val totalCost: Double,
    val reason: String
)
```

## TransportService

### Responsibilities

**Calcul de routes multi-participants** avec optimisation:
- Départ depuis lieux différents
- Point de rencontre optimal
- Segmentation du trajet en portions
- Calcul du coût total et du temps total

**Intégration avec providers**:
- Mock providers pour l'instant
- Structure pour APIs externes (Google Maps, SNCF, etc.)

**Optimization selon les modes**:
- Coût-optimized: Le moins cher
- Time-optimized: Le plus rapide
- Balanced: Compromis optimal

### API

```kotlin
class TransportService(
    private val database: WakevDb,
    private val transportProvider: TransportProvider
) {

    /**
     * Calcule une route optimisée pour tous les participants
     */
    suspend fun calculateOptimizedRoute(
        eventId: String,
        destination: String,
        optimizationMode: OptimizationMode = OptimizationMode.BALANCED
    ): Result<TransportRoute> {
        val participants = database.participantQueries
            .selectByEventId(eventId)
            .executeAsList()

        val departureLocations = participants.map { participant ->
            database.departureLocationQueries
                .selectByParticipantId(participant.id)
                .executeAsOneOrNull()
        }.filterNotNull()

        if (departureLocations.isEmpty()) {
            return Result.failure(NoDepartureLocationsException())
        }

        val route = transportProvider.findOptimalRoute(
            departureLocations = departureLocations,
            destination = destination,
            mode = optimizationMode
        )

        // Save route to database
        database.transportRouteQueries.insert(route)

        return Result.success(route)
    }

    /**
     * Suggère des points de rencontre
     */
    suspend fun suggestMeetingPoints(
        eventId: String,
        destination: String,
        maxDistance: Double = 200.0 // km
    ): Result<List<MeetingPointSuggestion>> {
        val participants = database.participantQueries
            .selectByEventId(eventId)
            .executeAsList()

        val departureLocations = participants.mapNotNull { participant ->
            database.departureLocationQueries
                .selectByParticipantId(participant.id)
                .executeAsOneOrNull()
        }

        val meetingPoints = transportProvider.findMeetingPoints(
            departureLocations = departureLocations,
            destination = destination,
            maxDistance = maxDistance
        )

        return Result.success(meetingPoints)
    }

    /**
     * Met à jour le lieu de départ d'un participant
     */
    suspend fun updateDepartureLocation(
        participantId: String,
        city: String,
        country: String,
        latitude: Double? = null,
        longitude: Double? = null,
        preferredDepartureTime: Instant? = null,
        transportMode: TransportMode
    ): Result<Unit> {
        val existing = database.departureLocationQueries
            .selectByParticipantId(participantId)
            .executeAsOneOrNull()

        val location = DepartureLocation(
            id = existing?.id ?: UUID.randomUUID().toString(),
            participantId = participantId,
            eventId = existing?.eventId ?: "",
            city = city,
            country = country,
            latitude = latitude,
            longitude = longitude,
            preferredDepartureTime = preferredDepartureTime,
            transportMode = transportMode
        )

        if (existing == null) {
            database.departureLocationQueries.insert(location)
        } else {
            database.departureLocationQueries.update(location)
        }

        return Result.success(Unit)
    }

    /**
     * Récupère toutes les routes calculées pour un événement
     */
    suspend fun getRoutesForEvent(eventId: String): List<TransportRoute> {
        return database.transportRouteQueries
            .selectByEventId(eventId)
            .executeAsList()
    }

    /**
     * Récupère la route la plus récente pour un événement
     */
    suspend fun getLatestRoute(eventId: String): TransportRoute? {
        return database.transportRouteQueries
            .selectLatestByEventId(eventId)
            .executeAsOneOrNull()
    }
}
```

## TransportProvider

### Mock Implementation

```kotlin
class TransportProvider : TransportProvider {

    override suspend fun findOptimalRoute(
        departureLocations: List<DepartureLocation>,
        destination: String,
        mode: OptimizationMode
    ): TransportRoute {
        // Mock implementation
        val totalCost = when (mode) {
            OptimizationMode.COST_OPTIMIZED -> 250.0
            OptimizationMode.TIME_OPTIMIZED -> 450.0
            OptimizationMode.BALANCED -> 350.0
        }

        val totalDuration = when (mode) {
            OptimizationMode.COST_OPTIMIZED -> Duration.ofHours(8)
            OptimizationMode.TIME_OPTIMIZED -> Duration.ofHours(4)
            OptimizationMode.BALANCED -> Duration.ofHours(5)
        }

        val participantRoutes = departureLocations.map { location ->
            val cost = totalCost / departureLocations.size
            val duration = totalDuration.div(departureLocations.size.toLong())

            ParticipantRoute(
                participantId = location.participantId,
                departureLocation = location,
                destination = destination,
                segments = listOf(
                    RouteSegment(
                        id = UUID.randomUUID().toString(),
                        transportMode = location.transportMode,
                        from = location.city,
                        to = destination,
                        departureTime = location.preferredDepartureTime ?: Instant.now(),
                        arrivalTime = (location.preferredDepartureTime ?: Instant.now()).plus(duration),
                        cost = cost,
                        duration = duration,
                        carrier = "Mock Carrier",
                        flightNumber = null,
                        trainNumber = null,
                        bookingUrl = null,
                        isDirect = true
                    )
                ),
                totalCost = cost,
                totalDuration = duration,
                departureTime = location.preferredDepartureTime ?: Instant.now(),
                arrivalTime = (location.preferredDepartureTime ?: Instant.now()).plus(duration)
            )
        }

        return TransportRoute(
            id = UUID.randomUUID().toString(),
            eventId = "",
            optimizationMode = mode,
            meetingPoint = null,
            totalCost = totalCost,
            totalDuration = totalDuration,
            participantRoutes = participantRoutes,
            createdAt = Instant.now(),
            isValid = true
        )
    }

    override suspend fun findMeetingPoints(
        departureLocations: List<DepartureLocation>,
        destination: String,
        maxDistance: Double
    ): List<MeetingPointSuggestion> {
        // Mock implementation - suggests intermediate cities
        return listOf(
            MeetingPointSuggestion(
                city = "Paris",
                country = "France",
                latitude = 48.8566,
                longitude = 2.3522,
                avgDistanceFromParticipants = 150.0,
                accessibilityScore = 0.9,
                totalCost = 300.0,
                reason = "Central hub with excellent transport connections"
            ),
            MeetingPointSuggestion(
                city = "Lyon",
                country = "France",
                latitude = 45.7640,
                longitude = 4.8357,
                avgDistanceFromParticipants = 120.0,
                accessibilityScore = 0.8,
                totalCost = 280.0,
                reason = "Good compromise between cost and distance"
            )
        )
    }
}
```

## Optimization Algorithms

### Cost Optimization

**Objectif**: Minimiser le coût total

```kotlin
fun optimizeForCost(
    departureLocations: List<DepartureLocation>,
    destination: String
): TransportRoute {
    // 1. Trouver les options les moins chères pour chaque participant
    // 2. Calculer le point de rencontre qui minimise la somme
    // 3. Retourner la route la moins chère

    val cheapestOptions = departureLocations.map { location ->
        transportProvider.getCheapestOptions(location, destination)
    }

    val meetingPoint = findOptimalMeetingPoint(
        cheapestOptions,
        OptimizationMode.COST_OPTIMIZED
    )

    return buildRoute(cheapestOptions, meetingPoint)
}
```

### Time Optimization

**Objectif**: Minimiser la durée totale

```kotlin
fun optimizeForTime(
    departureLocations: List<DepartureLocation>,
    destination: String
): TransportRoute {
    // 1. Trouver les options les plus rapides pour chaque participant
    // 2. Calculer le point de rencontre qui minimise la durée
    // 3. Retourner la route la plus rapide

    val fastestOptions = departureLocations.map { location ->
        transportProvider.getFastestOptions(location, destination)
    }

    val meetingPoint = findOptimalMeetingPoint(
        fastestOptions,
        OptimizationMode.TIME_OPTIMIZED
    )

    return buildRoute(fastestOptions, meetingPoint)
}
```

### Balanced Optimization

**Objectif**: Équilibrer coût et temps

```kotlin
fun optimizeBalanced(
    departureLocations: List<DepartureLocation>,
    destination: String
): TransportRoute {
    // 1. Pondérer le coût (50%) et le temps (50%)
    // 2. Calculer un score composite pour chaque option
    // 3. Retourner la route avec le meilleur score composite

    val allOptions = departureLocations.map { location ->
        val options = transportProvider.getAllOptions(location, destination)

        options.map { option ->
            val costScore = normalizeCost(option.cost)
            val timeScore = normalizeTime(option.duration)

            val compositeScore = (costScore * 0.5) + (timeScore * 0.5)

            option.copy(score = compositeScore)
        }.sortedByDescending { it.score }.first()
    }

    val meetingPoint = findOptimalMeetingPoint(
        allOptions,
        OptimizationMode.BALANCED
    )

    return buildRoute(allOptions, meetingPoint)
}
```

### Meeting Point Selection

```kotlin
fun findOptimalMeetingPoint(
    routes: List<ParticipantRoute>,
    mode: OptimizationMode
): String? {
    if (routes.size <= 1) return null

    val allCities = routes.flatMap { route ->
        route.segments.map { it.to }
    }

    val meetingPoints = allCities.distinct().map { city ->
        val avgDistance = calculateAverageDistance(routes, city)
        val totalCost = calculateTotalCostToCity(routes, city)
        val accessibility = calculateAccessibilityScore(city)

        val score = when (mode) {
            OptimizationMode.COST_OPTIMIZED ->
                (1.0 / totalCost) * 1000
            OptimizationMode.TIME_OPTIMIZED ->
                (1.0 / avgDistance) * 100
            OptimizationMode.BALANCED ->
                ((1.0 / totalCost) * 500 + (1.0 / avgDistance) * 50 + accessibility * 100)
        }

        MeetingPointSuggestion(
            city = city,
            country = "", // To be fetched
            latitude = 0.0,
            longitude = 0.0,
            avgDistanceFromParticipants = avgDistance,
            accessibilityScore = accessibility,
            totalCost = totalCost,
            reason = when (mode) {
                OptimizationMode.COST_OPTIMIZED -> "Cheapest meeting point"
                OptimizationMode.TIME_OPTIMIZED -> "Fastest meeting point"
                OptimizationMode.BALANCED -> "Best compromise"
            }
        )
    }

    return meetingPoints.maxByOrNull { it.accessibilityScore }?.city
}
```

## Database Schema

### DepartureLocation.sq

```sql
CREATE TABLE departure_location (
    id TEXT PRIMARY KEY,
    participant_id TEXT NOT NULL,
    event_id TEXT NOT NULL,
    city TEXT NOT NULL,
    country TEXT NOT NULL,
    latitude REAL,
    longitude REAL,
    preferred_departure_time INTEGER,
    transport_mode TEXT NOT NULL,
    created_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),
    updated_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),
    FOREIGN KEY (participant_id) REFERENCES participant(id) ON DELETE CASCADE,
    FOREIGN KEY (event_id) REFERENCES event(id) ON DELETE CASCADE
);

CREATE INDEX idx_departure_location_participant ON departure_location(participant_id);
CREATE INDEX idx_departure_location_event ON departure_location(event_id);

insertDepartureLocation:
INSERT INTO departure_location (id, participant_id, event_id, city, country, latitude, longitude, preferred_departure_time, transport_mode)
VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);

updateDepartureLocation:
UPDATE departure_location
SET city = ?, country = ?, latitude = ?, longitude = ?, preferred_departure_time = ?, transport_mode = ?, updated_at = strftime('%s', 'now')
WHERE id = ?;

deleteDepartureLocation:
DELETE FROM departure_location WHERE id = ?;

selectByParticipantId:
SELECT * FROM departure_location WHERE participant_id = ?;

selectByEventId:
SELECT * FROM departure_location WHERE event_id = ?;
```

### TransportRoute.sq

```sql
CREATE TABLE transport_route (
    id TEXT PRIMARY KEY,
    event_id TEXT NOT NULL,
    optimization_mode TEXT NOT NULL,
    meeting_point TEXT,
    total_cost REAL NOT NULL,
    total_duration INTEGER NOT NULL,
    is_valid INTEGER NOT NULL DEFAULT 1,
    created_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),
    FOREIGN KEY (event_id) REFERENCES event(id) ON DELETE CASCADE
);

CREATE INDEX idx_transport_route_event ON transport_route(event_id);
CREATE INDEX idx_transport_route_created_at ON transport_route(created_at DESC);

insertTransportRoute:
INSERT INTO transport_route (id, event_id, optimization_mode, meeting_point, total_cost, total_duration, is_valid)
VALUES (?, ?, ?, ?, ?, ?, ?);

updateTransportRoute:
UPDATE transport_route
SET is_valid = 0
WHERE id = ?;

selectByEventId:
SELECT * FROM transport_route WHERE event_id = ? ORDER BY created_at DESC;

selectLatestByEventId:
SELECT * FROM transport_route WHERE event_id = ? ORDER BY created_at DESC LIMIT 1;

deleteByEventId:
DELETE FROM transport_route WHERE event_id = ?;
```

### RouteSegment.sq

```sql
CREATE TABLE route_segment (
    id TEXT PRIMARY KEY,
    route_id TEXT NOT NULL,
    participant_id TEXT NOT NULL,
    transport_mode TEXT NOT NULL,
    from_city TEXT NOT NULL,
    to_city TEXT NOT NULL,
    departure_time INTEGER NOT NULL,
    arrival_time INTEGER NOT NULL,
    cost REAL NOT NULL,
    duration INTEGER NOT NULL,
    carrier TEXT,
    flight_number TEXT,
    train_number TEXT,
    booking_url TEXT,
    is_direct INTEGER NOT NULL DEFAULT 1,
    FOREIGN KEY (route_id) REFERENCES transport_route(id) ON DELETE CASCADE,
    FOREIGN KEY (participant_id) REFERENCES participant(id) ON DELETE CASCADE
);

CREATE INDEX idx_route_segment_route ON route_segment(route_id);
CREATE INDEX idx_route_segment_participant ON route_segment(participant_id);

insertRouteSegment:
INSERT INTO route_segment (id, route_id, participant_id, transport_mode, from_city, to_city, departure_time, arrival_time, cost, duration, carrier, flight_number, train_number, booking_url, is_direct)
VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);

selectByRouteId:
SELECT * FROM route_segment WHERE route_id = ?;

selectByParticipantId:
SELECT * FROM route_segment WHERE participant_id = ?;

deleteByRouteId:
DELETE FROM route_segment WHERE route_id = ?;
```

## API Endpoints

```
POST   /api/events/{id}/transport/routes           # Calculer route optimisée
GET    /api/events/{id}/transport/routes           # Récupérer toutes les routes
GET    /api/events/{id}/transport/routes/latest     # Récupérer la dernière route
PUT    /api/events/{id}/transport/participants/{participantId}/departure  # Mettre à jour départ
GET    /api/events/{id}/transport/meeting-points   # Suggérer points de rencontre
DELETE /api/events/{id}/transport/routes/{routeId} # Supprimer route
GET    /api/events/{id}/transport/participants/{participantId}/segments  # Segments participant
```

## Scenarios

### SCENARIO 1: Calculate Optimized Route

**GIVEN**: Événement avec 4 participants depuis différentes villes
**AND**: Destination: "Nice, France"
**WHEN**: Organisateur demande route optimisée (mode: BALANCED)
**THEN**: Système calcule route avec:
  - Coût total: 350€
  - Durée totale: 5h
  - Point de rencontre: "Lyon, France"
  - Segments pour chaque participant
**AND**: Route sauvegardée en base

```kotlin
val route = transportService.calculateOptimizedRoute(
    eventId = "event-1",
    destination = "Nice, France",
    optimizationMode = OptimizationMode.BALANCED
)

assertTrue(route.totalCost == 350.0)
assertTrue(route.totalDuration == Duration.ofHours(5))
assertEquals(route.participantRoutes.size, 4)
```

### SCENARIO 2: Suggest Meeting Points

**GIVEN**: Participants à Paris, Marseille, Lyon
**WHEN**: Organisateur demande suggestions de points de rencontre
**THEN**: Système retourne:
  - Paris (accessibility: 0.9, cost: 300€)
  - Lyon (accessibility: 0.8, cost: 280€)
**AND**: Suggestions triées par accessibilité

```kotlin
val meetingPoints = transportService.suggestMeetingPoints(
    eventId = "event-1",
    destination = "Nice, France"
)

assertTrue(meetingPoints.isNotEmpty())
assertTrue(meetingPoints[0].accessibilityScore > meetingPoints[1].accessibilityScore)
```

### SCENARIO 3: Update Departure Location

**GIVEN**: Participant à Paris
**WHEN**: Participant met à jour son départ vers "Lyon"
**THEN**: Lieu de départ mis à jour
**AND**: Recalcul automatique de la route si elle existe

```kotlin
transportService.updateDepartureLocation(
    participantId = "user-1",
    city = "Lyon",
    country = "France",
    transportMode = TransportMode.TRAIN
)

val updated = database.departureLocationQueries
    .selectByParticipantId("user-1")
    .executeAsOne()

assertEquals(updated.city, "Lyon")
```

### SCENARIO 4: Cost Optimization

**GIVEN**: Participants à Paris, Lyon, Marseille
**WHEN**: Organisateur demande route la moins chère
**THEN**: Route proposée:
  - Coût minimal: 250€
  - Peut prendre plus de temps
  - Utilise transport en commun

```kotlin
val route = transportService.calculateOptimizedRoute(
    eventId = "event-1",
    destination = "Nice, France",
    optimizationMode = OptimizationMode.COST_OPTIMIZED
)

assertTrue(route.totalCost < 300.0)
assertTrue(route.optimizationMode == OptimizationMode.COST_OPTIMIZED)
```

### SCENARIO 5: Time Optimization

**GIVEN**: Participants à Paris, Lyon, Marseille
**WHEN**: Organisateur demande route la plus rapide
**THEN**: Route proposée:
  - Durée minimale: 4h
  - Peut coûter plus cher
  - Utilise avion ou TGV

```kotlin
val route = transportService.calculateOptimizedRoute(
    eventId = "event-1",
    destination = "Nice, France",
    optimizationMode = OptimizationMode.TIME_OPTIMIZED
)

assertTrue(route.totalDuration < Duration.ofHours(5))
assertTrue(route.optimizationMode == OptimizationMode.TIME_OPTIMIZED)
```

## Implementation Notes

### Error Handling

```kotlin
class NoDepartureLocationsException : Exception("No departure locations provided")

class RouteCalculationFailedException(reason: String) : Exception(reason)

class InvalidDestinationException(destination: String) : Exception("Invalid destination: $destination")

sealed class TransportResult<T> {
    data class Success<T>(val value: T) : TransportResult<T>()
    data class Failure<T>(val error: Exception) : TransportResult<T>()
}
```

### Provider Integration Strategy

**Phase 1 (Current)**:
- Mock providers pour tous les types de transport
- Données fictives pour démonstration
- Pas d'appels API externes

**Phase 2 (Future)**:
- Intégration Google Maps API pour géocoding
- Intégration APIs transporteurs (SNCF, Air France, etc.)
- Intégration BlaBlaCar pour covoiturage
- Intégration RentalCars pour location de voitures

### Caching

```kotlin
class TransportCache(
    private val maxCacheSize: Int = 50,
    private val ttlSeconds: Long = 300 // 5 minutes
) {
    private val cache = mutableMapOf<String, CacheEntry>()

    data class CacheEntry(
        val route: TransportRoute,
        val timestamp: Instant
    )

    fun get(key: String): TransportRoute? {
        val entry = cache[key] ?: return null

        if (entry.timestamp.plusSeconds(ttlSeconds).isAfter(Instant.now())) {
            return entry.route
        }

        cache.remove(key)
        return null
    }

    fun put(key: String, route: TransportRoute) {
        if (cache.size >= maxCacheSize) {
            // Remove oldest entry
            val oldestKey = cache.minByOrNull { it.value.timestamp }?.key
            oldestKey?.let { cache.remove(it) }
        }

        cache[key] = CacheEntry(route, Instant.now())
    }
}
```

## Testing

### Unit Tests

#### TransportServiceTest

```kotlin
class TransportServiceTest {
    @Test
    fun `calculateOptimizedRoute with cost mode returns cheapest route`() {
        // Given
        val participants = createParticipants(3)
        val departureLocations = createDepartureLocations(participants)

        // When
        val result = transportService.calculateOptimizedRoute(
            eventId = "event-1",
            destination = "Nice, France",
            optimizationMode = OptimizationMode.COST_OPTIMIZED
        )

        // Then
        assertTrue(result.isSuccess)
        val route = result.getOrThrow()
        assertTrue(route.totalCost < 300.0)
        assertTrue(route.optimizationMode == OptimizationMode.COST_OPTIMIZED)
    }

    @Test
    fun `calculateOptimizedRoute with time mode returns fastest route`() {
        // Given
        val participants = createParticipants(3)
        val departureLocations = createDepartureLocations(participants)

        // When
        val result = transportService.calculateOptimizedRoute(
            eventId = "event-1",
            destination = "Nice, France",
            optimizationMode = OptimizationMode.TIME_OPTIMIZED
        )

        // Then
        assertTrue(result.isSuccess)
        val route = result.getOrThrow()
        assertTrue(route.totalDuration < Duration.ofHours(5))
        assertTrue(route.optimizationMode == OptimizationMode.TIME_OPTIMIZED)
    }

    @Test
    fun `calculateOptimizedRoute with balanced mode returns optimal compromise`() {
        // Given
        val participants = createParticipants(3)
        val departureLocations = createDepartureLocations(participants)

        // When
        val result = transportService.calculateOptimizedRoute(
            eventId = "event-1",
            destination = "Nice, France",
            optimizationMode = OptimizationMode.BALANCED
        )

        // Then
        assertTrue(result.isSuccess)
        val route = result.getOrThrow()
        assertTrue(route.totalCost > 250.0 && route.totalCost < 450.0)
        assertTrue(route.optimizationMode == OptimizationMode.BALANCED)
    }

    @Test
    fun `calculateOptimizedRoute with no departure locations returns error`() {
        // When
        val result = transportService.calculateOptimizedRoute(
            eventId = "event-empty",
            destination = "Nice, France"
        )

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NoDepartureLocationsException)
    }

    @Test
    fun `suggestMeetingPoints returns accessible locations`() {
        // Given
        val participants = createParticipants(3)
        val departureLocations = createDepartureLocations(participants)

        // When
        val result = transportService.suggestMeetingPoints(
            eventId = "event-1",
            destination = "Nice, France"
        )

        // Then
        assertTrue(result.isSuccess)
        val meetingPoints = result.getOrThrow()
        assertTrue(meetingPoints.isNotEmpty())
        assertTrue(meetingPoints.all { it.accessibilityScore > 0.5 })
    }

    @Test
    fun `updateDepartureLocation updates existing location`() {
        // Given
        val participantId = "user-1"
        transportService.updateDepartureLocation(
            participantId = participantId,
            city = "Paris",
            country = "France",
            transportMode = TransportMode.TRAIN
        )

        // When
        transportService.updateDepartureLocation(
            participantId = participantId,
            city = "Lyon",
            country = "France",
            transportMode = TransportMode.PLANE
        )

        // Then
        val location = database.departureLocationQueries
            .selectByParticipantId(participantId)
            .executeAsOne()

        assertEquals(location.city, "Lyon")
        assertEquals(location.transportMode, TransportMode.PLANE)
    }

    @Test
    fun `getRoutesForEvent returns routes in descending order`() {
        // Given
        createTestRoute("event-1", OptimizationMode.COST_OPTIMIZED)
        createTestRoute("event-1", OptimizationMode.TIME_OPTIMIZED)
        createTestRoute("event-1", OptimizationMode.BALANCED)

        // When
        val routes = transportService.getRoutesForEvent("event-1")

        // Then
        assertEquals(routes.size, 3)
        assertTrue(routes[0].createdAt.isAfter(routes[1].createdAt))
    }

    @Test
    fun `getLatestRoute returns most recent route`() {
        // Given
        val eventId = "event-1"
        val oldRoute = createTestRoute(eventId, OptimizationMode.COST_OPTIMIZED)
        Thread.sleep(100)
        val newRoute = createTestRoute(eventId, OptimizationMode.BALANCED)

        // When
        val latest = transportService.getLatestRoute(eventId)

        // Then
        assertNotNull(latest)
        assertEquals(latest?.id, newRoute.id)
        assertNotEquals(latest?.id, oldRoute.id)
    }
}
```

## Performance Considerations

### Database Indexes

```sql
-- Optimized queries
CREATE INDEX idx_departure_location_participant ON departure_location(participant_id);
CREATE INDEX idx_departure_location_event ON departure_location(event_id);
CREATE INDEX idx_transport_route_event ON transport_route(event_id);
CREATE INDEX idx_transport_route_created_at ON transport_route(created_at DESC);
CREATE INDEX idx_route_segment_route ON route_segment(route_id);
CREATE INDEX idx_route_segment_participant ON route_segment(participant_id);
```

### API Response Times

- **Calculate route**: < 2 seconds (mock) / < 5 seconds (real APIs)
- **Suggest meeting points**: < 1 second (mock) / < 3 seconds (real APIs)
- **Update departure location**: < 500ms
- **Get routes**: < 500ms

## Limitations

**Phase 1** (Current):
1. **Mock providers**: Toutes les données de transport sont mockées
2. **Pas d'intégration temps réel**: Pas d'appels aux APIs externes
3. **Calcul simplifié**: Algorithmes de base sans heuristiques avancées
4. **Pas de multi-modal routing**: Pas de combinaisons train+avion+bus

**Phase 2** (Future):
1. **Intégration APIs réelles**: Google Maps, transporteurs, etc.
2. **Multi-modal routing**: Combinaisons optimales de plusieurs modes
3. **Heuristiques avancées**: Algorithmes de pathfinding complexes
4. **Temps réel**: Mises à jour des prix et horaires
5. **Carpooling**: Intégration BlaBlaCar et autres

## Related Specs

- `event-organization/spec.md` - Main event management
- `calendar-management/spec.md` - Departure times in calendar
- `destination-planning/spec.md` - Destination selection
- `budget-management/spec.md` - Cost tracking
- `suggestion-management/spec.md` - Transport recommendations

---

**Version**: 1.0.0
**Last Updated**: 26 décembre 2025
**Maintainer**: Équipe Wakeve
