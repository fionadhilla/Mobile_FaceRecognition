// app/src/main/java/com/example/myapplication4/ui/edit_profile/EditProfileScreen.kt
package com.example.myapplication4.ui.edit_profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myapplication4.ui.components.TopBar
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onBackClick: () -> Unit,
    viewModel: EditProfileViewModel = hiltViewModel()
) {
    // Mengubah fullName menjadi name
    val name by viewModel.name.collectAsState() // Menggunakan viewModel.name
    val email by viewModel.email.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val updateSuccess by viewModel.updateSuccess.collectAsState()
    val error by viewModel.error.collectAsState()

    Scaffold(
        topBar = {
            TopBar(
                title = "Edit Profil",
                onBackClick = onBackClick
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // TextField untuk Nama Lengkap
            OutlinedTextField(
                value = name, // Menggunakan 'name'
                onValueChange = { viewModel.onNameChange(it) }, // Menggunakan onNameChange
                label = { Text("Nama Lengkap") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // TextField untuk Email
            OutlinedTextField(
                value = email,
                onValueChange = { viewModel.onEmailChange(it) },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Button(
                onClick = { viewModel.updateProfile() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !loading
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Simpan Perubahan")
                }
            }

            updateSuccess?.let { success ->
                if (success) {
                    Text("Profil berhasil diperbarui!", color = MaterialTheme.colorScheme.primary)
                } else {
                    Text("Gagal memperbarui profil.", color = MaterialTheme.colorScheme.error)
                }
            }

            error?.let { errorMessage ->
                Text("Error: $errorMessage", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}