package com.guyghost.wakeve.notification

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.ServiceLoader
import kotlinx.coroutines.runBlocking
import kotlin.io.path.name
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Compile-RED contract for device registration task 3.1.
 *
 * The production backend owns two durable concepts: a stable installation and immutable
 * registration history. Raw tokens cross only the registration command and the provider-only
 * decryption port; ordinary reads, diagnostics, rollback projections, and SQLite bytes never
 * expose them.
 */
class BackendDeviceRegistrationContractRedTest {
    @Test
    fun explicitConfigurationResolutionFailsClosedWithoutReadingTheRealEnvironment() {
        assertSanitizedConfigurationFailure(emptyMap(), "durable database path")
        assertSanitizedConfigurationFailure(
            mapOf(DATABASE_PATH_PROPERTY to "/tmp/wakeve-registration-contract.sqlite"),
            "legacy identity HMAC key"
        )
        assertSanitizedConfigurationFailure(
            mapOf(
                DATABASE_PATH_PROPERTY to "/tmp/wakeve-registration-contract.sqlite",
                LEGACY_HMAC_KEY_PROPERTY to HMAC_KEY_A
            ),
            "token encryption key"
        )

        val configured = explicitConfiguration(
            Path.of("/tmp/wakeve-registration-contract.sqlite"),
            HMAC_KEY_A
        )
        assertEquals(Path.of("/tmp/wakeve-registration-contract.sqlite"), configured.databasePath)
        assertSanitized(configured, HMAC_KEY_A, TOKEN_ENCRYPTION_KEY)
    }

    @Test
    fun productionFactoryIsServiceLoaderRegisteredWithoutOpeningAmbientConfiguration() {
        val factory = assertNotNull(
            ServiceLoader.load(BackendDeviceRegistrationStoreFactory::class.java).firstOrNull(),
            "the production backend registration factory must be ServiceLoader-registered"
        )
        assertIs<SqliteBackendDeviceRegistrationStoreFactory>(factory)
    }

    @Test
    fun stableInstallationsAndDistinctRegistrationIdsSurviveCloseAndReopen() = runBlocking {
        withConfiguredRuntime { factory, _, _ ->
            lateinit var iphone: BackendDeviceRegistration
            lateinit var ipad: BackendDeviceRegistration
            factory.open().use { store ->
                iphone = store.register(request("iphone", "user-a", "iphone-token", at = 100))
                ipad = store.register(request("ipad", "user-a", "ipad-token", at = 110))
                assertNotEquals(iphone.registrationId, ipad.registrationId)
                assertNotEquals(iphone.installationId, iphone.registrationId)
            }

            factory.open().use { restarted ->
                assertInstallation(restarted.installation("iphone"), "iphone", Platform.IOS, 100)
                assertInstallation(restarted.installation("ipad"), "ipad", Platform.IOS, 110)
                assertRegistration(restarted.registration(iphone.registrationId), iphone)
                assertRegistration(restarted.registration(ipad.registrationId), ipad)
                assertEquals(
                    setOf(iphone.registrationId, ipad.registrationId),
                    restarted.activeRegistrations("user-a", productionScope()).map { it.registrationId }.toSet()
                )
            }
        }
    }

    @Test
    fun rotationForTheSameAccountAndScopePersistsEveryRegistrationColumnAfterReopen() = runBlocking {
        val rawTokenV1 = "rotation-secret-token-v1"
        val rawTokenV2 = "rotation-secret-token-v2"
        withConfiguredRuntime { factory, databasePath, _ ->
            lateinit var first: BackendDeviceRegistration
            lateinit var rotated: BackendDeviceRegistration
            factory.open().use { store ->
                first = store.register(request("iphone", "user-a", rawTokenV1, at = 100))
            }
            factory.open().use { store ->
                rotated = store.register(request("iphone", "user-a", rawTokenV2, at = 120))
                assertEquals(first.registrationId, rotated.registrationId)
                assertNotEquals(first.tokenHash, rotated.tokenHash)
                assertEquals(100, rotated.createdAtEpochSeconds)
                assertEquals(120, rotated.updatedAtEpochSeconds)
                assertEquals(120, rotated.lastRegisteredAtEpochSeconds)
                assertTokenlessRegistrationDto(rotated, rawTokenV1, rawTokenV2)
            }
            factory.open().use { restarted ->
                assertRegistration(restarted.registration(rotated.registrationId), rotated)
                assertRegistration(restarted.activeRegistration("iphone"), rotated)
                assertEquals(listOf(rotated), restarted.registrationHistory("iphone"))
            }

            assertDatabaseFilesDoNotContain(databasePath, rawTokenV1, rawTokenV2)
            factory.openProviderTokenPort().use { providerTokens ->
                assertEquals(
                    true,
                    providerTokens.withDecryptedToken(rotated.registrationId) { token -> token == rawTokenV2 },
                    "only the provider port may observe the decrypted token"
                )
            }
        }
    }

