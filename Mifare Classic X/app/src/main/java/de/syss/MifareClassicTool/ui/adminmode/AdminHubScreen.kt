package de.syss.MifareClassicTool.ui.adminmode

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.syss.MifareClassicTool.ui.components.MctxModeBadge
import de.syss.MifareClassicTool.ui.components.MctxStatusBanner
import de.syss.MifareClassicTool.ui.components.NfcStatusBadge
import de.syss.MifareClassicTool.ui.components.PremiumScreenBackground

private data class AdminAction(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val expert: Boolean = false,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminHubScreen(
    onCreateVendor: () -> Unit,
    onImportExport: () -> Unit,
    onUidManager: () -> Unit,
    onExpertMode: () -> Unit,
    onSettings: () -> Unit,
    onHistory: () -> Unit = {},
    onDiagnostics: () -> Unit = {}
) {
    val actions = listOf(
        AdminAction("Nuovo vendor", "Crea chiavi, payload e regole per un nuovo profilo.", Icons.Filled.AddBusiness, onClick = onCreateVendor),
        AdminAction("Import ed export", "Backup locale e importazione validata dei profili.", Icons.Filled.ImportExport, onClick = onImportExport),
        AdminAction("Gestione UID", "Associa tessere conosciute ai vendor configurati.", Icons.Filled.Fingerprint, onClick = onUidManager),
        AdminAction("Cronologia operazioni", "Consulta l’audit locale con esiti e dettagli privacy-safe.", Icons.Filled.History, onClick = onHistory),
        AdminAction("Diagnostica NFC", "Verifica adattatore, dispositivo e controlli operativi.", Icons.Filled.Nfc, onClick = onDiagnostics),
        AdminAction("Impostazioni", "Preferenze NFC e comportamento degli strumenti.", Icons.Filled.Settings, onClick = onSettings),
        AdminAction("Expert Mode", "Accedi agli strumenti MIFARE avanzati e ai dump raw.", Icons.Filled.AdminPanelSettings, expert = true, onClick = onExpertMode)
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Control Center", style = MaterialTheme.typography.titleLarge)
                        Text("Configurazione e sicurezza", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = { NfcStatusBadge(modifier = Modifier.padding(end = 12.dp)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            )
        }
    ) { padding ->
        PremiumScreenBackground(modifier = Modifier.padding(padding)) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(240.dp),
                modifier = Modifier.fillMaxSize().widthIn(max = 1080.dp).align(Alignment.TopCenter),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                    MctxStatusBanner(
                        title = "Area amministratore",
                        message = "Le modifiche qui influenzano le operazioni degli utenti. Expert Mode resta separata.",
                        icon = Icons.Filled.Security
                    )
                }
                items(actions) { action ->
                    AdminActionCard(action)
                }
            }
        }
    }
}

@Composable
private fun AdminActionCard(action: AdminAction) {
    Card(
        onClick = action.onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (action.expert) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp, pressedElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = if (action.expert) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(
                        action.icon,
                        contentDescription = null,
                        modifier = Modifier.padding(10.dp).size(24.dp),
                        tint = if (action.expert) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                MctxModeBadge(if (action.expert) "Expert" else "Admin", expert = action.expert)
            }
            Text(action.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                action.description,
                style = MaterialTheme.typography.bodyMedium,
                color = if (action.expert) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
