# App Store Pricing And Availability Evidence - Wakeve

Date: 2026-06-01

Status: PENDING

Do not change the marker below until the exact App Store Connect Pricing and Availability page is recorded for the review build and the final storefront, price, tax, preorder, and distribution choices are consistent with Wakeve's business model and legal evidence.

APP_STORE_PRICING_AVAILABILITY_EVIDENCE_COMPLETE=false

## Apple Source Baseline

Last checked: 2026-05-28.

- Apple says pricing and availability properties determine where and when an app is available on the App Store and at what price.
- Apple says developers must set pricing for an app before submitting it for review.
- Apple says paid content on the App Store requires the membership Account Holder to accept the Paid Apps Agreement in Tax and Banking.
- Apple says if the most recent Paid Apps Agreement has not been accepted, the app can only be offered for free.
- Apple says the required role for setting a price is Account Holder, Admin, or App Manager.
- Apple says developers choose from up to 800 price points by default and can request access to additional higher price points.
- Apple says Apple periodically updates prices in some regions based on tax and foreign-exchange changes to keep prices equalized with the base country or region.
- Apple says a base country or region is used to automatically generate prices across other storefronts and currencies.
- Apple says prices in the base country or region are not adjusted by Apple for taxes or foreign exchange changes.
- Apple says manually managed storefront pricing makes the developer responsible for staying current with taxes and exchange rates.
- Apple says price changes can be scheduled with a start date and end date.
- Apple says pre-orders are available only if the app has never been published on the App Store.
- Apple says availability is the set of storefronts where the app is available to purchase or download.
- Apple says app availability must be set before App Review and can cover any of the 175 countries or regions where the App Store is available.
- Apple says choosing All Countries or Regions also makes the app available in future App Store countries or regions, while Specific Countries or Regions requires selecting the intended storefronts.
- Apple says once an app becomes available in a country or region, pre-orders are no longer possible in that same location.
- Apple says deselecting a country or region removes the app from sale there, while previous customers may continue to receive updates and redownload from purchase history if the necessary contract remains active.
- Apple says availability changes take effect immediately but may require up to 24 hours to be visible to all users.
- Apple says education and business distribution can include Apple Business Manager, Apple School Manager, education discounts, and custom app distribution.
- Apple says public apps are automatically available for volume purchase in Apple Business Manager and Apple School Manager for the same price.
- Apple says a 50% education discount can be offered for purchases of 20 or more copies.
- Apple says private distribution restricts the app to selected businesses or organizations through Apple Business Manager or Apple School Manager.
- Apple says unlisted apps require the app's availability to be public, App Review submission or approval, and an unlisted-link request.
- Apple says changing distribution from public to private, or private to public, requires a new app record and binary, except changing a public app to unlisted.
- Apple says custom app distribution is available only before the app has been approved.
- Apple says the tax category defaults to the App Store software category unless changed.
- Apple says last compatible version settings control which older app versions existing customers can download from iCloud, and versions should be excluded for legal or usability issues.

## Required Pricing And Availability Review

| Area | Required Evidence | Result |
| --- | --- | --- |
| Price | App Store Connect price is selected for the app. If Wakeve is free for the first release, record the free price tier and confirm no paid app sale is configured. | Pending |
| Paid Apps Agreement | Confirm not required for a free/no-IAP release, or active in App Store Connect if any paid app sale, In-App Purchase, subscription, or paid digital content is enabled. | Pending |
| Price schedule | Start date, end date, and any scheduled price changes are empty or intentionally configured for launch. | Pending |
| Storefront availability | App Store country/region availability is selected and does not contradict EU DSA handling, China mainland requirements, Korea rating requirements, or launch support coverage. | Pending |
| Pre-order | Pre-order is disabled for first release unless a launch owner records release date, support readiness, and App Review plan. | Pending |
| Distribution method | Public, Private, or Unlisted strategy is recorded. For the first App Store launch, public distribution is expected unless a new-app-record/private/unlisted strategy is intentionally approved. | Pending |
| Education and business distribution | Apple Business Manager, Apple School Manager, volume purchase, education discount, and custom app/private distribution choices are recorded and match the intended public launch. | Pending |
| Tax category | App Store tax category is reviewed; default App Store software category is accepted or a different category is documented. | Pending |
| Last compatible version | No previous public version exists for first release, or any legal/usability exclusion is documented. | Pending |

## Apple References

- App pricing and availability determines where and when the app is available and at what price: https://developer.apple.com/help/app-store-connect/reference/pricing-and-availability/app-pricing-and-availability
- App Store Connect price setup requires a selected price before review and uses Account Holder, Admin, or App Manager access: https://developer.apple.com/help/app-store-connect/manage-app-pricing/set-a-price
- App Store availability must be set before review and can target all or selected App Store countries and regions: https://developer.apple.com/help/app-store-connect/manage-your-apps-availability/manage-availability-for-your-app-on-the-app-store
- Distribution methods cover public, private, and unlisted App Store availability constraints: https://developer.apple.com/help/app-store-connect/manage-your-apps-availability/set-distribution-methods
- Submitting an app requires required metadata and a chosen build before adding the version for review: https://developer.apple.com/help/app-store-connect/manage-submissions-to-app-review/submit-an-app
- App information includes storefront-sensitive fields such as Content Rights, Age Rating, DSA, Korea, and China mainland availability considerations: https://developer.apple.com/help/app-store-connect/reference/app-information/app-information

## Evidence Commands

Run or record equivalent App Store Connect output:

