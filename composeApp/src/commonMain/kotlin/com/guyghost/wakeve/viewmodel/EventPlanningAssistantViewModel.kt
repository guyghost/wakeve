package com.guyghost.wakeve.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guyghost.wakeve.ai.EventPlanDraft
import com.guyghost.wakeve.ai.EventPlanningAiAssistant
import com.guyghost.wakeve.ai.EventPlanningAiAvailability
import com.guyghost.wakeve.ai.EventPlanningAiUpdate
import com.guyghost.wakeve.ai.EventPlanningPrompt
import com.guyghost.wakeve.ai.RuleBasedEventPlanningAiAssistant
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EventPlanningAssistantUiState(
    val prompt: String = "",
    val isExtracting: Boolean = false,
    val availability: EventPlanningAiAvailability = EventPlanningAiAvailability.FALLBACK_ONLY,
    val draft: EventPlanDraft? = null,
    val errorMessage: String? = null
)

class EventPlanningAssistantViewModel(
    private val assistant: EventPlanningAiAssistant = RuleBasedEventPlanningAiAssistant()
) : ViewModel() {
    private val _state = MutableStateFlow(EventPlanningAssistantUiState())
    val state: StateFlow<EventPlanningAssistantUiState> = _state.asStateFlow()

    private var extractionJob: Job? = null

    init {
        viewModelScope.launch {
            _state.update { it.copy(availability = assistant.availability()) }
        }
    }

    fun updatePrompt(prompt: String) {
        _state.update { it.copy(prompt = prompt, errorMessage = null) }
    }

    fun extract(referenceYear: Int? = null) {
        val prompt = _state.value.prompt.trim()
        if (prompt.isBlank()) {
            _state.update { it.copy(errorMessage = "Describe the event first.") }
            return
        }

        extractionJob?.cancel()
        extractionJob = viewModelScope.launch {
            _state.update { it.copy(isExtracting = true, draft = null, errorMessage = null) }
            assistant.extractEventPlan(
                EventPlanningPrompt(text = prompt, referenceYear = referenceYear)
            ).collect { update ->
                when (update) {
                    is EventPlanningAiUpdate.Availability -> {
                        _state.update { it.copy(availability = update.availability) }
                    }
                    is EventPlanningAiUpdate.Draft -> {
                        _state.update {
                            it.copy(
                                isExtracting = false,
                                draft = update.draft,
                                errorMessage = null
                            )
                        }
                    }
                    is EventPlanningAiUpdate.Error -> {
                        _state.update {
                            it.copy(
                                isExtracting = false,
                                draft = update.fallbackDraft ?: it.draft,
                                errorMessage = eventPlanExtractionFailureMessage()
                            )
                        }
                    }
                }
            }
            _state.update { it.copy(isExtracting = false) }
        }
    }

    fun clearDraft() {
        _state.update { it.copy(draft = null, errorMessage = null) }
    }
}

internal fun eventPlanExtractionFailureMessage(): String =
    "Impossible d'analyser cet evenement. Precisez les infos et reessayez."
