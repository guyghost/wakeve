import SwiftUI
import AuthenticationServices
import Shared

/// Gestionnaire centralisé pour l'authentification
@MainActor
class AuthenticationManager: NSObject, ObservableObject {
    @Published var currentUser: User?
    @Published var isAuthenticated = false
    @Published var isLoading = false
    @Published var errorMessage: String?
    
    private let authRepository: AuthRepository
    private let appleSignInDelegate: AppleSignInDelegate
    
    init(authRepository: AuthRepository) {
        self.authRepository = authRepository
        self.appleSignInDelegate = AppleSignInDelegate()
        super.init()
        
        // Restaurer la session existante
        Task {
            await restoreSession()
        }
    }
    
    // MARK: - Apple Sign-In
    
    /// Initier la connexion avec Apple
    func signInWithApple(window: UIWindow?) {
        let request = ASAuthorizationAppleIDProvider().createRequest()
        request.requestedScopes = [.fullName, .email]
        
        let controller = ASAuthorizationController(authorizationRequests: [request])
        controller.delegate = appleSignInDelegate
        controller.presentationContextProvider = appleSignInDelegate
        appleSignInDelegate.onCompletion = { [weak self] result in
            self?.handleAppleSignInResult(result)
        }
        controller.performRequests()
    }
    
    /// Gérer le résultat de connexion Apple
    private func handleAppleSignInResult(_ result: Result<ASAuthorizationAppleIDCredential, Error>) {
        Task {
            isLoading = true
            defer { isLoading = false }
            
            switch result {
            case .success(let credential):
                guard let idTokenData = credential.identityToken,
                      let idTokenString = String(data: idTokenData, encoding: .utf8) else {
                    errorMessage = "Impossible de récupérer le token Apple"
                    return
                }
                
                let loginResult = await authRepository.loginWithOAuth(
                    provider: "apple",
                    idToken: idTokenString,
                    accessToken: nil
                )
                
                handleAuthResult(loginResult)
                
            case .failure(let error):
                if !(error is ASAuthorizationError) ||
                   (error as? ASAuthorizationError)?.code != .canceled {
                    errorMessage = "Erreur de connexion Apple: \(error.localizedDescription)"
                }
            }
        }
    }
    
    // MARK: - Google Sign-In (via SDK)
    
    /// Initier la connexion avec Google
    func signInWithGoogle() {
        // À implémenter avec GoogleSignIn SDK
        // Pour le moment, affichage d'un message
        errorMessage = "La connexion Google sera bientôt disponible"
    }
    
    // MARK: - Email/Password Authentication
    
    /// Se connecter avec email et mot de passe
    func login(email: String, password: String) {
        Task {
            isLoading = true
            defer { isLoading = false }
            
            let result = await authRepository.login(email: email, password: password)
            handleAuthResult(result)
        }
    }
    
    /// S'inscrire avec email, nom et mot de passe
    func signUp(email: String, name: String, password: String) {
        Task {
            isLoading = true
            defer { isLoading = false }
            
            let result = await authRepository.signUp(email: email, name: name, password: password)
            handleAuthResult(result)
        }
    }
    
    /// Se déconnecter
    func logout() {
        Task {
            isLoading = true
            defer { isLoading = false }
            
            let result = await authRepository.logout()
            if result.isSuccess {
                currentUser = nil
                isAuthenticated = false
                errorMessage = nil
            } else {
                errorMessage = "Erreur de déconnexion"
            }
        }
    }
    
    // MARK: - Private Methods
    
    /// Restaurer la session existante
    private func restoreSession() async {
        let result = await authRepository.restoreSession()
        handleAuthResult(result)
    }
    
    /// Gérer le résultat de l'authentification
    private func handleAuthResult(_ result: Result<User, NSError>) {
        switch result {
        case .success(let user):
            currentUser = user
            isAuthenticated = true
            errorMessage = nil
            
        case .failure(let error):
            currentUser = nil
            isAuthenticated = false
            errorMessage = error.localizedDescription
        }
    }
}

// MARK: - Apple Sign-In Delegate

/// Délégué pour Apple Sign-In
class AppleSignInDelegate: NSObject, ASAuthorizationControllerDelegate, ASAuthorizationControllerPresentationContextProviding {
    var onCompletion: ((Result<ASAuthorizationAppleIDCredential, Error>) -> Void)?
    
    func authorizationController(
        controller: ASAuthorizationController,
        didCompleteWithAuthorization authorization: ASAuthorization
    ) {
        if let appleIDCredential = authorization.credential as? ASAuthorizationAppleIDCredential {
            onCompletion?(.success(appleIDCredential))
        }
    }
    
    func authorizationController(
        controller: ASAuthorizationController,
        didCompleteWithError error: Error
    ) {
        onCompletion?(.failure(error))
    }
    
    func presentationAnchor(for controller: ASAuthorizationController) -> ASPresentationAnchor {
        guard let window = UIApplication.shared.connectedScenes
            .compactMap({ $0 as? UIWindowScene })
            .first?.windows
            .first else {
            fatalError("Unable to find window")
        }
        return window
    }
}

// MARK: - Async Extension for AuthRepository

extension AuthRepository {
    /// Wrapper async pour login
    func login(email: String, password: String) async -> Result<User, NSError> {
        // Dans une véritable implémentation, cela utiliserait DispatchQueue
        // Pour le moment, simulation synchrone
        return Result(catching: {
            throw NSError(domain: "NotImplemented", code: -1)
        }).flatMap { _ in
            Result.failure(NSError(domain: "Auth", code: -1, userInfo: [
                NSLocalizedDescriptionKey: "À implémenter"
            ]))
        }
    }
    
    /// Wrapper async pour OAuth
    func loginWithOAuth(provider: String, idToken: String, accessToken: String?) async -> Result<User, NSError> {
        return Result.failure(NSError(domain: "Auth", code: -1, userInfo: [
            NSLocalizedDescriptionKey: "À implémenter"
        ]))
    }
    
    /// Wrapper async pour signup
    func signUp(email: String, name: String, password: String) async -> Result<User, NSError> {
        return Result.failure(NSError(domain: "Auth", code: -1, userInfo: [
            NSLocalizedDescriptionKey: "À implémenter"
        ]))
    }
    
    /// Wrapper async pour logout
    func logout() async -> Result<Void, NSError> {
        return Result.failure(NSError(domain: "Auth", code: -1, userInfo: [
            NSLocalizedDescriptionKey: "À implémenter"
        ]))
    }
    
    /// Wrapper async pour restoreSession
    func restoreSession() async -> Result<User, NSError> {
        return Result.failure(NSError(domain: "Auth", code: -1, userInfo: [
            NSLocalizedDescriptionKey: "À implémenter"
        ]))
    }
}
