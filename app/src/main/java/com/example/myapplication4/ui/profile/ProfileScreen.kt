package com.example.myapplication4.ui.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication4.R

@Composable
fun ProfileScreen() {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Header Section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .background(Color(0xFFF0F0F0)) // Light gray background to match XML feel
        ) {
            // Back Arrow
            Icon(
                imageVector = Icons.Filled.ArrowBack,
                contentDescription = "Back",
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 24.dp, top = 24.dp)
                    .size(24.dp) // Explicit size for consistency
                    .clickable { /* TODO: Handle back button click */ }
            )

            // Profile Title
            Text(
                text = "Profile",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 24.dp)
            )

            // Avatar
            // Using a placeholder drawable for the avatar.
            // Replace R.drawable.placeholder_avatar with your actual avatar drawable.
            // If you don't have a specific drawable, you can use a simple colored circle or a generic icon.
            Image(
                painter = painterResource(id = android.R.drawable.sym_def_app_icon), // Placeholder for tools:sample/avatars
                contentDescription = "Profile Picture",
                modifier = Modifier
                    .size(100.dp) // Adjusted size for better visual balance
                    .clip(CircleShape)
                    .background(Color.Gray) // Background for the circular avatar
                    .align(Alignment.BottomCenter) // Align to bottom center
                    .offset(y = (-40).dp) // Adjust vertical position to match XML's avatar placement
            )

            // Name Placeholder
            Text(
                text = "Name Placeholder",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 28.dp) // Adjusted padding to place text below avatar
            )
        }

        // Spacer to replicate LinearLayout's top margin for the first item
        Spacer(modifier = Modifier.height(16.dp))

        // Menu Items Section
        ProfileMenuItem(
            icon = Icons.Default.Edit,
            text = "Edit Profile",
            onClick = { /* TODO: Implement navigation to Edit Profile screen */ }
        )
        ProfileMenuItem(
            icon = Icons.Default.Edit, // Using Edit icon as a generic placeholder for menu items
            text = "Change Password",
            onClick = { /* TODO: Implement navigation to Change Password screen */ }
        )
        ProfileMenuItem(
            icon = Icons.Default.Edit,
            text = "Notification Settings",
            onClick = { /* TODO: Implement navigation to Notification Settings screen */ }
        )
        ProfileMenuItem(
            icon = Icons.Default.Edit,
            text = "About App",
            onClick = { /* TODO: Implement navigation to About App screen */ }
        )
        ProfileMenuItem(
            icon = Icons.Default.Edit,
            text = "Logout",
            onClick = { /* TODO: Implement logout functionality */ }
        )
    }
}

@Composable
fun ProfileMenuItem(icon: ImageVector, text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 12.dp) // Combined horizontal and vertical padding
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null, // Content description can be added if needed
            modifier = Modifier.size(24.dp) // Standard icon size
        )
        Spacer(modifier = Modifier.width(16.dp)) // Space between icon and text
        Text(
            text = text,
            fontSize = 16.sp,
            modifier = Modifier.weight(1f) // Text takes up remaining space
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Forward",
            modifier = Modifier.size(24.dp) // Standard icon size
        )
    }
    // Optional: Add a Divider after each item if you want visual separation like in some lists
    // Divider(modifier = Modifier.padding(horizontal = 32.dp))
}

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    // Anda perlu menyediakan tema untuk pratinjau agar Material Design berfungsi dengan baik
    // Misalnya, jika Anda memiliki tema kustom, gunakan itu.
    // Jika tidak, Anda bisa menggunakan MaterialTheme default atau membuat tema minimal.
    // Contoh penggunaan tema default:
    // MaterialTheme {
    //     ProfileScreen()
    // }
    // Atau jika Anda memiliki tema di proyek Anda:
    // YourAppNameTheme { // Ganti YourAppNameTheme dengan nama tema aplikasi Anda
    ProfileScreen()
    // }
}
