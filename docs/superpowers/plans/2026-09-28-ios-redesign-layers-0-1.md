# Refonte iOS — Couches 0 (nettoyage) et 1 (socle WK) — Plan d'implémentation

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Supprimer le code mort iOS identifié par la spec, puis poser le socle `WK` (tokens + composants de base + garde-fou anti-styles-en-dur) sans aucun changement visible pour l'utilisateur.

**Architecture:** Couche 0 : on extrait `EventNextAction` (seul type vivant de `HomeView.swift`) dans son propre fichier, puis on supprime `HomeView.swift`, `EventDetailExperienceView.swift` et `LiquidGlassTabBar`, en ajustant les tests de contrat source qui les référencent. Couche 1 : un namespace `WK` (`Theme/WK.swift`) et des composants SwiftUI dans `Components/WK/`, testés par XCTest (logique pure + contrastes AA) ; le garde-fou « lint » est un test de contrat XCTest à cliquet (SwiftLint n'est pas installé dans le projet ; le repo utilise déjà ce style de test source).

**Tech Stack:** SwiftUI (iOS 18.2 min, Liquid Glass sous `#available(iOS 26.0, *)`), XCTest, Xcode (dossiers synchronisés : tout fichier ajouté sous `iosApp/src/` ou `iosApp/WakeveTests/` est automatiquement dans la cible).

**Spec :** `docs/superpowers/specs/2026-09-28-ios-redesign-design.md` — **Proposition Swarm DAO :** #47.

**Écarts assumés vs spec (à reporter dans la spec en fin de plan, Task 16) :**
- Garde-fou = test XCTest `WKStyleGuardTests` (cliquet sur baseline) au lieu d'une règle SwiftLint.
- Couleurs de statut en sombre = hex explicites (et non « 22 % d'opacité »), pour être testables ; contrastes AA vérifiés.
- `WKModuleSheet` et `WKImmersiveScaffold` sont livrés avec leurs couches (5 et 8), pas ici (YAGNI).

---

## Commandes communes

Depuis la racine du worktree :

```bash
# Tous les tests unitaires iOS
xcodebuild test -project iosApp/iosApp.xcodeproj -scheme WakeveApp \
  -destination 'platform=iOS Simulator,name=iPhone 18 Pro' \
  -only-testing:WakeveTests 2>&1 | tail -30

# Une classe de test
xcodebuild test -project iosApp/iosApp.xcodeproj -scheme WakeveApp \
  -destination 'platform=iOS Simulator,name=iPhone 18 Pro' \
  -only-testing:WakeveTests/<NomDeLaClasse> 2>&1 | tail -30
```

Le premier build déclenche `:shared:embedAndSignAppleFrameworkForXcode` (Gradle) : compter plusieurs minutes. « `** TEST SUCCEEDED **` » = vert ; « `** TEST FAILED **` » ou « `** BUILD FAILED **` » = rouge.

**Avant la Task 1**, lancer la suite complète une fois et noter les tests déjà en échec (s'il y en a) dans le message du premier commit : ils ne doivent pas être imputés à ce plan.

## Structure des fichiers

| Fichier | Action | Responsabilité |
|---|---|---|
| `iosApp/src/Models/EventNextAction.swift` | Créer | Dérivation de la prochaine action d'un événement (déplacé tel quel) |
| `iosApp/src/Views/Events/HomeView.swift` | Supprimer | Code mort |
| `iosApp/src/Views/EventDetailExperienceView.swift` | Supprimer | Code mort |
| `iosApp/src/Components/DesignSystem/PremiumLiquidGlassComponents.swift` | Modifier | Retirer `LiquidGlassTabItem` + `LiquidGlassTabBar` |
| `iosApp/WakeveTests/PremiumHomeContractTests.swift` | Supprimer | Testait uniquement `HomeView` |
| `iosApp/WakeveTests/PremiumDesignSystemContractTests.swift` | Modifier | Retirer les références aux fichiers supprimés |
| `iosApp/WakeveTests/FindingsRegressionTests.swift` | Modifier | Pointer vers `Models/EventNextAction.swift` |
| `iosApp/src/Theme/WK.swift` | Créer | Tokens : couleurs, statuts, rayons, espacements, typo, mouvement, mood |
| `iosApp/src/Models/AppZone.swift` | Créer | Zones de navigation (`events`, `activity`) |
| `iosApp/src/Components/WK/WKCard.swift` | Créer | Carte |
| `iosApp/src/Components/WK/WKStatusPill.swift` | Créer | Pastille de statut |
| `iosApp/src/Components/WK/WKAvatarStack.swift` | Créer | Avatars empilés + libellé VoiceOver |
| `iosApp/src/Components/WK/WKHeroMetric.swift` | Créer | Grand chiffre héro |
| `iosApp/src/Components/WK/WKButtons.swift` | Créer | `WKPrimaryButton`, `WKChip`, `WKCircleButton` |
| `iosApp/src/Components/WK/WKActionBar.swift` | Créer | Action principale + icônes |
| `iosApp/src/Components/WK/WKModuleTile.swift` | Créer | Tuile de module du hub |
| `iosApp/src/Components/WK/WKFloatingNavBar.swift` | Créer | Barre flottante 3 zones |
| `iosApp/src/Components/WK/WKGallery.swift` | Créer | Previews clair / sombre / AX5 |
| `iosApp/src/Resources/{en,fr,es,it,pt}.lproj/Localizable.strings` | Modifier | Clés `wk.*` |
| `iosApp/WakeveTests/WKTokensTests.swift` | Créer | Valeurs + contrastes AA |
| `iosApp/WakeveTests/WKAvatarStackTests.swift` | Créer | Débordement + libellé |
| `iosApp/WakeveTests/WKComponentsContractTests.swift` | Créer | Contrats a11y (44 pt, labels) + clés localisées |
| `iosApp/WakeveTests/WKStyleGuardTests.swift` | Créer | Garde-fou anti-styles-en-dur à cliquet |

---

# Couche 0 — Nettoyage

### Task 1 : Extraire `EventNextAction` dans son propre fichier

**Files:**
- Create: `iosApp/src/Models/EventNextAction.swift`
- Modify: `iosApp/src/Views/Events/HomeView.swift` (retirer le bloc `// MARK: - Event Next Action` … jusqu'avant `// MARK: - Event Theme`, lignes ~43-107)
- Modify: `iosApp/WakeveTests/FindingsRegressionTests.swift:356`

- [ ] **Step 1 : Rediriger le test vers le futur fichier**

Dans `FindingsRegressionTests.swift`, méthode `testEventNextActionDoesNotBlockInvitationFirstPollsOnParticipants`, remplacer :

```swift
        let source = try readProjectFile("iosApp/src/Views/Events/HomeView.swift")
        let nextAction = slice(source, from: "struct EventNextAction", to: "// MARK: - Event Theme")
```

par :

```swift
        let source = try readProjectFile("iosApp/src/Models/EventNextAction.swift")
        let nextAction = slice(source, from: "struct EventNextAction", to: "// END EventNextAction")
```

- [ ] **Step 2 : Vérifier l'échec**

Run: `xcodebuild test … -only-testing:WakeveTests/FindingsRegressionTests/testEventNextActionDoesNotBlockInvitationFirstPollsOnParticipants`
Expected: FAIL (fichier introuvable → erreur levée par `readProjectFile`).

- [ ] **Step 3 : Créer le fichier et déplacer le type**

Couper dans `HomeView.swift` le bloc complet `struct EventNextAction { … }` (de `// MARK: - Event Next Action` jusqu'à l'accolade fermante précédant `// MARK: - Event Theme`) et le coller dans `iosApp/src/Models/EventNextAction.swift` :

```swift
import Foundation
import Shared

// MARK: - Event Next Action

/// Prochaine action utile pour un événement, dérivée de son statut.
/// Déplacé depuis HomeView.swift (couche 0 de la refonte iOS, proposition #47).
struct EventNextAction {
    // … corps strictement identique à l'original (title, shortTitle, subtitle,
    // blockedReason, systemImage, isBlocked, displaySubtitle, init(event:)) …
}
// END EventNextAction
```

Le corps doit être copié **à l'identique** (aucune modification de logique). Vérifier les imports en tête de `HomeView.swift` : si `Event` y vient d'un autre module que `Shared`, reprendre le même import.

- [ ] **Step 4 : Vérifier le succès et la compilation**

Run: même commande qu'au Step 2.
Expected: PASS, et `ContentView.swift:4066` (`EventNextAction(event: event)`) compile toujours.

- [ ] **Step 5 : Commit**

```bash
git add iosApp/src/Models/EventNextAction.swift iosApp/src/Views/Events/HomeView.swift iosApp/WakeveTests/FindingsRegressionTests.swift
git commit -m "refactor(ios): extract EventNextAction into its own model file

Refs Swarm DAO #47 (layer 0).

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

### Task 2 : Supprimer `HomeView.swift` (code mort)

**Files:**
- Delete: `iosApp/src/Views/Events/HomeView.swift`
- Delete: `iosApp/WakeveTests/PremiumHomeContractTests.swift`
- Modify: `iosApp/WakeveTests/PremiumDesignSystemContractTests.swift`

- [ ] **Step 1 : Confirmer qu'aucun type restant n'est utilisé ailleurs**

```bash
cd iosApp/src && for t in HomeEventFilter EventTheme HomeView HomeContentView EventsCarouselView VisualEventCard FilterDropdownMenu HomeEmptyStateView LoadingEventsView; do echo "$t $(grep -rlw "$t" . --include='*.swift' | grep -v Views/Events/HomeView.swift | wc -l)"; done
```

Expected: `0` pour chaque type. Si un compteur est > 0, **arrêter** et signaler (le type doit être extrait comme en Task 1).

- [ ] **Step 2 : Mettre à jour les tests de contrat**

Dans `PremiumDesignSystemContractTests.swift` :

1. `testSecondaryScreensStayWithinFixedTypographyBudgets` : supprimer la ligne
   `("iosApp/src/Views/Events/HomeView.swift", 7),`
2. `testSecondaryMotionAndLegalControlsHonorAccessibilitySettings` : supprimer la ligne
   `"iosApp/src/Views/Events/HomeView.swift",` du tableau `for path in [...]`.
3. `testRemainingHomeAndPollHighlightsUseSemanticColors` : supprimer les lignes qui lisent `home` et `blockedAction` et les deux assertions associées (`XCTAssertFalse(blockedAction.contains("Color.orange") …` et `XCTAssertTrue(blockedAction.contains("SemanticColor.warning") …`). Renommer la méthode en `testRemainingPollHighlightsUseSemanticColors`. Le reste (partie `pollResults`) est inchangé.

Supprimer `iosApp/WakeveTests/PremiumHomeContractTests.swift` (il ne teste que `HomeView`).

- [ ] **Step 3 : Supprimer le fichier**

```bash
git rm iosApp/src/Views/Events/HomeView.swift iosApp/WakeveTests/PremiumHomeContractTests.swift
```

- [ ] **Step 4 : Vérifier qu'aucune référence ne subsiste puis lancer les tests**

```bash
grep -rn "Views/Events/HomeView.swift" iosApp/ ; echo "exit=$?"
```
Expected: aucune ligne, `exit=1`.

Run: suite complète `-only-testing:WakeveTests`.
Expected: `** TEST SUCCEEDED **` (hors échecs préexistants notés avant la Task 1).

- [ ] **Step 5 : Commit**

```bash
git add -A iosApp/WakeveTests/PremiumDesignSystemContractTests.swift
git commit -m "chore(ios): remove unused HomeView and its contract tests

Refs Swarm DAO #47 (layer 0).

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

### Task 3 : Supprimer `EventDetailExperienceView.swift` (code mort)

**Files:**
- Delete: `iosApp/src/Views/EventDetailExperienceView.swift`
- Modify: `iosApp/WakeveTests/PremiumDesignSystemContractTests.swift`

- [ ] **Step 1 : Confirmer l'absence d'usage**

```bash
cd iosApp/src && for t in EventDetailExperienceView TransportPlanView; do echo "$t $(grep -rlw "$t" . --include='*.swift' | grep -v EventDetailExperienceView.swift | wc -l)"; done
```
Expected: `0` pour les deux.

- [ ] **Step 2 : Retirer le test dédié**

Dans `PremiumDesignSystemContractTests.swift`, supprimer entièrement la méthode `testEventDetailExperienceUsesDynamicTypeAndAccessibleToolbarTargets()`.

- [ ] **Step 3 : Supprimer le fichier**

```bash
git rm iosApp/src/Views/EventDetailExperienceView.swift
```

- [ ] **Step 4 : Vérifier**

```bash
grep -rn "EventDetailExperienceView" iosApp/ ; echo "exit=$?"
```
Expected: `exit=1`. Puis suite complète : `** TEST SUCCEEDED **`.

- [ ] **Step 5 : Commit**

```bash
git add -A iosApp/WakeveTests/PremiumDesignSystemContractTests.swift
git commit -m "chore(ios): remove unused EventDetailExperienceView

Refs Swarm DAO #47 (layer 0).

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

### Task 4 : Supprimer `LiquidGlassTabBar` (code mort)

**Files:**
- Modify: `iosApp/src/Components/DesignSystem/PremiumLiquidGlassComponents.swift` (~lignes 227-283)
- Modify: `iosApp/WakeveTests/PremiumDesignSystemContractTests.swift:11`

- [ ] **Step 1 : Mettre à jour le test d'inventaire**

Dans `testPremiumLiquidGlassComponentsExist`, supprimer la ligne `"struct LiquidGlassTabBar",` du tableau `requiredComponents`, et ajouter à la fin de la méthode :

```swift
        XCTAssertFalse(source.contains("struct LiquidGlassTabBar"), "LiquidGlassTabBar est remplacée par WKFloatingNavBar (#47).")
```

- [ ] **Step 2 : Vérifier l'échec**

Run: `-only-testing:WakeveTests/PremiumDesignSystemContractTests/testPremiumLiquidGlassComponentsExist`
Expected: FAIL sur « LiquidGlassTabBar est remplacée… ».

- [ ] **Step 3 : Supprimer les deux types**

Dans `PremiumLiquidGlassComponents.swift`, supprimer de la ligne `struct LiquidGlassTabItem: Identifiable, Hashable {` jusqu'à l'accolade fermante de `struct LiquidGlassTabBar` incluse (la ligne précédant `struct EventHeroCard<Content: View>: View {`). Supprimer aussi tout `#Preview` qui instancie `LiquidGlassTabBar` dans ce fichier, s'il y en a.

```bash
grep -rn "LiquidGlassTabItem\|LiquidGlassTabBar" iosApp/src ; echo "exit=$?"
```
Expected: `exit=1`.

- [ ] **Step 4 : Vérifier**

Run: même test qu'au Step 2 → PASS, puis suite complète → `** TEST SUCCEEDED **`.

- [ ] **Step 5 : Commit**

```bash
git add iosApp/src/Components/DesignSystem/PremiumLiquidGlassComponents.swift iosApp/WakeveTests/PremiumDesignSystemContractTests.swift
git commit -m "chore(ios): remove unused LiquidGlassTabBar

Refs Swarm DAO #47 (layer 0).

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

# Couche 1 — Socle WK

### Task 5 : Tokens de couleur et de statut (`WK.swift`)

**Files:**
- Create: `iosApp/src/Theme/WK.swift`
- Test: `iosApp/WakeveTests/WKTokensTests.swift`

- [ ] **Step 1 : Écrire les tests**

```swift
import XCTest
import SwiftUI
import UIKit
@testable import Wakeve

final class WKTokensTests: XCTestCase {

    private func rgb(_ color: Color, _ style: UIUserInterfaceStyle) -> (Int, Int, Int) {
        let resolved = UIColor(color).resolvedColor(with: UITraitCollection(userInterfaceStyle: style))
        var r: CGFloat = 0, g: CGFloat = 0, b: CGFloat = 0, a: CGFloat = 0
        resolved.getRed(&r, green: &g, blue: &b, alpha: &a)
        return (Int(round(r * 255)), Int(round(g * 255)), Int(round(b * 255)))
    }

    private func luminance(_ color: Color, _ style: UIUserInterfaceStyle) -> Double {
        let (r, g, b) = rgb(color, style)
        func channel(_ v: Int) -> Double {
            let c = Double(v) / 255
            return c <= 0.03928 ? c / 12.92 : pow((c + 0.055) / 1.055, 2.4)
        }
        return 0.2126 * channel(r) + 0.7152 * channel(g) + 0.0722 * channel(b)
    }

    private func contrast(_ a: Color, _ b: Color, _ style: UIUserInterfaceStyle) -> Double {
        let la = luminance(a, style), lb = luminance(b, style)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    func testSurfaceTokensMatchSpec() {
        XCTAssertTrue(rgb(WK.Colors.canvas, .light) == (0xF2, 0xF2, 0xF4))
        XCTAssertTrue(rgb(WK.Colors.canvas, .dark) == (0, 0, 0))
        XCTAssertTrue(rgb(WK.Colors.card, .light) == (0xFF, 0xFF, 0xFF))
        XCTAssertTrue(rgb(WK.Colors.card, .dark) == (0x1C, 0x1C, 0x1E))
        XCTAssertTrue(rgb(WK.Colors.cardInset, .light) == (0xF6, 0xF6, 0xF8))
    }

    func testAccentIsSingleIndigo() {
        XCTAssertTrue(rgb(WK.Colors.accent, .light) == (0x5B, 0x54, 0xD6))
        XCTAssertTrue(rgb(WK.Colors.accent, .dark) == (0x8B, 0x85, 0xF0))
    }

    func testStatusLightValuesMatchSpec() {
        XCTAssertTrue(rgb(WK.Status.confirmed.color, .light) == (0x2E, 0x9D, 0x5B))
        XCTAssertTrue(rgb(WK.Status.pending.fill, .light) == (0xFF, 0xF1, 0xD6))
        XCTAssertTrue(rgb(WK.Status.actionNeeded.onFill, .light) == (0xA1, 0x2E, 0x2E))
        XCTAssertTrue(rgb(WK.Status.draft.fill, .light) == (0xF2, 0xF2, 0xF4))
    }

    func testEveryStatusPillMeetsWCAGAAInBothModes() {
        for status in WK.Status.allCases {
            for style in [UIUserInterfaceStyle.light, .dark] {
                let ratio = contrast(status.onFill, status.fill, style)
                XCTAssertGreaterThanOrEqual(ratio, 4.5, "\(status) en \(style == .dark ? "sombre" : "clair") : \(ratio)")
            }
        }
    }

    func testPrimaryButtonMeetsWCAGAA() {
        for style in [UIUserInterfaceStyle.light, .dark] {
            XCTAssertGreaterThanOrEqual(contrast(WK.Colors.onPrimaryButton, WK.Colors.primaryButton, style), 4.5)
        }
    }

    func testTextOnAccentMeetsWCAGAA() {
        for style in [UIUserInterfaceStyle.light, .dark] {
            XCTAssertGreaterThanOrEqual(contrast(WK.Colors.onAccent, WK.Colors.accent, style), 4.5)
            XCTAssertGreaterThanOrEqual(contrast(WK.Colors.onAccentFill, WK.Colors.accentFill, style), 4.5)
        }
    }

    func testRadiiAndSpacingScale() {
        XCTAssertEqual(WK.Radius.sm, 12)
        XCTAssertEqual(WK.Radius.md, 18)
        XCTAssertEqual(WK.Radius.lg, 28)
        XCTAssertEqual([WK.Space.xxs, WK.Space.xs, WK.Space.sm, WK.Space.md, WK.Space.lg, WK.Space.xl], [4, 8, 12, 16, 24, 32])
        XCTAssertEqual(WK.Space.screen, 16)
        XCTAssertEqual(WK.Size.minTapTarget, 44)
    }
}
```

- [ ] **Step 2 : Vérifier l'échec**

Run: `-only-testing:WakeveTests/WKTokensTests`
Expected: BUILD FAILED — `cannot find 'WK' in scope`.

- [ ] **Step 3 : Implémenter `WK.swift`**

```swift
import SwiftUI
import UIKit

/// Tokens de la refonte iOS 2026 (proposition Swarm DAO #47).
/// Source unique pour toute nouvelle vue. Aucune valeur de style en dur hors de ce fichier.
enum WK {

    // MARK: - Primitives

    static func uiColor(_ hex: UInt32, alpha: CGFloat = 1) -> UIColor {
        UIColor(
            red: CGFloat((hex >> 16) & 0xFF) / 255,
            green: CGFloat((hex >> 8) & 0xFF) / 255,
            blue: CGFloat(hex & 0xFF) / 255,
            alpha: alpha
        )
    }

    static func dynamic(light: UInt32, dark: UInt32) -> Color {
        Color(uiColor: UIColor { traits in
            traits.userInterfaceStyle == .dark ? uiColor(dark) : uiColor(light)
        })
    }

    static func shape(_ radius: CGFloat) -> RoundedRectangle {
        RoundedRectangle(cornerRadius: radius, style: .continuous)
    }

    // MARK: - Couleurs

    enum Colors {
        static let canvas = WK.dynamic(light: 0xF2F2F4, dark: 0x000000)
        static let card = WK.dynamic(light: 0xFFFFFF, dark: 0x1C1C1E)
        static let cardInset = WK.dynamic(light: 0xF6F6F8, dark: 0x2C2C2E)

        static let textPrimary = Color(uiColor: .label)
        static let textSecondary = Color(uiColor: .secondaryLabel)
        static let textTertiary = Color(uiColor: .tertiaryLabel)

        static let accent = WK.dynamic(light: 0x5B54D6, dark: 0x8B85F0)
        static let accentFill = WK.dynamic(light: 0xE7E5FB, dark: 0x2A2750)
        static let onAccentFill = WK.dynamic(light: 0x3F3A9E, dark: 0xC9C5FA)
        static let onAccent = WK.dynamic(light: 0xFFFFFF, dark: 0x1C1C1E)

        static let primaryButton = WK.dynamic(light: 0x1C1C1E, dark: 0xFFFFFF)
        static let onPrimaryButton = WK.dynamic(light: 0xFFFFFF, dark: 0x1C1C1E)
    }

    // MARK: - Statuts

    /// Couleur = signal d'état et d'action requise, jamais décoration.
    enum Status: CaseIterable {
        case confirmed, pending, actionNeeded, draft

        var color: Color {
            switch self {
            case .confirmed: return WK.dynamic(light: 0x2E9D5B, dark: 0x4CC07A)
            case .pending: return WK.dynamic(light: 0xB7791F, dark: 0xE0A33F)
            case .actionNeeded: return WK.dynamic(light: 0xD64545, dark: 0xEF6B6B)
            case .draft: return WK.dynamic(light: 0x8A8A8E, dark: 0x8E8E93)
            }
        }

        var fill: Color {
            switch self {
            case .confirmed: return WK.dynamic(light: 0xDFF3E6, dark: 0x173826)
            case .pending: return WK.dynamic(light: 0xFFF1D6, dark: 0x3D2E12)
            case .actionNeeded: return WK.dynamic(light: 0xFDE7E7, dark: 0x44191A)
            case .draft: return WK.dynamic(light: 0xF2F2F4, dark: 0x2C2C2E)
            }
        }

        var onFill: Color {
            switch self {
            case .confirmed: return WK.dynamic(light: 0x1E6B3C, dark: 0x7FD8A2)
            case .pending: return WK.dynamic(light: 0x8A5A0B, dark: 0xF3C774)
            case .actionNeeded: return WK.dynamic(light: 0xA12E2E, dark: 0xF59A9A)
            case .draft: return WK.dynamic(light: 0x5A5A5F, dark: 0xAEAEB2)
            }
        }
    }

    // MARK: - Géométrie

    enum Radius {
        static let sm: CGFloat = 12
        static let md: CGFloat = 18
        static let lg: CGFloat = 28
    }

    enum Space {
        static let xxs: CGFloat = 4
        static let xs: CGFloat = 8
        static let sm: CGFloat = 12
        static let md: CGFloat = 16
        static let lg: CGFloat = 24
        static let xl: CGFloat = 32
        static let screen: CGFloat = 16
    }

    enum Size {
        static let minTapTarget: CGFloat = 44
        static let avatar: CGFloat = 28
        static let primaryButtonHeight: CGFloat = 52
    }
}
```

- [ ] **Step 4 : Vérifier le succès**

Run: `-only-testing:WakeveTests/WKTokensTests`
Expected: `** TEST SUCCEEDED **` (7 tests).

- [ ] **Step 5 : Commit**

```bash
git add iosApp/src/Theme/WK.swift iosApp/WakeveTests/WKTokensTests.swift
git commit -m "feat(ios): add WK color, status and geometry tokens

Single indigo accent, four status colors with AA-checked fills,
continuous radii and spacing scale. Refs Swarm DAO #47 (layer 1).

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

### Task 6 : Typographie, mouvement et mood immersif

**Files:**
- Modify: `iosApp/src/Theme/WK.swift`
- Test: `iosApp/WakeveTests/WKTokensTests.swift`

- [ ] **Step 1 : Ajouter les tests**

Ajouter dans `WKTokensTests` :

```swift
    func testMotionFallsBackToFadeUnderReduceMotion() {
        XCTAssertEqual(WK.Motion.snappy(reduceMotion: true), WK.Motion.reducedFade)
        XCTAssertEqual(WK.Motion.smooth(reduceMotion: true), WK.Motion.reducedFade)
        XCTAssertEqual(WK.Motion.bouncy(reduceMotion: true), WK.Motion.reducedFade)
        XCTAssertNotEqual(WK.Motion.bouncy(reduceMotion: false), WK.Motion.reducedFade)
    }

    func testImmersiveMoodTextIsReadableOnItsBackground() {
        for mood in EventMoodPalette.Mood.allCases {
            let m = WK.Mood(palette: .palette(for: mood))
            XCTAssertGreaterThanOrEqual(contrast(m.textPrimary, m.background, .dark), 7, "\(mood)")
            XCTAssertGreaterThanOrEqual(contrast(m.textSecondary, m.background, .dark), 4.5, "\(mood)")
        }
    }
```

- [ ] **Step 2 : Vérifier l'échec**

Run: `-only-testing:WakeveTests/WKTokensTests`
Expected: BUILD FAILED — `type 'WK' has no member 'Motion'`.

- [ ] **Step 3 : Implémenter**

Ajouter dans `enum WK` (après `Size`) :

```swift
    // MARK: - Typographie (toujours indexée sur Dynamic Type)

    enum Typo {
        static let display = Font.system(.largeTitle, design: .rounded).weight(.semibold)
        static let title = Font.system(.title2).weight(.semibold)
        static let headline = Font.headline
        static let body = Font.body
        static let caption = Font.footnote
        static let micro = Font.caption
    }

    // MARK: - Mouvement

    enum Motion {
        static let reducedFade = Animation.easeInOut(duration: 0.15)

        static func snappy(reduceMotion: Bool) -> Animation {
            reduceMotion ? reducedFade : .snappy(duration: 0.25)
        }

        static func smooth(reduceMotion: Bool) -> Animation {
            reduceMotion ? reducedFade : .smooth(duration: 0.35)
        }

        static func bouncy(reduceMotion: Bool) -> Animation {
            reduceMotion ? reducedFade : .bouncy(duration: 0.4)
        }
    }

    // MARK: - Mode immersif

    /// Ambiance sombre teintée par la palette de l'événement (invitation, jour J).
    struct Mood {
        let background: Color
        let surface: Color
        let textPrimary: Color
        let textSecondary: Color
        let pillStroke: Color
        let accent: Color

        init(palette: EventMoodPalette) {
            let tint = UIColor(palette.primary(for: .dark))
            background = Color(uiColor: WK.darkened(tint, towardsBlackBy: 0.72))
            surface = Color(uiColor: WK.uiColor(0xFFFFFF, alpha: 0.08))
            textPrimary = Color(uiColor: WK.uiColor(0xF2F7F6))
            textSecondary = Color(uiColor: WK.uiColor(0xB8C4C2))
            pillStroke = Color(uiColor: WK.uiColor(0xFFFFFF, alpha: 0.25))
            accent = palette.accent(for: .dark)
        }
    }

    static func darkened(_ color: UIColor, towardsBlackBy amount: CGFloat) -> UIColor {
        var r: CGFloat = 0, g: CGFloat = 0, b: CGFloat = 0, a: CGFloat = 0
        color.getRed(&r, green: &g, blue: &b, alpha: &a)
        let keep = 1 - amount
        return UIColor(red: r * keep, green: g * keep, blue: b * keep, alpha: 1)
    }
```

Les deux couleurs de texte sont opaques pour que le test mesure le rendu réel. Si le test échoue pour une palette, augmenter `towardsBlackBy` à `0.78` (ne jamais baisser le seuil).

- [ ] **Step 4 : Vérifier le succès**

Run: `-only-testing:WakeveTests/WKTokensTests`
Expected: `** TEST SUCCEEDED **` (9 tests).

- [ ] **Step 5 : Commit**

```bash
git add iosApp/src/Theme/WK.swift iosApp/WakeveTests/WKTokensTests.swift
git commit -m "feat(ios): add WK typography, motion and immersive mood tokens

Refs Swarm DAO #47 (layer 1).

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

### Task 7 : Clés localisées `wk.*`

**Files:**
- Modify: `iosApp/src/Resources/{en,fr,es,it,pt}.lproj/Localizable.strings`
- Test: `iosApp/WakeveTests/WKComponentsContractTests.swift`

- [ ] **Step 1 : Écrire le test**

```swift
import XCTest
@testable import Wakeve

final class WKComponentsContractTests: XCTestCase {

    static let requiredKeys = [
        "wk.avatars.others_format",
        "wk.nav.events",
        "wk.nav.create",
        "wk.nav.activity",
        "wk.nav.activity.badge_format"
    ]

    func testWKKeysAreLocalizedInSupportedLocales() throws {
        for locale in ["en", "fr", "es", "it", "pt"] {
            let strings = try readProjectFile("iosApp/src/Resources/\(locale).lproj/Localizable.strings")
            for key in Self.requiredKeys {
                XCTAssertTrue(strings.contains("\"\(key)\""), "Clé \(key) manquante pour \(locale).")
            }
        }
    }

    func readProjectFile(_ relativePath: String) throws -> String {
        let projectRoot = URL(fileURLWithPath: #filePath)
            .deletingLastPathComponent()   // WakeveTests
            .deletingLastPathComponent()   // iosApp
            .deletingLastPathComponent()   // racine
        return try String(contentsOf: projectRoot.appendingPathComponent(relativePath), encoding: .utf8)
    }
}
```

- [ ] **Step 2 : Vérifier l'échec**

Run: `-only-testing:WakeveTests/WKComponentsContractTests`
Expected: FAIL — « Clé wk.avatars.others_format manquante pour en. »

- [ ] **Step 3 : Ajouter les clés en fin de chaque fichier**

`en.lproj/Localizable.strings` :
```
/* WK design system (redesign 2026) */
"wk.avatars.others_format" = "%d others";
"wk.nav.events" = "Events";
"wk.nav.create" = "Create event";
"wk.nav.activity" = "Activity";
"wk.nav.activity.badge_format" = "%d to do";
```

`fr.lproj/Localizable.strings` :
```
/* WK design system (refonte 2026) */
"wk.avatars.others_format" = "%d autres";
"wk.nav.events" = "Événements";
"wk.nav.create" = "Créer un événement";
"wk.nav.activity" = "Activité";
"wk.nav.activity.badge_format" = "%d à traiter";
```

`es.lproj/Localizable.strings` :
```
/* WK design system (redesign 2026) */
"wk.avatars.others_format" = "%d más";
"wk.nav.events" = "Eventos";
"wk.nav.create" = "Crear evento";
"wk.nav.activity" = "Actividad";
"wk.nav.activity.badge_format" = "%d pendientes";
```

`it.lproj/Localizable.strings` :
```
/* WK design system (redesign 2026) */
"wk.avatars.others_format" = "altri %d";
"wk.nav.events" = "Eventi";
"wk.nav.create" = "Crea evento";
"wk.nav.activity" = "Attività";
"wk.nav.activity.badge_format" = "%d da fare";
```

`pt.lproj/Localizable.strings` :
```
/* WK design system (redesign 2026) */
"wk.avatars.others_format" = "mais %d";
"wk.nav.events" = "Eventos";
"wk.nav.create" = "Criar evento";
"wk.nav.activity" = "Atividade";
"wk.nav.activity.badge_format" = "%d pendentes";
```

- [ ] **Step 4 : Vérifier le succès**

Run: `-only-testing:WakeveTests/WKComponentsContractTests`
Expected: `** TEST SUCCEEDED **`.

- [ ] **Step 5 : Commit**

```bash
git add iosApp/src/Resources iosApp/WakeveTests/WKComponentsContractTests.swift
git commit -m "feat(ios): add localized strings for WK components

Refs Swarm DAO #47 (layer 1).

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

### Task 8 : `WKAvatarStack`

**Files:**
- Create: `iosApp/src/Components/WK/WKAvatarStack.swift`
- Test: `iosApp/WakeveTests/WKAvatarStackTests.swift`

- [ ] **Step 1 : Écrire les tests**

```swift
import XCTest
@testable import Wakeve

final class WKAvatarStackTests: XCTestCase {

    private func avatars(_ names: [String]) -> [WKAvatar] {
        names.enumerated().map { WKAvatar(id: "\($0.offset)", name: $0.element) }
    }

    func testShowsAllWhenWithinLimit() {
        let layout = WKAvatarStack.layout(count: 3, maxVisible: 4)
        XCTAssertEqual(layout.visible, 3)
        XCTAssertEqual(layout.overflow, 0)
    }

    func testCollapsesOverflowIntoCounter() {
        let layout = WKAvatarStack.layout(count: 9, maxVisible: 4)
        XCTAssertEqual(layout.visible, 4)
        XCTAssertEqual(layout.overflow, 5)
    }

    func testEmptyStack() {
        let layout = WKAvatarStack.layout(count: 0, maxVisible: 4)
        XCTAssertEqual(layout.visible, 0)
        XCTAssertEqual(layout.overflow, 0)
    }

    func testInitialsUseFirstLettersOfFirstTwoWords() {
        XCTAssertEqual(WKAvatar(id: "1", name: "léa martin").initials, "LM")
        XCTAssertEqual(WKAvatar(id: "2", name: "Tom").initials, "T")
        XCTAssertEqual(WKAvatar(id: "3", name: "  ").initials, "?")
    }

    func testAccessibilityLabelNamesFirstTwoThenCountsOthers() {
        let label = WKAvatarStack.accessibilityLabel(for: avatars(["Léa", "Tom", "Max", "Sam", "Zoé", "Ana"]), locale: Locale(identifier: "en"))
        XCTAssertTrue(label.contains("Léa"))
        XCTAssertTrue(label.contains("Tom"))
        XCTAssertFalse(label.contains("Max"))
        XCTAssertTrue(label.contains("4"))
    }

    func testAccessibilityLabelListsEveryoneWhenThreeOrLess() {
        let label = WKAvatarStack.accessibilityLabel(for: avatars(["Léa", "Tom", "Max"]), locale: Locale(identifier: "en"))
        XCTAssertEqual(label, "Léa, Tom, and Max")
    }
}
```

- [ ] **Step 2 : Vérifier l'échec**

Run: `-only-testing:WakeveTests/WKAvatarStackTests`
Expected: BUILD FAILED — `cannot find 'WKAvatar' in scope`.

- [ ] **Step 3 : Implémenter**

```swift
import SwiftUI

struct WKAvatar: Identifiable, Equatable {
    let id: String
    let name: String
    var imageURL: URL? = nil

    var initials: String {
        let letters = name
            .split(separator: " ")
            .prefix(2)
            .compactMap { $0.first.map { String($0).uppercased() } }
            .joined()
        return letters.isEmpty ? "?" : letters
    }

    /// Teinte stable dérivée de l'identifiant (même personne = même couleur partout).
    var tint: Color {
        let palette: [UInt32] = [0xDCD6F7, 0xBDE8CF, 0xF7C9C4, 0xFBE3B8, 0xCFE3F7, 0xF3D1E6]
        let index = abs(id.unicodeScalars.reduce(0) { $0 &+ Int($1.value) }) % palette.count
        return Color(uiColor: WK.uiColor(palette[index]))
    }
}

struct WKAvatarStack: View {
    let avatars: [WKAvatar]
    var maxVisible: Int = 4
    var size: CGFloat = WK.Size.avatar

    static func layout(count: Int, maxVisible: Int) -> (visible: Int, overflow: Int) {
        let visible = min(count, maxVisible)
        return (visible, max(0, count - visible))
    }

    static func accessibilityLabel(for avatars: [WKAvatar], locale: Locale = .current) -> String {
        let formatter = ListFormatter()
        formatter.locale = locale
        let names = avatars.map(\.name)
        let parts: [String]
        if names.count <= 3 {
            parts = names
        } else {
            let others = String(format: String(localized: "wk.avatars.others_format"), names.count - 2)
            parts = Array(names.prefix(2)) + [others]
        }
        return formatter.string(from: parts) ?? parts.joined(separator: ", ")
    }

    var body: some View {
        let layout = Self.layout(count: avatars.count, maxVisible: maxVisible)
        HStack(spacing: -size * 0.3) {
            ForEach(avatars.prefix(layout.visible)) { avatar in
                avatarView(avatar)
            }
            if layout.overflow > 0 {
                Text("+\(layout.overflow)")
                    .font(WK.Typo.micro.weight(.medium))
                    .foregroundStyle(WK.Colors.textSecondary)
                    .padding(.horizontal, WK.Space.xs)
                    .frame(minWidth: size, minHeight: size)
                    .background(WK.Colors.cardInset, in: Capsule())
                    .overlay(Capsule().stroke(WK.Colors.card, lineWidth: 1.5))
            }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(Self.accessibilityLabel(for: avatars))
    }

    @ViewBuilder
    private func avatarView(_ avatar: WKAvatar) -> some View {
        ZStack {
            Circle().fill(avatar.tint)
            if let url = avatar.imageURL {
                AsyncImage(url: url) { image in
                    image.resizable().scaledToFill()
                } placeholder: {
                    initialsView(avatar)
                }
                .clipShape(Circle())
            } else {
                initialsView(avatar)
            }
        }
        .frame(width: size, height: size)
        .overlay(Circle().stroke(WK.Colors.card, lineWidth: 1.5))
    }

    private func initialsView(_ avatar: WKAvatar) -> some View {
        Text(avatar.initials)
            .font(WK.Typo.micro.weight(.semibold))
            .foregroundStyle(Color(uiColor: WK.uiColor(0x1C1C1E)))
            .minimumScaleFactor(0.6)
    }
}
```

- [ ] **Step 4 : Vérifier le succès**

Run: `-only-testing:WakeveTests/WKAvatarStackTests`
Expected: `** TEST SUCCEEDED **` (6 tests).

- [ ] **Step 5 : Commit**

```bash
git add iosApp/src/Components/WK/WKAvatarStack.swift iosApp/WakeveTests/WKAvatarStackTests.swift
git commit -m "feat(ios): add WKAvatarStack with overflow counter and VoiceOver label

Refs Swarm DAO #47 (layer 1).

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

### Task 9 : `WKCard` et `WKStatusPill`

**Files:**
- Create: `iosApp/src/Components/WK/WKCard.swift`
- Create: `iosApp/src/Components/WK/WKStatusPill.swift`
- Test: `iosApp/WakeveTests/WKComponentsContractTests.swift`

- [ ] **Step 1 : Ajouter les tests**

Dans `WKComponentsContractTests` :

```swift
    func testCardUsesContinuousTokenRadii() throws {
        let source = try readProjectFile("iosApp/src/Components/WK/WKCard.swift")
        XCTAssertTrue(source.contains("WK.shape("), "WKCard doit utiliser WK.shape (coins continus).")
        XCTAssertTrue(source.contains("case standard, inset, selected"))
    }

    func testStatusPillExposesTextToVoiceOver() throws {
        let source = try readProjectFile("iosApp/src/Components/WK/WKStatusPill.swift")
        XCTAssertTrue(source.contains(".accessibilityLabel(text)"), "Le statut ne doit jamais être porté par la couleur seule.")
    }
```

- [ ] **Step 2 : Vérifier l'échec**

Run: `-only-testing:WakeveTests/WKComponentsContractTests`
Expected: FAIL (fichiers introuvables).

- [ ] **Step 3 : Implémenter**

`WKCard.swift` :

```swift
import SwiftUI

struct WKCard<Content: View>: View {
    enum Style { case standard, inset, selected }

    var style: Style = .standard
    var padding: CGFloat = WK.Space.sm
    var radius: CGFloat = WK.Radius.md
    @ViewBuilder let content: () -> Content

    var body: some View {
        content()
            .padding(padding)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(style == .inset ? WK.Colors.cardInset : WK.Colors.card, in: WK.shape(radius))
            .overlay {
                if style == .selected {
                    WK.shape(radius).strokeBorder(WK.Colors.accent, lineWidth: 1.5)
                }
            }
    }
}
```

`WKStatusPill.swift` :

```swift
import SwiftUI

struct WKStatusPill: View {
    let text: String
    let status: WK.Status

    var body: some View {
        Text(text)
            .font(WK.Typo.micro.weight(.medium))
            .foregroundStyle(status.onFill)
            .padding(.horizontal, WK.Space.xs)
            .padding(.vertical, WK.Space.xxs)
            .background(status.fill, in: Capsule())
            .accessibilityLabel(text)
    }
}
```

- [ ] **Step 4 : Vérifier le succès**

Run: `-only-testing:WakeveTests/WKComponentsContractTests` → `** TEST SUCCEEDED **`.

- [ ] **Step 5 : Commit**

```bash
git add iosApp/src/Components/WK/WKCard.swift iosApp/src/Components/WK/WKStatusPill.swift iosApp/WakeveTests/WKComponentsContractTests.swift
git commit -m "feat(ios): add WKCard and WKStatusPill

Refs Swarm DAO #47 (layer 1).

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

### Task 10 : Boutons — `WKPrimaryButton`, `WKChip`, `WKCircleButton`

**Files:**
- Create: `iosApp/src/Components/WK/WKButtons.swift`
- Test: `iosApp/WakeveTests/WKComponentsContractTests.swift`

- [ ] **Step 1 : Ajouter les tests**

```swift
    func testButtonsGuaranteeMinimumTapTarget() throws {
        let source = try readProjectFile("iosApp/src/Components/WK/WKButtons.swift")
        XCTAssertGreaterThanOrEqual(
            source.components(separatedBy: "WK.Size.minTapTarget").count - 1, 3,
            "WKPrimaryButton, WKChip et WKCircleButton doivent chacun garantir 44 pt."
        )
    }

    func testCircleButtonRequiresAccessibilityLabelAndHonorsReduceTransparency() throws {
        let source = try readProjectFile("iosApp/src/Components/WK/WKButtons.swift")
        let circle = source.components(separatedBy: "struct WKCircleButton").last ?? ""
        XCTAssertTrue(circle.contains("let accessibilityLabel: String"))
        XCTAssertTrue(circle.contains("accessibilityReduceTransparency"))
        XCTAssertTrue(circle.contains("#available(iOS 26.0, *)"))
    }
```

- [ ] **Step 2 : Vérifier l'échec**

Run: `-only-testing:WakeveTests/WKComponentsContractTests` → FAIL (fichier introuvable).

- [ ] **Step 3 : Implémenter**

```swift
import SwiftUI

/// Action principale de l'écran. Une seule par écran.
struct WKPrimaryButton: View {
    let title: String
    var systemImage: String? = nil
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Label {
                Text(title)
            } icon: {
                if let systemImage { Image(systemName: systemImage) }
            }
            .labelStyle(.titleAndIcon)
            .font(WK.Typo.headline)
            .foregroundStyle(WK.Colors.onPrimaryButton)
            .frame(maxWidth: .infinity, minHeight: max(WK.Size.primaryButtonHeight, WK.Size.minTapTarget))
            .background(WK.Colors.primaryButton, in: Capsule())
            .contentShape(Capsule())
        }
        .buttonStyle(.plain)
    }
}

/// Suggestion ou filtre.
struct WKChip: View {
    let title: String
    var systemImage: String? = nil
    var isSelected: Bool = false
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: WK.Space.xxs) {
                if let systemImage { Image(systemName: systemImage) }
                Text(title)
            }
            .font(WK.Typo.caption.weight(.medium))
            .foregroundStyle(isSelected ? WK.Colors.onPrimaryButton : WK.Colors.textPrimary)
            .padding(.horizontal, WK.Space.sm)
            .frame(minHeight: WK.Size.minTapTarget)
            .background(isSelected ? WK.Colors.primaryButton : WK.Colors.card, in: Capsule())
            .contentShape(Capsule())
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(isSelected ? .isSelected : [])
    }
}

/// Bouton rond de chrome (retour, réglages, menu). Seul usage du verre avec la barre flottante.
struct WKCircleButton: View {
    let systemImage: String
    let accessibilityLabel: String
    let action: () -> Void

    @Environment(\.accessibilityReduceTransparency) private var reduceTransparency

    var body: some View {
        Button(action: action) {
            Image(systemName: systemImage)
                .font(WK.Typo.headline)
                .foregroundStyle(WK.Colors.textPrimary)
                .frame(width: WK.Size.minTapTarget, height: WK.Size.minTapTarget)
                .modifier(CircleChrome(reduceTransparency: reduceTransparency))
                .contentShape(Circle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(accessibilityLabel)
    }

    private struct CircleChrome: ViewModifier {
        let reduceTransparency: Bool

        func body(content: Content) -> some View {
            if #available(iOS 26.0, *), !reduceTransparency {
                content.glassEffect(.regular.interactive(), in: Circle())
            } else if reduceTransparency {
                content.background(WK.Colors.card, in: Circle())
            } else {
                content.background(.regularMaterial, in: Circle())
            }
        }
    }
}
```

- [ ] **Step 4 : Vérifier le succès**

Run: `-only-testing:WakeveTests/WKComponentsContractTests` → `** TEST SUCCEEDED **`.

- [ ] **Step 5 : Commit**

```bash
git add iosApp/src/Components/WK/WKButtons.swift iosApp/WakeveTests/WKComponentsContractTests.swift
git commit -m "feat(ios): add WK primary button, chip and glass circle button

Refs Swarm DAO #47 (layer 1).

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

### Task 11 : `WKHeroMetric`

**Files:**
- Create: `iosApp/src/Components/WK/WKHeroMetric.swift`
- Test: `iosApp/WakeveTests/WKComponentsContractTests.swift`

- [ ] **Step 1 : Ajouter le test**

```swift
    func testHeroMetricCombinesValueAndCaptionForVoiceOver() throws {
        let source = try readProjectFile("iosApp/src/Components/WK/WKHeroMetric.swift")
        XCTAssertTrue(source.contains("static func accessibilitySummary"))
        XCTAssertTrue(source.contains("WK.Typo.display"))
    }

    func testHeroMetricSummaryReadsNaturally() {
        XCTAssertEqual(
            WKHeroMetric.accessibilitySummary(caption: "Week-end Annecy", value: "5", unit: "/8", subtitle: "votes reçus"),
            "Week-end Annecy, 5/8, votes reçus"
        )
        XCTAssertEqual(
            WKHeroMetric.accessibilitySummary(caption: "Rendez-vous", value: "19:30", unit: nil, subtitle: nil),
            "Rendez-vous, 19:30"
        )
    }
```

- [ ] **Step 2 : Vérifier l'échec**

Run: `-only-testing:WakeveTests/WKComponentsContractTests` → BUILD FAILED (`WKHeroMetric` introuvable).

- [ ] **Step 3 : Implémenter**

```swift
import SwiftUI

/// Grand chiffre en tête d'écran (votes reçus, heure, budget/personne).
struct WKHeroMetric: View {
    let caption: String
    let value: String
    var unit: String? = nil
    var subtitle: String? = nil
    var actionTitle: String? = nil
    var action: (() -> Void)? = nil

    static func accessibilitySummary(caption: String, value: String, unit: String?, subtitle: String?) -> String {
        [caption, value + (unit ?? ""), subtitle].compactMap { $0 }.joined(separator: ", ")
    }

    var body: some View {
        WKCard(padding: WK.Space.md, radius: WK.Radius.lg) {
            VStack(spacing: WK.Space.xxs) {
                Text(caption)
                    .font(WK.Typo.caption)
                    .foregroundStyle(WK.Colors.textSecondary)
                HStack(alignment: .firstTextBaseline, spacing: 2) {
                    Text(value)
                        .font(WK.Typo.display)
                        .foregroundStyle(WK.Colors.textPrimary)
                    if let unit {
                        Text(unit)
                            .font(WK.Typo.title)
                            .foregroundStyle(WK.Colors.textTertiary)
                    }
                }
                .monospacedDigit()
                if let subtitle {
                    Text(subtitle)
                        .font(WK.Typo.caption)
                        .foregroundStyle(WK.Colors.textPrimary)
                }
            }
            .frame(maxWidth: .infinity)
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(Self.accessibilitySummary(caption: caption, value: value, unit: unit, subtitle: subtitle))

            if let actionTitle, let action {
                WKChip(title: actionTitle, isSelected: true, action: action)
                    .frame(maxWidth: .infinity)
                    .padding(.top, WK.Space.xs)
            }
        }
    }
}
```

- [ ] **Step 4 : Vérifier le succès**

Run: `-only-testing:WakeveTests/WKComponentsContractTests` → `** TEST SUCCEEDED **`.

- [ ] **Step 5 : Commit**

```bash
git add iosApp/src/Components/WK/WKHeroMetric.swift iosApp/WakeveTests/WKComponentsContractTests.swift
git commit -m "feat(ios): add WKHeroMetric headline number card

Refs Swarm DAO #47 (layer 1).

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

### Task 12 : `WKActionBar` et `WKModuleTile`

**Files:**
- Create: `iosApp/src/Components/WK/WKActionBar.swift`
- Create: `iosApp/src/Components/WK/WKModuleTile.swift`
- Test: `iosApp/WakeveTests/WKComponentsContractTests.swift`

- [ ] **Step 1 : Ajouter les tests**

```swift
    func testActionBarSecondaryItemsAreLabelledIconButtons() throws {
        let source = try readProjectFile("iosApp/src/Components/WK/WKActionBar.swift")
        XCTAssertTrue(source.contains("struct Item: Identifiable"))
        XCTAssertTrue(source.contains(".accessibilityLabel(item.label)"))
        XCTAssertTrue(source.contains("WK.Size.minTapTarget"))
    }

    func testModuleTileSummaryIsReadWithTitleAndHighlightIsAnnounced() {
        XCTAssertEqual(
            WKModuleTile.accessibilityLabel(title: "Transport", summary: "2 sans place"),
            "Transport, 2 sans place"
        )
        let source = (try? readProjectFile("iosApp/src/Components/WK/WKModuleTile.swift")) ?? ""
        XCTAssertTrue(source.contains(".accessibilityAddTraits(isHighlighted ? .isSelected : [])"))
    }
```

- [ ] **Step 2 : Vérifier l'échec**

Run: `-only-testing:WakeveTests/WKComponentsContractTests` → BUILD FAILED (`WKModuleTile` introuvable).

- [ ] **Step 3 : Implémenter**

`WKActionBar.swift` :

```swift
import SwiftUI

/// Barre d'actions de sheet : une action principale + icônes secondaires groupées.
struct WKActionBar: View {
    struct Item: Identifiable {
        let id = UUID()
        let systemImage: String
        let label: String
        let action: () -> Void
    }

    let primaryTitle: String
    let primaryAction: () -> Void
    var secondary: [Item] = []

    var body: some View {
        HStack(spacing: WK.Space.xs) {
            Button(action: primaryAction) {
                Text(primaryTitle)
                    .font(WK.Typo.headline)
                    .foregroundStyle(WK.Colors.onAccent)
                    .padding(.horizontal, WK.Space.md)
                    .frame(minHeight: WK.Size.minTapTarget)
                    .background(WK.Colors.accent, in: Capsule())
                    .contentShape(Capsule())
            }
            .buttonStyle(.plain)

            if !secondary.isEmpty {
                HStack(spacing: 0) {
                    ForEach(secondary) { item in
                        Button(action: item.action) {
                            Image(systemName: item.systemImage)
                                .font(WK.Typo.body)
                                .foregroundStyle(WK.Colors.textPrimary)
                                .frame(width: WK.Size.minTapTarget, height: WK.Size.minTapTarget)
                                .contentShape(Rectangle())
                        }
                        .buttonStyle(.plain)
                        .accessibilityLabel(item.label)
                    }
                }
                .padding(.horizontal, WK.Space.xxs)
                .background(WK.Colors.cardInset, in: Capsule())
            }
        }
    }
}
```

`WKModuleTile.swift` :

```swift
import SwiftUI

/// Tuile de module du hub d'événement (Date, Lieu, Transport…).
struct WKModuleTile: View {
    let systemImage: String
    let title: String
    let summary: String
    var status: WK.Status? = nil
    var isHighlighted: Bool = false
    let action: () -> Void

    static func accessibilityLabel(title: String, summary: String) -> String {
        "\(title), \(summary)"
    }

    var body: some View {
        Button(action: action) {
            WKCard(style: isHighlighted ? .selected : .standard) {
                VStack(alignment: .leading, spacing: WK.Space.xxs) {
                    Image(systemName: systemImage)
                        .font(WK.Typo.headline)
                        .foregroundStyle(WK.Colors.accent)
                    Text(title)
                        .font(WK.Typo.headline)
                        .foregroundStyle(WK.Colors.textPrimary)
                    Text(summary)
                        .font(WK.Typo.caption)
                        .foregroundStyle(status?.color ?? WK.Colors.textSecondary)
                        .lineLimit(2)
                }
                .frame(maxWidth: .infinity, minHeight: WK.Size.minTapTarget * 2, alignment: .topLeading)
            }
            .contentShape(WK.shape(WK.Radius.md))
        }
        .buttonStyle(.plain)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(Self.accessibilityLabel(title: title, summary: summary))
        .accessibilityAddTraits(.isButton)
        .accessibilityAddTraits(isHighlighted ? .isSelected : [])
    }
}
```

Le `summary` coloré par `status.color` n'est pas le seul porteur d'information (le texte l'est) ; `status.color` sur `card` n'est pas soumis à AA 4.5 car il s'agit d'un renfort — mais ajouter dans `WKTokensTests` une vérification ≥ 3:1 (AA texte large / éléments graphiques) :

```swift
    func testStatusColorOnCardIsAtLeastThreeToOne() {
        for status in WK.Status.allCases {
            for style in [UIUserInterfaceStyle.light, .dark] {
                XCTAssertGreaterThanOrEqual(contrast(status.color, WK.Colors.card, style), 3, "\(status)")
            }
        }
    }
```

- [ ] **Step 4 : Vérifier le succès**

Run: `-only-testing:WakeveTests/WKComponentsContractTests -only-testing:WakeveTests/WKTokensTests`
Expected: `** TEST SUCCEEDED **`. Si `testStatusColorOnCardIsAtLeastThreeToOne` échoue pour `pending` en clair (#B7791F sur blanc ≈ 3.6:1 attendu) ou `draft`, assombrir la valeur fautive de 10 % dans `WK.Status.color` et relancer — ne pas baisser le seuil.

- [ ] **Step 5 : Commit**

```bash
git add iosApp/src/Components/WK/WKActionBar.swift iosApp/src/Components/WK/WKModuleTile.swift iosApp/WakeveTests/WKComponentsContractTests.swift iosApp/WakeveTests/WKTokensTests.swift
git commit -m "feat(ios): add WKActionBar and WKModuleTile

Refs Swarm DAO #47 (layer 1).

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

### Task 13 : `AppZone` et `WKFloatingNavBar`

**Files:**
- Create: `iosApp/src/Models/AppZone.swift`
- Create: `iosApp/src/Components/WK/WKFloatingNavBar.swift`
- Test: `iosApp/WakeveTests/WKComponentsContractTests.swift`

- [ ] **Step 1 : Ajouter les tests**

```swift
    func testAppZoneHasExactlyEventsAndActivity() {
        XCTAssertEqual(AppZone.allCases, [.events, .activity])
        XCTAssertEqual(AppZone.events.systemImage, "calendar")
        XCTAssertEqual(AppZone.activity.systemImage, "bell")
    }

    func testActivityBadgeLabelIsHiddenWhenZero() {
        XCTAssertNil(WKFloatingNavBar.badgeText(for: 0))
        XCTAssertEqual(WKFloatingNavBar.badgeText(for: 3), "3")
        XCTAssertEqual(WKFloatingNavBar.badgeText(for: 120), "99+")
    }

    func testFloatingNavBarUsesGlassWithFallbacks() throws {
        let source = try readProjectFile("iosApp/src/Components/WK/WKFloatingNavBar.swift")
        XCTAssertTrue(source.contains("#available(iOS 26.0, *)"))
        XCTAssertTrue(source.contains(".regularMaterial"))
        XCTAssertTrue(source.contains("accessibilityReduceTransparency"))
        XCTAssertTrue(source.contains("String(localized: \"wk.nav.create\")"))
    }
```

- [ ] **Step 2 : Vérifier l'échec**

Run: `-only-testing:WakeveTests/WKComponentsContractTests` → BUILD FAILED (`AppZone` introuvable).

- [ ] **Step 3 : Implémenter**

`AppZone.swift` :

```swift
import Foundation

/// Zones de premier niveau de la refonte (remplacera WakeveTab en couche 2).
enum AppZone: String, CaseIterable, Identifiable {
    case events
    case activity

    var id: String { rawValue }

    var title: String {
        switch self {
        case .events: return String(localized: "wk.nav.events")
        case .activity: return String(localized: "wk.nav.activity")
        }
    }

    var systemImage: String {
        switch self {
        case .events: return "calendar"
        case .activity: return "bell"
        }
    }
}
```

`WKFloatingNavBar.swift` :

```swift
import SwiftUI

/// Barre flottante 3 zones : Événements · ＋ · Activité.
struct WKFloatingNavBar: View {
    @Binding var selection: AppZone
    var activityBadge: Int = 0
    let onCreate: () -> Void

    @Environment(\.accessibilityReduceTransparency) private var reduceTransparency
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    static func badgeText(for count: Int) -> String? {
        guard count > 0 else { return nil }
        return count > 99 ? "99+" : "\(count)"
    }

    var body: some View {
        HStack(spacing: WK.Space.xs) {
            zoneButton(.events)
            Button(action: onCreate) {
                Image(systemName: "plus")
                    .font(WK.Typo.title)
                    .foregroundStyle(WK.Colors.textPrimary)
                    .frame(width: WK.Size.minTapTarget + WK.Space.md, height: WK.Size.minTapTarget)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel(String(localized: "wk.nav.create"))
            zoneButton(.activity)
        }
        .padding(WK.Space.xxs + 2)  // 6 pt : anneau intérieur de la capsule
        .modifier(BarChrome(reduceTransparency: reduceTransparency))
        .padding(.horizontal, WK.Space.lg)
    }

    private func zoneButton(_ zone: AppZone) -> some View {
        let isSelected = selection == zone
        let badge = zone == .activity ? Self.badgeText(for: activityBadge) : nil
        return Button {
            withAnimation(WK.Motion.snappy(reduceMotion: reduceMotion)) { selection = zone }
        } label: {
            HStack(spacing: WK.Space.xxs) {
                Image(systemName: zone.systemImage)
                if isSelected { Text(zone.title) }
            }
            .font(WK.Typo.caption.weight(.semibold))
            .foregroundStyle(isSelected ? WK.Colors.onAccentFill : WK.Colors.textSecondary)
            .padding(.horizontal, WK.Space.sm)
            .frame(minWidth: WK.Size.minTapTarget, minHeight: WK.Size.minTapTarget)
            .background {
                if isSelected { Capsule().fill(WK.Colors.accentFill) }
            }
            .overlay(alignment: .topTrailing) {
                if let badge {
                    Text(badge)
                        .font(WK.Typo.micro.weight(.bold))
                        .foregroundStyle(WK.Status.actionNeeded.fill)
                        .padding(.horizontal, WK.Space.xxs + 1)
                        .frame(minHeight: WK.Space.md)
                        .background(WK.Status.actionNeeded.onFill, in: Capsule())
                        .offset(x: 4, y: 2)
                        .accessibilityHidden(true)
                }
            }
            .contentShape(Capsule())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(zone.title)
        .accessibilityValue(badge.map { _ in String(format: String(localized: "wk.nav.activity.badge_format"), activityBadge) } ?? "")
        .accessibilityAddTraits(isSelected ? [.isSelected, .isButton] : .isButton)
    }

    private struct BarChrome: ViewModifier {
        let reduceTransparency: Bool

        func body(content: Content) -> some View {
            if #available(iOS 26.0, *), !reduceTransparency {
                content.glassEffect(.regular, in: Capsule())
            } else if reduceTransparency {
                content
                    .background(WK.Colors.card, in: Capsule())
                    .overlay(Capsule().stroke(WK.Colors.cardInset, lineWidth: 1))
            } else {
                content.background(.regularMaterial, in: Capsule())
            }
        }
    }
}
```

Le badge inverse la paire AA `actionNeeded` (`onFill` en fond, `fill` en texte) : contraste ≥ 4.5:1 dans les deux modes, déjà couvert par `testEveryStatusPillMeetsWCAGAAInBothModes`.

- [ ] **Step 4 : Vérifier le succès**

Run: `-only-testing:WakeveTests/WKComponentsContractTests` → `** TEST SUCCEEDED **`.

- [ ] **Step 5 : Commit**

```bash
git add iosApp/src/Models/AppZone.swift iosApp/src/Components/WK/WKFloatingNavBar.swift iosApp/WakeveTests/WKComponentsContractTests.swift
git commit -m "feat(ios): add AppZone and WKFloatingNavBar (glass with fallbacks)

Refs Swarm DAO #47 (layer 1).

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

### Task 14 : Galerie de previews

**Files:**
- Create: `iosApp/src/Components/WK/WKGallery.swift`

- [ ] **Step 1 : Écrire la galerie**

```swift
import SwiftUI

#if DEBUG
/// Galerie de composants WK — à vérifier en clair, sombre, AX5 et Reduce Transparency.
struct WKGallery: View {
    @State private var zone: AppZone = .events

    private let people = [
        WKAvatar(id: "1", name: "Léa Martin"), WKAvatar(id: "2", name: "Tom Durand"),
        WKAvatar(id: "3", name: "Max"), WKAvatar(id: "4", name: "Sam"), WKAvatar(id: "5", name: "Zoé"),
        WKAvatar(id: "6", name: "Ana")
    ]

    var body: some View {
        ZStack(alignment: .bottom) {
            ScrollView {
                VStack(spacing: WK.Space.sm) {
                    HStack {
                        WKCircleButton(systemImage: "chevron.left", accessibilityLabel: "Retour") {}
                        Spacer()
                        WKCircleButton(systemImage: "gearshape", accessibilityLabel: "Réglages") {}
                    }
                    WKHeroMetric(caption: "Prochaine étape · Week-end Annecy", value: "5", unit: "/8",
                                 subtitle: "votes reçus · clôture dans 2 j", actionTitle: "Relancer les 3") {}
                    HStack {
                        ForEach(WK.Status.allCases, id: \.self) { status in
                            WKStatusPill(text: "\(status)", status: status)
                        }
                    }
                    WKAvatarStack(avatars: people)
                    LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: WK.Space.xs) {
                        WKModuleTile(systemImage: "calendar", title: "Date", summary: "Sam. 18 · 6 oui", isHighlighted: true) {}
                        WKModuleTile(systemImage: "mappin", title: "Lieu", summary: "2 options") {}
                        WKModuleTile(systemImage: "car", title: "Transport", summary: "2 sans place", status: .pending) {}
                        WKModuleTile(systemImage: "eurosign", title: "Budget", summary: "~120 € / pers.") {}
                    }
                    WKCard(style: .inset) { Text("Carte inset").font(WK.Typo.body) }
                    HStack {
                        WKChip(title: "Oui", isSelected: true) {}
                        WKChip(title: "Peut-être") {}
                        WKChip(title: "Non") {}
                    }
                    WKActionBar(primaryTitle: "Proposer", primaryAction: {}, secondary: [
                        .init(systemImage: "person.badge.plus", label: "Assigner") {},
                        .init(systemImage: "bubble.left", label: "Commenter") {},
                        .init(systemImage: "square.and.arrow.up", label: "Partager") {}
                    ])
                    WKPrimaryButton(title: "Confirmer le 18 oct") {}
                }
                .padding(WK.Space.screen)
                .padding(.bottom, 96)
            }
            WKFloatingNavBar(selection: $zone, activityBadge: 3) {}
                .padding(.bottom, WK.Space.xs)
        }
        .background(WK.Colors.canvas)
    }
}

#Preview("Clair") { WKGallery() }
#Preview("Sombre") { WKGallery().preferredColorScheme(.dark) }
#Preview("AX5") { WKGallery().dynamicTypeSize(.accessibility5) }

#Preview("Mood immersif") {
    let mood = WK.Mood(palette: .palette(for: .evening))
    return VStack(alignment: .leading, spacing: WK.Space.xs) {
        Text("19:30").font(WK.Typo.display).foregroundStyle(mood.textPrimary)
        Text("Rendez-vous gare d'Annecy").font(WK.Typo.body).foregroundStyle(mood.textPrimary)
        Text("4 arrivés · Léa à 12 min").font(WK.Typo.caption).foregroundStyle(mood.textSecondary)
    }
    .padding(WK.Space.lg)
    .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottomLeading)
    .background(mood.background)
}
#endif
```

- [ ] **Step 2 : Compiler**

```bash
xcodebuild build -project iosApp/iosApp.xcodeproj -scheme WakeveApp \
  -destination 'platform=iOS Simulator,name=iPhone 18 Pro' 2>&1 | tail -5
```
Expected: `** BUILD SUCCEEDED **`.

- [ ] **Step 3 : Revue visuelle**

Ouvrir `WKGallery.swift` dans Xcode, afficher les 4 previews et vérifier : coins continus, aucune troncature bloquante en AX5, barre flottante lisible en sombre (badge compris), cibles ≥ 44 pt (Accessibility Inspector). Corriger toute anomalie dans le composant concerné (jamais dans la galerie) et relancer les tests `WKComponentsContractTests`.

- [ ] **Step 4 : Commit**

```bash
git add iosApp/src/Components/WK/WKGallery.swift
git commit -m "feat(ios): add WK component gallery previews (light, dark, AX5, mood)

Refs Swarm DAO #47 (layer 1).

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

### Task 15 : Garde-fou anti-styles-en-dur (cliquet)

**Files:**
- Test: `iosApp/WakeveTests/WKStyleGuardTests.swift`

- [ ] **Step 1 : Mesurer la baseline réelle après la couche 0**

```bash
cd iosApp/src
echo hex $(grep -rho "Color(hex:" Views | wc -l)
echo font $(grep -rho "\.font(\.system(size:" Views | wc -l)
echo radius $(grep -rhoE "cornerRadius: ?[0-9]" Views | wc -l)
```

Valeurs attendues (≈ avant couche 0 moins les fichiers supprimés) : hex 65, font 87, radius 68. **Utiliser les valeurs réellement mesurées** dans le Step 2.

- [ ] **Step 2 : Écrire le test**

```swift
import XCTest

/// Garde-fou de la refonte (#47) : aucun style en dur dans les composants WK,
/// et les compteurs de Views/ ne peuvent que baisser (cliquet). À la couche 9, baseline = 0.
final class WKStyleGuardTests: XCTestCase {

    private struct Rule {
        let name: String
        let regex: NSRegularExpression
    }

    private let rules: [Rule] = [
        Rule(name: "Color(hex:", regex: try! NSRegularExpression(pattern: #"Color\(hex:"#)),
        Rule(name: ".font(.system(size:", regex: try! NSRegularExpression(pattern: #"\.font\(\.system\(size:"#)),
        Rule(name: "cornerRadius littéral", regex: try! NSRegularExpression(pattern: #"cornerRadius: ?[0-9]"#))
    ]

    /// Baseline mesurée après la couche 0. Baisser ces valeurs quand un écran est migré.
    private let viewsBaseline: [String: Int] = [
        "Color(hex:": 65,
        ".font(.system(size:": 87,
        "cornerRadius littéral": 68
    ]

    private var srcRoot: URL {
        URL(fileURLWithPath: #filePath)
            .deletingLastPathComponent()   // WakeveTests
            .deletingLastPathComponent()   // iosApp
            .appendingPathComponent("src")
    }

    private func count(_ rule: Rule, under folder: String) throws -> Int {
        let root = srcRoot.appendingPathComponent(folder)
        guard let enumerator = FileManager.default.enumerator(at: root, includingPropertiesForKeys: nil) else {
            XCTFail("Dossier introuvable : \(root.path)")
            return 0
        }
        var total = 0
        for case let url as URL in enumerator where url.pathExtension == "swift" {
            let text = try String(contentsOf: url, encoding: .utf8)
            total += rule.regex.numberOfMatches(in: text, range: NSRange(text.startIndex..., in: text))
        }
        return total
    }

    func testWKComponentsHaveNoHardCodedStyles() throws {
        for rule in rules {
            XCTAssertEqual(try count(rule, under: "Components/WK"), 0, "Style en dur interdit dans Components/WK : \(rule.name)")
        }
    }

    func testViewsNeverGainHardCodedStyles() throws {
        for rule in rules {
            let current = try count(rule, under: "Views")
            let baseline = viewsBaseline[rule.name] ?? 0
            XCTAssertLessThanOrEqual(current, baseline, "Views/ : \(rule.name) passe de \(baseline) à \(current). Utiliser les tokens WK.")
            if current < baseline {
                print("ℹ️ WKStyleGuard : \(rule.name) est descendu à \(current) — abaisser la baseline à \(current).")
            }
        }
    }
}
```

- [ ] **Step 3 : Vérifier que le test est rouge si on triche**

Ajouter temporairement une ligne `let _ = Color(hex: "#000000")` dans n'importe quel fichier de `Views/`, lancer :

Run: `-only-testing:WakeveTests/WKStyleGuardTests`
Expected: FAIL « Views/ : Color(hex: passe de 65 à 66 ». Retirer la ligne.

- [ ] **Step 4 : Vérifier le vert**

Run: `-only-testing:WakeveTests/WKStyleGuardTests` → `** TEST SUCCEEDED **`.

- [ ] **Step 5 : Commit**

```bash
git add iosApp/WakeveTests/WKStyleGuardTests.swift
git commit -m "test(ios): add ratchet guard against hard-coded styles

Components/WK must stay at zero; Views/ counts can only go down.
Refs Swarm DAO #47 (layer 1).

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

### Task 16 : Vérification finale, spec et gouvernance

**Files:**
- Modify: `docs/superpowers/specs/2026-09-28-ios-redesign-design.md`

- [ ] **Step 1 : Suite complète**

Run: `-only-testing:WakeveTests` (suite complète).
Expected: `** TEST SUCCEEDED **`, hors échecs préexistants notés avant la Task 1.

- [ ] **Step 2 : Reporter les écarts dans la spec**

Dans la section 6 de la spec, remplacer la phrase « En mode sombre, `fill` = couleur à 22 % d'opacité sur `card`… » par : « En mode sombre, valeurs explicites définies dans `Theme/WK.swift` (`WK.Status`), contraste AA vérifié par `WKTokensTests`. » Ajouter `onAccent` et `onAccentFill` au tableau des tokens. Dans la section 9, remplacer « règle SwiftLint (avertissement) » par « test de contrat `WKStyleGuardTests` à cliquet » et la dernière phrase de la section par : « Garde-fou : `WKStyleGuardTests` (XCTest) — zéro style en dur dans `Components/WK`, compteurs de `Views/` à cliquet, baseline ramenée à 0 à la couche 9. » Dans la section 11, critère 4, remplacer « (règle SwiftLint en erreur) » par « (`WKStyleGuardTests` avec baseline 0) ». Préciser en section 9 que `WKModuleSheet` arrive en couche 5 et `WKImmersiveScaffold` en couche 8.

- [ ] **Step 3 : Commit**

```bash
git add docs/superpowers/specs/2026-09-28-ios-redesign-design.md
git commit -m "docs(ios): align redesign spec with layer 0-1 implementation choices

Refs Swarm DAO #47.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

- [ ] **Step 4 : Revue**

Demander une revue (`superpowers:requesting-code-review`) sur l'ensemble des commits des couches 0-1 : conformité à la spec, accessibilité, absence de régression.

---

## Plans suivants (un plan par couche, rédigé au démarrage de la couche)

| Couche | Plan à écrire | Pré-requis |
|---|---|---|
| 2 | `…-ios-redesign-layer-2-shell.md` : `AppRouter` (tests exhaustifs sur toutes les `IosRoute`), `NavigationStack`, `WKFloatingNavBar` branchée, flag `redesign2026` via `FeatureFlags` | Couches 0-1 |
| 3 | `…-layer-3-home.md` : `EventsHomeView`, dérivation statut → `WK.Status` + « qui doit agir », tri | 2 |
| 4 | `…-layer-4-hub.md` : `EventHubView`, modules par `EventStatus` | 3 |
| 5 | `…-layer-5-modules.md` : `WKModuleSheet` + un module par PR | 4 |
| 6 | `…-layer-6-activity.md` | 2 |
| 7 | `…-layer-7-create.md` | 2 |
| 8 | `…-layer-8-immersive.md` : `WKImmersiveScaffold` | 4 |
| 9 | `…-layer-9-switch.md` : bascule, suppression legacy, baselines à 0 | 3-8 |
