import SwiftUI

// MARK: - Auth View

/**
 * Main authentication view with Google, Apple, and Email sign-in options.
 * 
 * This view supports:
 * - Google Sign-In
 * - Apple Sign-In
 * - Email authentication with OTP
 * - Guest mode (skip authentication)
 */
struct AuthView: View {
    @State private var showEmailAuth = false
    @State private var isLoading = false
    @State private var errorMessage: String?
    
    var body: some View {
        ZStack {
            // Background gradient
            LinearGradient(
                colors: [Color(red: 0.6, green: 0.8, blue: 1.0), Color(red: 0.8, green: 0.6, blue: 1.0)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            .ignoresSafeArea()
            
            VStack {
                // Skip button (top right)
                HStack {
                    Spacer()
                    Button(action: skipToGuest) {
                        Text("Passer")
                            .font(.system(size: 17, weight: .medium))
                            .foregroundColor(.secondary)
                    }
                    .padding(.top, 16)
                    .padding(.trailing, 16)
                    .disabled(isLoading)
                }
                
                Spacer()
                
                // Main content
                VStack(spacing: 20) {
                    // Welcome text
                    VStack(spacing: 8) {
                        Text("Bienvenue sur Wakeve")
                            .font(.system(size: 28, weight: .bold))
                        Text("Connectez-vous pour commencer")
                            .font(.system(size: 16))
                            .foregroundColor(.secondary)
                    }
                    .padding(.top, 32)
                    
                    Spacer()
                    
                    // Error message
                    if let error = errorMessage {
                        Text(error)
                            .font(.system(size: 14))
                            .foregroundColor(.red)
                            .padding()
                            .background(Color.red.opacity(0.1))
                            .cornerRadius(12)
                    }
                    
                    // Google Sign-In button
                    authButton(
                        title: "Google",
                        icon: "g.circle.fill",
                        color: .white,
                        textColor: .black,
                        action: signInWithGoogle
                    )
                    
                    // Apple Sign-In button
                    authButton(
                        title: "Apple",
                        icon: "apple.logo",
                        color: .black,
                        textColor: .white,
                        action: signInWithApple
                    )
                    
                    // Email Sign-In button
                    authButton(
                        title: "Email",
                        icon: "envelope",
                        color: .white,
                        action: {
                            showEmailAuth = true
                        }
                    )
                    
                    Spacer()
                    
                    // Terms text
                    Text("En vous connectant, vous acceptez nos Conditions d'utilisation")
                        .font(.system(size: 12))
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                        .padding(.bottom, 24)
                }
                .padding(24)
                .background(.ultraThinMaterial)
                .cornerRadius(32)
                .padding(.horizontal, 16)
                .padding(.bottom, 32)
            }
            
            // Loading overlay
            if isLoading {
                Color.black.opacity(0.3)
                    .ignoresSafeArea()
                ProgressView()
                    .scaleEffect(1.5)
                    .tint(.white)
            }
        }
        .sheet(isPresented: $showEmailAuth) {
            EmailAuthView(
                isPresented: $showEmailAuth,
                isLoading: $isLoading,
                errorMessage: $errorMessage
            )
        }
    }
    
    // MARK: - Actions
    
    private func signInWithGoogle() {
        isLoading = true
        // TODO: Integrate with Kotlin AuthStateMachine via Kotlin/Native interop
        // For now, simulate a successful Google sign-in
        DispatchQueue.main.asyncAfter(deadline: .now() + 2) {
            isLoading = false
            // Navigate to main app
        }
    }
    
    private func signInWithApple() {
        isLoading = true
        // TODO: Integrate with Kotlin AuthStateMachine via Kotlin/Native interop
        DispatchQueue.main.asyncAfter(deadline: .now() + 2) {
            isLoading = false
            // Navigate to main app
        }
    }
    
    private func skipToGuest() {
        isLoading = true
        // TODO: Send SkipToGuest intent to AuthStateMachine
        DispatchQueue.main.asyncAfter(deadline: .now() + 1) {
            isLoading = false
            // Navigate to onboarding/home
        }
    }
    
    // MARK: - Helpers
    
    private func authButton(
        title: String,
        icon: String,
        color: Color,
        textColor: Color = .primary,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            HStack {
                Image(systemName: icon)
                    .font(.system(size: 24))
                Text(title)
                    .font(.system(size: 17, weight: .medium))
            }
            .frame(maxWidth: .infinity)
            .frame(height: 56)
            .background(color)
            .foregroundColor(textColor)
            .cornerRadius(16)
        }
        .disabled(isLoading)
        .padding(.horizontal, 24)
    }
}

// MARK: - Email Auth View

/**
 * Email authentication view with OTP input.
 */
struct EmailAuthView: View {
    @Binding var isPresented: Bool
    @Binding var isLoading: Bool
    @Binding var errorMessage: String?
    
    @State private var emailInput = ""
    @State private var otpInput = ""
    @State private var isOTPStage = false
    @State private var remainingTime = 300
    @State private var attemptsRemaining = 3
    
    private let timer = Timer.publish(every: 1, on: .main, in: .common).autoconnect()
    
