package com.guyghost.wakeve.gamification

import kotlinx.serialization.Serializable

/**
 * Badge Assets definition for the 15 core badges.
 * Contains visual assets (emoji/icon, color) for each badge.
 *
 * Each badge has:
 * - An emoji icon for display
 * - A Material Design 3 color (hex format)
 * - Rarity tier (COMMON, RARE, EPIC, LEGENDARY)
 * - Category (CREATION, VOTING, PARTICIPATION, ENGAGEMENT)
 * - Points reward and unlock requirement
 */
object BadgeAssets {

    /**
     * Complete map of all badge assets keyed by badge ID.
     */
    val BADGES: Map<String, BadgeAsset> = mapOf(
        // ========== CREATION Category ==========

        "badge-first-event" to BadgeAsset(
            id = "badge-first-event",
            name = "Premier événement",
            nameKey = "badge.first_event.name",
            description = "A créé votre premier événement",
            descriptionKey = "badge.first_event.description",
            icon = "🎉",
            color = "#6200EE",
            rarity = BadgeRarity.COMMON,
            category = BadgeCategory.CREATION,
            pointsReward = 10,
            requirement = 1
        ),

        "badge-dedicated" to BadgeAsset(
            id = "badge-dedicated",
            name = "Organisateur Dévoué",
            nameKey = "badge.dedicated.name",
            description = "A créé 5 événements",
            descriptionKey = "badge.dedicated.description",
            icon = "💪",
            color = "#7C4DFF",
            rarity = BadgeRarity.RARE,
            category = BadgeCategory.CREATION,
            pointsReward = 75,
            requirement = 5
        ),

        "badge-super-organizer" to BadgeAsset(
            id = "badge-super-organizer",
            name = "Super Organisateur",
            nameKey = "badge.super_organizer.name",
            description = "A créé 10 événements",
            descriptionKey = "badge.super_organizer.description",
            icon = "🏆",
            color = "#FF9800",
            rarity = BadgeRarity.EPIC,
            category = BadgeCategory.CREATION,
            pointsReward = 100,
            requirement = 10
        ),

        "badge-event-master" to BadgeAsset(
            id = "badge-event-master",
            name = "Event Master",
            nameKey = "badge.event_master.name",
            description = "A créé 5 événements en un mois",
            descriptionKey = "badge.event_master.description",
            icon = "👑",
            color = "#FFD700",
            rarity = BadgeRarity.LEGENDARY,
            category = BadgeCategory.CREATION,
            pointsReward = 250,
            requirement = 5
        ),

        "badge-industry" to BadgeAsset(
            id = "badge-industry",
            name = "Producteur d'Événements",
            nameKey = "badge.industry.name",
            description = "A créé 25 événements",
            descriptionKey = "badge.industry.description",
            icon = "🏭",
            color = "#FF5722",
            rarity = BadgeRarity.LEGENDARY,
            category = BadgeCategory.CREATION,
            pointsReward = 500,
            requirement = 25
        ),

        // ========== VOTING Category ==========

        "badge-first-vote" to BadgeAsset(
            id = "badge-first-vote",
            name = "Premier Vote",
            nameKey = "badge.first_vote.name",
            description = "A participé à son premier vote",
            descriptionKey = "badge.first_vote.description",
            icon = "👍",
            color = "#4CAF50",
            rarity = BadgeRarity.COMMON,
            category = BadgeCategory.VOTING,
            pointsReward = 5,
            requirement = 1
        ),

        "badge-early-bird" to BadgeAsset(
            id = "badge-early-bird",
            name = "Early Bird",
            nameKey = "badge.early_bird.name",
            description = "A voté dans les 24h sur 10 sondages",
            descriptionKey = "badge.early_bird.description",
            icon = "⏰",
            color = "#00BCD4",
            rarity = BadgeRarity.RARE,
            category = BadgeCategory.VOTING,
            pointsReward = 50,
            requirement = 10
        ),

        "badge-decision-maker" to BadgeAsset(
            id = "badge-decision-maker",
            name = "Decision Maker",
            nameKey = "badge.decision_maker.name",
            description = "A voté sur 50 sondages",
            descriptionKey = "badge.decision_maker.description",
            icon = "🗳️",
            color = "#2196F3",
            rarity = BadgeRarity.EPIC,
            category = BadgeCategory.VOTING,
            pointsReward = 100,
            requirement = 50
        ),

        "badge-quick-responder" to BadgeAsset(
            id = "badge-quick-responder",
            name = "Réponse Rapide",
            nameKey = "badge.quick_responder.name",
            description = "A répondu à 5 votes en moins de 2 heures",
            descriptionKey = "badge.quick_responder.description",
            icon = "⚡",
            color = "#FFEB3B",
            rarity = BadgeRarity.EPIC,
            category = BadgeCategory.VOTING,
            pointsReward = 75,
            requirement = 5
        ),

        "badge-voting-champion" to BadgeAsset(
            id = "badge-voting-champion",
            name = "Champion du Vote",
            nameKey = "badge.voting_champion.name",
            description = "A participé à 100 votes",
            descriptionKey = "badge.voting_champion.description",
            icon = "🛡️",
            color = "#3F51B5",
            rarity = BadgeRarity.LEGENDARY,
            category = BadgeCategory.VOTING,
            pointsReward = 250,
            requirement = 100
        ),

        // ========== PARTICIPATION Category ==========

        "badge-first-steps" to BadgeAsset(
            id = "badge-first-steps",
            name = "Premiers Pas",
            nameKey = "badge.first_steps.name",
            description = "A participé à son premier événement",
            descriptionKey = "badge.first_steps.description",
            icon = "👣",
            color = "#8BC34A",
            rarity = BadgeRarity.COMMON,
            category = BadgeCategory.PARTICIPATION,
            pointsReward = 10,
            requirement = 1
        ),

        "badge-team-player" to BadgeAsset(
            id = "badge-team-player",
            name = "Team Player",
            nameKey = "badge.team_player.name",
            description = "A participé à 20 événements",
            descriptionKey = "badge.team_player.description",
            icon = "🤝",
            color = "#009688",
            rarity = BadgeRarity.RARE,
            category = BadgeCategory.PARTICIPATION,
            pointsReward = 100,
            requirement = 20
        ),

        "badge-social-butterfly" to BadgeAsset(
            id = "badge-social-butterfly",
            name = "Social Butterfly",
            nameKey = "badge.social_butterfly.name",
            description = "A participé à 10 événements",
            descriptionKey = "badge.social_butterfly.description",
            icon = "🦋",
            color = "#E91E63",
            rarity = BadgeRarity.EPIC,
            category = BadgeCategory.PARTICIPATION,
            pointsReward = 150,
            requirement = 10
        ),

        "badge-party-animal" to BadgeAsset(
            id = "badge-party-animal",
            name = "Fêteur",
            nameKey = "badge.party_animal.name",
            description = "A participé à 25 événements",
            descriptionKey = "badge.party_animal.description",
            icon = "🎊",
            color = "#F44336",
            rarity = BadgeRarity.LEGENDARY,
            category = BadgeCategory.PARTICIPATION,
            pointsReward = 300,
            requirement = 25
        ),

        "badge-festival-goer" to BadgeAsset(
            id = "badge-festival-goer",
            name = "Festival Goer",
            nameKey = "badge.festival_goer.name",
            description = "A assisté à 5 événements sur une même journée",
            descriptionKey = "badge.festival_goer.description",
            icon = "🎪",
            color = "#9C27B0",
            rarity = BadgeRarity.EPIC,
            category = BadgeCategory.PARTICIPATION,
            pointsReward = 200,
            requirement = 5
        ),

        // ========== ENGAGEMENT Category ==========

        "badge-chatty" to BadgeAsset(
            id = "badge-chatty",
            name = "Bavard",
            nameKey = "badge.chatty.name",
            description = "A commenté 10 fois",
            descriptionKey = "badge.chatty.description",
            icon = "💬",
            color = "#673AB7",
            rarity = BadgeRarity.RARE,
            category = BadgeCategory.ENGAGEMENT,
            pointsReward = 25,
            requirement = 10
        ),

        "badge-conversationalist" to BadgeAsset(
            id = "badge-conversationalist",
            name = "Conversationalist",
            nameKey = "badge.conversationalist.name",
            description = "A commenté sur 30 événements",
            descriptionKey = "badge.conversationalist.description",
            icon = "🗨️",
            color = "#3F51B5",
            rarity = BadgeRarity.EPIC,
            category = BadgeCategory.ENGAGEMENT,
            pointsReward = 75,
            requirement = 30
        ),

        "badge-opinionated" to BadgeAsset(
            id = "badge-opinionated",
            name = "Avisé",
            nameKey = "badge.opinionated.name",
            description = "A voté sur 20 scénarios",
            descriptionKey = "badge.opinionated.description",
            icon = "🤝",
            color = "#607D8B",
            rarity = BadgeRarity.COMMON,
            category = BadgeCategory.ENGAGEMENT,
            pointsReward = 30,
            requirement = 20
        ),

        "badge-scenario-creator" to BadgeAsset(
            id = "badge-scenario-creator",
            name = "Créateur de Scénarios",
            nameKey = "badge.scenario_creator.name",
            description = "A créé 5 scénarios",
            descriptionKey = "badge.scenario_creator.description",
            icon = "📝",
            color = "#795548",
            rarity = BadgeRarity.RARE,
            category = BadgeCategory.ENGAGEMENT,
            pointsReward = 50,
            requirement = 5
        ),

        // ========== SPECIAL Category ==========

        "badge-legend" to BadgeAsset(
            id = "badge-legend",
            name = "Event Legend",
            nameKey = "badge.legend.name",
            description = "A atteint 10,000 points",
            descriptionKey = "badge.legend.description",
            icon = "⭐",
            color = "#FFD700",
            rarity = BadgeRarity.LEGENDARY,
            category = BadgeCategory.SPECIAL,
            pointsReward = 1000,
            requirement = 10000
        ),

        "badge-century-club" to BadgeAsset(
            id = "badge-century-club",
            name = "Club des Cent",
            nameKey = "badge.century_club.name",
            description = "A atteint 100 points totaux",
            descriptionKey = "badge.century_club.description",
            icon = "💯",
            color = "#FF9800",
            rarity = BadgeRarity.COMMON,
            category = BadgeCategory.SPECIAL,
            pointsReward = 50,
            requirement = 100
        ),

        "badge-millenium-club" to BadgeAsset(
            id = "badge-millenium-club",
            name = "Club des Mille",
            nameKey = "badge.millenium_club.name",
            description = "A atteint 1000 points totaux",
            descriptionKey = "badge.millenium_club.description",
            icon = "💎",
            color = "#00BCD4",
            rarity = BadgeRarity.EPIC,
            category = BadgeCategory.SPECIAL,
            pointsReward = 200,
            requirement = 1000
        )
    )

