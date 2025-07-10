package com.example.myapplication4.ui.camera

import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.myapplication4.ui.components.BottomNavBar

@Composable
fun CameraScreen(
    viewModel: CameraViewModel = viewModel(),
    onNavigateToLog: () -> Unit,
    onNavigateToAddFace: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val lensFacing by viewModel.lensFacing.collectAsState()
    val previewView = remember { PreviewView(context) }

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

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                selector,
                preview
            )
        } catch (e: Exception) {
            Log.e("CameraScreen", "Camera binding failed", e)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier
            .weight(1f)
            .fillMaxWidth()) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )

            IconButton(
                onClick = { viewModel.switchCamera() },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .size(48.dp)
                    .background(Color.White, shape = MaterialTheme.shapes.small)
            ) {
                Icon(
                    imageVector = Icons.Default.Cameraswitch,
                    contentDescription = "Switch Camera"
                )
            }
        }
        // Bottom Navigation Bar
        BottomNavBar(
            onLogClick = onNavigateToLog,
            onAddClick = onNavigateToAddFace,
            onProfileClick = onNavigateToProfile
        )
    }
}