package com.guyghost.wakeve.postevent

import com.guyghost.wakeve.models.Album
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.photoCount
import com.guyghost.wakeve.payment.SettlementRecord

enum class PostEventItemStatus {
    COMPLETE,
    NEEDS_ACTION,
    MISSING
}

enum class PostEventQuestion {
    WHO_OWES_WHOM,
    WHICH_PHOTOS_TO_SHARE,
    HOW_TO_REORGANIZE
}

enum class PostEventPrimaryAction {
    OPEN_EVENT,
    OPEN_SETTLEMENTS,
    OPEN_PHOTOS,
    RECREATE_EVENT
}

data class PostEventControlSummary(
    val eventId: String,
    val title: String,
    val isAfterEvent: Boolean,
    val settlementStatus: PostEventItemStatus,
    val photoStatus: PostEventItemStatus,
    val reorganizationStatus: PostEventItemStatus,
    val unresolvedSettlementCount: Int,
    val unresolvedSettlementTotal: Double,
    val sharedPhotoCount: Int,
    val headline: String,
    val body: String,
    val settlementLabel: String,
    val photoLabel: String,
    val reorganizationLabel: String,
    val primaryAction: PostEventPrimaryAction,
    val missingAnswers: List<PostEventQuestion>
)

fun Event.toPostEventControlSummary(
    settlements: List<SettlementRecord>,
    albums: List<Album>
): PostEventControlSummary {
    val eventSettlements = settlements.filter { it.eventId == id }
    val unresolvedSettlements = eventSettlements.filter { it.isUnresolvedSettlement() }
    val eventAlbums = albums.filter { it.eventId == id }
    val sharedPhotoCount = eventAlbums.sumOf { it.photoCount() }
    val isAfterEvent = status == EventStatus.FINALIZED

    val settlementStatus = when {
        !isAfterEvent -> PostEventItemStatus.MISSING
        unresolvedSettlements.isNotEmpty() -> PostEventItemStatus.NEEDS_ACTION
        eventSettlements.isNotEmpty() -> PostEventItemStatus.COMPLETE
        else -> PostEventItemStatus.MISSING
    }
    val photoStatus = when {
        !isAfterEvent -> PostEventItemStatus.MISSING
        sharedPhotoCount > 0 -> PostEventItemStatus.COMPLETE
        else -> PostEventItemStatus.MISSING
    }
    val reorganizationStatus = if (isAfterEvent) {
        PostEventItemStatus.COMPLETE
    } else {
        PostEventItemStatus.MISSING
    }

    val missingAnswers = buildList {
        if (settlementStatus == PostEventItemStatus.MISSING) add(PostEventQuestion.WHO_OWES_WHOM)
        if (photoStatus == PostEventItemStatus.MISSING) add(PostEventQuestion.WHICH_PHOTOS_TO_SHARE)
        if (reorganizationStatus == PostEventItemStatus.MISSING) add(PostEventQuestion.HOW_TO_REORGANIZE)
    }

    return PostEventControlSummary(
        eventId = id,
        title = "Apres ${title}",
        isAfterEvent = isAfterEvent,
        settlementStatus = settlementStatus,
        photoStatus = photoStatus,
        reorganizationStatus = reorganizationStatus,
        unresolvedSettlementCount = unresolvedSettlements.size,
        unresolvedSettlementTotal = unresolvedSettlements.sumOf { it.amount },
        sharedPhotoCount = sharedPhotoCount,
        headline = postEventHeadline(isAfterEvent, settlementStatus, photoStatus),
        body = postEventBody(
            isAfterEvent = isAfterEvent,
            unresolvedSettlementCount = unresolvedSettlements.size,
            unresolvedSettlementTotal = unresolvedSettlements.sumOf { it.amount },
            sharedPhotoCount = sharedPhotoCount
        ),
        settlementLabel = settlementStatus.toSettlementLabel(unresolvedSettlements.size),
        photoLabel = photoStatus.toPhotoLabel(sharedPhotoCount),
        reorganizationLabel = reorganizationStatus.toReorganizationLabel(title),
        primaryAction = postEventPrimaryAction(
            isAfterEvent = isAfterEvent,
            settlementStatus = settlementStatus,
            photoStatus = photoStatus
        ),
        missingAnswers = missingAnswers
    )
}