    @Test
    fun accountChangeAtomicallyClosesTheOldRegistrationAndCreatesANewOne() = runBlocking {
        withConfiguredRuntime { factory, _, _ ->
            lateinit var oldRegistration: BackendDeviceRegistration
            lateinit var newRegistration: BackendDeviceRegistration
            factory.open().use { store ->
                oldRegistration = store.register(request("shared-phone", "user-a", "account-a-token", at = 100))
                newRegistration = store.register(request("shared-phone", "user-b", "account-b-token", at = 120))
                assertNotEquals(oldRegistration.registrationId, newRegistration.registrationId)
            }

            factory.open().use { restarted ->
                val history = restarted.registrationHistory("shared-phone")
                val expectedClosed = oldRegistration.copy(
                    status = DeviceRegistrationStatus.UNREGISTERED,
                    updatedAtEpochSeconds = 120,
                    unregisteredAtEpochSeconds = 120,
                    unregisteredReason = DeviceRegistrationUnregisteredReason.ACCOUNT_CHANGED
                )
                assertEquals(2, history.size)
                assertEquals(1, history.count { it.status == DeviceRegistrationStatus.ACTIVE })
                assertRegistration(restarted.registration(oldRegistration.registrationId), expectedClosed)
                assertRegistration(restarted.activeRegistration("shared-phone"), newRegistration)
                assertNull(restarted.activeRegistrations("user-a", productionScope()).singleOrNull())
                assertEquals(
                    newRegistration.registrationId,
                    restarted.activeRegistrations("user-b", productionScope()).single().registrationId
                )
            }
        }
    }

    @Test
    fun replacementFaultAfterAttemptedClosureRollsBackTheExactActiveRegistration() = runBlocking {
        withConfiguredRuntime { factory, _, configuration ->
            lateinit var original: BackendDeviceRegistration
            factory.open().use { store ->
                original = store.register(request("shared-phone", "user-a", "original-token", at = 100))
            }

            val faultingFactory = SqliteBackendDeviceRegistrationStoreFactory(
                configuration = configuration,
                transactionFaultInjector = DeviceRegistrationTransactionFaultInjector { point ->
                    if (point == DeviceRegistrationTransactionFaultPoint.AFTER_ACTIVE_REGISTRATION_CLOSED) {
                        throw InjectedRegistrationTransactionFailure()
                    }
                }
            )
            assertFailsWith<InjectedRegistrationTransactionFailure> {
                runBlocking {
                    faultingFactory.open().use { store ->
                        store.register(request("shared-phone", "user-b", "replacement-token", at = 120))
                    }
                }
            }

            factory.open().use { restarted ->
                assertRegistration(restarted.activeRegistration("shared-phone"), original)
                assertRegistration(restarted.registration(original.registrationId), original)
                assertEquals(listOf(original), restarted.registrationHistory("shared-phone"))
                assertTrue(restarted.activeRegistrations("user-b", productionScope()).isEmpty())
            }
        }
    }

