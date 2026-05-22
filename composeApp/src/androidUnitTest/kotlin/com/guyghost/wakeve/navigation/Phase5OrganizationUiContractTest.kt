package com.guyghost.wakeve.navigation

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class Phase5OrganizationUiContractTest {
    @Test
    fun screenDefinesEventScopedRoutesForMeetingsBudgetPaymentAndTricount() {
        val source = projectFile(
            "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/navigation/Screen.kt"
        ).readText()

        assertTrue(
            source.contains("MeetingList") &&
                source.contains("""Screen("event/{eventId}/meetings")""") &&
                source.contains("fun createRoute(eventId: String) = \"event/\$eventId/meetings\""),
            "Android navigation must expose an event-scoped meetings route."
        )
        assertTrue(
            source.contains("BudgetOverview") &&
                source.contains("""Screen("event/{eventId}/budget")""") &&
                source.contains("fun createRoute(eventId: String) = \"event/\$eventId/budget\""),
            "Android navigation must expose an event-scoped budget/expenses route."
        )
        assertTrue(
            source.contains("PaymentPot") &&
                source.contains("""Screen("event/{eventId}/payment")""") &&
                source.contains("fun createRoute(eventId: String) = \"event/\$eventId/payment\""),
            "Android navigation must expose an event-scoped payment pot route for ORGANIZING events."
        )
        assertTrue(
            source.contains("Tricount") &&
                source.contains("""Screen("event/{eventId}/tricount")""") &&
                source.contains("fun createRoute(eventId: String) = \"event/\$eventId/tricount\""),
            "Android navigation must expose an event-scoped Tricount handoff route for ORGANIZING events."
        )
    }

    @Test
    fun eventDetailOrOrganizationDashboardExposesAllPhase5EntriesOnlyInOrganizingFlow() {
        val organizationSources = listOf(
            "composeApp/src/commonMain/kotlin/com/guyghost/wakeve/EventDetailScreen.kt",
            "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/event/ModernEventDetailView.kt",
            "composeApp/src/commonMain/kotlin/com/guyghost/wakeve/ui/screens/OrganizerDashboardScreen.kt"
        ).joinToString("\n") { projectFile(it).readText() }

        assertTrue(
            organizationSources.contains("EventStatus.ORGANIZING"),
            "The Android organization dashboard/detail surface must explicitly key Phase 5 entry points off ORGANIZING workflow state."
        )
        assertTrue(
            containsAny(organizationSources, "Meeting", "Meetings", "Reunion", "Reunions") &&
                containsAny(organizationSources, "meetings", "MeetingList"),
            "ORGANIZING dashboard/detail must expose a meetings entry point."
        )
        assertTrue(
            containsAny(organizationSources, "Budget", "Expense", "Expenses", "Depense", "Depenses") &&
                containsAny(organizationSources, "budget", "BudgetOverview", "BudgetDetail"),
            "ORGANIZING dashboard/detail must expose budget and expenses entry points."
        )
        assertTrue(
            containsAny(organizationSources, "Payment", "PaymentPot", "Cagnotte", "Pot"),
            "ORGANIZING dashboard/detail must expose a payment pot entry point."
        )
        assertTrue(
            organizationSources.contains("Tricount"),
            "ORGANIZING dashboard/detail must expose a Tricount handoff entry point."
        )
        assertFalse(
            Regex("""EventStatus\.(DRAFT|POLLING)[\s\S]{0,240}(Meeting|Budget|Payment|Tricount)""")
                .containsMatchIn(organizationSources),
            "Phase 5 organization entries must not be exposed from DRAFT or POLLING states."
        )
    }

    @Test
    fun phase5DestinationsDeriveAccessFromConfirmedAttendeeStateBeforeRenderingDetails() {
        val source = projectFile(
            "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/navigation/WakeveNavHost.kt"
        ).readText()
        val meetingBlock = source.substringAfter("route = Screen.MeetingList.route")
            .substringBefore("// ========================================\n        // SETTINGS")
        val budgetBlock = source.substringAfter("route = Screen.BudgetOverview.route")
            .substringBefore("// ========================================\n        // ORGANIZER DASHBOARD")
        val phase5Block = meetingBlock + "\n" + budgetBlock

        assertTrue(
            containsAny(
                phase5Block,
                "DateValidationState.VALIDATED_RETAINED_DATE",
                "canAccessOrganizationDetails",
                "hasValidatedDate",
                "participantRecords"
            ),
            "Meetings, budget, payment and Tricount destinations must derive access from confirmed attendee state, not only organizer identity."
        )
        assertTrue(
            containsAny(phase5Block, "AccessDenied", "Acces refuse", "access denied", "Confirm your attendance") ||
                containsAny(phase5Block, "return@composable", "navigateUp()"),
            "Phase 5 destinations must render or route away from an access-denied state for declined, pending, or non-participant users."
        )
        assertFalse(
            Regex("""(MeetingListScreen|BudgetOverviewScreen)\([\s\S]{0,600}isOrganizer\s*=""")
                .containsMatchIn(phase5Block) &&
                !containsAny(phase5Block, "isParticipantConfirmed", "canAccessOrganizationDetails", "DateValidationState"),
            "Organizer checks alone are insufficient for Phase 5 detail access."
        )
    }

    @Test
    fun phase5SurfacesExposeOfflineAndPendingSyncState() {
        val phase5Sources = listOf(
            "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/navigation/WakeveNavHost.kt",
            "composeApp/src/commonMain/kotlin/com/guyghost/wakeve/ui/meeting/MeetingListScreen.kt",
            "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/budget/BudgetOverviewScreen.kt",
            "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/budget/BudgetDetailScreen.kt"
        ).joinToString("\n") { projectFile(it).readText() }

        assertTrue(
            containsAny(phase5Sources, "pendingSync", "selectPending()", "PendingSync", "queued"),
            "Meetings, budget, payment and Tricount UI must show when local writes are pending sync."
        )
        assertTrue(
            containsAny(phase5Sources, "offline", "Offline", "hors ligne", "isOnline", "connectivity"),
            "Meetings, budget, payment and Tricount UI must show offline state instead of hiding local-first behavior."
        )
    }

    @Test
    fun tricountExternalLinksAreValidatedBeforeAnyOpenIntent() {
        val phase5Sources = listOf(
            "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/navigation/Screen.kt",
            "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/navigation/WakeveNavHost.kt",
            "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/budget/BudgetOverviewScreen.kt",
            "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/budget/BudgetDetailScreen.kt"
        ).joinToString("\n") { projectFile(it).readText() }

        assertTrue(
            phase5Sources.contains("Tricount"),
            "Android Phase 5 UI must include a Tricount handoff surface before it can expose external settlement links."
        )
        assertTrue(
            containsAny(
                phase5Sources,
                "SafeExternalLink",
                "verificationStatus",
                "isVerified",
                "sanitize",
                "Uri.parse",
                "URLUtil.isValidUrl"
            ),
            "Tricount links must be represented as safe link metadata and validated or sanitized before opening."
        )
        assertFalse(
            Regex("""Intent\(\s*Intent\.ACTION_VIEW[\s\S]{0,160}tricount""", RegexOption.IGNORE_CASE)
                .containsMatchIn(phase5Sources),
            "Android must not open raw Tricount ACTION_VIEW intents without validation/sanitization."
        )
    }

    @Test
    fun paymentPotAndTricountDashboardsExposeLifecycleActions() {
        val phase5Sources = listOf(
            "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/navigation/WakeveNavHost.kt",
            "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/budget/BudgetOverviewScreen.kt",
            "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/budget/BudgetDetailScreen.kt"
        ).joinToString("\n") { projectFile(it).readText() }

        assertTrue(
            containsAny(
                phase5Sources,
                "onCreatePaymentPot",
                "onCreatePot",
                "createPot(",
                "CreatePaymentPot",
                "Creer une cagnotte",
                "Créer une cagnotte"
            ),
            "Android payment pot dashboard must expose a create lifecycle action for organizers."
        )
        assertTrue(
            containsAny(phase5Sources, "onOpenPaymentPot", "OpenPaymentPot", "Ouvrir la cagnotte", "PaymentPotScreen("),
            "Android payment pot dashboard must expose an open/view lifecycle action."
        )
        assertTrue(
            containsAny(phase5Sources, "onClosePaymentPot", "closePot(", "ClosePaymentPot", "Fermer la cagnotte"),
            "Android payment pot dashboard must expose a close lifecycle action for organizers."
        )
        assertTrue(
            containsAny(phase5Sources, "onLinkTricount", "linkHandoff(", "LinkTricount", "Associer Tricount"),
            "Android Tricount dashboard must expose a link lifecycle action."
        )
        assertTrue(
            containsAny(
                phase5Sources,
                "onMarkTricountNotNeeded",
                "markNotNeeded(",
                "MarkTricountNotNeeded",
                "Tricount not needed",
                "Tricount non requis"
            ),
            "Android Tricount dashboard must expose an explicit not-needed lifecycle action."
        )
    }

    @Test
    fun paymentPotDashboardExposesCreateActivateAndCloseLifecycleActions() {
        val source = projectFile(
            "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/budget/BudgetOverviewScreen.kt"
        ).readText()
        val paymentPotBlock = source.substringAfter("fun PaymentPotScreen(")
            .substringBefore("@Composable\n@OptIn(ExperimentalMaterial3Api::class)\nfun TricountHandoffScreen")

        assertTrue(
            containsAny(
                paymentPotBlock,
                "onCreatePaymentPot",
                "createPaymentPot",
                "CreatePaymentPot",
                "Create pot",
                "Créer une cagnotte"
            ),
            "Android PaymentPotScreen must expose a real create-pot lifecycle action, not only static dashboard copy."
        )
        assertTrue(
            containsAny(
                paymentPotBlock,
                "onActivatePaymentPot",
                "activatePaymentPot",
                "ActivatePaymentPot",
                "Activate pot",
                "Activer la cagnotte"
            ),
            "Android PaymentPotScreen must expose an activate-pot lifecycle action for organizer settlement readiness."
        )
        assertTrue(
            containsAny(
                paymentPotBlock,
                "onClosePaymentPot",
                "closePaymentPot",
                "ClosePaymentPot",
                "Close pot",
                "Clôturer la cagnotte"
            ),
            "Android PaymentPotScreen must expose a close-pot lifecycle action instead of ending at a placeholder state."
        )
    }

    @Test
    fun tricountDashboardExposesLinkUnlinkAndOpenSafeLifecycleActions() {
        val source = projectFile(
            "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/budget/BudgetOverviewScreen.kt"
        ).readText()
        val tricountBlock = source.substringAfter("fun TricountHandoffScreen(")
            .substringBefore("data class SafeExternalLink")

        assertTrue(
            containsAny(
                tricountBlock,
                "onLinkTricount",
                "linkTricount",
                "LinkTricount",
                "Associer Tricount",
                "Link Tricount"
            ),
            "Android Tricount dashboard must expose a link/create handoff action for organizers."
        )
        assertTrue(
            containsAny(
                tricountBlock,
                "onUnlinkTricount",
                "unlinkTricount",
                "UnlinkTricount",
                "Dissocier Tricount",
                "Unlink Tricount"
            ),
            "Android Tricount dashboard must expose an unlink lifecycle action so stale settlement links can be removed."
        )
        assertTrue(
            containsAny(
                tricountBlock,
                "onOpenSafeTricount",
                "openSafeUrl",
                "OpenSafeUrl",
                "SafeUrlOpener",
                "validatedURL"
            ) && !tricountBlock.contains("/* Platform opener handles validatedURL only. */"),
            "Android Tricount dashboard must open only through an explicit safe-url abstraction, not a placeholder button."
        )
    }

    @Test
    fun phase5ExternalLinksUseSafeOpenAbstractionInsteadOfInlineRawOpeners() {
        val phase5Sources = listOf(
            "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/navigation/WakeveNavHost.kt",
            "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/budget/BudgetOverviewScreen.kt",
            "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/budget/BudgetDetailScreen.kt",
            "composeApp/src/commonMain/kotlin/com/guyghost/wakeve/ui/meeting/MeetingListScreen.kt"
        ).joinToString("\n") { projectFile(it).readText() }

        assertTrue(
            containsAny(
                phase5Sources,
                "OpenSafeUrl",
                "SafeUrlOpener",
                "SafeExternalLinkOpener",
                "onOpenSafeUrl",
                "openSafeUrl"
            ),
            "Phase 5 UI must delegate external link opening to a safe-link/open-safe-url abstraction."
        )
        assertFalse(
            containsAny(
                phase5Sources,
                "Intent(Intent.ACTION_VIEW",
                "startActivity(Intent",
                "/* Platform opener handles validatedURL only. */"
            ),
            "Phase 5 UI must not rely on raw open intents or placeholder safe-link comments for external links."
        )
    }

    private fun projectFile(relativePath: String): File {
        val root = generateSequence(File(System.getProperty("user.dir")).absoluteFile) { it.parentFile }
            .first { File(it, relativePath).exists() }
        return File(root, relativePath)
    }

    private fun containsAny(source: String, vararg candidates: String): Boolean {
        return candidates.any { source.contains(it) }
    }
}
