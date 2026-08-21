# iOS Event Detail Invitation Canvas Model

## Purpose
This model is the source of truth for presentation decisions in the iOS invitation-canvas Event Detail redesign. It projects existing structured Wakeve state into visible content and typed navigation. It owns no Event lifecycle transition.

## Structured Inputs
- `eventStatus`: `DRAFT | POLLING | CONFIRMED | COMPARING | ORGANIZING | FINALIZED`
- `hasRequiredSlots`: Boolean derived from structured slot data
- `primaryDate`: confirmed date or first proposed structured slot, otherwise absent
- `currentUserAccess`: `ORGANIZER | ELIGIBLE_PARTICIPANT | RESTRICTED_PARTICIPANT | NON_PARTICIPANT`
- `currentUserVote`: `REQUIRED | SUBMITTED | NOT_APPLICABLE | UNAVAILABLE`
- `participantData`: `LOADING | AVAILABLE(confirmedCount, pendingCount, identities) | UNAVAILABLE | FAILED`
- `responsibility`: `CURRENT_USER | ORGANIZER | GROUP | NONE | UNAVAILABLE`, supplied by a deterministic use case
- `readinessData`: `LOADING | AVAILABLE(typedItems) | UNAVAILABLE | FAILED`; each item has a typed readiness state and action destination
- `availableActions`: a structured set of `EDIT_DRAFT | SUBMIT_VOTE | VIEW_POLL_RESULTS | COMPARE_OPTIONS | CONTINUE_ORGANIZATION | VIEW_FINAL_DETAILS | SHOW_ACCESS_STATE`
- `relevantSync`: `NONE | PENDING(subject) | SYNCED(subject) | UNAVAILABLE`, where `subject` identifies the displayed event, participant response, or planning item
- `shareCapability`: `HIDDEN | REQUESTING | READY(serverIssuedPayload) | UNAVAILABLE(reason) | FAILED(reason)` from the authorized secure invitation flow
- `heroImageState`: `LOADING | AVAILABLE | MISSING | FAILED`
- `auxiliaryFreshness`: `CURRENT | STALE | UNAVAILABLE`; base Event loading/error/retry remains owned by the parent
- `accessibility`: Reduce Motion, Reduce Transparency, increased contrast, Dynamic Type, VoiceOver, and compact-height environment values

No display string, generated content, event description prose, or LLM result is an input for lifecycle state, permission, responsibility, action, sync, or sharing decisions.

## Typed Outputs
- `canvasMode`: exactly one exclusive mode from the matrix below
- `lifecycleLabelKey` and semantic lifecycle color
- optional `participantSummary`, only when `participantData == AVAILABLE`
- `responsibleActor`, copied from structured `responsibility` and filtered by access
- `primaryAction`: exactly one `CanvasAction`; `SHOW_DETAILS` is the always-safe fallback for a rendered canvas
- `primaryActionPlacement`: `IN_CANVAS | PERSISTENT_SAFE_AREA`, mutually exclusive
- `visibleSecondarySections`, filtered by existing workflow/access rules
- `shareControl`: hidden, requesting, ready, unavailable, or failed; never a constructed URL
- `syncDecoration`: none, pending with a typed subject, or synced with a typed subject
- `heroTreatment`: image, loading treatment, or event-mood fallback

`CanvasAction` is a typed navigation request:
- `EDIT_DRAFT`
- `SUBMIT_VOTE`
- `VIEW_POLL_RESULTS`
- `COMPARE_OPTIONS`
- `CONTINUE_ORGANIZATION`
- `VIEW_FINAL_DETAILS`
- `SHOW_ACCESS_STATE`
- `SHOW_DETAILS`

Canvas actions navigate to the owning flow. They do not perform the business mutation inside Event Detail.

## Total Exclusive Lifecycle/Access Projection
The mapper evaluates rows in table order. Exactly one row must match. Subconditions are explicit and exhaustive; no later visual decoration changes `canvasMode` or `primaryAction`.

