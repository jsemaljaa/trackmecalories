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
import com.jsemalja.trackmecalories.data.FoodEntry
import com.jsemalja.trackmecalories.data.FoodInfo
import com.jsemalja.trackmecalories.data.FoodRepository
import com.jsemalja.trackmecalories.ui.viewmodel.MainViewModel

// Control variable for scanner state
var isScanning by mutableStateOf(false)

/**
 * Screen providing barcode scanning functionality.
 * Uses ML Kit for barcode recognition and Open Food Facts API for data retrieval.
 */
@Composable
fun ScannerScreen(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
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
                onFoodFound = { foodInfo ->
                    foundFood = CustomFood(
                        barcode = foodInfo.barcode,
                        name = foodInfo.name,
                        kcalPer100 = foodInfo.kcalPer100,
                        proteinPer100 = foodInfo.proteinPer100,
                        carbsPer100 = foodInfo.carbsPer100,
                        fatPer100 = foodInfo.fatPer100,
                        fiberPer100 = foodInfo.fiberPer100,
                        saltPer100 = foodInfo.saltPer100
                    )
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
                viewModel.saveCustomFood(customFood)
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
            onConfirm = { amount, mealType ->
                val ratio = amount / 100.0
                viewModel.addEntry(FoodEntry(
                    name = foundFood!!.name,
                    kcal = (foundFood!!.kcalPer100 * ratio).toInt(),
                    protein = foundFood!!.proteinPer100 * ratio,
                    carbs = foundFood!!.carbsPer100 * ratio,
                    fat = foundFood!!.fatPer100 * ratio,
                    fiber = foundFood!!.fiberPer100 * ratio,
                    salt = foundFood!!.saltPer100 * ratio,
                    amount = amount,
                    unit = foundFood!!.unit,
                    mealType = mealType,
                    date = viewModel.selectedDate.value.toString()
                ))
                showPortionDialog = false
                foundFood = null
                isScanning = false
            }
        )
    }
}

@Composable
fun CameraPreviewScanner(
    onFoodFound: (FoodInfo) -> Unit,
    onNotFound: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val repository = remember { FoodRepository() }
    val barcodeScanner = remember { BarcodeScanning.getClient() }

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
                        if (!isScanning) {
                            processBarcode(code, repository, scope, onFoodFound, onNotFound)
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
    repository: FoodRepository,
    scope: kotlinx.coroutines.CoroutineScope,
    onFoodFound: (FoodInfo) -> Unit,
    onNotFound: (String) -> Unit
) {
    isScanning = true
    scope.launch(Dispatchers.IO) {
        val result = repository.getProductInfo(code)
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
        Text(text = "Camera access is needed!", textAlign = TextAlign.Center)
        Button(onClick = onRequestPermission) { Text("Grant Permission") }
    }
}
