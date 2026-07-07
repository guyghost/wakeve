import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ActivityRoutesErrorMessageTest {
    @Test
    fun activityFailureMessagesUseStableSafeCopy() {
        val messages = listOf(
            activityListFailureMessage(),
            activityScheduleFailureMessage(),
            activityDateListFailureMessage(),
            activityStatisticsFailureMessage(),
            activityDetailFailureMessage(),
            activityParticipantsFailureMessage(),
            activityCreateFailureMessage(),
            activityRegistrationFailureMessage(),
            activityUpdateFailureMessage(),
            activityDeleteFailureMessage(),
            activityUnregistrationFailureMessage()
        )

        assertEquals(messages.size, messages.distinct().size)
        messages.forEach { message ->
            assertFalse(message.isBlank())
            assertDoesNotExposeSensitiveDetails(message)
        }
    }

    private fun assertDoesNotExposeSensitiveDetails(message: String) {
        listOf(
            "secret@example.com",
            "SECRET",
            "SQL constraint",
            "http://internal.local",
            "token=",
            "activity-"
        ).forEach { sensitiveValue ->
            assertFalse(
                message.contains(sensitiveValue, ignoreCase = true),
                "Message should not expose `$sensitiveValue`: $message"
            )
        }
    }
}
