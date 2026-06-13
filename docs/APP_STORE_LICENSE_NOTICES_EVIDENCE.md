# App Store License Notices Evidence - Wakeve

Date: 2026-06-01

Status: PENDING

Do not change the marker below until the exact App Store review build has a complete dependency license inventory and every required third-party notice is bundled in the app or published through the documented support/legal surface.

APP_STORE_LICENSE_NOTICES_EVIDENCE_COMPLETE=false

## Apple Source Baseline

Last checked: 2026-05-27.

- Apple says App Review submissions should be final versions with all necessary metadata and fully functional URLs included.
- Apple says placeholder text, empty websites, and other temporary content should be removed before submission.
- Apple says app metadata, including privacy information, description, screenshots, and previews, must accurately reflect the app's core experience and stay up to date with new versions.
- Apple says apps should include only content the developer created or has a license to use.
- Apple says apps must not use protected third-party material, including trademarks, copyrighted works, or patented ideas, without permission.
- Apple says apps must not include misleading, false, or copycat representations, names, or metadata.
- Apple says apps should be submitted by the person or legal entity that owns or has licensed the intellectual property and other relevant rights.
- Apple says apps must be responsible for services provided by the app.
- Apple says review information should include the details needed for App Review when rights, access, or review flow evidence is required.

## Required License Review

| Area | Required Evidence | Result |
| --- | --- | --- |
| Dependency inventory | iOS app, shared Kotlin Multiplatform framework, Swift Package Manager, CocoaPods, Gradle, npm, and Fastlane/runtime dependencies used for the submitted build are inventoried with name, version, source, and license. | Pending |
| License compatibility | Every shipped dependency license is reviewed for App Store distribution compatibility, attribution obligations, source/code-offer obligations, and copyleft restrictions. | Pending |
| Notice obligations | MIT, BSD, Apache-2.0, ISC, CC-BY, and any other attribution-bearing licenses have required copyright notices preserved. | Pending |
| App-bundled notices | Required notices are reachable from the submitted iOS app or from a stable legal/support URL referenced by the app and App Review notes. | Pending |
| Generated artifacts | Generated license reports from Gradle, Swift Package Manager/CocoaPods, npm, and Fastlane/runtime tooling are attached or linked for the submitted build. | Pending |
| Prohibited or unclear licenses | No GPL/AGPL/LGPL/static-link risk, unknown license, missing license, or custom license remains unresolved for shipped app code. | Pending |
| Review notes | App Review notes identify the third-party notices location if the app exposes a dedicated acknowledgements/legal screen or support URL. | Pending |
| Final build match | The notice inventory matches the signed IPA/archive submitted to App Store Connect, not only the local development tree. | Pending |

## Apple References

- App Review Guideline 5.2 requires apps to include only content the developer created or has a license to use: https://developer.apple.com/app-store/review/guidelines/
- App Review Guideline 5.2.1 covers protected third-party material and requires the submitting entity to own or license relevant rights: https://developer.apple.com/app-store/review/guidelines/
- App metadata and review information must accurately describe app behavior and review-relevant access: https://developer.apple.com/app-store/review/guidelines/

## Evidence Commands

Run or record equivalent review output for the submitted build:

```bash
./scripts/app-store-license-inventory.sh --fetch-remote-metadata --output docs/APP_STORE_LICENSE_INVENTORY_DRAFT.md
./scripts/app-store-license-inventory.sh --fetch-remote-metadata --fail-on-unknown
./scripts/app-store-third-party-notices.sh --markdown-output docs/APP_STORE_THIRD_PARTY_NOTICES.md --web-output apps/landing/src/routes/third-party-notices/+page.svelte
./gradlew :shared:dependencies --configuration iosArm64MainCompileClasspath
./gradlew :shared:dependencies --configuration commonMainImplementationDependenciesMetadata
npm ls --all --json
cd apps/landing && npx --yes pnpm@10 licenses list --json
bundle exec fastlane action update_fastlane
find iosApp shared composeApp apps/landing fastlane -maxdepth 4 \( -iname '*license*' -o -iname 'NOTICE*' -o -iname 'COPYING*' \) | sort
rg -n "license|notice|acknowledg|third-party|open source|OSS" iosApp/src shared/src composeApp/src apps/landing/src docs
```

If a command is not applicable to the final iOS artifact, record that decision in the evidence table with the reason.

## Local Strict Inventory Result

Command run on 2026-06-13:

```bash
./scripts/app-store-license-inventory.sh --fetch-remote-metadata --fail-on-unknown
```

Result: passed locally for the submitted iOS scope in the current repository inventory.

- Dependencies listed: 316
- Unknown licenses: 3
- Copyleft keywords detected: 1
- Submitted iOS unknown/copyleft risks: 0
- Remote Maven POM fetching: enabled
- Remote RubyGems fetching: enabled

This is local pre-submission evidence only. It does not close AS-21 until the inventory is matched to the signed App Store review IPA/archive, legal/owner review approves any non-iOS scoped unknown or copyleft-risk dependency, required notices are reachable in the submitted app or stable legal/support URL, and `APP_STORE_LICENSE_NOTICES_EVIDENCE_COMPLETE=true` is set for the reviewed build.

