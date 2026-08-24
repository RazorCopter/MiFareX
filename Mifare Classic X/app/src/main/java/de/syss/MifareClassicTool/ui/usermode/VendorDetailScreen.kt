package de.syss.MifareClassicTool.ui.usermode

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.syss.MifareClassicTool.Activities.DumpEditor
import de.syss.MifareClassicTool.data.model.VendorEntity
import de.syss.MifareClassicTool.domain.model.PreflightResult
import de.syss.MifareClassicTool.domain.model.WriteOperationResult
import de.syss.MifareClassicTool.ui.components.NfcRingsAnimation
import de.syss.MifareClassicTool.ui.components.MctxModeBadge
import de.syss.MifareClassicTool.ui.components.NfcProgressStepper
import de.syss.MifareClassicTool.ui.components.NfcStatusBadge
import de.syss.MifareClassicTool.ui.components.VendorCircleIcon
import de.syss.MifareClassicTool.ui.components.getCategoryColor

/**
 * Vendor detail screen with NFC write flow.
 * Shows vendor info and handles the complete write lifecycle:
 * Idle → WaitingForTag → Writing → Result
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorDetailScreen(
    vendorId: String,
    onBackClick: () -> Unit,
    onEditClick: () -> Unit,
    viewModel: VendorWriteViewModel
) {
    DisposableEffect(vendorId) {
        viewModel.enterVendorDetail(vendorId)
        onDispose { viewModel.leaveVendorDetail(vendorId) }
    }

    // Cache the Flow so it's not recreated on every recomposition
    val vendorFlow = remember(vendorId) { viewModel.loadVendor(vendorId) }
    val vendor by vendorFlow.collectAsStateWithLifecycle(initialValue = null)
    val writeState by viewModel.writeState.collectAsStateWithLifecycle()
    val testState by viewModel.testState.collectAsStateWithLifecycle()
    val readState by viewModel.readState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(readState) {
        if (readState is NfcReadState.Success) {
            val dump = (readState as NfcReadState.Success).dump
            val intent = Intent(context, DumpEditor::class.java).apply {
                putExtra(DumpEditor.EXTRA_DUMP, dump)
            }
            context.startActivity(intent)
            viewModel.resetReadState()
        }
    }

    vendor?.let { v ->
        var showWriteConfirmation by remember(v.id) { mutableStateOf(false) }
        val hasKeys = remember(v.keysJson) {
            try { v.keysJson != "[]" && v.keysJson.isNotBlank() } catch (_: Exception) { false }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(v.name, maxLines = 1)
                            Text("Profilo operativo", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Indietro")
                        }
                    },
                    actions = {
                        NfcStatusBadge()
                        IconButton(onClick = onEditClick) {
                            Icon(Icons.Filled.Edit, "Modifica")
                        }
                    }
                )
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                // Main content
                VendorDetailContent(
                    vendor = v,
                    isWritable = viewModel.vendorIsWritable(v),
                    hasKeys = hasKeys,
                    onStartWrite = { showWriteConfirmation = true },
                    onStartTest = { viewModel.startTestKeys() },
                    onStartRead = { viewModel.startReadFlow() },
                    modifier = Modifier.fillMaxSize()
                )

                // Write overlay states
                AnimatedVisibility(
                    visible = writeState is NfcWriteState.WaitingForTag,
                    enter = fadeIn() + slideInVertically { it },
                    exit = fadeOut() + slideOutVertically { it }
                ) {
                    NfcWaitingOverlay(
                        vendorName = v.name,
                        onCancel = { viewModel.cancelWriteFlow() }
                    )
                }

                AnimatedVisibility(
                    visible = writeState is NfcWriteState.Verifying,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    val msg = (writeState as? NfcWriteState.Verifying)?.message
                        ?: "Verifica tag..."
                    VerifyingOverlay(message = msg)
                }

                AnimatedVisibility(
                    visible = writeState is NfcWriteState.Writing,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    val msg = (writeState as? NfcWriteState.Writing)?.progress
                        ?: "Scrittura in corso..."
                    WritingOverlay(progressMessage = msg)
                }

                AnimatedVisibility(
                    visible = writeState is NfcWriteState.Completed,
                    enter = fadeIn() + scaleIn(initialScale = 0.8f),
                    exit = fadeOut()
                ) {
                    val result = (writeState as? NfcWriteState.Completed)?.result
                    if (result != null) {
                        WriteResultOverlay(
                            result = result,
                            onDismiss = { viewModel.resetState() }
                        )
                    }
                }

                // Test Keys overlay states
                AnimatedVisibility(
                    visible = testState is NfcTestState.WaitingForTag,
                    enter = fadeIn() + slideInVertically { it },
                    exit = fadeOut() + slideOutVertically { it }
                ) {
                    NfcWaitingOverlay(
                        vendorName = "Verifica Chiavi",
                        onCancel = { viewModel.cancelTestKeys() }
                    )
                }

                AnimatedVisibility(
                    visible = testState is NfcTestState.Testing,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    val msg = (testState as? NfcTestState.Testing)?.message
                        ?: "Verifica chiavi..."
                    VerifyingOverlay(message = msg)
                }

                AnimatedVisibility(
                    visible = testState is NfcTestState.Result,
                    enter = fadeIn() + scaleIn(initialScale = 0.8f),
                    exit = fadeOut()
                ) {
                    val preflight = (testState as? NfcTestState.Result)?.preflight
                    if (preflight != null) {
                        TestKeysResultOverlay(
                            preflight = preflight,
                            onDismiss = { viewModel.resetTestState() }
                        )
                    }
                }

                // Read Tag overlay states
                AnimatedVisibility(
                    visible = readState is NfcReadState.WaitingForTag,
                    enter = fadeIn() + slideInVertically { it },
                    exit = fadeOut() + slideOutVertically { it }
                ) {
                    NfcWaitingOverlay(
                        vendorName = "Lettura Tag",
                        onCancel = { viewModel.cancelReadFlow() }
                    )
                }

                AnimatedVisibility(
                    visible = readState is NfcReadState.Reading,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    val msg = (readState as? NfcReadState.Reading)?.message
                        ?: "Lettura tag in corso..."
                    VerifyingOverlay(message = msg)
                }

                AnimatedVisibility(
                    visible = readState is NfcReadState.Error,
                    enter = fadeIn() + scaleIn(initialScale = 0.8f),
                    exit = fadeOut()
                ) {
                    val msg = (readState as? NfcReadState.Error)?.message
                    if (msg != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.92f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Error,
                                        contentDescription = null,
                                        tint = Color(0xFFC62828),
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Errore di Lettura",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = msg,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Button(
                                        onClick = { viewModel.resetReadState() },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Chiudi")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (showWriteConfirmation) {
            WriteConfirmationDialog(
                vendor = v,
                onConfirm = {
                    showWriteConfirmation = false
                    viewModel.startWriteFlow()
                },
                onDismiss = { showWriteConfirmation = false }
            )
        }
    } ?: run {
        // Loading state
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun VendorDetailContent(
    vendor: VendorEntity,
    isWritable: Boolean,
    hasKeys: Boolean,
    onStartWrite: () -> Unit,
    onStartTest: () -> Unit,
    onStartRead: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categoryColor = getCategoryColor(vendor.category)
    val scrollState = rememberScrollState()

    Box(modifier = modifier) {
        // Scrollable info section
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 920.dp)
                .align(Alignment.TopCenter)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Expandable hero banner ──────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(184.dp)
                    .graphicsLayer {
                        translationY = scrollState.value * 0.5f
                    }
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                categoryColor.copy(alpha = 0.85f),
                                categoryColor.copy(alpha = 0.4f),
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    VendorCircleIcon(
                        iconUri = vendor.iconUri,
                        category = vendor.category,
                        categoryColor = categoryColor,
                        size = 84.dp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    MctxModeBadge(if (isWritable) "Pronto" else "Configurazione incompleta")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = vendor.name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (!vendor.subtitle.isNullOrBlank()) {
                        Text(
                            text = vendor.subtitle,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // ── Info cards ──────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .padding(top = 16.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InfoCard(icon = Icons.Filled.Nfc, label = "Tipo Tag", value = vendor.tagType.displayName)
                InfoCard(icon = Icons.Filled.Category, label = "Categoria", value = vendor.category.displayName)
                if (!vendor.notes.isNullOrBlank()) {
                    InfoCard(icon = Icons.AutoMirrored.Filled.Notes, label = "Note", value = vendor.notes)
                }
                InfoCard(icon = Icons.Filled.History, label = "Scritture effettuate", value = "${vendor.writeCount}")
            }
            Spacer(modifier = Modifier.height(if (hasKeys) 190.dp else 126.dp))
        }

        // Pinned action button — glassmorphism overlay
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .widthIn(max = 920.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.0f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
        ) {
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                if (!isWritable) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Filled.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                "Configura chiavi e blocchi prima di scrivere",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
                
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onStartWrite,
                        enabled = isWritable,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = categoryColor)
                    ) {
                        Icon(Icons.Filled.Nfc, contentDescription = null, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Programma Tag",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (hasKeys) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = onStartRead, modifier = Modifier.weight(1f).height(48.dp)) {
                                Icon(Icons.Filled.Memory, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Leggi")
                            }
                            OutlinedButton(onClick = onStartTest, modifier = Modifier.weight(1f).height(48.dp)) {
                                Icon(Icons.Filled.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Test chiavi")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WriteConfirmationDialog(
    vendor: VendorEntity,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("Conferma operazione") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Stai per armare una sessione NFC di scrittura. La sessione verrà annullata uscendo dalla schermata.")
                Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(vendor.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Tag atteso · ${vendor.tagType.displayName}", style = MaterialTheme.typography.bodySmall)
                        Text("Controllo aree protette e read-back attivi", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        },
        confirmButton = { Button(onClick = onConfirm) { Text("Arma scrittura") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } }
    )
}

@Composable
private fun InfoCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Full-screen overlay shown while waiting for NFC tag tap.
 */
