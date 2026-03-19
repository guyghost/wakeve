# Proposition : Implémentation du Contenu des Tabs (Events, Explore, Profile)

**Date** : 27 décembre 2025
**Statut** : Proposition
**Priorité** : Haute

## Contexte

L'application Wakeve dispose maintenant d'un TabBar natif iOS avec 4 tabs : Home, Events, Explore et Profile. Cependant, seuls le tab Home est implémenté avec du contenu réel. Les tabs Events, Explore et Profile sont des placeholders avec un message "Content coming soon".

L'utilisateur a besoin d'accéder aux fonctionnalités de ces tabs pour une expérience complète de l'application :
- **Events** : Liste et gestion des événements auxquels l'utilisateur participe
- **Explore** : Découverte de nouvelles fonctionnalités, suggestions et inspirations
- **Profile** : Gestion du profil utilisateur, préférences et paramètres

## Objectifs

1. **Implémenter le tab Events** avec la liste des événements de l'utilisateur
2. **Implémenter le tab Explore** avec des suggestions et découvertes
3. **Implémenter le tab Profile** avec les préférences et paramètres utilisateur
4. **Assurer la cohérence visuelle** avec le design Liquid Glass sur iOS
5. **Utiliser les composants récents** (LiquidGlassCard, LiquidGlassButton)

## Périmètre (Scope)

### Inclus

#### Tab Events
- Liste des événements de l'utilisateur (triés par date)
- Filtrage par statut (À venir, En cours, Passés)
- Vue détaillée des événements (navigation vers EventDetailView)
- Pull-to-refresh pour synchroniser les événements
- Empty state quand aucun événement

#### Tab Explore
- Section "Suggestion de la journée" (événements inspirants ou lieux)
- Section "Dernières fonctionnalités" avec explication des nouveautés
- Section "Idées d'événements" (suggestions thématiques : weekend, team building, anniversaire)
- Cards interactives pour chaque section
- Navigation vers création d'événement depuis suggestions

#### Tab Profile
- Affichage du profil utilisateur (nom, email, avatar placeholder)
- Section "Mes Préférences" :
  - Notifications (push, email)
  - Thème (clair/sombre)
  - Langue
- Section "Apparence" :
  - Toggle dark mode
  - Toggle liquid glass (si iOS < 26, fallback material)
- Section "À propos" :
  - Version de l'app
  - Lien vers documentation
  - Lien vers GitHub
- Bouton de déconnexion (existant)

### Exclus

- Édition du profil utilisateur (photo, nom, email) - futur
- Paramètres avancés (gestion des données, suppression de compte) - futur
- Explore avec contenu dynamique basé sur IA - futur
- Intégration avec réseaux sociaux - futur
- Paramètres de confidentialité détaillés - futur

## Scénarios Utilisateur

### Scénario 1 : Navigation vers Events tab

**Given** L'utilisateur est sur le tab Home
**When** Il clique sur le tab Events
**Then** Une liste d'événements s'affiche
**And** Les événements sont triés par date du plus proche au plus lointain
**When** Il clique sur un événement
**Then** L'écran de détail de l'événement s'ouvre
**And** Il peut voir les détails complets de l'événement

### Scénario 2 : Filtrage des événements

**Given** L'utilisateur est sur le tab Events
**When** Il sélectionne le filtre "À venir"
**Then** Seuls les événements futurs s'affichent
**When** Il sélectionne le filtre "Passés"
**Then** Seuls les événements terminés s'affichent

### Scénario 3 : Exploration du tab Explore

**Given** L'utilisateur clique sur le tab Explore
**Then** La suggestion de la journée s'affiche en premier
**And** Les sections "Idées d'événements" et "Nouvelles fonctionnalités" sont accessibles par scroll
**When** Il clique sur une suggestion d'événement
**Then** L'écran de création d'événement s'ouvre avec les détails pré-remplis

### Scénario 4 : Gestion du profil

**Given** L'utilisateur clique sur le tab Profile
**Then** Son nom et email sont affichés
**When** Il active le dark mode
**Then** L'application passe immédiatement en mode sombre
**When** Il clique sur "Se déconnecter"
**Then** Il est redirigé vers l'écran de connexion
**And** Son état d'authentification est réinitialisé

### Scénario 5 : Empty state Events

**Given** L'utilisateur n'a aucun événement
**When** Il navigue vers le tab Events
**Then** Un message "Aucun événement" s'affiche
**And** Un bouton "Créer un événement" est proposé
**When** Il clique sur le bouton
**Then** L'écran de création d'événement s'ouvre

## Impact

### Expérience Utilisateur

- **Complétude** : Tous les tabs sont maintenant fonctionnels avec du contenu
- **Découverte** : Le tab Explore permet de découvrir les fonctionnalités
- **Gestion** : Le tab Profile centralise les paramètres utilisateur
- **Navigation** : Navigation fluide entre les différents tabs avec contenu cohérent

### Implémentation Technique

- **iOS** : Création de 3 nouvelles vues (`EventsTabView`, `ExploreTabView`, `ProfileTabView`)
- **Navigation** : Intégration avec `NavigationStack` pour chaque tab
- **State Management** : Utilisation de `@State` et `@Binding` pour les filtres et préférences
- **Persistance** : UserDefaults pour les préférences utilisateur (dark mode, notifications)

### Code Existant

