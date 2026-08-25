package de.syss.MifareClassicTool.ui.adminmode

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Nfc
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.syss.MifareClassicTool.data.model.UidEntry
import de.syss.MifareClassicTool.data.model.VendorEntity
import de.syss.MifareClassicTool.ui.components.MctxModeBadge
import de.syss.MifareClassicTool.ui.components.MctxStatusBanner
import de.syss.MifareClassicTool.ui.components.PremiumScreenBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UidManagerScreen(
    onBackClick: () -> Unit,
    viewModel: UidManagerViewModel = viewModel()
) {
    val uids by viewModel.uids.collectAsStateWithLifecycle(initialValue = emptyList())
    val vendors by viewModel.vendors.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Gestione UID")
                        Text("Associazioni Auto Mode", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Indietro")
                    }
                },
                actions = { MctxModeBadge("Admin", modifier = Modifier.padding(end = 12.dp)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Aggiungi UID")
            }
        }
    ) { padding ->
        PremiumScreenBackground(modifier = Modifier.padding(padding)) {
            if (uids.isEmpty()) {
                EmptyUidState(modifier = Modifier.fillMaxSize())
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().widthIn(max = 840.dp).align(Alignment.TopCenter),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        MctxStatusBanner(
                            title = "${uids.size} UID registrat${if (uids.size == 1) "o" else "i"}",
                            message = "Ogni UID può selezionare un solo vendor in Auto Mode.",
                            icon = Icons.Filled.Link
                        )
                    }
                    items(uids, key = { it.uid }) { entry ->
                        UidCard(
                            entry = entry,
                            currentVendor = viewModel.vendorForId(entry.vendorId),
                            vendors = vendors,
                            onReassociate = { newVendorId, newLabel -> viewModel.reassociate(entry.uid, newVendorId, newLabel) },
                            onDelete = { viewModel.delete(entry.uid) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        ManualUidDialog(
            vendors = vendors,
            onDismiss = { showAddDialog = false },
            onConfirm = { uid, label, vendorId ->
                viewModel.addManualUid(uid, label, vendorId)
                showAddDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UidCard(
    entry: UidEntry,
    currentVendor: VendorEntity?,
    vendors: List<VendorEntity>,
    onReassociate: (vendorId: String, label: String) -> Unit,
    onDelete: () -> Unit
) {
    var showReassignDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // UID + label + vendor info
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = entry.uid.uppercase().chunked(2).joinToString(" "),
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    entry.label?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                if (currentVendor != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Filled.Link,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            currentVendor.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        currentVendor.subtitle?.let {
                            Text(
                                "· $it",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Text(
                        "Vendor non trovato (ID: ${entry.vendorId.take(8)}…)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            // Actions
            IconButton(onClick = { showReassignDialog = true }) {
                Icon(Icons.Filled.Edit, "Modifica associazione", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(Icons.Filled.Delete, "Elimina", tint = MaterialTheme.colorScheme.error)
            }
        }
    }

    // ── Reassign dialog ──
    if (showReassignDialog) {
        ReassignDialog(
            uid = entry.uid,
            currentLabel = entry.label,
            currentVendorId = entry.vendorId,
            vendors = vendors,
            onConfirm = { newVendorId, newLabel ->
                onReassociate(newVendorId, newLabel)
                showReassignDialog = false
            },
            onDismiss = { showReassignDialog = false }
        )
    }

    // ── Delete confirm dialog ──
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            icon = { Icon(Icons.Filled.DeleteForever, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Elimina UID?") },
            text = {
                Text(
                    "L'UID ${entry.uid.uppercase().chunked(2).joinToString(" ")} " +
                    "non verrà più riconosciuto in Auto Mode."
                )
            },
            confirmButton = {
                Button(
                    onClick = { onDelete(); showDeleteConfirm = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Elimina") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Annulla") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReassignDialog(
    uid: String,
    currentLabel: String?,
    currentVendorId: String,
    vendors: List<VendorEntity>,
    onConfirm: (vendorId: String, label: String) -> Unit,
    onDismiss: () -> Unit
) {
    var selected by remember {
        mutableStateOf(vendors.find { it.id == currentVendorId })
    }
    var labelText by remember { mutableStateOf(currentLabel ?: "") }
    var dropdownExpanded by remember { mutableStateOf(false) }
    val isLabelValid = labelText.trim().isNotEmpty() && labelText.length <= 50

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Edit, null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("Modifica associazione") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        uid.uppercase().chunked(2).joinToString(" "),
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
                ExposedDropdownMenuBox(
                    expanded = dropdownExpanded,
                    onExpandedChange = { dropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selected?.name ?: "Seleziona Vendor…",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Vendor") },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
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
                                            Text(
                                                it,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                },
                                onClick = { selected = vendor; dropdownExpanded = false },
                                leadingIcon = if (vendor.id == currentVendorId) {
                                    { Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.primary) }
                                } else null
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = labelText,
                    onValueChange = { newValue ->
                        if (newValue.length <= 50) {
                            labelText = newValue
                        }
                    },
                    label = { Text("Label Proprietario") },
                    placeholder = { Text("es. Chiave Fabio") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = labelText.isNotEmpty() && !isLabelValid,
                    supportingText = {
                        Text("${labelText.length}/50")
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { selected?.let { onConfirm(it.id, labelText.trim()) } },
                enabled = selected != null && isLabelValid && (selected?.id != currentVendorId || labelText.trim() != currentLabel)
            ) { Text("Salva") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annulla") }
        }
    )
}

@Composable
private fun EmptyUidState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Outlined.Nfc,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Nessun UID registrato",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Usa Auto Mode e avvicina un tag:\nverrà chiesto di associarlo a un Vendor.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualUidDialog(
    vendors: List<VendorEntity>,
    onDismiss: () -> Unit,
    onConfirm: (uid: String, label: String, vendorId: String) -> Unit
) {
    var uidText by remember { mutableStateOf("") }
    var labelText by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var selectedVendor by remember { mutableStateOf<VendorEntity?>(null) }

    // Light validation: allow only hex chars and spaces, remove spaces for validation
    val isUidValid = uidText.replace(Regex("\\s+"), "").matches(Regex("^[0-9A-Fa-f]+$"))
    val isLabelValid = labelText.trim().isNotEmpty() && labelText.length <= 50

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Inserimento Manuale UID") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = uidText,
                    onValueChange = { newValue ->
                        uidText = newValue.uppercase()
                    },
                    label = { Text("UID (Esadecimale)") },
                    placeholder = { Text("es. 045A6112") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = uidText.isNotEmpty() && !isUidValid,
                    supportingText = {
                        if (uidText.isNotEmpty() && !isUidValid) {
                            Text("Solo caratteri esadecimali (0-9, A-F)")
                        }
                    }
                )

                OutlinedTextField(
                    value = labelText,
                    onValueChange = { newValue ->
                        if (newValue.length <= 50) {
                            labelText = newValue
                        }
                    },
                    label = { Text("Label Proprietario") },
                    placeholder = { Text("es. Chiave Fabio") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = labelText.isNotEmpty() && !isLabelValid,
                    supportingText = {
                        Text("${labelText.length}/50")
                    }
                )

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedVendor?.name ?: "Seleziona Vendor",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Vendor") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier.menuAnchor(
                            type = MenuAnchorType.PrimaryNotEditable,
                            enabled = true
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        if (vendors.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("Nessun vendor disponibile") },
                                onClick = { expanded = false }
                            )
                        } else {
                            vendors.forEach { vendor ->
                                DropdownMenuItem(
                                    text = { Text(vendor.name) },
                                    onClick = {
                                        selectedVendor = vendor
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val cleanUid = uidText.replace(Regex("\\s+"), "")
                    selectedVendor?.let {
                        onConfirm(cleanUid, labelText.trim(), it.id)
                    }
                },
                enabled = isUidValid && uidText.isNotEmpty() && isLabelValid && selectedVendor != null
            ) {
                Text("Associa e Salva")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla")
            }
        }
    )
}
