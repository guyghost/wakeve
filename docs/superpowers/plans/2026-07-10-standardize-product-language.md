# Standardize Wakeve Product Language Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Apply one deterministic, locale-complete product-language contract across Wakeve Android, iOS, Siri, notifications, accessibility, AI assistance, and private milestones without changing business state.

**Architecture:** A pure KMP product-language core maps stable domain inputs to semantic keys; platform resource catalogs render those keys. Android, iOS, Siri, notification delivery, and accessibility consume the same concepts, while release scripts enforce parity, literal, terminology, and projection contracts. Work follows Model → Review → Implement → Verify; each implementation task starts with tests delegated to `@tests`, implementation goes to `@codegen`, UI is checked by `@designer`, and each lot ends at an `@review` gate.

**Tech Stack:** Kotlin Multiplatform, Kotlin Test, Jetpack Compose/Android resources, SwiftUI/XCTest, Ktor, native `.lproj` resources, XState model tests, Bash/Ruby audit scripts, Gradle, Xcodebuild.

## Global Constraints

- `/models` and the explicit projection model are the source of truth; no free text or LLM output selects state, permissions, delivery status, retry outcome, or transitions.
- Preserve `EventStatus`, `Scenario`, `Inbox`, domain/API/storage/routes/analytics identifiers; only the UI-only Swift case `.groups` becomes `.ideas`.
- Supported product locales are exactly `en`, `fr`, `de`, `es`, `it`, and `pt`; iOS and Siri gain `de` rather than narrowing Android coverage.
- Canonical French lifecycle titles are exactly `Brouillon`, `Sondage en cours`, `Options à comparer`, `Date confirmée`, `Détails à préparer`, and `Prêt`.
- `Scenario` projects to Option; `Inbox` projects to Notifications; Messages means event-scoped conversations/comments only.
- Notifications expose `À traiter` and `Informations`; retries preserve semantic event and recipient-locale decision.
- AI actions name the benefit and every mutable output is labeled `Proposition à relire` until explicit deterministic acceptance.
- Milestones are private, non-comparative, sober, preparation-specific, and cannot alter permissions or lifecycle state.
- Accessible labels express action + target + relevant state in the active locale.
- No user-visible production literal is allowed outside a narrow reviewed allowlist for diagnostics, tests, or explicit previews.
- User-visible OpenSpec work must continue satisfying `openspec/specs/product-excellence/spec.md`.
- Do not archive `standardize-product-language` until every task below and `openspec validate standardize-product-language --strict` pass.

## File Structure and Boundaries

- Create `models/product-language.machine.ts` and `models/product-language.machine.test.ts`: executable XState v5 state/error/offline/AI projection model; no platform code.
- Create `models/product-language.inventory.json`: versioned exhaustive source/catalog inventory consumed by platform migration tasks and audited for drift.
- Create `shared/.../productlanguage/ProductLanguage.kt`: pure semantic identifiers and `projectEventState(input)`; no resource lookup.
- Create `shared/.../productlanguage/SupportedProductLocales.kt`: shared locale/fallback manifest.
- Create `shared/.../productlanguage/PrivateMilestone.kt`: private milestone eligibility only; never emits state transitions.
- Create `shared/.../productlanguage/NotificationLanguage.kt`: semantic notification request plus deterministic locale selection; no delivery I/O.
- Platform resource catalogs remain the only source of rendered translations.
- `scripts/audit-product-language.sh` owns static catalog/literal/forbidden-term validation; `scripts/test-critical-release-gates.sh` invokes it.
- Existing views keep their responsibilities; this change replaces copy/resource lookup and semantics, not navigation or domain architecture.

---

### Task 1: Executable Projection Model and Review Matrix

**Delegation:** `@tests` writes model tests first; `@codegen` writes the machine; `@review` checks every nominal/error/cancellation/retry/permission/offline/conflict/terminal branch before Task 2.

**Files:**
- Create: `models/product-language.machine.ts`
- Create: `models/product-language.machine.test.ts`
- Create: `models/product-language.review.md`
- Create: `models/product-language.inventory.json`
- Modify: `openspec/changes/standardize-product-language/tasks.md`

**Interfaces:**
- Consumes: `EventStatus = DRAFT | POLLING | COMPARING | CONFIRMED | ORGANIZING | FINALIZED`, deterministic role/facts/action inputs.
- Produces: XState v5 `productLanguageMachine`, pure `projectProductLanguage(input: ProjectionInput): ProjectionOutput`, reviewed inventory and branch evidence.

- [ ] **Step 1: Write the failing model tests**

```ts
import assert from 'node:assert/strict'
import test from 'node:test'
import { createActor } from 'xstate'
import { productLanguageMachine, projectProductLanguage } from './product-language.machine.ts'

const statuses = [
  ['DRAFT', 'event.state.draft'], ['POLLING', 'event.state.polling'],
  ['COMPARING', 'event.state.comparing'], ['CONFIRMED', 'event.state.confirmed'],
  ['ORGANIZING', 'event.state.organizing'], ['FINALIZED', 'event.state.finalized'],
] as const

test('machine projects every domain status without changing its identity', () => {
  for (const [status, titleKey] of statuses) {
    const actor = createActor(productLanguageMachine, { input: { status, role: 'ORGANIZER', pendingFacts: [], allowedAction: status === 'FINALIZED' ? null : 'EDIT' } }).start()
    assert.equal(actor.getSnapshot().value, status === 'FINALIZED' ? 'terminal' : 'ready')
    assert.equal(actor.getSnapshot().context.projection.titleKey, titleKey)
    assert.equal(actor.getSnapshot().context.projection.domainStatus, status)
  }
})

test('offline mutation enters pendingSync and retry recomputes deterministically', () => {
  const input = { status: 'CONFIRMED', role: 'ORGANIZER', pendingFacts: ['LOCAL_MUTATION'], allowedAction: 'RETRY_SYNC' } as const
  const actor = createActor(productLanguageMachine, { input }).start()
  assert.equal(actor.getSnapshot().value, 'pendingSync')
  assert.deepEqual(projectProductLanguage(input), { domainStatus: 'CONFIRMED', titleKey: 'event.state.confirmed', statusKey: 'sync.waiting', primaryActionKey: 'sync.retry', sharedConfirmation: false })
  actor.send({ type: 'SYNC_SUCCEEDED' })
  assert.equal(actor.getSnapshot().value, 'ready')
})
```

- [ ] **Step 2: Run the model test and confirm RED**

Run: `node --experimental-strip-types --test models/product-language.machine.test.ts`

Expected: FAIL with `Cannot find module './product-language.machine.ts'`.

- [ ] **Step 3: Implement the minimal pure model**

```ts
import { assign, setup } from 'xstate'

export type EventStatus = "DRAFT" | "POLLING" | "COMPARING" | "CONFIRMED" | "ORGANIZING" | "FINALIZED";
export type ProjectionInput = { status: EventStatus; role: "ORGANIZER" | "PARTICIPANT"; pendingFacts: ("LOCAL_MUTATION" | "SYNC_CONFLICT")[]; allowedAction: "EDIT" | "RETRY_SYNC" | null };
export type ProjectionOutput = { domainStatus: EventStatus; titleKey: string; statusKey: string | null; primaryActionKey: string | null; sharedConfirmation: boolean };

const titleKeys: Record<EventStatus, string> = {
  DRAFT: "event.state.draft", POLLING: "event.state.polling", COMPARING: "event.state.comparing",
  CONFIRMED: "event.state.confirmed", ORGANIZING: "event.state.organizing", FINALIZED: "event.state.finalized",
};

export function projectProductLanguage(input: ProjectionInput): ProjectionOutput {
  const pendingSync = input.pendingFacts.includes("LOCAL_MUTATION");
  return { domainStatus: input.status, titleKey: titleKeys[input.status], statusKey: pendingSync ? "sync.waiting" : null,
    primaryActionKey: input.allowedAction === "RETRY_SYNC" ? "sync.retry" : input.allowedAction === "EDIT" ? "event.action.continue" : null,
    sharedConfirmation: !pendingSync };
}

export const productLanguageMachine = setup({
  types: { context: {} as { input: ProjectionInput; projection: ProjectionOutput }, input: {} as ProjectionInput, events: {} as { type: 'SYNC_SUCCEEDED' } | { type: 'SYNC_FAILED' } },
  guards: { isTerminal: ({ context }) => context.input.status === 'FINALIZED', hasPendingSync: ({ context }) => context.input.pendingFacts.includes('LOCAL_MUTATION') },
  actions: { markSynced: assign(({ context }) => { const input = { ...context.input, pendingFacts: context.input.pendingFacts.filter(f => f !== 'LOCAL_MUTATION'), allowedAction: 'EDIT' as const }; return { input, projection: projectProductLanguage(input) } }) },
}).createMachine({
  id: 'productLanguage',
  context: ({ input }) => ({ input, projection: projectProductLanguage(input) }),
  initial: 'classify',
  states: {
    classify: { always: [{ guard: 'isTerminal', target: 'terminal' }, { guard: 'hasPendingSync', target: 'pendingSync' }, { target: 'ready' }] },
    ready: {},
    pendingSync: { on: { SYNC_SUCCEEDED: { target: 'ready', actions: 'markSynced' }, SYNC_FAILED: 'syncFailed' } },
    syncFailed: { on: { SYNC_SUCCEEDED: { target: 'ready', actions: 'markSynced' } } },
    terminal: { type: 'final' },
  },
})
```

