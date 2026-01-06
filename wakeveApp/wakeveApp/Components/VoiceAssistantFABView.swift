import SwiftUI

// Colors are defined in Theme/WakevColors.swift

// MARK: - Voice Assistant State Enum

/// Represents the current state of the voice assistant
enum VoiceAssistantState: Equatable {
    case idle
    case listening
    case error
    
    var displayText: String {
        switch self {
        case .idle:
            return "Idle"
        case .listening:
            return "Listening"
        case .error:
            return "Error"
        }
    }
    
    var iconName: String {
        switch self {
        case .idle:
            return "mic"
        case .listening:
            return "mic.fill"
        case .error:
            return "mic.slash"
        }
    }
}

// MARK: - Voice Assistant FAB View

/// Voice Assistant Floating Action Button
///
/// A floating microphone button with visual feedback for the Wakeve voice assistant.
/// Features Liquid Glass design with sound wave animation when listening.
///
/// Requirements:
/// - voice-104: Quick Actions via Voice (floating button, sound wave animation)
/// - voice-105: Accessibility (VoiceOver support, proper accessibility labels)
///
/// Features:
/// - Liquid Glass styled FAB with spring animations
/// - Sound wave animation when listening
/// - Status badge for state indication
/// - Full accessibility support (VoiceOver)
/// - Multi-language support via accessibility labels
/// - States: idle, listening, error
///
/// Example:
/// ```swift
/// VoiceAssistantFABView(
///     state: $state,
///     onTap: { state = state == .listening ? .idle : .listening },
///     onLongPress: { showQuickActions() }
/// )
/// ```
struct VoiceAssistantFABView: View {
    @Binding var state: VoiceAssistantState
    let onTap: () -> Void
    let onLongPress: () -> Void
    
    @State private var isPressed = false
    @State private var hasAppeared = false
    
    // MARK: - Core Constants
    
    private enum Core {
        static let buttonSize: CGFloat = 64
        static let iconSize: CGFloat = 28
        static let waveSize: CGFloat = 120
        static let minScale: CGFloat = 1.0
        static let listeningScale: CGFloat = 1.05
        static let pressedScale: CGFloat = 0.95
        static let cornerRadius: CGFloat = 32
        static let shadowRadius: CGFloat = 8
        static let shadowOpacity: Double = 0.15
    }
    
    var body: some View {
        ZStack {
            // Sound wave animation (only when listening and no error)
            if state == .listening {
                SoundWaveAnimation()
            }
            
            // Main button with Liquid Glass styling
            liquidGlassButton
                .scaleEffect(scaleEffect)
                .overlay(alignment: .topTrailing) {
                    stateBadge
                        .offset(x: 4, y: -4)
                }
                .accessibilityLabel(Text(accessibilityLabel))
                .accessibilityHint(Text(accessibilityHint))
                .accessibilityAddTraits(state == .listening ? [.isSelected, .playsSound] : [.isButton])
                .simultaneousGesture(
                    DragGesture(minimumDistance: 0)
                        .onChanged { _ in isPressed = true }
                        .onEnded { _ in isPressed = false }
                )
                .onLongPressGesture(
                    minimumDuration: 0.5,
                    pressing: { pressing in
                        withAnimation(.spring(response: 0.25, dampingFraction: 0.6)) {
                            isPressed = pressing
                        }
                    },
                    perform: onLongPress
                )
                .onAppear {
                    withAnimation(.spring(response: 0.4, dampingFraction: 0.8)) {
                        hasAppeared = true
                    }
                }
        }
    }
    
    // MARK: - Liquid Glass Button Component
    
    private var liquidGlassButton: some View {
        Button(action: onTap) {
            ZStack {
                // Background gradient
                Circle()
                    .fill(backgroundGradient)
                    .overlay(
                        Circle()
                            .stroke(borderGradient, lineWidth: 1.5)
                    )
                    .shadow(
                        color: shadowColor,
                        radius: state == .listening ? Core.shadowRadius : Core.shadowRadius / 2,
                        x: 0,
                        y: state == .listening ? Core.shadowRadius / 2 : Core.shadowRadius / 4
                    )
                
                // Microphone icon
                Image(systemName: state.iconName)
                    .font(.system(size: Core.iconSize, weight: .medium))
                    .foregroundColor(iconColor)
                    .scaleEffect(isPressed ? 0.9 : 1.0)
            }
            .frame(width: Core.buttonSize, height: Core.buttonSize)
        }
        .buttonStyle(PlainButtonStyle())
    }
    