    /**
     * Gets a specific badge asset by its ID.
     *
     * @param badgeId The unique identifier of the badge
     * @return The BadgeAsset if found, null otherwise
     */
    fun getBadgeAsset(badgeId: String): BadgeAsset? {
        return BADGES[badgeId]
    }

    /**
     * Gets all badge assets as a list.
     *
     * @return List of all BadgeAsset objects
     */
    fun getAllBadgeAssets(): List<BadgeAsset> {
        return BADGES.values.toList()
    }

    /**
     * Gets all badge assets for a specific category.
     *
     * @param category The category to filter by
     * @return List of BadgeAsset objects in the specified category
     */
    fun getBadgesByCategory(category: BadgeCategory): List<BadgeAsset> {
        return BADGES.values.filter { it.category == category }
    }

    /**
     * Gets all badge assets for a specific rarity.
     *
     * @param rarity The rarity tier to filter by
     * @return List of BadgeAsset objects with the specified rarity
     */
    fun getBadgesByRarity(rarity: BadgeRarity): List<BadgeAsset> {
        return BADGES.values.filter { it.rarity == rarity }
    }

    /**
     * Gets the color for a specific rarity tier.
     * Useful for displaying rarity indicators.
     *
     * @param rarity The rarity tier
     * @return Hex color code for the rarity
     */
    fun getColorForRarity(rarity: BadgeRarity): String {
        return when (rarity) {
            BadgeRarity.COMMON -> "#9E9E9E"      // Grey
            BadgeRarity.RARE -> "#2196F3"        // Blue
            BadgeRarity.EPIC -> "#9C27B0"        // Purple
            BadgeRarity.LEGENDARY -> "#FFD700"   // Gold
        }
    }

