package com.guyghost.wakeve.transport

import com.guyghost.wakeve.models.OptimizationType
import com.guyghost.wakeve.models.Route
import com.guyghost.wakeve.models.TransportLocation
import com.guyghost.wakeve.models.TransportMode
import com.guyghost.wakeve.models.TransportOption
import com.guyghost.wakeve.models.TransportPlan
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.math.abs
import kotlin.random.Random

/**
 * Service de transport pour optimiser les trajets multi-participants
 *
 * Calcule des routes optimisées selon différents critères:
 * - COST_MINIMIZE: Minimiser le coût total
 * - TIME_MINIMIZE: Minimiser le temps total
 * - BALANCED: Équilibre coût et temps
 */
class TransportService {

    /**
     * Calcule les options de transport entre deux points
     */
    suspend fun getTransportOptions(
        from: TransportLocation,
        to: TransportLocation,
        departureTime: String,
        mode: TransportMode? = null
    ): List<TransportOption> {
        val departureInstant = Instant.parse(departureTime)
        val options = mutableListOf<TransportOption>()

        // Génère des options mockées pour différents modes de transport
        val modesToUse = mode?.let { listOf(it) } ?: TransportMode.entries

        modesToUse.forEach { transportMode ->
            when (transportMode) {
                TransportMode.FLIGHT -> generateFlightOptions(from, to, departureInstant, options)
                TransportMode.TRAIN -> generateTrainOptions(from, to, departureInstant, options)
                TransportMode.BUS -> generateBusOptions(from, to, departureInstant, options)
                TransportMode.CAR -> generateCarOptions(from, to, departureInstant, options)
                TransportMode.RIDESHARE -> generateRideshareOptions(from, to, departureInstant, options)
                TransportMode.TAXI -> generateTaxiOptions(from, to, departureInstant, options)
                TransportMode.WALKING -> generateWalkingOptions(from, to, departureInstant, options)
            }
        }

        return options.sortedBy { it.cost }
    }

    /**
     * Optimise les routes pour un groupe de participants
     */
    suspend fun optimizeRoutes(
        participants: Map<String, TransportLocation>, // participantId -> home location
        destination: TransportLocation,
        eventTime: String, // ISO 8601
        optimizationType: OptimizationType = OptimizationType.BALANCED
    ): TransportPlan {
        val eventInstant = Instant.parse(eventTime)
        val participantRoutes = mutableMapOf<String, Route>()
        var totalGroupCost = 0.0
        val groupArrivals = mutableListOf<String>()

        // Pour chaque participant, calcule la meilleure route vers la destination
        participants.forEach { (participantId, homeLocation) ->
            val options = getTransportOptions(homeLocation, destination, eventTime)
            if (options.isNotEmpty()) {
                val bestOption = when (optimizationType) {
                    OptimizationType.COST_MINIMIZE -> options.minByOrNull { it.cost }
                    OptimizationType.TIME_MINIMIZE -> options.minByOrNull { it.durationMinutes }
                    OptimizationType.BALANCED -> options.minByOrNull { calculateBalancedScore(it) }
                }!!

                val route = Route(
                    id = "route-${participantId}-${Random.nextInt()}",
                    segments = listOf(bestOption),
                    totalDurationMinutes = bestOption.durationMinutes,
                    totalCost = bestOption.cost,
                    currency = bestOption.currency,
                    score = calculateRouteScore(bestOption, optimizationType)
                )

                participantRoutes[participantId] = route
                totalGroupCost += bestOption.cost

                // Calcule l'heure d'arrivée
                val arrivalTime = eventInstant.minus(bestOption.durationMinutes, DateTimeUnit.MINUTE)
                groupArrivals.add(arrivalTime.toString())
            }
        }

        // Trouve les points de rencontre optimaux
        val meetingPoints = findGroupMeetingPoints(participantRoutes, maxWaitTimeMinutes = 60)

        return TransportPlan(
            eventId = "temp-event-id", // Sera fourni par l'appelant
            participantRoutes = participantRoutes,
            groupArrivals = meetingPoints,
            totalGroupCost = totalGroupCost,
            optimizationType = optimizationType,
            createdAt = Clock.System.now().toString()
        )
    }

