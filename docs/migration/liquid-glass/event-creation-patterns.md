# 🎨 EventCreationSheet - Liquid Glass Implementation Patterns

**Document**: Migration patterns and best practices for Liquid Glass in EventCreationSheet  
**Date**: 28 décembre 2025  
**Status**: ✅ Complete  

## 📖 Table of Contents

1. [Material Hierarchy](#material-hierarchy)
2. [Pattern Implementation](#pattern-implementation)
3. [Shadow Strategy](#shadow-strategy)
4. [Corner Radius Guidelines](#corner-radius-guidelines)
5. [Color Integration](#color-integration)
6. [Code Examples](#code-examples)
7. [Best Practices](#best-practices)

---

## Material Hierarchy

### Overview

EventCreationSheet uses a two-tier material hierarchy for optimal visual depth and interaction feedback:

```
Tier 1: Primary Content Containers
├── FormCardModifier: .regularMaterial
│   └── Form rows, input sections
│   └── Emphasis: Medium (main content areas)
│   └── Shadow: Subtle (0.05 opacity, 6pt radius)
│
└── QuickEventCreationSheet Input: .regularMaterial
    └── Quick event title input
    └── Emphasis: Medium
    └── Shadow: Subtle (0.05 opacity, 6pt radius)

Tier 2: Interactive Elements
├── Date Buttons (normal): .ultraThinMaterial
│   └── Less emphasis, glass effect
│   └── Shadow: Very subtle (0.03 opacity, 2pt radius)
│
└── Date Buttons (selected): Color.blue.opacity(0.15) + border
    └── Higher emphasis during interaction
    └── Shadow: Medium (0.08 opacity, 4pt radius)
    └── Border: Blue stroke (0.5 opacity)
```

### Why This Hierarchy?

- **Tier 1** (.regularMaterial): Establish clear content areas with enough opacity for readability
- **Tier 2** (.ultraThinMaterial): Subtle, interactive elements that stand out on action
- **Progression**: Visual feedback increases with interaction intensity

---

## Pattern Implementation

### Pattern 1: Form Card with Shadows

**Use Case**: Grouped form sections (title, location, notes)

```swift
// ✅ PATTERN: FormCardModifier
struct FormCardModifier: ViewModifier {
    func body(content: Content) -> some View {
        content
            .background(.regularMaterial)  // Native Liquid Glass
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
            .shadow(color: .black.opacity(0.05), radius: 6, x: 0, y: 3)
    }
}

// Usage:
VStack(spacing: 0) {
    FormRow { TextField("Titre", text: $eventTitle) }
    FormRow { TextField("Lieu", text: $location) }
    FormRow(showSeparator: false) { TextField("Notes", text: $description) }
}
.formCard()  // Applies FormCardModifier
```

**Key Points:**
- `.regularMaterial` auto-adapts to dark/light mode
- Continuous corners for Apple consistency
- Subtle shadow (0.05 opacity) for depth without heaviness
- Shadow offset: (0, 3) for soft, top-aligned effect

---

### Pattern 2: Interactive Button with State Feedback

**Use Case**: Date/Time buttons with visual feedback

```swift
// ✅ PATTERN: Adaptive Button States
Button {
    withAnimation(.easeInOut(duration: 0.2)) {
        showDatePicker.toggle()
    }
} label: {
    Text(date, format: .dateTime.day().month().year())
        .font(.body)
        .padding(.horizontal, 12)
        .padding(.vertical, 6)
        .background(
            showDatePicker 
                ? Color.blue.opacity(0.15)      // Selected: opaque blue
                : .ultraThinMaterial            // Normal: glass effect
        )
        .overlay(
            RoundedRectangle(cornerRadius: 6, style: .continuous)
                .stroke(Color.blue.opacity(showDatePicker ? 0.5 : 0), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 6, style: .continuous))
        .foregroundStyle(Color.blue)
        .shadow(
            color: .black.opacity(showDatePicker ? 0.08 : 0.03),
            radius: showDatePicker ? 4 : 2,
            x: 0,
            y: 2
        )
}
```

**State Transitions:**

```
NORMAL STATE                          SELECTED STATE
┌──────────────────┐                ┌──────────────────┐
│ .ultraThinMat ▫▫ │                │ Blue.opacity(15%)│
│ Shadow: 0.03     │                │ Border: Blue     │
│ Radius: 2        │    click       │ Shadow: 0.08     │
│                  │       ──→       │ Radius: 4        │
└──────────────────┘                └──────────────────┘
```

**Key Points:**
- `.ultraThinMaterial` by default (minimum visual weight)
- State changes trigger shadow and border updates
- Smooth animation (0.2s) on toggle
- Blue border appears only when selected
- Shadow increases on interaction (visual feedback)

---

### Pattern 3: Input Card with Contained Shadow

**Use Case**: Quick event creation input (standalone card)

```swift
// ✅ PATTERN: Contained Shadow (RoundedRectangle with background)
TextField("Nom de l'événement", text: $eventTitle)
    .font(.title3)
    .multilineTextAlignment(.center)
    .padding()
    .background(
        RoundedRectangle(cornerRadius: 12, style: .continuous)
            .fill(.regularMaterial)
            .shadow(color: .black.opacity(0.05), radius: 6, x: 0, y: 3)
    )
    .padding(.horizontal, 32)
```

**Why Contained Shadow?**

- Applies shadow to the RoundedRectangle background shape
- Ensures shadow stays within the card bounds
- Better visual control than top-level shadow
- Consistent with design system expectations

**Alternative (less preferred):**
```swift
// ❌ Avoid: top-level shadow (may extend beyond bounds)
.shadow(color: .black.opacity(0.05), radius: 6, x: 0, y: 3)
```

---

## Shadow Strategy

### Shadow Opacity Scale

Based on visual weight and interaction:

```
Level   | Opacity | Radius | Y-Offset | Use Case              | Example
--------|---------|--------|----------|----------------------|-----------
Very Light | 0.02   | 2      | 1        | Text hover           | Optional
Light      | 0.03   | 2      | 2        | Normal state         | Date btn idle
Medium     | 0.05   | 6      | 3        | Card surface         | Form cards
Heavy      | 0.08   | 4      | 2        | Interactive/selected | Date btn active
Prominent  | 0.12   | 12     | 6        | Modal elevation      | Not used here
```

### Applied Shadows in EventCreationSheet

| Component | Normal | Selected | Justification |
|-----------|--------|----------|---------------|
| **Form cards** | 0.05, r:6 | — | Content areas need subtle depth |
| **Date buttons** | 0.03, r:2 | 0.08, r:4 | Interaction feedback |
| **Quick input** | 0.05, r:6 | — | Standalone card needs definition |

### Shadow Implementation Rules

1. **Always specify x: 0** (no horizontal offset)
2. **Y-offset matches radius trend** (larger radius → larger offset)
3. **Opacity < 0.15** (subtlety over prominence)
4. **Adaptive shadows** (change on interaction)

```swift
// ✅ GOOD: Consistent shadow pattern
.shadow(color: .black.opacity(0.05), radius: 6, x: 0, y: 3)

// ❌ AVOID: Too heavy
.shadow(color: .black.opacity(0.25), radius: 12, x: 2, y: 6)

// ❌ AVOID: Horizontal offset (not iOS style)
.shadow(color: .black.opacity(0.05), radius: 6, x: 3, y: 3)
```

---

## Corner Radius Guidelines

### Continuous Corners (Apple Standard)

All components use `.continuous` style for smooth, rounded appearance:

```swift
// ✅ CORRECT: Continuous corners
.clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))

// ❌ WRONG: Default rounded corners (old iOS style)
.clipShape(RoundedRectangle(cornerRadius: 12))
```

### Radius Scale in EventCreationSheet

| Component | Radius | Style | Notes |
|-----------|--------|-------|-------|
| **Form cards** | 12pt | continuous | Primary surface |
| **Date buttons** | 6pt | continuous | Secondary interactive |
| **Input cards** | 12pt | continuous | Standalone surface |
| **Quick input** | 12pt | continuous | Primary input |

### Why Continuous?

- **Visual Smoothness**: Subtle curves without sharp angles
- **Apple Consistency**: Matches iOS 14+ design language
- **Accessibility**: Better visual flow for all users
- **Material Depth**: Complements glass effect

---

## Color Integration

### System Colors vs Custom Colors

| Color Type | Usage | Status |
|-----------|-------|--------|
| **System Materials** | Card backgrounds | ✅ Used (`.regularMaterial`, `.ultraThinMaterial`) |
| **System Separators** | Form row dividers | ✅ Used (`Color(uiColor: .separator)`) |
| **System Labels** | Secondary text | ✅ Used (`Color(uiColor: .secondaryLabel)`) |
| **Custom hardcoded** | — | ❌ Removed/replaced |
| **Semantic colors** | Primary actions | ✅ Used (`.iOSSystemBlue`, `.iOSSystemGreen`) |

### Color Properties (Maintained)

```swift
private var backgroundColor: Color {
    colorScheme == .dark ? Color(uiColor: .systemGroupedBackground) : Color(uiColor: .systemGroupedBackground)
}
// → Used for main ScrollView background (.ignoresSafeArea)

private var separatorColor: Color {
    colorScheme == .dark ? .iOSDarkSeparator : Color(uiColor: .separator)
}
// → Used in FormRow for row dividers

private var secondaryLabelColor: Color {
    colorScheme == .dark ? .iOSSecondaryLabel : Color(uiColor: .secondaryLabel)
}
// → Used for icons and secondary text
```

### Why Maintain These?

1. **backgroundColor**: Essential for safe area background
2. **separatorColor**: Platform-standard FormRow pattern
3. **secondaryLabelColor**: Icon and secondary text styling

---

## Code Examples

### Complete Pattern: Form Row with Card

```swift
// Combining: FormRow + FormCardModifier + separators
private var basicInfoSection: some View {
    VStack(spacing: 0) {
        FormRow(showSeparator: true) {
            TextField("Titre", text: $eventTitle)
                .font(.body)
                .accessibilityLabel("Titre de l'événement")
        }
        
        FormRow(showSeparator: true) {
            HStack(spacing: 12) {
                Image(systemName: "location.fill")
                    .foregroundStyle(secondaryLabelColor)  // Icon color
                
                TextField("Lieu", text: $location)
                    .font(.body)
            }
        }
        
        FormRow(showSeparator: false) {  // Last row, no separator
            TextField("Notes", text: $description, axis: .vertical)
                .font(.body)
                .lineLimit(1...4)
        }
    }
    .formCard()  // Applies FormCardModifier
}

// Result: 
// - 3 rows in a single .regularMaterial card
// - Subtle separators between rows
// - 12pt continuous corners on card
// - Soft shadow for depth
```

### Complete Pattern: Interactive Date Button

```swift
// Combining: State binding + adaptive materials + shadows
DateTimeButtons(
    date: $startDate,
    showDatePicker: $showStartDatePicker,
    isAllDay: isAllDay
)

// Implementation:
struct DateTimeButtons: View {
    @Binding var date: Date
    @Binding var showDatePicker: Bool
    let isAllDay: Bool
    
    var body: some View {
        HStack(spacing: 8) {
            Button {
                withAnimation(.easeInOut(duration: 0.2)) {
                    showDatePicker.toggle()  // Smooth toggle
                }
            } label: {
                Text(date, format: .dateTime.day().month().year())
                    .font(.body)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 6)
                    .background(
                        showDatePicker 
                            ? Color.blue.opacity(0.15)       // Selected
                            : .ultraThinMaterial            // Normal
                    )
                    .overlay(
                        RoundedRectangle(cornerRadius: 6, style: .continuous)
                            .stroke(Color.blue.opacity(showDatePicker ? 0.5 : 0), lineWidth: 1)
                    )
                    .clipShape(RoundedRectangle(cornerRadius: 6, style: .continuous))
                    .foregroundStyle(Color.blue)
                    .shadow(
                        color: .black.opacity(showDatePicker ? 0.08 : 0.03),
                        radius: showDatePicker ? 4 : 2,
                        x: 0,
                        y: 2
                    )
            }
            
            // Time button (similar pattern, omitted for brevity)
        }
    }
}
```

---

## Best Practices

### ✅ DO

1. **Always use `.continuous` corner style**
   ```swift
   .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
   ```

2. **Use native materials for backgrounds**
   ```swift
   .background(.regularMaterial)      // Not Color(#1C1C1E)
   .background(.ultraThinMaterial)    // Not Color.gray.opacity(0.1)
   ```

3. **Apply shadows subtly**
   ```swift
   .shadow(color: .black.opacity(0.05), radius: 6, x: 0, y: 3)
   ```

4. **Animate state changes smoothly**
   ```swift
   withAnimation(.easeInOut(duration: 0.2)) {
       showDatePicker.toggle()
   }
   ```

5. **Use system colors for labels/text**
   ```swift
   Color(uiColor: .secondaryLabel)     // Not hardcoded gray
   Color(uiColor: .separator)          // Not hardcoded divider
   ```

6. **Maintain material hierarchy**
   - Primary content: `.regularMaterial`
   - Secondary/interactive: `.ultraThinMaterial`
   - Emphasis: Darker colors or borders

### ❌ DON'T

1. **Don't hardcode colors for materials**
   ```swift
   ❌ .background(Color(uiColor: .secondarySystemGroupedBackground))
   ✅ .background(.regularMaterial)
   ```

2. **Don't use rounded (non-continuous) corners**
   ```swift
   ❌ RoundedRectangle(cornerRadius: 12)
   ✅ RoundedRectangle(cornerRadius: 12, style: .continuous)
   ```

3. **Don't apply heavy shadows**
   ```swift
   ❌ .shadow(color: .black.opacity(0.25), radius: 16, x: 4, y: 8)
   ✅ .shadow(color: .black.opacity(0.05), radius: 6, x: 0, y: 3)
   ```

4. **Don't forget material adaptivity**
   ```swift
   ❌ Always use .regularMaterial (heavy)
   ✅ Use .ultraThinMaterial for secondary elements
   ```

5. **Don't mix design approaches**
   ```swift
   ❌ Some components with materials, others with hardcoded colors
   ✅ Consistent material hierarchy throughout
   ```

6. **Don't forget accessibility labels**
   ```swift
   ❌ TextField("Email")
   ✅ TextField("Email").accessibilityLabel("Email address field")
   ```

---

## Testing Checklist

- [ ] Verify materials in light mode
- [ ] Verify materials in dark mode
- [ ] Check shadows on different devices
- [ ] Test interaction animations (smooth?)
- [ ] Verify accessibility labels
- [ ] Test on iPhone 14+ (consistent rendering)
- [ ] Check form input responsiveness
- [ ] Validate color contrast (WCAG AA)

---

## References

- **Liquid Glass Guidelines**: `iosApp/LIQUID_GLASS_GUIDELINES.md`
- **Design System**: `.opencode/design-system.md`
- **Extensions**: `iosApp/iosApp/Extensions/ViewExtensions.swift`
- **Apple HIG**: [Human Interface Guidelines - Materials](https://developer.apple.com/design/human-interface-guidelines/materials)

---

**Last Updated**: 28 December 2025  
**Status**: ✅ Complete and validated