    @Test
    fun topicOnlyChangeCreatesANewRegistrationAndRecordsScopeChanged() = runBlocking {
        assertTrue(
            DeviceRegistrationScope.create(APNsEnvironment.PRODUCTION, "   ").isFailure,
            "a blank APNs topic must fail closed before registration mutation"
        )
        val originalScope = productionScope()
        val newTopicScope = scope(APNsEnvironment.PRODUCTION, "com.guyghost.wakeve.beta")
        withConfiguredRuntime { factory, _, _ ->
            lateinit var oldRegistration: BackendDeviceRegistration
            lateinit var newRegistration: BackendDeviceRegistration
            factory.open().use { store ->
                oldRegistration = store.register(request("iphone", "user-a", "topic-a-token", 100, originalScope))
                newRegistration = store.register(request("iphone", "user-a", "topic-b-token", 120, newTopicScope))
            }

            factory.open().use { restarted ->
                assertNotEquals(oldRegistration.registrationId, newRegistration.registrationId)
                assertRegistration(
                    restarted.registration(oldRegistration.registrationId),
                    oldRegistration.copy(
                        status = DeviceRegistrationStatus.UNREGISTERED,
                        updatedAtEpochSeconds = 120,
                        unregisteredAtEpochSeconds = 120,
                        unregisteredReason = DeviceRegistrationUnregisteredReason.SCOPE_CHANGED
                    )
                )
                assertRegistration(restarted.activeRegistration("iphone"), newRegistration)
                assertTrue(restarted.activeRegistrations("user-a", originalScope).isEmpty())
                assertEquals(
                    newRegistration.registrationId,
                    restarted.activeRegistrations("user-a", newTopicScope).single().registrationId
                )
            }
        }
    }

    @Test
    fun environmentOnlyChangeCreatesANewRegistrationAndRecordsScopeChanged() = runBlocking {
        val production = productionScope()
        val sandboxSameTopic = scope(APNsEnvironment.SANDBOX, "com.guyghost.wakeve")
        withConfiguredRuntime { factory, _, _ ->
            lateinit var oldRegistration: BackendDeviceRegistration
            lateinit var newRegistration: BackendDeviceRegistration
            factory.open().use { store ->
                oldRegistration = store.register(request("iphone", "user-a", "production-token", 100, production))
                newRegistration = store.register(request("iphone", "user-a", "sandbox-token", 120, sandboxSameTopic))
            }

            factory.open().use { restarted ->
                assertNotEquals(oldRegistration.registrationId, newRegistration.registrationId)
                assertRegistration(
                    restarted.registration(oldRegistration.registrationId),
                    oldRegistration.copy(
                        status = DeviceRegistrationStatus.UNREGISTERED,
                        updatedAtEpochSeconds = 120,
                        unregisteredAtEpochSeconds = 120,
                        unregisteredReason = DeviceRegistrationUnregisteredReason.SCOPE_CHANGED
                    )
                )
                assertRegistration(restarted.activeRegistration("iphone"), newRegistration)
                assertTrue(restarted.activeRegistrations("user-a", production).isEmpty())
                assertEquals(
                    newRegistration.registrationId,
                    restarted.activeRegistrations("user-a", sandboxSameTopic).single().registrationId
                )
            }
        }
    }

    @Test
    fun invalidationAndReregistrationPersistEveryLifecycleColumnAcrossReopen() = runBlocking {
        withConfiguredRuntime { factory, _, _ ->
            lateinit var iphone: BackendDeviceRegistration
            lateinit var ipad: BackendDeviceRegistration
            lateinit var invalidated: BackendDeviceRegistration
            lateinit var replacement: BackendDeviceRegistration
            factory.open().use { store ->
                iphone = store.register(request("iphone", "user-a", "invalid-token", at = 100))
                ipad = store.register(request("ipad", "user-a", "still-valid-token", at = 110))
                invalidated = store.invalidate(
                    registrationId = iphone.registrationId,
                    authenticatedUserId = "user-a",
                    reason = DeviceRegistrationInvalidationReason.BAD_DEVICE_TOKEN,
                    atEpochSeconds = 130
                )
            }

            factory.open().use { restartedAfterInvalidation ->
                assertRegistration(restartedAfterInvalidation.registration(iphone.registrationId), invalidated)
                assertRegistration(restartedAfterInvalidation.registration(ipad.registrationId), ipad)
                assertNull(restartedAfterInvalidation.activeRegistration("iphone"))
                replacement = restartedAfterInvalidation.register(
                    request("iphone", "user-a", "replacement-token", at = 140)
                )
                assertNotEquals(iphone.registrationId, replacement.registrationId)
            }

            factory.open().use { restartedAfterReregistration ->
                val history = restartedAfterReregistration.registrationHistory("iphone")
                assertEquals(listOf(invalidated, replacement), history)
                assertRegistration(restartedAfterReregistration.registration(invalidated.registrationId), invalidated)
                assertRegistration(restartedAfterReregistration.registration(replacement.registrationId), replacement)
                assertRegistration(restartedAfterReregistration.registration(ipad.registrationId), ipad)
                assertRegistration(restartedAfterReregistration.activeRegistration("iphone"), replacement)
                assertEquals(2, restartedAfterReregistration.activeRegistrations("user-a", productionScope()).size)
            }
        }
    }