- [ ] **Step 4: Run tests, complete the review matrix, and confirm GREEN**

Generate the inventory with this deterministic command and review every row:

```bash
rg -l 'Text\(|Button\(|contentDescription|accessibilityLabel|String\(localized:|Notification|Siri|Wakeve AI|Générer|Generate|Inbox|Scenario|Party Animal|Social Butterfly|Chatterbox|Event Master' composeApp/src/androidMain iosApp/src shared/src/commonMain server/src/main \
  | sort | jq -R -s '{version:1,files:(split("\n")|map(select(length>0))|map({path:.,category:(if test("AI|Suggestion|Recommendation";"i") then "ai-entry-point" elif test("Gamification|Badge|Achievement|Profile";"i") then "gamification-or-profile" elif startswith("composeApp/") then "android-ui" elif startswith("iosApp/") then "ios-ui" elif test("Notification|Siri";"i") then "delivery-or-siri" else "shared" end)}))}' > models/product-language.inventory.json
```

Run: `node --experimental-strip-types --test models/product-language.machine.test.ts && openspec validate standardize-product-language --strict`

Expected: Node reports 2 tests PASS; OpenSpec reports the change valid. `jq -e '.version == 1 and (.files | length > 0) and all(.files[]; (.path | length > 0) and (.category | length > 0))' models/product-language.inventory.json` exits 0. Record all ten review cases from `design.md` in `models/product-language.review.md`, then mark OpenSpec tasks 1.1–2.5 complete only when `@review` accepts them.

- [ ] **Step 5: Commit the reviewed model**

```bash
git add models/product-language.machine.ts models/product-language.machine.test.ts models/product-language.review.md models/product-language.inventory.json openspec/changes/standardize-product-language/tasks.md
git commit -m "docs(product-language): model deterministic projections"
```

### Task 2: Shared Concept Registry and State Projection Core

**Delegation:** `@tests` before `@codegen`; `@review` proves internal enum names and transition behavior are unchanged.

**Files:**
- Create: `shared/src/commonMain/kotlin/com/guyghost/wakeve/productlanguage/ProductLanguage.kt`
- Create: `shared/src/commonMain/kotlin/com/guyghost/wakeve/productlanguage/SupportedProductLocales.kt`
- Create: `shared/src/commonTest/kotlin/com/guyghost/wakeve/productlanguage/ProductLanguageTest.kt`
- Test: `shared/src/commonTest/kotlin/com/guyghost/wakeve/models/EventValidationTest.kt`

**Interfaces:**
- Consumes: `EventStatus`, `ProductLanguageInput(status, role, confirmedFacts, pendingFacts, allowedAction)`.
- Produces: `SemanticKey`, `ProductLanguageProjection`, `projectEventState(input): ProductLanguageProjection`, `SUPPORTED_PRODUCT_LOCALES`, `FALLBACK_PRODUCT_LOCALE`.

- [ ] **Step 1: Write the failing Kotlin contract**

```kotlin
class ProductLanguageTest {
    @Test fun canonicalStatesKeepDomainIdentity() {
        val expected = mapOf(EventStatus.DRAFT to "event.state.draft", EventStatus.POLLING to "event.state.polling", EventStatus.COMPARING to "event.state.comparing", EventStatus.CONFIRMED to "event.state.confirmed", EventStatus.ORGANIZING to "event.state.organizing", EventStatus.FINALIZED to "event.state.finalized")
        expected.forEach { (status, key) ->
            val result = projectEventState(ProductLanguageInput(status, UserRole.ORGANIZER, emptySet(), emptySet(), if (status == EventStatus.FINALIZED) null else AllowedAction.CONTINUE))
            assertEquals(status, result.domainStatus)
            assertEquals(key, result.title.value)
            assertEquals(status == EventStatus.FINALIZED, result.primaryAction == null)
        }
        assertEquals(setOf("en", "fr", "de", "es", "it", "pt"), SUPPORTED_PRODUCT_LOCALES)
        assertEquals("en", FALLBACK_PRODUCT_LOCALE)
    }
}
```

- [ ] **Step 2: Run RED**

Run: `./gradlew :shared:jvmTest --tests '*ProductLanguageTest*' --no-daemon`

Expected: compilation FAIL because `ProductLanguageInput` is unresolved.

- [ ] **Step 3: Implement the pure registry**

```kotlin
@JvmInline value class SemanticKey(val value: String)
enum class UserRole { ORGANIZER, PARTICIPANT }
enum class PendingFact { LOCAL_MUTATION, SYNC_CONFLICT }
enum class AllowedAction { CONTINUE, RETRY_SYNC }
data class ProductLanguageInput(val status: EventStatus, val role: UserRole, val confirmedFacts: Set<String>, val pendingFacts: Set<PendingFact>, val allowedAction: AllowedAction?)
data class ProductLanguageProjection(val domainStatus: EventStatus, val title: SemanticKey, val status: SemanticKey?, val primaryAction: SemanticKey?, val sharedConfirmation: Boolean)
val SUPPORTED_PRODUCT_LOCALES = setOf("en", "fr", "de", "es", "it", "pt")
const val FALLBACK_PRODUCT_LOCALE = "en"

fun projectEventState(input: ProductLanguageInput): ProductLanguageProjection {
    val title = SemanticKey("event.state.${input.status.name.lowercase()}")
    val pending = PendingFact.LOCAL_MUTATION in input.pendingFacts
    return ProductLanguageProjection(input.status, title, if (pending) SemanticKey("sync.waiting") else null,
        when (input.allowedAction) { AllowedAction.CONTINUE -> SemanticKey("event.action.continue"); AllowedAction.RETRY_SYNC -> SemanticKey("sync.retry"); null -> null }, !pending)
}
```

- [ ] **Step 4: Run GREEN and unchanged-domain regression**

Run: `./gradlew :shared:jvmTest --tests '*ProductLanguageTest*' --tests '*EventValidationTest*' --no-daemon`

Expected: PASS; `EventStatus.values()` remains the six original identifiers.

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/guyghost/wakeve/productlanguage shared/src/commonTest/kotlin/com/guyghost/wakeve/productlanguage
git commit -m "feat(product-language): add canonical projection core"
```

### Task 3: Locale Parity, Forbidden Terms, and Literal Audit Foundation

**Delegation:** `@tests` authors fixtures/assertions; `@codegen` implements scanner; `@review` approves every allowlist line.

**Files:**
- Create: `scripts/audit-product-language.sh`
- Create: `scripts/lib/audit_product_language.rb`
- Create: `scripts/product-language-allowlist.txt`
- Create: `scripts/tests/audit-product-language-test.sh`
- Modify: `scripts/test-critical-release-gates.sh`

**Interfaces:**
- Consumes: Android `values*/strings.xml`, iOS `*.lproj/Localizable.strings`, Siri locale resources, Kotlin/Swift production sources.
- Produces: executable `scripts/audit-product-language.sh`; exit 0 only with six-locale parity and no unallowlisted visible literal/term.

- [ ] **Step 1: Write a failing black-box scanner test**

```bash
#!/usr/bin/env bash
set -euo pipefail
tmp="$(mktemp -d)"; trap 'rm -rf "$tmp"' EXIT
for locale in en fr de es it pt; do
  dir="values-$locale"; [[ "$locale" == en ]] && dir="values"
  mkdir -p "$tmp/android/$dir"
  printf '<resources><string name="tab_ideas">Ideas</string></resources>' > "$tmp/android/$dir/strings.xml"