    // MARK: - State Badge Component
    
    private var stateBadge: some View {
        HStack(spacing: 4) {
            if let icon = badgeIcon {
                Image(systemName: icon)
                    .font(.caption2.weight(.medium))
            }
            if !badgeText.isEmpty {
                Text(badgeText)
                    .font(.caption2.weight(.medium))
            }
        }
        .padding(.horizontal, 8)
        .padding(.vertical, 4)
        .foregroundColor(badgeColor)
        .background(
            RoundedRectangle(cornerRadius: 12)
                .fill(badgeColor.opacity(0.15))
        )
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .stroke(badgeColor.opacity(0.3), lineWidth: 0.5)
        )
        .scaleEffect(hasAppeared ? 1.0 : 0.01)
        .animation(.spring(response: 0.3, dampingFraction: 0.7), value: hasAppeared)
        .animation(.spring(response: 0.3, dampingFraction: 0.7).delay(0.1), value: state)
    }
    
    // MARK: - Color Properties
    
    private var backgroundGradient: LinearGradient {
        LinearGradient(
            gradient: Gradient(colors: gradientColors),
            startPoint: .topLeading,
            endPoint: .bottomTrailing
        )
    }
    
    private var borderGradient: LinearGradient {
        LinearGradient(
            gradient: Gradient(colors: borderGradientColors),
            startPoint: .topLeading,
            endPoint: .bottomTrailing
        )
    }
    
    private var gradientColors: [Color] {
        switch state {
        case .idle:
            return [
                Color.wakevPrimary.opacity(0.6),
                Color.wakevAccent.opacity(0.6)
            ]
        case .listening:
            return [
                Color.wakevSuccess,
                Color.wakevSuccessLight
            ]
        case .error:
            return [
                Color.wakevError,
                Color.wakevErrorLight
            ]
        }
    }
    
    private var borderGradientColors: [Color] {
        switch state {
        case .idle:
            return [
                Color.wakevPrimary.opacity(0.4),
                Color.wakevAccent.opacity(0.4)
            ]
        case .listening:
            return [
                Color.wakevSuccess.opacity(0.5),
                Color.wakevSuccessLight.opacity(0.3)
            ]
        case .error:
            return [
                Color.wakevError.opacity(0.5),
                Color.wakevErrorLight.opacity(0.3)
            ]
        }
    }
    
    private var shadowColor: Color {
        switch state {
        case .idle:
            return Color.black.opacity(Core.shadowOpacity * 0.5)
        case .listening:
            return Color.wakevSuccess.opacity(Core.shadowOpacity)
        case .error:
            return Color.wakevError.opacity(Core.shadowOpacity)
        }
    }
    
    private var iconColor: Color {
        switch state {
        case .idle:
            return Color.wakevPrimary.opacity(0.9)
        case .listening:
            return Color.wakevSuccess
        case .error:
            return Color.wakevError
        }
    }
    
    private var badgeColor: Color {
        switch state {
        case .idle:
            return .secondary
        case .listening:
            return .green
        case .error:
            return .orange
        }
    }
    
    private var badgeText: String {
        switch state {
        case .idle:
            return ""
        case .listening:
            return ""
        case .error:
            return ""
        }
    }
    
    private var badgeIcon: String? {
        switch state {
        case .idle:
            return nil
        case .listening:
            return nil
        case .error:
            return nil
        }
    }
    
    private var scaleEffect: CGFloat {
        if isPressed {
            return Core.pressedScale
        } else if state == .listening {
            return Core.listeningScale
        } else {
            return hasAppeared ? Core.minScale : 0.01
        }
    }
    
    // MARK: - Accessibility Properties
    
    private var accessibilityLabel: String {
        switch state {
        case .listening:
            return "Voice assistant is listening. Double tap to stop."
        case .error:
            return "Voice assistant error. Double tap to retry."
        case .idle:
            return "Start voice assistant. Double tap to activate, long press for quick actions."
        }
    }
    
    private var accessibilityHint: String {
        switch state {
        case .listening:
            return "Stops voice recognition"
        case .error:
            return "Retries voice recognition"
        case .idle:
            return "Activates voice assistant for hands-free event management"
        }
    }
}

// MARK: - Sound Wave Animation