```bash
./scripts/lint-store-metadata.sh --ios-only
rg -n "Paid Apps Agreement|payment|Tricount|In-App Purchase|subscription|StoreKit|digital|EU storefront|China mainland|Korea|GRAC|ICP|pre-order|custom app|education|business" docs composeApp/metadata/ios iosApp/src shared/src/commonMain/kotlin server/src/main/kotlin
```

## Local Free/No-IAP Scan Result

This section records repository-side pricing and in-app purchase evidence only. It does not prove the App Store Connect price tier, storefront list, tax category, pre-order setting, or Paid Apps Agreement status.

Commands refreshed on 2026-06-01:

```bash
rg -n "StoreKit|SKProduct|Product\\.products|InAppPurchase|purchase\\(" iosApp/src shared/src/commonMain/kotlin server/src/main/kotlin
rg -n "real-world shared event expenses|do not sell or unlock app features|trusted-domain validated|Tricount|StoreKit products|in-app purchases|premium app features|subscriptions|digital content" composeApp/metadata/ios/review_information/notes.txt docs/APP_STORE_PAYMENT_COMPLIANCE.md docs/APP_STORE_PAYMENT_EVIDENCE.md
wc -l docs/APP_STORE_PAYMENT_COMPLIANCE.md docs/APP_STORE_PAYMENT_EVIDENCE.md composeApp/metadata/ios/review_information/notes.txt
shasum -a 256 docs/APP_STORE_PAYMENT_COMPLIANCE.md docs/APP_STORE_PAYMENT_EVIDENCE.md composeApp/metadata/ios/review_information/notes.txt
```

Local result:

- StoreKit/IAP source scan returned `NO_MATCHES` for `StoreKit`, `SKProduct`, `Product.products`, `InAppPurchase`, and `purchase(` in `iosApp/src`, `shared/src/commonMain/kotlin`, and `server/src/main/kotlin`.
- `docs/APP_STORE_PAYMENT_COMPLIANCE.md` states that Wakeve does not currently sell premium app features, subscriptions, digital content, consumable credits, boosts, or other digital goods.
- `docs/APP_STORE_PAYMENT_COMPLIANCE.md` states that Wakeve does not currently declare StoreKit products or App Store in-app purchases.
- `composeApp/metadata/ios/review_information/notes.txt` explains that payment, settlement, and Tricount screens are for real-world shared event expenses only and do not sell or unlock app features, subscriptions, digital content, digital credits, boosts, or premium functionality.
- Local file line counts: `docs/APP_STORE_PAYMENT_COMPLIANCE.md` 56 lines, `docs/APP_STORE_PAYMENT_EVIDENCE.md` 99 lines, and `composeApp/metadata/ios/review_information/notes.txt` 7 lines.
- SHA-256 hashes: `docs/APP_STORE_PAYMENT_COMPLIANCE.md` `c476fd4e818e2d78f7ece9005b8d2fdeb8e1dc3c50481cf12541409572f88ded`; `docs/APP_STORE_PAYMENT_EVIDENCE.md` `c4cf0ba2a78955828e45c1b1d763b245ca94ef66391cd6513bcd29fd7cd276c2`; `composeApp/metadata/ios/review_information/notes.txt` `dd457aa7207cd7cb4743ac32aedcdf61d99d2e27a093b7d2ac9d9a38d6efdc1b`.

Local pricing recommendation:

- The repository-side evidence supports configuring the first App Store version as a free app with no In-App Purchase products, no subscriptions, no paid app sale, and no paid digital-content unlocks.
- Because no paid app sale, IAP, subscription, or paid digital content is supported by the current local evidence, the Paid Apps Agreement should be recorded as not required for the first release unless App Store Connect or the business owner chooses a paid configuration.
- Final closure still requires App Store Connect Pricing and Availability evidence for price tier, storefronts, price schedule, pre-order, education/business distribution, custom app distribution, tax category, Paid Apps Agreement status, and reviewer/date.

## Evidence To Attach

Record these before final signoff:

- App Store Connect Pricing and Availability screenshot/export for the submitted version.
- Selected price tier and base country/region, or confirmation that Wakeve is free for first release.
- Paid Apps Agreement decision from App Store Connect Business/Agreements or Tax and Banking.
- Storefront availability list or export, including All Countries or Regions versus Specific Countries or Regions, EU availability decision, future-country checkbox state, and any excluded countries/regions.
- Pre-order decision and release date if enabled.
- Public, Private, or Unlisted distribution method decision, including any new-app-record or unlisted-link request implications.
- Apple Business Manager, Apple School Manager, volume purchase, education discount, and custom app/private distribution choices.
- Tax category decision and any non-default tax rationale.
- Reviewer/date confirming the choices match `docs/APP_STORE_PAYMENT_EVIDENCE.md`, `docs/APP_STORE_DSA_TRADER_STATUS.md`, and `docs/APP_STORE_AVAILABILITY_EVIDENCE.md`.

## Closure Rule

Set `APP_STORE_PRICING_AVAILABILITY_EVIDENCE_COMPLETE=true` only after:

- Price, price schedule, and storefront availability are visible in App Store Connect for the submitted app version.
- Paid Apps Agreement status supports the selected price and any enabled paid/IAP behavior.
- Storefront choices do not contradict DSA, country-specific legal requirements, support readiness, launch availability evidence, or future-country availability intent.
- Pre-order, distribution method, education/business, custom app/private distribution, unlisted-link, tax category, and last-compatible-version choices are either disabled/defaulted intentionally or documented with launch-owner approval.
- `APP_STORE_PRICING_AVAILABILITY_CONFIRMED=true` is set only in the final release shell or CI secret store after this file is updated.
- `docs/APP_STORE_FINAL_SIGNOFF.md` references this completed evidence for the submitted review build.
