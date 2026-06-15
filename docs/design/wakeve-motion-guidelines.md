# Wakeve Motion Guidelines for iOS

## Principle
Motion should clarify state, continuity, and feedback. It should never slow down coordination or distract from the group decision.

## Native First
Use native SwiftUI transitions and gestures for:
- navigation pushes and pops
- sheets
- menus
- tab switching
- search
- form interactions

Custom motion belongs mainly in content moments.

## Brand Motion Moments
Wakeve can use subtle branded motion when:
- an event opens from a card into detail
- a vote option is selected
- an invitation is confirmed
- a participant joins or confirms
- an event changes phase
- an empty state turns into the first event

## Timing
- Quick feedback: about 0.16 seconds.
- Standard content transition: about 0.26 seconds.
- Sheet or large panel movement: about 0.36 seconds.
- Confirmation emphasis: about 0.42 seconds.

Prefer spring animations with enough damping to feel physical without bouncing excessively.

## Reduce Motion
When Reduce Motion is enabled:
- remove decorative scale and parallax
- replace complex transitions with opacity or no animation
- preserve immediate feedback through color, symbol, or text state

## Loading
Prefer stable skeletons over disruptive spinners for content areas. Spinners are acceptable for launch, blocking authentication checks, or very short operations.

## Avoid
- confetti without a meaningful milestone
- long celebratory sequences
- layout shifts after selection
- motion on every repeated row
- animated backgrounds behind dense text
