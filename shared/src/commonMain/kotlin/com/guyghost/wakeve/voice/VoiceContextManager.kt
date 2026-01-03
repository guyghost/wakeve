package com.guyghost.wakeve.voice

import com.guyghost.wakeve.ml.Language
import com.guyghost.wakeve.ml.VoiceContext
import com.guyghost.wakeve.ml.VoiceIntent
import com.guyghost.wakeve.ml.VoiceSession
import com.guyghost.wakeve.ml.VoiceStep
import kotlinx.datetime.Clock

/**
 * Manages the context for multi-step voice assistant sessions.
 * Tracks the current step in the event creation flow and provides
 * appropriate prompts and transitions.
 *
 * This manager ensures that the voice assistant guides users through
 * the multi-step process of event creation (title → description → date → participants → confirm)
 * and handles transitions between steps based on user commands.
 */
class VoiceContextManager {

    /**
     * Updates the session context based on the intent that was just processed.
     *
     * @param session The current voice session
     * @param intent The intent that was just executed
     * @return Updated VoiceContext with the next step information
     */
    fun updateContext(
        session: VoiceSession,
        intent: VoiceIntent
    ): VoiceContext {
        val currentContext = session.context

        return when (intent) {
            // Event creation flow transitions
            VoiceIntent.CREATE_EVENT -> {
                // Start of new event creation
                currentContext.copy(
                    step = VoiceStep.TITLE,
                    eventId = currentContext.eventId ?: generateTempEventId()
                )
            }

            VoiceIntent.SET_TITLE -> {
                // After title is set, move to description
                currentContext.copy(step = VoiceStep.DESCRIPTION)
            }

            VoiceIntent.SET_DESCRIPTION -> {
                // After description is set, move to date
                currentContext.copy(step = VoiceStep.DATE)
            }

            VoiceIntent.SET_DATE -> {
                // After date is set, move to participants
                currentContext.copy(step = VoiceStep.PARTICIPANTS)
            }

            VoiceIntent.SET_PARTICIPANTS -> {
                // After participants are set, move to confirmation
                currentContext.copy(step = VoiceStep.CONFIRM)
            }

            // Poll-related intents - stay in current context but update eventId if needed
            VoiceIntent.ADD_SLOT -> currentContext

            VoiceIntent.CONFIRM_POLL -> currentContext

            // Quick actions - no context change needed
            VoiceIntent.SEND_INVITATIONS -> currentContext

            VoiceIntent.OPEN_CALENDAR -> currentContext

            VoiceIntent.CANCEL_EVENT -> {
                // Reset context after cancellation
                currentContext.copy(
                    eventId = null,
                    step = VoiceStep.COMPLETE,
                    suggestionsProvided = false
                )
            }

            VoiceIntent.GET_STATS -> currentContext
        }
    }

    /**
     * Gets the appropriate prompt for the current step in the conversation.
     * These prompts should be read aloud or displayed to guide the user.
     *
     * @param context The current voice context
     * @return The prompt string to show the user
     */
    fun getNextStepPrompt(context: VoiceContext): String {
        return when (context.step) {
            VoiceStep.TITLE -> getTitlePrompt(context.language)
            VoiceStep.DESCRIPTION -> getDescriptionPrompt(context.language)
            VoiceStep.DATE -> getDatePrompt(context.language)
            VoiceStep.PARTICIPANTS -> getParticipantsPrompt(context.language)
            VoiceStep.CONFIRM -> getConfirmPrompt(context.language)
            VoiceStep.COMPLETE -> getCompletePrompt(context.language)
        }
    }

