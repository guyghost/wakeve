package com.guyghost.wakeve.notification

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.deleteIfExists

internal class BackendNotificationDurabilityTestFixture(
    val root: Path = Files.createTempDirectory("wakeve-notification-durability-")
) : AutoCloseable {
    val databasePath: Path = root.resolve("notification.sqlite")
    val registrationConfiguration: DeviceRegistrationStoreConfiguration =
        DeviceRegistrationStoreConfiguration.resolve(
            systemProperties = mapOf(
                "wakeve.notification.device-registration.db.path" to databasePath.toString(),
                "wakeve.notification.device-registration.legacy-identity-hmac-key" to "h".repeat(32),
                "wakeve.notification.device-registration.token-encryption-key" to "e".repeat(32)
            ),
            environment = emptyMap()
        ).getOrThrow()
    val registrationFactory = SqliteBackendDeviceRegistrationStoreFactory(registrationConfiguration)
    val deliveryFactory = SqliteBackendNotificationDeliveryStoreFactory(registrationConfiguration)

    fun register(
        installationId: String,
        userId: String = "recipient-user",
        token: String = "token-$installationId"
    ): BackendDeviceRegistration = runBlocking {
        registrationFactory.open().use { store ->
            store.register(
                BackendDeviceRegistrationRequest.create(
                    installationId = installationId,
                    authenticatedUserId = userId,
                    platform = Platform.IOS,
                    scope = DeviceRegistrationScope.create(
                        APNsEnvironment.PRODUCTION,
                        "com.guyghost.wakeve"
                    ).getOrThrow(),
                    rawToken = token,
                    registeredAtEpochSeconds = 100
                ).getOrThrow()
            )
        }
    }

    override fun close() {
        Files.walk(root).use { paths ->
            paths.sorted(Comparator.reverseOrder()).forEach { it.deleteIfExists() }
        }
    }
}

/** Coordinates callers immediately before the public claim port without constraining its internals. */
internal class BackendNotificationConcurrentStartGate(
    private val participants: Int = 2
) {
    private val arrivals = AtomicInteger()
    private val allReady = CompletableDeferred<Unit>()
    private val release = CompletableDeferred<Unit>()

    suspend fun awaitReleaseBeforePortCall() {
        if (arrivals.incrementAndGet() == participants) allReady.complete(Unit)
        release.await()
    }

    suspend fun releaseTogether() {
        withTimeout(5_000) { allReady.await() }
        release.complete(Unit)
    }
}
