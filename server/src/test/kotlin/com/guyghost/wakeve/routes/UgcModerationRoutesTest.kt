package com.guyghost.wakeve.routes

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.guyghost.wakeve.JvmDatabaseFactory
import com.guyghost.wakeve.database.DatabaseProvider
import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.EventType
import com.guyghost.wakeve.models.TimeOfDay
import com.guyghost.wakeve.models.TimeSlot
import com.guyghost.wakeve.moderation.ModerationRepository
import com.guyghost.wakeve.module
import com.guyghost.wakeve.notification.APNsSender
import com.guyghost.wakeve.notification.EventNotificationTrigger
import com.guyghost.wakeve.notification.FCMSender
import com.guyghost.wakeve.notification.NotificationPreferencesRepository
import com.guyghost.wakeve.notification.NotificationService
import com.guyghost.wakeve.repository.DatabaseEventRepository
import com.guyghost.wakeve.repository.PotentialLocationRepository
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
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UgcModerationRoutesTest {
    private val jwtSecret = System.getenv("JWT_SECRET") ?: "default-secret-key-change-in-production"
    private val jwtIssuer = System.getenv("JWT_ISSUER") ?: "wakev-api"
    private val jwtAudience = System.getenv("JWT_AUDIENCE") ?: "wakev-client"
    private val json = Json { ignoreUnknownKeys = true }

    @BeforeTest
    fun setup() {
        DatabaseProvider.resetDatabase()
    }

    @AfterTest
    fun teardown() {
        DatabaseProvider.resetDatabase()
    }

    @Test
    fun `hard-policy comments chat messages and event text are rejected before visible persistence`() = testApplication {
        val fixture = createFixture("hard-policy")
        val token = createTestJwt(fixture.authorId)
        val client = createJsonClient()

        application { module(fixture.database) }

        val comment = client.post("/api/events/${fixture.eventId}/comments") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(commentBody("This contains a credible threat.", fixture.authorId, "Author"))
        }
        val chat = client.post("/api/events/${fixture.eventId}/chat/messages") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"content":"This contains a credible threat."}""")
        }
        val event = client.post("/api/events") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(createEventBody("credible threat"))
        }

        assertEquals(HttpStatusCode.BadRequest, comment.status)
        assertEquals(HttpStatusCode.BadRequest, chat.status)
        assertEquals(HttpStatusCode.BadRequest, event.status)
        assertEquals(0, fixture.database.commentQueries.countCommentsByEvent(fixture.eventId).executeAsOne())
        assertTrue(chat.bodyAsText().contains("hard_policy_term"))
    }

    @Test
    fun `hard-policy potential location text is rejected before persistence`() = testApplication {
        val fixture = createFixture("location-policy")
        val token = createTestJwt(fixture.authorId)
        val client = createJsonClient()
        val eventRepository = DatabaseEventRepository(fixture.database)
        val locationRepository = PotentialLocationRepository(eventRepository)

        application { module(fixture.database, eventRepository = eventRepository, locationRepository = locationRepository) }

        val response = client.post("/api/events/${fixture.eventId}/potential-locations") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "name": "credible threat",
                  "locationType": "VENUE",
                  "address": "Central station"
                }
                """.trimIndent()
            )
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("hard_policy_term"), response.bodyAsText())
        assertTrue(response.bodyAsText().contains("name"), response.bodyAsText())
        assertTrue(locationRepository.getLocationsByEventId(fixture.eventId).isEmpty())
    }

    @Test
    fun `uncertain comments and chat messages are persisted pending review and hidden from regular reads`() = testApplication {
        val fixture = createFixture("pending")
        val token = createTestJwt(fixture.authorId)
        val client = createJsonClient()

        application { module(fixture.database) }

        val pendingComment = client.post("/api/events/${fixture.eventId}/comments") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(commentBody("Please use an off-platform payment flow.", fixture.authorId, "Author"))
        }
        val visibleComments = client.get("/api/events/${fixture.eventId}/comments") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        val pendingChat = client.post("/api/events/${fixture.eventId}/chat/messages") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"content":"Please use an off-platform payment flow."}""")
        }
        val visibleChat = client.get("/api/events/${fixture.eventId}/chat/messages") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        val visibleChatBody = json.parseToJsonElement(visibleChat.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.Accepted, pendingComment.status)
        assertTrue(pendingComment.bodyAsText().contains("PENDING_REVIEW"))
        assertFalse(visibleComments.bodyAsText().contains("off-platform payment"))
        assertEquals(HttpStatusCode.Accepted, pendingChat.status)
        assertTrue(pendingChat.bodyAsText().contains("PENDING_REVIEW"))
        assertFalse(visibleChat.bodyAsText().contains("off-platform payment"))
        assertEquals(0, visibleChatBody.getValue("totalCount").jsonPrimitive.int)
    }

    @Test
    fun `comment list route applies bounded pagination parameters`() = testApplication {
        val fixture = createFixture("comment-pagination")
        val token = createTestJwt(fixture.authorId)
        val client = createJsonClient()

        application { module(fixture.database) }

        repeat(3) { index ->
            val response = client.post("/api/events/${fixture.eventId}/comments") {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(commentBody("Visible paginated comment $index", fixture.authorId, "Author"))
            }
            assertEquals(HttpStatusCode.Created, response.status, response.bodyAsText())
        }

        val limited = client.get("/api/events/${fixture.eventId}/comments?threaded=false&limit=1&offset=1") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        assertEquals(HttpStatusCode.OK, limited.status, limited.bodyAsText())
        assertEquals(1, json.parseToJsonElement(limited.bodyAsText()).jsonArray.size)
    }

    @Test
    fun `chat message route applies bounded pagination without full history fetch`() = testApplication {
        val fixture = createFixture("chat-pagination")
        val token = createTestJwt(fixture.authorId)
        val client = createJsonClient()

        application { module(fixture.database) }

        repeat(3) { index ->
            val response = client.post("/api/events/${fixture.eventId}/chat/messages") {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody("""{"content":"Visible chat message $index"}""")
            }
            assertEquals(HttpStatusCode.Created, response.status, response.bodyAsText())
        }

        val limited = client.get("/api/events/${fixture.eventId}/chat/messages?limit=1&offset=1") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        val body = json.parseToJsonElement(limited.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.OK, limited.status, limited.bodyAsText())
        assertEquals(1, body.getValue("messages").jsonArray.size)
        assertEquals(3, body.getValue("totalCount").jsonPrimitive.int)
        assertEquals(true, body.getValue("hasMore").jsonPrimitive.boolean)
    }

    @Test
    fun `comment creation rejects author impersonation from request body`() = testApplication {
        val fixture = createFixture("comment-impersonation")
        val token = createTestJwt(fixture.viewerId)
        val client = createJsonClient()

        application { module(fixture.database) }

        val response = client.post("/api/events/${fixture.eventId}/comments") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(commentBody("Forged author comment.", fixture.authorId, "Forged Author"))
        }

        assertEquals(HttpStatusCode.Forbidden, response.status, response.bodyAsText())
        assertTrue(response.bodyAsText().contains(commentAuthorForbiddenMessage()), response.bodyAsText())
        assertEquals(0, fixture.database.commentQueries.countCommentsByEvent(fixture.eventId).executeAsOne())
    }

    @Test
    fun `comment creation ignores forged author name from request body`() = testApplication {
        val fixture = createFixture("comment-name-forgery")
        val token = createTestJwt(fixture.viewerId, userName = "Trusted Viewer")
        val client = createJsonClient()

        application { module(fixture.database) }

        val response = client.post("/api/events/${fixture.eventId}/comments") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(commentBody("Name should be trusted.", fixture.viewerId, "Forged Organizer"))
        }
        val body = response.bodyAsText()

        assertEquals(HttpStatusCode.Created, response.status, body)
        assertTrue(body.contains("Trusted Viewer"), body)
        assertFalse(body.contains("Forged Organizer"), body)
    }

    @Test
    fun `comment update and delete are limited to author or organizer`() = testApplication {
        val fixture = createFixture("comment-mutation-auth")
        val authorToken = createTestJwt(fixture.authorId)
        val viewerToken = createTestJwt(fixture.viewerId)
        val client = createJsonClient()

        application { module(fixture.database) }

        val comment = client.post("/api/events/${fixture.eventId}/comments") {
            header(HttpHeaders.Authorization, "Bearer $authorToken")
            contentType(ContentType.Application.Json)
            setBody(commentBody("Original protected comment.", fixture.authorId, "Author"))
        }
        val commentId = json.parseToJsonElement(comment.bodyAsText()).jsonObjectValue("id")

        val deniedUpdate = client.put("/api/events/${fixture.eventId}/comments/$commentId") {
            header(HttpHeaders.Authorization, "Bearer $viewerToken")
            contentType(ContentType.Application.Json)
            setBody("""{"content":"Unauthorized edit."}""")
        }
        val deniedDelete = client.delete("/api/events/${fixture.eventId}/comments/$commentId") {
            header(HttpHeaders.Authorization, "Bearer $viewerToken")
        }
        val organizerDelete = client.delete("/api/events/${fixture.eventId}/comments/$commentId") {
            header(HttpHeaders.Authorization, "Bearer $authorToken")
        }

        assertEquals(HttpStatusCode.Created, comment.status, comment.bodyAsText())
        assertEquals(HttpStatusCode.Forbidden, deniedUpdate.status, deniedUpdate.bodyAsText())
        assertEquals(HttpStatusCode.Forbidden, deniedDelete.status, deniedDelete.bodyAsText())
        assertEquals(HttpStatusCode.NoContent, organizerDelete.status, organizerDelete.bodyAsText())
        assertEquals(0, fixture.database.commentQueries.countCommentsByEvent(fixture.eventId).executeAsOne())
    }

    @Test
    fun `blocked comment authors are hidden from direct and recent reads`() = testApplication {
        val fixture = createFixture("comment-direct-block")
        val authorToken = createTestJwt(fixture.authorId)
        val viewerToken = createTestJwt(fixture.viewerId)
        val client = createJsonClient()

        application { module(fixture.database) }

        val comment = client.post("/api/events/${fixture.eventId}/comments") {
            header(HttpHeaders.Authorization, "Bearer $authorToken")
            contentType(ContentType.Application.Json)
            setBody(commentBody("Blocked direct access comment.", fixture.authorId, "Author"))
        }
        val commentId = json.parseToJsonElement(comment.bodyAsText()).jsonObjectValue("id")
        val block = client.post("/api/moderation/blocks") {
            header(HttpHeaders.Authorization, "Bearer $viewerToken")
            contentType(ContentType.Application.Json)
            setBody("""{"blockedUserId":"${fixture.authorId}","eventId":"${fixture.eventId}","reason":"HARASSMENT"}""")
        }
        val direct = client.get("/api/events/${fixture.eventId}/comments/$commentId") {
            header(HttpHeaders.Authorization, "Bearer $viewerToken")
        }
        val recent = client.get("/api/events/${fixture.eventId}/comments/recent?since=2026-01-01T00:00:00Z&limit=10") {
            header(HttpHeaders.Authorization, "Bearer $viewerToken")
        }
        val topContributors = client.get("/api/events/${fixture.eventId}/comments/top-contributors?limit=10") {
            header(HttpHeaders.Authorization, "Bearer $viewerToken")
        }
        val statistics = client.get("/api/events/${fixture.eventId}/comments/statistics") {
            header(HttpHeaders.Authorization, "Bearer $viewerToken")
        }
        val sections = client.get("/api/events/${fixture.eventId}/comments/sections") {
            header(HttpHeaders.Authorization, "Bearer $viewerToken")
        }

        assertEquals(HttpStatusCode.Created, comment.status, comment.bodyAsText())
        assertEquals(HttpStatusCode.Created, block.status, block.bodyAsText())
        assertEquals(HttpStatusCode.NotFound, direct.status, direct.bodyAsText())
        assertFalse(recent.bodyAsText().contains("Blocked direct access comment"), recent.bodyAsText())
        assertFalse(topContributors.bodyAsText().contains(fixture.authorId), topContributors.bodyAsText())
        assertTrue(statistics.bodyAsText().contains(""""totalComments": 0"""), statistics.bodyAsText())
        assertFalse(statistics.bodyAsText().contains(fixture.authorId), statistics.bodyAsText())
        assertEquals("{}", sections.bodyAsText().trim(), sections.bodyAsText())
    }

    @Test
    fun `report and block endpoints create stable moderation records and suppress blocked user chat`() = testApplication {
        val fixture = createFixture("report-block")
        val authorToken = createTestJwt(fixture.authorId)
        val viewerToken = createTestJwt(fixture.viewerId)
        val client = createJsonClient()

        application { module(fixture.database) }

        val message = client.post("/api/events/${fixture.eventId}/chat/messages") {
            header(HttpHeaders.Authorization, "Bearer $authorToken")
            contentType(ContentType.Application.Json)
            setBody("""{"content":"Train leaves at 18:30."}""")
        }
        val comment = client.post("/api/events/${fixture.eventId}/comments") {
            header(HttpHeaders.Authorization, "Bearer $authorToken")
            contentType(ContentType.Application.Json)
            setBody(commentBody("Meet by the station.", fixture.authorId, "Author"))
        }
        val beforeBlock = client.get("/api/events/${fixture.eventId}/chat/messages") {
            header(HttpHeaders.Authorization, "Bearer $viewerToken")
        }
        val commentsBeforeBlock = client.get("/api/events/${fixture.eventId}/comments") {
            header(HttpHeaders.Authorization, "Bearer $viewerToken")
        }
        val report = client.post("/api/moderation/reports") {
            header(HttpHeaders.Authorization, "Bearer $viewerToken")
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "targetType": "CHAT_MESSAGE",
                  "targetId": "message-from-chat-response",
                  "eventId": "${fixture.eventId}",
                  "reason": "HARASSMENT",
                  "details": "Reviewer-visible report path"
                }
                """.trimIndent()
            )
        }
        val commentReport = client.post("/api/moderation/reports") {
            header(HttpHeaders.Authorization, "Bearer $viewerToken")
            contentType(ContentType.Application.Json)
            setBody("""{"targetType":"COMMENT","targetId":"comment-1","eventId":"${fixture.eventId}","reason":"HATE_OR_ABUSE"}""")
        }
        val eventReport = client.post("/api/moderation/reports") {
            header(HttpHeaders.Authorization, "Bearer $viewerToken")
            contentType(ContentType.Application.Json)
            setBody("""{"targetType":"EVENT","targetId":"${fixture.eventId}","eventId":"${fixture.eventId}","reason":"SPAM_OR_SCAM"}""")
        }
        val userReport = client.post("/api/moderation/reports") {
            header(HttpHeaders.Authorization, "Bearer $viewerToken")
            contentType(ContentType.Application.Json)
            setBody("""{"targetType":"USER","targetId":"${fixture.authorId}","eventId":"${fixture.eventId}","reason":"HARASSMENT"}""")
        }
        val block = client.post("/api/moderation/blocks") {
            header(HttpHeaders.Authorization, "Bearer $viewerToken")
            contentType(ContentType.Application.Json)
            setBody("""{"blockedUserId":"${fixture.authorId}","eventId":"${fixture.eventId}","reason":"HARASSMENT"}""")
        }
        val afterBlock = client.get("/api/events/${fixture.eventId}/chat/messages") {
            header(HttpHeaders.Authorization, "Bearer $viewerToken")
        }
        val commentsAfterBlock = client.get("/api/events/${fixture.eventId}/comments") {
            header(HttpHeaders.Authorization, "Bearer $viewerToken")
        }
        val unblock = client.delete("/api/moderation/blocks/${fixture.authorId}?eventId=${fixture.eventId}") {
            header(HttpHeaders.Authorization, "Bearer $viewerToken")
        }
        val commentsAfterUnblock = client.get("/api/events/${fixture.eventId}/comments") {
            header(HttpHeaders.Authorization, "Bearer $viewerToken")
        }

        val beforeBlockBody = beforeBlock.bodyAsText()
        assertEquals(HttpStatusCode.Created, message.status)
        assertEquals(HttpStatusCode.Created, comment.status)
        assertEquals(HttpStatusCode.OK, beforeBlock.status, beforeBlockBody)
        assertTrue(beforeBlockBody.contains("Train leaves"), beforeBlockBody)
        assertTrue(commentsBeforeBlock.bodyAsText().contains("Meet by the station"), commentsBeforeBlock.bodyAsText())
        assertEquals(HttpStatusCode.Created, report.status)
        assertTrue(report.bodyAsText().contains("CHAT_MESSAGE"))
        assertEquals(HttpStatusCode.Created, commentReport.status)
        assertTrue(commentReport.bodyAsText().contains("COMMENT"))
        assertEquals(HttpStatusCode.Created, eventReport.status)
        assertTrue(eventReport.bodyAsText().contains("EVENT"))
        assertEquals(HttpStatusCode.Created, userReport.status)
        assertTrue(userReport.bodyAsText().contains("USER"))
        assertEquals(HttpStatusCode.Created, block.status)
        assertFalse(afterBlock.bodyAsText().contains("Train leaves"), afterBlock.bodyAsText())
        assertFalse(commentsAfterBlock.bodyAsText().contains("Meet by the station"), commentsAfterBlock.bodyAsText())
        assertEquals(HttpStatusCode.NoContent, unblock.status)
        assertTrue(commentsAfterUnblock.bodyAsText().contains("Meet by the station"), commentsAfterUnblock.bodyAsText())
    }

    @Test
    fun `blocked chat authors are hidden from direct thread and section reads`() = testApplication {
        val fixture = createFixture("chat-direct-block")
        val authorToken = createTestJwt(fixture.authorId)
        val viewerToken = createTestJwt(fixture.viewerId)
        val client = createJsonClient()

        application { module(fixture.database) }

        val parent = client.post("/api/events/${fixture.eventId}/chat/messages") {
            header(HttpHeaders.Authorization, "Bearer $viewerToken")
            contentType(ContentType.Application.Json)
            setBody("""{"content":"Parent visible to viewer."}""")
        }
        val parentId = json.parseToJsonElement(parent.bodyAsText())
            .jsonObject.getValue("message").jsonObjectValue("id")
        val directMessage = client.post("/api/events/${fixture.eventId}/chat/messages") {
            header(HttpHeaders.Authorization, "Bearer $authorToken")
            contentType(ContentType.Application.Json)
            setBody("""{"content":"Blocked direct chat message.","section":"GENERAL"}""")
        }
        val directMessageId = json.parseToJsonElement(directMessage.bodyAsText())
            .jsonObject.getValue("message").jsonObjectValue("id")
        val reply = client.post("/api/events/${fixture.eventId}/chat/messages") {
            header(HttpHeaders.Authorization, "Bearer $authorToken")
            contentType(ContentType.Application.Json)
            setBody("""{"content":"Blocked thread reply.","parentMessageId":"$parentId"}""")
        }
        val block = client.post("/api/moderation/blocks") {
            header(HttpHeaders.Authorization, "Bearer $viewerToken")
            contentType(ContentType.Application.Json)
            setBody("""{"blockedUserId":"${fixture.authorId}","eventId":"${fixture.eventId}","reason":"HARASSMENT"}""")
        }
        val direct = client.get("/api/events/${fixture.eventId}/chat/messages/$directMessageId") {
            header(HttpHeaders.Authorization, "Bearer $viewerToken")
        }
        val replies = client.get("/api/events/${fixture.eventId}/chat/messages/$parentId/replies") {
            header(HttpHeaders.Authorization, "Bearer $viewerToken")
        }
        val section = client.get("/api/events/${fixture.eventId}/chat/messages/section/general") {
            header(HttpHeaders.Authorization, "Bearer $viewerToken")
        }
        val reaction = client.post("/api/events/${fixture.eventId}/chat/messages/$directMessageId/reactions") {
            header(HttpHeaders.Authorization, "Bearer $viewerToken")
            contentType(ContentType.Application.Json)
            setBody("""{"emoji":"OK"}""")
        }
        val read = client.post("/api/events/${fixture.eventId}/chat/messages/$directMessageId/read") {
            header(HttpHeaders.Authorization, "Bearer $viewerToken")
        }
        val unread = client.get("/api/events/${fixture.eventId}/chat/unread-count") {
            header(HttpHeaders.Authorization, "Bearer $viewerToken")
        }
        val unreadBody = json.parseToJsonElement(unread.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.Created, parent.status, parent.bodyAsText())
        assertEquals(HttpStatusCode.Created, directMessage.status, directMessage.bodyAsText())
        assertEquals(HttpStatusCode.Created, reply.status, reply.bodyAsText())
        assertEquals(HttpStatusCode.Created, block.status, block.bodyAsText())
        assertEquals(HttpStatusCode.NotFound, direct.status, direct.bodyAsText())
        assertFalse(replies.bodyAsText().contains("Blocked thread reply"), replies.bodyAsText())
        assertFalse(section.bodyAsText().contains("Blocked direct chat message"), section.bodyAsText())
        assertEquals(HttpStatusCode.NotFound, reaction.status, reaction.bodyAsText())
        assertEquals(HttpStatusCode.NotFound, read.status, read.bodyAsText())
        assertEquals(HttpStatusCode.OK, unread.status, unread.bodyAsText())
        assertEquals(0, unreadBody.getValue("unreadCount").jsonPrimitive.int)
    }

    @Test
    fun `chat pagination fills pages after blocked authors are filtered`() = testApplication {
        val fixture = createFixture("chat-block-pagination")
        val authorToken = createTestJwt(fixture.authorId)
        val viewerToken = createTestJwt(fixture.viewerId)
        val unblockedUserId = "unblocked-chat-block-pagination"
        val unblockedToken = createTestJwt(unblockedUserId)
        val client = createJsonClient()
        insertUser(fixture.database, unblockedUserId, "Unblocked Chat User")

        application { module(fixture.database) }

        val blockedMessage = client.post("/api/events/${fixture.eventId}/chat/messages") {
            header(HttpHeaders.Authorization, "Bearer $authorToken")
            contentType(ContentType.Application.Json)
            setBody("""{"content":"Blocked message should not consume page space."}""")
        }
        val visibleOne = client.post("/api/events/${fixture.eventId}/chat/messages") {
            header(HttpHeaders.Authorization, "Bearer $unblockedToken")
            contentType(ContentType.Application.Json)
            setBody("""{"content":"Visible page message one."}""")
        }
        val visibleTwo = client.post("/api/events/${fixture.eventId}/chat/messages") {
            header(HttpHeaders.Authorization, "Bearer $unblockedToken")
            contentType(ContentType.Application.Json)
            setBody("""{"content":"Visible page message two."}""")
        }
        val block = client.post("/api/moderation/blocks") {
            header(HttpHeaders.Authorization, "Bearer $viewerToken")
            contentType(ContentType.Application.Json)
            setBody("""{"blockedUserId":"${fixture.authorId}","eventId":"${fixture.eventId}","reason":"HARASSMENT"}""")
        }
        val paged = client.get("/api/events/${fixture.eventId}/chat/messages?limit=2&offset=0") {
            header(HttpHeaders.Authorization, "Bearer $viewerToken")
        }
        val pagedBodyText = paged.bodyAsText()
        val pagedBody = json.parseToJsonElement(pagedBodyText).jsonObject

        assertEquals(HttpStatusCode.Created, blockedMessage.status, blockedMessage.bodyAsText())
        assertEquals(HttpStatusCode.Created, visibleOne.status, visibleOne.bodyAsText())
        assertEquals(HttpStatusCode.Created, visibleTwo.status, visibleTwo.bodyAsText())
        assertEquals(HttpStatusCode.Created, block.status, block.bodyAsText())
        assertEquals(HttpStatusCode.OK, paged.status, pagedBodyText)
        assertEquals(2, pagedBody.getValue("messages").jsonArray.size)
        assertEquals(2, pagedBody.getValue("totalCount").jsonPrimitive.int)
        assertEquals(false, pagedBody.getValue("hasMore").jsonPrimitive.boolean)
        assertFalse(pagedBodyText.contains("Blocked message should not consume page space."), pagedBodyText)
        assertTrue(pagedBodyText.contains("Visible page message one."), pagedBodyText)
        assertTrue(pagedBodyText.contains("Visible page message two."), pagedBodyText)
    }

    @Test
    fun `chat replies require visible parent message`() = testApplication {
        val fixture = createFixture("chat-visible-parent")
        val authorToken = createTestJwt(fixture.authorId)
        val viewerToken = createTestJwt(fixture.viewerId)
        val client = createJsonClient()

        application { module(fixture.database) }

        val pendingParent = client.post("/api/events/${fixture.eventId}/chat/messages") {
            header(HttpHeaders.Authorization, "Bearer $authorToken")
            contentType(ContentType.Application.Json)
            setBody("""{"content":"Please use an off-platform payment flow."}""")
        }
        val pendingParentId = json.parseToJsonElement(pendingParent.bodyAsText())
            .jsonObject.getValue("message").jsonObjectValue("id")
        val replyToPending = client.post("/api/events/${fixture.eventId}/chat/messages") {
            header(HttpHeaders.Authorization, "Bearer $viewerToken")
            contentType(ContentType.Application.Json)
            setBody("""{"content":"Reply should not attach to hidden parent.","parentMessageId":"$pendingParentId"}""")
        }

        val blockedParent = client.post("/api/events/${fixture.eventId}/chat/messages") {
            header(HttpHeaders.Authorization, "Bearer $authorToken")
            contentType(ContentType.Application.Json)
            setBody("""{"content":"Visible until blocked parent."}""")
        }
        val blockedParentId = json.parseToJsonElement(blockedParent.bodyAsText())
            .jsonObject.getValue("message").jsonObjectValue("id")
        val block = client.post("/api/moderation/blocks") {
            header(HttpHeaders.Authorization, "Bearer $viewerToken")
            contentType(ContentType.Application.Json)
            setBody("""{"blockedUserId":"${fixture.authorId}","eventId":"${fixture.eventId}","reason":"HARASSMENT"}""")
        }
        val replyToBlocked = client.post("/api/events/${fixture.eventId}/chat/messages") {
            header(HttpHeaders.Authorization, "Bearer $viewerToken")
            contentType(ContentType.Application.Json)
            setBody("""{"content":"Reply should not attach to blocked parent.","parentMessageId":"$blockedParentId"}""")
        }

        assertEquals(HttpStatusCode.Accepted, pendingParent.status, pendingParent.bodyAsText())
        assertEquals(HttpStatusCode.NotFound, replyToPending.status, replyToPending.bodyAsText())
        assertEquals(HttpStatusCode.Created, blockedParent.status, blockedParent.bodyAsText())
        assertEquals(HttpStatusCode.Created, block.status, block.bodyAsText())
        assertEquals(HttpStatusCode.NotFound, replyToBlocked.status, replyToBlocked.bodyAsText())
    }

    @Test
    fun `chat sender display name falls back to email before user id`() = testApplication {
        val fixture = createFixture("chat-email-name")
        val token = createTestJwtWithoutUserName(fixture.viewerId, email = "trusted.viewer@example.test")
        val client = createJsonClient()

        application { module(fixture.database) }

        val response = client.post("/api/events/${fixture.eventId}/chat/messages") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"content":"Email fallback display name."}""")
        }
        val body = response.bodyAsText()

        assertEquals(HttpStatusCode.Created, response.status, body)
        assertTrue(body.contains("trusted.viewer"), body)
        assertFalse(body.contains("Unknown"), body)
    }

    @Test
    fun `chat mutations and replies cannot cross event boundaries`() = testApplication {
        val fixture = createFixture("chat-cross-event")
        val otherEventId = "event-ugc-chat-cross-event-other"
        val token = createTestJwt(fixture.authorId)
        val client = createJsonClient()
        createAdditionalEvent(fixture.database, otherEventId, fixture.authorId, "chat-cross-event-other")

        application { module(fixture.database) }

        val original = client.post("/api/events/${fixture.eventId}/chat/messages") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"content":"Original event-scoped message."}""")
        }
        val messageId = json.parseToJsonElement(original.bodyAsText())
            .jsonObject.getValue("message").jsonObjectValue("id")

        val crossEventUpdate = client.put("/api/events/$otherEventId/chat/messages/$messageId") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"content":"Cross-event edit should fail."}""")
        }
        val crossEventDelete = client.delete("/api/events/$otherEventId/chat/messages/$messageId") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        val crossEventReply = client.post("/api/events/$otherEventId/chat/messages") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"content":"Cross-event reply should fail.","parentMessageId":"$messageId"}""")
        }
        val originalDirect = client.get("/api/events/${fixture.eventId}/chat/messages/$messageId") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        val otherMessages = client.get("/api/events/$otherEventId/chat/messages") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        val otherMessagesBody = json.parseToJsonElement(otherMessages.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.Created, original.status, original.bodyAsText())
        assertEquals(HttpStatusCode.NotFound, crossEventUpdate.status, crossEventUpdate.bodyAsText())
        assertEquals(HttpStatusCode.NotFound, crossEventDelete.status, crossEventDelete.bodyAsText())
        assertEquals(HttpStatusCode.NotFound, crossEventReply.status, crossEventReply.bodyAsText())
        assertTrue(originalDirect.bodyAsText().contains("Original event-scoped message."), originalDirect.bodyAsText())
        assertFalse(originalDirect.bodyAsText().contains("Cross-event edit should fail"), originalDirect.bodyAsText())
        assertEquals(0, otherMessagesBody.getValue("messages").jsonArray.size)
    }

    @Test
    fun `event-scoped user blocks do not suppress other events`() = testApplication {
        val fixture = createFixture("event-scoped-block")
        val otherEventId = "event-ugc-event-scoped-block-other"
        val authorToken = createTestJwt(fixture.authorId)
        val viewerToken = createTestJwt(fixture.viewerId)
        val client = createJsonClient()
        createAdditionalEvent(fixture.database, otherEventId, fixture.authorId, "event-scoped-block-other")

        application { module(fixture.database) }

        val block = client.post("/api/moderation/blocks") {
            header(HttpHeaders.Authorization, "Bearer $viewerToken")
            contentType(ContentType.Application.Json)
            setBody("""{"blockedUserId":"${fixture.authorId}","eventId":"${fixture.eventId}","reason":"HARASSMENT"}""")
        }
        val otherChat = client.post("/api/events/$otherEventId/chat/messages") {
            header(HttpHeaders.Authorization, "Bearer $authorToken")
            contentType(ContentType.Application.Json)
            setBody("""{"content":"Other event chat remains visible."}""")
        }
        val otherComment = client.post("/api/events/$otherEventId/comments") {
            header(HttpHeaders.Authorization, "Bearer $authorToken")
            contentType(ContentType.Application.Json)
            setBody(commentBody("Other event comment remains visible.", fixture.authorId, "Author"))
        }
        val otherChatRead = client.get("/api/events/$otherEventId/chat/messages") {
            header(HttpHeaders.Authorization, "Bearer $viewerToken")
        }
        val otherCommentsRead = client.get("/api/events/$otherEventId/comments") {
            header(HttpHeaders.Authorization, "Bearer $viewerToken")
        }

        assertEquals(HttpStatusCode.Created, block.status, block.bodyAsText())
        assertEquals(HttpStatusCode.Created, otherChat.status, otherChat.bodyAsText())
        assertEquals(HttpStatusCode.Created, otherComment.status, otherComment.bodyAsText())
        assertTrue(otherChatRead.bodyAsText().contains("Other event chat remains visible."), otherChatRead.bodyAsText())
        assertTrue(otherCommentsRead.bodyAsText().contains("Other event comment remains visible."), otherCommentsRead.bodyAsText())
    }

    @Test
    fun `event-scoped moderation actions require event membership`() = testApplication {
        val fixture = createFixture("event-scope-auth")
        val outsiderId = "outsider-event-scope-auth"
        val outsiderToken = createTestJwt(outsiderId)
        val viewerToken = createTestJwt(fixture.viewerId)
        val client = createJsonClient()
        insertUser(fixture.database, outsiderId, "Outsider event-scope-auth")

        application { module(fixture.database) }

        val outsiderReport = client.post("/api/moderation/reports") {
            header(HttpHeaders.Authorization, "Bearer $outsiderToken")
            contentType(ContentType.Application.Json)
            setBody("""{"targetType":"USER","targetId":"${fixture.authorId}","eventId":"${fixture.eventId}","reason":"HARASSMENT"}""")
        }
        val outsiderBlock = client.post("/api/moderation/blocks") {
            header(HttpHeaders.Authorization, "Bearer $outsiderToken")
            contentType(ContentType.Application.Json)
            setBody("""{"blockedUserId":"${fixture.authorId}","eventId":"${fixture.eventId}","reason":"HARASSMENT"}""")
        }
        val targetOutsideEvent = client.post("/api/moderation/blocks") {
            header(HttpHeaders.Authorization, "Bearer $viewerToken")
            contentType(ContentType.Application.Json)
            setBody("""{"blockedUserId":"$outsiderId","eventId":"${fixture.eventId}","reason":"HARASSMENT"}""")
        }
        val globalBlock = client.post("/api/moderation/blocks") {
            header(HttpHeaders.Authorization, "Bearer $viewerToken")
            contentType(ContentType.Application.Json)
            setBody("""{"blockedUserId":"$outsiderId","reason":"HARASSMENT"}""")
        }

        assertEquals(HttpStatusCode.Forbidden, outsiderReport.status, outsiderReport.bodyAsText())
        assertEquals(HttpStatusCode.Forbidden, outsiderBlock.status, outsiderBlock.bodyAsText())
        assertEquals(HttpStatusCode.BadRequest, targetOutsideEvent.status, targetOutsideEvent.bodyAsText())
        assertEquals(HttpStatusCode.Created, globalBlock.status, globalBlock.bodyAsText())
    }

    @Test
    fun `blocked users do not receive new comment notifications`() = testApplication {
        val fixture = createFixture("block-notifications")
        val authorToken = createTestJwt(fixture.authorId)
        val blockedViewerId = fixture.viewerId
        val unblockedViewerId = "viewer-unblocked-notifications"
        val client = createJsonClient()
        insertUser(fixture.database, unblockedViewerId, "Unblocked Viewer")
        seedParticipant(fixture.database, fixture.eventId, blockedViewerId, "blocked")
        seedParticipant(fixture.database, fixture.eventId, unblockedViewerId, "unblocked")
        seedPushToken(fixture.database, blockedViewerId)
        seedPushToken(fixture.database, unblockedViewerId)
        seedNewCommentPreferences(fixture.database, blockedViewerId)
        seedNewCommentPreferences(fixture.database, unblockedViewerId)

        val eventRepository = DatabaseEventRepository(fixture.database)
        val moderationRepository = ModerationRepository(fixture.database)
        val notificationService = NotificationService(
            database = fixture.database,
            preferencesRepository = NotificationPreferencesRepository(fixture.database),
            fcmSender = SuccessfulFCMSender(),
            apnsSender = SuccessfulAPNsSender()
        )
        val notificationTrigger = EventNotificationTrigger(notificationService, eventRepository, moderationRepository)

        application {
            module(
                database = fixture.database,
                eventRepository = eventRepository,
                moderationRepository = moderationRepository,
                notificationService = notificationService,
                eventNotificationTrigger = notificationTrigger
            )
        }

        client.post("/api/moderation/blocks") {
            header(HttpHeaders.Authorization, "Bearer ${createTestJwt(blockedViewerId)}")
            contentType(ContentType.Application.Json)
            setBody("""{"blockedUserId":"${fixture.authorId}","eventId":"${fixture.eventId}","reason":"HARASSMENT"}""")
        }
        val comment = client.post("/api/events/${fixture.eventId}/comments") {
            header(HttpHeaders.Authorization, "Bearer $authorToken")
            contentType(ContentType.Application.Json)
            setBody(commentBody("Notification-visible comment.", fixture.authorId, "Author"))
        }

        assertEquals(HttpStatusCode.Created, comment.status)
        waitForNotification(fixture.database, unblockedViewerId)
        val notifications = fixture.database.notificationQueries
            .getNotifications(unblockedViewerId, 20)
            .executeAsList()
        assertTrue(notifications.isNotEmpty())
        val notificationData = json.parseToJsonElement(notifications.single().data_ ?: "{}").jsonObject
        assertEquals(
            "wakeve://event/${fixture.eventId}/details?tab=comments",
            notificationData["deepLink"]?.jsonPrimitive?.content
        )
        assertEquals("EVENT_DETAILS", notificationData["deepLinkRoute"]?.jsonPrimitive?.content)
        assertEquals("event/${fixture.eventId}/details", notificationData["deepLinkPath"]?.jsonPrimitive?.content)
        assertEquals("comments", notificationData["param_tab"]?.jsonPrimitive?.content)
        assertTrue(fixture.database.notificationQueries.getNotifications(blockedViewerId, 20).executeAsList().isEmpty())
    }

    @Test
    fun `moderator review requires moderator role and writes audit decisions`() = testApplication {
        val fixture = createFixture("moderator")
        val userToken = createTestJwt(fixture.viewerId)
        val moderatorToken = createTestJwt("moderator-user", role = "MODERATOR")
        val client = createJsonClient()

        application { module(fixture.database) }

        val report = client.post("/api/moderation/reports") {
            header(HttpHeaders.Authorization, "Bearer $userToken")
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "targetType": "USER",
                  "targetId": "${fixture.authorId}",
                  "eventId": "${fixture.eventId}",
                  "reason": "HARASSMENT"
                }
                """.trimIndent()
            )
        }
        val reportId = json.parseToJsonElement(report.bodyAsText()).jsonObjectValue("id")

        val denied = client.post("/api/moderation/reports/$reportId/decisions") {
            header(HttpHeaders.Authorization, "Bearer $userToken")
            contentType(ContentType.Application.Json)
            setBody(decisionBody(fixture.authorId))
        }
        val accepted = client.post("/api/moderation/reports/$reportId/decisions") {
            header(HttpHeaders.Authorization, "Bearer $moderatorToken")
            contentType(ContentType.Application.Json)
            setBody(decisionBody(fixture.authorId))
        }

        assertEquals(HttpStatusCode.Forbidden, denied.status)
        assertEquals(HttpStatusCode.Created, accepted.status)
        assertTrue(accepted.bodyAsText().contains("ACCEPTED"))
    }

    @Test
    fun `websocket chat delivery is authenticated and block-filtered per recipient`() {
        val websocketSource = readProjectFile("server/src/main/kotlin/com/guyghost/wakeve/routing/ChatWebSocket.kt")
        val applicationSource = readProjectFile("server/src/main/kotlin/com/guyghost/wakeve/Application.kt")
        val chatServiceSource = readProjectFile("server/src/main/kotlin/com/guyghost/wakeve/routing/ChatService.kt")

        assertTrue(websocketSource.contains("call.principal<JWTPrincipal>()"), "WebSocket route must use authenticated user identity.")
        assertTrue(websocketSource.contains("hasChatWebSocketAccess(database, eventId, userId)"), "WebSocket route must require event membership before connecting.")
        assertTrue(websocketSource.contains("ConcurrentHashMap<String, ConcurrentHashMap<String, EventChatConnection>>"), "Connections must be tracked per event and per user connection.")
        assertTrue(websocketSource.contains("moderationRepository?.isBlockedForEvent(connection.userId, senderId, eventId)"), "WebSocket broadcast must suppress blocked senders per recipient within the current event.")
        assertTrue(applicationSource.contains("chatWebSocketRoute(database, moderationRepository)"), "Application wiring must pass database and moderation repository to WebSocket route.")
        assertTrue(chatServiceSource.contains("eventConnections.broadcast(eventId, response, moderationRepository)"), "Chat service broadcasts must use moderation-aware delivery.")
    }

    @Test
    fun `websocket chat access is limited to event organizer or participants`() {
        val fixture = createFixture("websocket-membership")

        assertTrue(hasChatWebSocketAccess(fixture.database, fixture.eventId, fixture.authorId))
        assertTrue(hasChatWebSocketAccess(fixture.database, fixture.eventId, fixture.viewerId))
        assertFalse(hasChatWebSocketAccess(fixture.database, fixture.eventId, "outsider-user"))
        assertFalse(hasChatWebSocketAccess(fixture.database, "missing-event", fixture.authorId))
    }

    @Test
    fun `planning free-text routes use shared moderation guard before persistence`() {
        val applicationSource = readProjectFile("server/src/main/kotlin/com/guyghost/wakeve/Application.kt")
        val locationSource = readProjectFile("server/src/main/kotlin/com/guyghost/wakeve/routes/PotentialLocationRoutes.kt")
        val mealSource = readProjectFile("server/src/main/kotlin/com/guyghost/wakeve/routes/MealRoutes.kt")
        val budgetSource = readProjectFile("server/src/main/kotlin/com/guyghost/wakeve/routes/BudgetRoutes.kt")

        assertTrue(applicationSource.contains("val moderationPolicy = ModerationPolicy()"), "Application should share a moderation policy across user-authored route text.")
        assertTrue(applicationSource.contains("eventRoutes(eventRepository, gamificationService, eventNotificationTrigger, database, moderationPolicy)"), "Event title, description, and custom type should use shared moderation policy.")
        assertTrue(applicationSource.contains("potentialLocationRoutes(locationRepository, eventRepository, database, moderationPolicy)"), "Potential locations should receive the moderation policy.")
        assertTrue(applicationSource.contains("mealRoutes(mealRepository, eventRepository, database, moderationPolicy)"), "Meal planning text should receive the moderation policy.")
        assertTrue(applicationSource.contains("budgetRoutes(budgetRepository, eventRepository, database, moderationPolicy)"), "Budget item planning text should receive the moderation policy.")

        assertTrue(locationSource.contains("ModeratedTextField(\"name\", request.name)"), "Potential location name must be moderated.")
        assertTrue(locationSource.contains("ModeratedTextField(\"address\", request.address)"), "Potential location address must be moderated.")
        assertTrue(mealSource.contains("ModeratedTextField(\"name\", request.name)"), "Meal name must be moderated.")
        assertTrue(mealSource.contains("ModeratedTextField(\"location\", request.location)"), "Meal location must be moderated.")
        assertTrue(mealSource.contains("ModeratedTextField(\"notes\", request.notes)"), "Meal notes must be moderated.")
        assertTrue(budgetSource.contains("ModeratedTextField(\"name\", normalizedName)"), "Budget item name must be moderated.")
        assertTrue(budgetSource.contains("ModeratedTextField(\"description\", normalizedDescription)"), "Budget item description must be moderated.")
    }

    private fun ApplicationTestBuilder.createJsonClient() = createClient {
        install(ContentNegotiation) { json(json) }
    }

    private fun readProjectFile(relativePath: String): String {
        val candidates = listOf(
            File(relativePath),
            File("../$relativePath"),
            File("../../$relativePath")
        )
        return candidates.firstOrNull { it.exists() }?.readText()
            ?: error("Could not locate $relativePath from ${File(".").absolutePath}")
    }

    private fun createFixture(suffix: String): ModerationFixture {
        val database = DatabaseProvider.getDatabase(JvmDatabaseFactory(":memory:"))
        val eventRepository = DatabaseEventRepository(database)
        val eventId = "event-ugc-$suffix"
        val authorId = "author-$suffix"
        val viewerId = "viewer-$suffix"
        val now = "2026-06-13T10:00:00Z"

        insertUser(database, authorId, "Author $suffix")
        insertUser(database, viewerId, "Viewer $suffix")

        runBlocking {
            eventRepository.createEvent(
                Event(
                    id = eventId,
                    title = "UGC moderation $suffix",
                    description = "Moderation route test",
                    organizerId = authorId,
                    participants = emptyList(),
                    proposedSlots = listOf(
                        TimeSlot(
                            id = "slot-$suffix",
                            start = "2026-07-01T10:00:00Z",
                            end = "2026-07-01T12:00:00Z",
                            timezone = "UTC",
                            timeOfDay = TimeOfDay.SPECIFIC
                        )
                    ),
                    deadline = "2026-06-20T00:00:00Z",
                    status = EventStatus.DRAFT,
                    createdAt = now,
                    updatedAt = now,
                    eventType = EventType.OTHER
                )
            ).getOrThrow()
        }

        seedParticipant(database, eventId, viewerId, "viewer-$suffix")

        return ModerationFixture(database, eventId, authorId, viewerId)
    }

    private fun createAdditionalEvent(database: WakeveDb, eventId: String, organizerId: String, suffix: String) {
        val eventRepository = DatabaseEventRepository(database)
        runBlocking {
            eventRepository.createEvent(
                Event(
                    id = eventId,
                    title = "Additional UGC moderation $suffix",
                    description = "Additional moderation route test",
                    organizerId = organizerId,
                    participants = emptyList(),
                    proposedSlots = listOf(
                        TimeSlot(
                            id = "slot-$suffix",
                            start = "2026-07-02T10:00:00Z",
                            end = "2026-07-02T12:00:00Z",
                            timezone = "UTC",
                            timeOfDay = TimeOfDay.SPECIFIC
                        )
                    ),
                    deadline = "2026-06-20T00:00:00Z",
                    status = EventStatus.DRAFT,
                    createdAt = "2026-06-13T10:00:00Z",
                    updatedAt = "2026-06-13T10:00:00Z",
                    eventType = EventType.OTHER
                )
            ).getOrThrow()
        }
    }

    private fun insertUser(database: WakeveDb, userId: String, name: String) {
        database.userQueries.insertUser(
            id = userId,
            provider_id = "provider-$userId",
            email = "$userId@example.test",
            name = name,
            avatar_url = null,
            provider = "google",
            role = "USER",
            created_at = "2026-06-13T10:00:00Z",
            updated_at = "2026-06-13T10:00:00Z"
        )
    }

    private fun seedParticipant(database: WakeveDb, eventId: String, userId: String, suffix: String) {
        if (database.participantQueries.selectByEventIdAndUserId(eventId, userId).executeAsOneOrNull() != null) {
            return
        }
        database.participantQueries.insertParticipant(
            id = "participant-$suffix-$eventId",
            eventId = eventId,
            userId = userId,
            role = "PARTICIPANT",
            hasValidatedDate = 1,
            joinedAt = "2026-06-13T10:00:00Z",
            updatedAt = "2026-06-13T10:00:00Z"
        )
    }

    private fun seedPushToken(database: WakeveDb, userId: String) {
        database.notificationQueries.upsertToken(
            user_id = userId,
            platform = "IOS",
            token = "apns-$userId",
            updated_at = 1L
        )
    }

    private fun seedNewCommentPreferences(database: WakeveDb, userId: String) {
        database.userQueries.insertPreferences(
            user_id = userId,
            enabled_types = """["NEW_COMMENT"]""",
            quiet_hours_start = null,
            quiet_hours_end = null,
            sound_enabled = 1,
            vibration_enabled = 1,
            updated_at = 1L
        )
    }

    private fun waitForNotification(database: WakeveDb, userId: String) {
        repeat(20) {
            if (database.notificationQueries.getNotifications(userId, 20).executeAsList().isNotEmpty()) {
                return
            }
            Thread.sleep(50)
        }
    }

    private fun commentBody(content: String, authorId: String = "author-from-body", authorName: String = "Author"): String =
        """
        {
          "section": "GENERAL",
          "authorId": "$authorId",
          "authorName": "$authorName",
          "content": "$content"
        }
        """.trimIndent()

    private fun createEventBody(title: String): String =
        """
        {
          "title": "$title",
          "description": "Event description",
          "organizerId": "organizer-event",
          "deadline": "2026-06-20T00:00:00Z",
          "proposedSlots": [
            {
              "id": "slot-rejected",
              "start": "2026-07-01T10:00:00Z",
              "end": "2026-07-01T12:00:00Z",
              "timezone": "UTC",
              "timeOfDay": "SPECIFIC"
            }
          ]
        }
        """.trimIndent()

    private fun decisionBody(targetId: String): String =
        """
        {
          "targetType": "USER",
          "targetId": "$targetId",
          "action": "HIDE",
          "reason": "Report reviewed by moderator"
        }
        """.trimIndent()

    private fun createTestJwt(
        userId: String,
        role: String = "USER",
        userName: String = userId
    ): String =
        JWT.create()
            .withIssuer(jwtIssuer)
            .withAudience(jwtAudience)
            .withClaim("userId", userId)
            .withClaim("userName", userName)
            .withClaim("role", role)
            .withClaim("sessionId", "test-session-$userId")
            .withClaim("permissions", if (role == "MODERATOR") listOf("READ", "WRITE", "MODERATE") else listOf("READ", "WRITE"))
            .withExpiresAt(java.util.Date(System.currentTimeMillis() + 3_600_000))
            .sign(Algorithm.HMAC256(jwtSecret))

    private fun createTestJwtWithoutUserName(
        userId: String,
        email: String,
        role: String = "USER"
    ): String =
        JWT.create()
            .withIssuer(jwtIssuer)
            .withAudience(jwtAudience)
            .withClaim("userId", userId)
            .withClaim("email", email)
            .withClaim("role", role)
            .withClaim("sessionId", "test-session-$userId")
            .withClaim("permissions", if (role == "MODERATOR") listOf("READ", "WRITE", "MODERATE") else listOf("READ", "WRITE"))
            .withExpiresAt(java.util.Date(System.currentTimeMillis() + 3_600_000))
            .sign(Algorithm.HMAC256(jwtSecret))

    private fun kotlinx.serialization.json.JsonElement.jsonObjectValue(key: String): String =
        jsonObject[key]?.jsonPrimitive?.content ?: error("Missing JSON field $key")

    private class SuccessfulFCMSender : FCMSender {
        override suspend fun sendNotification(
            token: String,
            title: String,
            body: String,
            data: Map<String, String>
        ): Result<Unit> = Result.success(Unit)
    }

    private class SuccessfulAPNsSender : APNsSender {
        override suspend fun sendNotification(
            token: String,
            title: String,
            body: String,
            data: Map<String, String>
        ): Result<Unit> = Result.success(Unit)
    }

    private data class ModerationFixture(
        val database: WakeveDb,
        val eventId: String,
        val authorId: String,
        val viewerId: String
    )
}
