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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import androidx.lifecycle.compose.LocalLifecycleOwner

var isScanning by mutableStateOf(false)

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

    var foundFood by remember { mutableStateOf<FoodInfo?>(null) }
    var showCustomEntryDialog by remember { mutableStateOf(false) }
    var lastScannedBarcode by remember { mutableStateOf("") }

    if (hasCameraPermission) {
        Box(modifier = modifier.fillMaxSize()) {
            CameraPreviewScanner(
                onFoodFound = { food ->
                    foundFood = food
                    isScanning = true // Pause scanning
                },
                onNotFound = { barcode ->
                    lastScannedBarcode = barcode
                    scope.launch {
                        val local = viewModel.getCustomFood(barcode)
                        if (local != null) {
                            foundFood = FoodInfo(
                                name = local.name,
                                kcalPer100 = local.kcalPer100,
                                proteinPer100 = local.proteinPer100,
                                carbsPer100 = local.carbsPer100,
                                fatPer100 = local.fatPer100,
                                fiberPer100 = local.fiberPer100,
                                saltPer100 = local.saltPer100,
                                barcode = local.barcode
                            )
                        } else {
                            showCustomEntryDialog = true
                        }
                    }
                    isScanning = true // Pause scanning
                }
            )

            if (foundFood != null) {
                FoodResultCard(
                    food = foundFood!!,
                    onDismiss = {
                        foundFood = null
                        isScanning = false
                    },
                    onAdd = { amount, unit ->
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
                            unit = unit,
                            date = viewModel.selectedDate.value.toString()
                        ))
                        foundFood = null
                        isScanning = false
                    },
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
                )
            }
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
            onSave = { customFood, amount ->
                viewModel.saveCustomFood(customFood)
                val ratio = amount / 100.0
                viewModel.addEntry(FoodEntry(
                    name = customFood.name,
                    kcal = (customFood.kcalPer100 * ratio).toInt(),
                    protein = customFood.proteinPer100 * ratio,
                    carbs = customFood.carbsPer100 * ratio,
                    fat = customFood.fatPer100 * ratio,
                    fiber = customFood.fiberPer100 * ratio,
                    salt = customFood.saltPer100 * ratio,
                    amount = amount,
                    unit = customFood.unit,
                    date = viewModel.selectedDate.value.toString()
                ))
                showCustomEntryDialog = false
                isScanning = false
            }
        )
    }
}

@Composable
fun FoodResultCard(
    food: FoodInfo,
    onDismiss: () -> Unit,
    onAdd: (Double, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var amount by remember { mutableStateOf("100") }
    var unit by remember { mutableStateOf("g") }

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(food.name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }
            Text("${food.kcalPer100} kcal / 100g", style = MaterialTheme.typography.bodyMedium)
            
            Spacer(Modifier.height(16.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                
                var expanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(onClick = { expanded = true }) { Text(unit) }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        listOf("g", "ml", "pcs").forEach { u ->
                            DropdownMenuItem(text = { Text(u) }, onClick = { unit = u; expanded = false })
                        }
                    }
                }
            }
            
            Button(
                onClick = { onAdd(amount.toDoubleOrNull() ?: 0.0, unit) },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            ) {
                Text("Add to Log")
            }
        }
    }
}

@Composable
fun CustomFoodEntryDialog(
    barcode: String,
    onDismiss: () -> Unit,
    onSave: (CustomFood, Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var kcal by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    var fiber by remember { mutableStateOf("") }
    var salt by remember { mutableStateOf("") }
    var amountLogged by remember { mutableStateOf("100") }
    var unit by remember { mutableStateOf("g") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Food Not Found. Create Custom?") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { Text("Barcode: $barcode", style = MaterialTheme.typography.labelSmall) }
                item { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Food Name") }, modifier = Modifier.fillMaxWidth()) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = kcal, onValueChange = { kcal = it }, label = { Text("kcal/100") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        OutlinedTextField(value = protein, onValueChange = { protein = it }, label = { Text("P/100") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = carbs, onValueChange = { carbs = it }, label = { Text("C/100") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        OutlinedTextField(value = fat, onValueChange = { fat = it }, label = { Text("F/100") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = fiber, onValueChange = { fiber = it }, label = { Text("Fib/100") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        OutlinedTextField(value = salt, onValueChange = { salt = it }, label = { Text("Salt/100") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    }
                }
                item {
                   OutlinedTextField(value = amountLogged, onValueChange = { amountLogged = it }, label = { Text("Amount Eaten Now") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val food = CustomFood(
                    barcode = barcode,
                    name = name,
                    kcalPer100 = kcal.toDoubleOrNull() ?: 0.0,
                    proteinPer100 = protein.toDoubleOrNull() ?: 0.0,
                    carbsPer100 = carbs.toDoubleOrNull() ?: 0.0,
                    fatPer100 = fat.toDoubleOrNull() ?: 0.0,
                    fiberPer100 = fiber.toDoubleOrNull() ?: 0.0,
                    saltPer100 = salt.toDoubleOrNull() ?: 0.0,
                    unit = unit
                )
                onSave(food, amountLogged.toDoubleOrNull() ?: 0.0)
            }) { Text("Save & Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
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