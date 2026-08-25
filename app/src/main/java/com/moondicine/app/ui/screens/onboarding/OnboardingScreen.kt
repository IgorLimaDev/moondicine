package com.moondicine.app.ui.screens.onboarding

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.moondicine.app.ui.theme.*

@Composable
fun OnboardingScreen(
    onCompleted: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        // Progress indicator
        LinearProgressIndicator(
            progress = { (uiState.currentStep + 1).toFloat() / uiState.totalSteps.toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Etapa ${uiState.currentStep + 1} de ${uiState.totalSteps}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.height(40.dp))

        when (uiState.currentStep) {
            0 -> WelcomeStep(
                name = uiState.name,
                onNameChange = { viewModel.updateName(it) }
            )
            1 -> SpecialtyStep(
                selected = uiState.targetSpecialty,
                onSelect = { viewModel.updateTargetSpecialty(it) }
            )
            2 -> ExperienceStep(
                selected = uiState.experienceLevel,
                onSelect = { viewModel.updateExperienceLevel(it) }
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Navigation buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (uiState.currentStep > 0) {
                OutlinedButton(
                    onClick = { viewModel.previousStep() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Voltar")
                }
            }

            Button(
                onClick = {
                    if (uiState.currentStep < uiState.totalSteps - 1) {
                        viewModel.nextStep()
                    } else {
                        viewModel.completeOnboarding(onCompleted)
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = viewModel.canProceed() && !uiState.isCompleting
            ) {
                if (uiState.isCompleting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(if (uiState.currentStep < uiState.totalSteps - 1) "Próximo" else "Começar")
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        if (uiState.currentStep < uiState.totalSteps - 1) Icons.Filled.ArrowForward else Icons.Filled.Check,
                        contentDescription = null
                    )
                }
            }
        }
    }
}

@Composable
fun WelcomeStep(name: String, onNameChange: (String) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            Icons.Filled.WavingHand,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Secondary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Bem-vindo ao Moondicine!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Seu companheiro de estudos para residência médica.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(32.dp))
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Qual é o seu nome?") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpecialtyStep(selected: String, onSelect: (String) -> Unit) {
    val specialties = listOf(
        "Clínica Médica" to Icons.Filled.MonitorHeart,
        "Cirurgia Geral" to Icons.Filled.Healing,
        "Pediatria" to Icons.Filled.ChildCare,
        "Ginecologia e Obstetrícia" to Icons.Filled.Favorite,
        "Medicina Preventiva" to Icons.Filled.HealthAndSafety,
        "Ainda não decidido" to Icons.Filled.HelpOutline
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            Icons.Filled.MedicalServices,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Qual é a sua especialidade-alvo?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Isso ajuda a priorizar questões relevantes para você.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(24.dp))

        specialties.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { (name, icon) ->
                    val isSelected = selected == name
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelect(name) },
                        label = { Text(name, style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = {
                            Icon(
                                icon,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                // Fill remaining space if odd number
                if (rowItems.size < 2) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExperienceStep(selected: String, onSelect: (String) -> Unit) {
    val options = listOf(
        "first_time" to "Primeira vez fazendo a prova",
        "retake_1" to "Retomando (1ª tentativa)",
        "retake_2plus" to "Retomando (2+ tentativas)"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            Icons.Filled.School,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Tertiary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Qual é o seu nível de experiência?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Isso ajuda a calibrar a dificuldade das questões para você.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(24.dp))

        options.forEach { (value, label) ->
            val isSelected = selected == value
            Card(
                onClick = { onSelect(value) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = { onSelect(value) }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}
