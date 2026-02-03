package com.guyghost.wakeve.collaboration

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MentionParserTest {

    @Test
    fun testParseMentions_SingleMention() {
        val content = "Hello @alice, how are you?"
        val usernameMap = mapOf("alice" to "user_123")

        val result = MentionParser.parseMentions("comment_1", content, usernameMap)

        assertEquals(1, result.mentions.size)
        assertEquals("user_123", result.mentionedUserIds[0])
        assertEquals(6, result.mentions[0].startIndex)
        assertEquals(12, result.mentions[0].endIndex)
    }

    @Test
    fun testParseMentions_MultipleMentions() {
        val content = "@alice and @bob should meet at 3pm"
        val usernameMap = mapOf("alice" to "user_1", "bob" to "user_2")

        val result = MentionParser.parseMentions("comment_1", content, usernameMap)

        assertEquals(2, result.mentions.size)
        assertEquals("user_1", result.mentionedUserIds[0])
        assertEquals("user_2", result.mentionedUserIds[1])
    }

    @Test
    fun testParseMentions_NoMentions() {
        val content = "Hello everyone, let's meet at 3pm"
        val usernameMap = emptyMap<String, String>()

        val result = MentionParser.parseMentions("comment_1", content, usernameMap)

        assertEquals(0, result.mentions.size)
        assertEquals(0, result.mentionedUserIds.size)
    }

    @Test
    fun testParseMentions_DuplicateMentions() {
        val content = "@alice and @alice, please check this"
        val usernameMap = mapOf("alice" to "user_123")

        val result = MentionParser.parseMentions("comment_1", content, usernameMap)

        assertEquals(2, result.mentions.size) // Count duplicates
        assertEquals(listOf("user_123", "user_123"), result.mentionedUserIds)
    }

    @Test
    fun testParseMentions_UsernameWithUnderscore() {
        val content = "Hey @john_doe, check this out"
        val usernameMap = mapOf("john_doe" to "user_456")

        val result = MentionParser.parseMentions("comment_1", content, usernameMap)

        assertEquals(1, result.mentions.size)
        assertEquals("user_456", result.mentionedUserIds[0])
    }

    @Test
    fun testParseMentions_UsernameWithHyphen() {
        val content = "@user-123 should handle this"
        val usernameMap = mapOf("user-123" to "user_789")

        val result = MentionParser.parseMentions("comment_1", content, usernameMap)

        assertEquals(1, result.mentions.size)
        assertEquals("user_789", result.mentionedUserIds[0])
    }

    @Test
    fun testParseMentions_UnknownUsername() {
        val content = "Hey @unknown_user, what's up?"
        val usernameMap = mapOf("alice" to "user_123")

        val result = MentionParser.parseMentions("comment_1", content, usernameMap)

        assertEquals(1, result.mentions.size)
        assertEquals("unknown_user", result.mentionedUserIds[0]) // Use username directly
    }

    @Test
    fun testParseMentions_EmailAddress() {
        val content = "Email me at test@example.com, not @alice"
        val usernameMap = mapOf("alice" to "user_123")

        val result = MentionParser.parseMentions("comment_1", content, usernameMap)

        assertEquals(1, result.mentions.size)
        assertEquals("user_123", result.mentionedUserIds[0])
        assertEquals(content.indexOf("@alice"), result.mentions[0].startIndex)
    }

    @Test
    fun testExtractUsernames() {
        val content = "@alice and @bob are friends"

        val usernames = MentionParser.extractUsernames(content)

        assertEquals(listOf("@alice", "@bob"), usernames)
    }

    @Test
    fun testHighlightMentions() {
        val content = "Hello @alice and @bob"

        val highlighted = MentionParser.highlightMentions(content, "[@", "]")

        assertEquals("Hello [@alice] and [@bob]", highlighted)
    }

    @Test
    fun testStripMentions() {
        val content = "Hello @alice, check @bob"

        val stripped = MentionParser.stripMentions(content)

        assertEquals("Hello alice, check bob", stripped)
    }

    @Test
    fun testIsValidMention_Valid() {
        assertTrue(MentionParser.isValidMention("@alice"))
        assertTrue(MentionParser.isValidMention("@john_doe"))
        assertTrue(MentionParser.isValidMention("@user-123"))
    }

    @Test
    fun testIsValidMention_Invalid() {
        assertFalse(MentionParser.isValidMention("alice")) // Missing @
        assertFalse(MentionParser.isValidMention("@@alice")) // Double @
        assertFalse(MentionParser.isValidMention("@alice@bob.com")) // Email
        assertFalse(MentionParser.isValidMention("")) // Empty
    }
}