/// Animated sound waves that pulse outward when the voice assistant is listening.
///
/// Features:
/// - 3 concentric circles with staggered animation
/// - 1 second loop with smooth easing
/// - Liquid Glass color integration using design system
/// - Alpha fade for visual depth
struct SoundWaveAnimation: View {
    @State private var wave1Scale: CGFloat = 0.6
    @State private var wave1Opacity: Double = 0.5
    @State private var wave2Scale: CGFloat = 0.6
    @State private var wave2Opacity: Double = 0.5
    @State private var wave3Scale: CGFloat = 0.6
    @State private var wave3Opacity: Double = 0.5
    @State private var hasAppeared = false
    
    private enum Core {
        static let waveSize: CGFloat = 100
        static let animationDuration: Double = 1.2
        static let maxScale: CGFloat = 1.4
        static let minOpacity: Double = 0.0
        static let maxOpacity: Double = 0.5
    }
    
    var body: some View {
        ZStack {
            // Wave 1 (outermost)
            Circle()
                .stroke(Color.wakevSuccess.opacity(wave1Opacity), lineWidth: 2)
                .frame(width: Core.waveSize, height: Core.waveSize)
                .scaleEffect(wave1Scale)
            
            // Wave 2 (middle)
            Circle()
                .stroke(Color.wakevSuccess.opacity(wave2Opacity), lineWidth: 2)
                .frame(width: Core.waveSize, height: Core.waveSize)
                .scaleEffect(wave2Scale)
            
            // Wave 3 (innermost)
            Circle()
                .stroke(Color.wakevSuccess.opacity(wave3Opacity), lineWidth: 2)
                .frame(width: Core.waveSize, height: Core.waveSize)
                .scaleEffect(wave3Scale)
        }
        .scaleEffect(hasAppeared ? 1.0 : 0.01)
        .opacity(hasAppeared ? 1.0 : 0.0)
        .animation(.spring(response: 0.3, dampingFraction: 0.7), value: hasAppeared)
        .onAppear {
            withAnimation(.spring(response: 0.3, dampingFraction: 0.7)) {
                hasAppeared = true
            }
            
            // Start wave 1 animation
            withAnimation(
                .easeInOut(duration: Core.animationDuration)
                    .repeatForever(autoreverses: true)
            ) {
                wave1Scale = Core.maxScale
                wave1Opacity = Core.minOpacity
            }
            
            // Start wave 2 animation with delay
            withAnimation(
                .easeInOut(duration: Core.animationDuration)
                    .delay(0.2)
                    .repeatForever(autoreverses: true)
            ) {
                wave2Scale = Core.maxScale
                wave2Opacity = Core.minOpacity
            }
            
            // Start wave 3 animation with delay
            withAnimation(
                .easeInOut(duration: Core.animationDuration)
                    .delay(0.4)
                    .repeatForever(autoreverses: true)
            ) {
                wave3Scale = Core.maxScale
                wave3Opacity = Core.minOpacity
            }
        }
    }
}

// MARK: - Compact FAB

/// Compact voice assistant button for smaller spaces.
/// Same functionality but with reduced size (48pt).
struct VoiceAssistantCompactFABView: View {
    @Binding var state: VoiceAssistantState
    let onTap: () -> Void
    
    @State private var hasAppeared = false
    
    private enum Core {
        static let buttonSize: CGFloat = 48
        static let iconSize: CGFloat = 22
        static let waveSize: CGFloat = 70
        static let cornerRadius: CGFloat = 24
        static let shadowRadius: CGFloat = 4
        static let shadowOpacity: Double = 0.1
    }
    
    private var accessibilityLabel: String {
        switch state {
        case .listening:
            return "Voice assistant listening. Double tap to stop."
        case .error:
            return "Voice assistant error. Double tap to retry."
        case .idle:
            return "Voice assistant. Double tap to activate."
        }
    }
    
    var body: some View {
        ZStack {
            // Compact sound wave animation
            if state == .listening {
                CompactSoundWaveAnimation()
            }
            
            // Main button circle with Liquid Glass styling
            Button(action: onTap) {
                ZStack {
                    Circle()
                        .fill(compactBackgroundGradient)
                        .overlay(
                            Circle()
                                .stroke(compactBorderGradient, lineWidth: 1)
                        )
                        .shadow(
                            color: compactShadowColor,
                            radius: Core.shadowRadius,
                            x: 0,
                            y: Core.shadowRadius / 2
                        )
                    
                    Image(systemName: state.iconName)
                        .font(.system(size: Core.iconSize, weight: .medium))
                        .foregroundColor(compactIconColor)
                }
                .frame(width: Core.buttonSize, height: Core.buttonSize)
            }
            .buttonStyle(PlainButtonStyle())
            .scaleEffect(hasAppeared ? 1.0 : 0.01)
            .animation(.spring(response: 0.3, dampingFraction: 0.7), value: hasAppeared)
        }
        .accessibilityLabel(Text(accessibilityLabel))
        .onAppear {
            withAnimation(.spring(response: 0.3, dampingFraction: 0.7)) {
                hasAppeared = true
            }
        }
    }
    
