# Proposal: AI Innovation Suite for Event Planning

> **Change ID**: `add-ai-innovative-features`
> **Type**: New Features
> **Status**: 📋 Proposal
> **Date**: 2026-01-01

## Context

L'application Wakeve a franchi des jalons importants avec l'implémentation de la Phase 2.6 (Enhanced DRAFT Phase) et la validation du PRD complet des Phases 1 à 4. Bien que l'application soit techniquement solide et fonctionnelle, elle évolue dans un marché de la planification d'événements extrêmement saturé et compétitif (Doodle, Calendly, Facebook Events, etc.).

Pour s'imposer comme le leader de la nouvelle génération d'applications de planification, Wakeve doit transcender les fonctionnalités utilitaires classiques pour offrir une expérience "magique" et proactive. Cette proposition introduit une suite de 5 fonctionnalités basées sur l'intelligence artificielle et l'engagement communautaire, transformant Wakeve d'un simple outil de sondage en un véritable compagnon intelligent de vie sociale.

## Why

Le succès de Wakeve dépend de sa capacité à offrir une valeur ajoutée unique que les géants du secteur ne proposent pas encore de manière intégrée :

1.  **Différenciation par l'IA** : Utiliser le Machine Learning pour des recommandations proactives basées sur des patterns complexes impossibles à analyser manuellement.
2.  **Personnalisation profonde** : L'application apprend des préférences passées (lieux, horaires, types d'activités) pour réduire la friction de décision.
3.  **Engagement accru** : La gamification transforme la corvée de planification en une expérience ludique et gratifiante.
4.  **Réduction de la charge cognitive** : L'assistant vocal permet aux utilisateurs de gérer leurs événements dans des contextes "hands-free" (en conduisant, en cuisinant).
5.  **Innovation continue** : L'organisation automatique des souvenirs (photos) prolonge la durée de vie de l'application bien après la fin de l'événement.

## What Changes

### Feature 1: AI Predictive Recommendations (ML)

**Capacité :** `suggestion-management` (extension)

**Description :** Utilisation du Machine Learning pour transformer le `SuggestionService` en un moteur prédictif proactif.

*   **Meilleures dates/lieux** : Analyse croisée de l'historique des participants pour suggérer les créneaux avec la plus haute probabilité d'acceptation.
*   **Activités suggérées** : Recommandations contextuelles basées sur le `EventType` (ex: pour un `TEAM_BUILDING`, suggérer des activités de collaboration locales).
*   **Optimisation automatique** : Suggestion du créneau final qui maximise mathématiquement la satisfaction globale.

**Composants :** `RecommendationEngine` étendu, `UserPreferencesRepository`, Modèles ML (TensorFlow Lite en local + Backend).

---

### Feature 2: Intelligent Voice Assistant

**Capacité :** Nouveau service `VoiceAssistantService`

**Description :** Interface naturelle pour interagir avec Wakeve sans toucher l'écran.

*   **Création naturelle** : "Crée un anniversaire pour Sophie le week-end prochain avec 20 personnes."
*   **Interrogation de statut** : "Combien de personnes ont voté pour samedi ?"
*   **Actions rapides** : "Confirme la date du 15 juin et préviens tout le monde."

**Intégrations :** SiriKit (iOS), Google Assistant SDK (Android), App Actions.

---

### Feature 3: Gamification of Participation

**Capacité :** Nouveau service `GamificationService`

**Description :** Système de récompenses pour encourager la réactivité et la contribution.

*   **Points & Badges** : Points pour les votes rapides ("Early Bird"), l'organisation d'événements ("Master Planner"), et les suggestions pertinentes.
*   **Niveaux de profil** : Évolution de l'utilisateur de "Newbie" à "Event Legend".
*   **Leaderboard social** : Classement amical basé sur l'indice de fiabilité et de participation.

**Composants :** `GamificationService`, `UserPoints` & `UserBadges` models, `LeaderboardRepository`.

---

### Feature 4: Real-Time Chat Integration

**Capacité :** Extension de `collaboration-management`

**Description :** Passage d'un système de commentaires statiques à une communication instantanée riche.

