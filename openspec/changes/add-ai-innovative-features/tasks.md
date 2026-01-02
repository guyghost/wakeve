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

## Phase 3: Feature 2 - Intelligent Voice Assistant (Mois 5-6) ✅ COMPLETED
- [x] Implémenter `VoiceAssistantService` (commonMain).
- [x] Configurer SiriKit (Intent definitions, vocabulaire spécifique à Wakeve).
- [x] Configurer Google Assistant App Actions (shortcuts.xml, BII definitions).
- [x] Créer le `VoiceCommandParser` pour transformer le texte transcrit en `Intents`.
- [x] Ajouter le bouton micro flottant avec animation d'onde sonore (Liquid Glass / Material You).
- [x] Implémenter le retour vocal (Text-to-Speech) pour confirmer les actions.
- [x] Ajouter 5 tests d'accessibilité et de parsing vocal.

## Phase 4: Feature 3 - Gamification System (Mois 7-8) ✅ COMPLETED
- [x] Implémenter `GamificationService` et la logique de calcul de points.
- [x] Créer les assets graphiques pour les 15 premiers badges.
- [x] Développer `LeaderboardRepository` avec support de cache local.
- [x] Créer l'écran "Profil & Succès" sur Android.
- [x] Créer l'écran "Profil & Succès" sur iOS avec effets Liquid Glass.
- [x] Ajouter les animations de célébration lors du gain d'un badge.
   - [x] Android: BadgeCelebrationAnimation.kt (Material You, confettis, particles)
   - [x] iOS: BadgeCelebrationAnimation.swift (Liquid Glass, confettis)
- [x] Intégrer les notifications push "Badge débloqué" via `NotificationService`.
- [x] Ajouter 12 tests pour les règles de gamification.
   - [x] Tests 1-4: Points calculation (CREATE_EVENT, VOTE, COMMENT, PARTICIPATE)
   - [x] Tests 5-8: Badge unlocking (Super Organizer, Regular Attendee, Millennium Club, Dedicated Voter)
   - [x] Tests 9-10: Leaderboard ranking
   - [x] Tests 11-12: Points decay and duplicate prevention

## Phase 5: Feature 4 - Real-Time Chat Integration (Mois 9-10) ✅ COMPLETED
- [x] Implémenter `ChatService` sur le backend Ktor (WebSocket routing).
- [x] Créer le client `ChatRepository` avec gestion de la persistance offline.
- [x] Développer `ChatScreen` avec bulles de message et bulles d'action.
- [x] Ajouter le support des réactions émojis en temps réel.
- [x] Implémenter les "Typing Indicators" et les "Read Receipts".
- [x] Gérer la reconnexion automatique du WebSocket avec backoff exponentiel.
- [x] Ajouter 15 tests d'intégration pour le chat temps réel.

## Phase 6: Feature 5 - Photo Recognition & Albums (Mois 11-12) ✅ COMPLETED
- [x] Intégrer Google ML Kit Vision sur Android pour l'auto-tagging.
  - [x] Créer les modèles Photo, PhotoTag, PhotoCategory, BoundingBox, FaceDetection
  - [x] Créer l'interface PhotoRecognitionService commune
  - [x] Créer AndroidPhotoRecognitionService avec ML Kit Face Detection et Image Labeling
  - [x] Créer IosPhotoRecognitionService complète avec Apple Vision Framework
  - [x] Ajouter les dépendances ML Kit dans build.gradle.kts
- [x] Intégrer Apple Vision Framework sur iOS pour la reconnaissance faciale.
  - [x] Implémenter VNDetectFaceRectanglesRequest pour détection faciale
  - [x] Implémenter VNClassifyImageRequest pour classification d'images
  - [x] Implémenter VNRecognizeAnimalsRequest pour détection d'animaux
  - [x] Gestion de la mémoire avec imageOrientation et cache
- [x] Implémenter `PhotoRecognitionService` pour grouper les photos par tags/moments.
  - [x] Créer Album.kt modèle de données
  - [x] Créer PhotoRepository interface et implémentation
  - [x] Créer AlbumRepository interface et implémentation
  - [x] Implémenter PhotoRecognitionService avec processPhoto, createAutoAlbum, searchPhotos, findSimilarPhotos
  - [x] Créer Album.sq pour persistance SQLDelight
- [x] Créer la vue "Albums Intelligents" (Smart Grid).
  - [x] SmartAlbumsScreen.kt pour Android (Material You design)
  - [x] SmartAlbumsView.swift pour iOS (Liquid Glass design)
  - [x] AlbumCard composant avec animations et gradient overlay
  - [x] Support pour auto-generated badges et photo count indicators
- [x] Développer la logique de suggestion de partage intelligente.
  - [x] SharingSuggestionService interface avec 4 types de suggestions
  - [x] DefaultSharingSuggestionService implémentation multi-factor
  - [x] SuggestionReason enum (DETECTED_IN_PHOTOS, EVENT_PARTICIPANT, TAG_INTEREST, FREQUENT_SHARE_TARGET, MULTI_FACTOR)
  - [x] SharingInsights pour statistiques de partage personnalisé
  - [x] SharingScoringWeights (DEFAULT, CONSERVATIVE, AGGRESSIVE)
- [x] Optimiser l'usage mémoire lors du traitement ML sur les photos haute résolution.
  - [x] MemoryOptimizationConfig avec downsampling, memory pooling, batch processing
  - [x] AndroidMemoryOptimizedImageProcessor (Bitmap sampling, RGB_565, recycling)
  - [x] IosMemoryOptimizedImageProcessor (UIGraphicsImageRenderer, autoreleasepool)
  - [x] ImageMemoryPool interface et implémentation Android/iOS
  - [x] EstimateImageMemoryUsageMB et calculateOptimalDownsampleFactor helpers
- [x] Ajouter 10 tests unitaires pour le tagging et l'organisation des albums.
  - [x] Créer PhotoRecognitionServiceTest avec 10 tests unitaires
  - [x] Tests: processPhoto, createAutoAlbum, createCustomAlbum, searchPhotos, findSimilarPhotos

## Phase 7: Global Integration & Testing (Mois 12+) ✅ COMPLETED
- [x] Réaliser une passe complète de QA sur les 5 fonctionnalités combinées.
- [x] Optimiser la consommation de batterie globale (Profileur Android/Instruments iOS).
- [x] Mettre à jour toute la documentation technique (`docs/README.md`, `API.md`).
- [x] Préparer la version Beta pour les testeurs externes.
- [x] Finaliser le rapport de changement OpenSpec et archiver la proposition.
- [x] Valider l'accessibilité complète (Screen Readers) sur toutes les nouvelles vues.
