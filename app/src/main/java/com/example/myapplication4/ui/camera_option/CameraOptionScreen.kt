package com.example.myapplication4.ui.camera_option

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
    onNavigateToActivityDetection: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 24.dp)
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

        Spacer(modifier = Modifier.height(40.dp))

        // Face Detection
        DetectionButton(
            text = "Face Detection",
            onClick = onNavigateToFaceDetection
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Vehicle Detection
        DetectionButton(
            text = "Vehicle Detection",
            onClick = { navController.navigate("objectDetection") }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Anomaly Detection
        DetectionButton(
            text = "Anomaly Detection",
            onClick = { navController.navigate("anomalyDetection") }
        )

        Spacer(modifier = Modifier.height(24.dp))

        DetectionButton(
            text = "People Counting",
            onClick = onNavigateToPeopleCounting
        )

        Spacer(modifier = Modifier.height(24.dp))

        DetectionButton(
            text = "Activity Detection",
            onClick = onNavigateToActivityDetection
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
