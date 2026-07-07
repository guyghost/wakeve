# App Store Payment Evidence - Wakeve

Date: 2026-06-01

Status: PENDING

Do not change the marker below until the exact App Review build has been inspected and payment, settlement, and Tricount surfaces are proven to be limited to real-world shared-event expenses.

APP_STORE_PAYMENT_EVIDENCE_COMPLETE=false

## Apple Source Baseline

Last checked: 2026-05-28.

- Apple says apps may not use their own mechanisms to unlock content or functionality, including license keys, augmented reality markers, QR codes, cryptocurrencies, cryptocurrency wallets, and other methods.
- Apple says developers may apply for entitlements to link to their own website to purchase digital content or services, and those entitlements are not required for United States storefront apps.
- Apple says StoreKit External Purchase Link Entitlements are limited to specific iOS and iPadOS App Store storefronts.
- Apple says non-US storefront apps and metadata may not include buttons, external links, or other calls to action that direct customers to payment mechanisms other than in-app purchase unless an exception applies.
- Apple says apps that operate across multiple platforms may allow access to content, subscriptions, or features acquired elsewhere only if those items are also available as in-app purchases within the app.
- Apple says real-time person-to-person services between two individuals may use purchase methods other than in-app purchase.
- Apple says one-to-few and one-to-many real-time services must use in-app purchase.
- Apple says physical goods or services consumed outside the app must use purchase methods other than in-app purchase, such as Apple Pay or traditional credit card entry.
- Apple says free stand-alone companion apps to paid web-based tools do not need in-app purchase if there is no purchasing inside the app or calls to action for purchase outside the app.
- Apple says advertising management apps may use purchase methods other than in-app purchase for buying and managing advertising campaigns, but digital purchases for content experienced or consumed in the app must use in-app purchase.
- Apple says if any configured in-app purchase items cannot be found or reviewed in the app, the reason should be explained in App Review notes.

## Build Under Review

- App Store Connect version: TBD
- Build number: TBD
- Release commit: TBD
- TestFlight evidence reference: `docs/APP_STORE_TESTFLIGHT_EVIDENCE.md`
- Payment compliance policy reference: `docs/APP_STORE_PAYMENT_COMPLIANCE.md`
- App Review notes reference: `composeApp/metadata/ios/review_information/notes.txt`

## Review Scope

| Surface | Required Evidence | Result | Notes |
| --- | --- | --- | --- |
| Payment pots | Payment pots coordinate real-world shared-event expenses only. | Pending | TBD |
| Settlement suggestions | Suggested payments do not unlock app features, subscriptions, digital content, digital credits, boosts, or premium functionality. | Pending | TBD |
| Tricount handoff | Tricount/provider URLs are HTTPS-only, trusted-domain validated, and not arbitrary user-entered external purchase links. | Pending | TBD |
| Provider scope | Payment-pot URLs are restricted to trusted Tricount hosts for this submission; arbitrary external provider URLs are rejected. | Pending | TBD |
| Storefront-specific external purchase links | Wakeve does not use StoreKit external-purchase-link entitlements or storefront-specific digital-purchase calls to action for this submission. | Pending | TBD |
| In-app copy | Screens and metadata do not tell users to bypass App Store in-app purchase for digital goods or services. | Pending | TBD |
| StoreKit/IAP | The review build has no StoreKit products and does not sell digital goods outside App Store in-app purchase. | Pending | TBD |
| App Review notes | Notes explain real-world shared event expenses, no digital unlocks, and trusted Tricount link handling. | Pending | TBD |

## Evidence Commands

Run these before setting `APP_STORE_PAYMENT_COMPLIANCE_CONFIRMED=true`:

```bash
./scripts/audit-app-store-payment-compliance.sh
rg -n "StoreKit|SKProduct|SKProductsRequest|SKPayment|Product\\.products|InAppPurchase|purchase\\(|paywall" iosApp/src shared/src/commonMain/kotlin server/src/main/kotlin apps/landing/src
rg -n "PaymentPotRepository|TricountHandoffRepository|PaymentRoutes|paymentRoutes|tricountGroupUrl|providerUrl|isTrustedPaymentLink|isTrustedProviderUrl" iosApp/src shared/src/commonMain/kotlin server/src/main/kotlin apps/landing/src
rg -n "real-world shared event expenses|do not sell or unlock app features|trusted-domain validated|Tricount" composeApp/metadata/ios/review_information/notes.txt docs/APP_STORE_PAYMENT_COMPLIANCE.md docs/APP_STORE_REVIEW_GUIDELINE_AUDIT.md
./gradlew :shared:jvmTest --tests 'com.guyghost.wakeve.organization.EventOrganizationPhase5ReadinessTest'
```

