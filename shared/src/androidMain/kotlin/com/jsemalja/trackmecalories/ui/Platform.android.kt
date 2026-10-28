package com.jsemalja.trackmecalories.ui

import android.Manifest
import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BlurMaskFilter
import android.net.Uri
import android.os.Build
import android.os.LocaleList
import android.provider.Settings
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageAnalysis
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.jsemalja.trackmecalories.data.HealthData
import com.jsemalja.trackmecalories.ui.screens.settings.AppLanguage
import com.jsemalja.trackmecalories.ui.screens.settings.LanguageState
import java.io.File

@Composable
actual fun rememberCameraPermission(): PermissionState {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    return PermissionState(granted) { launcher.launch(Manifest.permission.CAMERA) }
}

/** CameraX preview with ML Kit's bundled (offline) barcode model. */
@Composable
actual fun BarcodeCamera(paused: Boolean, onBarcode: (String) -> Unit, modifier: Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val barcodeScanner = remember { BarcodeScanning.getClient() }

    // The analyzer callback below is captured once by `remember`, so it must read
    // these through States that always hold the latest values.
    val pausedState = rememberUpdatedState(paused)
    val onBarcodeState = rememberUpdatedState(onBarcode)

    val cameraController = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
            setImageAnalysisAnalyzer(
                ContextCompat.getMainExecutor(context),
                MlKitAnalyzer(
                    listOf(barcodeScanner),
                    ImageAnalysis.COORDINATE_SYSTEM_VIEW_REFERENCED,
                    ContextCompat.getMainExecutor(context)
                ) { result ->
                    val barcode = result.getValue(barcodeScanner)?.firstOrNull()
                    barcode?.rawValue?.let { code ->
                        if (!pausedState.value) onBarcodeState.value(code)
                    }
                }
            )
        }
    }

    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                controller = cameraController
                cameraController.bindToLifecycle(lifecycleOwner)
            }
        },
        modifier = modifier.fillMaxSize()
    )
}

@Composable
actual fun rememberHealthActions(onAccessResult: (anyGranted: Boolean) -> Unit): HealthActions {
    val context = LocalContext.current
    val samsungHealth = remember { context.packageManager.getLaunchIntentForPackage(HealthData.SAMSUNG_HEALTH) }
    val requestPermissions = rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) { granted ->
        onAccessResult(granted.isNotEmpty())
    }
    return HealthActions(
        requestAccess = { requestPermissions.launch(HealthData.permissions) },
        openManage = { runCatching { context.startActivity(HealthConnectClient.getHealthConnectManageDataIntent(context)) } },
        openInstall = {
            runCatching {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.apps.healthdata&url=healthconnect%3A%2F%2Fonboarding"))
                        .setPackage("com.android.vending")
                )
            }
        },
        openSamsungHealth = samsungHealth?.let { intent -> { runCatching { context.startActivity(intent) } } }
    )
}

@Composable
actual fun rememberFastingPermissions(onResult: () -> Unit): FastingPermissions {
    val context = LocalContext.current
    fun openNotificationSettings() {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            )
        }
    }
    val requestNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        // Denied for good earlier: the system won't ask again, so open the app's notification settings.
        if (!granted) openNotificationSettings()
        onResult()
    }
    val prompt: (() -> Unit)? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        { requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS) }
    } else null
    return FastingPermissions(
        askForNotifications = prompt ?: ::openNotificationSettings,
        promptForNotifications = prompt,
        // Coming back from the system screen re-checks it (MainActivity.onResume).
        openExactAlarmSettings = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            { runCatching { context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))) } }
        } else null
    )
}

/** Android 13+ per-app language (the same setting appears in the system's app info); null before. */
@Composable
internal actual fun rememberAppLanguage(): LanguageState? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
    val context = LocalContext.current
    return remember {
        val localeManager = context.getSystemService(LocaleManager::class.java)
        // Set from the system's app info it can carry a region ("ru-RU"), so only the language is compared.
        val tag = localeManager.applicationLocales.get(0)?.language
        LanguageState(AppLanguage.entries.firstOrNull { it.tag.isNotEmpty() && it.tag == tag } ?: AppLanguage.SYSTEM) { language ->
            // The activity restarts in the new language; the open tab is kept.
            localeManager.applicationLocales =
                if (language == AppLanguage.SYSTEM) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(language.tag)
        }
    }
}

/** Written to the cache dir and shared through the app's FileProvider. */
@Composable
actual fun rememberShareFile(): (fileName: String, mimeType: String, content: String, chooserTitle: String) -> Unit {
    val context = LocalContext.current
    return remember(context) {
        val share: (String, String, String, String) -> Unit = { fileName, mimeType, content, chooserTitle ->
            val file = File(context.cacheDir, fileName)
            file.writeText(content)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, chooserTitle))
        }
        share
    }
}

@Composable
actual fun rememberAppVersion(): String {
    val context = LocalContext.current
    return remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty()
    }
}

@Composable
actual fun rememberShowMessage(): (String) -> Unit {
    val context = LocalContext.current
    return remember(context) {
        val show: (String) -> Unit = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
        show
    }
}

/**
 * With the default decor fitting, a full-height dialog window is pushed below the status bar
 * and its bottom ends up off screen; here the window is laid out edge to edge instead.
 */
@Composable
actual fun FullScreenDialog(onDismissRequest: () -> Unit, content: @Composable () -> Unit) {
    Dialog(onDismissRequest, DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        val view = LocalView.current
        val window = (view.parent as? DialogWindowProvider)?.window
        val lightBars = MaterialTheme.colorScheme.surfaceContainerLowest.luminance() > 0.5f
        SideEffect {
            window?.let {
                // decorFitsSystemWindows alone leaves the window fitting the system bars.
                it.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                WindowCompat.setDecorFitsSystemWindows(it, false)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    it.attributes = it.attributes.apply {
                        fitInsetsTypes = 0
                        layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                    }
                }
                WindowCompat.getInsetsController(it, view).apply {
                    isAppearanceLightStatusBars = lightBars
                    isAppearanceLightNavigationBars = lightBars
                }
            }
        }
        content()
    }
}

actual val supportsWallpaperColors: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

@Composable
actual fun wallpaperColorScheme(dark: Boolean): ColorScheme? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
    val context = LocalContext.current
    return if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
}

/**
 * Status and navigation bar icons follow the app's light/dark, not the system's: with the
 * mode pinned to dark on a light phone, the default would draw dark icons on a dark page.
 */
@Composable
actual fun SystemBarsFollow(darkTheme: Boolean) {
    val activity = LocalContext.current.findActivity() as? ComponentActivity ?: return
    DisposableEffect(activity, darkTheme) {
        activity.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.Transparent.toArgb(), Color.Transparent.toArgb()) { darkTheme },
            // Same scrims as the androidx defaults (only shown with 3-button navigation).
            navigationBarStyle = SystemBarStyle.auto(LightScrim, DarkScrim) { darkTheme }
        )
        onDispose { }
    }
}

private val LightScrim = android.graphics.Color.argb(0xE6, 0xFF, 0xFF, 0xFF)
private val DarkScrim = android.graphics.Color.argb(0x80, 0x1B, 0x1B, 0x1B)

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

actual val NoFontPadding: PlatformTextStyle? = PlatformTextStyle(includeFontPadding = false)

/** Blur masks need hardware support (Android 9+); older phones draw no blur. */
actual fun Paint.blur(radiusPx: Float) {
    asFrameworkPaint().maskFilter = BlurMaskFilter(radiusPx, BlurMaskFilter.Blur.NORMAL)
}
