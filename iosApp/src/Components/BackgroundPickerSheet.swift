import SwiftUI
import PhotosUI

// MARK: - Event Background Model

enum EventBackground: Equatable {
    case gradient // Default gradient
    case preset(PresetBackground)
    case photo(UIImage)
    
    static func == (lhs: EventBackground, rhs: EventBackground) -> Bool {
        switch (lhs, rhs) {
        case (.gradient, .gradient):
            return true
        case (.preset(let a), .preset(let b)):
            return a == b
        case (.photo, .photo):
            return false // Photos are not equatable by content
        default:
            return false
        }
    }
}

// MARK: - Preset Background

struct PresetBackground: Identifiable, Equatable {
    let id: String
    let category: BackgroundCategory
    let emojis: [String]
    let backgroundColor: Color
    
    static func == (lhs: PresetBackground, rhs: PresetBackground) -> Bool {
        lhs.id == rhs.id
    }
}

enum BackgroundCategory: String, CaseIterable {
    case emoji = "Emoji"
    case photographic = "Photographique"
}

// MARK: - Preset Backgrounds Data

struct PresetBackgrounds {
    static let emoji: [PresetBackground] = [
        PresetBackground(
            id: "emoji_party",
            category: .emoji,
            emojis: ["🎉", "🎊", "🎈", "🎁", "🥳", "✨", "🎀", "🎵"],
            backgroundColor: Color(hex: "FF8C42")
        ),
        PresetBackground(
            id: "emoji_food",
            category: .emoji,
            emojis: ["🍕", "🍔", "🌮", "🍣", "🍩", "🧁", "🍰", "🍦"],
            backgroundColor: Color(hex: "4CAF50")
        ),
        PresetBackground(
            id: "emoji_drinks",
            category: .emoji,
            emojis: ["☕", "🍵", "🧋", "🥤", "🍷", "🍹", "🧃", "🫖"],
            backgroundColor: Color(hex: "C9A96E")
        ),
        PresetBackground(
            id: "emoji_sports",
            category: .emoji,
            emojis: ["⚽", "🏀", "🎾", "🏈", "🏐", "⛳", "🎯", "🏆"],
            backgroundColor: Color(hex: "7B1FA2")
        ),
        PresetBackground(
            id: "emoji_travel",
            category: .emoji,
            emojis: ["✈️", "🌍", "🗺️", "📸", "🏖️", "🗼", "🎒", "⛺"],
            backgroundColor: Color(hex: "9575CD")
        ),
        PresetBackground(
            id: "emoji_nature",
            category: .emoji,
            emojis: ["🌸", "🌺", "🌷", "🌻", "🌿", "🍀", "🌳", "🦋"],
            backgroundColor: Color(hex: "66BB6A")
        ),
        PresetBackground(
            id: "emoji_beach",
            category: .emoji,
            emojis: ["🏖️", "🌊", "☀️", "🐚", "🦀", "🏄", "⛱️", "🌴"],
            backgroundColor: Color(hex: "4DD0E1")
        ),
        PresetBackground(
            id: "emoji_music",
            category: .emoji,
            emojis: ["🎵", "🎶", "🎸", "🎹", "🎤", "🎧", "🥁", "🎺"],
            backgroundColor: Color(hex: "EF5350")
        ),
    ]
    
    static let photographic: [PresetBackground] = [
        PresetBackground(
            id: "photo_sunset",
            category: .photographic,
            emojis: ["🌅"],
            backgroundColor: Color(hex: "FF6B35")
        ),
        PresetBackground(
            id: "photo_night",
            category: .photographic,
            emojis: ["🌃"],
            backgroundColor: Color(hex: "1A237E")
        ),
        PresetBackground(
            id: "photo_forest",
            category: .photographic,
            emojis: ["🌲"],
            backgroundColor: Color(hex: "2E7D32")
        ),
        PresetBackground(
            id: "photo_ocean",
            category: .photographic,
            emojis: ["🌊"],
            backgroundColor: Color(hex: "0277BD")
        ),
        PresetBackground(
            id: "photo_mountain",
            category: .photographic,
            emojis: ["🏔️"],
            backgroundColor: Color(hex: "455A64")
        ),
        PresetBackground(
            id: "photo_city",
            category: .photographic,
            emojis: ["🏙️"],
            backgroundColor: Color(hex: "37474F")
        ),
    ]
    
    static var all: [PresetBackground] {
        emoji + photographic
    }
}

// MARK: - Background Picker Sheet

struct BackgroundPickerSheet: View {
    @Environment(\.dismiss) var dismiss
    @Binding var selectedBackground: EventBackground
    @State private var selectedPhotoItem: PhotosPickerItem?
    @State private var showingCamera = false
    