    @Test
    fun installationPlatformIsCanonicalAndCannotBeChangedByARegistration() = runBlocking {
        withConfiguredRuntime { factory, _, _ ->
            lateinit var ios: BackendDeviceRegistration
            factory.open().use { store ->
                ios = store.register(request("shared-installation", "user-a", "ios-token", at = 100))
                val rejected = assertFailsWith<IllegalArgumentException> {
                    runBlocking {
                        store.register(
                            request(
                                installationId = "shared-installation",
                                userId = "user-a",
                                rawToken = "android-token",
                                at = 120,
                                platform = Platform.ANDROID
                            )
                        )
                    }
                }
                assertSanitized(rejected, "android-token", "ios-token")
            }
            factory.open().use { restarted ->
                assertInstallation(restarted.installation("shared-installation"), "shared-installation", Platform.IOS, 100)
                assertRegistration(restarted.activeRegistration("shared-installation"), ios)
                assertEquals(listOf(ios), restarted.registrationHistory("shared-installation"))
            }
        }
    }

    @Test
    fun legacyBackfillIsReopenIdempotentAndExcludesRawTokenFromBothIdentities() = runBlocking {
        val first = legacyIds(
            hmacKey = HMAC_KEY_A,
            legacyRowKey = "legacy-row-42",
            firstRawToken = "legacy-token-a1",
            secondRawToken = "legacy-token-a2"
        )
        val sameKeyAndRowWithOtherTokens = legacyIds(
            hmacKey = HMAC_KEY_A,
            legacyRowKey = "legacy-row-42",
            firstRawToken = "legacy-token-b1",
            secondRawToken = "legacy-token-b2"
        )
        val differentHmacKey = legacyIds(
            hmacKey = HMAC_KEY_B,
            legacyRowKey = "legacy-row-42",
            firstRawToken = "legacy-token-c1",
            secondRawToken = "legacy-token-c2"
        )
        val differentRow = legacyIds(
            hmacKey = HMAC_KEY_A,
            legacyRowKey = "legacy-row-43",
            firstRawToken = "legacy-token-d1",
            secondRawToken = "legacy-token-d2"
        )

        assertEquals(first, sameKeyAndRowWithOtherTokens)
        assertNotEquals(first, differentHmacKey, "an unkeyed identity derivation must not satisfy the contract")
        assertNotEquals(first, differentRow, "the immutable legacy primary key is part of the identity")
    }

