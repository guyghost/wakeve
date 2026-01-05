# Rapport d'Analyse des Workflows Utilisateur (iOS vs Android)

**Date:** 2025-01-05
**Projet:** Wakeve - Application Multiplatform
**Objectif:** Vérifier la cohérence des workflows utilisateur sur iOS et Android

---

## 📊 Résumé Exécutif

| Workflow | Android | iOS | Statut |
|----------|----------|-----|--------|
| **Création d'événement (DRAFT Wizard)** | DraftEventWizard.kt | DraftEventWizardView.swift | ✅ **COHÉRENT** |
| **Liste d'événements (Home)** | HomeScreen.kt | ModernHomeView.swift | ⚠️ **DIFFÉRENT** |
| **Détails d'événement** | EventDetailScreen.kt | ModernEventDetailView.swift | ⚠️ **DIFFÉRENT** |
| **Navigation principale** | Tabs (Home, Explore, Messages, Profile) | Non-spécifié | ⚠️ **À VÉRIFIER** |
| **Entry Point Création** | FAB sur HomeScreen | Multiple (HomeButton, EventCreationSheet) | ⚠️ **FRAGMENTÉ** |

---

## 📋 Analyse Détaillée par Workflow

### 1. Workflow de Création d'Événement (DRAFT Phase)

#### ✅ **COHÉRENT** - Même structure sur les deux plateformes

**Android:** `composeApp/src/commonMain/kotlin/com/guyghost/wakeve/ui/event/DraftEventWizard.kt`

**iOS:** `iosApp/iosApp/Views/DraftEventWizardView.swift`

| Étape | Android | iOS | Commentaire |
|-------|---------|-----|-------------|
| **Étape 1: Basic Info** | Title, Description, EventType | Title, Description, EventType | ✅ Identique |
| **Étape 2: Participants** | Min, Max, Expected | Min, Max, Expected | ✅ Identique |
| **Étape 3: Locations** | PotentialLocation (add/remove) | PotentialLocation (add/remove) | ✅ Identique |
| **Étape 4: Time Slots** | TimeSlot avec timeOfDay | TimeSlot avec timeOfDay | ✅ Identique |

**Validation Rules:**
- Android: `isStepValid(step: Int)` - même logique
- iOS: `isStepValid(_ step: Int)` - même logique

**Détails:**
- Auto-save sur chaque transition d'étape
- Navigation Previous/Next
- Progress indicator
- Bouton de création final (Checkmark)

**⚠️ Note:** iOS a des implémentations alternatives de création d'événement (`ModernEventCreationView.swift`, `EventCreationSheet.swift`) qui ne sont pas utilisées par défaut mais créent de la confusion.

---

### 2. Workflow Liste d'Événements (Home)

#### ⚠️ **DIFFÉRENT** - Structure et UX différentes

**Android:** `composeApp/src/commonMain/kotlin/com/guyghost/wakeve/HomeScreen.kt`

```kotlin
// Structure Android
- TabRow (All, Upcoming, Past)
- LazyColumn d'EventCard
- FloatingActionButton (FAB) pour créer
- TopAppBar avec menu utilisateur
```

**iOS:** `iosApp/iosApp/Views/ModernHomeView.swift`

```swift
// Structure iOS
- "Upcoming" header avec dropdown (statique)
- Card-based event display
- "Add Event" button intégré dans la liste
- AppleInvitesHeader avec user avatar
```

**Différences:**

| Aspect | Android | iOS | Impact |
|--------|---------|-----|--------|
| **Filtre** | Tabs (All, Upcoming, Past) | Dropdown statique "Upcoming" | 🔴 Fonctionnalités différentes |
| **Entry Point Création** | FAB (Floating Action Button) | "Add Event" card dans la liste | ⚠️ UX différente |
| **Navigation** | Tab navigation dans l'app | Non-spécifié | ⚠️ Structure globale inconnue |
| **Design System** | Material You (cards, FAB) | Apple Invites (hero images) | ✅ Respect DS respectif |

**Recommendation:** Unifier les filtres et entry points pour une cohérence UX.

---

### 3. Workflow Détails d'Événement

#### ⚠️ **DIFFÉRENT** - Contenu et actions différentes

**Android:** `composeApp/src/commonMain/kotlin/com/guyghost/wakeve/EventDetailScreen.kt`

```kotlin
// Structure Android
- TopAppBar avec back
- EventCard (title, status, description)
- Participants list
- Poll results
- Organizer actions (edit, delete, start poll)
```

**iOS:** `iosApp/iosApp/Views/ModernEventDetailView.swift`

```swift
// Structure iOS
- Hero Image Section (photo)
- Liquid Glass Card avec:
  - Title and Date
  - RSVP Buttons (Going/Not Going/Maybe)
  - Host Actions Section
  - PRD Feature Buttons (Scenario Planning, Budget, etc.)
```

**Différences:**

