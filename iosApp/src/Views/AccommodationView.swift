import SwiftUI
import Shared

/**
 * Accommodation Management View (iOS)
 * 
 * Features:
 * - List of all accommodations with Liquid Glass design
 * - Add/Edit/Delete accommodation
 * - Booking status tracking
 * - Cost per person display
 * - Native iOS interactions (sheets, alerts)
 */
struct AccommodationView: View {
    let eventId: String
    let currentUserId: String
    let currentUserName: String
    @Environment(\.dismiss) private var dismiss
    
    @State private var accommodations: [AccommodationModel] = []
    @State private var isLoading = false
    @State private var showAddSheet = false
    @State private var selectedAccommodation: AccommodationModel?
    @State private var showDeleteAlert = false
    @State private var accommodationToDelete: AccommodationModel?
    
    // Comments state
    @State private var commentCount = 0
    @State private var showComments = false
    
    var body: some View {
        NavigationView {
            ZStack {
                // Background gradient
                LinearGradient(
                    colors: [Color(.systemBackground), Color(.systemGray6)],
                    startPoint: .top,
                    endPoint: .bottom
                )
                .ignoresSafeArea()
                
                if isLoading {
                    ProgressView()
                } else if accommodations.isEmpty {
                    emptyStateView
                } else {
                    accommodationsList
                }
            }
            .navigationTitle(String(localized: "accommodation.title"))
            .navigationBarTitleDisplayMode(.large)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(String(localized: "common.close")) { dismiss() }
                        .foregroundColor(.wakevePrimary)
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    HStack(spacing: 12) {
                        CommentButton(commentCount: commentCount) {
                            showComments = true
                        }
                        
                        Button {
                            selectedAccommodation = nil
                            showAddSheet = true
                        } label: {
                            Image(systemName: "plus.circle.fill")
                                .font(.title2)
                                .foregroundColor(.wakevePrimary)
                        }
                    }
                }
            }
            .sheet(isPresented: $showAddSheet) {
                AccommodationFormSheet(
                    eventId: eventId,
                    accommodation: selectedAccommodation,
                    onSave: { accommodation in
                        if let index = accommodations.firstIndex(where: { $0.id == accommodation.id }) {
                            accommodations[index] = accommodation
                        } else {
                            accommodations.append(accommodation)
                        }
                        showAddSheet = false
                        selectedAccommodation = nil
                    }
                )
            }
            .sheet(isPresented: $showComments) {
                NavigationView {
                    // TODO: Re-enable CommentsView when Shared types are properly integrated
                    Text("Comments - Coming Soon")
                        .font(.title2)
                        .foregroundColor(.secondary)
                }
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .navigationBarLeading) {
                        Button(String(localized: "common.close")) {
                            showComments = false
                        }
                        .foregroundColor(.wakevePrimary)
                    }
                }
            }
            .alert(String(localized: "accommodation.delete_title"), isPresented: $showDeleteAlert, presenting: accommodationToDelete) { accommodation in
                Button(String(localized: "common.cancel"), role: .cancel) {
                    accommodationToDelete = nil
                }
                .foregroundColor(.wakevePrimary)
                Button(String(localized: "common.delete"), role: .destructive) {
                    accommodations.removeAll { $0.id == accommodation.id }
                    accommodationToDelete = nil
                }
            } message: { _ in
                Text(String(localized: "accommodation.delete_message"))
            }
        }
        .onAppear {
            loadAccommodations()
            loadCommentCount()
        }
    }
    
    private var emptyStateView: some View {
        VStack(spacing: 24) {
            Image(systemName: "bed.double.fill")
                .font(.system(size: 64))
                .foregroundColor(.wakevePrimary)
            
            Text(String(localized: "accommodation.empty_title"))
                .font(.title2)
                .fontWeight(.bold)
            
            Text(String(localized: "accommodation.empty_message"))
                .font(.body)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 32)
            
            LiquidGlassButton(
                title: String(localized: "common.add"),
                icon: "plus",
                style: .primary,
                action: {
                    showAddSheet = true
                }
            )
        }
    }
    
    private var accommodationsList: some View {
        ScrollView {
            LazyVStack(spacing: 16) {
                ForEach(accommodations) { accommodation in
                    AccommodationCard(
                        accommodation: accommodation,
                        onEdit: {
                            selectedAccommodation = accommodation
                            showAddSheet = true
                        },
                        onDelete: {
                            accommodationToDelete = accommodation
                            showDeleteAlert = true
                        },
                        onBook: {
                            // Handle booking action
                            if let urlString = accommodation.bookingUrl,
                               let url = URL(string: urlString) {
                                UIApplication.shared.open(url)
                            }
                        }
                    )
                }
            }
            .padding()
        }
    }
    
    private func loadAccommodations() {
        // TODO: Load from repository
        isLoading = false
    }
    
    // MARK: - Comments
    
    private func loadCommentCount() {
        // TODO: Integrate with CommentRepository
        // For now, placeholder - should fetch count for section .ACCOMMODATION and sectionItemId = nil
        commentCount = 0
    }
}

