import Foundation
import Security
import Shared

/// Gestionnaire de stockage sécurisé pour les données sensibles
class SecureStorageManager {
    static let shared = SecureStorageManager()
    
    private let keychainService = "com.guyghost.wakeve"
    private let userSessionKey = "userSession"
    private let userDataKey = "userData"
    
    // MARK: - User Session
    
    /// Sauvegarde une session utilisateur de manière sécurisée
    func saveSession(_ session: UserSession) throws {
        let encoder = JSONEncoder()
        let sessionData = try encoder.encode(session)
        
        try saveToKeychain(data: sessionData, for: userSessionKey)
    }
    
    /// Récupère la session utilisateur sauvegardée
    func getSession() -> UserSession? {
        guard let data = try? retrieveFromKeychain(for: userSessionKey) else {
            return nil
        }
        
        let decoder = JSONDecoder()
        return try? decoder.decode(UserSession.self, from: data)
    }
    
    /// Efface la session utilisateur
    func clearSession() {
        try? deleteFromKeychain(for: userSessionKey)
    }
    
    // MARK: - User Data
    
    /// Sauvegarde les données utilisateur
    func saveUserData(_ user: User) throws {
        let encoder = JSONEncoder()
        let userData = try encoder.encode(user)
        
        try saveToKeychain(data: userData, for: userDataKey)
    }
    
    /// Récupère les données utilisateur sauvegardées
    func getUserData() -> User? {
        guard let data = try? retrieveFromKeychain(for: userDataKey) else {
            return nil
        }
        
        let decoder = JSONDecoder()
        return try? decoder.decode(User.self, from: data)
    }
    
    // MARK: - Generic Keychain Operations
    
    /// Sauvegarde des données dans le Keychain
    private func saveToKeychain(data: Data, for key: String) throws {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: keychainService,
            kSecAttrAccount as String: key,
            kSecValueData as String: data,
            kSecAttrAccessible as String: kSecAttrAccessibleWhenUnlockedThisDeviceOnly
        ]
        
        // Essayer d'abord de supprimer l'entrée existante
        SecItemDelete(query as CFDictionary)
        
        // Ajouter la nouvelle entrée
        let status = SecItemAdd(query as CFDictionary, nil)
        
        guard status == errSecSuccess else {
            throw KeychainError.saveFailed(status: status)
        }
    }
    
    /// Récupère des données du Keychain
    private func retrieveFromKeychain(for key: String) throws -> Data {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: keychainService,
            kSecAttrAccount as String: key,
            kSecReturnData as String: true
        ]
        
        var result: AnyObject?
        let status = SecItemCopyMatching(query as CFDictionary, &result)
        
        guard status == errSecSuccess else {
            if status == errSecItemNotFound {
                throw KeychainError.itemNotFound
            }
            throw KeychainError.retrievalFailed(status: status)
        }
        
        guard let data = result as? Data else {
            throw KeychainError.invalidData
        }
        
        return data
    }
    
    /// Efface une entrée du Keychain
    private func deleteFromKeychain(for key: String) throws {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: keychainService,
            kSecAttrAccount as String: key
        ]
        
        let status = SecItemDelete(query as CFDictionary)
        
        guard status == errSecSuccess || status == errSecItemNotFound else {
            throw KeychainError.deletionFailed(status: status)
        }
    }
    
    /// Efface toutes les données du Keychain pour cette app
    func clearAllData() throws {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: keychainService
        ]
        
        let status = SecItemDelete(query as CFDictionary)
        
        guard status == errSecSuccess || status == errSecItemNotFound else {
            throw KeychainError.deletionFailed(status: status)
        }
    }
}

// MARK: - Keychain Errors

enum KeychainError: LocalizedError {
    case saveFailed(status: OSStatus)
    case retrievalFailed(status: OSStatus)
    case deletionFailed(status: OSStatus)
    case itemNotFound
    case invalidData
    
    var errorDescription: String? {
        switch self {
        case .saveFailed(let status):
            return "Erreur de sauvegarde Keychain: \(status)"
        case .retrievalFailed(let status):
            return "Erreur de lecture Keychain: \(status)"
        case .deletionFailed(let status):
            return "Erreur de suppression Keychain: \(status)"
        case .itemNotFound:
            return "Élément non trouvé dans le Keychain"
        case .invalidData:
            return "Données invalides dans le Keychain"
        }
    }
}

// MARK: - Codable Support

extension UserSession: Codable {
    enum CodingKeys: String, CodingKey {
        case user
        case accessToken
        case refreshToken
        case expiresAt
        case createdAt
    }
    
    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        self.user = try container.decode(User.self, forKey: .user)
        self.accessToken = try container.decode(String.self, forKey: .accessToken)
        self.refreshToken = try container.decode(String.self, forKey: .refreshToken)
        self.expiresAt = try container.decode(Long.self, forKey: .expiresAt)
        self.createdAt = try container.decode(Long.self, forKey: .createdAt)
    }
    
    func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(user, forKey: .user)
        try container.encode(accessToken, forKey: .accessToken)
        try container.encode(refreshToken, forKey: .refreshToken)
        try container.encode(expiresAt, forKey: .expiresAt)
        try container.encode(createdAt, forKey: .createdAt)
    }
}

extension User: Codable {
    enum CodingKeys: String, CodingKey {
        case id
        case email
        case name
        case avatar
        case provider
        case createdAt
        case lastLogin
    }
    
    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        self.id = try container.decode(String.self, forKey: .id)
        self.email = try container.decode(String.self, forKey: .email)
        self.name = try container.decode(String.self, forKey: .name)
        self.avatar = try container.decodeIfPresent(String.self, forKey: .avatar)
        self.provider = try container.decode(AuthProvider.self, forKey: .provider)
        self.createdAt = try container.decode(String.self, forKey: .createdAt)
        self.lastLogin = try container.decodeIfPresent(String.self, forKey: .lastLogin)
    }
    
    func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(id, forKey: .id)
        try container.encode(email, forKey: .email)
        try container.encode(name, forKey: .name)
        try container.encodeIfPresent(avatar, forKey: .avatar)
        try container.encode(provider, forKey: .provider)
        try container.encode(createdAt, forKey: .createdAt)
        try container.encodeIfPresent(lastLogin, forKey: .lastLogin)
    }
}

extension AuthProvider: Codable {}

// MARK: - Biometric Authentication Support

/// Gestionnaire d'authentification biométrique
class BiometricAuthManager {
    static let shared = BiometricAuthManager()
    
    private let context = LAContext()
    
    /// Vérifie si la biométrie est disponible
    var isBiometricAvailable: Bool {
        var error: NSError?
        let available = context.canEvaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, error: &error)
        return available
    }
    
    /// Type de biométrie disponible
    var biometricType: String {
        guard isBiometricAvailable else { return "Aucune" }
        
        if context.biometryType == .faceID {
            return "Face ID"
        } else if context.biometryType == .touchID {
            return "Touch ID"
        } else {
            return "Biométrie"
        }
    }
    
    /// Authenticate using biometrics
    func authenticate(completion: @escaping (Bool, Error?) -> Void) {
        var error: NSError?
        
        guard context.canEvaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, error: &error) else {
            completion(false, error)
            return
        }
        
        context.evaluatePolicy(
            .deviceOwnerAuthenticationWithBiometrics,
            localizedReason: "Authentifiez-vous pour accéder à votre compte",
            reply: { success, error in
                DispatchQueue.main.async {
                    completion(success, error)
                }
            }
        )
    }
}

import LocalAuthentication

typealias LAContext = LocalAuthentication.LAContext
