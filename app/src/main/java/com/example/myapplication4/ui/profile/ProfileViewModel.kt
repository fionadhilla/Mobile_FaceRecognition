package com.example.myapplication4.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication4.data.model.User
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

    private val _userProfile = MutableStateFlow(User("", "", "")) // Nilai awal kosong
    val userProfile: StateFlow<User> = _userProfile.asStateFlow()

    val userName: StateFlow<String> = userProfile.value.fullName.let { MutableStateFlow(it) }.asStateFlow()

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