## Local Notices Generation Result

Commands run on 2026-06-13:

```bash
./scripts/app-store-license-inventory.sh --fetch-remote-metadata --output docs/APP_STORE_LICENSE_INVENTORY_DRAFT.md
./scripts/app-store-license-inventory.sh --fetch-remote-metadata --fail-on-unknown
./scripts/app-store-third-party-notices.sh --markdown-output docs/APP_STORE_THIRD_PARTY_NOTICES.md --web-output apps/landing/src/routes/third-party-notices/+page.svelte
wc -l docs/APP_STORE_LICENSE_INVENTORY_DRAFT.md docs/APP_STORE_THIRD_PARTY_NOTICES.md apps/landing/src/routes/third-party-notices/+page.svelte
shasum -a 256 docs/APP_STORE_LICENSE_INVENTORY_DRAFT.md docs/APP_STORE_THIRD_PARTY_NOTICES.md apps/landing/src/routes/third-party-notices/+page.svelte
```

Generated local artifacts:

- `docs/APP_STORE_LICENSE_INVENTORY_DRAFT.md`: 348 lines, SHA-256 `62dbb5f5bb604ecbbe12bd9eb7be254f7027f352db1a043289151ab8d24162e6`
- `docs/APP_STORE_THIRD_PARTY_NOTICES.md`: 371 lines, SHA-256 `d9586fc13c6458680525f6ea7046eae0bb56b663c064946fcede07e9a8624732`
- `apps/landing/src/routes/third-party-notices/+page.svelte`: 2313 lines, SHA-256 `977fca4f1ae05c3c8e28cb08e100531d6a4804734c97e88084fc25b2fdf710df`

Generated local notice coverage:

- `docs/APP_STORE_THIRD_PARTY_NOTICES.md` was regenerated from `docs/APP_STORE_LICENSE_INVENTORY_DRAFT.md`.
- `apps/landing/src/routes/third-party-notices/+page.svelte` was regenerated from the same inventory for the public notices route.
- The generated notices record `Dependencies listed: 316`, `Unknown licenses: 3`, `Copyleft keywords detected: 1`, and `Submitted iOS unknown/copyleft risks: 0`.
- `scripts/app-store-license-inventory.sh` now classifies Android-only, test-only, backend, release-tooling, web notice surface, and submitted-iOS dependencies separately. AndroidX, Google Play Services, Firebase Android, ML Kit, Coil, and Compose/App Android dependencies are no longer counted as `submitted-ios` evidence for the iOS App Store build.
- The notices route is local source evidence only. Final closure still requires the deployed `https://wakeve.app/third-party-notices` URL to be reachable and matched to the signed App Store review build.

## Evidence To Attach

Record these before final signoff:

- Dependency inventory export for the signed review build, including direct and transitive dependencies that ship inside the iOS app or shared framework.
- Local inventory draft from `docs/APP_STORE_LICENSE_INVENTORY_DRAFT.md`, generated by `./scripts/app-store-license-inventory.sh --fetch-remote-metadata`.
- `./scripts/app-store-license-inventory.sh --fetch-remote-metadata --fail-on-unknown` passes for dependencies marked as submitted iOS scope, and every remaining unknown/copyleft-risk dependency outside that scope is resolved manually, approved, or recorded as not shipped in the App Store review build.
- License classification for every shipped dependency and confirmation that no prohibited or unresolved license remains.
- Notice text or notice URL used by the App Store review build.
- Public notices draft from `docs/APP_STORE_THIRD_PARTY_NOTICES.md`, exposed at `https://wakeve.app/third-party-notices` by `apps/landing/src/routes/third-party-notices/+page.svelte`.
- Screenshot or source reference showing how users or App Review can reach third-party acknowledgements/notices when required.
- Reviewer/date and legal owner who approved the license inventory.
- Any exception decision for dependencies used only in build tooling and not shipped in the iOS app.

## Closure Rule

Set `APP_STORE_LICENSE_NOTICES_EVIDENCE_COMPLETE=true` only after:

- The signed App Store review build dependency inventory is complete.
- `docs/APP_STORE_LICENSE_INVENTORY_DRAFT.md` has been regenerated for the release commit and `./scripts/app-store-license-inventory.sh --fetch-remote-metadata --fail-on-unknown` passes for submitted iOS scope, with every unknown/copyleft-risk license outside that scope manually resolved, approved, or marked not shipped.
- Required third-party notices are bundled in-app or published through the documented support/legal surface.
- App Review notes reference the third-party notices URL and the deployed support page links to `/third-party-notices`.
- No shipped dependency has an unknown, prohibited, or unresolved license obligation.
- `docs/APP_STORE_CONTENT_RIGHTS_EVIDENCE.md` and `docs/APP_STORE_FINAL_SIGNOFF.md` reference this completed evidence for the submitted review build.
- `APP_STORE_LICENSE_NOTICES_CONFIRMED=true` is set only in the release shell or CI secret store after this evidence is complete.
