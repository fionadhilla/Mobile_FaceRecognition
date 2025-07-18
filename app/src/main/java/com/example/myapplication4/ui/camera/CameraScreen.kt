package com.example.myapplication4.ui.camera

import android.util.Log
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.myapplication4.domain.utils.MediaPipeUtils.toBitmap
import com.example.myapplication4.ui.components.BottomNavBar
import com.example.myapplication4.ui.components.FaceOverlay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import android.net.Uri
import com.example.myapplication4.face.FaceUtils

@Composable
fun CameraScreen(
    viewModel: CameraViewModel = hiltViewModel(),
    onNavigateToHistory: () -> Unit,
    onNavigateToAddFace: (Uri?) -> Unit,
    onNavigateToProfile: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    val lensFacing by viewModel.lensFacing.collectAsState()
    val detectionResult by viewModel.detectionResult.collectAsState()
    val isFaceDetected by viewModel.isFaceDetected.collectAsState()
    val verificationResult by viewModel.verificationResult.collectAsState()
    val previewView = remember {
        PreviewView(context).also {
            it.scaleType = PreviewView.ScaleType.FIT_CENTER
        }
    }
    var imageWidth by remember { mutableStateOf(1) }
    var imageHeight by remember { mutableStateOf(1) }
    val snackbarHostState = remember { SnackbarHostState() }
    var isMoreMenuExpanded by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val croppedFaceImageUri by viewModel.croppedFaceImageUri.collectAsState()
    var isCroppingInProgress by remember { mutableStateOf(false) }

    LaunchedEffect(croppedFaceImageUri) {
        if (isCroppingInProgress) {
            if (croppedFaceImageUri != null) {
                onNavigateToAddFace(croppedFaceImageUri)
            } else {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        message = "Gagal memotong wajah. Pastikan wajah terlihat jelas.",
                        duration = SnackbarDuration.Short
                    )
                }
                onNavigateToAddFace(null)
            }
            isCroppingInProgress = false
        }
    }

    LaunchedEffect(isFaceDetected) {
        if (isFaceDetected) {
            snackbarHostState.showSnackbar(
                message = "Wajah Terdeteksi",
                duration = SnackbarDuration.Short
            )
        }
    }

    LaunchedEffect(verificationResult) {
        verificationResult?.let { result ->
            val message = if (result.isMatch) {
                "Wajah dikenali: ${result.matchedUser?.name} (Jarak: %.2f)".format(result.distance)
            } else {
                "Wajah tidak dikenali. Jarak terdekat: %.2f".format(result.distance)
            }
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Long
            )
        }
    }

    // Camera binding
    LaunchedEffect(lensFacing) {
        val cameraProvider = withContext(Dispatchers.Main) {
            ProcessCameraProvider.getInstance(context).get()
        }

        val preview = Preview.Builder().build().apply {
            setSurfaceProvider(previewView.surfaceProvider)
        }

        val selector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        val imageAnalyzer = ImageAnalysis.Builder()
            .setTargetResolution(Size(480, 640))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { analysis ->
                analysis.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
                    try {
                        val bitmap = imageProxy.toBitmap()
                        imageWidth = imageProxy.width
                        imageHeight = imageProxy.height
                        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                        viewModel.processFrame(bitmap, rotationDegrees)
                    } catch (e: Exception) {
                        Log.e("Analyzer", "Error converting image", e)
                    } finally {
                        imageProxy.close()
                    }
                }
            }

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                selector,
                preview,
                imageAnalyzer
            )
        } catch (e: Exception) {
            Log.e("CameraScreen", "Camera binding failed", e)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            BottomNavBar(
                onHistoryClick = onNavigateToHistory,
                onAddClick = {
                    if (!isCroppingInProgress) {
                        if (isFaceDetected) {
                            isCroppingInProgress = true
                            viewModel.cropDetectedFace()
                        } else {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "Tidak ada wajah terdeteksi untuk ditambahkan.",
                                    duration = SnackbarDuration.Short
                                )
                            }
                            onNavigateToAddFace(null)
                        }
                    }
                },
                onProfileClick = onNavigateToProfile,
                isMoreMenuExpanded = isMoreMenuExpanded,
                onToggleMoreMenu = { isMoreMenuExpanded = !isMoreMenuExpanded },
                onMoreOptionSelected = { selected ->
                    isMoreMenuExpanded = false
                    when (selected) {
                        "face" -> {  }
                        "object" -> {  }
                        "more" -> {  }
                    }
                }
            )
        },
        modifier = Modifier.background(color = Color.LightGray)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier.fillMaxSize()
                )

                detectionResult?.let { result ->
                    FaceOverlay(
                        modifier = Modifier.fillMaxSize(),
                        detectionResult = result,
                        imageWidth = imageWidth,
                        imageHeight = imageHeight,
                        isFrontCamera = lensFacing == CameraSelector.LENS_FACING_FRONT
                    )
                }

                IconButton(
                    onClick = { viewModel.switchCamera() },
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Cameraswitch,
                        contentDescription = "Switch Camera"
                    )
                }
                verificationResult?.let { result ->
                    Text(
                        text = if (result.isMatch) "Wajah Dikenali: ${result.matchedUser?.name}" else "Wajah Tidak Dikenali",
                        color = if (result.isMatch) Color.Green else Color.Red,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 16.dp)
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}