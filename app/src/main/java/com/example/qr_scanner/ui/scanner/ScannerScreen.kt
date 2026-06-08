package com.example.qr_scanner.ui.scanner

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.qr_scanner.data.BarcodeScannerAnalyzer
import com.example.qr_scanner.data.ScanResult
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import kotlinx.coroutines.launch

@Composable
fun ScannerScreen(
    onResult: (String) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: ScannerViewModel = viewModel(factory = ScannerViewModel.Factory),
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionRequested by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasPermission = granted
        permissionRequested = true
    }

    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (hasPermission) {
                ScannerContent(
                    viewModel = viewModel,
                    onResult = onResult,
                    onOpenHistory = onOpenHistory,
                    onOpenSettings = onOpenSettings,
                    snackbarHostState = snackbarHostState,
                )
            } else {
                PermissionGate(
                    permissionRequested = permissionRequested,
                    onRequest = { launcher.launch(Manifest.permission.CAMERA) },
                    onOpenSettings = { openAppSettings(context) },
                )
            }
        }
    }
}

@Composable
private fun PermissionGate(
    permissionRequested: Boolean,
    onRequest: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val shouldShowRationale = activity?.let {
        androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(
            it,
            Manifest.permission.CAMERA,
        )
    } ?: false
    val permanentlyDenied = permissionRequested && !shouldShowRationale

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Necesitamos acceso a la cámara",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Para escanear códigos QR necesitamos usar la cámara de tu dispositivo. " +
                "Las imágenes no salen de tu teléfono.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        if (permanentlyDenied) {
            Text(
                text = "Denegaste el permiso permanentemente. Ábrelo desde Ajustes para continuar.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            Button(onClick = onOpenSettings) {
                Text("Abrir Ajustes")
            }
        } else {
            Button(onClick = onRequest) {
                Text("Conceder permiso")
            }
        }
    }
}

@Composable
private fun ScannerContent(
    viewModel: ScannerViewModel,
    onResult: (String) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    val analyzer = viewModel.analyzer

    val cameraController = remember { CameraController() }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val result = scanImageForQr(context, uri)
            if (result != null) {
                viewModel.onDetected(result, com.example.qr_scanner.data.history.ScanSource.GALLERY)
            } else {
                snackbarHostState.showSnackbar("No se detectó ningún QR en la imagen")
            }
        }
    }

    // Evento de un solo disparo: cada detección navega exactamente una vez.
    LaunchedEffect(Unit) {
        viewModel.scanEvents.collect { result ->
            vibrateShort(context)
            onResult(ScanResult.encode(result))
        }
    }

    // Reanuda el escaneo cada vez que el scanner vuelve a primer plano (tras
    // volver desde la pantalla de resultado). El analyzer se pausó al detectar.
    LifecycleResumeEffect(Unit) {
        viewModel.resumeScanning()
        onPauseOrDispose { }
    }

    LaunchedEffect(uiState.torchEnabled) {
        cameraController.setTorch(uiState.torchEnabled)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        CameraPreview(
            modifier = Modifier.fillMaxSize(),
            analyzer = analyzer,
            cameraController = cameraController,
            lifecycleOwner = lifecycleOwner,
            onFlashAvailable = { viewModel.setHasFlashUnit(it) },
        )
        ViewfinderOverlay(modifier = Modifier.fillMaxSize())

        // Barra superior sobre el preview: ajustes (izq.) e historial (der.).
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .systemBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            OverlayIconButton(
                icon = Icons.Outlined.Settings,
                contentDescription = "Ajustes",
                onClick = onOpenSettings,
            )
            OverlayIconButton(
                icon = Icons.Outlined.History,
                contentDescription = "Ver historial",
                onClick = onOpenHistory,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom,
        ) {
            Text(
                text = "Centra el código QR dentro del recuadro",
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(24.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilledIconButton(
                    onClick = { viewModel.toggleTorch() },
                    enabled = uiState.hasFlashUnit,
                    modifier = Modifier.size(56.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Icon(
                        imageVector = if (uiState.torchEnabled) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                        contentDescription = if (uiState.torchEnabled) "Apagar linterna" else "Encender linterna",
                    )
                }
                FilledIconButton(
                    onClick = {
                        galleryLauncher.launch(
                            androidx.activity.result.PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly,
                            ),
                        )
                    },
                    modifier = Modifier.size(56.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Image,
                        contentDescription = "Cargar imagen de galería",
                    )
                }
            }
        }
    }
}

@Composable
private fun OverlayIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.4f)),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
        )
    }
}

private fun vibrateShort(context: Context) {
    // La vibración es opcional: nunca debe tumbar la app si el sistema la
    // restringe (permiso revocado, modo no-molestar, ROMs como EMUI, etc.).
    runCatching {
        val effect = VibrationEffect.createOneShot(40L, VibrationEffect.DEFAULT_AMPLITUDE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator?.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            vibrator?.vibrate(effect)
        }
    }
}

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}

private suspend fun scanImageForQr(context: Context, uri: Uri): ScanResult? {
    val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
    )
    return try {
        val input = InputImage.fromFilePath(context, uri)
        val barcodes = kotlinx.coroutines.suspendCancellableCoroutine { cont ->
            scanner.process(input)
                .addOnSuccessListener { result -> cont.resumeWith(Result.success(result)) }
                .addOnFailureListener { error -> cont.resumeWith(Result.failure(error)) }
        }
        val first = barcodes.firstOrNull { !it.rawValue.isNullOrEmpty() }
        first?.let { ScanResult.from(it) }
    } catch (_: Exception) {
        null
    } finally {
        scanner.close()
    }
}

@Composable
private fun CameraPreview(
    modifier: Modifier,
    analyzer: BarcodeScannerAnalyzer,
    cameraController: CameraController,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    onFlashAvailable: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    androidx.compose.ui.viewinterop.AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
            val providerFuture = ProcessCameraProvider.getInstance(ctx)
            providerFuture.addListener({
                val provider = providerFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { it.setAnalyzer(Executors.newSingleThreadExecutor(), analyzer) }

                val selector = CameraSelector.DEFAULT_BACK_CAMERA
                provider.unbindAll()
                val camera = provider.bindToLifecycle(
                    lifecycleOwner,
                    selector,
                    preview,
                    imageAnalysis,
                )
                cameraController.bind(camera)
                onFlashAvailable(camera.cameraInfo.hasFlashUnit())
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
    )

    DisposableEffect(Unit) {
        onDispose {
            val providerFuture = ProcessCameraProvider.getInstance(context)
            try {
                providerFuture.get().unbindAll()
            } catch (_: Exception) {
                // Provider may not be ready; safe to ignore on teardown.
            }
            cameraController.bind(null)
        }
    }
}

private class CameraController {
    private var camera: androidx.camera.core.Camera? = null

    fun bind(camera: androidx.camera.core.Camera?) {
        this.camera = camera
    }

    fun setTorch(enabled: Boolean) {
        val cam = camera ?: return
        if (cam.cameraInfo.hasFlashUnit()) {
            cam.cameraControl.enableTorch(enabled)
        }
    }
}
