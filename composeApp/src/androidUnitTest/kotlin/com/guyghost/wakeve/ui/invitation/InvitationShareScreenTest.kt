package com.guyghost.wakeve.ui.invitation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class InvitationShareScreenTest {

    @Test
    fun createInviteUrl_trimsValidCode() {
        val result = createInviteUrl(" invite-code-123 ")

        assertEquals("https://wakeve.app/invite/invite-code-123", result)
    }

    @Test
    fun createInviteUrl_returnsNullForMissingCode() {
        assertNull(createInviteUrl(null))
        assertNull(createInviteUrl(" "))
    }

    @Test
    fun createInviteUrl_rejectsInjectedPathAndQueryDelimiters() {
        assertNull(createInviteUrl("invite/code"))
        assertNull(createInviteUrl("invite?next=evil"))
        assertNull(createInviteUrl("invite#fragment"))
    }

    @Test
    fun createInviteUrl_rejectsEncodedPathAndQueryDelimiters() {
        assertNull(createInviteUrl("invite%2Fcode"))
        assertNull(createInviteUrl("invite%3Fnext=evil"))
        assertNull(createInviteUrl("invite%23fragment"))
    }

    @Test
    fun createInvitationShareContent_normalizesTitleAndTrustedUrl() {
        val content = createInvitationShareContent(
            eventTitle = "  Week-end   Biarritz  ",
            invitationCode = " invite-code-123 "
        )

        assertEquals(
            InvitationShareContent(
                eventTitle = "Week-end Biarritz",
                inviteUrl = "https://wakeve.app/invite/invite-code-123"
            ),
            content
        )
    }

    @Test
    fun createInvitationShareContent_returnsNullForBlankTitleOrInvalidCode() {
        assertNull(createInvitationShareContent(" ", "invite-code-123"))
        assertNull(createInvitationShareContent("Week-end", null))
        assertNull(createInvitationShareContent("Week-end", "invite/code"))
    }
}
