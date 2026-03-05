# Explore Page - Scenarios/Templates Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Transform the iOS Explore page to feature circular category selectors and a Fitness+-inspired 2x3 grid of colorful event scenario cards, each opening a detail page.

**Architecture:** Static scenario data model in the ViewModel layer. Replace `CategoryChipsRow`/`CategoryChip` with circular `CategoryCircle` selectors. Add `ScenarioGridSection` to `DiscoverySections`. New `ScenarioDetailView` pushed via NavigationStack. All data hardcoded, no backend changes.

**Tech Stack:** SwiftUI, iOS 16+ (with iOS 26 glass effect fallbacks), SF Symbols, existing Wakeve color system.

---

### Task 1: Add Scenario Data Model

**Files:**
- Modify: `iosApp/src/ViewModels/ExploreViewModel.swift:506-514` (replace `EventIdea` references)

**Step 1: Add `EventScenario` struct and static data below `EventCategoryItem` enum (after line 301)**

Add this to the bottom of `ExploreViewModel.swift`, replacing nothing — pure addition:

```swift
// MARK: - Event Scenario Model

struct EventScenario: Identifiable {
    let id = UUID()
    let title: String
    let subtitle: String
    let description: String
    let eventType: String
    let suggestedTitle: String
    let checklistItems: [String]
    let icon: String
    let gradientColors: [Color]
}

extension EventScenario {
    static let allScenarios: [EventScenario] = [
        EventScenario(
            title: String(localized: "scenario.brunch.title"),
            subtitle: String(localized: "scenario.brunch.subtitle"),
            description: String(localized: "scenario.brunch.description"),
            eventType: "FOOD_TASTING",
            suggestedTitle: String(localized: "scenario.brunch.suggested_title"),
            checklistItems: [
                String(localized: "scenario.brunch.check1"),
                String(localized: "scenario.brunch.check2"),
                String(localized: "scenario.brunch.check3"),
                String(localized: "scenario.brunch.check4")
            ],
            icon: "fork.knife",
            gradientColors: [Color(hex: "F97316"), Color(hex: "DC2626")]
        ),
        EventScenario(
            title: String(localized: "scenario.team_building.title"),
            subtitle: String(localized: "scenario.team_building.subtitle"),
            description: String(localized: "scenario.team_building.description"),
            eventType: "TEAM_BUILDING",
            suggestedTitle: String(localized: "scenario.team_building.suggested_title"),
            checklistItems: [
                String(localized: "scenario.team_building.check1"),
                String(localized: "scenario.team_building.check2"),
                String(localized: "scenario.team_building.check3"),
                String(localized: "scenario.team_building.check4")
            ],
            icon: "person.3.fill",
            gradientColors: [Color(hex: "2563EB"), Color(hex: "06B6D4")]
        ),
        EventScenario(
            title: String(localized: "scenario.birthday.title"),
            subtitle: String(localized: "scenario.birthday.subtitle"),
            description: String(localized: "scenario.birthday.description"),
            eventType: "BIRTHDAY",
            suggestedTitle: String(localized: "scenario.birthday.suggested_title"),
            checklistItems: [
                String(localized: "scenario.birthday.check1"),
                String(localized: "scenario.birthday.check2"),
                String(localized: "scenario.birthday.check3"),
                String(localized: "scenario.birthday.check4")
            ],
            icon: "cake.fill",
            gradientColors: [Color(hex: "EC4899"), Color(hex: "8B5CF6")]
        ),
        EventScenario(
            title: String(localized: "scenario.cinema.title"),
            subtitle: String(localized: "scenario.cinema.subtitle"),
            description: String(localized: "scenario.cinema.description"),
            eventType: "PARTY",
            suggestedTitle: String(localized: "scenario.cinema.suggested_title"),
            checklistItems: [
                String(localized: "scenario.cinema.check1"),
                String(localized: "scenario.cinema.check2"),
                String(localized: "scenario.cinema.check3"),
                String(localized: "scenario.cinema.check4")
            ],
            icon: "film.fill",
            gradientColors: [Color(hex: "065F46"), Color(hex: "10B981")]
        ),
        EventScenario(
            title: String(localized: "scenario.hiking.title"),
            subtitle: String(localized: "scenario.hiking.subtitle"),
            description: String(localized: "scenario.hiking.description"),
            eventType: "OUTDOOR_ACTIVITY",
            suggestedTitle: String(localized: "scenario.hiking.suggested_title"),
            checklistItems: [
                String(localized: "scenario.hiking.check1"),
                String(localized: "scenario.hiking.check2"),
                String(localized: "scenario.hiking.check3"),
                String(localized: "scenario.hiking.check4")
            ],
            icon: "figure.hiking",
            gradientColors: [Color(hex: "0D9488"), Color(hex: "2563EB")]
        ),
        EventScenario(
            title: String(localized: "scenario.afterwork.title"),
            subtitle: String(localized: "scenario.afterwork.subtitle"),
            description: String(localized: "scenario.afterwork.description"),
            eventType: "PARTY",
            suggestedTitle: String(localized: "scenario.afterwork.suggested_title"),
            checklistItems: [
                String(localized: "scenario.afterwork.check1"),
                String(localized: "scenario.afterwork.check2"),
                String(localized: "scenario.afterwork.check3"),
                String(localized: "scenario.afterwork.check4")
            ],
            icon: "wineglass.fill",
            gradientColors: [Color(hex: "7C3AED"), Color(hex: "4338CA")]
        )
    ]
}
```

