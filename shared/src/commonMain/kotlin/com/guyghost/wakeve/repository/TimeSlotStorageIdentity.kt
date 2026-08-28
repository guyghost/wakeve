package com.guyghost.wakeve.repository

/**
 * Canonical, reversible storage identity for a logical event time slot.
 *
 * The length is the UTF-8 byte length, not the Kotlin character count. Encoding both
 * components as lowercase hexadecimal makes the representation delimiter-safe and
 * locale-independent.
 */
object TimeSlotStorageIdentity {
    private const val PREFIX = "slot:v1|"

    data class LogicalIdentity(
        val eventId: String,
        val logicalSlotId: String
    )

    fun physicalId(eventId: String, logicalSlotId: String): String {
        requireValid(eventId)
        requireValid(logicalSlotId)
        return buildString {
            append(PREFIX)
            append(field(eventId))
            append('|')
            append(field(logicalSlotId))
        }
    }

    fun decode(physicalSlotId: String): LogicalIdentity? {
        if (!physicalSlotId.startsWith(PREFIX)) return null
        val fields = physicalSlotId.removePrefix(PREFIX).split('|')
        if (fields.size != 2) return null
        val eventId = decodeField(fields[0]) ?: return null
        val logicalSlotId = decodeField(fields[1]) ?: return null
        return runCatching {
            requireValid(eventId)
            requireValid(logicalSlotId)
            LogicalIdentity(eventId, logicalSlotId)
        }.getOrNull()
    }

    fun logicalId(eventId: String, physicalSlotId: String): String? {
        val decoded = decode(physicalSlotId) ?: return null
        return decoded.logicalSlotId.takeIf { decoded.eventId == eventId }
    }

    private fun field(value: String): String {
        val bytes = value.encodeToByteArray()
        return "${bytes.size}:${bytes.joinToString(separator = "") { byte ->
            byte.toUByte().toString(16).padStart(2, '0')
        }}"
    }

    private fun decodeField(field: String): String? {
        val separator = field.indexOf(':')
        if (separator <= 0) return null
        val expectedSize = field.substring(0, separator).toIntOrNull() ?: return null
        if (expectedSize < 1) return null
        val hex = field.substring(separator + 1)
        if (hex.length != expectedSize * 2 || hex.any { it !in "0123456789abcdef" }) return null
        val bytes = ByteArray(expectedSize) { index ->
            hex.substring(index * 2, index * 2 + 2).toInt(16).toByte()
        }
        return runCatching { bytes.decodeToString(throwOnInvalidSequence = true) }.getOrNull()
    }

    private fun requireValid(value: String) {
        require(value.isNotBlank() && value.trim() == value) { "INVALID_IDENTIFIER" }
        // Kotlin strings can contain isolated UTF-16 surrogates. Reject them instead of
        // letting the UTF-8 encoder replace them and destroy injectivity.
        var index = 0
        while (index < value.length) {
            val unit = value[index]
            when {
                unit.isHighSurrogate() -> {
                    require(index + 1 < value.length && value[index + 1].isLowSurrogate()) {
                        "INVALID_IDENTIFIER"
                    }
                    index += 2
                }
                unit.isLowSurrogate() -> error("INVALID_IDENTIFIER")
                else -> index += 1
            }
        }
    }
}
