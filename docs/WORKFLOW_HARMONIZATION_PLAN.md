# Plan d'Action: Harmonisation des Workflows iOS/Android

**Basé sur:** WORKFLOW_ANALYSIS_REPORT.md
**Date:** 2025-01-05

---

## 🎯 Objectif

Assurer 100% de cohérence des workflows utilisateur entre iOS et Android pour les fonctionnalités critiques.

---

## 🔴 Phase 1: Correction Critique (Immédiat)

### 1.1 Standardiser les Entry Points de Création (iOS)

**Problème:** iOS a 4 entry points différents pour créer un événement, créant de la confusion.

**Action:**
```swift
// 1. Déprécier les vues alternatives dans iosApp/src/Views/
@available(*, deprecated, message: "Use DraftEventWizardView instead")
struct ModernEventCreationView: View { ... }

@available(*, deprecated, message: "Use DraftEventWizardView instead")
struct EventCreationSheet: View { ... }

@available(*, deprecated, message: "Use DraftEventWizardView instead")
struct QuickEventCreationSheet: View { ... }

// 2. Utiliser uniquement DraftEventWizardView comme entry point
// dans ModernHomeView, EventsTabView, etc.
```

**Fichiers à modifier:**
- `iosApp/src/Views/ModernHomeView.swift`
- `iosApp/src/Views/EventsTabView.swift`
- `iosApp/src/Views/CreateEventView.swift`

**Validation:**
- Vérifier que tous les entry points utilisent `DraftEventWizardView`
- Tester le workflow de création complet

---

### 1.2 Unifier les Filtres (Home Screen)

**Problème:** Android a des tabs fonctionnels (All, Upcoming, Past), iOS a un dropdown statique.

**Action iOS:**
```swift
// Remplacer le dropdown statique par un Picker fonctionnel dans ModernHomeView.swift

enum EventFilter: String, CaseIterable {
    case all = "Tous"
    case upcoming = "À venir"
    case past = "Passés"
}

@State private var selectedFilter: EventFilter = .upcoming

var filteredEvents: [Event] {
    switch selectedFilter {
    case .all:
        return events
    case .upcoming:
        return events.filter { isUpcoming($0) }
    case .past:
        return events.filter { isPast($0) }
    }
}

// Dans le body, remplacer le dropdown statique par:
Picker("Filtre", selection: $selectedFilter) {
    ForEach(EventFilter.allCases, id: \.self) { filter in
        Text(filter.rawValue).tag(filter)
    }
}
.pickerStyle(.segmented)
.padding(.horizontal, 16)
```

**Fichiers à modifier:**
- `iosApp/src/Views/ModernHomeView.swift`

**Validation:**
- Tester chaque filtre (All, Upcoming, Past)
- Vérifier que les résultats correspondent à Android

---

### 1.3 Harmoniser le Workflow de Vote

**Problème:** Android utilise un écran dédié, iOS utilise des RSVP buttons inline.

**Décision:** Adopter l'approche **screen dédié** pour la cohérence avec l'architecture MVI/State Machine.

**Action iOS:**
```swift
// Créer PollVotingView.swift
struct PollVotingView: View {
    let event: Event
    let userId: String
    let repository: EventRepositoryInterface
    let onVoteSubmitted: () -> Void

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                // Afficher chaque time slot avec boutons de vote
                ForEach(event.proposedSlots, id: \.id) { slot in
                    TimeSlotVoteCard(
                        slot: slot,
                        userVote: userVote(for: slot),
                        onVote: { response in
                            voteForSlot(slot.id, response: response)
                        }
                    )
                }

                // Bouton Submit
                Button("Soumettre") {
                    submitVotes()
                    onVoteSubmitted()
                }
                .disabled(!hasVoted)
            }
            .padding()
        }
        .navigationTitle("Votez pour le créneau")
    }
}

// Dans ModernEventDetailView.swift, remplacer RSVPButtonsSection par:
NavigationLink(destination: PollVotingView(...)) {
    Button("Participer au vote") {
        // Navigate to voting screen
    }
}
```

**Action Android:**
- Vérifier que PollVotingScreen.kt est fonctionnel
- S'assurer que la navigation depuis EventDetailScreen fonctionne

**Fichiers à créer:**
- `iosApp/src/Views/PollVotingView.swift`

**Fichiers à modifier:**
- `iosApp/src/Views/ModernEventDetailView.swift`
- `composeApp/src/commonMain/kotlin/com/guyghost/wakeve/EventDetailScreen.kt` (vérification)

**Validation:**
- Tester le workflow de vote complet sur iOS et Android
- Vérifier que les résultats sont synchronisés

---

## ⚠️ Phase 2: Feature Parity (Moyen terme)

### 2.1 Ajouter Hero Images sur Android

**Problème:** iOS affiche des hero images, Android non.

