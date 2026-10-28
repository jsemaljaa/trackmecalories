@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package com.jsemalja.trackmecalories.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.viewinterop.UIKitView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.jsemalja.trackmecalories.data.healthKit
import com.jsemalja.trackmecalories.ui.screens.settings.LanguageState
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.launch
import org.jetbrains.skia.FilterBlurMode
import org.jetbrains.skia.MaskFilter
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusNotDetermined
import platform.AVFoundation.AVCaptureConnection
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDeviceInput
import platform.AVFoundation.AVCaptureMetadataOutput
import platform.AVFoundation.AVCaptureMetadataOutputObjectsDelegateProtocol
import platform.AVFoundation.AVCaptureOutput
import platform.AVFoundation.AVCaptureSession
import platform.AVFoundation.AVCaptureVideoPreviewLayer
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.AVMetadataMachineReadableCodeObject
import platform.AVFoundation.AVMetadataObjectTypeCode128Code
import platform.AVFoundation.AVMetadataObjectTypeEAN13Code
import platform.AVFoundation.AVMetadataObjectTypeEAN8Code
import platform.AVFoundation.AVMetadataObjectTypeUPCECode
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.requestAccessForMediaType
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSBundle
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.writeToFile
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIAlertController
import platform.UIKit.UIAlertControllerStyleAlert
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.UIKit.UIView
import platform.UIKit.UIViewController
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatusDenied
import platform.UserNotifications.UNUserNotificationCenter
import platform.darwin.DISPATCH_QUEUE_PRIORITY_DEFAULT
import platform.darwin.DISPATCH_TIME_NOW
import platform.darwin.NSObject
import platform.darwin.dispatch_after
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_global_queue
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_time

private fun onMain(block: () -> Unit) = dispatch_async(dispatch_get_main_queue(), block)

private fun openUrl(url: String) {
    NSURL.URLWithString(url)?.let { UIApplication.sharedApplication.openURL(it, options = emptyMap<Any?, Any>(), completionHandler = null) }
}

private fun openAppSettings() = openUrl(UIApplicationOpenSettingsURLString)

/** The view controller on top, to present system sheets from. */
private fun topViewController(): UIViewController? {
    @Suppress("DEPRECATION")
    var top = UIApplication.sharedApplication.keyWindow?.rootViewController
    while (top?.presentedViewController != null) top = top.presentedViewController
    return top
}

@Composable
actual fun rememberCameraPermission(): PermissionState {
    var granted by remember { mutableStateOf(AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo) == AVAuthorizationStatusAuthorized) }
    return PermissionState(granted) {
        // iOS asks only once; after a "no" the switch is in the Settings app.
        if (AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo) == AVAuthorizationStatusNotDetermined) {
            AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { ok -> onMain { granted = ok } }
        } else {
            openAppSettings()
        }
    }
}

/** Hands each barcode AVFoundation reads to [onCode] (on the main queue). */
private class BarcodeDelegate(private val onCode: (String) -> Unit) : NSObject(), AVCaptureMetadataOutputObjectsDelegateProtocol {
    override fun captureOutput(output: AVCaptureOutput, didOutputMetadataObjects: List<*>, fromConnection: AVCaptureConnection) {
        (didOutputMetadataObjects.firstOrNull() as? AVMetadataMachineReadableCodeObject)?.stringValue?.let(onCode)
    }
}

/** A view whose preview layer follows its size. */
private class CameraPreviewView(session: AVCaptureSession) : UIView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0)) {
    private val preview = AVCaptureVideoPreviewLayer(session = session).apply { videoGravity = AVLayerVideoGravityResizeAspectFill }

    init {
        layer.addSublayer(preview)
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        preview.setFrame(bounds)
    }
}

/** AVFoundation's built-in barcode reader (works offline, like ML Kit's bundled model on Android). */
@Composable
actual fun BarcodeCamera(paused: Boolean, onBarcode: (String) -> Unit, modifier: Modifier) {
    val pausedState = rememberUpdatedState(paused)
    val onBarcodeState = rememberUpdatedState(onBarcode)
    val delegate = remember { BarcodeDelegate { code -> if (!pausedState.value) onBarcodeState.value(code) } }
    val session = remember { AVCaptureSession() }
    val preview = remember { CameraPreviewView(session) }

    DisposableEffect(session) {
        val device = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo)
        val input = device?.let { AVCaptureDeviceInput.deviceInputWithDevice(it, null) }
        if (input != null && session.canAddInput(input)) {
            session.addInput(input)
            val output = AVCaptureMetadataOutput()
            if (session.canAddOutput(output)) {
                session.addOutput(output)
                output.setMetadataObjectsDelegate(delegate, dispatch_get_main_queue())
                // Only types the output supports may be set, and only once it's on the session.
                val wanted = listOf(AVMetadataObjectTypeEAN13Code, AVMetadataObjectTypeEAN8Code, AVMetadataObjectTypeUPCECode, AVMetadataObjectTypeCode128Code)
                output.metadataObjectTypes = wanted.filter { it in output.availableMetadataObjectTypes }
            }
            // startRunning blocks, so it goes off the main thread.
            dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT.toLong(), 0u)) { session.startRunning() }
        }
        onDispose { session.stopRunning() }
    }

    UIKitView(factory = { preview }, modifier = modifier.fillMaxSize())
}

