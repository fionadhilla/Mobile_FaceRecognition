package com.example.myapplication4.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication4.data.model.Admin // Changed from User to Admin
import com.example.myapplication4.domain.usecase.GetUserProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val getUserProfileUseCase: GetUserProfileUseCase
) : ViewModel() {

    private val _userProfile = MutableStateFlow(Admin(0, "Nama Placeholder", "", "", "", java.time.LocalDateTime.now())) // Changed from User to Admin and added default values for Admin
    val userProfile: StateFlow<Admin> = _userProfile.asStateFlow() // Changed from User to Admin

    val userName: StateFlow<String> = userProfile.value.name.let { MutableStateFlow(it) }.asStateFlow() // Changed from fullName to name

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            getUserProfileUseCase().collect { profile ->
                _userProfile.value = profile
            }
        }
    }
}