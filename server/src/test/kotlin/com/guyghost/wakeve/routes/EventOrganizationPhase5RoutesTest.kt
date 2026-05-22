package com.guyghost.wakeve.routes

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.guyghost.wakeve.JvmDatabaseFactory
import com.guyghost.wakeve.budget.BudgetRepository
import com.guyghost.wakeve.database.DatabaseProvider
import com.guyghost.wakeve.models.BudgetCategory
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.EventType
import com.guyghost.wakeve.module
import com.guyghost.wakeve.repository.DatabaseEventRepository
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import sun.misc.Unsafe
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EventOrganizationPhase5RoutesTest {

    private val jwtSecret = System.getenv("JWT_SECRET") ?: "default-secret-key-change-in-production"
    private val jwtIssuer = System.getenv("JWT_ISSUER") ?: "wakev-api"
    private val jwtAudience = System.getenv("JWT_AUDIENCE") ?: "wakev-client"
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    @BeforeTest
    fun setup() {
        DatabaseProvider.resetDatabase()
    }

    @AfterTest
    fun teardown() {
        DatabaseProvider.resetDatabase()
    }

    @Test
    fun `Phase5 confirmed participant records expense with receipt metadata and pending offline sync`() = testApplication {
        val fixture = createFixture()
        val client = createJsonClient()

        application {
            module(fixture.database, fixture.eventRepository)
        }

        val response = client.post("/api/events/${fixture.eventId}/budget/expenses") {
            header(HttpHeaders.Authorization, "Bearer ${createTestJwt(fixture.confirmedParticipantId)}")
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "amount": 86.50,
                  "category": "ACTIVITIES",
                  "payerId": "${fixture.confirmedParticipantId}",
                  "splitParticipantIds": ["${fixture.confirmedParticipantId}", "${fixture.otherConfirmedParticipantId}"],
                  "receiptMetadata": {
                    "fileName": "climbing-receipt.jpg",
                    "mimeType": "image/jpeg",
                    "sha256": "b8f3f1d893f60a95ad82"
                  },
                  "clientSyncState": "OFFLINE"
                }
                """.trimIndent()
            )
        }

        assertEquals(
            HttpStatusCode.Created,
            response.status,
            "Confirmed participants must be able to record shared expenses in ORGANIZING."
        )

        val body = response.bodyAsText()
        assertTrue(body.contains("receiptMetadata"), "Expense response should preserve receipt metadata.")
        assertTrue(body.contains("PENDING"), "Offline expense creation should queue pending sync.")
    }

    @Test
    fun `Phase5 unconfirmed participant budget mutation is forbidden with audit reference`() = testApplication {
        val fixture = createFixture()
        val client = createJsonClient()

        application {
            module(fixture.database, fixture.eventRepository)
        }

        val response = client.post("/api/events/${fixture.eventId}/budget/items") {
            header(HttpHeaders.Authorization, "Bearer ${createTestJwt(fixture.unconfirmedParticipantId)}")
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "name": "Unauthorized expense",
                  "description": "Should be blocked before mutation",
                  "category": "OTHER",
                  "estimatedCost": 12.0
                }
                """.trimIndent()
            )
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
        assertTrue(
            response.bodyAsText().contains("audit", ignoreCase = true),
            "403 authorization denials for organization routes should include or reference an audit event."
        )
    }

    @Test
    fun `Phase5 non participant cannot read budget details`() = testApplication {
        val fixture = createFixture()
        val client = createJsonClient()

        application {
            module(fixture.database, fixture.eventRepository)
        }

        val response = client.get("/api/events/${fixture.eventId}/budget") {
            header(HttpHeaders.Authorization, "Bearer ${createTestJwt("outside-user")}")
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
        assertTrue(
            response.bodyAsText().contains("audit", ignoreCase = true),
            "Budget detail denials must include an audit reference for non-participants."
        )
    }

    @Test
    fun `Phase5 unconfirmed participant cannot read meeting details`() = testApplication {
        val fixture = createFixture()
        val client = createJsonClient()

        application {
            module(fixture.database, fixture.eventRepository)
        }

        val response = client.get("/api/events/${fixture.eventId}/meetings") {
            header(HttpHeaders.Authorization, "Bearer ${createTestJwt(fixture.unconfirmedParticipantId)}")
        }

        assertEquals(
            HttpStatusCode.Forbidden,
            response.status,
            "Unconfirmed participants must be denied full meeting details for the retained date."
        )
        assertTrue(
            response.bodyAsText().contains("audit", ignoreCase = true),
            "Meeting detail denials should be audited when attempted through the backend."
        )
    }

    @Test
    fun `Phase5 settlement suggestions are returned as persisted local records`() = testApplication {
        val fixture = createFixture()
        val budget = fixture.budgetRepository.getBudgetByEventId(fixture.eventId)!!
        val item = fixture.budgetRepository.createBudgetItem(
            budgetId = budget.id,
            category = BudgetCategory.MEALS,
            name = "Group dinner",
            description = "Shared restaurant bill",
            estimatedCost = 180.0,
            sharedBy = listOf(fixture.confirmedParticipantId, fixture.otherConfirmedParticipantId)
        )
        fixture.budgetRepository.markItemAsPaid(item.id, 180.0, fixture.confirmedParticipantId)

        val client = createJsonClient()

        application {
            module(fixture.database, fixture.eventRepository)
        }

        val response = client.get("/api/events/${fixture.eventId}/budget/settlements") {
            header(HttpHeaders.Authorization, "Bearer ${createTestJwt(fixture.organizerId)}")
        }

        assertEquals(HttpStatusCode.OK, response.status)

        val body = response.bodyAsText()
        assertTrue(body.contains("\"settlements\""))
        assertTrue(
            body.contains("settlementId") && body.contains("PERSISTED"),
            "Settlement suggestions must be persisted local records, not transient from/to/amount tuples only."
        )
    }

    @Test
    fun `Phase5 confirmed participant sees only own settlement obligations`() = testApplication {
        val fixture = createFixture()
        val thirdConfirmedParticipantId = "third-confirmed-user"
        val now = "2026-05-22T10:10:00Z"
        fixture.database.participantQueries.insertParticipant(
            id = "participant-$thirdConfirmedParticipantId",
            eventId = fixture.eventId,
            userId = thirdConfirmedParticipantId,
            role = "PARTICIPANT",
            hasValidatedDate = 1,
            joinedAt = now,
            updatedAt = now
        )
        val budget = fixture.budgetRepository.getBudgetByEventId(fixture.eventId)!!
        val participantExpense = fixture.budgetRepository.createBudgetItem(
            budgetId = budget.id,
            category = BudgetCategory.MEALS,
            name = "Participant visible dinner",
            description = "Includes requester",
            estimatedCost = 90.0,
            sharedBy = listOf(fixture.confirmedParticipantId, fixture.otherConfirmedParticipantId)
        )
        fixture.budgetRepository.markItemAsPaid(participantExpense.id, 90.0, fixture.otherConfirmedParticipantId)
        val unrelatedExpense = fixture.budgetRepository.createBudgetItem(
            budgetId = budget.id,
            category = BudgetCategory.ACTIVITIES,
            name = "Unrelated activity",
            description = "Does not include requester",
            estimatedCost = 60.0,
            sharedBy = listOf(fixture.otherConfirmedParticipantId, thirdConfirmedParticipantId)
        )
        fixture.budgetRepository.markItemAsPaid(unrelatedExpense.id, 60.0, thirdConfirmedParticipantId)

        val client = createJsonClient()

        application {
            module(fixture.database, fixture.eventRepository)
        }

        val response = client.get("/api/events/${fixture.eventId}/budget/settlements") {
            header(HttpHeaders.Authorization, "Bearer ${createTestJwt(fixture.confirmedParticipantId)}")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains(fixture.confirmedParticipantId))
        assertFalse(
            body.contains("\"fromParticipantId\":\"$thirdConfirmedParticipantId\"") ||
                body.contains("\"toParticipantId\":\"$thirdConfirmedParticipantId\""),
            "Confirmed participants should only receive their own settlement obligations."
        )
    }

    @Test
    fun `Phase5 payment readiness is incomplete until tricount synced or explicitly not needed`() = testApplication {
        val fixture = createFixture()
        val client = createJsonClient()

        application {
            module(fixture.database, fixture.eventRepository)
        }

        val response = client.get("/api/events/${fixture.eventId}/payment/readiness") {
            header(HttpHeaders.Authorization, "Bearer ${createTestJwt(fixture.organizerId)}")
        }

        assertEquals(HttpStatusCode.OK, response.status)

        val body = json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals("false", body["complete"]?.jsonPrimitive?.content)
        assertTrue(response.bodyAsText().contains("TRICOUNT_HANDOFF_REQUIRED"))
    }

    @Test
    fun `Phase5 organizer can create active payment pot for organizing event`() = testApplication {
        val fixture = createFixture()
        val client = createJsonClient()

        application {
            module(fixture.database, fixture.eventRepository)
        }

        val response = client.post("/api/events/${fixture.eventId}/payment/pot") {
            header(HttpHeaders.Authorization, "Bearer ${createTestJwt(fixture.organizerId)}")
            contentType(ContentType.Application.Json)
            setBody(paymentPotBody(fixture.eventId))
        }

        assertEquals(
            HttpStatusCode.Created,
            response.status,
            "Organizer must be able to create an event-scoped payment pot while the event is ORGANIZING."
        )

        val body = json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals(fixture.eventId, body["eventId"]?.jsonPrimitive?.content)
        assertEquals("1250.75", body["goalAmount"]?.jsonPrimitive?.content)
        assertEquals("EUR", body["currency"]?.jsonPrimitive?.content)
        assertEquals("TRICOUNT", body["paymentProvider"]?.jsonPrimitive?.content)
        assertEquals("ACTIVE", body["status"]?.jsonPrimitive?.content)
        assertEquals("https://tricount.com/group/phase5-pot", body["tricountLink"]?.jsonPrimitive?.content)
    }

    @Test
    fun `Phase5 organizer and confirmed participant can read payment pot while denied users are audited`() = testApplication {
        val fixture = createFixture()
        val client = createJsonClient()

        application {
            module(fixture.database, fixture.eventRepository)
        }

        val unconfirmedRead = client.get("/api/events/${fixture.eventId}/payment/pot") {
            header(HttpHeaders.Authorization, "Bearer ${createTestJwt(fixture.unconfirmedParticipantId)}")
        }
        val nonParticipantRead = client.get("/api/events/${fixture.eventId}/payment/pot") {
            header(HttpHeaders.Authorization, "Bearer ${createTestJwt("outside-user")}")
        }

        assertEquals(HttpStatusCode.Forbidden, unconfirmedRead.status)
        assertTrue(
            unconfirmedRead.bodyAsText().contains("audit", ignoreCase = true),
            "Unconfirmed participant payment pot denials must include an audit reference."
        )
        assertEquals(HttpStatusCode.Forbidden, nonParticipantRead.status)
        assertTrue(
            nonParticipantRead.bodyAsText().contains("audit", ignoreCase = true),
            "Non-participant payment pot denials must include an audit reference."
        )

        val created = client.post("/api/events/${fixture.eventId}/payment/pot") {
            header(HttpHeaders.Authorization, "Bearer ${createTestJwt(fixture.organizerId)}")
            contentType(ContentType.Application.Json)
            setBody(paymentPotBody(fixture.eventId))
        }
        assertEquals(HttpStatusCode.Created, created.status)

        val organizerRead = client.get("/api/events/${fixture.eventId}/payment/pot") {
            header(HttpHeaders.Authorization, "Bearer ${createTestJwt(fixture.organizerId)}")
        }
        val confirmedRead = client.get("/api/events/${fixture.eventId}/payment/pot") {
            header(HttpHeaders.Authorization, "Bearer ${createTestJwt(fixture.confirmedParticipantId)}")
        }

        assertEquals(HttpStatusCode.OK, organizerRead.status)
        assertEquals(HttpStatusCode.OK, confirmedRead.status)

        val confirmedBody = json.parseToJsonElement(confirmedRead.bodyAsText()).jsonObject
        assertEquals(fixture.eventId, confirmedBody["eventId"]?.jsonPrimitive?.content)
        assertEquals("ACTIVE", confirmedBody["status"]?.jsonPrimitive?.content)
        assertEquals("TRICOUNT", confirmedBody["paymentProvider"]?.jsonPrimitive?.content)
    }

    @Test
    fun `Phase5 confirmed participant can read payment readiness and non participant is denied with audit`() = testApplication {
        val fixture = createFixture()
        val client = createJsonClient()

        application {
            module(fixture.database, fixture.eventRepository)
        }

        val confirmedReadiness = client.get("/api/events/${fixture.eventId}/payment/readiness") {
            header(HttpHeaders.Authorization, "Bearer ${createTestJwt(fixture.confirmedParticipantId)}")
        }
        val nonParticipantReadiness = client.get("/api/events/${fixture.eventId}/payment/readiness") {
            header(HttpHeaders.Authorization, "Bearer ${createTestJwt("outside-user")}")
        }

        assertEquals(
            HttpStatusCode.OK,
            confirmedReadiness.status,
            "Confirmed participants must be able to read payment readiness for the retained event."
        )
        assertTrue(confirmedReadiness.bodyAsText().contains("TRICOUNT_HANDOFF_REQUIRED"))

        assertEquals(HttpStatusCode.Forbidden, nonParticipantReadiness.status)
        assertTrue(
            nonParticipantReadiness.bodyAsText().contains("audit", ignoreCase = true),
            "Payment readiness denials must include an audit reference for non-participants."
        )
    }

    @Test
    fun `Phase5 payment pot closure is organizer only and returns closed local first status`() = testApplication {
        val fixture = createFixture()
        val client = createJsonClient()

        application {
            module(fixture.database, fixture.eventRepository)
        }

        val participantClose = client.post("/api/events/${fixture.eventId}/payment/pot/close") {
            header(HttpHeaders.Authorization, "Bearer ${createTestJwt(fixture.confirmedParticipantId)}")
            contentType(ContentType.Application.Json)
            setBody("""{"eventId":"${fixture.eventId}"}""")
        }
        assertEquals(HttpStatusCode.Forbidden, participantClose.status)
        assertTrue(
            participantClose.bodyAsText().contains("audit", ignoreCase = true),
            "Payment pot closure denials must include an audit reference."
        )

        val created = client.post("/api/events/${fixture.eventId}/payment/pot") {
            header(HttpHeaders.Authorization, "Bearer ${createTestJwt(fixture.organizerId)}")
            contentType(ContentType.Application.Json)
            setBody(paymentPotBody(fixture.eventId))
        }
        assertEquals(HttpStatusCode.Created, created.status)

        val organizerClose = client.post("/api/events/${fixture.eventId}/payment/pot/close") {
            header(HttpHeaders.Authorization, "Bearer ${createTestJwt(fixture.organizerId)}")
            contentType(ContentType.Application.Json)
            setBody("""{"eventId":"${fixture.eventId}"}""")
        }

        assertEquals(HttpStatusCode.OK, organizerClose.status)
        val body = json.parseToJsonElement(organizerClose.bodyAsText()).jsonObject
        assertEquals(fixture.eventId, body["eventId"]?.jsonPrimitive?.content)
        assertEquals("1250.75", body["goalAmount"]?.jsonPrimitive?.content)
        assertEquals("EUR", body["currency"]?.jsonPrimitive?.content)
        assertEquals("TRICOUNT", body["paymentProvider"]?.jsonPrimitive?.content)
        assertEquals("CLOSED", body["status"]?.jsonPrimitive?.content)
        assertEquals("https://tricount.com/group/phase5-pot", body["tricountLink"]?.jsonPrimitive?.content)
    }

    @Test
    fun `Phase5 payment pot closure rejects mismatched event scope`() = testApplication {
        val fixture = createFixture()
        val client = createJsonClient()

        application {
            module(fixture.database, fixture.eventRepository)
        }

        val mismatchedClose = client.post("/api/events/${fixture.eventId}/payment/pot/close") {
            header(HttpHeaders.Authorization, "Bearer ${createTestJwt(fixture.organizerId)}")
            contentType(ContentType.Application.Json)
            setBody("""{"eventId":"other-event"}""")
        }

        assertEquals(
            HttpStatusCode.Forbidden,
            mismatchedClose.status,
            "Payment pot closure must reject payloads that try to close a pot outside the route event scope."
        )
        assertTrue(
            mismatchedClose.bodyAsText().contains("audit", ignoreCase = true),
            "Event-scope payment pot denials must include an audit reference."
        )
    }

    @Test
    fun `Phase5 unconfirmed participant cannot read payment readiness`() = testApplication {
        val fixture = createFixture()
        val client = createJsonClient()

        application {
            module(fixture.database, fixture.eventRepository)
        }

        val response = client.get("/api/events/${fixture.eventId}/payment/readiness") {
            header(HttpHeaders.Authorization, "Bearer ${createTestJwt(fixture.unconfirmedParticipantId)}")
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
        assertTrue(
            response.bodyAsText().contains("audit", ignoreCase = true),
            "Payment readiness denials must include an audit reference."
        )
    }

    @Test
    fun `Phase5 non participant cannot link tricount handoff`() = testApplication {
        val fixture = createFixture()
        val client = createJsonClient()

        application {
            module(fixture.database, fixture.eventRepository)
        }

        val response = client.post("/api/events/${fixture.eventId}/payment/tricount/link") {
            header(HttpHeaders.Authorization, "Bearer ${createTestJwt("outside-user")}")
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "provider": "TRICOUNT",
                  "providerId": "tri_456",
                  "providerUrl": "https://tricount.com/group/tri_456",
                  "syncStatus": "LINKED"
                }
                """.trimIndent()
            )
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
        assertTrue(
            response.bodyAsText().contains("audit", ignoreCase = true),
            "Tricount link authorization denials must be audited."
        )
    }

    @Test
    fun `Phase5 tricount link mutation is rejected unless event is organizing`() = testApplication {
        val fixture = createFixture(status = EventStatus.CONFIRMED)
        val client = createJsonClient()

        application {
            module(fixture.database, fixture.eventRepository)
        }

        val response = client.post("/api/events/${fixture.eventId}/payment/tricount/link") {
            header(HttpHeaders.Authorization, "Bearer ${createTestJwt(fixture.organizerId)}")
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "provider": "TRICOUNT",
                  "providerId": "tri_confirmed_blocked",
                  "providerUrl": "https://tricount.com/group/tri_confirmed_blocked",
                  "syncStatus": "LINKED"
                }
                """.trimIndent()
            )
        }

        assertEquals(
            HttpStatusCode.Conflict,
            response.status,
            "Tricount link mutations must be ORGANIZING-guarded even when the caller is the event organizer."
        )
        assertTrue(response.bodyAsText().contains("ORGANIZING"))
    }

    @Test
    fun `Phase5 tricount link mutation rejects every non organizing workflow status before persistence`() = testApplication {
        val fixture = createFixture()
        val client = createJsonClient()
        val blockedStatuses = listOf(
            EventStatus.DRAFT,
            EventStatus.POLLING,
            EventStatus.CONFIRMED,
            EventStatus.COMPARING,
            EventStatus.FINALIZED
        )

        blockedStatuses.forEach { status ->
            fixture.seedEvent(
                eventId = "event-phase5-${status.name.lowercase()}",
                status = status
            )
        }
        val baselinePendingSyncIds = blockedStatuses.associate { status ->
            val blockedEventId = "event-phase5-${status.name.lowercase()}"
            blockedEventId to fixture.pendingSyncMentions(blockedEventId).map { it.id }.toSet()
        }

        application {
            module(fixture.database, fixture.eventRepository)
        }

        val violations = mutableListOf<String>()

        blockedStatuses.forEach { status ->
            val blockedEventId = "event-phase5-${status.name.lowercase()}"
            val response = client.post("/api/events/$blockedEventId/payment/tricount/link") {
                header(HttpHeaders.Authorization, "Bearer ${createTestJwt(fixture.organizerId)}")
                contentType(ContentType.Application.Json)
                setBody(tricountLinkBody("tri-${status.name.lowercase()}", "https://tricount.com/group/${status.name.lowercase()}"))
            }

            if (response.status != HttpStatusCode.Conflict) {
                violations += "${status.name} returned ${response.status}; expected 409 Conflict"
            }
            if (fixture.database.tricountHandoffQueries.selectByEventId(blockedEventId).executeAsOneOrNull() != null) {
                violations += "${status.name} persisted a Tricount handoff despite workflow denial"
            }
            val newSyncRows = fixture.pendingSyncMentions(blockedEventId).filter { row ->
                row.id !in baselinePendingSyncIds.getValue(blockedEventId)
            }
            if (newSyncRows.isNotEmpty()) {
                violations += "${status.name} queued sync metadata despite workflow denial"
            }
        }

        assertTrue(
            violations.isEmpty(),
            "Tricount link mutations must be ORGANIZING-guarded before persistence or sync:\n${violations.joinToString("\n")}"
        )
    }

    @Test
    fun `Phase8 budget baseline and item mutations are rejected before organizing`() = testApplication {
        val fixture = createFixture(eventId = "event-phase8-budget-confirmed", status = EventStatus.CONFIRMED)
        val client = createJsonClient()
        val organizerToken = createTestJwt(fixture.organizerId)
        val seededItem = seedBudgetItem(fixture)

        application {
            module(fixture.database, fixture.eventRepository)
        }

        assertBudgetMutationRoutesAreWorkflowRejected(
            client = client,
            fixture = fixture,
            token = organizerToken,
            itemId = seededItem.id,
            reason = "Budget baseline and item mutations must wait until the event is ORGANIZING."
        )
    }

    @Test
    fun `Phase8 budget baseline and item mutations are rejected after finalization`() = testApplication {
        val fixture = createFixture(eventId = "event-phase8-budget-finalized", status = EventStatus.FINALIZED)
        val client = createJsonClient()
        val organizerToken = createTestJwt(fixture.organizerId)
        val seededItem = seedBudgetItem(fixture)

        application {
            module(fixture.database, fixture.eventRepository)
        }

        assertBudgetMutationRoutesAreWorkflowRejected(
            client = client,
            fixture = fixture,
            token = organizerToken,
            itemId = seededItem.id,
            reason = "FINALIZED events are read-only for budget baseline and item mutations."
        )
    }

    @Test
    fun `Phase8 confirmed participant cannot mutate budget baseline or items while organizing`() = testApplication {
        val fixture = createFixture(eventId = "event-phase8-budget-participant-organizing")
        val client = createJsonClient()
        val participantToken = createTestJwt(fixture.confirmedParticipantId)
        val seededItem = seedBudgetItem(fixture)
        val baselineBudget = fixture.budgetRepository.getBudgetByEventId(fixture.eventId)!!
        val baselineItems = fixture.budgetRepository.getBudgetItems(baselineBudget.id)

        application {
            module(fixture.database, fixture.eventRepository)
        }

        assertBudgetMutationRoutesAreForbiddenForParticipant(
            client = client,
            fixture = fixture,
            token = participantToken,
            itemId = seededItem.id
        )

        val budgetAfterDeniedMutations = fixture.budgetRepository.getBudgetByEventId(fixture.eventId)!!
        val itemAfterDeniedMutations = fixture.budgetRepository.getBudgetItemById(seededItem.id)
        val itemsAfterDeniedMutations = fixture.budgetRepository.getBudgetItems(baselineBudget.id)
        assertEquals(baselineBudget, budgetAfterDeniedMutations, "Denied participant baseline updates must not persist.")
        assertEquals(seededItem, itemAfterDeniedMutations, "Denied participant item updates/deletes must not persist.")
        assertEquals(
            baselineItems.map { it.id }.toSet(),
            itemsAfterDeniedMutations.map { it.id }.toSet(),
            "Denied participant item creation must not persist."
        )

        val readResponse = client.get("/api/events/${fixture.eventId}/budget") {
            header(HttpHeaders.Authorization, "Bearer $participantToken")
        }
        assertEquals(HttpStatusCode.OK, readResponse.status)

        val expenseResponse = client.post("/api/events/${fixture.eventId}/budget/expenses") {
            header(HttpHeaders.Authorization, "Bearer $participantToken")
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "amount": 42.0,
                  "category": "MEALS",
                  "payerId": "${fixture.confirmedParticipantId}",
                  "splitParticipantIds": ["${fixture.confirmedParticipantId}", "${fixture.otherConfirmedParticipantId}"],
                  "receiptMetadata": {
                    "fileName": "organizing-meal.pdf",
                    "mimeType": "application/pdf",
                    "sha256": "phase88confirmedparticipant"
                  },
                  "clientSyncState": "ONLINE"
                }
                """.trimIndent()
            )
        }
        assertEquals(
            HttpStatusCode.Created,
            expenseResponse.status,
            "Confirmed participants should keep permitted expense creation while baseline/items stay organizer-only."
        )
    }

    @Test
    fun `Phase5 suspicious tricount payment link is rejected or never trusted`() = testApplication {
        val fixture = createFixture()
        val client = createJsonClient()

        application {
            module(fixture.database, fixture.eventRepository)
        }

        val response = client.post("/api/events/${fixture.eventId}/payment/tricount/link") {
            header(HttpHeaders.Authorization, "Bearer ${createTestJwt(fixture.organizerId)}")
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "provider": "TRICOUNT",
                  "providerId": "tri_123",
                  "providerUrl": "https://tricount.example.evil/collect-login",
                  "syncStatus": "LINKED"
                }
                """.trimIndent()
            )
        }

        assertTrue(
            response.status == HttpStatusCode.BadRequest || response.status == HttpStatusCode.UnprocessableEntity,
            "Suspicious Tricount URLs should be rejected before they can be presented as trusted actions."
        )
    }

    @Test
    fun `Phase5 payment pot and tricount mutations are organizer only before creating local records`() = testApplication {
        val fixture = createFixture()
        val client = createJsonClient()
        val baselinePendingSyncIds = fixture.pendingSyncMentions(fixture.eventId).map { it.id }.toSet()

        application {
            module(fixture.database, fixture.eventRepository)
        }

        val unconfirmedPotCreate = client.post("/api/events/${fixture.eventId}/payment/pot") {
            header(HttpHeaders.Authorization, "Bearer ${createTestJwt(fixture.unconfirmedParticipantId)}")
            contentType(ContentType.Application.Json)
            setBody(paymentPotBody(fixture.eventId))
        }
        val nonParticipantPotCreate = client.post("/api/events/${fixture.eventId}/payment/pot") {
            header(HttpHeaders.Authorization, "Bearer ${createTestJwt("outside-user")}")
            contentType(ContentType.Application.Json)
            setBody(paymentPotBody(fixture.eventId))
        }
        val confirmedTricountLink = client.post("/api/events/${fixture.eventId}/payment/tricount/link") {
            header(HttpHeaders.Authorization, "Bearer ${createTestJwt(fixture.confirmedParticipantId)}")
            contentType(ContentType.Application.Json)
            setBody(tricountLinkBody("tri-confirmed", "https://tricount.com/group/tri-confirmed"))
        }
        val unconfirmedTricountLink = client.post("/api/events/${fixture.eventId}/payment/tricount/link") {
            header(HttpHeaders.Authorization, "Bearer ${createTestJwt(fixture.unconfirmedParticipantId)}")
            contentType(ContentType.Application.Json)
            setBody(tricountLinkBody("tri-unconfirmed", "https://tricount.com/group/tri-unconfirmed"))
        }

        assertEquals(HttpStatusCode.Forbidden, unconfirmedPotCreate.status)
        assertEquals(HttpStatusCode.Forbidden, nonParticipantPotCreate.status)
        assertEquals(HttpStatusCode.Forbidden, confirmedTricountLink.status)
        assertEquals(HttpStatusCode.Forbidden, unconfirmedTricountLink.status)
        assertNull(
            fixture.database.potQueries.selectActiveByEvent(fixture.eventId).executeAsOneOrNull(),
            "Denied payment pot mutations must not create a local pot."
        )
        assertNull(
            fixture.database.tricountHandoffQueries.selectByEventId(fixture.eventId).executeAsOneOrNull(),
            "Denied Tricount mutations must not create a handoff."
        )
        assertTrue(
            fixture.pendingSyncMentions(fixture.eventId).none { row -> row.id !in baselinePendingSyncIds },
            "Denied payment/Tricount mutations must not queue sync metadata."
        )
    }

    @Test
    fun `Phase5 payment pot creation rejects literal template tricount links before exposure`() = testApplication {
        val fixture = createFixture()
        val client = createJsonClient()
        val templateLink = "https://tricount.com/group/${'$'}{phase5PotId}"

        application {
            module(fixture.database, fixture.eventRepository)
        }

        val response = client.post("/api/events/${fixture.eventId}/payment/pot") {
            header(HttpHeaders.Authorization, "Bearer ${createTestJwt(fixture.organizerId)}")
            contentType(ContentType.Application.Json)
            setBody(paymentPotBody(fixture.eventId, templateLink))
        }

        assertTrue(
            response.status == HttpStatusCode.BadRequest || response.status == HttpStatusCode.UnprocessableEntity,
            "Payment pot links containing literal interpolation placeholders must be rejected."
        )
        assertFalse(
            containsLiteralTemplateMarker(response.bodyAsText()),
            "Rejected payment pot responses must never expose literal interpolation placeholders."
        )
        assertNull(
            fixture.database.potQueries.selectActiveByEvent(fixture.eventId).executeAsOneOrNull(),
            "Payment pots with placeholder links must not be persisted."
        )
    }

    @Test
    fun `Phase5 tricount link rejects literal template provider urls before exposure`() = testApplication {
        val fixture = createFixture()
        val client = createJsonClient()
        val templateLink = "https://tricount.com/group/${'$'}{providerId}"

        application {
            module(fixture.database, fixture.eventRepository)
        }

        val response = client.post("/api/events/${fixture.eventId}/payment/tricount/link") {
            header(HttpHeaders.Authorization, "Bearer ${createTestJwt(fixture.organizerId)}")
            contentType(ContentType.Application.Json)
            setBody(tricountLinkBody("tri-template", templateLink))
        }

        assertTrue(
            response.status == HttpStatusCode.BadRequest || response.status == HttpStatusCode.UnprocessableEntity,
            "Tricount provider URLs containing literal interpolation placeholders must be rejected."
        )
        assertFalse(
            containsLiteralTemplateMarker(response.bodyAsText()),
            "Rejected Tricount responses must never expose literal interpolation placeholders."
        )
        assertNull(
            fixture.database.tricountHandoffQueries.selectByEventId(fixture.eventId).executeAsOneOrNull(),
            "Tricount handoffs with placeholder links must not be persisted."
        )
    }

    @Test
    fun `Phase5 meeting proxy create never exposes literal template links`() = testApplication {
        val fixture = createFixture()
        val client = createJsonClient()

        application {
            module(fixture.database, fixture.eventRepository)
        }

        val response = client.post("/api/meetings/proxy/zoom/create") {
            header(HttpHeaders.Authorization, "Bearer ${createTestJwt(fixture.organizerId)}")
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "eventId": "${fixture.eventId}",
                  "title": "Planning meeting",
                  "scheduledFor": "2026-07-18T08:00:00Z",
                  "duration": 60,
                  "timezone": "Europe/Paris"
                }
                """.trimIndent()
            )
        }

        val body = response.bodyAsText()
        assertFalse(
            containsLiteralTemplateMarker(body),
            "Meeting proxy APIs must expose real safe links or a provider-configuration error, never literal placeholders."
        )
        if (response.status == HttpStatusCode.OK) {
            val responseBody = json.parseToJsonElement(body).jsonObject
            assertTrue(isSafeHttpsLink(responseBody["joinUrl"]?.jsonPrimitive?.content.orEmpty()))
            assertTrue(isSafeHttpsLink(responseBody["hostUrl"]?.jsonPrimitive?.content.orEmpty()))
        } else {
            assertEquals(
                HttpStatusCode.ServiceUnavailable,
                response.status,
                "Without provider credentials the route should fail closed instead of returning placeholder meeting links."
            )
        }
    }

    @Test
    fun `Phase5 meeting proxy creation requires event scoped membership authorization`() = testApplication {
        val fixture = createFixture()
        val client = createJsonClient()

        application {
            module(fixture.database, fixture.eventRepository)
        }

        val response = client.post("/api/meetings/proxy/zoom/create") {
            header(HttpHeaders.Authorization, "Bearer ${createTestJwt("non-member")}")
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "eventId": "${fixture.eventId}",
                  "title": "Unauthorized meeting",
                  "scheduledFor": "2026-07-18T08:00:00Z",
                  "duration": 60,
                  "timezone": "Europe/Paris"
                }
                """.trimIndent()
            )
        }

        assertEquals(
            HttpStatusCode.Forbidden,
            response.status,
            "Meeting link creation must be event-scoped and deny non-members before external provider handling."
        )
    }

    @Test
    fun `Phase5 zoom meeting proxy creation rejects confirmed non organizer before provider creation`() = testApplication {
        val fixture = createFixture()
        val client = createJsonClient()

        application {
            module(fixture.database, fixture.eventRepository)
        }

        val response = withProviderEnvironment("ZOOM_API_KEY" to "test-zoom-key") {
            client.post("/api/meetings/proxy/zoom/create") {
                header(HttpHeaders.Authorization, "Bearer ${createTestJwt(fixture.confirmedParticipantId)}")
                contentType(ContentType.Application.Json)
                setBody(
                    """
                    {
                      "eventId": "${fixture.eventId}",
                      "title": "Participant-created Zoom meeting",
                      "scheduledFor": "2026-07-18T08:00:00Z",
                      "duration": 60,
                      "timezone": "Europe/Paris"
                    }
                    """.trimIndent()
                )
            }
        }

        assertTrue(
            response.status == HttpStatusCode.Forbidden || response.status == HttpStatusCode.Conflict,
            "Zoom proxy creation must be organizer-only; confirmed participants must be denied before provider creation, got ${response.status} with ${response.bodyAsText()}."
        )
        assertFalse(
            response.bodyAsText().contains("joinUrl") || response.bodyAsText().contains("hostUrl"),
            "Denied Zoom proxy creation must not expose provider-created meeting URLs."
        )
    }

    @Test
    fun `Phase5 google meet proxy creation requires event scope before provider creation`() = testApplication {
        val fixture = createFixture()
        val client = createJsonClient()

        application {
            module(fixture.database, fixture.eventRepository)
        }

        val response = withProviderEnvironment("GOOGLE_MEET_CREDENTIALS" to """{"client_id":"test"}""") {
            client.post("/api/meetings/proxy/google-meet/create") {
                header(HttpHeaders.Authorization, "Bearer ${createTestJwt(fixture.organizerId)}")
                contentType(ContentType.Application.Json)
                setBody(
                    """
                    {
                      "title": "Unscoped Google Meet",
                      "scheduledFor": "2026-07-18T08:00:00Z",
                      "duration": 60,
                      "timezone": "Europe/Paris"
                    }
                    """.trimIndent()
                )
            }
        }

        assertEquals(
            HttpStatusCode.BadRequest,
            response.status,
            "Google Meet proxy creation must require an eventId and reject unscoped requests before provider creation."
        )
        assertFalse(
            response.bodyAsText().contains("meetingUrl") || response.bodyAsText().contains("meetingCode"),
            "Rejected Google Meet proxy creation must not expose provider-created meeting details."
        )
    }

    @Test
    fun `Phase5 google meet proxy creation rejects confirmed non organizer before provider creation`() = testApplication {
        val fixture = createFixture()
        val client = createJsonClient()

        application {
            module(fixture.database, fixture.eventRepository)
        }

        val response = withProviderEnvironment("GOOGLE_MEET_CREDENTIALS" to """{"client_id":"test"}""") {
            client.post("/api/meetings/proxy/google-meet/create") {
                header(HttpHeaders.Authorization, "Bearer ${createTestJwt(fixture.confirmedParticipantId)}")
                contentType(ContentType.Application.Json)
                setBody(
                    """
                    {
                      "eventId": "${fixture.eventId}",
                      "title": "Participant-created Google Meet",
                      "scheduledFor": "2026-07-18T08:00:00Z",
                      "duration": 60,
                      "timezone": "Europe/Paris"
                    }
                    """.trimIndent()
                )
            }
        }

        assertTrue(
            response.status == HttpStatusCode.Forbidden || response.status == HttpStatusCode.Conflict,
            "Google Meet proxy creation must be event-scoped and organizer-only; confirmed participants must be denied before provider creation, got ${response.status} with ${response.bodyAsText()}."
        )
        assertFalse(
            response.bodyAsText().contains("meetingUrl") || response.bodyAsText().contains("meetingCode"),
            "Denied Google Meet proxy creation must not expose provider-created meeting details."
        )
    }

    @Test
    fun `Phase5 zoom proxy response uses one meeting id across response fields`() = testApplication {
        val fixture = createFixture()
        val client = createJsonClient()

        application {
            module(fixture.database, fixture.eventRepository)
        }

        val response = withProviderEnvironment("ZOOM_API_KEY" to "test-zoom-key") {
            client.post("/api/meetings/proxy/zoom/create") {
                header(HttpHeaders.Authorization, "Bearer ${createTestJwt(fixture.organizerId)}")
                contentType(ContentType.Application.Json)
                setBody(
                    """
                    {
                      "eventId": "${fixture.eventId}",
                      "title": "Consistent Zoom meeting",
                      "scheduledFor": "2026-07-18T08:00:00Z",
                      "duration": 60,
                      "timezone": "Europe/Paris"
                    }
                    """.trimIndent()
                )
            }
        }

        assertEquals(
            HttpStatusCode.OK,
            response.status,
            "Configured Zoom proxy fixture should reach provider creation so the response contract can be validated."
        )

        val body = json.parseToJsonElement(response.bodyAsText()).jsonObject
        val meetingId = body["meetingId"]?.jsonPrimitive?.content.orEmpty()
        val joinUrl = body["joinUrl"]?.jsonPrimitive?.content.orEmpty()
        val hostUrl = body["hostUrl"]?.jsonPrimitive?.content.orEmpty()

        assertEquals(meetingId, zoomMeetingIdFromUrl(joinUrl), "joinUrl must reference the response meetingId.")
        assertEquals(meetingId, zoomMeetingIdFromUrl(hostUrl), "hostUrl must reference the response meetingId.")
    }

    private fun ApplicationTestBuilder.createJsonClient() = createClient {
        install(ContentNegotiation) {
            json(json)
        }
    }

    private fun createFixture(
        eventId: String = "event-phase5",
        status: EventStatus = EventStatus.ORGANIZING
    ): Phase5Fixture {
        val database = DatabaseProvider.getDatabase(JvmDatabaseFactory(":memory:"))
        val eventRepository = DatabaseEventRepository(database)
        val budgetRepository = BudgetRepository(database)
        val organizerId = "organizer-user"
        val confirmedParticipantId = "confirmed-user"
        val otherConfirmedParticipantId = "other-confirmed-user"
        val unconfirmedParticipantId = "pending-user"
        val now = "2026-05-22T10:00:00Z"

        runBlocking {
            eventRepository.createEvent(
                Event(
                    id = eventId,
                    title = "Phase 5 Organization",
                    description = "Meetings, budget, payment and tricount",
                    organizerId = organizerId,
                    participants = emptyList(),
                    proposedSlots = emptyList(),
                    deadline = "2026-06-01T00:00:00Z",
                    status = status,
                    finalDate = status.retainedDate(),
                    createdAt = now,
                    updatedAt = now,
                    eventType = EventType.OTHER
                )
            ).getOrThrow()
        }

        database.participantQueries.insertParticipant(
            id = "participant-$confirmedParticipantId",
            eventId = eventId,
            userId = confirmedParticipantId,
            role = "PARTICIPANT",
            hasValidatedDate = 1,
            joinedAt = now,
            updatedAt = now
        )
        database.participantQueries.insertParticipant(
            id = "participant-$otherConfirmedParticipantId",
            eventId = eventId,
            userId = otherConfirmedParticipantId,
            role = "PARTICIPANT",
            hasValidatedDate = 1,
            joinedAt = now,
            updatedAt = now
        )
        database.participantQueries.insertParticipant(
            id = "participant-$unconfirmedParticipantId",
            eventId = eventId,
            userId = unconfirmedParticipantId,
            role = "PARTICIPANT",
            hasValidatedDate = 0,
            joinedAt = now,
            updatedAt = now
        )

        budgetRepository.createBudget(eventId)

        return Phase5Fixture(
            database = database,
            eventRepository = eventRepository,
            budgetRepository = budgetRepository,
            eventId = eventId,
            organizerId = organizerId,
            confirmedParticipantId = confirmedParticipantId,
            otherConfirmedParticipantId = otherConfirmedParticipantId,
            unconfirmedParticipantId = unconfirmedParticipantId
        )
    }

    private fun Phase5Fixture.seedEvent(
        eventId: String,
        status: EventStatus
    ) {
        val now = "2026-05-22T10:00:00Z"
        runBlocking {
            eventRepository.createEvent(
                Event(
                    id = eventId,
                    title = "Phase 5 ${status.name}",
                    description = "Blocked Tricount workflow status",
                    organizerId = organizerId,
                    participants = emptyList(),
                    proposedSlots = emptyList(),
                    deadline = "2026-06-01T00:00:00Z",
                    status = status,
                    finalDate = status.retainedDate(),
                    createdAt = now,
                    updatedAt = now,
                    eventType = EventType.OTHER
                )
            ).getOrThrow()
        }

        database.participantQueries.insertParticipant(
            id = "participant-$eventId-$confirmedParticipantId",
            eventId = eventId,
            userId = confirmedParticipantId,
            role = "PARTICIPANT",
            hasValidatedDate = 1,
            joinedAt = now,
            updatedAt = now
        )
        database.participantQueries.insertParticipant(
            id = "participant-$eventId-$unconfirmedParticipantId",
            eventId = eventId,
            userId = unconfirmedParticipantId,
            role = "PARTICIPANT",
            hasValidatedDate = 0,
            joinedAt = now,
            updatedAt = now
        )
        budgetRepository.createBudget(eventId)
    }

    private fun EventStatus.retainedDate(): String? =
        when (this) {
            EventStatus.CONFIRMED,
            EventStatus.COMPARING,
            EventStatus.ORGANIZING,
            EventStatus.FINALIZED -> "2026-07-18T08:00:00Z"
            EventStatus.DRAFT,
            EventStatus.POLLING -> null
        }

    private fun Phase5Fixture.pendingSyncMentions(eventId: String) =
        database.syncMetadataQueries.selectPending().executeAsList().filter { row ->
            row.entityId == eventId || row.payload.contains(eventId)
        }

    private suspend fun assertBudgetMutationRoutesAreWorkflowRejected(
        client: io.ktor.client.HttpClient,
        fixture: Phase5Fixture,
        token: String,
        itemId: String,
        reason: String
    ) {
        val responses = listOf(
            "PUT budget baseline" to client.put("/api/events/${fixture.eventId}/budget") {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(budgetBaselineBody(fixture))
            },
            "POST budget item" to client.post("/api/events/${fixture.eventId}/budget/items") {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(createBudgetItemBody())
            },
            "PUT budget item" to client.put("/api/events/${fixture.eventId}/budget/items/$itemId") {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(updateBudgetItemBody(itemId, fixture.budgetRepository.getBudgetByEventId(fixture.eventId)!!.id))
            },
            "DELETE budget item" to client.delete("/api/events/${fixture.eventId}/budget/items/$itemId") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
        )

        val accepted = responses.filter { (_, response) -> response.status != HttpStatusCode.Conflict }
        assertTrue(
            accepted.isEmpty(),
            "$reason\nExpected HTTP 409 Conflict from every mutation route, got:\n" +
            responses.joinToString("\n") { (name, response) -> "$name -> ${response.status}" }
        )
    }

    private suspend fun assertBudgetMutationRoutesAreForbiddenForParticipant(
        client: io.ktor.client.HttpClient,
        fixture: Phase5Fixture,
        token: String,
        itemId: String
    ) {
        val responses = listOf(
            "PUT budget baseline" to client.put("/api/events/${fixture.eventId}/budget") {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(budgetBaselineBody(fixture))
            },
            "POST budget item" to client.post("/api/events/${fixture.eventId}/budget/items") {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(createBudgetItemBody())
            },
            "PUT budget item" to client.put("/api/events/${fixture.eventId}/budget/items/$itemId") {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(updateBudgetItemBody(itemId, fixture.budgetRepository.getBudgetByEventId(fixture.eventId)!!.id))
            },
            "DELETE budget item" to client.delete("/api/events/${fixture.eventId}/budget/items/$itemId") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
        )

        val accepted = responses.filter { (_, response) -> response.status != HttpStatusCode.Forbidden }
        assertTrue(
            accepted.isEmpty(),
            "Confirmed participants must not mutate budget baseline/items even while ORGANIZING.\n" +
                "Expected HTTP 403 Forbidden from every mutation route, got:\n" +
                responses.joinToString("\n") { (name, response) -> "$name -> ${response.status}" }
        )
    }

    private fun seedBudgetItem(fixture: Phase5Fixture) =
        fixture.budgetRepository.createBudgetItem(
            budgetId = fixture.budgetRepository.getBudgetByEventId(fixture.eventId)!!.id,
            category = BudgetCategory.MEALS,
            name = "Seeded budget line",
            description = "Existing line used to test update/delete workflow guards",
            estimatedCost = 80.0,
            sharedBy = listOf(fixture.organizerId, fixture.confirmedParticipantId)
        )

    private fun budgetBaselineBody(fixture: Phase5Fixture): String {
        val budget = fixture.budgetRepository.getBudgetByEventId(fixture.eventId)!!
        return """
            {
              "id": "${budget.id}",
              "eventId": "${fixture.eventId}",
              "totalEstimated": 1200.0,
              "totalActual": 0.0,
              "transportEstimated": 300.0,
              "transportActual": 0.0,
              "accommodationEstimated": 500.0,
              "accommodationActual": 0.0,
              "mealsEstimated": 250.0,
              "mealsActual": 0.0,
              "activitiesEstimated": 100.0,
              "activitiesActual": 0.0,
              "equipmentEstimated": 25.0,
              "equipmentActual": 0.0,
              "otherEstimated": 25.0,
              "otherActual": 0.0,
              "createdAt": "${budget.createdAt}",
              "updatedAt": "${budget.updatedAt}"
            }
        """.trimIndent()
    }

    private fun createBudgetItemBody(): String =
        """
        {
          "name": "Blocked budget line",
          "description": "This must not be created outside ORGANIZING",
          "category": "MEALS",
          "estimatedCost": 64.0,
          "sharedBy": ["organizer-user", "confirmed-user"]
        }
        """.trimIndent()

    private fun updateBudgetItemBody(itemId: String, budgetId: String): String =
        """
        {
          "id": "$itemId",
          "budgetId": "$budgetId",
          "category": "MEALS",
          "name": "Blocked budget line update",
          "description": "This must not be updated outside ORGANIZING",
          "estimatedCost": 96.0,
          "actualCost": 0.0,
          "isPaid": false,
          "paidBy": null,
          "sharedBy": ["organizer-user", "confirmed-user"],
          "notes": "workflow guard regression",
          "createdAt": "2026-05-22T10:00:00Z",
          "updatedAt": "2026-05-22T10:00:00Z"
        }
        """.trimIndent()

    private fun createTestJwt(userId: String): String {
        return JWT.create()
            .withIssuer(jwtIssuer)
            .withAudience(jwtAudience)
            .withClaim("userId", userId)
            .withClaim("sessionId", "test-session-$userId")
            .withClaim("permissions", listOf("READ", "WRITE"))
            .withExpiresAt(java.util.Date(System.currentTimeMillis() + 3_600_000))
            .sign(Algorithm.HMAC256(jwtSecret))
    }

    private fun paymentPotBody(
        eventId: String,
        tricountLink: String = "https://tricount.com/group/phase5-pot"
    ): String {
        return """
            {
              "eventId": "$eventId",
              "goalAmount": 1250.75,
              "currency": "EUR",
              "paymentProvider": "TRICOUNT",
              "status": "ACTIVE",
              "tricountLink": "$tricountLink"
            }
        """.trimIndent()
    }

    private fun tricountLinkBody(
        providerId: String,
        providerUrl: String,
        syncStatus: String = "LINKED"
    ): String =
        """
        {
          "provider": "TRICOUNT",
          "providerId": "$providerId",
          "providerUrl": "$providerUrl",
          "syncStatus": "$syncStatus"
        }
        """.trimIndent()

    private fun containsLiteralTemplateMarker(text: String): Boolean =
        text.contains("${'$'}{") || Regex("""\$[A-Za-z_][A-Za-z0-9_]*""").containsMatchIn(text)

    private fun isSafeHttpsLink(link: String): Boolean =
        link.startsWith("https://") && !containsLiteralTemplateMarker(link)

    private fun zoomMeetingIdFromUrl(url: String): String =
        Regex("""/j/([^?]+)""").find(url)?.groupValues?.get(1).orEmpty()

    private inline fun <T> withProviderEnvironment(
        vararg values: Pair<String, String>,
        block: () -> T
    ): T {
        val mutableEnvironment = mutableSystemEnvironment()
        val previousValues = values.associate { (key, _) -> key to mutableEnvironment[key] }

        values.forEach { (key, value) -> mutableEnvironment[key] = value }

        return try {
            block()
        } finally {
            previousValues.forEach { (key, value) ->
                if (value == null) {
                    mutableEnvironment.remove(key)
                } else {
                    mutableEnvironment[key] = value
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun mutableSystemEnvironment(): MutableMap<String, String> {
        val unsafeField = Unsafe::class.java.getDeclaredField("theUnsafe")
        unsafeField.isAccessible = true
        val unsafe = unsafeField.get(null) as Unsafe
        val environment = System.getenv()
        val delegateField = environment.javaClass.getDeclaredField("m")
        return unsafe.getObject(environment, unsafe.objectFieldOffset(delegateField)) as MutableMap<String, String>
    }

    private data class Phase5Fixture(
        val database: com.guyghost.wakeve.database.WakeveDb,
        val eventRepository: DatabaseEventRepository,
        val budgetRepository: BudgetRepository,
        val eventId: String,
        val organizerId: String,
        val confirmedParticipantId: String,
        val otherConfirmedParticipantId: String,
        val unconfirmedParticipantId: String
    )
}
