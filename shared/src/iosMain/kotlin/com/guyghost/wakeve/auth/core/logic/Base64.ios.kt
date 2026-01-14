package com.guyghost.wakeve.auth.core.logic

import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.nativeHeap
import platform.posix.memcpy
import kotlinx.datetime.Clock

internal actual fun decodeBase64ToString(input: String): String {
    // Decode Base64
    val decodedData = platform.Foundation.NSData.create(
        base64EncodedString = input,
        options = 0u
    ) ?: throw IllegalArgumentException("Invalid Base64 string")

    // Convert back to String
    val decodedString = NSString.create(
        data = decodedData,
        encoding = NSUTF8StringEncoding
    ) ?: throw IllegalArgumentException("Invalid UTF-8 data")

    return decodedString as String
}

internal actual fun currentTimeMillis(): Long {
    return Clock.System.now().toEpochMilliseconds()
}
