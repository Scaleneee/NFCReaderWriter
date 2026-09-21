package com.example.nfcreaderwriter.data.models

import android.nfc.NdefMessage

enum class NfcContentType(val label: String) {
    TEXT("Text"),
    URL("URL"),
    PHONE("Phone number"),
    EMAIL("Email"),
    LOCATION("Location"),
    CONTACT("Contact"),
    WIFI("Wi-Fi"),
    CUSTOM("Custom data")
}

data class NfcRecordInfo(
    val type: String,
    val content: String
)

data class NfcTagInfo(
    val uid: String,
    val technologies: List<String>,
    val ndefSupported: Boolean,
    val capacityBytes: Int?,
    val usedBytes: Int,
    val availableBytes: Int?,
    val writable: Boolean
)

data class NfcReadResult(
    val records: List<NfcRecordInfo>,
    val tagInfo: NfcTagInfo,
    val rawMessage: NdefMessage?
) {
    val mainContent: String
        get() = records.firstOrNull()?.content.orEmpty()
}

sealed interface NfcResult<out T> {
    data class Success<T>(val value: T) : NfcResult<T>
    data class Error(val message: String) : NfcResult<Nothing>
}
