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
    onNavigateToActivityDetection: () -> Unit,
    onNavigateToGestureDetection: () -> Unit,
    onNavigateToEmotionDetection: () -> Unit
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

            DetectionButton(
              text = "Face Detection",
              onClick = onNavigateToFaceDetection
            )

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

            Button(
                onClick = onNavigateToVehicleDetection,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Deteksi Kendaraan")
            }

            Spacer(modifier = Modifier.height(16.dp))

        
            Button(
                onClick = onNavigateToAnomalyDetection,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Deteksi Anomali")
            }

        DetectionButton(
            text = "People Counting",
            onClick = onNavigateToPeopleCounting
        )

        Spacer(modifier = Modifier.height(24.dp))

        DetectionButton(
            text = "Activity Detection",
            onClick = onNavigateToActivityDetection
        )

        Spacer(modifier = Modifier.height(24.dp))

        DetectionButton(
            text = "Gesture Detection",
            onClick = onNavigateToGestureDetection
        )

        Spacer(modifier = Modifier.height(24.dp))

        DetectionButton(
            text = "Emotion Detection",
            onClick = onNavigateToEmotionDetection
        )
        
        Spacer(modifier = Modifier.height(32.dp))
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
