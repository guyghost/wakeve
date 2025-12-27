# Spécification : User Onboarding

**Version** : 1.0.0
**Domain** : User Experience
**Dernière mise à jour** : 27 décembre 2025

---

## ADDED Requirements

### Requirement: First-Time Onboarding Display

Le système MUST afficher un écran d'onboarding de 4 étapes aux utilisateurs lors de leur première connexion authentifiée.

#### Scenario: First Launch After Registration (Android)

- **GIVEN** A user has registered on Wakeve Android
- **WHEN** They successfully authenticate
- **THEN** The onboarding screen SHALL be displayed (4 steps)
- **AND** The page title "Découverte" SHALL be shown

#### Scenario: First Launch After Registration (iOS)

- **GIVEN** A user has registered on Wakeve iOS
- **WHEN** They successfully authenticate
- **THEN** The onboarding screen SHALL be displayed (4 steps)
- **AND** The page title "Découverte" SHALL be shown

#### Scenario: Return Visit After Completion (Android)

- **GIVEN** A user has already completed onboarding
- **WHEN** They relaunch application
- **THEN** The onboarding screen SHALL NOT be displayed
- **AND** The user SHALL access directly to home screen

#### Scenario: Return Visit After Completion (iOS)

- **GIVEN** A user has already completed onboarding
- **WHEN** They relaunch application
- **THEN** The onboarding screen SHALL NOT be displayed
- **AND** The user SHALL access directly to home screen

### Requirement: Onboarding Persistence

Le système MUST persister l'état de complétion de l'onboarding pour éviter de le réafficher.

#### Scenario: Save Onboarding Completion (Android)

- **GIVEN** A user has just completed onboarding on Android
- **WHEN** The onboarding completion callback is triggered
- **THEN** The state SHALL be saved in SharedPreferences
- **AND** The key `HAS_COMPLETED_ONBOARDING` SHALL be set to true

#### Scenario: Save Onboarding Completion (iOS)

- **GIVEN** A user has just completed onboarding on iOS
- **WHEN** The onboarding completion callback is triggered
- **THEN** The state SHALL be saved in UserDefaults
- **AND** The key `hasCompletedOnboarding` SHALL be set to true

#### Scenario: Check Onboarding State (Android)

- **GIVEN** A user launches application on Android
- **WHEN** The onboarding state is checked
- **THEN** SharedPreferences SHALL be queried for `HAS_COMPLETED_ONBOARDING`
- **AND** The default value SHALL be false (not completed)

#### Scenario: Check Onboarding State (iOS)

- **GIVEN** A user launches application on iOS
- **WHEN** The onboarding state is checked
- **THEN** UserDefaults SHALL be queried for `hasCompletedOnboarding`
- **AND** The default value SHALL be false (not completed)

### Requirement: Cross-Platform Onboarding Content

L'onboarding MUST contenir 4 étapes identiques en contenu sur Android et iOS.

**Step 1: Créez vos événements**
- Icon : Calendar/Event icon
- Title : "Créez vos événements"
- Description : "Organisez facilement des événements entre amis et collègues. Définissez des dates, proposez des créneaux horaires et laissez les participants voter."
- Features :
  * Création rapide d'événements
  * Sondage de disponibilité
  * Calcul automatique du meilleur créneau

**Step 2: Collaborez en équipe**
- Icon : People/Group icon
- Title : "Collaborez en équipe"
- Description : "Travaillez ensemble sur l'organisation de l'événement. Partagez les responsabilités et suivez la progression en temps réel."
- Features :
  * Gestion des participants
  * Attribution des tâches
  * Suivi en temps réel

**Step 3: Organisez tout en un**
- Icon : Target/Checklist icon
- Title : "Organisez tout en un"
- Description : "Gérez l'hébergement, les repas, les activités et le budget. Tout au même endroit pour une organisation sans faille."
- Features :
  * Planification d'hébergement
  * Organisation des repas
  * Suivi du budget

**Step 4: Profitez de vos événements**
- Icon : Celebration/Party icon
- Title : "Profitez de vos événements"
- Description : "Une fois l'organisation terminée, profitez de l'événement avec vos proches sans stress."
- Features :
  * Vue d'ensemble
  * Rappels intégrés
  * Calendrier natif

#### Scenario: Display Onboarding Step 1 (Android)

- **GIVEN** The onboarding screen is displayed on Android
- **WHEN** The user is on step 1
- **THEN** The title "Créez vos événements" SHALL be displayed
- **AND** The description SHALL be displayed
- **AND** The 3 features SHALL be listed
- **AND** The calendar icon SHALL be displayed

#### Scenario: Display Onboarding Step 1 (iOS)

- **GIVEN** The onboarding screen is displayed on iOS
- **WHEN** The user is on step 1
- **THEN** The title "Créez vos événements" SHALL be displayed
- **AND** The description SHALL be displayed
- **AND** The 3 features SHALL be listed
- **AND** The calendar icon SHALL be displayed

#### Scenario: Display Onboarding Step 4 (Android)

