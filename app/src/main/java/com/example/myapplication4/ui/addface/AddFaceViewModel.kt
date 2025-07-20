package com.example.myapplication4.ui.addface

import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication4.domain.usecase.RegisterUserWithFaceUseCase
import com.example.myapplication4.face.FaceEmbedder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log
import com.example.myapplication4.data.api.ApiResult
import com.example.myapplication4.data.model.User

@HiltViewModel
class AddFaceViewModel @Inject constructor(
    private val registerUserWithFaceUseCase: RegisterUserWithFaceUseCase,
    private val faceEmbedder: FaceEmbedder
) : ViewModel() {
    val name = mutableStateOf("")
    val email = mutableStateOf("")
    val phone = mutableStateOf("")

    fun saveFaceData(faceBitmap: Bitmap?, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (faceBitmap == null) {
            onError("Tidak ada gambar wajah untuk disimpan.")
            return
        }

        if (name.value.isBlank() || email.value.isBlank()) {
            onError("Nama dan Email tidak boleh kosong.")
            return
        }

        if (phone.value.isBlank()) {
            onError("masukkan nomor telepon anda")
            return
        }

        viewModelScope.launch {
            val embeddings = faceEmbedder.getEmbeddings(faceBitmap)
            if (embeddings != null) {
                val user = User(name = name.value, email = email.value, phone = phone.value, embeddings = embeddings)
                when (val result = registerUserWithFaceUseCase(user.name, user.email, user.phone, user.embeddings)) {
                    is ApiResult.Success -> {
                        if (result.data) {
                            onSuccess()
                        } else {
                            onError("Gagal menyimpan data wajah.")
                        }
                    }
                    is ApiResult.Error -> {
                        onError("Error: ${result.message}")
                        Log.e("AddFace", "Error saving face data: ${result.message}", result.exception)
                    }
                    ApiResult.Loading -> { }
                }
            } else {
                onError("Gagal menghasilkan embeddings wajah.")
            }
        }
    }
}