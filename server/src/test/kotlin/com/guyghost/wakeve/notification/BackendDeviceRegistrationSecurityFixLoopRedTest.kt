package com.guyghost.wakeve.notification

import java.nio.file.FileSystemException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFileAttributeView
import java.nio.file.attribute.PosixFilePermission
import java.sql.DriverManager
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Additional security RED gate discovered after the approved 20-test task 3.1 baseline. */
class BackendDeviceRegistrationSecurityFixLoopRedTest {
    @Test
    fun ancestorSwapAfterFinalPathValidationBeforeJdbcOpenFailsClosedWithoutTouchingAttacker() {
        val root = Files.createTempDirectory("wakeve-registration-jdbc-open-toctou-contract-")
        try {
            val attackerAncestor = Files.createDirectory(root.resolve("attacker-ancestor"))
            val attackerParent = Files.createDirectory(attackerAncestor.resolve("database-parent"))
            Files.writeString(attackerParent.resolve("marker.txt"), "attacker-target-must-remain-unchanged")
            if (supportsPosix(attackerParent)) {
                Files.setPosixFilePermissions(attackerParent, BROAD_DIRECTORY_PERMISSIONS)
            }
            val linkProbe = root.resolve("symlink-support-probe")
            if (!createSymbolicLinkOrSkip(linkProbe, attackerAncestor.fileName)) return
            Files.delete(linkProbe)

            val trustedAncestor = Files.createDirectory(root.resolve("trusted-ancestor"))
            val configuredParent = Files.createDirectory(trustedAncestor.resolve("database-parent"))
            if (supportsPosix(configuredParent)) {
                Files.setPosixFilePermissions(configuredParent, OWNER_DIRECTORY_PERMISSIONS)
            }
            val configuredDatabase = configuredParent.resolve("registration.sqlite")
            val attackerBefore = directorySnapshot(attackerParent)
            var swapInjected = false

            val openResult = runCatching {
                SqliteBackendDeviceRegistrationStoreFactory(
                    configuration = configuration(configuredDatabase).getOrThrow(),
                    transactionFaultInjector = DeviceRegistrationTransactionFaultInjector { },
                    jdbcOpenTestHook = DeviceRegistrationJdbcOpenTestHook {
                        Files.delete(configuredDatabase)
                        Files.delete(configuredParent)
                        Files.delete(trustedAncestor)
                        Files.createSymbolicLink(trustedAncestor, attackerAncestor.fileName)
                        swapInjected = true
                    }
                ).open().close()
            }

            assertTrue(swapInjected, "the contract hook must run after final validation and before JDBC open")
            assertTrue(openResult.isFailure, "JDBC must fail closed after the validated ancestor is swapped")
            assertEquals(
                attackerBefore,
                directorySnapshot(attackerParent),
                "the attacker parent must not be chmoded or receive a SQLite database"
            )
        } finally {
            deleteRecursively(root)
        }
    }