@Composable
actual fun rememberHealthActions(onAccessResult: (anyGranted: Boolean) -> Unit): HealthActions {
    val scope = rememberCoroutineScope()
    val onResult by rememberUpdatedState(onAccessResult)
    return remember {
        HealthActions(
            requestAccess = { scope.launch { onResult(healthKit.requestAccess()) } },
            // Access is changed in the Health app (Sharing → Apps), not here.
            openManage = { openUrl("x-apple-health://") },
            openInstall = {},
            openSamsungHealth = null
        )
    }
}

@Composable
actual fun rememberFastingPermissions(onResult: () -> Unit): FastingPermissions {
    val result by rememberUpdatedState(onResult)
    return remember {
        val center = UNUserNotificationCenter.currentNotificationCenter()
        val prompt: () -> Unit = {
            center.requestAuthorizationWithOptions(UNAuthorizationOptionAlert or UNAuthorizationOptionSound) { _, _ -> onMain { result() } }
        }
        FastingPermissions(
            askForNotifications = {
                // After a "no" iOS won't ask again: the switch is in the Settings app.
                center.getNotificationSettingsWithCompletionHandler { settings ->
                    onMain { if (settings?.authorizationStatus == UNAuthorizationStatusDenied) openAppSettings() else prompt() }
                }
            },
            promptForNotifications = prompt,
            openExactAlarmSettings = null
        )
    }
}

/** iOS has its own per-app language in the Settings app. */
@Composable
internal actual fun rememberAppLanguage(): LanguageState? = null

/** Written to the temporary directory and offered through the share sheet. */
@Composable
actual fun rememberShareFile(): (fileName: String, mimeType: String, content: String, chooserTitle: String) -> Unit = remember {
    val share: (String, String, String, String) -> Unit = { fileName, _, content, _ ->
        val path = NSTemporaryDirectory() + fileName
        @Suppress("CAST_NEVER_SUCCEEDS")
        val written = (content as NSString).writeToFile(path, atomically = true, encoding = NSUTF8StringEncoding, error = null)
        check(written) { "Couldn't write $fileName" }
        val sheet = UIActivityViewController(activityItems = listOf(NSURL.fileURLWithPath(path)), applicationActivities = null)
        topViewController()?.presentViewController(sheet, animated = true, completion = null)
    }
    share
}

@Composable
actual fun rememberAppVersion(): String =
    NSBundle.mainBundle.infoDictionary?.get("CFBundleShortVersionString") as? String ?: ""

/** iOS has no toast: a small alert that dismisses itself after a moment. */
@Composable
actual fun rememberShowMessage(): (String) -> Unit = remember {
    val show: (String) -> Unit = { message ->
        val alert = UIAlertController.alertControllerWithTitle(null, message, UIAlertControllerStyleAlert)
        topViewController()?.presentViewController(alert, animated = true) {
            dispatch_after(dispatch_time(DISPATCH_TIME_NOW, 1_500_000_000L), dispatch_get_main_queue()) {
                alert.dismissViewControllerAnimated(true, completion = null)
            }
        }
    }
    show
}

@Composable
actual fun FullScreenDialog(onDismissRequest: () -> Unit, content: @Composable () -> Unit) {
    Dialog(onDismissRequest, DialogProperties(usePlatformDefaultWidth = false)) { content() }
}

actual val supportsWallpaperColors: Boolean = false

@Composable
actual fun wallpaperColorScheme(dark: Boolean): ColorScheme? = null

/** The status bar follows the system light/dark here. */
@Composable
actual fun SystemBarsFollow(darkTheme: Boolean) {}

actual val NoFontPadding: PlatformTextStyle? = null

/** Skia's sigma for an Android-style blur radius. */
actual fun Paint.blur(radiusPx: Float) {
    asFrameworkPaint().maskFilter = MaskFilter.makeBlur(FilterBlurMode.NORMAL, radiusPx * 0.57735f + 0.5f)
}
