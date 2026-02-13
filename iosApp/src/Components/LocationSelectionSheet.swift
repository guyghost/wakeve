import SwiftUI
import Shared

/// Bottom sheet for selecting a location.
/// Matches the design from lugarres.png with native iOS Liquid Glass styling.
///
/// Features:
/// - Search bar with voice input button
/// - Current location suggestion
/// - Optional custom location name input
/// - Checkmark confirmation button
/// - Liquid Glass dark theme design
///
/// Example:
/// ```swift
/// .sheet(isPresented: $showLocationSheet) {
///     LocationSelectionSheet(
///         onDismiss: { showLocationSheet = false },
///         onConfirm: { location in
///             selectedLocation = location
///             showLocationSheet = false
///         }
///     )
/// }
/// ```
struct LocationSelectionSheet: View {
    var onDismiss: () -> Void
    var onConfirm: (Shared.PotentialLocation_) -> Void
    
    // Form state
    @State private var searchText: String = ""
    @State private var customLocationName: String = ""
    @State private var useCurrentLocation: Bool = false
    
    @FocusState private var searchFieldFocused: Bool
    @FocusState private var customNameFieldFocused: Bool
    
    // MARK: - Computed Properties
    
    private var isValid: Bool {
        useCurrentLocation || !customLocationName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }
    
    // MARK: - Body
    
