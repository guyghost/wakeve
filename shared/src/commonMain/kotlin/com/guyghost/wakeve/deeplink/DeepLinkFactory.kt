package com.guyghost.wakeve.deeplink

/**
 * Factory for creating deep links from various sources.
 *
 * This factory provides convenient methods to create deep links for
 * different app screens and features, ensuring consistency in URI construction.
 */
object DeepLinkFactory {

    /**
     * Creates a deep link to the event details screen.
     *
     * @param eventId The ID of the event
     * @param tab Optional tab to open (e.g., "details", "votes", "participants")
     * @return A DeepLink object
     */
    fun createEventDetailsLink(
        eventId: String,
        tab: String? = null
    ): DeepLink {
        val normalizedEventId = normalizePathSegment(eventId, "eventId")
        val params = tab?.trim()?.takeIf { it.isNotEmpty() }?.let { mapOf("tab" to it) } ?: emptyMap()
        return DeepLink.create(
            route = "event/$normalizedEventId/details",
            parameters = params
        )
    }

    /**
     * Creates a deep link to the event poll voting screen.
     *
     * @param eventId The ID of the event
     * @param slotId Optional specific slot ID to highlight
     * @return A DeepLink object
     */
    fun createPollVoteLink(
        eventId: String,
        slotId: String? = null
    ): DeepLink {
        val normalizedEventId = normalizePathSegment(eventId, "eventId")
        val params = slotId?.trim()?.takeIf { it.isNotEmpty() }?.let { mapOf("slotId" to it) } ?: emptyMap()
        return DeepLink.create(
            route = "event/$normalizedEventId/poll",
            parameters = params
        )
    }

    /**
     * Creates a deep link to the scenario comparison screen.
     *
     * @param eventId The ID of the event
     * @param scenarioId Optional specific scenario to highlight
     * @return A DeepLink object
     */
    fun createScenarioComparisonLink(
        eventId: String,
        scenarioId: String? = null
    ): DeepLink {
        val normalizedEventId = normalizePathSegment(eventId, "eventId")
        val params = scenarioId?.trim()?.takeIf { it.isNotEmpty() }?.let { mapOf("scenarioId" to it) } ?: emptyMap()
        return DeepLink.create(
            route = "event/$normalizedEventId/scenarios",
            parameters = params
        )
    }

    /**
     * Creates a deep link to the meeting join screen.
     *
     * @param eventId The ID of the event
     * @param meetingId The ID of the meeting
     * @param autoJoin Whether to automatically join the meeting
     * @return A DeepLink object
     */
    fun createMeetingJoinLink(
        eventId: String,
        meetingId: String,
        autoJoin: Boolean = false
    ): DeepLink {
        val normalizedEventId = normalizePathSegment(eventId, "eventId")
        val normalizedMeetingId = normalizeParameterValue(meetingId, "meetingId")
        val params = buildMap {
            put("meetingId", normalizedMeetingId)
            if (autoJoin) put("autoJoin", "true")
        }
        return DeepLink.create(
            route = "event/$normalizedEventId/meetings",
            parameters = params
        )
    }

    /**
     * Creates a deep link to the event meetings list.
     *
     * @param eventId The ID of the event
     * @return A DeepLink object
     */
    fun createEventMeetingsLink(eventId: String): DeepLink {
        val normalizedEventId = normalizePathSegment(eventId, "eventId")
        return DeepLink.create(route = "event/$normalizedEventId/meetings")
    }

    /**
     * Creates a deep link to the notifications preferences screen.
     *
     * @param section Optional specific settings section to open
     * @return A DeepLink object
     */
    fun createNotificationPreferencesLink(
        section: String? = null
    ): DeepLink {
        val params = section?.trim()?.takeIf { it.isNotEmpty() }?.let { mapOf("section" to it) } ?: emptyMap()
        return DeepLink.create(
            route = "settings",
            parameters = params + mapOf("category" to "notifications")
        )
    }

    /**
     * Creates a deep link to the user profile screen.
     *
     * @param userId Optional user ID to view (null for current user)
     * @return A DeepLink object
     */
    fun createProfileLink(userId: String? = null): DeepLink {
        val params = userId?.trim()?.takeIf { it.isNotEmpty() }?.let { mapOf("userId" to it) } ?: emptyMap()
        return DeepLink.create(
            route = "profile",
            parameters = params
        )
    }

    /**
     * Creates a deep link to the notifications list screen.
     *
     * @param filter Optional filter for notifications
     * @return A DeepLink object
     */
    fun createNotificationsListLink(
        filter: String? = null
    ): DeepLink {
        val params = filter?.trim()?.takeIf { it.isNotEmpty() }?.let { mapOf("filter" to it) } ?: emptyMap()
        return DeepLink.create(
            route = "notifications",
            parameters = params
        )
    }

    /**
     * Creates a deep link to the home screen.
     *
     * @param tab Optional tab to open
     * @return A DeepLink object
     */
    fun createHomeLink(tab: String? = null): DeepLink {
        val params = tab?.trim()?.takeIf { it.isNotEmpty() }?.let { mapOf("tab" to it) } ?: emptyMap()
        return DeepLink.create(
            route = "home",
            parameters = params
        )
    }

    /**
     * Creates a deep link to accept an event invitation.
     *
     * @param code The invitation code from the shareable invite link
     * @return A DeepLink object
     */
    fun createInvitationLink(code: String): DeepLink {
        val normalizedCode = normalizePathSegment(code, "Invitation code")
        return DeepLink.create(route = "invite/$normalizedCode")
    }

