package com.jsemalja.trackmecalories.data

import com.jsemalja.trackmecalories.resources.Res
import com.jsemalja.trackmecalories.resources.fasting_notif_goal_text
import com.jsemalja.trackmecalories.resources.fasting_notif_goal_title
import com.jsemalja.trackmecalories.resources.fasting_notif_next_text
import com.jsemalja.trackmecalories.resources.fasting_notif_next_title
import com.jsemalja.trackmecalories.resources.unit_h
import com.jsemalja.trackmecalories.resources.unit_min
import org.jetbrains.compose.resources.getString
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusProvisional
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter

/**
 * The fasting timer on iOS: starting and ending fasts ([FastingTimerBase]), with local
 * notifications for "goal reached" and "time to fast". iOS has no ongoing notification
 * like Android's running timer (that would be a Live Activity), so that setting does nothing here.
 */
class IosFastingTimer(dao: FoodDao, prefs: UserPrefs) : FastingTimerBase(dao, prefs) {
    private val center = UNUserNotificationCenter.currentNotificationCenter()

    // The settings come back asynchronously, so access() reports the last answer (refreshed by sync()).
    private var notificationsAllowed = false

    override fun access() = FastingAccess(notifications = notificationsAllowed, exactAlarms = true)

    override suspend fun sync() {
        center.getNotificationSettingsWithCompletionHandler { settings ->
            val status = settings?.authorizationStatus
            notificationsAllowed = status == UNAuthorizationStatusAuthorized || status == UNAuthorizationStatusProvisional
        }
        val settings = prefs.fasting.value
        val active = dao.getActiveFastOnce()
        val now = currentTimeMillis()
        center.removePendingNotificationRequestsWithIdentifiers(listOf(ID_GOAL, ID_NEXT_FAST))
        // A "goal reached" alert is stale once that fast ends; "time to fast" once one starts.
        if (active == null || !settings.enabled) center.removeDeliveredNotificationsWithIdentifiers(listOf(ID_GOAL))
        if (active != null || !settings.enabled) center.removeDeliveredNotificationsWithIdentifiers(listOf(ID_NEXT_FAST))
        if (!settings.enabled) return

        val goalAt = active?.goalTime
        if (settings.notifyGoal && active != null && goalAt != null && goalAt > now) {
            val target = formatFastDuration(active.targetHours * HOUR_MS, getString(Res.string.unit_h), getString(Res.string.unit_min))
            schedule(ID_GOAL, goalAt - now, getString(Res.string.fasting_notif_goal_title), getString(Res.string.fasting_notif_goal_text, target))
        }
        val nextAt = if (active == null) dao.getLastFastOnce()?.endTime?.let { nextFastDue(it, settings.planHours) } else null
        if (settings.notifyNextFast && nextAt != null && nextAt > now) {
            schedule(ID_NEXT_FAST, nextAt - now, getString(Res.string.fasting_notif_next_title), getString(Res.string.fasting_notif_next_text))
        }
    }

    private fun schedule(id: String, inMs: Long, title: String, body: String) {
        val content = UNMutableNotificationContent().apply {
            setTitle(title)
            setBody(body)
            setSound(UNNotificationSound.defaultSound)
        }
        val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(inMs / 1000.0, repeats = false)
        center.addNotificationRequest(UNNotificationRequest.requestWithIdentifier(id, content, trigger), withCompletionHandler = null)
    }

    private companion object {
        const val ID_GOAL = "fasting_goal"
        const val ID_NEXT_FAST = "fasting_next"
    }
}