    @Test
    fun rollbackProjectionIsDeterministicTokenlessAndDoesNotMutateAssociations() = runBlocking {
        val tokenA = "rollback-secret-token-a"
        val tokenB = "rollback-secret-token-b"
        withConfiguredRuntime { factory, databasePath, _ ->
            lateinit var registrationA: BackendDeviceRegistration
            lateinit var registrationB: BackendDeviceRegistration
            lateinit var historiesBeforeRead: Map<String, List<BackendDeviceRegistration>>
            factory.open().use { store ->
                registrationA = store.register(request("iphone", "user-a", tokenA, at = 100))
                registrationB = store.register(request("ipad", "user-a", tokenB, at = 100))
            }

            val expectedRegistrationId = minOf(registrationA.registrationId, registrationB.registrationId)
            val expectedToken = if (expectedRegistrationId == registrationA.registrationId) tokenA else tokenB
            factory.open().use { restarted ->
                historiesBeforeRead = mapOf(
                    "iphone" to restarted.registrationHistory("iphone"),
                    "ipad" to restarted.registrationHistory("ipad")
                )
                val rollbackRead = assertNotNull(
                    restarted.legacyReadCompatibility("user-a", Platform.IOS, productionScope())
                )
                assertEquals(expectedRegistrationId, rollbackRead.selectedRegistrationId)
                assertEquals(productionScope(), rollbackRead.scope)
                assertEquals(DeviceRegistrationStatus.ACTIVE, rollbackRead.status)
                assertTokenlessRollbackDto(rollbackRead, tokenA, tokenB)
                assertEquals(historiesBeforeRead["iphone"], restarted.registrationHistory("iphone"))
                assertEquals(historiesBeforeRead["ipad"], restarted.registrationHistory("ipad"))
            }

            factory.open().use { restartedAfterRead ->
                assertEquals(historiesBeforeRead["iphone"], restartedAfterRead.registrationHistory("iphone"))
                assertEquals(historiesBeforeRead["ipad"], restartedAfterRead.registrationHistory("ipad"))
                assertRegistration(restartedAfterRead.registration(registrationA.registrationId), registrationA)
                assertRegistration(restartedAfterRead.registration(registrationB.registrationId), registrationB)
            }
            factory.openProviderTokenPort().use { providerTokens ->
                assertEquals(
                    true,
                    providerTokens.withDecryptedToken(expectedRegistrationId) { token -> token == expectedToken }
                )
            }
            assertDatabaseFilesDoNotContain(databasePath, tokenA, tokenB)
        }
    }

    private suspend fun legacyIds(
        hmacKey: String,
        legacyRowKey: String,
        firstRawToken: String,
        secondRawToken: String
    ): Pair<String, String> {
        lateinit var ids: Pair<String, String>
        lateinit var firstInstallation: BackendDeviceInstallation
        lateinit var firstRegistration: BackendDeviceRegistration
        withConfiguredRuntime(hmacKey = hmacKey) { factory, databasePath, _ ->
            val firstRequest = legacyRequest(legacyRowKey, firstRawToken, at = 100)
            factory.open().use { store ->
                val first = store.backfillLegacy(firstRequest)
                assertTrue(first.created)
                firstInstallation = first.installation
                firstRegistration = first.registration
                ids = firstInstallation.installationId to firstRegistration.registrationId
            }

            factory.open().use { restarted ->
                val duplicateAfterReopen = restarted.backfillLegacy(firstRequest)
                assertFalse(duplicateAfterReopen.created)
                assertEquals(ids.first, duplicateAfterReopen.installation.installationId)
                assertEquals(ids.second, duplicateAfterReopen.registration.registrationId)
                assertInstallation(restarted.installation(ids.first), firstInstallation)
                assertRegistration(restarted.registration(ids.second), firstRegistration)
            }

            factory.open().use { restartedWithDifferentToken ->
                val differentToken = restartedWithDifferentToken.backfillLegacy(
                    legacyRequest(legacyRowKey, secondRawToken, at = 120)
                )
                assertFalse(differentToken.created)
                assertEquals(ids.first, differentToken.installation.installationId)
                assertEquals(ids.second, differentToken.registration.registrationId)
            }
            factory.open().use { finalRestart ->
                assertEquals(ids.first, finalRestart.installation(ids.first)?.installationId)
                assertEquals(ids.second, finalRestart.registration(ids.second)?.registrationId)
            }
            assertDatabaseFilesDoNotContain(databasePath, firstRawToken, secondRawToken)
        }
        return ids
    }

    private fun legacyRequest(
        legacyRowKey: String,
        rawToken: String,
        at: Long
    ): LegacyNotificationTokenBackfill = LegacyNotificationTokenBackfill.create(
        legacyRowKey = legacyRowKey,
        userId = "user-a",
        platform = Platform.IOS,
        rawToken = rawToken,
        scope = productionScope(),
        updatedAtEpochSeconds = at
    ).getOrThrow().also { assertSanitized(it, rawToken, HMAC_KEY_A, HMAC_KEY_B) }

