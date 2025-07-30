package com.example.myapplication4.ui.addface

import android.util.Log
import android.util.Size
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.setValue
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.viewinterop.AndroidView
// import com.example.myapplication4.face.MediaPipeFaceDetector // TIDAK PERLU DIIMPOR LAGI DI SINI
import com.example.myapplication4.domain.utils.MediaPipeUtils.toBitmap
import com.example.myapplication4.ui.components.FaceOverlay
import java.util.concurrent.Executors
import androidx.navigation.NavController
import com.google.mediapipe.tasks.vision.facedetector.FaceDetectorResult
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.layout.layout


@androidx.annotation.OptIn(ExperimentalGetImage::class)
@Composable
fun AddFaceScreen(
    navController: NavController,
    viewModel: AddFaceViewModel = hiltViewModel(),
    onNavigateToCamera: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val recordingState by viewModel.recordingState.collectAsState()
    val recordingProgress by viewModel.recordingProgress.collectAsState()
    val message by viewModel.message.collectAsState()
    val lensFacing by viewModel.lensFacing.collectAsState()
    val isFaceDetected by viewModel.isFaceDetected.collectAsState()
    val imageDimensions by viewModel.imageDimensions.collectAsState()
    val previewView = remember { PreviewView(context) }

    val snackbarHostState = remember { SnackbarHostState() }

    val coroutineScope = rememberCoroutineScope()

    var imageWidth by remember { mutableStateOf(1) }
    var imageHeight by remember { mutableStateOf(1) }

    var userNameInput by remember { mutableStateOf(viewModel.name.value) }
    var userEmailInput by remember { mutableStateOf(viewModel.email.value) }
    var userPhoneInput by remember { mutableStateOf(viewModel.phone.value) }


    LaunchedEffect(Unit) {
        val cameraProvider = ProcessCameraProvider.getInstance(context).get()
        val preview = Preview.Builder().build().apply {
            setSurfaceProvider(previewView.surfaceProvider)
        }

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
            .build()

        val executor = Executors.newSingleThreadExecutor()
        val imageAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(Size(480, 640))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { analysis ->
                analysis.setAnalyzer(executor) { imageProxy ->
                    try {
                        if (imageProxy.image == null) {
                            Log.w("Analyzer", "imageProxy.image is null, skipping.")
                            imageProxy.close()
                            return@setAnalyzer
                        }
                        viewModel.updateImageDimensions(imageProxy.width, imageProxy.height)
                        viewModel.processFrame(imageProxy)
                    } catch (e: Exception) {
                        Log.e("Analyzer", "Crash in analyzer: ${e.message}", e)
                        imageProxy.close()
                    }
                }
            }

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalysis)
        } catch (e: Exception) {
            Log.e("AddFaceScreen", "Bind failed", e)
        }
    }

    LaunchedEffect(isFaceDetected) {
        if (isFaceDetected) { // Perubahan: langsung pakai isFaceDetected dari ViewModel
            snackbarHostState.showSnackbar(
                message = "Wajah Terdeteksi!",
                duration = SnackbarDuration.Short
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.resetState()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(paddingValues)
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(25.dp))

            Box(
                modifier = Modifier
                    .size(280.dp)
                    .fillMaxSize()
                    .background(Color.LightGray),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())


                val viewModelLiveDetectionResult by viewModel.liveDetectionResult.collectAsState()
                viewModelLiveDetectionResult?.let {
                    FaceOverlay(
                        modifier = Modifier.fillMaxSize(),
                        detectionResult = it,
                        imageWidth = imageDimensions.width,
                        imageHeight = imageDimensions.height,
                        isFrontCamera = lensFacing == CameraSelector.LENS_FACING_FRONT
                    )
                }
            }

            Spacer(modifier = Modifier.height(50.dp))

            OutlinedTextField(
                value = userNameInput,
                onValueChange = { userNameInput = it },
                label = { Text("Nama") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = userEmailInput,
                onValueChange = { userEmailInput = it },
                label = { Text("Email") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = userPhoneInput,
                onValueChange = { userPhoneInput = it },
                label = { Text("No Telepon (62)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            when (recordingState) {
                RecordingState.IDLE -> {
                    OutlinedButton(
                        onClick = {
                            if (userNameInput.isNotBlank() && userEmailInput.isNotBlank() && userPhoneInput.isNotBlank()) {
                                viewModel.startRecording(userNameInput, userEmailInput, userPhoneInput)
                            } else {
                                coroutineScope.launch {
                                    if (snackbarHostState.currentSnackbarData == null) {
                                        snackbarHostState.showSnackbar(
                                            message = "Harap isi semua kolom!",
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        enabled = userNameInput.isNotBlank() && userEmailInput.isNotBlank() && userPhoneInput.isNotBlank()
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Mulai Rekam Wajah")
                    }
                }

                RecordingState.RECORDING -> {
                    LinearProgressIndicator(
                        progress = recordingProgress,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Merekam... ${(recordingProgress * 100).toInt()}%")
                }

                RecordingState.PROCESSING -> {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Memproses wajah...")
                }

                RecordingState.DONE -> {
                    Text("Wajah berhasil ditambahkan!", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = { onNavigateToCamera() }) {
                        Text("Selesai")
                    }
                }
            }

            message?.let {
                Text(it, modifier = Modifier.padding(top = 12.dp))
            }
        }
    }
}