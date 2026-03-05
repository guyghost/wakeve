# Explore Page - Scenarios/Templates

**Date**: 2026-02-24
**Status**: Approved
**Scope**: iOS only (Android + Web follow-up)

## Inspiration

Apple Fitness+ "A decouvrir" section: 2-column grid of colorful gradient cards with scenario titles, plus horizontal circular activity type selectors.

## Changes

### 1. Replace Category Chips with Circular Type Selectors

Replace the current `CategoryChipsRow` (capsule-shaped filter chips) with horizontal scrollable circles containing SF Symbol icons on tinted backgrounds, with labels below. Tapping still filters the page content.

| Type | Icon | Tint Color |
|------|------|------------|
| Tous | square.grid.2x2.fill | Gray |
| Social | person.2.fill | Blue |
| Sport | figure.run | Green |
| Culture | book.fill | Purple |
| Professionnel | briefcase.fill | Orange |
| Food & Drink | fork.knife | Red |
| Bien-etre | heart.fill | Pink |

### 2. "A decouvrir" Grid - 6 Scenario Cards

2-column LazyVGrid with colorful gradient backgrounds, white bold text. Each card is ~160pt tall.

| # | Title | Subtitle | Gradient | EventType |
|---|-------|----------|----------|-----------|
| 1 | Organisez un brunch entre amis | Trouvez le meilleur moment pour bruncher | Orange -> Red | FOOD_TASTING |
| 2 | Planifiez votre team building | Motivez votre equipe | Blue -> Cyan | TEAM_BUILDING |
| 3 | Fete d'anniversaire surprise | Coordonnez la surprise parfaite | Pink -> Purple | BIRTHDAY |
| 4 | Soiree cinema maison | Invitez vos amis pour une soiree film | Dark Green -> Green | PARTY |
| 5 | Weekend randonnee | Planifiez une escapade nature | Teal -> Blue | OUTDOOR_ACTIVITY |
| 6 | Afterwork entre collegues | Decompressez ensemble | Purple -> Indigo | PARTY |

### 3. Scenario Detail Page

Opens on tap. Contains:
- Full-width gradient header matching card color
- Scenario title and detailed description (3-4 sentences)
- Suggested checklist items for planning
- "Creer cet evenement" CTA button that navigates to event creation pre-filled with type, suggested title, and description

### 4. Existing Sections Preserved

Trending, Nearby, and Recommended sections remain below the grid, unchanged.

## Data

All scenario data is static/hardcoded. No backend changes needed.

## Files to Modify

- `iosApp/src/Views/ExploreTabView.swift` - Replace chips with circles, add scenario grid
- `iosApp/src/Views/ScenarioDetailView.swift` - New detail page
- `iosApp/src/ViewModels/ExploreViewModel.swift` - Add scenario data model

## Out of Scope

- Android implementation
- Web implementation
- Backend API for scenarios
- Dynamic/personalized scenarios