    /**
     * Gets the completion message after a successful event creation.
     *
     * @param eventId The ID of the created event
     * @param language The user's language
     * @return Completion message
     */
    fun getCompletionMessage(eventId: String, language: Language): String {
        return when (language) {
            Language.EN -> "Event created successfully! You can now add time slots and invite participants."
            Language.FR -> "Événement créé avec succès ! Vous pouvez maintenant ajouter des créneaux et inviter des participants."
            Language.ES -> "¡Evento creado con éxito! Ahora puedes añadir horarios e invitar a participantes."
            Language.DE -> "Veranstaltung erfolgreich erstellt! Du kannst jetzt Zeiträume hinzufügen und Teilnehmer einladen."
            Language.IT -> "Evento creato con successo! Ora puoi aggiungere fasce orarie e invitare partecipanti."
        }
    }

    /**
     * Gets a suggestion message based on event type.
     *
     * @param eventType The type of event being created
     * @param language The user's language
     * @return Suggestion message
     */
    fun getEventTypeSuggestion(eventType: String?, language: Language): String? {
        if (eventType == null) return null

        return when (language) {
            Language.EN -> when (eventType.uppercase()) {
                "WEDDING" -> "For a wedding, you might want to suggest a weekend date and a venue that can accommodate all your guests."
                "BIRTHDAY" -> "Birthday parties are usually more fun in the afternoon or evening. Would you like me to suggest some time slots?"
                "TEAM_BUILDING" -> "Team building events work best on weekdays. Let me know if you need help finding a venue."
                "CULTURAL_EVENT" -> "Museums and theaters often have specific opening hours. Would you like suggestions for cultural venues?"
                "SPORT_EVENT" -> "For sports events, make sure to book a field or court in advance. How many participants are you expecting?"
                else -> null
            }
            Language.FR -> when (eventType.uppercase()) {
                "MARIAGE" -> "Pour un mariage, je vous recommande de choisir un week-end et une salle qui peut accueillir tous vos invités."
                "ANNIVERSAIRE" -> "Les anniversaires sont généralement plus sympas l'après-midi ou le soir. Voulez-vous que je vous propose des créneaux ?"
                "TEAM_BUILDING" -> "Les team building fonctionnent mieux en semaine. Avez-vous besoin d'aide pour trouver un lieu ?"
                "CULTURAL_EVENT" -> "Les musées et théâtres ont souvent des horaires spécifiques. Voulez-vous des suggestions de lieux culturels ?"
                "SPORT_EVENT" -> "Pour les événements sportifs, pensez à réserver un terrain à l'avance. Combien de participants ожидаez-vous ?"
                else -> null
            }
            Language.ES -> when (eventType.uppercase()) {
                "BODA" -> "Para una boda, te recomiendo elegir un fin de semana y un lugar que pueda acomodar a todos tus invitados."
                "CUMPLEAÑOS" -> "Los cumpleaños suelen ser más divertidos por la tarde. ¿Quieres que te sugiera algunos horarios?"
                else -> null
            }
            Language.DE -> when (eventType.uppercase()) {
                "HOCHZEIT" -> "Für eine Hochzeit empfehle ich, ein Wochenende und einen Veranstaltungsort zu wählen, der alle Gäste aufnehmen kann."
                else -> null
            }
            Language.IT -> null
        }
    }

    /**
     * Determines if the current step requires a response from the user.
     *
     * @param context The current voice context
     * @return True if waiting for user input, false if just providing information
     */
    fun isWaitingForInput(context: VoiceContext): Boolean {
        return context.step in listOf(
            VoiceStep.TITLE,
            VoiceStep.DESCRIPTION,
            VoiceStep.DATE,
            VoiceStep.PARTICIPANTS,
            VoiceStep.CONFIRM
        )
    }

    /**
     * Gets the default step if no context is available.
     *
     * @return Initial context for a new session
     */
    fun getInitialContext(language: Language): VoiceContext {
        return VoiceContext(
            eventId = null,
            step = VoiceStep.COMPLETE, // Ready to accept new commands
            language = language,
            suggestionsProvided = false
        )
    }