    @Test
    fun symlinkSwapsAfterConfigurationResolutionFailClosedWithoutMutatingTargets() {
        val root = Files.createTempDirectory("wakeve-registration-toctou-contract-")
        try {
            val swapTarget = Files.createDirectory(root.resolve("swap-target"))
            val swapTargetParent = Files.createDirectory(swapTarget.resolve("database-parent"))
            Files.writeString(swapTargetParent.resolve("marker.txt"), "target-must-remain-unchanged")
            if (supportsPosix(swapTargetParent)) {
                Files.setPosixFilePermissions(swapTargetParent, BROAD_DIRECTORY_PERMISSIONS)
            }

            val configuredIntermediate = Files.createDirectory(root.resolve("configured-intermediate"))
            val configuredParent = Files.createDirectory(
                configuredIntermediate.resolve("database-parent")
            )
            val intermediateDatabase = configuredParent.resolve("registration.sqlite")
            val intermediateConfiguration = configuration(intermediateDatabase).getOrThrow()
            Files.delete(configuredParent)
            Files.delete(configuredIntermediate)
            if (!createSymbolicLinkOrSkip(configuredIntermediate, swapTarget.fileName)) return

            val intermediateTargetBefore = directorySnapshot(swapTargetParent)
            val intermediateOpen = runCatching {
                SqliteBackendDeviceRegistrationStoreFactory(intermediateConfiguration).open().close()
            }
            val intermediateTargetAfter = directorySnapshot(swapTargetParent)

            val finalParent = Files.createDirectory(root.resolve("final-file-parent"))
            if (supportsPosix(finalParent)) {
                Files.setPosixFilePermissions(finalParent, OWNER_DIRECTORY_PERMISSIONS)
            }
            val finalDatabase = finalParent.resolve("registration.sqlite")
            val finalConfiguration = configuration(finalDatabase).getOrThrow()
            val finalTarget = root.resolve("final-target.bin")
            Files.writeString(finalTarget, "final-target-must-remain-unchanged")
            if (supportsPosix(finalTarget)) {
                Files.setPosixFilePermissions(finalTarget, BROAD_FILE_PERMISSIONS)
            }
            val finalTargetBefore = fileSnapshot(finalTarget)
            Files.createSymbolicLink(finalDatabase, finalParent.relativize(finalTarget))
            val finalOpen = runCatching {
                SqliteBackendDeviceRegistrationStoreFactory(finalConfiguration).open().close()
            }
            val finalTargetAfter = fileSnapshot(finalTarget)

            val violations = buildList {
                if (intermediateOpen.isSuccess) {
                    add("factory followed a swapped intermediate symlink")
                }
                if (intermediateTargetBefore != intermediateTargetAfter) {
                    add("intermediate symlink target was chmoded or received SQLite artifacts")
                }
                if (finalOpen.isSuccess) {
                    add("factory followed a swapped final-file symlink")
                }
                if (finalTargetBefore != finalTargetAfter) {
                    add("final-file symlink target was chmoded or opened as SQLite")
                }
            }
            assertTrue(violations.isEmpty(), "TOCTOU path validation failures: $violations")
        } finally {
            deleteRecursively(root)
        }
    }

    @Test
    fun sharedBroadParentIsRejectedUnchangedWhileDedicatedPathsRemainOwnerOnly() {
        val root = Files.createTempDirectory("wakeve-registration-parent-policy-contract-")
        try {
            if (!supportsPosix(root)) return
            val violations = mutableListOf<String>()

            val sharedParent = Files.createDirectory(root.resolve("shared-parent"))
            Files.setPosixFilePermissions(sharedParent, BROAD_DIRECTORY_PERMISSIONS)
            val sharedDatabase = sharedParent.resolve("registration.sqlite")
            val sharedConfiguration = configuration(sharedDatabase)
            val sharedOpen = sharedConfiguration.fold(
                onSuccess = { resolved ->
                    runCatching {
                        SqliteBackendDeviceRegistrationStoreFactory(resolved).open().close()
                    }
                },
                onFailure = { Result.failure(it) }
            )
            if (sharedOpen.isSuccess) violations += "broad/shared parent was accepted"
            if (Files.getPosixFilePermissions(sharedParent) != BROAD_DIRECTORY_PERMISSIONS) {
                violations += "broad/shared parent permissions were modified"
            }
            if (Files.exists(sharedDatabase)) {
                violations += "database was created inside rejected broad/shared parent"
            }

            val dedicatedParent = root.resolve("service-owned-parent")
            val dedicatedDatabase = dedicatedParent.resolve("registration.sqlite")
            val dedicatedOpen = runCatching {
                SqliteBackendDeviceRegistrationStoreFactory(
                    configuration(dedicatedDatabase).getOrThrow()
                ).open().close()
            }
            if (dedicatedOpen.isFailure) {
                violations += "service could not create its dedicated database parent"
            } else {
                if (Files.getPosixFilePermissions(dedicatedParent) != OWNER_DIRECTORY_PERMISSIONS) {
                    violations += "dedicated parent was not created with 0700 permissions"
                }
                if (Files.getPosixFilePermissions(dedicatedDatabase) != OWNER_FILE_PERMISSIONS) {
                    violations += "dedicated database was not created with 0600 permissions"
                }
            }

            val ownerOnlyParent = Files.createDirectory(root.resolve("owner-only-parent"))
            Files.setPosixFilePermissions(ownerOnlyParent, OWNER_DIRECTORY_PERMISSIONS)
            val broadDatabase = ownerOnlyParent.resolve("registration.sqlite")
            DriverManager.getConnection("jdbc:sqlite:$broadDatabase").use { connection ->
                connection.createStatement().use { statement ->
                    statement.execute("CREATE TABLE pre_existing_broad_file(id INTEGER PRIMARY KEY)")
                }
            }
            Files.setPosixFilePermissions(broadDatabase, BROAD_FILE_PERMISSIONS)
            val broadFileOpen = configuration(broadDatabase).fold(
                onSuccess = { resolved ->
                    runCatching {
                        SqliteBackendDeviceRegistrationStoreFactory(resolved).open().close()
                    }
                },
                onFailure = { Result.failure(it) }
            )
            val broadFilePermissionsAfter = Files.getPosixFilePermissions(broadDatabase)
            if (broadFileOpen.isSuccess && broadFilePermissionsAfter != OWNER_FILE_PERMISSIONS) {
                violations += "accepted broad database file was not normalized to 0600"
            }
            if (broadFileOpen.isFailure && broadFilePermissionsAfter != BROAD_FILE_PERMISSIONS) {
                violations += "rejected broad database file was modified before rejection"
            }

            assertTrue(violations.isEmpty(), "POSIX datastore ownership failures: $violations")
        } finally {
            deleteRecursively(root)
        }
    }

