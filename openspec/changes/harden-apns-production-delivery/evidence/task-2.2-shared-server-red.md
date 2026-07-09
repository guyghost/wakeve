# Task 2.2 — shared/server RED evidence

Date: 2026-07-09

Scope: tests only. No production Kotlin or SQLDelight schema was changed. Task 2.2 remains unchecked pending implementation and review.

## Shared delivery contracts

Command:

```bash
./gradlew :shared:jvmTest --tests 'com.guyghost.wakeve.notification.APNsProductionDeliveryRedTest'
```

Result: expected RED, while test compilation and test-class generation succeed (`compileTestKotlinJvm`, `jvmTestClasses`). Four runtime tests execute: three fail for the intended missing behavior and the local/backend ownership-boundary test passes.

Expected failures captured:

- provider errors are currently absorbed as success;
- `sent_at` is currently populated before provider acceptance;
- one `(user_id, platform)` slot still replaces a second iOS installation.

The passing boundary test creates the real local SQLDelight schema in SQLite and proves that it does **not** own `notification_recipient` or `notification_delivery`.

## Server APNs provider contracts

Command:

```bash
./gradlew :server:test \
  --tests 'com.guyghost.wakeve.notification.APNsProviderProductionContractRedTest' \
  --tests 'com.guyghost.wakeve.notification.BackendNotificationPersistenceRedTest'
```

Result: expected RED, while `compileTestKotlin` and `testClasses` succeed. Ten executable tests run: eight fail for intended missing persistence/provider behavior; fail-closed missing credentials and secret-redaction behavior already pass.

Expected failures captured:

- the real provider cannot accept a `.p8` credential or expose ES256 readiness;
- no observable HTTP/2 request exists for validating canonical endpoints and mandatory headers;
- the real result remains unclassified for `200` / `400` / `403` / `410` / `429` / `5xx` / unknown response scenarios because the provider has no HTTP seam yet;
- `Retry-After` and expiry-bounded retry metadata are not exposed;
- the server's real in-memory JDBC schema has no backend recipient/delivery repository yet, so `pendingTarget` fan-out, exact unique delivery identity, durable leases/restart, attempts/retry schedule and expiry all fail through the test-only planned port.

The passing redaction test captures Logback events plus the real returned exception after injecting marker values through token, title, body and data; none appear. It covers formatted/argument output and the exception/HTTP-error surface available today.

These are feature RED failures rather than syntax, fixture, or compilation failures.
