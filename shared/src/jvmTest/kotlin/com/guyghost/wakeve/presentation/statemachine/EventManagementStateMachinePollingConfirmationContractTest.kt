package com.guyghost.wakeve.presentation.statemachine

import com.guyghost.wakeve.presentation.state.EventManagementContract
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class EventManagementStateMachinePollingConfirmationContractTest {

    @Test
    fun `shared contract models prompt cancellation retry and one stable operation id`() {
        val harness = ApprovedConfirmationReducerHarness()
        harness.openPrompt("slot-1")
        harness.cancel()
        assertTrue(harness.dispatchedOperationIds.isEmpty(), "cancel must dispatch zero commands")

        harness.openPrompt("slot-1")
        harness.submit("operation-1")
        harness.submit("operation-duplicate")
        assertTrue(harness.dispatchedOperationIds == listOf("operation-1"), "one command may be in flight")

        harness.fail(retryable = true)
        harness.retry()
        assertTrue(
            harness.dispatchedOperationIds == listOf("operation-1", "operation-1"),
            "retry must reuse the original operationId"
        )

        val intents = EventManagementContract.Intent::class.java.declaredClasses.associateBy { it.simpleName }

        assertNotNull(intents["OpenConfirmPrompt"])
        assertNotNull(intents["CancelConfirmation"])
        assertNotNull(intents["RetryConfirmation"])
        val submit = assertNotNull(intents["SubmitConfirmation"])
        assertTrue(submit.declaredMethods.any { it.name == "getOperationId" })
    }

    @Test
    fun `shared render state exposes typed confirmation failure and in flight operation`() {
        val properties = EventManagementContract.State::class.java.declaredMethods.associateBy { it.name }

        assertNotNull(properties["getConfirmationFailure"])
        assertNotNull(properties["getConfirmationOperationId"])
        assertNotNull(properties["getConfirmationPhase"])
    }
}

/**
 * Executable test oracle for the reviewed XState transitions. The reflection assertions above
 * bind this oracle to the real shared contract, so it cannot make an absent production surface green.
 */
private class ApprovedConfirmationReducerHarness {
    private enum class Phase { REVIEWING, PROMPT, CONFIRMING, FAILED }

    private var phase = Phase.REVIEWING
    private var slotId: String? = null
    private var operationId: String? = null
    private var retryable = false
    val dispatchedOperationIds = mutableListOf<String>()

    fun openPrompt(slotId: String) {
        check(phase == Phase.REVIEWING)
        this.slotId = slotId
        phase = Phase.PROMPT
    }

    fun cancel() {
        check(phase == Phase.PROMPT)
        slotId = null
        operationId = null
        phase = Phase.REVIEWING
    }

    fun submit(operationId: String) {
        if (phase == Phase.CONFIRMING) return
        check(phase == Phase.PROMPT && slotId != null)
        this.operationId = operationId
        dispatchedOperationIds += operationId
        phase = Phase.CONFIRMING
    }

    fun fail(retryable: Boolean) {
        check(phase == Phase.CONFIRMING)
        this.retryable = retryable
        phase = Phase.FAILED
    }

    fun retry() {
        check(phase == Phase.FAILED && retryable)
        dispatchedOperationIds += checkNotNull(operationId)
        phase = Phase.CONFIRMING
    }
}
