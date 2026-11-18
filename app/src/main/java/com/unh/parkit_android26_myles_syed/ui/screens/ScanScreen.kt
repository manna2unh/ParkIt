package com.unh.parkit_android26_myles_syed.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

private val ScanNavy = Color(0xFF0B2A5B)
private val ScanGold = Color(0xFFF2B33D)

@Composable
fun ScanScreen(
    onClose: () -> Unit,
    onCodeScanned: (String) -> Boolean
) {

    val context = LocalContext.current
    var hasCamera by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
        )

    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { hasCamera = it }
    LaunchedEffect(Unit) { if (!hasCamera) launcher.launch(Manifest.permission.CAMERA) }

    var showManual by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val submit: (String) -> Boolean = { code ->
        val ok = onCodeScanned(code)
        error = if (ok) null else "Unknown lot code"
        ok
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (hasCamera) {
            CameraPreview(onCodeScanned = submit)
        } else {
            Text(
                "Camera permission is needed to scan.",
                color = Color.White,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Top bar
        Row(
            Modifier.fillMaxWidth().background(ScanNavy)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Close, contentDescription = "Close", tint = Color.White,
                modifier = Modifier.size(28.dp).clickable { onClose() }
            )
            Spacer(Modifier.weight(1f))
            Text("Scan lot barcode", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Medium)
        }

        // Hint pill
        Surface(
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 90.dp),
            shape = RoundedCornerShape(50),
            color = Color.Black.copy(alpha = 0.65f)
        ) {
            Text(
                "Align the barcode inside the frame",
                color = Color.White, fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
            )
        }
        error?.let {
            Surface(
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 140.dp),
                shape = RoundedCornerShape(50),
                color = Color(0xFFC0392B)
            ) {
                Text(it, color = Color.White, fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp))
            }
        }

        // Yellow frame
        Box(
            Modifier.align(Alignment.Center)
                .fillMaxWidth(0.75f).height(220.dp)
                .border(BorderStroke(3.dp, ScanGold), RoundedCornerShape(24.dp))
        )

        // Manual entry
        Row(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp)
                .clickable { showManual = true },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Keyboard, null, tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text("Enter code manually", color = Color.White, fontSize = 14.sp)
        }
    }

    if (showManual) {
        var code by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showManual = false },
            title = { Text("Enter lot code") },
            text = {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it; error = null },
                    singleLine = true,
                    isError = error != null,
                    supportingText = error?.let { { Text(it) } },
                    placeholder = { Text("e.g. UNH-01") }
                )
            },
            confirmButton = {
                TextButton(
                    enabled = code.isNotBlank(),
                    onClick = { if (submit(code.trim())) showManual = false }
                ) { Text("Confirm") }
            },
            dismissButton = { TextButton(onClick = { showManual = false }) { Text("Cancel") } }
        )
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
private fun CameraPreview(onCodeScanned: (String) -> Boolean) {
    val context = LocalContext.current
    val lifecycleOwner = context as LifecycleOwner
    val currentCallback by rememberUpdatedState(onCodeScanned)
    val executor = remember { Executors.newSingleThreadExecutor() }
    val scanner = remember { BarcodeScanning.getClient() }
    val handled = remember { booleanArrayOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            scanner.close()
            executor.shutdown()
            ProcessCameraProvider.getInstance(context).get().unbindAll()
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val providerFuture = ProcessCameraProvider.getInstance(ctx)
            providerFuture.addListener({
                val provider = providerFuture.get()
                val preview = Preview.Builder().build()
                    .also { it.setSurfaceProvider(previewView.surfaceProvider) }

                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(executor) { proxy ->
                    val media = proxy.image
                    if (media == null || handled[0]) { proxy.close(); return@setAnalyzer }
                    val image = InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees)
                    scanner.process(image)
                        .addOnSuccessListener { codes ->
                            val value = codes.firstOrNull()?.rawValue
                            if (value != null && !handled[0]) {
                                handled[0] = true
                                if (!currentCallback(value)) {
                                    // invalid code: allow scanning again after a short pause
                                    Handler(Looper.getMainLooper()).postDelayed({ handled[0] = false }, 1500)
                                }
                            }
                        }
                        .addOnCompleteListener { proxy.close() }
                }

                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis
                )
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        }
    )
}