    var body: some View {
        ZStack {
            // Background gradient
            LinearGradient(
                colors: [Color(red: 0.6, green: 0.8, blue: 1.0), Color(red: 0.8, green: 0.6, blue: 1.0)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            .ignoresSafeArea()
            
            VStack {
                // Back button
                HStack {
                    Button(action: {
                        isPresented = false
                    }) {
                        Image(systemName: "chevron.left")
                            .font(.system(size: 20, weight: .medium))
                    }
                    .padding(.top, 16)
                    .padding(.leading, 16)
                    Spacer()
                }
                
                Spacer()
                
                // Content
                VStack(spacing: 20) {
                    if isOTPStage {
                        otpInputView
                    } else {
                        emailInputView
                    }
                }
                .padding(24)
                .background(.ultraThinMaterial)
                .cornerRadius(32)
                .padding(.horizontal, 16)
                .padding(.bottom, 32)
                
                Spacer()
            }
            
            // Loading overlay
            if isLoading {
                Color.black.opacity(0.3)
                    .ignoresSafeArea()
                ProgressView()
                    .scaleEffect(1.5)
                    .tint(.white)
            }
        }
        .onReceive(timer) { _ in
            if isOTPStage && remainingTime > 0 {
                remainingTime -= 1
            }
        }
        .onDisappear {
            timer.upstream.connect().cancel()
        }
    }
    
    private var emailInputView: some View {
        VStack(spacing: 20) {
            Text("Entrez votre email")
                .font(.system(size: 24, weight: .bold))
            
            Text("Nous vous enverrons un code de vérification")
                .font(.system(size: 16))
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
            
            VStack(spacing: 8) {
                TextField("email@exemple.com", text: $emailInput)
                    .padding()
                    .background(Color.white)
                    .cornerRadius(12)
                    .onChange(of: emailInput) { _, _ in
                        errorMessage = nil
                    }
                
                if let error = errorMessage {
                    Text(error)
                        .font(.system(size: 14))
                        .foregroundColor(.red)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
            }
            
            Button(action: sendOTP) {
                Text("Envoyer le code")
                    .font(.system(size: 17, weight: .semibold))
                    .frame(maxWidth: .infinity)
                    .frame(height: 56)
                    .background(emailInput.isEmpty || isLoading ? Color.gray : Color.blue)
                    .foregroundColor(.white)
                    .cornerRadius(16)
            }
            .disabled(emailInput.isEmpty || isLoading)
            
            Spacer()
        }
    }
    
    private var otpInputView: some View {
        VStack(spacing: 20) {
            Text("Code de vérification")
                .font(.system(size: 24, weight: .bold))
            
            Text("Envoyé à \(emailInput)")
                .font(.system(size: 16))
                .foregroundColor(.secondary)
            
            // Timer
            HStack {
                Image(systemName: "clock")
                    .foregroundColor(remainingTime < 60 ? .red : .secondary)
                Text(String(format: "%02d:%02d", remainingTime / 60, remainingTime % 60))
                    .font(.system(size: 16, weight: .medium, design: .monospaced))
            }
            .padding(.vertical, 8)
            .padding(.horizontal, 16)
            .background(Color.white.opacity(0.8))
            .cornerRadius(8)
            
                VStack(spacing: 8) {
                    TextField("000000", text: $otpInput)
                        .multilineTextAlignment(.center)
                        .font(.system(size: 24, weight: .bold, design: .monospaced))
                        .padding()
                        .background(Color.white)
                        .cornerRadius(12)
                        .onChange(of: otpInput) { _, newValue in
                        errorMessage = nil
                        if newValue.count > 6 {
                            otpInput = String(newValue.prefix(6))
                        }
                        if newValue.count == 6 {
                            verifyOTP()
                        }
                    }
                
                if let error = errorMessage {
                    Text(error)
                        .font(.system(size: 14))
                        .foregroundColor(.red)
                }
                
                // Attempts remaining
                if attemptsRemaining > 0 {
                    Text("\(attemptsRemaining) tentatives restantes")
                        .font(.system(size: 12))
                        .foregroundColor(.orange)
                }
            }
            
            Button(action: verifyOTP) {
                Text("Vérifier")
                    .font(.system(size: 17, weight: .semibold))
                    .frame(maxWidth: .infinity)
                    .frame(height: 56)
                    .background(otpInput.count == 6 || isLoading ? Color.blue : Color.gray)
                    .foregroundColor(.white)
                    .cornerRadius(16)
            }
            .disabled(otpInput.count != 6 || isLoading)
            
            Button(action: resendOTP) {
                Text("Renvoyer le code")
                    .font(.system(size: 15))
                    .foregroundColor(remainingTime <= 0 || isLoading ? .blue : .gray)
            }
            .disabled(remainingTime > 0 || isLoading)
            
            Spacer()
        }
    }
    
    // MARK: - Actions
    
    private func sendOTP() {
        isLoading = true
        // TODO: Send SubmitEmail intent to AuthStateMachine
        DispatchQueue.main.asyncAfter(deadline: .now() + 1) {
            isOTPStage = true
            remainingTime = 300
            isLoading = false
        }
    }
    
    private func verifyOTP() {
        isLoading = true
        // TODO: Send SubmitOTP intent to AuthStateMachine
        DispatchQueue.main.asyncAfter(deadline: .now() + 1) {
            if otpInput == "123456" {
                // Success - navigate to main app
                isPresented = false
            } else {
                attemptsRemaining -= 1
                if attemptsRemaining == 0 {
                    errorMessage = "Trop de tentatives. Renvoyez le code."
                    isOTPStage = false
                    otpInput = ""
                    attemptsRemaining = 3
                } else {
                    errorMessage = "Code incorrect"
                }
            }
            isLoading = false
        }
    }
    
    private func resendOTP() {
        isLoading = true
        // TODO: Send ResendOTP intent to AuthStateMachine
        DispatchQueue.main.asyncAfter(deadline: .now() + 1) {
            remainingTime = 300
            isLoading = false
        }
    }
}

// MARK: - Previews

#Preview("Auth View") {
    AuthView()
}

#Preview("Email Auth") {
    EmailAuthView(
        isPresented: .constant(true),
        isLoading: .constant(false),
        errorMessage: .constant(nil)
    )
}
