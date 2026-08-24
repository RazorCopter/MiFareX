package de.syss.MifareClassicTool.domain.nfc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NfcOperationSessionCoordinatorTest {
    private var now = 1_000L
    private var nextToken = 40L
    private val coordinator = NfcOperationSessionCoordinator(
        clockMillis = { now },
        tokenSource = { ++nextToken }
    )

    @Test
    fun leavingOwnerDisarmsPendingWrite() {
        coordinator.arm(
            kind = NfcOperationKind.MANUAL_WRITE,
            ownerId = "vendor-detail:A",
            vendorId = "A",
            timeoutMillis = 30_000
        )

        assertTrue(coordinator.cancelOwner("vendor-detail:A"))
        assertNull(coordinator.claimNextTag())
    }

    @Test
    fun differentOwnerCannotCancelSession() {
        val session = coordinator.arm(
            kind = NfcOperationKind.MANUAL_WRITE,
            ownerId = "vendor-detail:A",
            vendorId = "A",
            timeoutMillis = 30_000
        )

        assertFalse(coordinator.cancelOwner("vendor-detail:B"))
        assertEquals(session, coordinator.current())
    }

    @Test
    fun claimedSessionKeepsCapturedVendorAndCannotBeClaimedTwice() {
        coordinator.arm(
            kind = NfcOperationKind.MANUAL_WRITE,
            ownerId = "vendor-detail:A",
            vendorId = "A",
            timeoutMillis = 30_000
        )

        val claimed = coordinator.claimNextTag()

        assertEquals("A", claimed?.vendorId)
        assertTrue(claimed?.claimed == true)
        assertNull(coordinator.claimNextTag())
        assertEquals(claimed, coordinator.current())
    }

    @Test
    fun expiredSessionRejectsTag() {
        coordinator.arm(
            kind = NfcOperationKind.READ_TAG,
            ownerId = "vendor-detail:A",
            vendorId = "A",
            timeoutMillis = 500
        )
        now += 500

        assertNull(coordinator.claimNextTag())
        assertNull(coordinator.current())
    }

    @Test
    fun newlyArmedOperationReplacesPreviousOwner() {
        coordinator.arm(
            kind = NfcOperationKind.MANUAL_WRITE,
            ownerId = "vendor-detail:A",
            vendorId = "A",
            timeoutMillis = 30_000
        )
        coordinator.arm(
            kind = NfcOperationKind.TEST_KEYS,
            ownerId = "vendor-detail:B",
            vendorId = "B",
            timeoutMillis = 30_000
        )

        val claimed = coordinator.claimNextTag()

        assertEquals(NfcOperationKind.TEST_KEYS, claimed?.kind)
        assertEquals("B", claimed?.vendorId)
    }
}
