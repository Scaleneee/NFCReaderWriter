package com.example.nfcreaderwriter.nfc

import android.content.Context
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.nfc.Tag
import com.example.nfcreaderwriter.data.models.NfcReadResult
import com.example.nfcreaderwriter.data.models.NfcResult

enum class NfcAvailability { READY, DISABLED, UNAVAILABLE }

class NFCManager(context: Context) {
    val adapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(context)

    fun availability(): NfcAvailability = when {
        adapter == null -> NfcAvailability.UNAVAILABLE
        adapter.isEnabled -> NfcAvailability.READY
        else -> NfcAvailability.DISABLED
    }

    fun read(tag: Tag): NfcResult<NfcReadResult> = NFCReader.read(tag)

    fun write(tag: Tag, message: NdefMessage, verify: Boolean): NfcResult<String> =
        NFCWriter.write(tag, message, verify)

    fun clear(tag: Tag, verify: Boolean): NfcResult<String> =
        NFCWriter.write(tag, NdefUtils.emptyMessage(), verify)

    fun makeReadOnly(tag: Tag): NfcResult<String> = NFCWriter.makeReadOnly(tag)
}
