# App Store Privacy Labels Draft - Wakeve

Date: 2026-05-27

This is a submission draft for App Store Connect privacy answers. It must be validated against the production backend, analytics/crash SDKs, and legal requirements before final submission.

Evidence for the exact App Review build must be recorded in `docs/APP_STORE_PRIVACY_EVIDENCE.md`. Do not set `APP_STORE_PRIVACY_SIGNOFF=true` until that file contains `APP_STORE_PRIVACY_EVIDENCE_COMPLETE=true`.

## Apple Source Baseline

Apple-source review date: 2026-05-27.

- Apple says App Store privacy details are required to submit new apps and app updates.
- Apple says privacy responses must include the practices of third-party partners whose code is integrated into the app.
- Apple says developers are responsible for keeping privacy responses accurate and up to date when practices change.
- Apple says developers must identify all data they or their third-party partners collect unless the data meets every optional-disclosure criterion.
- Apple defines collection as transmitting data off device so the developer or third-party partners can access it longer than needed to service the request in real time.
- Apple says data collected only for app functionality still needs to be declared with that purpose.
- Apple says developers must identify whether each collected data type is linked to the user's identity.
- Apple says data is often linked to identity unless it is de-identified or anonymized before collection and is not re-linked later.
- Apple defines tracking as linking app-collected user or device data with third-party data for targeted advertising or advertising measurement, or sharing app-collected user or device data with a data broker.
- Apple says data linked only on device and not sent off device in a user- or device-identifying way is not tracking.
- Apple requires a publicly accessible Privacy Policy URL for all apps.
- Apple says App Store Connect privacy responses are app-level and should be comprehensive if one platform collects more data than another.
- Apple says adding new privacy data types requires completing setup for each new data type before publishing.

## Tracking

- Does this app use data for tracking? **No**
- Does this app use third-party advertising? **No**
- Does this app share data with data brokers? **No**

Local enforcement now checks this no-tracking answer against `iosApp/src/PrivacyInfo.xcprivacy`, `iosApp/src/Info.plist`, iOS source code, and the built Release app. Before changing this answer, update the manifest, purpose strings, App Store Connect answers, and linter expectations together.

## Data Linked To The User

### Contact Info

- **Name**
  - Collected: Yes
  - Purpose: App Functionality, Account Management
  - Linked to identity: Yes
  - Used for tracking: No
- **Email Address**
  - Collected: Yes
  - Purpose: App Functionality, Account Management
  - Linked to identity: Yes
  - Used for tracking: No

### User Content

- **Other User Content**
  - Examples: event titles, descriptions, dates, participant lists, poll votes, scenarios, comments, budget items, meeting details.
  - Collected: Yes
  - Purpose: App Functionality
  - Linked to identity: Yes
  - Used for tracking: No

### Identifiers

- **User ID**
  - Collected: Yes
  - Purpose: App Functionality, Account Management
  - Linked to identity: Yes
  - Used for tracking: No
- **Device ID**
  - Examples: APNs/FCM push notification token.
  - Collected: Yes
  - Purpose: App Functionality
  - Linked to identity: Yes
  - Used for tracking: No

## Data Not Linked To The User

### Location

- **Approximate Location**
  - Collected: Optional, only when the user grants permission.
  - Purpose: App Functionality
  - Linked to identity: Yes, conservatively, because event locations and location preferences can be stored with event/user records.
  - Used for tracking: No
  - Validation needed: confirm production backend does not persist precise coordinates unless intentionally declared.

### Usage Data

- **Product Interaction**
  - Collected: If analytics are enabled.
  - Purpose: Analytics
  - Linked to identity: Yes, conservatively, because the local/server analytics schema contains `user_id`.
  - Used for tracking: No
  - Validation needed: confirm whether analytics is enabled in production and whether App Store Connect should mark it as linked or remove it if disabled.

### Diagnostics

- **Crash Data**
  - Collected: If crash reporting is enabled.
  - Purpose: App Functionality, Analytics
  - Linked to identity: No, per current privacy policy claim.
  - Used for tracking: No
  - Validation needed: confirm crash reports are anonymized and do not include user identifiers.

## Data Not Collected

Based on the current privacy policy and code audit, the intended App Store Connect answer should be **not collected** for:

- Contacts
- Browsing History
- Search History
- Financial Info
- Health and Fitness
- Sensitive Info
- Purchases
- Advertising Data

## Required Verification Before Submission

- Confirm whether photos or media are uploaded to the backend. If yes, add Photos or Videos under User Content.
- Confirm whether calendar write operations collect or transmit calendar data. The current policy states existing calendar data is not read or shared.
- Confirm whether Siri/speech features transmit speech/audio. The current policy says commands are processed locally.
- Confirm whether any analytics/crash provider is enabled in the iOS target and whether it links data to identity.
- Confirm App Store Connect privacy labels match `docs/PRIVACY_POLICY.md` and the live privacy URL.

## Privacy Manifest Alignment

The iOS privacy manifest currently declares these collected data types with `Tracking = false`:

- `NSPrivacyCollectedDataTypeName`
- `NSPrivacyCollectedDataTypeEmailAddress`
- `NSPrivacyCollectedDataTypeUserID`
- `NSPrivacyCollectedDataTypeDeviceID`
- `NSPrivacyCollectedDataTypeOtherUserContent`
- `NSPrivacyCollectedDataTypeCoarseLocation`
- `NSPrivacyCollectedDataTypeProductInteraction`

Before submission, mirror these declarations in App Store Connect unless legal/product review intentionally narrows a category and the code path is disabled or removed.