    /**
     * Resets the context after event creation is complete or cancelled.
     *
     * @param language The user's language
     * @return Fresh context for a new session
     */
    fun resetContext(language: Language): VoiceContext {
        return VoiceContext(
            eventId = null,
            step = VoiceStep.COMPLETE,
            language = language,
            suggestionsProvided = false
        )
    }

    // Private prompt generation methods
    private fun getTitlePrompt(language: Language): String {
        return when (language) {
            Language.EN -> "What is the title of your event?"
            Language.FR -> "Quel est le titre de votre événement ?"
            Language.ES -> "¿Cuál es el título de tu evento?"
            Language.DE -> "Wie lautet der Titel deiner Veranstaltung?"
            Language.IT -> "Qual è il titolo del tuo evento?"
        }
    }

    private fun getDescriptionPrompt(language: Language): String {
        return when (language) {
            Language.EN -> "What is the description or additional details for this event?"
            Language.FR -> "Quelle est la description ou les détails supplémentaires de cet événement ?"
            Language.ES -> "¿Cuál es la descripción o los detalles adicionales para este evento?"
            Language.DE -> "Wie lautet die Beschreibung oder weitere Details für diese Veranstaltung?"
            Language.IT -> "Qual è la descrizione o i dettagli aggiuntivi per questo evento?"
        }
    }

    private fun getDatePrompt(language: Language): String {
        return when (language) {
            Language.EN -> "What date and time would you like to schedule this event?"
            Language.FR -> "Quelle date et heure souhaitez-vous pour cet événement ?"
            Language.ES -> "¿Qué fecha y hora te gustaría programar para este evento?"
            Language.DE -> "Welches Datum und welche Uhrzeit möchtest du für diese Veranstaltung planen?"
            Language.IT -> "Quale data e orario vorresti programmare per questo evento?"
        }
    }

    private fun getParticipantsPrompt(language: Language): String {
        return when (language) {
            Language.EN -> "How many participants do you expect?"
            Language.FR -> "Combien de participants attendez-vous ?"
            Language.ES -> "¿Cuántos participantes esperas?"
            Language.DE -> "Wie viele Teilnehmer erwartest du?"
            Language.IT -> "Quanti partecipanti ti aspetti?"
        }
    }

    private fun getConfirmPrompt(language: Language): String {
        return when (language) {
            Language.EN -> "Would you like me to create this event with the information provided? Please confirm with \"yes\" or \"no\"."
            Language.FR -> "Voulez-vous que je crée cet événement avec les informations fournies ? Confirmez avec \"oui\" ou \"non\"."
            Language.ES -> "¿Te gustaría que creara este evento con la información proporcionada? Confirma con \"sí\" o \"no\"."
            Language.DE -> "Möchtest du, dass ich diese Veranstaltung mit den bereitgestellten Informationen erstelle? Bitte bestätige mit \"ja\" oder \"nein\"."
            Language.IT -> "Vorresti che creassi questo evento con le informazioni fornite? Conferma con \"sì\" o \"no\"."
        }
    }

    private fun getCompletePrompt(language: Language): String {
        return when (language) {
            Language.EN -> "Event created successfully! You can now add time slots, invite participants, or ask me for suggestions."
            Language.FR -> "Événement créé avec succès ! Vous pouvez maintenant ajouter des créneaux, inviter des participants ou me demander des suggestions."
            Language.ES -> "¡Evento creado con éxito! Ahora puedes añadir horarios, invitar a participantes o pedirme sugerencias."
            Language.DE -> "Veranstaltung erfolgreich erstellt! Du kannst jetzt Zeiträume hinzufügen, Teilnehmer einladen oder mich um Vorschläge bitten."
            Language.IT -> "Evento creato con successo! Ora puoi aggiungere fasce orarie, invitare partecipanti o chiedermi suggerimenti."
        }
    }

    private fun generateTempEventId(): String {
        return "temp_${Clock.System.now().toEpochMilliseconds()}"
    }
}
