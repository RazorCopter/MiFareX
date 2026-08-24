package de.syss.MifareClassicTool.bridge

import android.nfc.Tag
import android.nfc.TagLostException
import android.nfc.tech.MifareClassic
import android.util.Log
import android.util.SparseArray
import de.syss.MifareClassicTool.Common
import de.syss.MifareClassicTool.MCReader
import de.syss.MifareClassicTool.data.model.*
import de.syss.MifareClassicTool.domain.model.BlockWriteResult
import de.syss.MifareClassicTool.domain.model.PreflightResult
import de.syss.MifareClassicTool.domain.model.WriteOperationResult
import de.syss.MifareClassicTool.domain.nfc.PayloadSafetyValidator
import de.syss.MifareClassicTool.domain.nfc.PayloadValidationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * NfcBridge — Kotlin wrapper around legacy MCReader.java.
 *
 * This is the critical integration layer between the new Compose UI
 * and the existing Java NFC engine. It wraps MCReader calls in
 * coroutines and returns structured results, WITHOUT modifying
 * any legacy Java code.
 *
 * The write flow is "bulletproof":
 *   1. Pre-flight: verify tag type + authenticate all target sectors
 *   2. Write: only if pre-flight passes
 *   3. Result: structured feedback per-block
 *
 * All NFC operations are dispatched on Dispatchers.IO since they
 * are blocking I/O calls.
 */
class NfcBridge {

    companion object {
        private const val TAG = "NfcBridge"

        // Map TagType enum to expected MifareClassic size constants
        private fun expectedSectorCount(tagType: TagType): Int = when (tagType) {
            TagType.MIFARE_CLASSIC_1K -> 16
            TagType.MIFARE_CLASSIC_4K -> 40
            TagType.MIFARE_CLASSIC_MINI -> 5
        }
    }

    // ===================================================================
    //  PUBLIC API
    // ===================================================================

    /**
     * Full "bulletproof" vendor write: pre-flight check → write → result.
     *
     * This is the ONLY public method the ViewModel should call for
     * User Mode writes. It never throws; all error paths are captured
     * in the returned [WriteOperationResult].
     */
    suspend fun executeVendorWriteWithPreflight(
        tag: Tag,
        keys: List<SectorKey>,
        payload: PayloadConfig,
        tagType: TagType
    ): WriteOperationResult = withContext(Dispatchers.IO) {

        // ------- Step 0: basic config validation -------
        if (keys.isEmpty()) {
            return@withContext WriteOperationResult.PreflightFailed(
                PreflightResult.NoKeysConfigured
            )
        }
        val hasBlocks = payload.blocks.isNotEmpty() || payload.valueBlockOps.isNotEmpty()
        if (!hasBlocks) {
            return@withContext WriteOperationResult.PreflightFailed(
                PreflightResult.NoPayloadConfigured
            )
        }
        val validation = PayloadSafetyValidator.validate(payload, tagType)
        if (validation is PayloadValidationResult.Invalid) {
            return@withContext WriteOperationResult.PreflightFailed(
                PreflightResult.UnsafePayload(validation.violations.map { it.description() })
            )
        }

        // ------- Step 1: obtain MCReader -------
        val reader = MCReader.get(tag)
            ?: return@withContext WriteOperationResult.PreflightFailed(
                PreflightResult.TagNotSupported
            )

        try {
            reader.connect()

            // ------- Step 2: pre-flight check -------
            val preflightResult = runPreflight(reader, keys, payload, tagType)
            if (preflightResult !is PreflightResult.Ready) {
                return@withContext WriteOperationResult.PreflightFailed(preflightResult)
            }

            Log.i(TAG, "Pre-flight OK: ${preflightResult.sectorsVerified} settori verificati")

            // ------- Step 3: write -------
            when (payload.writeMode) {
                WriteMode.SELECTIVE_BLOCKS,
                WriteMode.FULL_DUMP -> writeBlocks(reader, keys, payload)

                WriteMode.VALUE_BLOCK_INCREMENT,
                WriteMode.VALUE_BLOCK_DECREMENT -> writeValueBlocks(reader, keys, payload)
            }
        } catch (e: TagLostException) {
            Log.e(TAG, "Tag lost during write operation", e)
            WriteOperationResult.Error("Tag rimosso durante la scrittura. Riprova tenendo il tag fermo.")
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during write", e)
            WriteOperationResult.Error("Errore imprevisto: ${e.localizedMessage ?: e.javaClass.simpleName}")
        } finally {
            safeClose(reader)
        }
    }

