package com.moondicine.app.ui.screens.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moondicine.app.data.database.entity.*
import com.moondicine.app.data.database.dao.QuestionErrorCount
import com.moondicine.app.data.repository.QuestionRepository
import com.moondicine.app.data.repository.UserProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class QuizQuestion(
    val question: QuestionEntity,
    val options: List<AnswerOptionEntity>,
    val userAnswer: UserAnswerEntity? = null,
    val explanation: AIExplanationEntity? = null,
    val isAnswered: Boolean = false,
    val isFlagged: Boolean = false
)

data class QuizUiState(
    val isLoading: Boolean = true,
    val questions: List<QuizQuestion> = emptyList(),
    val currentIndex: Int = 0,
    val selectedOptionId: Long? = null,
    val isAnswerRevealed: Boolean = false,
    val isExplanationLoading: Boolean = false,
    val explanation: AIExplanationEntity? = null,
    val correctCount: Int = 0,
    val wrongCount: Int = 0,
    val timeSpentSeconds: Int = 0,
    val isQuizFinished: Boolean = false,
    val errorMessage: String? = null,
    val showNoteDialog: Boolean = false,
    val quizMode: String = "teste",
    val isInfinitoMode: Boolean = false
) {
    val currentQuestion: QuizQuestion?
        get() = questions.getOrNull(currentIndex)

    val totalQuestions: Int
        get() = questions.size

    val progress: Float
        get() = if (questions.isEmpty()) 0f else currentIndex.toFloat() / questions.size.toFloat()

    val answeredCount: Int
        get() = questions.count { it.isAnswered }
}

