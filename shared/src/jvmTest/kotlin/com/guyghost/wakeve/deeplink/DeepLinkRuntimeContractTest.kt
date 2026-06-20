package com.guyghost.wakeve.deeplink

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse

class DeepLinkRuntimeContractTest {
    @Test
    fun commonMainDoesNotShipMockDeepLinkHandler() {
        val source = projectFile("shared/src/commonMain/kotlin/com/guyghost/wakeve/deeplink/DeepLinkHandler.kt").readText()

        assertFalse(
            source.contains("MockDeepLinkHandler"),
            "MockDeepLinkHandler accepts all links and must stay in commonTest, not commonMain runtime code."
        )
    }

    private fun projectFile(relativePath: String): File {
        val root = generateSequence(File(System.getProperty("user.dir")).absoluteFile) { it.parentFile }
            .first { File(it, relativePath).exists() }
        return File(root, relativePath)
    }
}