**Step 2: Verify the file compiles**

Run: `cd /Users/guy/Developer/dev/wakeve && xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'platform=iOS Simulator,name=iPhone 16' build 2>&1 | tail -20`
Expected: BUILD SUCCEEDED (or at least no errors in ExploreViewModel.swift)

**Step 3: Commit**

```bash
git add iosApp/src/ViewModels/ExploreViewModel.swift
git commit -m "feat(ios): add EventScenario data model with 6 scenarios"
```

---

### Task 2: Replace Category Chips with Circular Selectors

**Files:**
- Modify: `iosApp/src/Views/ExploreTabView.swift:54-106` (replace `CategoryChipsRow` and `CategoryChip`)

**Step 1: Replace `CategoryChipsRow` and `CategoryChip` structs (lines 54-106)**

Delete the existing `CategoryChipsRow` and `CategoryChip` structs and replace with:

```swift
// MARK: - Category Circle Selectors

struct CategoryCirclesRow: View {
    @Binding var selectedCategory: EventCategoryItem
    let onSelect: (EventCategoryItem) -> Void

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 20) {
                ForEach(EventCategoryItem.allCases) { category in
                    CategoryCircle(
                        category: category,
                        isSelected: selectedCategory == category,
                        onTap: {
                            selectedCategory = category
                            onSelect(category)
                        }
                    )
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
        }
    }
}

struct CategoryCircle: View {
    let category: EventCategoryItem
    let isSelected: Bool
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            VStack(spacing: 8) {
                ZStack {
                    Circle()
                        .fill(isSelected ? category.tintColor : category.tintColor.opacity(0.12))
                        .frame(width: 64, height: 64)

                    Image(systemName: category.icon)
                        .font(.title2)
                        .foregroundColor(isSelected ? .white : category.tintColor)
                }

                Text(category.displayName)
                    .font(.caption)
                    .fontWeight(isSelected ? .semibold : .regular)
                    .foregroundColor(isSelected ? .primary : .secondary)
                    .lineLimit(1)
            }
        }
        .buttonStyle(.plain)
        .accessibilityLabel(category.displayName)
        .accessibilityAddTraits(isSelected ? .isSelected : [])
    }
}
```

**Step 2: Add `tintColor` computed property to `EventCategoryItem`**

In `iosApp/src/ViewModels/ExploreViewModel.swift`, add inside the `EventCategoryItem` enum (after the `eventTypes` property, before the closing brace at line 301):

```swift
    var tintColor: Color {
        switch self {
        case .all: return .gray
        case .social: return .blue
        case .sport: return .green
        case .culture: return .purple
        case .professional: return .orange
        case .food: return .red
        case .wellness: return .pink
        }
    }
```

**Step 3: Update the reference in `ExploreTabView` body**

In `ExploreTabView` body (line 15), change `CategoryChipsRow` to `CategoryCirclesRow`:

```swift
                    CategoryCirclesRow(
                        selectedCategory: $viewModel.selectedCategory,
                        onSelect: { category in
                            viewModel.selectCategory(category)
                        }
                    )
                    .padding(.top, 8)
```

(The call signature is identical, only the struct name changes.)

**Step 4: Verify build**

Run: `cd /Users/guy/Developer/dev/wakeve && xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'platform=iOS Simulator,name=iPhone 16' build 2>&1 | tail -20`

**Step 5: Commit**

```bash
git add iosApp/src/Views/ExploreTabView.swift iosApp/src/ViewModels/ExploreViewModel.swift
git commit -m "feat(ios): replace category chips with circular selectors"
```

---

### Task 3: Add Scenario Grid Section to Explore Page

**Files:**
- Modify: `iosApp/src/Views/ExploreTabView.swift`

**Step 1: Add `ScenarioGridSection` view**

Add this new struct after `DiscoverySections` (after line 154) and before `ExploreSection`:

```swift
// MARK: - Scenario Grid Section ("A decouvrir")

struct ScenarioGridSection: View {
    let columns = [
        GridItem(.flexible(), spacing: 12),
        GridItem(.flexible(), spacing: 12)
    ]

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack(spacing: 8) {
                Image(systemName: "sparkle")
                    .foregroundColor(.wakeveAccent)
                Text(String(localized: "explore.discover"))
                    .font(.title3)
                    .fontWeight(.bold)
                    .foregroundColor(.primary)
            }
            .padding(.horizontal, 16)

            LazyVGrid(columns: columns, spacing: 12) {
                ForEach(EventScenario.allScenarios) { scenario in
                    NavigationLink(value: scenario) {
                        ScenarioCard(scenario: scenario)
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(.horizontal, 16)
        }
    }
}

struct ScenarioCard: View {
    let scenario: EventScenario

    var body: some View {
        ZStack(alignment: .bottomLeading) {
            // Gradient background
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .fill(
                    LinearGradient(
                        colors: scenario.gradientColors,
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )

            // Text content
            VStack(alignment: .leading, spacing: 4) {
                Spacer()
                Text(scenario.title)
                    .font(.subheadline)
                    .fontWeight(.bold)
                    .foregroundColor(.white)
                    .lineLimit(3)
                    .multilineTextAlignment(.leading)
            }
            .padding(14)
        }
        .frame(height: 140)
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
    }
}
```