    var body: some View {
        NavigationStack {
            ScrollView(showsIndicators: false) {
                VStack(alignment: .leading, spacing: 24) {
                    // Source options
                    sourceOptions
                    
                    // Emoji backgrounds
                    backgroundSection(
                        title: BackgroundCategory.emoji.rawValue,
                        backgrounds: PresetBackgrounds.emoji
                    )
                    
                    // Photographic backgrounds
                    backgroundSection(
                        title: BackgroundCategory.photographic.rawValue,
                        backgrounds: PresetBackgrounds.photographic
                    )
                    
                    Spacer(minLength: 32)
                }
                .padding(.horizontal, 16)
                .padding(.top, 8)
            }
            .background(Color(hex: "1A1A3E"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .principal) {
                    Text(String(localized: "events.add_background"))
                        .font(.system(size: 17, weight: .semibold))
                        .foregroundColor(.white)
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button(action: { dismiss() }) {
                        Image(systemName: "xmark")
                            .font(.system(size: 15, weight: .medium))
                            .foregroundColor(.white)
                            .frame(width: 30, height: 30)
                            .background(Color.white.opacity(0.15))
                            .clipShape(Circle())
                    }
                }
            }
            .toolbarBackground(Color(hex: "1A1A3E"), for: .navigationBar)
            .toolbarBackground(.visible, for: .navigationBar)
        }
        .onChange(of: selectedPhotoItem) { _, newItem in
            guard let newItem else { return }
            Task {
                if let data = try? await newItem.loadTransferable(type: Data.self),
                   let image = UIImage(data: data) {
                    selectedBackground = .photo(image)
                    dismiss()
                }
            }
        }
    }
    
    // MARK: - Source Options
    
    private var sourceOptions: some View {
        HStack(spacing: 24) {
            // Photos
            PhotosPicker(selection: $selectedPhotoItem, matching: .images) {
                sourceButton(icon: "photo.on.rectangle", label: String(localized: "events.background.photos"))
            }
            
            // Camera
            Button(action: { showingCamera = true }) {
                sourceButton(icon: "camera.fill", label: String(localized: "events.background.camera"))
            }
            .fullScreenCover(isPresented: $showingCamera) {
                CameraPickerView { image in
                    if let image {
                        selectedBackground = .photo(image)
                        dismiss()
                    }
                }
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.top, 8)
    }
    
    private func sourceButton(icon: String, label: String) -> some View {
        VStack(spacing: 8) {
            Image(systemName: icon)
                .font(.system(size: 24))
                .foregroundColor(.white)
                .frame(width: 56, height: 56)
                .background(Color.white.opacity(0.12))
                .clipShape(RoundedRectangle(cornerRadius: 16))
            
            Text(label)
                .font(.system(size: 13, weight: .medium))
                .foregroundColor(.white.opacity(0.8))
        }
    }
    
    // MARK: - Background Section
    
    private func backgroundSection(title: String, backgrounds: [PresetBackground]) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(title)
                .font(.system(size: 20, weight: .bold))
                .foregroundColor(.white)
            
            LazyVGrid(columns: [
                GridItem(.flexible(), spacing: 12),
                GridItem(.flexible(), spacing: 12),
                GridItem(.flexible(), spacing: 12)
            ], spacing: 12) {
                ForEach(backgrounds) { bg in
                    backgroundCard(bg)
                }
            }
        }
    }
    
    private func backgroundCard(_ background: PresetBackground) -> some View {
        Button(action: {
            selectedBackground = .preset(background)
            dismiss()
        }) {
            ZStack {
                RoundedRectangle(cornerRadius: 16)
                    .fill(background.backgroundColor)
                
                // Scatter emojis
                EmojiScatterView(emojis: background.emojis)
                
                // Selection indicator
                if case .preset(let selected) = selectedBackground, selected.id == background.id {
                    RoundedRectangle(cornerRadius: 16)
                        .stroke(Color.white, lineWidth: 3)
                    
                    Image(systemName: "checkmark.circle.fill")
                        .font(.system(size: 24))
                        .foregroundColor(.white)
                        .shadow(radius: 4)
                }
            }
            .aspectRatio(0.75, contentMode: .fit)
        }
    }
}

// MARK: - Emoji Scatter View

struct EmojiScatterView: View {
    let emojis: [String]
    var density: EmojiDensity = .normal
    
    enum EmojiDensity {
        case normal    // For picker thumbnails
        case high      // For full-screen background
    }
    
    var body: some View {
        GeometryReader { geometry in
            let scattered = scatteredEmojis(in: geometry.size)
            ForEach(Array(scattered.enumerated()), id: \.offset) { _, item in
                Text(item.emoji)
                    .font(.system(size: item.size))
                    .position(x: item.x, y: item.y)
                    .rotationEffect(.degrees(item.rotation))
            }
        }
        .clipped()
    }
    
