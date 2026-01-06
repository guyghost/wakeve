# 🔧 Accessibility Fixes - Action Items

**Status**: 🔴 **BLOCKING** - 10 critical issues, 23 major issues

---

## 🔴 CRITICAL FIXES (Must fix before merge)

### 1. ModernHomeView - Touch Targets
**File**: `iosApp/iosApp/Views/ModernHomeView.swift` (line 462-475)
**Issue**: ParticipantAvatar is 40x40pt, needs 44x44pt minimum
**Fix**:
```swift
Circle()
    .fill(avatarColor)
    .frame(width: 44, height: 44)  // Changed from 40 to 44
    .overlay(
        Text(initials)
            .font(.system(size: 16, weight: .semibold))
            .foregroundColor(.white)
    )
    .overlay(
        Circle()
            .stroke(Color.white, lineWidth: 2)
    )
```
**Time**: ~5 minutes

---

### 2. ModernHomeView - AdditionalParticipantsCount Accessibility
**File**: `iosApp/iosApp/Views/ModernHomeView.swift` (line 479-496)
**Issue**: No accessibility label for "+N" badge
**Fix**:
```swift
struct AdditionalParticipantsCount: View {
    let count: Int
    
    var body: some View {
        ZStack {
            Circle()
                .fill(.ultraThinMaterial)
            
            Text("+\(count)")
                .font(.caption.weight(.semibold))
                .foregroundColor(.white)
        }
        .frame(width: 44, height: 44)  // Also increase from 40 to 44
        .overlay(
            Circle()
                .stroke(Color.white, lineWidth: 2)
        )
        .accessibilityLabel("\(count) autres participants")  // ADD THIS
    }
}
```
**Time**: ~5 minutes

---

### 3. ModernHomeView - Calendar Icon Contrast
**File**: `iosApp/iosApp/Views/ModernHomeView.swift` (line 263-267)
**Issue**: White calendar icon on dark gradient has ~2.5:1 contrast (insufficient)
**Fix**:
```swift
// Option 1: Add shadow
Image(systemName: "calendar")
    .font(.system(size: 200))
    .foregroundColor(.white.opacity(0.05))
    .shadow(color: .black.opacity(0.3), radius: 2)  // ADD SHADOW
    .offset(x: geometry.size.width * 0.6, y: -50)

// Option 2: Use semi-opaque background
Image(systemName: "calendar")
    .font(.system(size: 200))
    .foregroundColor(.white)
    .background(Color.black.opacity(0.2))  // ADD BACKGROUND
    .offset(x: geometry.size.width * 0.6, y: -50)
```
**Time**: ~10 minutes

---

### 4. DraftEventWizardView - TextField Accessibility Labels
**File**: `iosApp/iosApp/Views/DraftEventWizardView.swift` (lines 224, 239)
**Issue**: TextFields have no accessibility labels
**Fix**:
```swift
// Step 1: Title TextField
TextField(NSLocalizedString("event_title_hint", comment: "Title placeholder"), text: $title)
    .padding(12)
    .background(.ultraThinMaterial)
    .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
    .overlay(
        RoundedRectangle(cornerRadius: 10, style: .continuous)
            .stroke(title.isEmpty ? Color.red.opacity(0.3) : Color.clear, lineWidth: 1)
    )
    .accessibilityLabel(NSLocalizedString("event_title", comment: "Title label"))  // ADD
    .accessibilityHint(title.isEmpty ? NSLocalizedString("required_field", comment: "Required") : "")  // ADD

// Step 1: Description TextField
TextField(NSLocalizedString("event_description_hint", comment: "Description placeholder"), text: $description, axis: .vertical)
    .lineLimit(3...5)
    .padding(12)
    .background(.ultraThinMaterial)
    .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
    .overlay(
        RoundedRectangle(cornerRadius: 10, style: .continuous)
            .stroke(description.isEmpty ? Color.red.opacity(0.3) : Color.clear, lineWidth: 1)
    )
    .accessibilityLabel(NSLocalizedString("event_description", comment: "Description label"))  // ADD
    .accessibilityHint(description.isEmpty ? NSLocalizedString("required_field", comment: "Required") : "")  // ADD
```
**Time**: ~10 minutes

---

