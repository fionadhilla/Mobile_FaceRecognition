package com.example.myapplication4.navigation

import androidx.compose.runtime.*
import androidx.navigation.*
import androidx.navigation.compose.*
import com.example.myapplication4.ui.addface.AddFaceScreen
import com.example.myapplication4.ui.camera.CameraScreen
import com.example.myapplication4.ui.login.LoginScreen
import com.example.myapplication4.ui.notifikasi.HistoryScreen
import com.example.myapplication4.ui.profile.ProfileScreen
import com.example.myapplication4.ui.login.LoginStateViewModel
import com.example.myapplication4.ui.edit_profile.EditProfileScreen

@Composable
fun AppNavGraph(
    navController: NavHostController,
    loginStateViewModel: LoginStateViewModel,
    startDestination: String
) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    loginStateViewModel.login()
                    navController.navigate("camera") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
        composable("camera") {
            CameraScreen(
                onNavigateToHistory = { navController.navigate("history") },
                onNavigateToAddFace = { navController.navigate("addFace") },
                onNavigateToProfile = { navController.navigate("profile") }
            )
        }

        composable("history") {
            HistoryScreen(navController = navController)
        }

        composable("profile") {
            ProfileScreen(
                navController = navController,
                loginStateViewModel = loginStateViewModel,
                onNavigateToEditProfile = {
                    navController.navigate("editProfile")
                }
            )
        }
        composable("editProfile") {
            EditProfileScreen(navController = navController)
        }

        composable("addFace") {
            AddFaceScreen(
                onBack = {
                    navController.navigate("camera")
                },
                onRetakePhoto = {
                    navController.navigate("camera")
                },
                onSave = { _,_ ->
                    navController.navigate("camera")
                }
            )
        }
    }
}