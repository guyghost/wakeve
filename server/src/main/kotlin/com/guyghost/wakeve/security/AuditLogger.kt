package com.guyghost.wakeve.security

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

/**
 * Structured audit logger for security events.
 * All events are logged in JSON format for log aggregation and analysis.
 */
class AuditLogger {
    private val logger = LoggerFactory.getLogger("AuditLogger")
    private val json = Json { prettyPrint = false }

    /**
     * Log an authentication failure event.
     * @param userId Optional user identifier (null if unknown)
     * @param reason Description of why authentication failed
     */
    fun logAuthenticationFailure(userId: String?, reason: String) {
        val event = AuditEvent(
            timestamp = Clock.System.now(),
            userId = userId,
            eventType = AuditEventType.AUTHENTICATION_FAILURE,
            details = mapOf(
                "reason" to reason,
                "sourceIp" to (ThreadLocal<String>().get() ?: "unknown")
            )
        )
        logEvent(event)
    }

    /**
     * Log an authorization failure event.
     * @param userId User identifier
     * @param resource The resource being accessed
     * @param action The action being attempted
     */
    fun logAuthorizationFailure(userId: String, resource: String, action: String) {
        val event = AuditEvent(
            timestamp = Clock.System.now(),
            userId = userId,
            eventType = AuditEventType.AUTHORIZATION_FAILURE,
            details = mapOf(
                "resource" to resource,
                "action" to action,
                "sourceIp" to (ThreadLocal<String>().get() ?: "unknown")
            )
        )
        logEvent(event)
    }

    /**
     * Log a sensitive operation event.
     * @param userId User performing the operation
     * @param operation Type of sensitive operation
     * @param details Additional context about the operation
     */
    fun logSensitiveOperation(userId: String, operation: String, details: Map<String, String>) {
        val event = AuditEvent(
            timestamp = Clock.System.now(),
            userId = userId,
            eventType = AuditEventType.SENSITIVE_OPERATION,
            details = details + mapOf(
                "operation" to operation,
                "sourceIp" to (ThreadLocal<String>().get() ?: "unknown")
            )
        )
        logEvent(event)
    }

    /**
     * Log a data access event for compliance tracking.
     * @param userId User accessing the data
     * @param resourceType Type of resource accessed
     * @param resourceId Identifier of the resource
     * @param scope Read/Write/Delete
     */
    fun logDataAccess(userId: String, resourceType: String, resourceId: String, scope: DataAccessScope) {
        val event = AuditEvent(
            timestamp = Clock.System.now(),
            userId = userId,
            eventType = AuditEventType.DATA_ACCESS,
            details = mapOf(
                "resourceType" to resourceType,
                "resourceId" to resourceId,
                "scope" to scope.name.lowercase(),
                "sourceIp" to (ThreadLocal<String>().get() ?: "unknown")
            )
        )
        logEvent(event)
    }

    /**
     * Log a configuration change event.
     * @param userId User making the change
     * @param configKey Configuration key changed
     * @param oldValue Previous value (optional)
     * @param newValue New value (optional)
     */
    fun logConfigurationChange(userId: String, configKey: String, oldValue: String?, newValue: String?) {
        val event = AuditEvent(
            timestamp = Clock.System.now(),
            userId = userId,
            eventType = AuditEventType.CONFIGURATION_CHANGE,
            details = buildMap {
                put("configKey", configKey)
                oldValue?.let { put("oldValue", it) }
                newValue?.let { put("newValue", it) }
                put("sourceIp", ThreadLocal<String>().get() ?: "unknown")
            }
        )
        logEvent(event)
    }

    private fun logEvent(event: AuditEvent) {
        val logEntry = json.encodeToString(event)
        when (event.eventType) {
            AuditEventType.AUTHENTICATION_FAILURE,
            AuditEventType.AUTHORIZATION_FAILURE -> logger.warn(logEntry)
            AuditEventType.SENSITIVE_OPERATION,
            AuditEventType.CONFIGURATION_CHANGE -> logger.info(logEntry)
            AuditEventType.DATA_ACCESS -> logger.debug(logEntry)
        }
    }
}

/**
 * Types of audit events for categorization.
 */
@Serializable
enum class AuditEventType {
    AUTHENTICATION_FAILURE,
    AUTHORIZATION_FAILURE,
    SENSITIVE_OPERATION,
    DATA_ACCESS,
    CONFIGURATION_CHANGE
}

/**
 * Scope of data access operations.
 */
enum class DataAccessScope {
    READ,
    WRITE,
    DELETE
}

/**
 * Structured audit event for JSON serialization.
 */
@Serializable
data class AuditEvent(
    val timestamp: Instant,
    val userId: String?,
    val eventType: AuditEventType,
    val details: Map<String, String>
)