## Local Payment Scan Result

Commands refreshed on 2026-06-01:

```bash
rg -n "StoreKit|SKProduct|SKProductsRequest|SKPayment|Product\\.products|InAppPurchase|purchase\\(|paywall" iosApp/src shared/src/commonMain/kotlin server/src/main/kotlin apps/landing/src
rg -n "PaymentPotRepository|TricountHandoffRepository|PaymentRoutes|paymentRoutes|tricountGroupUrl|providerUrl|isTrustedPaymentLink|isTrustedProviderUrl" iosApp/src shared/src/commonMain/kotlin server/src/main/kotlin apps/landing/src
rg -n "real-world shared event expenses|do not sell or unlock app features|trusted-domain validated|Tricount" composeApp/metadata/ios/review_information/notes.txt docs/APP_STORE_PAYMENT_COMPLIANCE.md docs/APP_STORE_REVIEW_GUIDELINE_AUDIT.md
./gradlew :shared:jvmTest --tests 'com.guyghost.wakeve.organization.EventOrganizationPhase5ReadinessTest'
```

Local result:

- Repeatable helper: `./scripts/audit-app-store-payment-compliance.sh` generates a timestamped report under `docs/app-store-payment/` with StoreKit/IAP, payment-surface, and policy-note scans. Generated reports explicitly record `Generated report can close AS-11 = no - local scan only`.
- StoreKit/IAP scan: `NO_MATCHES` for `StoreKit`, `SKProduct`, `SKProductsRequest`, `SKPayment`, `Product.products`, `InAppPurchase`, `purchase(`, and `paywall` in `iosApp/src`, `shared/src/commonMain/kotlin`, `server/src/main/kotlin`, and `apps/landing/src`.
- Payment surface scan: payment surfaces are present in `PaymentPotRepository`, `TricountHandoffRepository`, `server/src/main/kotlin/com/guyghost/wakeve/routes/PaymentRoutes.kt`, and `iosApp/src/Views/App/ContentView.swift`.
- Trusted link scan: `PaymentPotRepository.isTrustedPaymentLink` and `TricountHandoffRepository.isTrustedProviderUrl` require HTTPS Tricount hosts, reject non-Tricount providers when a payment-pot URL is present, and reject template markers.
- Regression test: `./gradlew :shared:jvmTest --tests 'com.guyghost.wakeve.organization.EventOrganizationPhase5ReadinessTest'` completed with `BUILD SUCCESSFUL` on 2026-06-01; the suite covers rejection of non-Tricount external provider links for payment pots.
- App Review notes/policy scan: `composeApp/metadata/ios/review_information/notes.txt`, `docs/APP_STORE_PAYMENT_COMPLIANCE.md`, and `docs/APP_STORE_REVIEW_GUIDELINE_AUDIT.md` describe payment, settlement, and Tricount as real-world shared event expenses only, with no digital unlocks and trusted-domain validation.
- The 2026-06-01 StoreKit/IAP source scan returned no matches in `iosApp/src`, `shared/src/commonMain/kotlin`, `server/src/main/kotlin`, or `apps/landing/src`.

This is local pre-submission evidence only. It does not close AS-11 until the uploaded TestFlight/App Review build is inspected for the same behavior, screenshots or reviewer notes prove the visible payment surfaces match this scope, and `APP_STORE_PAYMENT_EVIDENCE_COMPLETE=true` is set for that exact build.

Record the output or attach screenshots/notes showing:

- The payment surfaces inspected in the uploaded review build.
- No external payment flow unlocks app features, subscriptions, digital content, digital credits, boosts, or premium functionality.
- Tricount/provider links remain HTTPS-only and trusted-domain validated.
- Payment-pot URLs remain restricted to trusted Tricount hosts unless a new App Store payment review covers another provider.
- App Review notes match the uploaded build behavior.
- Any disabled or hidden payment surface is documented with the build flag/config used for App Review.

## Closure Rule

Set `APP_STORE_PAYMENT_EVIDENCE_COMPLETE=true` only after:

- The uploaded review build has been inspected for payment, settlement, and Tricount surfaces.
- App Review notes explain the real-world shared-event expense scope and no digital unlocks.
- StoreKit/IAP absence or App Store IAP compliance is verified for the review build.
- Tricount/provider URL safety and non-Tricount external-provider rejection are verified against the final build behavior.
- `APP_STORE_PAYMENT_COMPLIANCE_CONFIRMED=true` is set only in the final release shell or CI secret store after this file is updated.
