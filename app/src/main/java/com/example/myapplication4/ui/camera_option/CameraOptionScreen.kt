// In app/src/main/java/com/example/myapplication4/ui/camera_option/CameraOptionScreen.kt
package com.example.myapplication4.ui.camera_option

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun CameraOptionScreen(
    onNavigateToFaceDetection: () -> Unit,
    onNavigateToObjectDetection: () -> Unit,
    onNavigateToAnomalyDetection: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Choose Detection Type", style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(32.dp))

        Button(onClick = onNavigateToObjectDetection) {
            Text("Object Detection")
        }
        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onNavigateToFaceDetection) {
            Text("Live Face Detection (YOLOv12)")
        }
        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onNavigateToAnomalyDetection) {
            Text("Anomaly Detection")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CameraOptionScreenPreview() {
    MaterialTheme { // Wrap with MaterialTheme to apply default styling
        CameraOptionScreen(
            onNavigateToFaceDetection = {}, // Dummy lambda
            onNavigateToObjectDetection = {}, // Dummy lambda
            onNavigateToAnomalyDetection = {} // Dummy lambda
        )
    }
}