done
mkdir -p "$tmp/composeApp/src/androidMain/kotlin/example"

expect_failure() {
  local expected="$1"; shift
  local output
  if output="$("$@" 2>&1)"; then echo "expected failure containing: $expected" >&2; exit 1; fi
  grep -Fq "$expected" <<<"$output"
}

sed -i '' 's/name="tab_ideas"/name="other"/' "$tmp/android/values-fr/strings.xml"
expect_failure 'android fr: missing tab_ideas' scripts/audit-product-language.sh --fixture-root "$tmp" --catalogs-only
cp "$tmp/android/values/strings.xml" "$tmp/android/values-fr/strings.xml"

sed -i '' 's/>Ideas</>Wakeve AI</' "$tmp/android/values-de/strings.xml"
expect_failure 'android/de:tab_ideas: forbidden visible term' scripts/audit-product-language.sh --fixture-root "$tmp" --forbidden-terms-only
cp "$tmp/android/values/strings.xml" "$tmp/android/values-de/strings.xml"

printf 'fun Example() { Text("Inbox") }\n' > "$tmp/composeApp/src/androidMain/kotlin/example/Example.kt"
expect_failure 'composeApp/src/androidMain/kotlin/example/Example.kt:1: production literal' scripts/audit-product-language.sh --fixture-root "$tmp" --forbidden-terms-only
rm "$tmp/composeApp/src/androidMain/kotlin/example/Example.kt"

scripts/audit-product-language.sh --fixture-root "$tmp"
```

- [ ] **Step 2: Run RED**

Run: `bash scripts/tests/audit-product-language-test.sh`

Expected: FAIL because `scripts/audit-product-language.sh` does not exist.

- [ ] **Step 3: Implement the scanner contract**

```bash
#!/usr/bin/env bash
set -euo pipefail
root="."
mode="all"
while (($#)); do
  case "$1" in
    --fixture-root) root="${2:?missing fixture root}"; shift 2 ;;
    --catalogs-only) mode="catalogs"; shift ;;
    --forbidden-terms-only) mode="terms"; shift ;;
    *) echo "unknown argument: $1" >&2; exit 64 ;;
  esac
done
exec ruby scripts/lib/audit_product_language.rb "$root" "$mode"
```

Create `scripts/lib/audit_product_language.rb` with this complete executable implementation; any future scanner rule must first add a failing fixture to `scripts/tests/audit-product-language-test.sh`:

```ruby
#!/usr/bin/env ruby
require 'set'
root, mode = ARGV
abort 'usage: audit_product_language.rb ROOT MODE' unless root && %w[all catalogs terms].include?(mode)
locales = %w[en fr de es it pt]
findings = []
fixture = File.directory?(File.join(root, 'android'))
allowlist_path = File.join(root, 'scripts/product-language-allowlist.txt')
allowlist = File.file?(allowlist_path) ? File.readlines(allowlist_path, chomp: true).reject { |line| line.empty? || line.start_with?('#') }.to_set : Set.new
forbidden = /Wakeve AI|\bSc[eé]nario(?:s)?\b|\bScenario(?:s)?\b|\bInbox\b|\bGenerate\b|\bGénérer\b|Party Animal|Social Butterfly|Chatterbox|Event Master|Maître du vote/i

android_paths = locales.to_h do |locale|
  directory = locale == 'en' ? 'values' : "values-#{locale}"
  base = fixture ? 'android' : 'composeApp/src/androidMain/res'
  [locale, File.join(root, base, directory, 'strings.xml')]
end

ios_paths = fixture ? {} : locales.to_h { |locale| [locale, File.join(root, "iosApp/src/Resources/#{locale}.lproj/Localizable.strings")] }
siri_paths = fixture ? {} : locales.to_h { |locale| [locale, File.join(root, "iosApp/src/Siri/#{locale}.lproj/SiriIntents.strings")] }

def android_entries(path)
  return {} unless File.file?(path)
  File.read(path).scan(/<(?:string|plurals|string-array)\b[^>]*\bname="([^"]+)"[^>]*>(.*?)<\/(?:string|plurals|string-array)>/m).to_h.transform_values { |value| value.gsub(/<[^>]+>/, ' ').gsub(/\s+/, ' ').strip }
end

