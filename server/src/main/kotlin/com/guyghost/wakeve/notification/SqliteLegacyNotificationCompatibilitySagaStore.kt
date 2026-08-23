package com.guyghost.wakeve.notification

import java.nio.charset.StandardCharsets
import java.nio.ByteBuffer
import java.nio.file.Path
import java.security.MessageDigest
import java.security.SecureRandom
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Types
import java.util.Base64
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.math.max

internal class SqliteLegacyNotificationCompatibilitySagaStore(
    private val databasePath: Path,
    tokenEncryptionKey: ByteArray,
    private val jdbcOpenTestHook: DeviceRegistrationJdbcOpenTestHook,
    private val leaseClaimProbe: LegacyCompatibilityLeaseClaimProbe =
        NoOpLegacyCompatibilityLeaseClaimProbe
) : LegacyNotificationCompatibilitySagaStore, LegacyCompatibilityTokenCustodian {
    private val tokenCipher = LegacyCompatibilityTokenCipher(tokenEncryptionKey)
    private val closed = AtomicBoolean(false)

    init {
        synchronized(LEGACY_COMPATIBILITY_STORE_LOCK) {
            connection().use(::createSchema)
        }
    }

    override suspend fun persistIntent(
        command: LegacyCompatibilityCommand,
        rawToken: String?
    ): LegacyCompatibilitySnapshot = immediateTransaction { connection ->
            findRecord(connection, command.sagaId)?.let { existing ->
                require(existing.snapshot.requestKey == command.requestKey) {
                    "A compatibility saga ID cannot identify a different request"
                }
                return@immediateTransaction existing.snapshot
            }
            findRecordByRequestKey(connection, command.requestKey)?.let { existing ->
                return@immediateTransaction existing.snapshot
            }
            if (rawToken != null) {
                require(command.operation == LegacyCompatibilityOperation.REGISTER) {
                    "Only registration may persist an encrypted token payload"
                }
                require(compatibilityTokenFingerprint(rawToken) == command.tokenFingerprint) {
                    "The registration token fingerprint does not match the command"
                }
            }

            val state = when {
                command.clientGeneration == LegacyCompatibilityClientGeneration.N ->
                    LegacyCompatibilitySagaState.WRITING_V2
                command.operation == LegacyCompatibilityOperation.REGISTER ->
                    LegacyCompatibilitySagaState.WRITING_LEGACY
                else -> LegacyCompatibilitySagaState.WRITING_V2
            }
            val checkpoint = state.effectCheckpoint()
            val reference = effectReference(
                requestKey = command.requestKey,
                checkpoint = checkpoint,
                checkpointRevision = 1,
                fencingToken = 1
            )
            val snapshot = LegacyCompatibilitySnapshot(
                sagaId = command.sagaId,
                requestKey = command.requestKey,
                operation = command.operation,
                clientGeneration = command.clientGeneration,
                authenticatedUserId = command.authenticatedUserId,
                platform = command.platform,
                legacyPrimaryKeyFingerprint = command.legacyPrimaryKeyFingerprint,
                legacyInstallationId = command.legacyInstallationId,
                legacyRegistrationId = command.legacyRegistrationId,
                targetInstallationId = command.targetInstallationId,
                targetRegistrationId = command.targetRegistrationId,
                tokenFingerprint = command.tokenFingerprint,
                compatibilityGeneration = command.compatibilityGeneration,
                maxAttemptsPerStore = command.maxAttemptsPerStore,
                scope = command.scope,
                state = state,
                reconciliationStatus = LegacyCompatibilityReconciliationStatus.PENDING,
                responseDisposition = LegacyCompatibilityResponseDisposition.RECONCILIATION_ACCEPTED,
                legacyWriteStatus = if (
                    command.clientGeneration == LegacyCompatibilityClientGeneration.N
                ) LegacyCompatibilityWriteStatus.NOT_REQUIRED else LegacyCompatibilityWriteStatus.PENDING,
                v2WriteStatus = LegacyCompatibilityWriteStatus.PENDING,
                v2TargetKind = if (
                    command.clientGeneration == LegacyCompatibilityClientGeneration.N_MINUS_1
                ) {
                    LegacyCompatibilityV2TargetKind.LEGACY_DETERMINISTIC_INSTALLATION_ONLY
                } else {
                    LegacyCompatibilityV2TargetKind.EXACT_REGISTRATION_OR_INSTALLATION
                },
                legacyAttempt = 0,
                v2Attempt = 0,
                nextRetryAtEpochSeconds = null,
                checkpointRevision = 1,
                logicalNowEpochSeconds = command.initialNowEpochSeconds,
                clockRevision = 0,
                requiredEffect = reference,
                recoveryLease = null,
                lastFailure = null
            )
            val ciphertext = rawToken?.let { tokenCipher.encrypt(command.sagaId, it) }
            try {
                insertRecord(connection, snapshot, resumeCheckpoint = null, ciphertext = ciphertext)
            } finally {
                ciphertext?.fill(0)
            }
            insertEffectHistory(connection, command.sagaId, reference)
            snapshot
    }

    override suspend fun findBySagaId(sagaId: String): LegacyCompatibilitySnapshot? =
        synchronized(LEGACY_COMPATIBILITY_STORE_LOCK) {
            read { connection -> findRecord(connection, sagaId.trim())?.snapshot }
        }

    override suspend fun acknowledgeWriteSucceeded(
        sagaId: String,
        reference: LegacyCompatibilityEffectReference,
        outcome: LegacyCompatibilityWriteOutcome
    ): LegacyCompatibilitySnapshot = mutate(sagaId) { connection, record ->
        val current = record.snapshot
        if (!current.accepts(reference) || !reference.checkpoint.isWriteCheckpoint()) {
            return@mutate record
        }
        @Suppress("UNUSED_VARIABLE")
        val durableOutcome = outcome
        val updated = when (reference.checkpoint) {
            LegacyCompatibilityEffectCheckpoint.WRITE_LEGACY_STORE -> {
                val applied = current.copy(legacyWriteStatus = LegacyCompatibilityWriteStatus.APPLIED)
                if (
                    current.clientGeneration == LegacyCompatibilityClientGeneration.N_MINUS_1 &&
                    current.operation == LegacyCompatibilityOperation.REGISTER
                ) {
                    applied.withEffect(LegacyCompatibilitySagaState.WRITING_V2)
                } else {
                    applied.withEffect(LegacyCompatibilitySagaState.RECORDING_CONVERGENCE)
                }
            }
            LegacyCompatibilityEffectCheckpoint.WRITE_V2_REGISTRATION_STORE -> {
                val applied = current.copy(v2WriteStatus = LegacyCompatibilityWriteStatus.APPLIED)
                if (
                    current.clientGeneration == LegacyCompatibilityClientGeneration.N_MINUS_1 &&
                    current.operation == LegacyCompatibilityOperation.UNREGISTER
                ) {
                    applied.withEffect(LegacyCompatibilitySagaState.WRITING_LEGACY)
                } else {
                    applied.withEffect(LegacyCompatibilitySagaState.RECORDING_CONVERGENCE)
                }
            }
            else -> current
        }
        persistTransition(connection, record, updated, resumeCheckpoint = null)
    }

    override suspend fun acknowledgeWriteFailed(
        sagaId: String,
        reference: LegacyCompatibilityEffectReference,
        failure: LegacyCompatibilityFailure
    ): LegacyCompatibilitySnapshot = mutate(sagaId) { connection, record ->
        val current = record.snapshot
        if (!current.accepts(reference) || !reference.checkpoint.isWriteCheckpoint()) {
            return@mutate record
        }
        val nextLegacyAttempt = current.legacyAttempt +
            if (reference.checkpoint == LegacyCompatibilityEffectCheckpoint.WRITE_LEGACY_STORE) 1 else 0
        val nextV2Attempt = current.v2Attempt +
            if (reference.checkpoint == LegacyCompatibilityEffectCheckpoint.WRITE_V2_REGISTRATION_STORE) 1 else 0
        val failedAttempt = if (
            reference.checkpoint == LegacyCompatibilityEffectCheckpoint.WRITE_LEGACY_STORE
        ) nextLegacyAttempt else nextV2Attempt
        val afterAttempt = current.copy(
            legacyAttempt = nextLegacyAttempt,
            v2Attempt = nextV2Attempt,
            lastFailure = failure
        )
        val updated = if (failure.retryable && failedAttempt < current.maxAttemptsPerStore) {
            afterAttempt.withEffect(LegacyCompatibilitySagaState.RECORDING_RETRY)
        } else {
            afterAttempt.withEffect(LegacyCompatibilitySagaState.RECORDING_BLOCK)
        }
        persistTransition(
            connection = connection,
            previous = record,
            snapshot = updated,
            resumeCheckpoint = if (updated.state == LegacyCompatibilitySagaState.RECORDING_RETRY) {
                reference.checkpoint
            } else {
                null
            }
        )
    }

    override suspend fun recordRetry(
        sagaId: String,
        reference: LegacyCompatibilityEffectReference,
        nextRetryAtEpochSeconds: Long
    ): LegacyCompatibilitySnapshot = mutate(sagaId) { connection, record ->
        val current = record.snapshot
        if (
            !current.accepts(reference) ||
            reference.checkpoint != LegacyCompatibilityEffectCheckpoint.PERSIST_RETRY_SCHEDULE ||
            nextRetryAtEpochSeconds <= current.logicalNowEpochSeconds ||
            record.resumeCheckpoint == null
        ) {
            return@mutate record
        }
        val updated = current.copy(
            state = LegacyCompatibilitySagaState.RETRY_WAIT,
            nextRetryAtEpochSeconds = nextRetryAtEpochSeconds,
            requiredEffect = null,
            recoveryLease = null
        )
        persistTransition(connection, record, updated, record.resumeCheckpoint)
    }

    override suspend fun retryDue(
        sagaId: String,
        checkpointRevision: Long
    ): LegacyCompatibilitySnapshot = mutate(sagaId) { connection, record ->
        val current = record.snapshot
        val deadline = current.nextRetryAtEpochSeconds
        val resumeCheckpoint = record.resumeCheckpoint
        if (
            current.state != LegacyCompatibilitySagaState.RETRY_WAIT ||
            current.checkpointRevision != checkpointRevision ||
            deadline == null ||
            current.logicalNowEpochSeconds < deadline ||
            resumeCheckpoint == null
        ) {
            return@mutate record
        }
        val nextState = when (resumeCheckpoint) {
            LegacyCompatibilityEffectCheckpoint.WRITE_LEGACY_STORE ->
                LegacyCompatibilitySagaState.WRITING_LEGACY
            LegacyCompatibilityEffectCheckpoint.WRITE_V2_REGISTRATION_STORE ->
                LegacyCompatibilitySagaState.WRITING_V2
            else -> return@mutate record
        }
        val updated = current.copy(nextRetryAtEpochSeconds = null).withEffect(nextState)
        persistTransition(connection, record, updated, resumeCheckpoint = null)
    }

    override suspend fun recordConvergence(
        sagaId: String,
        reference: LegacyCompatibilityEffectReference
    ): LegacyCompatibilitySnapshot = mutate(sagaId) { connection, record ->
        val current = record.snapshot
        if (
            !current.accepts(reference) ||
            reference.checkpoint != LegacyCompatibilityEffectCheckpoint.PERSIST_CONVERGENCE ||
            current.legacyWriteStatus == LegacyCompatibilityWriteStatus.PENDING ||
            current.v2WriteStatus == LegacyCompatibilityWriteStatus.PENDING
        ) {
            return@mutate record
        }
        val updated = current.copy(
            state = LegacyCompatibilitySagaState.CONVERGED,
            reconciliationStatus = LegacyCompatibilityReconciliationStatus.CONVERGED,
            responseDisposition = LegacyCompatibilityResponseDisposition.CONVERGED_SUCCESS,
            nextRetryAtEpochSeconds = null,
            requiredEffect = null,
            recoveryLease = null
        )
        persistTransition(connection, record, updated, resumeCheckpoint = null)
    }

    override suspend fun recordBlocked(
        sagaId: String,
        reference: LegacyCompatibilityEffectReference
    ): LegacyCompatibilitySnapshot = mutate(sagaId) { connection, record ->
        val current = record.snapshot
        if (
            !current.accepts(reference) ||
            reference.checkpoint != LegacyCompatibilityEffectCheckpoint.PERSIST_BLOCKED_TERMINAL
        ) {
            return@mutate record
        }
        val updated = current.copy(
            state = LegacyCompatibilitySagaState.BLOCKED,
            reconciliationStatus = LegacyCompatibilityReconciliationStatus.BLOCKED,
            responseDisposition = LegacyCompatibilityResponseDisposition.BLOCKED_FAILURE,
            nextRetryAtEpochSeconds = null,
            requiredEffect = null,
            recoveryLease = null
        )
        persistTransition(connection, record, updated, resumeCheckpoint = null)
    }

    override suspend fun acquireRecoveryLease(
        request: LegacyCompatibilityRecoveryLeaseRequest
    ): LegacyCompatibilityRecoveryLease? {
        leaseClaimProbe.attemptStarted(request)
        return immediateTransaction { connection ->
            val record = findRecord(connection, request.sagaId)
                ?: return@immediateTransaction null
            leaseClaimProbe.validatedRead(
                request,
                LegacyCompatibilityLeaseClaimStrategy.SERIALIZED_MUTATION_AUTHORITY_ALREADY_HELD
            )
            val current = record.snapshot
            val requiredEffect = current.requiredEffect ?: return@immediateTransaction null
            val currentLease = current.recoveryLease
            if (
                request.holderId.isBlank() ||
                request.expectedEffectId != requiredEffect.effectId ||
                request.effectCheckpoint != requiredEffect.checkpoint ||
                request.checkpointRevision != requiredEffect.checkpointRevision ||
                request.expectedLeaseVersion != (currentLease?.leaseVersion ?: 0L) ||
                request.newLeaseVersion <= request.expectedLeaseVersion ||
                request.fencingToken <= max(
                    requiredEffect.fencingToken,
                    currentLease?.fencingToken ?: 0L
                ) ||
                request.expiresAtEpochSeconds <= current.logicalNowEpochSeconds ||
                currentLease?.let { current.logicalNowEpochSeconds < it.expiresAtEpochSeconds } == true
            ) {
                return@immediateTransaction null
            }

            val lease = LegacyCompatibilityRecoveryLease(
                sagaId = current.sagaId,
                leaseId = UUID.randomUUID().toString(),
                holderId = request.holderId,
                leaseVersion = request.newLeaseVersion,
                fencingToken = request.fencingToken,
                expiresAtEpochSeconds = request.expiresAtEpochSeconds,
                expectedEffectId = requiredEffect.effectId,
                effectCheckpoint = requiredEffect.checkpoint,
                checkpointRevision = requiredEffect.checkpointRevision,
                effectEmitted = false
            )
            val fencedReference = requiredEffect.copy(fencingToken = request.fencingToken)
            val updated = current.copy(requiredEffect = fencedReference, recoveryLease = lease)
            updateSnapshot(connection, updated, record.resumeCheckpoint)
            insertEffectHistory(connection, current.sagaId, fencedReference)
            lease
        }
    }

    override suspend fun requestRecovery(
        request: LegacyCompatibilityRecoveryRequest
    ): LegacyCompatibilityEffectReference? = immediateTransaction { connection ->
            val record = findRecord(connection, request.sagaId)
                ?: return@immediateTransaction null
            val current = record.snapshot
            val lease = current.recoveryLease ?: return@immediateTransaction null
            val required = current.requiredEffect ?: return@immediateTransaction null
            if (
                lease.effectEmitted ||
                current.logicalNowEpochSeconds >= lease.expiresAtEpochSeconds ||
                request.leaseId != lease.leaseId ||
                request.holderId != lease.holderId ||
                request.leaseVersion != lease.leaseVersion ||
                request.fencingToken != lease.fencingToken ||
                request.expiresAtEpochSeconds != lease.expiresAtEpochSeconds ||
                request.expectedEffectId != lease.expectedEffectId ||
                request.effectCheckpoint != lease.effectCheckpoint ||
                request.checkpointRevision != lease.checkpointRevision ||
                required.effectId != request.expectedEffectId ||
                required.checkpoint != request.effectCheckpoint ||
                required.checkpointRevision != request.checkpointRevision ||
                required.fencingToken != request.fencingToken
            ) {
                return@immediateTransaction null
            }
            val updated = current.copy(recoveryLease = lease.copy(effectEmitted = true))
            updateSnapshot(connection, updated, record.resumeCheckpoint)
            required
    }

    override suspend fun advanceLogicalClock(
        sagaId: String,
        clockRevision: Long,
        nowEpochSeconds: Long
    ): LegacyCompatibilitySnapshot = mutate(sagaId) { connection, record ->
        val current = record.snapshot
        if (
            clockRevision != current.clockRevision + 1 ||
            nowEpochSeconds < current.logicalNowEpochSeconds
        ) {
            return@mutate record
        }
        val updated = current.copy(
            logicalNowEpochSeconds = nowEpochSeconds,
            clockRevision = clockRevision
        )
        updateSnapshot(connection, updated, record.resumeCheckpoint)
        record.copy(snapshot = updated)
    }

    override suspend fun effectHistory(
        sagaId: String
    ): List<LegacyCompatibilityEffectReference> = synchronized(LEGACY_COMPATIBILITY_STORE_LOCK) {
        read { connection ->
            connection.prepareStatement(
                """
                SELECT effect_id, effect_checkpoint, checkpoint_revision, fencing_token
                FROM legacy_notification_compatibility_effect_history
                WHERE saga_id = ?
                ORDER BY sequence_id ASC
                """.trimIndent()
            ).use { statement ->
                statement.setString(1, sagaId.trim())
                statement.executeQuery().use { rows ->
                    buildList {
                        while (rows.next()) {
                            add(
                                LegacyCompatibilityEffectReference(
                                    effectId = rows.getString("effect_id"),
                                    checkpoint = LegacyCompatibilityEffectCheckpoint.valueOf(
                                        rows.getString("effect_checkpoint")
                                    ),
                                    checkpointRevision = rows.getLong("checkpoint_revision"),
                                    fencingToken = rows.getLong("fencing_token")
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    override suspend fun recoveryCandidateSagaIds(): List<String> =
        synchronized(LEGACY_COMPATIBILITY_STORE_LOCK) {
            read { connection ->
                connection.prepareStatement(
                    """
                    SELECT saga_id
                    FROM legacy_notification_compatibility_saga
                    WHERE state NOT IN ('CONVERGED', 'BLOCKED')
                    ORDER BY saga_id
                    """.trimIndent()
                ).use { statement ->
                    statement.executeQuery().use { rows ->
                        buildList { while (rows.next()) add(rows.getString("saga_id")) }
                    }
                }
            }
        }

    override suspend fun allocateCompatibilityGeneration(
        authenticatedUserId: String,
        stableTargetIdentity: String,
        operation: LegacyCompatibilityOperation,
        tokenFingerprint: String?
    ): Long {
        val normalizedUserId = authenticatedUserId.trim().takeIf(String::isNotEmpty)
            ?: throw IllegalArgumentException("authenticatedUserId is required")
        val normalizedTarget = stableTargetIdentity.trim().takeIf(String::isNotEmpty)
            ?: throw IllegalArgumentException("stableTargetIdentity is required")
        if (operation == LegacyCompatibilityOperation.REGISTER) {
            require(!tokenFingerprint.isNullOrBlank()) {
                "A token fingerprint is required for registration generation"
            }
        } else {
            require(tokenFingerprint == null) {
                "Unregistration generation must not carry a token fingerprint"
            }
        }
        return immediateTransaction { connection ->
            val subjectKey = opaqueCompatibilityDigest(listOf("subject", normalizedUserId))
            val targetKey = opaqueCompatibilityDigest(listOf("target", normalizedTarget))
            val desiredKey = opaqueCompatibilityDigest(
                listOf("desired", operation.name, tokenFingerprint)
            )
            val existing = connection.prepareStatement(
                """
                SELECT desired_key, compatibility_generation
                FROM legacy_notification_compatibility_generation
                WHERE subject_key = ? AND target_key = ?
                """.trimIndent()
            ).use { statement ->
                statement.setString(1, subjectKey)
                statement.setString(2, targetKey)
                statement.executeQuery().use { rows ->
                    if (rows.next()) rows.getString(1) to rows.getLong(2) else null
                }
            }
            if (existing?.first == desiredKey) return@immediateTransaction existing.second
            val generation = (existing?.second ?: 0L) + 1L
            connection.prepareStatement(
                """
                INSERT INTO legacy_notification_compatibility_generation(
                    subject_key, target_key, desired_key, compatibility_generation
                ) VALUES (?, ?, ?, ?)
                ON CONFLICT(subject_key, target_key) DO UPDATE SET
                    desired_key = excluded.desired_key,
                    compatibility_generation = excluded.compatibility_generation
                """.trimIndent()
            ).use { statement ->
                statement.setString(1, subjectKey)
                statement.setString(2, targetKey)
                statement.setString(3, desiredKey)
                statement.setLong(4, generation)
                statement.executeUpdate()
            }
            generation
        }
    }

    override suspend fun withRegistrationToken(
        sagaId: String,
        block: suspend (LegacyCompatibilityScopedToken) -> Unit
    ): Boolean {
        val normalizedSagaId = sagaId.trim()
        val ciphertext: ByteArray? = synchronized(LEGACY_COMPATIBILITY_STORE_LOCK) {
            read { connection ->
                connection.prepareStatement(
                    """
                    SELECT token_ciphertext
                    FROM legacy_notification_compatibility_saga
                    WHERE saga_id = ?
                    """.trimIndent()
                ).use { statement ->
                    statement.setString(1, normalizedSagaId)
                    statement.executeQuery().use { rows ->
                        if (rows.next()) rows.getBytes("token_ciphertext") else null
                    }
                }
            }
        }
        if (ciphertext == null) return false
        val plaintext = synchronized(LEGACY_COMPATIBILITY_STORE_LOCK) {
            try {
                tokenCipher.decrypt(normalizedSagaId, ciphertext)
            } finally {
                ciphertext.fill(0)
            }
        }
        val tokenChars = try {
            val decoded = StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(plaintext))
            try {
                CharArray(decoded.remaining()).also { decoded.get(it) }
            } finally {
                decoded.clear()
                while (decoded.hasRemaining()) decoded.put('\u0000')
            }
        } finally {
            plaintext.fill(0)
        }
        val scope = RevocableLegacyCompatibilityScopedToken(tokenChars)
        return try {
            block(scope)
            true
        } finally {
            scope.close()
        }
    }

    override fun close() {
        synchronized(LEGACY_COMPATIBILITY_STORE_LOCK) {
            if (closed.compareAndSet(false, true)) tokenCipher.close()
        }
    }

    private suspend fun mutate(
        sagaId: String,
        block: (Connection, SagaRecord) -> SagaRecord
    ): LegacyCompatibilitySnapshot = immediateTransaction { connection ->
            val record = findRecord(connection, sagaId.trim())
                ?: throw IllegalArgumentException("Compatibility saga is unavailable")
            block(connection, record).snapshot
    }

    private fun persistTransition(
        connection: Connection,
        previous: SagaRecord,
        snapshot: LegacyCompatibilitySnapshot,
        resumeCheckpoint: LegacyCompatibilityEffectCheckpoint?
    ): SagaRecord {
        updateSnapshot(connection, snapshot, resumeCheckpoint)
        val previousReference = previous.snapshot.requiredEffect
        val nextReference = snapshot.requiredEffect
        if (nextReference != null && nextReference != previousReference) {
            insertEffectHistory(connection, snapshot.sagaId, nextReference)
        }
        return SagaRecord(snapshot, resumeCheckpoint)
    }

    private fun insertRecord(
        connection: Connection,
        snapshot: LegacyCompatibilitySnapshot,
        resumeCheckpoint: LegacyCompatibilityEffectCheckpoint?,
        ciphertext: ByteArray?
    ) {
        connection.prepareStatement(
            """
            INSERT INTO legacy_notification_compatibility_saga(
                saga_id, request_key, operation, client_generation, authenticated_user_id,
                platform, legacy_primary_key_fingerprint, legacy_installation_id,
                legacy_registration_id, target_installation_id, target_registration_id,
                token_fingerprint, compatibility_generation, max_attempts_per_store,
                scope_environment, scope_topic, state, reconciliation_status,
                response_disposition, legacy_write_status, v2_write_status, v2_target_kind,
                legacy_attempt, v2_attempt, next_retry_at_epoch_seconds, checkpoint_revision,
                logical_now_epoch_seconds, clock_revision, required_effect_id,
                required_effect_checkpoint, required_effect_revision, required_effect_fencing,
                resume_checkpoint, lease_id, lease_holder_id, lease_version,
                lease_fencing_token, lease_expires_at_epoch_seconds, lease_effect_emitted,
                last_failure, token_ciphertext
            ) VALUES (
                ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
                ?, ?, ?, ?, ?, ?, ?, ?, NULL, NULL, NULL, NULL, NULL, NULL, ?, ?
            )
            """.trimIndent()
        ).use { statement ->
            var index = 1
            statement.setString(index++, snapshot.sagaId)
            statement.setString(index++, snapshot.requestKey)
            statement.setString(index++, snapshot.operation.name)
            statement.setString(index++, snapshot.clientGeneration.name)
            statement.setString(index++, snapshot.authenticatedUserId)
            statement.setString(index++, snapshot.platform.name)
            statement.setNullableString(index++, snapshot.legacyPrimaryKeyFingerprint)
            statement.setNullableString(index++, snapshot.legacyInstallationId)
            statement.setNullableString(index++, snapshot.legacyRegistrationId)
            statement.setString(index++, snapshot.targetInstallationId)
            statement.setNullableString(index++, snapshot.targetRegistrationId)
            statement.setNullableString(index++, snapshot.tokenFingerprint)
            statement.setLong(index++, snapshot.compatibilityGeneration)
            statement.setInt(index++, snapshot.maxAttemptsPerStore)
            statement.setString(index++, snapshot.scope.environment.name)
            statement.setString(index++, snapshot.scope.topic)
            statement.setString(index++, snapshot.state.name)
            statement.setString(index++, snapshot.reconciliationStatus.name)
            statement.setString(index++, snapshot.responseDisposition.name)
            statement.setString(index++, snapshot.legacyWriteStatus.name)
            statement.setString(index++, snapshot.v2WriteStatus.name)
            statement.setString(index++, snapshot.v2TargetKind.name)
            statement.setInt(index++, snapshot.legacyAttempt)
            statement.setInt(index++, snapshot.v2Attempt)
            statement.setNullableLong(index++, snapshot.nextRetryAtEpochSeconds)
            statement.setLong(index++, snapshot.checkpointRevision)
            statement.setLong(index++, snapshot.logicalNowEpochSeconds)
            statement.setLong(index++, snapshot.clockRevision)
            statement.setString(index++, snapshot.requiredEffect?.effectId)
            statement.setString(index++, snapshot.requiredEffect?.checkpoint?.name)
            statement.setNullableLong(index++, snapshot.requiredEffect?.checkpointRevision)
            statement.setNullableLong(index++, snapshot.requiredEffect?.fencingToken)
            statement.setString(index++, resumeCheckpoint?.name)
            statement.setString(index++, snapshot.lastFailure?.name)
            if (ciphertext == null) statement.setNull(index, Types.BLOB) else statement.setBytes(index, ciphertext)
            statement.executeUpdate()
        }
    }

    private fun updateSnapshot(
        connection: Connection,
        snapshot: LegacyCompatibilitySnapshot,
        resumeCheckpoint: LegacyCompatibilityEffectCheckpoint?
    ) {
        connection.prepareStatement(
            """
            UPDATE legacy_notification_compatibility_saga
            SET state = ?, reconciliation_status = ?, response_disposition = ?,
                legacy_write_status = ?, v2_write_status = ?, legacy_attempt = ?, v2_attempt = ?,
                next_retry_at_epoch_seconds = ?, checkpoint_revision = ?,
                logical_now_epoch_seconds = ?, clock_revision = ?, required_effect_id = ?,
                required_effect_checkpoint = ?, required_effect_revision = ?,
                required_effect_fencing = ?, resume_checkpoint = ?, lease_id = ?,
                lease_holder_id = ?, lease_version = ?, lease_fencing_token = ?,
                lease_expires_at_epoch_seconds = ?, lease_effect_emitted = ?, last_failure = ?
            WHERE saga_id = ?
            """.trimIndent()
        ).use { statement ->
            var index = 1
            statement.setString(index++, snapshot.state.name)
            statement.setString(index++, snapshot.reconciliationStatus.name)
            statement.setString(index++, snapshot.responseDisposition.name)
            statement.setString(index++, snapshot.legacyWriteStatus.name)
            statement.setString(index++, snapshot.v2WriteStatus.name)
            statement.setInt(index++, snapshot.legacyAttempt)
            statement.setInt(index++, snapshot.v2Attempt)
            statement.setNullableLong(index++, snapshot.nextRetryAtEpochSeconds)
            statement.setLong(index++, snapshot.checkpointRevision)
            statement.setLong(index++, snapshot.logicalNowEpochSeconds)
            statement.setLong(index++, snapshot.clockRevision)
            statement.setNullableString(index++, snapshot.requiredEffect?.effectId)
            statement.setNullableString(index++, snapshot.requiredEffect?.checkpoint?.name)
            statement.setNullableLong(index++, snapshot.requiredEffect?.checkpointRevision)
            statement.setNullableLong(index++, snapshot.requiredEffect?.fencingToken)
            statement.setNullableString(index++, resumeCheckpoint?.name)
            val lease = snapshot.recoveryLease
            statement.setNullableString(index++, lease?.leaseId)
            statement.setNullableString(index++, lease?.holderId)
            statement.setNullableLong(index++, lease?.leaseVersion)
            statement.setNullableLong(index++, lease?.fencingToken)
            statement.setNullableLong(index++, lease?.expiresAtEpochSeconds)
            if (lease == null) statement.setNull(index++, Types.INTEGER) else statement.setInt(index++, if (lease.effectEmitted) 1 else 0)
            statement.setNullableString(index++, snapshot.lastFailure?.name)
            statement.setString(index, snapshot.sagaId)
            check(statement.executeUpdate() == 1) { "Compatibility saga changed during update" }
        }
    }

    private fun findRecord(connection: Connection, sagaId: String): SagaRecord? =
        connection.prepareStatement(
            "SELECT * FROM legacy_notification_compatibility_saga WHERE saga_id = ?"
        ).use { statement ->
            statement.setString(1, sagaId)
            statement.executeQuery().use { rows -> if (rows.next()) rows.sagaRecord() else null }
        }

    private fun findRecordByRequestKey(connection: Connection, requestKey: String): SagaRecord? =
        connection.prepareStatement(
            "SELECT * FROM legacy_notification_compatibility_saga WHERE request_key = ?"
        ).use { statement ->
            statement.setString(1, requestKey)
            statement.executeQuery().use { rows -> if (rows.next()) rows.sagaRecord() else null }
        }

    private fun ResultSet.sagaRecord(): SagaRecord {
        val requiredEffectId = getString("required_effect_id")
        val requiredEffect = requiredEffectId?.let {
            LegacyCompatibilityEffectReference(
                effectId = it,
                checkpoint = LegacyCompatibilityEffectCheckpoint.valueOf(
                    getString("required_effect_checkpoint")
                ),
                checkpointRevision = getLong("required_effect_revision"),
                fencingToken = getLong("required_effect_fencing")
            )
        }
        val leaseId = getString("lease_id")
        val recoveryLease = leaseId?.let {
            LegacyCompatibilityRecoveryLease(
                sagaId = getString("saga_id"),
                leaseId = it,
                holderId = getString("lease_holder_id"),
                leaseVersion = getLong("lease_version"),
                fencingToken = getLong("lease_fencing_token"),
                expiresAtEpochSeconds = getLong("lease_expires_at_epoch_seconds"),
                expectedEffectId = checkNotNull(requiredEffect).effectId,
                effectCheckpoint = requiredEffect.checkpoint,
                checkpointRevision = requiredEffect.checkpointRevision,
                effectEmitted = getInt("lease_effect_emitted") == 1
            )
        }
        val snapshot = LegacyCompatibilitySnapshot(
            sagaId = getString("saga_id"),
            requestKey = getString("request_key"),
            operation = LegacyCompatibilityOperation.valueOf(getString("operation")),
            clientGeneration = LegacyCompatibilityClientGeneration.valueOf(getString("client_generation")),
            authenticatedUserId = getString("authenticated_user_id"),
            platform = Platform.valueOf(getString("platform")),
            legacyPrimaryKeyFingerprint = getString("legacy_primary_key_fingerprint"),
            legacyInstallationId = getString("legacy_installation_id"),
            legacyRegistrationId = getString("legacy_registration_id"),
            targetInstallationId = getString("target_installation_id"),
            targetRegistrationId = getString("target_registration_id"),
            tokenFingerprint = getString("token_fingerprint"),
            compatibilityGeneration = getLong("compatibility_generation"),
            maxAttemptsPerStore = getInt("max_attempts_per_store"),
            scope = DeviceRegistrationScope.create(
                APNsEnvironment.valueOf(getString("scope_environment")),
                getString("scope_topic")
            ).getOrThrow(),
            state = LegacyCompatibilitySagaState.valueOf(getString("state")),
            reconciliationStatus = LegacyCompatibilityReconciliationStatus.valueOf(
                getString("reconciliation_status")
            ),
            responseDisposition = LegacyCompatibilityResponseDisposition.valueOf(
                getString("response_disposition")
            ),
            legacyWriteStatus = LegacyCompatibilityWriteStatus.valueOf(getString("legacy_write_status")),
            v2WriteStatus = LegacyCompatibilityWriteStatus.valueOf(getString("v2_write_status")),
            v2TargetKind = LegacyCompatibilityV2TargetKind.valueOf(getString("v2_target_kind")),
            legacyAttempt = getInt("legacy_attempt"),
            v2Attempt = getInt("v2_attempt"),
            nextRetryAtEpochSeconds = nullableLong("next_retry_at_epoch_seconds"),
            checkpointRevision = getLong("checkpoint_revision"),
            logicalNowEpochSeconds = getLong("logical_now_epoch_seconds"),
            clockRevision = getLong("clock_revision"),
            requiredEffect = requiredEffect,
            recoveryLease = recoveryLease,
            lastFailure = getString("last_failure")?.let(LegacyCompatibilityFailure::valueOf)
        )
        return SagaRecord(
            snapshot = snapshot,
            resumeCheckpoint = getString("resume_checkpoint")
                ?.let(LegacyCompatibilityEffectCheckpoint::valueOf)
        )
    }

    private fun insertEffectHistory(
        connection: Connection,
        sagaId: String,
        reference: LegacyCompatibilityEffectReference
    ) {
        connection.prepareStatement(
            """
            INSERT OR IGNORE INTO legacy_notification_compatibility_effect_history(
                saga_id, effect_id, effect_checkpoint, checkpoint_revision, fencing_token
            ) VALUES (?, ?, ?, ?, ?)
            """.trimIndent()
        ).use { statement ->
            statement.setString(1, sagaId)
            statement.setString(2, reference.effectId)
            statement.setString(3, reference.checkpoint.name)
            statement.setLong(4, reference.checkpointRevision)
            statement.setLong(5, reference.fencingToken)
            statement.executeUpdate()
        }
    }

    private fun LegacyCompatibilitySnapshot.withEffect(
        state: LegacyCompatibilitySagaState
    ): LegacyCompatibilitySnapshot {
        val checkpoint = state.effectCheckpoint()
        val revision = checkpointRevision + 1L
        val fence = max(
            requiredEffect?.fencingToken ?: checkpointRevision,
            recoveryLease?.fencingToken ?: checkpointRevision
        ) + 1L
        return copy(
            state = state,
            checkpointRevision = revision,
            nextRetryAtEpochSeconds = null,
            requiredEffect = effectReference(requestKey, checkpoint, revision, fence),
            recoveryLease = null
        )
    }

    private fun LegacyCompatibilitySnapshot.accepts(
        reference: LegacyCompatibilityEffectReference
    ): Boolean = requiredEffect == reference

    private fun LegacyCompatibilitySagaState.effectCheckpoint(): LegacyCompatibilityEffectCheckpoint =
        when (this) {
            LegacyCompatibilitySagaState.WRITING_LEGACY ->
                LegacyCompatibilityEffectCheckpoint.WRITE_LEGACY_STORE
            LegacyCompatibilitySagaState.WRITING_V2 ->
                LegacyCompatibilityEffectCheckpoint.WRITE_V2_REGISTRATION_STORE
            LegacyCompatibilitySagaState.RECORDING_RETRY ->
                LegacyCompatibilityEffectCheckpoint.PERSIST_RETRY_SCHEDULE
            LegacyCompatibilitySagaState.RECORDING_CONVERGENCE ->
                LegacyCompatibilityEffectCheckpoint.PERSIST_CONVERGENCE
            LegacyCompatibilitySagaState.RECORDING_BLOCK ->
                LegacyCompatibilityEffectCheckpoint.PERSIST_BLOCKED_TERMINAL
            else -> error("$this has no pending compatibility effect")
        }

    private fun LegacyCompatibilityEffectCheckpoint.isWriteCheckpoint(): Boolean =
        this == LegacyCompatibilityEffectCheckpoint.WRITE_LEGACY_STORE ||
            this == LegacyCompatibilityEffectCheckpoint.WRITE_V2_REGISTRATION_STORE

    private fun effectReference(
        requestKey: String,
        checkpoint: LegacyCompatibilityEffectCheckpoint,
        checkpointRevision: Long,
        fencingToken: Long
    ): LegacyCompatibilityEffectReference = LegacyCompatibilityEffectReference(
        effectId = opaqueCompatibilityDigest(
            listOf("effect", requestKey, checkpoint.name, checkpointRevision.toString())
        ),
        checkpoint = checkpoint,
        checkpointRevision = checkpointRevision,
        fencingToken = fencingToken
    )

    private fun <T> read(block: (Connection) -> T): T {
        ensureOpen()
        return connection().use(block)
    }

    private fun <T> transaction(block: (Connection) -> T): T {
        ensureOpen()
        return connection().use { connection ->
            connection.autoCommit = false
            try {
                val result = block(connection)
                connection.commit()
                result
            } catch (failure: Throwable) {
                runCatching { connection.rollback() }
                throw failure
            }
        }
    }

    /**
     * SQLite's RESERVED writer lock is acquired before the authoritative row is read. This is
     * the cross-process mutation authority used by intent coalescing, leases and fenced ACKs.
     */
    private suspend fun <T> immediateTransaction(block: suspend (Connection) -> T): T {
        ensureOpen()
        val connection = connection()
        try {
            connection.createStatement().use { it.execute("BEGIN IMMEDIATE") }
            return try {
                val result = block(connection)
                connection.createStatement().use { it.execute("COMMIT") }
                result
            } catch (failure: Throwable) {
                runCatching {
                    connection.createStatement().use { it.execute("ROLLBACK") }
                }
                throw failure
            }
        } finally {
            connection.close()
        }
    }

    private fun connection(): Connection =
        openDeviceRegistrationJdbcConnection(databasePath, jdbcOpenTestHook).also { connection ->
            connection.createStatement().use { statement ->
                statement.execute("PRAGMA foreign_keys = ON")
                statement.execute("PRAGMA busy_timeout = 5000")
            }
        }

    private fun ensureOpen() {
        check(!closed.get()) { "Compatibility saga store is closed" }
    }

    private fun createSchema(connection: Connection) {
        connection.createStatement().use { statement ->
            statement.execute(
                """
                CREATE TABLE IF NOT EXISTS legacy_notification_compatibility_saga (
                    saga_id TEXT PRIMARY KEY NOT NULL,
                    request_key TEXT NOT NULL,
                    operation TEXT NOT NULL CHECK(operation IN ('REGISTER', 'UNREGISTER')),
                    client_generation TEXT NOT NULL CHECK(client_generation IN ('N', 'N_MINUS_1')),
                    authenticated_user_id TEXT NOT NULL,
                    platform TEXT NOT NULL CHECK(platform = 'IOS'),
                    legacy_primary_key_fingerprint TEXT,
                    legacy_installation_id TEXT,
                    legacy_registration_id TEXT,
                    target_installation_id TEXT NOT NULL,
                    target_registration_id TEXT,
                    token_fingerprint TEXT,
                    compatibility_generation INTEGER NOT NULL CHECK(compatibility_generation > 0),
                    max_attempts_per_store INTEGER NOT NULL CHECK(max_attempts_per_store > 0),
                    scope_environment TEXT NOT NULL,
                    scope_topic TEXT NOT NULL,
                    state TEXT NOT NULL,
                    reconciliation_status TEXT NOT NULL,
                    response_disposition TEXT NOT NULL,
                    legacy_write_status TEXT NOT NULL,
                    v2_write_status TEXT NOT NULL,
                    v2_target_kind TEXT NOT NULL,
                    legacy_attempt INTEGER NOT NULL CHECK(legacy_attempt >= 0),
                    v2_attempt INTEGER NOT NULL CHECK(v2_attempt >= 0),
                    next_retry_at_epoch_seconds INTEGER,
                    checkpoint_revision INTEGER NOT NULL CHECK(checkpoint_revision > 0),
                    logical_now_epoch_seconds INTEGER NOT NULL CHECK(logical_now_epoch_seconds >= 0),
                    clock_revision INTEGER NOT NULL CHECK(clock_revision >= 0),
                    required_effect_id TEXT,
                    required_effect_checkpoint TEXT,
                    required_effect_revision INTEGER,
                    required_effect_fencing INTEGER,
                    resume_checkpoint TEXT,
                    lease_id TEXT,
                    lease_holder_id TEXT,
                    lease_version INTEGER,
                    lease_fencing_token INTEGER,
                    lease_expires_at_epoch_seconds INTEGER,
                    lease_effect_emitted INTEGER CHECK(lease_effect_emitted IN (0, 1)),
                    last_failure TEXT,
                    token_ciphertext BLOB,
                    CHECK (
                        (required_effect_id IS NULL AND required_effect_checkpoint IS NULL
                            AND required_effect_revision IS NULL AND required_effect_fencing IS NULL)
                        OR
                        (required_effect_id IS NOT NULL AND required_effect_checkpoint IS NOT NULL
                            AND required_effect_revision IS NOT NULL AND required_effect_fencing IS NOT NULL)
                    ),
                    CHECK (
                        (lease_id IS NULL AND lease_holder_id IS NULL AND lease_version IS NULL
                            AND lease_fencing_token IS NULL AND lease_expires_at_epoch_seconds IS NULL
                            AND lease_effect_emitted IS NULL)
                        OR
                        (lease_id IS NOT NULL AND lease_holder_id IS NOT NULL AND lease_version IS NOT NULL
                            AND lease_fencing_token IS NOT NULL AND lease_expires_at_epoch_seconds IS NOT NULL
                            AND lease_effect_emitted IS NOT NULL)
                    )
                )
                """.trimIndent()
            )
            statement.execute(
                """
                CREATE TABLE IF NOT EXISTS legacy_notification_compatibility_effect_history (
                    sequence_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    saga_id TEXT NOT NULL,
                    effect_id TEXT NOT NULL,
                    effect_checkpoint TEXT NOT NULL,
                    checkpoint_revision INTEGER NOT NULL,
                    fencing_token INTEGER NOT NULL,
                    UNIQUE(saga_id, effect_id, fencing_token),
                    FOREIGN KEY(saga_id)
                        REFERENCES legacy_notification_compatibility_saga(saga_id)
                        ON DELETE RESTRICT
                )
                """.trimIndent()
            )
            statement.execute(
                """
                CREATE TABLE IF NOT EXISTS legacy_notification_compatibility_generation (
                    subject_key TEXT NOT NULL,
                    target_key TEXT NOT NULL,
                    desired_key TEXT NOT NULL,
                    compatibility_generation INTEGER NOT NULL CHECK(compatibility_generation > 0),
                    PRIMARY KEY(subject_key, target_key)
                )
                """.trimIndent()
            )
        }
    }

    private data class SagaRecord(
        val snapshot: LegacyCompatibilitySnapshot,
        val resumeCheckpoint: LegacyCompatibilityEffectCheckpoint?
    )
}

internal fun deriveLegacyCompatibilityIdentity(
    hmacKey: ByteArray,
    legacyRowKey: String
): LegacyCompatibilityIdentity {
    val normalizedRowKey = legacyRowKey.trim().takeIf(String::isNotEmpty)
        ?: throw IllegalArgumentException("legacyRowKey is required")
    return LegacyCompatibilityIdentity(
        installationId = legacyCompatibilityHmacIdentity(
            hmacKey,
            "wakeve/legacy-installation/v1",
            normalizedRowKey
        ),
        registrationId = legacyCompatibilityHmacIdentity(
            hmacKey,
            "wakeve/legacy-registration/v1",
            normalizedRowKey
        )
    )
}

internal fun compatibilityTokenFingerprint(rawToken: String): String {
    val tokenBytes = rawToken.toByteArray(StandardCharsets.UTF_8)
    return try {
        Base64.getUrlEncoder().withoutPadding().encodeToString(
            MessageDigest.getInstance("SHA-256").digest(tokenBytes)
        )
    } finally {
        tokenBytes.fill(0)
    }
}

private fun legacyCompatibilityHmacIdentity(
    hmacKey: ByteArray,
    domain: String,
    legacyRowKey: String
): String {
    val mac = Mac.getInstance("HmacSHA256")
    mac.init(SecretKeySpec(hmacKey, "HmacSHA256"))
    val input = "$domain\u0000$legacyRowKey".toByteArray(StandardCharsets.UTF_8)
    return try {
        "legacy-${Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(input))}"
    } finally {
        input.fill(0)
    }
}

private class RevocableLegacyCompatibilityScopedToken(
    private val tokenChars: CharArray
) : LegacyCompatibilityScopedToken, AutoCloseable {
    private val active = AtomicBoolean(true)

    override suspend fun consumeWith(sink: suspend (CharArray) -> Unit) {
        check(active.get()) { "Scoped registration token is closed" }
        val oneUseCopy = tokenChars.copyOf()
        try {
            sink(oneUseCopy)
        } finally {
            oneUseCopy.fill('\u0000')
        }
    }

    override fun close() {
        if (active.compareAndSet(true, false)) tokenChars.fill('\u0000')
    }
}

private class LegacyCompatibilityTokenCipher(secret: ByteArray) : AutoCloseable {
    private val keyBytes = secret.copyOf().let { keyMaterial ->
        try {
            MessageDigest.getInstance("SHA-256").digest(keyMaterial)
        } finally {
            keyMaterial.fill(0)
        }
    }
    private val random = SecureRandom()
    private val closed = AtomicBoolean(false)

    fun encrypt(sagaId: String, rawToken: String): ByteArray {
        ensureOpen()
        val nonce = ByteArray(NONCE_BYTES).also(random::nextBytes)
        val plaintext = rawToken.toByteArray(StandardCharsets.UTF_8)
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.ENCRYPT_MODE,
                SecretKeySpec(keyBytes, "AES"),
                GCMParameterSpec(TAG_BITS, nonce)
            )
            cipher.updateAAD(aad(sagaId))
            val encrypted = cipher.doFinal(plaintext)
            ByteArray(1 + nonce.size + encrypted.size).also { output ->
                output[0] = FORMAT_VERSION
                nonce.copyInto(output, 1)
                encrypted.copyInto(output, 1 + nonce.size)
            }
        } finally {
            nonce.fill(0)
            plaintext.fill(0)
        }
    }

    fun decrypt(sagaId: String, encoded: ByteArray): ByteArray {
        ensureOpen()
        require(encoded.size > 1 + NONCE_BYTES) { "Encrypted compatibility token is malformed" }
        require(encoded[0] == FORMAT_VERSION) { "Encrypted compatibility token version is unsupported" }
        val nonce = encoded.copyOfRange(1, 1 + NONCE_BYTES)
        val ciphertext = encoded.copyOfRange(1 + NONCE_BYTES, encoded.size)
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                SecretKeySpec(keyBytes, "AES"),
                GCMParameterSpec(TAG_BITS, nonce)
            )
            cipher.updateAAD(aad(sagaId))
            cipher.doFinal(ciphertext)
        } finally {
            nonce.fill(0)
            ciphertext.fill(0)
        }
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) keyBytes.fill(0)
    }

    private fun ensureOpen() {
        check(!closed.get()) { "Compatibility token cipher is closed" }
    }

    private fun aad(sagaId: String): ByteArray =
        "wakeve/legacy-compatibility-token/v1\u0000$sagaId"
            .toByteArray(StandardCharsets.UTF_8)

    private companion object {
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val NONCE_BYTES = 12
        const val TAG_BITS = 128
        const val FORMAT_VERSION: Byte = 1
    }
}

private fun java.sql.PreparedStatement.setNullableString(index: Int, value: String?) {
    if (value == null) setNull(index, Types.VARCHAR) else setString(index, value)
}

private fun java.sql.PreparedStatement.setNullableLong(index: Int, value: Long?) {
    if (value == null) setNull(index, Types.INTEGER) else setLong(index, value)
}

private fun ResultSet.nullableLong(column: String): Long? = getLong(column).takeUnless { wasNull() }

private val LEGACY_COMPATIBILITY_STORE_LOCK = Any()
