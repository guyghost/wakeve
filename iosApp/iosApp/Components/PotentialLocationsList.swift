import SwiftUI
import Shared

/// List of potential locations with add/remove functionality.
/// Mirrors Android's PotentialLocationsList with Liquid Glass design.
///
/// Features:
/// - Empty state when no locations
/// - List of locations with type-specific SF Symbols icons
/// - Add button in header
/// - Swipe-to-delete for each location
/// - Count badge showing number of locations
/// - VoiceOver accessibility
///
/// Example:
/// ```swift
/// PotentialLocationsList(
///     locations: $locations,
///     onAddLocation: { showSheet = true },
///     onRemoveLocation: { id in
///         locations.removeAll { $0.id == id }
///     },
///     enabled: true
/// )
/// ```
struct PotentialLocationsList: View {
    @Binding var locations: [Shared.PotentialLocation_]
    var onAddLocation: () -> Void
    var onRemoveLocation: (String) -> Void
    var enabled: Bool = true
    
    // MARK: - Body
    
    var body: some View {
        LiquidGlassCard(style: .regular, padding: 20) {
            VStack(alignment: .leading, spacing: 16) {
                // Header
                HStack {
                    HStack(spacing: 8) {
                        Image(systemName: "mappin.and.ellipse")
                            .font(.system(size: 20, weight: .medium))
                            .foregroundColor(.blue)
                        
                        Text("Potential Locations")
                            .font(.headline)
                            .foregroundColor(.primary)
                        
                        // Count badge
                        if !locations.isEmpty {
                            Text("\(locations.count)")
                                .font(.caption.weight(.semibold))
                                .foregroundColor(.white)
                                .padding(.horizontal, 6)
                                .padding(.vertical, 2)
                                .background(Color.blue)
                                .clipShape(RoundedRectangle(cornerRadius: 4, style: .continuous))
                        }
                    }
                    .accessibilityElement(children: .combine)
                    .accessibilityLabel("Potential Locations, \(locations.count) added")
                    
                    Spacer()
                    
                    // Add button
                    Button(action: onAddLocation) {
                        HStack(spacing: 4) {
                            Image(systemName: "plus")
                                .font(.system(size: 14, weight: .semibold))
                            Text("Add")
                                .font(.subheadline.weight(.medium))
                        }
                        .foregroundColor(.blue)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 6)
                        .background(Color.blue.opacity(0.12))
                        .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
                    }
                    .disabled(!enabled)
                    .accessibilityLabel("Add location")
                    .accessibilityHint("Tap to add a new potential location")
                }
                
                // Content
                if locations.isEmpty {
                    // Empty state
                    VStack(spacing: 12) {
                        Image(systemName: "mappin.slash")
                            .font(.system(size: 48, weight: .light))
                            .foregroundColor(.secondary.opacity(0.4))
                        
                        VStack(spacing: 4) {
                            Text("No locations yet")
                                .font(.body.weight(.medium))
                                .foregroundColor(.secondary.opacity(0.8))
                            
                            Text("Add potential venues, cities, or regions")
                                .font(.caption)
                                .foregroundColor(.secondary.opacity(0.6))
                                .multilineTextAlignment(.center)
                        }
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 24)
                    .accessibilityElement(children: .combine)
                    .accessibilityLabel("No locations yet. Add potential venues, cities, or regions")
                    .transition(.opacity.combined(with: .scale(scale: 0.95)))
                } else {
                    // Locations list
                    VStack(spacing: 8) {
                        ForEach(locations, id: \.id) { location in
                            LocationListItem(
                                location: location,
                                onRemove: {
                                    withAnimation(.spring(response: 0.3, dampingFraction: 0.8)) {
                                        onRemoveLocation(location.id)
                                    }
                                },
                                enabled: enabled
                            )
                            .transition(.asymmetric(
                                insertion: .scale.combined(with: .opacity),
                                removal: .scale.combined(with: .opacity)
                            ))
                        }
                    }
                    .transition(.opacity)
                }
            }
        }
        .animation(.easeInOut(duration: 0.25), value: locations.isEmpty)
        .animation(.spring(response: 0.3, dampingFraction: 0.8), value: locations.count)
    }
}

// MARK: - Location List Item

/// Single location item in the list with swipe-to-delete
private struct LocationListItem: View {
    let location: Shared.PotentialLocation_
    let onRemove: () -> Void
    let enabled: Bool
    
