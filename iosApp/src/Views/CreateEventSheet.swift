import SwiftUI
import Shared

// MARK: - Create Event Sheet

/// Full-screen bottom sheet for creating an event with immersive gradient design
struct CreateEventSheet: View {
    let userId: String
    let userName: String?
    
    @Environment(\.dismiss) var dismiss
    @State private var title = ""
    @State private var description = ""
    @State private var selectedDate: Date?
    @State private var selectedLocation: String?
    @State private var hasBackgroundImage = false
    @State private var showingImagePicker = false
    
    // Date picker sheet state
    @State private var showingDatePicker = false
    @State private var isAllDay = false
    @State private var startDate = Date()
    @State private var startTime = Date()
    @State private var hasEndTime = false
    @State private var endTime = Date().addingTimeInterval(3600) // +1 hour by default
    
    var onEventCreated: (Event) -> Void = { _ in }
    
    var body: some View {
        ZStack {
            // Gradient background
            gradientBackground
            
            ScrollView(showsIndicators: false) {
                VStack(spacing: 0) {
                    // Header
                    headerView
                        .padding(.horizontal, 20)
                        .padding(.top, 12)
                    
                    // Background Image Selector
                    backgroundImageSelector
                        .padding(.top, 40)
                    
                    // Main Event Card (contains title, date, location)
                    mainEventCard
                        .padding(.top, 32)
                        .padding(.horizontal, 16)
                    
                    // Organizer Card (separate card with organizer and description)
                    organizerCard
                        .padding(.top, 16)
                        .padding(.horizontal, 16)
                    
                    // Create Button
                    createButton
                        .padding(.top, 32)
                        .padding(.horizontal, 16)
                    
                    Spacer(minLength: 40)
                }
            }
        }
        // Date Picker Popup Overlay
        if showingDatePicker {
            DateTimePickerPopup(
                isAllDay: $isAllDay,
                startDate: $startDate,
                startTime: $startTime,
                hasEndTime: $hasEndTime,
                endTime: $endTime,
                onSave: {
                    showingDatePicker = false
                    selectedDate = startDate
                },
                onCancel: {
                    showingDatePicker = false
                }
            )
            .transition(.opacity.combined(with: .scale(scale: 0.9)))
            .zIndex(100)
        }
    }
    
    // MARK: - Gradient Background
    
    private var gradientBackground: some View {
        LinearGradient(
            gradient: Gradient(colors: [
                Color(hex: "FF6B35"),  // Orange
                Color(hex: "FF4757"),  // Red-orange
                Color(hex: "8B5CF6"),  // Purple
                Color(hex: "6366F1"),  // Indigo
                Color(hex: "3B82F6"),  // Blue
                Color(hex: "1E3A8A"),  // Dark blue
            ]),
            startPoint: .top,
            endPoint: .bottom
        )
        .ignoresSafeArea()
    }
    
    // MARK: - Header View
    
