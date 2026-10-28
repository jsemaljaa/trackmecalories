package com.jsemalja.trackmecalories.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.jsemalja.trackmecalories.ui.screens.settings.LanguageState
import org.jetbrains.skia.FilterBlurMode
import org.jetbrains.skia.MaskFilter
import platform.Foundation.NSBundle

// Placeholders until the iOS app exists (phase 3): camera scanning (AVFoundation), HealthKit,
// notification permissions and sharing still need real implementations.

@Composable
actual fun rememberCameraPermission(): PermissionState = PermissionState(granted = false, request = {})

@Composable
actual fun BarcodeCamera(paused: Boolean, onBarcode: (String) -> Unit, modifier: Modifier) {
    Box(modifier)
}

@Composable
actual fun rememberHealthActions(onAccessResult: (anyGranted: Boolean) -> Unit): HealthActions =
    HealthActions(requestAccess = { onAccessResult(false) }, openManage = {}, openInstall = {}, openSamsungHealth = null)

@Composable
actual fun rememberFastingPermissions(onResult: () -> Unit): FastingPermissions =
    FastingPermissions(askForNotifications = onResult, promptForNotifications = null, openExactAlarmSettings = null)

/** iOS has its own per-app language in the Settings app. */
@Composable
internal actual fun rememberAppLanguage(): LanguageState? = null

@Composable
actual fun rememberShareFile(): (fileName: String, mimeType: String, content: String, chooserTitle: String) -> Unit =
    { _, _, _, _ -> throw UnsupportedOperationException("Sharing isn't available on iOS yet") }

@Composable
actual fun rememberAppVersion(): String =
    NSBundle.mainBundle.infoDictionary?.get("CFBundleShortVersionString") as? String ?: ""

@Composable
actual fun rememberShowMessage(): (String) -> Unit = {}

@Composable
actual fun FullScreenDialog(onDismissRequest: () -> Unit, content: @Composable () -> Unit) {
    Dialog(onDismissRequest, DialogProperties(usePlatformDefaultWidth = false)) { content() }
}

actual val supportsWallpaperColors: Boolean = false

@Composable
actual fun wallpaperColorScheme(dark: Boolean): ColorScheme? = null

@Composable
actual fun SystemBarsFollow(darkTheme: Boolean) {}

actual val NoFontPadding: PlatformTextStyle? = null

/** Skia's sigma for an Android-style blur radius. */
actual fun Paint.blur(radiusPx: Float) {
    asFrameworkPaint().maskFilter = MaskFilter.makeBlur(FilterBlurMode.NORMAL, radiusPx * 0.57735f + 0.5f)
}
