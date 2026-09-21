package com.example.nfcreaderwriter.nfc

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import com.example.nfcreaderwriter.data.models.NfcContentType
import com.example.nfcreaderwriter.data.models.NfcRecordInfo
import java.nio.charset.StandardCharsets
import java.util.Locale

object NdefUtils {
    fun createMessage(type: NfcContentType, content: String): NdefMessage {
        val value = content.trim()
        require(value.isNotEmpty()) { "Enter content to write." }
        val record = when (type) {
            NfcContentType.TEXT -> NdefRecord.createTextRecord(Locale.getDefault().language, value)
            NfcContentType.URL -> NdefRecord.createUri(normalizeUrl(value))
            NfcContentType.PHONE -> NdefRecord.createUri(withScheme(value, "tel:"))
            NfcContentType.EMAIL -> NdefRecord.createUri(withScheme(value, "mailto:"))
            NfcContentType.LOCATION -> NdefRecord.createUri(withScheme(value, "geo:"))
            NfcContentType.CONTACT -> NdefRecord.createMime("text/vcard", value.toByteArray())
            NfcContentType.WIFI -> NdefRecord.createMime("text/plain", value.toByteArray())
            NfcContentType.CUSTOM -> NdefRecord.createMime("text/plain", value.toByteArray())
        }
        return NdefMessage(arrayOf(record))
    }

    fun emptyMessage(): NdefMessage = NdefMessage(
        arrayOf(NdefRecord(NdefRecord.TNF_EMPTY, byteArrayOf(), byteArrayOf(), byteArrayOf()))
    )

    fun parseMessage(message: NdefMessage?): List<NfcRecordInfo> =
        message?.records?.map(::parseRecord).orEmpty()

    private fun parseRecord(record: NdefRecord): NfcRecordInfo {
        if (record.tnf == NdefRecord.TNF_WELL_KNOWN && record.type.contentEquals(NdefRecord.RTD_TEXT)) {
            return NfcRecordInfo("Text", decodeText(record.payload))
        }
        if (record.tnf == NdefRecord.TNF_WELL_KNOWN && record.type.contentEquals(NdefRecord.RTD_URI)) {
            val uri = record.toUri()?.toString().orEmpty()
            val type = when {
                uri.startsWith("mailto:", true) -> "Email"
                uri.startsWith("tel:", true) -> "Phone number"
                uri.startsWith("geo:", true) -> "Location"
                else -> "URL"
            }
            return NfcRecordInfo(type, uri)
        }
        val mime = record.toMimeType()
        if (mime != null) {
            val label = when (mime.lowercase()) {
                "text/vcard" -> "Contact"
                "text/plain" -> if (String(record.payload).startsWith("WIFI:")) "Wi-Fi" else "Custom data"
                else -> "MIME ($mime)"
            }
            return NfcRecordInfo(label, String(record.payload, StandardCharsets.UTF_8))
        }
        val externalType = String(record.type, StandardCharsets.UTF_8)
        return NfcRecordInfo(
            type = if (externalType.isBlank()) "Unknown" else "External ($externalType)",
            content = record.toUri()?.toString()
                ?: String(record.payload, StandardCharsets.UTF_8).ifBlank { "Binary data (${record.payload.size} bytes)" }
        )
    }

    private fun decodeText(payload: ByteArray): String {
        if (payload.isEmpty()) return ""
        // The first NDEF Text byte stores the encoding flag and language-code length.
        val status = payload[0].toInt()
        val languageLength = status and 0x3F
        val charset = if (status and 0x80 == 0) StandardCharsets.UTF_8 else StandardCharsets.UTF_16
        val start = 1 + languageLength
        return if (start <= payload.size) String(payload, start, payload.size - start, charset) else ""
    }

    private fun normalizeUrl(value: String): String =
        if (value.contains("://")) value else "https://$value"

    private fun withScheme(value: String, scheme: String): String =
        if (value.startsWith(scheme, true)) value else "$scheme$value"
}
