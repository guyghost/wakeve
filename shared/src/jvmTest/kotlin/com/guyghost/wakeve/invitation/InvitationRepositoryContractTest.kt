package com.guyghost.wakeve.invitation

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.guyghost.wakeve.createFreshTestDatabase
import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.models.Invitation
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InvitationRepositoryContractTest {

    private lateinit var database: WakeveDb
    private lateinit var repository: InvitationRepository

    @BeforeTest
    fun setUp() {
        database = createFreshTestDatabase()
        repository = InvitationRepository(database)
        seedEvent("event-1")
    }

    @Test
    fun `incrementUses fails when invitation code does not exist`() {
        val result = repository.incrementUses("missing-code")

        assertNotFound(result, "Invitation not found: missing-code")
    }

    @Test
    fun `deleteById fails when invitation does not exist`() {
        val result = repository.deleteById("missing-invitation")

        assertNotFound(result, "Invitation not found: missing-invitation")
    }

    @Test
    fun `incrementUses updates existing invitation`() {
        repository.createInvitation(invitation()).getOrThrow()

        val result = repository.incrementUses("invite-code")

        assertTrue(result.isSuccess)
        assertEquals(1, repository.getByCode("invite-code")?.currentUses)
    }

    @Test
    fun `getByCodeResult returns existing invitation without hiding success state`() {
        repository.createInvitation(invitation()).getOrThrow()

        val result = repository.getByCodeResult("invite-code")

        assertTrue(result.isSuccess)
        assertEquals("invitation-1", result.getOrThrow()?.id)
    }

    @Test
    fun `getByCodeResult returns success null for missing invitation`() {
        val result = repository.getByCodeResult("missing-code")

        assertTrue(result.isSuccess)
        assertNull(result.getOrThrow())
    }

    @Test
    fun `getByCodeResult returns failure when database lookup fails`() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        WakeveDb.Schema.create(driver)
        val failingRepository = InvitationRepository(WakeveDb(driver))
        driver.close()

        val result = failingRepository.getByCodeResult("invite-code")

        assertFalse(result.isSuccess)
    }

    @Test
    fun `deleteById removes existing invitation`() {
        repository.createInvitation(invitation()).getOrThrow()

        val result = repository.deleteById("invitation-1")

        assertTrue(result.isSuccess)
        assertNull(repository.getById("invitation-1"))
    }

    private fun assertNotFound(result: Result<*>, expectedMessage: String) {
        assertFalse(result.isSuccess)
        val error = assertIs<IllegalArgumentException>(result.exceptionOrNull())
        assertContains(error.message.orEmpty(), expectedMessage)
    }

    private fun invitation(): Invitation = Invitation(
        id = "invitation-1",
        code = "invite-code",
        eventId = "event-1",
        createdBy = "organizer-1",
        expiresAt = null,
        maxUses = null,
        currentUses = 0,
        createdAt = "2026-06-20T10:00:00Z"
    )

    private fun seedEvent(eventId: String) {
        database.eventQueries.insertEvent(
            id = eventId,
            organizerId = "organizer-1",
            title = "Event",
            description = "Description",
            status = "POLLING",
            deadline = "2026-06-30T00:00:00Z",
            createdAt = "2026-06-20T00:00:00Z",
            updatedAt = "2026-06-20T00:00:00Z",
            version = 1,
            eventType = "OTHER",
            eventTypeCustom = null,
            minParticipants = null,
            maxParticipants = null,
            expectedParticipants = null,
            isSample = 0L
        )
    }
}
