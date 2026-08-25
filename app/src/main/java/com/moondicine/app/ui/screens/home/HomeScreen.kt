package com.moondicine.app.ui.screens.home

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.moondicine.app.ui.theme.CorrectGreen
import com.moondicine.app.ui.theme.IncorrectRed
import com.moondicine.app.ui.theme.Primary
import com.moondicine.app.ui.theme.Secondary
import com.moondicine.app.ui.theme.Tertiary
import com.moondicine.app.ui.theme.WarningOrange
import com.moondicine.app.ui.updates.UpdateDialog
import com.moondicine.app.ui.updates.UpdateViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onStartQuiz: (quizType: String, specialty: String, count: Int) -> Unit,
    onSelectExam: () -> Unit,
    onSelectSpecialty: () -> Unit,
    onUploadClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
    updateViewModel: UpdateViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val updateUiState by updateViewModel.uiState.collectAsState()
    var showUpdateDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    // Check for updates on first load
    androidx.compose.runtime.LaunchedEffect(Unit) {
        updateViewModel.checkForUpdates()
    }
    
    // Show update dialog when available
    if (updateUiState.updateInfo?.hasUpdate == true && !showUpdateDialog) {
        showUpdateDialog = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Moondicine",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { /* Settings */ }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Configurações")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(padding)
            ) {
                // Welcome header
                item(span = { GridItemSpan(2) }) {
                    WelcomeCard(uiState)
                }

                item(span = { GridItemSpan(2) }) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = viewModel::syncNow,
                            enabled = !uiState.isSyncing,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (uiState.isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Filled.Sync, contentDescription = null)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (uiState.isSyncing) "Sincronizando..." else "Sincronizar banco")
                        }

                        uiState.syncMessage?.let { message ->
                            Text(
                                text = message,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                item {
                    StatCard(
                        title = "Questões totais",
                        value = "${uiState.totalQuestions}",
                        icon = Icons.Filled.Quiz,
                        color = Primary
                    )
                }
                item {
                    StatCard(
                        title = "Respondidas",
                        value = "${uiState.totalAnswered}",
                        icon = Icons.Filled.CheckCircle,
                        color = CorrectGreen
                    )
                }
                item {
                    StatCard(
                        title = "Precisão",
                        value = "${(uiState.accuracy * 100).toInt()}%",
                        icon = Icons.Filled.TrendingUp,
                        color = Secondary
                    )
                }
                item {
                    StatCard(
                        title = "Sequência",
                        value = "${uiState.currentStreak} dias",
                        icon = Icons.Filled.LocalFireDepartment,
                        color = WarningOrange
                    )
                }

                item(span = { GridItemSpan(2) }) {
                    Text(
                        text = "Início rápido",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                item {
                    ActionCard(
                        title = "Quiz rápido",
                        subtitle = "10 questões aleatórias",
                        icon = Icons.Filled.FlashOn,
                        color = Primary,
                        onClick = { onStartQuiz("quick", "all", 10) }
                    )
                }
                item {
                    ActionCard(
                        title = "Quiz por especialidade",
                        subtitle = "Escolha seu foco",
                        icon = Icons.Filled.MedicalServices,
                        color = Tertiary,
                        onClick = { onStartQuiz("specialty", "all", 15) }
                    )
                }
                item {
                    ActionCard(
                        title = "Áreas fracas",
                        subtitle = "Reveja erros",
                        icon = Icons.Filled.SentimentDissatisfied,
                        color = IncorrectRed,
                        onClick = { onStartQuiz("weak", "all", 10) }
                    )
                }
                item {
                    ActionCard(
                        title = "Simulado",
                        subtitle = "Prova completa",
                        icon = Icons.Filled.Timer,
                        color = Secondary,
                        onClick = { onStartQuiz("exam", "all", 40) }
                    )
                }
                item {
                    ActionCard(
                        title = "Escolher prova",
                        subtitle = "Selecione uma prova específica",
                        icon = Icons.Filled.MenuBook,
                        color = Tertiary,
                        onClick = onSelectExam
                    )
                }
                item {
                    ActionCard(
                        title = "Escolher especialidade",
                        subtitle = "Selecione uma especialidade",
                        icon = Icons.Filled.MedicalServices,
                        color = Primary,
                        onClick = onSelectSpecialty
                    )
                }

                // Upload prompt if no questions
                if (uiState.totalQuestions == 0) {
                    item(span = { GridItemSpan(2) }) {
                        EmptyStateCard(onUploadClick)
                    }
                }

                // Specialties overview
                if (uiState.specialties.isNotEmpty()) {
                    item(span = { GridItemSpan(2) }) {
                        Text(
                            text = "Suas especialidades",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    items(uiState.specialties) { specialty ->
                        val stat = uiState.stats.find { it.specialty == specialty }
                        val accuracy = if (stat != null && stat.totalAnswered > 0) {
                            stat.totalCorrect.toFloat() / stat.totalAnswered.toFloat()
                        } else 0f

                        SpecialtyCard(
                            name = specialty,
                            questionCount = stat?.totalAnswered ?: 0,
                            accuracy = accuracy
                        )
                    }
                }
            }
        }

        // Show update dialog
        if (showUpdateDialog) {
            UpdateDialog(
                onDismiss = { showUpdateDialog = false },
                onOpenRelease = {
                    updateUiState.updateInfo?.releaseUrl?.let { url ->
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        } catch (_: Exception) { }
                    }
                    showUpdateDialog = false
                },
                viewModel = updateViewModel
            )
        }
    }
}

@Composable
fun WelcomeCard(uiState: HomeUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Bem-vindo de volta! 👋",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Você tem ${uiState.totalQuestions} questões disponíveis. Continue estudando!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: androidx.compose.ui.graphics.Color
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun EmptyStateCard(onUploadClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.CloudUpload,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Ainda não há questões!",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Envie um PDF de prova para começar",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onUploadClick) {
                Icon(Icons.Filled.CloudUpload, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Enviar PDF")
            }
        }
    }
}

@Composable
fun SpecialtyCard(
    name: String,
    questionCount: Int,
    accuracy: Float
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$questionCount respondidas · ${(accuracy * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { accuracy },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = when {
                    accuracy >= 0.8f -> CorrectGreen
                    accuracy >= 0.6f -> WarningOrange
                    else -> IncorrectRed
                },
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}
