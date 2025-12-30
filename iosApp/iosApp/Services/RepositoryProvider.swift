import Foundation
import Shared

/// iOS-specific provider for the persistent DatabaseEventRepository
/// This singleton ensures we use the same database instance throughout the app
class RepositoryProvider {
    static let shared = RepositoryProvider()
    
    private let databaseFactory: IosDatabaseFactory
    /// The database instance - exposed for state machine creation
    let database: WakevDb
    private let _repository: DatabaseEventRepository
    
    private init() {
        // Initialize the database factory and create the database
        self.databaseFactory = IosDatabaseFactory()
        self.database = DatabaseProvider.shared.getDatabase(factory: databaseFactory)
        
        // Create the repository with the database (no SyncManager for now)
        self._repository = DatabaseEventRepository(db: database, syncManager: nil)
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
