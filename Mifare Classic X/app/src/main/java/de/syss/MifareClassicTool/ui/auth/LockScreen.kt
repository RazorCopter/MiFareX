package de.syss.MifareClassicTool.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.syss.MifareClassicTool.ui.components.MctxModeBadge
import de.syss.MifareClassicTool.ui.components.PremiumScreenBackground

@Composable
fun LockScreen(onUnlockClick: () -> Unit, errorMessage: String? = null) {
    PremiumScreenBackground {
        BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            val wide = maxWidth >= 720.dp
            if (wide) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(48.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BrandPanel(Modifier.weight(1f))
                    UnlockPanel(onUnlockClick, errorMessage, Modifier.weight(1f))
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    BrandPanel(Modifier.fillMaxWidth())
                    Spacer(Modifier.height(24.dp))
                    UnlockPanel(onUnlockClick, errorMessage, Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun BrandPanel(modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.primaryContainer) {
            Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.padding(22.dp).size(54.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(18.dp))
        Text("MiFareX", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text("SECURE NFC OPERATIONS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun UnlockPanel(onUnlockClick: () -> Unit, errorMessage: String?, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.widthIn(max = 480.dp),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MctxModeBadge("Area protetta")
            Icon(Icons.Filled.VerifiedUser, contentDescription = null, modifier = Modifier.size(42.dp), tint = MaterialTheme.colorScheme.primary)
            Text("Conferma la tua identità", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            Text(
                "L’accesso protegge chiavi, profili e operazioni memorizzate su questo dispositivo.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (errorMessage != null) {
                Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.errorContainer) {
                    Text(
                        errorMessage,
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Button(onClick = onUnlockClick, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Icon(Icons.Filled.Fingerprint, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Sblocca con biometria", fontWeight = FontWeight.Bold)
            }
        }
    }
}