    /**
     * Run ONLY the pre-flight check (no write).
     * Useful for a "test keys" feature in Admin mode.
     */
    suspend fun runPreflightOnly(
        tag: Tag,
        keys: List<SectorKey>,
        payload: PayloadConfig,
        tagType: TagType
    ): PreflightResult = withContext(Dispatchers.IO) {

        if (keys.isEmpty()) return@withContext PreflightResult.NoKeysConfigured
        if (payload.blocks.isEmpty() && payload.valueBlockOps.isEmpty()) {
            return@withContext PreflightResult.NoPayloadConfigured
        }
        val validation = PayloadSafetyValidator.validate(payload, tagType)
        if (validation is PayloadValidationResult.Invalid) {
            return@withContext PreflightResult.UnsafePayload(
                validation.violations.map { it.description() }
            )
        }

        val reader = MCReader.get(tag)
            ?: return@withContext PreflightResult.TagNotSupported

        try {
            reader.connect()
            runPreflight(reader, keys, payload, tagType)
        } catch (e: TagLostException) {
            PreflightResult.TagLost
        } catch (e: Exception) {
            PreflightResult.ConnectionError(e.localizedMessage ?: "Errore connessione")
        } finally {
            safeClose(reader)
        }
    }

    /**
     * Read tag blocks using the vendor's keys and return a dump array
     * formatted for the legacy DumpEditor activity.
     */
    suspend fun readVendorDump(
        tag: Tag,
        keys: List<SectorKey>
    ): Array<String>? = withContext(Dispatchers.IO) {
        if (keys.isEmpty()) return@withContext null

        val reader = MCReader.get(tag) ?: return@withContext null

        try {
            reader.connect()
            
            // Extract all unique keys from the vendor configuration
            val uniqueHexKeys = mutableSetOf<String>()
            for (sk in keys) {
                sk.keyA?.let { uniqueHexKeys.add(it.uppercase()) }
                sk.keyB?.let { uniqueHexKeys.add(it.uppercase()) }
            }
            // Always add the default factory key as a fallback
            uniqueHexKeys.add(MCReader.DEFAULT_KEY)
            
            val uniqueKeys = uniqueHexKeys.map { Common.hex2Bytes(it) }
            val tmpDump = mutableListOf<String>()
            val sectorCount = reader.sectorCount
            
            for (i in 0 until sectorCount) {
                tmpDump.add("+Sector: $i")
                var sectorData: Array<String>? = null
                
                // Try to read the sector with every available key
                for (key in uniqueKeys) {
                    try {
                        // Try as Key A
                        val dataA = reader.readSector(i, key, false)
                        if (dataA != null) {
                            sectorData = dataA
                            break
                        }
                    } catch (e: TagLostException) {
                        throw e
                    } catch (e: Exception) {
                        // Auth failed, ignore and try next
                    }

                    try {
                        // Try as Key B
                        val dataB = reader.readSector(i, key, true)
                        if (dataB != null) {
                            sectorData = dataB
                            break
                        }
                    } catch (e: TagLostException) {
                        throw e
                    } catch (e: Exception) {
                        // Auth failed, ignore and try next
                    }
                }
                
                if (sectorData != null) {
                    tmpDump.addAll(sectorData)
                } else {
                    tmpDump.add("*No keys found or dead sector")
                }
            }
            return@withContext tmpDump.toTypedArray()

        } catch (e: Exception) {
            Log.e(TAG, "Error reading vendor dump", e)
            return@withContext null
        } finally {
            safeClose(reader)
        }
    }

