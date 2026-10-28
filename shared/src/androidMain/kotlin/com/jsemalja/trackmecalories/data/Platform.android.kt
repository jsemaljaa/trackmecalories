package com.jsemalja.trackmecalories.data

import java.text.Normalizer

internal actual fun String.decomposed(): String = Normalizer.normalize(this, Normalizer.Form.NFD)