    /** Localized badge names keyed by nameKey then locale. */
    private val badgeNames: Map<String, Map<String, String>> = mapOf(
        "badge.first_event.name" to mapOf(
            "en" to "First Event", "fr" to "Premier événement", "es" to "Primer evento",
            "it" to "Primo evento", "pt" to "Primeiro evento"
        ),
        "badge.dedicated.name" to mapOf(
            "en" to "Dedicated Organizer", "fr" to "Organisateur Dévoué", "es" to "Organizador Dedicado",
            "it" to "Organizzatore Dedicato", "pt" to "Organizador Dedicado"
        ),
        "badge.super_organizer.name" to mapOf(
            "en" to "Super Organizer", "fr" to "Super Organisateur", "es" to "Súper Organizador",
            "it" to "Super Organizzatore", "pt" to "Super Organizador"
        ),
        "badge.event_master.name" to mapOf(
            "en" to "Event Master", "fr" to "Event Master", "es" to "Event Master",
            "it" to "Event Master", "pt" to "Event Master"
        ),
        "badge.industry.name" to mapOf(
            "en" to "Event Producer", "fr" to "Producteur d'Événements", "es" to "Productor de Eventos",
            "it" to "Produttore di Eventi", "pt" to "Produtor de Eventos"
        ),
        "badge.first_vote.name" to mapOf(
            "en" to "First Vote", "fr" to "Premier Vote", "es" to "Primer Voto",
            "it" to "Primo Voto", "pt" to "Primeiro Voto"
        ),
        "badge.early_bird.name" to mapOf(
            "en" to "Early Bird", "fr" to "Early Bird", "es" to "Early Bird",
            "it" to "Early Bird", "pt" to "Early Bird"
        ),
        "badge.decision_maker.name" to mapOf(
            "en" to "Decision Maker", "fr" to "Decision Maker", "es" to "Decision Maker",
            "it" to "Decision Maker", "pt" to "Decision Maker"
        ),
        "badge.quick_responder.name" to mapOf(
            "en" to "Quick Responder", "fr" to "Réponse Rapide", "es" to "Respuesta Rápida",
            "it" to "Risposta Rapida", "pt" to "Resposta Rápida"
        ),
        "badge.voting_champion.name" to mapOf(
            "en" to "Voting Champion", "fr" to "Champion du Vote", "es" to "Campeón de Votación",
            "it" to "Campione di Voto", "pt" to "Campeão de Votação"
        ),
        "badge.first_steps.name" to mapOf(
            "en" to "First Steps", "fr" to "Premiers Pas", "es" to "Primeros Pasos",
            "it" to "Primi Passi", "pt" to "Primeiros Passos"
        ),
        "badge.team_player.name" to mapOf(
            "en" to "Team Player", "fr" to "Team Player", "es" to "Team Player",
            "it" to "Team Player", "pt" to "Team Player"
        ),
        "badge.social_butterfly.name" to mapOf(
            "en" to "Social Butterfly", "fr" to "Social Butterfly", "es" to "Social Butterfly",
            "it" to "Social Butterfly", "pt" to "Social Butterfly"
        ),
        "badge.party_animal.name" to mapOf(
            "en" to "Party Animal", "fr" to "Fêteur", "es" to "Fiestero",
            "it" to "Festaiolo", "pt" to "Festeiro"
        ),
        "badge.festival_goer.name" to mapOf(
            "en" to "Festival Goer", "fr" to "Festival Goer", "es" to "Festival Goer",
            "it" to "Festival Goer", "pt" to "Festival Goer"
        ),
        "badge.chatty.name" to mapOf(
            "en" to "Chatty", "fr" to "Bavard", "es" to "Parlanchín",
            "it" to "Chiacchierone", "pt" to "Tagarela"
        ),
        "badge.conversationalist.name" to mapOf(
            "en" to "Conversationalist", "fr" to "Conversationalist", "es" to "Conversationalist",
            "it" to "Conversationalist", "pt" to "Conversationalist"
        ),
        "badge.opinionated.name" to mapOf(
            "en" to "Opinionated", "fr" to "Avisé", "es" to "Experto",
            "it" to "Esperto", "pt" to "Opinador"
        ),
        "badge.scenario_creator.name" to mapOf(
            "en" to "Scenario Creator", "fr" to "Créateur de Scénarios", "es" to "Creador de Escenarios",
            "it" to "Creatore di Scenari", "pt" to "Criador de Cenários"
        ),
        "badge.legend.name" to mapOf(
            "en" to "Event Legend", "fr" to "Event Legend", "es" to "Event Legend",
            "it" to "Event Legend", "pt" to "Event Legend"
        ),
        "badge.century_club.name" to mapOf(
            "en" to "Century Club", "fr" to "Club des Cent", "es" to "Club de los Cien",
            "it" to "Club dei Cento", "pt" to "Clube dos Cem"
        ),
        "badge.millenium_club.name" to mapOf(
            "en" to "Millennium Club", "fr" to "Club des Mille", "es" to "Club de los Mil",
            "it" to "Club dei Mille", "pt" to "Clube dos Mil"
        )
    )

