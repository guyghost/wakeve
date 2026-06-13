# iOS Release Screen Evidence Audit

Generated: 2026-06-13T12:53:27Z

Status: LOCAL EVIDENCE

This report maps local simulator screenshots to the release-flow screens listed in ROADMAP.md P1.2. It does not close App Store/TestFlight screenshot evidence; final closure still requires the uploaded review build or TestFlight build.

## Summary

| Required screen | Result | Matches |
| --- | --- | ---: |
| Onboarding | PASS | 2 |
| Login and guest path | PASS | 7 |
| Create event | PASS | 1 |
| Event detail | MISSING | 0 |
| Organization | MISSING | 0 |

Missing required screens: 2

## Matched Evidence

### Onboarding

```text
- | xcodebuildmcp-iphone-onboarding-events-2026-05-27.jpg | Onboarding events page | iPhone simulator | 368x800 | 51e35ab3f6a712fb10e6beeefad7c47b552b698709830431930520dc576c5f62 |
- | xcodebuildmcp-iphone-onboarding-collaboration-2026-05-27.jpg | Onboarding collaboration page | iPhone simulator | 368x800 | 2112d41ae970736f99f822a98d2cff9e7c959932cfb0db3206873872d96fb7e6 |
```

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

### Create event

```text
- | xcodebuildmcp-iphone-create-event-2026-05-27.jpg | Create event | iPhone simulator | 368x800 | 5e13fafc7ad298126fcd982e6bbfad4115c1bd9b74fa2ebcf0adf09ccb21a99e |
```

### Event detail

No matching local screenshot evidence was found.

### Organization

No matching local screenshot evidence was found.

## Closure Notes

- Treat MISSING rows as the next screenshot-capture targets.
- Do not set App Store media, accessibility, or TestFlight evidence markers true from this local audit alone.
- Re-run after capturing screenshots from the uploaded TestFlight/App Review build.
