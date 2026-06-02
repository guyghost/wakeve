# App Store EULA Evidence - Wakeve

Date: 2026-06-01

Status: PENDING

Do not change the marker below until the App Store Connect License Agreement setting has been reviewed for the exact app record and aligned with Wakeve's public Terms of Service.

APP_STORE_EULA_EVIDENCE_COMPLETE=false

## Apple Source Baseline

Last checked: 2026-05-28.

- Apple App Store Connect App Information includes a License Agreement field.
- Apple's standard EULA applies in all regions when no custom EULA is provided.
- If no custom EULA is provided, Apple's standard EULA applies to the app and the license agreement link is not shown on the App Store product page.
- Apple publishes the standard Licensed Application End User License Agreement at `https://www.apple.com/legal/internet-services/itunes/dev/stdeula/`, and it must be recorded by URL/date when Wakeve uses the standard EULA option.
- Apple says a custom EULA can supersede the standard Apple EULA for one or more regions.
- App Store Connect custom EULA text is entered as plain text; HTML tags are stripped and escaped, and only line break characters are accepted.
- Apple says localized custom EULA text must be added for each language within the same text field, and the selected countries or regions must be recorded.
- A custom EULA choice must record all chosen countries or regions, because App Store Connect applies the custom agreement only to the selected territory scope.

## Required EULA Review

| Area | Required Evidence | Result |
| --- | --- | --- |
| License agreement choice | App Store Connect App Information records whether Wakeve uses Apple's standard EULA or a custom EULA. | Pending |
| Terms alignment | `docs/TERMS_OF_SERVICE.md`, the deployed `/terms` page, and the selected App Store Connect EULA choice do not conflict. | Pending |
| Standard EULA reference | If Apple's standard EULA is used, the Apple standard EULA URL, review date, and selected App Store Connect standard-option screenshot/export are attached. | Pending |
| Custom EULA scope | If a custom EULA is used, the plain-text agreement, countries/regions, and localized text strategy are recorded. | Pending |
| Custom EULA formatting | If a custom EULA is used, the submitted text is plain text only, contains no required HTML formatting, uses line breaks only, and includes any localized custom EULA text in the same field. | Pending |
| Territory scope | The reviewer records whether the standard EULA applies globally or which countries/regions use a custom EULA. | Pending |
| Standard EULA scope | If Apple's standard EULA is used, the release owner confirms Wakeve's public Terms of Service remain service terms and do not purport to replace the App Store license agreement. | Pending |
| Legal owner approval | Legal/product owner, date, App Store Connect screenshot/export, and selected territories are attached. | Pending |
| Product page consistency | App Store product page, support URL, privacy URL, and terms URL consistently describe the user agreement surface. | Pending |
| Final build match | The EULA decision is checked against the same App Store app record and version used for the submitted review build. | Pending |

## Apple References

- App Store Connect App Information includes a License Agreement property and allows either Apple's standard EULA or a custom license agreement: https://developer.apple.com/help/app-store-connect/reference/app-information/app-information
- Apple provides a standard EULA for all countries and regions; without a custom EULA, the standard EULA applies and no license-agreement link is shown on the product page: https://developer.apple.com/help/app-store-connect/manage-app-information/provide-a-custom-license-agreement/
- Custom EULA text is entered as plain text and can be applied to selected countries or regions: https://developer.apple.com/help/app-store-connect/manage-app-information/provide-a-custom-license-agreement/
- Apple standard Licensed Application End User License Agreement reference: https://www.apple.com/legal/internet-services/itunes/dev/stdeula/

## Evidence Commands

Run or record equivalent review output:

```bash
./scripts/lint-store-metadata.sh --ios-only
sed -n '1,220p' docs/TERMS_OF_SERVICE.md
sed -n '1,220p' webApp/src/routes/terms/+page.svelte
rg -n "EULA|license agreement|terms of service|Terms of Service|Apple standard EULA|custom EULA" docs webApp/src composeApp/metadata/ios
```

