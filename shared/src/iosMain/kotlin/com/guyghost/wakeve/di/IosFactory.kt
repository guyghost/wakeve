package com.guyghost.wakeve.di

import com.guyghost.wakeve.DatabaseEventRepository
import com.guyghost.wakeve.EventRepositoryInterface
import com.guyghost.wakeve.ScenarioRepository
import com.guyghost.wakeve.comment.CommentCache
import com.guyghost.wakeve.comment.CommentRepository
import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.meeting.MeetingRepository
import com.guyghost.wakeve.meeting.MeetingService
import com.guyghost.wakeve.meeting.MockMeetingPlatformProvider
import com.guyghost.wakeve.presentation.ObservableStateMachine
import com.guyghost.wakeve.presentation.state.EventManagementContract
import com.guyghost.wakeve.presentation.state.MeetingManagementContract
import com.guyghost.wakeve.presentation.state.ScenarioManagementContract
import com.guyghost.wakeve.presentation.statemachine.EventManagementStateMachine
import com.guyghost.wakeve.presentation.statemachine.MeetingServiceStateMachine
import com.guyghost.wakeve.presentation.statemachine.ScenarioManagementStateMachine
import com.guyghost.wakeve.presentation.usecase.CancelMeetingUseCase
import com.guyghost.wakeve.presentation.usecase.CreateEventUseCase
import com.guyghost.wakeve.presentation.usecase.CreateMeetingUseCase
import com.guyghost.wakeve.presentation.usecase.CreateScenarioUseCase
import com.guyghost.wakeve.presentation.usecase.DeleteScenarioUseCase
import com.guyghost.wakeve.presentation.usecase.GenerateMeetingLinkUseCase
import com.guyghost.wakeve.presentation.usecase.LoadEventsUseCase
import com.guyghost.wakeve.presentation.usecase.LoadMeetingsUseCase
import com.guyghost.wakeve.presentation.usecase.LoadScenariosUseCase
import com.guyghost.wakeve.presentation.usecase.UpdateMeetingUseCase
import com.guyghost.wakeve.presentation.usecase.UpdateScenarioUseCase
import com.guyghost.wakeve.presentation.usecase.VoteScenarioUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Factory for creating observable state machines for iOS.
 *
 * This factory is responsible for creating state machine instances that are
 * properly wrapped for SwiftUI consumption. It handles:
 * - Coroutine scope creation with proper dispatchers
 * - Dependency creation
 * - Wrapping state machines in ObservableStateMachine for Swift/iOS
 *
 * ## Usage in iOS/SwiftUI
 *
 * ```swift
 * import SwiftUI
 * import shared
 *
 * class EventListViewModel: ObservableObject {
 *     @Published var state: EventManagementContract.State
 *     private let wrapped: ViewModelWrapper
 *
 *     init() {
 *         // Create the observable state machine
 *         self.wrapped = IosFactory().createEventStateMachine()
 *         self.state = wrapped.currentState as! EventManagementContract.State
 *
 *         // Observe state changes
 *         wrapped.onStateChange = { [weak self] newState in
 *             self?.state = newState as? EventManagementContract.State ?? self?.state ?? .init()
 *         }
 *
 *         // Observe side effects
 *         wrapped.onSideEffect = { [weak self] effect in
 *             self?.handleSideEffect(effect)
 *         }
 *     }
 *
 *     func dispatch(_ intent: EventManagementContract.Intent) {
 *         wrapped.dispatch(intent: intent)
 *     }
 *
 *     private func handleSideEffect(_ effect: Any) {
 *         // Handle navigation, toasts, etc
 *     }
 *
 *     deinit {
 *         wrapped.dispose()
 *     }
 * }
 *
 * struct EventListView: View {
 *     @StateObject private var viewModel = EventListViewModel()
 *
 *     var body: some View {
 *         // Render using viewModel.state
 *     }
 * }
 * ```
 */
object IosFactory {

    /**
     * Create an observable Event Management state machine.
     *
     * Flow:
     * 1. Create a CoroutineScope with Main dispatcher
     * 2. Create dependencies manually (EventRepository, UseCases)
     * 3. Create the state machine with the scope
     * 4. Wrap it in ObservableStateMachine for SwiftUI
     * 5. Return the wrapper
     *
     * The returned wrapper exposes:
     * - `currentState`: The initial state
     * - `onStateChange`: Callback called when state updates
     * - `onSideEffect`: Callback called when side effects emit
     * - `dispatch(intent:)`: Method to dispatch intents
     * - `dispose()`: Method to clean up resources
     *
     * @param database The WakeveDb instance (must be provided by iOS app)
     * @return An ObservableStateMachine wrapper for Event Management
      */
     fun createEventStateMachine(database: WakeveDb): ObservableStateMachine<
             EventManagementContract.State,
             EventManagementContract.Intent,
             EventManagementContract.SideEffect
     > {
        // Create scope with Main dispatcher for iOS
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

        // Create dependencies
        val eventRepository: EventRepositoryInterface = DatabaseEventRepository(database)
        val loadEventsUseCase = LoadEventsUseCase(eventRepository)
        val createEventUseCase = CreateEventUseCase(eventRepository)

        // Create state machine
        val stateMachine = EventManagementStateMachine(
            loadEventsUseCase = loadEventsUseCase,
            createEventUseCase = createEventUseCase,
            eventRepository = eventRepository,
            scope = scope
        )

        // Wrap for SwiftUI
          return ObservableStateMachine(stateMachine)
      }

