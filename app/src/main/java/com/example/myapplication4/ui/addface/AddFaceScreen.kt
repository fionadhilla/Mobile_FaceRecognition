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
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.viewinterop.AndroidView
import com.example.myapplication4.face.MediaPipeFaceDetector
import com.example.myapplication4.domain.utils.MediaPipeUtils.toBitmap
import com.example.myapplication4.ui.components.FaceOverlay
import java.util.concurrent.Executors
import androidx.navigation.NavController
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facedetector.FaceDetectorResult
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import android.os.SystemClock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFaceScreen(
    navController: NavController, // Ubah parameter ke NavController
    viewModel: AddFaceViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val recordingState by viewModel.recordingState.collectAsState()
    val recordingProgress by viewModel.recordingProgress.collectAsState()
    val message by viewModel.message.collectAsState()
    val lensFacing by viewModel.lensFacing.collectAsState()

    var liveDetectionResult by remember { mutableStateOf<FaceDetectorResult?>(null) }
    val previewView = remember { PreviewView(context) }
    val liveFaceDetector = remember {
        try {
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath("blaze_face_short_range.tflite")
                .build()

            val options = com.google.mediapipe.tasks.vision.facedetector.FaceDetector.FaceDetectorOptions.builder()
                .setBaseOptions(baseOptions)
                .setMinDetectionConfidence(0.5f)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setResultListener { result: FaceDetectorResult, _ ->
                    liveDetectionResult = result
                }
                .setErrorListener { exception ->
                    Log.e("AddFaceScreen", "Live Face Detector Error: ${exception.message}")
                }
                .build()
            com.google.mediapipe.tasks.vision.facedetector.FaceDetector.createFromOptions(context, options)
        } catch (e: Exception) {
            Log.e("AddFaceScreen", "Failed to initialize Live Face Detector for overlay: ${e.message}")
            null
        }
    }
    var imageWidth by remember { mutableStateOf(1) }
    var imageHeight by remember { mutableStateOf(1) }

    // State untuk input pengguna
    var userNameInput by remember { mutableStateOf(viewModel.name.value) } // Ambil nilai awal dari ViewModel
    var userEmailInput by remember { mutableStateOf(viewModel.email.value) } // Ambil nilai awal dari ViewModel
    var userPhoneInput by remember { mutableStateOf(viewModel.phone.value) }


    // Inisialisasi dan bind CameraX
    LaunchedEffect(Unit) {
        val cameraProvider = withContext(Dispatchers.Main) {
            ProcessCameraProvider.getInstance(context).get()
        }

        val preview = Preview.Builder().build().apply {
            setSurfaceProvider(previewView.surfaceProvider)
        }

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_FRONT) // Biasanya untuk add face pakai kamera depan
            .build()

        val cameraExecutor = Executors.newSingleThreadExecutor()

        val imageAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(Size(480, 640)) // Resolusi target untuk analisis
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(cameraExecutor) { imageProxy ->
                    // Deteksi wajah untuk overlay real-time
                    try {
                        val bitmap = imageProxy.toBitmap()
                        imageWidth = bitmap.width
                        imageHeight = bitmap.height
                        liveFaceDetector?.detectAsync(BitmapImageBuilder(bitmap).build(), SystemClock.uptimeMillis())

                        // Teruskan frame ke ViewModel jika sedang merekam
                        if (recordingState == RecordingState.RECORDING) {
                            viewModel.processFrameForRecording(imageProxy)
                        } else {
                            imageProxy.close() // Penting: Tutup ImageProxy jika tidak digunakan oleh ViewModel
                        }
                    } catch (e: Exception) {
                        Log.e("AddFaceScreen", "Error analyzing image for overlay: ${e.message}")
                        imageProxy.close()
                    }
                }
            }

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalysis
            )
        } catch (e: Exception) {
            Log.e("AddFaceScreen", "Camera binding failed", e)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            liveFaceDetector?.close() // Pastikan face detector ditutup saat composable dibuang
            viewModel.resetState() // Reset state ViewModel saat layar dibuang
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tambah Wajah Baru") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = remember { SnackbarHostState() }) } // Snackbar host
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color.Black) // Background hitam untuk area kamera
            ) {
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier.fillMaxSize()
                )

                // Overlay Bounding Boxes real-time
                liveDetectionResult?.let { result ->
                    FaceOverlay(
                        modifier = Modifier.fillMaxSize(),
                        detectionResult = result,
                        imageWidth = imageWidth,
                        imageHeight = imageHeight,
                        isFrontCamera = lensFacing == CameraSelector.LENS_FACING_FRONT
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Input Nama
            OutlinedTextField(
                value = userNameInput,
                onValueChange = { userNameInput = it },
                label = { Text("Nama") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 4.dp),
                enabled = recordingState == RecordingState.IDLE // Hanya bisa diedit saat IDLE
            )

            // Input Email
            OutlinedTextField(
                value = userEmailInput,
                onValueChange = { userEmailInput = it },
                label = { Text("Email") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 4.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                enabled = recordingState == RecordingState.IDLE // Hanya bisa diedit saat IDLE
            )

            // Input Phone
            OutlinedTextField(
                value = userPhoneInput,
                onValueChange = { userPhoneInput = it },
                label = { Text("Phone (+62)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 4.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                enabled = recordingState == RecordingState.IDLE // Hanya bisa diedit saat IDLE
            )

            Spacer(modifier = Modifier.height(16.dp))

            // UI untuk kontrol perekaman/status
            when (recordingState) {
                RecordingState.IDLE -> {
                    Button(
                        onClick = {
                            if (userNameInput.isNotBlank() && userEmailInput.isNotBlank() && userPhoneInput.isNotBlank()) {
                                viewModel.startRecording(userNameInput, userEmailInput, userPhoneInput)
                            } else {
                                navController.currentBackStackEntry?.let { entry ->
                                    val snackbarHostState = entry.lifecycle.currentState.let {
                                        Log.e("AddFaceScreen", "Nama, Email, dan Nomor Telepon tidak boleh kosong.")
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .height(50.dp),
                        enabled = userNameInput.isNotBlank() && userEmailInput.isNotBlank() && userPhoneInput.isNotBlank()
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Mulai Rekam")
                        Spacer(Modifier.width(8.dp))
                        Text("Mulai Perekaman Wajah")
                    }
                }
                RecordingState.RECORDING -> {
                    LinearProgressIndicator(
                        progress = recordingProgress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Merekam... ${(recordingProgress * 100).toInt()}%")
                }
                RecordingState.PROCESSING -> {
                    CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Memproses wajah...")
                }
                RecordingState.DONE -> {
                    Text("Wajah berhasil ditambahkan!", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { navController.popBackStack() }) {
                        Text("Selesai")
                    }
                }
            }

            message?.let { msg ->
                Text(msg, modifier = Modifier.padding(16.dp))
            }
        }
    }
}