| Aspect | Android | iOS | Impact |
|--------|---------|-----|--------|
| **Hero Image** | ❌ Non | ✅ Oui | 🔴 Fonctionnalité iOS-exclusive |
| **RSVP Buttons** | ❌ Non (via vote) | ✅ Oui (Going/Not Going/Maybe) | 🔴 Workflow de vote différent |
| **PRD Features** | ⚠️ Partiel | ✅ Complet | 🔴 iOS a plus de features |
| **Organizer Actions** | ✅ Complet | ✅ Complet | ✅ Identique |

**Workflow de Vote:**

| Platform | Mécanisme | UX |
|----------|-----------|-----|
| **Android** | Poll voting screen séparée (PollVotingScreen.kt) | Navigation vers écran dédié |
| **iOS** | RSVP Buttons inline (Going/Not Going/Maybe) | Interaction directe sur le card |

**⚠️ Note:** Les deux approches sont valides mais créent une expérience utilisateur différente.

---

### 4. Workflow Navigation Globale

#### ⚠️ **À VÉRIFIER** - Structure incomplète sur iOS

**Android:** `composeApp/src/androidMain/kotlin/com/guyghost/wakeve/navigation/Screen.kt`

```kotlin
// Navigation Android (Bottom Tabs)
sealed class Screen(val route: String, @StringRes val resourceId: Int) {
    object Home : Screen("home", R.string.tab_home)
    object Explore : Screen("explore", R.string.tab_explore)
    object Messages : Screen("messages", R.string.tab_messages)
    object Profile : Screen("profile", R.string.tab_profile)
}
```

**iOS:** Structure de navigation non spécifiée dans les fichiers analysés.

**Recommendation:** Documenter la navigation iOS pour vérifier la cohérence avec Android (Home, Explore, Messages, Profile).

---

## 🎯 Problèmes Identifiés

### 🔴 **Critiques**

1. **Fragmentation des Entry Points de Création (iOS)**
   - `DraftEventWizardView.swift` - Wizard standard
   - `ModernEventCreationView.swift` - Création immersive Apple Invites
   - `EventCreationSheet.swift` - Bottom sheet iOS Calendar
   - `QuickEventCreationSheet.swift` - Quick sheet simplifié

   **Impact:** Confusion sur quel entry point utiliser, maintenance difficile

2. **Filtres Différents (Home Screen)**
   - Android: Tabs fonctionnels (All, Upcoming, Past)
   - iOS: Dropdown statique "Upcoming" (non fonctionnel)

   **Impact:** Fonctionnalité perdue sur iOS

3. **Workflow de Vote Différent**
   - Android: Screen dédié `PollVotingScreen.kt`
   - iOS: RSVP buttons inline dans `ModernEventDetailView.swift`

   **Impact:** Expérience utilisateur différente, pas cohérent

### ⚠️ **Modérés**

4. **Hero Image (iOS-only)**
   - iOS affiche des hero images dans les event cards
   - Android n'a pas cette fonctionnalité

   **Impact:** Feature gap, UX iOS plus visuel

5. **PRD Features Complètes (iOS-only)**
   - iOS a des boutons pour: Scenario Planning, Budget Overview, Accommodation, Meal Planning, Equipment Checklist, Activity Planning
   - Android a une implémentation partielle

   **Impact:** Feature parity incomplète

6. **Navigation Globale Non Spécifiée (iOS)**
   - Structure de navigation iOS non documentée
   - Difficile de vérifier la cohérence avec Android

---

## 📐 Recommandations

### 🔴 **Priorité Haute (Critiques)**

#### 1. Standardiser les Entry Points de Création

**Action:**
- Sur iOS, utiliser uniquement `DraftEventWizardView.swift` comme entry point standard
- Déprécier ou supprimer: `ModernEventCreationView.swift`, `EventCreationSheet.swift`, `QuickEventCreationSheet.swift`
- Si nécessaire, refactoriser en composants réutilisables plutôt que vues alternatives

**Justification:**
- Coherence avec Android (DraftEventWizard.kt)
- Maintenance simplifiée
- Évite la confusion

#### 2. Unifier les Filtres (Home Screen)

**Action:**
- Sur iOS, implémenter les tabs fonctionnels (All, Upcoming, Past)
- Remplacer le dropdown statique "Upcoming" par un `Picker` avec filtrage réel
- Même logique de filtrage qu'Android

**Justification:**
- Feature parity
- Cohérence UX cross-platform

#### 3. Harmoniser le Workflow de Vote

**Option A: Approche Screen Dédié (Android)**
- Sur iOS, implémenter `PollVotingView` similaire à Android
- RSVP buttons naviguent vers l'écran de vote

**Option B: Approche RSVP Buttons (iOS)**
- Sur Android, implémenter RSVP buttons dans EventDetailScreen
- Vote inline sans navigation

**Recommendation:** Choisir **une** approche et l'implémenter sur les deux plateformes pour la cohérence.