    @Test
    fun reentrantCloseCannotEraseProviderKeyInsideDecryptionCallback() = runBlocking {
        val root = Files.createTempDirectory("wakeve-registration-reentrant-close-contract-")
        val databasePath = root.resolve("registration.sqlite")
        val factory = SqliteBackendDeviceRegistrationStoreFactory(
            configuration(databasePath).getOrThrow()
        )
        try {
            val rawToken = "reentrant-close-secret-token"
            val registration = factory.open().use { store ->
                store.register(request("reentrant-phone", rawToken))
            }
            val providerTokenPort = factory.openProviderTokenPort()
            var outerObservedToken = false
            var nestedObservedToken = false
            var reentrantClose: Result<Unit>? = null
            var nestedDecryption: Result<Boolean>? = null

            val completionResult = runCatching {
                providerTokenPort.withDecryptedToken(registration.registrationId) { token ->
                    outerObservedToken = token == rawToken
                    reentrantClose = runCatching { providerTokenPort.close() }
                    nestedDecryption = runCatching {
                        runBlocking {
                            providerTokenPort.withDecryptedToken(registration.registrationId) { nested ->
                                nestedObservedToken = nested == rawToken
                            }
                        }
                    }
                }
            }
            var preExternalCloseObservedToken = false
            val preExternalCloseProbe = runCatching {
                providerTokenPort.withDecryptedToken(registration.registrationId) { token ->
                    preExternalCloseObservedToken = token == rawToken
                }
            }
            // This external close must finish the explicit-rejection branch and remain
            // idempotent after a successful deferred close. It must not mask a successful no-op.
            val externalClose = runCatching { providerTokenPort.close() }
            val postExternalCloseProbe = runCatching {
                providerTokenPort.withDecryptedToken(registration.registrationId) { }
            }
            val completion = completionResult.getOrThrow()

            assertEquals(true, completion, "outer callback must produce one coherent completion")
            assertTrue(outerObservedToken, "outer callback did not observe its selected token")
            val closeAttempt = assertNotNull(reentrantClose)
            if (closeAttempt.isSuccess) {
                assertTrue(
                    preExternalCloseProbe.isFailure,
                    "successful reentrant close was a no-op; port remained open after callback"
                )
                assertIs<IllegalStateException>(
                    preExternalCloseProbe.exceptionOrNull(),
                    "successful deferred close must reject provider operations after callback"
                )
            } else {
                assertIs<IllegalStateException>(
                    closeAttempt.exceptionOrNull(),
                    "reentrant close rejection must be explicit"
                )
                assertTrue(
                    preExternalCloseProbe.isSuccess,
                    "rejected reentrant close must leave the port open until external close"
                )
                assertEquals(
                    true,
                    preExternalCloseProbe.getOrNull(),
                    "pre-external-close provider operation must complete coherently"
                )
                assertTrue(
                    preExternalCloseObservedToken,
                    "rejected reentrant close erased key material before external close"
                )
            }
            val nestedAttempt = assertNotNull(nestedDecryption)
            assertTrue(
                nestedAttempt.isSuccess,
                "reentrant close erased or closed the provider key before the callback returned: " +
                    nestedAttempt.exceptionOrNull()?.javaClass?.simpleName
            )
            assertTrue(
                nestedObservedToken,
                "key material was not usable for the remainder of the active callback"
            )
            externalClose.getOrThrow()
            assertTrue(
                postExternalCloseProbe.isFailure,
                "external close did not leave the provider port closed"
            )
            assertIs<IllegalStateException>(
                postExternalCloseProbe.exceptionOrNull(),
                "closed provider port must reject later provider operations"
            )
            Unit
        } finally {
            deleteRecursively(root)
        }
    }

