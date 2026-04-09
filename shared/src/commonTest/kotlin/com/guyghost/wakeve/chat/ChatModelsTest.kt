package com.guyghost.wakeve.chat

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for ChatModels — message models, reactions,
 * file size formatting, and participant presence.
 */
class ChatModelsTest {

    // ========================================================================
    // ChatImageAttachment Tests
    // ========================================================================

    @Test
    fun `formattedSize shows bytes for small files`() {
        val attachment = ChatImageAttachment(uri = "file:///img.jpg", mimeType = "image/jpeg", sizeBytes = 512)
        assertEquals("512 B", attachment.formattedSize)
    }

    @Test
    fun `formattedSize shows KB for medium files`() {
        val attachment = ChatImageAttachment(uri = "file:///img.jpg", mimeType = "image/jpeg", sizeBytes = 2048)
        assertEquals("2 KB", attachment.formattedSize)
    }

    @Test
    fun `formattedSize shows MB for large files`() {
        val attachment = ChatImageAttachment(uri = "file:///img.jpg", mimeType = "image/jpeg", sizeBytes = 5 * 1024 * 1024)
        assertEquals("5 MB", attachment.formattedSize)
    }

    @Test
    fun `dimensions returns formatted string when width and height present`() {
        val attachment = ChatImageAttachment(
            uri = "file:///img.jpg", mimeType = "image/jpeg",
            sizeBytes = 100, width = 1920, height = 1080
        )
        assertEquals("1920×1080", attachment.dimensions)
    }

    @Test
    fun `dimensions returns null when dimensions missing`() {
        val attachment = ChatImageAttachment(
            uri = "file:///img.jpg", mimeType = "image/jpeg", sizeBytes = 100
        )
        assertNull(attachment.dimensions)
    }

    @Test
    fun `dimensions returns null with only width`() {
        val attachment = ChatImageAttachment(
            uri = "file:///img.jpg", mimeType = "image/jpeg",
            sizeBytes = 100, width = 1920
        )
        assertNull(attachment.dimensions)
    }

    // ========================================================================
    // ChatMessage Tests
    // ========================================================================

    private fun testMessage(
        senderId: String = "user-1",
        content: String = "Hello!",
        reactions: List<Reaction> = emptyList(),
        imageAttachment: ChatImageAttachment? = null,
        parentId: String? = null
    ) = ChatMessage(
        id = "msg-1",
        eventId = "event-1",
        senderId = senderId,
        senderName = "Alice",
        content = content,
        timestamp = "2026-01-01T00:00:00Z",
        reactions = reactions,
        imageAttachment = imageAttachment,
        parentId = parentId
    )

    @Test
    fun `isFromUser returns true for matching userId`() {
        val msg = testMessage(senderId = "user-1")
        assertTrue(msg.isFromUser("user-1"))
    }

    @Test
    fun `isFromUser returns false for different userId`() {
        val msg = testMessage(senderId = "user-1")
        assertFalse(msg.isFromUser("user-2"))
    }

    @Test
    fun `isImageMessage returns true when attachment present`() {
        val msg = testMessage(
            imageAttachment = ChatImageAttachment(uri = "file:///img.jpg", mimeType = "image/jpeg", sizeBytes = 100)
        )
        assertTrue(msg.isImageMessage)
    }

    @Test
    fun `isImageMessage returns false when no attachment`() {
        val msg = testMessage()
        assertFalse(msg.isImageMessage)
    }

    @Test
    fun `isTextMessage returns true for content without attachment`() {
        val msg = testMessage(content = "Hello!")
        assertTrue(msg.isTextMessage)
    }

    @Test
    fun `isTextMessage returns false with attachment`() {
        val msg = testMessage(
            content = "Photo",
            imageAttachment = ChatImageAttachment(uri = "file:///img.jpg", mimeType = "image/jpeg", sizeBytes = 100)
        )
        assertFalse(msg.isTextMessage)
    }

    @Test
    fun `isEmpty returns true for blank content without attachment`() {
        val msg = testMessage(content = "")
        assertTrue(msg.isEmpty)
    }

    @Test
    fun `isEmpty returns true for whitespace content without attachment`() {
        val msg = testMessage(content = "   ")
        assertTrue(msg.isEmpty)
    }

    @Test
    fun `isEmpty returns false for content with text`() {
        val msg = testMessage(content = "Hello")
        assertFalse(msg.isEmpty)
    }

