package de.syss.MifareClassicTool.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun NfcRingsAnimation(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    isWriting: Boolean = false,
    isDone: Boolean = false,
    size: Dp = 180.dp
) {
    val ringCount = 3
    val duration = if (isWriting) 800 else 1500
    val infiniteTransition = rememberInfiniteTransition(label = "nfc_rings")

    val progresses = (0 until ringCount).map { i ->
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(duration, easing = EaseOutCubic),
                repeatMode = RepeatMode.Restart,
                initialStartOffset = StartOffset(i * duration / ringCount)
            ),
            label = "ring_$i"
        )
    }

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = this.center
            val maxRadius = this.size.minDimension / 2f
            val strokeWidth = if (isWriting) 4.dp.toPx() else 2.5.dp.toPx()

            progresses.forEach { progress ->
                val p = progress.value
                val radius = maxRadius * p
                val alpha = (1f - p) * (if (isWriting) 0.8f else 0.5f)
                drawCircle(
                    color = color.copy(alpha = alpha),
                    radius = radius,
                    center = center,
                    style = Stroke(width = strokeWidth)
                )
            }
        }

        // Center icon
        if (isDone) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(size * 0.38f)
            )
        } else {
            Icon(
                imageVector = Icons.Filled.Nfc,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(size * 0.38f)
            )
        }
    }
}