    private var headerView: some View {
        HStack {
            // Close button
            Button(action: { dismiss() }) {
                Image(systemName: "xmark")
                    .font(.system(size: 20, weight: .medium))
                    .foregroundColor(.white)
                    .frame(width: 44, height: 44)
                    .background(Color.white.opacity(0.15))
                    .clipShape(Circle())
            }
            
            Spacer()
            
            // Preview button
            Button(action: {}) {
                Text("Aperçu")
                    .font(.system(size: 16, weight: .medium))
                    .foregroundColor(.white)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 8)
                    .background(Color.white.opacity(0.15))
                    .cornerRadius(20)
            }
        }
    }
    
    // MARK: - Background Image Selector
    
    private var backgroundImageSelector: some View {
        VStack(spacing: 16) {
            // Image icon
            Button(action: { showingImagePicker = true }) {
                ZStack {
                    Circle()
                        .fill(Color.white.opacity(0.1))
                        .frame(width: 80, height: 80)
                    
                    Image(systemName: "photo")
                        .font(.system(size: 32))
                        .foregroundColor(.white.opacity(0.8))
                }
            }
            
            // Add background button
            Button(action: { showingImagePicker = true }) {
                Text("Ajouter un arrière-plan")
                    .font(.system(size: 16, weight: .medium))
                    .foregroundColor(.white)
                    .padding(.horizontal, 24)
                    .padding(.vertical, 12)
                    .background(Color.white.opacity(0.15))
                    .cornerRadius(24)
            }
        }
    }
    
    // MARK: - Main Event Card
    
    private var mainEventCard: some View {
        VStack(spacing: 0) {
            // Event Title Input (inside the card now)
            ZStack(alignment: .center) {
                if title.isEmpty {
                    Text("Titre de\nl'évènement")
                        .font(.system(size: 32, weight: .bold))
                        .foregroundColor(.white.opacity(0.4))
                        .multilineTextAlignment(.center)
                        .lineSpacing(4)
                }
                
                TextField("", text: $title)
                    .font(.system(size: 32, weight: .bold))
                    .foregroundColor(.white)
                    .multilineTextAlignment(.center)
                    .minimumScaleFactor(0.5)
                    .padding(.horizontal, 24)
                    .padding(.vertical, 32)
            }
            
            Divider()
                .background(Color.white.opacity(0.1))
                .padding(.horizontal, 24)
            
            // Date & Time Row
            DetailRow(
                icon: "calendar.badge.plus",
                label: selectedDate != nil ? formattedDateTime() : "Date et heure",
                isPlaceholder: selectedDate == nil,
                iconColor: Color(hex: "8B5CF6")
            ) {
                showingDatePicker = true
            }
            
            Divider()
                .background(Color.white.opacity(0.1))
                .padding(.horizontal, 24)
            
            // Location Row
            DetailRow(
                icon: "mappin.circle.fill",
                label: selectedLocation ?? "Lieu",
                isPlaceholder: selectedLocation == nil,
                iconColor: Color(hex: "6366F1")
            ) {
                // Show location picker
            }
        }
        .background(Color(hex: "1A1A3E").opacity(0.7))
        .cornerRadius(28)
    }
    
    // MARK: - Organizer Card (separate card)
    
    private var organizerCard: some View {
        VStack(spacing: 16) {
            // Profile photo
            ZStack {
                Circle()
                    .fill(Color(hex: "FF6B35"))
                    .frame(width: 56, height: 56)
                
                if let name = userName {
                    Text(String(name.prefix(1)).uppercased())
                        .font(.system(size: 24, weight: .bold))
                        .foregroundColor(.white)
                } else {
                    Image(systemName: "person.fill")
                        .font(.system(size: 24))
                        .foregroundColor(.white)
                }
            }
            
            // Organizer text
            Text("Organisé par \(userName ?? "Vous")")
                .font(.system(size: 18, weight: .medium))
                .foregroundColor(.white)
            
            // Description button
            Button(action: {}) {
                Text("Ajouter une description")
                    .font(.system(size: 16, weight: .medium))
                    .foregroundColor(Color(hex: "1A1A3E"))
                    .padding(.horizontal, 24)
                    .padding(.vertical, 12)
                    .background(Color.white.opacity(0.9))
                    .cornerRadius(24)
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 24)
        .background(Color(hex: "0F1B3A").opacity(0.8))
        .cornerRadius(28)
    }
    

    
    // MARK: - Create Button
    
    private var createButton: some View {
        Button(action: createEvent) {
            Text("Créer l'évènement")
                .font(.system(size: 18, weight: .semibold))
                .foregroundColor(canCreate ? Color(hex: "6C5CE7") : .white.opacity(0.6))
                .frame(maxWidth: .infinity)
                .frame(height: 56)
                .background(Color.white.opacity(canCreate ? 1.0 : 0.3))
                .cornerRadius(28)
        }
        .disabled(!canCreate)
    }
    
    // MARK: - Helpers
    
    private var canCreate: Bool {
        !title.isEmpty
    }
    
    private func formattedDateTime() -> String {
        let dateFormatter = DateFormatter()
        dateFormatter.dateStyle = .medium
        dateFormatter.timeStyle = .none
        dateFormatter.locale = Locale(identifier: "fr_FR")
        
        let timeFormatter = DateFormatter()
        timeFormatter.dateFormat = "HH:mm"
        
        if isAllDay {
            return dateFormatter.string(from: startDate) + " (Jour entier)"
        } else {
            return dateFormatter.string(from: startDate) + " à " + timeFormatter.string(from: startTime)
        }
    }
    
    private func createEvent() {
        let event = Event(
            id: "event-\(Int(Date().timeIntervalSince1970 * 1000))",
            title: title,
            description: description,
            organizerId: userId,
            participants: [],
            proposedSlots: [],
            deadline: ISO8601DateFormatter().string(from: Date()),
            status: .draft,
            finalDate: nil,
            createdAt: ISO8601DateFormatter().string(from: Date()),
            updatedAt: ISO8601DateFormatter().string(from: Date()),
            eventType: .other,
            eventTypeCustom: nil,
            minParticipants: nil,
            maxParticipants: nil,
            expectedParticipants: nil,
            heroImageUrl: nil
        )
        
        onEventCreated(event)
        dismiss()
    }
}

// MARK: - Date Time Picker Popup

struct DateTimePickerPopup: View {
    @Binding var isAllDay: Bool
    @Binding var startDate: Date
    @Binding var startTime: Date
    @Binding var hasEndTime: Bool
    @Binding var endTime: Date
    
    let onSave: () -> Void
    let onCancel: () -> Void
    
