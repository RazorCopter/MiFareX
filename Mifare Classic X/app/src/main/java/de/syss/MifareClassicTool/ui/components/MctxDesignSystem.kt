package de.syss.MifareClassicTool.ui.components

import android.nfc.NfcAdapter
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.PortableWifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

object MctxSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
}

enum class NfcUiStatus { READY, DISABLED, UNAVAILABLE }

@Composable
fun rememberNfcUiStatus(): NfcUiStatus {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    fun currentStatus(): NfcUiStatus {
        val adapter = NfcAdapter.getDefaultAdapter(context)
        return when {
            adapter == null -> NfcUiStatus.UNAVAILABLE
            adapter.isEnabled -> NfcUiStatus.READY
            else -> NfcUiStatus.DISABLED
        }
    }

    var status by remember { mutableStateOf(currentStatus()) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) status = currentStatus()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return status
}

@Composable
fun NfcStatusBadge(
    modifier: Modifier = Modifier,
    status: NfcUiStatus = rememberNfcUiStatus()
) {
    val ready = status == NfcUiStatus.READY
    val label = when (status) {
        NfcUiStatus.READY -> "NFC pronto"
        NfcUiStatus.DISABLED -> "NFC disattivato"
        NfcUiStatus.UNAVAILABLE -> "NFC non disponibile"
    }
    val foreground = if (ready) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    val background = if (ready) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
    } else {
        MaterialTheme.colorScheme.errorContainer
    }
    Surface(
        modifier = modifier.semantics { contentDescription = label },
        shape = CircleShape,
        color = background
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = if (ready) Icons.Filled.Nfc else Icons.Filled.PortableWifiOff,
                contentDescription = null,
                tint = foreground,
                modifier = Modifier.size(16.dp)
            )
            Text(label, style = MaterialTheme.typography.labelMedium, color = foreground)
        }
    }
}

@Composable
fun MctxModeBadge(label: String, modifier: Modifier = Modifier, expert: Boolean = false) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = if (expert) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.secondaryContainer
    ) {
        Text(
            text = label.uppercase(),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (expert) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
fun PremiumScreenBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.surfaceContainerLow,
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surface
                    )
                )
            ),
        content = content
    )
}

@Composable
fun AdaptiveContent(
    modifier: Modifier = Modifier,
    maxWidth: Dp = 920.dp,
    horizontalPadding: Dp = 16.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = maxWidth)
                .padding(horizontal = horizontalPadding),
            content = content
        )
    }
}

@Composable
fun MctxSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            eyebrow?.let {
                Text(
                    it.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(title, style = MaterialTheme.typography.titleLarge)
        }
        trailing?.invoke(this)
    }
}

@Composable
fun MctxStatusBanner(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Filled.CheckCircle,
    warning: Boolean = false
) {
    val container = if (warning) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.primaryContainer
    val content = if (warning) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onPrimaryContainer
    Surface(modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = container) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(24.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = content)
                Text(message, style = MaterialTheme.typography.bodySmall, color = content.copy(alpha = 0.82f))
            }
        }
    }
}

@Composable
fun NfcProgressStepper(activeStep: Int, modifier: Modifier = Modifier) {
    val steps = listOf("Verifica", "Scrittura", "Conferma")
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        steps.forEachIndexed { index, label ->
            val completed = index < activeStep
            val active = index == activeStep
            val color = when {
                completed || active -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.outlineVariant
            }
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp)
                        .background(color, CircleShape)
                        .size(height = if (active) 4.dp else 3.dp, width = 80.dp)
                )
                Text(
                    label,
                    modifier = Modifier.padding(top = 5.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
