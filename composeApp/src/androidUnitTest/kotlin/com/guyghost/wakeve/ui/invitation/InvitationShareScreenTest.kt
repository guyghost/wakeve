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
}
