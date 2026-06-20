# iOS Release Screen Evidence Audit

Generated: 2026-06-20T21:10:30Z

Status: LOCAL EVIDENCE

This report maps local simulator screenshots to the release-flow screens listed in ROADMAP.md P1.2. It does not close App Store/TestFlight screenshot evidence; final closure still requires the uploaded review build or TestFlight build.

## Summary

| Required screen | Local result | Local matches | Next capture target |
| --- | --- | ---: | --- |
| Onboarding | PASS | 2 | Uploaded review build: first-run onboarding carousel |
| Login and guest path | PASS | 7 | Uploaded review build: login screen plus Continue as guest result |
| Create event | PASS | 1 | Uploaded review build: create-event wizard and preview |
| Event detail | MISSING | 0 | Uploaded review build: event detail for a seeded or reviewer-created event |
| Organization | MISSING | 0 | Uploaded review build: confirmed/organizing event with scenario or organization dashboard visible |

Local coverage: 3 / 5 required screens

Missing required screens: 2

## Inventory Source

- Source index: `docs/app-store-evidence/README.md`
- Inventory rows considered: 13

## Matched Evidence

### Onboarding

```text
- | xcodebuildmcp-iphone-onboarding-events-2026-05-27.jpg | Onboarding events page | iPhone simulator | 368x800 | 51e35ab3f6a712fb10e6beeefad7c47b552b698709830431930520dc576c5f62 |
- | xcodebuildmcp-iphone-onboarding-collaboration-2026-05-27.jpg | Onboarding collaboration page | iPhone simulator | 368x800 | 2112d41ae970736f99f822a98d2cff9e7c959932cfb0db3206873872d96fb7e6 |
```

Next capture target: Uploaded review build: first-run onboarding carousel

### Login and guest path

```text
- | xcodebuildmcp-iphone-login-2026-05-27.jpg | Login | iPhone simulator | 368x800 | 66680062ada1589e6985bbad2fb2e77458f7bb57a2d1887e12490877b5cd9ed8 |
- | xcodebuildmcp-iphone-login-guest-2026-05-27.jpg | Guest login path | iPhone simulator | 368x800 | a0e0a4feb9d8a6ba92f8d4ed971fb501236901611928d19f5dfde5220d64ebf5 |
- | xcodebuildmcp-iphone-login-refresh-2026-05-28.jpg | Login refresh | iPhone simulator | 368x800 | e5908b977b931b73347a86c3bb18e77a6c95fb8e6b263173b2cbc50dce169036 |
- | xcodebuildmcp-iphone-login-accessibility-2026-05-28.jpg | Login accessibility pass | iPhone simulator | 368x800 | 82176c7b6c7803f6fc132e1178501e1c2fb70e82d9a37d0266ef06ddb8729fa3 |
- | xcodebuildmcp-iphone-post-login-home-2026-05-27.jpg | Authenticated home | iPhone simulator | 368x800 | 25162ae1ce2a4774affbc92d3d36cb5934b988e39c09329abe3473706dc9ecd6 |
- | xcodebuildmcp-iphone-post-login-home-2026-05-28.jpg | Authenticated home refresh | iPhone simulator | 368x800 | c889225f81c53860cd208f5b0d1738926de9407e2b64f9096273c050b89b7c90 |
- | xcodebuildmcp-ipad-login-2026-05-27.jpg | Login | iPad simulator | 599x800 | 07ec532e8b6a7e0fb9a4bc882f1e9f4792a2211527cfa94aa9a5f518a58e311c |
```

Next capture target: Uploaded review build: login screen plus Continue as guest result

### Create event

```text
- | xcodebuildmcp-iphone-create-event-2026-05-27.jpg | Create event | iPhone simulator | 368x800 | 5e13fafc7ad298126fcd982e6bbfad4115c1bd9b74fa2ebcf0adf09ccb21a99e |
```

Next capture target: Uploaded review build: create-event wizard and preview

### Event detail

No matching local screenshot evidence was found.

Next capture target: Uploaded review build: event detail for a seeded or reviewer-created event

### Organization

No matching local screenshot evidence was found.

Next capture target: Uploaded review build: confirmed/organizing event with scenario or organization dashboard visible

## Evidence Integrity

