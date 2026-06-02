# App Store Account Deletion Evidence - Wakeve

Date: 2026-05-27

Status: PENDING

Do not change the marker below until the exact App Review build has been inspected and the in-app account deletion flow, backend deletion behavior, local cleanup, and provider revocation handling are verified.

APP_STORE_ACCOUNT_DELETION_EVIDENCE_COMPLETE=false

## Apple Source Baseline

Last checked: 2026-05-28.

- Apple App Review Guideline 5.1.1(v), last updated 2026-02-06, requires apps that support account creation to offer account deletion inside the app.
- Apple's account deletion support guidance requires users to be able to initiate account deletion from the app and expects the deletion flow to cover the account, associated personal data, and shared user-generated content handling where applicable.
- Apple says the account deletion option should be easy to find and is typically included in account settings.
- Apple says deleting only by temporary deactivation or disablement is insufficient; the user must be offered deletion of the entire account record and associated personal data unless retention is legally required.
- Apple says automatically generated or guest-style accounts also need an option to delete the account and associated data.
- Apple allows manual or delayed deletion processes if the app tells the user how long deletion will take and confirms when deletion is completed.
- Apple says all users should be allowed to delete their accounts regardless of location if the app supports account creation.
- Apple says user-generated content shared with others is part of the data users expect to be deleted, unless local law or regulation requires retention and that retention is disclosed.
- Wakeve supports account creation through email OTP, Google login, Sign in with Apple, and guest-backed sessions, so the App Store review build needs real in-app deletion evidence before this marker can become complete.

## Apple References

- Offering account deletion in your app: https://developer.apple.com/support/offering-account-deletion-in-your-app
- App Review Guidelines, Guideline 5.1.1(v): https://developer.apple.com/app-store/review/guidelines/#data-collection-and-storage
- Account deletion within apps upcoming requirement: https://developer.apple.com/news/upcoming-requirements/?id=06302022b

## Build Under Review

- App Store Connect version: TBD
- Build number: TBD
- Release commit: TBD
- TestFlight evidence reference: `docs/APP_STORE_TESTFLIGHT_EVIDENCE.md`
- OpenSpec implementation reference: `openspec/changes/add-in-app-account-deletion/`
- App Review notes reference: `composeApp/metadata/ios/review_information/notes.txt`

## Required Account Deletion Review

| Area | Required Evidence | Result | Notes |
| --- | --- | --- | --- |
| In-app initiation | Profile or account settings expose a clearly named Delete Account action for authenticated users. | Pending | TBD |
| Guest data deletion | Guest users can delete locally stored guest data or guest-backed account data from the app. | Pending | TBD |
| Confirmation | The destructive confirmation explains permanence and deletion scope before the request is sent. | Pending | TBD |
| Backend deletion | Authenticated deletion endpoint deletes or anonymizes the full account record and associated personal data according to the documented retention policy. | Pending | TBD |
| Shared UGC handling | User-generated content owned by the deleted user is deleted or anonymized unless legally retained, and any retention is disclosed. | Pending | TBD |
| Session and token revocation | Active sessions, bearer tokens, push tokens, and stored auth credentials are revoked or removed. | Pending | TBD |
| Sign in with Apple | Apple token revocation is attempted where authorization material is available, without blocking Wakeve data erasure on transient provider failures. | Pending | TBD |
| Local cleanup | Keychain tokens, cached profile state, synced local user data, and analytics identifiers are cleared after successful deletion. | Pending | TBD |
| Delayed completion | If deletion is manual/asynchronous, the app explains the expected completion timeline and records the final confirmation path. | Pending | TBD |
| Completion messaging | The app shows success or documented asynchronous completion messaging that App Review can verify. | Pending | TBD |
| Tests | Backend and iOS/simulator evidence cover success, auth failure, repeat deletion, offline failure, cleanup, and guest deletion. | Pending | TBD |
| Reviewer path | App Review notes explain where to find Delete Account and what result to expect. | Pending | TBD |

## Evidence Commands

Run or record equivalent release evidence before setting `APP_STORE_ACCOUNT_DELETION_CONFIRMED=true`:

```bash
openspec validate add-in-app-account-deletion --strict
rg -n "deleteAccount|deleteUserAccount|accountDeletion|/api/user/delete|delete-account" server/src/main/kotlin shared/src/commonMain/kotlin iosApp/src
rg -n "Delete Account|Delete Guest Data|Data Management|Profile Settings" iosApp/src
rg -n "clear.*Keychain|Keychain.*clear|clear.*token|remove.*token|guest.*delete|delete.*guest|analytics.*clear|clear.*profile" iosApp/src shared/src/iosMain/kotlin shared/src/commonMain/kotlin
rg -n "account deletion|delete account|deleteAccount|deleteUserAccount|/api/user/delete" server/src/test shared/src/commonTest
```

Record the output or attach screenshots/logs showing:

- The deletion flow in the uploaded TestFlight/App Review build.
- The authenticated backend route result and idempotent repeat behavior.
- The guest data deletion behavior.
- The shared user-generated content deletion, anonymization, or legally required retention behavior.
- The local credential/cache cleanup result.
- The expected completion timeline if deletion is asynchronous or manually reviewed.
- Sign in with Apple revocation handling or documented unavailability for the tested account.
- App Review notes that match the uploaded build.

## OpenSpec Proposal Validation Result

Command run on 2026-05-27:

```bash
openspec validate add-in-app-account-deletion --strict
```

Local result:

- `Change 'add-in-app-account-deletion' is valid`
- Active tasks remain `0/14`, so this is proposal-readiness evidence only.
- No implementation or App Review readiness credit is taken from this validation until the proposal is approved, implemented, tested, and evidenced against the uploaded review build.

## Closure Rule

Set `APP_STORE_ACCOUNT_DELETION_EVIDENCE_COMPLETE=true` only after:

- OpenSpec `add-in-app-account-deletion` has no unchecked implementation tasks or has been archived after completion.
- Backend deletion, shared UGC handling, session revocation, push token removal, local cleanup, guest deletion, completion timeline, and Sign in with Apple revocation behavior are verified for the review build.
- The iOS flow is reachable from the uploaded build and shows confirmation plus completion messaging.
- Backend and iOS/simulator verification evidence is recorded in this file or linked from it.
- `APP_STORE_ACCOUNT_DELETION_CONFIRMED=true` is set only in the release shell or CI secret store after this evidence is complete.
