# App Store Payment Compliance - Wakeve

Date: 2026-06-01

Status: NOT SIGNED OFF

This document records the first-submission payment decision for App Store Review. It covers Wakeve's payment pot, shared expense, settlement, and Tricount handoff surfaces.

Evidence for the exact App Review build must be recorded in `docs/APP_STORE_PAYMENT_EVIDENCE.md`. Do not set `APP_STORE_PAYMENT_COMPLIANCE_CONFIRMED=true` until that file contains `APP_STORE_PAYMENT_EVIDENCE_COMPLETE=true`.

## Apple Source Baseline

Apple-source review date: 2026-05-28.

- App Review Guideline 3.1.1 says apps that unlock features, functionality, subscriptions, in-app currencies, game levels, premium content, full-version access, or other digital goods must use App Store in-app purchase.
- App Review Guideline 3.1.1 says apps may not use their own mechanisms to unlock app content or functionality, including license keys, QR codes, cryptocurrencies, or cryptocurrency wallets.
- App Review Guideline 3.1.1(a) says external purchase links for digital content or services require the applicable StoreKit External Purchase Link entitlement or another expressly permitted storefront rule.
- App Review Guideline 3.1.1(a) says United States storefront apps do not require those entitlements for buttons, external links, or other calls to action, but this does not remove the need to prove the app's non-US behavior and payment scope.
- App Review Guideline 3.1.1(a) says the StoreKit External Purchase Link entitlements are limited to the iOS or iPadOS App Store in specific storefronts.
- App Review Guideline 3.1.3 says apps using purchase methods other than in-app purchase must not encourage users in the app to use an alternative purchase method, except where expressly permitted.
- App Review Guideline 3.1.3(b) says multiplatform service access to content, subscriptions, or features acquired elsewhere is allowed only when those items are also available as in-app purchases in the app, where applicable.
- App Review Guideline 3.1.3(d) says real-time person-to-person services between two individuals may use purchase methods other than in-app purchase, while one-to-few and one-to-many real-time services must use in-app purchase.
- App Review Guideline 3.1.3(e) says physical goods or services consumed outside the app must use purchase methods other than in-app purchase, such as Apple Pay or traditional card entry.
- Apple StoreKit documentation says external purchase APIs and entitlements are for qualifying apps only and require Apple-granted entitlements plus the required app entitlement and property-list configuration.
- App Store Connect App Review information requires review contact details and allows App Review notes for information needed to test the app, including app-specific settings, test accounts, and review-relevant conditions.
- App Store Connect in-app purchase information says review notes can provide additional information that helps Apple review an in-app purchase submission.

## Current Product Behavior

- Wakeve records shared expenses, participant balances, settlement suggestions, and payment-readiness state.
- Wakeve can store a trusted Tricount handoff URL and open it only after local trust validation.
- For the first App Store submission, Wakeve treats external payment handoff as Tricount-only; arbitrary external payment providers are rejected when a payment-pot URL is present.
- Wakeve does not currently sell premium app features, subscriptions, digital content, consumable credits, boosts, or other digital goods.
- Wakeve does not currently declare StoreKit products or App Store in-app purchases.
- Local source scan refreshed on 2026-06-01 found no StoreKit/IAP/paywall API references in `iosApp/src`, `shared/src/commonMain/kotlin`, `server/src/main/kotlin`, or `apps/landing/src`.

## App Store Review Position

Wakeve must not submit with ambiguous payment behavior. For the first App Store submission, payment surfaces must be positioned as shared-event expense coordination for real-world events and services consumed outside the app.

Before setting `APP_STORE_PAYMENT_COMPLIANCE_CONFIRMED=true`, verify and record evidence that:

- Any external payment or Tricount handoff is only for real-world event expenses, shared costs, or services consumed outside the app.
- No external payment flow unlocks app features, subscriptions, digital content, digital credits, boosts, or premium functionality.
- No in-app copy tells users to bypass App Store in-app purchase for digital goods or services.
- If Wakeve later monetizes digital features, the release either uses App Store in-app purchase or has a documented entitlement/legal basis for the relevant storefronts.
- App Review notes explain the business model if payment surfaces are enabled for the review build.
- Tricount/provider URLs are HTTPS-only, trusted-domain validated, and not arbitrary user-entered external purchase links.
- Payment-pot links are restricted to trusted Tricount hosts for this submission; any future provider expansion requires a new App Store payment review and evidence update.
- Storefront-specific external-purchase-link behavior is not used for this first submission because Wakeve is not selling digital content or services.

## First Submission Recommendation

Keep payment and Tricount surfaces enabled only if they are clearly limited to real-world shared-event settlement and the review build still rejects arbitrary external payment-provider URLs. If that cannot be proven in the TestFlight build and review notes, hide the payment surfaces for App Review or defer submission until the payment flow is clarified.

Set `APP_STORE_PAYMENT_COMPLIANCE_CONFIRMED=true` only after the evidence above is captured in `docs/APP_STORE_PAYMENT_EVIDENCE.md`, `docs/APP_STORE_FINAL_SIGNOFF.md`, and the final release shell or CI secret store.
