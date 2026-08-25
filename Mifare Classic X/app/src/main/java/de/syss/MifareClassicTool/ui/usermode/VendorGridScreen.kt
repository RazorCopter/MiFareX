package de.syss.MifareClassicTool.ui.usermode

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.syss.MifareClassicTool.ui.components.MctxModeBadge
import de.syss.MifareClassicTool.ui.components.NfcStatusBadge
import de.syss.MifareClassicTool.ui.components.NfcUiStatus
import de.syss.MifareClassicTool.ui.components.PremiumScreenBackground
import de.syss.MifareClassicTool.ui.components.VendorCard
import de.syss.MifareClassicTool.ui.components.rememberNfcUiStatus
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch
import de.syss.MifareClassicTool.AppConstants

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorGridScreen(
    onVendorClick: (String) -> Unit,
    onAddVendorClick: () -> Unit,
    onEditVendorClick: (String) -> Unit = {},
    onImportExportClick: () -> Unit = {},
    onHistoryClick: () -> Unit = {},
    viewModel: VendorGridViewModel = viewModel()
) {
    val vendors by viewModel.vendors.collectAsStateWithLifecycle(initialValue = emptyList())
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val nfcStatus = rememberNfcUiStatus()
    val focusRequester = remember { FocusRequester() }
    var isGridView by rememberSaveable { mutableStateOf(true) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("MiFareX", style = MaterialTheme.typography.titleLarge)
                        Text("Operator console v${AppConstants.VERSION_NAME}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                },
                actions = {
                    IconButton(onClick = { isGridView = !isGridView }) {
                        Icon(if (isGridView) Icons.Filled.ViewList else Icons.Filled.GridView, contentDescription = "Cambia visualizzazione")
                    }
                    IconButton(onClick = onHistoryClick) {
                        Icon(Icons.Filled.History, contentDescription = "Cronologia operazioni")
                    }
                    IconButton(onClick = onImportExportClick) {
                        Icon(Icons.Filled.ImportExport, contentDescription = "Importa o esporta profili")
                    }
                    IconButton(onClick = viewModel::toggleSearch) {
                        Icon(Icons.Filled.Search, contentDescription = "Cerca vendor")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddVendorClick) {
                Icon(Icons.Filled.Add, contentDescription = "Crea nuovo vendor")
            }
        }
    ) { paddingValues ->
        PremiumScreenBackground(modifier = Modifier.padding(paddingValues)) {
            Column(modifier = Modifier.fillMaxSize()) {
                AnimatedVisibility(
                    visible = viewModel.isSearchVisible,
                    enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                    exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()
                ) {
                    TextField(
                        value = searchQuery,
                        onValueChange = viewModel::updateSearch,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .focusRequester(focusRequester),
                        placeholder = { Text("Cerca per nome o categoria") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.updateSearch("") }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Cancella ricerca")
                                }
                            }
                        },
                        singleLine = true,
                        shape = MaterialTheme.shapes.large,
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                    )
                    LaunchedEffect(Unit) { focusRequester.requestFocus() }
                }

                if (vendors.isEmpty()) {
                    EmptyVendorState(onAddClick = onAddVendorClick, nfcStatus = nfcStatus, modifier = Modifier.fillMaxSize())
                } else {
                    LazyVerticalGrid(
                        columns = if (isGridView) GridCells.Fixed(2) else GridCells.Fixed(1),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            OperatorHero(vendorCount = vendors.size, nfcStatus = nfcStatus)
                        }
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Profili operativi", style = MaterialTheme.typography.titleLarge)
                                    Text("Seleziona il profilo da usare", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text("${vendors.size}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        items(vendors, key = { it.id }) { vendor ->
                            val index = vendors.indexOf(vendor)
                            val animatedAlpha = remember { androidx.compose.animation.core.Animatable(0f) }
                            val animatedOffsetY = remember { androidx.compose.animation.core.Animatable(24f) }
                            LaunchedEffect(vendor.id) {
                                kotlinx.coroutines.delay(index * 50L)
                                kotlinx.coroutines.coroutineScope {
                                    launch { animatedAlpha.animateTo(1f, tween(300)) }
                                    launch { animatedOffsetY.animateTo(0f, tween(350, easing = androidx.compose.animation.core.EaseOutCubic)) }
                                }
                            }
                            VendorCard(
                                vendor = vendor,
                                onClick = { onVendorClick(vendor.id) },
                                onEditClick = { onEditVendorClick(vendor.id) },
                                onDuplicateClick = { viewModel.duplicateVendor(vendor.id) },
                                modifier = Modifier
                                    .animateItem()
                                    .graphicsLayer {
                                        alpha = animatedAlpha.value
                                        translationY = animatedOffsetY.value * density
                                    }
                                    .then(if (isGridView) Modifier else Modifier.height(96.dp)),
                                isGrid = isGridView
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OperatorHero(vendorCount: Int, nfcStatus: NfcUiStatus) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                MctxModeBadge("Operatore")
                NfcStatusBadge(status = nfcStatus)
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    if (nfcStatus == NfcUiStatus.READY) "Sistema pronto" else "Controlla lo stato NFC",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    "$vendorCount profili disponibili. Apri un profilo per verificare e programmare il tag.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f)
                )
            }
        }
    }
}

@Composable
private fun EmptyVendorState(
    onAddClick: () -> Unit,
    nfcStatus: NfcUiStatus,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        MctxModeBadge("Operatore")
        Spacer(modifier = Modifier.height(18.dp))
        Icon(Icons.Filled.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(72.dp))
        Spacer(modifier = Modifier.height(20.dp))
        Text("Nessun profilo operativo", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Crea un vendor per definire chiavi e payload prima di programmare un tag.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        NfcStatusBadge(status = nfcStatus)
        Spacer(modifier = Modifier.height(28.dp))
        Button(onClick = onAddClick, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Crea il primo vendor", fontWeight = FontWeight.Bold)
        }
    }
}
