package com.example.myapplication4.ui.crowd_detection

import android.Manifest
import android.net.Uri
import android.util.Log
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
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
import androidx.compose.material.icons.filled.PhotoCamera
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myapplication4.ui.components.BottomNavBarMoreOption
import com.example.myapplication4.ui.components.CrowdDetectionOverlay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun CrowdDetectionCameraScreen(
    crowdDetectionViewModel: CrowdDetectionViewModel = hiltViewModel(),
    onNavigateToHistory: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToMore: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val isModelLoaded by crowdDetectionViewModel.isModelLoaded.collectAsState()
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasCameraPermission = isGranted
        }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            cameraLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val lensFacing by crowdDetectionViewModel.lensFacing.collectAsState()
    val detectionResult by crowdDetectionViewModel.detectionResult.collectAsState()
    val imageCapture = remember { ImageCapture.Builder().build() }
    val imageDimensions by crowdDetectionViewModel.imageDimensions.collectAsState()

    Scaffold(
        bottomBar = {
            BottomNavBarMoreOption(
                onHistoryClick = onNavigateToHistory,
                onProfileClick = onNavigateToProfile,
                onMoreClick = onNavigateToMore
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                if (hasCameraPermission) {
                    val previewView = remember { PreviewView(context) }
                    LaunchedEffect(lensFacing) {
                        val cameraProvider = withContext(Dispatchers.Main) {
                            ProcessCameraProvider.getInstance(context).get()
                        }

                        cameraProvider.unbindAll()

                        val preview = Preview.Builder().build().apply {
                            setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val selector = CameraSelector.Builder()
                            .requireLensFacing(lensFacing)
                            .build()

                        val imageAnalyzer = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also { analysis ->
                                analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                    crowdDetectionViewModel.processFrame(imageProxy)
                                }
                            }

                        try {
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                selector,
                                preview,
                                imageCapture,
                                imageAnalyzer
                            )
                        } catch (e: Exception) {
                            Log.e("CrowdDetectionScreen", "Camera binding failed", e)
                        }
                    }

                    AndroidView(
                        factory = { previewView },
                        modifier = Modifier.fillMaxSize()
                    )

                    CrowdDetectionOverlay(
                        modifier = Modifier.fillMaxSize(),
                        detectionResult = detectionResult,
                        imageWidth = imageDimensions.width,
                        imageHeight = imageDimensions.height,
                        isFrontCamera = lensFacing == CameraSelector.LENS_FACING_FRONT
                    )

                    if (isModelLoaded) {
                        val firstDetection = detectionResult?.boundingBoxes?.firstOrNull()?.let { box ->
                            detectionResult?.labels?.getOrNull(0)?.let { label ->
                                detectionResult?.scores?.getOrNull(0)?.let { score ->
                                    object {
                                        val label = label
                                        val score = score
                                    }
                                }
                            }
                        }

                        val displayText = if (firstDetection != null) {
                            "Kerumunan Yang Terdeteksi: ${firstDetection.label} (${"%.2f".format(firstDetection.score)})"
                        } else {
                            "Tidak ada kerumunan terdeteksi"
                        }
                        Text(
                            text = displayText,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                            color = if (firstDetection != null) Color.Red else Color.Green,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    IconButton(
                        onClick = { crowdDetectionViewModel.switchCamera() },
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                            .size(48.dp)
                            .background(
                                MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(12.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cameraswitch,
                            contentDescription = "Switch Camera"
                        )
                    }

                    IconButton(
                        onClick = {
                            val photoFile = File(
                                context.externalMediaDirs.firstOrNull(),
                                "${SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(System.currentTimeMillis())}.jpg"
                            )
                            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                            imageCapture.takePicture(
                                outputOptions,
                                ContextCompat.getMainExecutor(context),
                                object : ImageCapture.OnImageSavedCallback {
                                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                        val savedUri = outputFileResults.savedUri ?: Uri.fromFile(photoFile)
                                        Log.d("CrowdDetectionScreen", "Photo capture succeeded: $savedUri")
                                    }

                                    override fun onError(exception: ImageCaptureException) {
                                        Log.e("CrowdDetectionScreen", "Photo capture failed: ${exception.message}", exception)
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar(
                                                message = "Gagal mengambil gambar: ${exception.message}",
                                                duration = SnackbarDuration.Short
                                            )
                                        }
                                    }
                                }
                            )
                        },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                            .size(64.dp)
                            .background(
                                MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(32.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = "Take Picture",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}