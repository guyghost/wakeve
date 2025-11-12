import SwiftUI
import Shared

struct LoginView: View {
    @StateObject private var authManager: AuthenticationManager
    @State private var email = ""
    @State private var password = ""
    @State private var isShowingSignUp = false
    
    init(authRepository: AuthRepository) {
        _authManager = StateObject(wrappedValue: AuthenticationManager(authRepository: authRepository))
    }
    
    var body: some View {
        ZStack {
            // Fond dégradé
            LinearGradient(
                gradient: Gradient(colors: [
                    LiquidGlassDesign.backgroundColor,
                    LiquidGlassDesign.backgroundColor.opacity(0.95)
                ]),
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            .ignoresSafeArea()
            
            ScrollView {
                VStack(spacing: LiquidGlassDesign.spacingL) {
                    // Header
                    VStack(spacing: LiquidGlassDesign.spacingM) {
                        Image(systemName: "calendar.circle.fill")
                            .font(.system(size: 64))
                            .foregroundColor(LiquidGlassDesign.accentBlue)
                        
                        VStack(spacing: LiquidGlassDesign.spacingS) {
                            Text("Wakeve")
                                .font(LiquidGlassDesign.titleL)
                            Text("Planifiez vos événements ensemble")
                                .font(LiquidGlassDesign.bodySmall)
                                .foregroundColor(.secondary)
                        }
                    }
                    .padding(.vertical, LiquidGlassDesign.spacingXL)
                    
                    if !isShowingSignUp {
                        loginForm
                    } else {
                        signUpForm
                    }
                    
                    Spacer()
                }
                .padding(.horizontal, LiquidGlassDesign.spacingL)
                .padding(.vertical, LiquidGlassDesign.spacingL)
            }
            
            // Loading overlay
            if authManager.isLoading {
                ProgressView()
                    .scaleEffect(1.5)
                    .background(Color.black.opacity(0.3))
                    .ignoresSafeArea()
            }
        }
        .alert("Erreur", isPresented: .constant(authManager.errorMessage != nil)) {
            Button("OK") {
                authManager.errorMessage = nil
            }
        } message: {
            if let error = authManager.errorMessage {
                Text(error)
            }
        }
    }
    
    // MARK: - Login Form
    
    private var loginForm: some View {
        VStack(spacing: LiquidGlassDesign.spacingL) {
            VStack(spacing: LiquidGlassDesign.spacingM) {
                VStack(alignment: .leading, spacing: LiquidGlassDesign.spacingS) {
                    Text("Email")
                        .font(LiquidGlassDesign.titleS)
                    TextField("votre@email.com", text: $email)
                        .textFieldStyle(LiquidGlassTextFieldStyle())
                        .keyboardType(.emailAddress)
                        .autocorrectionDisabled()
                }
                
                VStack(alignment: .leading, spacing: LiquidGlassDesign.spacingS) {
                    Text("Mot de passe")
                        .font(LiquidGlassDesign.titleS)
                    SecureField("••••••••", text: $password)
                        .textFieldStyle(LiquidGlassTextFieldStyle())
                }
            }
            .liquidGlassCard()
            
            // Error message
            if let error = authManager.errorMessage {
                HStack(spacing: LiquidGlassDesign.spacingM) {
                    Image(systemName: "exclamationmark.circle.fill")
                        .foregroundColor(LiquidGlassDesign.errorRed)
                    
                    Text(error)
                        .font(LiquidGlassDesign.bodySmall)
                        .foregroundColor(LiquidGlassDesign.errorRed)
                    
                    Spacer()
                }
                .padding(LiquidGlassDesign.spacingM)
                .background(
                    RoundedRectangle(cornerRadius: LiquidGlassDesign.radiusM)
                        .fill(LiquidGlassDesign.errorRed.opacity(0.1))
                )
            }
            
            // Login Button
            Button(action: {
                authManager.login(email: email, password: password)
            }) {
                Text("Se connecter")
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(LiquidGlassButtonStyle(isEnabled: !email.isEmpty && !password.isEmpty))
            .disabled(email.isEmpty || password.isEmpty || authManager.isLoading)
            
            // OAuth Buttons
            VStack(spacing: LiquidGlassDesign.spacingM) {
                Divider()
                    .opacity(0.3)
                
                Text("Ou connectez-vous avec")
                    .font(LiquidGlassDesign.bodySmall)
                    .foregroundColor(.secondary)
                
                HStack(spacing: LiquidGlassDesign.spacingM) {
                    // Apple Sign-In Button
                    Button(action: {
                        authManager.signInWithApple(window: nil)
                    }) {
                        HStack {
                            Image(systemName: "applelogo")
                                .font(.system(size: 18, weight: .semibold))
                            Text("Apple")
                                .font(LiquidGlassDesign.bodySmall)
                        }
                        .frame(maxWidth: .infinity)
                        .padding(LiquidGlassDesign.spacingM)
                        .foregroundColor(LiquidGlassDesign.accentBlue)
                        .background(
                            RoundedRectangle(cornerRadius: LiquidGlassDesign.radiusL)
                                .fill(LiquidGlassDesign.glassColor)
                        )
                        .overlay(
                            RoundedRectangle(cornerRadius: LiquidGlassDesign.radiusL)
                                .stroke(LiquidGlassDesign.accentBlue.opacity(0.3), lineWidth: 1)
                        )
                    }
                    
                    // Google Sign-In Button
                    Button(action: {
                        authManager.signInWithGoogle()
                    }) {
                        HStack {
                            Image(systemName: "g.circle")
                                .font(.system(size: 18, weight: .semibold))
                            Text("Google")
                                .font(LiquidGlassDesign.bodySmall)
                        }
                        .frame(maxWidth: .infinity)
                        .padding(LiquidGlassDesign.spacingM)
                        .foregroundColor(LiquidGlassDesign.accentBlue)
                        .background(
                            RoundedRectangle(cornerRadius: LiquidGlassDesign.radiusL)
                                .fill(LiquidGlassDesign.glassColor)
                        )
                        .overlay(
                            RoundedRectangle(cornerRadius: LiquidGlassDesign.radiusL)
                                .stroke(LiquidGlassDesign.accentBlue.opacity(0.3), lineWidth: 1)
                        )
                    }
                }
            }
            
            // Sign Up Link
            HStack(spacing: LiquidGlassDesign.spacingS) {
                Text("Pas encore inscrit?")
                    .font(LiquidGlassDesign.bodySmall)
                    .foregroundColor(.secondary)
                
                Button(action: {
                    isShowingSignUp = true
                    email = ""
                    password = ""
                }) {
                    Text("S'inscrire")
                        .font(LiquidGlassDesign.bodySmall)
                        .fontWeight(.semibold)
                        .foregroundColor(LiquidGlassDesign.accentBlue)
                }
            }
            .frame(maxWidth: .infinity, alignment: .center)
        }
    }
    
    // MARK: - Sign Up Form
    
    private var signUpForm: some View {
        VStack(spacing: LiquidGlassDesign.spacingL) {
            VStack(spacing: LiquidGlassDesign.spacingM) {
                VStack(alignment: .leading, spacing: LiquidGlassDesign.spacingS) {
                    Text("Email")
                        .font(LiquidGlassDesign.titleS)
                    TextField("votre@email.com", text: $email)
                        .textFieldStyle(LiquidGlassTextFieldStyle())
                        .keyboardType(.emailAddress)
                        .autocorrectionDisabled()
                }
                
                VStack(alignment: .leading, spacing: LiquidGlassDesign.spacingS) {
                    Text("Mot de passe")
                        .font(LiquidGlassDesign.titleS)
                    SecureField("••••••••", text: $password)
                        .textFieldStyle(LiquidGlassTextFieldStyle())
                }
            }
            .liquidGlassCard()
            
            // Error message
            if let error = authManager.errorMessage {
                HStack(spacing: LiquidGlassDesign.spacingM) {
                    Image(systemName: "exclamationmark.circle.fill")
                        .foregroundColor(LiquidGlassDesign.errorRed)
                    
                    Text(error)
                        .font(LiquidGlassDesign.bodySmall)
                        .foregroundColor(LiquidGlassDesign.errorRed)
                    
                    Spacer()
                }
                .padding(LiquidGlassDesign.spacingM)
                .background(
                    RoundedRectangle(cornerRadius: LiquidGlassDesign.radiusM)
                        .fill(LiquidGlassDesign.errorRed.opacity(0.1))
                )
            }
            
            // Sign Up Button
            Button(action: {
                authManager.signUp(email: email, name: "", password: password)
            }) {
                Text("S'inscrire")
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(LiquidGlassButtonStyle(isEnabled: !email.isEmpty && !password.isEmpty))
            .disabled(email.isEmpty || password.isEmpty || authManager.isLoading)
            
            // Back to Login Link
            Button(action: {
                isShowingSignUp = false
                email = ""
                password = ""
            }) {
                Text("Retour à la connexion")
                    .font(LiquidGlassDesign.bodySmall)
                    .foregroundColor(LiquidGlassDesign.accentBlue)
            }
            .frame(maxWidth: .infinity, alignment: .center)
        }
    }
}

#Preview {
    LoginView(authRepository: EventRepository())
}
