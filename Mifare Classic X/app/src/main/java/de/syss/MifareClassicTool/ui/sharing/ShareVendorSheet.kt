package de.syss.MifareClassicTool.ui.sharing

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import de.syss.MifareClassicTool.data.model.VendorEntity
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareVendorSheet(
    vendor: VendorEntity,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isGenerating by remember { mutableStateOf(true) }

    LaunchedEffect(vendor) {
        qrBitmap = QrCodeGenerator.generateQrCodeForVendor(vendor, 512)
        isGenerating = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Title
            Text(
                "Condividi ${vendor.name}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 12.dp)
            )

            // QR Code display
            if (isGenerating) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(256.dp)
                        .padding(vertical = 24.dp)
                )
            } else {
                qrBitmap?.let { bitmap ->
                    Surface(
                        modifier = Modifier
                            .size(256.dp)
                            .padding(8.dp),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceContainerLowest
                    ) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "QR Code per ${vendor.name}",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                        )
                    }
                } ?: run {
                    Text(
                        "Errore nella generazione del QR Code",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            // Description
            Text(
                "Scansiona questo codice per importare il profilo ${vendor.name} su un altro dispositivo",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Chiudi")
                }

                Button(
                    onClick = {
                        qrBitmap?.let { bitmap ->
                            shareQrCodeFile(context, bitmap, vendor.name)
                            onDismiss()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = qrBitmap != null
                ) {
                    Icon(Icons.Filled.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scarica QR")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

private fun shareQrCodeFile(context: android.content.Context, bitmap: Bitmap, vendorName: String) {
    try {
        val cachePath = File(context.cacheDir, "qr_codes")
        cachePath.mkdirs()

        val filename = "${vendorName.replace(" ", "_")}_qr_${System.currentTimeMillis()}.png"
        val file = File(cachePath, filename)

        FileOutputStream(file).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "QR Code vendor: $vendorName")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "Condividi QR Code"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
