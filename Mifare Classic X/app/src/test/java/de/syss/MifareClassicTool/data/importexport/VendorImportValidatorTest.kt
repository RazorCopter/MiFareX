package de.syss.MifareClassicTool.data.importexport

import de.syss.MifareClassicTool.data.model.UidEntry
import de.syss.MifareClassicTool.data.model.VendorConfig
import de.syss.MifareClassicTool.data.model.VendorExportBundle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class VendorImportValidatorTest {
    private fun vendor(id: String = "vendor-a") = VendorConfig(id = id, name = "Test")

    @Test
    fun acceptsAndNormalizesValidUidAssociation() {
        val result = VendorImportValidator.validate(
            VendorExportBundle(
                vendors = listOf(vendor()),
                uids = listOf(UidEntry(uid = "A1B2C3D4", vendorId = "vendor-a", label = "  Card  "))
            )
        )

        assertEquals("a1b2c3d4", result.uids.single().uid)
        assertEquals("Card", result.uids.single().label)
    }

    @Test
    fun rejectsUnknownSchemaVersion() {
        assertThrows(InvalidVendorImportException::class.java) {
            VendorImportValidator.validate(VendorExportBundle(version = 2, vendors = listOf(vendor())))
        }
    }

    @Test
    fun rejectsDuplicateVendorIdBeforeDatabaseWrite() {
        assertThrows(InvalidVendorImportException::class.java) {
            VendorImportValidator.validate(VendorExportBundle(vendors = listOf(vendor(), vendor())))
        }
    }

    @Test
    fun rejectsUidReferencingVendorOutsideBundle() {
        assertThrows(InvalidVendorImportException::class.java) {
            VendorImportValidator.validate(
                VendorExportBundle(
                    vendors = listOf(vendor()),
                    uids = listOf(UidEntry(uid = "a1b2c3d4", vendorId = "missing"))
                )
            )
        }
    }
}
