package com.jsemalja.trackmecalories.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.text.PlatformTextStyle
import com.jsemalja.trackmecalories.ui.screens.settings.LanguageState

// The few things the shared screens need from the platform itself. Android: Platform.android.kt
// (the code the screens used before they moved to :shared). iOS: Platform.ios.kt.

/** A runtime permission: whether it's granted, and how to ask (the answer updates [granted]). */
class PermissionState(val granted: Boolean, val request: () -> Unit)

/** The camera permission, for the barcode scanner. */
@Composable
expect fun rememberCameraPermission(): PermissionState

/** A live camera preview that reports every barcode it reads; nothing is reported while [paused]. */
@Composable
expect fun BarcodeCamera(paused: Boolean, onBarcode: (String) -> Unit, modifier: Modifier = Modifier)

/** What the Health settings page can do in the platform's health store. */
class HealthActions(
    val requestAccess: () -> Unit,
    val openManage: () -> Unit,
    val openInstall: () -> Unit,
    /** Opens Samsung Health; null when it isn't installed. */
    val openSamsungHealth: (() -> Unit)?
)

/** [onAccessResult] gets whether anything was granted after [HealthActions.requestAccess]. */
@Composable
expect fun rememberHealthActions(onAccessResult: (anyGranted: Boolean) -> Unit): HealthActions

/** The permissions fasting alerts need. */
class FastingPermissions(
    /** Asks for notifications, or opens the app's notification settings when the system won't ask. */
    val askForNotifications: () -> Unit,
    /** Asks only where the system shows a prompt (Android 13+); null elsewhere. */
    val promptForNotifications: (() -> Unit)?,
    /** Opens the system screen that allows exact alarms; null where there's no such setting. */
    val openExactAlarmSettings: (() -> Unit)?
)

/** [onResult] runs after a permission prompt was answered. */
@Composable
expect fun rememberFastingPermissions(onResult: () -> Unit): FastingPermissions

/** The in-app language; null where the platform has no per-app language. */
@Composable
internal expect fun rememberAppLanguage(): LanguageState?

/** Writes [content] to a file named [fileName] and offers to share it; throws if that fails. */
@Composable
expect fun rememberShareFile(): (fileName: String, mimeType: String, content: String, chooserTitle: String) -> Unit

/** The app's version name, "2.2". */
@Composable
expect fun rememberAppVersion(): String

/** Shows a short message that goes away by itself (a Toast on Android). */
@Composable
expect fun rememberShowMessage(): (String) -> Unit

/** A full-screen dialog drawn edge to edge; the content pads itself for the system bars. */
@Composable
expect fun FullScreenDialog(onDismissRequest: () -> Unit, content: @Composable () -> Unit)

/** Whether the system offers colours taken from the wallpaper (Android 12+). */
expect val supportsWallpaperColors: Boolean

/** The wallpaper colour scheme, or null where there's none. */
@Composable
expect fun wallpaperColorScheme(dark: Boolean): ColorScheme?

/** Light or dark system bar icons to match the theme. */
@Composable
expect fun SystemBarsFollow(darkTheme: Boolean)

/** Blurs what this paint draws by [radiusPx] (glows and soft shadows). */
expect fun Paint.blur(radiusPx: Float)

/** Text without the platform's extra font padding (Android's includeFontPadding = false); null where there's none. */
expect val NoFontPadding: PlatformTextStyle?
