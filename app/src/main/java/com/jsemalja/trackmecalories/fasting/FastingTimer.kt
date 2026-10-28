package com.jsemalja.trackmecalories.fasting

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.text.format.DateFormat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.jsemalja.trackmecalories.R
import com.jsemalja.trackmecalories.data.Fast
import com.jsemalja.trackmecalories.data.FastingAccess
import com.jsemalja.trackmecalories.data.FastingTimerBase
import com.jsemalja.trackmecalories.data.FastingSettings
import com.jsemalja.trackmecalories.data.FoodDao
import com.jsemalja.trackmecalories.data.HOUR_MS
import com.jsemalja.trackmecalories.data.UserPrefs
import com.jsemalja.trackmecalories.data.fastProgress
import com.jsemalja.trackmecalories.data.formatFastDuration
import com.jsemalja.trackmecalories.data.goalTime
import com.jsemalja.trackmecalories.data.nextFastDue
import java.util.Date

/**
 * The fasting timer on Android: starting and ending fasts ([FastingTimerBase]), and keeping its
 * notifications and alarms in step with the running fast and [FastingSettings]. Shared by the app and
 * [FastingReceiver] (notification buttons, alarms, reboot); every change ends with [sync].
 */
class FastingTimer(context: Context, dao: FoodDao, prefs: UserPrefs) : FastingTimerBase(dao, prefs) {
    private val context = context.applicationContext
    private val notifications = NotificationManagerCompat.from(this.context)
    private val alarms = this.context.getSystemService(AlarmManager::class.java)

    override fun access() = FastingAccess(
        notifications = notifications.areNotificationsEnabled(),
        exactAlarms = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()
    )

    override suspend fun sync() {
        val settings = prefs.fasting.value
        val active = dao.getActiveFastOnce()
        val now = System.currentTimeMillis()
        createChannels()

        if (settings.enabled && settings.notifyOngoing && active != null) post(ID_ONGOING, ongoing(active, now))
        else notifications.cancel(ID_ONGOING)
        // A "goal reached" alert is stale once that fast ends; "time to fast" once one starts.
        if (active == null || !settings.enabled) notifications.cancel(ID_GOAL)
        if (active != null || !settings.enabled) notifications.cancel(ID_NEXT_FAST)

        val goalAt = active?.goalTime
        if (settings.enabled && settings.notifyGoal && goalAt != null && goalAt > now) schedule(ACTION_GOAL, goalAt)
        else cancelAlarm(ACTION_GOAL)

        val nextAt = if (active == null) dao.getLastFastOnce()?.endTime?.let { nextFastDue(it, settings.planHours) } else null
        if (settings.enabled && settings.notifyNextFast && nextAt != null && nextAt > now) schedule(ACTION_NEXT_FAST, nextAt)
        else cancelAlarm(ACTION_NEXT_FAST)
    }

    /** The goal alarm went off. Checked again: the fast may have ended or moved since it was set. */
    suspend fun onGoalAlarm() {
        val settings = prefs.fasting.value
        val active = dao.getActiveFastOnce()
        if (settings.enabled && settings.notifyGoal && active != null && fastProgress(active, System.currentTimeMillis()).reached) {
            createChannels()
            val target = duration(active.targetHours * HOUR_MS)
            post(
                ID_GOAL,
                alert(context.getString(R.string.fasting_notif_goal_title), context.getString(R.string.fasting_notif_goal_text, target))
                    .addAction(0, context.getString(R.string.fasting_end), broadcast(ACTION_END))
                    .build()
            )
        }
        sync()
    }

    /** The eating window closed. Skipped if a fast already started. */
    suspend fun onNextFastAlarm() {
        val settings = prefs.fasting.value
        if (settings.enabled && settings.notifyNextFast && dao.getActiveFastOnce() == null) {
            createChannels()
            post(
                ID_NEXT_FAST,
                alert(context.getString(R.string.fasting_notif_next_title), context.getString(R.string.fasting_notif_next_text))
                    .addAction(0, context.getString(R.string.fasting_start), broadcast(ACTION_START))
                    .build()
            )
        }
        sync()
    }

