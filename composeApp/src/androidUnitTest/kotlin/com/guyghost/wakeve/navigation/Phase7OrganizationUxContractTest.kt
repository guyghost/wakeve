package com.guyghost.wakeve.navigation

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class Phase7OrganizationUxContractTest {
    @Test
    fun organizationDashboardExposesEveryFinalizationReadinessSection() {
        val organizationSources = androidOrganizationSources()
        val expectedSections = mapOf(
            "participants" to listOf("Participants", "participants", "attendance", "confirmed attendee"),
            "scenario" to listOf("Scenario", "scenario"),
            "destination" to listOf("Destination", "destination"),
            "lodging" to listOf("Lodging", "Accommodation", "Hebergement", "lodging", "accommodation"),
            "transport" to listOf("Transport", "transport"),
            "meetings" to listOf("Meetings", "Meeting", "Reunions", "meeting"),
            "calendar" to listOf("Calendar", "Calendrier", "calendar"),
            "notifications" to listOf("Notifications", "Notification", "reminder", "Rappel"),
            "budget" to listOf("Budget", "Expense", "Depenses", "budget"),
            "payment" to listOf("Payment", "PaymentPot", "Cagnotte", "payment"),
            "tricount" to listOf("Tricount", "tricount"),
            "sync" to listOf("Sync", "pendingSync", "Queued", "offline", "sync"),
            "unsafe links" to listOf("Unsafe", "SafeExternalLink", "safe link", "external link"),
            "access control" to listOf("AccessDenied", "accessControl", "canAccessOrganizationDetails")
        )

        val missing = expectedSections.filterValues { candidates ->
            candidates.none { organizationSources.contains(it, ignoreCase = true) }
        }.keys

        assertTrue(
            missing.isEmpty(),
            "Phase 7 organization dashboard must expose every finalization readiness section. Missing: $missing"
        )
    }

    @Test
    fun organizationStatesAreVisiblyDistinctAcrossAccessSyncAndReadiness() {
        val organizationSources = androidOrganizationSources()
        val expectedStates = mapOf(
            "access denied" to listOf("AccessDenied", "access denied", "Confirm your attendance"),
            "optional not needed" to listOf("notNeeded", "NotNeeded", "not needed", "non requis"),
            "incomplete" to listOf("Incomplete", "incomplete", "missing", "required"),
            "complete" to listOf("Complete", "complete", "ready", "pret"),
            "pending sync" to listOf("pendingSync", "PendingSync", "queued", "Pending"),
            "failed sync" to listOf("failedSync", "FailedSync", "retry", "conflict", "ConflictDetected"),
            "read only finalized" to listOf("readOnly", "ReadOnly", "FINALIZED", "Finalized")
        )

        val missing = expectedStates.filterValues { candidates ->
            candidates.none { organizationSources.contains(it, ignoreCase = true) }
        }.keys

        assertTrue(
            missing.isEmpty(),
            "Phase 7 UI must distinguish access, readiness, sync, and read-only states. Missing: $missing"
        )
        assertFalse(
            Regex("""pendingSync\s*=\s*false|PendingSync\s*\(\s*false""").containsMatchIn(organizationSources),
            "Phase 7 UI must derive pending-sync state from repositories instead of hard-coded false values."
        )
    }

    @Test
    fun finalizedOrganizationUxKeepsDetailsButDisablesMutationActions() {
        val organizationSources = androidOrganizationSources()
        val finalizedBlocks = Regex(
            """EventStatus\.FINALIZED[\s\S]{0,1600}""",
            RegexOption.IGNORE_CASE
        ).findAll(organizationSources).joinToString("\n") { it.value }

        assertTrue(
            finalizedBlocks.isNotBlank(),
            "Android must have an explicit FINALIZED organization UI branch."
        )
        assertTrue(
            containsAny(
                finalizedBlocks,
                "readOnly",
                "ReadOnly",
                "enabled = false",
                ".disabled",
                "mutationsDisabled",
                "viewOnly"
            ),
            "FINALIZED organization sections must be rendered read-only, not as active planning controls."
        )
        assertFalse(
            Regex("""EventStatus\.FINALIZED[\s\S]{0,900}(onCreate|onAdd|onLink|onClose|onMark|Create|Ajouter|Associer)""")
                .containsMatchIn(organizationSources),
            "FINALIZED UI must not expose organization mutation actions."
        )
    }

    @Test
    fun preConfirmedAndUnauthorizedUsersDoNotSeeConfirmedOnlyOrganizationDetails() {
        val organizationSources = androidOrganizationSources()

        assertFalse(
            Regex(
                """EventStatus\.(DRAFT|POLLING)[\s\S]{0,900}(Phase5OrganizationEntryCard|MeetingList|BudgetOverview|PaymentPot|Tricount|TransportPlanningEntryCard|OrganizationReadiness)""",
                RegexOption.IGNORE_CASE
            ).containsMatchIn(organizationSources),
            "DRAFT and POLLING states must not expose confirmed-only organization dashboard details."
        )
        assertTrue(
            containsAny(
                organizationSources,
                "canAccessOrganizationDetails",
                "isParticipantConfirmed",
                "DateValidationState.VALIDATED_RETAINED_DATE"
            ),
            "Android organization surfaces must gate detail access through organizer-or-confirmed-attendee policy."
        )
        assertTrue(
            containsAny(
                organizationSources,
                "AccessDenied",
                "access denied",
                "Acces refuse",
                "Confirm your attendance",
                "confirmationRequired"
            ),
            "Android must render an access-denied or confirmation-required state for declined, pending, and non-participant users."
        )
    }

    @Test
    fun confirmedParticipantAndDeniedAccessStatesAreExplicitOnOrganizationRoutes() {
        val navHost = projectFile(
            "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/navigation/WakeveNavHost.kt"
        ).readText()
        val sensitiveRoutes = listOf("TransportPlanningScreen", "MeetingListScreen", "BudgetOverviewScreen", "PaymentPotScreen", "TricountHandoffScreen")
        val missingRouteContracts = sensitiveRoutes.filterNot { route ->
            navHost.contains(route) &&
                containsAny(
                    navHost,
                    "isParticipantConfirmed",
                    "canAccessOrganizationDetails",
                    "DateValidationState.VALIDATED_RETAINED_DATE",
                    "participantRecords"
                )
        }

        assertTrue(
            missingRouteContracts.isEmpty(),
            "Each Android organization route must admit confirmed participants and reject denied users. Missing route contracts: $missingRouteContracts"
        )
        assertTrue(
            containsAny(
                navHost,
                "DECLINED",
                "PENDING",
                "NOT_INVITED",
                "DateValidationState.NOT_VALIDATED",
                "DateValidationState.DECLINED_RETAINED_DATE",
                "accessDeniedReason"
            ),
            "Android route gating must distinguish declined, pending, and non-participant access states instead of silently hiding details."
        )
    }

    @Test
    fun offlinePendingAndFailedSyncStatesDoNotReadAsServerConfirmed() {
        val organizationSources = androidOrganizationSources()

        assertTrue(
            containsAny(organizationSources, "pendingSync", "PendingSync", "queued", "local write", "local-first"),
            "Android organization sections must expose pending local-first writes."
        )
        assertTrue(
            containsAny(
                organizationSources,
                "not yet synced",
                "not server confirmed",
                "server-confirmed",
                "queued for sync",
                "Synchronisation en attente",
                "pending server confirmation"
            ),
            "Pending-sync copy must make clear local writes are not server-confirmed yet."
        )
        assertTrue(
            containsAny(
                organizationSources,
                "failedSync",
                "FailedSync",
                "retry",
                "Retry",
                "conflict",
                "ConflictDetected",
                "resolveConflict"
            ),
            "Failed retryable sync or conflict states must be visible/actionable before finalization."
        )
    }

    @Test
    fun visiblePendingSyncBannersUseLocalizedUserCopy() {
        val pendingCopy = androidVisibleOrganizationCopyLiterals().filter { literal ->
            containsAny(
                literal.text,
                "pending sync",
                "queued",
                "server confirmed",
                "Synchronisation en attente",
                "Modifications locales en attente"
            )
        }
        val forbiddenPhrases = listOf("queued", "not server confirmed", "server-confirmed", "pendingSync")
        val exposedTechnicalPhrases = pendingCopy.filter { literal ->
            forbiddenPhrases.any { phrase -> literal.text.contains(phrase, ignoreCase = true) }
        }

        assertTrue(
            pendingCopy.any { literal ->
                containsAny(
                    literal.text,
                    "Synchronisation en attente",
                    "Modifications locales en attente d'envoi"
                )
            },
            "Pending-sync banners must use localized user copy such as Synchronisation en attente / Modifications locales en attente d'envoi. " +
                "Found: ${pendingCopy.joinToString { it.display() }}"
        )
        assertTrue(
            exposedTechnicalPhrases.isEmpty(),
            "Pending-sync banners must not expose implementation phrases like queued or not server confirmed. " +
                "Found: ${exposedTechnicalPhrases.joinToString { it.display() }}"
        )
    }

    @Test
    fun readinessSectionsAndStateBadgesUseStableLocalizationKeys() {
        val organizationSources = androidOrganizationSources()
        val expectedStableKeys = listOf(
            "organization.section.participants",
            "organization.section.scenario",
            "organization.section.destination",
            "organization.section.lodging",
            "organization.section.transport",
            "organization.section.meetings",
            "organization.section.calendar",
            "organization.section.notifications",
            "organization.section.budget",
            "organization.section.payment",
            "organization.section.tricount",
            "organization.section.sync",
            "organization.section.unsafe_links",
            "organization.section.access_control",
            "organization.state.empty",
            "organization.state.optional_not_needed",
            "organization.state.incomplete",
            "organization.state.complete",
            "organization.state.pending_sync",
            "organization.state.failed_sync",
            "organization.state.access_denied"
        )
        val missing = expectedStableKeys.filterNot { key ->
            organizationSources.contains(key) ||
                organizationSources.contains(key.replace(".", "_")) ||
                organizationSources.contains(key.replace(".", "_").uppercase())
        }

        assertTrue(
            missing.isEmpty(),
            "Android Phase 7 organization UX must use stable keys/labels matching iOS for sections and state badges. Missing: $missing"
        )
    }

    @Test
    fun visibleOrganizationCopyDoesNotExposeTechnicalContractMarkers() {
        val bannedVisibleMarkers = listOf(
            "AccessDenied",
            "readOnly",
            "ReadOnly",
            "viewOnly",
            "mutationsDisabled",
            "pendingSync",
            "PendingSync",
            "not server confirmed",
            "queued for sync",
            "pending server confirmation",
            "server-confirmed"
        )
        val bannedVisiblePatterns = listOf(
            Regex("""\bqueued\b""", RegexOption.IGNORE_CASE),
            Regex("""\bComing Soon\b""", RegexOption.IGNORE_CASE),
            Regex("""organization\.(section|state)\.""", RegexOption.IGNORE_CASE)
        )

        val violations = androidVisibleOrganizationCopyLiterals().filter { literal ->
            bannedVisibleMarkers.any { marker -> literal.text.contains(marker, ignoreCase = false) } ||
                bannedVisiblePatterns.any { pattern -> pattern.containsMatchIn(literal.text) }
        }

        assertTrue(
            violations.isEmpty(),
            "Visible Android organization copy must be product-ready and must not expose technical contract markers. " +
            "Violations: ${violations.joinToString { it.display() }}"
        )
    }

    @Test
    fun directAndroidPhase5OrganizationRoutesRejectPreOrganizingStatuses() {
        val navHost = projectFile(
            "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/navigation/WakeveNavHost.kt"
        ).readText()
        val phase5AccessPolicy = slice(
            navHost,
            "private data class Phase5AccessState",
            "private fun WakeveDb.hasPendingPhase5Sync"
        )
        val directRouteBlocks = mapOf(
            "meetings" to routeBlock(navHost, "Screen.MeetingList.route"),
            "budget" to routeBlock(navHost, "Screen.BudgetOverview.route"),
            "payment" to routeBlock(navHost, "Screen.PaymentPot.route"),
            "tricount" to routeBlock(navHost, "Screen.Tricount.route")
        )

        assertTrue(
            containsAny(phase5AccessPolicy, "EventStatus.ORGANIZING", "EventStatus.FINALIZED") &&
                containsAny(
                    phase5AccessPolicy,
                    "EventStatus.DRAFT",
                    "EventStatus.POLLING",
                    "EventStatus.CONFIRMED",
                    "EventStatus.COMPARING",
                    "preOrganizing",
                    "allowedOrganizationStatuses"
                ),
            "rememberPhase5Access must reject direct meetings/budget/payment/Tricount entry for pre-organizing statuses " +
                "and only allow ORGANIZING/FINALIZED logistics routes."
        )

        val missingStatusAwarePolicy = directRouteBlocks.filterValues { block ->
            !containsAny(
                block,
                "canEnterOrganizationRoutes",
                "canAccessOrganizationDetails",
                "isOrganizationStatusAllowed",
                "allowedOrganizationStatuses"
            )
        }.keys

        assertTrue(
            missingStatusAwarePolicy.isEmpty(),
            "Every direct Android Phase 5 route must apply the status-aware organization access policy. Missing: $missingStatusAwarePolicy"
        )
    }

    @Test
    fun finalizedAndroidOrganizationRoutesPassWorkflowReadOnlyStateToMutablePhase5Screens() {
        val navHost = projectFile(
            "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/navigation/WakeveNavHost.kt"
        ).readText()
        val mutableScreens = listOf(
            "MeetingListScreen",
            "PaymentPotScreen",
            "TricountHandoffScreen"
        )
        val missingReadOnlyRouteState = mutableScreens.filter { screenName ->
            val screenCall = Regex("""$screenName\s*\([\s\S]{0,900}\)""")
                .find(navHost)
                ?.value
                .orEmpty()

            !containsAny(
                screenCall,
                "isReadOnly",
                "readOnly",
                "canMutate",
                "canCreate",
                "canManage",
                "mutationsEnabled",
                "eventStatus",
                "EventStatus.FINALIZED",
                "isFinalized"
            )
        }

        assertTrue(
            missingReadOnlyRouteState.isEmpty(),
            "Android finalized organization routes must pass workflow/read-only state into mutable Phase 5 screens. " +
                "Routes still gated only by organizer/access state: $missingReadOnlyRouteState"
        )
        assertFalse(
            Regex("""isOrganizer\s*=\s*phase5Access\.isOrganizer""").containsMatchIn(navHost),
            "Android Phase 5 routes must not pass phase5Access.isOrganizer as the only mutation gate; FINALIZED must be read-only."
        )
    }

    @Test
    fun finalizedAndroidMeetingPaymentAndTricountSurfacesExposeReadOnlyMutationGuards() {
        val meetingScreen = projectFile(
            "composeApp/src/commonMain/kotlin/com/guyghost/wakeve/ui/meeting/MeetingListScreen.kt"
        ).readText()
        val budgetScreens = projectFile(
            "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/budget/BudgetOverviewScreen.kt"
        ).readText()

        val missingReadOnlyContracts = buildList {
            if (!containsAny(meetingScreen, "isReadOnly", "readOnly", "canCreateMeetings", "canMutate", "mutationsEnabled")) {
                add("MeetingListScreen")
            }
            if (!containsAny(budgetScreens, "isReadOnly", "readOnly", "canManagePayment", "canManageTricount", "canMutate", "mutationsEnabled")) {
                add("BudgetOverviewScreen payment/tricount")
            }
        }

        assertTrue(
            missingReadOnlyContracts.isEmpty(),
            "Android meeting/payment/Tricount screens must expose explicit read-only mutation guards for FINALIZED events. " +
                "Missing contracts: $missingReadOnlyContracts"
        )

        val organizerOnlyMutationControls = listOf(
            Regex("""if\s*\(\s*isOrganizer\s*\)[\s\S]{0,600}(showCreateDialog|onEditClick|onGenerateLinkClick)"""),
            Regex("""enabled\s*=\s*isOrganizer\s*(?:&&\s*(?:pot|handoff)[^\n,)]*)?""")
        ).flatMap { pattern ->
            pattern.findAll("$meetingScreen\n$budgetScreens").map { it.value.take(180) }.toList()
        }

        assertTrue(
            organizerOnlyMutationControls.isEmpty(),
            "FINALIZED organization mutations must not be controlled only by isOrganizer. " +
                "Organizer-only controls found: $organizerOnlyMutationControls"
        )
    }

    @Test
    fun androidMeetingVisibleCopyIsProductReadyAndLocalizedForFrenchUsers() {
        val forbiddenEnglishVisiblePhrases = listOf(
            "Link:",
            "Edit",
            "Share Link",
            "No meetings yet",
            "Create Meeting",
            "Edit Meeting",
            "Duration (hours)",
            "Generate Meeting Link",
            "Cancel"
        )
        val meetingCopy = androidVisibleOrganizationCopyLiterals().filter { literal ->
            literal.relativePath.endsWith("MeetingListScreen.kt")
        }
        val violations = meetingCopy.filter { literal ->
            forbiddenEnglishVisiblePhrases.any { phrase ->
                literal.text.contains(phrase, ignoreCase = false)
            }
        }

        assertTrue(
            violations.isEmpty(),
            "Android meeting visible copy must be product-ready and localized for the French organization flow. " +
                "Forbidden English copy found: ${violations.joinToString { it.display() }}"
        )
    }

    @Test
    fun androidMeetingCardsUseDisplayLabelsInsteadOfPlatformEnumNames() {
        val meetingScreen = projectFile(
            "composeApp/src/commonMain/kotlin/com/guyghost/wakeve/ui/meeting/MeetingListScreen.kt"
        ).readText()

        assertFalse(
            Regex("""Text\s*\(\s*[\s\S]{0,160}text\s*=\s*meeting\.platform\.name""")
                .containsMatchIn(meetingScreen),
            "Android meeting cards must not render meeting.platform.name because enum names like GOOGLE_MEET are visible to users."
        )
        assertTrue(
            containsAny(meetingScreen, "Zoom") &&
                containsAny(meetingScreen, "Google Meet") &&
                containsAny(meetingScreen, "FaceTime"),
            "Android meeting cards must use product-ready platform display labels such as Zoom, Google Meet, and FaceTime."
        )
    }

    @Test
    fun androidMeetingVisibleTextDoesNotRenderRawEnumNames() {
        val meetingListScreen = projectFile(
            "composeApp/src/commonMain/kotlin/com/guyghost/wakeve/ui/meeting/MeetingListScreen.kt"
        ).readText()
        val meetingDetailScreen = projectFile(
            "composeApp/src/commonMain/kotlin/com/guyghost/wakeve/ui/meeting/MeetingDetailScreen.kt"
        ).readText()
        val visibleTextSources = mapOf(
            "MeetingListScreen.kt" to meetingListScreen,
            "MeetingDetailScreen.kt" to meetingDetailScreen
        )

        val violations = visibleTextSources.flatMap { (fileName, source) ->
            listOf(
                Regex("""Text\s*\(\s*[\s\S]{0,180}(meeting\.platform\.name|platform\.name)"""),
                Regex("""MeetingDetailsRow\s*\([\s\S]{0,180}value\s*=\s*meeting\.platform\.name""")
            ).flatMap { pattern ->
                pattern.findAll(source).map { "$fileName: ${it.value.take(180)}" }.toList()
            }
        }

        assertTrue(
            violations.isEmpty(),
            "Android visible meeting text must not render raw enum names such as GOOGLE_MEET. " +
                "Use stable French product labels instead. Violations: $violations"
        )
    }

    @Test
    fun androidMeetingGenerateLinkDialogUsesDisplayLabelsInsteadOfPlatformEnumNames() {
        val meetingScreen = projectFile(
            "composeApp/src/commonMain/kotlin/com/guyghost/wakeve/ui/meeting/MeetingListScreen.kt"
        ).readText()
        val generateLinkDialog = slice(meetingScreen, "private fun GenerateLinkDialog", "\n}")

        assertFalse(
            Regex("""Text\s*\(\s*platform\.name\s*\)""").containsMatchIn(generateLinkDialog),
            "Android generate-link dialog must not render platform.name because enum names like GOOGLE_MEET are visible to users."
        )
        assertTrue(
            containsAny(generateLinkDialog, "Zoom") &&
                containsAny(generateLinkDialog, "Google Meet") &&
                containsAny(generateLinkDialog, "FaceTime"),
            "Android generate-link dialog must use product-ready platform display labels such as Zoom, Google Meet, and FaceTime."
        )
    }

    @Test
    fun androidMeetingDetailUsesDisplayLabelsInsteadOfPlatformEnumNames() {
        val detailScreen = projectFile(
            "composeApp/src/commonMain/kotlin/com/guyghost/wakeve/ui/meeting/MeetingDetailScreen.kt"
        ).readText()

        assertFalse(
            Regex("""MeetingDetailsRow\s*\([\s\S]{0,180}value\s*=\s*meeting\.platform\.name""")
                .containsMatchIn(detailScreen),
            "Android meeting detail must not render meeting.platform.name because enum names like GOOGLE_MEET are visible to users."
        )
        assertTrue(
            containsAny(detailScreen, "Zoom") &&
                containsAny(detailScreen, "Google Meet") &&
                containsAny(detailScreen, "FaceTime") &&
                containsAny(detailScreen, "réunion personnalisée", "Reunion personnalisee", "personnalisée"),
            "Android meeting detail must use product-ready platform display labels such as Zoom, Google Meet, FaceTime, and réunion personnalisée."
        )
    }

    @Test
    fun androidMeetingDateFormattingDoesNotExposeEnglishEnumMonthNames() {
        val meetingScreen = projectFile(
            "composeApp/src/commonMain/kotlin/com/guyghost/wakeve/ui/meeting/MeetingListScreen.kt"
        ).readText()
        val dateFormatter = slice(meetingScreen, "private fun formatDateTime", "\n}")

        assertFalse(
            containsAny(dateFormatter, "month.name", ".name.take(3)"),
            "Android meeting date formatting must not use month.name or name.take(3), which renders English enum month fragments."
        )
        assertTrue(
            containsAny(
                dateFormatter,
                "Locale.FRANCE",
                "Locale.FRENCH",
                "janv.",
                "févr.",
                "mars",
                "avr.",
                "mai",
                "juin",
                "juil.",
                "août",
                "sept.",
                "oct.",
                "nov.",
                "déc."
            ),
            "Android meeting date formatting must use French locale formatting or an explicit French month table."
        )
    }

    @Test
    fun androidMeetingDetailDateFormattingDoesNotExposeEnglishEnumMonthNames() {
        val detailScreen = projectFile(
            "composeApp/src/commonMain/kotlin/com/guyghost/wakeve/ui/meeting/MeetingDetailScreen.kt"
        ).readText()
        val dateFormatter = slice(detailScreen, "private fun formatDateTime", "\n}")

        assertFalse(
            containsAny(dateFormatter, "month.name", ".name.take(3)"),
            "Android meeting detail date formatting must not use month.name or name.take(3), which renders English enum month fragments."
        )
        assertTrue(
            containsAny(
                dateFormatter,
                "Locale.FRANCE",
                "Locale.FRENCH",
                "janv.",
                "févr.",
                "mars",
                "avr.",
                "mai",
                "juin",
                "juil.",
                "août",
                "sept.",
                "oct.",
                "nov.",
                "déc."
            ),
            "Android meeting detail date formatting must use French locale formatting or an explicit French month table."
        )
    }

    @Test
    fun androidBudgetDetailDirectRouteRequiresPhase5WorkflowAndReadOnlyState() {
        val navHost = projectFile(
            "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/navigation/WakeveNavHost.kt"
        ).readText()
        val budgetDetailRoute = routeBlock(navHost, "Screen.BudgetDetail.route")
        val budgetDetailCall = Regex("""BudgetDetailScreen\s*\([\s\S]{0,1200}\)""")
            .find(budgetDetailRoute)
            ?.value
            .orEmpty()

        assertTrue(
            containsAny(
                budgetDetailRoute,
                "canEnterOrganizationRoutes",
                "isOrganizationStatusAllowed",
                "allowedOrganizationStatuses",
                "EventStatus.ORGANIZING",
                "EventStatus.FINALIZED"
            ),
            "BudgetDetail direct route must apply the Phase 5 workflow policy and reject pre-ORGANIZING statuses."
        )
        assertFalse(
            Regex("""if\s*\(\s*!phase5Access\.canAccessOrganizationDetails\s*\)""")
                .containsMatchIn(budgetDetailRoute),
            "BudgetDetail direct route must not gate only on confirmed-participant detail access; it must require ORGANIZING/FINALIZED logistics access."
        )
        assertTrue(
            containsAny(
                budgetDetailCall,
                "isReadOnly",
                "readOnly",
                "canManageBudget",
                "canMutateBudget",
                "mutationsEnabled",
                "EventStatus.FINALIZED"
            ),
            "BudgetDetailScreen must receive workflow/read-only mutation state from the direct route."
        )
    }

    @Test
    fun finalizedAndroidBudgetSurfacesHideOrDisableBudgetMutations() {
        val budgetOverview = projectFile(
            "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/budget/BudgetOverviewScreen.kt"
        ).readText()
        val budgetDetail = projectFile(
            "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/budget/BudgetDetailScreen.kt"
        ).readText()
        val emptyBudgetState = slice(budgetOverview, "budgetState == null ->", "else ->")

        assertTrue(
            containsAny(
                budgetDetail,
                "isReadOnly",
                "readOnly",
                "canManageBudget",
                "canMutateBudget",
                "canAddBudgetItem",
                "mutationsEnabled"
            ),
            "BudgetDetailScreen must expose explicit read-only/mutation guards so FINALIZED events cannot add, delete, or mark expenses paid."
        )
        assertFalse(
            Regex("""floatingActionButton\s*=\s*\{[\s\S]{0,300}FloatingActionButton\(""")
                .containsMatchIn(budgetDetail) &&
                !containsAny(
                    slice(budgetDetail, "floatingActionButton =", ") { padding ->"),
                    "if (!isReadOnly",
                    "if (canManageBudget",
                    "if (canMutateBudget",
                    "if (canAddBudgetItem",
                    "mutationsEnabled"
                ),
            "BudgetDetail add-expense FAB must be hidden or disabled when the event is FINALIZED/read-only."
        )
        assertFalse(
            Regex("""BudgetItemCard\s*\([\s\S]{0,700}(onDelete|onMarkPaid)\s*=""")
                .containsMatchIn(budgetDetail) &&
                !containsAny(
                    slice(budgetDetail, "BudgetItemCard(", ")"),
                    "isReadOnly",
                    "canManageBudget",
                    "canMutateBudget",
                    "mutationsEnabled"
                ),
            "BudgetDetail delete/pay actions must be guarded by read-only mutation state."
        )
        assertTrue(
            containsAny(
                emptyBudgetState,
                "isReadOnly",
                "readOnly",
                "canManageBudget",
                "canCreateBudget",
                "mutationsEnabled",
                "enabled ="
            ),
            "Budget overview empty state must not always expose createBudget; it must hide or disable creation for FINALIZED/read-only and participant-only contexts."
        )
    }

    @Test
    fun androidPhase5AccessDeniedCopyIsFrenchAndProductReady() {
        val navHost = projectFile(
            "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/navigation/WakeveNavHost.kt"
        ).readText()
        val phase5RouteBlocks = listOf(
            routeBlock(navHost, "Screen.MeetingList.route"),
            routeBlock(navHost, "Screen.PaymentPot.route"),
            routeBlock(navHost, "Screen.Tricount.route"),
            routeBlock(navHost, "Screen.BudgetOverview.route"),
            routeBlock(navHost, "Screen.BudgetDetail.route")
        )
        val accessDeniedMessages = phase5RouteBlocks.flatMap { block ->
            Regex("""AccessDenied\s*\([\s\S]{0,220}message\s*=\s*"([^"]+)"""")
                .findAll(block)
                .map { it.groupValues[1] }
                .toList()
        }
        val englishMessages = accessDeniedMessages.filter { message ->
            containsAny(message, "Confirm your attendance", "before opening", "payment details", "budget details", "expense details")
        }

        assertTrue(
            accessDeniedMessages.isNotEmpty(),
            "Android Phase 5 routes must expose explicit access-denied copy for unauthorized users."
        )
        assertTrue(
            englishMessages.isEmpty(),
            "Android Phase 5 access-denied visible copy must be French/product-ready, not English placeholder text. Found: $englishMessages"
        )
        assertTrue(
            accessDeniedMessages.all { message ->
                containsAny(message, "Confirmez", "présence", "accéder", "ouvrir", "détails")
            },
            "Android Phase 5 access-denied messages should explain in French that attendance confirmation is required. Found: $accessDeniedMessages"
        )
    }

    private fun androidOrganizationSources(): String =
        listOf(
            "composeApp/src/commonMain/kotlin/com/guyghost/wakeve/EventDetailScreen.kt",
            "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/event/ModernEventDetailView.kt",
            "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/navigation/WakeveNavHost.kt",
            "composeApp/src/commonMain/kotlin/com/guyghost/wakeve/ui/scenario/ScenarioManagementScreen.kt",
            "composeApp/src/commonMain/kotlin/com/guyghost/wakeve/ui/scenario/ScenarioComparisonScreen.kt",
            "composeApp/src/commonMain/kotlin/com/guyghost/wakeve/ui/transport/TransportPlanningScreen.kt",
            "composeApp/src/commonMain/kotlin/com/guyghost/wakeve/ui/meeting/MeetingListScreen.kt",
            "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/budget/BudgetOverviewScreen.kt",
            "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/budget/BudgetDetailScreen.kt"
        ).joinToString("\n") { relativePath ->
            val file = projectFile(relativePath)
            if (file.exists()) file.readText() else ""
        }

    private fun projectFile(relativePath: String): File {
        val root = generateSequence(File(System.getProperty("user.dir")).absoluteFile) { it.parentFile }
            .first { File(it, relativePath).exists() }
        return File(root, relativePath)
    }

    private fun containsAny(source: String, vararg candidates: String): Boolean {
        return candidates.any { source.contains(it, ignoreCase = true) }
    }

    private fun routeBlock(source: String, routeMarker: String): String =
        slice(source, "route = $routeMarker", "\n        composable(")

    private fun slice(source: String, from: String, to: String): String {
        val start = source.indexOf(from)
        if (start == -1) return ""
        val tail = source.substring(start)
        val end = tail.indexOf(to, startIndex = from.length)
        return if (end == -1) tail else tail.substring(0, end)
    }

    private fun androidVisibleOrganizationCopyLiterals(): List<VisibleCopyLiteral> {
        val visibleApiPattern = Regex(
            """\b(Text|Button|OutlinedButton|TextButton|AlertDialog|TopAppBar)\s*\("""
        )
        val visibleArgumentPattern = Regex("""\b(title|label)\s*=\s*\{\s*Text\(""")
        val sourceFiles = listOf(
            "composeApp/src/commonMain/kotlin/com/guyghost/wakeve/EventDetailScreen.kt",
            "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/event/ModernEventDetailView.kt",
            "composeApp/src/commonMain/kotlin/com/guyghost/wakeve/ui/transport/TransportPlanningScreen.kt",
            "composeApp/src/commonMain/kotlin/com/guyghost/wakeve/ui/meeting/MeetingListScreen.kt",
            "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/budget/BudgetOverviewScreen.kt"
        )

        return sourceFiles.flatMap { relativePath ->
            val file = projectFile(relativePath)
            val lines = file.readLines()
            val visibleRanges = mutableListOf<IntRange>()

            lines.forEachIndexed { index, line ->
                if (
                    visibleApiPattern.containsMatchIn(line) ||
                    visibleArgumentPattern.containsMatchIn(line)
                ) {
                    visibleRanges += index..(index + 10).coerceAtMost(lines.lastIndex)
                }
            }

            lines.flatMapIndexed { index, line ->
                if (visibleRanges.none { index in it }) {
                    emptyList()
                } else {
                    extractStringLiterals(line)
                        .filter { it.isLikelyVisibleCopy() }
                        .map { VisibleCopyLiteral(relativePath, index + 1, it) }
                }
            }
        }
    }

    private fun extractStringLiterals(line: String): List<String> =
        Regex(""""([^"\\]*(?:\\.[^"\\]*)*)"""")
            .findAll(line)
            .map { it.groupValues[1] }
            .toList()

    private fun String.isLikelyVisibleCopy(): Boolean {
        if (isBlank()) return false
        if (startsWith("http://") || startsWith("https://")) return false
        if (startsWith("com.guyghost.")) return false
        return any { it.isLetter() }
    }

    private data class VisibleCopyLiteral(
        val relativePath: String,
        val line: Int,
        val text: String
    ) {
        fun display(): String = "$relativePath:$line \"$text\""
    }
}