*   **Canaux thématiques** : Threads automatiques pour le "Transport", le "Logement", etc.
*   **Indicateurs temps réel** : Typing indicators, read receipts, réactions émojis.
*   **Persistance intelligente** : Historique lié directement aux décisions prises (le chat nourrit les choix de l'événement).

**Backend :** WebSockets (Ktor) avec fallback Server-Sent Events (SSE).

---

### Feature 5: Photo Recognition & Auto-Tagging

**Capacité :** Nouveau service `PhotoRecognitionService`

**Description :** Organisation intelligente des médias après l'événement.

*   **Auto-tagging** : Reconnaissance locale des visages, des lieux et des objets (nourriture, cadeaux).
*   **Albums intelligents** : Regroupement automatique des meilleures photos par moment fort de l'événement.
*   **Privacy-First** : Analyse ML effectuée majoritairement sur l'appareil (on-device).

**Composants :** ML Kit (Android), Vision Framework (iOS), `PhotoRecognitionService`.

## Scope

### In Scope
- **AI Recommendations** : Moteur de scoring ML, stockage des préférences, UI avec badges "AI Suggestion".
- **Voice Assistant** : Service de parsing de langage naturel, intégration Siri/Google, UI de feedback vocal.
- **Gamification** : Logique de points, système de badges, UI de profil et leaderboard.
- **Real-Time Chat** : Backend WebSocket, client temps réel, interface de chat moderne avec réactions.
- **Photo Recognition** : Analyse locale des images, tags automatiques, vue "Smart Album".

### Out of Scope
- Intégration de Video Calls (Zoom/Meet natif).
- Expériences de Réalité Augmentée (AR).
- Monétisation et paiements in-app (Phase ultérieure).
- Partage direct vers les réseaux sociaux externes (Instagram/Facebook).

## Impact

### Benefits
- **Différenciation radicale** : Positionne Wakeve comme l'app la plus technologiquement avancée du segment.
- **Expérience "Magique"** : L'IA réduit drastiquement l'effort manuel des organisateurs.
- **Rétention utilisateur** : La gamification et le chat créent une habitude d'utilisation quotidienne.
- **Accessibilité accrue** : L'assistant vocal ouvre l'app aux utilisateurs ayant des besoins spécifiques.

### Risks & Mitigation
- **Complexité Technique** : 5 features majeures représentent une charge de travail considérable.
  - *Mitigation* : Approche itérative (Sprint par feature), utilisation intensive de librairies ML existantes.
- **Performance** : L'analyse ML et les WebSockets peuvent impacter la batterie.
  - *Mitigation* : Optimisation des connexions, exécution des tâches lourdes pendant la charge.
- **Confidentialité (Privacy)** : La reconnaissance de photos et de voix est sensible.
  - *Mitigation* : Traitement 100% local pour la reconnaissance faciale, transparence totale sur l'usage des données.

## Success Criteria

### Functional
- ✅ Recommandations ML acceptées par les utilisateurs dans > 60% des cas.
- ✅ Assistant vocal comprend correctement > 85% des intentions de création.
- ✅ Temps de réponse du chat < 200ms en conditions normales.
- ✅ Identification correcte des visages/objets sur > 75% des photos uploadées.

### Non-Functional
- ✅ Tests unitaires et d'intégration couvrant > 80% de la nouvelle logique métier.
- ✅ Latence réseau optimisée pour les WebSockets.
- ✅ Conformité RGPD pour toutes les fonctionnalités IA.

## Timeline

**Estimation Totale : 10 - 12 mois**

1.  **Mois 1-2 : Infrastructure & Setup** (WebSocket Server, ML Pipeline).
2.  **Mois 3-4 : Feature 1 - AI Recommendations** (ML models integration).
3.  **Mois 5-6 : Feature 2 - Voice Assistant** (Platform integrations).
4.  **Mois 7-8 : Feature 3 - Gamification** (UI/UX focus).
5.  **Mois 9-10 : Feature 4 - Real-Time Chat** (Scalability & Sync).
6.  **Mois 11-12 : Feature 5 - Photo Recognition** (On-device ML).
7.  **Mois 12+ : Polishing & Beta Globale.**
