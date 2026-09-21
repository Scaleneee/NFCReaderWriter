package com.example.nfcreaderwriter

import android.nfc.NfcAdapter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.nfcreaderwriter.navigation.AppNavigation
import com.example.nfcreaderwriter.ui.theme.NFCReaderWriterTheme

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels { AppViewModel.Factory(application) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state by viewModel.uiState.collectAsState()
            NFCReaderWriterTheme {
                AppNavigation(state = state, viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshNfcStatus()
        val adapter = viewModel.nfcManager.adapter ?: return
        if (!adapter.isEnabled) return

        // Reader Mode exists only while this Activity is visible. The ViewModel and NFC
        // helpers never retain an Activity, which prevents lifecycle-related memory leaks.
        adapter.enableReaderMode(
            this,
            viewModel::onTagDiscovered,
            NfcAdapter.FLAG_READER_NFC_A or
                NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_NFC_F or
                NfcAdapter.FLAG_READER_NFC_V or
                NfcAdapter.FLAG_READER_NFC_BARCODE,
            Bundle().apply {
                // A short presence interval notices a removed tag without excessive polling.
                putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250)
            }
        )
    }

    override fun onPause() {
        viewModel.nfcManager.adapter?.disableReaderMode(this)
        super.onPause()
    }
}
