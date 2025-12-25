# Design System Wakeve

Ce document définit le design system unifié pour toutes les plateformes de l'application Wakeve. Il garantit une cohérence visuelle entre Android (Material You + Jetpack Compose), iOS (Liquid Glass + SwiftUI) et Web (TailwindCSS), tout en respectant les guidelines natives de chaque plateforme.

## Principes Directeurs

### 1. Cohérence Cross-Platform
- **Code couleur unifié** : Mêmes couleurs sur toutes les plateformes
- **Hiérarchie visuelle identique** : Structure et espacements cohérents
- **Comportements adaptés** : Respect des conventions natives de chaque OS

### 2. Design Natif
- **Android** : Material You avec Jetpack Compose
- **iOS** : Liquid Glass avec SwiftUI (iOS 15+)
- **Web** : TailwindCSS (layout à définir ultérieurement)

### 3. Accessibilité First
- Contraste WCAG AA minimum
- Support du mode sombre automatique
- Tailles de texte adaptatives
- Navigation clavier complète (web)

---

## Palette de Couleurs Unifiée

### Couleurs Primaires

```
Primary (Bleu)
- Light: #4A90E2
- Main:  #2563EB
- Dark:  #1E40AF

Accent (Violet)
- Light: #8B5CF6
- Main:  #7C3AED
- Dark:  #6D28D9

Success (Vert)
- Light: #10B981
- Main:  #059669
- Dark:  #047857

Warning (Jaune/Orange)
- Light: #F59E0B
- Main:  #D97706
- Dark:  #B45309

Error (Rouge)
- Light: #EF4444
- Main:  #DC2626
- Dark:  #B91C1C
```

### Couleurs Neutres

```
Background
- Light Mode: #FFFFFF
- Dark Mode:  #0F172A

Surface
- Light Mode: #F8FAFC
- Dark Mode:  #1E293B

Surface Variant
- Light Mode: #F1F5F9
- Dark Mode:  #334155

Border
- Light Mode: #E2E8F0
- Dark Mode:  #475569

Text Primary
- Light Mode: #0F172A
- Dark Mode:  #F1F5F9

Text Secondary
- Light Mode: #475569
- Dark Mode:  #94A3B8

Text Tertiary
- Light Mode: #64748B
- Dark Mode:  #64748B
```

### Couleurs de Statut d'Événement

```
Draft (Brouillon)
- Color: #64748B (Gray)

Polling (Sondage)
- Color: #2563EB (Primary Blue)

Comparing (Comparaison)
- Color: #8B5CF6 (Accent Purple)

Confirmed (Confirmé)
- Color: #059669 (Success Green)

Organizing (Organisation)
- Color: #D97706 (Warning Orange)

Finalized (Finalisé)
- Color: #7C3AED (Accent Dark Purple)
```

---

## Typographie

### Échelle de Police

```
Display
- Size: 57px / 3.5rem
- Weight: 700 (Bold)
- Line Height: 64px / 1.12
- Usage: Hero sections, splash screens

Headline Large
- Size: 32px / 2rem
- Weight: 700 (Bold)
- Line Height: 40px / 1.25
- Usage: Page titles

Headline Medium
- Size: 28px / 1.75rem
- Weight: 600 (SemiBold)
- Line Height: 36px / 1.29
- Usage: Section titles

Headline Small
- Size: 24px / 1.5rem
- Weight: 600 (SemiBold)
- Line Height: 32px / 1.33
- Usage: Card titles

Title Large
- Size: 22px / 1.375rem
- Weight: 500 (Medium)
- Line Height: 28px / 1.27
- Usage: List headers

Title Medium
- Size: 16px / 1rem
- Weight: 500 (Medium)
- Line Height: 24px / 1.5
- Usage: List items, labels

Title Small
- Size: 14px / 0.875rem
- Weight: 500 (Medium)
- Line Height: 20px / 1.43
- Usage: Captions, small labels

Body Large
- Size: 16px / 1rem
- Weight: 400 (Regular)
- Line Height: 24px / 1.5
- Usage: Main text content

Body Medium
- Size: 14px / 0.875rem
- Weight: 400 (Regular)
- Line Height: 20px / 1.43
- Usage: Secondary text

Body Small
- Size: 12px / 0.75rem
- Weight: 400 (Regular)
- Line Height: 16px / 1.33
- Usage: Helper text, timestamps

Label Large
- Size: 14px / 0.875rem
- Weight: 500 (Medium)
- Line Height: 20px / 1.43
- Usage: Button text

Label Medium
- Size: 12px / 0.75rem
- Weight: 500 (Medium)
- Line Height: 16px / 1.33
- Usage: Small button text

Label Small
- Size: 11px / 0.6875rem
- Weight: 500 (Medium)
- Line Height: 16px / 1.45
- Usage: Badges, tags
```

