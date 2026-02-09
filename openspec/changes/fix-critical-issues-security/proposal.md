# Change: Fix Critical Security Issues

## Why
L'analyse du code a révélé **6 issues critiques** de sécurité et d'architecture qui doivent être corrigées avant toute mise en production :

1. **Secret JWT en dur** - Permet la falsification de tokens
2. **Bypass whitelist IP** - Wildcard 0.0.0.0/0 autorise n'importe quelle IP
3. **Blocking call dans async** - Épuisement du pool de threads
4. **FC&IS violations** - Core contient des fonctions non-déterministes
5. **Indexes DB manquants** - Scan complet sur tables fréquemment requêtées
6. **Tests désactivés** - Meeting use case tests non fonctionnels

Ces problèmes représentent des **risques de sécurité immédiats** et une **dette technique bloquante**.

## What Changes

### Security Fixes
- [ ] Supprimer le secret JWT par défaut en production
- [ ] Retirer le wildcard 0.0.0.0/0 de la whitelist
- [ ] Remplacer runBlocking par des fonctions suspend avec cache

### Architecture Fixes  
- [ ] Déplacer Random.nextInt() du Core vers le Shell
- [ ] Déplacer currentTimeMillis() du Core vers le Shell
- [ ] Retirer expect/actual du Core (déplacer vers Shell)

### Database Fixes
- [ ] Ajouter indexes sur Event (organizerId, status)
- [ ] Ajouter indexes sur Vote (eventId, timeslotId)
- [ ] Ajouter indexes sur Participant (eventId, role)
- [ ] Ajouter indexes sur TimeSlot (eventId, startTime)
- [ ] Ajouter indexes sur Scenario (eventId)

### Test Fixes
- [ ] Activer les tests meeting désactivés
- [ ] Implémenter mocking approprié pour tests

## Impact
- **Affected specs**: security, auth, database, testing
- **Affected code**: 
  - `server/src/main/kotlin/com/guyghost/wakeve/Application.kt`
  - `server/src/main/kotlin/com/guyghost/wakeve/security/SecurityConfig.kt`
  - `shared/src/commonMain/kotlin/com/guyghost/wakeve/auth/core/`
  - `shared/src/commonMain/sqldelight/**/*.sq`
  - `shared/src/commonTest/kotlin/com/guyghost/wakeve/presentation/usecase/`

## Risks
- **JWT Secret**: Breaking change si applications clientes dépendent du secret (non - côté serveur uniquement)
- **IP Whitelist**: Peut bloquer l'accès aux métriques si mal configuré
- **FC&IS**: Changement d'API pour création d'utilisateurs (time parameter requis)

## Testing Strategy
- Tests unitaires pour chaque correction
- Tests d'intégration pour JWT blacklist
- Benchmarks pour indexes DB
- Tests end-to-end pour auth flow
