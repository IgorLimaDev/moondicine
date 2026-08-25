package com.moondicine.app.ui.screens.upload

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moondicine.app.ai.CohereService
import com.moondicine.app.ai.models.ParsedQuestion
import com.moondicine.app.data.database.entity.AnswerOptionEntity
import com.moondicine.app.data.database.entity.QuestionEntity
import com.moondicine.app.data.pdf.PdfTextExtractor
import com.moondicine.app.data.repository.QuestionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class UploadState {
    IDLE,
    SELECTING,
    EXTRACTING_TEXT,
    ANALYZING_AI,
    PARSING_QUESTIONS,
    STORING,
    COMPLETE,
    ERROR
}

data class UploadUiState(
    val state: UploadState = UploadState.IDLE,
    val progress: Float = 0f,
    val statusMessage: String = "",
    val questionsFileUri: Uri? = null,
    val answersFileUri: Uri? = null,
    val isDualMode: Boolean = false,
    val extractedQuestionsCount: Int = 0,
    val errorMessage: String? = null
)

@HiltViewModel
class UploadViewModel @Inject constructor(
    private val pdfTextExtractor: PdfTextExtractor,
    private val cohereService: CohereService,
    private val questionRepository: QuestionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UploadUiState())
    val uiState: StateFlow<UploadUiState> = _uiState.asStateFlow()

    fun setQuestionsFile(uri: Uri) {
        _uiState.update { it.copy(questionsFileUri = uri) }
    }

    fun setAnswersFile(uri: Uri) {
        _uiState.update { it.copy(answersFileUri = uri) }
    }

    fun setDualMode(enabled: Boolean) {
        _uiState.update { it.copy(isDualMode = enabled) }
    }

    fun startProcessing() {
        val state = _uiState.value
        if (state.questionsFileUri == null) return

        viewModelScope.launch {
            try {
                // Step 1: Extract text from PDF
                _uiState.update {
                    it.copy(
                        state = UploadState.EXTRACTING_TEXT,
                        progress = 0.1f,
                        statusMessage = "Extraindo texto do PDF..."
                    )
                }

                val extractResult = pdfTextExtractor.extractText(state.questionsFileUri)
                val questionsText = extractResult.getOrElse { e ->
                    _uiState.update {
                        it.copy(
                            state = UploadState.ERROR,
                            errorMessage = "Não foi possível extrair o texto: ${e.message}"
                        )
                    }
                    return@launch
                }

                // Also extract answers text if dual mode
                val answersText = if (state.isDualMode && state.answersFileUri != null) {
                    _uiState.update {
                        it.copy(
                            progress = 0.2f,
                            statusMessage = "Extraindo texto do gabarito..."
                        )
                    }
                    val answerResult = pdfTextExtractor.extractText(state.answersFileUri)
                    answerResult.getOrNull()
                } else null

                // Step 2: Send to AI for parsing
                _uiState.update {
                    it.copy(
                        state = UploadState.ANALYZING_AI,
                        progress = 0.4f,
                        statusMessage = "A IA está analisando o conteúdo da prova..."
                    )
                }

                val combinedText = if (answersText != null) {
                    "$questionsText\n\n--- ANSWER KEY ---\n\n$answersText"
                } else {
                    questionsText
                }

                val parseResult = cohereService.parseQuestionsFromText(combinedText)
                val parsedQuestions = parseResult.getOrElse { e ->
                    _uiState.update {
                        it.copy(
                            state = UploadState.ERROR,
                            errorMessage = "A análise pela IA falhou: ${e.message}"
                        )
                    }
                    return@launch
                }

                if (parsedQuestions.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            state = UploadState.ERROR,
                            errorMessage = "Nenhuma questão foi encontrada no PDF. Verifique o arquivo."
                        )
                    }
                    return@launch
                }

                // Step 3: Store in database
                _uiState.update {
                    it.copy(
                        state = UploadState.STORING,
                        progress = 0.7f,
                        statusMessage = "Salvando ${parsedQuestions.size} questões no banco local..."
                    )
                }

                val examSource = extractExamSource(questionsText)
                storeQuestions(parsedQuestions, examSource)

                // Done
                _uiState.update {
                    it.copy(
                        state = UploadState.COMPLETE,
                        progress = 1.0f,
                        statusMessage = "${parsedQuestions.size} questões importadas com sucesso!",
                        extractedQuestionsCount = parsedQuestions.size
                    )
                }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        state = UploadState.ERROR,
                        errorMessage = "Erro inesperado: ${e.message}"
                    )
                }
            }
        }
    }

    private fun extractExamSource(text: String): String {
        // Try to identify the exam source from the text
        val patterns = listOf(
            Regex("(ENAM|ANCAR|VINCI|REVALIDA)\\s*\\d{4}", RegexOption.IGNORE_CASE),
            Regex("Exame\\s+Nacional.*?\\d{4}", RegexOption.IGNORE_CASE),
            Regex("Prova.*?\\d{4}", RegexOption.IGNORE_CASE)
        )

        for (pattern in patterns) {
            val match = pattern.find(text)
            if (match != null) return match.value.trim()
        }

        return "Prova importada"
    }

    private suspend fun storeQuestions(parsedQuestions: List<ParsedQuestion>, examSource: String) {
        for ((index, parsed) in parsedQuestions.withIndex()) {
            val progress = 0.7f + (index.toFloat() / parsedQuestions.size.toFloat()) * 0.25f
            _uiState.update {
                it.copy(
                    progress = progress,
                    statusMessage = "Salvando questão ${index + 1} de ${parsedQuestions.size}..."
                )
            }

            val question = QuestionEntity(
                examSource = examSource,
                questionNumber = parsed.questionNumber,
                questionText = parsed.questionText,
                specialty = parsed.specialty,
                subTopic = parsed.subTopic,
                difficulty = parsed.difficulty.coerceIn(1, 5)
            )

            val options = parsed.options.map { option ->
                AnswerOptionEntity(
                    questionId = 0, // Will be set by repository
                    optionLetter = option.letter,
                    optionText = option.text,
                    isCorrect = option.letter == parsed.correctAnswer
                )
            }

            questionRepository.insertQuestionWithOptions(question, options)
        }
    }

    fun reset() {
        _uiState.value = UploadUiState()
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null, state = UploadState.IDLE) }
    }
}
