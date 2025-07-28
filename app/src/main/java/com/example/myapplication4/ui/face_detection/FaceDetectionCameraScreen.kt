// In app/src/main/java/com/example/myapplication4/ui/facedetection/FaceDetectionCameraScreen.kt
package com.example.myapplication4.ui.facedetection

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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myapplication4.domain.utils.MediaPipeUtils.toBitmap // Reconfirm if this utility is still appropriate or need custom
import com.example.myapplication4.ui.components.BottomNavBar
import com.example.myapplication4.ui.components.FaceOverlay // Will need to adapt this component
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun FaceDetectionCameraScreen(
    viewModel: FaceDetectionCameraViewModel = hiltViewModel(),
    onNavigateToHistory: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToMore: () -> Unit,
    // onNavigateToAddFace: (Uri?) -> Unit // Removed if only detection is needed
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    val lensFacing by viewModel.lensFacing.collectAsState()
    val detectedFaces by viewModel.detectedFaces.collectAsState() // Observe the new StateFlow
    val isFaceDetected by viewModel.isFaceDetected.collectAsState()
    val previewView = remember {
        PreviewView(context).also {
            it.scaleType = PreviewView.ScaleType.FILL_CENTER // Often better for preview
        }
    }
    var imageWidth by remember { mutableStateOf(1) }
    var imageHeight by remember { mutableStateOf(1) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Removed cropping related states and LaunchedEffect if only detection is needed
    // val croppedFaceImageUri by viewModel.croppedFaceImageUri.collectAsState()
    // var isCroppingInProgress by remember { mutableStateOf(false) }

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
            .setTargetResolution(Size(previewView.width, previewView.height)) // Match preview view size for analysis if possible
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { analysis ->
                analysis.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
                    try {
                        val bitmap = imageProxy.toBitmap() // Ensure this utility correctly handles imageProxy to Bitmap
                        imageWidth = imageProxy.width
                        imageHeight = imageProxy.height
                        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                        viewModel.processFrame(bitmap, rotationDegrees)
                    } catch (e: Exception) {
                        Log.e("Analyzer", "Error processing image frame", e)
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
            Log.e("FaceDetectionCameraScreen", "Camera binding failed", e)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            BottomNavBar(
                onHistoryClick = onNavigateToHistory,
                // Removed onAddClick as it's for face recognition, not just detection
                onAddClick = { /* No action or provide a different action for Face Detection mode */ },
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
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier.fillMaxSize()
                )

                // Pass the list of RectF to FaceOverlay
                FaceOverlay(
                    modifier = Modifier.fillMaxSize(),
                    detectedFaces = detectedFaces, // Pass the new data
                    imageWidth = imageWidth,
                    imageHeight = imageHeight,
                    isFrontCamera = lensFacing == CameraSelector.LENS_FACING_FRONT
                )


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