| Event status | Access | Additional structured condition | Exclusive canvas mode | Allowed primary action |
|---|---|---|---|---|
| `DRAFT` | `ORGANIZER` | `hasRequiredSlots == false` | `DRAFT_ORGANIZER_BLOCKED` | `EDIT_DRAFT`, falling back to `SHOW_DETAILS` |
| `DRAFT` | `ORGANIZER` | `hasRequiredSlots == true` | `DRAFT_ORGANIZER_READY` | `EDIT_DRAFT`, falling back to `SHOW_DETAILS` |
| `DRAFT` | `ELIGIBLE_PARTICIPANT` | any | `DRAFT_PARTICIPANT_READ_ONLY` | `SHOW_ACCESS_STATE` |
| `DRAFT` | `RESTRICTED_PARTICIPANT | NON_PARTICIPANT` | any | `DRAFT_RESTRICTED` | `SHOW_ACCESS_STATE` |
| `POLLING` | `ORGANIZER` | any vote state | `POLLING_ORGANIZER` | `VIEW_POLL_RESULTS`, falling back to `SHOW_DETAILS` |
| `POLLING` | `ELIGIBLE_PARTICIPANT` | `currentUserVote == REQUIRED` | `POLLING_RESPONSE_DUE` | `SUBMIT_VOTE`, falling back to `SHOW_DETAILS` |
| `POLLING` | `ELIGIBLE_PARTICIPANT` | `currentUserVote == SUBMITTED` | `POLLING_RESPONSE_SUBMITTED` | `VIEW_POLL_RESULTS`, falling back to `SHOW_DETAILS` |
| `POLLING` | `ELIGIBLE_PARTICIPANT` | `currentUserVote == NOT_APPLICABLE | UNAVAILABLE` | `POLLING_PARTICIPANT_NEUTRAL` | `VIEW_POLL_RESULTS`, falling back to `SHOW_DETAILS` |
| `POLLING` | `RESTRICTED_PARTICIPANT | NON_PARTICIPANT` | any | `POLLING_RESTRICTED` | `SHOW_ACCESS_STATE` |
| `CONFIRMED` | `ORGANIZER` | any | `CONFIRMED_ORGANIZER` | `COMPARE_OPTIONS` or `CONTINUE_ORGANIZATION` by typed priority, falling back to `SHOW_DETAILS` |
| `CONFIRMED` | `ELIGIBLE_PARTICIPANT` | any | `CONFIRMED_PARTICIPANT` | permitted `COMPARE_OPTIONS` or `CONTINUE_ORGANIZATION`, falling back to `SHOW_DETAILS` |
| `CONFIRMED` | `RESTRICTED_PARTICIPANT | NON_PARTICIPANT` | any | `CONFIRMED_RESTRICTED` | `SHOW_ACCESS_STATE` |
| `COMPARING` | `ORGANIZER` | any | `COMPARING_ORGANIZER` | `COMPARE_OPTIONS`, falling back to `SHOW_DETAILS` |
| `COMPARING` | `ELIGIBLE_PARTICIPANT` | any | `COMPARING_PARTICIPANT` | permitted `COMPARE_OPTIONS`, falling back to `SHOW_DETAILS` |
| `COMPARING` | `RESTRICTED_PARTICIPANT | NON_PARTICIPANT` | any | `COMPARING_RESTRICTED` | `SHOW_ACCESS_STATE` |
| `ORGANIZING` | `ORGANIZER` | any | `ORGANIZING_ORGANIZER` | `CONTINUE_ORGANIZATION` using the next typed incomplete readiness item, falling back to `SHOW_DETAILS` |
| `ORGANIZING` | `ELIGIBLE_PARTICIPANT` | any | `ORGANIZING_PARTICIPANT` | permitted `CONTINUE_ORGANIZATION`, falling back to `SHOW_DETAILS` |
| `ORGANIZING` | `RESTRICTED_PARTICIPANT | NON_PARTICIPANT` | any | `ORGANIZING_RESTRICTED` | `SHOW_ACCESS_STATE` |
| `FINALIZED` | `ORGANIZER | ELIGIBLE_PARTICIPANT` | any | `FINALIZED_READ_ONLY` | `VIEW_FINAL_DETAILS`, falling back to `SHOW_DETAILS` |
| `FINALIZED` | `RESTRICTED_PARTICIPANT | NON_PARTICIPANT` | any | `FINALIZED_RESTRICTED_READ_ONLY` | `SHOW_ACCESS_STATE` |

