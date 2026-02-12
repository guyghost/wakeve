# Liquid Glass Design System - iOS Components

## Overview

This document describes the standardized Liquid Glass UI components created for the Wakeve iOS app. These components follow the Liquid Glass design language introduced in iOS 18 and provide consistent styling across the application.

## Components

### 1. LiquidGlassCard

Standardized card component with Liquid Glass effect.

**Usage:**
```swift
LiquidGlassCard(cornerRadius: 16, padding: 16) {
    VStack(alignment: .leading, spacing: 12) {
        Text("Title")
            .font(.headline)
    }
}
```

**Parameters:**
- `cornerRadius`: Corner radius (default: 16)
- `padding`: Internal padding (default: 16)
- `opacity`: Background opacity (default: 0.8)
- `intensity`: Effect intensity (default: 1.0)

---

### 2. LiquidGlassButton

Standardized button component with three styles.

**Usage:**
```swift
// Primary button (gradient background)
LiquidGlassButton(title: "Confirmer", style: .primary) {
    confirmAction()
}

// Secondary button (outline style)
LiquidGlassButton(title: "Annuler", style: .secondary) {
    cancelAction()
}

// Text button (transparent)
LiquidGlassButton(title: "En savoir plus", style: .text) {
    moreInfo()
}
```

**Styles:**
- `.primary`: Gradient background (wakevPrimary to wakevAccent)
- `.secondary`: Outline style with liquid glass
- `.text`: Transparent background, just text

---

### 3. LiquidGlassIconButton

Icon button variant for floating action buttons and small actions.

**Usage:**
```swift
// FAB style
LiquidGlassIconButton(icon: "plus", size: 56) {
    createEvent()
}

// Small icon button
LiquidGlassIconButton(icon: "checkmark", size: 44) {
    confirmAction()
}

// Custom colors
LiquidGlassIconButton(
    icon: "star.fill",
    size: 44,
    gradientColors: [.purple, .pink]
) {
    favoriteAction()
}
```

**Parameters:**
- `icon`: SF Symbol name
- `size`: Button size (default: 56)
- `gradientColors`: Array of two colors for gradient (default: wakevPrimary, wakevAccent)

---

### 4. LiquidGlassBadge

Badge component for status indicators.

**Usage:**
```swift
// Basic badge
LiquidGlassBadge(text: "Confirmé", style: .success)

// Badge with icon
LiquidGlassBadge(
    text: "Sondage",
    icon: "chart.bar.fill",
    style: .info
)
```

**Styles:**
- `.default`: Neutral gray
- `.success`: Green (confirmed/finalized)
- `.warning`: Yellow/Orange (draft/polling)
- `.info`: Blue (polling/in-progress)
- `.accent`: Purple (comparing/special)

**Convenience Methods:**
```swift
LiquidGlassBadge.draft()
LiquidGlassBadge.polling()
LiquidGlassBadge.comparing()
LiquidGlassBadge.confirmed()
LiquidGlassBadge.organizing()
LiquidGlassBadge.finalized()

// From MockEventStatus
LiquidGlassBadge.from(status: event.status)
```

---

### 5. LiquidGlassDivider

Divider component with subtle Liquid Glass effect.

**Usage:**
```swift
// Default divider
LiquidGlassDivider()

// Subtle divider
LiquidGlassDivider(style: .subtle)

// Prominent divider
LiquidGlassDivider(style: .prominent)

// Vertical divider
LiquidGlassDivider(orientation: .vertical)
```

**Styles:**
- `.subtle`: Very thin, low opacity (0.5pt)
- `.default`: Standard thickness (1pt)
- `.prominent`: Thicker, more visible (2pt)

---

### 6. LiquidGlassListItem

List item component with Liquid Glass effect.

**Usage:**
```swift
// Basic list item
LiquidGlassListItem(
    title: "Event Title",
    subtitle: "Event description",
    icon: "calendar",
    iconColor: .blue
) {
    Text("Additional content")
        .font(.caption)
}

// With trailing content
LiquidGlassListItem(
    title: "Event with Badge",
    icon: "checkmark.circle",
    iconColor: .green
) {
    EmptyView()
} trailing: {
    LiquidGlassBadge(text: "Confirmé", style: .success)
}
```

**Styles:**
- `.default`: Standard spacing (16pt padding, 16pt corner radius)
- `.prominent`: Larger spacing (20pt padding, 20pt corner radius)
- `.compact`: Tighter spacing (12pt padding, 12pt corner radius)

---

## Design System Colors

The components use the following design system colors:

| Color | Usage |
|-------|-------|
| `.wakevPrimary` | Primary brand color |
| `.wakevAccent` | Accent/secondary color |
| `.wakevSuccess` | Success states (confirmed, finalized) |
| `.wakevWarning` | Warning states (draft, organizing) |
| `.wakevError` | Error states |

---

## File Structure

```
iosApp/
└── iosApp/
    ├── Theme/
    │   ├── WakeveColors.swift        # Color definitions
    │   └── LiquidGlassModifier.swift # Base liquid glass modifier
    └── UIComponents/
        ├── LiquidGlassCard.swift    # Card component
        ├── LiquidGlassButton.swift  # Button components (primary, secondary, text, icon)
        ├── LiquidGlassBadge.swift   # Badge component with convenience methods
        ├── LiquidGlassDivider.swift # Divider component
        └── LiquidGlassListItem.swift # List item component
```

---

## Migration Guide

### Replacing `.glassCard()`

**Before:**
```swift
VStack { ... }
.padding(16)
.glassCard(cornerRadius: 16, material: .regularMaterial)
```

**After:**
```swift
LiquidGlassCard(cornerRadius: 16, padding: 16) {
    VStack { ... }
}
```

### Replacing Custom Status Badges

**Before:**
```swift
HStack(spacing: 4) {
    Circle()
        .fill(statusColor)
        .frame(width: 8, height: 8)
    Text(statusText)
        .font(.caption2.weight(.medium))
        .foregroundColor(statusColor)
}
.padding(.horizontal, 8)
.padding(.vertical, 4)
.ultraThinGlass(cornerRadius: 12)
```

**After:**
```swift
LiquidGlassBadge.from(status: event.status)
```

### Replacing Custom FAB

**Before:**
```swift
Button {
    showEventCreationSheet = true
} label: {
    Image(systemName: "plus")
        .font(.system(size: 20, weight: .semibold))
        .foregroundColor(.white)
        .frame(width: 56, height: 56)
        .background(
            Circle()
                .fill(
                    LinearGradient(...)
                )
        )
        .shadow(...)
}
```

**After:**
```swift
LiquidGlassIconButton(icon: "plus") {
    showEventCreationSheet = true
}
```

---

## Accessibility

All components maintain accessibility support:

- `accessibilityLabel`: Descriptive label for VoiceOver
- `accessibilityHint`: Additional guidance for actions
- Proper hit areas (minimum 44×44pt for tappable elements)
- Semantic colors for color-blind friendly status indicators

---

## Preview Support

All components include SwiftUI previews for development:

```bash
# Open in Xcode canvas
# Cmd + Option + Enter to show preview
```

---

## Best Practices

1. **Consistency**: Use standardized components instead of custom implementations
2. **Accessibility**: Maintain accessibility labels and hints
3. **Composition**: Combine components for complex UIs
4. **Styling**: Use design system colors instead of hardcoded values
5. **Testing**: Verify components render correctly in light and dark modes
