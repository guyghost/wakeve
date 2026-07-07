# App Store Review Guideline Audit - Wakeve

Date: 2026-05-27

Status: NOT READY

This audit maps the first App Store submission risks to repository evidence and manual gates. It complements `docs/APP_STORE_READINESS.md`, `docs/APP_STORE_SUBMISSION_RUNBOOK.md`, and `docs/APP_STORE_FINAL_SIGNOFF.md`.

## Apple Sources Checked

- Last checked: 2026-05-27. The App Review Guidelines page reports last update: 2026-02-06.
- App Review Guidelines: https://developer.apple.com/app-store/review/guidelines/
- App submission SDK requirements: https://developer.apple.com/app-store/submitting/
- Accessibility Nutrition Labels overview: https://developer.apple.com/help/app-store-connect/manage-app-accessibility/overview-of-accessibility-nutrition-labels/
- Accessibility Nutrition Labels management: https://developer.apple.com/help/app-store-connect/manage-app-accessibility/manage-accessibility-nutrition-labels
- EU Digital Services Act trader requirements: https://developer.apple.com/help/app-store-connect/manage-compliance-information/manage-european-union-digital-services-act-trader-requirements

## Apple Source Baseline

Apple-source review date: 2026-05-27.

- Apple says every app, app update, bundle, in-app purchase, and in-app event submitted to App Store Connect is reviewed for privacy, security, safety, and reliability.
- Apple says apps with user-generated content must include filtering for objectionable material, reporting with timely responses, blocking for abusive users, and published contact information.
- Apple says submissions to App Review should be final versions with all necessary metadata and fully functional URLs, with placeholder text, empty websites, and temporary content removed.
- Apple says apps should be tested on device for bugs and stability before submission, and login apps must include demo account information or an approved built-in demo mode.
- Apple says metadata should accurately describe the app and should not include hidden, dormant, or undocumented features.
- Apple says apps that use third-party or social login services for the primary account must also offer an equivalent login option with privacy-friendly account setup characteristics.
- Apple says apps that support account creation must also let users initiate account deletion from within the app.
- Apple says user data collection, storage, and use must comply with App Store privacy requirements and privacy responses must be accurate.
- Apple says in-app purchase is required for digital content, features, subscriptions, consumables, or other digital goods unlocked in the app unless an allowed exception applies.
- Apple says apps may use purchase methods other than in-app purchase for goods or services consumed outside the app.
- Apple says apps uploaded to App Store Connect must meet the current minimum SDK requirements; starting April 2026, iOS and iPadOS apps must be built with the iOS and iPadOS 26 SDK or later.
- Apple says Accessibility Nutrition Labels help users understand accessibility support before download and become visible on App Store product pages on OS 26-era platforms.
- Apple says developers distributing in the European Union must provide Digital Services Act trader status information and related contact details where required.
- Apple says App Review rejections and appeals are handled through App Store Connect communication with the App Review team.

## Review Gate Matrix

| Area | Current evidence | Submission decision |
| --- | --- | --- |
| Guideline 1.2 User-Generated Content | Local implementation evidence now covers server filtering for comments, chat, event text, potential locations, meal planning, and budget item text; report/block/unblock endpoints; block-filtered reads, WebSocket delivery, and notifications; iOS report/block/unblock entry points; hidden/pending/rejected states; and a final-audit UGC gate regression script. OpenSpec `openspec/changes/add-ugc-moderation-controls/` remains open for App Store evidence and final validation. | Do not submit until `APP_STORE_UGC_MODERATION_CONFIRMED=true` is backed by uploaded-build device/API evidence and `docs/APP_STORE_UGC_MODERATION_EVIDENCE.md` is complete. |
| Guideline 2.1 App Completeness | Local Release build without signing passes, metadata validates, iOS screenshots exist, and review notes explain guest access. Live URLs, signed archive, App Review phone, TestFlight smoke tests, and production services remain incomplete. | Do not submit yet. |
| Guideline 2.3 Accurate Metadata | Fastlane metadata, screenshots, privacy/support URLs, age rating, copyright, review notes, and no on-device-only privacy claim are linted. | Ready locally, pending live URL and App Store Connect manual verification. |
| Guideline 3.1.1 In-App Purchase / Guideline 3.1.3 Other Purchase Methods | Wakeve has payment pot, settlement, and Tricount handoff surfaces. App Review notes now explain the real-world shared-expense scope and no digital unlocks, but the review build still has not been manually verified. | Do not submit until `APP_STORE_PAYMENT_COMPLIANCE_CONFIRMED=true` is backed by TestFlight/App Review notes evidence. |
| Guideline 4.8 Login Services | iOS exposes Sign in with Apple and the entitlement is declared because the app also has Google login. | Ready locally, pending Apple Developer capability/profile verification. |
| Guideline 5.1.1 Privacy | Privacy policy, App Store privacy-label draft, privacy manifest, no IDFA/ATT usage, purpose strings, and required-reason APIs are checked locally. | Pending product/legal approval and production-backend confirmation. |
| Guideline 5.1.1(v) Account deletion | Wakeve supports account creation through email, OAuth, guest sessions, and Sign in with Apple. Local implementation evidence now covers the iOS Profile -> Data Management deletion path, authenticated `DELETE /api/user/delete`, local cleanup wiring, backend deletion/anonymization, session/token/push-token cleanup, and Apple revocation attempt/failure handling. Uploaded-build App Review evidence remains incomplete. | Do not submit until `APP_STORE_ACCOUNT_DELETION_CONFIRMED=true` is backed by uploaded-build device/API evidence. |
| App Store SDK minimum | Local toolchain is Xcode 27.0 with iOS SDK 27.0; the linter checks the SDK upload minimum. | Ready locally. |
| Accessibility Nutrition Labels | Conservative draft exists and linter prevents unsupported claims before TestFlight/device evidence. | Do not publish labels until iPhone/iPad evidence exists, or leave labels unpublished where allowed. |
| Digital Services Act | DSA trader status decision is documented separately. | Pending App Store Connect confirmation or EU storefront decision. |

