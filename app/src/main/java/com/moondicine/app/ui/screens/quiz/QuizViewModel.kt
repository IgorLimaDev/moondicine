package com.moondicine.app.ui.screens.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moondicine.app.ai.CohereService
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
    private val userProgressRepository: UserProgressRepository,
    private val cohereService: CohereService
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
                    val examQuestions = questionRepository.getQuestionsByExamSource(examSource)
                    if (isInfiniteMode) examQuestions else if (questionCount > 0) examQuestions.take(questionCount) else examQuestions
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

                // Load options for each question
                val quizQuestions = questions.map { question ->
                    val options = questionRepository.getOptionsForQuestion(question.id)
                    QuizQuestion(
                        question = question,
                        options = options
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

            // Check cache first
            val cached = userProgressRepository.getExplanation(quizQuestion.question.id)
            if (cached != null) {
                _uiState.update {
                    it.copy(
                        explanation = cached,
                        isExplanationLoading = false
                    )
                }
                return@launch
            }

            // Generate new explanation
            val correctOption = quizQuestion.options.find { it.isCorrect }
            if (correctOption == null) {
                _uiState.update { it.copy(isExplanationLoading = false) }
                return@launch
            }

            val optionsMap = quizQuestion.options.associate { it.optionLetter to it.optionText }

            val result = cohereService.generateExplanation(
                questionText = quizQuestion.question.questionText,
                options = optionsMap,
                correctAnswer = correctOption.optionLetter,
                userAnswer = userAnswerLetter
            )

            result.onSuccess { response ->
                val explanation = AIExplanationEntity(
                    questionId = quizQuestion.question.id,
                    explanationText = response.correctReasoning,
                    correctReasoning = response.correctReasoning,
                    wrongReasoning = com.google.gson.Gson().toJson(response.wrongReasoning)
                )
                userProgressRepository.cacheExplanation(explanation)
                _uiState.update {
                    it.copy(
                        explanation = explanation,
                        isExplanationLoading = false
                    )
                }
            }.onFailure {
                _uiState.update { it.copy(isExplanationLoading = false) }
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
            _uiState.update { it.copy(isQuizFinished = true) }
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

        // Persist flag to database if the question has been answered
        if (current.userAnswer != null) {
            viewModelScope.launch {
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
        _uiState.update { it.copy(isQuizFinished = true) }
    }
}