**Action Android:**
```kotlin
// 1. Ajouter un champ heroImageUrl dans le modèle Event
// shared/src/commonMain/kotlin/com/guyghost/wakeve/models/Event.kt
data class Event(
    // ... champs existants
    val heroImageUrl: String? = null, // Nouveau champ
    // ...
)

// 2. Ajouter un composant HeroImageSection
// composeApp/src/commonMain/kotlin/com/guyghost/wakeve/ui/components/HeroImageSection.kt
@Composable
fun HeroImageSection(
    imageUrl: String?,
    modifier: Modifier = Modifier
) {
    imageUrl?.let { url ->
        AsyncImage(
            model = url,
            contentDescription = null,
            modifier = modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(MaterialTheme.shapes.medium)
        )
    }
}

// 3. Intégrer dans EventDetailScreen
HeroImageSection(
    imageUrl = event.heroImageUrl,
    modifier = Modifier.fillMaxWidth()
)
```

**Fichiers à créer:**
- `composeApp/src/commonMain/kotlin/com/guyghost/wakeve/ui/components/HeroImageSection.kt`

**Fichiers à modifier:**
- `shared/src/commonMain/kotlin/com/guyghost/wakeve/models/Event.kt`
- `composeApp/src/commonMain/kotlin/com/guyghost/wakeve/EventDetailScreen.kt`

**Validation:**
- Tester l'affichage des hero images
- Vérifier le fallback quand imageUrl est null

---

### 2.2 Compléter les PRD Features sur Android

**Problème:** iOS a toutes les PRD features, Android est partiel.

**Action:**
Pour chaque feature manquante sur Android, créer l'écran correspondant:
- `ScenarioPlanningScreen.kt` (existe déjà)
- `BudgetOverviewScreen.kt` (existe déjà)
- `AccommodationScreen.kt` (existe déjà)
- `MealPlanningScreen.kt` (existe déjà)
- `EquipmentChecklistScreen.kt` (existe déjà)
- `ActivityPlanningScreen.kt` (existe déjà)

**Vérification:**
- Les écrans existent mais ne sont peut-être pas intégrés dans le workflow
- Ajouter des boutons dans EventDetailScreen pour naviguer vers ces écrans

**Action Android:**
```kotlin
// Dans EventDetailScreen.kt, ajouter:
if (event.status == EventStatus.CONFIRMED || event.status == EventStatus.ORGANIZING) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Planification")
            .style(MaterialTheme.typography.titleMedium)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Scenario Planning
            SmallFeatureButton(
                icon = Icons.Default.Map,
                label = "Scénarios",
                onClick = { onNavigateTo("scenario/${event.id}") }
            )

            // Budget
            SmallFeatureButton(
                icon = Icons.Default.AttachMoney,
                label = "Budget",
                onClick = { onNavigateTo("budget/${event.id}") }
            )

            // Accommodation
            SmallFeatureButton(
                icon = Icons.Default.Hotel,
                label = "Hébergement",
                onClick = { onNavigateTo("accommodation/${event.id}") }
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Meal Planning
            SmallFeatureButton(
                icon = Icons.Default.Restaurant,
                label = "Repas",
                onClick = { onNavigateTo("meal/${event.id}") }
            )

            // Equipment
            SmallFeatureButton(
                icon = Icons.Default.Checklist,
                label = "Équipement",
                onClick = { onNavigateTo("equipment/${event.id}") }
            )

            // Activities
            SmallFeatureButton(
                icon = Icons.Default.DirectionsRun,
                label = "Activités",
                onClick = { onNavigateTo("activity/${event.id}") }
            )
        }
    }
}
```

**Validation:**
- Tester chaque bouton de navigation
- Vérifier que les écrans sont accessibles

---

### 2.3 Documenter la Navigation iOS

**Problème:** Structure de navigation iOS non spécifiée.

**Action:**
```swift
// Créer iosApp/src/Navigation/AppNavigation.swift

enum AppTab: String, CaseIterable {
    case home = "home"
    case explore = "explore"
    case messages = "messages"
    case profile = "profile"

    var title: String {
        switch self {
        case .home: return "Accueil"
        case .explore: return "Explorer"
        case .messages: return "Messages"
        case .profile: return "Profil"
        }
    }

    var iconName: String {
        switch self {
        case .home: return "house.fill"
        case .explore: return "safari.fill"
        case .messages: return "message.fill"
        case .profile: return "person.fill"
        }
    }
}

// Créer le TabView principal
struct MainTabView: View {
    @State private var selectedTab: AppTab = .home

    var body: some View {
        TabView(selection: $selectedTab) {
            ModernHomeView(...)
                .tabItem {
                    Label(AppTab.home.title, systemImage: AppTab.home.iconName)
                }
                .tag(AppTab.home)

            ExploreView(...)
                .tabItem {
                    Label(AppTab.explore.title, systemImage: AppTab.explore.iconName)
                }
                .tag(AppTab.explore)

            MessagesView(...)
                .tabItem {
                    Label(AppTab.messages.title, systemImage: AppTab.messages.iconName)
                }
                .tag(AppTab.messages)

            ProfileView(...)
                .tabItem {
                    Label(AppTab.profile.title, systemImage: AppTab.profile.iconName)
                }
                .tag(AppTab.profile)
        }
    }
}
```

**Fichiers à créer:**
- `iosApp/src/Navigation/AppNavigation.swift`
- `iosApp/src/Views/ExploreView.swift`
- `iosApp/src/Views/MessagesView.swift`

