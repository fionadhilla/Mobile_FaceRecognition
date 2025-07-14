package com.example.myapplication4.ui.profile

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ProfileViewModel : ViewModel(){
    private val _userName = MutableStateFlow("Name Placeholder")
    val userName: StateFlow<String> = _userName
}