package com.jsemalja.trackmecalories

import android.content.Context
import android.content.Intent

// QuickAction (in :shared) as intents on [MainActivity].

fun QuickAction.intent(context: Context): Intent = Intent(context, MainActivity::class.java)
    .setAction(action)
    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)

fun QuickAction.Companion.from(intent: Intent?): QuickAction? = QuickAction.entries.firstOrNull { it.action == intent?.action }