    private fun request(
        installationId: String,
        userId: String,
        rawToken: String,
        at: Long,
        scope: DeviceRegistrationScope = productionScope(),
        platform: Platform = Platform.IOS
    ): BackendDeviceRegistrationRequest = BackendDeviceRegistrationRequest.create(
        installationId = installationId,
        authenticatedUserId = userId,
        platform = platform,
        scope = scope,
        rawToken = rawToken,
        registeredAtEpochSeconds = at
    ).getOrThrow().also { assertSanitized(it, rawToken) }

    private fun productionScope(): DeviceRegistrationScope = scope(
        APNsEnvironment.PRODUCTION,
        "com.guyghost.wakeve"
    )

    private fun scope(environment: APNsEnvironment, topic: String): DeviceRegistrationScope =
        DeviceRegistrationScope.create(environment, topic).getOrThrow()

    private fun assertInstallation(
        actual: BackendDeviceInstallation?,
        installationId: String,
        platform: Platform,
        createdAt: Long
    ) {
        val installation = assertNotNull(actual)
        assertEquals(installationId, installation.installationId)
        assertEquals(platform, installation.platform)
        assertEquals(createdAt, installation.createdAtEpochSeconds)
        assertTrue(installation.updatedAtEpochSeconds >= createdAt)
    }

    private fun assertInstallation(
        actual: BackendDeviceInstallation?,
        expected: BackendDeviceInstallation
    ) {
        val installation = assertNotNull(actual)
        assertEquals(expected.installationId, installation.installationId)
        assertEquals(expected.platform, installation.platform)
        assertEquals(expected.createdAtEpochSeconds, installation.createdAtEpochSeconds)
        assertEquals(expected.updatedAtEpochSeconds, installation.updatedAtEpochSeconds)
        assertSanitized(installation)
    }

    private fun assertRegistration(actual: BackendDeviceRegistration?, expected: BackendDeviceRegistration) {
        val registration = assertNotNull(actual)
        assertEquals(expected.registrationId, registration.registrationId)
        assertEquals(expected.installationId, registration.installationId)
        assertEquals(expected.userId, registration.userId)
        assertEquals(expected.scope, registration.scope)
        assertEquals(expected.tokenHash, registration.tokenHash)
        assertEquals(expected.status, registration.status)
        assertEquals(expected.createdAtEpochSeconds, registration.createdAtEpochSeconds)
        assertEquals(expected.updatedAtEpochSeconds, registration.updatedAtEpochSeconds)
        assertEquals(expected.lastRegisteredAtEpochSeconds, registration.lastRegisteredAtEpochSeconds)
        assertEquals(expected.invalidatedAtEpochSeconds, registration.invalidatedAtEpochSeconds)
        assertEquals(expected.invalidationReason, registration.invalidationReason)
        assertEquals(expected.unregisteredAtEpochSeconds, registration.unregisteredAtEpochSeconds)
        assertEquals(expected.unregisteredReason, registration.unregisteredReason)
        assertTokenlessRegistrationDto(registration)
    }

    private fun assertTokenlessRegistrationDto(registration: BackendDeviceRegistration, vararg forbidden: String) {
        assertTrue(registration.tokenHash.isNotBlank())
        assertEquals(
            setOf(
                "registrationId", "installationId", "userId", "scope", "tokenHash", "status",
                "createdAtEpochSeconds", "updatedAtEpochSeconds", "lastRegisteredAtEpochSeconds",
                "invalidatedAtEpochSeconds", "invalidationReason", "unregisteredAtEpochSeconds",
                "unregisteredReason"
            ),
            BackendDeviceRegistration::class.java.declaredFields.filterNot { it.isSynthetic }.map { it.name }.toSet(),
            "ordinary registration reads must never expose token or ciphertext fields"
        )
        assertSanitized(registration, *forbidden)
    }

    private fun assertTokenlessRollbackDto(read: LegacyNotificationTokenRead, vararg forbidden: String) {
        assertEquals(
            setOf(
                "selectedRegistrationId", "tokenHash", "platform", "scope", "status",
                "createdAtEpochSeconds", "lastRegisteredAtEpochSeconds"
            ),
            LegacyNotificationTokenRead::class.java.declaredFields.filterNot { it.isSynthetic }.map { it.name }.toSet(),
            "rollback compatibility is metadata-only; provider secret access is a separate port"
        )
        assertSanitized(read, *forbidden)
    }

