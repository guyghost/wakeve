package com.guyghost.wakeve.notification

import java.sql.Connection
import java.sql.SQLException
import java.util.UUID

/**
 * Backend ingestion owns one SQLite transaction for the domain event, its logical notification,
 * recipients and all already-resolved deliveries. Provider I/O is deliberately outside it.
 */
class BackendNotificationIngestionService(
    private val storeFactory: BackendNotificationDeliveryRuntimeFactory,
    private val faultInjector: BackendNotificationIngestionFaultInjector,
    private val committedPort: BackendNotificationIngestionCommittedPort
) {
    suspend fun ingest(command: BackendNotificationIngestionCommand): BackendNotificationIngestionReceipt {
        validate(command)
        val sqliteFactory = storeFactory as? SqliteBackendNotificationDeliveryStoreFactory
            ?: error("Backend notification ingestion requires the durable SQLite runtime")
        storeFactory.open().close()
        val databasePath = sqliteFactory.preparedDatabasePath()
        val result = synchronized(backendNotificationRuntimeLock) {
            openDeviceRegistrationJdbcConnection(databasePath, DeviceRegistrationJdbcOpenTestHook { }).use { connection ->
                connection.createStatement().use {
                    it.execute("PRAGMA foreign_keys = ON")
                    it.execute("PRAGMA busy_timeout = 5000")
                }
                ensureBackendNotificationRuntimeSchema(connection)
                persistTransaction(connection, command)
            }
        }
        if (result.created) committedPort.committed(result)
        return result
    }

    private fun persistTransaction(
        connection: Connection,
        command: BackendNotificationIngestionCommand
    ): BackendNotificationIngestionReceipt {
        val effectKey = canonicalEffectKey(command)
        val initialDispatchStatus = if (command.recipients.all { it.registrationIds.isEmpty() }) {
            BackendEffectDispatchStatus.PENDING_RECIPIENT
        } else {
            BackendEffectDispatchStatus.QUEUED
        }
        connection.createStatement().use { it.execute("BEGIN IMMEDIATE") }
        try {
            existingReceipt(connection, command, effectKey)?.also {
                connection.createStatement().use { statement -> statement.execute("COMMIT") }
                return it
            }
            val transactionId = "ingestion:${UUID.randomUUID()}"
            connection.prepareStatement(
                """
                INSERT INTO domain_event_ingestion(
                    domain_event_id, effect_type, schema_version, transaction_id,
                    effect_key, logical_notification_id, identity_version
                ) VALUES (?, ?, ?, ?, ?, ?, 'CANONICAL_V2')
                """.trimIndent()
            ).use { statement ->
                statement.setString(1, command.domainEventId)
                statement.setString(2, command.effectType)
                statement.setInt(3, command.schemaVersion)
                statement.setString(4, transactionId)
                statement.setString(5, effectKey.value)
                statement.setString(6, command.logicalNotificationId)
                statement.executeUpdate()
            }
            faultInjector.inject(BackendNotificationIngestionCheckpoint.AFTER_DOMAIN_EVENT_INGESTION_WRITE)

            connection.prepareStatement(
                """
                INSERT INTO notification_logical(
                    logical_notification_id, effect_key, decision_sync_status, effect_dispatch_status
                    , identity_version
                ) VALUES (?, ?, ?, ?, 'CANONICAL_V2')
                """.trimIndent()
            ).use { statement ->
                statement.setString(1, command.logicalNotificationId)
                statement.setString(2, effectKey.value)
                statement.setString(3, BackendDecisionSyncStatus.ACKNOWLEDGED.name)
                statement.setString(4, initialDispatchStatus.name)
                statement.executeUpdate()
            }
            faultInjector.inject(BackendNotificationIngestionCheckpoint.AFTER_LOGICAL_NOTIFICATION_WRITE)

            val recipientEntries = command.recipients.map { intent ->
                val recipientKey = BackendCanonicalNotificationIdentity.recipientKey(
                    effectKey, intent.participantId, intent.channel
                )
                val registrationIds = intent.registrationIds.distinct().sorted()
                connection.prepareStatement(
                    """
                    INSERT INTO notification_recipient(
                        recipient_key, effect_key, status, expires_at_epoch_seconds, identity_version
                    ) VALUES (?, ?, ?, ?, 'CANONICAL_V2')
                    """.trimIndent()
                ).use { statement ->
                    statement.setString(1, recipientKey.value)
                    statement.setString(2, effectKey.value)
                    statement.setString(
                        3,
                        if (registrationIds.isEmpty()) BackendRecipientStatus.PENDING_TARGET.name
                        else BackendRecipientStatus.TARGETED.name
                    )
                    statement.setLong(4, intent.expiresAtEpochSeconds)
                    statement.executeUpdate()
                }
                recipientKey to (intent to registrationIds)
            }
            faultInjector.inject(BackendNotificationIngestionCheckpoint.AFTER_RECIPIENTS_WRITE)

            val deliveryKeys = linkedSetOf<DeliveryKey>()
            recipientEntries.forEach { (recipientKey, pair) ->
                val (intent, registrationIds) = pair
                registrationIds.forEach { registrationId ->
                    connection.prepareStatement(
                        "INSERT INTO notification_recipient_registration(recipient_key, device_registration_id) VALUES (?, ?)"
                    ).use { statement ->
                        statement.setString(1, recipientKey.value)
                        statement.setString(2, registrationId)
                        statement.executeUpdate()
                    }
                    val deliveryKey = BackendCanonicalNotificationIdentity.deliveryKey(
                        recipientKey, registrationId, intent.provider
                    )
                    deliveryKeys += deliveryKey
                    connection.prepareStatement(
                        """
                        INSERT INTO notification_delivery(
                            delivery_key, logical_notification_id, idempotency_key, recipient_key,
                            device_registration_id, provider, status, attempt, expires_at_epoch_seconds,
                            identity_version
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, 0, ?, 'CANONICAL_V2')
                        """.trimIndent()
                    ).use { statement ->
                        statement.setString(1, deliveryKey.value)
                        statement.setString(2, command.logicalNotificationId)
                        statement.setString(3, deliveryKey.value)
                        statement.setString(4, recipientKey.value)
                        statement.setString(5, registrationId)
                        statement.setString(6, intent.provider)
                        statement.setString(7, BackendDeliveryStatus.POLICY_CHECK.name)
                        statement.setLong(8, intent.expiresAtEpochSeconds)
                        statement.executeUpdate()
                    }
                    connection.prepareStatement(
                        "INSERT INTO notification_delivery_authority(delivery_key, authority, fencing_token) VALUES (?, ?, 1)"
                    ).use { statement ->
                        statement.setString(1, deliveryKey.value)
                        statement.setString(2, BackendDeliveryAuthority.OUTBOX_V2.wireValue)
                        statement.executeUpdate()
                    }
                }
            }
            faultInjector.inject(BackendNotificationIngestionCheckpoint.AFTER_DELIVERIES_WRITE)
            connection.createStatement().use { it.execute("COMMIT") }
            return BackendNotificationIngestionReceipt(
                transactionId = transactionId,
                effectKey = effectKey,
                deliveryKeys = deliveryKeys,
                created = true,
                decisionSyncStatus = BackendDecisionSyncStatus.ACKNOWLEDGED,
                effectDispatchStatus = initialDispatchStatus
            )
        } catch (failure: Throwable) {
            runCatching { connection.createStatement().use { it.execute("ROLLBACK") } }
                .onFailure(failure::addSuppressed)
            if (failure is SQLException && isUniqueConflict(failure)) {
                existingReceipt(connection, command, effectKey)?.let { return it }
            }
            throw failure
        }
    }

    private fun existingReceipt(
        connection: Connection,
        command: BackendNotificationIngestionCommand,
        expectedEffectKey: EffectKey
    ): BackendNotificationIngestionReceipt? = connection.prepareStatement(
        """
        SELECT ingestion.transaction_id, ingestion.effect_key, ingestion.logical_notification_id,
               logical.effect_dispatch_status
        FROM domain_event_ingestion ingestion
        JOIN notification_logical logical
          ON logical.logical_notification_id = ingestion.logical_notification_id
        WHERE domain_event_id = ? AND effect_type = ? AND schema_version = ?
        """.trimIndent()
    ).use { statement ->
        statement.setString(1, command.domainEventId)
        statement.setString(2, command.effectType)
        statement.setInt(3, command.schemaVersion)
        statement.executeQuery().use { rows ->
            if (!rows.next()) return@use null
            check(rows.getString("effect_key") == expectedEffectKey.value &&
                rows.getString("logical_notification_id") == command.logicalNotificationId) {
                "Domain event ingestion identity conflicts with its durable record"
            }
            checkDurableRecipientIdentity(connection, command, expectedEffectKey)
            BackendNotificationIngestionReceipt(
                transactionId = rows.getString("transaction_id"),
                effectKey = expectedEffectKey,
                deliveryKeys = connection.prepareStatement(
                    "SELECT delivery_key FROM notification_delivery WHERE logical_notification_id = ? ORDER BY delivery_key"
                ).use { deliveries ->
                    deliveries.setString(1, command.logicalNotificationId)
                    deliveries.executeQuery().use { deliveryRows ->
                        buildSet { while (deliveryRows.next()) add(DeliveryKey(deliveryRows.getString(1))) }
                    }
                },
                created = false,
                decisionSyncStatus = BackendDecisionSyncStatus.ACKNOWLEDGED,
                effectDispatchStatus = BackendEffectDispatchStatus.valueOf(
                    rows.getString("effect_dispatch_status")
                )
            )
        }
    }

    private fun checkDurableRecipientIdentity(
        connection: Connection,
        command: BackendNotificationIngestionCommand,
        effectKey: EffectKey
    ) {
        val expected = command.recipients.associateBy { intent ->
            BackendCanonicalNotificationIdentity.recipientKey(
                effectKey, intent.participantId, intent.channel
            ).value
        }
        check(expected.size == command.recipients.size) {
            "Domain event ingestion contains duplicate recipient identities"
        }
        val durableRecipients = connection.prepareStatement(
            "SELECT recipient_key, expires_at_epoch_seconds FROM notification_recipient WHERE effect_key = ?"
        ).use { statement ->
            statement.setString(1, effectKey.value)
            statement.executeQuery().use { rows ->
                buildMap {
                    while (rows.next()) put(rows.getString(1), rows.getLong(2))
                }
            }
        }
        check(durableRecipients.keys == expected.keys && expected.all { (recipientKey, intent) ->
            durableRecipients[recipientKey] == intent.expiresAtEpochSeconds
        }) {
            "Domain event ingestion recipient identity conflicts with its durable record"
        }
        expected.forEach { (recipientKeyValue, intent) ->
            val expectedRegistrationIds = intent.registrationIds.toSet()
            if (expectedRegistrationIds.isEmpty()) return@forEach
            val durableRegistrationIds = connection.prepareStatement(
                "SELECT device_registration_id FROM notification_recipient_registration WHERE recipient_key = ?"
            ).use { statement ->
                statement.setString(1, recipientKeyValue)
                statement.executeQuery().use { rows ->
                    buildSet { while (rows.next()) add(rows.getString(1)) }
                }
            }
            check(durableRegistrationIds == expectedRegistrationIds) {
                "Domain event ingestion registration identity conflicts with its durable record"
            }
            val recipientKey = RecipientKey(recipientKeyValue)
            expectedRegistrationIds.forEach { registrationId ->
                val deliveryKey = BackendCanonicalNotificationIdentity.deliveryKey(
                    recipientKey, registrationId, intent.provider
                )
                val exactDelivery = connection.prepareStatement(
                    """
                    SELECT 1 FROM notification_delivery
                    WHERE delivery_key = ? AND logical_notification_id = ? AND idempotency_key = ?
                      AND recipient_key = ? AND device_registration_id = ? AND provider = ?
                    """.trimIndent()
                ).use { statement ->
                    statement.setString(1, deliveryKey.value)
                    statement.setString(2, command.logicalNotificationId)
                    statement.setString(3, deliveryKey.value)
                    statement.setString(4, recipientKeyValue)
                    statement.setString(5, registrationId)
                    statement.setString(6, intent.provider)
                    statement.executeQuery().use { it.next() }
                }
                check(exactDelivery) {
                    "Domain event ingestion delivery identity conflicts with its durable record"
                }
            }
        }
    }

    private fun validate(command: BackendNotificationIngestionCommand) {
        require(command.domainEventId.isNotBlank()) { "domainEventId is required" }
        require(command.effectType == "DATE_CONFIRMED") { "Unsupported notification effect type" }
        require(command.schemaVersion > 0) { "schemaVersion must be positive" }
        require(command.logicalNotificationId.isNotEmpty() &&
            command.logicalNotificationId.trim() == command.logicalNotificationId) {
            "logicalNotificationId must be a non-empty canonical identity component"
        }
        require(command.recipients.isNotEmpty()) { "At least one recipient is required" }
        command.recipients.forEach { recipient ->
            require(recipient.participantId.isNotBlank()) { "participantId is required" }
            require(recipient.channel == "push") { "Unsupported notification channel" }
            require(recipient.provider == "apns") { "Unsupported notification provider" }
            require(recipient.expiresAtEpochSeconds > 0) { "Recipient expiry must be positive" }
            recipient.registrationIds.forEach { registrationId ->
                require(registrationId.isNotEmpty() && registrationId.trim() == registrationId) {
                    "registrationId must be a canonical identity component"
                }
            }
        }
    }

    private fun canonicalEffectKey(command: BackendNotificationIngestionCommand): EffectKey =
        BackendCanonicalNotificationIdentity.effectKey(
            command.domainEventId, command.effectType, command.schemaVersion
        )

    private fun isUniqueConflict(failure: SQLException): Boolean =
        failure.message?.contains("UNIQUE constraint failed", ignoreCase = true) == true
}