def apple_entries(path)
  return {} unless File.file?(path)
  File.read(path).scan(/^\s*"((?:\\.|[^"])*)"\s*=\s*"((?:\\.|[^"])*)"\s*;/).to_h
end

catalogs = {
  'android' => android_paths.transform_values { |path| android_entries(path) },
  'ios' => ios_paths.transform_values { |path| apple_entries(path) },
  'siri' => siri_paths.transform_values { |path| apple_entries(path) },
}
paths = { 'android' => android_paths, 'ios' => ios_paths, 'siri' => siri_paths }

if %w[all catalogs].include?(mode)
  catalogs.each do |platform, entries_by_locale|
    next if entries_by_locale.empty?
    base_keys = entries_by_locale.fetch('en').keys.to_set
    locales.each do |locale|
      path = paths.fetch(platform).fetch(locale)
      findings << "#{platform} #{locale}: missing file #{path}" unless File.file?(path)
      keys = entries_by_locale.fetch(locale, {}).keys.to_set
      (base_keys - keys).sort.each { |key| findings << "#{platform} #{locale}: missing #{key}" }
      (keys - base_keys).sort.each { |key| findings << "#{platform} #{locale}: extra #{key}" }
    end
  end
end

if %w[all terms].include?(mode)
  catalogs.each do |platform, entries_by_locale|
    entries_by_locale.each do |locale, entries|
      entries.each do |key, value|
        identity = "#{platform}/#{locale}:#{key}"
        findings << "#{identity}: forbidden visible term" if value.match?(forbidden) && !allowlist.include?(identity)
      end
    end
  end

  source_globs = ['composeApp/src/androidMain/**/*.kt', 'iosApp/src/**/*.swift']
  source_globs.flat_map { |glob| Dir.glob(File.join(root, glob)) }.sort.each do |path|
    next if path.include?('/Preview') || path.include?('/Tests/')
    File.readlines(path, chomp: true).each_with_index do |line, index|
      relative = path.delete_prefix("#{root}/")
      identity = "#{relative}:#{index + 1}"
      findings << "#{identity}: forbidden visible term" if line.match?(forbidden) && !allowlist.include?(identity)
      literal = line.match?(/(?:Text|Button|accessibilityLabel|contentDescription)\s*[=(]\s*"[^"\\]+"/)
      findings << "#{identity}: production literal" if literal && !allowlist.include?(identity)
    end
  end
end

puts findings.uniq.sort
exit findings.empty? ? 0 : 1
```

The allowlist format is one exact `relative/path:line` for source or `platform/locale:key` for a resource value. Do not allow glob entries or term-wide exceptions.

- [ ] **Step 4: Run fixture and repository audits**

Run: `bash scripts/tests/audit-product-language-test.sh && scripts/audit-product-language.sh`

Expected: all three RED fixtures observe their precise finding, the clean fixture exits 0, and the repository audit FAILS with current catalog/value/literal debt.

- [ ] **Step 5: Wire the release gate and commit**

Add `run_gate "product language" scripts/audit-product-language.sh` alongside existing critical gates.

```bash
git add scripts/audit-product-language.sh scripts/lib/audit_product_language.rb scripts/product-language-allowlist.txt scripts/tests/audit-product-language-test.sh scripts/test-critical-release-gates.sh
git commit -m "test(product-language): add release audit foundation"
```

### Task 4: Complete Android Catalogs and Semantic Keys

**Delegation:** `@tests` adds the parity test before translators/resources are changed; `@codegen` reconciles keys; `@review` and localization review approve concepts, plurals, dates, error/retry/offline copy.

**Files:**
- Modify: `composeApp/src/androidMain/res/values/strings.xml`
- Modify: `composeApp/src/androidMain/res/values-en/strings.xml`
- Modify: `composeApp/src/androidMain/res/values-de/strings.xml`
- Modify: `composeApp/src/androidMain/res/values-es/strings.xml`
- Modify: `composeApp/src/androidMain/res/values-it/strings.xml`
- Modify: `composeApp/src/androidMain/res/values-pt/strings.xml`
- Create: `composeApp/src/androidUnitTest/kotlin/com/guyghost/wakeve/localization/ProductLanguageCatalogContractTest.kt`

**Interfaces:**
- Consumes: semantic keys from Task 2.
- Produces: six equal Android catalogs including `tab_ideas`, lifecycle, notifications filters, AI benefits/review label, sync/error/permission, accessibility, and private milestone keys.

- [ ] **Step 1: Write the failing catalog contract**

```kotlin
@Test fun everyAndroidLocaleHasCanonicalKeys() {
    val required = setOf("tab_ideas", "event_state_draft", "event_state_polling", "event_state_comparing", "event_state_confirmed", "event_state_organizing", "event_state_finalized", "notifications_filter_todo", "notifications_filter_information", "ai_prepare_options", "ai_proposal_to_review", "sync_waiting", "sync_retry", "milestone_first_event", "milestone_regular_voting", "milestone_organization_ready")
    val files = listOf("values", "values-en", "values-de", "values-es", "values-it", "values-pt").map { projectFile("composeApp/src/androidMain/res/$it/strings.xml") }
    files.forEach { xml -> required.forEach { key -> assertTrue(xml.contains("name=\"$key\""), "$key missing") } }
}
```

- [ ] **Step 2: Run RED**

Run: `./gradlew :composeApp:testDebugUnitTest --tests '*ProductLanguageCatalogContractTest*' --no-daemon --no-configuration-cache`

Expected: FAIL listing missing keys/locales.

- [ ] **Step 3: Add complete resources**

Add the entire reviewed key set to all six files. Use Android plurals for counts, positional format parameters such as `%1$s`, and locale-neutral semantic names. The French canonical values must match Global Constraints exactly; translations must express the same concept rather than enum spellings.

- [ ] **Step 4: Run GREEN and parity scanner**

Run: `./gradlew :composeApp:testDebugUnitTest --tests '*ProductLanguageCatalogContractTest*' --no-daemon --no-configuration-cache && scripts/audit-product-language.sh --catalogs-only`

Expected: PASS; zero Android missing/extra keys and no fallback leakage.

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/androidMain/res composeApp/src/androidUnitTest/kotlin/com/guyghost/wakeve/localization/ProductLanguageCatalogContractTest.kt
git commit -m "feat(android): complete product language catalogs"
```

### Task 5: Android Navigation, Workflow Copy, Notifications/Messages, and Accessibility

**Delegation:** `@tests` first; `@codegen` migrates bounded screen groups; `@designer` validates Material compact/medium/expanded and font scale; `@review` checks semantics and internal identifiers.

**Files:**
- Modify: `composeApp/src/androidMain/kotlin/com/guyghost/wakeve/navigation/WakeveBottomBar.kt`
- Modify: `composeApp/src/androidMain/kotlin/com/guyghost/wakeve/navigation/WakeveAdaptiveNavigationScaffold.kt`
- Modify: `composeApp/src/androidMain/kotlin/com/guyghost/wakeve/navigation/WakeveNavHost.kt`
- Modify: `composeApp/src/androidMain/kotlin/com/guyghost/wakeve/EventCreationScreen.kt`
- Modify: `composeApp/src/androidMain/kotlin/com/guyghost/wakeve/EventDetailScreen.kt`
- Modify: `composeApp/src/androidMain/kotlin/com/guyghost/wakeve/InboxScreen.kt`
- Modify: `composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/event/EventWorkspaceModels.kt`
- Modify: `composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/screens/SettingsScreen.kt`
- Modify: every Android production path listed in `models/product-language.inventory.json`; update the inventory in Task 1 if review finds an omitted path before editing it.
- Create: `composeApp/src/androidUnitTest/kotlin/com/guyghost/wakeve/productlanguage/AndroidProductLanguageContractTest.kt`

**Interfaces:**
- Consumes: Android resource IDs and Task 2 projections.
- Produces: localized Compose copy/semantics; `Screen.Inbox` remains internal; visible tabs use Idées/Notifications/Messages.

- [ ] **Step 1: Write failing source and projection tests**

```kotlin
@Test fun navigationUsesCanonicalResourcesWithoutRenamingRoutes() {
    val bottomBar = projectFile("composeApp/src/androidMain/kotlin/com/guyghost/wakeve/navigation/WakeveBottomBar.kt")
    assertTrue(bottomBar.contains("R.string.tab_ideas"))
    assertTrue(bottomBar.contains("Screen.Inbox"))
    assertFalse(bottomBar.contains("label = \"Explorer\""))
}

@Test fun ambiguousActionsHaveTargetAndStateKeys() {
    val findings = productionSources().filter { it.contains("contentDescription = \"") }
    assertTrue(findings.isEmpty(), findings.joinToString())
}
```

- [ ] **Step 2: Run RED**

Run: `./gradlew :composeApp:testDebugUnitTest --tests '*AndroidProductLanguageContractTest*' --no-daemon --no-configuration-cache`

Expected: FAIL on hard-coded navigation and semantics.

- [ ] **Step 3: Migrate Compose surfaces in reviewable batches**

Replace literals with `stringResource`/`pluralStringResource`; render lifecycle from `projectEventState`; show actual pending/offline/error/cancellation/permission/terminal outcomes; use semantics such as `stringResource(R.string.a11y_open_event_details, event.title)` and `stringResource(R.string.a11y_mark_accommodation_paid, accommodation.name, paidState)`. Keep `Screen.Inbox`, routes, state-machine intents, repository calls, and analytics untouched. Run the targeted test after each screen group.

- [ ] **Step 4: Verify Android UI and semantics**

Run: `./gradlew :composeApp:testDebugUnitTest --tests '*AndroidProductLanguageContractTest*' --tests '*NotificationsScreenFilterTest*' --no-daemon --no-configuration-cache && ./gradlew :composeApp:assembleDebug --no-daemon`

Expected: PASS and `BUILD SUCCESSFUL`. `@designer` records no lost primary meaning at font scales 1.0 and 2.0 on compact, medium, expanded previews; TalkBack labels name action, target, state.

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/androidMain composeApp/src/androidUnitTest/kotlin/com/guyghost/wakeve/productlanguage
git commit -m "feat(android): apply canonical product language"
```

### Task 6: iOS Catalog Parity and Bilingual Surface Removal

**Delegation:** `@tests` first; `@codegen` adds `de` and migrates strings; localization review then `@review`.

**Files:**
- Modify: `iosApp/src/Resources/{en,fr,es,it,pt}.lproj/Localizable.strings`
- Create: `iosApp/src/Resources/de.lproj/Localizable.strings`
- Modify: `iosApp/src/Views/EventDetailExperienceView.swift`
- Modify: `iosApp/src/Views/App/ContentView.swift`
- Modify: every iOS production path listed in `models/product-language.inventory.json`; update the inventory in Task 1 if review finds an omitted path before editing it.
- Create: `iosApp/WakeveTests/ProductLanguageCatalogContractTests.swift`
- Modify: `iosApp/iosApp.xcodeproj/project.pbxproj` only if the synchronized resource group does not auto-include `de.lproj`.

**Interfaces:**
- Consumes: same semantic concepts as Android.
- Produces: six equal iOS catalogs; locale-aware formatters; no visible Swift literal or missing key.

- [ ] **Step 1: Write failing XCTest contract**

```swift
func testEveryLocaleContainsCanonicalKeys() throws {
    let keys = ["tab.ideas", "event.state.draft", "event.state.polling", "event.state.comparing", "event.state.confirmed", "event.state.organizing", "event.state.finalized", "notifications.filter.todo", "notifications.filter.information", "ai.prepare_options", "ai.proposal_to_review"]
    for locale in ["en", "fr", "de", "es", "it", "pt"] {
        let strings = try readProjectFile("iosApp/src/Resources/\(locale).lproj/Localizable.strings")
        keys.forEach { XCTAssertTrue(strings.contains("\"\($0)\""), "\(locale) missing \($0)") }
    }
    let detail = try readProjectFile("iosApp/src/Views/EventDetailExperienceView.swift")
    XCTAssertFalse(detail.contains("Locale(identifier: \"fr_FR\")"))
}
```

- [ ] **Step 2: Run RED**

Run: `xcodebuild test -project iosApp/iosApp.xcodeproj -scheme WakeveApp -destination 'platform=iOS Simulator,name=iPhone 17 Pro' -only-testing:WakeveTests/ProductLanguageCatalogContractTests CODE_SIGNING_ALLOWED=NO`

Expected: FAIL because `de.lproj`/keys are missing and fixed French locale remains.

- [ ] **Step 3: Complete catalogs and replace literals**

Use `String(localized:)`, localized format keys, `Locale.autoupdatingCurrent`, and localized accessibility labels. Add all keys missing from usages, remove no key until source-usage validation proves it unused, and migrate every reported production literal including Event Detail, options, meals, profile, settings, errors, retries, offline and terminal states.

- [ ] **Step 4: Run GREEN, parity, and build**

Run: `scripts/audit-ios-localization-parity.sh --base en && xcodebuild test -project iosApp/iosApp.xcodeproj -scheme WakeveApp -destination 'platform=iOS Simulator,name=iPhone 17 Pro' -only-testing:WakeveTests/ProductLanguageCatalogContractTests CODE_SIGNING_ALLOWED=NO && xcodebuild build -project iosApp/iosApp.xcodeproj -scheme WakeveApp -destination 'platform=iOS Simulator,name=iPhone 17 Pro' CODE_SIGNING_ALLOWED=NO`

Expected: six locales, zero parity findings, tests PASS, build succeeds.

- [ ] **Step 5: Commit**

```bash
git add iosApp/src/Resources iosApp/src/Views iosApp/WakeveTests/ProductLanguageCatalogContractTests.swift iosApp/iosApp.xcodeproj/project.pbxproj
git commit -m "feat(ios): complete product language localization"
```

### Task 7: Migrate the iOS Ideas Tab Safely

**Delegation:** `@tests` first; `@codegen`; `@designer` checks destination promise; `@review` verifies restoration/deep links and no workflow change.

**Files:**
- Modify: `iosApp/src/Models/WakeveTab.swift`
- Modify: `iosApp/src/Views/App/ContentView.swift`
- Modify: `iosApp/WakeveTests/PremiumNavigationContractTests.swift`

**Interfaces:**
- Consumes: `WakeveTab` UI state and existing tab destination.
- Produces: `WakeveTab.ideas`, raw value `ideas`, title key `tab.ideas`; same content/deep-link destination as former `.groups`.

- [ ] **Step 1: Change tests to the approved contract**

```swift
func testWakeveTabsUseIdeasDestination() throws {
    XCTAssertEqual(WakeveTab.allCases, [.home, .ideas, .messages, .profile])
    XCTAssertEqual(WakeveTab.ideas.rawValue, "ideas")
    XCTAssertEqual(WakeveTab.ideas.title, String(localized: "tab.ideas"))
    let source = try readProjectFile("iosApp/src/Views/App/ContentView.swift")
    XCTAssertTrue(source.contains("tabContent(for: .ideas)"))
    XCTAssertFalse(source.contains("tabContent(for: .groups)"))
}
```

- [ ] **Step 2: Run RED**

Run: `xcodebuild test -project iosApp/iosApp.xcodeproj -scheme WakeveApp -destination 'platform=iOS Simulator,name=iPhone 17 Pro' -only-testing:WakeveTests/PremiumNavigationContractTests CODE_SIGNING_ALLOWED=NO`

Expected: compile FAIL because `.ideas` does not exist.

- [ ] **Step 3: Rename only the UI enum/callsites**

```swift
enum WakeveTab: String, CaseIterable, Identifiable {
    case home, ideas, messages, profile
    var id: String { rawValue }
    var title: String { switch self { case .home: String(localized: "tab.upcoming"); case .ideas: String(localized: "tab.ideas"); case .messages: String(localized: "tab.messages"); case .profile: String(localized: "tab.profile") } }
    var systemImage: String { switch self { case .home: "calendar"; case .ideas: "sparkles"; case .messages: "message"; case .profile: "person.crop.circle" } }
}
```

Update `.groups` callsites to `.ideas`; do not rename group domain types, routes, storage, analytics, or event state.

- [ ] **Step 4: Run GREEN**

Run: `xcodebuild test -project iosApp/iosApp.xcodeproj -scheme WakeveApp -destination 'platform=iOS Simulator,name=iPhone 17 Pro' -only-testing:WakeveTests/PremiumNavigationContractTests CODE_SIGNING_ALLOWED=NO`

Expected: PASS; Ideas opens the former preparation/templates content.

- [ ] **Step 5: Commit**

```bash
git add iosApp/src/Models/WakeveTab.swift iosApp/src/Views/App/ContentView.swift iosApp/WakeveTests/PremiumNavigationContractTests.swift
git commit -m "feat(ios): rename preparation tab to ideas"
```

### Task 8: Native Siri Localization for Six Locales

**Delegation:** `@tests`, then `@codegen`, then native-language and `@review` spoken-naturalness gate.

**Files:**
- Delete: `iosApp/src/Siri/SiriIntents.strings`
- Create: `iosApp/src/Siri/{en,fr,de,es,it,pt}.lproj/SiriIntents.strings`
- Modify: `iosApp/src/Siri/WakeveSiriManager.swift`
- Modify: `iosApp/src/Siri/WakeveIntents.intentdefinition`
- Create: `iosApp/WakeveTests/SiriProductLanguageContractTests.swift`

**Interfaces:**
- Consumes: native locale selected by iOS/App Intents.
- Produces: identical Siri semantic keys in six native resource directories; no suffix keys such as `_en` or `_fr`.

- [ ] **Step 1: Write failing resource contract**

```swift
func testSiriUsesNativePerLocaleResources() throws {
    for locale in ["en", "fr", "de", "es", "it", "pt"] {
        let strings = try readProjectFile("iosApp/src/Siri/\(locale).lproj/SiriIntents.strings")
        XCTAssertTrue(strings.contains("\"siri.create_event.title\""))
        XCTAssertFalse(strings.contains("_\(locale)\""))
    }
    XCTAssertFalse(FileManager.default.fileExists(atPath: projectPath("iosApp/src/Siri/SiriIntents.strings")))
}
```

- [ ] **Step 2: Run RED**

Run: `xcodebuild test -project iosApp/iosApp.xcodeproj -scheme WakeveApp -destination 'platform=iOS Simulator,name=iPhone 17 Pro' -only-testing:WakeveTests/SiriProductLanguageContractTests CODE_SIGNING_ALLOWED=NO`

Expected: FAIL because localized Siri directories do not exist.

- [ ] **Step 3: Create native resources and remove manager literals**

Move phrases/responses into each `.lproj`, retain one semantic key set, reference it from intent definition/manager, and use localized format arguments rather than concatenation. Cover success, validation, cancellation, permission, unavailable, retry, and no-state-change responses.

- [ ] **Step 4: Run GREEN and build**

Run: `xcodebuild test -project iosApp/iosApp.xcodeproj -scheme WakeveApp -destination 'platform=iOS Simulator,name=iPhone 17 Pro' -only-testing:WakeveTests/SiriProductLanguageContractTests CODE_SIGNING_ALLOWED=NO && xcodebuild build -project iosApp/iosApp.xcodeproj -scheme WakeveApp -destination 'platform=iOS Simulator,name=iPhone 17 Pro' CODE_SIGNING_ALLOWED=NO`

Expected: PASS; build resolves all six native Siri localizations.

- [ ] **Step 5: Commit**

```bash
git add iosApp/src/Siri iosApp/WakeveTests/SiriProductLanguageContractTests.swift iosApp/iosApp.xcodeproj/project.pbxproj
git commit -m "feat(siri): add native locale resources"
```

### Task 9: Recipient-Locale Notification Model and Delivery

**Delegation:** `@tests` before `@codegen`; notification/platform `@review` validates retry, queue, duplicate, permission and fallback invariants.

**Files:**
- Create: `shared/src/commonMain/kotlin/com/guyghost/wakeve/productlanguage/NotificationLanguage.kt`
- Create: `shared/src/commonTest/kotlin/com/guyghost/wakeve/productlanguage/NotificationLanguageTest.kt`
- Modify: `shared/src/commonMain/kotlin/com/guyghost/wakeve/models/NotificationModels.kt`
- Modify: `shared/src/commonMain/kotlin/com/guyghost/wakeve/notification/NotificationService.kt`
- Modify: `server/src/main/kotlin/com/guyghost/wakeve/routes/NotificationRoutes.kt`
- Modify: `server/src/main/kotlin/com/guyghost/wakeve/notification/PushNotificationSender.kt`
- Create: `server/src/test/kotlin/com/guyghost/wakeve/notification/RecipientLocaleNotificationTest.kt`
- Modify: `models/notification-delivery.machine.ts`
- Modify: `models/notification-machines.test.ts`

**Interfaces:**
- Consumes: `SemanticNotification(eventId, recipientId, template, arguments, localeTag, deduplicationKey)` and recipient stored locale.
- Produces: `resolveNotificationLocale(localeTag): String`; localized `NotificationMessage`; retries preserve semantic event, locale and deduplication key.

- [ ] **Step 1: Write failing pure and server tests**

```kotlin
@Test fun retryPreservesRecipientLocaleAndSemanticIdentity() {
    val request = SemanticNotification("event-1", "user-1", NotificationTemplate.DATE_CONFIRMED, mapOf("eventTitle" to "Lyon"), "fr-FR", "event-1:confirmed:user-1")
    val retry = request.forRetry()
    assertEquals("fr", resolveNotificationLocale(request.localeTag))
    assertEquals(request, retry)
    assertEquals("en", resolveNotificationLocale("xx-ZZ"))
}
```

- [ ] **Step 2: Run RED**

Run: `./gradlew :shared:jvmTest --tests '*NotificationLanguageTest*' --no-daemon && ./gradlew :server:test --tests '*RecipientLocaleNotificationTest*' --no-daemon`

Expected: compilation FAIL because semantic notification types do not exist.

- [ ] **Step 3: Implement semantic composition before delivery**

```kotlin
enum class NotificationTemplate { DATE_CONFIRMED, POLL_CLOSING, MESSAGE_RECEIVED, SYNC_FAILED }
data class SemanticNotification(val eventId: String, val recipientId: String, val template: NotificationTemplate, val arguments: Map<String, String>, val localeTag: String?, val deduplicationKey: String) { fun forRetry() = this }
fun resolveNotificationLocale(localeTag: String?): String = localeTag?.substringBefore('-')?.takeIf { it in SUPPORTED_PRODUCT_LOCALES } ?: FALLBACK_PRODUCT_LOCALE
```

Resolve title/body from locale template tables at the composition boundary; enqueue semantic payload plus resolved locale; never concatenate ad hoc visible copy. Permission denial must say push was not delivered and direct to settings or Notifications history. Preserve existing deep links, quiet hours and duplicate suppression.

- [ ] **Step 4: Run notification and model suites**

Run: `./gradlew :shared:jvmTest --tests '*NotificationLanguageTest*' --no-daemon && ./gradlew :server:test --tests '*RecipientLocaleNotificationTest*' --no-daemon && node --experimental-strip-types --test models/notification-machines.test.ts`

Expected: PASS for two recipients/two locales, fallback, offline queue, idempotent retry, duplicate suppression, denial and deep-link cases.

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/guyghost/wakeve/productlanguage/NotificationLanguage.kt shared/src/commonTest/kotlin/com/guyghost/wakeve/productlanguage/NotificationLanguageTest.kt shared/src/commonMain/kotlin/com/guyghost/wakeve/models/NotificationModels.kt shared/src/commonMain/kotlin/com/guyghost/wakeve/notification/NotificationService.kt server/src models/notification-delivery.machine.ts models/notification-machines.test.ts
git commit -m "feat(notifications): localize by recipient locale"
```

### Task 10: Notifications/Messages Taxonomy and Deep Links

**Delegation:** `@tests`, `@codegen`, `@designer`, `@review`.

**Files:**
- Modify: `composeApp/src/androidMain/kotlin/com/guyghost/wakeve/InboxScreen.kt`
- Modify: `composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/notification/NotificationsScreen.kt`
- Modify: `composeApp/src/androidUnitTest/kotlin/com/guyghost/wakeve/ui/notification/NotificationsScreenFilterTest.kt`
- Modify: `iosApp/src/Views/Inbox/InboxView.swift`
- Modify: `iosApp/src/Views/Inbox/InboxDetailView.swift`
- Create: `iosApp/WakeveTests/NotificationTaxonomyContractTests.swift`
- Test: `composeApp/src/androidUnitTest/kotlin/com/guyghost/wakeve/deeplink/AndroidNavigationDeepLinkParserTest.kt`

**Interfaces:**
- Consumes: existing `Inbox` internal models/routes and notification-to-message deep links.
- Produces: visible Notifications with TO_DO/INFORMATION filters; visible Messages only at event conversation destination.

- [ ] **Step 1: Write failing taxonomy tests**

```kotlin
@Test fun notificationFiltersRemainDistinctFromMessages() {
    assertEquals(NotificationInboxFilter.TO_DO, parseNotificationInboxFilter("todo"))
    assertEquals(NotificationInboxFilter.INFORMATION, parseNotificationInboxFilter("information"))
    assertEquals("messages", notificationMessageTarget("event-1").section)
}
```

```swift
func testInboxIsInternalOnly() throws {
    let source = try readProjectFile("iosApp/src/Views/Inbox/InboxView.swift")
    XCTAssertTrue(source.contains("notifications.filter.todo"))
    XCTAssertTrue(source.contains("notifications.filter.information"))
    XCTAssertFalse(source.contains("Text(\"Inbox\")"))
}
```

- [ ] **Step 2: Run RED**

Run: `./gradlew :composeApp:testDebugUnitTest --tests '*NotificationsScreenFilterTest*' --no-daemon --no-configuration-cache && xcodebuild test -project iosApp/iosApp.xcodeproj -scheme WakeveApp -destination 'platform=iOS Simulator,name=iPhone 17 Pro' -only-testing:WakeveTests/NotificationTaxonomyContractTests CODE_SIGNING_ALLOWED=NO`

Expected: at least one suite FAIL on old Inbox filters/copy.

- [ ] **Step 3: Implement visible taxonomy without internal rename**

Add TO_DO/INFORMATION filter mapping from semantic notification types, localize titles, and retain `Inbox` class/route names. A message notification source remains Notifications; activation deep-links to the event Messages section without renaming either surface.

- [ ] **Step 4: Run GREEN and deep-link regressions**

Run: `./gradlew :composeApp:testDebugUnitTest --tests '*NotificationsScreenFilterTest*' --tests '*AndroidNavigationDeepLinkParserTest*' --no-daemon --no-configuration-cache && xcodebuild test -project iosApp/iosApp.xcodeproj -scheme WakeveApp -destination 'platform=iOS Simulator,name=iPhone 17 Pro' -only-testing:WakeveTests/NotificationTaxonomyContractTests CODE_SIGNING_ALLOWED=NO`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/androidMain/kotlin/com/guyghost/wakeve/InboxScreen.kt composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/notification composeApp/src/androidUnitTest iosApp/src/Views/Inbox iosApp/WakeveTests/NotificationTaxonomyContractTests.swift
git commit -m "feat(notifications): clarify messages taxonomy"
```

### Task 11: Benefit-Led AI Copy and Deterministic Acceptance

**Delegation:** `@tests`, `@codegen`, `@designer`, `@review` verifies manual path and no LLM-owned transitions.

**Files:**
- Modify: Android/iOS resource catalogs from Tasks 4 and 6
- Modify: paths in `models/product-language.inventory.json` whose reviewed inventory category is `ai-entry-point`; Task 1 must record that category before implementation.
- Create: `shared/src/commonTest/kotlin/com/guyghost/wakeve/productlanguage/AiLanguageBoundaryTest.kt`
- Create: `iosApp/WakeveTests/AiProductLanguageContractTests.swift`

**Interfaces:**
- Consumes: existing deterministic AI validation/apply use cases and `AiInteractionMetadata` contracts.
- Produces: benefit keys `ai.prepare_options`, `ai.summarize_responses`, `ai.propose_message`, `ai.complete_list`, `ai.proposal_to_review`; no new mutation API.

- [ ] **Step 1: Write failing boundary tests**

```kotlin
@Test fun aiCopyCannotRepresentAppliedState() {
    val output = AiCopyProjection.proposal(AiBenefit.PREPARE_OPTIONS)
    assertEquals(SemanticKey("ai.prepare_options"), output.action)
    assertEquals(SemanticKey("ai.proposal_to_review"), output.status)
    assertFalse(output.applied)
}
```

- [ ] **Step 2: Run RED**

Run: `./gradlew :shared:jvmTest --tests '*AiLanguageBoundaryTest*' --no-daemon`

Expected: compilation FAIL because `AiCopyProjection` is absent.

- [ ] **Step 3: Implement copy projection and migrate entry points**

```kotlin
enum class AiBenefit { PREPARE_OPTIONS, SUMMARIZE_RESPONSES, PROPOSE_MESSAGE, COMPLETE_LIST }
data class AiCopyProjection(val action: SemanticKey, val status: SemanticKey, val applied: Boolean = false) {
    companion object { fun proposal(benefit: AiBenefit) = AiCopyProjection(SemanticKey("ai.${benefit.name.lowercase()}"), SemanticKey("ai.proposal_to_review")) }
}
```

Map enum names explicitly if resource spelling differs. On unavailable/invalid/rejected/cancelled/retry, retain manual inputs and existing deterministic apply action; never dispatch a state intent from copy.

- [ ] **Step 4: Run AI architecture and platform copy tests**

Run: `./gradlew :shared:jvmTest --tests '*AiLanguageBoundaryTest*' --tests '*DeterministicAiArchitectureGuardTest*' --no-daemon && xcodebuild test -project iosApp/iosApp.xcodeproj -scheme WakeveApp -destination 'platform=iOS Simulator,name=iPhone 17 Pro' -only-testing:WakeveTests/AiProductLanguageContractTests CODE_SIGNING_ALLOWED=NO`

Expected: PASS; manual path available in every failure case.

- [ ] **Step 5: Commit**

```bash
git add shared/src composeApp/src/androidMain iosApp/src iosApp/WakeveTests/AiProductLanguageContractTests.swift
git commit -m "feat(ai): use benefit-led reviewable language"
```

### Task 12: Replace Social Gamification with Private Milestones

**Delegation:** `@tests`, `@codegen`, `@designer`, `@review` verifies no public comparison and no permission/state dependency.

**Files:**
- Create: `shared/src/commonMain/kotlin/com/guyghost/wakeve/productlanguage/PrivateMilestone.kt`
- Create: `shared/src/commonTest/kotlin/com/guyghost/wakeve/productlanguage/PrivateMilestoneTest.kt`
- Modify: `shared/src/commonMain/kotlin/com/guyghost/wakeve/gamification/UserBadges.kt`
- Modify: `shared/src/commonMain/kotlin/com/guyghost/wakeve/gamification/GamificationModels.kt`
- Modify: `server/src/main/kotlin/com/guyghost/wakeve/routes/GamificationRoutes.kt`
- Modify: paths in `models/product-language.inventory.json` whose reviewed inventory category is `gamification-or-profile`; Task 1 must record that category before implementation.

**Interfaces:**
- Consumes: preparation facts only.
- Produces: `PrivateMilestoneType`, `evaluatePrivateMilestones(facts): Set<PrivateMilestoneType>`; no score/rank/permission/status output.

- [ ] **Step 1: Write failing milestone invariant tests**

```kotlin
@Test fun milestonesArePrivateAndStateNeutral() {
    val result = evaluatePrivateMilestones(PreparationFacts(createdEvents = 1, completedVotes = 5, organizationReady = true))
    assertEquals(setOf(PrivateMilestoneType.FIRST_EVENT, PrivateMilestoneType.REGULAR_VOTING, PrivateMilestoneType.ORGANIZATION_READY), result)
    assertFalse(PrivateMilestone::class.members.any { it.name in setOf("rank", "score", "permission", "eventStatus") })
}
```

- [ ] **Step 2: Run RED**

Run: `./gradlew :shared:jvmTest --tests '*PrivateMilestoneTest*' --no-daemon`

Expected: compilation FAIL because private milestone types are absent.

- [ ] **Step 3: Implement minimal private milestone core and remove public UI**

```kotlin
enum class PrivateMilestoneType { FIRST_EVENT, REGULAR_VOTING, ORGANIZATION_READY }
data class PreparationFacts(val createdEvents: Int, val completedVotes: Int, val organizationReady: Boolean)
data class PrivateMilestone(val type: PrivateMilestoneType, val title: SemanticKey)
fun evaluatePrivateMilestones(facts: PreparationFacts) = buildSet {
    if (facts.createdEvents >= 1) add(PrivateMilestoneType.FIRST_EVENT)
    if (facts.completedVotes >= 5) add(PrivateMilestoneType.REGULAR_VOTING)
    if (facts.organizationReady) add(PrivateMilestoneType.ORGANIZATION_READY)
}
```

Remove leaderboard/rank/competitive score/social-archetype presentation and endpoints from user-facing flows; preserve unrelated notification badge counts. Do not connect milestones to EventStatus or authorization.

- [ ] **Step 4: Run GREEN and forbidden-copy audit**

Run: `./gradlew :shared:jvmTest --tests '*PrivateMilestoneTest*' --no-daemon && ./gradlew :server:test --tests '*Gamification*' --no-daemon && scripts/audit-product-language.sh --forbidden-terms-only`

Expected: PASS; no visible Party Animal, Social Butterfly, Chatterbox, Event Master, leaderboard, rank, or competitive score.

- [ ] **Step 5: Commit**

```bash
git add shared/src server/src/main/kotlin/com/guyghost/wakeve/routes/GamificationRoutes.kt composeApp/src/androidMain iosApp/src
git commit -m "refactor(gamification): keep private preparation milestones"
```

### Task 13: Long Text, Dynamic Type, TalkBack, and VoiceOver Gates

**Delegation:** `@tests` adds automated contracts; `@codegen` fixes layouts/semantics; `@designer` performs visual pass; `@review` accessibility gate.

**Files:**
- Modify: `scripts/audit-ios-accessibility-source.sh`
- Create: `scripts/audit-product-language-layouts.sh`
- Create: `composeApp/src/androidInstrumentedTest/kotlin/com/guyghost/wakeve/productlanguage/ProductLanguageAccessibilityTest.kt`
- Create: `iosApp/WakeveTests/ProductLanguageAccessibilityContractTests.swift`
- Modify: affected Compose/SwiftUI views only where verification finds loss of meaning.

**Interfaces:**
- Consumes: localized UI from Tasks 5–12.
- Produces: release evidence that critical action/target/state remains available at Android font scale 2.0, iOS AX5, pseudo locale, compact and large layouts.

- [ ] **Step 1: Add failing semantics contracts**

```kotlin
@Test fun paymentActionNamesTargetAndState() {
    composeRule.onNodeWithContentDescription("Marquer le logement Chalet comme payé").assertExists().assertHasClickAction()
}
```

```swift
func testAmbiguousControlsUseLocalizedAccessibilityLabels() throws {
    for path in actionableSwiftFiles() {
        let source = try readProjectFile(path)
        XCTAssertFalse(source.contains(".accessibilityLabel(\"More\")"), path)
        XCTAssertFalse(source.contains(".accessibilityLabel(\"Avatar\")"), path)
    }
}
```

- [ ] **Step 2: Run RED**

Run: `./gradlew :composeApp:connectedDebugAndroidTest --no-daemon && xcodebuild test -project iosApp/iosApp.xcodeproj -scheme WakeveApp -destination 'platform=iOS Simulator,name=iPhone 17 Pro' -only-testing:WakeveTests/ProductLanguageAccessibilityContractTests CODE_SIGNING_ALLOWED=NO`

Expected: FAIL on at least one ambiguous/missing semantic label before migration completion.

- [ ] **Step 3: Fix semantics and flexible layouts**

Use localized action+target+state values, multiline text, scalable frames, semantic grouping, and labels independent of icon/visual adjacency. Do not shorten translations by removing consequence or state.

- [ ] **Step 4: Run automated and manual gates**

Run: `scripts/audit-ios-accessibility-source.sh && scripts/audit-product-language-layouts.sh && ./gradlew :composeApp:connectedDebugAndroidTest --no-daemon && xcodebuild test -project iosApp/iosApp.xcodeproj -scheme WakeveApp -destination 'platform=iOS Simulator,name=iPhone 17 Pro' -only-testing:WakeveTests/ProductLanguageAccessibilityContractTests CODE_SIGNING_ALLOWED=NO`

Expected: PASS. `@designer` records screenshots for Android compact/expanded with font 2.0 and iPhone/iPad with AX5/pseudo localization; `@review` confirms primary target, state, consequence and action are preserved.

- [ ] **Step 5: Commit**

```bash
git add scripts/audit-ios-accessibility-source.sh scripts/audit-product-language-layouts.sh composeApp/src iosApp/src iosApp/WakeveTests/ProductLanguageAccessibilityContractTests.swift
git commit -m "test(accessibility): gate contextual localized semantics"
```

### Task 14: Release Gates, Full Verification, and OpenSpec Evidence

**Delegation:** `@tests` owns automated verification; `@designer` signs visual evidence; `@review` performs final spec/code/accessibility review. `@docs` updates evidence and OpenSpec tasks only after proof.

**Files:**
- Modify: `scripts/test-critical-release-gates.sh`
- Modify: `.github/workflows/ci.yml`
- Create: `docs/product-language/README.md`
- Create: `docs/product-language/verification-2026-07-10.md`
- Modify: `openspec/changes/standardize-product-language/tasks.md`

**Interfaces:**
- Consumes: every deliverable and test above.
- Produces: blocking CI gate, reproducible evidence, completed OpenSpec checklist; no archive yet unless deployment/acceptance policy separately authorizes it.

- [ ] **Step 1: Add a failing CI invocation contract**

```bash
rg -q 'audit-product-language.sh' scripts/test-critical-release-gates.sh
rg -q 'scripts/test-critical-release-gates.sh' .github/workflows/ci.yml
```

If the second assertion is RED, add an explicit CI step in `.github/workflows/ci.yml`:

```yaml
- name: Run critical release gates
  env:
    IOS_CONTRACTS_DESTINATION: platform=iOS Simulator,name=iPhone 17 Pro
  run: scripts/test-critical-release-gates.sh
```

- [ ] **Step 2: Run the gate before final cleanup**

Run: `scripts/test-critical-release-gates.sh`

Expected: FAIL if any missing locale, fallback risk, visible forbidden term/literal, projection divergence, absent Siri/notification template, or accessibility issue remains. Fix findings in their owning task; do not add broad allowlist patterns.

- [ ] **Step 3: Document exact registry and verification procedures**

`README.md` must list supported locales, canonical concepts, identifier boundaries, resource ownership, semantic notification/Siri contracts, allowlist review policy, and how to add a key. `verification-2026-07-10.md` must record command, timestamp, exit status, suite counts, simulator/device, and reviewer for every command below.

- [ ] **Step 4: Run the complete verification stack**

```bash
node --experimental-strip-types --test models/product-language.machine.test.ts models/notification-machines.test.ts
./gradlew :shared:allTests :composeApp:testDebugUnitTest :server:test --no-daemon --no-configuration-cache
./gradlew :composeApp:assembleDebug :server:build --no-daemon --no-configuration-cache
scripts/audit-ios-localization-parity.sh --base en
scripts/audit-ios-accessibility-source.sh
scripts/audit-product-language.sh
IOS_CONTRACTS_DESTINATION='platform=iOS Simulator,name=iPhone 17 Pro' scripts/test-critical-release-gates.sh
xcodebuild test -project iosApp/iosApp.xcodeproj -scheme WakeveApp -destination 'platform=iOS Simulator,name=iPhone 17 Pro' CODE_SIGNING_ALLOWED=NO
xcodebuild build -project iosApp/iosApp.xcodeproj -scheme WakeveApp -configuration Release -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO
openspec validate standardize-product-language --strict
git diff --check
```

Expected: every command exits 0; Gradle reports `BUILD SUCCESSFUL`; all XCTest suites pass; six locale parity rows have zero findings; all release gates PASS; OpenSpec strict validation succeeds; `git diff --check` is silent.

- [ ] **Step 5: Perform final invariant and spec coverage review**

Run:

```bash
rg -n 'copy string|localizedString|String\(localized:.*==' shared composeApp iosApp server || true
git diff --name-only | rg '(database|sqldelight|routes|analytics)' || true
rg -n 'Scenario|Inbox|DRAFT|POLLING|COMPARING|CONFIRMED|ORGANIZING|FINALIZED' shared/src/commonMain | head -100
```

Expected: no copy-based state discriminator; no persistence/API/analytics rename; internal identifiers remain present only behind presentation boundaries. `@review` maps all eleven OpenSpec requirements to Tasks 2–14 and approves nominal, validation, cancellation, retry, permission, offline, conflict, terminal, AI-unavailable, long-text and accessibility cases.

- [ ] **Step 6: Mark evidence-backed tasks complete and commit**

```bash
git add scripts .github/workflows docs/product-language openspec/changes/standardize-product-language/tasks.md
git commit -m "chore(product-language): enforce release contracts"
```

## Execution Notes

- Each task is a fresh review boundary. Do not combine commits across tasks.
- Within broad platform migrations (Tasks 5 and 6), repeat RED → minimal screen-group migration → targeted GREEN for navigation, creation/polling, options/organization, meals, profile/settings, and error/offline/terminal groups before the task-level commit.
- If a scanner reports a false positive, `@review` must prove the occurrence is non-user-visible before adding one exact path/line pattern to the allowlist.
- If implementation reveals a behavior not representable by the Task 1 model, stop, update the model and review it before coding that behavior.

## Self-Review Record

- **Spec coverage:** Registry/parity (Tasks 2–4, 6); deterministic projection/offline/terminal (Tasks 1–2, 5–6); identifier separation (Tasks 2, 5, 7, 10, 14); Ideas (Tasks 5, 7); Notifications/Messages (Tasks 9–10); benefit-led AI (Task 11); private milestones (Task 12); recipient locale/Siri (Tasks 8–9); accessible language/error/retry/permission/long text (Tasks 5–6, 9, 13); release gates (Tasks 3, 14).
- **Placeholder scan:** No deferred-work marker or follow-up placeholder remains. Broad migrations are bounded by scanner-generated exact findings and named screen groups, with concrete resource APIs, tests, commands and acceptance results.
- **Type consistency:** XState `productLanguageMachine` consumes `ProjectionInput` and stores `ProjectionOutput`; `SemanticKey`, `ProductLanguageInput`, `ProductLanguageProjection`, `projectEventState`, `SUPPORTED_PRODUCT_LOCALES`, `FALLBACK_PRODUCT_LOCALE`, `SemanticNotification`, `resolveNotificationLocale`, `AiCopyProjection`, and `evaluatePrivateMilestones` retain the same signatures wherever consumed.
- **Command consistency:** TypeScript model tests use the repository's `node --experimental-strip-types --test` runner; targeted shared tests use `:shared:jvmTest --tests`; only the full gate uses untargeted `:shared:allTests`; the iOS contracts destination is passed to the existing `scripts/test-critical-release-gates.sh`.
- **Inventory consistency:** every broad migration consumes the versioned `{path, category}` rows in `models/product-language.inventory.json`; inventory generation and schema validation are explicit in Task 1.
- **Scope:** One proposal and one plan; no independent subsystem or business-state change was introduced.
