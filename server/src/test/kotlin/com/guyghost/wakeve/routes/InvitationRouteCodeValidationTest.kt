package com.guyghost.wakeve.routes

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class InvitationRouteCodeValidationTest {
    @Test
    fun normalizeInvitationRouteCodeTrimsValidCodes() {
        assertEquals("ORGRED12", normalizeInvitationRouteCode("  ORGRED12  "))
    }

    @Test
    fun normalizeInvitationRouteCodeRejectsBlankCodes() {
        assertNull(normalizeInvitationRouteCode(""))
        assertNull(normalizeInvitationRouteCode("   "))
        assertNull(normalizeInvitationRouteCode(null))
    }

    @Test
    fun normalizeInvitationRouteCodeRejectsPathAndQueryInjection() {
        assertNull(normalizeInvitationRouteCode("ORGRED12/other"))
        assertNull(normalizeInvitationRouteCode("ORGRED12?admin=true"))
        assertNull(normalizeInvitationRouteCode("ORGRED12#fragment"))
    }

    @Test
    fun normalizeInvitationRouteCodeRejectsEncodedDelimiters() {
        assertNull(normalizeInvitationRouteCode("ORGRED12%2Fother"))
        assertNull(normalizeInvitationRouteCode("ORGRED12%3Fadmin=true"))
        assertNull(normalizeInvitationRouteCode("ORGRED12%23fragment"))
    }

    @Test
    fun normalizeInvitationEventRouteIdTrimsValidIds() {
        assertEquals("event-org-access", normalizeInvitationEventRouteId("  event-org-access  "))
    }

    @Test
    fun normalizeInvitationEventRouteIdRejectsBlankIds() {
        assertNull(normalizeInvitationEventRouteId(""))
        assertNull(normalizeInvitationEventRouteId("   "))
        assertNull(normalizeInvitationEventRouteId(null))
    }

    @Test
    fun normalizeInvitationEventRouteIdRejectsPathAndQueryInjection() {
        assertNull(normalizeInvitationEventRouteId("event-org-access/other"))
        assertNull(normalizeInvitationEventRouteId("event-org-access?invite=true"))
        assertNull(normalizeInvitationEventRouteId("event-org-access#fragment"))
    }

    @Test
    fun normalizeInvitationEventRouteIdRejectsEncodedDelimiters() {
        assertNull(normalizeInvitationEventRouteId("event-org-access%2Fother"))
        assertNull(normalizeInvitationEventRouteId("event-org-access%3Finvite=true"))
        assertNull(normalizeInvitationEventRouteId("event-org-access%23fragment"))
    }
}
