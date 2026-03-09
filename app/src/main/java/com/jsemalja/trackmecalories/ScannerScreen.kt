package com.jsemalja.trackmecalories

import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageAnalysis
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import androidx.lifecycle.compose.LocalLifecycleOwner

var isScanning by mutableStateOf(false)

@Composable
fun ScannerScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    // State to track if we have permission
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Launcher to trigger the system dialog
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
        }
    )

    if (hasCameraPermission) {
        // If granted, show the actual Camera Preview
        CameraPreviewScanner(modifier)
    } else {
        // If not, show a screen with a button to ask for it
        PermissionRequestScreen(
            onRequestPermission = { launcher.launch(Manifest.permission.CAMERA) },
            modifier = modifier
        )
    }
}

@Composable
fun PermissionRequestScreen(onRequestPermission: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Fixed: Using standard Icon with your drawable
        Icon(
            painter = painterResource(id = R.drawable.ic_camera),
            contentDescription = null,
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Camera access is needed to scan barcodes and find those calories!",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRequestPermission) {
            Text("Grant Permission")
        }
    }
}

@Composable
fun CameraPreviewScanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val repository = remember { FoodRepository() }

    // 1. Create the scanner instance ONCE and remember it
    val barcodeScanner = remember { BarcodeScanning.getClient() }

    val cameraController = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
            setImageAnalysisAnalyzer(
                ContextCompat.getMainExecutor(context),
                MlKitAnalyzer(
                    listOf(barcodeScanner), // Use the remembered scanner
                    ImageAnalysis.COORDINATE_SYSTEM_VIEW_REFERENCED,
                    ContextCompat.getMainExecutor(context)
                ) { result ->
                    // 2. Use the EXACT SAME scanner instance to get the value
                    val barcode = result.getValue(barcodeScanner)?.firstOrNull()
                    barcode?.rawValue?.let { code ->
                        onBarcodeDetected(code, context, repository, scope)
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

fun onBarcodeDetected(
    code: String,
    context: android.content.Context,
    repository: FoodRepository,
    scope: kotlinx.coroutines.CoroutineScope
) {
    if (isScanning) return // If we are already fetching, do nothing

    isScanning = true

    scope.launch(Dispatchers.IO) {
        try {
            val result = repository.getProductInfo(code)

            withContext(Dispatchers.Main) {
                // You can now see the raw JSON in your toast
                Toast.makeText(context, "Found: $result", Toast.LENGTH_LONG).show()
                isScanning = false // Allow scanning again
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                isScanning = false
            }
        }
    }
}