    /**
     * Create an observable Meeting Service state machine.
     *
     * Flow:
     * 1. Create a CoroutineScope with Main dispatcher
     * 2. Create dependencies manually (MeetingService, MeetingRepository, UseCases)
     * 3. Create a state machine with a scope
     * 4. Wrap it in ObservableStateMachine for SwiftUI
     * 5. Return the wrapper
     *
     * The returned wrapper exposes:
     * - `currentState`: The initial state
     * - `onStateChange`: Callback called when state updates
     * - `onSideEffect`: Callback called when side effects emit
     * - `dispatch(intent:)`: Method to dispatch intents
     * - `dispose()`: Method to clean up resources
     *
     * @param database The WakeveDb instance (must be provided by iOS app)
     * @return An ObservableStateMachine wrapper for Meeting Service
     */
    fun createMeetingStateMachine(database: WakeveDb): ObservableStateMachine<
        MeetingManagementContract.State,
        MeetingManagementContract.Intent,
        MeetingManagementContract.SideEffect
    > {
        // Create scope with Main dispatcher for iOS
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

        // Create dependencies
        val platformCalendarService = com.guyghost.wakeve.calendar.PlatformCalendarServiceImpl()
        val calendarService = com.guyghost.wakeve.calendar.CalendarService(database, platformCalendarService)
        val notificationService = com.guyghost.wakeve.DefaultNotificationService()
        val meetingRepository = MeetingRepository(database)
        val meetingPlatformProvider = MockMeetingPlatformProvider() // Use mock for now, can be replaced with real provider
        val meetingService = MeetingService(
            database = database,
            calendarService = calendarService,
            notificationService = notificationService
        )

        // Create use cases
        val loadMeetingsUseCase = LoadMeetingsUseCase(meetingRepository)
        val createMeetingUseCase = CreateMeetingUseCase(meetingService, meetingRepository)
        val updateMeetingUseCase = UpdateMeetingUseCase(meetingRepository)
        val cancelMeetingUseCase = CancelMeetingUseCase(meetingService, meetingRepository)
        val generateMeetingLinkUseCase = GenerateMeetingLinkUseCase(meetingPlatformProvider, meetingRepository)

        // Create state machine
        val stateMachine = MeetingServiceStateMachine(
            loadMeetingsUseCase = loadMeetingsUseCase,
            createMeetingUseCase = createMeetingUseCase,
            updateMeetingUseCase = updateMeetingUseCase,
            cancelMeetingUseCase = cancelMeetingUseCase,
            generateMeetingLinkUseCase = generateMeetingLinkUseCase,
            scope = scope
        )

        // Wrap for SwiftUI
        return ObservableStateMachine(stateMachine)
    }

    /**
     * Create an observable Scenario Management state machine.
     *
     * Flow:
     * 1. Create a CoroutineScope with Main dispatcher
     * 2. Create dependencies manually (ScenarioRepository, UseCases)
     * 3. Create the state machine with the scope
     * 4. Wrap it in ObservableStateMachine for SwiftUI
     * 5. Return the wrapper
     *
     * The returned wrapper exposes:
     * - `currentState`: The initial state
     * - `onStateChange`: Callback called when state updates
     * - `onSideEffect`: Callback called when side effects emit
     * - `dispatch(intent:)`: Method to dispatch intents
     * - `dispose()`: Method to clean up resources
     *
     * @param database The WakeveDb instance (must be provided by iOS app)
     * @return An ObservableStateMachine wrapper for Scenario Management
     */
    fun createScenarioStateMachine(database: WakeveDb): ObservableStateMachine<
        ScenarioManagementContract.State,
        ScenarioManagementContract.Intent,
        ScenarioManagementContract.SideEffect
    > {
        // Create scope with Main dispatcher for iOS
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

        // Create dependencies
        val scenarioRepository = ScenarioRepository(database)
        val loadScenariosUseCase = LoadScenariosUseCase(scenarioRepository)
        val createScenarioUseCase = CreateScenarioUseCase(scenarioRepository)
        val updateScenarioUseCase = UpdateScenarioUseCase(scenarioRepository)
        val deleteScenarioUseCase = DeleteScenarioUseCase(scenarioRepository)
        val voteScenarioUseCase = VoteScenarioUseCase(scenarioRepository)

        // Create state machine
        val stateMachine = ScenarioManagementStateMachine(
            loadScenariosUseCase = loadScenariosUseCase,
            createScenarioUseCase = createScenarioUseCase,
            updateScenarioUseCase = updateScenarioUseCase,
            deleteScenarioUseCase = deleteScenarioUseCase,
            voteScenarioUseCase = voteScenarioUseCase,
            scope = scope
        )

        // Wrap for SwiftUI
        return ObservableStateMachine(stateMachine)
    }

    /**
     * Create a CommentRepository for iOS.
     *
     * Provides access to comment CRUD operations for the CommentsView.
     *
     * @param database The WakeveDb instance (must be provided by iOS app)
     * @return A CommentRepository instance
     */
    fun createCommentRepository(database: WakeveDb): CommentRepository {
        return CommentRepository(database)
    }
}
