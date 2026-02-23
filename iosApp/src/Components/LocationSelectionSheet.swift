import SwiftUI
import Shared
import CoreLocation
import Combine

/// Bottom sheet for selecting a location.
/// Matches the design from lugarres.png with native iOS Liquid Glass styling.
///
/// Features:
/// - Search bar with voice input button
/// - Current location suggestion with geolocation
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
    @State private var currentAddress: String? = nil
    @State private var isLoadingLocation: Bool = false
    @State private var showPermissionAlert: Bool = false
    
    @FocusState private var searchFieldFocused: Bool
    @FocusState private var customNameFieldFocused: Bool
    
    @StateObject private var locationManager = LocationManager()
    
    // MARK: - Computed Properties
    
    private var isValid: Bool {
        useCurrentLocation || !customLocationName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }
    
    private var displayLocationName: String {
        if useCurrentLocation {
            return currentAddress ?? String(localized: "location.my_current_position")
        }
        return customLocationName.trimmingCharacters(in: .whitespacesAndNewlines)
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
            .navigationTitle(String(localized: "events.location"))
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
                    .accessibilityLabel(String(localized: "common.confirm"))
                    .accessibilityHint(String(localized: "location.validate_selection"))
                }
            }
            .background(Color(.systemBackground))
        }
        .alert(String(localized: "location.allow_title"), isPresented: $showPermissionAlert) {
            Button(String(localized: "common.cancel"), role: .cancel) {}
            Button(String(localized: "location.open_settings")) {
                if let url = URL(string: UIApplication.openSettingsURLString) {
                    UIApplication.shared.open(url)
                }
            }
        } message: {
            Text(String(localized: "location.permission_message"))
        }
        .onChange(of: locationManager.authorizationStatus) { _, newStatus in
            handleAuthorizationChange(newStatus)
        }
        .onChange(of: locationManager.location) { _, newLocation in
            if let location = newLocation {
                reverseGeocode(location)
            }
        }
        .onReceive(locationManager.$error) { error in
            if let error = error {
                print("Location error: \(error.localizedDescription)")
                isLoadingLocation = false
            }
        }
    }
    
    // MARK: - Authorization Handling
    
    private func handleAuthorizationChange(_ status: CLAuthorizationStatus) {
        switch status {
        case .authorizedWhenInUse, .authorizedAlways:
            if useCurrentLocation {
                locationManager.requestLocation()
            }
        case .denied, .restricted:
            if useCurrentLocation {
                showPermissionAlert = true
                useCurrentLocation = false
                isLoadingLocation = false
            }
        case .notDetermined:
            if useCurrentLocation {
                locationManager.requestAuthorization()
            }
        @unknown default:
            break
        }
    }
    
    // MARK: - Geocoding
    
    private func reverseGeocode(_ location: CLLocation) {
        let geocoder = CLGeocoder()
        geocoder.reverseGeocodeLocation(location) { placemarks, error in
            isLoadingLocation = false
            
            if let error = error {
                print("Reverse geocoding error: \(error.localizedDescription)")
                currentAddress = String(localized: "location.my_current_position")
                return
            }
            
            if let placemark = placemarks?.first {
                // Build address string from available components
                var addressComponents: [String] = []
                
                if let subThoroughfare = placemark.subThoroughfare {
                    addressComponents.append(subThoroughfare)
                }
                if let thoroughfare = placemark.thoroughfare {
                    addressComponents.append(thoroughfare)
                }
                if let locality = placemark.locality {
                    addressComponents.append(locality)
                }
                
                if addressComponents.isEmpty {
                    currentAddress = String(localized: "location.my_current_position")
                } else {
                    currentAddress = addressComponents.joined(separator: ", ")
                }
            } else {
                currentAddress = String(localized: "location.my_current_position")
            }
        }
    }
    
    // MARK: - Subviews
    
    private var searchBarView: some View {
        HStack(spacing: 12) {
            Image(systemName: "magnifyingglass")
                .font(.system(size: 17, weight: .medium))
                .foregroundColor(.secondary)
            
            TextField(String(localized: "location.search_places"), text: $searchText)
                .font(.body)
                .foregroundColor(.primary)
                .focused($searchFieldFocused)
                .onChange(of: searchFieldFocused) { _, isFocused in
                    if isFocused {
                        requestLocationPermissionForSearch()
                    }
                }
            
            if !searchText.isEmpty {
                Button {
                    searchText = ""
                } label: {
                    Image(systemName: "xmark.circle.fill")
                        .font(.system(size: 20))
                        .foregroundColor(.secondary)
                }
                .accessibilityLabel(String(localized: "location.clear_search"))
            }
            
            Button {
                // Voice search action
                triggerVoiceSearch()
            } label: {
                Image(systemName: "mic")
                    .font(.system(size: 20, weight: .medium))
                    .foregroundColor(.secondary)
            }
            .accessibilityLabel(String(localized: "location.voice_search"))
            .accessibilityHint(String(localized: "location.voice_search_hint"))
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
            Text(String(localized: "location.suggestions"))
                .font(.subheadline.weight(.semibold))
                .foregroundColor(.secondary)
                .padding(.horizontal)
            
            // Current Location Button
            Button {
                withAnimation(.easeInOut(duration: 0.2)) {
                    useCurrentLocation.toggle()
                    if useCurrentLocation {
                        customLocationName = ""
                        requestCurrentLocation()
                    } else {
                        currentAddress = nil
                    }
                }
            } label: {
                HStack(spacing: 12) {
                    ZStack {
                        Circle()
                            .fill(Color.blue.opacity(0.15))
                            .frame(width: 36, height: 36)
                        
                        if isLoadingLocation {
                            ProgressView()
                                .scaleEffect(0.8)
                                .frame(width: 16, height: 16)
                        } else {
                            Image(systemName: "location.fill")
                                .font(.system(size: 16, weight: .semibold))
                                .foregroundColor(.blue)
                        }
                    }
                    
                    VStack(alignment: .leading, spacing: 2) {
                        Text(String(localized: "location.current_position"))
                            .font(.body.weight(.medium))
                            .foregroundColor(.primary)
                        
                        if useCurrentLocation, let address = currentAddress {
                            Text(address)
                                .font(.caption)
                                .foregroundColor(.secondary)
                                .lineLimit(1)
                        }
                    }
                    
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
            .accessibilityLabel(String(localized: "location.current_position"))
            .accessibilityHint(String(localized: "location.use_current_position_hint"))
            .accessibilityValue(useCurrentLocation ? String(localized: "common.selected") : String(localized: "common.not_selected"))
            .padding(.horizontal)
        }
    }
    
    private var customLocationSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(String(localized: "location.place_name"))
                .font(.subheadline.weight(.semibold))
                .foregroundColor(.secondary)
                .padding(.horizontal)
            
            // Custom location text field
            TextField("", text: $customLocationName)
                .font(.body)
                .foregroundColor(.primary)
                .placeholder(when: customLocationName.isEmpty) {
                    Text(String(localized: "location.place_name_placeholder"))
                        .font(.body)
                        .foregroundColor(.secondary.opacity(0.6))
                }
                .focused($customNameFieldFocused)
                .padding(12)
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
                            currentAddress = nil
                        }
                    }
                }
                .padding(.horizontal)
                .accessibilityLabel(String(localized: "location.place_name"))
                .accessibilityHint(String(localized: "location.custom_place_hint"))
            
            // Help text
            Text(String(localized: "location.optional_hint"))
                .font(.caption)
                .foregroundColor(.secondary)
                .padding(.horizontal)
        }
    }
    
    // MARK: - Helpers
    
    private func requestLocationPermissionForSearch() {
        let status = locationManager.authorizationStatus
        switch status {
        case .notDetermined:
            // Request permission when user taps search field
            locationManager.requestAuthorization()
        case .authorizedWhenInUse, .authorizedAlways:
            // Permission already granted, user can type freely
            break
        case .denied, .restricted:
            // Permission denied, show alert but still allow typing
            // User can still search by typing manually
            break
        @unknown default:
            break
        }
    }
    
    private func requestCurrentLocation() {
        isLoadingLocation = true
        
        let status = locationManager.authorizationStatus
        switch status {
        case .notDetermined:
            locationManager.requestAuthorization()
        case .authorizedWhenInUse, .authorizedAlways:
            locationManager.requestLocation()
        case .denied, .restricted:
            showPermissionAlert = true
            isLoadingLocation = false
            useCurrentLocation = false
        @unknown default:
            isLoadingLocation = false
        }
    }
    
    private func triggerVoiceSearch() {
        let generator = UINotificationFeedbackGenerator()
        generator.notificationOccurred(.success)
    }
    
    private func confirmSelection() {
        guard isValid else { return }
        
        let locationName: String
        let locationType: Shared.LocationType
        
        if useCurrentLocation {
            locationName = currentAddress ?? String(localized: "location.my_current_position")
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

// MARK: - Location Manager

class LocationManager: NSObject, ObservableObject, CLLocationManagerDelegate {
    private let manager = CLLocationManager()
    
    @Published var location: CLLocation?
    @Published var authorizationStatus: CLAuthorizationStatus = .notDetermined
    @Published var error: Error?
    
    override init() {
        super.init()
        manager.delegate = self
        authorizationStatus = manager.authorizationStatus
    }
    
    func requestAuthorization() {
        manager.requestWhenInUseAuthorization()
    }
    
    func requestLocation() {
        error = nil
        manager.requestLocation()
    }
    
    // MARK: - CLLocationManagerDelegate
    
    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        location = locations.first
    }
    
    func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        self.error = error
        print("Location manager error: \(error.localizedDescription)")
    }
    
    func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        authorizationStatus = manager.authorizationStatus
    }
}

// MARK: - Preview

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