    var body: some View {
        NavigationView {
            ScrollView {
                VStack(alignment: .leading, spacing: 24) {
                    // Search Bar
                    searchBarView
                    
                    // Suggestions Section
                    suggestionsSection
                    
                    // Custom Location Name Section
                    customLocationSection
                    
                    Spacer(minLength: 40)
                }
                .padding(.top, 8)
            }
            .navigationTitle("Lieu")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button {
                        confirmSelection()
                    } label: {
                        Image(systemName: "checkmark")
                            .font(.system(size: 16, weight: .semibold))
                            .foregroundColor(.white)
                            .frame(width: 32, height: 32)
                            .background(
                                Circle()
                                    .fill(isValid ? Color.blue : Color.gray)
                            )
                    }
                    .disabled(!isValid)
                    .accessibilityLabel("Confirmer")
                    .accessibilityHint("Valider la sélection du lieu")
                }
            }
            .background(Color(.systemBackground))
        }
    }
    
    // MARK: - Subviews
    
    private var searchBarView: some View {
        HStack(spacing: 12) {
            Image(systemName: "magnifyingglass")
                .font(.system(size: 17, weight: .medium))
                .foregroundColor(.secondary)
            
            TextField("Rechercher des lieux", text: $searchText)
                .font(.body)
                .foregroundColor(.primary)
                .focused($searchFieldFocused)
            
            if !searchText.isEmpty {
                Button {
                    searchText = ""
                } label: {
                    Image(systemName: "xmark.circle.fill")
                        .font(.system(size: 20))
                        .foregroundColor(.secondary)
                }
                .accessibilityLabel("Effacer la recherche")
            }
            
            Button {
                // Voice search action
                triggerVoiceSearch()
            } label: {
                Image(systemName: "mic")
                    .font(.system(size: 20, weight: .medium))
                    .foregroundColor(.secondary)
            }
            .accessibilityLabel("Recherche vocale")
            .accessibilityHint("Tapez pour rechercher un lieu par la voix")
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 10)
        .background(
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .fill(Color(.secondarySystemBackground))
        )
        .overlay(
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .stroke(Color.primary.opacity(0.08), lineWidth: 1)
        )
        .padding(.horizontal)
    }
    
    private var suggestionsSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Suggestions")
                .font(.subheadline.weight(.semibold))
                .foregroundColor(.secondary)
                .padding(.horizontal)
            
            // Current Location Button
            Button {
                withAnimation(.easeInOut(duration: 0.2)) {
                    useCurrentLocation.toggle()
                    if useCurrentLocation {
                        customLocationName = ""
                    }
                }
            } label: {
                HStack(spacing: 12) {
                    ZStack {
                        Circle()
                            .fill(Color.blue.opacity(0.15))
                            .frame(width: 36, height: 36)
                        
                        Image(systemName: "location.fill")
                            .font(.system(size: 16, weight: .semibold))
                            .foregroundColor(.blue)
                    }
                    
                    Text("Position actuelle")
                        .font(.body.weight(.medium))
                        .foregroundColor(.primary)
                    
                    Spacer()
                    
                    if useCurrentLocation {
                        Image(systemName: "checkmark.circle.fill")
                            .font(.system(size: 22))
                            .foregroundColor(.blue)
                            .transition(.scale.combined(with: .opacity))
                    }
                }
                .padding(12)
                .background(
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .fill(useCurrentLocation 
                            ? Color.blue.opacity(0.08)
                            : Color(.secondarySystemBackground))
                )
                .overlay(
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .stroke(useCurrentLocation 
                            ? Color.blue.opacity(0.3)
                            : Color.primary.opacity(0.06), lineWidth: 1)
                )
            }
            .accessibilityLabel("Position actuelle")
            .accessibilityHint("Utiliser votre position actuelle comme lieu")
            .accessibilityValue(useCurrentLocation ? "Sélectionné" : "Non sélectionné")
            .padding(.horizontal)
        }
    }
    
    private var customLocationSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Nom du lieu")
                .font(.subheadline.weight(.semibold))
                .foregroundColor(.secondary)
                .padding(.horizontal)
            
            // Custom location text field
            ZStack(alignment: .leading) {
                if customLocationName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                    Text("Exemple : chez Guy MANDINA NZEZA")
                        .font(.body)
                        .foregroundColor(.secondary.opacity(0.6))
                        .padding(.horizontal, 16)
                        .padding(.vertical, 12)
                        .accessibilityHidden(true)
                }

                TextField("", text: $customLocationName)
                    .font(.body)
                    .foregroundColor(.primary)
                    .focused($customNameFieldFocused)
                    .padding(12)
            }
            .background(
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .fill(Color(.secondarySystemBackground))
            )
            .overlay(
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .stroke(Color.primary.opacity(0.08), lineWidth: 1)
            )
            .onChange(of: customLocationName) { _, newValue in
                if !newValue.isEmpty {
                    withAnimation(.easeInOut(duration: 0.2)) {
                        useCurrentLocation = false
                    }
                }
            }
            .padding(.horizontal)
            .accessibilityLabel("Nom du lieu")
            .accessibilityHint("Entrez un nom de lieu personnalisé")
            
            // Help text
            Text("Facultatif. Ceci apparaît sur l'invitation.")
                .font(.caption)
                .foregroundColor(.secondary)
                .padding(.horizontal)
        }
    }
    
    // MARK: - Helpers
    
    private func triggerVoiceSearch() {
        // TODO: Implement voice search using Speech framework
        // For now, show a placeholder feedback
        let generator = UINotificationFeedbackGenerator()
        generator.notificationOccurred(.success)
    }
    
    private func confirmSelection() {
        guard isValid else { return }
        
        let locationName: String
        let locationType: Shared.LocationType
        
        if useCurrentLocation {
            locationName = "Ma position actuelle"
            locationType = .specificVenue
        } else {
            locationName = customLocationName.trimmingCharacters(in: .whitespacesAndNewlines)
            locationType = .specificVenue
        }
        
        let location = Shared.PotentialLocation_(
            id: "loc-\(Date().timeIntervalSince1970)",
            eventId: "temp-event",
            name: locationName,
            locationType: locationType,
            address: nil,
            coordinates: nil,
            createdAt: ISO8601DateFormatter().string(from: Date())
        )
        
        onConfirm(location)
    }
}

// MARK: - Previews

struct LocationSelectionSheet_Previews: PreviewProvider {
    static var previews: some View {
        VStack {
            Text("Tap below to show sheet")
                .padding()
            
            Button("Show Location Selection") {
                // Preview only
            }
        }
        .sheet(isPresented: .constant(true)) {
            LocationSelectionSheet(
                onDismiss: {
                    print("Dismissed")
                },
                onConfirm: { location in
                    print("Confirmed: \(location.name)")
                }
            )
        }
    }
}
