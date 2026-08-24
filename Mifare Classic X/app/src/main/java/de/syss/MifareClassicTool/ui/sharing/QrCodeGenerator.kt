package de.syss.MifareClassicTool.ui.sharing

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import de.syss.MifareClassicTool.data.model.VendorEntity
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

object QrCodeGenerator {
    private val json = Json { ignoreUnknownKeys = true }

    @OptIn(ExperimentalEncodingApi::class)
    fun generateQrCodeForVendor(vendor: VendorEntity, size: Int = 512): Bitmap? = try {
        val jsonString = json.encodeToString(vendor)
        val compressed = compressGzip(jsonString)
        val encoded = Base64.encode(compressed)

        val hints = mapOf(
            EncodeHintType.MARGIN to 1,
            EncodeHintType.ERROR_CORRECTION to com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.H
        )

        val writer = MultiFormatWriter()
        val bitMatrix = writer.encode(encoded, BarcodeFormat.QR_CODE, size, size, hints)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
        bitmap
    } catch (e: WriterException) {
        e.printStackTrace()
        null
    }

    @OptIn(ExperimentalEncodingApi::class)
    fun decodeQrContent(qrContent: String): VendorEntity? = try {
        val decodedBytes = Base64.decode(qrContent)
        val decompressed = decompressGzip(decodedBytes)
        json.decodeFromString(decompressed)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }

    private fun compressGzip(input: String): ByteArray {
        val outputStream = ByteArrayOutputStream()
        GZIPOutputStream(outputStream).use { gzipStream ->
            gzipStream.write(input.toByteArray())
        }
        return outputStream.toByteArray()
    }

    private fun decompressGzip(input: ByteArray): String {
        return java.util.zip.GZIPInputStream(input.inputStream()).use { gzipStream ->
            gzipStream.bufferedReader().readText()
        }
    }
}
