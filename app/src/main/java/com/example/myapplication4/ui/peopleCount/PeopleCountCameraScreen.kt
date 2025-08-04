package com.example.myapplication4.ui.peopleCount

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
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.myapplication4.ui.components.BottomNavBarPeopleCount
import com.example.myapplication4.ui.components.DetectionOverlay

@Composable
fun PeopleCountCameraScreen(
    viewModel: PeopleCountViewModel = hiltViewModel(),
    onNavigateToHistory: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToMore: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val lensFacing by viewModel.lensFacing.collectAsState()
    val detectedPeople by viewModel.detectedPeople.collectAsState()
    val peopleCount by viewModel.peopleCount.collectAsState()

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val cameraExecutor = remember { java.util.concurrent.Executors.newSingleThreadExecutor() }
    val cameraView = remember { PreviewView(context) }

    LaunchedEffect(lensFacing) {
        val cameraProvider = cameraProviderFuture.get()
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(cameraView.surfaceProvider)
        }
        val CameraSelector = CameraSelector.Builder()
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
                CameraSelector,
                preview,
                imageAnalysis
            )
        } catch (e: Exception) {
            Log.d("PeopleCountCam", "error message: $e")
        }
    }

    Scaffold (
        bottomBar = {
            BottomNavBarPeopleCount(
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
                AndroidView(
                    factory = { cameraView },
                    modifier = Modifier.fillMaxSize()
                )

                // Gunakan komponen modular DetectionOverlay
                DetectionOverlay(boundingBoxes = detectedPeople)

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

                Text(
                    text = "People Count: $peopleCount",
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    color = Color.White
                )
            }
        }
    }
}