### Familles de Police

- **Android**: Roboto (system default via Material You)
- **iOS**: San Francisco (system default)
- **Web**: Inter ou system font stack

```css
/* Web font stack */
font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', 
             'Roboto', 'Oxygen', 'Ubuntu', 'Cantarell', 'Fira Sans', 
             'Droid Sans', 'Helvetica Neue', sans-serif;
```

---

## Espacements (Spacing Scale)

Échelle basée sur 4px, cohérente sur toutes les plateformes:

```
xs:   4px  / 0.25rem / 4.dp
sm:   8px  / 0.5rem  / 8.dp
md:   12px / 0.75rem / 12.dp
lg:   16px / 1rem    / 16.dp
xl:   20px / 1.25rem / 20.dp
2xl:  24px / 1.5rem  / 24.dp
3xl:  32px / 2rem    / 32.dp
4xl:  40px / 2.5rem  / 40.dp
5xl:  48px / 3rem    / 48.dp
6xl:  64px / 4rem    / 64.dp
```

### Padding Standards

```
Card Content: 16dp / 1rem
Screen Edges: 16dp / 1rem
Section Spacing: 24dp / 1.5rem
Component Spacing: 12dp / 0.75rem
```

---

## Composants Communs

### Boutons

#### Primary Button
```
Background: Primary Color
Text: White
Height: 48dp / 3rem
Corner Radius: 12dp / 0.75rem
Padding Horizontal: 24dp / 1.5rem
Font: Label Large / 500
Shadow: Small (Android/Web), None (iOS)
```

#### Secondary Button
```
Background: Surface
Border: 1px Primary Color
Text: Primary Color
Height: 48dp / 3rem
Corner Radius: 12dp / 0.75rem
Padding Horizontal: 24dp / 1.5rem
Font: Label Large / 500
```

#### Text Button
```
Background: Transparent
Text: Primary Color
Height: 40dp / 2.5rem
Padding Horizontal: 16dp / 1rem
Font: Label Large / 500
```

### Cartes (Cards)

#### Standard Card
```
Background: Surface
Corner Radius: 16dp / 1rem
Padding: 16dp / 1rem
Shadow: Medium (Android/Web)
Material: .regularMaterial (iOS)
Border: 1px Border Color (optional)
```

#### Elevated Card
```
Background: Surface
Corner Radius: 20dp / 1.25rem
Padding: 20dp / 1.25rem
Shadow: Large (Android/Web)
Material: .thickMaterial (iOS)
```

### Input Fields

#### Text Field
```
Background: Surface Variant
Height: 56dp / 3.5rem
Corner Radius: 12dp / 0.75rem
Padding: 16dp / 1rem
Border: 1px Border Color (focus: Primary)
Font: Body Large
Label: Title Small
```

#### Text Area
```
Background: Surface Variant
Min Height: 120dp / 7.5rem
Corner Radius: 12dp / 0.75rem
Padding: 16dp / 1rem
Border: 1px Border Color (focus: Primary)
Font: Body Large
```

