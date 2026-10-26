package com.jsemalja.trackmecalories.ui.screens

import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageAnalysis
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.jsemalja.trackmecalories.data.CustomFood
import com.jsemalja.trackmecalories.data.FoodInfo
import com.jsemalja.trackmecalories.data.toCustomFood
import com.jsemalja.trackmecalories.ui.viewmodel.MainViewModel
import androidx.compose.ui.res.stringResource
import com.jsemalja.trackmecalories.R

/**
 * Screen providing barcode scanning functionality.
 * Uses ML Kit for barcode recognition and Open Food Facts API for data retrieval.
 */
@Composable
fun ScannerScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    onClose: () -> Unit,
    // Recipe ingredient mode: hand the scanned food and amount back instead of logging it.
    onPick: ((CustomFood, Double) -> Unit)? = null
) {
    // Full-screen overlay over the app: opaque background, close button over the camera.
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 1f)) {
        Box(Modifier.fillMaxSize()) {
            ScannerContent(viewModel, Modifier.fillMaxSize(), onLogged = onClose, onPick = onPick)
            FilledTonalIconButton(
                onClick = onClose,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(12.dp)
                    .align(Alignment.TopStart)
            ) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
            }
        }
    }
}

@Composable
private fun ScannerContent(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    onLogged: () -> Unit,
    onPick: ((CustomFood, Double) -> Unit)?
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Scoped to this screen's composition, so it can't leak state across recompositions/screens.
    var isScanning by remember { mutableStateOf(false) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
        }
    )

    var foundFood by remember { mutableStateOf<CustomFood?>(null) }
    var showCustomEntryDialog by remember { mutableStateOf(false) }
    var lastScannedBarcode by remember { mutableStateOf("") }

    var showPortionDialog by remember { mutableStateOf(false) }

    if (hasCameraPermission) {
        Box(modifier = modifier.fillMaxSize()) {
            CameraPreviewScanner(
                isScanning = isScanning,
                onScanningChanged = { isScanning = it },
                lookupBarcode = viewModel::lookupBarcode,
                onFoodFound = { foodInfo ->
                    foundFood = foodInfo.toCustomFood()
                    isScanning = true
                    showPortionDialog = true
                },
                onNotFound = { barcode ->
                    lastScannedBarcode = barcode
                    scope.launch {
                        val local = viewModel.getCustomFood(barcode)
                        if (local != null) {
                            foundFood = local
                            showPortionDialog = true
                        } else {
                            showCustomEntryDialog = true
                        }
                    }
                    isScanning = true
                }
            )
        }
    } else {
        PermissionRequestScreen(
            onRequestPermission = { launcher.launch(Manifest.permission.CAMERA) },
            modifier = modifier
        )
    }

    if (showCustomEntryDialog) {
        CustomFoodEntryDialog(
            barcode = lastScannedBarcode,
            onDismiss = {
                showCustomEntryDialog = false
                isScanning = false
            },
            onSave = { customFood ->
                foundFood = customFood
                showCustomEntryDialog = false
                showPortionDialog = true
            }
        )
    }

    if (showPortionDialog && foundFood != null) {
        PortionEntryDialog(
            food = foundFood!!,
            onDismiss = {
                showPortionDialog = false
                foundFood = null
                isScanning = false
            },
            showMeal = onPick == null,
            confirmLabel = if (onPick != null) stringResource(R.string.recipe_add) else null,
            onConfirm = { food, amount, mealType, confidence, portion ->
                if (onPick != null) {
                    onPick(food, amount)
                    return@PortionEntryDialog
                }
                viewModel.logCustomFood(food, amount, mealType, viewModel.selectedDate.value.toString(), confidence, portion)

                showPortionDialog = false
                foundFood = null
                isScanning = false
                onLogged()
            }
        )
    }
}

@Composable
fun CameraPreviewScanner(
    isScanning: Boolean,
    onScanningChanged: (Boolean) -> Unit,
    lookupBarcode: suspend (String) -> FoodInfo?,
    onFoodFound: (FoodInfo) -> Unit,
    onNotFound: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val barcodeScanner = remember { BarcodeScanning.getClient() }

    // The analyzer callback below is captured once by `remember`, so it must read
    // scanning state through a State that always holds the latest value.
    val isScanningState = rememberUpdatedState(isScanning)

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
                        if (!isScanningState.value) {
                            processBarcode(code, lookupBarcode, scope, onScanningChanged, onFoodFound, onNotFound)
                        }
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
        modifier = Modifier.fillMaxSize()
    )
}

private fun processBarcode(
    code: String,
    lookupBarcode: suspend (String) -> FoodInfo?,
    scope: kotlinx.coroutines.CoroutineScope,
    onScanningChanged: (Boolean) -> Unit,
    onFoodFound: (FoodInfo) -> Unit,
    onNotFound: (String) -> Unit
) {
    onScanningChanged(true)
    scope.launch(Dispatchers.IO) {
        val result = lookupBarcode(code)
        withContext(Dispatchers.Main) {
            if (result != null) {
                onFoodFound(result)
            } else {
                onNotFound(code)
            }
        }
    }
}

@Composable
fun PermissionRequestScreen(onRequestPermission: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(imageVector = Icons.Default.Camera, contentDescription = null, modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(16.dp))
        Text(text = stringResource(R.string.camera_needed), textAlign = TextAlign.Center)
        Button(onClick = onRequestPermission) { Text(stringResource(R.string.grant_permission)) }
    }
}
