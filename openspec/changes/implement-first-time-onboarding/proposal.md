# Proposition : Onboarding au Premier Lancement

**Date** : 27 décembre 2025
**Statut** : Proposition
**Priorité** : Haute

## Contexte

Actuellement, l'application Wakeve affiche directement l'écran d'accueil après le splash screen, sans présenter l'onboarding aux nouveaux utilisateurs. Bien qu'un écran d'onboarding existe côté Android (`OnboardingScreen.kt`), il n'est jamais affiché dans le flow utilisateur.

Ceci manque d'une expérience utilisateur cohérente pour les nouveaux utilisateurs, qui n'ont pas de présentation des fonctionnalités clés de l'application (création d'événements, collaboration, organisation complète).

## Objectifs

1. **Afficher l'onboarding aux nouveaux utilisateurs** lors de leur première connexion
2. **Implémenter l'écran d'onboarding pour iOS** (SwiftUI) - actuellement inexistant
3. **Assurer la cohérence visuelle** entre Android (Material You) et iOS (Liquid Glass)
4. **Mémoriser le passage de l'onboarding** pour ne pas l'afficher aux utilisateurs existants

## Périmètre (Scope)

### Inclus

- Détection du premier lancement (SharedPreferences Android, UserDefaults iOS)
- Écran d'onboarding Android (4 étapes, déjà implémenté mais non utilisé)
- **Nouveau** : Écran d'onboarding iOS (4 étapes, SwiftUI + Liquid Glass)
- Intégration dans le flow de navigation après la première connexion authentifiée
- Persistance de l'état d'onboarding complété
- Design cross-platform cohérent

### Exclus

- Modifications du flow d'authentification (hors scope)
- Onboarding pour fonctionnalités spécifiques (transport, budget, etc.)
- A/B testing ou analytics de l'onboarding
- Personnalisation de l'onboarding basée sur le profil utilisateur

## Scénarios Utilisateur

### Scénario 1 : Premier lancement après inscription (Android)

**Given** Un utilisateur s'inscrit pour la première fois sur Android
**When** Il se connecte avec succès
**Then** L'écran d'onboarding s'affiche avec 4 étapes (créer événements, collaborer, organiser, profiter)
**When** Il complète les 4 étapes
**Then** L'état d'onboarding est sauvegardé
**And** L'utilisateur est redirigé vers l'écran d'accueil
**And** L'onboarding ne s'affiche plus aux prochains lancements

### Scénario 2 : Premier lancement après inscription (iOS)

**Given** Un utilisateur s'inscrit pour la première fois sur iOS
**When** Il se connecte avec succès
**Then** L'écran d'onboarding s'affiche avec 4 étapes (créer événements, collaborer, organiser, profiter)
**When** Il complète les 4 étapes
**Then** L'état d'onboarding est sauvegardé dans UserDefaults
**And** L'utilisateur est redirigé vers l'écran d'accueil
**And** L'onboarding ne s'affiche plus aux prochains lancements

### Scénario 3 : Utilisateur existant (Android/iOS)

**Given** Un utilisateur s'est déjà connecté et a complété l'onboarding
**When** Il relance l'application
**Then** L'écran d'onboarding ne s'affiche PAS
**And** L'utilisateur accède directement à l'écran d'accueil

### Scénario 4 : Skip l'onboarding (Android/iOS)

**Given** Un nouvel utilisateur voit l'écran d'onboarding
**When** Il clique sur "Passer" ou ferme l'onboarding
**Then** L'état d'onboarding est marqué comme complété
**And** L'utilisateur est redirigé vers l'écran d'accueil

## Impact

### Expérience Utilisateur

- **Amélioration** : Nouveaux utilisateurs découvrent les fonctionnalités clés de Wakeve
- **Cohérence** : Même expérience d'onboarding sur Android et iOS
- **Persistance** : L'onboarding ne se répète pas inutilement

### Implémentation Technique

- **Android** : Modification de `App.kt` pour vérifier l'état d'onboarding avant d'afficher Home
- **iOS** : Création d'un nouveau fichier `OnboardingView.swift` dans `iosApp/iosApp/Views/`
- **Stockage** :
  - Android : SharedPreferences (`HAS_COMPLETED_ONBOARDING`)
  - iOS : UserDefaults (`hasCompletedOnboarding`)

### Code Existant