@Composable
fun NfcWaitingOverlay(
    vendorName: String,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.92f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp).widthIn(max = 520.dp)
        ) {
            MctxModeBadge("Sessione NFC")
            Spacer(modifier = Modifier.height(20.dp))
            NfcRingsAnimation(
                color = MaterialTheme.colorScheme.primary,
                isWriting = false,
                size = 180.dp
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Avvicina il Tag",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tieni il tag fermo vicino al telefono\nper programmare «$vendorName»",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            NfcProgressStepper(activeStep = 0)
            Spacer(modifier = Modifier.height(32.dp))
            OutlinedButton(
                onClick = onCancel,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Text("Annulla")
            }
        }
    }
}

/**
 * Overlay shown during pre-flight verification.
 */
@Composable
private fun VerifyingOverlay(
    message: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.92f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp).widthIn(max = 520.dp)) {
            NfcProgressStepper(activeStep = 0)
            Spacer(modifier = Modifier.height(24.dp))
            NfcRingsAnimation(
                color = MaterialTheme.colorScheme.tertiary,
                isWriting = true,
                size = 180.dp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                message,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Non rimuovere il tag",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * Overlay shown during the write operation.
 */
@Composable
private fun WritingOverlay(
    modifier: Modifier = Modifier,
    progressMessage: String = "Scrittura in corso..."
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.92f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp).widthIn(max = 520.dp)) {
            NfcProgressStepper(activeStep = 1)
            Spacer(modifier = Modifier.height(24.dp))
            NfcRingsAnimation(
                color = MaterialTheme.colorScheme.primary,
                isWriting = true,
                size = 180.dp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                progressMessage,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Non rimuovere il tag",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * Convert a PreflightResult failure into user-facing Italian text.
 * Internal so it can be reused by AutoModeScreen.
 */
internal fun preflightResultToUiText(reason: PreflightResult): Pair<String, String> {
    return when (reason) {
        is PreflightResult.TagNotSupported -> Pair(
            "Tag Non Supportato",
            "Questo tag non è un MIFARE Classic.\nVerifica di usare il tag corretto."
        )
        is PreflightResult.TagTypeMismatch -> Pair(
            "Tag Incompatibile",
            "Atteso: ${reason.expectedType}\n" +
                "Tag rilevato: ${reason.actualSectorCount} settori.\n" +
                "Il tag ha meno settori di quelli richiesti."
        )
        is PreflightResult.KeyAuthFailed -> Pair(
            "Chiavi Errate",
            "Autenticazione fallita su ${reason.failedSectors.size}/${reason.totalSectors} settori.\n" +
                "Settori falliti: ${reason.failedSectors.joinToString(", ")}.\n" +
                "Verifica le chiavi nella configurazione del Vendor."
        )
        is PreflightResult.NoKeysConfigured -> Pair(
            "Nessuna Chiave Configurata",
            "Il Vendor non ha chiavi configurate.\n" +
                "Vai in \"Configura\" per aggiungere le chiavi dei settori."
        )
        is PreflightResult.NoPayloadConfigured -> Pair(
            "Nessun Payload Configurato",
            "Il Vendor non ha blocchi da scrivere.\n" +
                "Vai in \"Configura\" per aggiungere i dati da scrivere."
        )
        is PreflightResult.UnsafePayload -> Pair(
            "Payload Bloccato",
            "La configurazione tenta di scrivere aree protette o non valide:\n" +
                reason.violations.joinToString("\n") { "• $it" }
        )
        is PreflightResult.TagLost -> Pair(
            "Tag Rimosso",
            "Il tag è stato allontanato durante la verifica.\n" +
                "Riprova tenendo il tag fermo vicino al telefono."
        )
        is PreflightResult.ConnectionError -> Pair(
            "Errore di Connessione",
            "Impossibile comunicare con il tag.\n${reason.message}"
        )
        is PreflightResult.Ready -> Pair(
            "Pronto",
            "Il tag è verificato e pronto per la scrittura."
        )
    }
}

/**
 * Overlay showing the result of the write operation.
 * Handles Success, Partial, Error, and PreflightFailed.
 */
@Composable
private fun WriteResultOverlay(
    result: WriteOperationResult,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Haptic design system: double tap for success, long+short for error
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(result) {
        when (result) {
            is WriteOperationResult.Success -> {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                kotlinx.coroutines.delay(120)
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            is WriteOperationResult.Partial -> {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            else -> {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                kotlinx.coroutines.delay(80)
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        }
    }

    val (icon, iconColor, title, description) = when (result) {
        is WriteOperationResult.Success -> Quad(
            Icons.Filled.CheckCircle,
            Color(0xFF4CAF50),
            "Scrittura Completata! ✓",
            "${result.blocksWritten}/${result.totalBlocks} blocchi scritti con successo"
        )
        is WriteOperationResult.Partial -> Quad(
            Icons.Filled.Warning,
            Color(0xFFF57F17),
            "Scrittura Parziale ⚠",
            "${result.blocksWritten}/${result.totalBlocks} blocchi scritti.\n${result.failures.size} blocchi falliti."
        )
        is WriteOperationResult.Error -> Quad(
            Icons.Filled.Error,
            Color(0xFFC62828),
            "Errore di Scrittura ✗",
            result.message
        )
        is WriteOperationResult.PreflightFailed -> {
            val (pfTitle, pfDesc) = preflightResultToUiText(result.reason)
            Quad(
                Icons.Filled.GppBad,
                Color(0xFFE65100),
                pfTitle,
                pfDesc
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.92f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp)
                .padding(32.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                NfcProgressStepper(activeStep = 2)
                Spacer(modifier = Modifier.height(18.dp))
                if (result is WriteOperationResult.Success) {
                    NfcRingsAnimation(
                        color = iconColor,
                        isWriting = false,
                        isDone = true,
                        size = 120.dp
                    )
                } else {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(64.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                // Show failed blocks for partial results
                if (result is WriteOperationResult.Partial) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFC62828).copy(alpha = 0.08f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            result.failures.forEach { failure ->
                                Text(
                                    text = "Settore ${failure.sector}, Blocco ${failure.block}: ${failure.errorMessage}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFC62828)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Chiudi")
                }
            }
        }
    }
}

/** Simple data holder for 4 values (destructuring). */
private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

/**
 * Overlay showing the result of the test keys preflight operation.
 */
@Composable
private fun TestKeysResultOverlay(
    preflight: PreflightResult,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(preflight) {
        if (preflight is PreflightResult.Ready) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            kotlinx.coroutines.delay(120)
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        } else {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            kotlinx.coroutines.delay(80)
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    val (icon, iconColor, title, description) = when (preflight) {
        is PreflightResult.Ready -> Quad(
            Icons.Filled.CheckCircle,
            Color(0xFF4CAF50),
            "Chiavi Verificate! ✓",
            preflightResultToUiText(preflight).second
        )
        else -> {
            val (pfTitle, pfDesc) = preflightResultToUiText(preflight)
            Quad(
                Icons.Filled.GppBad,
                Color(0xFFE65100),
                pfTitle,
                pfDesc
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.92f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp)
                .padding(32.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (preflight is PreflightResult.Ready) {
                    NfcRingsAnimation(
                        color = iconColor,
                        isWriting = false,
                        isDone = true,
                        size = 120.dp
                    )
                } else {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(64.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Chiudi")
                }
            }
        }
    }
}