## Guideline 1.2 UGC Readiness Gate

Wakeve includes user-generated content surfaces: event comments, chat messages, event titles/descriptions, locations, and planning details. Apple requires apps with user-generated content to include moderation protections before App Review.

Do not submit until there is current evidence for all of the following:

- Filtering: objectionable content can be filtered or prevented before it is posted, or a server-side moderation policy is active for comments, chat, event text, potential locations, and planning free-text.
- Report: users can report offensive comments, chat messages, event text, or users from the app or a clearly linked support path that includes the content/user identifier.
- Block: users can block abusive users or otherwise prevent continued unwanted contact from the abusive user.
- Published contact information: `support@wakeve.app` is live and reachable from the App Store support URL and in-app support path.
- Timely response process: the release owner has an operational process for reviewing abuse reports during TestFlight and App Review.
- Reviewer evidence: App Review notes or demo data explain how to find and verify the moderation/report/block flows if they are not obvious.

Set `APP_STORE_UGC_MODERATION_CONFIRMED=true` only after this evidence is recorded in `docs/APP_STORE_UGC_MODERATION_EVIDENCE.md` and referenced from `docs/APP_STORE_FINAL_SIGNOFF.md`.

Latest local evidence on 2026-06-13:

- `./gradlew :server:test --tests com.guyghost.wakeve.routes.UgcModerationRoutesTest` passed and covers hard-policy rejection, pending-review hiding, reporting, block/unblock filters, notification suppression, WebSocket block-filter wiring, and potential-location moderation.
- `xcodebuild test -project iosApp/iosApp.xcodeproj -scheme WakeveApp -destination 'platform=iOS Simulator,name=iPhone 17' -only-testing:WakeveTests/FindingsRegressionTests/testUgcModerationReportBlockControlsAreReviewerVisible` passed and covers reviewer-visible iOS moderation entry points and states.
- `./scripts/test-app-store-ugc-gates.sh` passed and proves the final audit still rejects missing `APP_STORE_UGC_MODERATION_CONFIRMED`, incomplete UGC OpenSpec tasks, and incomplete UGC evidence even when the signoff is forced.

## Guideline 3.1 Payment compliance gate

Wakeve includes shared-expense payment surfaces, payment pots, settlement suggestions, and Tricount external handoff links. App Review must be able to distinguish this from selling digital goods or unlocking app features outside App Store in-app purchase.

Do not submit until there is current evidence for all of the following:

- Scope: payment and Tricount surfaces are limited to real-world event expenses, shared costs, or services consumed outside the app.
- No digital unlock: No external payment flow unlocks app features, subscriptions, digital content, digital credits, boosts, or premium functionality.
- Metadata and copy: app metadata, screenshots, and in-app copy do not encourage users to bypass App Store in-app purchase for digital goods or services.
- Review explanation: App Review notes explain the business model and where the reviewer can inspect payment surfaces if they are enabled.
- Link safety: Tricount/provider URLs are HTTPS-only and trusted-domain validated before opening.
- Future monetization: if Wakeve later sells digital features, a separate release must use App Store in-app purchase or document the applicable external-purchase entitlement/legal basis for each storefront.

Set `APP_STORE_PAYMENT_COMPLIANCE_CONFIRMED=true` only after this evidence is recorded in `docs/APP_STORE_FINAL_SIGNOFF.md`. See `docs/APP_STORE_PAYMENT_COMPLIANCE.md`.

## Guideline 5.1.1(v) Account Deletion Gate

Wakeve supports account creation and account-backed use through email OTP, Google login, Sign in with Apple, and guest sessions. Apple requires apps that support account creation to let users initiate account deletion within the app.

Do not submit until there is current evidence for all of the following:

- In-app initiation: the iOS app exposes a clearly named Delete Account action that is easy to find from Profile Settings or equivalent account settings.
- Confirmation: the flow clearly explains that deletion is permanent and asks the user to confirm before starting deletion.
- Scope: the backend deletion path deletes the entire account record and associated personal data that Wakeve is not legally required to retain. Temporary deactivation alone is not sufficient.
- Completion messaging: if deletion is asynchronous or manually fulfilled, the app tells the user how long deletion will take and confirms completion through an appropriate channel.
- Sign in with Apple token revocation: accounts created with Sign in with Apple revoke user tokens as part of the deletion process where applicable.
- Guest account handling: automatically generated guest accounts can also be deleted with their associated data.
- Reviewer evidence: App Review notes explain exactly where the reviewer can initiate deletion and what confirmation/result they should see.

Set `APP_STORE_ACCOUNT_DELETION_CONFIRMED=true` only after this evidence is recorded in `docs/APP_STORE_ACCOUNT_DELETION_EVIDENCE.md` and referenced from `docs/APP_STORE_FINAL_SIGNOFF.md`.

## First Submission Recommendation

Keep comments/chat disabled for the App Store review build unless the UGC readiness gate is complete. If comments/chat remain enabled, treat missing report/block/filter evidence as a submission blocker, not a warning. Do not submit any build with account creation until the account deletion gate is complete. Do not submit payment or Tricount surfaces until the payment compliance gate proves they are for real-world shared-event expenses and do not unlock digital app functionality.