    // The system's chronometer counts the elapsed time up from the start, so nothing has to
    // re-post this every second.
    private fun ongoing(fast: Fast, now: Long) = NotificationCompat.Builder(context, CHANNEL_TIMER)
        .setSmallIcon(R.drawable.ic_stat_fasting)
        .setContentTitle(context.getString(R.string.fasting_notif_title))
        .setContentText(
            if (fastProgress(fast, now).reached) {
                context.getString(R.string.fasting_goal_reached, duration(fast.targetHours * HOUR_MS))
            } else {
                context.getString(R.string.fasting_goal_at, duration(fast.targetHours * HOUR_MS), clock(fast.goalTime))
            }
        )
        .setWhen(fast.startTime)
        .setShowWhen(true)
        .setUsesChronometer(true)
        .setOngoing(true)
        .setSilent(true)
        .setCategory(NotificationCompat.CATEGORY_STATUS)
        .setContentIntent(openApp())
        .addAction(0, context.getString(R.string.fasting_end), broadcast(ACTION_END))
        .build()

    private fun alert(title: String, text: String) = NotificationCompat.Builder(context, CHANNEL_ALERTS)
        .setSmallIcon(R.drawable.ic_stat_fasting)
        .setContentTitle(title)
        .setContentText(text)
        .setAutoCancel(true)
        .setCategory(NotificationCompat.CATEGORY_REMINDER)
        .setContentIntent(openApp())

    private fun duration(ms: Long) =
        formatFastDuration(ms, context.getString(R.string.unit_h), context.getString(R.string.unit_min))

    // Follows the phone's 12/24-hour setting.
    private fun clock(ms: Long): String = DateFormat.getTimeFormat(context).format(Date(ms))

    // Without the permission (Android 13+) the system would drop it anyway.
    @SuppressLint("MissingPermission")
    private fun post(id: Int, notification: android.app.Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        notifications.notify(id, notification)
    }

    private fun createChannels() {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_TIMER, context.getString(R.string.fasting_channel_timer), NotificationManager.IMPORTANCE_LOW)
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ALERTS, context.getString(R.string.fasting_channel_alerts), NotificationManager.IMPORTANCE_DEFAULT)
        )
    }

    // Exact when the user allows it (Android 14+ asks); otherwise the system may deliver it
    // somewhat late, but never early.
    private fun schedule(action: String, at: Long) {
        val pending = alarmIntent(action, PendingIntent.FLAG_UPDATE_CURRENT) ?: return
        try {
            if (access().exactAlarms) alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
            else alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
        } catch (e: SecurityException) {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
        }
    }

    private fun cancelAlarm(action: String) {
        alarmIntent(action, PendingIntent.FLAG_NO_CREATE)?.let {
            alarms.cancel(it)
            it.cancel()
        }
    }

    private fun alarmIntent(action: String, flags: Int): PendingIntent? = PendingIntent.getBroadcast(
        context,
        requestCode(action),
        Intent(context, FastingReceiver::class.java).setAction(action),
        flags or PendingIntent.FLAG_IMMUTABLE
    )

    private fun broadcast(action: String): PendingIntent = alarmIntent(action, PendingIntent.FLAG_UPDATE_CURRENT)!!

    private fun openApp(): PendingIntent = PendingIntent.getActivity(
        context,
        REQUEST_OPEN,
        context.packageManager.getLaunchIntentForPackage(context.packageName)!!,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    private fun requestCode(action: String) = when (action) {
        ACTION_GOAL -> 1
        ACTION_NEXT_FAST -> 2
        ACTION_END -> 3
        else -> 4
    }

    companion object {
        const val ACTION_GOAL = "com.jsemalja.trackmecalories.fasting.GOAL"
        const val ACTION_NEXT_FAST = "com.jsemalja.trackmecalories.fasting.NEXT_FAST"
        const val ACTION_END = "com.jsemalja.trackmecalories.fasting.END"
        const val ACTION_START = "com.jsemalja.trackmecalories.fasting.START"

        private const val CHANNEL_TIMER = "fasting_timer"
        private const val CHANNEL_ALERTS = "fasting_alerts"
        private const val ID_ONGOING = 1001
        private const val ID_GOAL = 1002
        private const val ID_NEXT_FAST = 1003
        private const val REQUEST_OPEN = 5
    }
}
