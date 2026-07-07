package com.guyghost.wakeve.deeplink

/**
 * Test double for deep link handling.
 */
class MockDeepLinkHandler : BaseDeepLinkHandler() {
    private val handledDeepLinks = mutableListOf<DeepLink>()

    override fun canHandleDeepLink(deepLink: DeepLink): Boolean = true

    override fun handleDefault(deepLink: DeepLink): Boolean {
        handledDeepLinks.add(deepLink)
        return true
    }

    fun getHandledDeepLinks(): List<DeepLink> = handledDeepLinks.toList()

    fun clearHandledDeepLinks() {
        handledDeepLinks.clear()
    }

    fun wasHandled(uri: String): Boolean {
        return handledDeepLinks.any { it.fullUri == uri }
    }
}