### 5. DraftEventWizardView - Validation Feedback
**File**: `iosApp/iosApp/Views/DraftEventWizardView.swift` (lines 154-173, 183-191)
**Issue**: "Next" button disabled with no VoiceOver announcement
**Fix**:
```swift
if currentStep < steps.count - 1 {
    Button {
        if isStepValid(currentStep) {
            onSaveStep(buildEvent())
            withAnimation {
                currentStep += 1
            }
        }
    } label: {
        HStack {
            Text(NSLocalizedString("next", comment: "Next button"))
            Image(systemName: "chevron.right")
                .font(.system(size: 14, weight: .semibold))
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 14)
        .background(isStepValid(currentStep) ? Color.blue : Color.gray)
        .foregroundColor(.white)
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }
    .disabled(!isStepValid(currentStep))
    // ADD THESE LINES:
    .accessibilityLabel(NSLocalizedString("next", comment: "Next button"))
    .accessibilityHint(isStepValid(currentStep) ? "" : NSLocalizedString("fill_required_fields", comment: "Fill all required fields first"))
}
```
**Time**: ~10 minutes

---

### 6. DraftEventWizardView - Button "Previous" Contrast
**File**: `iosApp/iosApp/Views/DraftEventWizardView.swift` (lines 147)
**Issue**: Button with `.secondary.opacity(0.1)` has ~3.5:1 contrast (borderline)
**Fix**:
```swift
if currentStep > 0 {
    Button {
        withAnimation {
            currentStep -= 1
        }
    } label: {
        HStack {
            Image(systemName: "chevron.left")
                .font(.system(size: 14, weight: .semibold))
            Text(NSLocalizedString("previous", comment: "Previous button"))
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 14)
        .background(Color.secondary.opacity(0.2))  // INCREASED from 0.1 to 0.2
        .foregroundColor(.primary)
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }
}

// OR use border instead:
.border(Color.secondary.opacity(0.5), width: 1)
```
**Time**: ~5 minutes

---

### 7. ModernEventDetailView - Hero Text Contrast
**File**: `iosApp/iosApp/Views/ModernEventDetailView.swift` (lines 300-320)
**Issue**: White text on top of dark gradient has ~2.5:1 contrast
**Fix**:
```swift
// Add shadow to improve contrast
VStack(alignment: .leading, spacing: 12) {
    // ... existing code ...
}
.frame(maxWidth: .infinity, alignment: .leading)
.padding(20)
.background(
    LinearGradient(
        colors: [Color.black.opacity(0.3), Color.black.opacity(0.7)],  // Darker gradient
        startPoint: .top,
        endPoint: .bottom
    )
)
// ADD SHADOW to text
.shadow(color: .black.opacity(0.4), radius: 2, x: 0, y: 1)
```
**Time**: ~5 minutes

---

### 8. ModernEventDetailView - Back Button Label
**File**: `iosApp/iosApp/Views/ModernEventDetailView.swift` (line 199)
**Issue**: Back button (xmark) has no accessibility label
**Fix**:
```swift
Button(action: onBack) {
    Image(systemName: "xmark")
        .font(.system(size: 16, weight: .semibold))
        .foregroundColor(.white)
        .padding(12)
        .background(.ultraThinMaterial)
        .clipShape(Circle())
}
.accessibilityLabel(NSLocalizedString("back", comment: "Back button"))  // ADD
```
**Time**: ~5 minutes

---

### 9. ProfileScreen - Colored Circles Accessibility
**File**: `iosApp/iosApp/Views/ProfileScreen.swift` (line 120-134)
**Issue**: Colored circles alone don't signify state, need labels
**Fix**:
```swift
struct PointBreakdownRow: View {
    let label: String
    let points: Int
    let color: Color
    
    var body: some View {
        HStack {
            Circle()
                .fill(color)
                .frame(width: 12, height: 12)
                .accessibilityHidden(true)  // Hide from VoiceOver
            
            Text(label)
                .font(.subheadline)
                .foregroundColor(.primary)
            
            Spacer()
            
            Text(formatPoints(points))
                .font(.headline)
                .fontWeight(.semibold)
                .foregroundColor(color)
        }
        // GROUP the elements
        .accessibilityElement(children: .combine)  // ADD THIS
        .accessibilityLabel("\(label): \(formatPoints(points)) points")  // ADD THIS
    }
}
```
**Time**: ~10 minutes

---

