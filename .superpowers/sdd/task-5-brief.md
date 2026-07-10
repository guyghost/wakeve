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
    val findings = productionSources().filter { it.readText().contains("contentDescription = \"") }
    assertTrue(findings.isEmpty(), findings.joinToString())
}

private fun projectFile(path: String): String = java.io.File(System.getProperty("user.dir")).resolve(path).readText()
private fun productionSources(): List<java.io.File> = java.io.File(System.getProperty("user.dir"), "composeApp/src/androidMain").walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
```

- [ ] **Step 2: Run RED**

Run: `./gradlew :composeApp:testDebugUnitTest --tests '*AndroidProductLanguageContractTest*' --no-daemon --no-configuration-cache`

Expected: test executes and FAILS `assertTrue(bottomBar.contains("R.string.tab_ideas"))` or the literal-semantics assertion; Kotlin compilation succeeds.

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

