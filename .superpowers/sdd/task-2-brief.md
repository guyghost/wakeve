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

- [ ] **Step 1: Create a neutral compilable product-language API, then write the failing Kotlin contract**

Create the Task 2 production files with the exact types below and neutral behavior before adding the test:

```kotlin
@JvmInline value class SemanticKey(val value: String)
enum class UserRole { ORGANIZER, PARTICIPANT }
enum class PendingFact { LOCAL_MUTATION, SYNC_CONFLICT }
enum class AllowedAction { CONTINUE, RETRY_SYNC }
data class ProductLanguageInput(val status: EventStatus, val role: UserRole, val confirmedFacts: Set<String>, val pendingFacts: Set<PendingFact>, val allowedAction: AllowedAction?)
data class ProductLanguageProjection(val domainStatus: EventStatus, val title: SemanticKey, val status: SemanticKey?, val primaryAction: SemanticKey?, val sharedConfirmation: Boolean)
val SUPPORTED_PRODUCT_LOCALES: Set<String> = emptySet()
const val FALLBACK_PRODUCT_LOCALE = ""
fun projectEventState(input: ProductLanguageInput) = ProductLanguageProjection(input.status, SemanticKey("unmodeled"), null, null, false)
```

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

Expected: test task executes and FAILS `expected <event.state.draft> but was <unmodeled>` (and locale-set assertions); compilation succeeds.

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

