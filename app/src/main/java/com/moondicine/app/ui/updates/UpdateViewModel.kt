package com.moondicine.app.ui.updates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moondicine.app.data.update.UpdateInfo
import com.moondicine.app.data.update.UpdateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UpdateUiState(
    val isChecking: Boolean = false,
    val updateInfo: UpdateInfo? = null,
    val errorMessage: String? = null,
    val isDownloading: Boolean = false,
    val downloadProgress: Int = 0,
    val isDownloaded: Boolean = false,
    val downloadError: String? = null
)

@HiltViewModel
class UpdateViewModel @Inject constructor(
    private val updateRepository: UpdateRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UpdateUiState())
    val uiState: StateFlow<UpdateUiState> = _uiState.asStateFlow()

    init {
        observeDownloadState()
    }

    private fun observeDownloadState() {
        viewModelScope.launch {
            updateRepository.downloadState.collect { state ->
                when (state) {
                    is UpdateRepository.DownloadState.Idle -> {}
                    is UpdateRepository.DownloadState.Downloading -> {
                        _uiState.update { it.copy(isDownloading = true, downloadProgress = state.progress, downloadError = null) }
                    }
                    is UpdateRepository.DownloadState.Downloaded -> {
                        _uiState.update { it.copy(isDownloading = false, isDownloaded = true, downloadProgress = 100) }
                    }
                    is UpdateRepository.DownloadState.Error -> {
                        _uiState.update { it.copy(isDownloading = false, downloadError = state.message) }
                    }
                    is UpdateRepository.DownloadState.Installing -> {
                        _uiState.update { it.copy(isDownloading = false) }
                    }
                }
            }
        }
    }

    fun checkForUpdates() {
        if (_uiState.value.isChecking) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isChecking = true, errorMessage = null)
            val result = updateRepository.checkForUpdates()
            result.onSuccess { updateInfo ->
                _uiState.value = _uiState.value.copy(isChecking = false, updateInfo = updateInfo)
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(isChecking = false, errorMessage = error.message)
            }
        }
    }

    fun downloadUpdate() {
        val url = _uiState.value.updateInfo?.downloadUrl ?: return
        viewModelScope.launch {
            updateRepository.downloadApk(url)
        }
    }

    fun installUpdate() {
        val state = updateRepository.downloadState.value
        if (state is UpdateRepository.DownloadState.Downloaded) {
            updateRepository.installApk(state.file)
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null, downloadError = null)
    }

    fun resetDownload() {
        updateRepository.resetDownloadState()
        _uiState.value = _uiState.value.copy(isDownloading = false, downloadProgress = 0, isDownloaded = false, downloadError = null)
    }
}