**Validation:**
- Tester la navigation entre tabs
- Vérifier la cohérence avec Android (Home, Explore, Messages, Profile)

---

## 📊 Phase 3: Tests & Documentation (Long terme)

### 3.1 Ajouter des Tests iOS

**Action:**
```swift
// Créer iosApp/WakeveTests/WorkflowTests.swift
class WorkflowTests: XCTestCase {
    func testDraftEventWizardSteps() {
        // Tester chaque étape du wizard
        // Vérifier la validation
    }

    func testHomeFilters() {
        // Tester les filtres (All, Upcoming, Past)
        // Vérifier que les résultats sont corrects
    }

    func testPollVotingWorkflow() {
        // Tester le workflow de vote
        // Vérifier la soumission des votes
    }
}
```

**Fichiers à créer:**
- `iosApp/WakeveTests/WorkflowTests.swift`

---

### 3.2 Mettre à jour AGENTS.md

**Action:**
- Documenter les workflows standardisés
- Mettre à jour les specs pour refléter les changements
- Ajouter des guidelines pour la cohérence cross-platform

---

## 📝 Checklist d'Implémentation

### Phase 1: Critique (Immédiat)

- [ ] **1.1** Déprécier les vues alternatives de création iOS
  - [ ] Marquer ModernEventCreationView comme deprecated
  - [ ] Marquer EventCreationSheet comme deprecated
  - [ ] Marquer QuickEventCreationSheet comme deprecated
  - [ ] Vérifier que tous les entry points utilisent DraftEventWizardView
  - [ ] Tester le workflow de création

- [ ] **1.2** Implémenter les filtres fonctionnels iOS
  - [ ] Créer l'enum EventFilter
  - [ ] Ajouter la logique de filtrage
  - [ ] Remplacer le dropdown statique par Picker segmenté
  - [ ] Tester chaque filtre (All, Upcoming, Past)

- [ ] **1.3** Harmoniser le workflow de vote
  - [ ] Créer PollVotingView sur iOS
  - [ ] Modifier ModernEventDetailView pour naviguer vers PollVotingView
  - [ ] Vérifier PollVotingScreen.kt sur Android
  - [ ] Tester le workflow de vote sur les deux plateformes

### Phase 2: Feature Parity (Moyen terme)

- [ ] **2.1** Ajouter Hero Images sur Android
  - [ ] Ajouter le champ heroImageUrl dans Event.kt
  - [ ] Créer HeroImageSection.kt
  - [ ] Intégrer dans EventDetailScreen
  - [ ] Tester l'affichage des images

- [ ] **2.2** Compléter les PRD Features sur Android
  - [ ] Ajouter les boutons de navigation dans EventDetailScreen
  - [ ] Vérifier l'existence de tous les écrans PRD
  - [ ] Tester la navigation vers chaque feature

- [ ] **2.3** Documenter la navigation iOS
  - [ ] Créer AppNavigation.swift
  - [ ] Créer ExploreView.swift
  - [ ] Créer MessagesView.swift
  - [ ] Créer MainTabView
  - [ ] Tester la navigation entre tabs

### Phase 3: Tests & Documentation

- [ ] **3.1** Ajouter des tests iOS
  - [ ] Créer WorkflowTests.swift
  - [ ] Écrire des tests pour DraftEventWizard
  - [ ] Écrire des tests pour HomeFilters
  - [ ] Écrire des tests pour PollVotingWorkflow

- [ ] **3.2** Mettre à jour la documentation
  - [ ] Mettre à jour AGENTS.md avec les workflows standardisés
  - [ ] Mettre à jour les specs OpenSpec si nécessaire
  - [ ] Créer un guide de cohérence cross-platform

---

## 🎯 Métriques de Succès

Après implémentation, les métriques suivantes devraient être atteintes:

| Métrique | Actuel | Cible |
|----------|--------|-------|
| **Cohérence Workflows Critiques** | 55% | 100% |
| **Cohérence Entry Points** | 30% | 100% |
| **Feature Parity** | 70% | 95% |
| **Couverture Tests iOS** | 0% | 60% |

---

## 📅 Timeline

- **Phase 1 (Critique):** 2-3 jours
- **Phase 2 (Feature Parity):** 1 semaine
- **Phase 3 (Tests & Documentation):** 3-4 jours

**Total:** ~2 semaines

---

## 🔗 Ressources

- [WORKFLOW_ANALYSIS_REPORT.md](WORKFLOW_ANALYSIS_REPORT.md) - Rapport d'analyse complet
- [AGENTS.md](../AGENTS.md) - Guide des agents IA
- [openspec/specs/](./openspec/specs/) - Spécifications OpenSpec

---

## 💡 Notes

- **Principe:** "Une seule façon de faire" - Standardiser les workflows pour éviter la confusion
- **Priorité:** Focus d'abord sur les workflows critiques (création, home, vote)
- **Validation:** Toujours tester sur les deux plateformes après chaque modification
- **Documentation:** Mettre à jour la documentation au fur et à mesure des changements
