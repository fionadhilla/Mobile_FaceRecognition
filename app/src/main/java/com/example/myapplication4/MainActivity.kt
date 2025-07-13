package com.example.myapplication4

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.myapplication4.navigation.AppNavGraph
import com.example.myapplication4.ui.login.LoginStateViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val cameraPermissionState =
                rememberPermissionState(permission = Manifest.permission.CAMERA)

            LaunchedEffect(Unit) {
                if (!cameraPermissionState.status.isGranted) {
                    cameraPermissionState.launchPermissionRequest()
                }
            }

            if (cameraPermissionState.status.isGranted) {
                val navController = rememberNavController()
                val loginStateViewModel: LoginStateViewModel = viewModel()
                val isLoggedIn by loginStateViewModel.isLoggedIn.collectAsState()
                AppNavGraph(
                    navController,
                    loginStateViewModel,
                    startDestination = if (isLoggedIn) "camera" else "login")
            }
        }
    }
}