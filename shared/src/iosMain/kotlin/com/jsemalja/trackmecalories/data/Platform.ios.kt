package com.jsemalja.trackmecalories.data

import platform.Foundation.NSString
import platform.Foundation.decomposedStringWithCanonicalMapping

@Suppress("CAST_NEVER_SUCCEEDS")
internal actual fun String.decomposed(): String = (this as NSString).decomposedStringWithCanonicalMapping