**Step 2: Make `EventScenario` conform to `Hashable` for `NavigationLink(value:)`**

In `iosApp/src/ViewModels/ExploreViewModel.swift`, update the struct declaration:

```swift
struct EventScenario: Identifiable, Hashable {
    let id = UUID()
    // ... rest same

    // Add Hashable conformance
    static func == (lhs: EventScenario, rhs: EventScenario) -> Bool {
        lhs.id == rhs.id
    }

    func hash(into hasher: inout Hasher) {
        hasher.combine(id)
    }
}
```

**Step 3: Insert `ScenarioGridSection` into `DiscoverySections`**

In `DiscoverySections` body (line 114), add the scenario grid as the first item inside the `VStack(spacing: 24)`, before the trending section:

```swift
    var body: some View {
        VStack(spacing: 24) {
            // Scenario templates grid
            ScenarioGridSection()

            // Trending section
            if !viewModel.trendingEvents.isEmpty {
            // ... rest unchanged
```

**Step 4: Add `navigationDestination` for scenario detail in `ExploreTabView`**

In `ExploreTabView` body, inside the `NavigationStack` (after `.onChange` at line 48), add:

```swift
            .navigationDestination(for: EventScenario.self) { scenario in
                ScenarioDetailView(scenario: scenario)
            }
```

(ScenarioDetailView will be created in the next task.)

**Step 5: Verify build** (will fail until Task 4 adds ScenarioDetailView — create a placeholder first)

Add a temporary placeholder at the bottom of `ExploreTabView.swift`:

```swift
// MARK: - Scenario Detail (placeholder)

struct ScenarioDetailView: View {
    let scenario: EventScenario
    var body: some View {
        Text(scenario.title)
    }
}
```

Run: `cd /Users/guy/Developer/dev/wakeve && xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'platform=iOS Simulator,name=iPhone 16' build 2>&1 | tail -20`

**Step 6: Commit**

```bash
git add iosApp/src/Views/ExploreTabView.swift iosApp/src/ViewModels/ExploreViewModel.swift
git commit -m "feat(ios): add scenario grid section with gradient cards"
```

---

### Task 4: Create Scenario Detail View

**Files:**
- Create: `iosApp/src/Views/ScenarioDetailView.swift`
- Modify: `iosApp/src/Views/ExploreTabView.swift` (remove placeholder)

**Step 1: Create `ScenarioDetailView.swift`**

```swift
import SwiftUI

/// Detail view for an event scenario/template.
/// Shows full description, planning checklist, and CTA to create event.
struct ScenarioDetailView: View {
    let scenario: EventScenario
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        ScrollView {
            VStack(spacing: 0) {
                // Gradient header
                ZStack(alignment: .bottomLeading) {
                    LinearGradient(
                        colors: scenario.gradientColors,
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                    .frame(height: 220)

                    VStack(alignment: .leading, spacing: 8) {
                        Image(systemName: scenario.icon)
                            .font(.largeTitle)
                            .foregroundColor(.white.opacity(0.9))

                        Text(scenario.title)
                            .font(.title)
                            .fontWeight(.bold)
                            .foregroundColor(.white)

                        Text(scenario.subtitle)
                            .font(.subheadline)
                            .foregroundColor(.white.opacity(0.85))
                    }
                    .padding(20)
                    .padding(.bottom, 8)
                }

                VStack(alignment: .leading, spacing: 24) {
                    // Description
                    Text(scenario.description)
                        .font(.body)
                        .foregroundColor(.secondary)
                        .lineSpacing(4)

                    // Checklist
                    VStack(alignment: .leading, spacing: 16) {
                        Text(String(localized: "scenario.checklist_title"))
                            .font(.headline)
                            .foregroundColor(.primary)

                        ForEach(Array(scenario.checklistItems.enumerated()), id: \.offset) { index, item in
                            HStack(alignment: .top, spacing: 12) {
                                Image(systemName: "\(index + 1).circle.fill")
                                    .font(.title3)
                                    .foregroundColor(scenario.gradientColors.first ?? .blue)

                                Text(item)
                                    .font(.body)
                                    .foregroundColor(.primary)
                            }
                        }
                    }

                    // CTA Button
                    Button(action: createEvent) {
                        HStack {
                            Image(systemName: "plus.circle.fill")
                            Text(String(localized: "scenario.create_event"))
                        }
                        .font(.headline)
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 16)
                        .background(
                            LinearGradient(
                                colors: scenario.gradientColors,
                                startPoint: .leading,
                                endPoint: .trailing
                            )
                        )
                        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                    }
                    .padding(.top, 8)
                }
                .padding(20)
            }
        }
        .ignoresSafeArea(edges: .top)
        .navigationBarTitleDisplayMode(.inline)
        .toolbarBackground(.hidden, for: .navigationBar)
    }

    private func createEvent() {
        // TODO: Navigate to event creation pre-filled with scenario data
        // scenario.eventType, scenario.suggestedTitle, scenario.description
        dismiss()
    }
}
```

