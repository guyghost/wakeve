package com.guyghost.wakeve.collaboration

import com.guyghost.wakeve.models.Mention
import com.guyghost.wakeve.currentTimeMillis
import kotlinx.serialization.Serializable

/**
 * Result of parsing mentions from content
 *
 * @property mentions List of parsed mentions with position information
 * @property sanitizedContent Content with @mentions removed/replaced
 * @property mentionedUserIds List of user IDs mentioned
 */
@Serializable
data class MentionParseResult(
    val mentions: List<Mention>,
    val sanitizedContent: String,
    val mentionedUserIds: List<String>
)

/**
 * Mention Parser
 *
 * Pure function to parse @username mentions from comment content.
 * Supports multiple mentions and position tracking for highlighting.
 *
 * Example:
 * ```
 * val content = "Hey @alice and @bob, let's meet at 3pm"
 * val result = MentionParser.parseMentions(content)
 * // result.mentionedUserIds == ["alice", "bob"]
 * ```
 */
object MentionParser {

    /**
     * Regex pattern for @username mentions.
     *
     * Pattern: @ followed by username (alphanumeric, underscores, hyphens)
     * - Matches: @alice, @john_doe, @user-123
     * - Does not match: @@alice, email@test.com
     */
    private val MENTION_PATTERN = Regex("""(?<![A-Za-z0-9._%+-])@([a-zA-Z0-9_-]+)""")

    /**
     * Parse mentions from comment content.
     *
     * Extracts all @username mentions and their positions.
     *
     * @param commentId Comment ID (for creating Mention objects)
     * @param content Comment text content
     * @param usernameToUserIdMap Map of username to user ID for resolution
     * @return MentionParseResult with all mentions
     */
    fun parseMentions(
        commentId: String,
        content: String,
        usernameToUserIdMap: Map<String, String> = emptyMap()
    ): MentionParseResult {
        val mentions = mutableListOf<Mention>()
        val mentionedUserIds = mutableListOf<String>()

        // Find all matches
        MENTION_PATTERN.findAll(content).forEach { match ->
            val username = match.groupValues[1]
            val userId = usernameToUserIdMap[username] ?: username

            val mention = Mention(
                id = generateMentionId(),
                commentId = commentId,
                mentionedUserId = userId,
                startIndex = match.range.first,
                endIndex = match.range.last + 1
            )

            mentions.add(mention)
            mentionedUserIds.add(userId)
        }

        return MentionParseResult(
            mentions = mentions,
            sanitizedContent = content,  // Keep original content for now
            mentionedUserIds = mentionedUserIds
        )
    }

    /**
     * Extract usernames from content (lightweight version).
     *
     * Returns list of @username strings found in content.
     *
     * @param content Comment text content
     * @return List of usernames (with @ prefix)
     */
    fun extractUsernames(content: String): List<String> {
        return MENTION_PATTERN.findAll(content)
            .map { it.value }
            .distinct()
            .toList()
    }

    /**
     * Highlight mentions in content for display.
     *
     * Wraps @username mentions with styling markers.
     * Can be used by UI components to render highlighted text.
     *
     * Example:
     * ```
     * highlightMentions("Hello @alice") -> "Hello [@alice]"
     * ```
     *
     * @param content Comment text content
     * @param prefixMarker Prefix to add before mention (e.g., "[")
     * @param suffixMarker Suffix to add after mention (e.g., "]")
     * @return Content with highlighted mentions
     */
    fun highlightMentions(
        content: String,
        prefixMarker: String = "[@",
        suffixMarker: String = "]"
    ): String {
        return content.replace(MENTION_PATTERN, "$prefixMarker$1$suffixMarker")
    }

    /**
     * Strip mention markers from content.
     *
     * Removes @ symbols from content for plain text rendering.
     *
     * @param content Comment text content
     * @return Content without @ symbols
     */
    fun stripMentions(content: String): String {
        return content.replace("@", "")
    }

    /**
     * Validate mention format.
     *
     * Checks if a mention string is valid.
     *
     * @param mention Mention string (e.g., "@alice")
     * @return true if valid format
     */
    fun isValidMention(mention: String): Boolean {
        return MENTION_PATTERN.matches(mention)
    }

    /**
     * Generate unique mention ID.
     */
    private fun generateMentionId(): String {
        return "mention_${currentTimeMillis()}_${(0..9999).random()}"
    }
}
