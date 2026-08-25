package com.moondicine.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.moondicine.app.ui.theme.CorrectGreen
import com.moondicine.app.ui.theme.Primary
import com.moondicine.app.ui.updates.UpdateDialog
import com.moondicine.app.ui.updates.UpdateViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    viewModel: UpdateViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showUpdateDialog by remember { mutableStateOf(false) }
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.checkForUpdates()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    
    if (uiState.updateInfo?.hasUpdate == true) {
        showUpdateDialog = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configurações") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        androidx.compose.material.icons.filled.ArrowBack
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App Info Section
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material.icons.filled.Info
                        Spacer(modifier = androidx.compose.foundation.layout.width(12.dp))
                        Column {
                            Text("Sobre o Moondicine", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Versão 1.0.0", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                    }
                }
            }
            
            // Update Section
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material.icons.filled.SystemUpdate
                        Spacer(modifier = androidx.compose.foundation.layout.width(12.dp))
                        Column {
                            Text("Atualizações", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Verificar novas versões", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                    }
                }
            }
            
            // Check for Updates Button
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { viewModel.checkForUpdates() },
                colors = CardDefaults.cardColors(containerColor = Primary.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (uiState.isChecking) {
                        CircularProgressIndicator(
                            modifier = androidx.compose.foundation.layout.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        androidx.compose.material.icons.filled.SystemUpdate
                    }
                    Spacer(modifier = androidx.compose.foundation.layout.width(12.dp))
                    Text(
                        text = if (uiState.isChecking) "Verificando..." else "Verificar atualizações",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Primary
                    )
                }
            }
            
            // Update Status
            uiState.updateInfo?.let { updateInfo ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (updateInfo.hasUpdate) CorrectGreen.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            androidx.compose.material.icons.filled.CheckCircle
                            Spacer(modifier = androidx.compose.foundation.layout.width(12.dp))
                            Column {
                                Text(
                                    text = if (updateInfo.hasUpdate) "Nova versão ${updateInfo.latestVersion} disponível!" else "Você está na versão mais recente",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (updateInfo.hasUpdate) CorrectGreen else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Versão atual: ${updateInfo.currentVersion}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }
            
            uiState.errorMessage?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.Red.copy(alpha = 0.1f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row {
                            androidx.compose.material.icons.filled.Info
                            Spacer(modifier = androidx.compose.foundation.layout.width(8.dp))
                            Text("Erro ao verificar atualizações", style = MaterialTheme.typography.labelLarge, color = androidx.compose.ui.graphics.Color.Red)
                        }
                        Spacer(modifier = androidx.compose.foundation.layout.size(8.dp))
                        Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    }
                }
            }
        }
    }
}