**Step 2: Remove the placeholder `ScenarioDetailView` from `ExploreTabView.swift`**

Delete the temporary placeholder struct added in Task 3.

**Step 3: Verify build**

Run: `cd /Users/guy/Developer/dev/wakeve && xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'platform=iOS Simulator,name=iPhone 16' build 2>&1 | tail -20`

**Step 4: Commit**

```bash
git add iosApp/src/Views/ScenarioDetailView.swift iosApp/src/Views/ExploreTabView.swift
git commit -m "feat(ios): add scenario detail view with gradient header and checklist"
```

---

### Task 5: Remove Old EventIdeasSection

**Files:**
- Modify: `iosApp/src/Views/ExploreTabView.swift`

**Step 1: Remove `EventIdeasSection` from `DiscoverySections`**

In `DiscoverySections` body, remove the line:
```swift
            // Ideas section (kept from original)
            EventIdeasSection()
                .padding(.horizontal, 16)
```

**Step 2: Remove `EventIdeasSection`, `EventIdea`, and `EventIdeaCard` structs**

Delete everything from `// MARK: - Event Ideas Section (kept from original)` (line 464) through `EventIdeaCard` closing brace (line 561).

**Step 3: Verify build**

Run: `cd /Users/guy/Developer/dev/wakeve && xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'platform=iOS Simulator,name=iPhone 16' build 2>&1 | tail -20`

**Step 4: Commit**

```bash
git add iosApp/src/Views/ExploreTabView.swift
git commit -m "refactor(ios): remove old EventIdeasSection replaced by scenario grid"
```

---

### Task 6: Add Localization Strings for All 5 Languages

**Files:**
- Modify: `iosApp/src/Resources/fr.lproj/Localizable.strings`
- Modify: `iosApp/src/Resources/en.lproj/Localizable.strings`
- Modify: `iosApp/src/Resources/es.lproj/Localizable.strings`
- Modify: `iosApp/src/Resources/it.lproj/Localizable.strings`
- Modify: `iosApp/src/Resources/pt.lproj/Localizable.strings`

**Step 1: Add French strings (primary language) to `fr.lproj/Localizable.strings`**

Append:

```
// MARK: - Explore Scenarios
"explore.discover" = "À découvrir";
"scenario.checklist_title" = "Comment organiser";
"scenario.create_event" = "Créer cet événement";

// Brunch
"scenario.brunch.title" = "Organisez un brunch entre amis";
"scenario.brunch.subtitle" = "Trouvez le meilleur moment pour bruncher";
"scenario.brunch.description" = "Rien de tel qu'un brunch pour se retrouver entre amis. Choisissez le créneau idéal, définissez le menu ensemble et profitez d'un moment convivial autour de bons plats.";
"scenario.brunch.suggested_title" = "Brunch entre amis";
"scenario.brunch.check1" = "Choisir un lieu ou un hôte";
"scenario.brunch.check2" = "Définir le menu et les contributions";
"scenario.brunch.check3" = "Trouver la date idéale avec le sondage";
"scenario.brunch.check4" = "Envoyer les invitations";

// Team Building
"scenario.team_building.title" = "Planifiez votre team building";
"scenario.team_building.subtitle" = "Motivez votre équipe";
"scenario.team_building.description" = "Renforcez la cohésion de votre équipe avec un événement team building mémorable. Trouvez le créneau qui convient à tous et organisez une activité fédératrice.";
"scenario.team_building.suggested_title" = "Team building d'équipe";
"scenario.team_building.check1" = "Définir le type d'activité";
"scenario.team_building.check2" = "Réserver un lieu ou un prestataire";
"scenario.team_building.check3" = "Sonder les disponibilités de l'équipe";
"scenario.team_building.check4" = "Confirmer et partager les détails";

// Birthday
"scenario.birthday.title" = "Fête d'anniversaire surprise";
"scenario.birthday.subtitle" = "Coordonnez la surprise parfaite";
"scenario.birthday.description" = "Organisez une fête d'anniversaire mémorable en toute discrétion. Coordonnez-vous avec les invités pour trouver le moment parfait et préparer la surprise.";
"scenario.birthday.suggested_title" = "Anniversaire surprise";
"scenario.birthday.check1" = "Créer la liste d'invités";
"scenario.birthday.check2" = "Choisir un thème et un lieu";
"scenario.birthday.check3" = "Coordonner les contributions (cadeau, gâteau)";
"scenario.birthday.check4" = "Sonder les disponibilités en secret";

// Cinema
"scenario.cinema.title" = "Soirée cinéma maison";
"scenario.cinema.subtitle" = "Invitez vos amis pour une soirée film";
"scenario.cinema.description" = "Organisez une soirée cinéma chez vous ou chez un ami. Choisissez le film ensemble, préparez les snacks et passez un moment détente entre proches.";
"scenario.cinema.suggested_title" = "Soirée cinéma";
"scenario.cinema.check1" = "Voter pour le film à regarder";
"scenario.cinema.check2" = "Choisir la date avec le sondage";
"scenario.cinema.check3" = "Organiser les snacks et boissons";
"scenario.cinema.check4" = "Préparer l'installation (écran, son)";

// Hiking
"scenario.hiking.title" = "Weekend randonnée";
"scenario.hiking.subtitle" = "Planifiez une escapade nature";
"scenario.hiking.description" = "Évadez-vous le temps d'un weekend en pleine nature. Trouvez le créneau idéal, choisissez l'itinéraire et organisez les détails logistiques en groupe.";
"scenario.hiking.suggested_title" = "Randonnée en groupe";
"scenario.hiking.check1" = "Choisir le parcours et le niveau de difficulté";
"scenario.hiking.check2" = "Trouver la date qui convient à tous";
"scenario.hiking.check3" = "Organiser le transport et le covoiturage";
"scenario.hiking.check4" = "Préparer la liste du matériel nécessaire";

// Afterwork
"scenario.afterwork.title" = "Afterwork entre collègues";
"scenario.afterwork.subtitle" = "Décompressez ensemble";
"scenario.afterwork.description" = "Terminez la semaine en beauté avec un afterwork convivial. Trouvez le bar idéal, sondez les collègues et organisez un moment de détente après le travail.";
"scenario.afterwork.suggested_title" = "Afterwork";
"scenario.afterwork.check1" = "Choisir un bar ou restaurant";
"scenario.afterwork.check2" = "Sonder les disponibilités";
"scenario.afterwork.check3" = "Réserver une table si nécessaire";
"scenario.afterwork.check4" = "Partager l'adresse et les détails";
```