### 10. CreateEventView - Saving Overlay VoiceOver
**File**: `iosApp/iosApp/Views/CreateEventView.swift` (lines 47-76)
**Issue**: Overlay blocks VoiceOver navigation
**Fix**:
```swift
.overlay {
    if isSaving {
        savingOverlay
    }
}

// ...

private var savingOverlay: some View {
    ZStack {
        Color.black.opacity(0.4)
            .ignoresSafeArea()
        
        VStack(spacing: 16) {
            ProgressView()
                .progressViewStyle(CircularProgressViewStyle(tint: .white))
                .scaleEffect(1.5)
                .accessibilityLabel(NSLocalizedString("saving", comment: "Saving"))  // ADD
            
            Text("Création en cours...")
                .font(.subheadline.weight(.medium))
                .foregroundColor(.white)
        }
        .padding(32)
        .background(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .fill(.ultraThinMaterial)
        )
    }
    .accessibilityViewIsModal(true)  // ADD THIS - indicates modal for VoiceOver
}
```
**Time**: ~5 minutes

---

## 🟠 MAJOR FIXES (Important, non-blocking)

### Icon Accessibility (7 screens)
**Files**: 
- ModernHomeView (line 408 in EventStatusBadge)
- ModernEventDetailView (line 408)
- ExploreView (line 175)
- EventsTabView (line 147)
- CreateEventView (various)

**Issue**: Decorative icons not marked as hidden from VoiceOver
**Fix Pattern**:
```swift
Image(systemName: "chart.bar")
    .font(.system(size: 20, weight: .semibold))
    .accessibilityHidden(true)  // ADD THIS LINE
```
**Time**: ~20 minutes total (2-3 min per file)

---

### VoiceOver Grouping (5 screens)
**Issue**: Cards and rows read individual elements instead of grouping
**Fix Pattern**:
```swift
// For EventCard
Button(action: onTap) {
    // ... existing card content ...
}
.accessibilityElement(children: .combine)  // ADD THIS
.accessibilityLabel(event.title)  // ADD THIS
.accessibilityHint("Double tap to view details")  // ADD THIS

// For PointBreakdownRow
HStack { /* ... */ }
    .accessibilityElement(children: .combine)  // ADD THIS
```
**Time**: ~20 minutes total

---

### Touch Targets
**Issue**: TimeSlotRow delete button, small icons
**Fix**:
```swift
Button { onRemove() } label: {
    Image(systemName: "trash.fill")
        .foregroundColor(.red)
        .frame(width: 44, height: 44)  // Ensure min size
        .contentShape(Rectangle())  // Make tap area larger
}
.accessibilityLabel(NSLocalizedString("delete", comment: "Delete"))
```
**Time**: ~15 minutes

---

## 📋 Implementation Order

1. **Week 1 (Critical fixes)**:
   - ModernHomeView: ParticipantAvatar (5 min)
   - ModernHomeView: AdditionalParticipantsCount (5 min)
   - ModernHomeView: Calendar icon contrast (10 min)
   - DraftEventWizardView: TextField labels (10 min)
   - DraftEventWizardView: Validation feedback (10 min)
   - **Total: 40 minutes**

2. **Week 1 (Continued)**:
   - DraftEventWizardView: Previous button contrast (5 min)
   - ModernEventDetailView: Hero text contrast (5 min)
   - ModernEventDetailView: Back button label (5 min)
   - ProfileScreen: Colored circles (10 min)
   - CreateEventView: Saving overlay (5 min)
   - **Total: 30 minutes**

3. **Week 2 (Major fixes)**:
   - Icon accessibility across all screens (20 min)
   - VoiceOver grouping on cards/rows (20 min)
   - Touch target improvements (15 min)
   - Test all changes with VoiceOver
   - **Total: 55+ minutes**

---

## ✅ Testing Checklist

- [ ] Test with VoiceOver enabled (Settings > Accessibility > VoiceOver)
- [ ] Test with Dynamic Type (Settings > Display & Brightness > Text Size)
- [ ] Test contrast ratios with Stark app or similar
- [ ] Test touch targets with accessibility inspector
- [ ] Test keyboard navigation
- [ ] Test with Dark Mode enabled
- [ ] Test on actual device (not just simulator)

---

## 📊 Expected Results After Fixes

| Category | Before | After |
|----------|--------|-------|
| Critical Issues | 10 | 0 |
| Major Issues | 23 | 8-10 |
| Overall WCAG AA | ❌ | ✅ |
| Screens Approved | 1/8 | 7-8/8 |

---

## 📚 References

- [Apple's Accessibility Documentation](https://developer.apple.com/accessibility/swiftui)
- [WCAG 2.1 Accessibility Guidelines](https://www.w3.org/WAI/WCAG21/quickref)
- [iOS Accessibility Guidelines](https://developer.apple.com/design/human-interface-guidelines/accessibility)
- [Stark Contrast Checker](https://www.getstark.co)

