package de.syss.MifareClassicTool.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import de.syss.MifareClassicTool.data.model.VendorCategory
import de.syss.MifareClassicTool.data.model.VendorEntity
import de.syss.MifareClassicTool.data.model.WriteResult

private fun writeStatusColor(result: WriteResult): Color? = when (result) {
    WriteResult.SUCCESS -> Color(0xFF4CAF50)
    WriteResult.PARTIAL -> Color(0xFFFFB300)
    WriteResult.FAILED -> Color(0xFFEF5350)
    WriteResult.NEVER_USED -> null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorCard(
    vendor: VendorEntity,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categoryColor = getCategoryColor(vendor.category)

    val statusColor = writeStatusColor(vendor.lastWriteResult)

    Box(modifier = modifier.height(176.dp)) {
        Card(
            onClick = onClick,
            modifier = Modifier
                .fillMaxSize()
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            categoryColor.copy(alpha = 0.4f),
                            categoryColor.copy(alpha = 0.08f)
                        )
                    ),
                    shape = RoundedCornerShape(20.dp)
                ),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 2.dp,
                pressedElevation = 6.dp
            )
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Top accent bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    categoryColor,
                                    categoryColor.copy(alpha = 0.3f)
                                )
                            )
                        )
                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                )

                // Left status border
                if (statusColor != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .fillMaxHeight(0.6f)
                            .width(3.dp)
                            .clip(RoundedCornerShape(topEnd = 3.dp, bottomEnd = 3.dp))
                            .background(statusColor)
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 16.dp, start = 12.dp, end = 12.dp, bottom = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Icon — custom image or category fallback
                    VendorCircleIcon(
                        iconUri = vendor.iconUri,
                        category = vendor.category,
                        categoryColor = categoryColor,
                        size = 56.dp
                    )

                    // Name + subtitle (always reserve subtitle space for uniform height)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = vendor.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = vendor.subtitle ?: " ",
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Badge
                    WriteStatusBadge(result = vendor.lastWriteResult, writeCount = vendor.writeCount)
                }
            }
        }

        // Edit button — top-right overlay
        IconButton(
            onClick = onEditClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 6.dp, end = 2.dp)
                .size(32.dp)
        ) {
            Icon(
                Icons.Filled.Edit,
                contentDescription = "Modifica",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun WriteStatusBadge(
    result: WriteResult,
    writeCount: Int,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, label) = when (result) {
        WriteResult.SUCCESS -> Triple(
            Color(0xFF2E7D32).copy(alpha = 0.15f),
            Color(0xFF4CAF50),
            "✓ $writeCount"
        )
        WriteResult.PARTIAL -> Triple(
            Color(0xFFF57F17).copy(alpha = 0.15f),
            Color(0xFFFFB300),
            "⚠ Parziale"
        )
        WriteResult.FAILED -> Triple(
            Color(0xFFC62828).copy(alpha = 0.15f),
            Color(0xFFEF5350),
            "✗ Errore"
        )
        WriteResult.NEVER_USED -> Triple(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            "Mai usato"
        )
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = bgColor
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}

/**
 * Circular vendor icon — shows a custom AsyncImage if [iconUri] is set,
 * otherwise falls back to the category vector icon.
 */
@Composable
fun VendorCircleIcon(
    iconUri: String?,
    category: VendorCategory,
    categoryColor: Color,
    size: Dp = 56.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        categoryColor.copy(alpha = 0.9f),
                        categoryColor.copy(alpha = 0.55f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        if (!iconUri.isNullOrBlank()) {
            AsyncImage(
                model = iconUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(CircleShape)
            )
        } else {
            Icon(
                imageVector = getCategoryIcon(category),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(size * 0.5f)
            )
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
    VendorCategory.CAR_WASH -> Color(0xFF1565C0)
    VendorCategory.GYM -> Color(0xFF2E7D32)
    VendorCategory.VENDING -> Color(0xFF8B5000)
    VendorCategory.ACCESS_CONTROL -> Color(0xFFC62828)
    VendorCategory.PARKING -> Color(0xFF6A1B9A)
    VendorCategory.CUSTOM -> MaterialTheme.colorScheme.primary
}
