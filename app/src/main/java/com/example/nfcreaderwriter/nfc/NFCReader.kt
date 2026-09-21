package com.example.nfcreaderwriter.nfc

import android.nfc.Tag
import android.nfc.tech.Ndef
import com.example.nfcreaderwriter.data.models.NfcReadResult
import com.example.nfcreaderwriter.data.models.NfcResult
import com.example.nfcreaderwriter.data.models.NfcTagInfo
import java.io.IOException

object NFCReader {
    fun read(tag: Tag): NfcResult<NfcReadResult> = try {
        val uid = tag.id.joinToString("") { "%02X".format(it) }
        val technologies = tag.techList.map { it.substringAfterLast('.') }
        val ndef = Ndef.get(tag)
        // A tag may be detectable while not exposing standard NDEF data (for example,
        // a protected transit or access card). We report its basic technology only.
        if (ndef == null) {
            NfcResult.Success(
                NfcReadResult(
                    records = emptyList(),
                    tagInfo = NfcTagInfo(uid, technologies, false, null, 0, null, false),
                    rawMessage = null
                )
            )
        } else {
            try {
                ndef.connect()
                val message = ndef.ndefMessage ?: ndef.cachedNdefMessage
                val used = message?.toByteArray()?.size ?: 0
                val capacity = ndef.maxSize
                NfcResult.Success(
                    NfcReadResult(
                        records = NdefUtils.parseMessage(message),
                        tagInfo = NfcTagInfo(
                            uid = uid,
                            technologies = technologies,
                            ndefSupported = true,
                            capacityBytes = capacity,
                            usedBytes = used,
                            availableBytes = (capacity - used).coerceAtLeast(0),
                            writable = ndef.isWritable
                        ),
                        rawMessage = message
                    )
                )
            } finally {
                runCatching { ndef.close() }
            }
        }
    } catch (_: SecurityException) {
        NfcResult.Error("The tag connection was lost. Hold the tag still and try again.")
    } catch (_: IOException) {
        NfcResult.Error("Could not read the tag. Keep it against the phone a little longer.")
    } catch (error: Exception) {
        NfcResult.Error(error.message ?: "This NFC tag could not be read.")
    }
}
