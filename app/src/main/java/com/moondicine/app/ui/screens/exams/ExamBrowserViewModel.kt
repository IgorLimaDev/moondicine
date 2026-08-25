package com.moondicine.app.ui.screens.exams

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moondicine.app.data.database.entity.AIExplanationEntity
import com.moondicine.app.data.database.entity.AnswerOptionEntity
import com.moondicine.app.data.database.entity.QuestionEntity
import com.moondicine.app.data.database.entity.UserAnswerEntity
import com.moondicine.app.data.repository.QuestionRepository
import com.moondicine.app.data.repository.UserProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExamQuestionItem(
    val question: QuestionEntity,
    val options: List<AnswerOptionEntity>,
    val latestAnswer: UserAnswerEntity?,
    val explanation: AIExplanationEntity?
) {
    val isAnswered: Boolean get() = latestAnswer != null
}

data class ExamSection(
    val source: String,
    val questions: List<ExamQuestionItem>,
    val isExpanded: Boolean = false
)

data class ExamBrowserUiState(
    val isLoading: Boolean = true,
    val exams: List<ExamSection> = emptyList(),
    val errorMessage: String? = null
)

@HiltViewModel
class ExamBrowserViewModel @Inject constructor(
    private val questionRepository: QuestionRepository,
    private val userProgressRepository: UserProgressRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ExamBrowserUiState())
    val uiState: StateFlow<ExamBrowserUiState> = _uiState.asStateFlow()

    init {
        loadExams()
    }

    fun loadExams() {
        viewModelScope.launch {
            _uiState.value = ExamBrowserUiState(isLoading = true)
            runCatching {
                questionRepository.getExamSources().map { source ->
                    val questions = questionRepository.getQuestionsByExamSource(source).map { question ->
                        ExamQuestionItem(
                            question = question,
                            options = questionRepository.getOptionsForQuestion(question.id),
                            latestAnswer = userProgressRepository.getLatestAnswerForQuestion(question.id),
                            explanation = userProgressRepository.getExplanation(question.id)
                        )
                    }
                    ExamSection(source = source, questions = questions)
                }
            }.onSuccess { exams ->
                _uiState.value = ExamBrowserUiState(isLoading = false, exams = exams)
            }.onFailure { error ->
                _uiState.value = ExamBrowserUiState(
                    isLoading = false,
                    errorMessage = "Não foi possível carregar as provas: ${error.message}"
                )
            }
        }
    }

    fun toggleExam(source: String) {
        _uiState.value = _uiState.value.copy(
            exams = _uiState.value.exams.map { exam ->
                if (exam.source == source) exam.copy(isExpanded = !exam.isExpanded) else exam
            }
        )
    }
}
