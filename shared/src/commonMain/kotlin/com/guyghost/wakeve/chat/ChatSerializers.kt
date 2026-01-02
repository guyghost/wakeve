package com.guyghost.wakeve.chat

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Custom serializer for CommentSection enum.
 * Handles null values and enum serialization/deserialization.
 */
object CommentSectionSerializer : KSerializer<CommentSection?> {
    override val descriptor = PrimitiveSerialDescriptor("CommentSection", PrimitiveKind.STRING)
    
    override fun serialize(encoder: Encoder, value: CommentSection?) {
        encoder.encodeString(value?.name ?: "NULL")
    }
    
    override fun deserialize(decoder: Decoder): CommentSection? {
        val name = decoder.decodeString()
        return if (name == "NULL") null else CommentSection.valueOf(name)
    }
}

/**
 * Custom serializer for MessageStatus enum.
 */
object MessageStatusSerializer : KSerializer<MessageStatus> {
    override val descriptor = PrimitiveSerialDescriptor("MessageStatus", PrimitiveKind.STRING)
    
    override fun serialize(encoder: Encoder, value: MessageStatus) {
        encoder.encodeString(value.name)
    }
    
    override fun deserialize(decoder: Decoder): MessageStatus {
        return MessageStatus.valueOf(decoder.decodeString())
    }
}
