package de.syss.MifareClassicTool.ui.history

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.syss.MifareClassicTool.data.model.OperationLogEntity
import de.syss.MifareClassicTool.data.model.OperationOutcome
import de.syss.MifareClassicTool.data.model.OperationSource
import de.syss.MifareClassicTool.data.model.OperationType
import de.syss.MifareClassicTool.ui.components.MctxModeBadge
import de.syss.MifareClassicTool.ui.components.PremiumScreenBackground
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class HistoryTypeFilter(val label: String, val accepted: Set<OperationType>?) {
    ALL("Tutte", null),
    WRITES("Scritture", setOf(OperationType.MANUAL_WRITE, OperationType.AUTO_WRITE)),
    READS("Letture", setOf(OperationType.READ)),
    TESTS("Test", setOf(OperationType.KEY_TEST)),
    SIMULATIONS("Simulazioni", setOf(OperationType.DRY_RUN))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OperationHistoryScreen(
    onBackClick: () -> Unit,
    viewModel: OperationHistoryViewModel = viewModel()
) {
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    var showClearDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cronologia operazioni") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                },
                actions = {
                    if (entries.isNotEmpty()) {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(Icons.Filled.DeleteSweep, contentDescription = "Cancella cronologia")
                        }
                    }
                }
            )
        }
    ) { padding ->
        PremiumScreenBackground(modifier = Modifier.padding(padding)) {
            OperationHistoryContent(entries = entries)
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Cancellare la cronologia?") },
            text = { Text("Verranno rimossi solo i metadati operativi locali. Vendor e configurazioni non saranno modificati.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearHistory()
                    showClearDialog = false
                }) { Text("Cancella") }
            },
            dismissButton = { TextButton(onClick = { showClearDialog = false }) { Text("Annulla") } }
        )
    }
}

@Composable
fun OperationHistoryContent(
    entries: List<OperationLogEntity>,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    var typeFilter by remember { mutableStateOf(HistoryTypeFilter.ALL) }
    var outcomeFilter by remember { mutableStateOf<OperationOutcome?>(null) }
    val filtered = remember(entries, query, typeFilter, outcomeFilter) {
        entries.filter { entry ->
            val typeMatches = typeFilter.accepted?.contains(entry.type) != false
            val outcomeMatches = outcomeFilter == null || entry.outcome == outcomeFilter
            val textMatches = query.isBlank() || listOfNotNull(
                entry.vendorName,
                entry.summary,
                entry.uidSuffix
            ).any { it.contains(query.trim(), ignoreCase = true) }
            typeMatches && outcomeMatches && textMatches
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Audit locale", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "Nessuna chiave, payload o dump viene archiviato",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                MctxModeBadge("${entries.size} eventi")
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                placeholder = { Text("Cerca vendor, esito o UID parziale") },
                singleLine = true
            )
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HistoryTypeFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = typeFilter == filter,
                        onClick = { typeFilter = filter },
                        label = { Text(filter.label) }
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(selected = outcomeFilter == null, onClick = { outcomeFilter = null }, label = { Text("Ogni esito") })
                OperationOutcome.entries.forEach { outcome ->
                    FilterChip(
                        selected = outcomeFilter == outcome,
                        onClick = { outcomeFilter = outcome },
                        label = { Text(outcome.displayName) }
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Filled.History, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(16.dp))
                Text(if (entries.isEmpty()) "Nessuna operazione registrata" else "Nessun risultato per questi filtri", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text(
                    if (entries.isEmpty()) "Le prossime letture, verifiche e scritture appariranno qui." else "Modifica ricerca, tipo o stato.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtered, key = { it.id }) { entry -> OperationLogCard(entry) }
            }
        }
    }
}

@Composable
private fun OperationLogCard(entry: OperationLogEntity) {
    var expanded by remember(entry.id) { mutableStateOf(false) }
    val outcomeColor = when (entry.outcome) {
        OperationOutcome.SUCCESS -> Color(0xFF2E7D32)
        OperationOutcome.PARTIAL -> Color(0xFFB26A00)
        OperationOutcome.FAILED -> MaterialTheme.colorScheme.error
        OperationOutcome.BLOCKED -> MaterialTheme.colorScheme.tertiary
    }
    val typeIcon: ImageVector = when (entry.type) {
        OperationType.MANUAL_WRITE -> Icons.Filled.Nfc
        OperationType.AUTO_WRITE -> Icons.Filled.AutoMode
        OperationType.READ -> Icons.Filled.Visibility
        OperationType.KEY_TEST -> Icons.Filled.Key
        OperationType.DRY_RUN -> Icons.Filled.CheckCircle
    }

    Card(
        onClick = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = MaterialTheme.shapes.medium, color = outcomeColor.copy(alpha = 0.12f)) {
                    Icon(typeIcon, contentDescription = null, tint = outcomeColor, modifier = Modifier.padding(9.dp).size(21.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(entry.type.displayName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        entry.vendorName ?: if (entry.source == OperationSource.AUTO_MODE) "Auto Mode" else "Operazione locale",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Riduci dettagli" else "Mostra dettagli"
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    when (entry.outcome) {
                        OperationOutcome.SUCCESS -> Icons.Filled.CheckCircle
                        OperationOutcome.PARTIAL -> Icons.Filled.RemoveCircle
                        else -> Icons.Filled.Error
                    },
                    contentDescription = null,
                    tint = outcomeColor,
                    modifier = Modifier.size(16.dp)
                )
                Text(entry.summary, style = MaterialTheme.typography.bodyMedium)
            }
            Text(formatTimestamp(entry.timestamp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (expanded) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("Esito · ${entry.outcome.displayName}", style = MaterialTheme.typography.bodySmall)
                        entry.uidSuffix?.let { Text("UID · $it", style = MaterialTheme.typography.bodySmall) }
                        entry.durationMillis?.let { Text("Durata · ${it} ms", style = MaterialTheme.typography.bodySmall) }
                        if (entry.blocksAttempted != null) {
                            Text("Operazioni · ${entry.blocksCompleted ?: 0}/${entry.blocksAttempted}", style = MaterialTheme.typography.bodySmall)
                        }
                        entry.technicalDetails?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String =
    SimpleDateFormat("dd MMM yyyy · HH:mm:ss", Locale.ITALIAN).format(Date(timestamp))
