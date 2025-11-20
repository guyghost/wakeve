package com.guyghost.wakeve.crdt

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Serializer for LWWRegister
 */
class LWWRegisterSerializer<T>(private val valueSerializer: KSerializer<T>) : KSerializer<LWWRegister<T>> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("LWWRegister") {
        element("value", valueSerializer.descriptor)
        element<Long>("timestamp")
        element<String>("nodeId")
    }

    override fun serialize(encoder: Encoder, value: LWWRegister<T>) {
        val composite = encoder.beginStructure(descriptor)
        composite.encodeSerializableElement(descriptor, 0, valueSerializer, value.value)
        composite.encodeLongElement(descriptor, 1, value.timestamp)
        composite.encodeStringElement(descriptor, 2, value.nodeId)
        composite.endStructure(descriptor)
    }

    override fun deserialize(decoder: Decoder): LWWRegister<T> {
        val composite = decoder.beginStructure(descriptor)
        val value = composite.decodeSerializableElement(descriptor, 0, valueSerializer)
        val timestamp = composite.decodeLongElement(descriptor, 1)
        val nodeId = composite.decodeStringElement(descriptor, 2)
        composite.endStructure(descriptor)
        return LWWRegister(value, timestamp, nodeId)
    }
}</content>
<parameter name="filePath">shared/src/commonMain/kotlin/com/guyghost/wakeve/crdt/CRDTSerialization.kt