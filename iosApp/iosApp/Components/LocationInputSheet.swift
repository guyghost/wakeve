import SwiftUI
import Shared

/// Sheet for adding a potential location.
/// Mirrors Android's LocationInputDialog with native iOS sheet presentation.
///
/// Features:
/// - Name input (required)
/// - Type picker (City, Region, Specific Venue, Online)
/// - Address input (optional, hidden for Online type)
/// - Context-sensitive help text
/// - Validation (name required)
/// - VoiceOver accessibility
///
/// Example:
/// ```swift
/// .sheet(isPresented: $showLocationSheet) {
///     LocationInputSheet(
///         eventId: event.id,
///         onDismiss: { showLocationSheet = false },
///         onConfirm: { location in
///             locations.append(location)
///             showLocationSheet = false
///         }
///     )
/// }
/// ```
struct LocationInputSheet: View {
    let eventId: String
    var onDismiss: () -> Void
    var onConfirm: (Shared.PotentialLocation_) -> Void
    
    // Form state
    @State private var name: String = ""
    @State private var selectedType: Shared.LocationType = .city
    @State private var address: String = ""
    
    @FocusState private var nameFieldFocused: Bool
    
    // MARK: - Computed Properties
    
    private var isValid: Bool {
        !name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }
    
    // MARK: - Body
    
