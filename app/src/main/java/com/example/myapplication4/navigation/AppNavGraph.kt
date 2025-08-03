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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.currentBackStackEntryAsState

import android.net.Uri
import androidx.compose.material3.Text
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.compose.ui.platform.LocalContext
import com.example.myapplication4.ui.camera_option.CameraOptionScreen

@Composable
fun AppNavGraph(
    navController: NavHostController,
    loginStateViewModel: LoginStateViewModel,
    startDestination: String
) {
    val activityViewModelStoreOwner = checkNotNull(LocalContext.current as? androidx.lifecycle.ViewModelStoreOwner) {
        "AppNavGraph harus berada dalam konteks ViewModelStoreOwner (misalnya, Activity)."
    }

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
                viewModel = hiltViewModel(viewModelStoreOwner = activityViewModelStoreOwner),
                onNavigateToHistory = { navController.navigate("history") },
                onNavigateToAddFace = { navController.navigate("addFace") },
                onNavigateToProfile = { navController.navigate("profile") },
                onNavigateToMore = { navController.navigate("cameraOption") }
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
            EditProfileScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable("addFace") {
            AddFaceScreen(
                navController = navController,
                onNavigateToCamera = {navController.navigate("camera")}
            )
        }

        composable("cameraOption"){
           CameraOptionScreen(
               navController = navController,
               onNavigateToFaceDetection = {
                   navController.navigate("camera")
               }
           )
        }

        composable("objectDetection") {
            Text("Object Detection Screen (Not Implemented Yet)")
        }

        composable("anomalyDetection") {
            Text("Anomaly Detection Screen (Not Implemented Yet)")
        }
    }
}