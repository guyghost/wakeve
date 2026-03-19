# Context: Implement iOS MeetingListView

## Objective
Implémenter la vue MeetingList pour iOS en SwiftUI, équivalente à la version Android.

## Scope

### Fichier à Créer
**Path**: `wakeveApp/wakeveApp/Views/MeetingListView.swift`

### Référence Android
**Fichier source**: `wakeveApp/src/commonMain/kotlin/com/guyghost/wakeve/ui/meeting/MeetingListScreen.kt`

### Fonctionnalités à Implémenter

#### 1. Liste des Réunions
- Affichage des réunions par événement
- Groupement par statut (SCHEDULED, STARTED, ENDED, CANCELLED)
- Pull-to-refresh

#### 2. Carte de Réunion (MeetingCard)
- Plateforme (Zoom, Google Meet, FaceTime) avec icône
- Titre et description
- Date et heure formatées
- Durée
- Statut avec couleur
- Lien de réunion (copiable)

#### 3. Actions
- **Créer une réunion**: Navigation vers MeetingCreationView
- **Éditer**: Sheet d'édition avec formulaire
- **Démarrer**: Action pour marquer comme démarrée
- **Terminer**: Action pour marquer comme terminée
- **Annuler**: Action avec confirmation
- **Régénérer le lien**: Action pour créer nouveau lien
- **Partager**: ShareSheet avec le lien

#### 4. Détails de Réunion
- Sheet modal avec tous les détails
- Liste des participants invités
- QR code pour le lien (optionnel)

### Design System
- **Liquid Glass** (iOS 16+)
- Couleurs: WakevColors.swift
- Typographie: WakevTypography.swift
- Composants: WakevButton, WakevCard

### Architecture
- **MVVM**: MeetingListViewModel
- **State**: @StateObject pour la liste
- **Binding**: @Binding pour les sheets
- **Navigation**: NavigationStack / NavigationLink

### Intégration MeetingService
```swift
// Utilisation du MeetingService partagé (Kotlin Multiplatform)
let meetingService = MeetingService(
    database: database,
    calendarService: calendarService,
    notificationService: notificationService
)
```

