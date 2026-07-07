package com.guyghost.wakeve.moderation

class ModerationPolicy(
    private val hardPolicyTerms: Set<String> = DEFAULT_HARD_POLICY_TERMS,
    private val pendingReviewTerms: Set<String> = DEFAULT_PENDING_REVIEW_TERMS
) {
    fun evaluate(text: String): ModerationResult {
        val normalized = text.normalizeForModeration()

        if (normalized.isBlank()) {
            return ModerationResult(
                status = ModerationStatus.REJECTED,
                reasonCode = "blank_content",
                userMessage = SAFE_REJECTION_MESSAGE
            )
        }

        if (hardPolicyTerms.any { normalized.contains(it) }) {
            return ModerationResult(
                status = ModerationStatus.REJECTED,
                reasonCode = "hard_policy_term",
                userMessage = SAFE_REJECTION_MESSAGE
            )
        }

        if (containsSpamPattern(normalized) || pendingReviewTerms.any { normalized.contains(it) }) {
            return ModerationResult(
                status = ModerationStatus.PENDING_REVIEW,
                reasonCode = "needs_review",
                userMessage = "Your content is pending review before it is visible to other participants."
            )
        }

        return ModerationResult(
            status = ModerationStatus.APPROVED,
            reasonCode = "approved",
            userMessage = "Content approved."
        )
    }

    private fun containsSpamPattern(normalized: String): Boolean {
        val linkCount = LINK_PATTERN.findAll(normalized).count()
        val repeatedPhoneLikeDigits = DIGIT_PATTERN.findAll(normalized).count() >= 3
        val repeatedSalesWords = SPAM_WORDS.count { normalized.contains(it) } >= 2
        return linkCount >= 2 || repeatedPhoneLikeDigits || repeatedSalesWords
    }

    private fun String.normalizeForModeration(): String =
        trim()
            .lowercase()
            .replace(WHITESPACE_PATTERN, " ")

    companion object {
        const val SAFE_REJECTION_MESSAGE = "This content cannot be posted. Please revise it and try again."

        private val LINK_PATTERN = Regex("""https?://|www\.""")
        private val DIGIT_PATTERN = Regex("""\d{3,}""")
        private val WHITESPACE_PATTERN = Regex("""\s+""")

        private val SPAM_WORDS = setOf(
            "free money",
            "limited offer",
            "wire transfer",
            "crypto giveaway"
        )

        private val DEFAULT_HARD_POLICY_TERMS = setOf(
            "kill yourself",
            "credible threat",
            "violent threat",
            "sexual content involving minors",
            "doxxing address"
        )

        private val DEFAULT_PENDING_REVIEW_TERMS = setOf(
            "meet privately with strangers",
            "off-platform payment",
            "adult-only meetup"
        )
    }
}
