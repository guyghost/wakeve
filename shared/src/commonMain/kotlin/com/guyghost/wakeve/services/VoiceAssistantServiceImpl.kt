package com.guyghost.wakeve.services

import com.guyghost.wakeve.EventRepositoryInterface
import com.guyghost.wakeve.calendar.CalendarService
import com.guyghost.wakeve.ml.Language
import com.guyghost.wakeve.ml.VoiceCommand
import com.guyghost.wakeve.ml.VoiceContext
import com.guyghost.wakeve.ml.VoiceSession
import com.guyghost.wakeve.ml.VoiceSession as VoiceSessionModel
import com.guyghost.wakeve.ml.SessionStatus
import com.guyghost.wakeve.ml.VoiceIntent
import com.guyghost.wakeve.models.EventType
import com.guyghost.wakeve.voice.VoiceCommandParser
import com.guyghost.wakeve.voice.VoiceContextManager
import com.guyghost.wakeve.voice.handlers.VoiceEventHandlers
import com.guyghost.wakeve.voice.handlers.VoicePollHandlers
import com.guyghost.wakeve.voice.handlers.VoiceActionHandlers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Implementation of the intelligent voice assistant service.
 * Provides voice-based event management with multi-step conversation support.
 *
 * This implementation:
 * - Parses natural language commands into structured intents
 * - Manages multi-step conversation flow for event creation
 * - Provides contextual suggestions based on event type
 * - Handles all supported voice intents
 *
 * @property voiceCommandParser Parser for extracting intents and parameters from voice transcripts
 * @property eventRepository Repository for event data persistence
 * @property calendarService Optional service for calendar integration
 */
