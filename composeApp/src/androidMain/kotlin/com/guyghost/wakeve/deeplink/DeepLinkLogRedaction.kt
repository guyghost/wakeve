package com.guyghost.wakeve.deeplink

private val INVITE_DEEP_LINK_PATTERNS = listOf(
    Regex("""(?i)(wakeve://invite/)([^/?#]+)"""),
    Regex("""(?i)(https://wakeve\.app/invite/)([^/?#]+)""")
)

private val SENSITIVE_QUERY_PARAM_PATTERN =
    Regex("""(?i)([?&](?:code|state|id_token|access_token|refresh_token|token|user)=)([^&#]*)""")

internal fun redactDeepLinkForLog(rawValue: String?): String {
    val raw = rawValue ?: return "<null>"
    if (raw.isBlank()) return "<blank>"

    val inviteRedacted = INVITE_DEEP_LINK_PATTERNS.fold(raw) { current, pattern ->
        pattern.replace(current) { match ->
            "${match.groupValues[1]}<redacted>"
        }
    }

    return SENSITIVE_QUERY_PARAM_PATTERN.replace(inviteRedacted) { match ->
        "${match.groupValues[1]}<redacted>"
    }
}
