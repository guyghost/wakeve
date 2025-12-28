# LiquidGlassCard Component Reference

**Location:** `iosApp/iosApp/Components/LiquidGlassCard.swift`

## Overview

`LiquidGlassCard` is a reusable SwiftUI component that implements Apple's Liquid Glass design system. It provides a flexible, easy-to-use interface for creating glass-effect cards with multiple style options.

## Features

- ✅ **4 Glass Styles**: Regular, Thin, Ultra Thin, Thick
- ✅ **Automatic Styling**: Default corner radius, padding, and shadows per style
- ✅ **Continuous Corners**: Uses `.continuous` style for smooth Apple-native appearance
- ✅ **Customizable**: Full control over all parameters
- ✅ **Lightweight**: Minimal performance overhead
- ✅ **Accessible**: Proper contrast and legibility

## Usage Examples

### Basic Regular Card
```swift
LiquidGlassCard {
    Text("Hello, Liquid Glass!")
        .font(.headline)
}
```

### Thin Card (Subtle)
```swift
LiquidGlassCard.thin {
    HStack {
        Text("Secondary Information")
        Spacer()
        Image(systemName: "arrow.right")
    }
}
```

### Ultra Thin Card (Very Subtle)
```swift
LiquidGlassCard.ultraThin {
    Label("Info", systemImage: "info.circle.fill")
}
```

### Thick Card (Prominent)
```swift
LiquidGlassCard.thick {
    VStack(spacing: 12) {
        Image(systemName: "star.fill")
            .font(.title)
        Text("Featured Event")
    }
}
```

### Custom Configuration
```swift
LiquidGlassCard(
    style: .regular,
    cornerRadius: 24,
    padding: 20,
    shadow: true
) {
    // Your content
}
```

## Styles Reference

### `.regular` (Default)
- **Material**: `.regularMaterial`
- **Corner Radius**: 16pt (default)
- **Padding**: 16pt (default)
- **Shadow**: Yes (opacity: 0.05, radius: 8)
- **Use Case**: Standard cards, list items

### `.thin`
- **Material**: `.thinMaterial`
- **Corner Radius**: 16pt (default)
- **Padding**: 16pt (default)
- **Shadow**: No
- **Use Case**: Secondary cards, subtle backgrounds

### `.ultraThin`
- **Material**: `.ultraThinMaterial`
- **Corner Radius**: 12pt (default)
- **Padding**: 12pt (default)
- **Shadow**: No
- **Use Case**: Very subtle separation, background overlays

### `.thick`
- **Material**: `.thickMaterial`
- **Corner Radius**: 20pt (default)
- **Padding**: 20pt (default)
- **Shadow**: Yes (opacity: 0.08, radius: 12)
- **Use Case**: Elevated cards, modals, hero sections

## Parameters

### Style Enum
```swift
enum GlassStyle {
    case regular
    case thin
    case ultraThin
    case thick
}
```

### Initialization
```swift
init(
    style: GlassStyle = .regular,
    cornerRadius: CGFloat? = nil,
    padding: CGFloat = 16,
    shadow: Bool? = nil,
    @ViewBuilder content: () -> Content
)
```

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `style` | `GlassStyle` | `.regular` | Glass material style |
| `cornerRadius` | `CGFloat?` | Computed per style | Custom corner radius |
| `padding` | `CGFloat` | 16 | Internal content padding |
| `shadow` | `Bool?` | Computed per style | Enable shadow |
| `content` | `@ViewBuilder` | Required | Card content |

## Design Compliance

✅ **Apple HIG Liquid Glass**
- Uses only native materials (`.regularMaterial`, `.thinMaterial`, etc.)
- Continuous corners (`.continuous` style)
- Subtle shadows (0.05-0.08 opacity)
- Proper spacing (12-20pt padding)

✅ **Wakeve Design System**
- Consistent corner radius (12-20pt)
- Material-based elevation
- Shadow properties aligned with Material You
- Cross-platform adaptability

## Migration from ViewExtensions

Previous extensions (`.glassCard()`, `.thinGlass()`, etc.) are still available in `ViewExtensions.swift` but should be replaced with `LiquidGlassCard` for consistency:

### Before
```swift
VStack {
    Text("Old style")
}
.glassCard()
```

### After
```swift
LiquidGlassCard {
    Text("New style")
}
```

## Performance Notes

- Minimal overhead: Single `background()` + `clipShape()` + optional `shadow()`
- Safe to use in lists with `LazyVStack`
- No animation overhead unless modifier is applied

## Accessibility

- No special accessibility considerations
- Content inside card maintains native accessibility
- Continuous corners prevent edge clipping issues
- Shadow doesn't interfere with contrast ratios

## Testing

Preview included in the file (`LiquidGlassCard_Previews`) with all 4 styles and custom variants.

```bash
# Build and preview in Xcode
open iosApp/iosApp.xcodeproj
# Select Canvas Preview for LiquidGlassCard
```

## Related Files

- `iosApp/iosApp/Theme/LiquidGlassModifier.swift` - Legacy modifier
- `iosApp/iosApp/Extensions/ViewExtensions.swift` - Legacy extensions
- `iosApp/LIQUID_GLASS_GUIDELINES.md` - Design guidelines
- `.opencode/design-system.md` - System-wide design tokens

## Future Enhancements

- [ ] Animation builders (enter/exit)
- [ ] Gradient overlays
- [ ] Border options
- [ ] Background color customization
- [ ] Size variants (compact, standard, large)

---

**Last Updated:** December 28, 2025  
**Version:** 1.0.0  
**Status:** ✅ Production Ready
