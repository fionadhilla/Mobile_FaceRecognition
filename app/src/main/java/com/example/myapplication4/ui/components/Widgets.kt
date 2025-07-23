package com.example.myapplication4.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview

// Definisikan Composable TopBar di sini
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    title: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primary, // Gunakan warna tema primer
    titleContentColor: Color = MaterialTheme.colorScheme.onPrimary // Gunakan warna 'onPrimary' untuk teks
) {
    TopAppBar(
        title = { Text(text = title, color = titleContentColor) },
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Kembali",
                    tint = titleContentColor // Gunakan warna yang sama untuk ikon
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = containerColor // Atur warna latar belakang AppBar
        )
    )
}

// Anda bisa menambahkan Composable Widgets lainnya di sini jika diperlukan
// class Widgets { } // Kelas ini tidak lagi diperlukan jika Anda hanya memiliki fungsi Composable level-atas

@Preview(showBackground = true)
@Composable
fun TopBarPreview() {
    MaterialTheme {
        TopBar(title = "Contoh Judul", onBackClick = {})
    }
}