**Step 2: Add English strings to `en.lproj/Localizable.strings`**

```
// MARK: - Explore Scenarios
"explore.discover" = "Discover";
"scenario.checklist_title" = "How to organize";
"scenario.create_event" = "Create this event";

"scenario.brunch.title" = "Organize a brunch with friends";
"scenario.brunch.subtitle" = "Find the best time to brunch";
"scenario.brunch.description" = "Nothing beats a brunch to catch up with friends. Pick the ideal time slot, plan the menu together, and enjoy a lovely meal with your crew.";
"scenario.brunch.suggested_title" = "Brunch with friends";
"scenario.brunch.check1" = "Choose a venue or host";
"scenario.brunch.check2" = "Plan the menu and contributions";
"scenario.brunch.check3" = "Find the ideal date with the poll";
"scenario.brunch.check4" = "Send out invitations";

"scenario.team_building.title" = "Plan your team building";
"scenario.team_building.subtitle" = "Motivate your team";
"scenario.team_building.description" = "Strengthen your team's bond with a memorable team building event. Find a time that works for everyone and organize an engaging activity.";
"scenario.team_building.suggested_title" = "Team building event";
"scenario.team_building.check1" = "Define the activity type";
"scenario.team_building.check2" = "Book a venue or provider";
"scenario.team_building.check3" = "Poll the team's availability";
"scenario.team_building.check4" = "Confirm and share details";

"scenario.birthday.title" = "Surprise birthday party";
"scenario.birthday.subtitle" = "Coordinate the perfect surprise";
"scenario.birthday.description" = "Organize a memorable birthday party in secret. Coordinate with guests to find the perfect time and prepare the surprise together.";
"scenario.birthday.suggested_title" = "Surprise birthday";
"scenario.birthday.check1" = "Create the guest list";
"scenario.birthday.check2" = "Choose a theme and venue";
"scenario.birthday.check3" = "Coordinate contributions (gift, cake)";
"scenario.birthday.check4" = "Poll availability secretly";

"scenario.cinema.title" = "Movie night at home";
"scenario.cinema.subtitle" = "Invite your friends for a film night";
"scenario.cinema.description" = "Host a movie night at your place or a friend's. Vote on the film together, prepare the snacks, and enjoy a relaxing evening with your crew.";
"scenario.cinema.suggested_title" = "Movie night";
"scenario.cinema.check1" = "Vote on which movie to watch";
"scenario.cinema.check2" = "Pick the date with the poll";
"scenario.cinema.check3" = "Organize snacks and drinks";
"scenario.cinema.check4" = "Set up the viewing area (screen, sound)";

"scenario.hiking.title" = "Weekend hiking trip";
"scenario.hiking.subtitle" = "Plan a nature getaway";
"scenario.hiking.description" = "Escape for a weekend in nature. Find the ideal date, choose the trail, and organize logistics together as a group.";
"scenario.hiking.suggested_title" = "Group hiking trip";
"scenario.hiking.check1" = "Choose the trail and difficulty level";
"scenario.hiking.check2" = "Find a date that works for everyone";
"scenario.hiking.check3" = "Organize transport and carpooling";
"scenario.hiking.check4" = "Prepare the gear checklist";

"scenario.afterwork.title" = "Afterwork with colleagues";
"scenario.afterwork.subtitle" = "Unwind together";
"scenario.afterwork.description" = "End the week on a high note with a casual afterwork. Find the perfect bar, poll your colleagues, and organize a relaxing moment after work.";
"scenario.afterwork.suggested_title" = "Afterwork";
"scenario.afterwork.check1" = "Choose a bar or restaurant";
"scenario.afterwork.check2" = "Poll availability";
"scenario.afterwork.check3" = "Book a table if needed";
"scenario.afterwork.check4" = "Share the address and details";
```

