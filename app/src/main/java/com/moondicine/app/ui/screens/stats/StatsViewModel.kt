package com.moondicine.app.ui.screens.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moondicine.app.data.database.entity.UserStatsEntity
import com.moondicine.app.data.repository.QuestionRepository
import com.moondicine.app.data.repository.UserProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StatsUiState(
    val isLoading: Boolean = true,
    val totalQuestions: Int = 0,
    val totalAnswered: Int = 0,
    val totalCorrect: Int = 0,
    val totalWrong: Int = 0,
    val accuracy: Float = 0f,
    val specialtyStats: List<UserStatsEntity> = emptyList(),
    val errorMessage: String? = null
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val questionRepository: QuestionRepository,
    private val userProgressRepository: UserProgressRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init {
        loadStats()
    }

    private fun loadStats() {
        viewModelScope.launch {
            try {
                refreshData()
                _uiState.update { it.copy(isLoading = false, errorMessage = null) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Erro ao carregar estatísticas: ${e.message}")
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            try {
                refreshData()
                _uiState.update { it.copy(errorMessage = null) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "Erro ao atualizar estatísticas: ${e.message}")
                }
            }
        }
    }

    private suspend fun refreshData() {
        val totalQ = questionRepository.getQuestionCount()
        val totalA = userProgressRepository.getTotalAnswered()
        val totalC = userProgressRepository.getTotalCorrect()
        val totalW = userProgressRepository.getTotalWrong()
        val accuracy = if (totalA > 0) totalC.toFloat() / totalA.toFloat() else 0f
        val stats = userProgressRepository.getAllStats()

        _uiState.update {
            it.copy(
                totalQuestions = totalQ,
                totalAnswered = totalA,
                totalCorrect = totalC,
                totalWrong = totalW,
                accuracy = accuracy,
                specialtyStats = stats
            )
        }
    }
}
