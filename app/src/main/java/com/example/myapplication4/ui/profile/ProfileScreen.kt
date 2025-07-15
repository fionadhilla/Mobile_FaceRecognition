package com.example.myapplication4.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.myapplication4.ui.login.LoginStateViewModel
import com.example.myapplication4.ui.profile.ProfileViewModel

@Composable
fun ProfileScreen(navController: NavController,
                  viewModel: ProfileViewModel = hiltViewModel(),
                  loginStateViewModel: LoginStateViewModel,
                  onNavigateToEditProfile: () -> Unit = {})
{
    val userProfile by viewModel.userProfile.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .background(Color.Gray)
        ) {
            IconButton(
                onClick = {
                    navController.popBackStack()
                },
                modifier = Modifier.padding(start = 16.dp, top = 24.dp).align(Alignment.TopStart)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }

            Text(
                text = "Profile",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 34.dp),
                color = Color.White
            )

            Column(
                modifier = Modifier.align(Alignment.BottomCenter),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "Profile Image",
                    modifier = Modifier.size(80.dp),
                    tint = Color.White
                )
                Text(
                    text = userProfile.fullName,
                    color = Color.White,
                    modifier = Modifier.padding(top = 8.dp, bottom = 50.dp),
                )
            }
        }

        // Menu List
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .background(color = Color.White)
                .padding(horizontal = 32.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ProfileMenuItem("Edit Profile", onClick = onNavigateToEditProfile)
            ProfileMenuItem("Change Password") { /* Navigate */ }
            ProfileMenuItem("Notification Settings") { /* Navigate */ }
            ProfileMenuItem("About App") { /* Navigate */ }
            Button(
                onClick = {
                    loginStateViewModel.logout()
                    navController.navigate("login") {
                        popUpTo("camera") { inclusive = true }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Log Out")
            }
        }
    }
}

@Composable
fun ProfileMenuItem(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Edit, contentDescription = null, tint = Color.Gray)
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = text)
        }
        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null)
    }
}

//@Preview(showBackground = true, widthDp = 360, heightDp = 640)
//@Composable
//fun ProfileScreenPreview() {
//    val navController = rememberNavController()
//
//    val profileViewModel = ProfileViewModel()
//    profileViewModel.setUserName("Jane Doe")
//
//    val loginStateViewModel = object : LoginStateViewModel() {
//        override fun logout() {
//
//        }
//    }
//
//    ProfileScreen(
//        navController = navController,
//        viewModel = profileViewModel,
//        loginStateViewModel = loginStateViewModel,
//        onNavigateToEditProfile = {}
//    )
//}