    private var compactBackgroundGradient: LinearGradient {
        LinearGradient(
            gradient: Gradient(colors: compactGradientColors),
            startPoint: .topLeading,
            endPoint: .bottomTrailing
        )
    }
    
    private var compactBorderGradient: LinearGradient {
        LinearGradient(
            gradient: Gradient(colors: compactBorderGradientColors),
            startPoint: .topLeading,
            endPoint: .bottomTrailing
        )
    }
    
    private var compactGradientColors: [Color] {
        switch state {
        case .idle:
            return [Color.wakevPrimary.opacity(0.6), Color.wakevAccent.opacity(0.6)]
        case .listening:
            return [Color.wakevSuccess, Color.wakevSuccessLight]
        case .error:
            return [Color.wakevError, Color.wakevErrorLight]
        }
    }
    
    private var compactBorderGradientColors: [Color] {
        switch state {
        case .idle:
            return [Color.wakevPrimary.opacity(0.4), Color.wakevAccent.opacity(0.4)]
        case .listening:
            return [Color.wakevSuccess.opacity(0.5), Color.wakevSuccessLight.opacity(0.3)]
        case .error:
            return [Color.wakevError.opacity(0.5), Color.wakevErrorLight.opacity(0.3)]
        }
    }
    
    private var compactShadowColor: Color {
        switch state {
        case .idle:
            return Color.black.opacity(Core.shadowOpacity * 0.5)
        case .listening:
            return Color.wakevSuccess.opacity(Core.shadowOpacity)
        case .error:
            return Color.wakevError.opacity(Core.shadowOpacity)
        }
    }
    
    private var compactIconColor: Color {
        switch state {
        case .idle:
            return Color.wakevPrimary.opacity(0.9)
        case .listening:
            return Color.wakevSuccess
        case .error:
            return Color.wakevError
        }
    }
}

/// Compact version of the sound wave animation for smaller buttons.
struct CompactSoundWaveAnimation: View {
    @State private var waveScale: CGFloat = 0.7
    @State private var waveOpacity: Double = 0.4
    @State private var hasAppeared = false
    
    private enum Core {
        static let waveSize: CGFloat = 70
        static let animationDuration: Double = 1.0
        static let maxScale: CGFloat = 1.3
    }
    
    var body: some View {
        Circle()
            .stroke(Color.wakevSuccess.opacity(waveOpacity), lineWidth: 1.5)
            .frame(width: Core.waveSize, height: Core.waveSize)
            .scaleEffect(waveScale)
            .opacity(hasAppeared ? 1.0 : 0.0)
            .animation(.spring(response: 0.3, dampingFraction: 0.7), value: hasAppeared)
            .onAppear {
                withAnimation(.spring(response: 0.3, dampingFraction: 0.7)) {
                    hasAppeared = true
                }
                
                withAnimation(
                    .easeInOut(duration: Core.animationDuration)
                        .repeatForever(autoreverses: true)
                ) {
                    waveScale = Core.maxScale
                    waveOpacity = 0
                }
            }
    }
}

// MARK: - Convenience Initializers

extension VoiceAssistantFABView {
    /// Convenience initializer using isListening binding (legacy support)
    init(
        isListening: Binding<Bool>,
        hasError: Binding<Bool>,
        onTap: @escaping () -> Void,
        onLongPress: @escaping () -> Void
    ) {
        self._state = Binding(
            get: {
                if hasError.wrappedValue {
                    return .error
                } else if isListening.wrappedValue {
                    return .listening
                } else {
                    return .idle
                }
            },
            set: { newValue in
                switch newValue {
                case .listening:
                    isListening.wrappedValue = true
                    hasError.wrappedValue = false
                case .error:
                    isListening.wrappedValue = false
                    hasError.wrappedValue = true
                case .idle:
                    isListening.wrappedValue = false
                    hasError.wrappedValue = false
                }
            }
        )
        self.onTap = onTap
        self.onLongPress = onLongPress
    }
}

