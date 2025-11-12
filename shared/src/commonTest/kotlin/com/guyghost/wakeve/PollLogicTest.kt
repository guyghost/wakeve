package com.guyghost.wakeve

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import com.guyghost.wakeve.models.Poll
import com.guyghost.wakeve.models.TimeSlot
import com.guyghost.wakeve.models.Vote

class PollLogicTest {

    private val slot1 = TimeSlot("slot-1", "2025-12-01T10:00:00Z", "2025-12-01T12:00:00Z", "UTC")
    private val slot2 = TimeSlot("slot-2", "2025-12-02T10:00:00Z", "2025-12-02T12:00:00Z", "UTC")
    private val slot3 = TimeSlot("slot-3", "2025-12-03T10:00:00Z", "2025-12-03T12:00:00Z", "UTC")

    @Test
    fun calculateBestSlotWithYesMajority() {
        val votes = mapOf(
            "p1" to mapOf("slot-1" to Vote.YES, "slot-2" to Vote.NO),
            "p2" to mapOf("slot-1" to Vote.YES, "slot-2" to Vote.MAYBE),
            "p3" to mapOf("slot-1" to Vote.YES, "slot-2" to Vote.NO)
        )
        val poll = Poll("poll-1", "event-1", votes)
        
        val bestSlot = PollLogic.calculateBestSlot(poll, listOf(slot1, slot2))
        
        assertNotNull(bestSlot)
        assertEquals("slot-1", bestSlot.id)
    }

    @Test
    fun calculateBestSlotWithMixedVotes() {
        val votes = mapOf(
            "p1" to mapOf("slot-1" to Vote.YES, "slot-2" to Vote.MAYBE),
            "p2" to mapOf("slot-1" to Vote.MAYBE, "slot-2" to Vote.YES),
            "p3" to mapOf("slot-1" to Vote.MAYBE, "slot-2" to Vote.YES)
        )
        val poll = Poll("poll-1", "event-1", votes)
        
        val bestSlot = PollLogic.calculateBestSlot(poll, listOf(slot1, slot2))
        
        assertNotNull(bestSlot)
        // slot1: 2+1+1 = 4, slot2: 1+2+2 = 5 -> slot2 should win
        assertEquals("slot-2", bestSlot.id)
    }

    @Test
    fun getSlotScoresBreakdown() {
        val votes = mapOf(
            "p1" to mapOf("slot-1" to Vote.YES, "slot-2" to Vote.NO),
            "p2" to mapOf("slot-1" to Vote.YES, "slot-2" to Vote.MAYBE),
            "p3" to mapOf("slot-1" to Vote.MAYBE, "slot-2" to Vote.NO)
        )
        val poll = Poll("poll-1", "event-1", votes)
        
        val scores = PollLogic.getSlotScores(poll, listOf(slot1, slot2))
        
        assertEquals(2, scores.size)
        
        val slot1Score = scores.find { it.slotId == "slot-1" }
        assertNotNull(slot1Score)
        assertEquals(2, slot1Score.yesCount)  // p1, p2
        assertEquals(1, slot1Score.maybeCount)  // p3
        assertEquals(0, slot1Score.noCount)
        assertEquals(5, slot1Score.totalScore)  // 2*2 + 1*1 = 5
        
        val slot2Score = scores.find { it.slotId == "slot-2" }
        assertNotNull(slot2Score)
        assertEquals(0, slot2Score.yesCount)
        assertEquals(1, slot2Score.maybeCount)  // p2
        assertEquals(2, slot2Score.noCount)  // p1, p3
        assertEquals(-1, slot2Score.totalScore)  // 1*1 - 2*1 = -1
    }

    @Test
    fun getBestSlotWithScoreDetails() {
        val votes = mapOf(
            "p1" to mapOf("slot-1" to Vote.YES, "slot-2" to Vote.MAYBE, "slot-3" to Vote.NO),
            "p2" to mapOf("slot-1" to Vote.YES, "slot-2" to Vote.YES, "slot-3" to Vote.MAYBE)
        )
        val poll = Poll("poll-1", "event-1", votes)
        
        val result = PollLogic.getBestSlotWithScore(poll, listOf(slot1, slot2, slot3))
        
        assertNotNull(result)
        val (bestSlot, score) = result
        // slot-1: p1=YES (2) + p2=YES (2) = 4 (BEST)
        // slot-2: p1=MAYBE (1) + p2=YES (2) = 3
        // slot-3: p1=NO (-1) + p2=MAYBE (1) = 0
        assertEquals("slot-1", bestSlot.id)
        assertEquals(2, score.yesCount)  // p1, p2
        assertEquals(0, score.maybeCount)
        assertEquals(0, score.noCount)
        assertEquals(4, score.totalScore)  // 2*2 = 4
    }

    @Test
    fun emptySlotsList() {
        val poll = Poll("poll-1", "event-1", emptyMap())
        
        val bestSlot = PollLogic.calculateBestSlot(poll, emptyList())
        
        assertNull(bestSlot)
    }

    @Test
    fun allNegativeVotes() {
        val votes = mapOf(
            "p1" to mapOf("slot-1" to Vote.NO),
            "p2" to mapOf("slot-1" to Vote.NO)
        )
        val poll = Poll("poll-1", "event-1", votes)
        
        val bestSlot = PollLogic.calculateBestSlot(poll, listOf(slot1))
        
        assertNotNull(bestSlot)
        assertEquals("slot-1", bestSlot.id)
    }
}
