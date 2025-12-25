import SwiftUI

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
    @Environment(\.dismiss) private var dismiss
    
    @State private var accommodations: [AccommodationModel] = []
    @State private var isLoading = false
    @State private var showAddSheet = false
    @State private var selectedAccommodation: AccommodationModel?
    @State private var showDeleteAlert = false
    @State private var accommodationToDelete: AccommodationModel?
    
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
            .navigationTitle("Hébergement")
            .navigationBarTitleDisplayMode(.large)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Fermer") { dismiss() }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button {
                        selectedAccommodation = nil
                        showAddSheet = true
                    } label: {
                        Image(systemName: "plus.circle.fill")
                            .font(.title2)
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
            .alert("Supprimer l'hébergement ?", isPresented: $showDeleteAlert, presenting: accommodationToDelete) { accommodation in
                Button("Annuler", role: .cancel) {
                    accommodationToDelete = nil
                }
                Button("Supprimer", role: .destructive) {
                    accommodations.removeAll { $0.id == accommodation.id }
                    accommodationToDelete = nil
                }
            } message: { _ in
                Text("Cette action est irréversible. Toutes les affectations de chambres seront également supprimées.")
            }
        }
        .onAppear {
            loadAccommodations()
        }
    }
    
    private var emptyStateView: some View {
        VStack(spacing: 24) {
            Image(systemName: "bed.double.fill")
                .font(.system(size: 64))
                .foregroundColor(.blue)
            
            Text("Aucun hébergement")
                .font(.title2)
                .fontWeight(.bold)
            
            Text("Ajoutez votre premier hébergement pour organiser la logistique")
                .font(.body)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 32)
            
            Button {
                showAddSheet = true
            } label: {
                Label("Ajouter", systemImage: "plus")
                    .font(.headline)
                    .padding(.horizontal, 24)
                    .padding(.vertical, 12)
            }
            .buttonStyle(.borderedProminent)
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
}

struct AccommodationCard: View {
    let accommodation: AccommodationModel
    let onEdit: () -> Void
    let onDelete: () -> Void
    
    var body: some View {
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
                        Text(accommodation.typeLabel)
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                }
                
                Spacer()
                
                BookingStatusBadge(status: accommodation.bookingStatus)
            }
            
            Divider()
            
            // Details
            VStack(spacing: 12) {
                InfoRow(
                    icon: "location.fill",
                    text: accommodation.address,
                    color: .blue
                )
                
                InfoRow(
                    icon: "calendar",
                    text: "\(accommodation.checkInDate) → \(accommodation.checkOutDate) (\(accommodation.totalNights) nuits)",
                    color: .blue
                )
                
                InfoRow(
                    icon: "person.2.fill",
                    text: "Capacité: \(accommodation.capacity) personnes",
                    color: .blue
                )
                
                InfoRow(
                    icon: "eurosign.circle.fill",
                    text: formatPrice(accommodation.totalCost) + " (\(formatPrice(accommodation.pricePerNight))/nuit)",
                    color: .green
                )
            }
            
            if let notes = accommodation.notes, !notes.isEmpty {
                Text(notes)
                    .font(.caption)
                    .foregroundColor(.secondary)
                    .padding(.top, 4)
            }
            
            // Actions
            HStack {
                Spacer()
                
                Button(action: onEdit) {
                    Label("Modifier", systemImage: "pencil")
                        .font(.subheadline)
                }
                .buttonStyle(.bordered)
                
                Button(action: onDelete) {
                    Label("Supprimer", systemImage: "trash")
                        .font(.subheadline)
                }
                .buttonStyle(.bordered)
                .tint(.red)
            }
        }
        .padding(20)
        .glassCard()
    }
}

struct InfoRow: View {
    let icon: String
    let text: String
    let color: Color
    
    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: icon)
                .font(.body)
                .foregroundColor(color)
                .frame(width: 20)
            
            Text(text)
                .font(.subheadline)
                .foregroundColor(.primary)
            
            Spacer()
        }
    }
}

