package com.guyghost.wakeve.notification

import java.nio.charset.StandardCharsets
import java.nio.file.FileSystemException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFileAttributeView
import java.nio.file.attribute.PosixFilePermission
import java.sql.DriverManager
import java.sql.SQLException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Security RED gate for OpenSpec task 3.1.
 *
 * These tests deliberately exercise the real SQLite adapter and its provider-only token port.
 * They neither replace the durable store with an in-memory fake nor weaken the approved 3.1
 * behavior contract.
 */
class BackendDeviceRegistrationSecurityContractRedTest {
    @Test
    fun providerCallbackResultCannotReturnTheRawToken() = runBlocking {
        val rawToken = "provider-scope-secret-token"
        withConfiguredRuntime { factory, _, _ ->
            val registration = factory.open().use { store ->
                store.register(request("provider-phone", "user-a", rawToken, at = 100))
            }

            factory.openProviderTokenPort().use { providerTokenPort ->
                val externallyReturned: Any = providerTokenPort.withDecryptedToken(
                    registration.registrationId
                ) { decryptedToken ->
                    decryptedToken
                }

                assertNotEquals(
                    rawToken,
                    externallyReturned,
                    "the provider port must return a fixed tokenless result, never the callback result"
                )
                assertFalse(
                    externallyReturned.toString().contains(rawToken),
                    "a provider completion/receipt must not expose the decrypted token"
                )
            }
        }
    }

    @Test
    fun closeWaitsForConcurrentRegistrationBeforeDestroyingKeyMaterial() = runBlocking {
        withConfiguredRuntime { factory, _, configuration ->
            factory.open().use { store ->
                store.register(request("shared-phone", "user-a", "account-a-token", at = 100))
            }

            val provisionalClosureReached = CountDownLatch(1)
            val releaseRegistration = CountDownLatch(1)
            val faultingFactory = SqliteBackendDeviceRegistrationStoreFactory(
                configuration = configuration,
                transactionFaultInjector = DeviceRegistrationTransactionFaultInjector { point ->
                    if (point == DeviceRegistrationTransactionFaultPoint.AFTER_ACTIVE_REGISTRATION_CLOSED) {
                        provisionalClosureReached.countDown()
                        check(releaseRegistration.await(5, TimeUnit.SECONDS)) {
                            "timed out waiting to release the registration transaction"
                        }
                    }
                }
            )
            val store = faultingFactory.open()
            val registrationResult = AtomicReference<Result<BackendDeviceRegistration>?>(null)
            val closeResult = AtomicReference<Result<Unit>?>(null)
            val closeThreadStarted = CountDownLatch(1)

            val registrationThread = thread(name = "registration-security-contract") {
                registrationResult.set(
                    runCatching {
                        runBlocking {
                            store.register(
                                request("shared-phone", "user-b", "account-b-token", at = 120)
                            )
                        }
                    }
                )
            }
            assertTrue(
                provisionalClosureReached.await(5, TimeUnit.SECONDS),
                "the deterministic transaction seam was not reached"
            )
            val closeThread = thread(name = "registration-close-security-contract") {
                closeThreadStarted.countDown()
                closeResult.set(runCatching { store.close() })
            }

            assertTrue(
                closeThreadStarted.await(5, TimeUnit.SECONDS),
                "the store close thread did not start immediately before the real close call"
            )
            val closeBlockedOnSharedMonitor = awaitThreadState(
                thread = closeThread,
                expected = Thread.State.BLOCKED,
                timeout = 2,
                unit = TimeUnit.SECONDS
            )
            releaseRegistration.countDown()
            registrationThread.join(5_000)
            closeThread.join(5_000)

            assertFalse(registrationThread.isAlive, "registration worker did not terminate")
            assertTrue(
                closeBlockedOnSharedMonitor.reachedExpected,
                "store close never blocked on the real shared monitor: " +
                    closeBlockedOnSharedMonitor.diagnostic()
            )
            assertEquals(
                Thread.State.TERMINATED,
                closeThread.state,
                "store close must terminate after the registration releases the shared monitor"
            )
            val replacement = assertNotNull(registrationResult.get()).getOrThrow()
            assertNotNull(closeResult.get()).getOrThrow()

            factory.open().use { restarted ->
                val active = assertNotNull(restarted.activeRegistration("shared-phone"))
                assertEquals(replacement.registrationId, active.registrationId)
                assertEquals("user-b", active.userId)
                assertEquals(1, restarted.registrationHistory("shared-phone").count {
                    it.status == DeviceRegistrationStatus.ACTIVE
                })
            }
            factory.openProviderTokenPort().use { restartedProvider ->
                var observedReplacement = false
                restartedProvider.withDecryptedToken(replacement.registrationId) { token ->
                    observedReplacement = token == "account-b-token"
                }
                assertTrue(
                    observedReplacement,
                    "reopen must decrypt the coherently committed replacement with configured key material"
                )
            }
        }
    }