### Badges

#### Status Badge
```
Background: Status Color (10% opacity)
Text: Status Color
Height: 24dp / 1.5rem
Padding: 4dp 12dp / 0.25rem 0.75rem
Corner Radius: 12dp / 0.75rem
Font: Label Small / 500
```

---

## Implémentation par Plateforme

### Android (Jetpack Compose + Material You)

#### Structure de Fichiers
```
composeApp/src/commonMain/kotlin/com/guyghost/wakeve/
├── theme/
│   ├── Color.kt          # Palette de couleurs
│   ├── Typography.kt     # Système typographique
│   ├── Theme.kt          # Configuration Material Theme
│   └── Spacing.kt        # Échelle d'espacements
└── components/
    ├── WakevButton.kt    # Composants boutons
    ├── WakevCard.kt      # Composants cartes
    ├── WakevTextField.kt # Composants input
    └── WakevBadge.kt     # Composants badges
```

#### Exemple Color.kt
```kotlin
object WakevColors {
    // Primary
    val PrimaryLight = Color(0xFF4A90E2)
    val Primary = Color(0xFF2563EB)
    val PrimaryDark = Color(0xFF1E40AF)
    
    // Accent
    val AccentLight = Color(0xFF8B5CF6)
    val Accent = Color(0xFF7C3AED)
    val AccentDark = Color(0xFF6D28D9)
    
    // Success
    val SuccessLight = Color(0xFF10B981)
    val Success = Color(0xFF059669)
    val SuccessDark = Color(0xFF047857)
    
    // Warning
    val WarningLight = Color(0xFFF59E0B)
    val Warning = Color(0xFFD97706)
    val WarningDark = Color(0xFFB45309)
    
    // Error
    val ErrorLight = Color(0xFFEF4444)
    val Error = Color(0xFFDC2626)
    val ErrorDark = Color(0xFFB91C1C)
}

// Dynamic Color Scheme
@Composable
fun WakevColorScheme(
    darkTheme: Boolean = isSystemInDarkTheme()
): ColorScheme {
    return if (darkTheme) {
        darkColorScheme(
            primary = WakevColors.Primary,
            secondary = WakevColors.Accent,
            tertiary = WakevColors.AccentLight,
            error = WakevColors.Error,
            background = Color(0xFF0F172A),
            surface = Color(0xFF1E293B),
            // ... autres couleurs
        )
    } else {
        lightColorScheme(
            primary = WakevColors.Primary,
            secondary = WakevColors.Accent,
            tertiary = WakevColors.AccentLight,
            error = WakevColors.Error,
            background = Color(0xFFFFFFFF),
            surface = Color(0xFFF8FAFC),
            // ... autres couleurs
        )
    }
}
```

#### Exemple Composant
```kotlin
@Composable
fun WakevCard(
    modifier: Modifier = Modifier,
    elevated: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(if (elevated) 20.dp else 16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (elevated) 8.dp else 2.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(if (elevated) 20.dp else 16.dp),
            content = content
        )
    }
}
```

### iOS (SwiftUI + Liquid Glass)

#### Structure de Fichiers
```
iosApp/iosApp/
├── Theme/
│   ├── WakevColors.swift      # Palette de couleurs
│   ├── WakevTypography.swift  # Système typographique
│   └── LiquidGlassModifier.swift  # Extensions Liquid Glass
├── Extensions/
│   └── ViewExtensions.swift   # Extensions générales
└── Components/
    ├── WakevButton.swift      # Composants boutons
    ├── WakevCard.swift        # Composants cartes
    └── WakevBadge.swift       # Composants badges
```

