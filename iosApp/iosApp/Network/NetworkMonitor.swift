import Foundation
import Network
import Shared

/// Moniteur réseau pour détecter la connectivité
@MainActor
class NetworkMonitor: ObservableObject {
    @Published var isOnline = true
    @Published var connectionType: ConnectionType = .unknown
    
    private let monitor = NWPathMonitor()
    private let queue = DispatchQueue.global(qos: .background)
    private var listeners = [(Bool) -> Void]()
    
    enum ConnectionType {
        case unknown
        case wifi
        case cellular
        case wired
        
        var description: String {
            switch self {
            case .unknown:
                return "Connexion inconnue"
            case .wifi:
                return "WiFi"
            case .cellular:
                return "Données mobiles"
            case .wired:
                return "Filaire"
            }
        }
    }
    
    init() {
        monitor.pathUpdateHandler = { [weak self] path in
            DispatchQueue.main.async {
                self?.handlePathUpdate(path)
            }
        }
        monitor.start(queue: queue)
    }
    
    deinit {
        monitor.cancel()
    }
    
    // MARK: - Public Methods
    
    func addNetworkListener(_ listener: @escaping (Bool) -> Void) {
        listeners.append(listener)
    }
    
    func removeNetworkListener(_ listener: @escaping (Bool) -> Void) {
        listeners.removeAll { _ in true } // Simplification: supprimer tous
    }
    
    // MARK: - Private Methods
    
    private func handlePathUpdate(_ path: NWPath) {
        let newStatus = path.status == .satisfied
        let newType = determineConnectionType(path)
        
        // Mettre à jour le statut
        if isOnline != newStatus {
            isOnline = newStatus
            notifyListeners(newStatus)
        }
        
        // Mettre à jour le type de connexion
        connectionType = newType
    }
    
    private func determineConnectionType(_ path: NWPath) -> ConnectionType {
        if path.usesInterfaceType(.wifi) {
            return .wifi
        } else if path.usesInterfaceType(.cellular) {
            return .cellular
        } else if path.usesInterfaceType(.wiredEthernet) {
            return .wired
        } else {
            return .unknown
        }
    }
    
    private func notifyListeners(_ isOnline: Bool) {
        listeners.forEach { $0(isOnline) }
    }
}

// MARK: - Conformance to Shared NetworkMonitor

extension NetworkMonitor: com_guyghost_wakeve_NetworkMonitor {
    func isNetworkAvailable() -> Bool {
        return isOnline
    }
    
    func addNetworkListener(listener: @escaping (Swift.Bool) -> Swift.Void) {
        addNetworkListener(listener)
    }
    
    func removeNetworkListener(listener: @escaping (Swift.Bool) -> Swift.Void) {
        removeNetworkListener(listener)
    }
}