private fun SettlementRecord.isUnresolvedSettlement(): Boolean {
    if (amount <= 0.01) return false

    return status.uppercase() !in setOf(
        "PAID",
        "SETTLED",
        "COMPLETED",
        "CLOSED"
    )
}

private fun postEventHeadline(
    isAfterEvent: Boolean,
    settlementStatus: PostEventItemStatus,
    photoStatus: PostEventItemStatus
): String =
    when {
        !isAfterEvent -> "Recap disponible apres finalisation"
        settlementStatus == PostEventItemStatus.NEEDS_ACTION -> "Remboursements a traiter"
        settlementStatus == PostEventItemStatus.MISSING || photoStatus == PostEventItemStatus.MISSING ->
            "Recap incomplet"
        else -> "Evenement pret a reutiliser"
    }

private fun postEventBody(
    isAfterEvent: Boolean,
    unresolvedSettlementCount: Int,
    unresolvedSettlementTotal: Double,
    sharedPhotoCount: Int
): String =
    when {
        !isAfterEvent -> "Terminez l'evenement pour afficher remboursements, photos et relance rapide."
        unresolvedSettlementCount > 0 ->
            "$unresolvedSettlementCount remboursement${if (unresolvedSettlementCount > 1) "s" else ""} a suivre pour ${unresolvedSettlementTotal.toInt()}."
        sharedPhotoCount == 0 -> "Ajoutez les photos et verifiez les remboursements avant d'archiver le groupe."
        else -> "$sharedPhotoCount photo${if (sharedPhotoCount > 1) "s" else ""} partagee${if (sharedPhotoCount > 1) "s" else ""}; vous pouvez relancer une nouvelle edition."
    }

private fun PostEventItemStatus.toSettlementLabel(unresolvedCount: Int): String =
    when (this) {
        PostEventItemStatus.COMPLETE -> "Remboursements soldes"
        PostEventItemStatus.NEEDS_ACTION ->
            "$unresolvedCount remboursement${if (unresolvedCount > 1) "s" else ""} en attente"
        PostEventItemStatus.MISSING -> "Remboursements non renseignes"
    }

private fun PostEventItemStatus.toPhotoLabel(sharedPhotoCount: Int): String =
    when (this) {
        PostEventItemStatus.COMPLETE ->
            "$sharedPhotoCount photo${if (sharedPhotoCount > 1) "s" else ""} partagee${if (sharedPhotoCount > 1) "s" else ""}"
        PostEventItemStatus.NEEDS_ACTION -> "Photos a verifier"
        PostEventItemStatus.MISSING -> "Photos non partagees"
    }

private fun PostEventItemStatus.toReorganizationLabel(eventTitle: String): String =
    when (this) {
        PostEventItemStatus.COMPLETE -> "Relance rapide disponible pour $eventTitle"
        PostEventItemStatus.NEEDS_ACTION -> "Relance a verifier"
        PostEventItemStatus.MISSING -> "Relance disponible apres finalisation"
    }

private fun postEventPrimaryAction(
    isAfterEvent: Boolean,
    settlementStatus: PostEventItemStatus,
    photoStatus: PostEventItemStatus
): PostEventPrimaryAction =
    when {
        !isAfterEvent -> PostEventPrimaryAction.OPEN_EVENT
        settlementStatus == PostEventItemStatus.NEEDS_ACTION ||
            settlementStatus == PostEventItemStatus.MISSING -> PostEventPrimaryAction.OPEN_SETTLEMENTS
        photoStatus == PostEventItemStatus.MISSING -> PostEventPrimaryAction.OPEN_PHOTOS
        else -> PostEventPrimaryAction.RECREATE_EVENT
    }
