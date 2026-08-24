package de.syss.MifareClassicTool.domain.nfc

import java.util.concurrent.atomic.AtomicLong

enum class NfcOperationKind {
    MANUAL_WRITE,
    TEST_KEYS,
    READ_TAG,
    AUTO_WRITE
}

data class NfcOperationSession(
    val token: Long,
    val kind: NfcOperationKind,
    val ownerId: String,
    val vendorId: String?,
    val expiresAtMillis: Long,
    val claimed: Boolean = false
)

/**
 * Owns the single NFC operation that may receive the next discovered tag.
 *
 * A session is bound to the UI route that created it, expires automatically,
 * and can claim at most one tag. This prevents an Activity-scoped ViewModel
 * from keeping a write armed after its screen has left the navigation stack.
 */
class NfcOperationSessionCoordinator(
    private val clockMillis: () -> Long = System::currentTimeMillis,
    private val tokenSource: () -> Long = AtomicLong().let { counter ->
        { counter.incrementAndGet() }
    }
) {
    private var active: NfcOperationSession? = null

    @Synchronized
    fun arm(
        kind: NfcOperationKind,
        ownerId: String,
        vendorId: String?,
        timeoutMillis: Long
    ): NfcOperationSession {
        require(ownerId.isNotBlank()) { "ownerId must not be blank" }
        require(timeoutMillis > 0) { "timeoutMillis must be positive" }

        return NfcOperationSession(
            token = tokenSource(),
            kind = kind,
            ownerId = ownerId,
            vendorId = vendorId,
            expiresAtMillis = clockMillis() + timeoutMillis
        ).also { active = it }
    }

    @Synchronized
    fun claimNextTag(): NfcOperationSession? {
        val session = active ?: return null
        if (session.claimed) return null
        if (clockMillis() >= session.expiresAtMillis) {
            active = null
            return null
        }
        return session.copy(claimed = true).also { active = it }
    }

    @Synchronized
    fun cancelOwner(ownerId: String): Boolean {
        if (active?.ownerId != ownerId) return false
        active = null
        return true
    }

    @Synchronized
    fun finish(token: Long): Boolean {
        if (active?.token != token) return false
        active = null
        return true
    }

    @Synchronized
    fun isCurrent(token: Long): Boolean {
        val session = active ?: return false
        if (!session.claimed && clockMillis() >= session.expiresAtMillis) {
            active = null
            return false
        }
        return session.token == token
    }

    @Synchronized
    fun current(): NfcOperationSession? {
        val session = active ?: return null
        if (!session.claimed && clockMillis() >= session.expiresAtMillis) {
            active = null
            return null
        }
        return session
    }
}
