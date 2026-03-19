# Context: Update Documentation & Architecture

## Objective
Mettre à jour la documentation du projet Wakeve pour refléter les nouvelles fonctionnalités implémentées.

## Scope

### 1. README.md Principal
**Fichier**: `/Users/guy/Developer/dev/wakeve/README.md`

**Sections à mettre à jour**:
- **Features**: Ajouter MeetingService avec liens Zoom/Google Meet/FaceTime
- **API Endpoints**: Documenter les nouveaux endpoints MeetingProxyRoutes
- **Architecture**: Documenter le pattern FC&IS pour MeetingService
- **Tests**: Mentionner les 22 tests E2E ajoutés

### 2. Architecture MeetingService
**Fichier**: `/Users/guy/Developer/dev/wakeve/docs/architecture/meeting-service.md` (à créer)

**Contenu**:
- Diagramme C4 (Component level) selon Simon Brown
- Description du pattern Provider
- Flux de création de réunion
- Sécurité (proxy backend)
- Diagramme de séquence pour la création d'une réunion

### 3. API Documentation
**Fichier**: `/Users/guy/Developer/dev/wakeve/docs/api/meeting-api.md` (à créer)

**Endpoints à documenter**:
```
POST /api/meetings/proxy/zoom/create
POST /api/meetings/proxy/google-meet/create
POST /api/meetings/proxy/zoom/{id}/cancel
GET /api/meetings/proxy/zoom/{id}/status
```

Pour chaque endpoint:
- Description
- Request/Response format
- Exemples curl
- Codes d'erreur

### 4. Changelog
**Fichier**: `/Users/guy/Developer/dev/wakeve/CHANGELOG.md` (à créer ou mettre à jour)

**Format**: Keep a Changelog
```markdown
## [Unreleased]
### Added
- MeetingService avec support Zoom, Google Meet, FaceTime
- MeetingProxyRoutes pour sécuriser les clés API
- 22 tests E2E (PRD Workflow, Multi-user, Offline Sync)
- UI MeetingListScreen avec édition et régénération de liens
```

## Livrables
1. README.md mis à jour
2. docs/architecture/meeting-service.md créé
3. docs/api/meeting-api.md créé
4. CHANGELOG.md créé/mis à jour

## Format
- Markdown conforme GitHub
- Diagrammes en PlantUML ou Mermaid
- Exemples de code Kotlin
