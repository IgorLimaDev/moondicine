package com.moondicine.app.ui.screens.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moondicine.app.data.database.entity.AIExplanationEntity
import com.moondicine.app.data.database.entity.AnswerOptionEntity
import com.moondicine.app.data.database.entity.QuestionEntity
import com.moondicine.app.data.database.entity.UserAnswerEntity
import com.moondicine.app.data.repository.QuestionRepository
import com.moondicine.app.data.repository.UserProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReviewItem(
    val question: QuestionEntity,
    val options: List<AnswerOptionEntity>,
    val userAnswer: UserAnswerEntity?,
    val explanation: AIExplanationEntity? = null,
    val errorCount: Int = 0
)

data class ReviewUiState(
    val isLoading: Boolean = true,
    val items: List<ReviewItem> = emptyList(),
    val selectedTab: Int = 0, // 0 = wrong answers, 1 = flagged
    val filterSpecialty: String? = null,
    val expandedQuestionId: Long? = null
)

@HiltViewModel
class ReviewViewModel @Inject constructor(
    private val questionRepository: QuestionRepository,
    private val userProgressRepository: UserProgressRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReviewUiState())
    val uiState: StateFlow<ReviewUiState> = _uiState.asStateFlow()

    init {
        loadWrongAnswers()
    }

    fun selectTab(tab: Int) {
        _uiState.update { it.copy(selectedTab = tab) }
        when (tab) {
            0 -> loadWrongAnswers()
            1 -> loadFlaggedAnswers()
        }
    }

    fun refresh() {
        when (_uiState.value.selectedTab) {
            0 -> loadWrongAnswers()
            1 -> loadFlaggedAnswers()
        }
    }

    fun toggleExpanded(questionId: Long) {
        _uiState.update {
            it.copy(
                expandedQuestionId = if (it.expandedQuestionId == questionId) null else questionId
            )
        }
    }

    private fun loadWrongAnswers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val errorCounts = userProgressRepository.getMostMissedQuestionIds(100)
            val items = errorCounts.mapNotNull { errorCount ->
                val question = questionRepository.getQuestionById(errorCount.questionId) ?: return@mapNotNull null
                val options = questionRepository.getOptionsForQuestion(question.id)
                val userAnswer = userProgressRepository.getLatestAnswerForQuestion(question.id)
                val explanation = userProgressRepository.getExplanation(question.id)

                ReviewItem(
                    question = question,
                    options = options,
                    userAnswer = userAnswer,
                    explanation = explanation,
                    errorCount = errorCount.errorCount
                )
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    items = items
                )
            }
        }
    }

    private fun loadFlaggedAnswers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val flaggedQuestionIds = userProgressRepository.getFlaggedQuestionIds()
            val items = flaggedQuestionIds.mapNotNull { questionId ->
                val question = questionRepository.getQuestionById(questionId) ?: return@mapNotNull null
                val options = questionRepository.getOptionsForQuestion(question.id)
                val userAnswer = userProgressRepository.getLatestAnswerForQuestion(question.id)
                val explanation = userProgressRepository.getExplanation(question.id)

                ReviewItem(
                    question = question,
                    options = options,
                    userAnswer = userAnswer,
                    explanation = explanation
                )
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    items = items
                )
            }
        }
    }

    fun filterBySpecialty(specialty: String?) {
        _uiState.update { it.copy(filterSpecialty = specialty) }
    }
}