    private fun assertDatabaseFilesDoNotContain(databasePath: Path, vararg forbidden: String) {
        assertTrue(Files.isRegularFile(databasePath), "the production store must create a durable SQLite database")
        val sqliteHeader = Files.readAllBytes(databasePath).take(16).toByteArray()
        assertEquals(
            "SQLite format 3\u0000",
            String(sqliteHeader, StandardCharsets.US_ASCII),
            "a process-local or fake store must not satisfy the durability contract"
        )
        Files.list(databasePath.parent).use { paths ->
            paths.filter { it.name.startsWith(databasePath.name) }.forEach { file ->
                val bytes = String(Files.readAllBytes(file), StandardCharsets.ISO_8859_1)
                forbidden.forEach { raw -> assertFalse(bytes.contains(raw), "raw token leaked at rest in ${file.fileName}") }
            }
        }
    }

    private fun assertSanitized(value: Any, vararg forbidden: String) {
        forbidden.forEach { raw ->
            assertFalse(value.toString().contains(raw), "diagnostic output leaked protected material")
        }
    }

    private fun assertSanitizedConfigurationFailure(
        systemProperties: Map<String, String>,
        expectedReason: String
    ) {
        val failure = assertNotNull(
            DeviceRegistrationStoreConfiguration.resolve(
                environment = emptyMap(),
                systemProperties = systemProperties
            ).exceptionOrNull()
        )
        assertIs<IllegalStateException>(failure)
        assertTrue(failure.message.orEmpty().contains(expectedReason, ignoreCase = true))
        assertSanitized(failure, HMAC_KEY_A, HMAC_KEY_B, TOKEN_ENCRYPTION_KEY)
    }

    private fun explicitConfiguration(
        databasePath: Path,
        hmacKey: String
    ): DeviceRegistrationStoreConfiguration = DeviceRegistrationStoreConfiguration.resolve(
        environment = emptyMap(),
        systemProperties = mapOf(
            DATABASE_PATH_PROPERTY to databasePath.toString(),
            LEGACY_HMAC_KEY_PROPERTY to hmacKey,
            TOKEN_ENCRYPTION_KEY_PROPERTY to TOKEN_ENCRYPTION_KEY
        )
    ).getOrThrow()

    private suspend fun withConfiguredRuntime(
        hmacKey: String = HMAC_KEY_A,
        block: suspend (
            BackendDeviceRegistrationStoreFactory,
            Path,
            DeviceRegistrationStoreConfiguration
        ) -> Unit
    ) {
        val directory = Files.createTempDirectory("wakeve-device-registration-contract-")
        val databasePath = directory.resolve("registration.sqlite")
        val configuration = explicitConfiguration(databasePath, hmacKey)
        val factory = SqliteBackendDeviceRegistrationStoreFactory(configuration = configuration)
        try {
            block(factory, databasePath, configuration)
        } finally {
            Files.walk(directory).sorted(Comparator.reverseOrder<Path>()).use { paths ->
                paths.forEach { Files.deleteIfExists(it) }
            }
        }
    }

    private class InjectedRegistrationTransactionFailure : RuntimeException("injected transaction failure")

    private companion object {
        const val DATABASE_PATH_PROPERTY = "wakeve.notification.device-registration.db.path"
        const val LEGACY_HMAC_KEY_PROPERTY = "wakeve.notification.device-registration.legacy-identity-hmac-key"
        const val TOKEN_ENCRYPTION_KEY_PROPERTY = "wakeve.notification.device-registration.token-encryption-key"
        const val HMAC_KEY_A = "test-only-hmac-key-a-with-at-least-32-bytes"
        const val HMAC_KEY_B = "test-only-hmac-key-b-with-at-least-32-bytes"
        const val TOKEN_ENCRYPTION_KEY = "test-only-encryption-key-with-at-least-32-bytes"
    }
}
