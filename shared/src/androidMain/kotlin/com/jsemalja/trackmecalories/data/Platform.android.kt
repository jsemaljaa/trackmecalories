package com.jsemalja.trackmecalories.data

import android.util.Log
import java.text.Normalizer
import java.util.Locale

internal actual fun String.decomposed(): String = Normalizer.normalize(this, Normalizer.Form.NFD)

internal actual fun deviceLanguage(): String = Locale.getDefault().language

internal actual fun logDebug(tag: String, message: String) {
    Log.d(tag, message)
}

internal actual fun logWarning(tag: String, message: String) {
    Log.w(tag, message)
}

internal actual fun logError(tag: String, message: String, error: Throwable?) {
    Log.e(tag, message, error)
}