struct AccommodationCard: View {
    let accommodation: AccommodationModel
    let onEdit: () -> Void
    let onDelete: () -> Void
    let onBook: () -> Void
    
    var body: some View {
        LiquidGlassCard(cornerRadius: 16, padding: 20) {
            VStack(alignment: .leading, spacing: 16) {
                // Header
                HStack(alignment: .top) {
                    VStack(alignment: .leading, spacing: 6) {
                        Text(accommodation.name)
                            .font(.headline)
                            .fontWeight(.bold)
                        
                        HStack(spacing: 6) {
                            Image(systemName: accommodation.typeIcon)
                                .font(.caption)
                                .foregroundColor(.wakevePrimary)
                            Text(accommodation.typeLabel)
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                    }
                    
                    Spacer()
                    
                    BookingStatusBadge(status: accommodation.bookingStatus)
                }
                
                LiquidGlassDivider(style: .default)
                
                // Details
                VStack(spacing: 12) {
                    InfoRow(
                        label: String(localized: "accommodation.address"),
                        value: accommodation.address,
                        icon: "location.fill"
                    )
                    
                    InfoRow(
                        label: String(localized: "accommodation.period"),
                        value: "\(accommodation.checkInDate) → \(accommodation.checkOutDate) (\(accommodation.totalNights) \(String(localized: "accommodation.nights")))",
                        icon: "calendar"
                    )
                    
                    InfoRow(
                        label: String(localized: "accommodation.capacity"),
                        value: "\(String(localized: "accommodation.capacity")): \(accommodation.capacity) \(String(localized: "accommodation.persons"))",
                        icon: "person.2.fill"
                    )
                    
                    InfoRow(
                        label: String(localized: "accommodation.price"),
                        value: formatPrice(accommodation.totalCost) + " (\(formatPrice(accommodation.pricePerNight))/\(String(localized: "accommodation.night")))",
                        icon: "eurosign.circle.fill"
                    )
                }
                
                if let notes = accommodation.notes, !notes.isEmpty {
                    Text(notes)
                        .font(.caption)
                        .foregroundColor(.secondary)
                        .padding(.top, 4)
                }
                
                // Actions
                HStack(spacing: 12) {
                    // Book button - primary action
                    if accommodation.bookingStatus != "CONFIRMED" {
                        LiquidGlassButton(
                            title: String(localized: "accommodation.book"),
                            icon: "safari.fill",
                            style: .primary,
                            size: .medium,
                            action: onBook
                        )
                    }
                    
                    Spacer()
                    
                    LiquidGlassButton(
                        title: String(localized: "common.edit"),
                        icon: "pencil",
                        style: .secondary,
                        size: .small,
                        action: onEdit
                    )
                    
                    LiquidGlassButton(
                        title: String(localized: "common.delete"),
                        icon: "trash",
                        style: .secondary,
                        size: .small,
                        action: onDelete
                    )
                }
            }
        }
    }
}

// MARK: - Booking Status Badge

struct BookingStatusBadge: View {
    let status: String