    // ===================================================================
    //  PRE-FLIGHT CHECK
    // ===================================================================

    /**
     * Pre-flight check steps:
     *
     * 1. **Tag type match**: compare tag's actual sector count vs. vendor config.
     *    We use a soft check: actual must be >= expected (a 4K tag can serve 1K payloads).
     *
     * 2. **Key authentication**: for every sector referenced by the payload,
     *    attempt authenticateSectorWithKeyA and/or KeyB using the vendor's stored
     *    keys. If auth fails on ANY required sector → abort.
     *
     * This uses the MifareClassic API directly (via MCReader's public getters)
     * rather than MCReader's private authenticate(), keeping legacy code untouched.
     */
    private fun runPreflight(
        reader: MCReader,
        keys: List<SectorKey>,
        payload: PayloadConfig,
        tagType: TagType
    ): PreflightResult {

        val actualSectorCount = reader.sectorCount

        // --- 1. Collect all sectors that will be touched ---
        val requiredSectors = mutableSetOf<Int>()
        payload.blocks.forEach { requiredSectors.add(it.sector) }
        payload.valueBlockOps.forEach { requiredSectors.add(it.sector) }

        if (requiredSectors.isEmpty()) {
            return PreflightResult.NoPayloadConfigured
        }

        // --- 2. Tag type check: only verify the sectors the payload actually needs ---
        // We don't enforce the configured tagType's total sector count — only that every
        // sector referenced by the payload exists on this specific tag.
        val maxRequiredSector = requiredSectors.max()
        if (maxRequiredSector >= actualSectorCount) {
            return PreflightResult.TagTypeMismatch(
                expectedType = tagType.displayName,
                actualSectorCount = actualSectorCount
            )
        }

        // --- 3. Authenticate each required sector with the vendor's keys ---
        // Multiple SectorKey entries with the same sector are tried in sequence.
        val failedSectors = mutableListOf<Int>()
        val keyMap = buildKeyMapFromSectorKeys(keys)

        for (sector in requiredSectors.sorted()) {
            val candidates = keyMap[sector]
            if (candidates.isNullOrEmpty()) {
                Log.w(TAG, "Pre-flight: no keys for sector $sector")
                failedSectors.add(sector)
                continue
            }

            // Try each candidate key pair until one authenticates successfully.
            var sectorOk = false
            for (keyPair in candidates) {
                val miniKeyMap = SparseArray<Array<ByteArray?>>(1).also { map ->
                    map.put(sector, keyPair)
                }
                try {
                    val readResult = reader.readAsMuchAsPossible(miniKeyMap)
                    if (readResult != null && readResult.size() > 0) {
                        sectorOk = true
                        break
                    }
                } catch (e: TagLostException) {
                    return PreflightResult.TagLost
                } catch (e: Exception) {
                    Log.d(TAG, "Pre-flight: candidate key failed for sector $sector", e)
                }
            }
            if (!sectorOk) {
                Log.w(TAG, "Pre-flight: all ${candidates.size} key(s) failed for sector $sector")
                failedSectors.add(sector)
            }
        }

        return if (failedSectors.isEmpty()) {
            PreflightResult.Ready(
                sectorsVerified = requiredSectors.size,
                tagSectorCount = actualSectorCount
            )
        } else {
            PreflightResult.KeyAuthFailed(
                failedSectors = failedSectors,
                totalSectors = requiredSectors.size
            )
        }
    }

    // ===================================================================
    //  WRITE OPERATIONS (unchanged logic, improved error handling)
    // ===================================================================

