package com.guyghost.wakeve.ui.event

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guyghost.wakeve.EventRepositoryInterface
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.repository.OrderBy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing paginated event lists.
 *
 * Handles loading events in pages, tracking loading state, and managing errors.
 * Supports offline operation with automatic loading of cached data.
 */
class PaginatedEventViewModel(
    private val eventRepository: EventRepositoryInterface,
    private val pageSize: Int = 50
) : ViewModel(), IPaginatedEventViewModel {

    private val _events = MutableStateFlow<List<Event>>(emptyList())
    override val events: StateFlow<List<Event>> = _events.asStateFlow()

    private val _isLoadingNextPage = MutableStateFlow(false)
    override val isLoadingNextPage: StateFlow<Boolean> = _isLoadingNextPage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    override val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    override val error: StateFlow<String?> = _error.asStateFlow()

    private val _hasMorePages = MutableStateFlow(true)
    override val hasMorePages: StateFlow<Boolean> = _hasMorePages.asStateFlow()

    private var currentPage = 0
    private var hasMorePagesValue = true

    init {
        loadInitialPage()
    }

    private fun loadInitialPage() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                eventRepository.getEventsPaginated(
                    page = 0,
                    pageSize = pageSize,
                    orderBy = OrderBy.CREATED_AT_DESC
                ).collect { events ->
                    _events.value = events
                    hasMorePagesValue = events.size == pageSize
                    _hasMorePages.value = hasMorePagesValue
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _error.value = e.message
                _isLoading.value = false
            }
        }
    }

    override fun loadNextPage() {
        if (_isLoadingNextPage.value || !hasMorePagesValue) return

        viewModelScope.launch {
            _isLoadingNextPage.value = true
            try {
                currentPage++
                eventRepository.getEventsPaginated(
                    page = currentPage,
                    pageSize = pageSize,
                    orderBy = OrderBy.CREATED_AT_DESC
                ).collect { newEvents ->
                    _events.value = _events.value + newEvents
                    hasMorePagesValue = newEvents.size == pageSize
                    _hasMorePages.value = hasMorePagesValue
                }
            } catch (e: Exception) {
                _error.value = e.message
                currentPage-- // Rollback on error
            } finally {
                _isLoadingNextPage.value = false
            }
        }
    }

    override fun refresh() {
        currentPage = 0
        hasMorePagesValue = true
        _hasMorePages.value = true
        loadInitialPage()
    }
}
