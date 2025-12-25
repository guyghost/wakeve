import SwiftUI
import Shared
import PhotosUI

// MARK: - Font Styles

enum TitleFontStyle: String, CaseIterable, Hashable {
    case sanFrancisco = "SF Pro Display"
    case rounded = "SF Pro Rounded"
    case serif = "New York"
    case monospaced = "SF Mono"

    var font: Font {
        switch self {
        case .sanFrancisco:
            return .system(size: 32, weight: .bold, design: .default)
        case .rounded:
            return .system(size: 32, weight: .bold, design: .rounded)
        case .serif:
            return .system(size: 32, weight: .bold, design: .serif)
        case .monospaced:
            return .system(size: 32, weight: .bold, design: .monospaced)
        }
    }
}

/// Apple Invites-style event creation view
/// Features: Gradient background, image picker, modern form design
struct AppleInvitesEventCreationView: View {
    let userId: String
    let repository: EventRepository
    let onEventCreated: (String) -> Void
    let onBack: () -> Void

    @State private var eventTitle = ""
    @State private var eventDescription = ""
    @State private var selectedDate = Date()
    @State private var location = ""
    @State private var timeSlots: [TimeSlot] = []
    @State private var deadline = Date().addingTimeInterval(7 * 24 * 60 * 60)
    @State private var isLoading = false
    @State private var errorMessage = ""
    @State private var showError = false
    @State private var showPreview = false
    @State private var selectedBackgroundImage: PhotosPickerItem?
    @State private var backgroundImage: Image?
    @State private var backgroundUIImage: UIImage?
    @State private var dominantColor: Color = Color.clear
    @State private var selectedFontStyle: TitleFontStyle = .sanFrancisco
    @FocusState private var isTitleFieldFocused: Bool

