package com.guyghost.wakeve.auth.core.logic

import java.util.Base64
import java.nio.charset.StandardCharsets

internal actual fun decodeBase64ToString(input: String): String {
    return String(Base64.getDecoder().decode(input), StandardCharsets.UTF_8)
}

internal actual fun currentTimeMillis(): Long {
    return System.currentTimeMillis()
}
