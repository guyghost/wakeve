import SwiftUI
import Shared

/// List of potential locations with add/remove functionality.
/// Mirrors Android's PotentialLocationsList with Liquid Glass design.
///
/// Features:
/// - Empty state when no locations
/// - List of locations with type-specific SF Symbols icons
/// - Add button in header using LiquidGlassButton
/// - Count badge using LiquidGlassBadge
/// - Swipe-to-delete for each location
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
        LiquidGlassCard(cornerRadius: 16, padding: 20) {
            VStack(alignment: .leading, spacing: 16) {
                // Header
                headerView
                
                // Divider
                LiquidGlassDivider(style: .subtle)
                    .padding(.vertical, 8)
                
                // Content
                if locations.isEmpty {
                    emptyStateView
                } else {
                    locationsListView
                }
            }
        }
        .animation(.easeInOut(duration: 0.25), value: locations.isEmpty)
        .animation(.spring(response: 0.3, dampingFraction: 0.8), value: locations.count)
    }
    
    // MARK: - Subviews
    
    private var headerView: some View {
        HStack {
            // Icon and Title
            HStack(spacing: 8) {
                ZStack {
                    Circle()
                        .fill(
                            LinearGradient(
                                gradient: Gradient(colors: [
                                    Color.wakevePrimary.opacity(0.2),
                                    Color.wakeveAccent.opacity(0.15)
                                ]),
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            )
                        )
                        .frame(width: 32, height: 32)
                    
                    Image(systemName: "mappin.and.ellipse")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(.wakevePrimary)
                }
                
                Text("Potential Locations")
                    .font(.headline)
                    .foregroundColor(.primary)
                
                // Count badge using LiquidGlassBadge
                if !locations.isEmpty {
                    LiquidGlassBadge(
                        text: "\(locations.count)",
                        style: .info
                    )
                }
            }
            .accessibilityElement(children: .combine)
            .accessibilityLabel("Potential Locations, \(locations.count) added")
            
            Spacer()
            
            // Add button using LiquidGlassButton
            LiquidGlassButton(
                title: "Add",
                style: .secondary
            ) {
                onAddLocation()
            }
            .frame(width: 80, height: 36)
            .disabled(!enabled)
            .accessibilityLabel("Add location")
            .accessibilityHint("Tap to add a new potential location")
        }
    }
    
    private var emptyStateView: some View {
        VStack(spacing: 16) {
            ZStack {
                Circle()
                    .fill(
                        LinearGradient(
                            gradient: Gradient(colors: [
                                Color.wakevePrimary.opacity(0.1),
                                Color.wakeveAccent.opacity(0.05)
                            ]),
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
                    .frame(width: 80, height: 80)
                
                Image(systemName: "mappin.slash")
                    .font(.system(size: 32, weight: .light))
                    .foregroundColor(.wakevePrimary.opacity(0.5))
            }
            
            VStack(spacing: 4) {
                Text("No locations yet")
                    .font(.body.weight(.medium))
                    .foregroundColor(.primary)
                
                Text("Add potential venues, cities, or regions")
                    .font(.caption)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 24)
        .accessibilityElement(children: .combine)
        .accessibilityLabel("No locations yet. Add potential venues, cities, or regions")
        .transition(.opacity.combined(with: .scale(scale: 0.95)))
    }
    
    private var locationsListView: some View {
        VStack(spacing: 12) {
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

// MARK: - Location List Item

/// Single location item in the list with swipe-to-delete
private struct LocationListItem: View {
    let location: Shared.PotentialLocation_
    let onRemove: () -> Void
    let enabled: Bool
    
    var body: some View {
        LiquidGlassListItem(
            title: location.name,
            subtitle: locationTypeSubtitle,
            icon: locationIcon,
            iconColor: iconColor,
            style: .default
        ) {
            removeButton
        }
        .accessibilityElement(children: .contain)
        .accessibilityLabel("\(location.name), \(locationTypeText)")
        .accessibilityValue(location.address ?? "No address")
    }
    
    // MARK: - Subviews
    
    private var removeButton: some View {
        Button(action: onRemove) {
            Image(systemName: "trash.fill")
                .font(.system(size: 14, weight: .medium))
                .foregroundColor(.wakeveError)
                .frame(width: 32, height: 32)
                .background(
                    Circle()
                        .fill(Color.wakeveError.opacity(0.1))
                )
        }
        .disabled(!enabled)
        .accessibilityLabel("Remove \(location.name)")
        .accessibilityHint("Double tap to remove this location")
    }
    
    // MARK: - Helpers
    
    private var locationTypeSubtitle: String {
        var parts: [String] = [locationTypeText]
        
        if let address = location.address, !address.isEmpty {
            parts.append(address)
        }
        
        return parts.joined(separator: " • ")
    }
    
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
    
    private var iconColor: Color {
        switch location.locationType {
        case Shared.LocationType.city:
            return .wakevePrimary
        case Shared.LocationType.region:
            return .cyan
        case Shared.LocationType.specificVenue:
            return .orange
        case Shared.LocationType.online:
            return .purple
        default:
            return .wakeveAccent
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