class VoiceAssistantServiceImpl(
    private val voiceCommandParser: VoiceCommandParser,
    private val eventRepository: EventRepositoryInterface,
    private val calendarService: CalendarService? = null
) : VoiceAssistantService {

    // In-memory session storage (in production, use database)
    private val sessions = mutableMapOf<String, VoiceSessionModel>()
    private val sessionMutex = Mutex()

    // Handlers for different intent categories
    private val eventHandlers = VoiceEventHandlers(eventRepository)
    private val pollHandlers = VoicePollHandlers(eventRepository)
    private val actionHandlers = VoiceActionHandlers(eventRepository, calendarService)
    private val contextManager = VoiceContextManager()

    /**
     * Start a new voice assistant session.
     * Creates a new session tracking the multi-step conversation flow.
     *
     * @param userId ID of the user starting the session
     * @param language Language for speech recognition (default: FR)
     * @return VoiceResult containing the new session or error
     */
    override suspend fun startSession(
        userId: String,
        language: Language
    ): VoiceResult<VoiceSession> {
        return try {
            val sessionId = generateSessionId()
            val timestamp = getCurrentTimestamp()

            val session = VoiceSessionModel(
                sessionId = sessionId,
                userId = userId,
                commands = emptyList(),
                context = VoiceContext(
                    eventId = null,
                    step = com.guyghost.wakeve.ml.VoiceStep.COMPLETE,
                    language = language,
                    suggestionsProvided = false
                ),
                status = SessionStatus.ACTIVE,
                startTime = timestamp,
                endTime = null
            )

            // Save session
            sessionMutex.withLock {
                sessions[sessionId] = session
            }

            VoiceResult.Success(session)
        } catch (e: Exception) {
            VoiceResult.Error(e)
        }
    }

    /**
     * Process a voice command transcript and extract intent/parameters.
     * Uses NLP to classify intent and extract entities like dates and counts.
     *
     * @param sessionId ID of the active session
     * @param transcript Text transcript of the voice command
     * @return VoiceResult containing the processed command or error
     */
    override suspend fun processCommand(
        sessionId: String,
        transcript: String
    ): VoiceResult<VoiceCommand> {
        return try {
            // Get current session
            val session = sessionMutex.withLock {
                sessions[sessionId]
            } ?: return VoiceResult.Error(IllegalStateException("Session not found: $sessionId"))

            if (session.status != SessionStatus.ACTIVE) {
                return VoiceResult.Error(IllegalStateException("Session is not active"))
            }

            // Parse transcript into intent
            val command = voiceCommandParser.parse(
                transcript = transcript,
                language = session.context.language,
                context = session.context
            )

            // Execute the command
            val executionResult = executeCommand(session, command)

            // Update session context based on intent
            val updatedContext = contextManager.updateContext(session, command.intent)

            // Add command to session history
            val updatedSession = session.copy(
                commands = session.commands + command,
                context = updatedContext
            )

            // Save updated session
            sessionMutex.withLock {
                sessions[sessionId] = updatedSession
            }

            // Return result with execution status
            VoiceResult.Success(command)
        } catch (e: Exception) {
            VoiceResult.Error(e)
        }
    }

    /**
     * Get contextual suggestions based on the current event being created.
     * Suggestions are personalized based on event type, location, and preferences.
     *
     * @param eventId ID of the event (null if new event)
     * @param eventType Type of event being created (null if unknown)
     * @return VoiceResult containing list of suggestions or error
     */
    override suspend fun getContextualSuggestions(
        eventId: String?,
        eventType: EventType?
    ): VoiceResult<List<String>> {
        return try {
            val suggestions = generateSuggestions(eventId, eventType)
            VoiceResult.Success(suggestions)
        } catch (e: Exception) {
            VoiceResult.Error(e)
        }
    }

    /**
     * End an active voice session.
     * Marks the session as completed or cancelled.
     *
     * @param sessionId ID of the session to end
     * @return VoiceResult containing the completed session or error
     */
    override suspend fun endSession(sessionId: String): VoiceResult<VoiceSession> {
        return try {
            val session = sessionMutex.withLock {
                sessions[sessionId]
            } ?: return VoiceResult.Error(IllegalStateException("Session not found: $sessionId"))

            val completedSession = session.copy(
                status = SessionStatus.COMPLETED,
                endTime = getCurrentTimestamp()
            )

            sessionMutex.withLock {
                sessions[sessionId] = completedSession
            }

            VoiceResult.Success(completedSession)
        } catch (e: Exception) {
            VoiceResult.Error(e)
        }
    }

    /**
     * Executes a parsed voice command by dispatching to the appropriate handler.
     *
     * @param session The current voice session
     * @param command The parsed voice command
     * @return Result indicating success or failure
     */
    private suspend fun executeCommand(
        session: VoiceSession,
        command: VoiceCommand
    ): Result<Unit> {
        return when (command.intent) {
            VoiceIntent.CREATE_EVENT -> {
                eventHandlers.handleCreateEvent(session, command).map { }
            }

            VoiceIntent.SET_TITLE -> {
                eventHandlers.handleSetTitle(session, command).map { }
            }

            VoiceIntent.SET_DESCRIPTION -> {
                eventHandlers.handleSetDescription(session, command).map { }
            }

            VoiceIntent.SET_DATE -> {
                eventHandlers.handleSetDate(session, command).map { }
            }

            VoiceIntent.SET_PARTICIPANTS -> {
                eventHandlers.handleSetParticipants(session, command).map { }
            }

            VoiceIntent.ADD_SLOT -> {
                pollHandlers.handleAddSlot(session, command).map { }
            }

            VoiceIntent.CONFIRM_POLL -> {
                pollHandlers.handleConfirmPoll(session, command).map { }
            }

            VoiceIntent.SEND_INVITATIONS -> {
                actionHandlers.handleSendInvitations(session, command).map { }
            }

            VoiceIntent.OPEN_CALENDAR -> {
                actionHandlers.handleOpenCalendar(session, command).map { }
            }

            VoiceIntent.CANCEL_EVENT -> {
                actionHandlers.handleCancelEvent(session, command).map { }
            }

            VoiceIntent.GET_STATS -> {
                // Check if it's event stats or poll stats
                val isPollStats = command.rawTranscript.contains("vot") ||
                    command.rawTranscript.contains("people") ||
                    command.rawTranscript.contains("personnes")

                if (isPollStats) {
                    pollHandlers.handleGetStats(session, command).map { }
                } else {
                    actionHandlers.handleGetEventStats(session, command).map { }
                }
            }
        }
    }

    /**
     * Generates contextual suggestions based on event type and other factors.
     *
     * @param eventId The event ID (if available)
     * @param eventType The type of event
     * @return List of suggestion strings
     */
    private fun generateSuggestions(
        eventId: String?,
        eventType: EventType?
    ): List<String> {
        val suggestions = mutableListOf<String>()

        if (eventType != null) {
            when (eventType) {
                EventType.BIRTHDAY -> suggestions.addAll(
                    listOf(
                        "Penses à prévoir un gâteau et des bougies !",
                        "Veux-tu que je suggère des activités ludiques ?",
                        "As-tu prévu une musique ou un playlist ?"
                    )
                )

                EventType.WEDDING -> suggestions.addAll(
                    listOf(
                        "N'oublie pas de vérifier la disponibilité du lieu pour toute la journée",
                        "Veux-tu que je t'aide à trouver un traiteur ?",
                        "As-tu prévu un фотographe ou un vidéaste ?"
                    )
                )

                EventType.TEAM_BUILDING -> suggestions.addAll(
                    listOf(
                        "Je peux suggérer des activités de cohésion d'équipe",
                        "Penses aux restrictions alimentaires des participants",
                        "Veux-tu que je trouve des idées d'activités originales ?"
                    )
                )

                EventType.CULTURAL_EVENT -> suggestions.addAll(
                    listOf(
                        "Je peux t'aider avec un musée, un théâtre ou une galerie d'art",
                        "Veux-tu que je vérifie les horaires d'ouverture ?",
                        "As-tu pensé à prévoir un guide pour la visite ?"
                    )
                )

                EventType.SPORT_EVENT -> suggestions.addAll(
                    listOf(
                        "Veux-tu organiser un tournoi ou un match amical ?",
                        "Je peux trouver des installations sportives dans la région",
                        "As-tu prévu un arbitre ou un organisateur ?"
                    )
                )

                EventType.PARTY -> suggestions.addAll(
                    listOf(
                        "As-tu prévu une musique ou une playlist ?",
                        "Veux-tu que je suggère des lieux pour faire la fête ?",
                        "N'oublie pas de prévoir des rafraîchissements !"
                    )
                )

                EventType.CONFERENCE -> suggestions.addAll(
                    listOf(
                        "As-tu prévu une salle avec l'équipement nécessaire ?",
                        "Veux-tu que je t'aide à créer l'ordre du jour ?",
                        "N'oublie pas les pauses café !"
                    )
                )

                EventType.FOOD_TASTING -> suggestions.addAll(
                    listOf(
                        "As-tu pris en compte les restrictions alimentaires ?",
                        "Veux-tu que je suggère des restaurants adaptés ?",
                        "Penses à la réservation à l'avance"
                    )
                )

                EventType.OUTDOOR_ACTIVITY -> suggestions.addAll(
                    listOf(
                        "Je peux t'aider à trouver des activités en plein air",
                        "Veux-tu que je vérifie la météo pour le jour J ?",
                        "As-tu prévu un plan B en cas de mauvais temps ?"
                    )
                )

                EventType.FAMILY_GATHERING -> suggestions.addAll(
                    listOf(
                        "Penses aux activités pour tous les âges",
                        "Veux-tu que je suggère des lieux adaptés aux familles ?",
                        "N'oublie pas les préférences de chacun !"
                    )
                )

                EventType.WORKSHOP -> suggestions.addAll(
                    listOf(
                        "As-tu prévu le matériel nécessaire ?",
                        "Veux-tu que je t'aide à trouver un lieu adapté ?",
                        "N'oublie pas de prévoir les pauses"
                    )
                )

                EventType.TECH_MEETUP -> suggestions.addAll(
                    listOf(
                        "As-tu prévu l'équipement technique nécessaire ?",
                        "Veux-tu que je t'aide à promouvoir l'événement ?",
                        "N'oublie pas de prévoir le networking !"
                    )
                )

                EventType.WELLNESS_EVENT -> suggestions.addAll(
                    listOf(
                        "Je peux t'aider à trouver un lieu paisible",
                        "Veux-tu que je suggère des activités relaxantes ?",
                        "As-tu prévu desoptions pour tous les niveaux ?"
                    )
                )

                EventType.CREATIVE_WORKSHOP -> suggestions.addAll(
                    listOf(
                        "As-tu prévu tout le matériel créatif ?",
                        "Veux-tu que je t'aide à trouver un atelier ?",
                        "N'oublie pas de prévoir des защитные fournitures !"
                    )
                )

                EventType.OTHER -> suggestions.addAll(
                    listOf(
                        "Comment puis-je t'aider à organiser cet événement ?",
                        "Veux-tu que je te suggère des idées en fonction du type d'événement ?",
                        "As-tu déjà un lieu en tête ?"
                    )
                )

                EventType.CUSTOM -> suggestions.addAll(
                    listOf(
                        "Peux-tu m'en dire plus sur ton événement personnalisé ?",
                        "Je peux t'aider à personnaliser les suggestions",
                        "As-tu des préférences spécifiques pour cet événement ?"
                    )
                )

                // Handle remaining event types
                else -> suggestions.addAll(
                    listOf(
                        "Comment puis-je t'aider avec ce type d'événement ?",
                        "Veux-tu que je te suggère des idées adaptées ?",
                        "N'hésite pas à me donner plus de détails !"
                    )
                )
            }
        } else {
            suggestions.addAll(
                listOf(
                    "Quel type d'événement veux-tu créer ?",
                    "Je peux t'aider à organiser ton événement étape par étape",
                    "Dis-moi en plus sur ton événement pour que je puisse mieux t'aider"
                )
            )
        }

        return suggestions
    }

    /**
     * Gets the current session by ID.
     */
    private suspend fun getSession(sessionId: String): VoiceSession? {
        return sessionMutex.withLock {
            sessions[sessionId]
        }
    }

    /**
     * Updates an existing session.
     */
    private suspend fun updateSession(session: VoiceSession) {
        sessionMutex.withLock {
            sessions[session.sessionId] = session
        }
    }

    /**
     * Generates a unique session ID.
     */
    private fun generateSessionId(): String {
        return "voice_${Clock.System.now().toEpochMilliseconds()}_${(1000..9999).random()}"
    }

    /**
     * Gets the current timestamp in ISO format.
     */
    private fun getCurrentTimestamp(): String {
        return Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            .toString()
            .replace("T", " ")
            .substringBefore(".")
    }
}