    @Test
    fun providerCloseWaitsForConcurrentScopedDecryption() = runBlocking {
        withConfiguredRuntime { factory, _, _ ->
            val rawToken = "concurrent-provider-secret"
            val registration = factory.open().use { store ->
                store.register(request("provider-phone", "user-a", rawToken, at = 100))
            }
            val providerTokenPort = factory.openProviderTokenPort()
            val callbackEntered = CountDownLatch(1)
            val releaseCallback = CountDownLatch(1)
            val providerResult = AtomicReference<Result<Any?>?>(null)
            val callbackObservedToken = AtomicBoolean(false)
            val closeResult = AtomicReference<Result<Unit>?>(null)
            val closeThreadStarted = CountDownLatch(1)

            val providerThread = thread(name = "provider-decrypt-security-contract") {
                providerResult.set(
                    runCatching {
                        runBlocking {
                            providerTokenPort.withDecryptedToken(registration.registrationId) { token ->
                                callbackObservedToken.set(token == rawToken)
                                callbackEntered.countDown()
                                check(releaseCallback.await(5, TimeUnit.SECONDS)) {
                                    "timed out waiting to release the provider callback"
                                }
                            }
                        }
                    }
                )
            }
            assertTrue(
                callbackEntered.await(5, TimeUnit.SECONDS),
                "the provider callback did not reach the deterministic barrier"
            )
            val closeThread = thread(name = "provider-close-security-contract") {
                closeThreadStarted.countDown()
                closeResult.set(runCatching { providerTokenPort.close() })
            }

            assertTrue(
                closeThreadStarted.await(5, TimeUnit.SECONDS),
                "the provider close thread did not start immediately before the real close call"
            )
            val closeBlockedOnSharedMonitor = awaitThreadState(
                thread = closeThread,
                expected = Thread.State.BLOCKED,
                timeout = 2,
                unit = TimeUnit.SECONDS
            )
            releaseCallback.countDown()
            providerThread.join(5_000)
            closeThread.join(5_000)

            assertFalse(providerThread.isAlive, "provider worker did not terminate")
            assertTrue(
                closeBlockedOnSharedMonitor.reachedExpected,
                "provider close never blocked on the real shared monitor: " +
                    closeBlockedOnSharedMonitor.diagnostic()
            )
            assertEquals(
                Thread.State.TERMINATED,
                closeThread.state,
                "provider close must terminate after the callback releases the shared monitor"
            )
            assertTrue(callbackObservedToken.get(), "the selected token was not observed inside its scope")
            assertNotNull(providerResult.get()).getOrThrow()
            assertNotNull(closeResult.get()).getOrThrow()

            factory.openProviderTokenPort().use { restartedProvider ->
                var observedAfterReopen = false
                restartedProvider.withDecryptedToken(registration.registrationId) { token ->
                    observedAfterReopen = token == rawToken
                }
                assertTrue(observedAfterReopen, "provider reopen must retain a decryptable durable token")
            }
        }
    }