extension VoiceAssistantCompactFABView {
    /// Convenience initializer using isListening binding (legacy support)
    init(
        isListening: Binding<Bool>,
        hasError: Binding<Bool>,
        onTap: @escaping () -> Void
    ) {
        self._state = Binding(
            get: {
                if hasError.wrappedValue {
                    return .error
                } else if isListening.wrappedValue {
                    return .listening
                } else {
                    return .idle
                }
            },
            set: { newValue in
                switch newValue {
                case .listening:
                    isListening.wrappedValue = true
                    hasError.wrappedValue = false
                case .error:
                    isListening.wrappedValue = false
                    hasError.wrappedValue = true
                case .idle:
                    isListening.wrappedValue = false
                    hasError.wrappedValue = false
                }
            }
        )
        self.onTap = onTap
    }
}

// MARK: - Previews

#Preview("VoiceAssistantFAB - Idle") {
    VStack(spacing: 40) {
        VoiceAssistantFABView(
            state: .constant(.idle),
            onTap: {},
            onLongPress: {}
        )
        
        Text("Idle State")
            .font(.caption)
            .foregroundStyle(.secondary)
    }
    .padding()
    .background(Color.gray.opacity(0.1))
}

#Preview("VoiceAssistantFAB - Listening") {
    VStack(spacing: 40) {
        VoiceAssistantFABView(
            state: .constant(.listening),
            onTap: {},
            onLongPress: {}
        )
        
        Text("Listening State")
            .font(.caption)
            .foregroundStyle(.secondary)
    }
    .padding()
    .background(Color.gray.opacity(0.1))
}

#Preview("VoiceAssistantFAB - Error") {
    VStack(spacing: 40) {
        VoiceAssistantFABView(
            state: .constant(.error),
            onTap: {},
            onLongPress: {}
        )
        
        Text("Error State")
            .font(.caption)
            .foregroundStyle(.secondary)
    }
    .padding()
    .background(Color.gray.opacity(0.1))
}

#Preview("Sound Wave Animation") {
    VStack(spacing: 20) {
        SoundWaveAnimation()
            .frame(width: 150, height: 150)
        
        Text("Sound Wave Animation")
            .font(.caption)
            .foregroundStyle(.secondary)
    }
    .padding()
    .background(Color.gray.opacity(0.1))
}

#Preview("Compact FAB") {
    VStack(spacing: 40) {
        HStack(spacing: 20) {
            VoiceAssistantCompactFABView(
                state: .constant(.idle),
                onTap: {}
            )
            
            VoiceAssistantCompactFABView(
                state: .constant(.listening),
                onTap: {}
            )
        }
        
        Text("Compact FAB Variants")
            .font(.caption)
            .foregroundStyle(.secondary)
    }
    .padding()
    .background(Color.gray.opacity(0.1))
}

#Preview("FAB with Labels") {
    VStack(spacing: 30) {
        // Show all three states with labels
        VStack(spacing: 8) {
            VoiceAssistantFABView(
                state: .constant(.idle),
                onTap: {},
                onLongPress: {}
            )
            Text("Idle")
                .font(.caption2)
                .foregroundStyle(.secondary)
        }
        
        VStack(spacing: 8) {
            VoiceAssistantFABView(
                state: .constant(.listening),
                onTap: {},
                onLongPress: {}
            )
            Text("Listening")
                .font(.caption2)
                .foregroundStyle(.secondary)
        }
        
        VStack(spacing: 8) {
            VoiceAssistantFABView(
                state: .constant(.error),
                onTap: {},
                onLongPress: {}
            )
            Text("Error")
                .font(.caption2)
                .foregroundStyle(.secondary)
        }
    }
    .padding(40)
    .background(Color.gray.opacity(0.1))
}

#Preview("Legacy API Support") {
    VStack(spacing: 30) {
        // Test legacy binding API
        VStack(spacing: 8) {
            VoiceAssistantFABView(
                isListening: .constant(false),
                hasError: .constant(false),
                onTap: {},
                onLongPress: {}
            )
            Text("Legacy: Idle")
                .font(.caption2)
                .foregroundStyle(.secondary)
        }
        
        VStack(spacing: 8) {
            VoiceAssistantFABView(
                isListening: .constant(true),
                hasError: .constant(false),
                onTap: {},
                onLongPress: {}
            )
            Text("Legacy: Listening")
                .font(.caption2)
                .foregroundStyle(.secondary)
        }
        
        VStack(spacing: 8) {
            VoiceAssistantFABView(
                isListening: .constant(false),
                hasError: .constant(true),
                onTap: {},
                onLongPress: {}
            )
            Text("Legacy: Error")
                .font(.caption2)
                .foregroundStyle(.secondary)
        }
    }
    .padding(40)
    .background(Color.gray.opacity(0.1))
}