    private fun writeBlocks(
        reader: MCReader,
        keys: List<SectorKey>,
        payload: PayloadConfig
    ): WriteOperationResult {
        val results = mutableListOf<BlockWriteResult>()
        val keyMap = buildKeyMapFromSectorKeys(keys)

        for (writeBlock in payload.blocks) {
            if (writeBlock.sector == 0 && writeBlock.block == 0 && !payload.writeManufacturerBlock) {
                Log.d(TAG, "Skipping manufacturer block (sector 0, block 0)")
                continue
            }

            val candidates = keyMap.get(writeBlock.sector)
            if (candidates.isNullOrEmpty()) {
                Log.w(TAG, "No key for sector ${writeBlock.sector}, skipping")
                results.add(BlockWriteResult(writeBlock.sector, writeBlock.block, 4))
                continue
            }

            results.add(
                tryWriteWithAllKeys(
                    reader, writeBlock.sector, writeBlock.block,
                    Common.hex2Bytes(writeBlock.data), candidates
                )
            )
        }

        return WriteOperationResult.fromResults(results)
    }

    private fun writeValueBlocks(
        reader: MCReader,
        keys: List<SectorKey>,
        payload: PayloadConfig
    ): WriteOperationResult {
        val results = mutableListOf<BlockWriteResult>()
        val keyMap = buildKeyMapFromSectorKeys(keys)

        for (op in payload.valueBlockOps) {
            val candidates = keyMap.get(op.sector)
            if (candidates.isNullOrEmpty()) {
                results.add(BlockWriteResult(op.sector, op.block, 4))
                continue
            }

            val increment = op.operation == WriteMode.VALUE_BLOCK_INCREMENT
            results.add(
                tryValueBlockWithAllKeys(reader, op.sector, op.block, op.value, increment, candidates)
            )
        }

        return WriteOperationResult.fromResults(results)
    }

    /**
     * Try writeBlock with every candidate key pair (KeyB first, then KeyA per pair)
     * until one succeeds. All candidates for a sector are attempted in list order.
     */
    private fun tryWriteWithAllKeys(
        reader: MCReader,
        sector: Int,
        block: Int,
        data: ByteArray,
        candidates: List<Array<ByteArray?>>
    ): BlockWriteResult {
        val expectedHex = Common.bytes2Hex(data)
        var writeSucceeded = false
        for ((i, keyPair) in candidates.withIndex()) {
            val keyB = keyPair[1]
            val keyA = keyPair[0]

            if (keyB != null) {
                val code = reader.writeBlock(sector, block, data, keyB, true)
                if (code == 0) {
                    writeSucceeded = true
                    if (readBlock(reader, sector, block, keyB, true) == expectedHex) {
                        return BlockWriteResult(sector, block, 0)
                    }
                    Log.w(TAG, "Read-back mismatch S${sector}B${block} with KeyB")
                }
                Log.d(TAG, "Candidate #$i KeyB failed S${sector}B${block} (code=$code)")
            }
            if (keyA != null) {
                val code = reader.writeBlock(sector, block, data, keyA, false)
                if (code == 0) {
                    writeSucceeded = true
                    if (readBlock(reader, sector, block, keyA, false) == expectedHex) {
                        return BlockWriteResult(sector, block, 0)
                    }
                    Log.w(TAG, "Read-back mismatch S${sector}B${block} with KeyA")
                }
                Log.d(TAG, "Candidate #$i KeyA failed S${sector}B${block} (code=$code)")
            }
        }
        Log.w(TAG, "All ${candidates.size} candidate(s) failed for S${sector}B${block}")
        return BlockWriteResult(sector, block, if (writeSucceeded) 5 else 4)
    }

