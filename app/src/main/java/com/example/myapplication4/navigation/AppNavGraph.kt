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
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.myapplication4.ui.camera_option.CameraOptionScreen

import android.net.Uri
import androidx.compose.material3.Text
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.compose.ui.platform.LocalContext

@Composable
fun AppNavGraph(
    navController: NavHostController,
    loginStateViewModel: LoginStateViewModel,
    startDestination: String
) {
    // Dapatkan ViewModelStoreOwner dari Activity saat ini.
    // Ini menjamin instance ViewModel akan stabil sepanjang siklus hidup Activity.
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
                onNavigateToAddFace = { imageUri ->
                    val route = if (imageUri != null) {
                        "addFace?imageUri=${Uri.encode(imageUri.toString())}"
                    } else {
                        "addFace?imageUri=null"
                    }
                    navController.navigate(route)
                },
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
                onBackClick = { navController.popBackStack() } // Panggil navController.popBackStack() saat onBackClick dipanggil
            )
        }

        composable(
            "addFace?imageUri={imageUri}",
            arguments = listOf(navArgument("imageUri") {
                type = NavType.StringType
                nullable = true
            })
        ) { backStackEntry ->
            val imageUriString = backStackEntry.arguments?.getString("imageUri")
            val imageUri = imageUriString?.let {
                if (it == "null") null else Uri.parse(Uri.decode(it))
            }

            AddFaceScreen(
                initialImageUri = imageUri,
                onBack = {
                    navController.navigate("camera") { popUpTo("camera") { inclusive = true } }
                },
                onRetakePhoto = {
                    navController.navigate("camera") { popUpTo("camera") { inclusive = true } }
                },
                onSave = { _,_ ->
                    navController.navigate("camera") { popUpTo("camera") { inclusive = true } }
                }
            )
        }

        composable("cameraOption") {
            CameraOptionScreen(navController = navController)
        }

        // Placeholder composables for detection types
        composable("objectDetection") {
            Text("Object Detection Screen (Not Implemented Yet)")
        }
        composable("faceDetection") {
            Text("Face Detection Screen (Not Implemented Yet)")
        }
        composable("anomalyDetection") {
            Text("Anomaly Detection Screen (Not Implemented Yet)")
        }
    }
}