- `WakevTabBar.swift` : TabBar container déjà implémenté
- `ContentView.swift` : Structure avec placeholder views à remplacer
- `LiquidGlassCard.swift` : Composant carte à utiliser
- `LiquidGlassButton.swift` : Composant bouton à utiliser
- `AuthStateManager.swift` : Gestion de l'authentification (déconnexion)

## Design System

### iOS (Liquid Glass + SwiftUI)

- **Colors** : Palette Wakeve (#2563EB primary, #7C3AED accent)
- **Typography** : Échelle iOS (LargeTitle, Title3, Body, etc.)
- **Materials** : Utiliser les composants `LiquidGlassCard` et `LiquidGlassButton`
- **Spacing** : Échelle de 4px (xs: 4px, sm: 8px, md: 12px, lg: 16px, etc.)
- **Animations** : Transitions fluides avec `animation(.spring())`

### Structure des Vues

#### EventsTabView
```swift
struct EventsTabView: View {
    @State private var selectedFilter: EventFilter = .upcoming
    @State private var events: [Event] = []

    var body: some View {
        NavigationStack {
            VStack {
                // Filter pills
                // Events list (LazyVStack)
            }
            .navigationTitle("Mes Événements")
            .refreshable {
                // Fetch events
            }
        }
    }
}
```

#### ExploreTabView
```swift
struct ExploreTabView: View {
    var body: some View {
        ScrollView {
            VStack(spacing: 24) {
                // Suggestion of the day card
                // Event ideas section
                // New features section
            }
            .padding()
        }
        .navigationTitle("Explorer")
    }
}
```

#### ProfileTabView
```swift
struct ProfileTabView: View {
    @AppStorage("darkMode") private var darkMode = false
    @Environment(\.auth) var auth

    var body: some View {
        ScrollView {
            VStack(spacing: 24) {
                // User profile header
                // Preferences section
                // Appearance section
                // About section
                // Sign out button
            }
            .padding()
        }
        .navigationTitle("Mon Profil")
    }
}
```

## Livrables

### Tâches d'implémentation

#### Events Tab
- [ ] Créer `EventsTabView.swift` dans `iosApp/iosApp/Views/`
- [ ] Implémenter la liste d'événements avec LazyVStack
- [ ] Ajouter les filtres (À venir, En cours, Passés)
- [ ] Intégrer le pull-to-refresh
- [ ] Ajouter l'empty state avec CTA
- [ ] Connecter à EventDetailView pour la navigation
- [ ] Tester l'affichage des événements
- [ ] Tester les filtres

#### Explore Tab
- [ ] Créer `ExploreTabView.swift` dans `iosApp/iosApp/Views/`
- [ ] Implémenter la section "Suggestion de la journée"
- [ ] Créer des cards pour suggestions d'événements
- [ ] Ajouter la section "Nouvelles fonctionnalités"
- [ ] Ajouter la section "Idées d'événements"
- [ ] Connecter les CTAs à la création d'événement
- [ ] Tester l'exploration et les interactions

#### Profile Tab
- [ ] Créer `ProfileTabView.swift` dans `iosApp/iosApp/Views/`
- [ ] Implémenter l'en-tête du profil (nom, email, avatar)
- [ ] Créer la section "Mes Préférences" avec toggles
- [ ] Ajouter la section "Apparence" (dark mode toggle)
- [ ] Ajouter la section "À propos"
- [ ] Connecter le bouton "Se déconnecter" à AuthStateManager
- [ ] Tester la modification des préférences
- [ ] Tester la déconnexion

#### Navigation & State
- [ ] Intégrer NavigationStack dans chaque tab
- [ ] Persister les préférences utilisateur dans UserDefaults
- [ ] Tester la navigation entre les tabs
- [ ] Tester la persistance des préférences

### Tests

- Test d'affichage du tab Events
- Test des filtres d'événements
- Test de navigation vers EventDetailView
- Test de pull-to-refresh
- Test de l'empty state Events
- Test d'affichage du tab Explore
- Test des interactions dans Explore
- Test d'affichage du tab Profile
- Test du toggle dark mode
- Test de la déconnexion
- Test de la persistance des préférences

## Risques et Mitigations

### Risque 1 : Pas de données d'événements réelles

**Mitigation** : Utiliser des mock data pour le développement et les tests, préparer l'intégration avec le backend

### Risque 2 : Navigation complexe entre tabs

**Mitigation** : Utiliser NavigationStack indépendant pour chaque tab, bien documenter le flow

### Risque 3 : Préférences utilisateur non persistées

**Mitigation** : Utiliser @AppStorage pour UserDefaults automatique, tester après redémarrage de l'app

## Success Criteria

✅ Le tab Events affiche une liste d'événements avec filtres
✅ Le tab Explore affiche des suggestions et découvertes
✅ Le tab Profile affiche les préférences utilisateur et fonctionne
✅ La navigation entre les tabs est fluide
✅ Le design respecte Liquid Glass sur tous les tabs
✅ Les préférences utilisateur sont persistées
✅ La déconnexion fonctionne correctement
✅ Tous les tests passent

## Documentation

- `IMPLEMENTATION_SUMMARY.md` : Résumé de l'implémentation après complétion
- Mise à jour de `QUICK_START.md` avec description des tabs

## Notes

- Utiliser les composants existants `LiquidGlassCard` et `LiquidGlassButton` pour la cohérence
- Le dark mode doit être testé en mode clair et sombre
- Les sections du tab Explore pourront être enrichies ultérieurement avec du contenu dynamique
- Les préférences utilisateur pourront être étendues à l'avenir (notifications, langues, etc.)
