package com.guyghost.wakeve.notification

enum class NotificationUserValue {
    CRITICAL,
    ACTIONABLE,
    CONTEXTUAL,
    NOISY
}

enum class NotificationSpamRisk {
    LOW,
    MEDIUM,
    HIGH
}

data class NotificationDeliveryProfile(
    val priority: NotificationPriority,
    val userValue: NotificationUserValue,
    val spamRisk: NotificationSpamRisk,
    val bypassQuietHours: Boolean,
    val batchWhenPossible: Boolean,
    val reason: String
)

fun NotificationType.deliveryProfile(): NotificationDeliveryProfile =
    when (this) {
        NotificationType.EVENT_INVITE -> NotificationDeliveryProfile(
            priority = NotificationPriority.HIGH,
            userValue = NotificationUserValue.ACTIONABLE,
            spamRisk = NotificationSpamRisk.LOW,
            bypassQuietHours = false,
            batchWhenPossible = false,
            reason = "Nouvelle invitation: l'utilisateur doit savoir qu'une decision de presence est attendue."
        )

        NotificationType.DATE_CONFIRMED,
        NotificationType.SCENARIO_SELECTED -> NotificationDeliveryProfile(
            priority = NotificationPriority.HIGH,
            userValue = NotificationUserValue.CRITICAL,
            spamRisk = NotificationSpamRisk.LOW,
            bypassQuietHours = false,
            batchWhenPossible = false,
            reason = "Decision structurante: date ou scenario final change la suite de l'organisation."
        )

        NotificationType.MEETING_REMINDER -> NotificationDeliveryProfile(
            priority = NotificationPriority.URGENT,
            userValue = NotificationUserValue.CRITICAL,
            spamRisk = NotificationSpamRisk.LOW,
            bypassQuietHours = true,
            batchWhenPossible = false,
            reason = "Rappel imminent: l'utilisateur risque de manquer le depart ou la reunion."
        )

        NotificationType.PAYMENT_DUE -> NotificationDeliveryProfile(
            priority = NotificationPriority.MEDIUM,
            userValue = NotificationUserValue.ACTIONABLE,
            spamRisk = NotificationSpamRisk.MEDIUM,
            bypassQuietHours = false,
            batchWhenPossible = false,
            reason = "Budget ou remboursement a traiter: utile, mais pas assez urgent pour couper le calme."
        )

        NotificationType.VOTE_REMINDER,
        NotificationType.VOTE_CLOSE_REMINDER,
        NotificationType.DEADLINE_REMINDER,
        NotificationType.MENTION -> NotificationDeliveryProfile(
            priority = NotificationPriority.MEDIUM,
            userValue = NotificationUserValue.ACTIONABLE,
            spamRisk = NotificationSpamRisk.MEDIUM,
            bypassQuietHours = false,
            batchWhenPossible = false,
            reason = "Action attendue: relancer le vote, repondre a une mention ou traiter une echeance."
        )

        NotificationType.NEW_SCENARIO -> NotificationDeliveryProfile(
            priority = NotificationPriority.MEDIUM,
            userValue = NotificationUserValue.CONTEXTUAL,
            spamRisk = NotificationSpamRisk.MEDIUM,
            bypassQuietHours = false,
            batchWhenPossible = true,
            reason = "Nouvelle option a comparer: utile, mais plusieurs ajouts doivent etre regroupes."
        )

        NotificationType.EVENT_UPDATE -> NotificationDeliveryProfile(
            priority = NotificationPriority.LOW,
            userValue = NotificationUserValue.CONTEXTUAL,
            spamRisk = NotificationSpamRisk.HIGH,
            bypassQuietHours = false,
            batchWhenPossible = true,
            reason = "Changement de programme: utile seulement si regroupe ou clairement actionnable."
        )

        NotificationType.NEW_COMMENT,
        NotificationType.COMMENT_REPLY -> NotificationDeliveryProfile(
            priority = NotificationPriority.LOW,
            userValue = NotificationUserValue.NOISY,
            spamRisk = NotificationSpamRisk.HIGH,
            bypassQuietHours = false,
            batchWhenPossible = true,
            reason = "Conversation: a regrouper pour eviter que Wakeve ressemble a un chat bruyant."
        )
    }

fun NotificationType.isHighSpamRisk(): Boolean =
    deliveryProfile().spamRisk == NotificationSpamRisk.HIGH

fun NotificationType.shouldBatchWhenPossible(): Boolean =
    deliveryProfile().batchWhenPossible