    var body: some View {
        HStack(spacing: 12) {
            // Type icon
            Image(systemName: locationIcon)
                .font(.system(size: 20, weight: .medium))
                .foregroundColor(.blue)
                .frame(width: 24)
            
            // Location info
            VStack(alignment: .leading, spacing: 4) {
                Text(location.name)
                    .font(.body.weight(.medium))
                    .foregroundColor(.primary)
                
                HStack(spacing: 4) {
                    Text(locationTypeText)
                        .font(.caption)
                        .foregroundColor(.secondary)
                    
                    if let address = location.address, !address.isEmpty {
                        Text("•")
                            .font(.caption)
                            .foregroundColor(.secondary)
                        
                        Text(address)
                            .font(.caption)
                            .foregroundColor(.secondary)
                            .lineLimit(1)
                    }
                }
            }
            
            Spacer()
            
            // Delete button
            Button(action: onRemove) {
                Image(systemName: "trash.fill")
                    .font(.system(size: 16, weight: .medium))
                    .foregroundColor(.red)
                    .frame(width: 32, height: 32)
            }
            .disabled(!enabled)
            .accessibilityLabel("Remove \(location.name)")
            .accessibilityHint("Double tap to remove this location")
        }
        .padding(12)
        .background(.ultraThinMaterial)
        .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 10, style: .continuous)
                .stroke(Color.primary.opacity(0.08), lineWidth: 1)
        )
        .accessibilityElement(children: .contain)
        .accessibilityLabel("\(location.name), \(locationTypeText)")
        .accessibilityValue(location.address ?? "No address")
    }
    
    // MARK: - Helpers
    
    private var locationIcon: String {
        switch location.locationType {
        case Shared.LocationType.city:
            return "building.2.fill"
        case Shared.LocationType.region:
            return "globe.americas.fill"
        case Shared.LocationType.specificVenue:
            return "mappin.circle.fill"
        case Shared.LocationType.online:
            return "video.fill"
        default:
            return "mappin.and.ellipse"
        }
    }
    
    private var locationTypeText: String {
        switch location.locationType {
        case Shared.LocationType.city:
            return "City"
        case Shared.LocationType.region:
            return "Region"
        case Shared.LocationType.specificVenue:
            return "Venue"
        case Shared.LocationType.online:
            return "Online"
        default:
            return "Location"
        }
    }
}

// MARK: - Previews

struct PotentialLocationsList_Previews: PreviewProvider {
    static var previews: some View {
        ScrollView {
            VStack(spacing: 24) {
                // Preview 1: Empty state
                PreviewWrapper(
                    locations: [],
                    title: "Empty State"
                )
                
                // Preview 2: One location
                PreviewWrapper(
                    locations: [
                        createMockLocation(name: "Paris", type: .city, address: "France")
                    ],
                    title: "Single Location"
                )
                
                // Preview 3: Multiple locations
                PreviewWrapper(
                    locations: [
                        createMockLocation(name: "Paris", type: .city, address: "France"),
                        createMockLocation(name: "Central Europe", type: .region, address: nil),
                        createMockLocation(name: "Conference Hall A", type: .specificVenue, address: "123 Main St"),
                        createMockLocation(name: "Zoom Meeting", type: .online, address: nil)
                    ],
                    title: "Multiple Locations"
                )
                
                // Preview 4: Disabled state
                PreviewWrapper(
                    locations: [
                        createMockLocation(name: "Paris", type: .city, address: "France"),
                        createMockLocation(name: "London", type: .city, address: "UK")
                    ],
                    title: "Disabled State",
                    enabled: false
                )
            }
            .padding()
        }
        .background(Color(red: 0.95, green: 0.95, blue: 0.97))
    }
    
    /// Helper to create mock PotentialLocation
    private static func createMockLocation(
        name: String,
        type: Shared.LocationType,
        address: String?
    ) -> Shared.PotentialLocation_ {
        return Shared.PotentialLocation_(
            id: UUID().uuidString,
            eventId: "preview-event",
            name: name,
            locationType: type,
            address: address,
            coordinates: nil,
            createdAt: ISO8601DateFormatter().string(from: Date())
        )
    }
    
    /// Helper wrapper for previews with state
    private struct PreviewWrapper: View {
        @State var locations: [Shared.PotentialLocation_]
        let title: String
        var enabled: Bool = true
        
        var body: some View {
            VStack(alignment: .leading, spacing: 8) {
                Text(title)
                    .font(.headline)
                    .foregroundColor(.secondary)
                    .padding(.horizontal, 4)
                
                PotentialLocationsList(
                    locations: $locations,
                    onAddLocation: {
                        print("Add location tapped")
                    },
                    onRemoveLocation: { id in
                        locations.removeAll { $0.id == id }
                    },
                    enabled: enabled
                )
            }
        }
    }
}
