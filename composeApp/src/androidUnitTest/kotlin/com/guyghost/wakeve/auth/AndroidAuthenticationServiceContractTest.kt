package com.guyghost.wakeve.auth

import kotlin.test.Test
import kotlin.test.assertEquals

class AndroidAuthenticationServiceContractTest {
    @Test
    fun resolveAndroidAuthServerBaseUrl_usesProductionDefaultWhenBlank() {
        val result = resolveAndroidAuthServerBaseUrl("  ")

        assertEquals("https://api.wakeve.app", result)
    }

    @Test
    fun resolveAndroidAuthServerBaseUrl_keepsConfiguredServerUrl() {
        val result = resolveAndroidAuthServerBaseUrl("http://10.0.2.2:8080")

        assertEquals("http://10.0.2.2:8080", result)
    }

    @Test
    fun resolveAndroidAuthServerBaseUrl_trimsTrailingSlash() {
        val result = resolveAndroidAuthServerBaseUrl("https://api.wakeve.app/")

        assertEquals("https://api.wakeve.app", result)
    }

    @Test
    fun resolveAndroidAuthServerBaseUrl_removesApiSuffix() {
        val result = resolveAndroidAuthServerBaseUrl("https://api.wakeve.app/api")

        assertEquals("https://api.wakeve.app", result)
    }
}
