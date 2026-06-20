# ios-design-system Specification

## Purpose
Define Wakeve's iOS design system, including native navigation conventions, branded content surfaces, Liquid Glass fallbacks, accessibility behavior, motion, typography, color, and documentation expectations.

## Requirements
### Requirement: Centralized iOS Design Tokens
The iOS application SHALL expose a centralized design token system for semantic colors, typography, spacing, radius, blur, opacity, elevation/depth, motion timing/easing, shadows, glass materials, and event imagery/gradients.

The token system SHALL make dark mode the default design target while fully supporting light mode, increased contrast, dynamic text sizes, Reduce Motion, and Reduce Transparency. The primary visual identity SHALL avoid saturated purple dominance and prefer a balanced palette of graphite, midnight blue, soft ivory, muted lavender, pale blue, and warm amber accents used sparingly for selected states, primary actions, progress, and confirmation feedback.

#### Scenario: A screen needs semantic styling
- **WHEN** an iOS SwiftUI screen needs a background, surface, text, border, separator, accent, destructive, progress, confirmation, or state color
- **THEN** it SHALL use centralized semantic iOS design tokens instead of local hard-coded palettes
- **AND** the token SHALL adapt to dark mode, light mode, increased contrast, and reduced transparency where applicable.

#### Scenario: A screen needs event theming
- **WHEN** an iOS event screen renders an event preview, invitation, detail header, or poll experience
- **THEN** it SHALL use event imagery or an event gradient token selected from the event type or explicit event background
- **AND** text and controls over the image or gradient SHALL preserve readable contrast in dark and light mode.

### Requirement: Reusable iOS Surface Components
The iOS application SHALL provide reusable SwiftUI components for screen backgrounds, Liquid Glass cards, Liquid Glass buttons, toolbars, tab bars, bottom sheets, action buttons, circular icon buttons, search fields, list rows, avatars, section headers, vote controls, empty states, and loading skeletons.

Cards and surfaces SHALL be meaningful UI structures, not generic decoration. The design system SHALL avoid double-nested cards unless required for clear grouping, and SHALL prefer whitespace when a container does not add meaning.

#### Scenario: A screen renders a card or grouped row
- **WHEN** a SwiftUI screen needs a card, grouped list row, or elevated content surface
- **THEN** it SHALL use the shared surface component with standardized radius, material, border, shadow, blur, and fallback behavior
- **AND** the surface SHALL have a clear purpose in the screen hierarchy.

#### Scenario: A screen renders primary or secondary actions
- **WHEN** a SwiftUI screen renders a primary, secondary, neutral, destructive, or event-next action
- **THEN** it SHALL use the shared action component so sizing, capsule shape, typography, disabled state, glass treatment, haptics, and contrast are consistent.

#### Scenario: A screen has no content or is loading
- **WHEN** a SwiftUI screen has no events, no search results, no participants, no transport, or is loading data
- **THEN** it SHALL render a shared EmptyState or LoadingSkeleton component that keeps layout stable and provides one clear next action when an action is available.

### Requirement: Screenshot-Inspired Experience Consistency
The iOS application SHALL align onboarding, home, event detail, notification, profile, friend/participant management, poll voting, transport, messages, and event preview experiences with a premium Apple-style visual system.

Each screen SHALL have one clear intention, an explicit orientation point, predictable next action, and progressively disclosed secondary actions. The UI SHALL avoid exposing every feature at once and SHALL group features naturally into Events, Groups, Messages, Profile, and event-specific sections.

#### Scenario: A user moves between high-visibility iOS screens
- **WHEN** the user navigates between Home, Event Detail, Vote, Transport, Participants, Messages, Create Event, Profile, and event preview screens
- **THEN** the screens SHALL preserve consistent typography hierarchy, native toolbar behavior, glass surfaces, icon sizing, dark-mode behavior, motion language, and contextual action placement.

#### Scenario: A utility screen uses system controls
- **WHEN** an iOS screen uses a `Form`, toggles, date pickers, search, menus, or system rows
- **THEN** it SHALL still use the design system's accent, text, section, toolbar, and background tokens while preserving native control behavior.

### Requirement: Accessible Liquid Glass Fallbacks
The iOS design system SHALL support native Liquid Glass where available and accessible fallbacks where Liquid Glass, transparency, motion, or contrast conditions require alternatives.

Native Liquid Glass SHALL be applied to interactive controls and meaningful elevated surfaces with consistent shapes. Decorative glass noise, unreadable blur, and permanent secondary actions SHALL be avoided.