    /**
     * Trouve les points de rencontre optimaux pour le groupe
     */
    suspend fun findGroupMeetingPoints(
        routes: Map<String, Route>,
        maxWaitTimeMinutes: Int = 60
    ): List<String> {
        if (routes.isEmpty()) return emptyList()

        // Pour simplifier, retourne les heures d'arrivée triées
        val arrivalTimes = routes.values
            .flatMap { route -> route.segments.map { segment -> segment.arrivalTime } }
            .sorted()

        // Groupe les arrivées proches dans le temps
        val meetingPoints = mutableListOf<String>()
        var currentGroup = mutableListOf<String>()

        arrivalTimes.forEach { time ->
            if (currentGroup.isEmpty()) {
                currentGroup.add(time)
            } else {
                val lastTime = Instant.parse(currentGroup.last())
                val currentTime = Instant.parse(time)
                val diffMinutes = abs((currentTime - lastTime).inWholeMinutes)

                if (diffMinutes <= maxWaitTimeMinutes) {
                    currentGroup.add(time)
                } else {
                    // Ajoute le point de rencontre (heure moyenne du groupe)
                    meetingPoints.add(calculateGroupMeetingTime(currentGroup))
                    currentGroup = mutableListOf(time)
                }
            }
        }

        if (currentGroup.isNotEmpty()) {
            meetingPoints.add(calculateGroupMeetingTime(currentGroup))
        }

        return meetingPoints
    }

    // Méthodes privées d'aide

    private fun calculateBalancedScore(option: TransportOption): Double {
        // Score équilibré: 40% coût, 60% temps
        val normalizedCost = option.cost / 1000.0 // Suppose coût max 1000€
        val normalizedTime = option.durationMinutes / 1440.0 // 24h en minutes
        return 0.4 * normalizedCost + 0.6 * normalizedTime
    }

    private fun calculateRouteScore(option: TransportOption, type: OptimizationType): Double {
        return when (type) {
            OptimizationType.COST_MINIMIZE -> option.cost
            OptimizationType.TIME_MINIMIZE -> option.durationMinutes.toDouble()
            OptimizationType.BALANCED -> calculateBalancedScore(option)
        }
    }

    private fun calculateGroupMeetingTime(times: List<String>): String {
        val instants = times.map { Instant.parse(it) }
        val averageEpoch = instants.sumOf { it.toEpochMilliseconds() } / instants.size
        return Instant.fromEpochMilliseconds(averageEpoch).toString()
    }

    // Génération d'options mockées pour chaque mode de transport

    private fun generateFlightOptions(
        from: TransportLocation,
        to: TransportLocation,
        departureTime: Instant,
        options: MutableList<TransportOption>
    ) {
        // Simule quelques vols avec escale ou direct
        val airlines = listOf("Air France", "Ryanair", "EasyJet", "Lufthansa")

        repeat(3) {
            val airline = airlines.random()
            val hasStopover = Random.nextBoolean()
            val baseDuration = 120 + Random.nextInt(240) // 2-6h
            val duration = if (hasStopover) baseDuration + 60 else baseDuration
            val stops = if (hasStopover) listOf(TransportLocation("CDG", "Paris Charles de Gaulle")) else emptyList()

            val cost = when {
                airline == "Ryanair" -> 50.0 + Random.nextDouble(100.0)
                airline == "EasyJet" -> 80.0 + Random.nextDouble(120.0)
                else -> 150.0 + Random.nextDouble(300.0)
            }

            options.add(TransportOption(
                id = "flight-${Random.nextInt()}",
                mode = TransportMode.FLIGHT,
                provider = airline,
                departure = from,
                arrival = to,
                departureTime = departureTime.toString(),
                arrivalTime = departureTime.plus(duration, DateTimeUnit.MINUTE).toString(),
                durationMinutes = duration,
                cost = cost,
                stops = stops,
                bookingUrl = "https://$airline.com/booking"
            ))
        }
    }

    private fun generateTrainOptions(
        from: TransportLocation,
        to: TransportLocation,
        departureTime: Instant,
        options: MutableList<TransportOption>
    ) {
        val trains = listOf("TGV", "Eurostar", "Thalys", "SNCF")

        repeat(2) {
            val train = trains.random()
            val duration = 180 + Random.nextInt(300) // 3-8h
            val cost = 45.0 + Random.nextDouble(155.0)

            options.add(TransportOption(
                id = "train-${Random.nextInt()}",
                mode = TransportMode.TRAIN,
                provider = train,
                departure = from,
                arrival = to,
                departureTime = departureTime.toString(),
                arrivalTime = departureTime.plus(duration, DateTimeUnit.MINUTE).toString(),
                durationMinutes = duration,
                cost = cost,
                stops = emptyList(),
                bookingUrl = "https://sncf.com/booking"
            ))
        }
    }

