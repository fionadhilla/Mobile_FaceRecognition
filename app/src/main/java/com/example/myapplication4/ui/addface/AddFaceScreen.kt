package com.example.myapplication4.ui.addface

import android.util.Log
import android.util.Size
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.myapplication4.ui.components.FaceOverlay
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

@androidx.annotation.OptIn(ExperimentalGetImage::class)
@Composable
fun AddFaceScreen(
    navController: NavController,
    viewModel: AddFaceViewModel = hiltViewModel(),
    onNavigateToCamera: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    val previewView = remember { PreviewView(context) }
    val snackbarHostState = remember { SnackbarHostState() }

    val recordingState by viewModel.recordingState.collectAsState()
    val recordingProgress by viewModel.recordingProgress.collectAsState()
    val message by viewModel.message.collectAsState()
    val errorEmail by viewModel.emailError.collectAsState()
    val errorPhone by viewModel.phoneError.collectAsState()
    val lensFacing by viewModel.lensFacing.collectAsState()
    val isFaceDetected by viewModel.isFaceDetected.collectAsState()
    val imageDimensions by viewModel.imageDimensions.collectAsState()
    val detectionResult by viewModel.liveDetectionResult.collectAsState()

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
        if (isFaceDetected) {
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

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .background(Color.White)
                .padding(paddingValues)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.weight(1f))

                Text("")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Kamera Preview + Overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f)
                    .background(Color.LightGray),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

                detectionResult?.let {
                    FaceOverlay(
                        modifier = Modifier.fillMaxSize(),
                        detectionResult = it,
                        imageWidth = imageDimensions.width,
                        imageHeight = imageDimensions.height,
                        isFrontCamera = lensFacing == CameraSelector.LENS_FACING_FRONT
                    )
                }

                if (recordingState == RecordingState.PROCESSING) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color.White)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Memproses wajah...", color = Color.White)
                        }
                    }
                }

            }

            // Progress RECORDING (di bawah kamera)
            if (recordingState == RecordingState.RECORDING) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { recordingProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Merekam... ${(recordingProgress * 100).toInt()}%",
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Gerakan wajah agar melihat ke sisi kanan dan sisi kiri",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        color = Color.Black,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (recordingState == RecordingState.DONE) {
                Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Wajah berhasil ditambahkan!",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Input Form
            OutlinedTextField(
                value = userNameInput,
                onValueChange = { userNameInput = it },
                label = { Text("Nama") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = userEmailInput,
                onValueChange = { userEmailInput = it },
                label = { Text("Email") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (errorEmail?.isNotBlank() == true) {
                errorEmail?.let {
                    Text(
                        text = it,
                        color = Color.Red,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(0.dp))
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = userPhoneInput,
                onValueChange = { userPhoneInput = it },
                label = { Text("No Telepon (62)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (errorPhone?.isNotBlank() == true) {
                errorPhone?.let {
                    Text(
                        text = it,
                        color = Color.Red,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(0.dp))
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Tombol aksi
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

                RecordingState.DONE -> {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = { onNavigateToCamera() }) {
                        Text("Selesai")
                    }
                }

                else -> {}
            }
        }
    }
}
