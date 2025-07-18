package com.example.myapplication4.ui.edit_profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication4.data.model.UserProfile
import com.example.myapplication4.domain.usecase.GetUserProfileUseCase
import com.example.myapplication4.domain.usecase.UpdateUserProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val updateUserProfileUseCase: UpdateUserProfileUseCase
) : ViewModel() {

    private val _fullName = MutableStateFlow("")
    val fullName = _fullName.asStateFlow()

    private val _email = MutableStateFlow("")
    val email = _email.asStateFlow()

    private val _phone = MutableStateFlow("")
    val phone = _phone.asStateFlow()

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            getUserProfileUseCase().collect { profile ->
                _fullName.value = profile.name
                _email.value = profile.email
            }
        }
    }

    fun onFullNameChange(newValue: String) {
        _fullName.value = newValue
    }

    fun onEmailChange(newValue: String) {
        _email.value = newValue
    }

    fun onPhoneChange(newValue: String) {
        _phone.value = newValue
    }

    fun saveChanges(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val updatedProfile = UserProfile(
                name = _fullName.value,
                email = _email.value,
                phoneNumber = _phone.value
            )
            val success = updateUserProfileUseCase(updatedProfile)
            if (success) {
                onSuccess()
            }
        }
    }
}