package com.guyghost.wakeve.crdt

import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.Vote

/**
 * CRDT-based Event using CRDTMap for fields
 */
class CRDTEvent(
    private val fields: CRDTMap<String, Any> = CRDTMap()
) : CRDT<Event> {

    fun updateField(field: String, value: Any, timestamp: Long, nodeId: String) {
        fields.put(field, value, timestamp, nodeId)
    }

    fun getField(field: String): Any? = fields.get(field)

    override fun merge(other: CRDT<Event>): CRDT<Event> {
        if (other !is CRDTEvent) return this
        return CRDTEvent(fields.merge(other.fields) as CRDTMap<String, Any>)
    }

    override fun value(): Event {
        val map = fields.value()
        return Event(
            id = map["id"] as? String ?: "",
            title = map["title"] as? String ?: "",
            description = map["description"] as? String ?: "",
            organizerId = map["organizerId"] as? String ?: "",
            participants = map["participants"] as? List<String> ?: emptyList(),
            status = map["status"] as? EventStatus ?: EventStatus.DRAFT,
            finalDate = map["finalDate"] as? String,
            deadline = map["deadline"] as? String ?: "",
            createdAt = map["createdAt"] as? String ?: "",
            updatedAt = map["updatedAt"] as? String ?: ""
        )
    }

    override fun equals(other: CRDT<Event>): Boolean {
        if (other !is CRDTEvent) return false
        return this.fields.equals(other.fields)
    }
}

/**
 * CRDT-based Votes using CRDTMap of participant to list of votes
 */
class CRDTVotes(
    private val participantVotes: CRDTMap<String, List<Vote>> = CRDTMap()
) : CRDT<Map<String, List<Vote>>> {

    fun updateVotes(participantId: String, votes: List<Vote>, timestamp: Long, nodeId: String) {
        participantVotes.put(participantId, votes, timestamp, nodeId)
    }

    fun getVotes(participantId: String): List<Vote>? = participantVotes.get(participantId)

    override fun merge(other: CRDT<Map<String, List<Vote>>>): CRDT<Map<String, List<Vote>>> {
        if (other !is CRDTVotes) return this
        return CRDTVotes(participantVotes.merge(other.participantVotes) as CRDTMap<String, List<Vote>>)
    }

    override fun value(): Map<String, List<Vote>> = participantVotes.value()

    override fun equals(other: CRDT<Map<String, List<Vote>>>): Boolean {
        if (other !is CRDTVotes) return false
        return this.participantVotes.equals(other.participantVotes)
    }
}

/**
 * CRDT-based Participants using GSet
 */
class CRDTParticipants(
    private val participants: GSet<String> = GSet()
) : CRDT<Set<String>> {

    fun addParticipant(participantId: String) {
        participants.add(participantId)
    }

    override fun merge(other: CRDT<Set<String>>): CRDT<Set<String>> {
        if (other !is CRDTParticipants) return this
        return CRDTParticipants(participants.merge(other.participants) as GSet<String>)
    }

    override fun value(): Set<String> = participants.value()

    override fun equals(other: CRDT<Set<String>>): Boolean {
        if (other !is CRDTParticipants) return false
        return this.participants.equals(other.participants)
    }
}</content>
<parameter name="filePath">shared/src/commonMain/kotlin/com/guyghost/wakeve/crdt/CRDTModels.kt