**Step 3: Add Spanish strings to `es.lproj/Localizable.strings`**

```
// MARK: - Explore Scenarios
"explore.discover" = "Descubrir";
"scenario.checklist_title" = "Cómo organizar";
"scenario.create_event" = "Crear este evento";

"scenario.brunch.title" = "Organiza un brunch con amigos";
"scenario.brunch.subtitle" = "Encuentra el mejor momento para brunchar";
"scenario.brunch.description" = "Nada mejor que un brunch para reencontrarse con amigos. Elige el horario ideal, planifica el menú juntos y disfruta de un momento agradable.";
"scenario.brunch.suggested_title" = "Brunch con amigos";
"scenario.brunch.check1" = "Elegir un lugar o anfitrión";
"scenario.brunch.check2" = "Definir el menú y las contribuciones";
"scenario.brunch.check3" = "Encontrar la fecha ideal con la encuesta";
"scenario.brunch.check4" = "Enviar las invitaciones";

"scenario.team_building.title" = "Planifica tu team building";
"scenario.team_building.subtitle" = "Motiva a tu equipo";
"scenario.team_building.description" = "Fortalece la cohesión de tu equipo con un evento memorable. Encuentra un horario que funcione para todos y organiza una actividad unificadora.";
"scenario.team_building.suggested_title" = "Team building de equipo";
"scenario.team_building.check1" = "Definir el tipo de actividad";
"scenario.team_building.check2" = "Reservar un lugar o proveedor";
"scenario.team_building.check3" = "Sondear la disponibilidad del equipo";
"scenario.team_building.check4" = "Confirmar y compartir detalles";

"scenario.birthday.title" = "Fiesta de cumpleaños sorpresa";
"scenario.birthday.subtitle" = "Coordina la sorpresa perfecta";
"scenario.birthday.description" = "Organiza una fiesta de cumpleaños memorable en secreto. Coordínate con los invitados para encontrar el momento perfecto y preparar la sorpresa.";
"scenario.birthday.suggested_title" = "Cumpleaños sorpresa";
"scenario.birthday.check1" = "Crear la lista de invitados";
"scenario.birthday.check2" = "Elegir un tema y lugar";
"scenario.birthday.check3" = "Coordinar contribuciones (regalo, pastel)";
"scenario.birthday.check4" = "Sondear disponibilidad en secreto";

"scenario.cinema.title" = "Noche de cine en casa";
"scenario.cinema.subtitle" = "Invita a tus amigos a ver una película";
"scenario.cinema.description" = "Organiza una noche de cine en tu casa o la de un amigo. Voten por la película juntos, preparen los snacks y disfruten de una velada relajante.";
"scenario.cinema.suggested_title" = "Noche de cine";
"scenario.cinema.check1" = "Votar por la película";
"scenario.cinema.check2" = "Elegir la fecha con la encuesta";
"scenario.cinema.check3" = "Organizar snacks y bebidas";
"scenario.cinema.check4" = "Preparar la instalación (pantalla, sonido)";

"scenario.hiking.title" = "Fin de semana de senderismo";
"scenario.hiking.subtitle" = "Planifica una escapada a la naturaleza";
"scenario.hiking.description" = "Escápate un fin de semana en plena naturaleza. Encuentra la fecha ideal, elige la ruta y organiza los detalles logísticos en grupo.";
"scenario.hiking.suggested_title" = "Senderismo en grupo";
"scenario.hiking.check1" = "Elegir la ruta y nivel de dificultad";
"scenario.hiking.check2" = "Encontrar la fecha que convenga a todos";
"scenario.hiking.check3" = "Organizar transporte y viaje compartido";
"scenario.hiking.check4" = "Preparar la lista de material necesario";

"scenario.afterwork.title" = "Afterwork con colegas";
"scenario.afterwork.subtitle" = "Desconecten juntos";
"scenario.afterwork.description" = "Termina la semana con un afterwork informal. Encuentra el bar ideal, sondea a los colegas y organiza un momento de relax después del trabajo.";
"scenario.afterwork.suggested_title" = "Afterwork";
"scenario.afterwork.check1" = "Elegir un bar o restaurante";
"scenario.afterwork.check2" = "Sondear disponibilidad";
"scenario.afterwork.check3" = "Reservar mesa si es necesario";
"scenario.afterwork.check4" = "Compartir dirección y detalles";
```

