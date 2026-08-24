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
import de.syss.MifareClassicTool.ui.components.HexVisualTransformation
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
import de.syss.MifareClassicTool.ui.components.MctxModeBadge
import de.syss.MifareClassicTool.ui.components.PremiumScreenBackground

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
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(if (isEditing) "Modifica Vendor" else "Nuovo Vendor")
                        Text("Configurazione profilo", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
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
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            )
        }
    ) { padding ->
        PremiumScreenBackground(modifier = Modifier.padding(padding)) {
            if (viewModel.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 840.dp)
                        .align(Alignment.TopCenter)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    MctxModeBadge(if (isEditing) "Admin · Modifica" else "Admin · Nuovo profilo")
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
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
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
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SectorKeysSection(viewModel: VendorEditorViewModel) {
    SectionHeader("Chiavi per Settore (Keys)")

    // Existing keys with SwipeToDismiss
    viewModel.sectorKeys.forEach { keyItem ->
        key(System.identityHashCode(keyItem)) {
            val dismissState = rememberSwipeToDismissBoxState(
                confirmValueChange = {
                    if (it == SwipeToDismissBoxValue.EndToStart) {
                        val idx = viewModel.sectorKeys.indexOf(keyItem)
                        if (idx >= 0) viewModel.removeSectorKeyAt(idx)
                        true
                    } else false
                }
            )
            SwipeToDismissBox(
                state = dismissState,
                enableDismissFromStartToEnd = false,
            backgroundContent = {
                val color by animateColorAsState(
                    when (dismissState.targetValue) {
                        SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                        else -> Color.Transparent
                    }
                )
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(vertical = 4.dp)
                        .background(color, RoundedCornerShape(8.dp))
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Rimuovi",
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "Settore ${keyItem.sector}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            keyItem.label?.let { lbl ->
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        lbl,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        keyItem.keyA?.let {
                            Text("Key A: $it", style = MaterialTheme.typography.bodySmall,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                        }
                        keyItem.keyB?.let {
                            Text("Key B: $it", style = MaterialTheme.typography.bodySmall,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                        }
                    }
                    }
                }
            }
        }
    }

    var showAddKeySheet by remember { mutableStateOf(false) }

    OutlinedButton(
        onClick = { showAddKeySheet = true },
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
    ) {
        Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("Aggiungi Chiave")
    }

    if (showAddKeySheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddKeySheet = false }
        ) {
            var newKeySector by remember { mutableStateOf("") }
            var newKeyA by remember { mutableStateOf("") }
            var newKeyB by remember { mutableStateOf("") }
            var newKeyLabel by remember { mutableStateOf("") }

            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .padding(bottom = 32.dp), // Extra padding for navigation bar
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Nuova Chiave",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                val hexRegex = remember { Regex("^[0-9A-Fa-f]*$") }
                val parsedSector = newKeySector.toIntOrNull()
                val sectorError = newKeySector.isNotBlank() && (parsedSector == null || parsedSector !in 0..39)
                val keyAError = newKeyA.isNotBlank() && (newKeyA.length != 12 || !newKeyA.matches(hexRegex))
                val keyBError = newKeyB.isNotBlank() && (newKeyB.length != 12 || !newKeyB.matches(hexRegex))

                OutlinedTextField(
                    value = newKeyLabel,
                    onValueChange = { newKeyLabel = it },
                    label = { Text("Descrizione") },
                    placeholder = { Text("es. Tessera Blu, Card Mario…") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                )
                
                OutlinedTextField(
                    value = newKeySector,
                    onValueChange = { newKeySector = it },
                    label = { Text("Settore") },
                    placeholder = { Text("0") },
                    isError = sectorError,
                    supportingText = {
                        if (sectorError) {
                            Text("Deve essere 0-39", color = MaterialTheme.colorScheme.error)
                        } else {
                            Text("0-based (0 = 1° settore)")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newKeyA,
                        onValueChange = { 
                            val stripped = it.replace(" ", "").uppercase()
                            if (stripped.length <= 12 && stripped.matches(hexRegex)) newKeyA = stripped 
                        },
                        label = { Text("Key A (opzionale)") },
                        placeholder = { Text("FFFFFFFFFFFF") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        isError = keyAError,
                        visualTransformation = HexVisualTransformation(),
                        supportingText = if (keyAError) {
                            { Text("12 hex") }
                        } else null
                    )
                    OutlinedTextField(
                        value = newKeyB,
                        onValueChange = { 
                            val stripped = it.replace(" ", "").uppercase()
                            if (stripped.length <= 12 && stripped.matches(hexRegex)) newKeyB = stripped 
                        },
                        label = { Text("Key B (obbligatoria)") },
                        placeholder = { Text("FFFFFFFFFFFF") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        isError = keyBError,
                        visualTransformation = HexVisualTransformation(),
                        supportingText = if (keyBError) {
                            { Text("12 hex") }
                        } else null
                    )
                }
                
                Button(
                    onClick = {
                        val sector = newKeySector.toIntOrNull() ?: return@Button
                        viewModel.addSectorKey(
                            sector,
                            newKeyA.ifBlank { null },
                            newKeyB.ifBlank { null },
                            newKeyLabel.ifBlank { null }
                        )
                        showAddKeySheet = false
                    },
                    modifier = Modifier.align(Alignment.End),
                    enabled = newKeySector.isNotBlank()
                            && !sectorError
                            && newKeyB.isNotBlank()
                            && !keyBError
                            && !keyAError
                ) {
                    Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Aggiungi")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PayloadBlocksSection(viewModel: VendorEditorViewModel) {
    SectionHeader("Blocchi da Scrivere (Payload)")

    // Existing blocks with SwipeToDismiss
    viewModel.writeBlocks.forEach { blockItem ->
        key(System.identityHashCode(blockItem)) {
            val dismissState = rememberSwipeToDismissBoxState(
                confirmValueChange = {
                    if (it == SwipeToDismissBoxValue.EndToStart) {
                        val idx = viewModel.writeBlocks.indexOf(blockItem)
                        if (idx >= 0) viewModel.removeWriteBlock(idx)
                        true
                    } else false
                }
            )
            SwipeToDismissBox(
                state = dismissState,
                enableDismissFromStartToEnd = false,
            backgroundContent = {
                val color by animateColorAsState(
                    when (dismissState.targetValue) {
                        SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                        else -> Color.Transparent
                    }
                )
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(vertical = 4.dp)
                        .background(color, RoundedCornerShape(8.dp))
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Rimuovi",
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "S${blockItem.sector} B${blockItem.block}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            blockItem.data,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                    }
                }
            }
        }
    }

    var showAddBlockSheet by remember { mutableStateOf(false) }

    OutlinedButton(
        onClick = { showAddBlockSheet = true },
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
    ) {
        Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("Aggiungi Blocco")
    }

    if (showAddBlockSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddBlockSheet = false }
        ) {
            var newBlockSector by remember { mutableStateOf("") }
            var newBlockBlock by remember { mutableStateOf("") }
            var newBlockData by remember { mutableStateOf("") }

            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Nuovo Blocco Payload",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                val hexRegex = remember { Regex("^[0-9A-Fa-f]*$") }
                val parsedSector = newBlockSector.toIntOrNull()
                val sectorError = newBlockSector.isNotBlank() && (parsedSector == null || parsedSector !in 0..39)
                val parsedBlock = newBlockBlock.toIntOrNull()
                val blockError = newBlockBlock.isNotBlank() && (parsedBlock == null || parsedBlock !in 0..2)
                val dataError = newBlockData.isNotBlank() && (newBlockData.length != 32 || !newBlockData.matches(hexRegex))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newBlockSector,
                        onValueChange = { newBlockSector = it },
                        label = { Text("Settore") },
                        placeholder = { Text("0") },
                        isError = sectorError,
                        supportingText = {
                            if (sectorError) {
                                Text("Deve essere 0-39", color = MaterialTheme.colorScheme.error)
                            } else {
                                Text("0-based (0 = 1° settore)")
                            }
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = newBlockBlock,
                        onValueChange = { if (it.length <= 1) newBlockBlock = it },
                        label = { Text("Blocco (0, 1, 2)") },
                        placeholder = { Text("0, 1, 2") },
                        isError = blockError,
                        supportingText = {
                            if (blockError) {
                                Text("Solo 0, 1, 2! (3 è il Trailer)", color = MaterialTheme.colorScheme.error)
                            } else {
                                Text("Solo blocchi dati 0, 1, 2")
                            }
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
                OutlinedTextField(
                    value = newBlockData,
                    onValueChange = { 
                        val stripped = it.replace(" ", "").uppercase()
                        if (stripped.length <= 32 && stripped.matches(hexRegex)) newBlockData = stripped 
                    },
                    label = { Text("Dati (32 hex chars)") },
                    placeholder = { Text("00000000000000000000000000000000") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = dataError,
                    visualTransformation = HexVisualTransformation(),
                    supportingText = if (dataError) {
                        { Text("Deve essere esattamente 32 caratteri hex (0-9, A-F)") }
                    } else {
                        { Text("${newBlockData.length}/32 caratteri") }
                    }
                )
                Button(
                    onClick = {
                        val sector = newBlockSector.toIntOrNull() ?: return@Button
                        val block = newBlockBlock.toIntOrNull() ?: return@Button
                        if (block in 0..2) {
                            viewModel.addWriteBlock(sector, block, newBlockData)
                            showAddBlockSheet = false
                        }
                    },
                    modifier = Modifier.align(Alignment.End),
                    enabled = newBlockSector.isNotBlank()
                            && !sectorError
                            && newBlockBlock.isNotBlank()
                            && !blockError
                            && newBlockData.isNotBlank()
                            && !dataError
                ) {
                    Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Aggiungi")
                }
            }
        }
    }
}
