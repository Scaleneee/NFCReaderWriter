package com.example.nfcreaderwriter.data.models

import java.util.UUID

enum class OperationType(val label: String) {
    READ("NFC read"),
    WRITE("NFC write"),
    QR_TO_NFC("QR → NFC"),
    NFC_TO_QR("NFC → QR"),
    COPY("Copy NFC"),
    CLEAR("Clear NFC"),
    LOCK("Lock NFC")
}

data class HistoryItem(
    val id: String = UUID.randomUUID().toString(),
    val operation: OperationType,
    val contentPreview: String,
    val timestamp: Long = System.currentTimeMillis(),
    val successful: Boolean
)