    @Test
    fun sqlConstraintsRejectEveryContradictoryLifecycleCombination() = runBlocking {
        withConfiguredRuntime { factory, databasePath, _ ->
            val registration = factory.open().use { store ->
                store.register(request("sql-phone", "user-a", "sql-secret-token", at = 100))
            }
            val invalidMutations = linkedMapOf(
                "ACTIVE with invalidated_at" to
                    "status='ACTIVE', invalidated_at_epoch_seconds=101",
                "ACTIVE with invalidation_reason" to
                    "status='ACTIVE', invalidation_reason='BAD_DEVICE_TOKEN'",
                "ACTIVE with unregistration fields" to
                    "status='ACTIVE', unregistered_at_epoch_seconds=101, unregistered_reason='LOGOUT'",
                "INVALID missing invalidated_at" to
                    "status='INVALID', invalidation_reason='BAD_DEVICE_TOKEN'",
                "INVALID missing invalidation_reason" to
                    "status='INVALID', invalidated_at_epoch_seconds=101",
                "INVALID with unregistration fields" to
                    "status='INVALID', invalidated_at_epoch_seconds=101, " +
                        "invalidation_reason='BAD_DEVICE_TOKEN', unregistered_at_epoch_seconds=101, " +
                        "unregistered_reason='LOGOUT'",
                "INVALID with unknown reason" to
                    "status='INVALID', invalidated_at_epoch_seconds=101, invalidation_reason='FREE_TEXT'",
                "UNREGISTERED missing unregistered_at" to
                    "status='UNREGISTERED', unregistered_reason='LOGOUT'",
                "UNREGISTERED missing unregistered_reason" to
                    "status='UNREGISTERED', unregistered_at_epoch_seconds=101",
                "UNREGISTERED with invalidation fields" to
                    "status='UNREGISTERED', unregistered_at_epoch_seconds=101, " +
                        "unregistered_reason='LOGOUT', invalidated_at_epoch_seconds=101, " +
                        "invalidation_reason='BAD_DEVICE_TOKEN'",
                "UNREGISTERED with unknown reason" to
                    "status='UNREGISTERED', unregistered_at_epoch_seconds=101, " +
                        "unregistered_reason='FREE_TEXT'"
            )
            val acceptedInvalidMutations = mutableListOf<String>()

            DriverManager.getConnection("jdbc:sqlite:$databasePath").use { connection ->
                connection.autoCommit = false
                invalidMutations.forEach { (label, mutation) ->
                    val result = runCatching {
                        connection.prepareStatement(
                            "UPDATE device_registration SET $mutation WHERE registration_id = ?"
                        ).use { statement ->
                            statement.setString(1, registration.registrationId)
                            assertEquals(1, statement.executeUpdate(), "fixture row must be updated")
                        }
                    }
                    connection.rollback()
                    when (val failure = result.exceptionOrNull()) {
                        null -> acceptedInvalidMutations += label
                        is SQLException -> Unit
                        else -> throw failure
                    }
                }
            }

            assertTrue(
                acceptedInvalidMutations.isEmpty(),
                "SQL lifecycle constraints accepted contradictory rows: $acceptedInvalidMutations"
            )
        }
    }

    @Test
    fun configurationRejectsRelativeMemoryAndFileUriPaths() {
        val unsafePaths = listOf(
            "relative/registration.sqlite",
            ":memory:",
            "file:///tmp/wakeve-registration-security.sqlite"
        )
        val accepted = unsafePaths.filter { path ->
            configuration(path).isSuccess
        }

        assertTrue(accepted.isEmpty(), "unsafe durable database paths were accepted: $accepted")
    }

    @Test
    fun configurationRejectsDatabaseAndParentSymlinks() {
        val directory = Files.createTempDirectory("wakeve-registration-symlink-contract-")
        try {
            val targetDatabase = Files.createFile(directory.resolve("target.sqlite"))
            val databaseLink = directory.resolve("database-link.sqlite")
            val realParent = Files.createDirectory(directory.resolve("real-parent"))
            val parentLink = directory.resolve("parent-link")
            try {
                Files.createSymbolicLink(databaseLink, targetDatabase.fileName)
                Files.createSymbolicLink(parentLink, realParent.fileName)
            } catch (_: UnsupportedOperationException) {
                return
            } catch (_: FileSystemException) {
                return
            } catch (_: SecurityException) {
                return
            }

            val accepted = listOf(
                databaseLink,
                parentLink.resolve("registration.sqlite")
            ).filter { path -> configuration(path.toString()).isSuccess }

            assertTrue(accepted.isEmpty(), "symbolic-link database paths were accepted: $accepted")
        } finally {
            deleteRecursively(directory)
        }
    }

