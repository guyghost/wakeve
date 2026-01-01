# Tasks: AI Innovation Suite Implementation

## Phase 1: Infrastructure & Common Setup (Mois 1-2) ✅ COMPLETED
- [x] Mettre à jour `libs.versions.toml` avec les dépendances ML (TensorFlow Lite, ML Kit).
- [x] Configurer le serveur Ktor pour supporter les WebSockets (Phase 4 infrastructure).
- [x] Designer le schéma de base de données SQLDelight pour `UserPoints`, `UserBadges` et `ChatMessages`.
- [x] Créer les interfaces communes pour `RecommendationEngine` et `VoiceAssistantService`.
- [x] Définir les `EventType` étendus pour supporter les recommandations contextuelles.
- [x] Mettre en place les outils de monitoring de performance ML sur Android et iOS.

## Phase 2: Feature 1 - AI Predictive Recommendations (Mois 3-4) ✅ COMPLETED
- [x] Implémenter `UserPreferencesRepository` pour collecter l'historique de vote anonymisé.
- [x] Développer l'algorithme de scoring ML pour le `RecommendationEngine`.
- [x] Créer les modèles de données pour les suggestions "AI-powered".
- [x] Intégrer les badges "IA" dans les vues de sondage Android (Compose).
- [x] Intégrer les badges "IA" dans les vues de sondage iOS (SwiftUI).
- [x] Ajouter 8 tests unitaires pour la logique de recommandation ML.
- [x] Valider la précision des recommandations avec des jeux de données simulés.

## Phase 3: Feature 2 - Intelligent Voice Assistant (Mois 5-6)
- [ ] Implémenter `VoiceAssistantService` (commonMain).
- [ ] Configurer SiriKit (Intent definitions, vocabulaire spécifique à Wakeve).
- [ ] Configurer Google Assistant App Actions (shortcuts.xml, BII definitions).
- [ ] Créer le `VoiceCommandParser` pour transformer le texte transcrit en `Intents`.
- [ ] Ajouter le bouton micro flottant avec animation d'onde sonore (Liquid Glass / Material You).
- [ ] Implémenter le retour vocal (Text-to-Speech) pour confirmer les actions.
- [ ] Ajouter 5 tests d'accessibilité et de parsing vocal.

## Phase 4: Feature 3 - Gamification System (Mois 7-8)
- [ ] Implémenter `GamificationService` et la logique de calcul de points.
- [ ] Créer les assets graphiques pour les 15 premiers badges.
- [ ] Développer `LeaderboardRepository` avec support de cache local.
- [ ] Créer l'écran "Profil & Succès" sur Android.
- [ ] Créer l'écran "Profil & Succès" sur iOS avec effets Liquid Glass.
- [ ] Ajouter les animations de célébration lors du gain d'un badge.
- [ ] Intégrer les notifications push "Badge débloqué" via `NotificationService`.
- [ ] Ajouter 12 tests pour les règles de gamification.

## Phase 5: Feature 4 - Real-Time Chat Integration (Mois 9-10)
- [ ] Implémenter `ChatService` sur le backend Ktor (WebSocket routing).
- [ ] Créer le client `ChatRepository` avec gestion de la persistance offline.
- [ ] Développer `ChatScreen` avec bulles de message et bulles d'action.
- [ ] Ajouter le support des réactions émojis en temps réel.
- [ ] Implémenter les "Typing Indicators" et les "Read Receipts".
- [ ] Gérer la reconnexion automatique du WebSocket avec backoff exponentiel.
- [ ] Ajouter 15 tests d'intégration pour le chat temps réel.

## Phase 6: Feature 5 - Photo Recognition & Albums (Mois 11-12)
- [ ] Intégrer Google ML Kit Vision sur Android pour l'auto-tagging.
- [ ] Intégrer Apple Vision Framework sur iOS pour la reconnaissance faciale.
- [ ] Implémenter `PhotoRecognitionService` pour grouper les photos par tags/moments.
- [ ] Créer la vue "Albums Intelligents" (Smart Grid).
- [ ] Développer la logique de suggestion de partage intelligente.
- [ ] Optimiser l'usage mémoire lors du traitement ML sur les photos haute résolution.
- [ ] Ajouter 10 tests unitaires pour le tagging et l'organisation des albums.

## Phase 7: Global Integration & Testing (Mois 12+)
- [ ] Réaliser une passe complète de QA sur les 5 fonctionnalités combinées.
- [ ] Optimiser la consommation de batterie globale (Profileur Android/Instruments iOS).
- [ ] Mettre à jour toute la documentation technique (`docs/README.md`, `API.md`).
- [ ] Préparer la version Beta pour les testeurs externes.
- [ ] Finaliser le rapport de changement OpenSpec et archiver la proposition.
- [ ] Valider l'accessibilité complète (Screen Readers) sur toutes les nouvelles vues.