    /**
     * Creates a deep link from a notification type and event ID.
     *
     * @param notificationType The type of notification
     * @param eventId The event ID associated with the notification
     * @param additionalParams Additional parameters specific to the notification
     * @return A DeepLink object
     */
    fun createFromNotification(
        notificationType: String,
        eventId: String?,
        additionalParams: Map<String, String> = emptyMap()
    ): DeepLink {
        return runCatching {
            when (notificationType.uppercase()) {
            "EVENT_INVITE" -> {
                val invitationCode = additionalParams["code"]?.trim()?.takeIf { it.isNotEmpty() }
                    ?: additionalParams["invitationCode"]?.trim()?.takeIf { it.isNotEmpty() }
                if (invitationCode != null) {
                    createInvitationLink(invitationCode)
                } else {
                    createEventDetailsLink(
                        eventId = eventId.orEmpty(),
                        tab = "details"
                    )
                }
            }
            "DATE_CONFIRMED", "EVENT_UPDATE" -> {
                createEventDetailsLink(
                    eventId = eventId.orEmpty(),
                    tab = "details"
                )
            }
            "DEADLINE_REMINDER", "VOTE_REMINDER", "VOTE_CLOSE_REMINDER" -> {
                createPollVoteLink(eventId = eventId.orEmpty())
            }
            "NEW_SCENARIO", "SCENARIO_SELECTED" -> {
                createScenarioComparisonLink(eventId = eventId.orEmpty())
            }
            "MEETING_REMINDER" -> {
                val meetingId = additionalParams["meetingId"].orEmpty()
                createMeetingJoinLink(
                    eventId = eventId.orEmpty(),
                    meetingId = meetingId,
                    autoJoin = additionalParams["autoJoin"] == "true"
                )
            }
            "MENTION", "NEW_COMMENT", "COMMENT_REPLY" -> {
                val section = additionalParams["section"] ?: "comments"
                createEventDetailsLink(
                    eventId = eventId.orEmpty(),
                    tab = section
                )
            }
            "PAYMENT_DUE" -> {
                createEventDetailsLink(
                    eventId = eventId.orEmpty(),
                    tab = "budget"
                )
            }
            else -> createHomeLink()
            }
        }.getOrElse {
            createNotificationsListLink(filter = "unread")
        }
    }

    private fun normalizePathSegment(value: String, fieldName: String): String {
        return normalizeDeepLinkPathParameter(value, fieldName)
    }

    private fun normalizeParameterValue(value: String, fieldName: String): String {
        val normalized = value.trim()
        require(normalized.isNotEmpty()) { "$fieldName cannot be blank" }
        return normalized
    }
}

/**
 * Extension functions for creating deep links more conveniently.
 */
object DeepLinkUriBuilder {

    /**
     * Builder class for constructing deep link URIs fluently.
     */
    class Builder(private val baseRoute: String) {
        private val pathParams = mutableMapOf<String, String>()
        private val queryParams = mutableMapOf<String, String>()

        /**
         * Adds a path parameter.
         *
         * @param key The parameter key (should match {key} in route)
         * @param value The parameter value
         * @return This builder for chaining
         */
        fun pathParam(key: String, value: String): Builder {
            pathParams[key] = value
            return this
        }

        /**
         * Adds a query parameter.
         *
         * @param key The parameter key
         * @param value The parameter value
         * @return This builder for chaining
         */
        fun queryParam(key: String, value: String): Builder {
            queryParams[key] = value
            return this
        }

        /**
         * Adds multiple query parameters.
         *
         * @param params The parameters to add
         * @return This builder for chaining
         */
        fun queryParams(params: Map<String, String>): Builder {
            queryParams.putAll(params)
            return this
        }

        /**
         * Builds the deep link URI string.
         *
         * @return The complete URI
         */
        fun build(): String {
            var resolvedPath = baseRoute
            pathParams.forEach { (key, value) ->
                resolvedPath = resolvedPath.replace("{$key}", normalizeDeepLinkPathParameter(value, key))
            }

            val queryString = if (queryParams.isNotEmpty()) {
                "?" + queryParams.entries.joinToString("&") { (key, value) ->
                    "${encodeUriComponent(key)}=${encodeUriComponent(value)}"
                }
            } else {
                ""
            }

            return "${DeepLink.WAKEVE_SCHEME}://$resolvedPath$queryString"
        }

        /**
         * Builds and parses the deep link.
         *
         * @return Result containing the DeepLink or an exception
         */
        fun buildDeepLink(): Result<DeepLink> {
            return DeepLink.parse(build())
        }

        private fun encodeUriComponent(value: String): String {
            return value
                .replace("%", "%25")
                .replace("&", "%26")
                .replace("=", "%3D")
                .replace("?", "%3F")
                .replace(" ", "%20")
        }
    }

    /**
     * Starts building a deep link for the given route.
     *
     * @param route The base route pattern
     * @return A builder instance
     */
    fun route(route: String): Builder = Builder(route)

    /**
     * Starts building a deep link for a specific DeepLinkRoute.
     *
     * @param deepLinkRoute The route enum value
     * @return A builder instance
     */
    fun route(deepLinkRoute: DeepLinkRoute): Builder = Builder(deepLinkRoute.pattern)
}
