package com.moondicine.app.ui.screens.quiz

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moondicine.app.ui.theme.*

@Composable
fun QuizResultScreen(
    totalQuestions: Int,
    correctAnswers: Int,
    timeSpent: Int,
    onBackToHome: () -> Unit,
    onReviewAnswers: () -> Unit
) {
    val wrongAnswers = totalQuestions - correctAnswers
    val accuracy = if (totalQuestions > 0) (correctAnswers.toFloat() / totalQuestions.toFloat()) else 0f
    val minutes = timeSpent / 60
    val seconds = timeSpent % 60

    val message = when {
        accuracy >= 0.9f -> "Excelente! 🏆"
        accuracy >= 0.8f -> "Muito bem! 🎉"
        accuracy >= 0.7f -> "Bom trabalho! 👍"
        accuracy >= 0.6f -> "Continue praticando! 💪"
        else -> "Não desista! 📚"
    }

    val messageDetail = when {
        accuracy >= 0.9f -> "Você está pronto para a prova!"
        accuracy >= 0.8f -> "Você está indo muito bem!"
        accuracy >= 0.7f -> "Você está no caminho certo!"
        accuracy >= 0.6f -> "Um pouco mais de estudo vai te levar lá!"
        else -> "Revise suas áreas fracas e tente novamente!"
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Score Circle
            Card(
                modifier = Modifier.size(200.dp),
                shape = RoundedCornerShape(100.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        accuracy >= 0.8f -> CorrectGreen.copy(alpha = 0.1f)
                        accuracy >= 0.6f -> WarningOrange.copy(alpha = 0.1f)
                        else -> IncorrectRed.copy(alpha = 0.1f)
                    }
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${(accuracy * 100).toInt()}%",
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                accuracy >= 0.8f -> CorrectGreen
                                accuracy >= 0.6f -> WarningOrange
                                else -> IncorrectRed
                            },
                            fontSize = 48.sp
                        )
                        Text(
                            text = "$correctAnswers/$totalQuestions",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Message
            Text(
                text = message,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = messageDetail,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ResultStat(
                    icon = Icons.Filled.CheckCircle,
                    value = "$correctAnswers",
                    label = "Corretas",
                    color = CorrectGreen
                )
                ResultStat(
                    icon = Icons.Filled.Cancel,
                    value = "$wrongAnswers",
                    label = "Erradas",
                    color = IncorrectRed
                )
                ResultStat(
                    icon = Icons.Filled.Timer,
                    value = "%d:%02d".format(minutes, seconds),
                    label = "Tempo",
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Action Buttons
            Button(
                onClick = onReviewAnswers,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Icon(Icons.Filled.FlashOn, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Revisar respostas")
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onBackToHome,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Icon(Icons.Filled.Home, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Voltar para o início")
            }
        }
    }
}

@Composable
fun ResultStat(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}