**Step 4: Add Italian strings to `it.lproj/Localizable.strings`**

```
// MARK: - Explore Scenarios
"explore.discover" = "Da scoprire";
"scenario.checklist_title" = "Come organizzare";
"scenario.create_event" = "Crea questo evento";

"scenario.brunch.title" = "Organizza un brunch tra amici";
"scenario.brunch.subtitle" = "Trova il momento migliore per il brunch";
"scenario.brunch.description" = "Niente di meglio di un brunch per ritrovarsi tra amici. Scegli l'orario ideale, pianifica il menu insieme e goditi un momento conviviale.";
"scenario.brunch.suggested_title" = "Brunch tra amici";
"scenario.brunch.check1" = "Scegliere un luogo o un ospite";
"scenario.brunch.check2" = "Definire il menu e i contributi";
"scenario.brunch.check3" = "Trovare la data ideale con il sondaggio";
"scenario.brunch.check4" = "Inviare gli inviti";

"scenario.team_building.title" = "Pianifica il tuo team building";
"scenario.team_building.subtitle" = "Motiva il tuo team";
"scenario.team_building.description" = "Rafforza la coesione del tuo team con un evento memorabile. Trova un orario che vada bene per tutti e organizza un'attività coinvolgente.";
"scenario.team_building.suggested_title" = "Team building di squadra";
"scenario.team_building.check1" = "Definire il tipo di attività";
"scenario.team_building.check2" = "Prenotare un luogo o fornitore";
"scenario.team_building.check3" = "Sondare la disponibilità del team";
"scenario.team_building.check4" = "Confermare e condividere i dettagli";

"scenario.birthday.title" = "Festa di compleanno a sorpresa";
"scenario.birthday.subtitle" = "Coordina la sorpresa perfetta";
"scenario.birthday.description" = "Organizza una festa di compleanno memorabile in segreto. Coordinati con gli invitati per trovare il momento perfetto e preparare la sorpresa.";
"scenario.birthday.suggested_title" = "Compleanno a sorpresa";
"scenario.birthday.check1" = "Creare la lista degli invitati";
"scenario.birthday.check2" = "Scegliere un tema e un luogo";
"scenario.birthday.check3" = "Coordinare i contributi (regalo, torta)";
"scenario.birthday.check4" = "Sondare la disponibilità in segreto";

"scenario.cinema.title" = "Serata cinema a casa";
"scenario.cinema.subtitle" = "Invita i tuoi amici per una serata film";
"scenario.cinema.description" = "Organizza una serata cinema a casa tua o di un amico. Votate insieme il film, preparate gli snack e godetevi una serata rilassante.";
"scenario.cinema.suggested_title" = "Serata cinema";
"scenario.cinema.check1" = "Votare il film da guardare";
"scenario.cinema.check2" = "Scegliere la data con il sondaggio";
"scenario.cinema.check3" = "Organizzare snack e bevande";
"scenario.cinema.check4" = "Preparare l'impianto (schermo, audio)";

"scenario.hiking.title" = "Weekend di escursionismo";
"scenario.hiking.subtitle" = "Pianifica una fuga nella natura";
"scenario.hiking.description" = "Fuggi per un weekend nella natura. Trova la data ideale, scegli il percorso e organizza i dettagli logistici in gruppo.";
"scenario.hiking.suggested_title" = "Escursione di gruppo";
"scenario.hiking.check1" = "Scegliere il percorso e il livello di difficoltà";
"scenario.hiking.check2" = "Trovare la data che va bene per tutti";
"scenario.hiking.check3" = "Organizzare trasporto e carpooling";
"scenario.hiking.check4" = "Preparare la lista dell'attrezzatura necessaria";

"scenario.afterwork.title" = "Afterwork con i colleghi";
"scenario.afterwork.subtitle" = "Rilassatevi insieme";
"scenario.afterwork.description" = "Chiudi la settimana in bellezza con un afterwork informale. Trova il bar ideale, sonda i colleghi e organizza un momento di relax dopo il lavoro.";
"scenario.afterwork.suggested_title" = "Afterwork";
"scenario.afterwork.check1" = "Scegliere un bar o ristorante";
"scenario.afterwork.check2" = "Sondare la disponibilità";
"scenario.afterwork.check3" = "Prenotare un tavolo se necessario";
"scenario.afterwork.check4" = "Condividere indirizzo e dettagli";
```

**Step 5: Add Portuguese strings to `pt.lproj/Localizable.strings`**

