package com.guyghost.wakeve.deeplink

internal sealed class PendingInviteProcessingInput {
    data object None : PendingInviteProcessingInput()
    data object RequireAuthentication : PendingInviteProcessingInput()
    data class Accept(val code: String) : PendingInviteProcessingInput()
}

internal data class PendingInviteProcessingResult(
    val clearPendingInviteCode: Boolean,
    val acceptedEventId: String?,
    val navigateToAuth: Boolean,
    val message: String
)

internal fun pendingInviteProcessingInput(
    pendingInviteCode: String?,
    isAuthenticated: Boolean,
    processingInviteCode: String?
): PendingInviteProcessingInput {
    val inviteCode = pendingInviteCode ?: return PendingInviteProcessingInput.None
    if (!isAuthenticated) return PendingInviteProcessingInput.RequireAuthentication
    if (processingInviteCode == inviteCode) return PendingInviteProcessingInput.None
    return PendingInviteProcessingInput.Accept(inviteCode)
}

internal fun pendingInviteProcessingResult(
    result: InvitationDeepLinkAcceptanceResult
): PendingInviteProcessingResult {
    return when (result) {
        is InvitationDeepLinkAcceptanceResult.Accepted -> PendingInviteProcessingResult(
            clearPendingInviteCode = true,
            acceptedEventId = result.eventId,
            navigateToAuth = false,
            message = result.message
        )
        is InvitationDeepLinkAcceptanceResult.AuthenticationRequired -> PendingInviteProcessingResult(
            clearPendingInviteCode = false,
            acceptedEventId = null,
            navigateToAuth = true,
            message = result.message
        )
        is InvitationDeepLinkAcceptanceResult.Rejected -> PendingInviteProcessingResult(
            clearPendingInviteCode = true,
            acceptedEventId = null,
            navigateToAuth = false,
            message = result.message
        )
        is InvitationDeepLinkAcceptanceResult.RetryableFailure -> PendingInviteProcessingResult(
            clearPendingInviteCode = false,
            acceptedEventId = null,
            navigateToAuth = false,
            message = result.message
        )
    }
}