    @Test
    fun sqliteFilesAndParentAreCreatedOwnerOnlyOnPosixFilesystems() {
        val root = Files.createTempDirectory("wakeve-registration-permissions-contract-")
        try {
            if (Files.getFileAttributeView(root, PosixFileAttributeView::class.java) == null) return
            val directory = root.resolve("private-store")
            val databasePath = directory.resolve("registration.sqlite")
            val resolved = configuration(databasePath.toString()).getOrThrow()

            SqliteBackendDeviceRegistrationStoreFactory(resolved).open().close()

            assertEquals(
                OWNER_DIRECTORY_PERMISSIONS,
                Files.getPosixFilePermissions(directory),
                "device-registration database parent must be owner-only"
            )
            assertEquals(
                OWNER_FILE_PERMISSIONS,
                Files.getPosixFilePermissions(databasePath),
                "device-registration database file must be owner-only"
            )
        } finally {
            deleteRecursively(root)
        }
    }

    @Test
    fun preExistingBroadPosixPermissionsAreRejectedOrMadeOwnerOnlyBeforeUse() {
        val root = Files.createTempDirectory("wakeve-registration-permissions-verification-contract-")
        try {
            if (Files.getFileAttributeView(root, PosixFileAttributeView::class.java) == null) return
            val insecureCases = mutableListOf<String>()

            val broadParent = Files.createDirectory(root.resolve("broad-parent-store"))
            Files.setPosixFilePermissions(broadParent, PosixFilePermission.entries.toSet())
            val databaseCreatedByStore = broadParent.resolve("registration.sqlite")
            if (!isRejectedOrNormalizedOwnerOnly(databaseCreatedByStore)) {
                insecureCases += "broad pre-existing parent was accepted without 0700/0600 normalization"
            }

            val ownerOnlyParent = Files.createDirectory(root.resolve("pre-existing-sqlite-store"))
            Files.setPosixFilePermissions(ownerOnlyParent, OWNER_DIRECTORY_PERMISSIONS)
            val preExistingDatabase = ownerOnlyParent.resolve("registration.sqlite")
            DriverManager.getConnection("jdbc:sqlite:$preExistingDatabase").use { connection ->
                connection.createStatement().use { statement ->
                    statement.execute("CREATE TABLE pre_existing_permission_fixture(id INTEGER PRIMARY KEY)")
                }
            }
            Files.newInputStream(preExistingDatabase).use { input ->
                assertEquals(
                    "SQLite format 3\u0000",
                    String(input.readNBytes(SQLITE_HEADER_BYTES), StandardCharsets.US_ASCII),
                    "the permission fixture must be a real SQLite database"
                )
            }
            Files.setPosixFilePermissions(preExistingDatabase, BROAD_FILE_PERMISSIONS)
            if (!isRejectedOrNormalizedOwnerOnly(preExistingDatabase)) {
                insecureCases += "pre-existing SQLite file chmod 0666 was accepted without 0600 normalization"
            }

            assertTrue(
                insecureCases.isEmpty(),
                "unsafe pre-existing POSIX permissions remained usable: $insecureCases"
            )
        } finally {
            deleteRecursively(root)
        }
    }

    private fun isRejectedOrNormalizedOwnerOnly(databasePath: Path): Boolean {
        val resolved = configuration(databasePath.toString()).getOrNull() ?: return true
        val opened = runCatching {
            SqliteBackendDeviceRegistrationStoreFactory(resolved).open().close()
        }
        if (opened.isFailure) return true
        return Files.getPosixFilePermissions(databasePath.parent) == OWNER_DIRECTORY_PERMISSIONS &&
            Files.getPosixFilePermissions(databasePath) == OWNER_FILE_PERMISSIONS
    }