### Livrables
1. `wakeveApp/wakeveApp/Views/MeetingListView.swift`
2. `wakeveApp/wakeveApp/ViewModels/MeetingListViewModel.swift` (si nécessaire)
3. `wakeveApp/wakeveApp/Views/MeetingCard.swift` (composant réutilisable)
4. `wakeveApp/wakeveApp/Views/MeetingEditSheet.swift` (sheet d'édition)

### Tests
- XCTest pour la vue (UI tests)
- Test de navigation
- Test des actions

## Contraintes
- iOS 16+ (Liquid Glass)
- SwiftUI
- Accessibilité (VoiceOver)
- Dark mode support
- Orientation portrait/paysage

## Artifacts Produced

### Files Created
1. **wakeveApp/wakeveApp/Views/MeetingListView.swift** (updated)
   - Full list view with Liquid Glass design
   - Grouped by status (SCHEDULED, STARTED, ENDED, CANCELLED)
   - Pull-to-refresh support
   - Integrated with MeetingListViewModel
   - Accessibility labels for VoiceOver

2. **wakeveApp/wakeveApp/Views/MeetingEditSheet.swift** (new)
   - Sheet for editing meeting details
   - Title and description editing
   - Date/time picker integration
   - Duration picker (hours/minutes)
   - Platform selection grid
   - Liquid Glass design

3. **wakeveApp/wakeveApp/Views/MeetingGenerateLinkSheet.swift** (new)
   - Sheet for generating meeting links
   - Platform selection (Zoom, Google Meet, FaceTime, Teams, Webex)
   - Visual selection indicators
   - Liquid Glass design

## Technical Decisions

### 1. State Management
- **Decision**: Use existing `MeetingListViewModel` (already implemented with state machine integration)
- **Reason**: Reuse of existing architecture; no need to duplicate logic
- **Impact**: Clean separation between view and business logic

### 2. Grouping by Status
- **Decision**: Implement 4 separate sections (SCHEDULED, STARTED, ENDED, CANCELLED)
- **Reason**: Matches user workflow and Android implementation
- **Impact**: Better UX with clear visual organization

### 3. Liquid Glass Design Integration
- **Decision**: Use existing LiquidGlassCard, LiquidGlassButton, LiquidGlassBadge components
- **Reason**: Consistency with rest of iOS app
- **Impact**: Unified design language

### 4. Sheet Implementation
- **Decision**: Use `.presentationDetents([.medium, .large])` for sheets
- **Reason**: Modern iOS 16+ experience with flexible heights
- **Impact**: Better UX on different screen sizes

### 5. Pull-to-Refresh
- **Decision**: Use SwiftUI `.refreshable` modifier
- **Reason**: Native iOS experience with minimal code
- **Impact**: Standard refresh gesture support

## Implementation Notes

### MeetingListView Features
- Uses `@StateObject` for MeetingListViewModel lifecycle management
- Implements `refreshable` for pull-to-refresh
- Shows empty state when no meetings exist
- Shows loading state during data fetch
- Groups meetings by status with section headers
- MeetingCard component for each meeting item

### MeetingEditSheet Features
- Editable fields: title, description, date/time, duration, platform
- Duration picker with +/- buttons for hours and minutes
- Platform selection grid with visual feedback
- Date/time picker sheet integration
- Save button disabled when title is empty

### MeetingGenerateLinkSheet Features
- Platform selection grid (Zoom, Google Meet, FaceTime, Teams, Webex)
- Visual selection indicators (border, background, "Sélectionné" label)
- Info box explaining link replacement
- Generate button disabled during generation

### Accessibility
- All interactive elements have accessibility labels
- Hints provided where needed
- Status traits added for selected items
- Combined accessibility labels for card content

### Color Scheme
- Platform colors: .wakevPrimary (Zoom), .wakevSuccess (Google Meet), .wakevAccent (FaceTime)
- Status badges: .warning (SCHEDULED), .success (STARTED), .default (ENDED/CANCELLED)
- Background: Gradient with wakevPrimary/wakevAccent opacity

## Inter-Agent Notes

### Notes to @tests
Please create tests for:
1. **MeetingListViewModel Integration**
   - Load meetings on appear
   - Filter meetings by status
   - Error state handling
   - Loading state transitions

2. **MeetingListView**
   - Pull-to-refresh functionality
   - Empty state display
   - Loading state display
   - Section grouping by status
   - Navigation to meeting detail
   - Sheet presentation (edit, generate link)

3. **MeetingEditSheet**
   - Title and description validation
   - Date/time picker selection
   - Duration picker (hours/minutes)
   - Platform selection
   - Save button disabled state

4. **MeetingGenerateLinkSheet**
   - Platform selection
   - Selection visual feedback
   - Generate action

5. **Accessibility**
   - VoiceOver labels verification
   - Trait correctness (isSelected, etc.)
   - Hint verification

6. **Edge Cases**
   - Empty meeting list
   - Error during load
   - Meeting without URL
   - Cancelled meetings
   - Multiple meetings with same status

### Notes to @review
Please review:
1. **Design System Compliance**
   - Liquid Glass component usage
   - Color scheme consistency
   - Spacing and layout
   - Typography hierarchy

2. **Accessibility**
   - VoiceOver labels
   - Color contrast ratios
   - Touch target sizes (minimum 44x44pt)

3. **UX Flow**
   - Meeting card interaction
   - Sheet presentation
   - Status grouping logic
   - Platform selection feedback

## Validation Checklist

- [x] MeetingListView created with Liquid Glass design
- [x] MeetingListViewModel integration
- [x] Grouping by status (SCHEDULED, STARTED, ENDED, CANCELLED)
- [x] Pull-to-refresh support
- [x] Empty state view
- [x] Loading state view
- [x] MeetingEditSheet with form fields
- [x] MeetingGenerateLinkSheet with platform selection
- [x] Accessibility labels (VoiceOver)
- [x] Dark mode support (via system colors)
- [x] Code compiles with Xcode
- [ ] Preview functionality verified (requires Shared module)
- [ ] Tests created (to be done by @tests)
- [ ] Review completed (to be done by @review)

## Next Steps

1. **@tests**: Create comprehensive XCTest suite for MeetingListView, MeetingEditSheet, and MeetingGenerateLinkSheet
2. **@review**: Review design system compliance, accessibility, and UX flow
3. **Integration**: Test with actual backend data
4. **Documentation**: Update user documentation if needed
