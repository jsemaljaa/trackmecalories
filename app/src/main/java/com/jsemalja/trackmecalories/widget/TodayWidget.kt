package com.jsemalja.trackmecalories.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.widget.RemoteViews
import com.jsemalja.trackmecalories.QuickAction
import com.jsemalja.trackmecalories.R
import com.jsemalja.trackmecalories.data.Database
import com.jsemalja.trackmecalories.data.UserGoal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.abs
import com.jsemalja.trackmecalories.intent

/**
 * Home-screen widget: today's calories (eaten / left) with buttons to add food, quick-add
 * calories or scan a barcode. Refreshed by the app whenever today's log changes, and every
 * 30 minutes (so it rolls over at midnight).
 */
class TodayWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        scope.launch {
            try {
                render(context, manager, ids)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        /** Re-renders every placed widget; cheap to call often. */
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, TodayWidget::class.java))
            if (ids.isEmpty()) return
            scope.launch { render(context.applicationContext, manager, ids) }
        }

        private suspend fun render(context: Context, manager: AppWidgetManager, ids: IntArray) {
            val dao = Database.get(context).foodDao()
            val eaten = dao.getEntriesForDateOnce(LocalDate.now().toString()).sumOf { it.kcal }
            val goal = (dao.getUserGoalOnce() ?: UserGoal()).calorieGoal.coerceAtLeast(1)
            val left = goal - eaten

            val views = RemoteViews(context.packageName, R.layout.widget_today).apply {
                setTextViewText(R.id.widget_left_value, "%,d".format(abs(left)))
                setTextViewText(
                    R.id.widget_left_label,
                    context.getString(if (left < 0) R.string.kcal_over else R.string.kcal_left)
                )
                setTextViewText(R.id.widget_eaten, context.getString(R.string.widget_eaten_of, "%,d".format(eaten), "%,d".format(goal)))
                setProgressBar(R.id.widget_progress, 100, (eaten * 100 / goal).coerceIn(0, 100), false)
                setOnClickPendingIntent(R.id.widget_root, pendingIntent(context, null))
                setOnClickPendingIntent(R.id.widget_add, pendingIntent(context, QuickAction.ADD_FOOD))
                setOnClickPendingIntent(R.id.widget_quick, pendingIntent(context, QuickAction.QUICK_ADD))
                setOnClickPendingIntent(R.id.widget_scan, pendingIntent(context, QuickAction.SCAN))
            }
            manager.updateAppWidget(ids, views)
        }

        private fun pendingIntent(context: Context, action: QuickAction?): PendingIntent {
            val intent = action?.intent(context)
                ?: context.packageManager.getLaunchIntentForPackage(context.packageName)!!
            return PendingIntent.getActivity(
                context,
                action?.ordinal ?: 99,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
    }
}