#### Exemple WakevColors.swift
```swift
import SwiftUI

struct WakevColors {
    // Primary
    static let primaryLight = Color(hex: "4A90E2")
    static let primary = Color(hex: "2563EB")
    static let primaryDark = Color(hex: "1E40AF")
    
    // Accent
    static let accentLight = Color(hex: "8B5CF6")
    static let accent = Color(hex: "7C3AED")
    static let accentDark = Color(hex: "6D28D9")
    
    // Success
    static let successLight = Color(hex: "10B981")
    static let success = Color(hex: "059669")
    static let successDark = Color(hex: "047857")
    
    // Warning
    static let warningLight = Color(hex: "F59E0B")
    static let warning = Color(hex: "D97706")
    static let warningDark = Color(hex: "B45309")
    
    // Error
    static let errorLight = Color(hex: "EF4444")
    static let error = Color(hex: "DC2626")
    static let errorDark = Color(hex: "B91C1C")
}

extension Color {
    init(hex: String) {
        let scanner = Scanner(string: hex)
        var rgbValue: UInt64 = 0
        scanner.scanHexInt64(&rgbValue)
        
        let r = Double((rgbValue & 0xFF0000) >> 16) / 255.0
        let g = Double((rgbValue & 0x00FF00) >> 8) / 255.0
        let b = Double(rgbValue & 0x0000FF) / 255.0
        
        self.init(red: r, green: g, blue: b)
    }
}
```

#### Extensions Liquid Glass (déjà implémentées)
Les extensions pour le Liquid Glass sont déjà présentes dans `ViewExtensions.swift`:
- `.glassCard()` - Carte standard avec matériau
- `.thinGlass()` - Carte subtile
- `.ultraThinGlass()` - Carte très subtile
- `.thickGlass()` - Carte prononcée
- `.continuousCornerRadius()` - Coins continus Apple

#### Guidelines Liquid Glass iOS
Voir le fichier détaillé: `iosApp/LIQUID_GLASS_GUIDELINES.md`

**Principes clés:**
1. Utiliser les matériaux système (`.regularMaterial`, `.thinMaterial`, etc.)
2. Toujours utiliser `.continuous` pour les coins arrondis
3. Ombres subtiles (`opacity: 0.05-0.08`)
4. Rayons de coins: 8-12px (petits), 16-20px (moyens), 24-32px (grands)

#### Exemple Composant iOS
```swift
struct WakevCard<Content: View>: View {
    let content: Content
    let elevated: Bool
    
    init(
        elevated: Bool = false,
        @ViewBuilder content: () -> Content
    ) {
        self.elevated = elevated
        self.content = content()
    }
    
    var body: some View {
        content
            .padding(elevated ? 20 : 16)
            .background(elevated ? .thickMaterial : .regularMaterial)
            .clipShape(RoundedRectangle(
                cornerRadius: elevated ? 20 : 16,
                style: .continuous
            ))
            .shadow(
                color: .black.opacity(elevated ? 0.08 : 0.05),
                radius: elevated ? 12 : 8,
                x: 0,
                y: elevated ? 6 : 4
            )
    }
}
```

### Web (TailwindCSS)

#### Configuration TailwindCSS

**Note**: Le layout web sera défini ultérieurement. Cette configuration établit la palette de couleurs et les tokens de design.

