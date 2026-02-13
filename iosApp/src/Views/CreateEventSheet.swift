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
                    
                    // Event Title Input
                    eventTitleInput
                        .padding(.top, 40)
                        .padding(.horizontal, 24)
                    
                    // Event Details Card
                    eventDetailsCard
                        .padding(.top, 32)
                        .padding(.horizontal, 16)
                    
                    // Description Input
                    descriptionInput
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
        .sheet(isPresented: $showingDatePicker) {
            DateTimePickerSheet(
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
        }
    }
    
    // MARK: - Gradient Background
    
    private var gradientBackground: some View {
        LinearGradient(
            gradient: Gradient(colors: [
                Color(hex: "FF6B35"),  // Orange
                Color(hex: "FF8C42"),  // Light orange
                Color(hex: "9B59B6"),  // Purple
                Color(hex: "6C5CE7"),  // Deep purple
                Color(hex: "4834D4"),  // Blue-purple
                Color(hex: "0984E3"),  // Blue
                Color(hex: "0C2461"),  // Dark blue
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
    
    // MARK: - Event Title Input
    
    private var eventTitleInput: some View {
        ZStack(alignment: .center) {
            if title.isEmpty {
                Text("Titre de\nl'évènement")
                    .font(.system(size: 36, weight: .bold))
                    .foregroundColor(.white.opacity(0.4))
                    .multilineTextAlignment(.center)
            }
            
            TextField("", text: $title)
                .font(.system(size: 36, weight: .bold))
                .foregroundColor(.white)
                .multilineTextAlignment(.center)
                .minimumScaleFactor(0.5)
        }
    }
    
    // MARK: - Event Details Card
    
    private var eventDetailsCard: some View {
        VStack(spacing: 0) {
            // Date & Time Row
            DetailRow(
                icon: "calendar.badge.plus",
                label: selectedDate != nil ? formattedDateTime() : "Date et heure",
                isPlaceholder: selectedDate == nil
            ) {
                showingDatePicker = true
            }
            
            Divider()
                .background(Color.white.opacity(0.1))
                .padding(.horizontal, 20)
            
            // Location Row
            DetailRow(
                icon: "mappin.circle.fill",
                label: selectedLocation ?? "Lieu",
                isPlaceholder: selectedLocation == nil
            ) {
                // Show location picker
            }
        }
        .padding(.vertical, 8)
        .background(Color(hex: "1A1A2E").opacity(0.6))
        .cornerRadius(24)
    }
    
    // MARK: - Description Input
    
    private var descriptionInput: some View {
        HStack {
            Spacer()
            
            if description.isEmpty {
                Button(action: {}) {
                    Text("Ajouter une description")
                        .font(.system(size: 16, weight: .medium))
                        .foregroundColor(.white)
                        .padding(.horizontal, 20)
                        .padding(.vertical, 10)
                        .background(Color.white.opacity(0.15))
                        .cornerRadius(20)
                }
            } else {
                Text(description)
                    .font(.system(size: 16))
                    .foregroundColor(.white)
                    .padding(16)
            }
            
            Spacer()
        }
        .background(Color(hex: "1A1A2E").opacity(0.6))
        .cornerRadius(24)
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

// MARK: - Date Time Picker Sheet

struct DateTimePickerSheet: View {
    @Binding var isAllDay: Bool
    @Binding var startDate: Date
    @Binding var startTime: Date
    @Binding var hasEndTime: Bool
    @Binding var endTime: Date
    
    let onSave: () -> Void
    let onCancel: () -> Void
    
    var body: some View {
        ZStack {
            // Dark semi-transparent background
            Color.black.opacity(0.4)
                .ignoresSafeArea()
            
            // Liquid Glass Card
            VStack(spacing: 0) {
                // Header
                HStack(spacing: 0) {
                    // Close button
                    Button(action: onCancel) {
                        Image(systemName: "xmark")
                            .font(.system(size: 18, weight: .medium))
                            .foregroundColor(.white)
                            .frame(width: 32, height: 32)
                    }
                    
                    Spacer()
                    
                    Text("Date et heure")
                        .font(.system(size: 17, weight: .semibold))
                        .foregroundColor(.white)
                    
                    Spacer()
                    
                    // Save button
                    Button(action: onSave) {
                        Image(systemName: "checkmark")
                            .font(.system(size: 18, weight: .semibold))
                            .foregroundColor(.black)
                            .frame(width: 32, height: 32)
                            .background(Color.white)
                            .clipShape(Circle())
                    }
                }
                .padding(.horizontal, 16)
                .padding(.top, 16)
                
                // All Day Toggle
                HStack {
                    Text("Jour entier")
                        .font(.system(size: 17))
                        .foregroundColor(.white)
                    
                    Spacer()
                    
                    Toggle("", isOn: $isAllDay)
                        .toggleStyle(SwitchToggleStyle(tint: Color(hex: "34C759")))
                        .frame(width: 51, height: 31)
                }
                .padding(.horizontal, 20)
                .padding(.vertical, 12)
                
                Divider()
                    .background(Color.white.opacity(0.15))
                    .padding(.horizontal, 20)
                
                // Start Date/Time Section - Same row layout
                HStack(spacing: 8) {
                    Text("Début")
                        .font(.system(size: 17))
                        .foregroundColor(.white)
                    
                    Spacer()
                    
                    // Date Button
                    Button(action: {}) {
                        Text(formattedDate(startDate))
                            .font(.system(size: 15, weight: .medium))
                            .foregroundColor(.white)
                            .padding(.horizontal, 14)
                            .padding(.vertical, 8)
                            .background(Color.white.opacity(0.12))
                            .cornerRadius(16)
                    }
                    
                    if !isAllDay {
                        // Time Button
                        Button(action: {}) {
                            Text(formattedTime(startTime))
                                .font(.system(size: 15, weight: .medium))
                                .foregroundColor(.white)
                                .padding(.horizontal, 14)
                                .padding(.vertical, 8)
                                .background(Color.white.opacity(0.12))
                                .cornerRadius(16)
                        }
                    }
                }
                .padding(.horizontal, 20)
                .padding(.vertical, 12)
                
                if hasEndTime && !isAllDay {
                    Divider()
                        .background(Color.white.opacity(0.15))
                        .padding(.horizontal, 20)
                    
                    HStack(spacing: 8) {
                        Text("Fin")
                            .font(.system(size: 17))
                            .foregroundColor(.white)
                        
                        Spacer()
                        
                        Button(action: {}) {
                            Text(formattedTime(endTime))
                                .font(.system(size: 15, weight: .medium))
                                .foregroundColor(.white)
                                .padding(.horizontal, 14)
                                .padding(.vertical, 8)
                                .background(Color.white.opacity(0.12))
                                .cornerRadius(16)
                        }
                    }
                    .padding(.horizontal, 20)
                    .padding(.vertical, 12)
                }
                
                // Add End Time Link
                if !hasEndTime && !isAllDay {
                    Divider()
                        .background(Color.white.opacity(0.15))
                        .padding(.horizontal, 20)
                    
                    Button(action: { hasEndTime = true }) {
                        Text("Ajouter une heure de fin")
                            .font(.system(size: 17))
                            .foregroundColor(Color(hex: "0A84FF"))
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding(.horizontal, 20)
                            .padding(.vertical, 12)
                    }
                }
                
                Spacer(minLength: 8)
            }
            .background(
                RoundedRectangle(cornerRadius: 24)
                    .fill(Color.white.opacity(0.15))
                    .background(.ultraThinMaterial)
            )
            .cornerRadius(24)
            .padding(.horizontal, 12)
            .padding(.bottom, 20)
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
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            HStack(spacing: 12) {
                Spacer()
                
                Image(systemName: icon)
                    .font(.system(size: 24))
                    .foregroundColor(Color(hex: "6C5CE7"))
                
                Text(label)
                    .font(.system(size: 18, weight: isPlaceholder ? .regular : .medium))
                    .foregroundColor(isPlaceholder ? .white.opacity(0.9) : .white)
                
                Spacer()
            }
            .padding(.vertical, 20)
            .padding(.horizontal, 24)
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
