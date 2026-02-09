package com.guyghost.wakeve.auth.core.logic

import android.util.Base64

internal actual fun decodeBase64ToString(input: String): String {
    val decodedBytes = runCatching {
        val base64Class = Class.forName("java.util.Base64")
        val decoder = base64Class.getMethod("getDecoder").invoke(null)
        decoder.javaClass.getMethod("decode", String::class.java).invoke(decoder, input) as ByteArray
    }.getOrElse {
        Base64.decode(input, Base64.DEFAULT)
    }
    return String(decodedBytes, Charsets.UTF_8)
}

internal actual fun currentTimeMillis(): Long {
    return System.currentTimeMillis()
}