    private struct ScatteredEmoji {
        let emoji: String
        let x: CGFloat
        let y: CGFloat
        let size: CGFloat
        let rotation: Double
    }
    
    private func scatteredEmojis(in size: CGSize) -> [ScatteredEmoji] {
        guard size.width > 0, size.height > 0 else { return [] }
        
        switch density {
        case .normal:
            return normalDensity(in: size)
        case .high:
            return highDensity(in: size)
        }
    }
    
    // Compact layout for picker thumbnails
    private func normalDensity(in size: CGSize) -> [ScatteredEmoji] {
        let slots: [(CGFloat, CGFloat, CGFloat, Double)] = [
            (0.20, 0.15, 22, -15),
            (0.72, 0.18, 18, 10),
            (0.42, 0.35, 24, -5),
            (0.85, 0.45, 20, 20),
            (0.15, 0.55, 19, -10),
            (0.62, 0.60, 23, 15),
            (0.35, 0.78, 21, -20),
            (0.80, 0.80, 17, 5),
        ]
        
        return (0..<emojis.count).map { i in
            let s = slots[i % slots.count]
            return ScatteredEmoji(
                emoji: emojis[i],
                x: size.width * s.0,
                y: size.height * s.1,
                size: s.2,
                rotation: s.3
            )
        }
    }
    
    // Larger emojis spread across the full screen
    private func highDensity(in size: CGSize) -> [ScatteredEmoji] {
        // Place many larger emojis across the full screen
        let slots: [(CGFloat, CGFloat, CGFloat, Double)] = [
            // Top area
            (0.12, 0.05, 44, -12),
            (0.55, 0.03, 36, 18),
            (0.88, 0.07, 40, -8),
            (0.35, 0.10, 48, 5),
            (0.72, 0.12, 34, -20),
            // Upper middle
            (0.08, 0.18, 42, 15),
            (0.45, 0.20, 50, -6),
            (0.82, 0.22, 38, 22),
            (0.25, 0.26, 46, -18),
            (0.65, 0.28, 40, 10),
            // Center
            (0.15, 0.35, 52, -10),
            (0.50, 0.38, 44, 14),
            (0.85, 0.36, 36, -25),
            (0.35, 0.42, 48, 8),
            (0.70, 0.44, 42, -15),
            // Lower middle
            (0.10, 0.52, 46, 20),
            (0.42, 0.55, 38, -12),
            (0.78, 0.53, 50, 6),
            (0.25, 0.60, 44, -22),
            (0.60, 0.62, 40, 16),
            // Bottom area
            (0.18, 0.72, 48, -8),
            (0.52, 0.74, 42, 12),
            (0.85, 0.70, 36, -18),
            (0.38, 0.80, 50, 5),
            (0.72, 0.82, 44, -14),
            (0.08, 0.88, 38, 20),
            (0.55, 0.90, 46, -10),
            (0.90, 0.92, 40, 8),
        ]
        
        return slots.enumerated().map { index, s in
            ScatteredEmoji(
                emoji: emojis[index % emojis.count],
                x: size.width * s.0,
                y: size.height * s.1,
                size: s.2,
                rotation: s.3
            )
        }
    }
}

// MARK: - Camera Picker

struct CameraPickerView: UIViewControllerRepresentable {
    let onImagePicked: (UIImage?) -> Void
    @Environment(\.dismiss) var dismiss
    
    func makeUIViewController(context: Context) -> UIImagePickerController {
        let picker = UIImagePickerController()
        picker.sourceType = .camera
        picker.delegate = context.coordinator
        return picker
    }
    
    func updateUIViewController(_ uiViewController: UIImagePickerController, context: Context) {}
    
    func makeCoordinator() -> Coordinator {
        Coordinator(onImagePicked: onImagePicked, dismiss: dismiss)
    }
    
    class Coordinator: NSObject, UIImagePickerControllerDelegate, UINavigationControllerDelegate {
        let onImagePicked: (UIImage?) -> Void
        let dismiss: DismissAction
        
        init(onImagePicked: @escaping (UIImage?) -> Void, dismiss: DismissAction) {
            self.onImagePicked = onImagePicked
            self.dismiss = dismiss
        }
        
        func imagePickerController(_ picker: UIImagePickerController, didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey: Any]) {
            let image = info[.originalImage] as? UIImage
            onImagePicked(image)
            dismiss()
        }
        
        func imagePickerControllerDidCancel(_ picker: UIImagePickerController) {
            onImagePicked(nil)
            dismiss()
        }
    }
}
