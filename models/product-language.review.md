# Product Language Projection Review

## Scope and inventory review

Task 1 models presentation language as a deterministic projection. It does not modify a domain status, permission, route, persistence contract, analytics identifier, or existing workflow transition. `projectProductLanguage` consumes typed facts and an allowed action; it never consumes free text or generated content.

The generated inventory contains 263 non-empty, sorted paths. Every row was reviewed as an in-scope migration candidate and retained without manual filtering. The command-defined categories are discovery heuristics only: a category does not assert that every string in a file has that semantic role. String-level, locale-completeness, notification, Siri, and accessibility audits remain implementation work in their corresponding OpenSpec tasks.

## Projection invariants

- `domainStatus` is returned unchanged for all six `EventStatus` values.
- Stable title keys are selected only from `EventStatus`.
- A `LOCAL_MUTATION` projects `sync.waiting`, prevents shared confirmation, and selects `sync.retry` only when retry is allowed.
- `SYNC_SUCCEEDED` removes only `LOCAL_MUTATION`, recomputes the projection, and reaches `ready` without changing the domain status.
- `FINALIZED` reaches a terminal machine state and has no primary action when the caller supplies no allowed action.
- Role and `SYNC_CONFLICT` are typed deterministic inputs but do not yet select a branch; future branches must remain explicit and test-first.

## Required review matrix

| Case | Task 1 evidence | Review result |
|---|---|---|
| Nominal success | All six statuses map to stable title keys while preserving identity. | Accepted for status projection. Result-specific explanations remain later registry work. |
| Validation error | No validation event or free-text discriminator exists in this bounded machine. | No implicit transition; explicit validation projection remains out of scope. |
| Cancellation | No cancellation event exists, so the model cannot emit success copy or imply persistence after cancellation. | Safe by absence; explicit cancellation language remains out of scope. |
| Retry | Pending local input is preserved until `SYNC_SUCCEEDED`; retry uses `sync.retry` and recomputes from facts. | Accepted for the modeled sync retry. |
| Permission denied/restricted | Permission is not an input or event in Task 1. | No implicit branch; permission impact and recovery remain out of scope. |
| Offline | `LOCAL_MUTATION` enters `pendingSync`, projects `sync.waiting`, and sets `sharedConfirmation` to false. | Accepted. |
| Sync conflict | `SYNC_CONFLICT` is typed but intentionally has no transition or copy projection yet. | Not accepted as complete; requires a deterministic conflict branch in a later test-first lot. |
| Terminal state | `FINALIZED` enters the final `terminal` state; the tested caller supplies no allowed action. | Accepted for the modeled terminal projection. |
| AI unavailable/rejected | The machine has no LLM input, generated-text event, or AI-owned transition. | Deterministic boundary accepted; manual-path copy remains out of scope. |
| Long text/accessibility | Output consists of semantic keys, not platform literals. | Architecture accepted; rendering, spoken labels, and long-text behavior remain platform verification work. |

## Review decision

Accepted as the executable Task 1 projection foundation. The model proves nominal status identity, offline pending state, retry recomputation, and terminal classification. It does not constitute completion evidence for the broader OpenSpec Model and Review checklist items 1.1–2.5; those items require full semantic registries, locale coverage, notification/accessibility models, and cross-discipline review. Their checkboxes therefore remain open.