    /** Localized badge descriptions keyed by descriptionKey then locale. */
    private val badgeDescriptions: Map<String, Map<String, String>> = mapOf(
        "badge.first_event.description" to mapOf(
            "en" to "Created your first event", "fr" to "A créé votre premier événement",
            "es" to "Creó su primer evento", "it" to "Ha creato il primo evento", "pt" to "Criou o primeiro evento"
        ),
        "badge.dedicated.description" to mapOf(
            "en" to "Created 5 events", "fr" to "A créé 5 événements",
            "es" to "Creó 5 eventos", "it" to "Ha creato 5 eventi", "pt" to "Criou 5 eventos"
        ),
        "badge.super_organizer.description" to mapOf(
            "en" to "Created 10 events", "fr" to "A créé 10 événements",
            "es" to "Creó 10 eventos", "it" to "Ha creato 10 eventi", "pt" to "Criou 10 eventos"
        ),
        "badge.event_master.description" to mapOf(
            "en" to "Created 5 events in one month", "fr" to "A créé 5 événements en un mois",
            "es" to "Creó 5 eventos en un mes", "it" to "Ha creato 5 eventi in un mese", "pt" to "Criou 5 eventos em um mês"
        ),
        "badge.industry.description" to mapOf(
            "en" to "Created 25 events", "fr" to "A créé 25 événements",
            "es" to "Creó 25 eventos", "it" to "Ha creato 25 eventi", "pt" to "Criou 25 eventos"
        ),
        "badge.first_vote.description" to mapOf(
            "en" to "Participated in your first vote", "fr" to "A participé à son premier vote",
            "es" to "Participó en su primer voto", "it" to "Ha partecipato al primo voto", "pt" to "Participou na primeira votação"
        ),
        "badge.early_bird.description" to mapOf(
            "en" to "Voted within 24h on 10 polls", "fr" to "A voté dans les 24h sur 10 sondages",
            "es" to "Votó en 24h en 10 encuestas", "it" to "Ha votato entro 24h su 10 sondaggi", "pt" to "Votou em 24h em 10 enquetes"
        ),
        "badge.decision_maker.description" to mapOf(
            "en" to "Voted on 50 polls", "fr" to "A voté sur 50 sondages",
            "es" to "Votó en 50 encuestas", "it" to "Ha votato su 50 sondaggi", "pt" to "Votou em 50 enquetes"
        ),
        "badge.quick_responder.description" to mapOf(
            "en" to "Responded to 5 votes in less than 2 hours", "fr" to "A répondu à 5 votes en moins de 2 heures",
            "es" to "Respondió a 5 votos en menos de 2 horas", "it" to "Ha risposto a 5 voti in meno di 2 ore", "pt" to "Respondeu a 5 votos em menos de 2 horas"
        ),
        "badge.voting_champion.description" to mapOf(
            "en" to "Participated in 100 votes", "fr" to "A participé à 100 votes",
            "es" to "Participó en 100 votos", "it" to "Ha partecipato a 100 voti", "pt" to "Participou em 100 votos"
        ),
        "badge.first_steps.description" to mapOf(
            "en" to "Participated in your first event", "fr" to "A participé à son premier événement",
            "es" to "Participó en su primer evento", "it" to "Ha partecipato al primo evento", "pt" to "Participou no primeiro evento"
        ),
        "badge.team_player.description" to mapOf(
            "en" to "Participated in 20 events", "fr" to "A participé à 20 événements",
            "es" to "Participó en 20 eventos", "it" to "Ha partecipato a 20 eventi", "pt" to "Participou em 20 eventos"
        ),
        "badge.social_butterfly.description" to mapOf(
            "en" to "Participated in 10 events", "fr" to "A participé à 10 événements",
            "es" to "Participó en 10 eventos", "it" to "Ha partecipato a 10 eventi", "pt" to "Participou em 10 eventos"
        ),
        "badge.party_animal.description" to mapOf(
            "en" to "Participated in 25 events", "fr" to "A participé à 25 événements",
            "es" to "Participó en 25 eventos", "it" to "Ha partecipato a 25 eventi", "pt" to "Participou em 25 eventos"
        ),
        "badge.festival_goer.description" to mapOf(
            "en" to "Attended 5 events on the same day", "fr" to "A assisté à 5 événements sur une même journée",
            "es" to "Asistió a 5 eventos en el mismo día", "it" to "Ha partecipato a 5 eventi nella stessa giornata", "pt" to "Participou de 5 eventos no mesmo dia"
        ),
        "badge.chatty.description" to mapOf(
            "en" to "Commented 10 times", "fr" to "A commenté 10 fois",
            "es" to "Comentó 10 veces", "it" to "Ha commentato 10 volte", "pt" to "Comentou 10 vezes"
        ),
        "badge.conversationalist.description" to mapOf(
            "en" to "Commented on 30 events", "fr" to "A commenté sur 30 événements",
            "es" to "Comentó en 30 eventos", "it" to "Ha commentato su 30 eventi", "pt" to "Comentou em 30 eventos"
        ),
        "badge.opinionated.description" to mapOf(
            "en" to "Voted on 20 scenarios", "fr" to "A voté sur 20 scénarios",
            "es" to "Votó en 20 escenarios", "it" to "Ha votato su 20 scenari", "pt" to "Votou em 20 cenários"
        ),
        "badge.scenario_creator.description" to mapOf(
            "en" to "Created 5 scenarios", "fr" to "A créé 5 scénarios",
            "es" to "Creó 5 escenarios", "it" to "Ha creato 5 scenari", "pt" to "Criou 5 cenários"
        ),
        "badge.legend.description" to mapOf(
            "en" to "Reached 10,000 points", "fr" to "A atteint 10 000 points",
            "es" to "Alcanzó 10.000 puntos", "it" to "Ha raggiunto 10.000 punti", "pt" to "Alcançou 10.000 pontos"
        ),
        "badge.century_club.description" to mapOf(
            "en" to "Reached 100 total points", "fr" to "A atteint 100 points totaux",
            "es" to "Alcanzó 100 puntos totales", "it" to "Ha raggiunto 100 punti totali", "pt" to "Alcançou 100 pontos totais"
        ),
        "badge.millenium_club.description" to mapOf(
            "en" to "Reached 1,000 total points", "fr" to "A atteint 1 000 points totaux",
            "es" to "Alcanzó 1.000 puntos totales", "it" to "Ha raggiunto 1.000 punti totali", "pt" to "Alcançou 1.000 pontos totais"
        )
    )

