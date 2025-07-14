package com.example.myapplication4.ui.addface

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class AddFaceViewModel : ViewModel() {
    val name = mutableStateOf("")
    val email = mutableStateOf("")
}