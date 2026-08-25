package com.moondicine.app.ui.updates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moondicine.app.data.update.UpdateInfo
import com.moondicine.app.data.update.UpdateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UpdateUiState(
    val isChecking: Boolean = false,
    val updateInfo: UpdateInfo? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class UpdateViewModel @Inject constructor(
    private val updateRepository: UpdateRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(UpdateUiState())
    val uiState: StateFlow<UpdateUiState> = _uiState.asStateFlow()
    
    fun checkForUpdates() {
        if (_uiState.value.isChecking) return
        
        viewModelScope.launch {
            _uiState.value = UpdateUiState(isChecking = true)
            val result = updateRepository.checkForUpdates()
            
            result.onSuccess { updateInfo ->
                _uiState.value = UpdateUiState(
                    isChecking = false,
                    updateInfo = updateInfo
                )
            }.onFailure { error ->
                _uiState.value = UpdateUiState(
                    isChecking = false,
                    errorMessage = error.message
                )
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}