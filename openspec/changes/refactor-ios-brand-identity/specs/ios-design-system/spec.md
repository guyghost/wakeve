## ADDED Requirements
### Requirement: Native UI Layer and Branded Content Layer Separation
The iOS application MUST separate platform UI chrome from Wakeve branded content surfaces. Navigation bars, tab bars, toolbars, context menus, search, forms, sheets, alerts, and standard system actions MUST remain familiar, predictable, and iOS-native. Event, group, invitation, voting, transport, message, empty-state, loading, preview, and widget surfaces MUST be the primary places where Wakeve expresses brand personality.

The application MUST NOT make one-off actions such as create event into permanent tab destinations, MUST NOT repeat the logo in every toolbar, and MUST NOT tint native navigation surfaces decoratively when the tint does not communicate state or hierarchy.

#### Scenario: User navigates through app chrome
- **WHEN** a user uses tabs, navigation bars, toolbar actions, menus, search, or sheets
- **THEN** those controls MUST use native SwiftUI/iOS interaction patterns, familiar labels, and standard action placement
- **AND** Wakeve brand styling MUST remain restrained to semantic accenting rather than overriding platform conventions.

#### Scenario: User views event or social content
- **WHEN** a user views an event card, event hero, event detail header, invitation preview, vote summary, transport card, group card, message preview, empty state, loading state, or widget preview
- **THEN** the surface MUST be allowed to express Wakeve identity through mood palettes, imagery, gradients, social details, human microcopy, progress, and decision state
- **AND** the surface MUST preserve readable contrast, Dynamic Type, and native interaction feedback.

### Requirement: Mature Wakeve Color and Mood System
The iOS application MUST use a mature Wakeve color system composed of brand tokens, semantic tokens, and event mood palettes. The palette MUST reduce decorative saturated purple dominance and prefer midnight blue, graphite, soft ivory, warm peach, muted lavender, subtle amber, and blue grey. Color MUST have an explicit function such as hierarchy, status, grouping, selection, CTA, progress, feedback, or event mood.

Brand color MUST live primarily in content and semantic accents. Native toolbars and tab bars MUST avoid decorative brand backgrounds unless the color communicates a selected state or system-supported tint.

#### Scenario: A screen needs reusable brand color
- **WHEN** SwiftUI code needs Wakeve brand color, semantic UI color, or event mood color
- **THEN** it MUST use centralized tokens such as `BrandColor`, `SemanticColor`, or `EventMoodPalette`
- **AND** the selected token MUST adapt to light mode, dark mode, increased contrast, and reduced transparency when applicable.

#### Scenario: An event has a recognizable mood
- **WHEN** an event represents an evening, travel, birthday, family, dinner, beach, weekend, or equivalent social mood
- **THEN** the event content surface MUST select a suitable mood palette or imagery treatment
- **AND** the palette MUST avoid saturated dark-mode glare and pure-white light-mode surfaces.

### Requirement: Wakeve Voice, Logo, Iconography, and Motion Restraint
The iOS application MUST define and apply a calm, warm, clear Wakeve voice in content surfaces while preserving clarity for functional actions. Standard actions such as share, delete, edit, search, back, close, and add MUST use SF Symbols. Custom icons MAY be used only for Wakeve-specific concepts such as shared moments, social voting, group coordination, event mood, and shared travel.

The Wakeve logo MUST appear only in brand moments such as onboarding, splash, initial home, empty states, App Store/marketing contexts, or selected previews. Motion MUST be subtle, useful, responsive, and respectful of Reduce Motion.

#### Scenario: A user encounters standard actions
- **WHEN** the UI presents share, delete, edit, search, back, close, add, or other conventional actions
- **THEN** the action MUST use familiar SF Symbols and clear accessible labels
- **AND** custom brand icons MUST NOT replace symbols users expect from iOS.

#### Scenario: A user reaches a brand moment
- **WHEN** the user sees onboarding, first home, an empty state, an invitation preview, an event confirmation, a widget preview, or an App Store/marketing surface
- **THEN** the UI MAY use Wakeve logo, warmer microcopy, mood imagery, and custom visual details
- **AND** these elements MUST NOT consume useful space in routine navigation or repeated toolbars.

#### Scenario: A user triggers branded motion
- **WHEN** the user opens an event, creates an event, selects a vote, sees a participant arrive, confirms an invitation, changes event state, or moves from empty state to first event
- **THEN** motion MUST reinforce continuity, hierarchy, feedback, or emotion
- **AND** it MUST avoid long decorative animations, layout instability, and behavior that violates Reduce Motion.

### Requirement: iOS Brand Documentation and Preview Direction
The project MUST document Wakeve's iOS brand rules in the `docs/design/` tree, including an iOS brand audit, brand guidelines, voice and tone, and motion guidelines. The documentation MUST distinguish what remains native from what is custom and MUST identify where Wakeve's brand expression belongs.

The iOS brand system MUST include a direction for widgets or extension previews such as next event, active vote, guest list, notification preview, and possible Live Activity, even when implementation starts as a visual preview rather than a shipped extension target.

#### Scenario: A developer refactors an iOS component
- **WHEN** a developer changes an iOS navigation element, content card, event visual, empty state, icon, copy string, animation, widget preview, or branded surface
- **THEN** the design documentation MUST explain whether the element is native UI layer or branded content layer
- **AND** it MUST provide guidance for light mode, dark mode, Dynamic Type, iconography, logo use, and motion.

#### Scenario: The team reviews completion of the iOS brand refactor
- **WHEN** the refactor is reviewed
- **THEN** the deliverables MUST summarize what changed, what remains native, what is custom, where Wakeve brand expression appears, what trade-offs were made, and what improvements remain.
