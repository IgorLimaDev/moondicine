package com.moondicine.app.ui.screens.quiz

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.moondicine.app.data.database.entity.AnswerOptionEntity
import com.moondicine.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    quizType: String,
    specialtyFilter: String,
    examSource: String,
    questionCount: Int,
    onQuizFinished: (total: Int, correct: Int, time: Int) -> Unit,
    onBackClick: () -> Unit,
    viewModel: QuizViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showExitDialog by remember { mutableStateOf(false) }

    val answeredCount = uiState.questions.count { it.isAnswered }

    LaunchedEffect(quizType, specialtyFilter, examSource, questionCount) {
        viewModel.loadQuestions(quizType, specialtyFilter, examSource, questionCount)
    }

    LaunchedEffect(uiState.isQuizFinished) {
        if (uiState.isQuizFinished) {
            onQuizFinished(
                uiState.totalQuestions,
                uiState.correctCount,
                uiState.timeSpentSeconds
            )
        }
    }

    // Exit confirmation dialog
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Sair do quiz?") },
            text = {
                Text(
                    if (answeredCount > 0)
                        "Você respondeu $answeredCount de ${uiState.totalQuestions} questões. Seu progresso será salvo."
                    else
                        "Nenhuma questão foi respondida ainda."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showExitDialog = false
                    viewModel.finishQuiz()
                }) {
                    Text("Sair")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("Continuar")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Quiz em andamento",
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (uiState.currentQuestion != null) {
                            Text(
                                text = "${uiState.currentIndex + 1} de ${uiState.totalQuestions}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { showExitDialog = true }) {
                        Icon(Icons.Filled.Close, contentDescription = "Sair do quiz")
                    }
                },
                actions = {
                    // Timer
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(
                            Icons.Filled.Timer,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        val minutes = uiState.timeSpentSeconds / 60
                        val seconds = uiState.timeSpentSeconds % 60
                        Text(
                            text = "%d:%02d".format(minutes, seconds),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = uiState.errorMessage!!,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onBackClick) {
                            Text("Voltar")
                        }
                    }
                }
            }
            uiState.currentQuestion != null -> {
                val quizQuestion = uiState.currentQuestion!!

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    // Progress Bar
                    LinearProgressIndicator(
                        progress = { uiState.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    // Score display
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "✓ ${uiState.correctCount}",
                            style = MaterialTheme.typography.labelLarge,
                            color = CorrectGreen
                        )
                        Text(
                            text = quizQuestion.question.specialty,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "✗ ${uiState.wrongCount}",
                            style = MaterialTheme.typography.labelLarge,
                            color = IncorrectRed
                        )
                    }

                    // Question Content
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp)
                    ) {
                        // Question Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AssistChip(
                                onClick = {},
                                label = {
                                    Text(
                                        text = quizQuestion.question.specialty,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Filled.MedicalServices,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Difficulty indicator
                                repeat(5) { index ->
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .padding(1.dp)
                                            .clip(RoundedCornerShape(50))
                                            .background(
                                                if (index < quizQuestion.question.difficulty) {
                                                    when (quizQuestion.question.difficulty) {
                                                        1, 2 -> DifficultyEasy
                                                        3 -> DifficultyMedium
                                                        else -> DifficultyHard
                                                    }
                                                } else {
                                                    MaterialTheme.colorScheme.surfaceVariant
                                                }
                                            )
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))

                                // Flag button
                                IconButton(
                                    onClick = { viewModel.toggleFlag() },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        if (quizQuestion.isFlagged) Icons.Filled.Flag else Icons.Filled.Flag,
                                        contentDescription = "Marcar",
                                        tint = if (quizQuestion.isFlagged) WarningOrange else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Question Text
                        Text(
                            text = quizQuestion.question.questionText,
                            style = MaterialTheme.typography.bodyLarge,
                            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Answer Options
                        quizQuestion.options.forEach { option ->
                            AnswerOptionItem(
                                option = option,
                                isSelected = uiState.selectedOptionId == option.id,
                                isRevealed = uiState.isAnswerRevealed,
                                isCorrect = option.isCorrect,
                                onClick = { viewModel.selectOption(option.id) }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Explanation Section (after answer revealed)
                        AnimatedVisibility(visible = uiState.isAnswerRevealed) {
                            Column {
                                Spacer(modifier = Modifier.height(16.dp))

                                if (uiState.isExplanationLoading) {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text("Gerando explicação da IA...")
                                        }
                                    }
                                } else if (uiState.explanation != null) {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Text(
                                                text = "📖 Explicação",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = uiState.explanation!!.correctReasoning,
                                                style = MaterialTheme.typography.bodyMedium
                                            )

                                            // Wrong answer explanations
                                            val wrongReasonings: Map<String, String> = try {
                                                com.google.gson.Gson().fromJson(
                                                    uiState.explanation!!.wrongReasoning,
                                                    com.google.gson.reflect.TypeToken.getParameterized(
                                                        Map::class.java, String::class.java, String::class.java
                                                    ).type
                                                ) ?: emptyMap()
                                            } catch (e: Exception) {
                                                emptyMap()
                                            }

                                            if (wrongReasonings.isNotEmpty()) {
                                                Spacer(modifier = Modifier.height(12.dp))
                                                Divider()
                                                Spacer(modifier = Modifier.height(12.dp))

                                                wrongReasonings.forEach { (letter, reasoning) ->
                                                    Row(modifier = Modifier.padding(vertical = 4.dp)) {
                                                        Text(
                                                            text = "❌ Por que $letter está errado:",
                                                            style = MaterialTheme.typography.labelLarge,
                                                            color = IncorrectRed,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                    }
                                                    Text(
                                                        text = reasoning,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Note button
                                OutlinedButton(
                                    onClick = { viewModel.showNoteDialog() },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Filled.Note, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Adicionar anotação")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Bottom Action Bar
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        tonalElevation = 3.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Previous
                            OutlinedButton(
                                onClick = { viewModel.previousQuestion() },
                                enabled = uiState.currentIndex > 0,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.ArrowBack, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Anterior")
                            }

                            // Confirm / Next
                            if (!uiState.isAnswerRevealed) {
                                Button(
                                    onClick = { viewModel.confirmAnswer() },
                                    enabled = uiState.selectedOptionId != null,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Confirmar")
                                }
                            } else {
                                Button(
                                    onClick = { viewModel.nextQuestion() },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        if (uiState.currentIndex < uiState.totalQuestions - 1) "Próximo"
                                        else "Finalizar"
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Filled.ArrowForward, contentDescription = null)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Note Dialog
    if (uiState.showNoteDialog) {
        var noteText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { viewModel.hideNoteDialog() },
            title = { Text("Adicionar anotação") },
            text = {
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Sua anotação") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.saveNote(noteText)
                        viewModel.hideNoteDialog()
                    }
                ) {
                    Text("Salvar")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideNoteDialog() }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun AnswerOptionItem(
    option: AnswerOptionEntity,
    isSelected: Boolean,
    isRevealed: Boolean,
    isCorrect: Boolean,
    onClick: () -> Unit
) {
    val containerColor = when {
        isRevealed && isCorrect -> CorrectGreenLight
        isRevealed && isSelected && !isCorrect -> IncorrectRedLight
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surface
    }

    val borderColor = when {
        isRevealed && isCorrect -> CorrectGreen
        isRevealed && isSelected && !isCorrect -> IncorrectRed
        isSelected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    }

    val textColor = when {
        isRevealed && isCorrect -> CorrectGreen
        isRevealed && isSelected && !isCorrect -> IncorrectRed
        isSelected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected || (isRevealed && isCorrect)) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(enabled = !isRevealed) { onClick() },
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Letter badge
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(textColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = option.optionLetter,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = option.optionText,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                modifier = Modifier.weight(1f)
            )

            if (isRevealed) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = if (isCorrect) Icons.Filled.CheckCircle else {
                        if (isSelected) Icons.Filled.Cancel else Icons.Filled.Circle
                    },
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
