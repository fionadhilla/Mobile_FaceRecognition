package com.example.myapplication4.ui.camera_option

import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun CameraOptionScreen(
    navController: NavController,
    onNavigateToFaceDetection: () -> Unit,
    onNavigateToPeopleCounting: () -> Unit,
    onNavigateToObjectDetection: () -> Unit,
    onNavigateToVehicleDetection: () -> Unit,
    onNavigateToAnomalyDetection: () -> Unit,
    onNavigateToCrowdDetection: () -> Unit,

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
            
            Spacer(modifier = Modifier.height(24.dp))

            DetectionButton(
                text = "People Counting",
                onClick = onNavigateToPeopleCounting
            )

            Spacer(modifier = Modifier.height(16.dp))

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

            // Tombol Crowd Detection
            Button(
                onClick = onNavigateToCrowdDetection,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Deteksi Keramaian")
            }
        }
    }
}

@Composable
fun DetectionButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(Color.LightGray)
            .clickable { onClick() }
            .padding(start = 24.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Black
        )
    }
}
