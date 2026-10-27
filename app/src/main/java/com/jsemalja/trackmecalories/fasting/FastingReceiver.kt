package com.jsemalja.trackmecalories.fasting

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.jsemalja.trackmecalories.data.Database
import com.jsemalja.trackmecalories.data.UserPrefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Fasting alarms and notification buttons, plus re-scheduling after a reboot, an app update or
 * the exact-alarm permission changing (the system drops alarms on reboot).
 */
class FastingReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        scope.launch {
            try {
                val timer = FastingTimer(context, Database.get(context).foodDao(), UserPrefs(context))
                when (intent.action) {
                    FastingTimer.ACTION_GOAL -> timer.onGoalAlarm()
                    FastingTimer.ACTION_NEXT_FAST -> timer.onNextFastAlarm()
                    FastingTimer.ACTION_END -> timer.end()
                    FastingTimer.ACTION_START -> timer.start()
                    else -> timer.sync()
                }
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
