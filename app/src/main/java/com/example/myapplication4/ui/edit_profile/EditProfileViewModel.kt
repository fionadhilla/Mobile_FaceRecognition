package com.example.myapplication4.ui.edit_profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication4.data.model.User
import com.example.myapplication4.domain.usecase.GetUserProfileUseCase
import com.example.myapplication4.domain.usecase.UpdateUserProfileUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
                _fullName.value = profile.fullName
                _email.value = profile.email
                _phone.value = profile.phoneNumber
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
            val updatedProfile = User(
                fullName = _fullName.value,
                email = _email.value,
                phoneNumber = _phone.value
            )
            val success = updateUserProfileUseCase(updatedProfile)
            if (success) {
                onSuccess() // Panggil callback jika penyimpanan berhasil
            }
        }
    }
}