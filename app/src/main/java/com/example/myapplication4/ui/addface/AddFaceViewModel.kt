package com.example.myapplication4.ui.addface

import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication4.domain.usecase.RegisterUserWithFaceUseCase // Ubah impor
import com.example.myapplication4.face.FaceEmbedder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddFaceViewModel @Inject constructor(
    private val registerUserWithFaceUseCase: RegisterUserWithFaceUseCase, // Ubah nama variabel
    private val faceEmbedder: FaceEmbedder
) : ViewModel() {
    val name = mutableStateOf("")
    val email = mutableStateOf("")

    fun saveFaceData(faceBitmap: Bitmap?, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (faceBitmap == null) {
            onError("Tidak ada gambar wajah untuk disimpan.")
            return
        }

        if (name.value.isBlank() || email.value.isBlank()) {
            onError("Nama dan Email tidak boleh kosong.")
            return
        }

        viewModelScope.launch {
            val embeddings = faceEmbedder.getEmbeddings(faceBitmap)
            if (embeddings != null) {
                // Panggil use case yang diperbarui
                val success = registerUserWithFaceUseCase(name.value, email.value, embeddings)
                if (success) {
                    onSuccess()
                } else {
                    onError("Gagal menyimpan data wajah.")
                }
            } else {
                onError("Gagal menghasilkan embeddings wajah.")
            }
        }
    }
}