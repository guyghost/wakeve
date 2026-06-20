package com.guyghost.wakeve.notification

import com.guyghost.wakeve.deeplink.normalizeDeepLinkPathSegment
import com.guyghost.wakeve.deeplink.parseDeepLinkParts
import java.net.URI
import java.net.URLDecoder

internal fun resolveNotificationClickTarget(data: Map<String, String>): String? {
    val deepLink = data["deepLink"]?.trim()
    if (!deepLink.isNullOrBlank() && isSupportedNotificationClickDeepLink(deepLink)) {
        return deepLink
    }

    return normalizeDeepLinkPathSegment(data["eventId"] ?: data["event_id"])
}

internal fun isDeepLinkClickTarget(target: String): Boolean {
    return isSupportedNotificationClickDeepLink(target.trim())
}

private fun isSupportedNotificationClickDeepLink(rawDeepLink: String): Boolean {
    return try {
        val uri = URI(rawDeepLink)
        if (hasUnsupportedNotificationDeepLinkComponents(uri)) {
            return false
        }

        parseDeepLinkParts(
            scheme = uri.scheme,
            host = uri.host,
            pathSegments = uri.path
                ?.split("/")
                ?.filter { it.isNotBlank() }
                .orEmpty(),
            queryParameters = parseQueryParameters(uri.rawQuery)
        ) != null
    } catch (e: Exception) {
        false
    }
}

private fun hasUnsupportedNotificationDeepLinkComponents(uri: URI): Boolean {
    return uri.fragment != null || uri.userInfo != null || uri.port != -1
}

private fun parseQueryParameters(rawQuery: String?): Map<String, String> {
    return rawQuery
        ?.split("&")
        ?.mapNotNull { pair ->
            val index = pair.indexOf("=")
            if (index <= 0) {
                null
            } else {
                decodeQueryComponent(pair.substring(0, index)) to
                    decodeQueryComponent(pair.substring(index + 1))
            }
        }
        ?.toMap()
        .orEmpty()
}

private fun decodeQueryComponent(value: String): String =
    URLDecoder.decode(value, "UTF-8")
