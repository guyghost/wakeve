package com.guyghost.wakeve.notification

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NotificationClickTargetTest {

    @Test
    fun resolveNotificationClickTarget_prefersSupportedDeepLinkOverEventId() {
        val result = resolveNotificationClickTarget(
            mapOf(
                "deepLink" to " wakeve://event/event-123/details?tab=comments ",
                "eventId" to "event-fallback"
            )
        )

        assertEquals("wakeve://event/event-123/details?tab=comments", result)
    }

    @Test
    fun resolveNotificationClickTarget_fallsBackToTrimmedEventId() {
        val result = resolveNotificationClickTarget(
            mapOf("event_id" to " event-123 ")
        )

        assertEquals("event-123", result)
    }

    @Test
    fun resolveNotificationClickTarget_rejectsUnsupportedDeepLinkAndUnsafeEventId() {
        val result = resolveNotificationClickTarget(
            mapOf(
                "deepLink" to "https://evil.example/event/event-123",
                "eventId" to "event/123"
            )
        )

        assertNull(result)
    }

    @Test
    fun resolveNotificationClickTarget_fallsBackWhenWakeveDeepLinkRouteIsUnsupported() {
        val result = resolveNotificationClickTarget(
            mapOf(
                "deepLink" to "wakeve://event/event-123/cancel",
                "eventId" to "event-fallback"
            )
        )

        assertEquals("event-fallback", result)
    }

    @Test
    fun resolveNotificationClickTarget_acceptsUniversalInviteDeepLink() {
        val result = resolveNotificationClickTarget(
            mapOf("deepLink" to "https://wakeve.app/invite/INVITE123")
        )

        assertEquals("https://wakeve.app/invite/INVITE123", result)
    }

    @Test
    fun resolveNotificationClickTarget_rejectsDeepLinkFragmentsAndFallsBack() {
        val result = resolveNotificationClickTarget(
            mapOf(
                "deepLink" to "wakeve://event/event-123/details#comments",
                "eventId" to "event-fallback"
            )
        )

        assertEquals("event-fallback", result)
    }

    @Test
    fun resolveNotificationClickTarget_rejectsAmbiguousUniversalLinkAuthority() {
        val result = resolveNotificationClickTarget(
            mapOf(
                "deepLink" to "https://user@wakeve.app/invite/INVITE123",
                "eventId" to "event-fallback"
            )
        )

        assertEquals("event-fallback", result)
    }

    @Test
    fun resolveNotificationClickTarget_rejectsPlaintextUniversalInviteDeepLink() {
        val result = resolveNotificationClickTarget(
            mapOf("deepLink" to "http://wakeve.app/invite/INVITE123")
        )

        assertNull(result)
    }

    @Test
    fun isDeepLinkClickTarget_detectsWakeveAndUniversalLinksOnly() {
        assertTrue(isDeepLinkClickTarget("wakeve://event/event-123/details"))
        assertTrue(isDeepLinkClickTarget("https://wakeve.app/invite/INVITE123"))
        assertFalse(isDeepLinkClickTarget("wakeve://event/event-123/cancel"))
        assertFalse(isDeepLinkClickTarget("wakeve://event/event-123/details#comments"))
        assertFalse(isDeepLinkClickTarget("wakeve://unknown/event-123"))
        assertFalse(isDeepLinkClickTarget("https://wakeve.app/event/event-123"))
        assertFalse(isDeepLinkClickTarget("http://wakeve.app/invite/INVITE123"))
        assertFalse(isDeepLinkClickTarget("https://wakeve.app:443/invite/INVITE123"))
        assertFalse(isDeepLinkClickTarget("https://user@wakeve.app/invite/INVITE123"))
        assertFalse(isDeepLinkClickTarget("event-123"))
        assertFalse(isDeepLinkClickTarget("https://evil.example/invite/INVITE123"))
    }
}
