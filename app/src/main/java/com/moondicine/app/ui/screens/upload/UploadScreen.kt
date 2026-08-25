package com.moondicine.app.ui.screens.upload

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.moondicine.app.ui.theme.CorrectGreen
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadScreen(
    onUploadComplete: () -> Unit,
    viewModel: UploadViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val questionsPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.setQuestionsFile(it) }
    }

    val answersPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.setAnswersFile(it) }
    }

    LaunchedEffect(uiState.state) {
        if (uiState.state == UploadState.COMPLETE) {
            delay(1500)
            onUploadComplete()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Enviar prova") },
                navigationIcon = {
                    IconButton(onClick = { onUploadComplete() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Modo de envio",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !uiState.isDualMode,
                            onClick = { viewModel.setDualMode(false) },
                            label = { Text("Um PDF") },
                            leadingIcon = if (!uiState.isDualMode) {
                                {
                                    Icon(
                                        Icons.Filled.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            } else null,
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = uiState.isDualMode,
                            onClick = { viewModel.setDualMode(true) },
                            label = { Text("Dois PDFs") },
                            leadingIcon = if (uiState.isDualMode) {
                                {
                                    Icon(
                                        Icons.Filled.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            } else null,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (uiState.isDualMode) {
                            "Envie um PDF com as questões e outro com o gabarito"
                        } else {
                            "Envie um único PDF contendo perguntas e respostas"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            FileSelectionCard(
                title = "PDF de questões",
                subtitle = if (uiState.questionsFileUri != null) "Arquivo selecionado" else "Toque para selecionar",
                isSelected = uiState.questionsFileUri != null,
                onClick = { questionsPickerLauncher.launch("application/pdf") },
                icon = Icons.Filled.Quiz
            )

            Spacer(modifier = Modifier.height(12.dp))

            AnimatedVisibility(visible = uiState.isDualMode) {
                FileSelectionCard(
                    title = "PDF do gabarito",
                    subtitle = if (uiState.answersFileUri != null) "Arquivo selecionado" else "Toque para selecionar",
                    isSelected = uiState.answersFileUri != null,
                    onClick = { answersPickerLauncher.launch("application/pdf") },
                    icon = Icons.Filled.Key
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            AnimatedVisibility(visible = uiState.state != UploadState.IDLE && uiState.state != UploadState.ERROR) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val animatedProgress by animateFloatAsState(
                            targetValue = uiState.progress,
                            label = "progress"
                        )

                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = uiState.statusMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "${(animatedProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (uiState.state == UploadState.ERROR && uiState.errorMessage != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Filled.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uiState.errorMessage!!,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(onClick = { viewModel.clearError() }) {
                            Text("Tentar novamente")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val canUpload = uiState.questionsFileUri != null &&
                    (!uiState.isDualMode || uiState.answersFileUri != null) &&
                    uiState.state != UploadState.EXTRACTING_TEXT &&
                    uiState.state != UploadState.ANALYZING_AI &&
                    uiState.state != UploadState.STORING

            Button(
                onClick = { viewModel.startProcessing() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = canUpload
            ) {
                if (uiState.state == UploadState.EXTRACTING_TEXT ||
                    uiState.state == UploadState.ANALYZING_AI ||
                    uiState.state == UploadState.STORING
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(Icons.Filled.CloudUpload, contentDescription = null)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when (uiState.state) {
                        UploadState.EXTRACTING_TEXT -> "Extraindo texto..."
                        UploadState.ANALYZING_AI -> "A IA está analisando..."
                        UploadState.STORING -> "Salvando questões..."
                        UploadState.COMPLETE -> "Concluído!"
                        else -> "Processar PDF"
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileSelectionCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
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
            Icon(
                imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Filled.Upload,
                contentDescription = null,
                tint = if (isSelected) CorrectGreen else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