| File | Result | Expected dimensions | Actual dimensions | Expected SHA-256 | Actual SHA-256 |
| --- | --- | --- | --- | --- | --- |
| `xcodebuildmcp-iphone-onboarding-events-2026-05-27.jpg` | PASS | 368x800 | 368x800 | `51e35ab3f6a712fb10e6beeefad7c47b552b698709830431930520dc576c5f62` | `51e35ab3f6a712fb10e6beeefad7c47b552b698709830431930520dc576c5f62` |
| `xcodebuildmcp-iphone-onboarding-collaboration-2026-05-27.jpg` | PASS | 368x800 | 368x800 | `2112d41ae970736f99f822a98d2cff9e7c959932cfb0db3206873872d96fb7e6` | `2112d41ae970736f99f822a98d2cff9e7c959932cfb0db3206873872d96fb7e6` |
| `xcodebuildmcp-iphone-login-2026-05-27.jpg` | PASS | 368x800 | 368x800 | `66680062ada1589e6985bbad2fb2e77458f7bb57a2d1887e12490877b5cd9ed8` | `66680062ada1589e6985bbad2fb2e77458f7bb57a2d1887e12490877b5cd9ed8` |
| `xcodebuildmcp-iphone-login-guest-2026-05-27.jpg` | PASS | 368x800 | 368x800 | `a0e0a4feb9d8a6ba92f8d4ed971fb501236901611928d19f5dfde5220d64ebf5` | `a0e0a4feb9d8a6ba92f8d4ed971fb501236901611928d19f5dfde5220d64ebf5` |
| `xcodebuildmcp-iphone-login-refresh-2026-05-28.jpg` | PASS | 368x800 | 368x800 | `e5908b977b931b73347a86c3bb18e77a6c95fb8e6b263173b2cbc50dce169036` | `e5908b977b931b73347a86c3bb18e77a6c95fb8e6b263173b2cbc50dce169036` |
| `xcodebuildmcp-iphone-login-accessibility-2026-05-28.jpg` | PASS | 368x800 | 368x800 | `82176c7b6c7803f6fc132e1178501e1c2fb70e82d9a37d0266ef06ddb8729fa3` | `82176c7b6c7803f6fc132e1178501e1c2fb70e82d9a37d0266ef06ddb8729fa3` |
| `xcodebuildmcp-iphone-post-login-home-2026-05-27.jpg` | PASS | 368x800 | 368x800 | `25162ae1ce2a4774affbc92d3d36cb5934b988e39c09329abe3473706dc9ecd6` | `25162ae1ce2a4774affbc92d3d36cb5934b988e39c09329abe3473706dc9ecd6` |
| `xcodebuildmcp-iphone-post-login-home-2026-05-28.jpg` | PASS | 368x800 | 368x800 | `c889225f81c53860cd208f5b0d1738926de9407e2b64f9096273c050b89b7c90` | `c889225f81c53860cd208f5b0d1738926de9407e2b64f9096273c050b89b7c90` |
| `xcodebuildmcp-iphone-home-dark-high-contrast-axxxl-2026-05-28.jpg` | PASS | 368x800 | 368x800 | `3b80c6b3c379d2035ab9e789767838a3efe2cddefb3ac2571eb5682dccd71324` | `3b80c6b3c379d2035ab9e789767838a3efe2cddefb3ac2571eb5682dccd71324` |
| `xcodebuildmcp-iphone-home-dark-high-contrast-axxxl-fixed-2026-05-28.jpg` | PASS | 368x800 | 368x800 | `826464f6f30bc59544f31f65a2606aeceb2e6546615a3fd28ed9d91cd2fb03b3` | `826464f6f30bc59544f31f65a2606aeceb2e6546615a3fd28ed9d91cd2fb03b3` |
| `xcodebuildmcp-iphone-home-high-contrast-axxxl-fixed-no-truncation-2026-05-28.jpg` | PASS | 368x800 | 368x800 | `8ae7ca621e72646ef112016df51bac937284b91aef515dbb3c51d22451b4a9a1` | `8ae7ca621e72646ef112016df51bac937284b91aef515dbb3c51d22451b4a9a1` |
| `xcodebuildmcp-iphone-create-event-2026-05-27.jpg` | PASS | 368x800 | 368x800 | `5e13fafc7ad298126fcd982e6bbfad4115c1bd9b74fa2ebcf0adf09ccb21a99e` | `5e13fafc7ad298126fcd982e6bbfad4115c1bd9b74fa2ebcf0adf09ccb21a99e` |
| `xcodebuildmcp-ipad-login-2026-05-27.jpg` | PASS | 599x800 | 599x800 | `07ec532e8b6a7e0fb9a4bc882f1e9f4792a2211527cfa94aa9a5f518a58e311c` | `07ec532e8b6a7e0fb9a4bc882f1e9f4792a2211527cfa94aa9a5f518a58e311c` |

Integrity checked: 13 indexed screenshots

Integrity failures: 0

## Closure Notes

- Treat MISSING rows as the next screenshot-capture targets.
- Local PASS rows prove only that a matching local simulator screenshot is indexed; they do not prove App Store Connect media readiness.
- Do not set App Store media, accessibility, or TestFlight evidence markers true from this local audit alone.
- Re-run after capturing screenshots from the uploaded TestFlight/App Review build.
