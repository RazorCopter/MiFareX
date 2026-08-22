package de.syss.MifareClassicTool.ui.adminmode

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import de.syss.MifareClassicTool.data.model.TagType
import de.syss.MifareClassicTool.data.model.VendorCategory
import de.syss.MifareClassicTool.ui.components.getCategoryColor
import de.syss.MifareClassicTool.ui.components.getCategoryIcon

/**
 * Screen for creating or editing a Vendor configuration.
 * Includes form fields for name, category, keys, and payload blocks.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorEditorScreen(
    vendorId: String?,
    onBackClick: () -> Unit,
    onSaved: () -> Unit,
    onDeleted: () -> Unit = onSaved,
    viewModel: VendorEditorViewModel = viewModel()
) {
    val isEditing = vendorId != null
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val iconPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { viewModel.onIconPicked(it) } }

    LaunchedEffect(vendorId) {
        vendorId?.let { viewModel.loadVendor(it) }
    }

    LaunchedEffect(viewModel.isSaved) {
        if (viewModel.isSaved) onSaved()
    }

    LaunchedEffect(viewModel.isDeleted) {
        if (viewModel.isDeleted) onDeleted()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (isEditing) "Modifica Vendor" else "Nuovo Vendor")
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Indietro")
                    }
                },
                actions = {
                    if (isEditing) {
                        IconButton(
                            onClick = { showDeleteConfirm = true },
                            enabled = !viewModel.isLoading
                        ) {
                            Icon(
                                Icons.Filled.DeleteForever,
                                contentDescription = "Elimina Vendor",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    TextButton(
                        onClick = { viewModel.saveVendor() },
                        enabled = viewModel.name.isNotBlank() && !viewModel.isLoading
                    ) {
                        Text("Salva", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    ) { padding ->
        if (viewModel.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // === Icon Section ===
                VendorIconSection(
                    iconUri = viewModel.iconUri,
                    category = viewModel.category,
                    onPickIconClick = { iconPickerLauncher.launch("image/*") },
                    onRemoveIconClick = { viewModel.removeIcon() }
                )

                HorizontalDivider()

                // === Basic Info Section ===
                SectionHeader("Informazioni Base")

                OutlinedTextField(
                    value = viewModel.name,
                    onValueChange = { viewModel.name = it },
                    label = { Text("Nome Vendor *") },
                    placeholder = { Text("es. Autolavaggio Mario") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                )

                OutlinedTextField(
                    value = viewModel.subtitle,
                    onValueChange = { viewModel.subtitle = it },
                    label = { Text("Sottotitolo") },
                    placeholder = { Text("es. Via Roma 42, Milano") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = viewModel.notes,
                    onValueChange = { viewModel.notes = it },
                    label = { Text("Note") },
                    placeholder = { Text("Note aggiuntive...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )

                // === Category Selector ===
                SectionHeader("Categoria")

                var categoryExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it }
                ) {
                    OutlinedTextField(
                        value = "${viewModel.category.emoji} ${viewModel.category.displayName}",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) }
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        VendorCategory.entries.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text("${cat.emoji} ${cat.displayName}") },
                                onClick = {
                                    viewModel.category = cat
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                // === Tag Type Selector ===
                SectionHeader("Tipo Tag")

                var tagTypeExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = tagTypeExpanded,
                    onExpandedChange = { tagTypeExpanded = it }
                ) {
                    OutlinedTextField(
                        value = viewModel.tagType.displayName,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = tagTypeExpanded) }
                    )
                    ExposedDropdownMenu(
                        expanded = tagTypeExpanded,
                        onDismissRequest = { tagTypeExpanded = false }
                    ) {
                        TagType.entries.forEach { tt ->
                            DropdownMenuItem(
                                text = { Text("${tt.displayName} (${tt.sectorCount} settori)") },
                                onClick = {
                                    viewModel.tagType = tt
                                    tagTypeExpanded = false
                                }
                            )
                        }
                    }
                }

                HorizontalDivider()

                // === Sector Keys Section ===
                SectorKeysSection(viewModel)

                HorizontalDivider()

                // === Payload Blocks Section ===
                PayloadBlocksSection(viewModel)

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            icon = {
                Icon(Icons.Filled.DeleteForever, null, tint = MaterialTheme.colorScheme.error)
            },
            title = { Text("Elimina Vendor?") },
            text = {
                Text(
                    "Questa azione è irreversibile. Il vendor \"${viewModel.name}\" " +
                    "verrà eliminato insieme a tutte le associazioni UID collegate."
                )
            },
            confirmButton = {
                Button(
                    onClick = { showDeleteConfirm = false; viewModel.deleteVendor() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Elimina") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Annulla") }
            }
        )
    }
}

@Composable
private fun VendorIconSection(
    iconUri: String?,
    category: VendorCategory,
    onPickIconClick: () -> Unit,
    onRemoveIconClick: () -> Unit
) {
    val categoryColor = getCategoryColor(category)

    SectionHeader("Icona Vendor")

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Circular preview (custom image or fallback icon)
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            categoryColor.copy(alpha = 0.7f),
                            categoryColor.copy(alpha = 0.3f)
                        )
                    )
                )
                .border(2.dp, categoryColor.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (!iconUri.isNullOrBlank()) {
                AsyncImage(
                    model = iconUri,
                    contentDescription = "Icona vendor",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                )
            } else {
                Icon(
                    imageVector = getCategoryIcon(category),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledTonalButton(
                onClick = onPickIconClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.AddPhotoAlternate, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (iconUri != null) "Cambia icona" else "Scegli icona")
            }
            if (!iconUri.isNullOrBlank()) {
                OutlinedButton(
                    onClick = onRemoveIconClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Filled.DeleteOutline, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Rimuovi icona")
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
private fun SectorKeysSection(viewModel: VendorEditorViewModel) {
    SectionHeader("Chiavi per Settore (Keys)")

    // Existing keys (index-based for multi-key support per sector)
    viewModel.sectorKeys.forEachIndexed { index, key ->
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Settore ${key.sector}  ·  #${index + 1}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    key.keyA?.let {
                        Text("Key A: $it", style = MaterialTheme.typography.bodySmall,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                    }
                    key.keyB?.let {
                        Text("Key B: $it", style = MaterialTheme.typography.bodySmall,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                    }
                }
                IconButton(onClick = { viewModel.removeSectorKeyAt(index) }) {
                    Icon(Icons.Filled.Delete, "Rimuovi", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    // Add key form
    var newKeySector by remember { mutableStateOf("") }
    var newKeyA by remember { mutableStateOf("") }
    var newKeyB by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val hexRegex = remember { Regex("^[0-9A-Fa-f]*$") }
            val keyAError = newKeyA.isNotBlank() && (newKeyA.length != 12 || !newKeyA.matches(hexRegex))
            val keyBError = newKeyB.isNotBlank() && (newKeyB.length != 12 || !newKeyB.matches(hexRegex))

            OutlinedTextField(
                value = newKeySector,
                onValueChange = { newKeySector = it },
                label = { Text("Settore") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            OutlinedTextField(
                value = newKeyA,
                onValueChange = { if (it.length <= 12) newKeyA = it.uppercase() },
                label = { Text("Key A (12 hex)") },
                placeholder = { Text("FFFFFFFFFFFF") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = keyAError,
                supportingText = if (keyAError) {
                    { Text("Deve essere esattamente 12 caratteri hex") }
                } else null
            )
            OutlinedTextField(
                value = newKeyB,
                onValueChange = { if (it.length <= 12) newKeyB = it.uppercase() },
                label = { Text("Key B (12 hex)") },
                placeholder = { Text("FFFFFFFFFFFF") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = keyBError,
                supportingText = if (keyBError) {
                    { Text("Deve essere esattamente 12 caratteri hex") }
                } else null
            )
            Button(
                onClick = {
                    val sector = newKeySector.toIntOrNull() ?: return@Button
                    viewModel.addSectorKey(
                        sector,
                        newKeyA.ifBlank { null },
                        newKeyB.ifBlank { null }
                    )
                    newKeySector = ""; newKeyA = ""; newKeyB = ""
                },
                modifier = Modifier.align(Alignment.End),
                enabled = newKeySector.isNotBlank()
                        && (newKeyA.isNotBlank() || newKeyB.isNotBlank())
                        && !keyAError && !keyBError
            ) {
                Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Aggiungi Chiave")
            }
        }
    }
}

@Composable
private fun PayloadBlocksSection(viewModel: VendorEditorViewModel) {
    SectionHeader("Blocchi da Scrivere (Payload)")

    // Existing blocks
    viewModel.writeBlocks.forEachIndexed { index, block ->
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "S${block.sector} B${block.block}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        block.data,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
                IconButton(onClick = { viewModel.removeWriteBlock(index) }) {
                    Icon(Icons.Filled.Delete, "Rimuovi", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    // Add block form
    var newBlockSector by remember { mutableStateOf("") }
    var newBlockBlock by remember { mutableStateOf("") }
    var newBlockData by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = newBlockSector,
                    onValueChange = { newBlockSector = it },
                    label = { Text("Settore") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = newBlockBlock,
                    onValueChange = { newBlockBlock = it },
                    label = { Text("Blocco") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
            OutlinedTextField(
                value = newBlockData,
                onValueChange = { newBlockData = it.uppercase() },
                label = { Text("Dati (32 hex chars)") },
                placeholder = { Text("00000000000000000000000000000000") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Button(
                onClick = {
                    val sector = newBlockSector.toIntOrNull() ?: return@Button
                    val block = newBlockBlock.toIntOrNull() ?: return@Button
                    viewModel.addWriteBlock(sector, block, newBlockData)
                    newBlockSector = ""; newBlockBlock = ""; newBlockData = ""
                },
                modifier = Modifier.align(Alignment.End),
                enabled = newBlockSector.isNotBlank() && newBlockBlock.isNotBlank() && newBlockData.length == 32
            ) {
                Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Aggiungi Blocco")
            }
        }
    }
}