```javascript
// tailwind.config.js
module.exports = {
  content: [
    "./src/**/*.{js,jsx,ts,tsx}",
  ],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        primary: {
          light: '#4A90E2',
          DEFAULT: '#2563EB',
          dark: '#1E40AF',
        },
        accent: {
          light: '#8B5CF6',
          DEFAULT: '#7C3AED',
          dark: '#6D28D9',
        },
        success: {
          light: '#10B981',
          DEFAULT: '#059669',
          dark: '#047857',
        },
        warning: {
          light: '#F59E0B',
          DEFAULT: '#D97706',
          dark: '#B45309',
        },
        error: {
          light: '#EF4444',
          DEFAULT: '#DC2626',
          dark: '#B91C1C',
        },
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', '-apple-system', 'sans-serif'],
      },
      fontSize: {
        'display': ['57px', { lineHeight: '64px', fontWeight: '700' }],
        'headline-lg': ['32px', { lineHeight: '40px', fontWeight: '700' }],
        'headline-md': ['28px', { lineHeight: '36px', fontWeight: '600' }],
        'headline-sm': ['24px', { lineHeight: '32px', fontWeight: '600' }],
        'title-lg': ['22px', { lineHeight: '28px', fontWeight: '500' }],
        'title-md': ['16px', { lineHeight: '24px', fontWeight: '500' }],
        'title-sm': ['14px', { lineHeight: '20px', fontWeight: '500' }],
        'body-lg': ['16px', { lineHeight: '24px', fontWeight: '400' }],
        'body-md': ['14px', { lineHeight: '20px', fontWeight: '400' }],
        'body-sm': ['12px', { lineHeight: '16px', fontWeight: '400' }],
        'label-lg': ['14px', { lineHeight: '20px', fontWeight: '500' }],
        'label-md': ['12px', { lineHeight: '16px', fontWeight: '500' }],
        'label-sm': ['11px', { lineHeight: '16px', fontWeight: '500' }],
      },
      spacing: {
        'xs': '4px',
        'sm': '8px',
        'md': '12px',
        'lg': '16px',
        'xl': '20px',
        '2xl': '24px',
        '3xl': '32px',
        '4xl': '40px',
        '5xl': '48px',
        '6xl': '64px',
      },
      borderRadius: {
        'sm': '8px',
        'md': '12px',
        'lg': '16px',
        'xl': '20px',
        '2xl': '24px',
      },
    },
  },
  plugins: [],
}
```

#### Exemple Composant Web
```tsx
// WakevCard.tsx (React/TypeScript)
interface WakevCardProps {
  children: React.ReactNode;
  elevated?: boolean;
  className?: string;
}

export const WakevCard: React.FC<WakevCardProps> = ({ 
  children, 
  elevated = false,
  className = ''
}) => {
  return (
    <div className={`
      ${elevated ? 'p-5 rounded-xl shadow-lg' : 'p-4 rounded-lg shadow-md'}
      bg-white dark:bg-slate-800
      border border-slate-200 dark:border-slate-700
      ${className}
    `}>
      {children}
    </div>
  );
};
```

---

## Corner Radius Standards

### Par Type de Composant

```
Badges, Tags: 12dp / 0.75rem
Buttons: 12dp / 0.75rem
Input Fields: 12dp / 0.75rem
Standard Cards: 16dp / 1rem
Elevated Cards: 20dp / 1.25rem
Large Cards: 24dp / 1.5rem
Modals, Sheets: 28dp / 1.75rem
```

### Implémentation iOS (Continuous Corners)
Sur iOS, **toujours** utiliser `.continuous` style pour les coins arrondis:
```swift
.clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
// ou via helper
.continuousCornerRadius(16)
```

---

## Ombres (Shadows)

### Échelle d'Ombres

#### Small
```
Android: elevation = 2.dp
iOS: radius = 4, opacity = 0.05, offset = (0, 2)
Web: shadow-sm (0 1px 2px 0 rgb(0 0 0 / 0.05))
```

#### Medium
```
Android: elevation = 4.dp
iOS: radius = 8, opacity = 0.05, offset = (0, 4)
Web: shadow-md (0 4px 6px -1px rgb(0 0 0 / 0.1))
```

#### Large
```
Android: elevation = 8.dp
iOS: radius = 12, opacity = 0.08, offset = (0, 6)
Web: shadow-lg (0 10px 15px -3px rgb(0 0 0 / 0.1))
```

**Note iOS**: Les ombres sur iOS doivent être subtiles avec le Liquid Glass. Préférer les matériaux pour la profondeur.

---

## Iconographie

### Tailles d'Icônes

