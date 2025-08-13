// app/src/main/java/com/example/myapplication4/ui/profile/ProfileViewModel.kt
package com.example.myapplication4.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication4.data.model.Admin
import com.example.myapplication4.data.repository.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userProfileRepository: UserProfileRepository // Now it's UserProfileRepository, not AdminRepository
) : ViewModel() {

    private val _adminProfile = MutableStateFlow<Admin?>(null)
    val adminProfile: StateFlow<Admin?> = _adminProfile.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Assuming you have a way to get the current adminId, e.g., from SharedPreferences or LoginStateViewModel
    // For demonstration, let's use a placeholder. In a real app, this would come from your auth logic.
    private var currentAdminId: String? = null // You need to set this after successful login

    fun setAdminId(id: String) {
        currentAdminId = id
        fetchProfile()
    }

    fun fetchProfile() {
        currentAdminId?.let { id ->
            _loading.value = true
            _error.value = null
            viewModelScope.launch {
                userProfileRepository.getProfile(id).collectLatest { result ->
                    _loading.value = false
                    result.onSuccess { admin ->
                        _adminProfile.value = admin
                    }.onFailure { throwable ->
                        _error.value = throwable.message
                        _adminProfile.value = null // Clear profile on error
                    }
                }
            }
        } ?: run {
            _error.value = "Admin ID is not set. Cannot fetch profile."
        }
    }

    // You might also want a logout function here
    fun logout() {
        // Clear admin data, token, etc.
        _adminProfile.value = null
        currentAdminId = null
        // Navigate to login screen (handled in UI layer usually)
        // Optionally, disconnect WebSocket if not used for other purposes
        // webSocketAuthService.disconnect() // If you inject it directly here
    }
}