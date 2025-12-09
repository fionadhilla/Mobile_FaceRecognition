package com.example.myapplication4.ui.camera_option

import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
    onNavigateToEmotionDetection: () -> Unit,
    onNavigateToThreatDetection: () -> Unit,
    onNavigateToVehicleDetection: () -> Unit,
    onNavigateToCrowdDetection: () -> Unit
) {
    val state = rememberScrollState()
    LaunchedEffect(Unit) { state.animateScrollTo(100) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 24.dp)
            .verticalScroll(state)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                modifier = Modifier
                    .size(32.dp)
                    .clickable { navController.popBackStack() }
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "More Detection Option",
                style = MaterialTheme.typography.headlineMedium
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

        DetectionButton(
            text = "Face Detection",
            onClick = onNavigateToFaceDetection
        )

            // Tombol Object Detection
            Button(
                onClick = onNavigateToObjectDetection,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Deteksi Objek")
            }
   

        DetectionButton(
            text = "Vehicle Detection",
            onClick = onNavigateToVehicleDetection
        )

            Button(
                onClick = onNavigateToVehicleDetection,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Deteksi Kendaraan")
            }

//        DetectionButton(
//            text = "Anomaly Detection",
//            onClick = { navController.navigate("anomalyDetection") }
//        )
//
//        Spacer(modifier = Modifier.height(24.dp))

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

        Spacer(modifier = Modifier.height(24.dp))

        DetectionButton(
            text = "Threat Detection",
            onClick = onNavigateToThreatDetection
        )

        Spacer(modifier = Modifier.height(24.dp))

        DetectionButton(
            text = "Crowd Detection",
            onClick = onNavigateToCrowdDetection
        )
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