    var body: some View {
        ZStack {
            // Background - either selected image or gradient
            if let backgroundImage = backgroundImage {
                // Blurred background on top transitioning to dominant color at bottom
                ZStack(alignment: .top) {
                    // Top: Blurred image
                    backgroundImage
                        .resizable()
                        .aspectRatio(contentMode: .fill)
                        .ignoresSafeArea()
                        .blur(radius: 20)

                    // Bottom: Dominant color extracted from image
                    LinearGradient(
                        colors: [
                            Color.clear,
                            dominantColor.opacity(0.3),
                            dominantColor.opacity(0.6),
                            dominantColor.opacity(0.8),
                            dominantColor
                        ],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                    .ignoresSafeArea()

                    // Subtle dark overlay for better text readability
                    LinearGradient(
                        colors: [
                            Color.black.opacity(0.05),
                            Color.black.opacity(0.1),
                            Color.black.opacity(0.15),
                            Color.black.opacity(0.2),
                            Color.black.opacity(0.25)
                        ],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                    .ignoresSafeArea()
                }
            } else {
                // Beautiful gradient background (Orange → Pink → Purple → Dark Blue)
                LinearGradient(
                    colors: [
                        Color(red: 1.0, green: 0.38, blue: 0.27),  // Orange-red
                        Color(red: 0.95, green: 0.26, blue: 0.42), // Pink-red
                        Color(red: 0.65, green: 0.22, blue: 0.68), // Purple
                        Color(red: 0.35, green: 0.20, blue: 0.60), // Deep purple
                        Color(red: 0.15, green: 0.15, blue: 0.35)  // Dark blue
                    ],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
                .ignoresSafeArea()
            }

            VStack(spacing: 0) {
                // Header
                HStack {
                    Button(action: onBack) {
                        Image(systemName: "xmark")
                            .font(.system(size: 18, weight: .semibold))
                            .foregroundColor(.white)
                            .frame(width: 44, height: 44)
                    }

                    Spacer()

                    Button(action: { showPreview = true }) {
                        Text("Preview")
                            .font(.system(size: 17, weight: .semibold))
                            .foregroundColor(.white)
                            .padding(.horizontal, 20)
                            .padding(.vertical, 8)
                            .background(Color.white.opacity(0.2))
                            .continuousCornerRadius(20)
                    }
                }
                .padding(.horizontal, 20)
                .padding(.top, 60)

                ScrollView {
                    VStack(spacing: 24) {
                        // Edit Background Button (overlay on background)
                        if backgroundImage != nil {
                            PhotosPicker(selection: $selectedBackgroundImage, matching: .images) {
                                Text("Edit Background")
                                    .font(.system(size: 17, weight: .semibold))
                                    .foregroundColor(.white)
                                    .padding(.horizontal, 24)
                                    .padding(.vertical, 12)
                                    .background(Color.black.opacity(0.4))
                                    .background(.ultraThinMaterial)
                                    .continuousCornerRadius(24)
                            }
                            .padding(.top, 60)
                            .onChange(of: selectedBackgroundImage) { _, newValue in
                                Task {
                                    if let data = try? await newValue?.loadTransferable(type: Data.self),
                                       let uiImage = UIImage(data: data) {
                                        backgroundImage = Image(uiImage: uiImage)
                                        backgroundUIImage = uiImage
                                        dominantColor = extractDominantColor(from: uiImage)
                                    }
                                }
                            }
                        } else {
                            // Add Background Section (when no background)
                            PhotosPicker(selection: $selectedBackgroundImage, matching: .images) {
                                VStack(spacing: 16) {
                                    ZStack {
                                        Circle()
                                            .fill(Color.white.opacity(0.2))
                                            .frame(width: 80, height: 80)

                                        Image(systemName: "photo")
                                            .font(.system(size: 36))
                                            .foregroundColor(.white.opacity(0.7))
                                    }

                                    Text("Add Background")
                                        .font(.system(size: 20, weight: .semibold))
                                        .foregroundColor(.white)
                                }
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 40)
                            }
                            .onChange(of: selectedBackgroundImage) { _, newValue in
                                Task {
                                    if let data = try? await newValue?.loadTransferable(type: Data.self),
                                       let uiImage = UIImage(data: data) {
                                        backgroundImage = Image(uiImage: uiImage)
                                        backgroundUIImage = uiImage
                                        dominantColor = extractDominantColor(from: uiImage)
                                    }
                                }
                            }
                        }

                        // Event Title Card with glassmorphism
                        VStack(spacing: 0) {
                            TextField("Event Title", text: $eventTitle)
                                .font(selectedFontStyle.font)
                                .foregroundColor(.white)
                                .multilineTextAlignment(.center)
                                .padding(.vertical, 32)
                                .padding(.horizontal, 20)
                                .focused($isTitleFieldFocused)

                            // Font Style Picker (appears when editing)
                            if isTitleFieldFocused {
                                FontStylePicker(selectedStyle: $selectedFontStyle)
                                    .padding(.horizontal, 20)
                                    .padding(.bottom, 20)
                                    .transition(.move(edge: .top).combined(with: .opacity))
                            }
                        }
                        .background(.ultraThinMaterial)
                        .background(Color.white.opacity(0.1))
                        .continuousCornerRadius(20)
                        .shadow(color: Color.black.opacity(0.1), radius: 10, x: 0, y: 5)
                        .animation(.spring(response: 0.3), value: isTitleFieldFocused)

                        // Date and Location Card with glassmorphism
                        VStack(spacing: 20) {
                            DateButton(
                                icon: "calendar.badge.clock",
                                label: "Date and Time",
                                date: selectedDate
                            )

                            Divider()
                                .background(Color.white.opacity(0.2))

                            // Location
                            HStack(spacing: 12) {
                                Image(systemName: "mappin.circle")
                                    .font(.system(size: 24))
                                    .foregroundColor(.white.opacity(0.7))

                                TextField("Location", text: $location)
                                    .font(.system(size: 17))
                                    .foregroundColor(.white)

                                Spacer()
                            }
                        }
                        .padding(24)
                        .background(.ultraThinMaterial)
                        .background(Color.white.opacity(0.1))
                        .continuousCornerRadius(20)
                        .shadow(color: Color.black.opacity(0.1), radius: 10, x: 0, y: 5)

                        // Hosted By Card with glassmorphism
                        VStack(spacing: 16) {
                            // Avatar and name
                            VStack(spacing: 12) {
                                Circle()
                                    .fill(
                                        LinearGradient(
                                            colors: [Color.blue, Color.purple],
                                            startPoint: .topLeading,
                                            endPoint: .bottomTrailing
                                        )
                                    )
                                    .frame(width: 56, height: 56)
                                    .overlay(
                                        Text(String(userId.prefix(1).uppercased()))
                                            .font(.system(size: 24, weight: .semibold))
                                            .foregroundColor(.white)
                                    )

                                Text("Hosted by \(userId)")
                                    .font(.system(size: 17, weight: .semibold))
                                    .foregroundColor(.white)
                            }

                            // Description
                            TextEditor(text: $eventDescription)
                                .font(.system(size: 17))
                                .foregroundColor(.white)
                                .frame(height: 100)
                                .scrollContentBackground(.hidden)
                                .background(Color.clear)
                                .overlay(
                                    Group {
                                        if eventDescription.isEmpty {
                                            Text("Add a description.")
                                                .font(.system(size: 17))
                                                .foregroundColor(.white.opacity(0.5))
                                                .allowsHitTesting(false)
                                        }
                                    }
                                )
                        }
                        .padding(24)
                        .background(.ultraThinMaterial)
                        .background(Color.white.opacity(0.1))
                        .continuousCornerRadius(20)
                        .shadow(color: Color.black.opacity(0.1), radius: 10, x: 0, y: 5)

                        Spacer()
                            .frame(height: 40)
                    }
                    .padding(.horizontal, 20)
                    .padding(.top, 32)
                }

                // Bottom Bar
                HStack {
                    Button(action: {}) {
                        Image(systemName: "photo.on.rectangle")
                            .font(.system(size: 24))
                            .foregroundColor(.white.opacity(0.7))
                            .frame(width: 44, height: 44)
                    }

                    Spacer()

                    Button(action: {
                        Task {
                            await createEvent()
                        }
                    }) {
                        Image(systemName: isLoading ? "circle" : "checkmark")
                            .font(.system(size: 24, weight: .semibold))
                            .foregroundColor(.white)
                            .frame(width: 44, height: 44)
                    }
                    .disabled(eventTitle.isEmpty || isLoading)
                }
                .padding(.horizontal, 20)
                .padding(.vertical, 12)
                .background(.ultraThinMaterial)
            }
        }
        .alert("Error", isPresented: $showError) {
            Button("OK", role: .cancel) {}
        } message: {
            Text(errorMessage)
        }
    }

    private func createEvent() async {
        guard !eventTitle.isEmpty else { return }

        isLoading = true

        do {
            let now = ISO8601DateFormatter().string(from: Date())

            // Create a default time slot based on selected date
            let timeSlot = TimeSlot(
                id: UUID().uuidString,
                start: ISO8601DateFormatter().string(from: selectedDate),
                end: ISO8601DateFormatter().string(from: selectedDate.addingTimeInterval(2 * 60 * 60)),
                timezone: TimeZone.current.identifier
            )

            let event = Event(
                id: UUID().uuidString,
                title: eventTitle,
                description: eventDescription.isEmpty ? location : "\(location)\n\(eventDescription)",
                organizerId: userId,
                participants: [],
                proposedSlots: [timeSlot],
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

// MARK: - Date Button Component

struct DateButton: View {
    let icon: String
    let label: String
    let date: Date

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: icon)
                .font(.system(size: 24))
                .foregroundColor(.white.opacity(0.7))

            VStack(alignment: .leading, spacing: 2) {
                Text(label)
                    .font(.system(size: 17))
                    .foregroundColor(.white)

                Text(formattedDate)
                    .font(.system(size: 15))
                    .foregroundColor(.white.opacity(0.6))
            }

            Spacer()

            Image(systemName: "chevron.right")
                .font(.system(size: 14, weight: .semibold))
                .foregroundColor(.white.opacity(0.4))
        }
    }

    private var formattedDate: String {
        let formatter = DateFormatter()
        formatter.dateFormat = "MMM d, yyyy • h:mm a"
        return formatter.string(from: date)
    }
}

// MARK: - Font Style Picker Component

struct FontStylePicker: View {
    @Binding var selectedStyle: TitleFontStyle

    var body: some View {
        HStack(spacing: 12) {
            ForEach(TitleFontStyle.allCases, id: \.self) { style in
                FontStyleButton(
                    style: style,
                    isSelected: selectedStyle == style,
                    action: {
                        selectedStyle = style
                    }
                )
            }
        }
        .padding(.top, 12)
    }
}

struct FontStyleButton: View {
    let style: TitleFontStyle
    let isSelected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text("Aa")
                .font(fontForButton)
                .foregroundColor(.white)
                .frame(width: 60, height: 44)
                .background(
                    isSelected ?
                    Color.white.opacity(0.3) :
                    Color.white.opacity(0.1)
                )
                .continuousCornerRadius(8)
                .overlay(
                    RoundedRectangle(cornerRadius: 8, style: .continuous)
                        .stroke(
                            isSelected ? Color.blue : Color.clear,
                            lineWidth: 2
                        )
                )
        }
    }

    private var fontForButton: Font {
        switch style {
        case .sanFrancisco:
            return .system(size: 20, weight: .bold, design: .default)
        case .rounded:
            return .system(size: 20, weight: .bold, design: .rounded)
        case .serif:
            return .system(size: 20, weight: .bold, design: .serif)
        case .monospaced:
            return .system(size: 20, weight: .bold, design: .monospaced)
        }
    }
}

// MARK: - Color Extraction Helper

/// Extracts the dominant/average color from an image
func extractDominantColor(from image: UIImage) -> Color {
    guard let cgImage = image.cgImage else {
        return Color.blue.opacity(0.7) // Fallback color
    }

    // Resize to small size for performance
    let size = CGSize(width: 50, height: 50)
    UIGraphicsBeginImageContext(size)
    image.draw(in: CGRect(origin: .zero, size: size))
    guard let resizedImage = UIGraphicsGetImageFromCurrentImageContext()?.cgImage else {
        UIGraphicsEndImageContext()
        return Color.blue.opacity(0.7)
    }
    UIGraphicsEndImageContext()

    // Get pixel data
    guard let dataProvider = resizedImage.dataProvider,
          let pixelData = dataProvider.data,
          let data = CFDataGetBytePtr(pixelData) else {
        return Color.blue.opacity(0.7)
    }

    var totalRed: Int = 0
    var totalGreen: Int = 0
    var totalBlue: Int = 0
    let pixelCount = Int(size.width * size.height)

    // Calculate average color from bottom half (where the gradient will be)
    let startY = Int(size.height * 0.5)
    for y in startY..<Int(size.height) {
        for x in 0..<Int(size.width) {
            let pixelIndex = ((Int(size.width) * y) + x) * 4
            totalRed += Int(data[pixelIndex])
            totalGreen += Int(data[pixelIndex + 1])
            totalBlue += Int(data[pixelIndex + 2])
        }
    }

    let bottomPixelCount = Int(size.width * size.height * 0.5)
    let avgRed = Double(totalRed) / Double(bottomPixelCount) / 255.0
    let avgGreen = Double(totalGreen) / Double(bottomPixelCount) / 255.0
    let avgBlue = Double(totalBlue) / Double(bottomPixelCount) / 255.0

    // Make the color slightly darker and more saturated for better effect
    return Color(
        red: avgRed * 0.8,
        green: avgGreen * 0.8,
        blue: avgBlue * 0.8
    )
}