    @Test
    fun `isEmpty returns false with attachment even without text`() {
        val msg = testMessage(
            content = "",
            imageAttachment = ChatImageAttachment(uri = "file:///img.jpg", mimeType = "image/jpeg", sizeBytes = 100)
        )
        assertFalse(msg.isEmpty)
    }

    // ========================================================================
    // Reaction Tests
    // ========================================================================

    @Test
    fun `groupedReactions groups by emoji`() {
        val reactions = listOf(
            Reaction("user-1", "❤️", "2026-01-01T00:00:00Z"),
            Reaction("user-2", "❤️", "2026-01-01T00:01:00Z"),
            Reaction("user-3", "👍", "2026-01-01T00:02:00Z")
        )
        val msg = testMessage(reactions = reactions)
        val grouped = msg.groupedReactions()

        assertEquals(2, grouped.size)
        assertEquals(2, grouped["❤️"]?.size)
        assertEquals(1, grouped["👍"]?.size)
    }

    @Test
    fun `groupedReactions returns empty map for no reactions`() {
        val msg = testMessage()
        assertTrue(msg.groupedReactions().isEmpty())
    }

    @Test
    fun `hasUserReacted returns true for matching reaction`() {
        val msg = testMessage(reactions = listOf(
            Reaction("user-1", "❤️", "2026-01-01T00:00:00Z")
        ))
        assertTrue(msg.hasUserReacted("user-1", "❤️"))
    }

    @Test
    fun `hasUserReacted returns false for wrong emoji`() {
        val msg = testMessage(reactions = listOf(
            Reaction("user-1", "❤️", "2026-01-01T00:00:00Z")
        ))
        assertFalse(msg.hasUserReacted("user-1", "👍"))
    }

    @Test
    fun `hasUserReacted returns false for wrong user`() {
        val msg = testMessage(reactions = listOf(
            Reaction("user-1", "❤️", "2026-01-01T00:00:00Z")
        ))
        assertFalse(msg.hasUserReacted("user-2", "❤️"))
    }

    // ========================================================================
    // MessageStatus Tests
    // ========================================================================

    @Test
    fun `MessageStatus has all expected values`() {
        val statuses = MessageStatus.entries
        assertEquals(4, statuses.size)
        assertTrue(statuses.contains(MessageStatus.SENT))
        assertTrue(statuses.contains(MessageStatus.DELIVERED))
        assertTrue(statuses.contains(MessageStatus.FAILED))
        assertTrue(statuses.contains(MessageStatus.READ))
    }

    // ========================================================================
    // CommentSection Tests
    // ========================================================================

    @Test
    fun `CommentSection has all expected values`() {
        val sections = CommentSection.entries
        assertEquals(6, sections.size)
        assertTrue(sections.contains(CommentSection.GENERAL))
        assertTrue(sections.contains(CommentSection.TRANSPORT))
        assertTrue(sections.contains(CommentSection.FOOD))
    }

    // ========================================================================
    // TypingIndicator Tests
    // ========================================================================

    @Test
    fun `TypingIndicator holds all fields`() {
        val indicator = TypingIndicator(
            userId = "user-1",
            userName = "Alice",
            chatId = "event-1",
            lastSeenTyping = "2026-01-01T00:00:00Z"
        )
        assertEquals("user-1", indicator.userId)
        assertEquals("Alice", indicator.userName)
        assertEquals("event-1", indicator.chatId)
    }

    // ========================================================================
    // ChatParticipant Tests
    // ========================================================================

    @Test
    fun `ChatParticipant defaults to offline`() {
        val participant = ChatParticipant(userId = "user-1", userName = "Alice")
        assertFalse(participant.isOnline)
        assertNull(participant.lastSeen)
    }

    @Test
    fun `ChatParticipant can be online`() {
        val participant = ChatParticipant(
            userId = "user-1", userName = "Alice",
            isOnline = true, lastSeen = "2026-01-01T00:00:00Z"
        )
        assertTrue(participant.isOnline)
        assertNotNull(participant.lastSeen)
    }

    // ========================================================================
    // Threaded Message Tests
    // ========================================================================

    @Test
    fun `message with parentId is a reply`() {
        val reply = testMessage(parentId = "msg-parent")
        assertEquals("msg-parent", reply.parentId)
    }

    @Test
    fun `message without parentId is top-level`() {
        val msg = testMessage()
        assertNull(msg.parentId)
    }

    @Test
    fun `message with section is categorized`() {
        val msg = testMessage().copy(section = CommentSection.TRANSPORT)
        assertEquals(CommentSection.TRANSPORT, msg.section)
    }
}