    var body: some View {
        NavigationView {
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    // Header
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Add Location")
                            .font(.largeTitle.weight(.bold))
                            .foregroundColor(.primary)
                        
                        Text("Add a potential location for your event")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                    }
                    .padding(.horizontal)
                    .padding(.top, 8)
                    
                    Divider()
                        .padding(.horizontal)
                    
                    // Form Fields
                    VStack(spacing: 16) {
                        // Name Field (Required)
                        VStack(alignment: .leading, spacing: 8) {
                            Text("Location Name")
                                .font(.subheadline.weight(.medium))
                                .foregroundColor(.primary)
                            
                            TextField("e.g., Paris, Hotel Royal, Online", text: $name)
                                .font(.body)
                                .padding(12)
                                .background(.ultraThinMaterial)
                                .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                                .overlay(
                                    RoundedRectangle(cornerRadius: 10, style: .continuous)
                                        .stroke(
                                            name.isEmpty ? Color.red.opacity(0.3) : Color.primary.opacity(0.1),
                                            lineWidth: 1
                                        )
                                )
                                .focused($nameFieldFocused)
                                .accessibilityLabel("Location Name")
                                .accessibilityHint("Enter the name of the location, required field")
                            
                            if name.isEmpty {
                                Label("Location name is required", systemImage: "exclamationmark.circle.fill")
                                    .font(.caption)
                                    .foregroundColor(.red)
                                    .transition(.opacity)
                            }
                        }
                        .padding(.horizontal)
                        
                        // Type Picker
                        VStack(alignment: .leading, spacing: 8) {
                            Text("Location Type")
                                .font(.subheadline.weight(.medium))
                                .foregroundColor(.primary)
                            
                            Menu {
                                ForEach([Shared.LocationType.city, .region, .specificVenue, .online], id: \.hashValue) { type in
                                    Button {
                                        withAnimation(.easeInOut(duration: 0.2)) {
                                            selectedType = type
                                        }
                                    } label: {
                                        HStack {
                                            Text(locationTypeText(type))
                                            if type.hashValue == selectedType.hashValue {
                                                Spacer()
                                                Image(systemName: "checkmark")
                                                    .foregroundColor(.blue)
                                            }
                                        }
                                    }
                                }
                            } label: {
                                HStack {
                                    Text(locationTypeText(selectedType))
                                        .font(.body)
                                        .foregroundColor(.primary)
                                    
                                    Spacer()
                                    
                                    Image(systemName: "chevron.up.chevron.down")
                                        .font(.system(size: 14, weight: .medium))
                                        .foregroundColor(.secondary)
                                }
                                .padding(12)
                                .background(.ultraThinMaterial)
                                .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                                .overlay(
                                    RoundedRectangle(cornerRadius: 10, style: .continuous)
                                        .stroke(Color.primary.opacity(0.1), lineWidth: 1)
                                )
                            }
                            .accessibilityLabel("Location Type")
                            .accessibilityValue(locationTypeText(selectedType))
                            .accessibilityHint("Select the type of location")
                        }
                        .padding(.horizontal)
                        
                        // Address Field (Optional, hidden for Online)
                        if selectedType != .online {
                            VStack(alignment: .leading, spacing: 8) {
                                Text("Address (optional)")
                                    .font(.subheadline.weight(.medium))
                                    .foregroundColor(.primary)
                                
                                TextField("Street, City, Country", text: $address, axis: .vertical)
                                    .font(.body)
                                    .lineLimit(2...3)
                                    .padding(12)
                                    .background(.ultraThinMaterial)
                                    .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                                    .overlay(
                                        RoundedRectangle(cornerRadius: 10, style: .continuous)
                                            .stroke(Color.primary.opacity(0.1), lineWidth: 1)
                                    )
                                    .accessibilityLabel("Address")
                                    .accessibilityHint("Optional field for the location's address")
                            }
                            .padding(.horizontal)
                            .transition(.opacity.combined(with: .move(edge: .top)))
                        }
                        
                        // Help Text
                        HStack(alignment: .top, spacing: 8) {
                            Image(systemName: "lightbulb.fill")
                                .font(.system(size: 14))
                                .foregroundColor(.yellow)
                            
                            Text(helpText)
                                .font(.caption)
                                .foregroundColor(.secondary)
                                .fixedSize(horizontal: false, vertical: true)
                        }
                        .padding(12)
                        .background(Color.blue.opacity(0.08))
                        .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
                        .padding(.horizontal)
                    }
                    
                    Divider()
                        .padding(.horizontal)
                    
                    // Action Buttons
                    HStack(spacing: 12) {
                        Button("Cancel") {
                            onDismiss()
                        }
                        .font(.body.weight(.medium))
                        .foregroundColor(.secondary)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                        .background(Color.secondary.opacity(0.1))
                        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                        .accessibilityLabel("Cancel")
                        .accessibilityHint("Dismiss without adding location")
                        
                        Button("Add Location") {
                            addLocation()
                        }
                        .font(.body.weight(.semibold))
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                        .background(isValid ? Color.blue : Color.gray)
                        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                        .disabled(!isValid)
                        .accessibilityLabel("Add Location")
                        .accessibilityHint("Confirm and add this location")
                    }
                    .padding(.horizontal)
                    .padding(.bottom, 20)
                }
            }
            .navigationBarHidden(true)
        }
        .onAppear {
            // Auto-focus name field
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
                nameFieldFocused = true
            }
        }
        .animation(.easeInOut(duration: 0.25), value: selectedType.hashValue)
    }
    
    // MARK: - Helpers
    
    private func locationTypeText(_ type: Shared.LocationType) -> String {
        switch type {
        case Shared.LocationType.city:
            return "City"
        case Shared.LocationType.region:
            return "Region"
        case Shared.LocationType.specificVenue:
            return "Specific Venue"
        case Shared.LocationType.online:
            return "Online"
        default:
            return "Location"
        }
    }
    
    private var helpText: String {
        switch selectedType {
        case Shared.LocationType.city:
            return "Great for flexible events that can happen anywhere in the city"
        case Shared.LocationType.region:
            return "Perfect for regional events (e.g., \"South of France\")"
        case Shared.LocationType.specificVenue:
            return "Use this for a specific address or venue"
        case Shared.LocationType.online:
            return "For virtual events via Zoom, Meet, etc."
        default:
            return "Select a location type above"
        }
    }
    
    private func addLocation() {
        guard isValid else { return }
        
        let trimmedName = name.trimmingCharacters(in: .whitespacesAndNewlines)
        let trimmedAddress = address.trimmingCharacters(in: .whitespacesAndNewlines)
        
        let location = Shared.PotentialLocation_(
            id: "loc-\(Date().timeIntervalSince1970)",
            eventId: eventId,
            name: trimmedName,
            locationType: selectedType,
            address: trimmedAddress.isEmpty ? nil : trimmedAddress,
            coordinates: nil, // Phase 4 feature
            createdAt: ISO8601DateFormatter().string(from: Date())
        )
        
        onConfirm(location)
    }
}

// MARK: - Previews

struct LocationInputSheet_Previews: PreviewProvider {
    static var previews: some View {
        VStack {
            Text("Tap below to show sheet")
                .padding()
            
            Button("Show Location Input") {
                // Preview only
            }
        }
        .sheet(isPresented: .constant(true)) {
            LocationInputSheet(
                eventId: "preview-event",
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
