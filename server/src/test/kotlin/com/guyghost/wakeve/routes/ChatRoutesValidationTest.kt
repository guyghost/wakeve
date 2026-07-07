package com.guyghost.wakeve.routes

import kotlin.test.Test
import kotlin.test.assertEquals

class ChatRoutesValidationTest {
    @Test
    fun parseChatMessageLimit_defaultsMissingOrInvalidLimit() {
        assertEquals(100, parseChatMessageLimit(null))
        assertEquals(100, parseChatMessageLimit("not-a-number"))
    }

    @Test
    fun parseChatMessageLimit_acceptsTrimmedValidLimit() {
        assertEquals(25, parseChatMessageLimit(" 25 "))
    }

    @Test
    fun parseChatMessageLimit_clampsOutsideBounds() {
        assertEquals(1, parseChatMessageLimit("0"))
        assertEquals(1, parseChatMessageLimit("-10"))
        assertEquals(100, parseChatMessageLimit("100000"))
    }

    @Test
    fun parseChatMessageOffset_defaultsAndClampsOutsideBounds() {
        assertEquals(0, parseChatMessageOffset(null))
        assertEquals(0, parseChatMessageOffset("not-a-number"))
        assertEquals(0, parseChatMessageOffset("-10"))
        assertEquals(15, parseChatMessageOffset(" 15 "))
        assertEquals(10_000, parseChatMessageOffset("100000"))
    }
}