- **GIVEN** The onboarding screen is displayed on Android
- **WHEN** The user navigates to step 4
- **THEN** The title "Profitez de vos événements" SHALL be displayed
- **AND** The description SHALL be displayed
- **AND** The 3 features SHALL be listed
- **AND** The celebration icon SHALL be displayed
- **AND** The button SHALL show "Commencer"

#### Scenario: Display Onboarding Step 4 (iOS)

- **GIVEN** The onboarding screen is displayed on iOS
- **WHEN** The user navigates to step 4
- **THEN** The title "Profitez de vos événements" SHALL be displayed
- **AND** The description SHALL be displayed
- **AND** The 3 features SHALL be listed
- **AND** The celebration icon SHALL be displayed
- **AND** The button SHALL show "Commencer"

### Requirement: Android Onboarding Implementation

Android MUST utiliser l'écran `OnboardingScreen.kt` existant avec Jetpack Compose et Material You.

**Navigation Flow** :
```
Splash (2s)
  ↓
Check onboarding state
  ↓
!onboardingCompleted ? → OnboardingScreen → HomeScreen
onboardingCompleted ? → HomeScreen
```

**Technical Details** :
- Use `HorizontalPager` for navigation between steps
- Apply Material Theme 3 colors and typography
- Smooth animation with `animateFloatAsState`
- Page indicators at bottom of screen
- "Suivant" / "Commencer" button at bottom
- Back button to navigate to previous step

#### Scenario: Navigate Onboarding Steps Forward (Android)

- **GIVEN** The onboarding screen is displayed on Android
- **WHEN** The user swipes right or clicks "Suivant" button
- **THEN** The HorizontalPager SHALL animate to next page
- **AND** The page indicators SHALL update
- **AND** The content SHALL change smoothly

#### Scenario: Navigate Onboarding Steps Backward (Android)

- **GIVEN** The onboarding screen is displayed on Android and user is on step 2+
- **WHEN** The user clicks the back button
- **THEN** The HorizontalPager SHALL animate to previous page
- **AND** The page indicators SHALL update

#### Scenario: Complete Onboarding (Android)

- **GIVEN** The onboarding screen is displayed on Android
- **WHEN** The user reaches last step and clicks "Commencer" button
- **THEN** The onboarding state SHALL be marked as completed
- **AND** The user SHALL be navigated to HomeScreen

### Requirement: iOS Onboarding Implementation

iOS MUST avoir un nouvel écran `OnboardingView.swift` avec SwiftUI et Liquid Glass.

**Navigation Flow** :
```
Splash (iOS launch screen)
  ↓
Check onboarding state
  ↓
!onboardingCompleted ? → OnboardingView → ContentView (Home)
onboardingCompleted ? → ContentView (Home)
```

**Technical Details** :
- Use `TabView` with `PageTabViewStyle(.indexDisplayMode(automatic))`
- Apply Liquid Glass design (materials, continuous corners)
- Use WakevColors.swift color palette
- Use SF Symbols or emojis for icons
- Native SwiftUI animation

#### Scenario: Navigate Onboarding Steps Forward (iOS)

- **GIVEN** The onboarding screen is displayed on iOS
- **WHEN** The user swipes left
- **THEN** The TabView SHALL animate to next page
- **AND** The page indicators SHALL update
- **AND** The content SHALL change smoothly

#### Scenario: Navigate Onboarding Steps Backward (iOS)

- **GIVEN** The onboarding screen is displayed on iOS and user is on step 2+
- **WHEN** The user swipes right
- **THEN** The TabView SHALL animate to previous page
- **AND** The page indicators SHALL update

#### Scenario: Complete Onboarding (iOS)

- **GIVEN** The onboarding screen is displayed on iOS
- **WHEN** The user reaches last step and clicks "Commencer" button
- **THEN** The onboarding state SHALL be marked as completed
- **AND** The user SHALL be navigated to ContentView (Home)

### Requirement: Onboarding Skip Functionality

L'utilisateur MUST pouvoir skip l'onboarding à tout moment.

**Implementation Details** :
- "Passer" button available on each page
- Skip = mark onboarding as completed + navigate to Home
- Skip is saved in persistence

#### Scenario: Skip Onboarding Step 1 (Android)

- **GIVEN** The user is on onboarding step 1 on Android
- **WHEN** The user clicks "Passer" button
- **THEN** The onboarding state SHALL be marked as completed
- **AND** The user SHALL be navigated to HomeScreen immediately
- **AND** No additional onboarding steps SHALL be displayed

#### Scenario: Skip Onboarding Step 2 (iOS)

- **GIVEN** The user is on onboarding step 2 on iOS
- **WHEN** The user clicks "Passer" button
- **THEN** The onboarding state SHALL be marked as completed
- **AND** The user SHALL be navigated to ContentView (Home) immediately
- **AND** No additional onboarding steps SHALL be displayed

#### Scenario: Skip Onboarding On Any Step (Android)

- **GIVEN** The user is on any onboarding step on Android (1-4)
- **WHEN** The user clicks "Passer" button
- **THEN** The onboarding state SHALL be marked as completed
- **AND** The user SHALL be navigated to HomeScreen immediately

