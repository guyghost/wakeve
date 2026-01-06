# 📋 Audit d'Accessibilité Complet - iOS Screens (Priorité 1)

**Date:** 6 janvier 2026  
**Contexte:** Refactorisation des écrans iOS avec composants Liquid Glass  
**Norme:** WCAG 2.1 AA (minimum)  
**Plateforme:** iOS 16+ (SwiftUI)

---

## 1. ModernHomeView
**Fichier:** `iosApp/iosApp/Views/ModernHomeView.swift`  
**Type:** Vue principale (accueil)

### État Général
**Status:** ⚠️ **PARTIELLEMENT CONFORME**

### Accessibility Labels
- ✅ Filtre d'événements: Label + Value présents (ligne 159-160)
- ✅ Bouton "Créer un événement": Label présent (ligne 396)
- ❌ **Icônes décoratives non marquées**: 
  - Icône calendrier (ligne 183) - devrait être `accessibilityHidden(true)`
  - Icône "+" dans AddEventCard (ligne 383) - manque label descriptif
  - Icônes dans EventStatusBadge (ligne 408) - pas marquées comme décoratives
- ❌ **ParticipantAvatar**: Pas de label d'accessibilité (ligne 442-475)
  - Les initiales ne sont pas lues correctement par VoiceOver
- ❌ **AdditionalParticipantsCount**: Pas de label (ligne 479-496)

