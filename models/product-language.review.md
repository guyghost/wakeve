# Product Language Projection Review

## Scope and inventory review

Task 1 models presentation language as a deterministic projection. It does not modify a domain status, permission, route, persistence contract, analytics identifier, or existing workflow transition. `projectProductLanguage` consumes typed facts and an allowed action; it never consumes free text or generated content.

The generated inventory contains 263 non-empty, sorted paths. Path exhaustiveness and semantic category validation are separate checks: the first retains every regex-matched file, while the second asserts all six expected categories are non-empty, prevents the AI bucket from absorbing `main` paths, and checks exact APNs, FCM, AI, and Android sentinels. The resulting distribution is AI 9, Android UI 53, delivery/Siri 38, gamification/profile 7, iOS UI 71, and shared 85. Categories remain discovery aids, not proof that every string in a file has the category's role. String-level and locale-completeness audits remain later OpenSpec work.

## Projection invariants

- `domainStatus` is returned unchanged for all six `EventStatus` values.
- Stable title keys are selected only from `EventStatus`.
- A `LOCAL_MUTATION` projects `sync.waiting`, prevents shared confirmation, and selects `sync.retry` only when retry is allowed.
- `SYNC_FAILED` changes the projection to `sync.failed`; only an authorized `RETRY_SYNC` returns to pending synchronization. External success and unauthorized retry are ignored while failed.
- `SYNC_CONFLICT` projects the affected event-details key, prevents shared confirmation, and exposes and accepts only the single action authorized by `allowedAction`.
- Machine actions consume existing authority and never manufacture edit, retry, or resolution permission.
- Validation, cancellation, denied/restricted permission, and unavailable/rejected AI outcomes have typed executable branches without changing `domainStatus`.
- `FINALIZED` reaches a terminal state and suppresses editing even if the caller provides it.
- No free text, copy, generated content, or LLM output is accepted as an input or event discriminator.
- Budget item accessibility is a deterministic projection of typed item facts: paid/unpaid state names the item target, and mark-paid/edit/delete actions name their target. Action chips expose one merged semantic description so TalkBack does not repeat the visible action label.

## Required review matrix

| Case | Task 1 evidence | Review result |
|---|---|---|
| Nominal success | All six statuses map to stable title keys while preserving identity. | Accepted for status projection. Result-specific explanations remain later registry work. |
| Validation error | Typed error code, field, and correction enter `validationError`, preserve status, and project field-specific detail and focus action keys. | Accepted by executable test. |
| Cancellation | Typed cancellation enters `cancelled`, projects no success/persistence action, and clears shared confirmation. | Accepted by executable test. |
| Retry | Failure projects `sync.failed`; `SYNC_SUCCEEDED` is ignored there and `RETRY_SYNC` restarts only when retry was authorized by input. | Accepted by positive and negative executable tests. |
| Permission denied/restricted | Both facts enter `permissionBlocked`, name the event-update impact, and expose settings only when allowed. | Accepted by two executable tests. |
| Offline | `LOCAL_MUTATION` enters `pendingSync`, projects `sync.waiting`, and sets `sharedConfirmation` to false. | Accepted. |
| Sync conflict | Conflict identifies affected event details, blocks shared confirmation, and guards visible CTA plus resolve/retry events with the exact allowed action. | Accepted by positive and negative executable tests. |
| Terminal state | `FINALIZED` enters final `terminal` and suppresses a stale caller-provided edit CTA. | Accepted by executable test. |
| AI unavailable/rejected | Both typed outcomes enter `manualFallback`, preserve status, and expose a manual path; no AI event can transition domain state. | Accepted by two executable tests. |
| Long text/accessibility | Output is semantic keys, not rendered copy. Budget item projections additionally require action + target + applicable state, exposed once to TalkBack. | Batch 3 source and catalog contracts verify the Budget projection; runtime font-scale validation remains later work. |

## Review decision

Accepted for the executable projection branches listed above. This does not prove the broader OpenSpec checklist items 1.1–2.5: full string and locale coverage, notification/accessibility models, glossary review, product-excellence review, and cross-discipline approvals are absent. Their checkboxes remain open.