    /**
     * Try writeValueBlock with every candidate key pair until one succeeds.
     */
    private fun tryValueBlockWithAllKeys(
        reader: MCReader,
        sector: Int,
        block: Int,
        value: Int,
        increment: Boolean,
        candidates: List<Array<ByteArray?>>
    ): BlockWriteResult {
        var writeSucceeded = false
        for ((i, keyPair) in candidates.withIndex()) {
            val keyB = keyPair[1]
            val keyA = keyPair[0]

            if (keyB != null) {
                val before = readBlock(reader, sector, block, keyB, true)?.let(::decodeValueBlock)
                if (before != null) {
                    val code = reader.writeValueBlock(sector, block, value, increment, keyB, true)
                    if (code == 0) {
                        writeSucceeded = true
                        val expected = if (increment) before + value else before - value
                        val after = readBlock(reader, sector, block, keyB, true)?.let(::decodeValueBlock)
                        if (after == expected) return BlockWriteResult(sector, block, 0)
                        Log.w(TAG, "Value read-back mismatch S${sector}B${block} with KeyB")
                    }
                    Log.d(TAG, "Candidate #$i KeyB failed VB S${sector}B${block} (code=$code)")
                }
            }
            if (keyA != null) {
                val before = readBlock(reader, sector, block, keyA, false)?.let(::decodeValueBlock)
                if (before != null) {
                    val code = reader.writeValueBlock(sector, block, value, increment, keyA, false)
                    if (code == 0) {
                        writeSucceeded = true
                        val expected = if (increment) before + value else before - value
                        val after = readBlock(reader, sector, block, keyA, false)?.let(::decodeValueBlock)
                        if (after == expected) return BlockWriteResult(sector, block, 0)
                        Log.w(TAG, "Value read-back mismatch S${sector}B${block} with KeyA")
                    }
                    Log.d(TAG, "Candidate #$i KeyA failed VB S${sector}B${block} (code=$code)")
                }
            }
        }
        return BlockWriteResult(sector, block, if (writeSucceeded) 5 else 4)
    }

    // ===================================================================
    //  HELPERS
    // ===================================================================

    private fun readBlock(
        reader: MCReader,
        sector: Int,
        block: Int,
        key: ByteArray,
        useAsKeyB: Boolean
    ): String? = try {
        reader.readSector(sector, key, useAsKeyB)?.getOrNull(block)?.uppercase()
    } catch (e: TagLostException) {
        throw e
    } catch (e: Exception) {
        Log.d(TAG, "Read-back failed S${sector}B${block}", e)
        null
    }

    private fun decodeValueBlock(hex: String): Int? {
        if (!Common.isValueBlock(hex)) return null
        val bytes = Common.hex2Bytes(hex)
        return (bytes[0].toInt() and 0xFF) or
            ((bytes[1].toInt() and 0xFF) shl 8) or
            ((bytes[2].toInt() and 0xFF) shl 16) or
            (bytes[3].toInt() shl 24)
    }

    /**
     * Groups all SectorKey entries by sector into a map of candidate key pairs.
     * Each list entry is Array<ByteArray?>[2]: [0]=KeyA, [1]=KeyB.
     * Multiple entries with the same sector are preserved — the caller
     * iterates through them in order until one authenticates.
     */
    private fun buildKeyMapFromSectorKeys(
        keys: List<SectorKey>
    ): Map<Int, List<Array<ByteArray?>>> {
        val result = mutableMapOf<Int, MutableList<Array<ByteArray?>>>()
        for (sk in keys) {
            val pair = arrayOfNulls<ByteArray>(2)
            sk.keyA?.let { pair[0] = Common.hex2Bytes(it) }
            sk.keyB?.let { pair[1] = Common.hex2Bytes(it) }
            result.getOrPut(sk.sector) { mutableListOf() }.add(pair)
        }
        
        // Always append the default factory key as a fallback for every referenced sector
        // This allows preflight and write operations to succeed on brand new tags.
        val defaultKeyBytes = Common.hex2Bytes(MCReader.DEFAULT_KEY)
        val defaultPair = arrayOf(defaultKeyBytes, defaultKeyBytes)
        
        for ((_, list) in result) {
            // Check if default key is already in the list to avoid duplicate attempts
            var hasDefault = false
            for (pair in list) {
                if (pair[0]?.contentEquals(defaultKeyBytes) == true && 
                    pair[1]?.contentEquals(defaultKeyBytes) == true) {
                    hasDefault = true
                    break
                }
            }
            if (!hasDefault) {
                list.add(defaultPair)
            }
        }
        return result
    }

    /**
     * Safely close the MCReader, swallowing any exceptions.
     */
    private fun safeClose(reader: MCReader) {
        try {
            reader.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error closing MCReader", e)
        }
    }
}