- Android : `OnboardingScreen.kt` déjà implémenté avec 4 étapes complètes
- iOS : Aucun onboarding existant (à créer de zéro)

## Design System

### Android (Material You + Jetpack Compose)

- **Colors** : Utiliser Material Theme 3 colors (`primary`, `onPrimary`, `surface`, etc.)
- **Typography** : Échelle Material (headlineLarge, bodyLarge, etc.)
- **Shapes** : CircleShape pour les icônes, RoundedCornerShape pour les éléments
- **Animation** : HorizontalPager avec animation fluide entre étapes

### iOS (Liquid Glass + SwiftUI)

- **Colors** : Palette de couleurs Wakeve définie dans `WakevColors.swift`
- **Typography** : Échelle iOS (LargeTitle, Title3, Body, etc.)
- **Materials** : Utiliser `.ultraThinMaterial` ou `.regularMaterial` pour le fond
- **Shapes** : Coins arrondis continus (`.continuous` corner radius)
- **Animation** : TabView avec `PageTabViewStyle` pour l'effet page swipe

### Contenu de l'Onboarding

Les 4 étapes sont les mêmes sur les deux plateformes :

1. **Créez vos événements**
   - Icone : 📅
   - Description : "Organisez facilement des événements entre amis et collègues. Définissez des dates, proposez des créneaux horaires et laissez les participants voter."
   - Features : Création rapide, Sondage de disponibilité, Calcul automatique

2. **Collaborez en équipe**
   - Icone : 👥
   - Description : "Travaillez ensemble sur l'organisation de l'événement. Partagez les responsabilités et suivez la progression en temps réel."
   - Features : Gestion des participants, Attribution des tâches, Suivi en temps réel

3. **Organisez tout en un**
   - Icone : 🎯
   - Description : "Gérez l'hébergement, les repas, les activités et le budget. Tout au même endroit pour une organisation sans faille."
   - Features : Planification d'hébergement, Organisation des repas, Suivi du budget

4. **Profitez de vos événements**
   - Icone : 🎉
   - Description : "Une fois l'organisation terminée, profitez de l'événement avec vos proches sans stress."
   - Features : Vue d'ensemble, Rappels intégrés, Calendrier natif

## Livrables

### Tâches d'implémentation

- [ ] Modifier le flow Android pour déclencher l'onboarding au premier lancement
- [ ] Créer l'écran d'onboarding iOS (`OnboardingView.swift`)
- [ ] Implémenter la persistance de l'état d'onboarding (SharedPreferences + UserDefaults)
- [ ] Tester le flow sur Android
- [ ] Tester le flow sur iOS
- [ ] Valider la cohérence visuelle cross-platform

### Tests

- Test de premier lancement (Android)
- Test de premier lancement (iOS)
- Test de lancement répété (pas d'onboarding)
- Test de skip onboarding
- Test de persistance après réinstallation

## Risques et Mitigations

### Risque 1 : Incohérence visuelle entre Android et iOS

**Mitigation** : Utiliser les guidelines respectives (Material You / Liquid Glass) tout en gardant le même contenu et structure

### Risque 2 : État d'onboarding non persisté

**Mitigation** : Tests de persistence et fallback vers Home si erreur de lecture

### Risque 3 : Flow d'authentification complexe

**Mitigation** : L'onboarding se déclenche UNIQUEMENT après authentification réussie, évitant les cas edge

## Success Criteria

✅ L'onboarding s'affiche au premier lancement authentifié (Android)
✅ L'onboarding s'affiche au premier lancement authentifié (iOS)
✅ L'onboarding ne s'affiche pas aux lancements suivants
✅ Les 4 étapes sont cohérentes entre Android et iOS
✅ Le design respecte Material You (Android) et Liquid Glass (iOS)
✅ Tous les tests passent

## Documentation

- `openspec/specs/user-onboarding/spec.md` : Delta des spécifications
- `IMPLEMENTATION_SUMMARY.md` : Résumé de l'implémentation après complétion

## Notes

- L'écran d'onboarding Android existe déjà (`OnboardingScreen.kt`), il suffit de l'intégrer dans le flow
- Pour iOS, créer `OnboardingView.swift` en suivant le pattern des autres vues SwiftUI
- L'onboarding doit être optionnel (possibilité de skip)
- L'état d'onboarding doit persister même si l'utilisateur se déconnecte/reconnecte