```
Small: 16dp / 1rem
Medium: 20dp / 1.25rem
Large: 24dp / 1.5rem
XLarge: 32dp / 2rem
```

### Bibliothèques d'Icônes

- **Android**: Material Symbols / Material Icons
- **iOS**: SF Symbols
- **Web**: Heroicons / Lucide Icons (compatible avec TailwindCSS)

### Mapping Cross-Platform
Maintenir une table de correspondance pour les icônes communes:

```
checkmark: 
  - Android: Icons.Default.Check
  - iOS: checkmark
  - Web: heroicons/check

event:
  - Android: Icons.Default.Event
  - iOS: calendar
  - Web: heroicons/calendar

person:
  - Android: Icons.Default.Person
  - iOS: person
  - Web: heroicons/user
```

---

## États Interactifs

### Boutons

```
Normal: 
  - Opacity: 100%
  
Hover (Web/Desktop):
  - Opacity: 90%
  - Scale: 1.02

Pressed:
  - Opacity: 80%
  - Scale: 0.98

Disabled:
  - Opacity: 40%
  - Cursor: not-allowed
```

### Cartes Cliquables

```
Normal:
  - Shadow: Medium
  
Hover:
  - Shadow: Large
  - Scale: 1.01
  
Pressed:
  - Shadow: Small
  - Scale: 0.99
```

---

## Animations et Transitions

### Durées Standards

```
Fast: 150ms (petits changements, hovers)
Medium: 250ms (transitions standards)
Slow: 400ms (modals, grandes transitions)
```

### Courbes d'Animation

```
Standard: cubic-bezier(0.4, 0.0, 0.2, 1)
Decelerate: cubic-bezier(0.0, 0.0, 0.2, 1)
Accelerate: cubic-bezier(0.4, 0.0, 1, 1)
```

### Implémentation

**Android (Compose)**:
```kotlin
animateContentSize(
    animationSpec = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )
)
```

**iOS (SwiftUI)**:
```swift
.animation(.spring(response: 0.3, dampingFraction: 0.7), value: state)
```

**Web (TailwindCSS)**:
```css
transition-all duration-250 ease-in-out
```

---

## Dark Mode

### Stratégie
- **Support automatique** basé sur les préférences système
- **Toggle manuel** disponible dans les paramètres
- **Contraste amélioré** pour les éléments importants en mode sombre

### Adaptations Spécifiques

#### iOS Liquid Glass
Les matériaux iOS s'adaptent automatiquement au mode sombre. Pas d'ajustement nécessaire.

#### Android Material You
Material Theme 3 gère automatiquement le mode sombre via le ColorScheme.

#### Web TailwindCSS
Utiliser la classe `dark:` pour tous les styles sensibles au thème:
```html
<div class="bg-white dark:bg-slate-800 text-slate-900 dark:text-slate-100">
```

---

## Accessibilité

### Contraste Minimum
- **Normal Text**: 4.5:1 (WCAG AA)
- **Large Text** (18px+): 3:1 (WCAG AA)
- **UI Components**: 3:1

### Tailles de Touch Target
- **Minimum**: 44dp / 44pt / 44px (iOS guidelines)
- **Recommandé**: 48dp / 48pt / 48px (Material guidelines)

### Navigation Clavier (Web)
- Tous les éléments interactifs accessibles via Tab
- Focus visible avec outline personnalisé
- Skip links pour navigation rapide

### Screen Readers
- Labels ARIA appropriés (web)
- `contentDescription` (Android)
- `accessibilityLabel` (iOS)

---

## Guidelines de Contribution

### Ajout de Nouvelles Couleurs
1. Définir la couleur dans les 3 plateformes simultanément
2. Fournir les variantes Light/Main/Dark
3. Tester en mode clair et sombre
4. Vérifier le contraste WCAG

