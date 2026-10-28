package com.jsemalja.trackmecalories.ui.screens

import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jsemalja.trackmecalories.data.CustomFood
import com.jsemalja.trackmecalories.data.FoodInfo
import com.jsemalja.trackmecalories.data.toCustomFood
import com.jsemalja.trackmecalories.ui.viewmodel.MainViewModel
import org.jetbrains.compose.resources.stringResource
import com.jsemalja.trackmecalories.resources.*
import com.jsemalja.trackmecalories.ui.BarcodeCamera
import com.jsemalja.trackmecalories.ui.rememberCameraPermission

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
                    // Clear of the status bar and, in landscape, the camera cutout.
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                    .padding(12.dp)
                    .align(Alignment.TopStart)
            ) {
                Icon(Icons.Default.Close, contentDescription = stringResource(Res.string.close))
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
    val scope = rememberCoroutineScope()

    // Scoped to this screen's composition, so it can't leak state across recompositions/screens.
    var isScanning by remember { mutableStateOf(false) }

    val camera = rememberCameraPermission()

    var foundFood by remember { mutableStateOf<CustomFood?>(null) }
    var showCustomEntryDialog by remember { mutableStateOf(false) }
    var lastScannedBarcode by remember { mutableStateOf("") }

    var showPortionDialog by remember { mutableStateOf(false) }

    if (camera.granted) {
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
            onRequestPermission = camera.request,
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
        val requestKey = rememberRequestKey()
        PortionEntryDialog(
            food = foundFood!!,
            onDismiss = {
                showPortionDialog = false
                foundFood = null
                isScanning = false
            },
            showMeal = onPick == null,
            confirmLabel = if (onPick != null) stringResource(Res.string.recipe_add) else null,
            onConfirm = { food, amount, mealType, confidence, portion ->
                if (onPick != null) {
                    onPick(food, amount)
                    return@PortionEntryDialog
                }
                viewModel.logCustomFood(food, amount, mealType, viewModel.selectedDate.value.toString(), confidence, portion, requestKey)

                showPortionDialog = false
                foundFood = null
                isScanning = false
                onLogged()
            }
        )
    }
}

/** The camera, looking each barcode up; [isScanning] pauses it while one is being handled. */
@Composable
fun CameraPreviewScanner(
    isScanning: Boolean,
    onScanningChanged: (Boolean) -> Unit,
    lookupBarcode: suspend (String) -> FoodInfo?,
    onFoodFound: (FoodInfo) -> Unit,
    onNotFound: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    BarcodeCamera(
        paused = isScanning,
        onBarcode = { code -> processBarcode(code, lookupBarcode, scope, onScanningChanged, onFoodFound, onNotFound) }
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
    scope.launch(Dispatchers.Default) {
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
        Text(text = stringResource(Res.string.camera_needed), textAlign = TextAlign.Center)
        Button(onClick = onRequestPermission) { Text(stringResource(Res.string.grant_permission)) }
    }
}
