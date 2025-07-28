package com.example.myapplication4.ui.camera_option

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun CameraOptionScreen(
    navController: NavController
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
    ) {
        Text(text = "Choose Detection Type", style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(32.dp))

        Button(onClick = { navController.navigate("objectDetection") }) {
            Text(text = "Object Detection")
        }
        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { navController.navigate("faceDetection") }) {
            Text(text = "Face Detection")
        }
        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { navController.navigate("anomalyDetection") }) {
            Text(text = "Anomaly Detection")
        }
    }
}