## Local Terms And EULA Alignment Scan Result

Local scan date: 2026-06-01

Result: Wakeve has local Terms of Service sources and a deployed-route implementation ready for review, but this is pre-submission evidence only because the App Store Connect License Agreement setting and legal owner approval are not captured.

- Source Terms document: `docs/TERMS_OF_SERVICE.md`, 226 lines, SHA-256 `acb3e0af841629dbeabf6c868d7474848e3217b625ce0e826e86dcabf2abfd95`.
- Public terms route source: `webApp/src/routes/terms/+page.svelte`, 121 lines, SHA-256 `b97e8ab88b11dc4c17ac6c9c11fae3076fc64e919554fb61c360d6bfd87d9ca4`.
- Public terms route exposes `Terms of Service - Wakeve`, effective date `April 14, 2026`, service scope, account access, user conduct/content, privacy link, intellectual property, third-party services, disclaimers/liability, termination, France governing law, and `legal@wakeve.app`.
- Local Terms document covers acceptance, service description, account registration, guest mode, user conduct, privacy, intellectual property, third-party services, disclaimers, limitation of liability, termination, dispute resolution, governing law, and legal contact.
- iOS metadata privacy URLs for `en-US` and `fr-FR` both point to `https://wakeve.app/privacy`; support URLs for both locales point to `https://wakeve.app/support`.
- No repository file currently records `APP_STORE_EULA_EVIDENCE_COMPLETE=true`, and this file keeps `APP_STORE_EULA_EVIDENCE_COMPLETE=false`.
- The current local evidence supports using Apple's standard EULA with Wakeve's public Terms of Service as service terms, but the App Store Connect License Agreement setting has not been captured.
- The current local evidence does not prove which App Store Connect License Agreement option is selected. If the standard option is selected, attach the Apple standard EULA URL/date plus App Store Connect evidence. If a custom option is selected, attach the exact plain-text EULA, selected countries/regions, localization handling, and legal approval.
- The local Terms document still describes account deletion by email. This is not treated as AS-09 account deletion closure; the review build still needs the in-app account deletion evidence tracked in `docs/APP_STORE_ACCOUNT_DELETION_EVIDENCE.md` before final signoff.
- The 2026-06-01 refresh confirms the local Terms and public `/terms` route hashes still match the repository paths checked during App Store metadata linting.

This local scan does not close AS-22. The final reviewer must record the App Store Connect License Agreement decision, confirm whether Apple's standard EULA or a custom EULA is selected, attach legal/product owner approval, verify selected countries or regions if a custom EULA is used, and compare the decision with the live `/terms` page before setting `APP_STORE_EULA_EVIDENCE_COMPLETE=true`.

## Evidence To Attach

Record these before final signoff:

- App Store Connect screenshot or API/export evidence of the License Agreement setting.
- Decision: Apple standard EULA or custom EULA.
- If standard EULA: Apple standard EULA URL `https://www.apple.com/legal/internet-services/itunes/dev/stdeula/`, review date, confirmation that Wakeve's public Terms of Service are service terms, and confirmation that the public terms do not conflict with Apple's standard EULA.
- If custom EULA: exact plain-text EULA, territory list, localization handling in the same text field, no HTML-dependent formatting, line breaks only, and legal approval.
- Country or region scope evidence showing whether the standard EULA is global or a custom EULA applies to selected countries/regions.
- Reviewer/date and legal owner who approved the final choice.

## Closure Rule

Set `APP_STORE_EULA_EVIDENCE_COMPLETE=true` only after:

- The App Store Connect License Agreement setting is recorded for the submitted app record.
- The selected EULA choice is aligned with Wakeve's public Terms of Service and `/terms` page.
- Any custom EULA plain text, territory scope, and localization decision are recorded if applicable.
- `docs/APP_STORE_FINAL_SIGNOFF.md` references this completed evidence for the submitted review build.
- `APP_STORE_EULA_CONFIRMED=true` is set only in the release shell or CI secret store after this evidence is complete.
