package com.guyghost.wakeve

object PollLogic {
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
}