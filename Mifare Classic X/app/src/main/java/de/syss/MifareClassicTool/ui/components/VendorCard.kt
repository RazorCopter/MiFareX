package de.syss.MifareClassicTool.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileCopy
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalCarWash
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import de.syss.MifareClassicTool.data.model.VendorCategory
import de.syss.MifareClassicTool.data.model.VendorEntity
import de.syss.MifareClassicTool.data.model.WriteResult

private fun writeStatusColor(result: WriteResult): Color? = when (result) {
    WriteResult.SUCCESS -> Color(0xFF48C774)
    WriteResult.PARTIAL -> Color(0xFFFFB95F)
    WriteResult.FAILED -> Color(0xFFFF6B63)
    WriteResult.NEVER_USED -> null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorCard(
    vendor: VendorEntity,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onDuplicateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categoryColor = getCategoryColor(vendor.category)
    var menuExpanded by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.98f else 1f, label = "vendor_card_scale")

    Card(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier
            .height(204.dp)
            .scale(scale)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(listOf(categoryColor.copy(alpha = 0.5f), MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))),
                shape = MaterialTheme.shapes.large
            )
            .semantics { role = Role.Button },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp, pressedElevation = 5.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(Brush.horizontalGradient(listOf(categoryColor, categoryColor.copy(alpha = 0.15f))))
            )
            Column(
                modifier = Modifier.fillMaxSize().padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    VendorCircleIcon(vendor.iconUri, vendor.category, categoryColor, size = 52.dp)
                    Box {
                        IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "Azioni per ${vendor.name}")
                        }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text("Modifica") },
                                leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                                onClick = { menuExpanded = false; onEditClick() }
                            )
                            DropdownMenuItem(
                                text = { Text("Duplica") },
                                leadingIcon = { Icon(Icons.Filled.FileCopy, contentDescription = null) },
                                onClick = { menuExpanded = false; onDuplicateClick() }
                            )
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(vendor.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        vendor.subtitle ?: vendor.category.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    WriteStatusBadge(vendor.lastWriteResult, vendor.writeCount)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Apri", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun WriteStatusBadge(result: WriteResult, writeCount: Int, modifier: Modifier = Modifier) {
    val statusColor = writeStatusColor(result) ?: MaterialTheme.colorScheme.onSurfaceVariant
    val label = when (result) {
        WriteResult.SUCCESS -> "Verificato · $writeCount"
        WriteResult.PARTIAL -> "Parziale"
        WriteResult.FAILED -> "Errore"
        WriteResult.NEVER_USED -> "Mai usato"
    }
    Surface(modifier = modifier, shape = CircleShape, color = statusColor.copy(alpha = 0.14f)) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(statusColor))
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = statusColor)
        }
    }
}

@Composable
fun VendorCircleIcon(
    iconUri: String?,
    category: VendorCategory,
    categoryColor: Color,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(MaterialTheme.shapes.medium)
            .background(Brush.linearGradient(listOf(categoryColor.copy(alpha = 0.95f), categoryColor.copy(alpha = 0.58f)))),
        contentAlignment = Alignment.Center
    ) {
        if (!iconUri.isNullOrBlank()) {
            AsyncImage(model = iconUri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(MaterialTheme.shapes.medium))
        } else {
            Icon(getCategoryIcon(category), contentDescription = null, tint = Color.White, modifier = Modifier.size(size * 0.5f))
        }
    }
}

fun getCategoryIcon(category: VendorCategory): ImageVector = when (category) {
    VendorCategory.CAR_WASH -> Icons.Filled.LocalCarWash
    VendorCategory.GYM -> Icons.Filled.FitnessCenter
    VendorCategory.VENDING -> Icons.Filled.LocalCafe
    VendorCategory.ACCESS_CONTROL -> Icons.Filled.Lock
    VendorCategory.PARKING -> Icons.Filled.LocalParking
    VendorCategory.CUSTOM -> Icons.Filled.Nfc
}

@Composable
fun getCategoryColor(category: VendorCategory): Color = when (category) {
    VendorCategory.CAR_WASH -> Color(0xFF168AAD)
    VendorCategory.GYM -> Color(0xFF2B9360)
    VendorCategory.VENDING -> Color(0xFFAA6D13)
    VendorCategory.ACCESS_CONTROL -> Color(0xFFB84A4A)
    VendorCategory.PARKING -> Color(0xFF7268B5)
    VendorCategory.CUSTOM -> MaterialTheme.colorScheme.primary
}