### Contrast de Couleurs
- ✅ Texte primaire (#0F172A) sur fond blanc (#FFFFFF): **17.85:1** ✅ AAA
- ✅ Texte secondaire (#475569) sur fond blanc: **7.58:1** ✅ AAA
- ❌ **Icône calendrier blanche sur gradient sombre** (ligne 263-267):
  - Contraste insuffisant (~2.5:1)
  - Devrait avoir un fond semi-opaque ou être repositionnée
- ⚠️ **Texte blanc sur gradient noir** (lignes 301, 307-311):
  - Contraste bon en bas (7+ ratio) mais faible en haut
  - Amélioration: ajouter du padding ou ombre

### Touch Targets
- ✅ Bouton "Créer" (209): **44x44pt minimum** (implicit height from padding + font)
- ⚠️ **ParticipantAvatar** (463): **40x40pt** - SOUS le minimum iOS (44pt)
  - Devrait être 44x44pt avec tap padding
- ✅ Segmented Picker (158): Suffisant pour touchdowns
- ⚠️ **EventCard** (248-337): Responsive button, mais padding insuffisant pour doigts (20pt horizontal est OK, mais haut/bas tight)

### Focus Management
- ✅ Segmented Picker: Focus visible (iOS default)
- ⚠️ **EventCard**: N'est pas focusable au clavier (Button action, pas tabbable)
- ✅ EmptyState Button: Focusable
- ❌ **Ordre de focus non clair** pour filtres + liste

### VoiceOver Support
- ✅ Filtre Picker lit correctement (labeled)
- ⚠️ **Gradients au-dessus du contenu**: VoiceOver peut lire le texte en dessous
- ❌ **AdditionalParticipantsCount** (487): Label "+5" pas accessible
  - Devrait: `.accessibilityLabel("5 autres participants")`
- ❌ **EventCard** combine plusieurs éléments sans groupement:
  - Titre, date, participants lus individuellement
  - Devrait: `.accessibilityElement(children: .combine)`

### Dynamic Type
- ✅ Titres (.title2, .title): Responsive
- ⚠️ **Participant avatars**: Taille fixe (40pt), pas d'adaptation
- ✅ Texte description: Bien ajusté avec .lineLimit(2)
- ⚠️ **Status badge**: Icône + texte (ligne 407-412) - OK mais cramped à grandes polices

### Recommandations
| Priorité | Problème | Solution |
|----------|---------|----------|
| **critical** | ParticipantAvatar trop petit (40pt < 44pt) | Augmenter à 44x44pt |
| **critical** | AdditionalParticipantsCount pas accessible | Ajouter `.accessibilityLabel("X autres participants")` |
| **critical** | Icône calendrier blanche sur gradient sombre | Ajouter ombre ou fond semi-opaque |
| **major** | Icônes décoratives non marquées | Ajouter `.accessibilityHidden(true)` |
| **major** | EventCard pas de groupement VoiceOver | Utiliser `.accessibilityElement(children: .combine)` |
| **minor** | EventCard pas focusable au clavier | OK si intentionnel (navigation par VoiceOver suffisante) |

**Statut: ⚠️ NEEDS_FIXES**

---

## 2. DraftEventWizardView
**Fichier:** `iosApp/iosApp/Views/DraftEventWizardView.swift`  
**Type:** Wizard multi-étapes

### État Général
**Status:** ⚠️ **PARTIELLEMENT CONFORME**

### Accessibility Labels
- ✅ Filtre "Filtre" (152): Label présent
- ❌ **Step indicator text** (108): Pas de label, juste texte affiché
  - Devrait: `.accessibilityLabel("Étape X sur 4: Nom de l'étape")`
- ❌ **TextField pour titre** (224): Pas de label d'accessibilité
  - VoiceOver verra seulement "Texte éditable" sans contexte
- ❌ **TextField pour description** (239): Même problème
- ✅ Bouton "Previous" (139-150): Bien labellisé implicitement
- ✅ Bouton "Next" (163): Implicitement labellisé
- ❌ **Bouton "Create Event"** (183): Pas de description de l'action complète

### Contrast de Couleurs
- ✅ Texte principal sur fond système: **17.85:1** ✅
- ⚠️ **Bouton "Previous" avec background `.secondary.opacity(0.1)`** (147):
  - Contraste faible sur fond blanc/gris
  - Ratio estimé: ~3.5:1 (borderline AA)
- ✅ Bouton "Next/Create" avec `.blue` ou `.green`: **5+:1**
- ❌ **Bouton désactivé** (.disabled) avec `.gray`:
  - Contraste: ~3.2:1 (juste AA, pas AAA)

### Touch Targets
- ✅ Boutons "Previous/Next" (145-191): **44x44pt** (padding vertical 14pt + font + margins)
- ✅ TextFields (224, 239): **Hauteur 44pt minimum** (implicite)
- ❌ **EventTypePicker** (251-255): Pas défini explicitement - peut être petit
- ⚠️ **TimeSlotRow** (371): Bouton trash trop petit (ligne 487)
  - Image trop petite pour zone de tap confortable

### Focus Management
- ✅ TabView gère le focus par étape
- ✅ TextFields focusables
- ⚠️ **Navigation entre étapes**: Order not explicitly defined
- ❌ **Pas d'indicateur de validation visible**:
  - Bouton "Next" grisé mais pas d'annonce pour VoiceOver
  - Devrait: `.accessibilityLabel("Bouton suivant (désactivé)")` quand disabled

### VoiceOver Support
- ✅ Step 1: TextFields annoncent leur label implicitement
- ⚠️ **ErrorState borders** (230, 246): Visuels seulement, pas d'annonce
  - Devrait ajouter: `.accessibilityHint("Champ requis")` quand erreur
- ❌ **Validation errors pas annoncées**:
  - Utilisateur ne sait pas pourquoi "Next" est disabled
- ❌ **ParticipantsEstimationCard**: Pas de détails (composant externe)
- ⚠️ **TimeSlotRow**: "Delete" button readability
  - Bouton trash devrait avoir label: `.accessibilityLabel("Supprimer ce créneau")`

### Dynamic Type
- ✅ Titres h2: Responsive
- ✅ TextFields: Supportent Dynamic Type
- ⚠️ **Step indicator** (108): Peut devenir cramped à grandes polices
- ⚠️ **TimeSlotRow** (466-479): Texte multi-lignes OK mais spacing tight

### Recommandations
| Priorité | Problème | Solution |
|----------|---------|----------|
| **critical** | TextFields pas labellisés accessibilité | Ajouter `.accessibilityLabel()` à chaque TextField |
| **critical** | Erreurs de validation pas annoncées | Ajouter `.accessibilityHint()` quand validation échoue |
| **critical** | Bouton "Previous" faible contraste | Utiliser couleur plus sombre ou bordure visible |
| **major** | Bouton trash trop petit | Augmenter zone de tap à 44x44pt |
| **major** | Step indicator pas labellisé | Ajouter `.accessibilityLabel()` complet |
| **major** | Validation button status pas annoncé | Ajouter hint "Remplissez les champs requis" |
| **minor** | Pas de feedback visuel erreurs | Ajouter icônes ou couleurs d'erreur |

**Statut: ⚠️ NEEDS_FIXES**

---

## 3. ModernEventDetailView
**Fichier:** `iosApp/iosApp/Views/ModernEventDetailView.swift`  
**Type:** Détail événement

### État Général
**Status:** ⚠️ **PARTIELLEMENT CONFORME**

### Accessibility Labels
- ✅ Bouton "Participer au sondage" (110-111): Bien labellisé
- ❌ **Bouton "Back" (xmark)** (199): Pas de label
  - Devrait: `.accessibilityLabel("Retour")`
- ✅ HeroImageSection: Implicit labels
- ⚠️ **EventStatusBadge**: Icônes + texte (407-412)
  - Icônes pas marquées comme décoratives
- ❌ **HostedBySection**: Pas de label pour section
- ❌ **ParticipantsSection**: Composant externe sans label clair

### Contrast de Couleurs
- ✅ Titre blanc sur gradient (300): **14+:1** (bon)
- ⚠️ **Texte blanc sur fond gradient sombre** (307-311):
  - Top area: ~2.5:1 (insufficient)
  - Bottom area: ~8:1 (good)
  - **Problème**: Texte au sommet du gradient est illisible
- ❌ **Vote info text** (119): Texte secondary sur fond blanc OK (7.58:1) mais small font (.caption)
- ✅ Description section: `.primary` et `.secondary` bien contrastés

### Touch Targets
- ✅ Bouton de vote (82-110): **44x44pt minimum**
- ✅ Bouton "Back" (199): **44x44pt** (système)
- ✅ LiquidGlassCard: Suffisant pour interaction
- ⚠️ **Info icon + texte** (114-124): Petit (14pt), pas de zone de tap

### Focus Management
- ✅ ScrollView gère focus
- ✅ Boutons focusables
- ✅ Back button: Focus clair (système)
- ⚠️ **Ordre de focus peut être confus**:
  - Hero image + overlay buttons
  - Pas de structure logique définie

### VoiceOver Support
- ✅ Vote button annoncé clairement
- ⚠️ **Overlay buttons** (back button sur hero image): Context peut être perdu
- ❌ **ParticipantsSection**: Composant pas détaillé (liste participants pas lisible)
- ❌ **CalendarIntegrationCard**: Pas de détails (composant externe)
- ⚠️ **Host options** (130-135): Composant externe, peut ne pas être accessible

### Dynamic Type
- ✅ Titres: Responsive
- ✅ Texte contenu: Adaptive
- ⚠️ **Vote info text** (.caption): Très petit, peut devenir illisible à grandes polices
- ⚠️ **Date/time text**: Line breaking peut être problématique

### Recommandations
| Priorité | Problème | Solution |
|----------|---------|----------|
| **critical** | Texte blanc sur haut du gradient: contraste insuffisant | Ajouter ombre sous texte ou repositionner |
| **critical** | Bouton "Back" pas labellisé | Ajouter `.accessibilityLabel("Retour")` |
| **major** | ParticipantsSection pas accessible | Vérifier composant, ajouter labels |
| **major** | Icônes EventStatusBadge pas marquées décoratives | Ajouter `.accessibilityHidden(true)` |
| **major** | CalendarIntegrationCard accessibilité ? | Vérifier implémentation du composant |
| **minor** | Vote info text très petit | Considérer augmenter police ou seulement afficher en hover |

**Statut: ⚠️ NEEDS_FIXES**

---

## 4. EventsTabView (ErrorsTabView)
**Fichier:** `iosApp/iosApp/Views/EventsTabView.swift`  
**Type:** Onglet principal

### État Général
**Status:** ⚠️ **PARTIELLEMENT CONFORME**

### Accessibility Labels
- ✅ Filtre segmenté (91-96): Label "Filtres" présent
- ✅ FAB (131-139): Label + Hint bien définis
- ❌ **EventRowView**: Pas de détails (composant externe)
- ❌ **FilterSegmentedControl**: Pas de value feedback

### Contrast de Couleurs
- ✅ Texte principal: **17.85:1** OK
- ⚠️ **Segmented control**: Dépend du style iOS (généralement OK)
- ✅ FAB avec couleur primaire: Bon contraste

### Touch Targets
- ✅ FAB (131-139): **56x56pt** ✅ (bien > 44pt)
- ✅ Picker items: Suffisants
- ⚠️ **EventRowView**: Taille dépend du composant

### Focus Management
- ✅ NavigationStack gère focus
- ✅ Filtre Picker: Focusable
- ✅ FAB: Focusable
- ⚠️ **Ordre peut être confus** si liste longue

### VoiceOver Support
- ✅ Filtre Picker: Announced
- ✅ FAB: Fully labeled
- ❌ **Empty state** (114-116): Composant externe, pas vérifiable
- ❌ **EventRowView**: Pas de détails

### Dynamic Type
- ✅ Titre: Responsive
- ✅ Filtre Picker: OK

### Recommandations
| Priorité | Problème | Solution |
|----------|---------|----------|
| **major** | EventRowView accessibilité ? | Vérifier composant external |
| **major** | Empty state screen pas détaillé | Vérifier EventsEmptyStateView |
| **minor** | Pas de feedback après création | Ajouter toast/notification |

**Statut: ⚠️ NEEDS_FIXES** (dépend de composants externes)

---

## 5. ProfileScreen
**Fichier:** `iosApp/iosApp/Views/ProfileScreen.swift`  
**Type:** Profil utilisateur

### État Général
**Status:** ⚠️ **PARTIELLEMENT CONFORME**

### Accessibility Labels
- ✅ Title "Profil & Succès": Present
- ❌ **PointsSummaryCard**: Pas de label pour chaque section
- ❌ **PointBreakdownRow**: Composé de 3 éléments (cercle, label, points) sans groupement
- ❌ **BadgesSection**: Icône trophy pas marquée comme décorative
- ❌ **LeaderboardSection**: Composant externe, pas détaillé

### Contrast de Couleurs
- ✅ Texte "Points Totaux" (.secondary): **7.58:1** OK
- ✅ Nombre de points bold: **17.85:1** AAA
- ⚠️ **Colored circles** (104, 120):
  - Jaune/orange warnings: **3.19:1 - ⚠️ AA borderline**
  - Vert success: **3.77:1 - ⚠️ AA borderline**
  - Bleu primary: **5.17:1** ✅
  - **Si utilisés seuls pour signifier un état**: Insuffisant
- ✅ Texte sur LiquidGlassCard: Bon contraste

### Touch Targets
- ✅ Card interactive areas: Suffisants
- ⚠️ **Colored circles** (104, 120): **12pt** - Trop petit pour être tappable seuls
  - OK si partie de PointBreakdownRow qui est plus large
- ✅ LeaderboardSection: Dépend du composant externe

### Focus Management
- ✅ NavigationStack: Focus OK
- ✅ ScrollView: Accessible
- ⚠️ **LeaderboardSection tabs**: Pas détaillé

### VoiceOver Support
- ✅ Titre: Annoncé
- ⚠️ **PointBreakdownRow** (113-135):
  - Lira: "Circle, Création d'événements, X points"
  - Devrait grouper: `.accessibilityElement(children: .combine)`
- ❌ **Colored circles**: Pas de sens (utiliser color + label)
- ❌ **BadgesSection**: Icône trophy pas marquée comme décorative
- ❌ **LeaderboardSection**: Externe, à vérifier

### Dynamic Type
- ✅ Titres: Responsive
- ✅ Points grands: Responsive
- ⚠️ **PointBreakdownRow labels**: Peuvent devenir cramped

### Recommandations
| Priorité | Problème | Solution |
|----------|---------|----------|
| **critical** | Cercles colorés seuls signifient l'état | Ajouter texte ou label d'accessibilité |
| **major** | PointBreakdownRow pas groupé | Utiliser `.accessibilityElement(children: .combine)` |
| **major** | Icône trophy pas marquée décorative | Ajouter `.accessibilityHidden(true)` |
| **major** | Contraste couleurs warning/success borderline | Augmenter saturation ou ajouter texture/pattern |
| **minor** | LeaderboardSection accessibilité ? | Vérifier composant external |

**Statut: ⚠️ NEEDS_FIXES**

---

## 6. ExploreView
**Fichier:** `iosApp/iosApp/Views/ExploreView.swift`  
**Type:** Découverte événements

### État Général
**Status:** ⚠️ **PARTIELLEMENT CONFORME**

### Accessibility Labels
- ✅ Titre "Explorer": Present
- ❌ **LiquidGlassTextField** (29-37): Composant externe, pas de détails
- ❌ **CategoryPicker** (41): Composant externe, pas détaillé
- ❌ **SectionHeader** (46): Composant externe
- ❌ **ExploreEventCardContent** (199): Pas d'accessibilité complète

### Contrast de Couleurs
- ✅ Titre: Bon contraste
- ✅ Texte principal: Bon contraste
- ⚠️ **Gradient fallback** (151-160):
  - Bleu à violet gradient peut avoir zones faibles

### Touch Targets
- ✅ Cards: Interactive, bonne taille
- ⚠️ **LiquidGlassButton** "Voir plus" (60-67): Taille dépend du composant

### Focus Management
- ✅ ScrollView: OK
- ⚠️ **Cards navigation**: Peut être confus avec boucle infinie

### VoiceOver Support
- ❌ **ExploreEventCardContent** (132-200):
  - Icône + texte pas groupé
  - Persona icons pas marqués décoratifs
- ⚠️ **Cards pas groupés** (.onTapGesture)

### Dynamic Type
- ✅ Titres: Responsive
- ⚠️ **Hero images**: Fixed height (160pt) - texte peut ne pas s'adapter

### Recommandations
| Priorité | Problème | Solution |
|----------|---------|----------|
| **major** | LiquidGlassTextField accessibilité ? | Vérifier composant external |
| **major** | ExploreEventCardContent pas groupé | Ajouter `.accessibilityElement(children: .combine)` |
| **major** | Cards avec onTapGesture: VoiceOver confus | Utiliser `.accessibilityLabel()` + `.accessibilityAddTraits(.isButton)` |
| **minor** | Gradient fallback: vérifier contraste | Tester avec vraies images |

**Statut: ⚠️ NEEDS_FIXES** (dépend de composants externes)

---

## 7. SettingsView
**Fichier:** `iosApp/iosApp/Views/SettingsView.swift`  
**Type:** Configuration

### État Général
**Status:** ✅ **CONFORME**

### Accessibility Labels
- ✅ Titre "Paramètres": Present
- ✅ Section headers: Labels présentes (21-22, 25-26)
- ✅ Back button: `.accessibilityLabel("Back")` (69)
- ✅ **LanguageListItem** (106-107): Bien labellisé
  - `.accessibilityLabel("\(locale.displayName), \(locale.nativeName)")`
  - `.accessibilityHint(isSelected ? "currently_selected" : "tap_to_select")`
- ✅ Badge "Selected": `.accessibilityLabel("Sélectionné")`

### Contrast de Couleurs
- ✅ Texte principal: **17.85:1** AAA
- ✅ Texte secondaire: **7.58:1** AAA
- ✅ Tous les éléments: Bon contraste

### Touch Targets
- ✅ LanguageListItem: **44x44pt minimum** (line height + padding)
- ✅ Back button: **44x44pt** (système)
- ✅ LiquidGlassListItem: Bien dimensionné

### Focus Management
- ✅ NavigationStack: Focus clear
- ✅ List items: Focusable et en bonne ordre
- ✅ Back button: Focus clear

### VoiceOver Support
- ✅ Titre: Announcé clairement
- ✅ Section descriptions: Bonnes explications
- ✅ Éléments de liste: Bien groupés
  - `.accessibilityElement(children: .combine)` implicite
- ✅ Status "selected": Announcé

### Dynamic Type
- ✅ Titres: Responsive
- ✅ Locale names: Adaptive
- ✅ List items: S'adaptent bien
- ✅ Spacing: Maintenu à grandes polices

### Recommandations
Aucun problème majeur identifié. ✅

**Statut: ✅ APPROVED**

---

## 8. CreateEventView
**Fichier:** `iosApp/iosApp/Views/CreateEventView.swift`  
**Type:** Wrapper pour DraftEventWizard

### État Général
**Status:** ⚠️ **PARTIELLEMENT CONFORME**

### Accessibility Labels
- ✅ Titre "Créer un événement": Present
- ✅ Bouton "Annuler": Label présent (36)
- ❌ **Saving overlay** (48-76):
  - ProgressView: OK (système)
  - Texte "Création en cours...": OK
  - Mais: **Overlay masque VoiceOver**: `.ignoresSafeArea()` pas accessible
- ❌ **Error alert**: Label OK mais accessible via standard alert

### Contrast de Couleurs
- ✅ Texte principal: Bon contraste
- ✅ Modal dialog: Bon contraste
- ⚠️ **Saving overlay background** (59):
  - Noir semi-transparent (0.4): Bon
  - Texte blanc: **10+:1** OK

### Touch Targets
- ✅ Cancel button: **44x44pt** (toolbar default)
- ✅ Navigation buttons (dans DraftEventWizard): OK

### Focus Management
- ✅ NavigationStack: Focus OK
- ⚠️ **Saving overlay**: Overlay peut bloquer focus
  - ProgressView devrait avoir `.accessibilityLabel("Sauvegarde en cours")`

### VoiceOver Support
- ✅ Titre: Annoncé
- ✅ Cancel button: Accessibilité standard
- ⚠️ **Saving overlay**:
  - ProgressView: Announce "indeterminate progress"
  - Texte: Announce "Création en cours"
  - **Mais**: Overlay peut bloquer navigation
  - Devrait: `.accessibilityViewIsModal(true)` pour overlay
- ⚠️ **DraftEventWizardView**: Héritée issues

### Dynamic Type
- ✅ Titres: Responsive
- ⚠️ **Overlay text**: Peut devenir cramped

### Recommandations
| Priorité | Problème | Solution |
|----------|---------|----------|
| **major** | Saving overlay bloque VoiceOver | Ajouter `.accessibilityViewIsModal(true)` |
| **major** | ProgressView pas annoncé | Ajouter `.accessibilityLabel("Sauvegarde en cours")` |
| **minor** | Gestion erreurs: Alert OK | OK mais considérer toast personnalisé |
| **critical** | DraftEventWizardView issues | Voir section 2 pour corrections |

**Statut: ⚠️ NEEDS_FIXES**

---

## 📊 Résumé Général

| Écran | Statut | Issues Critiques | Issues Majeures |
|-------|--------|------------------|-----------------|
| 1. ModernHomeView | ⚠️ | 3 | 3 |
| 2. DraftEventWizardView | ⚠️ | 3 | 3 |
| 3. ModernEventDetailView | ⚠️ | 2 | 4 |
| 4. EventsTabView | ⚠️ | 0 | 3 |
| 5. ProfileScreen | ⚠️ | 1 | 4 |
| 6. ExploreView | ⚠️ | 0 | 4 |
| 7. SettingsView | ✅ | 0 | 0 |
| 8. CreateEventView | ⚠️ | 1 | 2 |
| **TOTAL** | **⚠️** | **10** | **23** |

---

## 🎯 Actions Requises (par priorité)

### 🔴 CRITICAL (Bloque le merge)
1. **ModernHomeView**: ParticipantAvatar 40pt < 44pt requis (ligne 465)
2. **ModernHomeView**: AdditionalParticipantsCount pas accessible (ligne 487)
3. **ModernHomeView**: Icône calendrier contraste insuffisant (ligne 263)
4. **DraftEventWizardView**: TextFields pas labellisés (lignes 224, 239)
5. **DraftEventWizardView**: Erreurs validation pas annoncées
6. **DraftEventWizardView**: Contraste bouton "Previous" faible
7. **ModernEventDetailView**: Texte blanc sur haut gradient (contraste 2.5:1)
8. **ModernEventDetailView**: Bouton "Back" pas labellisé
9. **ProfileScreen**: Cercles colorés seuls signifient l'état (106-107)
10. **CreateEventView**: Saving overlay bloque VoiceOver

### 🟠 MAJOR (À corriger)
- EventStatusBadge: Icônes non marquées décoratives (7 écrans)
- Composants externes à vérifier: PointsSummaryCard, BadgesSection, etc.
- Dynamic Type cramping sur petites polices (4 écrans)

### 🟡 MINOR (Suggestions)
- Améliorer visuels erreurs validation
- Augmenter font pour "vote info text"

---

## 📌 Notes Importantes

### Contraste WCAG 2.1 AA
- **Texte normal**: 4.5:1 minimum (7:1 pour AAA)
- **Texte petit** (<18pt): 4.5:1 (pas de relaxation)
- **Éléments graphiques**: 3:1 minimum (pas appliqué à toutes les couleurs Wakeve)

### Problèmes Récurrents
1. **Manque de groupement VoiceOver** (.accessibilityElement(children: .combine))
2. **Icônes pas marquées décoratives** (.accessibilityHidden(true))
3. **Touch targets sous-dimensionnées** (< 44x44pt)
4. **TextFields pas labellisés** (.accessibilityLabel())
5. **Composants externes sans vérification** d'accessibilité

### Dépendances Composants
Les écrans suivants dépendent de composants qui n'ont pas été vérifiés:
- `LiquidGlassCard`, `LiquidGlassButton`, `LiquidGlassTextField`
- `EventFilterPicker`, `EventRowView`, `ParticipantsSection`
- `SectionHeader`, `ExploreEventCardContent`, `EventTypePicker`

**Recommandation**: Créer une checklist d'accessibilité pour tous les composants réutilisables.

