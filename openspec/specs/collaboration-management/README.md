# Collaboration Management

Index de la documentation pour la fonctionnalité de collaboration (système de commentaires).

Documents:

- `spec.md` - Spécification technique et API REST (modèles, endpoints, notifications, cas d'usage)
- `user-guide.md` - Guide utilisateur pour les plateformes (Android, iOS, Web)
- `api-reference.md` - (À venir) Référence API détaillée avec exemples cURL et codes d'erreur
- `diagrams/` - (Optionnel) Diagrammes Mermaid pour les flux et séquences

Liens rapides:

- Spec technique: ./spec.md
- Guide utilisateur: ./user-guide.md

---

Considérations:
- Les endpoints sont implémentés côté serveur dans `server/src/main/kotlin/com/guyghost/wakeve/routes/CommentRoutes.kt`
- Les modèles partagés se trouvent dans `shared/src/commonMain/kotlin/com/guyghost/wakeve/models/CommentModels.kt`

*Fin du README*