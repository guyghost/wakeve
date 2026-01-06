import SwiftUI
import Shared

/// Main entry point for creating a new event
/// Wraps DraftEventWizardView and handles repository integration
struct CreateEventView: View {
    let userId: String
    let repository: EventRepositoryInterface
    var onEventCreated: (String) -> Void
    
    @Environment(\.dismiss) private var dismiss
    @State private var showError = false
    @State private var errorMessage = ""
    @State private var isSaving = false
    
    var body: some View {
        NavigationStack {
            DraftEventWizardView(
                initialEvent: nil,
                onSaveStep: { event in
                    // Auto-save on step transition (background)
                    autoSaveEvent(event)
                },
                onComplete: { event in
                    // Final save when user clicks "Create Event"
                    createEvent(event)
                },
                onCancel: {
                    dismiss()
                }
            )
            .navigationTitle("Créer un événement")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Annuler") {
                        dismiss()
                    }
                    .foregroundColor(.secondary)
                }
            }
            .alert("Erreur", isPresented: $showError) {
                Button("OK", role: .cancel) { }
            } message: {
                Text(errorMessage)
            }
            .overlay {
                if isSaving {
                    savingOverlay
                }
            }
        }
    }
    
    // MARK: - Saving Overlay
    
    private var savingOverlay: some View {
        ZStack {
            Color.black.opacity(0.4)
                .ignoresSafeArea()
            
            VStack(spacing: 16) {
                ProgressView()
                    .progressViewStyle(CircularProgressViewStyle(tint: .white))
                    .scaleEffect(1.5)
                
                Text("Création en cours...")
                    .font(.subheadline.weight(.medium))
                    .foregroundColor(.white)
            }
            .padding(32)
            .background(
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .fill(.ultraThinMaterial)
            )
        .accessibilityViewIsModal(true)  // WCAG 4.1.3: Mark overlay as modal for VoiceOver
        }
    }
    
    // MARK: - Repository Operations
    
    /// Auto-save event during wizard navigation (non-blocking)
    private func autoSaveEvent(_ event: Shared.Event) {
        Task {
            do {
                // Save the event in the repository (create if doesn't exist, otherwise update)
                // This is a background save, so we don't block the UI
                let result = try await repository.saveEvent(event: event)

                // Log success (optional)
                print("✅ Event auto-saved: \(event.id)")
            } catch {
                // Log error but don't show to user (it's background save)
                print("⚠️ Auto-save failed: \(error.localizedDescription)")
                // We'll retry on next step transition or final save
            }
        }
    }
    
    /// Create event in repository and notify parent
    private func createEvent(_ event: Shared.Event) {
        isSaving = true

        Task {
            do {
                // Save the event in the repository (create if doesn't exist, otherwise update)
                // This handles the case where the event was already auto-saved during wizard steps
                let result = try await repository.saveEvent(event: event)

                // Extract event ID from result
                if let savedEvent = result as? Shared.Event {
                    // Success - notify parent and dismiss
                    await MainActor.run {
                        isSaving = false
                        onEventCreated(savedEvent.id)
                        dismiss()
                    }
                } else {
                    // Unexpected result type
                    throw NSError(
                        domain: "CreateEventView",
                        code: -1,
                        userInfo: [NSLocalizedDescriptionKey: "Unexpected result type from saveEvent"]
                    )
                }
            } catch {
                // Show error to user
                await MainActor.run {
                    isSaving = false
                    errorMessage = "Impossible de créer l'événement: \(error.localizedDescription)"
                    showError = true
                }
            }
        }
    }
}

// MARK: - Preview

#Preview("Create Event") {
    CreateEventView(
        userId: "user123",
        repository: RepositoryProvider.shared.repository,
        onEventCreated: { eventId in
            print("Event created: \(eventId)")
        }
    )
}
