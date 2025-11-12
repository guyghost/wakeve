package com.guyghost.wakeve

object PollLogic {
    data class SlotScore(
        val slotId: String,
        val yesCount: Int,
        val maybeCount: Int,
        val noCount: Int,
        val totalScore: Int
    )

    fun calculateBestSlot(poll: Poll, slots: List<TimeSlot>): TimeSlot? {
        if (slots.isEmpty()) return null

        val scores = slots.associateWith { slot ->
            poll.votes.values.sumOf { participantVotes ->
                when (participantVotes[slot.id]) {
                    Vote.YES -> 2
                    Vote.MAYBE -> 1
                    Vote.NO -> -1
                    null -> 0
                }
            }
        }

        return scores.maxByOrNull { it.value }?.key
    }

    fun getSlotScores(poll: Poll, slots: List<TimeSlot>): List<SlotScore> {
        return slots.map { slot ->
            val scores = poll.votes.values.mapNotNull { participantVotes ->
                participantVotes[slot.id]
            }
            
            val yesCount = scores.count { it == Vote.YES }
            val maybeCount = scores.count { it == Vote.MAYBE }
            val noCount = scores.count { it == Vote.NO }
            val totalScore = yesCount * 2 + maybeCount * 1 - noCount * 1
            
            SlotScore(
                slotId = slot.id,
                yesCount = yesCount,
                maybeCount = maybeCount,
                noCount = noCount,
                totalScore = totalScore
            )
        }
    }

    fun getBestSlotWithScore(poll: Poll, slots: List<TimeSlot>): Pair<TimeSlot, SlotScore>? {
        if (slots.isEmpty()) return null

        val scores = getSlotScores(poll, slots)
        val bestScore = scores.maxByOrNull { it.totalScore } ?: return null
        val bestSlot = slots.find { it.id == bestScore.slotId } ?: return null
        
        return bestSlot to bestScore
    }
}