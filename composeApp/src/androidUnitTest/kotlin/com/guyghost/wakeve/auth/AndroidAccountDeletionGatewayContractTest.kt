package com.guyghost.wakeve.auth

import org.junit.Test
import kotlin.test.assertContains
import java.io.File

class AndroidAccountDeletionGatewayContractTest {
    @Test
    fun androidAccountDeletionGatewayCallsStableDeleteRoute() {
        val source = projectFile(
            "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/auth/AndroidAccountDeletionGateway.kt"
        ).readText()

        assertContains(source, "/api/user/delete")
        assertContains(source, "Bearer")
        assertContains(source, "AccountDeletionResponse")
    }

    private fun projectFile(relativePath: String): File {
        val root = generateSequence(File(System.getProperty("user.dir")).absoluteFile) { it.parentFile }
            .first { File(it, relativePath).exists() }
        return File(root, relativePath)
    }
}
