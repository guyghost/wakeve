# Implementation Notes: Premium iOS Liquid Glass UX

## Design Decisions

- The refactor stays presentation-only on iOS. Shared KMP repositories, state machines, access checks, sync metadata, and existing ViewModels remain the source of business behavior.
- Premium Liquid Glass primitives are centralized in `PremiumLiquidGlassComponents.swift` and semantic tokens are centralized in `DesignSystem.swift`.
- Create Event is contextual and uses a five-step wizard: name, date, place, invite people, confirm.
- Messages now presents event-based conversation previews over the existing `InboxViewModel` data, preserving notification read/delete behavior.
- Transport and Participants keep their existing repository and workflow guards while moving to route-first and grouped presentation.

## Verification Evidence

- `mcp__xcodebuildmcp.build_sim`: succeeded after Messages and Create Event refactors.
- `mcp__xcodebuildmcp.test_sim`: `122 passed, 0 failed, 0 skipped`.
- `mcp__xcodebuildmcp.build_run_sim` with `--wakeve-debug-authenticated`: succeeded.
- `openspec validate refactor-ios-premium-liquid-glass-ux --strict`: succeeded after checklist update.

## Visual Checks

- Home light mode screenshot: `/var/folders/1t/456kc0651bl7mgrc62_m43g80000gn/T/screenshot_optimized_c35e720b-5bea-428d-a9ec-23ef61dd5631.jpg`
- Home dark mode screenshot: `/var/folders/1t/456kc0651bl7mgrc62_m43g80000gn/T/screenshot_optimized_b4aaf4f7-d373-4d24-bd30-763a4493e7e0.jpg`
- Messages dark mode empty/search screen: `/var/folders/1t/456kc0651bl7mgrc62_m43g80000gn/T/screenshot_optimized_0194851a-316c-461a-b72b-4ba4880e6fe8.jpg`
- Create Event dark mode wizard step 1: `/var/folders/1t/456kc0651bl7mgrc62_m43g80000gn/T/screenshot_optimized_88ed0b9d-9a61-4250-837f-0ae1335678a8.jpg`
- Event Detail dark mode screen: `/var/folders/1t/456kc0651bl7mgrc62_m43g80000gn/T/screenshot_optimized_6d425e66-5b92-4647-9139-36371e18357b.jpg`

Runtime UI snapshots also verified:
- Home tab labels and contextual create action.
- Messages tab with search field and empty state.
- Create Event wizard step `1 / 5`.
- Event Detail toolbar, summary, and next action.
