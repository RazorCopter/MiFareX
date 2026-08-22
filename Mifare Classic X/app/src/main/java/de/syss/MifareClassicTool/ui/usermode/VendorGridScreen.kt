package de.syss.MifareClassicTool.ui.usermode

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.syss.MifareClassicTool.ui.components.VendorCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorGridScreen(
    onVendorClick: (String) -> Unit,
    onAddVendorClick: () -> Unit,
    onEditVendorClick: (String) -> Unit = {},
    onImportExportClick: () -> Unit = {},
    viewModel: VendorGridViewModel = viewModel()
) {
    val vendors by viewModel.vendors.collectAsStateWithLifecycle(initialValue = emptyList())
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            "MiFare Classic X",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Seleziona un Vendor per programmare",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onImportExportClick) {
                        Icon(Icons.Filled.ImportExport, contentDescription = "Import / Export")
                    }
                    IconButton(onClick = { viewModel.toggleSearch() }) {
                        Icon(Icons.Filled.Search, contentDescription = "Cerca")
                    }
                    IconButton(onClick = onAddVendorClick) {
                        Icon(Icons.Filled.Add, contentDescription = "Nuovo Vendor")
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val focusRequester = remember { FocusRequester() }
            AnimatedVisibility(
                visible = viewModel.isSearchVisible,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = viewModel::updateSearch,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .focusRequester(focusRequester),
                    placeholder = { Text("Cerca vendor…") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateSearch("") }) {
                                Icon(Icons.Filled.Close, contentDescription = "Cancella ricerca")
                            }
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.extraLarge
                )
                LaunchedEffect(Unit) { focusRequester.requestFocus() }
            }

            if (vendors.isEmpty()) {
                EmptyVendorState(
                    onAddClick = onAddVendorClick,
                    modifier = Modifier.fillMaxSize().weight(1f)
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(
                        start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(vendors, key = { it.id }) { vendor ->
                        VendorCard(
                            vendor = vendor,
                            onClick = { onVendorClick(vendor.id) },
                            onEditClick = { onEditVendorClick(vendor.id) },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyVendorState(
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.3f, targetValue = 0.8f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse
            ), label = "alpha"
        )
        Text(
            "📡",
            style = MaterialTheme.typography.displayLarge.copy(
                color = MaterialTheme.colorScheme.primary.copy(alpha = alpha)
            )
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Nessun Vendor configurato",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Usa il tasto + in alto per creare\nil tuo primo Vendor",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        FilledTonalButton(onClick = onAddClick) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Crea Vendor")
        }
    }
}