### Création de Nouveaux Composants
1. Créer d'abord la version Android (Compose) dans `commonMain`
2. Créer la version iOS (SwiftUI) avec équivalence visuelle
3. Planifier la version Web (TailwindCSS) - implémentation ultérieure
4. Documenter l'usage dans ce fichier

### Modification de Composants Existants
1. Assurer la cohérence visuelle entre plateformes
2. Tester sur les 3 plateformes si possible
3. Mettre à jour ce document
4. Créer un snapshot visuel (screenshots)

---

## Ressources et Références

### Documentation Officielle

**Android Material You:**
- [Material Design 3](https://m3.material.io/)
- [Jetpack Compose Material3](https://developer.android.com/jetpack/compose/designsystems/material3)

**iOS Liquid Glass:**
- [Apple - Adopting Liquid Glass](https://developer.apple.com/documentation/technologyoverviews/adopting-liquid-glass)
- [Human Interface Guidelines - Materials](https://developer.apple.com/design/human-interface-guidelines/materials)
- **Implémentation locale**: `iosApp/LIQUID_GLASS_GUIDELINES.md`

**TailwindCSS:**
- [TailwindCSS Documentation](https://tailwindcss.com/docs)
- [TailwindCSS Dark Mode](https://tailwindcss.com/docs/dark-mode)

### Outils de Design

- **Figma**: Pour les maquettes et prototypes
- **Contrast Checker**: [WebAIM Contrast Checker](https://webaim.org/resources/contrastchecker/)
- **Color Converter**: [RGB to Hex](https://www.rgbtohex.net/)

---

## Checklist d'Implémentation

### Phase 1: Couleurs (À faire)
- [ ] Créer `Color.kt` dans `shared/commonMain` avec palette unifiée
- [ ] Créer `WakevColors.swift` dans `iosApp/Theme/`
- [ ] Configurer `tailwind.config.js` avec palette web
- [ ] Tester les couleurs en mode clair et sombre

### Phase 2: Typographie (À faire)
- [ ] Créer `Typography.kt` avec échelle Material
- [ ] Créer `WakevTypography.swift` avec échelle iOS
- [ ] Configurer fontSize dans Tailwind
- [ ] Documenter l'usage des styles

### Phase 3: Composants de Base (À faire)
- [ ] Créer composants boutons (3 plateformes)
- [ ] Créer composants cartes (3 plateformes)
- [ ] Créer composants input (3 plateformes)
- [ ] Créer composants badges (3 plateformes)

### Phase 4: Composants Avancés (À faire)
- [ ] Navigation (BottomNav, Tabs)
- [ ] Modals et Dialogs
- [ ] Lists et Grids
- [ ] Loading states et skeletons

### Phase 5: Validation (À faire)
- [ ] Tests visuels cross-platform
- [ ] Tests accessibilité
- [ ] Tests mode sombre
- [ ] Documentation finale

---

## Notes Importantes

### Liquid Glass sur iOS
Les guidelines Liquid Glass sont **déjà implémentées** dans l'application iOS via:
- `iosApp/iosApp/Extensions/ViewExtensions.swift` - Extensions helpers
- `iosApp/iosApp/Theme/LiquidGlassModifier.swift` - Modifier personnalisé
- `iosApp/LIQUID_GLASS_GUIDELINES.md` - Documentation complète

**Toutes les vues iOS modernes utilisent déjà le Liquid Glass.**

### Material You sur Android
Material You avec support du **dynamic theming** sera implémenté dans la Phase 3. La palette de couleurs définie ci-dessus servira de base, avec adaptation automatique aux couleurs système sur Android 12+.

### Layout Web
Le layout et l'architecture des composants web seront définis ultérieurement. Ce document établit les fondations (couleurs, typographie, tokens) nécessaires pour garantir la cohérence visuelle future.

---

**Dernière mise à jour**: 25 décembre 2025  
**Version**: 1.0.0  
**Mainteneurs**: Équipe Design & Engineering Wakeve
