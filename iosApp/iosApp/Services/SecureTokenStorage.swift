//
//  SecureTokenStorage.swift
//  iosApp
//
//  Created by Wakev on 2025
//

import Foundation
import Security

/**
 * Secure token storage for iOS using Keychain
 */
class SecureTokenStorage: SecureTokenStorageProtocol {
    private let serviceName = "com.guyghost.wakeve"
    private let accessTokenKey = "access_token"
    private let refreshTokenKey = "refresh_token"
    private let userIdKey = "user_id"
    private let tokenExpiryKey = "token_expiry"

    func storeAccessToken(_ token: String) async throws {
        try storeString(token, forKey: accessTokenKey)
    }

    func storeRefreshToken(_ token: String) async throws {
        try storeString(token, forKey: refreshTokenKey)
    }

    func storeUserId(_ userId: String) async throws {
        try storeString(userId, forKey: userIdKey)
    }

    func storeTokenExpiry(_ expiryTimestamp: Int64) async throws {
        let data = withUnsafeBytes(of: expiryTimestamp) { Data($0) }
        try storeData(data, forKey: tokenExpiryKey)
    }

    func getAccessToken() async -> String? {
        return try? getString(forKey: accessTokenKey)
    }

    func getRefreshToken() async -> String? {
        return try? getString(forKey: refreshTokenKey)
    }

    func getUserId() async -> String? {
        return try? getString(forKey: userIdKey)
    }

    func getTokenExpiry() async -> Int64? {
        guard let data = try? getData(forKey: tokenExpiryKey) else { return nil }
        return data.withUnsafeBytes { $0.load(as: Int64.self) }
    }

    func clearAllTokens() async throws {
        try deleteString(forKey: accessTokenKey)
        try deleteString(forKey: refreshTokenKey)
        try deleteString(forKey: userIdKey)
        try deleteData(forKey: tokenExpiryKey)
    }

    func isTokenExpired() async -> Bool {
        guard let expiry = await getTokenExpiry() else { return true }
        return Date().timeIntervalSince1970 * 1000 >= Double(expiry)
    }

    func hasValidToken() async -> Bool {
        guard let token = await getAccessToken(), !token.isEmpty else { return false }
        return !(await isTokenExpired())
    }

    // MARK: - Private Keychain operations

    private func storeString(_ string: String, forKey key: String) throws {
        let data = string.data(using: .utf8)!
        try storeData(data, forKey: key)
    }

    private func storeData(_ data: Data, forKey key: String) throws {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: serviceName,
            kSecAttrAccount as String: key,
            kSecValueData as String: data,
            kSecAttrAccessible as String: kSecAttrAccessibleAfterFirstUnlock
        ]

        // Delete existing item
        SecItemDelete(query as CFDictionary)

        // Add new item
        let status = SecItemAdd(query as CFDictionary, nil)
        guard status == errSecSuccess else {
            throw KeychainError.operationFailed(status: status)
        }
    }

    private func getString(forKey key: String) throws -> String {
        let data = try getData(forKey: key)
        guard let string = String(data: data, encoding: .utf8) else {
            throw KeychainError.invalidData
        }
        return string
    }

    private func getData(forKey key: String) throws -> Data {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: serviceName,
            kSecAttrAccount as String: key,
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne
        ]

        var result: AnyObject?
        let status = SecItemCopyMatching(query as CFDictionary, &result)

        guard status == errSecSuccess,
              let data = result as? Data else {
            throw KeychainError.itemNotFound
        }

        return data
    }

    private func deleteString(forKey key: String) throws {
        try deleteData(forKey: key)
    }

    private func deleteData(forKey key: String) throws {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: serviceName,
            kSecAttrAccount as String: key
        ]

        let status = SecItemDelete(query as CFDictionary)
        guard status == errSecSuccess || status == errSecItemNotFound else {
            throw KeychainError.operationFailed(status: status)
        }
    }
}

// MARK: - Supporting Types

protocol SecureTokenStorageProtocol {
    func storeAccessToken(_ token: String) async throws
    func storeRefreshToken(_ token: String) async throws
    func storeUserId(_ userId: String) async throws
    func storeTokenExpiry(_ expiryTimestamp: Int64) async throws

    func getAccessToken() async -> String?
    func getRefreshToken() async -> String?
    func getUserId() async -> String?
    func getTokenExpiry() async -> Int64?

    func clearAllTokens() async throws
    func isTokenExpired() async -> Bool
    func hasValidToken() async -> Bool
}

enum KeychainError: Error {
    case operationFailed(status: OSStatus)
    case itemNotFound
    case invalidData
}