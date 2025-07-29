// In app/src/main/java/com/example/myapplication4/ui/camera/CameraScreen.kt
package com.example.myapplication4.ui.camera

import android.net.Uri
import android.util.Log
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
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
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myapplication4.domain.utils.MediaPipeUtils.toBitmap
import com.example.myapplication4.ui.components.BottomNavBar
import com.example.myapplication4.ui.components.FaceOverlay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun CameraScreen(
    viewModel: CameraViewModel = hiltViewModel(),
    onNavigateToHistory: () -> Unit,
    onNavigateToAddFace: (Uri?) -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToMore: () -> Unit, // Ini adalah callback untuk navigasi ke CameraOptionScreen
    // Hapus parameter onNavigateToObjectDetection, onNavigateToFaceDetection, onNavigateToAnomalyDetection dari sini
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    val lensFacing by viewModel.lensFacing.collectAsState()
    val detectionResult by viewModel.detectionResult.collectAsState()
    val isFaceDetected by viewModel.isFaceDetected.collectAsState()
    val previewView = remember {
        PreviewView(context).also {
            it.scaleType = PreviewView.ScaleType.FIT_CENTER
        }
    }
    val imageCapture = remember { ImageCapture.Builder().build() }
    var imageWidth by remember { mutableStateOf(1) }
    var imageHeight by remember { mutableStateOf(1) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val croppedFaceImageUri by viewModel.croppedFaceImageUri.collectAsState()
    var isCroppingInProgress by remember { mutableStateOf(false) }

    // Hapus state isMoreMenuExpanded dari sini
    // var isMoreMenuExpanded by remember { mutableStateOf(false) }

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

    // Show snackbar when face is detected
    LaunchedEffect(isFaceDetected) {
        if (isFaceDetected) {
            snackbarHostState.showSnackbar(
                message = "Wajah Terdeteksi",
                duration = SnackbarDuration.Short
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
                imageCapture,
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
                onMoreClick = onNavigateToMore // Langsung panggil onNavigateToMore
                // Hapus isMoreMenuExpanded, onToggleMoreMenu, onMoreOptionSelected dari sini
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
                        detectedFaces  = result.detections().map { it.boundingBox() },
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

                // Tombol Ambil Gambar
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
                                    Log.d("CameraScreen", "Photo capture succeeded: $savedUri")
                                    // TODO: Lakukan sesuatu dengan savedUri, misalnya tampilkan atau proses lebih lanjut
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    Log.e("CameraScreen", "Photo capture failed: ${exception.message}", exception)
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
                        .background(MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(32.dp))
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