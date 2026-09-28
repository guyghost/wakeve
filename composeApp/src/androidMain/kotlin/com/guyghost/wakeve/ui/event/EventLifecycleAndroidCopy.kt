package com.guyghost.wakeve.ui.event

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.core.os.ConfigurationCompat
import com.guyghost.wakeve.R
import java.util.Locale

/** Android string resource for each shared finalization blocker code. */
internal val eventLifecycleBlockerLabelResources: Map<String, Int> = mapOf(
    "ORGANIZER_REQUIRED" to R.string.event_lifecycle_blocker_organizer_required,
    "CONFIRMED_PARTICIPANTS_REQUIRED" to R.string.event_lifecycle_blocker_confirmed_participants_required,
    "FINAL_SCENARIO_REQUIRED" to R.string.event_lifecycle_blocker_final_scenario_required,
    "DESTINATION_REQUIRED" to R.string.event_lifecycle_blocker_destination_required,
    "LODGING_REQUIRED" to R.string.event_lifecycle_blocker_lodging_required,
    "TRANSPORT_REQUIRED" to R.string.event_lifecycle_blocker_transport_required,
    "MEETING_REQUIRED" to R.string.event_lifecycle_blocker_meeting_required,
    "CALENDAR_CONFIRMED_DATE_REQUIRED" to R.string.event_lifecycle_blocker_calendar_confirmed_date_required,
    "NOTIFICATION_REMINDERS_REQUIRED" to R.string.event_lifecycle_blocker_notification_reminders_required,
    "BUDGET_REQUIRED" to R.string.event_lifecycle_blocker_budget_required,
    "PAYMENT_POT_REQUIRED" to R.string.event_lifecycle_blocker_payment_pot_required,
    "TRICOUNT_HANDOFF_REQUIRED" to R.string.event_lifecycle_blocker_tricount_handoff_required,
    "CRITICAL_SYNC_PENDING" to R.string.event_lifecycle_blocker_critical_sync_pending,
    "CRITICAL_SYNC_FAILED" to R.string.event_lifecycle_blocker_critical_sync_failed,
    "CRITICAL_CONFLICT_PENDING" to R.string.event_lifecycle_blocker_critical_conflict_pending,
    "UNSAFE_EXTERNAL_LINKS" to R.string.event_lifecycle_blocker_unsafe_external_links,
    "ACCESS_ORGANIZER_REQUIRED" to R.string.event_lifecycle_blocker_access_organizer_required,
    "ACCESS_CONFIRMED_PARTICIPANTS_REQUIRED" to R.string.event_lifecycle_blocker_access_confirmed_participants_required,
    "EVENT_NOT_ORGANIZING" to R.string.event_lifecycle_blocker_event_not_organizing,
)

/** Builds the localized organizer lifecycle copy from Android resources for the current locale. */
@Composable
fun rememberEventLifecycleCopy(): EventLifecycleCopy {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    return remember(context, configuration) {
        val resources = context.resources
        val locale = ConfigurationCompat.getLocales(configuration)[0] ?: Locale.getDefault()
        EventLifecycleCopy(
            organizingTitle = resources.getString(R.string.event_lifecycle_organizing_title),
            organizingBody = resources.getString(R.string.event_lifecycle_organizing_body),
            organizingAction = resources.getString(R.string.event_lifecycle_organizing_action),
            organizingConfirmMessage = resources.getString(R.string.event_lifecycle_organizing_confirm_message),
            comparingBody = resources.getString(R.string.event_lifecycle_comparing_body),
            finalizeTitle = resources.getString(R.string.event_lifecycle_finalize_title),
            finalizeBody = resources.getString(R.string.event_lifecycle_finalize_body),
            finalizeAction = resources.getString(R.string.event_lifecycle_finalize_action),
            finalizeConfirmMessage = resources.getString(R.string.event_lifecycle_finalize_confirm_message),
            cancel = resources.getString(R.string.cancel),
            blockedIntro = resources.getString(R.string.event_lifecycle_blocked_intro),
            genericError = resources.getString(R.string.event_lifecycle_error_generic),
            blockerLabels = eventLifecycleBlockerLabelResources.mapValues { (_, id) -> resources.getString(id) },
            joinLabels = { labels -> joinLocalized(labels, locale) },
        )
    }
}

private fun joinLocalized(labels: List<String>, locale: Locale): String =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        android.icu.text.ListFormatter.getInstance(locale).format(labels)
    } else {
        labels.joinToString(", ")
    }
