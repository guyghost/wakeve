import SwiftUI
import Shared

/// Modern event creation view inspired by Apple Invites
/// Features: Immersive background, glassmorphism cards, and intuitive inputs
struct ModernEventCreationView: View {
    @State private var eventTitle = ""
    @State private var eventDescription = ""
    @State private var timeSlots: [TimeSlot] = []
    @State private var deadline = Date().addingTimeInterval(7 * 24 * 60 * 60) // 1 week from now
    @State private var isLoading = false
    @State private var errorMessage = ""
    @State private var showError = false
    @State private var hasCustomBackground = false
    
    // For demo purposes, we'll toggle between gradient and a placeholder image state
    // In a real app, this would hold the selected image data
    
    let userId: String
    let repository: EventRepository
    let onEventCreated: (String) -> Void
    let onBack: () -> Void

    var body: some View {
        ZStack {
            // 1. Background Layer
            backgroundLayer
                .ignoresSafeArea()
            
            // 2. Main Content
            VStack(spacing: 0) {
                // Top Bar
                HStack {
                    Button(action: onBack) {
                        Image(systemName: "xmark")
                            .font(.system(size: 17, weight: .semibold))
                            .foregroundColor(.white)
                            .frame(width: 32, height: 32)
                            .background(.ultraThinMaterial)
                            .clipShape(Circle())
                    }
                    
                    Spacer()
                    
                    Button {
                        // Preview action (placeholder)
                    } label: {
                        Text("Preview")
                            .font(.system(size: 15, weight: .semibold))
                            .foregroundColor(.white)
                            .padding(.horizontal, 16)
                            .padding(.vertical, 8)
                            .background(.ultraThinMaterial)
                            .clipShape(Capsule())
                    }
                }
                .padding(.horizontal, 20)
                .padding(.top, 60) // Adjust for safe area
                
                Spacer()
                
                // "Edit Background" Button
                Button {
                    withAnimation {
                        hasCustomBackground.toggle()
                    }
                } label: {
                    Text(hasCustomBackground ? "Edit Background" : "Add Background")
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundColor(.white)
                        .padding(.horizontal, 24)
                        .padding(.vertical, 12)
                        .background(Color(red: 0.6, green: 0.4, blue: 0.0).opacity(0.8)) // Gold/Brownish color from screenshot
                        .clipShape(Capsule())
                        .shadow(color: .black.opacity(0.2), radius: 4, x: 0, y: 2)
                }
                .padding(.bottom, 40)
                
                // Bottom Card
                VStack(spacing: 24) {
                    // Event Title
                    TextField("Event Title", text: $eventTitle)
                        .font(.system(size: 34, weight: .bold))
                        .foregroundColor(.white)
                        .multilineTextAlignment(.center)
                        .placeholder(when: eventTitle.isEmpty, alignment: .center) {
                            Text("Event Title")
                                .font(.system(size: 34, weight: .bold))
                                .foregroundColor(.white.opacity(0.5))
                                .multilineTextAlignment(.center)
                        }
                        .padding(.top, 8)
                    
                    // Details List
                    VStack(spacing: 0) {
                        // Date and Time
                        Button {
                            showDatePicker = true
                        } label: {
                            HStack(spacing: 16) {
                                Image(systemName: "calendar.badge.plus")
                                    .font(.system(size: 20))
                                    .frame(width: 24)
                                    .foregroundColor(.white.opacity(0.9))
                                
                                VStack(alignment: .leading, spacing: 2) {
                                    Text("Date and Time")
                                        .font(.system(size: 17, weight: .medium))
                                        .foregroundColor(.white)
                                    
                                    if !timeSlots.isEmpty {
                                        Text(timeSlots[0].start)
                                            .font(.system(size: 15))
                                            .foregroundColor(.white.opacity(0.7))
                                    }
                                }
                                
                                Spacer()
                            }
                            .padding(.vertical, 16)
                        }
                        
                        Divider().background(Color.white.opacity(0.2))
                        
                        // Location
                        Button {
                            // Open location picker
                        } label: {
                            HStack(spacing: 16) {
                                Image(systemName: "mappin.and.ellipse")
                                    .font(.system(size: 20))
                                    .frame(width: 24)
                                    .foregroundColor(.white.opacity(0.9))
                                
                                Text("Location")
                                    .font(.system(size: 17, weight: .medium))
                                    .foregroundColor(.white)
                                
                                Spacer()
                            }
                            .padding(.vertical, 16)
                        }
                    }
                    .padding(.horizontal, 4)
                    
                    // Host Info
                    HStack(spacing: 12) {
                        // Avatar Placeholder
                        Circle()
                            .fill(Color.white.opacity(0.2))
                            .frame(width: 44, height: 44)
                            .overlay(
                                Image(systemName: "person.fill")
                                    .foregroundColor(.white)
                                    .font(.system(size: 20))
                            )
                        
                        VStack(alignment: .leading, spacing: 2) {
                            Text("Hosted by You")
                                .font(.system(size: 15, weight: .semibold))
                                .foregroundColor(.white)
                            
                            TextField("Add a description.", text: $eventDescription)
                                .font(.system(size: 15))
                                .foregroundColor(.white.opacity(0.9))
                                .placeholder(when: eventDescription.isEmpty) {
                                    Text("Add a description.")
                                        .font(.system(size: 15))
                                        .foregroundColor(.white.opacity(0.5))
                                }
                        }
                    }
                    .padding(16)
                    .background(Color.black.opacity(0.2))
                    .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                }
                .padding(24)
                .background(
                    // Gradient Card Background matching the screenshot (Green -> Blue)
                    LinearGradient(
                        colors: [
                            Color(red: 0.0, green: 0.5, blue: 0.2).opacity(0.9), // Green
                            Color(red: 0.0, green: 0.2, blue: 0.6).opacity(0.9)  // Blue
                        ],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                )
                .clipShape(RoundedRectangle(cornerRadius: 32, style: .continuous))
                .padding(.horizontal, 16)
                .padding(.bottom, 16)
                
                // Bottom Action Bar
                HStack {
                    Button {
                        // Shared Album action
                    } label: {
                        VStack(spacing: 4) {
                            Image(systemName: "photo.on.rectangle")
                                .font(.system(size: 20))
                            Text("Shared Album")
                                .font(.system(size: 10))
                        }
                        .foregroundColor(.white.opacity(0.8))
                    }
                    
                    Spacer()
                    
                    Button(action: onBack) {
                        Image(systemName: "xmark")
                            .font(.system(size: 17, weight: .medium))
                            .foregroundColor(.white.opacity(0.8))
                    }
                }
                .padding(.horizontal, 32)
                .padding(.top, 16)
                .padding(.bottom, 8)
            }
        }
        .sheet(isPresented: $showDatePicker) {
            VStack {
                DatePicker("Select Date", selection: $selectedDate)
                    .datePickerStyle(.graphical)
                    .padding()
                
                Button("Done") {
                    showDatePicker = false
                    // Update timeSlots with single slot for now
                    let start = ISO8601DateFormatter().string(from: selectedDate)
                    let end = ISO8601DateFormatter().string(from: selectedDate.addingTimeInterval(3600))
                    timeSlots = [TimeSlot(id: UUID().uuidString, start: start, end: end, timezone: TimeZone.current.identifier)]
                }
                .padding()
                .buttonStyle(.borderedProminent)
            }
            .presentationDetents([.medium])
        }
        .alert("Error", isPresented: $showError) {
            Button("OK", role: .cancel) {}
        } message: {
            Text(errorMessage)
        }
    }
    
    // MARK: - Subviews
    
    private var backgroundLayer: some View {
        Group {
            if hasCustomBackground {
                // Placeholder for image background
                GeometryReader { proxy in
                    Image(systemName: "party.popper.fill") // Placeholder
                        .resizable()
                        .aspectRatio(contentMode: .fill)
                        .frame(width: proxy.size.width, height: proxy.size.height)
                        .overlay(Color.black.opacity(0.2)) // Dim for text readability
                        .background(Color.orange) // Fallback
                }
            } else {
                // Default Gradient - More vibrant Apple-like colors
                LinearGradient(
                    colors: [
                        Color(red: 1.0, green: 0.3, blue: 0.2), // Bright Red/Orange
                        Color(red: 0.8, green: 0.1, blue: 0.5), // Magenta
                        Color(red: 0.4, green: 0.0, blue: 0.8), // Purple
                        Color(red: 0.1, green: 0.1, blue: 0.6)  // Deep Blue
                    ],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
            }
        }
    }
    
    private var canCreate: Bool {
        !eventTitle.isEmpty
    }
    
    @State private var showDatePicker = false
    @State private var selectedDate = Date()
    

    


    private func createEvent() async {
        guard canCreate else { return }

        isLoading = true

        do {
            // Ensure we have at least one slot if none defined (use current time if needed)
            if timeSlots.isEmpty {
                let start = ISO8601DateFormatter().string(from: selectedDate)
                let end = ISO8601DateFormatter().string(from: selectedDate.addingTimeInterval(3600))
                timeSlots = [TimeSlot(id: UUID().uuidString, start: start, end: end, timezone: TimeZone.current.identifier)]
            }

            let now = ISO8601DateFormatter().string(from: Date())
            let event = Event(
                id: UUID().uuidString,
                title: eventTitle,
                description: eventDescription,
                organizerId: userId,
                participants: [],
                proposedSlots: timeSlots,
                deadline: ISO8601DateFormatter().string(from: deadline),
                status: EventStatus.draft,
                finalDate: nil,
                createdAt: now,
                updatedAt: now
            )

            let result = try await repository.createEvent(event: event)

            if let createdEvent = result as? Event {
                isLoading = false
                onEventCreated(createdEvent.id)
            } else {
                isLoading = false
                errorMessage = "Failed to create event"
                showError = true
            }
        } catch {
            isLoading = false
            errorMessage = error.localizedDescription
            showError = true
        }
    }
}



