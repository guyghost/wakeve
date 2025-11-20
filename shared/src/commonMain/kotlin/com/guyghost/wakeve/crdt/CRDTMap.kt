package com.guyghost.wakeve.crdt

/**
 * CRDT Map using LWW Registers for each key
 */
class CRDTMap<K, V>(
    private val registers: MutableMap<K, LWWRegister<V>> = mutableMapOf()
) : CRDT<Map<K, V>> {

    fun put(key: K, value: V, timestamp: Long, nodeId: String) {
        registers[key] = LWWRegister(value, timestamp, nodeId)
    }

    fun get(key: K): V? = registers[key]?.value

    override fun merge(other: CRDT<Map<K, V>>): CRDT<Map<K, V>> {
        if (other !is CRDTMap<K, V>) return this

        val merged = CRDTMap<K, V>()
        val allKeys = (registers.keys + other.registers.keys).toSet()

        for (key in allKeys) {
            val thisReg = registers[key]
            val otherReg = other.registers[key]

            when {
                thisReg != null && otherReg != null -> {
                    merged.registers[key] = thisReg.merge(otherReg) as LWWRegister<V>
                }
                thisReg != null -> {
                    merged.registers[key] = thisReg
                }
                otherReg != null -> {
                    merged.registers[key] = otherReg
                }
            }
        }

        return merged
    }

    override fun value(): Map<K, V> {
        return registers.mapValues { it.value.value }
    }

    override fun equals(other: CRDT<Map<K, V>>): Boolean {
        if (other !is CRDTMap<K, V>) return false
        return this.registers == other.registers
    }
}</content>
<parameter name="filePath">shared/src/commonMain/kotlin/com/guyghost/wakeve/crdt/CRDTMap.kt