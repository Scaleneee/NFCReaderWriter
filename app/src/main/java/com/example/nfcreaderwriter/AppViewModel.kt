package com.example.nfcreaderwriter

import android.app.Application
import android.nfc.NdefMessage
import android.nfc.Tag
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.nfcreaderwriter.data.local.AppPreferences
import com.example.nfcreaderwriter.data.models.HistoryItem
import com.example.nfcreaderwriter.data.models.NfcContentType
import com.example.nfcreaderwriter.data.models.NfcReadResult
import com.example.nfcreaderwriter.data.models.NfcResult
import com.example.nfcreaderwriter.data.models.OperationType
import com.example.nfcreaderwriter.nfc.NFCManager
import com.example.nfcreaderwriter.nfc.NdefUtils
import com.example.nfcreaderwriter.nfc.NfcAvailability
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AppUiState(
    val nfcAvailability: NfcAvailability = NfcAvailability.UNAVAILABLE,
    val waitingMessage: String? = null,
    val isBusy: Boolean = false,
    val operationMessage: String? = null,
    val operationSuccessful: Boolean? = null,
    val lastRead: NfcReadResult? = null,
    val copySourcePreview: String? = null,
    val history: List<HistoryItem> = emptyList(),
    val automaticallyVerify: Boolean = true,
    val saveHistory: Boolean = true
)

