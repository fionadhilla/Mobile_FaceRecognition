package com.example.myapplication4.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.myapplication4.ui.camera.CameraScreen

@Composable
fun AppNavGraph(navController: NavHostController) {
    val navController = rememberNavController()
    NavHost(navController, startDestination = "camera") {
        composable("camera") {
            CameraScreen(
                onNavigateToLog = { navController.navigate("history") },
                onNavigateToAddFace = { navController.navigate("addFace") },
                onNavigateToProfile = { navController.navigate("profile") }
            )
        }
//        composable("history") { NotificationScreen() }
//        composable("addFace") { AddFaceScreen() }
//        composable("profile") { ProfileScreen() }
    }
}