package com.example.myapplication4.ui.activityDetection

import android.Manifest
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.myapplication4.ui.components.BottomNavBarMoreOption
import com.example.myapplication4.ui.components.ActivityDetectionOverlay
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ActivityDetectionScreen(
    viewModel: ActivityDetectionViewModel = hiltViewModel(),
    onNavigateToHistory: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToMore: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraPermissionState = rememberPermissionState(permission = Manifest.permission.CAMERA)
    val lensFacing by viewModel.lensFacing.collectAsState()
    val detectionResults by viewModel.detectionResults.collectAsState()
    val isModelReady by viewModel.isModelReady.collectAsState()

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val cameraView = remember { PreviewView(context) }

    // Pemicu pemuatan model saat LaunchedEffect pertama kali masuk
    LaunchedEffect(Unit) {
        viewModel.loadModel()
    }

    LaunchedEffect(lensFacing, isModelReady) {
        if (cameraPermissionState.status.isGranted && isModelReady) {
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(cameraView.surfaceProvider)
            }
            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            viewModel.startCameraAnalysis(imageAnalysis)

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
            } catch (e: Exception) {
                Log.d("ActivityDetectionScreen", "error message: $e")
            }
        } else if (!isModelReady && cameraPermissionState.status.isGranted) {
            // Model belum siap, tunggu
            Log.d("ActivityDetectionScreen", "Model is not ready, waiting...")
        } else {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            val cameraProvider = cameraProviderFuture.get()
            cameraProvider.unbindAll()
        }
    }

    val detectedActivities = detectionResults.map { it.label }.distinct().joinToString(separator = ", ")

    Scaffold(
        bottomBar = {
            BottomNavBarMoreOption(
                onHistoryClick = onNavigateToHistory,
                onProfileClick = onNavigateToProfile,
                onMoreClick = onNavigateToMore
            )
        },
        modifier = Modifier.background(color = Color.LightGray)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (isModelReady) {
                    AndroidView(
                        factory = { cameraView },
                        modifier = Modifier.fillMaxSize()
                    )
                    ActivityDetectionOverlay(results = detectionResults)
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                        Text(
                            text = "Memuat model...",
                            modifier = Modifier.align(Alignment.Center).padding(top = 80.dp),
                            color = Color.White
                        )
                    }
                }

                if (isModelReady) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(16.dp)
                            .background(
                                Color.Black.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Aktivitas terdeteksi: $detectedActivities",
                            color = Color.White
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
                }
            }
        }
    }
}