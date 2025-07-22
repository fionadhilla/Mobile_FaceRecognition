package com.example.myapplication4.ui.edit_profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication4.data.model.Admin // Changed from User to Admin
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

    // private val _phone = MutableStateFlow("") // Removed phone
    // val phone = _phone.asStateFlow() // Removed phone

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            getUserProfileUseCase().collect { profile ->
                _fullName.value = profile.name // Changed from fullName to name
                _email.value = profile.email
                // _phone.value = profile.phoneNumber // Removed phone
            }
        }
    }

    fun onFullNameChange(newValue: String) {
        _fullName.value = newValue
    }

    fun onEmailChange(newValue: String) {
        _email.value = newValue
    }

    // fun onPhoneChange(newValue: String) { // Removed phone
    //     _phone.value = newValue
    // }

    fun saveChanges(onSuccess: () -> Unit) {
        viewModelScope.launch {
            // Need to retrieve current admin data to preserve id, password, role, createdAt
            val currentProfile = getUserProfileUseCase().value
            val updatedProfile = Admin(
                id = currentProfile.id,
                name = _fullName.value, // Changed from fullName to name
                password = currentProfile.password, // Preserve existing password
                email = _email.value,
                role = currentProfile.role, // Preserve existing role
                createdAt = currentProfile.createdAt // Preserve existing createdAt
            )
            val success = updateUserProfileUseCase(updatedProfile)
            if (success) {
                onSuccess() // Panggil callback jika penyimpanan berhasil
            }
        }
    }
}