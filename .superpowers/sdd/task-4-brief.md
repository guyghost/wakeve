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

private fun projectFile(path: String): String = java.io.File(System.getProperty("user.dir")).resolve(path).readText()
```

- [ ] **Step 2: Run RED**

Run: `./gradlew :composeApp:testDebugUnitTest --tests '*ProductLanguageCatalogContractTest*' --no-daemon --no-configuration-cache`

Expected: test executes and FAILS `assertTrue` with a concrete key message such as `tab_ideas missing`; resource/test compilation succeeds.

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