```
// MARK: - Explore Scenarios
"explore.discover" = "Descobrir";
"scenario.checklist_title" = "Como organizar";
"scenario.create_event" = "Criar este evento";

"scenario.brunch.title" = "Organize um brunch com amigos";
"scenario.brunch.subtitle" = "Encontre o melhor momento para brunchar";
"scenario.brunch.description" = "Nada melhor que um brunch para reencontrar os amigos. Escolha o horário ideal, planeje o menu juntos e aproveite um momento agradável.";
"scenario.brunch.suggested_title" = "Brunch com amigos";
"scenario.brunch.check1" = "Escolher um local ou anfitrião";
"scenario.brunch.check2" = "Definir o menu e as contribuições";
"scenario.brunch.check3" = "Encontrar a data ideal com a enquete";
"scenario.brunch.check4" = "Enviar os convites";

"scenario.team_building.title" = "Planeje seu team building";
"scenario.team_building.subtitle" = "Motive sua equipe";
"scenario.team_building.description" = "Fortaleça a coesão da sua equipe com um evento memorável. Encontre um horário que funcione para todos e organize uma atividade envolvente.";
"scenario.team_building.suggested_title" = "Team building de equipe";
"scenario.team_building.check1" = "Definir o tipo de atividade";
"scenario.team_building.check2" = "Reservar um local ou fornecedor";
"scenario.team_building.check3" = "Sondar a disponibilidade da equipe";
"scenario.team_building.check4" = "Confirmar e compartilhar detalhes";

"scenario.birthday.title" = "Festa de aniversário surpresa";
"scenario.birthday.subtitle" = "Coordene a surpresa perfeita";
"scenario.birthday.description" = "Organize uma festa de aniversário memorável em segredo. Coordene-se com os convidados para encontrar o momento perfeito e preparar a surpresa.";
"scenario.birthday.suggested_title" = "Aniversário surpresa";
"scenario.birthday.check1" = "Criar a lista de convidados";
"scenario.birthday.check2" = "Escolher um tema e local";
"scenario.birthday.check3" = "Coordenar contribuições (presente, bolo)";
"scenario.birthday.check4" = "Sondar disponibilidade em segredo";

"scenario.cinema.title" = "Noite de cinema em casa";
"scenario.cinema.subtitle" = "Convide seus amigos para uma sessão de filme";
"scenario.cinema.description" = "Organize uma noite de cinema na sua casa ou de um amigo. Votem juntos no filme, preparem os lanches e aproveitem uma noite relaxante.";
"scenario.cinema.suggested_title" = "Noite de cinema";
"scenario.cinema.check1" = "Votar no filme para assistir";
"scenario.cinema.check2" = "Escolher a data com a enquete";
"scenario.cinema.check3" = "Organizar lanches e bebidas";
"scenario.cinema.check4" = "Preparar a instalação (tela, som)";

"scenario.hiking.title" = "Fim de semana de trilha";
"scenario.hiking.subtitle" = "Planeje uma escapada na natureza";
"scenario.hiking.description" = "Fuja por um fim de semana na natureza. Encontre a data ideal, escolha a trilha e organize os detalhes logísticos em grupo.";
"scenario.hiking.suggested_title" = "Trilha em grupo";
"scenario.hiking.check1" = "Escolher a trilha e nível de dificuldade";
"scenario.hiking.check2" = "Encontrar a data que funcione para todos";
"scenario.hiking.check3" = "Organizar transporte e carona";
"scenario.hiking.check4" = "Preparar a lista de material necessário";

"scenario.afterwork.title" = "Afterwork com colegas";
"scenario.afterwork.subtitle" = "Relaxem juntos";
"scenario.afterwork.description" = "Termine a semana em grande com um afterwork informal. Encontre o bar ideal, sonde os colegas e organize um momento de descontração após o trabalho.";
"scenario.afterwork.suggested_title" = "Afterwork";
"scenario.afterwork.check1" = "Escolher um bar ou restaurante";
"scenario.afterwork.check2" = "Sondar disponibilidade";
"scenario.afterwork.check3" = "Reservar mesa se necessário";
"scenario.afterwork.check4" = "Compartilhar endereço e detalhes";
```

**Step 6: Verify build**

Run: `cd /Users/guy/Developer/dev/wakeve && xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'platform=iOS Simulator,name=iPhone 16' build 2>&1 | tail -20`

**Step 7: Commit**

```bash
git add iosApp/src/Resources/
git commit -m "feat(i18n): add scenario strings for FR, EN, ES, IT, PT"
```

---

### Task 7: Final Verification

**Step 1: Full clean build**

Run: `cd /Users/guy/Developer/dev/wakeve && xcodebuild clean build -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'platform=iOS Simulator,name=iPhone 16' 2>&1 | tail -30`

**Step 2: Visual check — launch simulator**

Run: `cd /Users/guy/Developer/dev/wakeve && xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'platform=iOS Simulator,name=iPhone 16' -derivedDataPath build/ build 2>&1 | tail -5`

Then: `xcrun simctl boot "iPhone 16" 2>/dev/null; xcrun simctl install "iPhone 16" build/Build/Products/Debug-iphonesimulator/iosApp.app && xcrun simctl launch "iPhone 16" com.guyghost.wakeve`

Verify:
- Circular category selectors display correctly
- Scenario grid shows 6 colorful gradient cards in 2 columns
- Tapping a card opens the detail page with gradient header
- Trending/Recommended sections still appear below
- Search still works

**Step 3: Commit any fixes if needed**