    private fun awaitThreadState(
        thread: Thread,
        expected: Thread.State,
        timeout: Long,
        unit: TimeUnit
    ): ThreadStateObservation {
        val deadline = System.nanoTime() + unit.toNanos(timeout)
        var lastState = thread.state
        var lastNonEmptyStack = thread.stackTrace.toList()
        while (System.nanoTime() < deadline) {
            lastState = thread.state
            val observedStack = thread.stackTrace.toList()
            if (observedStack.isNotEmpty()) lastNonEmptyStack = observedStack
            if (lastState == expected) {
                return ThreadStateObservation(true, lastState, observedStack)
            }
            if (lastState == Thread.State.TERMINATED) break
            TimeUnit.MILLISECONDS.sleep(1)
        }
        return ThreadStateObservation(false, lastState, lastNonEmptyStack)
    }

    private fun request(
        installationId: String,
        userId: String,
        token: String,
        at: Long
    ): BackendDeviceRegistrationRequest = BackendDeviceRegistrationRequest.create(
        installationId = installationId,
        authenticatedUserId = userId,
        platform = Platform.IOS,
        scope = DeviceRegistrationScope.create(
            APNsEnvironment.PRODUCTION,
            "com.guyghost.wakeve"
        ).getOrThrow(),
        rawToken = token,
        registeredAtEpochSeconds = at
    ).getOrThrow()

    private fun configuration(path: String): Result<DeviceRegistrationStoreConfiguration> =
        DeviceRegistrationStoreConfiguration.resolve(
            environment = emptyMap(),
            systemProperties = mapOf(
                DATABASE_PATH_PROPERTY to path,
                LEGACY_HMAC_KEY_PROPERTY to HMAC_KEY,
                TOKEN_ENCRYPTION_KEY_PROPERTY to TOKEN_ENCRYPTION_KEY
            )
        )

    private suspend fun withConfiguredRuntime(
        block: suspend (
            BackendDeviceRegistrationStoreFactory,
            Path,
            DeviceRegistrationStoreConfiguration
        ) -> Unit
    ) {
        val directory = Files.createTempDirectory("wakeve-registration-security-contract-")
        val databasePath = directory.resolve("registration.sqlite")
        val configuration = configuration(databasePath.toString()).getOrThrow()
        val factory = SqliteBackendDeviceRegistrationStoreFactory(configuration)
        try {
            block(factory, databasePath, configuration)
        } finally {
            deleteRecursively(directory)
        }
    }

    private fun deleteRecursively(directory: Path) {
        Files.walk(directory).sorted(Comparator.reverseOrder<Path>()).use { paths ->
            paths.forEach { Files.deleteIfExists(it) }
        }
    }

    private companion object {
        const val DATABASE_PATH_PROPERTY = "wakeve.notification.device-registration.db.path"
        const val LEGACY_HMAC_KEY_PROPERTY =
            "wakeve.notification.device-registration.legacy-identity-hmac-key"
        const val TOKEN_ENCRYPTION_KEY_PROPERTY =
            "wakeve.notification.device-registration.token-encryption-key"
        const val HMAC_KEY = "security-contract-hmac-key-with-at-least-32-bytes"
        const val TOKEN_ENCRYPTION_KEY = "security-contract-encryption-key-with-at-least-32-bytes"

        val OWNER_DIRECTORY_PERMISSIONS = setOf(
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE,
            PosixFilePermission.OWNER_EXECUTE
        )
        val OWNER_FILE_PERMISSIONS = setOf(
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE
        )
        val BROAD_FILE_PERMISSIONS = setOf(
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE,
            PosixFilePermission.GROUP_READ,
            PosixFilePermission.GROUP_WRITE,
            PosixFilePermission.OTHERS_READ,
            PosixFilePermission.OTHERS_WRITE
        )
        const val SQLITE_HEADER_BYTES = 16
    }
}

private data class ThreadStateObservation(
    val reachedExpected: Boolean,
    val state: Thread.State,
    val stack: List<StackTraceElement>
) {
    fun diagnostic(): String = "state=$state, stack=" +
        stack.joinToString(prefix = "[", postfix = "]") { frame ->
            "${frame.className}.${frame.methodName}:${frame.lineNumber}"
        }
}
