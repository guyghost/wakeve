# App Store Review Access Evidence - Wakeve

Date: 2026-05-28

Status: PENDING

Do not change the marker below until the exact uploaded App Store Connect review build gives App Review a reliable way to inspect the app without repository-committed passwords.

APP_STORE_REVIEW_ACCESS_EVIDENCE_COMPLETE=false

## Apple Source Baseline

Last checked: 2026-05-28.

- Apple says App Review Information in App Store Connect should include all details needed for review.
- Apple says if some features require signing in, the submission should provide a valid demo account username and password.
- Apple says apps with account-based features should include either an active demo account or a fully featured demo mode, plus any other resources needed to review the app.
- Apple says App Store review details include contact information, demo account information, notes, and review attachments.
- Apple says required App Store review details include contact first and last name, contact phone number, contact email address, whether testing requires a demo account, demo account name/password if the app uses single sign-on, and optional notes.
- Apple says App Review contact information includes name, email, and phone number for the person App Review can contact if it needs more information.
- Apple says the demo account used during App Review must not expire.
- Apple says App Review notes can provide additional information that helps reviewers understand the app, including app-specific settings, test registration, account details, and test stream URLs when applicable.
- Apple says the Notes field can contain up to 4000 bytes and can be written in any language.
- Apple says if an app uses a single sign-on service, demo account login information for that service should be included.
- Apple says details for additional accounts should be included in the Notes field.
- Apple says required metadata must be provided and the right build must be added before submitting an app version for review.
- Apple says the required role to submit an app is Account Holder, Admin, or App Manager.
- Apple says clicking Add for Review changes the app status to Ready for Review, but the submission is not sent to App Review until Submit for Review is clicked.
- Apple says App Review messages can be managed in the App Review section, and replies may include attachments such as screenshots and supporting documents until resubmission.
- Apple says every app version and its content are reviewed to ensure a safe and trustworthy experience.
- Apple says submission is not sent to App Review until the app is explicitly submitted for review after the required items are prepared.
- Apple says app metadata and review information must accurately describe app behavior and review-relevant access.
- Apple says partnership documentation or authorization should be attached in App Store Connect when needed, with descriptions or links in Review Notes.

## Build Under Review

- App Store Connect version: TBD
- Build number: TBD
- Release commit: TBD
- Review notes source: `composeApp/metadata/ios/review_information/notes.txt`
- TestFlight evidence source: `docs/APP_STORE_TESTFLIGHT_EVIDENCE.md`
- Final signoff reference: `docs/APP_STORE_FINAL_SIGNOFF.md`

## Review Access Path

| Access Method | Required Evidence | Status | Notes |
| --- | --- | --- | --- |
| Guest access | App Review notes explain how to continue as guest, and the uploaded build exposes that path. | Local source ready; uploaded build pending | `LoginView` exposes `Continue as guest` / `Continuer en invité` outside DEBUG, creating a local-only guest session. |
| Demo credentials | Only needed if guest access cannot reach all reviewable surfaces. Enter credentials in App Store Connect manually; do not commit passwords. | Pending | TBD |
| Review details API fields | App Store Connect review detail fields for contact name/email/phone, demo-account requirement, demo account values if needed, notes, and attachments match this evidence. | Pending | TBD |
| Auth providers | Sign in with Apple and other login surfaces are usable or clearly optional for review. | Pending | TBD |
| Restricted surfaces | Account deletion, UGC moderation, payment, calendar, notifications, and Universal Links are reachable through guest data, demo data, or clear reviewer notes. | Pending | TBD |
| App Review communication | If App Review raises unresolved issues, the release owner can reply from App Store Connect and attach screenshots/supporting documents before resubmission. | Pending | TBD |

## Local Reviewer Access Scan Result

Local scan date: 2026-05-28

This is local repository and simulator evidence only. It does not complete App Review access for the uploaded App Store Connect review build.

