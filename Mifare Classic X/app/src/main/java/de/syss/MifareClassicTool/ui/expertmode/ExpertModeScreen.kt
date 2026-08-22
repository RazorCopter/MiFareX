package de.syss.MifareClassicTool.ui.expertmode

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.syss.MifareClassicTool.bridge.LegacyInterop
import de.syss.MifareClassicTool.bridge.LegacyTool

/**
 * Expert Mode screen showing all legacy MIFARE Classic tools
 * as a grid of clickable cards. Each card launches the corresponding
 * legacy Java Activity via Intent — no code modifications needed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpertModeScreen() {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            "Expert Mode",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Strumenti avanzati MIFARE Classic",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            items(LegacyTool.entries.toList()) { tool ->
                LegacyToolCard(
                    tool = tool,
                    onClick = { LegacyInterop.launchTool(context, tool) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LegacyToolCard(
    tool: LegacyTool,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val icon = getToolIcon(tool)

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = tool.displayName,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = tool.displayName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = tool.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Map legacy tools to Material Design icons.
 */
private fun getToolIcon(tool: LegacyTool): ImageVector {
    return when (tool) {
        LegacyTool.READ_TAG -> Icons.Filled.Nfc
        LegacyTool.WRITE_TAG -> Icons.Filled.Edit
        LegacyTool.DUMP_EDITOR -> Icons.Filled.DeveloperBoard
        LegacyTool.KEY_EDITOR -> Icons.Filled.Key
        LegacyTool.TAG_INFO -> Icons.Filled.Info
        LegacyTool.ACCESS_CONDITIONS -> Icons.Filled.Lock
        LegacyTool.ACCESS_CONDITION_TOOL -> Icons.Filled.LockOpen
        LegacyTool.VALUE_BLOCK_TOOL -> Icons.Filled.Calculate
        LegacyTool.VALUE_BLOCKS_TO_INT -> Icons.Filled.Numbers
        LegacyTool.DIFF_TOOL -> Icons.Filled.CompareArrows
        LegacyTool.BCC_TOOL -> Icons.Filled.Verified
        LegacyTool.CLONE_UID -> Icons.Filled.ContentCopy
        LegacyTool.HEX_TO_ASCII -> Icons.Filled.Translate
        LegacyTool.DATA_CONVERSION -> Icons.Filled.SwapHoriz
        LegacyTool.IMPORT_EXPORT -> Icons.Filled.ImportExport
        LegacyTool.UID_LOG -> Icons.Filled.History
    }
}