    var body: some View {
        ZStack {
            // Semi-transparent overlay to block interaction with background
            Color.black.opacity(0.01)
                .ignoresSafeArea()
                .contentShape(Rectangle())
            
            // Liquid Glass Card - Centered compact popup with shadow
            VStack(spacing: 0) {
                // Header
                HStack(spacing: 0) {
                    // Close button
                    Button(action: onCancel) {
                        Image(systemName: "xmark")
                            .font(.system(size: 17, weight: .medium))
                            .foregroundColor(.white)
                            .frame(width: 24, height: 24)
                    }
                    
                    Spacer()
                    
                    Text("Date et heure")
                        .font(.system(size: 17, weight: .semibold))
                        .foregroundColor(.white)
                    
                    Spacer()
                    
                    // Save button
                    Button(action: onSave) {
                        Image(systemName: "checkmark")
                            .font(.system(size: 17, weight: .semibold))
                            .foregroundColor(.black)
                            .frame(width: 28, height: 28)
                            .background(Color.white)
                            .clipShape(Circle())
                    }
                }
                .padding(.horizontal, 12)
                .padding(.top, 12)
                
                // All Day Toggle
                HStack {
                    Text("Jour entier")
                        .font(.system(size: 16))
                        .foregroundColor(.white)
                    
                    Spacer()
                    
                    Toggle("", isOn: $isAllDay)
                        .toggleStyle(SwitchToggleStyle(tint: Color(hex: "34C759")))
                        .frame(width: 48, height: 28)
                }
                .padding(.horizontal, 14)
                .padding(.vertical, 8)
                
                Divider()
                    .background(Color.white.opacity(0.1))
                    .padding(.horizontal, 14)
                
                // Start Date/Time Section - Same row layout
                HStack(spacing: 6) {
                    Text("Début")
                        .font(.system(size: 16))
                        .foregroundColor(.white)
                    
                    Spacer()
                    
                    // Date Button
                    Button(action: {}) {
                        Text(formattedDate(startDate))
                            .font(.system(size: 14, weight: .medium))
                            .foregroundColor(.white)
                            .padding(.horizontal, 10)
                            .padding(.vertical, 5)
                            .background(Color.white.opacity(0.12))
                            .cornerRadius(12)
                    }
                    
                    if !isAllDay {
                        // Time Button
                        Button(action: {}) {
                            Text(formattedTime(startTime))
                                .font(.system(size: 14, weight: .medium))
                                .foregroundColor(.white)
                                .padding(.horizontal, 10)
                                .padding(.vertical, 5)
                                .background(Color.white.opacity(0.12))
                                .cornerRadius(12)
                        }
                    }
                }
                .padding(.horizontal, 14)
                .padding(.vertical, 8)
                
                if hasEndTime && !isAllDay {
                    Divider()
                        .background(Color.white.opacity(0.1))
                        .padding(.horizontal, 14)
                    
                    HStack(spacing: 6) {
                        Text("Fin")
                            .font(.system(size: 16))
                            .foregroundColor(.white)
                        
                        Spacer()
                        
                        Button(action: {}) {
                            Text(formattedTime(endTime))
                                .font(.system(size: 14, weight: .medium))
                                .foregroundColor(.white)
                                .padding(.horizontal, 10)
                                .padding(.vertical, 5)
                                .background(Color.white.opacity(0.12))
                                .cornerRadius(12)
                        }
                    }
                    .padding(.horizontal, 14)
                    .padding(.vertical, 8)
                }
                
                // Add End Time Link
                if !hasEndTime && !isAllDay {
                    Divider()
                        .background(Color.white.opacity(0.1))
                        .padding(.horizontal, 14)
                    
                    Button(action: { hasEndTime = true }) {
                        Text("Ajouter une heure de fin")
                            .font(.system(size: 16))
                            .foregroundColor(Color(hex: "0A84FF"))
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding(.horizontal, 14)
                            .padding(.vertical, 8)
                    }
                }
                
            }
            .frame(maxWidth: 320)
            .background(
                RoundedRectangle(cornerRadius: 24)
                    .fill(Color.white.opacity(0.15))
                    .background(.ultraThinMaterial)
            )
            .cornerRadius(24)
            .shadow(color: Color.black.opacity(0.15), radius: 40, x: 0, y: 20)
        }
    }
    
    private func formattedDate(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.dateStyle = .medium
        formatter.timeStyle = .none
        formatter.locale = Locale(identifier: "fr_FR")
        return formatter.string(from: date)
    }
    
    private func formattedTime(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "HH:mm"
        return formatter.string(from: date)
    }
}

// MARK: - Detail Row

struct DetailRow: View {
    let icon: String
    let label: String
    let isPlaceholder: Bool
    let iconColor: Color
    let action: () -> Void
    
    init(icon: String, label: String, isPlaceholder: Bool, iconColor: Color = Color(hex: "6C5CE7"), action: @escaping () -> Void) {
        self.icon = icon
        self.label = label
        self.isPlaceholder = isPlaceholder
        self.iconColor = iconColor
        self.action = action
    }
    
    var body: some View {
        Button(action: action) {
            VStack(spacing: 8) {
                Image(systemName: icon)
                    .font(.system(size: 28))
                    .foregroundColor(iconColor)
                
                Text(label)
                    .font(.system(size: 18, weight: isPlaceholder ? .regular : .medium))
                    .foregroundColor(isPlaceholder ? .white.opacity(0.9) : .white)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 20)
        }
    }
}

// MARK: - Preview

struct CreateEventSheet_Previews: PreviewProvider {
    static var previews: some View {
        CreateEventSheet(
            userId: "user-123",
            userName: "Guy MANDINA NZEZA"
        )
    }
}