If the domain introduces an unknown lifecycle value, the mapper fails closed to a non-mutating unsupported presentation with `SHOW_DETAILS`; it does not reuse a nearby lifecycle label or mutating action.

## Orthogonal Projection Rules

### Responsibility and readiness
- A “you must act” claim is rendered only for `responsibility == CURRENT_USER`.
- Organizer/group responsibility uses distinct localized output and never implies the current user is responsible.
- `responsibility == UNAVAILABLE` omits the actor claim.
- Readiness copy/action is rendered only from `readinessData == AVAILABLE`; loading, unavailable, or failed readiness never fabricates an incomplete item.

### Participant data
- Confirmed and pending counts are rendered only from `participantData == AVAILABLE`.
- Counts and avatar identities come from the same snapshot and must reconcile.
- Loading, unavailable, or failed data uses neutral localized output and makes no confirmed/pending count claim.

### Sync and freshness
- `PENDING(subject)` decorates only the matching displayed subject and takes precedence over a synced/confirmed claim for that subject.
- `UNAVAILABLE` makes no sync claim.
- `auxiliaryFreshness == STALE` may label auxiliary data as last known; it cannot upgrade a value to confirmed.
- Base Event loading, terminal error, retry budget, and retry effects belong to the parent screen model. The canvas is not rendered without a structured Event.

### Secure sharing
- `READY(serverIssuedPayload)` may show an enabled share control only when access policy permits it.
- Activating share emits the typed authorized callback with the opaque server-owned payload.
- `REQUESTING`, `UNAVAILABLE`, and `FAILED` render only their honest structured states.
- `HIDDEN` renders no share control.
- The canvas never invokes `InvitationTokenCodec`, derives a token, creates a redeemable URL, logs a token, or substitutes a local fallback.

### Hero imagery
- `AVAILABLE` uses the event image with a readable scrim and a declared focal-point/crop policy.
- `LOADING`, `MISSING`, or `FAILED` uses the event mood treatment without changing semantic content or action availability.
- The image is decorative when it adds no information beyond the event text; otherwise it receives a localized accessibility description from structured metadata.

### Accessibility placement
- Standard sizes use `IN_CANVAS`.
- Compact-height devices or accessibility Dynamic Type use `PERSISTENT_SAFE_AREA` when the semantic canvas and action cannot fit together.
- The two placements are mutually exclusive; exactly one primary action exists in the accessibility tree.
- VoiceOver order remains: lifecycle state, title/date, organizer when available, participant context when available, actor/next action, primary action, details.

## Events and Effects
| Event | Result |
|---|---|
| Structured input changes | Recompute the total projection |
| Primary action tapped | Emit its typed navigation callback; no Event mutation in the canvas |
| Back tapped | Emit native back navigation only |
| Share tapped | Emit the authorized secure-share callback only when `shareCapability == READY` |
| Menu tapped | Show only typed permission-filtered secondary actions |
| Hero image loads/fails | Change `heroTreatment` only |
| Permission changes | Recompute the total projection and remove newly unauthorized content |
| Sync completes | Remove pending decoration only after repository confirmation |
| Parent retry completes | Recompute from new structured inputs; canvas owns no retry budget or network effect |
| Accessibility environment changes | Recompute layout/styling only; semantic state and action identity stay stable |

## Invariants
1. Exactly one lifecycle/access row matches and exactly one primary action is visually prominent.
2. Event state, permission, responsibility, readiness, action destination, sync, and sharing come only from typed structured inputs.
3. A pending or stale subject is never labeled server-confirmed.
4. Restricted organization data is never rendered for restricted or non-participant access.
5. `FINALIZED` never exposes `EDIT_DRAFT`, `SUBMIT_VOTE`, `COMPARE_OPTIONS`, or `CONTINUE_ORGANIZATION`; its fallback is `SHOW_DETAILS`.
6. Image loading failure never blocks event information or navigation.
7. Secondary modules remain reachable only when existing workflow/access guards allow them.
8. LLM output stays in the existing reviewable section and never affects this model.
9. Accessibility reflow changes placement, never semantic ordering or action identity, and never duplicates the primary action.
10. Display copy is output; no transition, guard, permission, or destination parses free text.
11. The share control never creates or exposes a local/predictable invitation token.
12. Avatar identities and participant counts come from one structured snapshot and reconcile.
