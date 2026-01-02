import SwiftUI

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
/// - Floating circular button (64pt)
/// - Liquid Glass material styling
/// - Sound wave animation when listening
/// - Full accessibility support (VoiceOver)
/// - Multi-language support via accessibility labels
/// - States: idle, listening, error
///
/// Example:
/// ```swift
/// VoiceAssistantFABView(
///     isListening: $isListening,
///     onTap: { isListening.toggle() },
///     onLongPress: { showQuickActions() }
/// )
/// ```
struct VoiceAssistantFABView: View {
    @Binding var isListening: Bool
    @Binding var hasError: Bool
    let onTap: () -> Void
    let onLongPress: () -> Void
    
    @State private var isPressed = false
    
    // MARK: - Accessibility Properties
    
    private var accessibilityLabel: String {
        if isListening {
            return "Voice assistant is listening. Double tap to stop."
        } else if hasError {
            return "Voice assistant error. Double tap to retry."
        } else {
            return "Start voice assistant. Double tap to activate, long press for quick actions."
        }
    }
    
    private var accessibilityHint: String {
        if isListening {
            return "Stops voice recognition"
        } else if hasError {
            return "Retries voice recognition"
        } else {
            return "Activates voice assistant for hands-free event management"
        }
    }
    
    var body: some View {
        ZStack {
            // Sound wave animation (only when listening and no error)
            if isListening && !hasError {
                SoundWaveAnimation()
            }
            
            // Main button circle with Liquid Glass styling
            Circle()
                .fill(backgroundColor)
                .overlay(
                    Circle()
                        .stroke(borderColor, lineWidth: 1.5)
                )
                .shadow(
                    color: buttonShadowColor,
                    radius: isListening ? 8 : 4,
                    x: 0,
                    y: isListening ? 4 : 2
                )
                .scaleEffect(isPressed ? 0.95 : (isListening ? 1.05 : 1.0))
                .animation(.spring(response: 0.3, dampingFraction: 0.6), value: isPressed)
                .animation(.easeInOut(duration: 1.0).repeatForever(autoreverses: true), value: isListening)
            
            // Microphone icon
            Image(systemName: iconName)
                .font(.system(size: 28, weight: .medium))
                .foregroundColor(iconColor)
                .scaleEffect(isPressed ? 0.9 : 1.0)
                .animation(.spring(response: 0.2, dampingFraction: 0.7), value: isPressed)
        }
        .frame(width: 64, height: 64)
        .accessibilityLabel(Text(accessibilityLabel))
        .accessibilityHint(Text(accessibilityHint))
        .accessibilityAddTraits(isListening ? [.isSelected, .playsSound] : [.isButton])
        .onTapGesture {
            onTap()
        }
        .onLongPressGesture(
            minimumDuration: 0.5,
            pressing: { pressing in
                isPressed = pressing
            },
            perform: {
                onLongPress()
            }
        )
    }
    
    // MARK: - Computed Properties
    
    private var backgroundColor: Color {
        if hasError {
            return .red.opacity(0.15)
        } else if isListening {
            return .accentColor.opacity(0.15)
        } else {
            return .clear
        }
    }
    
    private var borderColor: Color {
        if hasError {
            return .red.opacity(0.3)
        } else if isListening {
            return .accentColor.opacity(0.4)
        } else {
            return .gray.opacity(0.2)
        }
    }
    
    private var buttonShadowColor: Color {
        if hasError {
            return .black.opacity(0.1)
        } else if isListening {
            return .black.opacity(0.15)
        } else {
            return .black.opacity(0.08)
        }
    }
    
    private var iconName: String {
        if hasError {
            return "mic.slash"
        } else if isListening {
            return "mic.fill"
        } else {
            return "mic"
        }
    }
    
    private var iconColor: Color {
        if hasError {
            return .red
        } else if isListening {
            return .accentColor
        } else {
            return .secondary
        }
    }
}

// MARK: - Sound Wave Animation

/// Animated sound waves that pulse outward when the voice assistant is listening.
///
/// Features:
/// - 3 concentric circles with staggered animation
/// - 1 second loop with smooth easing
/// - Liquid Glass color integration
/// - Alpha fade for visual depth
struct SoundWaveAnimation: View {
    @State private var wave1Scale: CGFloat = 0.6
    @State private var wave1Opacity: Double = 0.5
    @State private var wave2Scale: CGFloat = 0.6
    @State private var wave2Opacity: Double = 0.5
    @State private var wave3Scale: CGFloat = 0.6
    @State private var wave3Opacity: Double = 0.5
    