    private var statusInfo: (style: LiquidGlassBadgeStyle, label: String) {
        switch status {
        case "SEARCHING": return (.info, String(localized: "status.searching"))
        case "RESERVED": return (.warning, String(localized: "status.reserved"))
        case "CONFIRMED": return (.success, String(localized: "status.confirmed"))
        case "CANCELLED": return (.default, String(localized: "status.cancelled"))
        default: return (.default, String(localized: "common.unknown"))
        }
    }

    var body: some View {
        LiquidGlassBadge(
            text: statusInfo.label,
            style: statusInfo.style
        )
    }
}

// InfoRow is defined in Components/SharedComponents.swift

// CommentButton is defined in Components/CommentButton.swift

struct AccommodationFormSheet: View {
    let eventId: String
    let accommodation: AccommodationModel?
    let onSave: (AccommodationModel) -> Void
    
    @Environment(\.dismiss) private var dismiss
    @State private var name = ""
    @State private var selectedType = "HOTEL"
    @State private var address = ""
    @State private var capacity = ""
    @State private var pricePerNight = ""
    @State private var totalNights = ""
    @State private var checkInDate = ""
    @State private var checkOutDate = ""
    @State private var bookingUrl = ""
    @State private var notes = ""
    @State private var selectedStatus = "SEARCHING"
    
    let accommodationTypes = [
        ("HOTEL", String(localized: "accommodation.type.hotel")),
        ("AIRBNB", "Airbnb"),
        ("CAMPING", "Camping"),
        ("HOSTEL", String(localized: "accommodation.type.hostel")),
        ("VACATION_RENTAL", String(localized: "accommodation.type.vacation_rental")),
        ("OTHER", String(localized: "accommodation.type.other"))
    ]
    
    let bookingStatuses = [
        ("SEARCHING", String(localized: "status.searching")),
        ("RESERVED", String(localized: "status.reserved")),
        ("CONFIRMED", String(localized: "status.confirmed")),
        ("CANCELLED", String(localized: "status.cancelled"))
    ]
    
    var body: some View {
        NavigationView {
            Form {
                Section(String(localized: "accommodation.section.general_info")) {
                    TextField(String(localized: "accommodation.field.name"), text: $name)

                    Picker(String(localized: "accommodation.field.type"), selection: $selectedType) {
                        ForEach(accommodationTypes, id: \.0) { type in
                            Text(type.1).tag(type.0)
                        }
                    }
                    
                    TextField(String(localized: "accommodation.address"), text: $address, axis: .vertical)
                        .lineLimit(2...4)
                }
                
                Section(String(localized: "accommodation.section.capacity_duration")) {
                    TextField(String(localized: "accommodation.field.capacity"), text: $capacity)
                        .keyboardType(.numberPad)

                    TextField(String(localized: "accommodation.field.number_of_nights"), text: $totalNights)
                        .keyboardType(.numberPad)
                }
                
                Section(String(localized: "accommodation.section.pricing")) {
                    TextField(String(localized: "accommodation.field.price_per_night"), text: $pricePerNight)
                        .keyboardType(.decimalPad)
                    
                    if let price = Double(pricePerNight), let nights = Int(totalNights), nights > 0 {
                        HStack {
                            Text(String(localized: "accommodation.total"))
                                .foregroundColor(.secondary)
                            Spacer()
                            Text(formatPrice(Int(price * Double(nights) * 100)))
                                .fontWeight(.semibold)
                        }
                    }
                }
                
                Section(String(localized: "accommodation.section.dates")) {
                    TextField("Check-in (YYYY-MM-DD)", text: $checkInDate)
                    TextField("Check-out (YYYY-MM-DD)", text: $checkOutDate)
                }
                
                Section(String(localized: "accommodation.section.booking")) {
                    Picker(String(localized: "accommodation.field.status"), selection: $selectedStatus) {
                        ForEach(bookingStatuses, id: \.0) { status in
                            Text(status.1).tag(status.0)
                        }
                    }
                    
                    TextField(String(localized: "accommodation.field.booking_url"), text: $bookingUrl)
                        .keyboardType(.URL)
                        .textInputAutocapitalization(.never)
                }
                
                Section(String(localized: "accommodation.section.notes")) {
                    TextField(String(localized: "accommodation.field.notes"), text: $notes, axis: .vertical)
                        .lineLimit(3...6)
                }
            }
            .navigationTitle(accommodation == nil ? String(localized: "accommodation.add_title") : String(localized: "accommodation.edit_title"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(String(localized: "common.cancel")) { dismiss() }
                        .foregroundColor(.wakevePrimary)
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(String(localized: "common.save")) {
                        saveAccommodation()
                    }
                    .disabled(!isFormValid)
                    .fontWeight(.semibold)
                    .foregroundColor(.wakevePrimary)
                }
            }
        }
        .onAppear {
            if let accommodation = accommodation {
                name = accommodation.name
                selectedType = accommodation.type
                address = accommodation.address
                capacity = String(accommodation.capacity)
                pricePerNight = String(format: "%.2f", Double(accommodation.pricePerNight) / 100.0)
                totalNights = String(accommodation.totalNights)
                checkInDate = accommodation.checkInDate
                checkOutDate = accommodation.checkOutDate
                bookingUrl = accommodation.bookingUrl ?? ""
                notes = accommodation.notes ?? ""
                selectedStatus = accommodation.bookingStatus
            }
        }
    }
    
