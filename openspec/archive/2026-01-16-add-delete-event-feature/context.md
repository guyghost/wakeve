# Context: Delete Event Feature

## Objectif
Implémenter la fonctionnalité de suppression d'événements pour l'organisateur, avec cascade delete des données liées et UI de confirmation.

## Contraintes
- Plateforme : KMP (Android + iOS)
- Offline first : Oui (sync metadata pour tombstone)
- Design system : Material You (Android) + Liquid Glass (iOS)

## Décisions Techniques
| Décision | Justification | Agent |
|----------|---------------|-------|
| Transaction SQLite pour cascade | Garantir atomicité | @orchestrator |
| userId dans DeleteEvent intent | Vérification autorisation côté state machine | @orchestrator |
| Soft-block FINALIZED status | Événement terminé ne doit pas être supprimé | @orchestrator |
| Haptic feedback iOS (heavy + success) | Feedback utilisateur pour action destructive | @codegen |
| Error color for confirm button | WCAG: distinction visuelle pour action destructive | @codegen |

## Artéfacts Produits
| Fichier | Agent | Status |
|---------|-------|--------|
| shared/src/commonTest/kotlin/com/guyghost/wakeve/offline/DeleteEventOfflineTest.kt | @tests | ✅ Created |
| shared/src/commonTest/kotlin/com/guyghost/wakeve/e2e/DeleteEventE2ETest.kt | @tests | ✅ Created |
| wakeveApp/iosAppUITests/DeleteEventUITests.swift | @tests | ✅ Created |
| wakeveApp/wakeveApp/Views/ModernEventDetailView.swift | @codegen | ✅ Modified (haptic + a11y) |
| wakeveApp/src/commonMain/kotlin/com/guyghost/wakeve/EventDetailScreen.kt | @codegen | ✅ Modified (a11y) |

## Notes Inter-Agents
<!-- Format: [@source → @destination] Message -->
[@codegen → @review] Android delete button uses error color scheme for destructive action (WCAG compliance)
[@codegen → @review] iOS uses `.destructive` role which automatically applies correct styling
[@tests → @review] 10 offline tests + 6 E2E KMP tests + 10 iOS UI tests created
[@codegen → @review] Accessibility labels added with hints for VoiceOver/TalkBack