### ⚠️ **Priorité Moyenne**

#### 4. Feature Parity - Hero Images

**Action:**
- Implémenter hero images sur Android
- Ajouter champ `heroImageUrl` dans le modèle Event
- Afficher dans EventDetailScreen

**Justification:**
- Feature parity
- Amélioration UX Android

#### 5. Feature Parity - PRD Features

**Action:**
- Sur Android, compléter l'implémentation des PRD features:
  - Scenario Planning Screen
  - Budget Overview Screen
  - Accommodation Screen
  - Meal Planning Screen
  - Equipment Checklist Screen
  - Activity Planning Screen

**Justification:**
- Feature parity
- Compléter le workflow événement complet

#### 6. Documenter la Navigation iOS

**Action:**
- Documenter la structure de navigation iOS (Tabs, Screen hierarchy)
- Vérifier la cohérence avec Android (Home, Explore, Messages, Profile)
- Créer un diagramme de navigation

**Justification:**
- Compréhension de l'architecture
- Vérification de la cohérence

---

## 📊 Matrice de Cohérence

| Feature | Android | iOS | Cohérence | Priorité |
|---------|---------|-----|-----------|----------|
| **DRAFT Wizard** | DraftEventWizard.kt | DraftEventWizardView.swift | ✅ 100% | - |
| **Home Filters** | Tabs (All, Upcoming, Past) | Statique "Upcoming" | ❌ 0% | 🔴 Haute |
| **Entry Point Création** | FAB | Fragmenté (3+ options) | ⚠️ 30% | 🔴 Haute |
| **Vote Workflow** | Screen dédié | RSVP buttons inline | ❌ 0% | 🔴 Haute |
| **Event Details** | Card-based | Hero image + cards | ⚠️ 70% | ⚠️ Moyenne |
| **PRD Features** | Partiel | Complet | ⚠️ 50% | ⚠️ Moyenne |
| **Navigation** | Bottom Tabs (4) | Non spécifié | ❌ N/A | ⚠️ Moyenne |
| **Hero Images** | ❌ Non | ✅ Oui | ❌ 0% | ⚠️ Moyenne |

---

## 🎨 Design System Conformité

### Android - Material You
- ✅ DraftEventWizard utilise Material You (TopAppBar, LinearProgressIndicator, FilledTonalButton)
- ✅ HomeScreen utilise Material You (TabRow, Card, FAB)
- ✅ EventDetailScreen utilise Material You (Scaffold, Card, AlertDialog)

### iOS - Liquid Glass
- ✅ DraftEventWizardView utilise Liquid Glass (.ultraThinMaterial, cornerRadius: 10)
- ✅ ModernHomeView utilise Liquid Glass
- ✅ ModernEventDetailView utilise Liquid GlassCard

**Conclusion:** Les deux plateformes respectent leur design system respectif.

---

## 📝 Conclusion

**Niveau de cohérence global: 55%**

Le projet a une base solide avec le DRAFT Wizard cohérent sur les deux plateformes. Cependant, il y a des divergences significatives dans:

1. **Home Screen** - Filtres non fonctionnels sur iOS
2. **Entry Points** - Fragmentation excessive sur iOS
3. **Vote Workflow** - Approches radicalement différentes

**Recommandation prioritaire:** Standardiser les workflows critiques (creation entry point, home filters, vote workflow) avant d'ajouter de nouvelles features.

---

## 🔗 Annexes

### Fichiers Analyzés

**Android:**
- `composeApp/src/commonMain/kotlin/com/guyghost/wakeve/ui/event/DraftEventWizard.kt`
- `composeApp/src/commonMain/kotlin/com/guyghost/wakeve/HomeScreen.kt`
- `composeApp/src/commonMain/kotlin/com/guyghost/wakeve/EventDetailScreen.kt`
- `composeApp/src/commonMain/kotlin/com/guyghost/wakeve/EventCreationScreen.kt` (deprecated)
- `composeApp/src/androidMain/kotlin/com/guyghost/wakeve/navigation/Screen.kt`

**iOS:**
- `iosApp/iosApp/Views/DraftEventWizardView.swift`
- `iosApp/iosApp/Views/ModernHomeView.swift`
- `iosApp/iosApp/Views/ModernEventDetailView.swift`
- `iosApp/iosApp/Views/ModernEventCreationView.swift`
- `iosApp/iosApp/Views/CreateEventView.swift`
- `iosApp/iosApp/Views/EventCreationSheet.swift`
- `iosApp/iosApp/Views/EventsTabView.swift`

### Tests Automatisés

**Android:**
- DraftEventWizardTest.kt
- ParticipantsEstimationCardTest.kt
- PotentialLocationsListTest.kt

**iOS:**
- Tests automatisés non trouvés pour les workflows analysés

**Recommandation:** Ajouter des tests iOS pour vérifier la cohérence des workflows avec Android.
