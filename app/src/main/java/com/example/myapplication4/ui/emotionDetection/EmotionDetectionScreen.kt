package com.example.myapplication4.ui.emotionDetection

import android.Manifest
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myapplication4.ui.components.BottomNavBarMoreOption
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun EmotionDetectionScreen(
    viewModel: EmotionDetectionViewModel = hiltViewModel(),
    onNavigateToHistory : () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToMore: () -> Unit
) {
    val cameraPermissionState = rememberPermissionState(permission = Manifest.permission.CAMERA)

    val detectedEmotion by viewModel.detectedEmotion.collectAsState()
    val lensFacing by viewModel.lensFacing.collectAsState()
    val isModelLoaded by viewModel.isModelLoaded.collectAsState()

    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val previewView = remember { PreviewView(context) }

    LaunchedEffect(lensFacing) {
        val cameraProvider = cameraProviderFuture.get()
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        val imageAnalyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(cameraExecutor) { imageProxy ->
                    viewModel.processImageProxy(imageProxy)
                }
            }

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalyzer
            )
        } catch (e: Exception) {
            Log.e("EmotionDetectionCamera", "Error binding camera: ${e.message}")
        }
    }

    if (cameraPermissionState.status.isGranted) {
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier.fillMaxSize()
                )

                if (isModelLoaded) {
                    val displayText = detectedEmotion
                    Text(
                        text = "Emosi: $displayText",
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        color = Color.Green,
                        style = MaterialTheme.typography.titleMedium
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
    } else {
        LaunchedEffect(Unit) {
            cameraPermissionState.launchPermissionRequest()
        }
    }
}