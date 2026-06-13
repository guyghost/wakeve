# App Store Media And Localization Audit

Generated: 2026-06-13T12:23:27Z

Status: LOCAL EVIDENCE

This report audits local Fastlane/App Store metadata and screenshots. It does not close AS-20 until the App Store Connect media page and uploaded review build are checked.

## Summary

| Check | Result |
| --- | --- |
| Locales | `en-US fr-FR` |
| Screenshot aggregate hash | `e1d72a791111bc43b561e7b463043167e860a47f3c290443c0a015f64ef3effe` |
| App preview videos | 0 |
| Findings | 0 |

## Screenshot Inventory

### Fastlane Upload Screenshots

| Locale | File | Dimensions | Family | SHA-256 | Result |
| --- | --- | ---: | --- | --- | --- |
| `en-US` | `composeApp/screenshots/ios/en-US/01-iphone-home.png` | 1320x2868 | iPhone | `5052bab23e7ff422caee326069fefd6625f3d2832332fa5103529f1db384fe6a` | PASS |
| `en-US` | `composeApp/screenshots/ios/en-US/02-ipad-home.png` | 2048x2732 | iPad | `e00faca9365c79f0ad5984066de67c7fffcd9271343273f7d81e4ff0df6baef4` | PASS |
| `fr-FR` | `composeApp/screenshots/ios/fr-FR/01-iphone-home.png` | 1320x2868 | iPhone | `5052bab23e7ff422caee326069fefd6625f3d2832332fa5103529f1db384fe6a` | PASS |
| `fr-FR` | `composeApp/screenshots/ios/fr-FR/02-ipad-home.png` | 2048x2732 | iPad | `e00faca9365c79f0ad5984066de67c7fffcd9271343273f7d81e4ff0df6baef4` | PASS |

### Metadata Screenshots

| Locale | File | Dimensions | Family | SHA-256 | Result |
| --- | --- | ---: | --- | --- | --- |
| `en-US` | `composeApp/metadata/ios/en-US/screenshots/01-iphone-home.png` | 1320x2868 | iPhone | `5052bab23e7ff422caee326069fefd6625f3d2832332fa5103529f1db384fe6a` | PASS |
| `en-US` | `composeApp/metadata/ios/en-US/screenshots/02-ipad-home.png` | 2048x2732 | iPad | `e00faca9365c79f0ad5984066de67c7fffcd9271343273f7d81e4ff0df6baef4` | PASS |
| `fr-FR` | `composeApp/metadata/ios/fr-FR/screenshots/01-iphone-home.png` | 1320x2868 | iPhone | `5052bab23e7ff422caee326069fefd6625f3d2832332fa5103529f1db384fe6a` | PASS |
| `fr-FR` | `composeApp/metadata/ios/fr-FR/screenshots/02-ipad-home.png` | 2048x2732 | iPad | `e00faca9365c79f0ad5984066de67c7fffcd9271343273f7d81e4ff0df6baef4` | PASS |

## Localized Metadata Field Lengths

| Locale | Field | Characters | Limit | Result |
| --- | --- | ---: | ---: | --- |
| `en-US` | `name` | 6 | 30 | PASS |
| `en-US` | `subtitle` | 24 | 30 | PASS |
| `en-US` | `description` | 1540 | 4000 | PASS |
| `en-US` | `keywords` | 70 | 100 | PASS |
| `en-US` | `promotional_text` | 85 | 170 | PASS |
| `en-US` | `release_notes` | 179 | 4000 | PASS |
| `en-US` | `privacy_url` | 26 | 500 | PASS |
| `en-US` | `support_url` | 26 | 500 | PASS |
| `fr-FR` | `name` | 6 | 30 | PASS |
| `fr-FR` | `subtitle` | 29 | 30 | PASS |
| `fr-FR` | `description` | 1987 | 4000 | PASS |
| `fr-FR` | `keywords` | 88 | 100 | PASS |
| `fr-FR` | `promotional_text` | 109 | 170 | PASS |
| `fr-FR` | `release_notes` | 196 | 4000 | PASS |
| `fr-FR` | `privacy_url` | 26 | 500 | PASS |
| `fr-FR` | `support_url` | 26 | 500 | PASS |

## App Preview Inventory

No .mov, .mp4, or .m4v files were found under local iOS metadata or screenshot directories. First-release decision remains: omit app previews.

## Closure Notes

- Keep `APP_STORE_MEDIA_LOCALIZATION_EVIDENCE_COMPLETE=false` until App Store Connect media, localized metadata, editable status, and uploaded-build accuracy are reviewed.
- Attach the generated report to `docs/APP_STORE_MEDIA_LOCALIZATION_EVIDENCE.md` after meaningful media or metadata changes.
- If app previews are added, validate duration, format, poster frame, locale fallback, rights, and source-device capture.
