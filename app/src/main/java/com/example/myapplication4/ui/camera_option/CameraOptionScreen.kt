package com.example.myapplication4.ui.camera_option

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraOptionScreen(
    navController: NavController,
    onNavigateToFaceDetection: () -> Unit,
    onNavigateToObjectDetection: () -> Unit,
    onNavigateToVehicleDetection: () -> Unit,
    onNavigateToAnomalyDetection: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pilih Mode Kamera") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Tombol Face Recognition
            Button(
                onClick = onNavigateToFaceDetection,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Deteksi Wajah")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tombol Object Detection
            Button(
                onClick = onNavigateToObjectDetection,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Deteksi Objek")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tombol BARU: Vehicle Detection
            Button(
                onClick = onNavigateToVehicleDetection,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Deteksi Kendaraan")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tombol Anomaly Detection
            Button(
                onClick = onNavigateToAnomalyDetection,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Deteksi Anomali")
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewCameraOptionScreen() {
    CameraOptionScreen(
        navController = rememberNavController(),
        onNavigateToFaceDetection = {},
        onNavigateToObjectDetection = {},
        onNavigateToVehicleDetection = {},
        onNavigateToAnomalyDetection = {}
    )
}
