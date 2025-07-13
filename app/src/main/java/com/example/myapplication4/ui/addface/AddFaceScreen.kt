package com.example.myapplication4.ui.addface

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material.icons.filled.Person

@Composable
fun AddFaceScreen(
    viewModel: AddFaceViewModel = viewModel(),
    onBack: () -> Unit,
    onRetakePhoto: () -> Unit,
    onSave: (String, String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ){
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    // Top Bar
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier
                                .size(28.dp)
                                .clickable { onBack() }
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Tambah Wajah",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Wajah Placeholder
                    Box(
                        modifier = Modifier
                            .width(250.dp)
                            .aspectRatio(1f)
                            .background(Color.LightGray)
                            .align(alignment = Alignment.CenterHorizontally),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Wajah",
                            tint = Color.DarkGray,
                            modifier = Modifier.size(55.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Foto Ulang Button
                    TextButton(
                        onClick = onRetakePhoto,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("Foto Ulang")
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Input Nama
                    OutlinedTextField(
                        value = viewModel.name.value,
                        onValueChange = { viewModel.name.value = it },
                        label = { Text("Nama") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Input Email
                    OutlinedTextField(
                        value = viewModel.email.value,
                        onValueChange = { viewModel.email.value = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Simpan Button
                    Button(
                        onClick = {
                            onSave(viewModel.name.value, viewModel.email.value)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                            .height(48.dp),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("Simpan Wajah")
                    }
                }
            }

}
