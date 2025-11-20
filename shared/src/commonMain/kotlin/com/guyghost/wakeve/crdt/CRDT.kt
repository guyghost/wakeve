package com.guyghost.wakeve.crdt

/**
 * Base interface for Conflict-Free Replicated Data Types
 */
interface CRDT<T> {
    /**
     * Merge this CRDT with another instance
     */
    fun merge(other: CRDT<T>): CRDT<T>

    /**
     * Get the current value
     */
    fun value(): T

    /**
     * Check if this CRDT is equal to another
     */
    fun equals(other: CRDT<T>): Boolean
}

/**
 * Last-Write-Wins Register CRDT
 */
data class LWWRegister<T>(
    val value: T,
    val timestamp: Long,
    val nodeId: String
) : CRDT<T> {

    override fun merge(other: CRDT<T>): CRDT<T> {
        if (other !is LWWRegister<T>) return this

        return if (this.timestamp > other.timestamp ||
                   (this.timestamp == other.timestamp && this.nodeId > other.nodeId)) {
            this
        } else {
            other
        }
    }

    override fun value(): T = value

    override fun equals(other: CRDT<T>): Boolean {
        if (other !is LWWRegister<T>) return false
        return this.value == other.value &&
               this.timestamp == other.timestamp &&
               this.nodeId == other.nodeId
    }
}

/**
 * Grow-Only Set CRDT
 */
class GSet<T>(
    private val elements: MutableSet<T> = mutableSetOf()
) : CRDT<Set<T>> {

    fun add(element: T) {
        elements.add(element)
    }

    override fun merge(other: CRDT<Set<T>>): CRDT<Set<T>> {
        if (other !is GSet<T>) return this

        val merged = GSet<T>()
        merged.elements.addAll(this.elements)
        merged.elements.addAll(other.elements)
        return merged
    }

    override fun value(): Set<T> = elements.toSet()

    override fun equals(other: CRDT<Set<T>>): Boolean {
        if (other !is GSet<T>) return false
        return this.elements == other.elements
    }
}

/**
 * Positive-Negative Counter CRDT
 */
class PNCounter(
    private val increments: MutableMap<String, Long> = mutableMapOf(),
    private val decrements: MutableMap<String, Long> = mutableMapOf()
) : CRDT<Long> {

    fun increment(nodeId: String, amount: Long = 1) {
        increments[nodeId] = increments.getOrDefault(nodeId, 0) + amount
    }

    fun decrement(nodeId: String, amount: Long = 1) {
        decrements[nodeId] = decrements.getOrDefault(nodeId, 0) + amount
    }

    override fun merge(other: CRDT<Long>): CRDT<Long> {
        if (other !is PNCounter) return this

        val merged = PNCounter()
        // Merge increments
        val allNodes = (increments.keys + other.increments.keys).toSet()
        for (node in allNodes) {
            merged.increments[node] = maxOf(
                increments.getOrDefault(node, 0),
                other.increments.getOrDefault(node, 0)
            )
        }
        // Merge decrements
        val allDecNodes = (decrements.keys + other.decrements.keys).toSet()
        for (node in allDecNodes) {
            merged.decrements[node] = maxOf(
                decrements.getOrDefault(node, 0),
                other.decrements.getOrDefault(node, 0)
            )
        }
        return merged
    }

    override fun value(): Long {
        val totalIncrements = increments.values.sum()
        val totalDecrements = decrements.values.sum()
        return totalIncrements - totalDecrements
    }

    override fun equals(other: CRDT<Long>): Boolean {
        if (other !is PNCounter) return false
        return this.increments == other.increments &&
               this.decrements == other.decrements
    }
}</content>
<parameter name="filePath">shared/src/commonMain/kotlin/com/guyghost/wakeve/crdt/CRDT.kt