package com.moondicine.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moondicine.app.data.repository.QuestionRepository
import com.moondicine.app.data.repository.SupabaseSyncRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val isResetting: Boolean = false,
    val resetMessage: String? = null,
    val showResetConfirm: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val questionRepository: QuestionRepository,
    private val supabaseSyncRepository: SupabaseSyncRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun showResetConfirmDialog() {
        _uiState.update { it.copy(showResetConfirm = true) }
    }

    fun dismissResetConfirmDialog() {
        _uiState.update { it.copy(showResetConfirm = false) }
    }

    fun resetDatabase() {
        if (_uiState.value.isResetting) return

        viewModelScope.launch {
            _uiState.update { it.copy(isResetting = true, resetMessage = null, showResetConfirm = false) }
            try {
                // Clear all questions (cascades to options, explanations, notes, answers)
                questionRepository.deleteAll()

                // Re-sync from Supabase
                val result = supabaseSyncRepository.syncQuestionBank()

                _uiState.update {
                    it.copy(
                        isResetting = false,
                        resetMessage = result.fold(
                            onSuccess = { count -> "Banco resetado! $count questões baixadas do Supabase." },
                            onFailure = { error ->
                                "Banco limpo, mas falha ao sincronizar: ${error.message}"
                            }
                        )
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isResetting = false,
                        resetMessage = "Erro ao resetar banco: ${e.message}"
                    )
                }
            }
        }
    }
}
