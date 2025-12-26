import SwiftUI

// MARK: - Shared Models for Wakeve iOS

/// Participant model used across all views
struct ParticipantModel: Identifiable {
    let id: String
    let name: String
    let email: String
}