    /**
     * Gets the localized badge name for a given key and locale.
     */
    fun localizedBadgeName(nameKey: String, locale: String = "fr"): String? {
        return badgeNames[nameKey]?.get(locale) ?: badgeNames[nameKey]?.get("fr")
    }

    /**
     * Gets the localized badge description for a given key and locale.
     */
    fun localizedBadgeDescription(descriptionKey: String, locale: String = "fr"): String? {
        return badgeDescriptions[descriptionKey]?.get(locale) ?: badgeDescriptions[descriptionKey]?.get("fr")
    }
}

/**
 * Visual asset definition for a badge.
 *
 * @property id Unique identifier for the badge (e.g., "badge-super-organizer")
 * @property name Display name of the badge (e.g., "Super Organisateur")
 * @property description Description of how to earn the badge
 * @property icon Emoji or icon identifier for display
 * @property color Hex color code (Material Design 3 palette)
 * @property rarity Rarity level affecting display and prestige
 * @property category Category of the badge for organization
 * @property pointsReward Points awarded when badge is unlocked
 * @property requirement Threshold needed to unlock (events count, votes, etc.)
 */
@Serializable
data class BadgeAsset(
    val id: String,
    val name: String,
    val nameKey: String,
    val description: String,
    val descriptionKey: String,
    val icon: String,
    val color: String,
    val rarity: BadgeRarity,
    val category: BadgeCategory,
    val pointsReward: Int,
    val requirement: Int
) {
    /**
     * Gets the localized badge name for the given locale.
     * Falls back to French, then to the default [name].
     */
    fun localizedName(locale: String = "fr"): String {
        return BadgeAssets.localizedBadgeName(nameKey, locale) ?: name
    }

    /**
     * Gets the localized badge description for the given locale.
     * Falls back to French, then to the default [description].
     */
    fun localizedDescription(locale: String = "fr"): String {
        return BadgeAssets.localizedBadgeDescription(descriptionKey, locale) ?: description
    }
}
