package com.guyghost.wakeve.presentation.statemachine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for the base [StateMachine] class.
 *
 * Verifies:
 * - Initial state is correct
 * - State updates are immutable
 * - Intents are dispatched correctly
 * - Side effects are emitted
 * - StateFlow and Channel work properly
 * - Thread-safety with concurrent dispatch
 */
class StateMachineTest {

    // ========================================================================
    // Test Data Classes
    // ========================================================================

    data class TestState(
        val value: String = "initial",
        val counter: Int = 0
    )

    sealed interface TestIntent {
        data class SetValue(val newValue: String) : TestIntent
        data class Increment(val unused: Unit = Unit) : TestIntent
        data class EmitEffect(val effect: TestEffect) : TestIntent
    }

    sealed interface TestEffect {
        data class ValueChanged(val value: String) : TestEffect
        data object CounterIncremented : TestEffect
    }

    /**
     * Minimal test state machine for testing base class functionality.
     */
    class TestStateMachine(scope: CoroutineScope) : StateMachine<TestState, TestIntent, TestEffect>(
        initialState = TestState(),
        scope = scope
    ) {
        override suspend fun handleIntent(intent: TestIntent) {
            when (intent) {
                is TestIntent.SetValue -> {
                    updateState { it.copy(value = intent.newValue) }
                }
                is TestIntent.Increment -> {
                    updateState { it.copy(counter = it.counter + 1) }
                }
                is TestIntent.EmitEffect -> {
                    emitSideEffect(intent.effect)
                }
            }
        }
    }

    // ========================================================================
    // Tests
    // ========================================================================

    @Test
    fun testInitialState() = runTest {
        val scope = CoroutineScope(StandardTestDispatcher(testScheduler) + SupervisorJob())
        val stateMachine = TestStateMachine(scope)

        val currentState = stateMachine.state.value

        assertEquals("initial", currentState.value)
        assertEquals(0, currentState.counter)
    }

    @Test
    fun testDispatchIntent() = runTest {
        val scope = CoroutineScope(StandardTestDispatcher(testScheduler) + SupervisorJob())
        val stateMachine = TestStateMachine(scope)

        stateMachine.dispatch(TestIntent.SetValue("updated"))
        advanceUntilIdle()

        val currentState = stateMachine.state.value
        assertEquals("updated", currentState.value)
    }

    @Test
    fun testUpdateState() = runTest {
        val scope = CoroutineScope(StandardTestDispatcher(testScheduler) + SupervisorJob())
        val stateMachine = TestStateMachine(scope)

        // Dispatch intent to update state
        stateMachine.dispatch(TestIntent.Increment())
        advanceUntilIdle()

        val firstState = stateMachine.state.value
        assertEquals(1, firstState.counter)

        // Dispatch another increment
        stateMachine.dispatch(TestIntent.Increment())
        advanceUntilIdle()

        val secondState = stateMachine.state.value
        assertEquals(2, secondState.counter)

        // Verify immutability - old state should not have changed
        assertEquals(1, firstState.counter)
    }

    @Test
    fun testEmitSideEffect() = runTest {
        val scope = CoroutineScope(StandardTestDispatcher(testScheduler) + SupervisorJob())
        val stateMachine = TestStateMachine(scope)

        // Dispatch intent that emits side effect
        stateMachine.dispatch(TestIntent.EmitEffect(TestEffect.ValueChanged("new")))
        advanceUntilIdle()

        // Collect first effect
        val effect = stateMachine.sideEffect.first()
        assertEquals(TestEffect.ValueChanged("new"), effect)
    }

    @Test
    fun testSideEffectReceivedOnce() = runTest {
        val scope = CoroutineScope(StandardTestDispatcher(testScheduler) + SupervisorJob())
        val stateMachine = TestStateMachine(scope)

        val emittedEffects = mutableListOf<TestEffect>()

        // Dispatch multiple intents that emit side effects
        stateMachine.dispatch(TestIntent.EmitEffect(TestEffect.CounterIncremented))
        advanceUntilIdle()

        stateMachine.dispatch(TestIntent.EmitEffect(TestEffect.CounterIncremented))
        advanceUntilIdle()

        // Collect both effects
        emittedEffects.add(stateMachine.sideEffect.first())
        emittedEffects.add(stateMachine.sideEffect.first())

        // Verify each side effect is received once
        assertEquals(2, emittedEffects.size)
        assertTrue(emittedEffects.all { it is TestEffect.CounterIncremented })
    }

    @Test
    fun testConcurrentDispatch() = runTest {
        val scope = CoroutineScope(StandardTestDispatcher(testScheduler) + SupervisorJob())
        val stateMachine = TestStateMachine(scope)

        // Dispatch multiple intents concurrently
        repeat(10) { index ->
            stateMachine.dispatch(TestIntent.Increment())
        }
        advanceUntilIdle()

        // All increments should have been processed
        assertEquals(10, stateMachine.state.value.counter)
    }

    @Test
    fun testStateFlowCollection() = runTest {
        val scope = CoroutineScope(StandardTestDispatcher(testScheduler) + SupervisorJob())
        val stateMachine = TestStateMachine(scope)

        val collectedStates = mutableListOf<TestState>()

        // Collect initial state
        collectedStates.add(stateMachine.state.value)
        assertEquals("initial", collectedStates[0].value)

        // Dispatch intent to change state
        stateMachine.dispatch(TestIntent.SetValue("first"))
        advanceUntilIdle()

        collectedStates.add(stateMachine.state.value)
        assertEquals("first", collectedStates[1].value)

        // Dispatch another change
        stateMachine.dispatch(TestIntent.SetValue("second"))
        advanceUntilIdle()

        collectedStates.add(stateMachine.state.value)
        assertEquals("second", collectedStates[2].value)

        assertEquals(3, collectedStates.size)
    }

    @Test
    fun testSideEffectFlowCollection() = runTest {
        val scope = CoroutineScope(StandardTestDispatcher(testScheduler) + SupervisorJob())
        val stateMachine = TestStateMachine(scope)

        val collectedEffects = mutableListOf<TestEffect>()

        // Dispatch intents that emit side effects
        stateMachine.dispatch(TestIntent.EmitEffect(TestEffect.ValueChanged("value1")))
        advanceUntilIdle()

        stateMachine.dispatch(TestIntent.EmitEffect(TestEffect.CounterIncremented))
        advanceUntilIdle()

        stateMachine.dispatch(TestIntent.EmitEffect(TestEffect.ValueChanged("value2")))
        advanceUntilIdle()

        // Collect all effects
        collectedEffects.add(stateMachine.sideEffect.first())
        collectedEffects.add(stateMachine.sideEffect.first())
        collectedEffects.add(stateMachine.sideEffect.first())

        // All effects should be collected in order
        assertEquals(3, collectedEffects.size)
        assertTrue(collectedEffects[0] is TestEffect.ValueChanged)
        assertTrue(collectedEffects[1] is TestEffect.CounterIncremented)
        assertTrue(collectedEffects[2] is TestEffect.ValueChanged)
    }
}
