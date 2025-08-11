package com.example.myapplication4.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.myapplication4.ui.addface.AddFaceScreen
import com.example.myapplication4.ui.camera.CameraScreen
import com.example.myapplication4.ui.camera_option.CameraOptionScreen
import com.example.myapplication4.ui.crowd_detection.CrowdDetectionCameraScreen
import com.example.myapplication4.ui.crowd_detection.CrowdDetectionViewModel
import com.example.myapplication4.ui.edit_profile.EditProfileScreen
import com.example.myapplication4.ui.login.LoginScreen
import com.example.myapplication4.ui.login.LoginStateViewModel
import com.example.myapplication4.ui.notifikasi.HistoryScreen
import com.example.myapplication4.ui.profile.ProfileScreen
import com.example.myapplication4.ui.vehicle_detection.VehicleDetectionScreen
import com.example.myapplication4.ui.vehicle_detection.VehicleDetectionViewModel

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
//            AddFaceScreen(
//                navController = navController,
//                onNavigateToCamera = {navController.navigate("camera")}
//            )
        }

        composable("peopleCount") {
//            PeopleCountCameraScreen(
//                onNavigateToHistory = { navController.navigate("history") },
//                onNavigateToProfile = { navController.navigate("profile") },
//                onNavigateToMore = { navController.navigate("cameraOption") }
//            )
        }

        composable("ActivityDetection") {
//            ActivityDetectionScreen(
//                onNavigateToHistory = { navController.navigate("history") },
//                onNavigateToProfile = { navController.navigate("profile") },
//                onNavigateToMore = { navController.navigate("cameraOption") }
//            )
        }

        composable("cameraOption"){
            CameraOptionScreen(
                navController = navController,
                onNavigateToFaceDetection = {
                    navController.navigate("camera")
                },

                onNavigateToObjectDetection = {
                    navController.navigate("ObjectDetection")
                },

                onNavigateToAnomalyDetection = {
                    navController.navigate("AnomalyDetection")
                },

                onNavigateToVehicleDetection = {
                    navController.navigate("VehicleDetection")
                },

                onNavigateToCrowdDetection = {
                    navController.navigate("CrowdDetection")
                }
            )
        }

        composable("objectDetection") {
            Text("Object Detection Screen (Not Implemented Yet)")
        }

        composable("vehicleDetection") {
            VehicleDetectionScreen(
                viewModel = hiltViewModel<VehicleDetectionViewModel>(viewModelStoreOwner = activityViewModelStoreOwner),
                onNavigateToAddFace = { navController.navigate("addFace") },
                onNavigateToHistory = { navController.navigate("history") },
                onNavigateToProfile = { navController.navigate("profile") },
                onNavigateToMore = { navController.navigate("cameraOption") }
            )
        }

        composable("CrowdDetection") {
            CrowdDetectionCameraScreen(
                crowdDetectionViewModel  = hiltViewModel<CrowdDetectionViewModel>(viewModelStoreOwner = activityViewModelStoreOwner),
                onNavigateToAddFace = { navController.navigate("addFace") },
                onNavigateToHistory = { navController.navigate("history") },
                onNavigateToProfile = { navController.navigate("profile") },
                onNavigateToMore = { navController.navigate("cameraOption") }
            )
        }

        composable("anomalyDetection") {
            Text("Anomaly Detection Screen (Not Implemented Yet)")
        }
    }
}