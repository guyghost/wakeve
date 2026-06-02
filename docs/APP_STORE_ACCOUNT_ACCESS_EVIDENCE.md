# App Store Account Access Evidence - Wakeve

Date: 2026-05-27

Status: PENDING

Do not change the marker below until the Apple account and App Store Connect team used for the release can create or edit the Wakeve app record, upload builds, add the app version for review, and submit it for App Review.

APP_STORE_ACCOUNT_ACCESS_EVIDENCE_COMPLETE=false

## Apple Source Baseline

Last checked: 2026-05-27.

- Apple says App Store Connect is used to submit apps for distribution, manage apps, distribute beta versions with TestFlight, accept legal agreements, enter tax and banking information, and manage app metadata.
- Apple says user roles determine access to App Store Connect and Apple Developer website sections and privileges for performing tasks.
- Apple says the Account Holder signs legal agreements, renews membership, creates Developer ID certificates, and is the only user who can sign legal agreements.
- Apple says two-step verification or two-factor authentication must be enabled to sign in to App Store Connect.
- Apple says app submission requires required metadata, choosing the build for the version, and an Account Holder, Admin, or App Manager role.
- Apple says choosing a build to submit requires Account Holder, Admin, or App Manager access.
- Apple says uploading builds requires Account Holder, Admin, App Manager, or Developer access.
- Apple says each uploaded build is associated with the app and version record using the bundle ID and version number in the app bundle, while the build string uniquely identifies the build.
- Apple says adding or editing users requires Account Holder, Admin, or App Manager access, while changing user roles requires Account Holder or Admin access.
- Apple says free apps can be distributed on the App Store under the Apple Developer Program License Agreement.
- Apple says selling apps or offering In-App Purchases requires the Account Holder to sign the Paid Apps Agreement.

## Required Apple Account Evidence

| Area | Required Evidence | Result |
| --- | --- | --- |
| Apple account | `APPLE_ID` belongs to the intended App Store Connect user, not a placeholder or personal test account. | Pending |
| Two-factor authentication | The release operator can complete Apple Account 2FA for App Store Connect and Fastlane sessions. | Pending |
| App Store Connect role | The user has Account Holder, Admin, or App Manager role for manual App Review submission. | Pending |
| Build upload role | The user or API key can upload builds with Account Holder, Admin, App Manager, or Developer privileges. | Pending |
| App record access | Bundle ID `com.guyghost.wakeve` and the Wakeve app record are visible to the release operator. | Pending |
| Agreements | Latest Apple Developer Program License Agreement and any required App Store Connect agreements are active. | Pending |
| Paid Apps Agreement | Confirm not required for the first release, or signed if Wakeve sells paid apps, In-App Purchases, subscriptions, or paid digital content. | Pending |
| App Store Connect team | `ITC_TEAM_ID` maps to the App Store Connect team that owns the Wakeve app record. | Pending |

## Local Gate Coverage

This section records repository-side release-gate coverage only. It does not prove real Apple account access, App Store Connect role membership, agreements, 2FA, or app record ownership.

Commands and files checked locally on 2026-05-27:

```bash
rg -n "APPLE_ID|ITC_TEAM_ID|TEAM_ID|APPLE_TEAM_ID" .env.appstore.example fastlane/Fastfile scripts/app-store-submission-audit.sh .github/workflows/store-readiness.yml
APP_REVIEW_PHONE_NUMBER='+33123456789' ./scripts/app-store-submission-audit.sh --skip-preflight
```

Observed local gate coverage:

- `.env.appstore.example` documents safe placeholders for `APPLE_ID=release@example.com`, `ITC_TEAM_ID=123456789`, `TEAM_ID=ABCDE12345`, and `APPLE_TEAM_ID=ABCDE12345`, with comments stating they are intentionally rejected by final gates.
- `fastlane/Fastfile` requires `APPLE_ID`, `ITC_TEAM_ID`, `TEAM_ID`, and `APPLE_TEAM_ID` before `submission_ready` and `upload_appstore`.
- `fastlane/Fastfile` requires `APPLE_ID`, `ITC_TEAM_ID`, and `TEAM_ID` before `upload_testflight`.
- `fastlane/Fastfile` rejects the documented placeholders `release@example.com`, `123456789`, and `ABCDE12345`.
- `scripts/app-store-submission-audit.sh` validates `APPLE_ID` as an email-shaped value, `ITC_TEAM_ID` as numeric, `TEAM_ID` and `APPLE_TEAM_ID` as 10-character uppercase Apple Team IDs, rejects the documented placeholders, and requires `TEAM_ID` to match `APPLE_TEAM_ID`.
- `.github/workflows/store-readiness.yml` has expected-failure checks proving `submission_ready`, `upload_testflight`, and the final non-uploading audit reject the documented Apple release placeholders.

Current local audit state:

- With only `APP_REVIEW_PHONE_NUMBER='+33123456789'`, `./scripts/app-store-submission-audit.sh --skip-preflight` still reports missing `APPLE_ID`, `ITC_TEAM_ID`, `TEAM_ID`, and `APPLE_TEAM_ID` as blockers.
- This is correct for local preparation: the project must not be treated as ready until release secrets and Apple/App Store Connect evidence are supplied outside the repository.

## Apple References

- Submit an app requires required metadata, a selected build, and an Account Holder, Admin, or App Manager role: https://developer.apple.com/help/app-store-connect/manage-submissions-to-app-review/submit-an-app
- App Store Connect roles define what each user can perform, including Account Holder agreement responsibilities and App Manager app management: https://developer.apple.com/help/app-store-connect/reference/account-management/role-permissions
- Creating an app record requires Account Holder, Admin, or App Manager access, and the latest agreement must be signed: https://developer.apple.com/help/app-store-connect/create-an-app-record/add-a-new-app
- Uploading builds requires Account Holder, Admin, App Manager, or Developer access: https://developer.apple.com/help/app-store-connect/manage-builds/upload-builds
- Paid Apps Agreement is required to sell apps or offer In-App Purchases: https://developer.apple.com/help/app-store-connect/manage-agreements/sign-and-update-agreements/

## Evidence To Attach

Record these before final signoff:

- App Store Connect user email or release secret name corresponding to `APPLE_ID`.
- App Store Connect team name and numeric `ITC_TEAM_ID`.
- User role screenshot/export showing Account Holder, Admin, or App Manager for manual submission.
- Build upload credential path: Apple ID session, app-specific password, or API key owner/role, without committing secrets.
- App record screenshot/export for Bundle ID `com.guyghost.wakeve`.
- Agreements status screenshot/export from App Store Connect Business or Agreements.
- Paid Apps Agreement decision: not applicable for free/no-IAP release, or signed with tax/banking status if paid/IAP is enabled.
- Reviewer/date and final release shell command showing `APPLE_ID` and `ITC_TEAM_ID` were non-placeholder values.

## Closure Rule

Set `APP_STORE_ACCOUNT_ACCESS_EVIDENCE_COMPLETE=true` only after:

- The same App Store Connect team owns the app record, build upload, and manual review submission.
- The release operator has Account Holder, Admin, or App Manager permissions for final manual App Review submission.
- The account can complete 2FA and access the required App Store Connect pages.
- Required agreements are active and do not block app record creation, build upload, or App Review submission.
- `docs/APP_STORE_FINAL_SIGNOFF.md` references this completed evidence for the submitted review build.
