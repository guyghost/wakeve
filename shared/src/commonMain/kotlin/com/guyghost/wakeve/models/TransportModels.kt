package com.guyghost.wakeve.models

import kotlinx.serialization.Serializable

@Serializable
data class Location(
    val name: String,
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val iataCode: String? = null // For airports
)

@Serializable
enum class TransportMode {
    FLIGHT, TRAIN, BUS, CAR, RIDESHARE, TAXI, WALKING
}

@Serializable
data class TransportOption(
    val id: String,
    val mode: TransportMode,
    val provider: String, // e.g., "Air France", "SNCF", "Uber"
    val departure: Location,
    val arrival: Location,
    val departureTime: String, // ISO 8601
    val arrivalTime: String, // ISO 8601
    val durationMinutes: Int,
    val cost: Double, // in EUR
    val currency: String = "EUR",
    val stops: List<Location> = emptyList(),
    val bookingUrl: String? = null
)

@Serializable
data class Route(
    val id: String,
    val segments: List<TransportOption>,
    val totalDurationMinutes: Int,
    val totalCost: Double,
    val currency: String = "EUR",
    val score: Double // Optimization score
)

@Serializable
data class TransportPlan(
    val eventId: String,
    val participantRoutes: Map<String, Route>, // participantId -> Route
    val groupArrivals: List<String>, // ISO times when group arrives
    val totalGroupCost: Double,
    val optimizationType: OptimizationType,
    val createdAt: String // ISO 8601
)

@Serializable
enum class OptimizationType {
    COST_MINIMIZE, TIME_MINIMIZE, BALANCED
}

interface TransportService {
    suspend fun getTransportOptions(
        from: Location,
        to: Location,
        departureTime: String,
        mode: TransportMode? = null
    ): List<TransportOption>

    suspend fun optimizeRoutes(
        participants: Map<String, Location>, // participantId -> home location
        destination: Location,
        eventTime: String, // ISO 8601
        optimizationType: OptimizationType = OptimizationType.BALANCED
    ): TransportPlan

    suspend fun findGroupMeetingPoints(
        routes: Map<String, Route>,
        maxWaitTimeMinutes: Int = 60
    ): List<String> // ISO times
}