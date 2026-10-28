package com.jsemalja.trackmecalories.data

import platform.Foundation.NSLocale
import platform.Foundation.NSString
import platform.Foundation.currentLocale
import platform.Foundation.decomposedStringWithCanonicalMapping
import platform.Foundation.languageCode

@Suppress("CAST_NEVER_SUCCEEDS")
internal actual fun String.decomposed(): String = (this as NSString).decomposedStringWithCanonicalMapping

internal actual fun deviceLanguage(): String = NSLocale.currentLocale.languageCode

internal actual fun logDebug(tag: String, message: String) {
    println("D/$tag: $message")
}

internal actual fun logWarning(tag: String, message: String) {
    println("W/$tag: $message")
}

internal actual fun logError(tag: String, message: String, error: Throwable?) {
    println("E/$tag: $message${error?.let { "\n" + it.stackTraceToString() } ?: ""}")
}
