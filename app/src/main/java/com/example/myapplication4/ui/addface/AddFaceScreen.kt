package com.example.myapplication4.ui.addface

import android.util.Log
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
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.asImageBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myapplication4.ui.camera.CameraViewModel
import androidx.compose.foundation.Image
import android.net.Uri
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import java.io.File
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@Composable
fun AddFaceScreen(
    viewModel: AddFaceViewModel = hiltViewModel(),
    initialImageUri: Uri?,
    onBack: () -> Unit,
    onRetakePhoto: () -> Unit,
    onSave: () -> Unit // Ubah parameter onSave
) {
    val context = LocalContext.current
    var loadedFaceBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoadingBitmap by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()


    LaunchedEffect(initialImageUri) {
        if (initialImageUri != null) {
            isLoadingBitmap = true
            try {
                val inputStream = context.contentResolver.openInputStream(initialImageUri)
                loadedFaceBitmap = inputStream?.use {
                    BitmapFactory.decodeStream(it)
                }
                Log.d("AddFaceScreen", "Bitmap loaded from URI: ${loadedFaceBitmap != null}")
            } catch (e: Exception) {
                Log.e("AddFaceScreen", "Failed to load bitmap from URI: ${e.message}", e)
                loadedFaceBitmap = null
            } finally {
                isLoadingBitmap = false
            }
        } else {
            loadedFaceBitmap = null
        }
    }

    DisposableEffect(initialImageUri) {
        onDispose {
            // Cleanup temporary file when screen is disposed or URI changes
            initialImageUri?.path?.let { path ->
                val file = File(path)
                if (file.exists()) {
                    file.delete()
                    Log.d("AddFaceScreen", "Temporary cropped face file deleted: $path")
                }
            }
            loadedFaceBitmap?.recycle()
            loadedFaceBitmap = null
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(innerPadding) // Terapkan padding dari Scaffold
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
                            .clickable {
                                onBack()
                            }
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Tambah Wajah",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    modifier = Modifier
                        .width(250.dp)
                        .aspectRatio(1f)
                        .background(Color.LightGray)
                        .align(alignment = Alignment.CenterHorizontally),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoadingBitmap) {
                        CircularProgressIndicator()
                    } else if (loadedFaceBitmap != null) {
                        Image(
                            bitmap = loadedFaceBitmap!!.asImageBitmap(),
                            contentDescription = "Cropped Face",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Wajah",
                            tint = Color.DarkGray,
                            modifier = Modifier.size(70.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Foto Ulang Button
                TextButton(
                    onClick = {
                        onRetakePhoto()
                    },
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
                        viewModel.saveFaceData(
                            faceBitmap = loadedFaceBitmap,
                            onSuccess = {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Wajah berhasil disimpan")
                                }
                                onSave()
                            },
                            onError = { message ->
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Error: $message")
                                }
                            }
                        )
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
}