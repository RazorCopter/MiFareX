package de.syss.MifareClassicTool.ui.usermode

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.syss.MifareClassicTool.data.model.VendorEntity
import de.syss.MifareClassicTool.domain.model.WriteOperationResult
import de.syss.MifareClassicTool.ui.components.MctxModeBadge
import de.syss.MifareClassicTool.ui.components.NfcProgressStepper
import de.syss.MifareClassicTool.ui.components.NfcRingsAnimation
import de.syss.MifareClassicTool.ui.components.NfcStatusBadge
import de.syss.MifareClassicTool.ui.components.PremiumScreenBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoModeScreen(onBackClick: () -> Unit, viewModel: VendorWriteViewModel) {
    val autoState by viewModel.autoModeState.collectAsStateWithLifecycle()

    DisposableEffect(Unit) {
        viewModel.startAutoMode()
        onDispose { viewModel.stopAutoMode() }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Auto Mode")
                        Text("Flusso operatore continuo", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                },
                actions = { NfcStatusBadge(modifier = Modifier.padding(end = 12.dp)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            )
        }
    ) { padding ->
        PremiumScreenBackground(modifier = Modifier.padding(padding)) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                when (val state = autoState) {
                    is AutoModeState.Off -> Unit
                    is AutoModeState.Listening -> ListeningPanel()
                    is AutoModeState.Writing -> WritingPanel(state.vendorName)
                    is AutoModeState.Done -> DonePanel(state.vendorName, state.result) { viewModel.resetAutoMode() }
                    is AutoModeState.UnknownUid -> UnknownUidDialog(
                        uid = state.uid,
                        viewModel = viewModel,
                        onDismiss = viewModel::dismissUnknownUid
                    )
                }
            }
        }
    }
}

@Composable
private fun ListeningPanel() {
    OperationCard {
        MctxModeBadge("Auto mode armato")
        NfcRingsAnimation(color = MaterialTheme.colorScheme.primary, isWriting = false, size = 144.dp)
        Text("In ascolto", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "Avvicina un tag. Se l’UID è registrato, MiFareX seleziona il profilo e avvia la procedura verificata.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        NfcProgressStepper(activeStep = 0)
        Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.tertiaryContainer) {
            Text(
                "Un solo tag alla volta · mantienilo fermo fino al risultato",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun WritingPanel(vendorName: String) {
    OperationCard {
        MctxModeBadge("Operazione in corso")
        NfcProgressStepper(activeStep = 1)
        NfcRingsAnimation(color = MaterialTheme.colorScheme.primary, isWriting = true, size = 132.dp)
        Text("Scrittura e verifica", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(vendorName, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Text("Non rimuovere il tag", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DonePanel(vendorName: String, result: WriteOperationResult, onContinue: () -> Unit) {
    val success = result is WriteOperationResult.Success
    val partial = result is WriteOperationResult.Partial
    val iconColor = when {
        success -> Color(0xFF48C774)
        partial -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }
    val icon = when {
        success -> Icons.Filled.CheckCircle
        partial -> Icons.Filled.Warning
        else -> Icons.Filled.Error
    }
    val label = when {
        success -> "Scrittura verificata"
        partial -> "Scrittura parziale"
        else -> "Scrittura fallita"
    }
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(result) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        if (success) {
            kotlinx.coroutines.delay(120)
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    OperationCard {
        NfcProgressStepper(activeStep = 2)
        Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(68.dp))
        Text(label, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text(vendorName, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        if (success) {
            MctxModeBadge("Read-back completato")
        } else {
            val detail = when (result) {
                is WriteOperationResult.Error -> result.message
                is WriteOperationResult.Partial -> "${result.blocksWritten}/${result.totalBlocks} blocchi scritti"
                is WriteOperationResult.PreflightFailed -> preflightResultToUiText(result.reason).second
                else -> ""
            }
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
        Button(onClick = onContinue, modifier = Modifier.fillMaxWidth().height(50.dp)) {
            Icon(Icons.Filled.Nfc, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Continua con il prossimo tag")
        }
    }
}

@Composable
private fun OperationCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp).padding(24.dp),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnknownUidDialog(uid: String, viewModel: VendorWriteViewModel, onDismiss: () -> Unit) {
    val vendors by viewModel.getAllVendors().collectAsStateWithLifecycle(initialValue = emptyList())
    var selectedVendor by remember { mutableStateOf<VendorEntity?>(null) }
    var dropdownExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Nfc, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary) },
        title = { Text("UID non riconosciuto") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MctxModeBadge("Richiede associazione", expert = true)
                Text("Il tag non è associato. Scegli un vendor: la scrittura avverrà soltanto al prossimo avvicinamento.")
                Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceContainerHighest) {
                    Text(
                        uid.uppercase().chunked(2).joinToString(" "),
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center
                    )
                }
                ExposedDropdownMenuBox(expanded = dropdownExpanded, onExpandedChange = { dropdownExpanded = it }) {
                    OutlinedTextField(
                        value = selectedVendor?.name ?: "Seleziona vendor",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(dropdownExpanded) }
                    )
                    ExposedDropdownMenu(expanded = dropdownExpanded, onDismissRequest = { dropdownExpanded = false }) {
                        vendors.forEach { vendor ->
                            DropdownMenuItem(
                                text = { Text(vendor.name) },
                                onClick = { selectedVendor = vendor; dropdownExpanded = false }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = selectedVendor != null,
                onClick = {
                    selectedVendor?.let { viewModel.associateUidOnly(uid, it.id, context) }
                    onDismiss()
                }
            ) { Text("Associa UID") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } }
    )
}
