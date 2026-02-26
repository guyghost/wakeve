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
    
    // Event info sheet state
    @State private var showingEventInfoSheet = false
    
    // Location sheet state
    @State private var showingLocationSheet = false

    // Preview state
    @State private var showingPreview = false
    
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

            // Date Picker Bottom Sheet Overlay
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
                .transition(.move(edge: .bottom).combined(with: .opacity))
                .zIndex(100)
            }
        }
        .animation(.spring(response: 0.35, dampingFraction: 0.85), value: showingDatePicker)
        .sheet(isPresented: $showingEventInfoSheet) {
            EventInfoSheet(
                description: $description,
                organizerName: .constant(userName ?? String(localized: "leaderboard.you")),
                organizerPhotoUrl: .constant(nil),
                onDismiss: {
                    showingEventInfoSheet = false
                },
                onConfirm: {
                    showingEventInfoSheet = false
                }
            )
        }
        .sheet(isPresented: $showingLocationSheet) {
            LocationSelectionSheet(
                onDismiss: {
                    showingLocationSheet = false
                },
                onConfirm: { location in
                    selectedLocation = location.name
                    showingLocationSheet = false
                }
            )
        }
        .fullScreenCover(isPresented: $showingPreview) {
            EventPreviewSheet(
                title: title,
                description: description,
                userName: userName,
                selectedDate: selectedDate,
                selectedLocation: selectedLocation,
                isAllDay: isAllDay,
                startDate: startDate,
                startTime: startTime,
                hasEndTime: hasEndTime,
                endTime: endTime
            )
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
            Button(action: { showingPreview = true }) {
                Text(String(localized: "events.preview"))
                    .font(.system(size: 16, weight: .medium))
                    .foregroundColor(title.isEmpty ? .white.opacity(0.3) : .white)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 8)
                    .background(Color.white.opacity(title.isEmpty ? 0.05 : 0.15))
                    .cornerRadius(20)
            }
            .disabled(title.isEmpty)
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
                Text(String(localized: "events.add_background"))
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
                    Text(String(localized: "events.title_placeholder"))
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
                label: selectedDate != nil ? formattedDateTime() : String(localized: "events.date_and_time"),
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
                label: selectedLocation ?? String(localized: "events.location"),
                isPlaceholder: selectedLocation == nil,
                iconColor: Color(hex: "6366F1")
            ) {
                showingLocationSheet = true
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
            Text(String(format: String(localized: "events.organized_by"), userName ?? String(localized: "leaderboard.you")))
                .font(.system(size: 18, weight: .medium))
                .foregroundColor(.white)
            
            // Description or Button
            if description.isEmpty {
                // Description button
                Button(action: {
                    showingEventInfoSheet = true
                }) {
                    Text(String(localized: "events.add_description"))
                        .font(.system(size: 16, weight: .medium))
                        .foregroundColor(Color(hex: "1A1A3E"))
                        .padding(.horizontal, 24)
                        .padding(.vertical, 12)
                        .background(Color.white.opacity(0.9))
                        .cornerRadius(24)
                }
            } else {
                // Description text (tappable to edit)
                Button(action: {
                    showingEventInfoSheet = true
                }) {
                    Text(description)
                        .font(.system(size: 16))
                        .foregroundColor(.white.opacity(0.9))
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 24)
                        .padding(.vertical, 8)
                }
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
            Text(String(localized: "events.create_event_button"))
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
            return dateFormatter.string(from: startDate) + " (\(String(localized: "events.all_day")))"
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
        ZStack(alignment: .bottom) {
            // Semi-transparent overlay to block interaction with background
            Color.black.opacity(0.4)
                .ignoresSafeArea()
                .contentShape(Rectangle())
                .onTapGesture { onCancel() }
            
            // Bottom sheet card
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

                    Text(String(localized: "events.date_and_time"))
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
                .padding(.horizontal, 16)
                .padding(.top, 16)
                .padding(.bottom, 12)

                // Grouped section with lighter background
                VStack(spacing: 0) {
                    // All Day Toggle
                    HStack {
                        Text(String(localized: "events.all_day"))
                            .font(.system(size: 16))
                            .foregroundColor(.white)

                        Spacer()

                        Toggle("", isOn: $isAllDay)
                            .toggleStyle(SwitchToggleStyle(tint: Color(hex: "34C759")))
                            .frame(width: 48, height: 28)
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 12)

                    Divider()
                        .background(Color.white.opacity(0.15))

                    // Start Date/Time Section
                    HStack(spacing: 6) {
                        Text(String(localized: "events.start"))
                            .font(.system(size: 16))
                            .foregroundColor(.white)

                        Spacer()

                        DatePicker("", selection: $startDate, displayedComponents: .date)
                            .labelsHidden()
                            .datePickerStyle(.compact)

                        if !isAllDay {
                            DatePicker("", selection: $startTime, displayedComponents: .hourAndMinute)
                                .labelsHidden()
                                .datePickerStyle(.compact)
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 12)

                    if hasEndTime && !isAllDay {
                        Divider()
                            .background(Color.white.opacity(0.15))

                        HStack(spacing: 6) {
                            Text(String(localized: "events.end"))
                                .font(.system(size: 16))
                                .foregroundColor(.white)

                            Spacer()

                            DatePicker("", selection: $endTime, displayedComponents: .hourAndMinute)
                                .labelsHidden()
                                .datePickerStyle(.compact)
                        }
                        .padding(.horizontal, 16)
                        .padding(.vertical, 12)
                    }

                    // Add End Time Link
                    if !hasEndTime && !isAllDay {
                        Divider()
                            .background(Color.white.opacity(0.15))

                        Button(action: { hasEndTime = true }) {
                            Text(String(localized: "events.add_end_time"))
                                .font(.system(size: 16))
                                .foregroundColor(Color(hex: "0A84FF"))
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .padding(.horizontal, 16)
                                .padding(.vertical, 12)
                        }
                    }
                }
                .background(Color.white.opacity(0.08))
                .cornerRadius(16)
                .padding(.horizontal, 12)
                .padding(.bottom, 16)
                .environment(\.colorScheme, .dark)
                .tint(.white)
            }
            .background(
                RoundedRectangle(cornerRadius: 24)
                    .fill(Color(hex: "1A1A3E").opacity(0.95))
                    .background(.ultraThinMaterial)
            )
            .cornerRadius(24)
            .padding(.horizontal, 16)
            .padding(.bottom, 16)
            .shadow(color: Color.black.opacity(0.25), radius: 40, x: 0, y: -10)
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

// MARK: - Event Preview Sheet

struct EventPreviewSheet: View {
    @Environment(\.dismiss) var dismiss

    let title: String
    let description: String
    let userName: String?
    let selectedDate: Date?
    let selectedLocation: String?
    let isAllDay: Bool
    let startDate: Date
    let startTime: Date
    let hasEndTime: Bool
    let endTime: Date

    var body: some View {
        ZStack(alignment: .top) {
            // Dark background behind everything
            Color(hex: "0A0A1A")
                .ignoresSafeArea()

            VStack(spacing: 0) {
                // Gradient hero card with rounded bottom corners
                ZStack(alignment: .bottom) {
                    LinearGradient(
                        gradient: Gradient(colors: [
                            Color(hex: "FF6B35"),
                            Color(hex: "FF4757"),
                            Color(hex: "8B5CF6"),
                            Color(hex: "6366F1"),
                            Color(hex: "3B82F6"),
                            Color(hex: "1E3A8A"),
                        ]),
                        startPoint: .top,
                        endPoint: .bottom
                    )
                    .clipShape(RoundedCorner(radius: 32, corners: [.bottomLeft, .bottomRight]))

                    VStack(spacing: 0) {
                        // Header
                        HStack {
                            Button(action: { dismiss() }) {
                                Image(systemName: "chevron.left")
                                    .font(.system(size: 17, weight: .semibold))
                                    .foregroundColor(.white)
                                    .frame(width: 40, height: 40)
                                    .background(Color.white.opacity(0.15))
                                    .clipShape(Circle())
                            }

                            Spacer()

                            Button(action: {}) {
                                Text(String(localized: "onboarding.next"))
                                    .font(.system(size: 16, weight: .semibold))
                                    .foregroundColor(Color(hex: "1A1A3E"))
                                    .padding(.horizontal, 20)
                                    .padding(.vertical, 10)
                                    .background(Color(hex: "F5F0E8"))
                                    .cornerRadius(20)
                            }
                        }
                        .padding(.horizontal, 16)
                        .padding(.top, 60)

                        Spacer()

                        // Event title at bottom of gradient
                        VStack(spacing: 12) {
                            Text(title)
                                .font(.system(size: 34, weight: .bold))
                                .foregroundColor(.white)
                                .multilineTextAlignment(.center)
                                .padding(.horizontal, 24)

                            if selectedDate != nil || selectedLocation != nil {
                                VStack(spacing: 6) {
                                    if selectedDate != nil {
                                        HStack(spacing: 6) {
                                            Image(systemName: "calendar")
                                                .font(.system(size: 14))
                                            Text(formattedDateTime())
                                                .font(.system(size: 15, weight: .medium))
                                        }
                                        .foregroundColor(.white.opacity(0.9))
                                    }

                                    if let location = selectedLocation {
                                        HStack(spacing: 6) {
                                            Image(systemName: "mappin")
                                                .font(.system(size: 14))
                                            Text(location)
                                                .font(.system(size: 15, weight: .medium))
                                        }
                                        .foregroundColor(.white.opacity(0.9))
                                    }
                                }
                            }
                        }
                        .padding(.bottom, 32)

                        // RSVP buttons card inside gradient
                        HStack(spacing: 0) {
                            rsvpColumn(icon: "checkmark.circle", label: String(localized: "common.yes"))

                            Rectangle()
                                .fill(Color.white.opacity(0.15))
                                .frame(width: 1, height: 40)

                            rsvpColumn(icon: "xmark.circle", label: String(localized: "common.no"))

                            Rectangle()
                                .fill(Color.white.opacity(0.15))
                                .frame(width: 1, height: 40)

                            rsvpColumn(icon: "questionmark.circle", label: String(localized: "events.rsvp.maybe"))
                        }
                        .padding(.vertical, 14)
                        .background(
                            LinearGradient(
                                colors: [Color(hex: "3B2078"), Color(hex: "2D1B69")],
                                startPoint: .top,
                                endPoint: .bottom
                            )
                        )
                        .cornerRadius(20)
                        .padding(.horizontal, 20)
                        .padding(.bottom, 48)
                    }
                }
                .frame(height: UIScreen.main.bounds.height * 0.75)

                // Bottom section - Invitation preview
                VStack(alignment: .leading, spacing: 16) {
                    Text(String(localized: "events.preview.invitation_title"))
                        .font(.system(size: 20, weight: .bold))
                        .foregroundColor(.white)

                    HStack(alignment: .top, spacing: 14) {
                        Image(systemName: "text.below.photo.fill")
                            .font(.system(size: 20))
                            .foregroundColor(Color(hex: "8B5CF6"))
                            .frame(width: 24)
                            .padding(.top, 2)

                        Text(String(localized: "events.preview.invitation_description"))
                            .font(.system(size: 15))
                            .foregroundColor(.white.opacity(0.6))
                            .lineSpacing(4)
                    }
                }
                .padding(.horizontal, 20)
                .padding(.top, 24)

                Spacer()
            }
        }
    }

    private func rsvpColumn(icon: String, label: String) -> some View {
        VStack(spacing: 6) {
            Image(systemName: icon)
                .font(.system(size: 22))
                .foregroundColor(.white.opacity(0.9))
            Text(label)
                .font(.system(size: 13, weight: .medium))
                .foregroundColor(.white.opacity(0.7))
        }
        .frame(maxWidth: .infinity)
    }

    private func formattedDateTime() -> String {
        let dateFormatter = DateFormatter()
        dateFormatter.dateStyle = .medium
        dateFormatter.timeStyle = .none
        dateFormatter.locale = Locale(identifier: "fr_FR")

        let timeFormatter = DateFormatter()
        timeFormatter.dateFormat = "HH:mm"

        var result = dateFormatter.string(from: startDate)

        if isAllDay {
            result += " (\(String(localized: "events.all_day")))"
        } else {
            result += " à " + timeFormatter.string(from: startTime)
            if hasEndTime {
                result += " - " + timeFormatter.string(from: endTime)
            }
        }

        return result
    }
}

// MARK: - Rounded Corner Shape

struct RoundedCorner: Shape {
    var radius: CGFloat
    var corners: UIRectCorner

    func path(in rect: CGRect) -> Path {
        let path = UIBezierPath(
            roundedRect: rect,
            byRoundingCorners: corners,
            cornerRadii: CGSize(width: radius, height: radius)
        )
        return Path(path.cgPath)
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
