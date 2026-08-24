package de.syss.MifareClassicTool.ui.adminmode

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import de.syss.MifareClassicTool.ui.components.MctxModeBadge
import de.syss.MifareClassicTool.ui.components.MctxStatusBanner
import de.syss.MifareClassicTool.ui.components.PremiumScreenBackground
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportExportScreen(
    onBackClick: () -> Unit,
    onUidManagerClick: () -> Unit = {},
    viewModel: ImportExportViewModel = viewModel()
) {
    val state = viewModel.state
    val vendorCount = viewModel.vendorCount
    val uidCount = viewModel.uidCount
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let(viewModel::exportToUri)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::importFromUri)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Dati e backup")
                        Text("Import / Export locale", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Indietro") }
                },
                actions = { MctxModeBadge("Admin", modifier = Modifier.padding(end = 12.dp)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            )
        }
    ) { padding ->
        PremiumScreenBackground(modifier = Modifier.padding(padding)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().widthIn(max = 840.dp).align(Alignment.TopCenter),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    MctxStatusBanner(
                        title = "$vendorCount vendor · $uidCount UID",
                        message = "I dati restano sul dispositivo o nella posizione scelta tramite Android.",
                        icon = Icons.Filled.Storage
                    )
                }
                item {
                    FileActionCard(
                        title = "Esporta backup JSON",
                        description = if (vendorCount > 0) "Salva profili, configurazioni e associazioni UID." else "Crea prima almeno un vendor.",
                        icon = Icons.Filled.FileUpload,
                        enabled = state !is ImportExportState.Loading && vendorCount > 0,
                        onClick = {
                            val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
                            exportLauncher.launch("mctx_vendors_$timestamp.json")
                        }
                    )
                }
                item {
                    FileActionCard(
                        title = "Importa con validazione",
                        description = "Seleziona un JSON. Schema e contenuti vengono verificati prima della sostituzione.",
                        icon = Icons.Filled.FileDownload,
                        enabled = state !is ImportExportState.Loading,
                        onClick = { importLauncher.launch(arrayOf("application/json", "text/plain")) }
                    )
                }
                item {
                    FileActionCard(
                        title = "Gestisci associazioni UID",
                        description = "Controlla quale profilo viene selezionato in Auto Mode.",
                        icon = Icons.Filled.Fingerprint,
                        enabled = state !is ImportExportState.Loading,
                        onClick = onUidManagerClick
                    )
                }
                item { ImportExportFeedback(state = state, onDismiss = viewModel::resetState) }
                item {
                    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainer) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(Icons.Filled.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Formato portabile", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(
                                    "Conserva i backup in una posizione protetta. Il file può contenere materiale tecnico sensibile.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FileActionCard(
    title: String,
    description: String,
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primaryContainer) {
                Box(modifier = Modifier.padding(11.dp), contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(25.dp))
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ImportExportFeedback(state: ImportExportState, onDismiss: () -> Unit) {
    AnimatedVisibility(
        visible = state !is ImportExportState.Idle,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        val loading = state is ImportExportState.Loading
        val success = state is ImportExportState.Success
        val color = when {
            success -> Color(0xFF2F8D57)
            loading -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.error
        }
        val message = when (state) {
            is ImportExportState.Loading -> "Operazione in corso…"
            is ImportExportState.Success -> state.message
            is ImportExportState.Error -> state.message
            else -> ""
        }
        Surface(shape = MaterialTheme.shapes.large, color = color.copy(alpha = 0.12f), modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                if (loading) CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
                else Icon(if (success) Icons.Filled.CheckCircle else Icons.Filled.Error, contentDescription = null, tint = color)
                Spacer(Modifier.width(12.dp))
                Text(message, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                if (!loading) {
                    IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, contentDescription = "Chiudi messaggio") }
                }
            }
        }
    }
}