    var body: some View {
        ZStack {
            // Wave 1 (outermost)
            Circle()
                .stroke(Color.accentColor.opacity(wave1Opacity), lineWidth: 2)
                .frame(width: 100, height: 100)
                .scaleEffect(wave1Scale)
            
            // Wave 2 (middle)
            Circle()
                .stroke(Color.accentColor.opacity(wave2Opacity), lineWidth: 2)
                .frame(width: 100, height: 100)
                .scaleEffect(wave2Scale)
            
            // Wave 3 (innermost)
            Circle()
                .stroke(Color.accentColor.opacity(wave3Opacity), lineWidth: 2)
                .frame(width: 100, height: 100)
                .scaleEffect(wave3Scale)
        }
        .onAppear {
            // Start animations with staggered delays
            withAnimation(
                .easeInOut(duration: 1.2)
                .repeatForever(autoreverses: true)
            ) {
                wave1Scale = 1.4
                wave1Opacity = 0
            }
            
            withAnimation(
                .easeInOut(duration: 1.2)
                .delay(0.2)
                .repeatForever(autoreverses: true)
            ) {
                wave2Scale = 1.4
                wave2Opacity = 0
            }
            
            withAnimation(
                .easeInOut(duration: 1.2)
                .delay(0.4)
                .repeatForever(autoreverses: true)
            ) {
                wave3Scale = 1.4
                wave3Opacity = 0
            }
        }
    }
}

// MARK: - Compact FAB

/// Compact voice assistant button for smaller spaces.
/// Same functionality but with reduced size (48pt).
struct VoiceAssistantCompactFABView: View {
    @Binding var isListening: Bool
    @Binding var hasError: Bool
    let onTap: () -> Void
    
    private var accessibilityLabel: String {
        if isListening {
            return "Voice assistant listening. Double tap to stop."
        } else if hasError {
            return "Voice assistant error. Double tap to retry."
        } else {
            return "Voice assistant. Double tap to activate."
        }
    }
    
    var body: some View {
        ZStack {
            // Compact sound wave animation
            if isListening && !hasError {
                CompactSoundWaveAnimation()
            }
            
            // Main button circle
            Circle()
                .fill(compactBackgroundColor)
                .overlay(
                    Circle()
                        .stroke(compactBorderColor, lineWidth: 1)
                )
                .shadow(radius: 3, y: 2)
            
            // Microphone icon
            Image(systemName: iconName)
                .font(.system(size: 22, weight: .medium))
                .foregroundColor(iconColor)
        }
        .frame(width: 48, height: 48)
        .accessibilityLabel(Text(accessibilityLabel))
        .onTapGesture {
            onTap()
        }
    }
    
    private var compactBackgroundColor: Color {
        if hasError {
            return .red.opacity(0.15)
        } else if isListening {
            return .accentColor.opacity(0.15)
        } else {
            return .clear
        }
    }
    
    private var compactBorderColor: Color {
        if hasError {
            return .red.opacity(0.3)
        } else if isListening {
            return .accentColor.opacity(0.4)
        } else {
            return .gray.opacity(0.2)
        }
    }
    
    private var iconName: String {
        if hasError {
            return "mic.slash"
        } else if isListening {
            return "mic.fill"
        } else {
            return "mic"
        }
    }
    
    private var iconColor: Color {
        if hasError {
            return .red
        } else if isListening {
            return .accentColor
        } else {
            return .secondary
        }
    }
}

/// Compact version of the sound wave animation for smaller buttons.
struct CompactSoundWaveAnimation: View {
    @State private var waveScale: CGFloat = 0.7
    @State private var waveOpacity: Double = 0.4
    
    var body: some View {
        Circle()
            .stroke(Color.accentColor.opacity(waveOpacity), lineWidth: 1.5)
            .frame(width: 70, height: 70)
            .scaleEffect(waveScale)
            .onAppear {
                withAnimation(
                    .easeInOut(duration: 1.0)
                    .repeatForever(autoreverses: true)
                ) {
                    waveScale = 1.3
                    waveOpacity = 0
                }
            }
    }
}

// MARK: - Previews

#Preview("VoiceAssistantFAB - Idle") {
    VStack(spacing: 40) {
        VoiceAssistantFABView(
            isListening: .constant(false),
            hasError: .constant(false),
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
            isListening: .constant(true),
            hasError: .constant(false),
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
            isListening: .constant(false),
            hasError: .constant(true),
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
            .frame(width: 120, height: 120)
        
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
                isListening: .constant(false),
                hasError: .constant(false),
                onTap: {}
            )
            
            VoiceAssistantCompactFABView(
                isListening: .constant(true),
                hasError: .constant(false),
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
                isListening: .constant(false),
                hasError: .constant(false),
                onTap: {},
                onLongPress: {}
            )
            Text("Idle")
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
            Text("Listening")
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
            Text("Error")
                .font(.caption2)
                .foregroundStyle(.secondary)
        }
    }
    .padding(40)
    .background(Color.gray.opacity(0.1))
}
