package com.guyghost.wakeve.deeplink

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class DeepLinkLogRedactionTest {

    @Test
    fun redactDeepLinkForLog_redactsCustomInviteCode() {
        val result = redactDeepLinkForLog("wakeve://invite/INVITE-SECRET-123?source=push")

        assertEquals("wakeve://invite/<redacted>?source=push", result)
        assertFalse(result.contains("INVITE-SECRET-123"))
    }

    @Test
    fun redactDeepLinkForLog_redactsUniversalInviteCode() {
        val result = redactDeepLinkForLog("https://wakeve.app/invite/INVITE-SECRET-123#join")

        assertEquals("https://wakeve.app/invite/<redacted>#join", result)
        assertFalse(result.contains("INVITE-SECRET-123"))
    }

    @Test
    fun redactDeepLinkForLog_keepsNonInviteDeepLinksReadable() {
        val result = redactDeepLinkForLog("wakeve://event/event-123/details?tab=comments")

        assertEquals("wakeve://event/event-123/details?tab=comments", result)
    }

    @Test
    fun redactDeepLinkForLog_redactsAppleAuthSecrets() {
        val result = redactDeepLinkForLog(
            "wakeve://apple-auth-callback?code=AUTH-CODE-123&state=STATE-456&user={email:test@example.com}"
        )

        assertEquals(
            "wakeve://apple-auth-callback?code=<redacted>&state=<redacted>&user=<redacted>",
            result
        )
        assertFalse(result.contains("AUTH-CODE-123"))
        assertFalse(result.contains("STATE-456"))
        assertFalse(result.contains("test@example.com"))
    }
}