private sealed interface SessionMode {
    data object Idle : SessionMode
    data class Read(val forQr: Boolean) : SessionMode
    data class Write(
        val message: NdefMessage,
        val operation: OperationType,
        val preview: String
    ) : SessionMode
    data object CopySource : SessionMode
    data class CopyDestination(val message: NdefMessage, val preview: String) : SessionMode
    data object Clear : SessionMode
}

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = AppPreferences(application)
    val nfcManager = NFCManager(application)
    private var sessionMode: SessionMode = SessionMode.Idle

    private val _uiState = MutableStateFlow(
        AppUiState(
            nfcAvailability = nfcManager.availability(),
            history = preferences.loadHistory(),
            automaticallyVerify = preferences.automaticallyVerify,
            saveHistory = preferences.saveHistory
        )
    )
    val uiState = _uiState.asStateFlow()

    fun refreshNfcStatus() {
        _uiState.update { it.copy(nfcAvailability = nfcManager.availability()) }
    }

    fun beginRead(forQr: Boolean = false) {
        if (!ensureNfcReady()) return
        sessionMode = SessionMode.Read(forQr)
        _uiState.update {
            it.copy(
                waitingMessage = "Hold an NFC tag near your phone",
                operationMessage = null,
                operationSuccessful = null,
                lastRead = null
            )
        }
    }

    fun beginWrite(type: NfcContentType, content: String, operation: OperationType = OperationType.WRITE) {
        val message = runCatching { NdefUtils.createMessage(type, content) }
            .getOrElse {
                showError(it.message ?: "Enter valid content.")
                return
            }
        beginMessageWrite(message, operation, content)
    }

    fun beginQrWrite(content: String) {
        val type = if (content.trim().startsWith("http://", true) || content.trim().startsWith("https://", true)) {
            NfcContentType.URL
        } else {
            NfcContentType.TEXT
        }
        beginWrite(type, content, OperationType.QR_TO_NFC)
    }

    private fun beginMessageWrite(message: NdefMessage, operation: OperationType, preview: String) {
        if (!ensureNfcReady()) return
        sessionMode = SessionMode.Write(message, operation, preview)
        _uiState.update {
            it.copy(
                waitingMessage = "Ready to write — hold a writable NFC tag near your phone",
                operationMessage = null,
                operationSuccessful = null
            )
        }
    }

    fun beginCopy() {
        if (!ensureNfcReady()) return
        sessionMode = SessionMode.CopySource
        _uiState.update {
            it.copy(
                waitingMessage = "Scan the source NFC tag",
                operationMessage = null,
                operationSuccessful = null,
                copySourcePreview = null
            )
        }
    }

    fun beginClear() {
        if (!ensureNfcReady()) return
        sessionMode = SessionMode.Clear
        _uiState.update {
            it.copy(
                waitingMessage = "Hold the NFC tag you want to clear near your phone",
                operationMessage = null,
                operationSuccessful = null
            )
        }
    }

    fun cancelSession() {
        sessionMode = SessionMode.Idle
        _uiState.update { it.copy(waitingMessage = null, isBusy = false) }
    }

    fun clearOperationMessage() {
        _uiState.update { it.copy(operationMessage = null, operationSuccessful = null) }
    }

    fun onTagDiscovered(tag: Tag) {
        val mode = sessionMode
        if (mode is SessionMode.Idle || _uiState.value.isBusy) return
        _uiState.update { it.copy(isBusy = true, waitingMessage = "Tag detected — keep it still…") }
        viewModelScope.launch(Dispatchers.IO) {
            when (mode) {
                is SessionMode.Read -> handleRead(tag, mode.forQr)
                is SessionMode.Write -> handleWrite(tag, mode.message, mode.operation, mode.preview)
                SessionMode.CopySource -> handleCopySource(tag)
                is SessionMode.CopyDestination -> handleWrite(tag, mode.message, OperationType.COPY, mode.preview)
                SessionMode.Clear -> handleClear(tag)
                SessionMode.Idle -> Unit
            }
        }
    }

    private fun handleRead(tag: Tag, forQr: Boolean) {
        when (val result = nfcManager.read(tag)) {
            is NfcResult.Success -> {
                val read = result.value
                val hasQrContent = !forQr || read.mainContent.isNotBlank()
                if (hasQrContent) {
                    sessionMode = SessionMode.Idle
                    _uiState.update {
                        it.copy(
                            lastRead = read,
                            isBusy = false,
                            waitingMessage = null,
                            operationMessage = if (forQr) "QR code generated from the NFC content." else "Tag read successfully",
                            operationSuccessful = true
                        )
                    }
                    addHistory(
                        if (forQr) OperationType.NFC_TO_QR else OperationType.READ,
                        read.mainContent.ifBlank { "Empty or non-NDEF tag" },
                        true
                    )
                } else {
                    finishWithError("This tag has no readable NDEF content to convert.", OperationType.NFC_TO_QR)
                }
            }
            is NfcResult.Error -> finishWithError(result.message, if (forQr) OperationType.NFC_TO_QR else OperationType.READ)
        }
    }

    private fun handleWrite(tag: Tag, message: NdefMessage, operation: OperationType, preview: String) {
        when (val result = nfcManager.write(tag, message, _uiState.value.automaticallyVerify)) {
            is NfcResult.Success -> finishWithSuccess(result.value, operation, preview)
            is NfcResult.Error -> finishWithError(result.message, operation, preview)
        }
    }

    private fun handleCopySource(tag: Tag) {
        when (val result = nfcManager.read(tag)) {
            is NfcResult.Success -> {
                val message = result.value.rawMessage
                if (message == null || result.value.records.isEmpty()) {
                    finishWithError("The source tag has no NDEF records to copy.", OperationType.COPY)
                } else {
                    // Copy only the public NDEF message. Android does not expose or rewrite tag UIDs.
                    val preview = result.value.records.joinToString(" • ") { it.content }.take(180)
                    sessionMode = SessionMode.CopyDestination(message, preview)
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            waitingMessage = "Source read. Now scan the destination NFC tag",
                            copySourcePreview = preview,
                            operationMessage = "Source tag captured",
                            operationSuccessful = true
                        )
                    }
                }
            }
            is NfcResult.Error -> finishWithError(result.message, OperationType.COPY)
        }
    }

    private fun handleClear(tag: Tag) {
        when (val result = nfcManager.clear(tag, _uiState.value.automaticallyVerify)) {
            is NfcResult.Success -> finishWithSuccess("NFC tag cleared successfully", OperationType.CLEAR, "NDEF data removed")
            is NfcResult.Error -> finishWithError(result.message, OperationType.CLEAR)
        }
    }

    private fun finishWithSuccess(message: String, operation: OperationType, preview: String) {
        sessionMode = SessionMode.Idle
        _uiState.update {
            it.copy(isBusy = false, waitingMessage = null, operationMessage = message, operationSuccessful = true)
        }
        addHistory(operation, preview, true)
    }

    private fun finishWithError(message: String, operation: OperationType, preview: String = message) {
        sessionMode = SessionMode.Idle
        _uiState.update {
            it.copy(isBusy = false, waitingMessage = null, operationMessage = message, operationSuccessful = false)
        }
        addHistory(operation, preview, false)
    }

    private fun showError(message: String) {
        _uiState.update { it.copy(operationMessage = message, operationSuccessful = false) }
    }

    private fun ensureNfcReady(): Boolean {
        refreshNfcStatus()
        val availability = _uiState.value.nfcAvailability
        if (availability == NfcAvailability.READY) return true
        showError(
            if (availability == NfcAvailability.DISABLED) "NFC is disabled. Turn it on in Android settings."
            else "This device does not support NFC."
        )
        return false
    }

    private fun addHistory(operation: OperationType, preview: String, successful: Boolean) {
        val updated = preferences.addHistory(
            HistoryItem(
                operation = operation,
                contentPreview = preview.replace('\n', ' ').take(180),
                successful = successful
            )
        )
        _uiState.update { it.copy(history = updated) }
    }

    fun setAutomaticallyVerify(value: Boolean) {
        preferences.automaticallyVerify = value
        _uiState.update { it.copy(automaticallyVerify = value) }
    }

    fun setSaveHistory(value: Boolean) {
        preferences.saveHistory = value
        _uiState.update { it.copy(saveHistory = value) }
    }

    fun clearHistory() {
        preferences.clearHistory()
        _uiState.update { it.copy(history = emptyList()) }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = AppViewModel(application) as T
    }
}