@HiltViewModel
class QuizViewModel @Inject constructor(
    private val questionRepository: QuestionRepository,
    private val userProgressRepository: UserProgressRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuizUiState())
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    private var startTime = 0L
    private var timerJob: kotlinx.coroutines.Job? = null

    fun loadQuestions(quizType: String, specialtyFilter: String, examSource: String, questionCount: Int, quizMode: String = "teste") {
        val isInfiniteMode = quizMode == "infinito"
        viewModelScope.launch {
            try {
                val questions = if (examSource != "all") {
                    val examQuestions = if (isInfiniteMode) {
                        questionRepository.getQuestionsByExamSource(examSource)
                    } else {
                        val unanswered = questionRepository.getUnansweredByExamSource(examSource)
                        if (questionCount > 0) unanswered.shuffled().take(questionCount) else unanswered
                    }
                    examQuestions
                } else {
                    when (quizType) {
                        "quick" -> {
                            if (isInfiniteMode) {
                                questionRepository.getAllAvailableQuestions()
                            } else {
                                questionRepository.getUnansweredQuestions(questionCount)
                            }
                        }
                        "specialty" -> {
                            if (specialtyFilter != "all") {
                                if (isInfiniteMode) {
                                    questionRepository.getAllBySpecialty(specialtyFilter)
                                } else {
                                    questionRepository.getUnansweredBySpecialty(specialtyFilter, questionCount)
                                }
                            } else {
                                if (isInfiniteMode) {
                                    questionRepository.getAllAvailableQuestions()
                                } else {
                                    questionRepository.getUnansweredQuestions(questionCount)
                                }
                            }
                        }
                        "weak" -> {
                            val missedIds = userProgressRepository.getMostMissedQuestionIds(questionCount)
                            missedIds.mapNotNull { questionRepository.getQuestionById(it.questionId) }
                        }
                        "exam" -> {
                            if (isInfiniteMode) {
                                questionRepository.getAllAvailableQuestions()
                            } else {
                                questionRepository.getUnansweredQuestions(questionCount)
                            }
                        }
                        else -> questionRepository.getUnansweredQuestions(questionCount)
                    }
                }

                if (questions.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Nenhuma questão disponível. Envie um PDF de prova primeiro!"
                        )
                    }
                    return@launch
                }

                // Load options and flags for each question
                val flaggedIds = userProgressRepository.getFlaggedQuestionIds().toSet()
                val quizQuestions = questions.map { question ->
                    val options = questionRepository.getOptionsForQuestion(question.id)
                    QuizQuestion(
                        question = question,
                        options = options,
                        isFlagged = question.id in flaggedIds
                    )
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        questions = quizQuestions,
                        quizMode = quizMode,
                        isInfinitoMode = isInfiniteMode
                    )
                }

                // Start timer
                startTimer()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Não foi possível carregar as questões: ${e.message}"
                    )
                }
            }
        }
    }

    fun selectOption(optionId: Long) {
        if (_uiState.value.isAnswerRevealed) return
        _uiState.update { it.copy(selectedOptionId = optionId) }
    }

    fun confirmAnswer() {
        val state = _uiState.value
        val current = state.currentQuestion ?: return
        val selectedId = state.selectedOptionId ?: return

        viewModelScope.launch {
            try {
                val selectedOption = current.options.find { it.id == selectedId }
                val isCorrect = selectedOption?.isCorrect == true
                val timeSpent = state.timeSpentSeconds

            // Save user answer
            val answer = UserAnswerEntity(
                questionId = current.question.id,
                selectedOptionId = selectedId,
                isCorrect = isCorrect,
                timeSpentSeconds = timeSpent,
                isFlagged = current.isFlagged
            )
                val answerId = userProgressRepository.recordAnswer(answer, current.question.specialty)

            // Update question list
            val updatedQuestions = state.questions.toMutableList()
            updatedQuestions[state.currentIndex] = current.copy(
                userAnswer = answer.copy(id = answerId),
                isAnswered = true
            )

                _uiState.update {
                    it.copy(
                        questions = updatedQuestions,
                        isAnswerRevealed = true,
                        correctCount = state.correctCount + if (isCorrect) 1 else 0,
                        wrongCount = state.wrongCount + if (!isCorrect) 1 else 0
                    )
                }

                // Load explanation
                loadExplanation(current, selectedOption?.optionLetter)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "Não foi possível salvar seu progresso: ${e.message}")
                }
            }
        }
    }

    private fun loadExplanation(quizQuestion: QuizQuestion, userAnswerLetter: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExplanationLoading = true) }

            val explanation = userProgressRepository.getExplanation(quizQuestion.question.id)
            _uiState.update {
                it.copy(
                    explanation = explanation,
                    isExplanationLoading = false
                )
            }
        }
    }

    fun nextQuestion() {
        val state = _uiState.value
        if (state.currentIndex < state.questions.size - 1) {
            _uiState.update {
                it.copy(
                    currentIndex = it.currentIndex + 1,
                    selectedOptionId = null,
                    isAnswerRevealed = false,
                    explanation = null,
                    isExplanationLoading = false
                )
            }
        } else {
            // Quiz finished
            timerJob?.cancel()
            viewModelScope.launch {
                updateStreak()
                _uiState.update { it.copy(isQuizFinished = true) }
            }
        }
    }

    fun previousQuestion() {
        val state = _uiState.value
        if (state.currentIndex > 0) {
            _uiState.update {
                it.copy(
                    currentIndex = it.currentIndex - 1,
                    selectedOptionId = null,
                    isAnswerRevealed = false,
                    explanation = null,
                    isExplanationLoading = false
                )
            }
        }
    }

    fun toggleFlag() {
        val state = _uiState.value
        val current = state.currentQuestion ?: return
        val newFlagState = !current.isFlagged
        val updatedQuestions = state.questions.toMutableList()
        updatedQuestions[state.currentIndex] = current.copy(isFlagged = newFlagState)
        _uiState.update { it.copy(questions = updatedQuestions) }

        // Always persist flag to database
        viewModelScope.launch {
            userProgressRepository.toggleQuestionFlag(current.question.id)
            // Also update user_answers if already answered
            if (current.userAnswer != null) {
                userProgressRepository.updateFlag(current.question.id, newFlagState)
            }
        }
    }

    fun showNoteDialog() {
        _uiState.update { it.copy(showNoteDialog = true) }
    }

    fun hideNoteDialog() {
        _uiState.update { it.copy(showNoteDialog = false) }
    }

    fun saveNote(noteText: String) {
        val current = _uiState.value.currentQuestion ?: return
        viewModelScope.launch {
            userProgressRepository.saveNote(
                QuestionNoteEntity(
                    questionId = current.question.id,
                    noteText = noteText
                )
            )
        }
    }

    private fun startTimer() {
        startTime = System.currentTimeMillis()
        timerJob = viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(1000)
                val elapsed = ((System.currentTimeMillis() - startTime) / 1000).toInt()
                _uiState.update { it.copy(timeSpentSeconds = elapsed) }
            }
        }
    }

    fun finishQuiz() {
        timerJob?.cancel()
        viewModelScope.launch {
            updateStreak()
            _uiState.update { it.copy(isQuizFinished = true) }
        }
    }

    private suspend fun updateStreak() {
        try {
            val profile = userProgressRepository.getUserProfile()
                ?: UserProfileEntity(id = 1, onboardingCompleted = true)
            val now = System.currentTimeMillis()
            val lastDate = profile.lastStudyDate
            val dayMs = 24 * 60 * 60 * 1000L

            val newStreak = when {
                lastDate == 0L -> 1
                // Same day — don't increment, keep current streak
                (now / dayMs) == (lastDate / dayMs) -> profile.currentStreakDays.coerceAtLeast(1)
                // Next consecutive day
                (now / dayMs) - (lastDate / dayMs) == 1L -> profile.currentStreakDays + 1
                // Streak broken
                else -> 1
            }

            userProgressRepository.saveUserProfile(
                profile.copy(
                    currentStreakDays = newStreak,
                    lastStudyDate = now
                )
            )
        } catch (_: Exception) { }
    }
}
