import Foundation
import Shared

/// iOS-specific provider for the persistent DatabaseEventRepository
/// This singleton ensures we use the same database instance throughout the app
class RepositoryProvider {
    static let shared = RepositoryProvider()

    private let databaseFactory: IosDatabaseFactory
    /// The database instance - exposed for state machine creation
    let database: WakeveDb
    private let _repository: DatabaseEventRepository

    /// SyncManager for offline-first synchronization
    let syncManager: SyncManager

    /// Token storage for authentication
    private let tokenStorage = SecureTokenStorage()

    private init() {
        // Initialize the database factory and create the database
        self.databaseFactory = IosDatabaseFactory()
        self.database = DatabaseProvider.shared.getDatabase(factory: databaseFactory)

        // Create sync dependencies
        let networkDetector = KtorSyncHttpClientKt.createNetworkStatusDetector()
        let httpClient = KtorSyncHttpClientKt.createSyncHttpClient(baseUrl: "http://localhost:8080")
        let userRepository = UserRepository(db: database)
        let eventRepository = DatabaseEventRepository(db: database, syncManager: nil)
        let metrics = InMemorySyncMetrics()
        let alertManager = LoggingSyncAlertManager()

        // Capture tokenStorage for closure
        let storage = self.tokenStorage

        // Create the SyncManager with all dependencies
        self.syncManager = SyncManager(
            database: database,
            eventRepository: eventRepository,
            userRepository: userRepository,
            networkDetector: networkDetector,
            httpClient: httpClient,
            authTokenProvider: {
                // Synchronous access - return cached token
                // SecureTokenStorage uses async, so we use a blocking approach
                var token: String? = nil
                let semaphore = DispatchSemaphore(value: 0)
                Task {
                    token = await storage.getAccessToken()
                    semaphore.signal()
                }
                semaphore.wait()
                return token
            },
            authTokenRefreshProvider: nil,
            maxRetries: 3,
            baseRetryDelayMs: 1000,
            metrics: metrics,
            alertManager: alertManager
        )

        // Create the repository with the SyncManager enabled
        self._repository = DatabaseEventRepository(db: database, syncManager: syncManager)
    }

    /// Returns the persistent event repository backed by SQLDelight
    var repository: EventRepositoryInterface {
        return _repository
    }

    /// Convenience accessor for DatabaseEventRepository specifically
    var databaseRepository: DatabaseEventRepository {
        return _repository
    }
}