#### Scenario: The OS supports native Liquid Glass
- **WHEN** the app runs on an iOS version with native Liquid Glass support
- **THEN** shared glass components SHALL use the native glass API with consistent shapes, grouped containers where appropriate, and interactive treatment only for tappable or focusable elements.

#### Scenario: Transparency or motion is reduced, or native glass is unavailable
- **WHEN** Reduce Transparency is enabled, Reduce Motion is enabled, increased contrast is enabled, or native Liquid Glass is unavailable
- **THEN** shared glass components SHALL render opaque or material-backed fallbacks that preserve contrast, hierarchy, hit targets, and orientation.

### Requirement: iOS Design System Documentation
The project SHALL document the iOS design system in the `docs/` tree.

#### Scenario: A developer builds or updates an iOS screen
- **WHEN** a developer needs to choose tokens, components, layout patterns, or migration rules for an iOS screen
- **THEN** the documentation SHALL provide token tables, component guidance, screenshot-inspired principles, accessibility rules, and migration checklists.

### Requirement: Destination-Only iOS Tab Navigation
The iOS application SHALL provide a predictable tab bar containing only navigation destinations. The tab bar SHALL include Home, Groups, Messages, and Profile. It SHALL NOT include Create Event as a permanent tab destination.

Create Event SHALL appear contextually as a floating Liquid Glass button on Home, a toolbar action where directly relevant, or a bottom sheet action when the user is already in a flow.

#### Scenario: User opens the app tab bar
- **WHEN** the authenticated user views the main iOS tab bar
- **THEN** the tab bar SHALL show Home, Groups, Messages, and Profile as destinations
- **AND** each tab SHALL use an explicit label and a clean line icon
- **AND** no tab item SHALL represent a one-off action such as creating an event.

#### Scenario: User creates an event from Home
- **WHEN** the user is on Home and wants to create an event
- **THEN** a contextual floating Liquid Glass create action SHALL be available
- **AND** activating it SHALL open the create event flow without changing the tab model.

### Requirement: Premium iOS Screen Intent Hierarchy
Each major iOS screen SHALL be designed around one clear intention, a clear toolbar/title, a primary next action, and progressive disclosure for secondary actions.

Home SHALL prioritize greeting, featured next event, upcoming events grouped by relevance or date, and a subtle empty state. Event Detail SHALL prioritize immersive event image, event title/date/place, participants preview, urgent next action, and progressive sections for Overview, Participants, Vote, Transport, and Messages. Vote, Transport, Participants, Messages, and Create Event SHALL each follow their specific intent hierarchy.

#### Scenario: User lands on Home
- **WHEN** the user opens Home with upcoming events
- **THEN** the screen SHALL show a greeting, one featured event as the visual anchor, upcoming events grouped by relevance or date, and a floating contextual create action
- **AND** it SHALL NOT mix unrelated modules such as votes, transport, tasks, and chat previews at the same visual priority.

#### Scenario: User opens Event Detail
- **WHEN** the user opens an event detail screen
- **THEN** the screen SHALL show an immersive event image or event visual, title, date, place, participants preview, and the most urgent next action
- **AND** secondary sections SHALL be progressively revealed or organized as Overview, Participants, Vote, Transport, and Messages.

#### Scenario: User votes on an event option
- **WHEN** the user opens the voting flow
- **THEN** the screen SHALL present one question, response progress, clear options, selected state, confirmation feedback, and one primary glass capsule action.

#### Scenario: User creates an event
- **WHEN** the user starts the create event flow
- **THEN** the flow SHALL use lightweight steps for name, date, place, invite people, and confirm
- **AND** each step SHALL contain one main task, a strong title, a large input or selection control, a progress indicator, and one contextual next action.

### Requirement: Premium iOS Motion and Interaction States
The iOS application SHALL use subtle, physical, useful motion and micro-interactions for navigation, bottom sheets, voting selection, confirmation, pull-to-refresh, long press previews, tab switching, empty states, and loading states.

Motion SHALL feel native and responsive, target 60fps, and respect Reduce Motion. Loading states SHALL use stable blurred or material skeletons rather than disruptive spinners wherever possible.

#### Scenario: User selects a vote option
- **WHEN** the user selects a vote option
- **THEN** the selected option SHALL provide a subtle expansion or emphasis, haptic feedback where appropriate, and confirmation feedback without shifting unrelated layout.

#### Scenario: User opens a bottom sheet
- **WHEN** the user opens a bottom sheet for contextual actions or advanced options
- **THEN** the sheet SHALL animate with native depth, background scale or blur where appropriate, and a stable readable fallback when motion or transparency is reduced.

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