struct BookingStatusBadge: View {
    let status: String
    
    var statusInfo: (color: Color, label: String) {
        switch status {
        case "SEARCHING": return (.gray, "Recherche")
        case "RESERVED": return (.orange, "Réservé")
        case "CONFIRMED": return (.green, "Confirmé")
        case "CANCELLED": return (.red, "Annulé")
        default: return (.gray, "Inconnu")
        }
    }
    
    var body: some View {
        Text(statusInfo.label)
            .font(.caption)
            .fontWeight(.medium)
            .padding(.horizontal, 12)
            .padding(.vertical, 6)
            .background(statusInfo.color.opacity(0.15))
            .foregroundColor(statusInfo.color)
            .continuousCornerRadius(8)
    }
}

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
        ("HOTEL", "Hôtel"),
        ("AIRBNB", "Airbnb"),
        ("CAMPING", "Camping"),
        ("HOSTEL", "Auberge"),
        ("VACATION_RENTAL", "Location de vacances"),
        ("OTHER", "Autre")
    ]
    
    let bookingStatuses = [
        ("SEARCHING", "Recherche"),
        ("RESERVED", "Réservé"),
        ("CONFIRMED", "Confirmé"),
        ("CANCELLED", "Annulé")
    ]
    
    var body: some View {
        NavigationView {
            Form {
                Section("Informations générales") {
                    TextField("Nom", text: $name)
                    
                    Picker("Type", selection: $selectedType) {
                        ForEach(accommodationTypes, id: \.0) { type in
                            Text(type.1).tag(type.0)
                        }
                    }
                    
                    TextField("Adresse", text: $address, axis: .vertical)
                        .lineLimit(2...4)
                }
                
                Section("Capacité et durée") {
                    TextField("Capacité (personnes)", text: $capacity)
                        .keyboardType(.numberPad)
                    
                    TextField("Nombre de nuits", text: $totalNights)
                        .keyboardType(.numberPad)
                }
                
                Section("Tarification") {
                    TextField("Prix par nuit (€)", text: $pricePerNight)
                        .keyboardType(.decimalPad)
                    
                    if let price = Double(pricePerNight), let nights = Int(totalNights), nights > 0 {
                        HStack {
                            Text("Total")
                                .foregroundColor(.secondary)
                            Spacer()
                            Text(formatPrice(Int(price * Double(nights) * 100)))
                                .fontWeight(.semibold)
                        }
                    }
                }
                
                Section("Dates") {
                    TextField("Check-in (YYYY-MM-DD)", text: $checkInDate)
                    TextField("Check-out (YYYY-MM-DD)", text: $checkOutDate)
                }
                
                Section("Réservation") {
                    Picker("Statut", selection: $selectedStatus) {
                        ForEach(bookingStatuses, id: \.0) { status in
                            Text(status.1).tag(status.0)
                        }
                    }
                    
                    TextField("URL de réservation (optionnel)", text: $bookingUrl)
                        .keyboardType(.URL)
                        .textInputAutocapitalization(.never)
                }
                
                Section("Notes") {
                    TextField("Notes (optionnel)", text: $notes, axis: .vertical)
                        .lineLimit(3...6)
                }
            }
            .navigationTitle(accommodation == nil ? "Ajouter hébergement" : "Modifier hébergement")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Annuler") { dismiss() }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Enregistrer") {
                        saveAccommodation()
                    }
                    .disabled(!isFormValid)
                    .fontWeight(.semibold)
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
        case "HOTEL": return "Hôtel"
        case "AIRBNB": return "Airbnb"
        case "CAMPING": return "Camping"
        case "HOSTEL": return "Auberge"
        case "VACATION_RENTAL": return "Location de vacances"
        default: return "Autre"
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
    AccommodationView(eventId: "event-1")
}
