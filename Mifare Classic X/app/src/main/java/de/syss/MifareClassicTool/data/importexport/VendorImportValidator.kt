package de.syss.MifareClassicTool.data.importexport

import de.syss.MifareClassicTool.data.model.UidEntry
import de.syss.MifareClassicTool.data.model.VendorConfig
import de.syss.MifareClassicTool.data.model.VendorExportBundle
import de.syss.MifareClassicTool.domain.nfc.PayloadSafetyValidator
import de.syss.MifareClassicTool.domain.nfc.PayloadValidationResult

class InvalidVendorImportException(message: String) : IllegalArgumentException(message)

/** Validates the complete import before any row is changed in Room. */
object VendorImportValidator {
    private const val SUPPORTED_VERSION = 1
    private val uidPattern = Regex("[0-9a-f]{8,20}")

    fun validate(bundle: VendorExportBundle): ValidatedVendorImport {
        if (bundle.version != SUPPORTED_VERSION) {
            throw InvalidVendorImportException(
                "Versione di importazione non supportata: ${bundle.version}."
            )
        }

        val vendorIds = mutableSetOf<String>()
        bundle.vendors.forEachIndexed { index, vendor -> validateVendor(index, vendor, vendorIds) }

        val normalizedUids = bundle.uids.mapIndexed { index, entry ->
            validateUid(index, entry, vendorIds)
        }
        if (normalizedUids.map { it.uid }.toSet().size != normalizedUids.size) {
            throw InvalidVendorImportException("Il file contiene UID duplicati.")
        }

        return ValidatedVendorImport(bundle.vendors, normalizedUids)
    }

    private fun validateVendor(index: Int, vendor: VendorConfig, seenIds: MutableSet<String>) {
        val prefix = "Vendor #${index + 1}"
        if (vendor.id.isBlank()) throw InvalidVendorImportException("$prefix: id mancante.")
        if (!seenIds.add(vendor.id)) {
            throw InvalidVendorImportException("$prefix: id duplicato '${vendor.id}'.")
        }
        if (vendor.name.isBlank()) throw InvalidVendorImportException("$prefix: nome mancante.")
        if (vendor.writeCount < 0) {
            throw InvalidVendorImportException("$prefix: writeCount non può essere negativo.")
        }
        if (vendor.createdAt < 0 || vendor.updatedAt < 0) {
            throw InvalidVendorImportException("$prefix: timestamp non valido.")
        }

        val payload = vendor.payload
        if (payload.blocks.isNotEmpty() || payload.valueBlockOps.isNotEmpty()) {
            when (val validation = PayloadSafetyValidator.validate(payload, vendor.tagType)) {
                PayloadValidationResult.Valid -> Unit
                is PayloadValidationResult.Invalid -> throw InvalidVendorImportException(
                    "$prefix: payload non sicuro: " +
                        validation.violations.joinToString { it.description() }
                )
            }
        }
    }

    private fun validateUid(index: Int, entry: UidEntry, vendorIds: Set<String>): UidEntry {
        val prefix = "Associazione UID #${index + 1}"
        val normalizedUid = entry.uid.lowercase()
        if (!uidPattern.matches(normalizedUid) || normalizedUid.length % 2 != 0) {
            throw InvalidVendorImportException("$prefix: UID non valido.")
        }
        if (entry.vendorId !in vendorIds) {
            throw InvalidVendorImportException("$prefix: vendorId non presente nel file.")
        }
        if (entry.createdAt < 0) throw InvalidVendorImportException("$prefix: timestamp non valido.")
        return entry.copy(uid = normalizedUid, label = entry.label?.trim()?.ifBlank { null })
    }
}

data class ValidatedVendorImport(
    val vendors: List<VendorConfig>,
    val uids: List<UidEntry>
)
