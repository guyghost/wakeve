# App Store DSA Trader Status - Wakeve

Date: 2026-06-01

This file records the App Store Connect Digital Services Act trader status decision for Wakeve.

## Why This Matters

Apple's App Store Connect DSA guidance requires developers to declare whether they are acting as a trader. If Wakeve is distributed in any European Union storefront as a trader, Apple verifies and displays trader contact information on the App Store product page. Wakeve's first release plan must therefore include an explicit App Store Connect decision before App Review submission.

## Current Decision

Status: not confirmed.

APP_STORE_DSA_TRADER_STATUS_EVIDENCE_COMPLETE=false

## Apple Source Baseline

Last checked: 2026-06-01.

- Apple says Articles 30 and 31 of the Digital Services Act require Apple to verify and display trader contact information for all traders distributing apps on the App Store in the European Union.
- Apple says trader contact information includes an address, phone number, and email address for display on the App Store product page.
- Apple says verified trader contact information is published on the App Store product page when the app is distributed in any of the 27 EU territories.
- Apple says developers still need to declare a trader status even if they do not distribute apps in the EU.
- Apple says App Store Connect asks developers to disclose whether or not they are a trader when submitting a new app if trader status has not already been confirmed.
- Apple says developers can turn off or specify trader status for each specific app that they distribute.
- Apple says developers must assess whether they are a trader for EU law purposes because Apple cannot determine whether a developer is a trader.
- Apple says trader self-assessment may consider revenue, commercial practices, VAT registration, and whether the app is developed in a professional or business capacity.
- Apple says a developer not distributing apps on the App Store in the EU is not acting as a trader on the App Store.
- Apple says non-trader status informs EU consumers that consumer rights from applicable consumer protection laws will not apply to contracts between the developer and them.
- Apple says organization accounts display the D-U-N-S address and must enter a phone number and email address for product-page display when acting as a trader.
- Apple says individual accounts must enter an address or P.O. Box, phone number, and email address for product-page display when acting as a trader.
- Apple says traders need to provide payment account details if they have not already entered them in App Store Connect.
- Apple says traders are asked to certify that they only offer products or services that comply with applicable EU law.
- Apple says the required role for account-level EU DSA compliance information is Account Holder or Admin.
- Apple says the app-specific DSA status and optional Labels and Markings URL are managed from App Information by an Account Holder or Admin.
- Apple says a Labels and Markings URL can be shown on App Store product pages for apps identified as trader apps when EU law requires labels or markings.

## Current 2026-06-01 Local Status

Repository-side checks cannot determine Wakeve's legal trader status or change App Store Connect EU storefront availability. The local evidence only confirms that the submission gates still require the App Store Connect DSA decision before final upload.

- `APP_STORE_DSA_TRADER_STATUS_EVIDENCE_COMPLETE=false` remains intentional because no App Store Connect screenshot/export or owner decision is recorded.
- `docs/APP_STORE_PRICING_AVAILABILITY_EVIDENCE.md` remains pending and requires the final storefront list, including whether EU countries are enabled or excluded.
- `docs/APP_STORE_AVAILABILITY_EVIDENCE.md` remains pending and explicitly requires that any EU storefront change for DSA reasons does not contradict this file.
- If the first release enables any EU storefront, the release owner must record either verified trader details or a non-trader declaration that is compatible with Wakeve's legal/business status.
- If the first release excludes EU storefronts, the App Store Connect Pricing and Availability evidence must record that exclusion and this file must reference the same storefront decision.

Before upload or manual submission, complete one of these App Store Connect paths:

- Provide and verify trader status for EU distribution.
- Disable EU availability for the first release and record that choice in App Store Connect.
- If not acting as a trader, declare non-trader status in App Store Connect and confirm the App Store product page disclosure is acceptable for the chosen storefront availability.

Do not set `APP_STORE_DSA_TRADER_STATUS_CONFIRMED=true` until the App Store Connect record reflects the chosen path and this file contains `APP_STORE_DSA_TRADER_STATUS_EVIDENCE_COMPLETE=true`.

## Repeatable Preparation Report

Run this helper to generate a timestamped AS-08 evidence template:

```bash
./scripts/prepare-app-store-dsa-trader-status-evidence.sh
```

Generated reports are written under `docs/app-store-dsa/` and explicitly record `Generated report can close AS-08 = no - preparation evidence only`. The helper lists the three acceptable App Store Connect decision paths, required owner approval, pricing/availability cross-checks, app availability cross-checks, and the screenshot/export fields that must be completed before AS-08 can close.

## App Store Connect Actions

Before App Review:

- Open the App Store Connect app record for Bundle ID `com.guyghost.wakeve`.
- Confirm whether Wakeve will be distributed in the European Union for the first release.
- If EU distribution remains enabled, provide the required trader information and wait for verification where App Store Connect requires it.
- If EU distribution is disabled, confirm storefront availability excludes EU countries for this release.
- If non-trader status is selected, confirm the App Store Connect DSA declaration is complete and compatible with Wakeve's business/legal status.
- If trader status is selected, confirm whether a Labels and Markings URL is not applicable or enter the final URL required by EU law.
- Re-check `docs/APP_STORE_AVAILABILITY_DECISIONS.md` so general storefront/device availability and DSA availability do not contradict each other.

## Required Evidence

Record the final evidence in this file and in `docs/APP_STORE_FINAL_SIGNOFF.md` before setting `APP_STORE_DSA_TRADER_STATUS_CONFIRMED=true`:

- App Store Connect DSA status selected: trader, non-trader, or EU storefront disabled.
- App-specific DSA setting for Bundle ID `com.guyghost.wakeve` is recorded separately from the account-level compliance status.
- If trader: contact information provided, verified where required, and acceptable for display on the EU product page.
- If trader: organization versus individual trader contact requirements are checked against the enrolled Apple Developer Program account type.
- If trader: payment account details are present if App Store Connect requires them.
- If trader: EU-law certification is accepted and a Labels and Markings URL is either entered or documented as not applicable.
- If non-trader: legal/product owner has accepted the consumer-rights disclosure shown to EU users.
- If EU storefront disabled: storefront availability excludes EU countries for the first release.
- `docs/APP_STORE_PRICING_AVAILABILITY_EVIDENCE.md` and `docs/APP_STORE_AVAILABILITY_EVIDENCE.md` do not contradict the selected DSA path.
- Reviewer/date and App Store Connect screenshot or export reference.

Set `APP_STORE_DSA_TRADER_STATUS_EVIDENCE_COMPLETE=true` only after the chosen DSA path is visible in App Store Connect and does not contradict the storefront availability recorded in `docs/APP_STORE_AVAILABILITY_EVIDENCE.md`.

## Verification

The final local audit requires:

```bash
APP_STORE_DSA_TRADER_STATUS_CONFIRMED=true \
./scripts/app-store-submission-audit.sh --check-live-urls --run-submission-ready
```

The App Store upload lane also requires `APP_STORE_DSA_TRADER_STATUS_CONFIRMED=true`.

## Apple Reference

- Manage European Union Digital Services Act trader requirements: https://developer.apple.com/help/app-store-connect/manage-compliance-information/manage-european-union-digital-services-act-trader-requirements