    private fun generateBusOptions(
        from: TransportLocation,
        to: TransportLocation,
        departureTime: Instant,
        options: MutableList<TransportOption>
    ) {
        val buses = listOf("FlixBus", "Eurolines", "BlaBlaBus")

        repeat(2) {
            val bus = buses.random()
            val duration = 300 + Random.nextInt(600) // 5-15h
            val cost = 20.0 + Random.nextDouble(80.0)

            options.add(TransportOption(
                id = "bus-${Random.nextInt()}",
                mode = TransportMode.BUS,
                provider = bus,
                departure = from,
                arrival = to,
                departureTime = departureTime.toString(),
                arrivalTime = departureTime.plus(duration, DateTimeUnit.MINUTE).toString(),
                durationMinutes = duration,
                cost = cost,
                stops = emptyList(),
                bookingUrl = "https://$bus.com/booking"
            ))
        }
    }

    private fun generateCarOptions(
        from: TransportLocation,
        to: TransportLocation,
        departureTime: Instant,
        options: MutableList<TransportOption>
    ) {
        val duration = 240 + Random.nextInt(480) // 4-12h
        val cost = 30.0 + Random.nextDouble(100.0) // Carburant + péages

        options.add(TransportOption(
            id = "car-${Random.nextInt()}",
            mode = TransportMode.CAR,
            provider = "Personal Car",
            departure = from,
            arrival = to,
            departureTime = departureTime.toString(),
            arrivalTime = departureTime.plus(duration, DateTimeUnit.MINUTE).toString(),
            durationMinutes = duration,
            cost = cost,
            stops = emptyList()
        ))
    }

    private fun generateRideshareOptions(
        from: TransportLocation,
        to: TransportLocation,
        departureTime: Instant,
        options: MutableList<TransportOption>
    ) {
        val services = listOf("BlaBlaCar", "Uber", "Lyft")

        repeat(2) {
            val service = services.random()
            val duration = 180 + Random.nextInt(300) // 3-8h
            val cost = 25.0 + Random.nextDouble(75.0)

            options.add(TransportOption(
                id = "rideshare-${Random.nextInt()}",
                mode = TransportMode.RIDESHARE,
                provider = service,
                departure = from,
                arrival = to,
                departureTime = departureTime.toString(),
                arrivalTime = departureTime.plus(duration, DateTimeUnit.MINUTE).toString(),
                durationMinutes = duration,
                cost = cost,
                stops = emptyList(),
                bookingUrl = "https://$service.com/booking"
            ))
        }
    }

    private fun generateTaxiOptions(
        from: TransportLocation,
        to: TransportLocation,
        departureTime: Instant,
        options: MutableList<TransportOption>
    ) {
        val duration = 60 + Random.nextInt(180) // 1-4h
        val cost = 50.0 + Random.nextDouble(200.0)

        options.add(TransportOption(
            id = "taxi-${Random.nextInt()}",
            mode = TransportMode.TAXI,
            provider = "Local Taxi",
            departure = from,
            arrival = to,
            departureTime = departureTime.toString(),
            arrivalTime = departureTime.plus(duration, DateTimeUnit.MINUTE).toString(),
            durationMinutes = duration,
            cost = cost,
            stops = emptyList()
        ))
    }

    private fun generateWalkingOptions(
        from: TransportLocation,
        to: TransportLocation,
        departureTime: Instant,
        options: MutableList<TransportOption>
    ) {
        // Marche seulement si très proche
        if (from.name == to.name) {
            options.add(TransportOption(
                id = "walking-${Random.nextInt()}",
                mode = TransportMode.WALKING,
                provider = "Walking",
                departure = from,
                arrival = to,
                departureTime = departureTime.toString(),
                arrivalTime = departureTime.plus(30, DateTimeUnit.MINUTE).toString(),
                durationMinutes = 30,
                cost = 0.0,
                stops = emptyList()
            ))
        }
    }
}