package com.example.myapplication4.ui.camera
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.camera.core.CameraSelector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CameraViewModel : ViewModel() {
    private val _lensFacing = MutableStateFlow(CameraSelector.LENS_FACING_BACK)
    val lensFacing: StateFlow<Int> = _lensFacing

    fun switchCamera() {
        viewModelScope.launch {
            val newLens = if (_lensFacing.value == CameraSelector.LENS_FACING_BACK)
                CameraSelector.LENS_FACING_FRONT
            else
                CameraSelector.LENS_FACING_BACK

            _lensFacing.emit(newLens)
        }
    }
}