- Review notes exist at `composeApp/metadata/ios/review_information/notes.txt` and currently describe a `"Continuer en invité"` / `"Continue as guest"` guest flow, local-only guest session behavior, offline event creation, local-first sync, payment/Tricount review notes, no digital feature unlocks, trusted-domain Tricount links, and third-party notices at `https://wakeve.app/third-party-notices`.
- Review notes byte length: `1548`, under Apple's 4000-byte Notes limit.
- Review contact metadata exists with `first_name=Wakeve`, `last_name=Support`, and `email_address=support@wakeve.app`.
- `composeApp/metadata/ios/review_information/` currently contains only `email_address.txt`, `first_name.txt`, `last_name.txt`, and `notes.txt`.
- No `demo_password.txt` is committed. No `demo_user.txt` is committed. No `phone_number.txt` is committed; the review phone is supplied through `APP_REVIEW_PHONE_NUMBER` or entered manually in App Store Connect.
- Fastlane maps the review notes and contact fields into `app_review_information_from_metadata`, and reads the phone from `APP_REVIEW_PHONE_NUMBER` or `composeApp/metadata/ios/review_information/phone_number.txt`.
- Local credential scan found no review-information file matching `*password*`, `*user*`, or `*phone*`.
- Local iPhone DEBUG simulator evidence exists at `docs/app-store-evidence/xcodebuildmcp-iphone-login-2026-05-27.jpg`.
- Updated local iPhone DEBUG simulator evidence exists at `docs/app-store-evidence/xcodebuildmcp-iphone-login-guest-2026-05-27.jpg`.
- The updated local accessibility hierarchy exposed `Se connecter`, `Sign in with Apple`, `Continue as guest`, `Development mode: Skip authentication`, `Read Privacy Policy`, and `Read Terms of Service`.
- `iosApp/src/Views/Auth/LoginView.swift` now renders `guestAccessButton` before the `#if DEBUG` development skip button.
- The release-visible guest button uses `auth.continue_as_guest` with localized labels including `Continue as guest` and `Continuer en invité`, plus accessibility label `Continue as guest`.
- `iosApp/src/Services/AuthStateManager.swift` now exposes `continueAsGuest()`, stores a local `wakeve_guest_user_id`, restores the guest session on app launch, and skips token refresh for guest sessions.
- `iosApp/src/Views/Auth/LoginView.swift` wraps the skip-authentication button in `#if DEBUG`; the localized French label is `Passer (Développement)`.

Reviewer access remains open until a signed TestFlight/App Store Connect review build proves that the release-visible guest path reaches the reviewable surfaces listed below. If guest access cannot reach a required review surface, configure a dedicated App Store Connect demo account manually and update the review notes to match the uploaded build.

## Evidence Commands

Run these against the release metadata and uploaded review build before final signoff:

```bash
rg -n "Guest|guest|demo|review|Delete Account|moderation|payment|Tricount" \
  composeApp/metadata/ios/review_information/notes.txt \
  docs/APP_STORE_LAUNCH_CHECKLIST.md \
  docs/APP_STORE_TESTFLIGHT_EVIDENCE.md

wc -c composeApp/metadata/ios/review_information/notes.txt
find composeApp/metadata/ios/review_information -maxdepth 1 -type f \( -name "*password*" -o -name "*user*" -o -name "*phone*" \) -print
./scripts/lint-store-metadata.sh --ios-only
```

Record:

- The exact App Review notes submitted with the build.
- Whether review uses guest mode or manually entered demo credentials in App Store Connect.
- Whether App Store Connect `signInRequired` / demo-account fields are disabled for guest review or populated for a non-expiring demo account.
- The submitted App Store Connect review detail output or screenshot showing contact name, email, phone, notes, demo-account choice, and any attachments.
- The screens App Review should inspect for account deletion, UGC moderation, and payment/Tricount behavior if those surfaces are enabled.
- The TestFlight smoke result proving the access path works on the uploaded build.
- The owner and response path for App Review messages and unresolved issues after submission.

## Closure Rule

Set `APP_STORE_REVIEW_ACCESS_EVIDENCE_COMPLETE=true` only after:

- App Review notes are uploaded and accurately describe guest or demo access for the review build.
- Do not commit passwords: no demo password or private reviewer credential is committed to the repository.
- Review notes remain within Apple's 4000-byte limit and match the selected App Store Connect demo-account setting.
- Required contact name, email, and phone are present in App Store Connect, with `APP_REVIEW_PHONE_NUMBER` or manual entry used for the phone.
- TestFlight smoke testing confirms the reviewer access path reaches the required review surfaces.
- The release owner can respond to App Review messages and attach supporting files if the submission receives unresolved issues.
- `docs/APP_STORE_FINAL_SIGNOFF.md` references this completed evidence for the uploaded review build.

## Apple References

- App Review overview: https://developer.apple.com/app-store/review/
- App Store review details API: https://developer.apple.com/documentation/appstoreconnectapi/app-store-review-details
- Platform version App Review information fields: https://developer.apple.com/help/app-store-connect/reference/platform-version-information
- Submit an app: https://developer.apple.com/help/app-store-connect/manage-submissions-to-app-review/submit-an-app/
- Reply to App Review messages: https://developer.apple.com/help/app-store-connect/manage-submissions-to-app-review/reply-to-app-review-messages/