    private var isFormValid: Bool {
        !name.isEmpty && !address.isEmpty && !capacity.isEmpty &&
        !pricePerNight.isEmpty && !totalNights.isEmpty &&
        !checkInDate.isEmpty && !checkOutDate.isEmpty
    }
    
    private func saveAccommodation() {
        guard let capacityInt = Int(capacity),
              let priceDouble = Double(pricePerNight),
              let nightsInt = Int(totalNights) else {
            return
        }
        
        let priceInCents = Int(priceDouble * 100)
        let totalCost = priceInCents * nightsInt
        
        let newAccommodation = AccommodationModel(
            id: accommodation?.id ?? UUID().uuidString,
            eventId: eventId,
            name: name,
            type: selectedType,
            address: address,
            capacity: capacityInt,
            pricePerNight: priceInCents,
            totalNights: nightsInt,
            totalCost: totalCost,
            bookingStatus: selectedStatus,
            bookingUrl: bookingUrl.isEmpty ? nil : bookingUrl,
            checkInDate: checkInDate,
            checkOutDate: checkOutDate,
            notes: notes.isEmpty ? nil : notes,
            createdAt: accommodation?.createdAt ?? ISO8601DateFormatter().string(from: Date()),
            updatedAt: ISO8601DateFormatter().string(from: Date())
        )
        
        onSave(newAccommodation)
        dismiss()
    }
}

// MARK: - Models

struct AccommodationModel: Identifiable {
    let id: String
    let eventId: String
    let name: String
    let type: String
    let address: String
    let capacity: Int
    let pricePerNight: Int  // in cents
    let totalNights: Int
    let totalCost: Int  // in cents
    let bookingStatus: String
    let bookingUrl: String?
    let checkInDate: String
    let checkOutDate: String
    let notes: String?
    let createdAt: String
    let updatedAt: String
    
    var typeIcon: String {
        switch type {
        case "HOTEL": return "building.2.fill"
        case "AIRBNB": return "house.fill"
        case "CAMPING": return "tent.fill"
        case "HOSTEL": return "bed.double.fill"
        case "VACATION_RENTAL": return "key.fill"
        default: return "mappin.circle.fill"
        }
    }
    
    var typeLabel: String {
        switch type {
        case "HOTEL": return String(localized: "accommodation.type.hotel")
        case "AIRBNB": return "Airbnb"
        case "CAMPING": return "Camping"
        case "HOSTEL": return String(localized: "accommodation.type.hostel")
        case "VACATION_RENTAL": return String(localized: "accommodation.type.vacation_rental")
        default: return String(localized: "accommodation.type.other")
        }
    }
}

// MARK: - Helpers

private func formatPrice(_ cents: Int) -> String {
    let euros = Double(cents) / 100.0
    return String(format: "%.2f €", euros)
}

// MARK: - Preview

#Preview {
    AccommodationView(
        eventId: "event-1",
        currentUserId: "user-1",
        currentUserName: "Test User"
    )
}