    private fun request(
        installationId: String,
        rawToken: String
    ): BackendDeviceRegistrationRequest = BackendDeviceRegistrationRequest.create(
        installationId = installationId,
        authenticatedUserId = "user-a",
        platform = Platform.IOS,
        scope = DeviceRegistrationScope.create(
            APNsEnvironment.PRODUCTION,
            "com.guyghost.wakeve"
        ).getOrThrow(),
        rawToken = rawToken,
        registeredAtEpochSeconds = 100
    ).getOrThrow()

    private fun configuration(path: Path): Result<DeviceRegistrationStoreConfiguration> =
        DeviceRegistrationStoreConfiguration.resolve(
            environment = emptyMap(),
            systemProperties = mapOf(
                DATABASE_PATH_PROPERTY to path.toString(),
                LEGACY_HMAC_KEY_PROPERTY to HMAC_KEY,
                TOKEN_ENCRYPTION_KEY_PROPERTY to TOKEN_ENCRYPTION_KEY
            )
        )

    private fun createSymbolicLinkOrSkip(link: Path, target: Path): Boolean = try {
        Files.createSymbolicLink(link, target)
        true
    } catch (_: UnsupportedOperationException) {
        false
    } catch (_: FileSystemException) {
        false
    } catch (_: SecurityException) {
        false
    }

    private fun directorySnapshot(path: Path): DirectorySnapshot = DirectorySnapshot(
        permissions = posixPermissionsOrNull(path),
        entries = Files.list(path).use { entries ->
            entries.map { it.fileName.toString() }.sorted().toList()
        },
        marker = Files.readString(path.resolve("marker.txt"))
    )

    private fun fileSnapshot(path: Path): FileSnapshot = FileSnapshot(
        permissions = posixPermissionsOrNull(path),
        bytes = Files.readAllBytes(path).toList()
    )

    private fun supportsPosix(path: Path): Boolean =
        Files.getFileAttributeView(path, PosixFileAttributeView::class.java) != null

    private fun posixPermissionsOrNull(path: Path): Set<PosixFilePermission>? =
        if (supportsPosix(path)) Files.getPosixFilePermissions(path) else null

    private fun deleteRecursively(root: Path) {
        Files.walk(root).sorted(Comparator.reverseOrder<Path>()).use { paths ->
            paths.forEach { Files.deleteIfExists(it) }
        }
    }

    private data class DirectorySnapshot(
        val permissions: Set<PosixFilePermission>?,
        val entries: List<String>,
        val marker: String
    )

    private data class FileSnapshot(
        val permissions: Set<PosixFilePermission>?,
        val bytes: List<Byte>
    )

    private companion object {
        const val DATABASE_PATH_PROPERTY = "wakeve.notification.device-registration.db.path"
        const val LEGACY_HMAC_KEY_PROPERTY =
            "wakeve.notification.device-registration.legacy-identity-hmac-key"
        const val TOKEN_ENCRYPTION_KEY_PROPERTY =
            "wakeve.notification.device-registration.token-encryption-key"
        const val HMAC_KEY = "security-fix-loop-hmac-key-with-at-least-32-bytes"
        const val TOKEN_ENCRYPTION_KEY =
            "security-fix-loop-encryption-key-with-at-least-32-bytes"

        val OWNER_DIRECTORY_PERMISSIONS = setOf(
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE,
            PosixFilePermission.OWNER_EXECUTE
        )
        val OWNER_FILE_PERMISSIONS = setOf(
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE
        )
        val BROAD_DIRECTORY_PERMISSIONS = PosixFilePermission.entries.toSet()
        val BROAD_FILE_PERMISSIONS = setOf(
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE,
            PosixFilePermission.GROUP_READ,
            PosixFilePermission.GROUP_WRITE,
            PosixFilePermission.OTHERS_READ,
            PosixFilePermission.OTHERS_WRITE
        )
    }
}
