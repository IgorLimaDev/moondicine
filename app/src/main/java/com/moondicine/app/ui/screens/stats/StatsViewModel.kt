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
    val specialtyStats: List<UserStatsEntity> = emptyList()
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
            val totalQ = questionRepository.getQuestionCount()
            val totalA = userProgressRepository.getTotalAnswered()
            val totalC = userProgressRepository.getTotalCorrect()
            val totalW = userProgressRepository.getTotalWrong()
            val accuracy = if (totalA > 0) totalC.toFloat() / totalA.toFloat() else 0f
            val stats = userProgressRepository.getAllStats()

            _uiState.update {
                it.copy(
                    isLoading = false,
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

    fun refresh() {
        viewModelScope.launch {
            val totalQ = questionRepository.getQuestionCount()
            val totalA = userProgressRepository.getTotalAnswered()
            val totalC = userProgressRepository.getTotalCorrect()
            val totalW = userProgressRepository.getTotalWrong()
            val accuracy = if (totalA > 0) totalC.toFloat() / totalA.toFloat() else 0f
            val stats = userProgressRepository.getAllStats()

            _uiState.update {
                it.copy(
                    isLoading = false,
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
}
