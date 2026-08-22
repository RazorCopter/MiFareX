package de.syss.MifareClassicTool.ui.usermode

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.syss.MifareClassicTool.data.model.VendorEntity
import de.syss.MifareClassicTool.domain.model.WriteOperationResult

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun AutoModeScreen(
    onBackClick: () -> Unit,
    viewModel: VendorWriteViewModel
) {
    val autoState by viewModel.autoModeState.collectAsStateWithLifecycle()

    // Start listening when screen appears, stop when it disappears
    DisposableEffect(Unit) {
        viewModel.startAutoMode()
        onDispose { viewModel.stopAutoMode() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Auto Mode") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Indietro")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            when (val s = autoState) {
                is AutoModeState.Off -> Unit

                is AutoModeState.Listening -> ListeningPanel()

                is AutoModeState.Writing -> WritingPanel(vendorName = s.vendorName)

                is AutoModeState.Done -> DonePanel(
                    vendorName = s.vendorName,
                    result = s.result,
                    onContinue = { viewModel.resetAutoMode() }
                )

                is AutoModeState.UnknownUid -> UnknownUidDialog(
                    uid = s.uid,
                    viewModel = viewModel,
                    onDismiss = { viewModel.dismissUnknownUid() }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────
//  Listening panel — pulsating NFC icon
// ─────────────────────────────────────────────
@Composable
private fun ListeningPanel() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier.padding(32.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Nfc,
            contentDescription = null,
            modifier = Modifier.size((80 * scale).dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = alpha)
        )
        Text(
            "In ascolto…",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Avvicina un tag NFC al telefono.\nViene programmato automaticamente\nse l'UID è registrato.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// ─────────────────────────────────────────────
//  Writing panel
// ─────────────────────────────────────────────
@Composable
private fun WritingPanel(vendorName: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier.padding(32.dp)
    ) {
        CircularProgressIndicator(modifier = Modifier.size(64.dp), strokeWidth = 5.dp)
        Text(
            "Scrittura in corso…",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            vendorName,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            "Non allontanare il tag!",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ─────────────────────────────────────────────
//  Done panel
// ─────────────────────────────────────────────
@Composable
private fun DonePanel(
    vendorName: String,
    result: WriteOperationResult,
    onContinue: () -> Unit
) {
    val isSuccess = result is WriteOperationResult.Success
    val isPartial = result is WriteOperationResult.Partial
    val iconColor = when {
        isSuccess -> Color(0xFF2E7D32)
        isPartial -> Color(0xFFF57F17)
        else -> Color(0xFFC62828)
    }
    val icon = when {
        isSuccess -> Icons.Filled.CheckCircle
        isPartial -> Icons.Filled.Warning
        else -> Icons.Filled.Error
    }
    val label = when {
        isSuccess -> "Scrittura completata!"
        isPartial -> "Scrittura parziale"
        else -> "Scrittura fallita"
    }

    // Haptic feedback on result
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(result) {
        haptic.performHapticFeedback(
            if (isSuccess) HapticFeedbackType.LongPress else HapticFeedbackType.LongPress
        )
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(32.dp)
    ) {
        Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(72.dp))
        Text(label, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            vendorName,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary
        )

        // Detail message for non-success
        if (!isSuccess) {
            val detail = when (result) {
                is WriteOperationResult.Error -> result.message
                is WriteOperationResult.Partial ->
                    "${result.blocksWritten}/${result.totalBlocks} blocchi scritti"
                is WriteOperationResult.PreflightFailed ->
                    preflightResultToUiText(result.reason).second
                else -> ""
            }
            if (detail.isNotBlank()) {
                Text(
                    detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Nfc, null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Prossimo Tag")
        }
    }
}

// ─────────────────────────────────────────────
//  Unknown UID dialog
// ─────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnknownUidDialog(
    uid: String,
    viewModel: VendorWriteViewModel,
    onDismiss: () -> Unit
) {
    val vendors by viewModel.getAllVendors().collectAsStateWithLifecycle(initialValue = emptyList())
    var selectedVendor by remember { mutableStateOf<VendorEntity?>(null) }
    var dropdownExpanded by remember { mutableStateOf(false) }

    // We don't have a Tag reference here — store it in ViewModel when UnknownUid fires
    // The dialog only associates; writing happens on next tap with known UID
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Filled.Nfc, null, tint = MaterialTheme.colorScheme.primary)
        },
        title = {
            Text("Nuovo UID rilevato", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = uid.uppercase().chunked(2).joinToString(" "),
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
                Text(
                    "Associa questo tag a un Vendor per programmarlo automaticamente.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ExposedDropdownMenuBox(
                    expanded = dropdownExpanded,
                    onExpandedChange = { dropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedVendor?.name ?: "Seleziona Vendor…",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded)
                        }
                    )
                    ExposedDropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false }
                    ) {
                        vendors.forEach { vendor ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(vendor.name, fontWeight = FontWeight.Medium)
                                        vendor.subtitle?.let {
                                            Text(it, style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                },
                                onClick = {
                                    selectedVendor = vendor
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedVendor?.let { vendor ->
                        viewModel.associateUidOnly(uid, vendor.id)
                        onDismiss()
                    }
                },
                enabled = selectedVendor != null
            ) {
                Text("Associa")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annulla") }
        }
    )
}
