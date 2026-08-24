package de.syss.MifareClassicTool.ui.diagnostics

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.syss.MifareClassicTool.ui.components.NfcStatusBadge
import de.syss.MifareClassicTool.ui.components.NfcUiStatus
import de.syss.MifareClassicTool.ui.components.PremiumScreenBackground
import de.syss.MifareClassicTool.ui.components.rememberNfcUiStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NfcDiagnosticsScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val status = rememberNfcUiStatus()
    val ready = status == NfcUiStatus.READY

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Diagnostica NFC") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                },
                actions = { NfcStatusBadge(modifier = Modifier.padding(end = 12.dp), status = status) }
            )
        }
    ) { padding ->
        PremiumScreenBackground(modifier = Modifier.padding(padding)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().widthIn(max = 760.dp).align(Alignment.TopCenter),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    DiagnosticCard(
                        icon = Icons.Filled.Nfc,
                        title = "Adattatore NFC",
                        value = when (status) {
                            NfcUiStatus.READY -> "Disponibile e attivo"
                            NfcUiStatus.DISABLED -> "Disponibile ma disattivato"
                            NfcUiStatus.UNAVAILABLE -> "Non rilevato sul dispositivo"
                        },
                        healthy = ready
                    )
                }
                item {
                    DiagnosticCard(
                        icon = Icons.Filled.PhoneAndroid,
                        title = "Dispositivo",
                        value = "${Build.MANUFACTURER} ${Build.MODEL} · Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
                        healthy = true
                    )
                }
                item {
                    DiagnosticCard(
                        icon = Icons.Filled.Memory,
                        title = "Compatibilità MIFARE Classic",
                        value = "La compatibilità completa dipende dal chipset NFC e dal tag. Usa ‘Test chiavi’ su un profilo per verificarla senza scrivere dati.",
                        healthy = null
                    )
                }
                item {
                    DiagnosticCard(
                        icon = Icons.Filled.Info,
                        title = "Controlli attivi",
                        value = "Timeout sessione, autenticazione preliminare, protezione manufacturer/trailer e verifica read-back.",
                        healthy = true
                    )
                }
                if (!ready && status != NfcUiStatus.UNAVAILABLE) {
                    item {
                        Button(
                            onClick = { context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS)) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.OpenInNew, contentDescription = null)
                            Text(" Apri impostazioni NFC")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiagnosticCard(
    icon: ImageVector,
    title: String,
    value: String,
    healthy: Boolean?
) {
    val color = when (healthy) {
        true -> Color(0xFF2E7D32)
        false -> MaterialTheme.colorScheme.error
        null -> MaterialTheme.colorScheme.tertiary
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(
                if (healthy == false) Icons.Filled.Warning else if (healthy == true) Icons.Filled.CheckCircle else Icons.Filled.Info,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
