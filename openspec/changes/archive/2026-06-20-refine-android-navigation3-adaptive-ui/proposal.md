# Change: Refine Android adaptive UI and Navigation 3 readiness

## Why
Wakeve's Android event workspace is now Compose-first and has a basic list-detail layout, but the next adaptive pass needs to be more precise: navigation chrome must switch by capability, event cards and filters must adapt to available space, state must survive rotation, and screenshot coverage must prove phone, foldable, tablet, and desktop-size behavior.

## What Changes
- Audit current Android screens for remaining adaptive problems and document findings.
- Add adaptive UI helpers that classify width, height, pointer precision, and content space without hardcoding device categories.
- Introduce an adaptive navigation scaffold that uses bottom navigation on compact width and a navigation rail when larger width is available.
- Keep current Navigation 2 runtime only where Navigation 3 cannot be fully adopted safely in this pass, and add migration notes aligned with the official Navigation 3 state-driven back stack model.
- Refine event list/detail:
  - single pane on narrow windows,
  - side-by-side list + detail on larger windows,
  - "Select an event" placeholder when no event is selected.
- Make event cards use an adaptive grid: 1 column narrow, 2 columns medium, and 3+ columns on large windows.
- Make filters horizontally scroll on small containers and wrap from 600dp container width.
- Improve landscape behavior with reduced chrome, edge-to-edge support, and state preservation through rotation.
- Add previews and screenshot/UI tests for phone portrait, phone landscape, foldable, tablet, and desktop-size windows.

## Impact
- Affected specs: `android-ui-system`
- Affected code:
  - `composeApp/src/androidMain/kotlin/com/guyghost/wakeve/navigation/`
  - `composeApp/src/androidMain/kotlin/com/guyghost/wakeve/theme/`
  - `composeApp/src/commonMain/kotlin/com/guyghost/wakeve/ui/designsystem/`
  - `composeApp/src/commonMain/kotlin/com/guyghost/wakeve/ui/event/`
  - `composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/event/`
  - `composeApp/src/androidInstrumentedTest/kotlin/com/guyghost/wakeve/`
