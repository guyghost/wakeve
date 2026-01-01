package com.guyghost.wakeve.models

import kotlinx.serialization.Serializable

/**
 * Event type classification for personalized suggestions and categorization.
 *
 * Used by agents (Suggestions, Destination) to provide relevant recommendations.
 */
@Serializable
enum class EventType {
    /** Birthday party or celebration */
    BIRTHDAY,
    
    /** Wedding ceremony and/or reception */
    WEDDING,
    
    /** Team building activity or corporate retreat */
    TEAM_BUILDING,
    
    /** Professional conference or summit */
    CONFERENCE,
    
    /** Educational workshop or training session */
    WORKSHOP,
    
    /** Social party or gathering */
    PARTY,
    
    /** Sports event or tournament */
    SPORTS_EVENT,
    
    /** Cultural event (museum visit, theater, etc.) */
    CULTURAL_EVENT,
    
    /** Family gathering or reunion */
    FAMILY_GATHERING,
    
    /** Sports competition, tournament, or training session */
    SPORT_EVENT,
    
    /** Outdoor activities (hiking, kayaking, camping, etc.) */
    OUTDOOR_ACTIVITY,
    
    /** Culinary tasting experiences (wine, cheese, cooking classes) */
    FOOD_TASTING,
    
    /** Technology meetups (hackathons, tech conferences, developer events) */
    TECH_MEETUP,
    
    /** Wellness activities (yoga, meditation, spa, relaxation) */
    WELLNESS_EVENT,
    
    /** Creative workshops (painting, pottery, writing, crafts) */
    CREATIVE_WORKSHOP,
    
    /** Other type of event (default) */
    OTHER,
    
    /** Custom event type (requires eventTypeCustom text) */
    CUSTOM;

    val displayName: String
        get() = when (this) {
            BIRTHDAY -> "Birthday"
            WEDDING -> "Wedding"
            TEAM_BUILDING -> "Team Building"
            CONFERENCE -> "Conference"
            WORKSHOP -> "Workshop"
            PARTY -> "Party"
            SPORTS_EVENT -> "Sports Event"
            CULTURAL_EVENT -> "Cultural Event"
            FAMILY_GATHERING -> "Family Gathering"
            SPORT_EVENT -> "Sport Event"
            OUTDOOR_ACTIVITY -> "Outdoor Activity"
            FOOD_TASTING -> "Food Tasting"
            TECH_MEETUP -> "Tech Meetup"
            WELLNESS_EVENT -> "Wellness Event"
            CREATIVE_WORKSHOP -> "Creative Workshop"
            OTHER -> "Other"
            CUSTOM -> "Custom"
        }

    val suggestionCategory: SuggestionCategory
        get() = when (this) {
            BIRTHDAY -> SuggestionCategory.SOCIAL
            WEDDING -> SuggestionCategory.SOCIAL
            TEAM_BUILDING -> SuggestionCategory.SOCIAL
            CONFERENCE -> SuggestionCategory.TECH
            WORKSHOP -> SuggestionCategory.CREATIVE
            PARTY -> SuggestionCategory.SOCIAL
            SPORTS_EVENT -> SuggestionCategory.SPORTS
            CULTURAL_EVENT -> SuggestionCategory.CULTURAL
            FAMILY_GATHERING -> SuggestionCategory.SOCIAL
            SPORT_EVENT -> SuggestionCategory.SPORTS
            OUTDOOR_ACTIVITY -> SuggestionCategory.OUTDOOR
            FOOD_TASTING -> SuggestionCategory.FOOD
            TECH_MEETUP -> SuggestionCategory.TECH
            WELLNESS_EVENT -> SuggestionCategory.WELLNESS
            CREATIVE_WORKSHOP -> SuggestionCategory.CREATIVE
            OTHER -> SuggestionCategory.SOCIAL
            CUSTOM -> SuggestionCategory.SOCIAL
        }

    /**
     * Returns contextual tags for AI-powered suggestions based on event type.
     * These tags are used by the Suggestions agent to provide relevant recommendations.
     *
     * @return List of tags matching this event type for location and activity suggestions
     */
    fun getSuggestionTags(): List<String> = when (this) {
        BIRTHDAY -> listOf("restaurant", "bar", "party_venue", "rooftop", "private_room")
        WEDDING -> listOf("wedding_venue", "hotel", "garden", "ballroom", "restaurant_private")
        TEAM_BUILDING -> listOf("coworking", "retreat_center", "activity_center", "restaurant_group")
        CONFERENCE -> listOf("conference_center", "auditorium", "hotel_ballroom", "university")
        WORKSHOP -> listOf("workshop_studio", "classroom", "cooking_school", "art_gallery")
        PARTY -> listOf("nightclub", "bar", "lounge", "party_venue", "private_rental")
        SPORTS_EVENT -> listOf("stadium", "arena", "sports_center", "gym", "field")
        CULTURAL_EVENT -> listOf("museum", "theater", "gallery", "cultural_center", "concert_venue")
        FAMILY_GATHERING -> listOf("restaurant_family", "park", "home_rental", "community_center")
        SPORT_EVENT -> listOf("stadium", "arena", "sports_center", "gym", "field")
        OUTDOOR_ACTIVITY -> listOf("park", "beach", "mountain", "camping_site", "trail", "nature_reserve")
        FOOD_TASTING -> listOf("restaurant", "wine_bar", "cafe", "bakery", "market", "cooking_class")
        TECH_MEETUP -> listOf("coworking", "conference_center", "auditorium", "tech_hub", "startup_space")
        WELLNESS_EVENT -> listOf("spa", "yoga_studio", "meditation_center", "gym", "wellness_center")
        CREATIVE_WORKSHOP -> listOf("art_gallery", "workshop_studio", "craft_center", "pottery_studio")
        OTHER -> listOf("restaurant", "cafe", "bar", "venue")
        CUSTOM -> listOf("restaurant", "cafe", "venue", "flexible_space")
    }
}

/**
 * Categories for organizing AI suggestions based on event type context.
 * Used by the Suggestions agent to provide relevant recommendations.
 */
@Serializable
enum class SuggestionCategory {
    /** Cultural and artistic experiences */
    CULTURAL,

    /** Sports and athletic activities */
    SPORTS,

    /** Food and beverage experiences */
    FOOD,

    /** Technology and professional events */
    TECH,

    /** Wellness and relaxation activities */
    WELLNESS,

    /** Creative and artistic workshops */
    CREATIVE,

    /** Social gatherings and celebrations */
    SOCIAL,

    /** Outdoor and nature activities */
    OUTDOOR,

    /** Travel and accommodation suggestions */
    TRAVEL
}
