import Foundation
import BackgroundTasks
import Shared

/// Gestionnaire de synchronisation offline-first
@MainActor
class SyncManager: ObservableObject {
    @Published var syncState: SyncState?
    @Published var conflicts: [SyncConflict] = []
    @Published var isSyncing = false
    @Published var lastSyncError: String?
    
    private let syncRepository: SyncRepository
    private let networkMonitor: NetworkMonitor
    private let authManager: AuthenticationManager?
    private var syncTimer: Timer?
    private let syncInterval: TimeInterval = 60 // Sync toutes les 60 secondes
    
    // Background task identifier
    private static let backgroundSyncTaskId = "com.guyghost.wakeve.background-sync"
    
    init(
        syncRepository: SyncRepository,
        networkMonitor: NetworkMonitor,
        authManager: AuthenticationManager?
    ) {
        self.syncRepository = syncRepository
        self.networkMonitor = networkMonitor
        self.authManager = authManager
        
        // Enregistrer les listeners
        registerNetworkListener()
        registerSyncListeners()
        
        // Programmer les sync en arrière-plan
        scheduleSyncTask()
        
        // Démarrer le timer de sync
        startSyncTimer()
    }
    
    deinit {
        syncTimer?.invalidate()
    }
    
    // MARK: - Public Methods
    
    /// Initie une synchronisation immédiate
    func syncNow() async {
        guard !isSyncing else { return }
        guard let user = authManager?.currentUser else { return }
        
        isSyncing = true
        lastSyncError = nil
        
        defer { isSyncing = false }
        
        // Effectuer la synchronisation
        // TODO: Implémenter l'appel réel au syncRepository
    }
    
    /// Résout un conflit
    func resolveConflict(_ conflictId: String, using strategy: ResolutionStrategy) async {
        // TODO: Appeler syncRepository.resolveConflict
    }
    
    /// Récupère l'état de synchronisation
    func updateSyncState() async {
        // TODO: Appeler syncRepository.getSyncState
    }
    
    /// Enregistre une opération locale
    func recordChange(
        entityType: String,
        entityId: String,
        operation: SyncOperation,
        data: [String: Any]
    ) async {
        guard let user = authManager?.currentUser else { return }
        
        // TODO: Appeler syncRepository.recordChange
    }
    
    // MARK: - Private Methods
    
    private func registerNetworkListener() {
        networkMonitor.addNetworkListener { [weak self] isOnline in
            if isOnline {
                Task {
                    await self?.syncNow()
                }
            }
        }
    }
    
    private func registerSyncListeners() {
        // TODO: Enregistrer les listeners SyncEvent
    }
    
    private func startSyncTimer() {
        syncTimer = Timer.scheduledTimer(withTimeInterval: syncInterval, repeats: true) { [weak self] _ in
            guard self?.networkMonitor.isOnline == true else { return }
            
            Task {
                await self?.syncNow()
            }
        }
    }
    
    private func scheduleSyncTask() {
        let request = BGProcessingTaskRequest(identifier: Self.backgroundSyncTaskId)
        request.requiresNetworkConnectivity = true
        
        do {
            try BGTaskScheduler.shared.submit(request)
        } catch {
            print("Erreur lors de la planification de la sync: \(error)")
        }
    }
    
    /// Enregistre le handler pour la sync en arrière-plan
    static func registerBackgroundTask() {
        BGTaskScheduler.shared.register(
            forTaskWithIdentifier: backgroundSyncTaskId,
            using: nil
        ) { task in
            // TODO: Implémenter la sync en arrière-plan
            task.setTaskCompleted(success: true)
        }
    }
}

// MARK: - UI Model for Sync Status

struct SyncStatusView: View {
    @ObservedObject var syncManager: SyncManager
    @ObservedObject var networkMonitor: NetworkMonitor
    
    var body: some View {
        HStack(spacing: LiquidGlassDesign.spacingS) {
            // Indicateur de connexion
            Circle()
                .fill(networkMonitor.isOnline ? LiquidGlassDesign.successGreen : Color.gray)
                .frame(width: 8, height: 8)
            
            // Statut texte
            VStack(alignment: .leading, spacing: 2) {
                if syncManager.isSyncing {
                    HStack(spacing: 4) {
                        ProgressView()
                            .scaleEffect(0.8)
                        Text("Synchronisation...")
                            .font(LiquidGlassDesign.caption)
                    }
                } else if let error = syncManager.lastSyncError {
                    HStack(spacing: 4) {
                        Image(systemName: "exclamationmark.circle.fill")
                            .font(.system(size: 10))
                            .foregroundColor(LiquidGlassDesign.errorRed)
                        Text("Erreur: \(error)")
                            .font(LiquidGlassDesign.caption)
                            .foregroundColor(LiquidGlassDesign.errorRed)
                    }
                } else if networkMonitor.isOnline {
                    HStack(spacing: 4) {
                        Image(systemName: "checkmark.circle.fill")
                            .font(.system(size: 10))
                            .foregroundColor(LiquidGlassDesign.successGreen)
                        Text("Synchronisé")
                            .font(LiquidGlassDesign.caption)
                    }
                } else {
                    HStack(spacing: 4) {
                        Image(systemName: "wifi.slash")
                            .font(.system(size: 10))
                        Text("Hors ligne")
                            .font(LiquidGlassDesign.caption)
                    }
                }
                
                // Nombre de changements en attente
                if let state = syncManager.syncState, state.pendingChangesCount > 0 {
                    Text("\(state.pendingChangesCount) changement(s) en attente")
                        .font(.system(size: 9))
                        .foregroundColor(.secondary)
                }
            }
            
            Spacer()
        }
        .padding(LiquidGlassDesign.spacingS)
        .background(
            RoundedRectangle(cornerRadius: LiquidGlassDesign.radiusS)
                .fill(LiquidGlassDesign.glassColor)
        )
    }
}

import SwiftUI
