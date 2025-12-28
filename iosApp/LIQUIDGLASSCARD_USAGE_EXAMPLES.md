# LiquidGlassCard - Usage Examples

Exemples pratiques d'utilisation du composant `LiquidGlassCard` dans les vues iOS.

## Table of Contents
1. [Basic Cards](#basic-cards)
2. [Event Cards](#event-cards)
3. [Form Elements](#form-elements)
4. [Status & State](#status--state)
5. [Complex Layouts](#complex-layouts)
6. [Best Practices](#best-practices)

---

## Basic Cards

### Simple Text Card
```swift
LiquidGlassCard {
    VStack(alignment: .leading, spacing: 8) {
        Text("Welcome to Wakeve")
            .font(.headline)
        Text("Plan events with your friends")
            .font(.caption)
            .foregroundColor(.secondary)
    }
}
```

### Card with Icon
```swift
LiquidGlassCard {
    HStack(spacing: 12) {
        Image(systemName: "calendar")
            .font(.title3)
            .foregroundColor(.blue)
        VStack(alignment: .leading, spacing: 4) {
            Text("Next Event")
                .font(.headline)
            Text("December 28, 2025")
                .font(.caption)
                .foregroundColor(.secondary)
        }
        Spacer()
    }
}
```

### Card with Action Button
```swift
LiquidGlassCard {
    VStack(spacing: 12) {
        Text("Create a new event")
            .font(.headline)
        Button(action: {}) {
            HStack {
                Image(systemName: "plus")
                Text("New Event")
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 8)
            .background(Color.blue)
            .foregroundColor(.white)
            .cornerRadius(8)
        }
    }
}
```

---

## Event Cards

### Event Summary Card
```swift
LiquidGlassCard.regular {
    VStack(alignment: .leading, spacing: 12) {
        // Header
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                Text("Summer Camping Trip")
                    .font(.headline)
                Text("Group Event • 8 participants")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            Spacer()
            Image(systemName: "tent.2")
                .font(.title3)
                .foregroundColor(.blue)
        }
        
        Divider()
        
        // Details
        HStack(spacing: 20) {
            VStack(alignment: .leading, spacing: 4) {
                Text("Date")
                    .font(.caption)
                    .foregroundColor(.secondary)
                Text("July 15-22")
                    .font(.subheadline)
                    .fontWeight(.semibold)
            }
            
            VStack(alignment: .leading, spacing: 4) {
                Text("Status")
                    .font(.caption)
                    .foregroundColor(.secondary)
                Text("Confirmed")
                    .font(.subheadline)
                    .fontWeight(.semibold)
                    .foregroundColor(.green)
            }
        }
    }
}
```

### Event Card with Poll
```swift
LiquidGlassCard {
    VStack(alignment: .leading, spacing: 12) {
        Text("Vote on the date")
            .font(.headline)
        
        VStack(spacing: 8) {
            ForEach(["July 15-22", "August 5-12", "September 1-8"], id: \.self) { date in
                HStack {
                    Text(date)
                        .font(.subheadline)
                    Spacer()
                    Text("✓ 6 votes")
                        .font(.caption)
                        .foregroundColor(.green)
                }
                .padding(10)
                .background(Color(.systemGray6))
                .cornerRadius(8)
            }
        }
        
        Button(action: {}) {
            Text("View Results")
                .font(.caption)
                .foregroundColor(.blue)
        }
    }
}
```

---

## Form Elements

### Input Card
```swift
LiquidGlassCard {
    VStack(alignment: .leading, spacing: 8) {
        Text("Event Title")
            .font(.caption)
            .foregroundColor(.secondary)
        TextField("Enter event title", text: .constant(""))
            .textFieldStyle(.roundedBorder)
    }
}
```

### Section Header Card
```swift
LiquidGlassCard.thin {
    HStack {
        Text("Participants")
            .font(.headline)
        Spacer()
        Text("8 people")
            .font(.caption)
            .foregroundColor(.secondary)
    }
}
```

### Settings Option
```swift
LiquidGlassCard {
    HStack {
        VStack(alignment: .leading, spacing: 4) {
            Text("Enable Notifications")
                .font(.subheadline)
            Text("Get notified of event updates")
                .font(.caption2)
                .foregroundColor(.secondary)
        }
        Spacer()
        Toggle("", isOn: .constant(true))
    }
}
```

---

## Status & State

### Status Badge Card
```swift
LiquidGlassCard.ultraThin {
    HStack(spacing: 8) {
        Circle()
            .fill(Color.green)
            .frame(width: 8, height: 8)
        Text("Event Confirmed")
            .font(.caption)
            .foregroundColor(.green)
    }
}
```

### Empty State
```swift
LiquidGlassCard.thin {
    VStack(spacing: 12) {
        Image(systemName: "calendar.badge.exclamationmark")
            .font(.title)
            .foregroundColor(.gray)
        Text("No Events Yet")
            .font(.headline)
        Text("Create your first event to get started")
            .font(.caption)
            .foregroundColor(.secondary)
            .multilineTextAlignment(.center)
    }
    .frame(maxWidth: .infinity)
    .padding(.vertical, 24)
}
```

### Loading State
```swift
LiquidGlassCard {
    HStack(spacing: 12) {
        ProgressView()
            .scaleEffect(0.8)
        Text("Loading events...")
            .font(.subheadline)
            .foregroundColor(.secondary)
        Spacer()
    }
}
```

---

## Complex Layouts

### Budget Breakdown Card
```swift
LiquidGlassCard.thick {
    VStack(alignment: .leading, spacing: 16) {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                Text("Trip Budget")
                    .font(.headline)
                Text("Total estimated cost")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            Spacer()
            Text("$240")
                .font(.title3)
                .fontWeight(.semibold)
                .foregroundColor(.blue)
        }
        
        Divider()
        
        VStack(spacing: 8) {
            BudgetRow(label: "Transportation", amount: "$80", percentage: 0.33)
            BudgetRow(label: "Accommodation", amount: "$120", percentage: 0.50)
            BudgetRow(label: "Activities", amount: "$40", percentage: 0.17)
        }
    }
}

struct BudgetRow: View {
    let label: String
    let amount: String
    let percentage: Double
    
    var body: some View {
        VStack(spacing: 4) {
            HStack {
                Text(label)
                    .font(.caption)
                Spacer()
                Text(amount)
                    .font(.caption)
                    .fontWeight(.semibold)
            }
            GeometryReader { geometry in
                ZStack(alignment: .leading) {
                    RoundedRectangle(cornerRadius: 4)
                        .fill(Color(.systemGray5))
                    RoundedRectangle(cornerRadius: 4)
                        .fill(Color.blue)
                        .frame(width: geometry.size.width * percentage)
                }
            }
            .frame(height: 6)
        }
    }
}
```

### Participant List Card
```swift
LiquidGlassCard {
    VStack(alignment: .leading, spacing: 12) {
        Text("Participants")
            .font(.headline)
        
        VStack(spacing: 8) {
            ForEach(["Alice", "Bob", "Charlie", "Diana"], id: \.self) { name in
                HStack {
                    Circle()
                        .fill(Color.blue)
                        .frame(width: 32, height: 32)
                        .overlay(
                            Text(String(name.prefix(1)))
                                .font(.caption)
                                .fontWeight(.semibold)
                                .foregroundColor(.white)
                        )
                    Text(name)
                        .font(.subheadline)
                    Spacer()
                    Image(systemName: "checkmark.circle.fill")
                        .foregroundColor(.green)
                        .font(.caption)
                }
            }
        }
    }
}
```

---

## Best Practices

### ✅ DO

#### 1. Use appropriate style for context
```swift
// Good: Regular card for main content
LiquidGlassCard {
    EventDetailsView()
}

// Good: Thin card for secondary info
LiquidGlassCard.thin {
    MetadataView()
}

// Good: Thick card for modals/elevated containers
LiquidGlassCard.thick {
    ModalContentView()
}
```

#### 2. Keep content readable
```swift
// Good: Clear hierarchy
LiquidGlassCard {
    VStack(alignment: .leading, spacing: 8) {
        Text("Primary Information")
            .font(.headline)
        Text("Secondary detail")
            .font(.caption)
            .foregroundColor(.secondary)
    }
}
```

#### 3. Use consistent spacing
```swift
// Good: Standard padding matches card padding
LiquidGlassCard(padding: 16) {
    VStack(spacing: 12) { // Consistent internal spacing
        Text("Item 1")
        Text("Item 2")
    }
}
```

#### 4. Customize when needed
```swift
// Good: Custom corner radius for special cases
LiquidGlassCard(cornerRadius: 24) {
    SpecialContent()
}
```

### ❌ DON'T

#### 1. Don't overuse shadows
```swift
// Bad: Multiple nested shadows
LiquidGlassCard {
    VStack {
        Text("Item")
            .shadow(radius: 4) // Avoid additional shadows
    }
    .shadow(radius: 4) // Card already has shadow
}

// Good: Let the card handle visual hierarchy
LiquidGlassCard {
    Text("Item")
}
```

#### 2. Don't ignore readability
```swift
// Bad: Poor contrast
LiquidGlassCard {
    Text("Content")
        .foregroundColor(.gray) // Hard to read
        .font(.caption2) // Too small
}

// Good: Proper contrast and size
LiquidGlassCard {
    Text("Content")
        .foregroundColor(.primary)
        .font(.subheadline)
}
```

#### 3. Don't mix too many styles
```swift
// Bad: Inconsistent styling
VStack {
    LiquidGlassCard { ... }
    Text("Regular text")
        .background(Color.blue) // Different style
    LiquidGlassCard.thin { ... }
}

// Good: Consistent visual language
VStack {
    LiquidGlassCard { ... }
    LiquidGlassCard.thin { ... }
}
```

#### 4. Don't force custom styles for standard cases
```swift
// Bad: Over-customization
LiquidGlassCard(
    style: .regular,
    cornerRadius: 8, // Too small
    padding: 8, // Too tight
    shadow: false // Removes hierarchy
) { ... }

// Good: Use sensible defaults
LiquidGlassCard { ... }
```

---

## Performance Tips

### Use in Lists
```swift
// Safe: LiquidGlassCard is lightweight
List {
    ForEach(events) { event in
        LiquidGlassCard {
            EventRowView(event: event)
        }
        .listRowBackground(Color.clear)
        .listRowInsets(EdgeInsets())
    }
}
```

### Use in ScrollView
```swift
// Good: Efficient rendering
ScrollView {
    VStack(spacing: 16) {
        ForEach(items) { item in
            LiquidGlassCard {
                ItemView(item: item)
            }
        }
    }
    .padding()
}
```

---

**Last Updated:** December 28, 2025  
**Component Version:** 1.0.0
