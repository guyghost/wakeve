# iOS Localization Parity Audit

Generated: 2026-06-13T13:48:20Z

Status: LOCAL SOURCE AUDIT

This report supports roadmap P1.3 by checking iOS localization key parity. It does not prove App Store Connect localized metadata or signed-build UI behavior.

Base locale: `en`

## Summary

| Locale | Syntax | Keys | Duplicate Keys | Missing vs en | Extra vs en |
| --- | --- | ---: | ---: | ---: | ---: |
| en | OK | 848 | 0 | 0 | 0 |
| es | OK | 848 | 0 | 0 | 0 |
| fr | OK | 848 | 0 | 0 | 0 |
| it | OK | 848 | 0 | 0 | 0 |
| pt | OK | 848 | 0 | 0 | 0 |
| Total findings |  |  |  |  | 0 |

## Findings

No plist syntax, duplicate-key, missing-key, or extra-key findings were found.

## Closure Notes

- Add every release-visible iOS key to all supported `.lproj/Localizable.strings` files.
- Keep App Store metadata localization evidence separate from app source string parity.