#### Scenario: Skip Onboarding On Any Step (iOS)

- **GIVEN** The user is on any onboarding step on iOS (1-4)
- **WHEN** The user clicks "Passer" button
- **THEN** The onboarding state SHALL be marked as completed
- **AND** The user SHALL be navigated to ContentView (Home) immediately

## MODIFIED Requirements

Aucun requirement modifié.

## REMOVED Requirements

Aucun requirement supprimé.

---

## Technical Specifications

### Android Implementation

#### Files
- `composeApp/src/commonMain/kotlin/com/guyghost/wakeve/OnboardingScreen.kt` (exists, reuse)
- `composeApp/src/androidMain/kotlin/com/guyghost/wakeve/App.kt` (modify)

#### Key Components

```kotlin
enum class OnboardingStep(
    val title: String,
    val description: String,
    val icon: String,
    val features: List<String>
)

@Composable
fun OnboardingScreen(
    onOnboardingComplete: () -> Unit
)
```

#### Persistence
```kotlin
private const val PREFS_NAME = "wakeve_prefs"
private const val HAS_COMPLETED_ONBOARDING = "has_completed_onboarding"

fun hasCompletedOnboarding(context: Context): Boolean
fun markOnboardingComplete(context: Context)
```

### iOS Implementation

#### New Files
- `iosApp/iosApp/Views/OnboardingView.swift`
- `iosApp/iosApp/Models/OnboardingData.swift`

#### Modified Files
- `iosApp/iosApp/ContentView.swift` (add onboarding check)
- `iosApp/iosApp/iOSApp.swift` (add onboarding check in app launch)

#### Key Components

```swift
struct OnboardingView: View {
    @State private var currentPage = 0
    let onOnboardingComplete: () -> Void
}

struct OnboardingStep {
    let title: String
    let description: String
    let icon: String
    let features: [String]
}
```

#### Persistence
```swift
struct UserDefaultsKeys {
    static let hasCompletedOnboarding = "hasCompletedOnboarding"
}

func hasCompletedOnboarding() -> Bool
func markOnboardingComplete()
```

### Design System Compliance

#### Android (Material You + Jetpack Compose)
- Colors: MaterialTheme.colorScheme (primary, onPrimary, surface, onSurface)
- Typography: MaterialTheme.typography (headlineLarge, bodyLarge, labelMedium)
- Shapes: CircleShape for icons (120dp), RoundedCornerShape for buttons
- Animation: animateFloatAsState with 250ms duration

#### iOS (Liquid Glass + SwiftUI)
- Colors: WakevColors (primary, primaryLight, primaryDark, etc.)
- Materials: .ultraThinMaterial or .thinMaterial for background
- Shapes: .continuous corner radius (60dp for icons, 12-16dp for elements)
- Animation: Native SwiftUI TabView animations

---

## Testing Requirements

### Unit Tests
- [ ] Test `hasCompletedOnboarding()` returns false for first launch (Android)
- [ ] Test `hasCompletedOnboarding()` returns true after completion (Android)
- [ ] Test `markOnboardingComplete()` saves state (Android)
- [ ] Test `hasCompletedOnboarding()` returns false for first launch (iOS)
- [ ] Test `hasCompletedOnboarding()` returns true after completion (iOS)
- [ ] Test `markOnboardingComplete()` saves state (iOS)

### Integration Tests
- [ ] Test complete flow Android: Splash → Onboarding → Home
- [ ] Test complete flow iOS: Splash → Onboarding → Home
- [ ] Test skip functionality on Android
- [ ] Test skip functionality on iOS
- [ ] Test no onboarding display after completion (Android)
- [ ] Test no onboarding display after completion (iOS)

### UI Tests
- [ ] Test navigation between steps (swipe + buttons) on Android
- [ ] Test navigation between steps (swipe) on iOS
- [ ] Test page indicators update correctly
- [ ] Test button text changes ("Suivant" → "Commencer")
- [ ] Test accessibility labels (screen reader)

---

## Dependencies

### Android
- Jetpack Compose
- Material3
- Foundation (HorizontalPager)
- AndroidX DataStore (optional, for persistence)

### iOS
- SwiftUI
- Liquid Glass materials (iOS 16+)
- SF Symbols
- UserDefaults

---

## Success Criteria

✅ Onboarding displays on first authenticated launch (Android)
✅ Onboarding displays on first authenticated launch (iOS)
✅ Onboarding does NOT display on subsequent launches
✅ 4 steps are consistent between Android and iOS
✅ Design respects Material You (Android) and Liquid Glass (iOS)
✅ Onboarding state persists between app sessions
✅ Skip functionality works on all steps
✅ All tests pass

---

## Related Documentation

- Design System: `.opencode/design-system.md`
- Liquid Glass Guidelines: `iosApp/LIQUID_GLASS_GUIDELINES.md`
- Architecture: `docs/ARCHITECTURE.md`
- AGENTS.md: Workflow and agent responsibilities
