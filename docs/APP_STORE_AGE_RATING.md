# App Store Age Rating - Wakeve

Date: 2026-05-27

This file records the App Store Connect age-rating questionnaire evidence for Wakeve. Fastlane uploads the current answers from `composeApp/metadata/ios/app_rating_config.json`, but App Store Connect may still show the generated global age rating and operating-system-specific display values for manual review.

Apple's 2026 submission guidance says App Store ratings now use the updated age-rating system, and developers must answer the updated questions in App Store Connect before submitting updates. Treat the JSON below as a source-controlled draft only; the final release owner must compare it with the live App Store Connect questionnaire before App Review submission.

## Apple Source Baseline

Apple-source review date: 2026-05-27.

- Apple says age rating is a required app information property that helps users assess content and potentially objectionable material and supports parental controls.
- Apple says developers determine the age rating by responding to the App Store Connect age rating questionnaire.
- Apple says the questionnaire covers content descriptors, in-app controls, and capabilities, including the frequency or presence of each content type.
- Apple says App Store Connect translates questionnaire answers into an Apple global age rating plus region-specific ratings where required.
- Apple says age ratings may vary based on OS version, including Apple devices running iOS 26, iPadOS 26, macOS Tahoe 26, tvOS 26, visionOS 26, and watchOS 26 or later.
- Apple says an Unrated app cannot be published on the App Store.
- Apple says the updated age rating questions had to be answered by January 31, 2026 to avoid an interruption when submitting app updates in App Store Connect.
- Apple says Unrestricted Web Access means users can navigate to any webpage within the app or freely browse the web.
- Apple says the 4+ rating can still include capabilities such as user-generated content, messaging and chat, advertising, parental controls, and age assurance under the updated system.
- Apple says Made for Kids can only be selected when the calculated rating is 4+ or 9+, cannot be changed after App Review approval, and requires subsequent updates to follow Kids category guidelines.
- Apple says Made for Kids cannot be selected for visionOS apps and Kids category apps cannot be made available on visionOS.
- Apple says developers can override to a higher age rating when the app's content, features, or EULA age requirements need a higher rating.
- Apple says the override applies in all regions where the app is available and may map to different region-specific rating values.
- Apple says App Store Connect shows global and region-specific ratings after saving the questionnaire.
- Apple says Korea region-specific rating overrides may require GRAC-issued rating details and App Review information.

## Current Draft

Status: draft, pending App Store Connect verification.

Current `app_rating_config.json` answers:

- Alcohol, tobacco, or drug use/references: `NONE`
- Contests: `NONE`
- Simulated gambling: `NONE`
- Gambling: `false`
- Medical or treatment information: `NONE`
- Profanity or crude humor: `NONE`
- Sexual content or nudity: `NONE`
- Graphic sexual content and nudity: `NONE`
- Horror or fear themes: `NONE`
- Mature or suggestive themes: `NONE`
- Cartoon or fantasy violence: `NONE`
- Realistic violence: `NONE`
- Prolonged graphic or sadistic realistic violence: `NONE`
- Unrestricted web access: `false`
- Seventeen plus override: `false`
- Kids category age band: not set

## Product Evidence

- Wakeve is an event-planning utility, not a game, gambling product, medical product, dating product, marketplace, or unrestricted web browser.
- Payment, settlement, and Tricount surfaces are scoped to real-world shared-event expenses and do not unlock digital goods or gambling.
- User-generated content exists in event titles, descriptions, comments/chat, locations, and messages. This does not currently require a restricted-content age answer, but it does require the separate user-generated content moderation gate in `docs/APP_STORE_REVIEW_GUIDELINE_AUDIT.md`.
- Location usage is When In Use only and supports event planning. It is not background tracking.
- External links are limited to legal/support URLs and trusted provider handoff URLs documented in `docs/APP_STORE_PAYMENT_COMPLIANCE.md`; the app does not expose unrestricted web browsing.

## Required Evidence

Before final submission:

- Compare the App Store Connect generated age rating against `composeApp/metadata/ios/app_rating_config.json`.
- Confirm any new App Store Connect age-rating questionnaire questions introduced after this draft are answered honestly.
- Confirm the App Store Connect questionnaire is the updated 2026 age-rating questionnaire and that every new required answer is represented in the final evidence or added to `app_rating_config.json`.
- Confirm the generated rating is acceptable for the selected app category, countries/regions, and device availability.
- If App Store Connect shows a higher rating than expected, record the reason and do not override downward without legal/product approval.
- Record reviewer/date and App Store Connect screenshot or export reference in `docs/APP_STORE_FINAL_SIGNOFF.md`.

## Verification

Run:

```bash
./scripts/lint-store-metadata.sh --ios-only
bundle exec fastlane ios validate_metadata
```

The final App Store upload lane passes `app_rating_config_path: repo_path("composeApp/metadata/ios/app_rating_config.json")` to Fastlane `deliver`.

## Apple Reference

- Set an app age rating: https://developer.apple.com/help/app-store-connect/manage-app-information/set-an-app-age-rating
- Age ratings reference: https://developer.apple.com/help/app-store-connect/reference/age-ratings/
- 2026 age rating update: https://developer.apple.com/news/upcoming-requirements/
