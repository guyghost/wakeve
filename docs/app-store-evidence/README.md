# App Store Evidence Index

Date: 2026-06-13

This directory stores local simulator screenshots used as partial App Store readiness evidence. These files are not final App Store Connect or TestFlight evidence. Final signoff still requires screenshots or exports from the exact uploaded review build where the relevant evidence file says so.

## Scope

- Source: XcodeBuildMCP local simulator sessions.
- Build type: Debug simulator unless a row explicitly says otherwise.
- Evidence value: partial local UI, accessibility, and layout evidence.
- Limitation: not sufficient for `APP_STORE_*_EVIDENCE_COMPLETE=true` markers.

## Screenshot Inventory

| File | Screen | Device | Dimensions | SHA-256 |
| --- | --- | --- | --- | --- |
| `xcodebuildmcp-iphone-onboarding-events-2026-05-27.jpg` | Onboarding events page | iPhone simulator | 368x800 | `51e35ab3f6a712fb10e6beeefad7c47b552b698709830431930520dc576c5f62` |
| `xcodebuildmcp-iphone-onboarding-collaboration-2026-05-27.jpg` | Onboarding collaboration page | iPhone simulator | 368x800 | `2112d41ae970736f99f822a98d2cff9e7c959932cfb0db3206873872d96fb7e6` |
| `xcodebuildmcp-iphone-login-2026-05-27.jpg` | Login | iPhone simulator | 368x800 | `66680062ada1589e6985bbad2fb2e77458f7bb57a2d1887e12490877b5cd9ed8` |
| `xcodebuildmcp-iphone-login-guest-2026-05-27.jpg` | Guest login path | iPhone simulator | 368x800 | `a0e0a4feb9d8a6ba92f8d4ed971fb501236901611928d19f5dfde5220d64ebf5` |
| `xcodebuildmcp-iphone-login-refresh-2026-05-28.jpg` | Login refresh | iPhone simulator | 368x800 | `e5908b977b931b73347a86c3bb18e77a6c95fb8e6b263173b2cbc50dce169036` |
| `xcodebuildmcp-iphone-login-accessibility-2026-05-28.jpg` | Login accessibility pass | iPhone simulator | 368x800 | `82176c7b6c7803f6fc132e1178501e1c2fb70e82d9a37d0266ef06ddb8729fa3` |
| `xcodebuildmcp-iphone-post-login-home-2026-05-27.jpg` | Authenticated home | iPhone simulator | 368x800 | `25162ae1ce2a4774affbc92d3d36cb5934b988e39c09329abe3473706dc9ecd6` |
| `xcodebuildmcp-iphone-post-login-home-2026-05-28.jpg` | Authenticated home refresh | iPhone simulator | 368x800 | `c889225f81c53860cd208f5b0d1738926de9407e2b64f9096273c050b89b7c90` |
| `xcodebuildmcp-iphone-home-dark-high-contrast-axxxl-2026-05-28.jpg` | Home, dark mode, high contrast, accessibility XXXL | iPhone simulator | 368x800 | `3b80c6b3c379d2035ab9e789767838a3efe2cddefb3ac2571eb5682dccd71324` |
| `xcodebuildmcp-iphone-home-dark-high-contrast-axxxl-fixed-2026-05-28.jpg` | Home, dark/high-contrast/XXXL after layout fix | iPhone simulator | 368x800 | `826464f6f30bc59544f31f65a2606aeceb2e6546615a3fd28ed9d91cd2fb03b3` |
| `xcodebuildmcp-iphone-home-high-contrast-axxxl-fixed-no-truncation-2026-05-28.jpg` | Home, high-contrast/XXXL no-truncation check | iPhone simulator | 368x800 | `8ae7ca621e72646ef112016df51bac937284b91aef515dbb3c51d22451b4a9a1` |
| `xcodebuildmcp-iphone-create-event-2026-05-27.jpg` | Create event | iPhone simulator | 368x800 | `5e13fafc7ad298126fcd982e6bbfad4115c1bd9b74fa2ebcf0adf09ccb21a99e` |
| `xcodebuildmcp-ipad-login-2026-05-27.jpg` | Login | iPad simulator | 599x800 | `07ec532e8b6a7e0fb9a4bc882f1e9f4792a2211527cfa94aa9a5f518a58e311c` |

## Related Evidence Files

- `docs/APP_STORE_ACCESSIBILITY_EVIDENCE.md`
- `docs/APP_STORE_MEDIA_LOCALIZATION_EVIDENCE.md`
- `docs/APP_STORE_TESTFLIGHT_EVIDENCE.md`
- `docs/ios-release-screen-evidence/release-screen-evidence-2026-06-13T12-53-27Z.md`
- `docs/APP_STORE_FINAL_SIGNOFF.md`

## Refresh Commands

```bash
./scripts/audit-ios-release-screen-evidence.sh
for file in docs/app-store-evidence/*.{jpg,jpeg,png}; do
  sips -g pixelWidth -g pixelHeight "$file"
  shasum -a 256 "$file"
done
```

Before final submission, replace or supplement this local index with evidence from the uploaded TestFlight/App Review build, then update the matching `APP_STORE_*_EVIDENCE_COMPLETE` marker only when that file's closure rule is satisfied.
