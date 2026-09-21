package com.example.nfcreaderwriter.ui.read

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.nfcreaderwriter.AppUiState
import com.example.nfcreaderwriter.AppViewModel
import com.example.nfcreaderwriter.ui.components.LabelValue
import com.example.nfcreaderwriter.ui.components.ResultBanner
import com.example.nfcreaderwriter.ui.components.SectionCard
import com.example.nfcreaderwriter.ui.components.WaitingCard

@Composable
fun ReadScreen(state: AppUiState, viewModel: AppViewModel, modifier: Modifier = Modifier) {
    LaunchedEffect(Unit) { viewModel.beginRead() }
    DisposableEffect(Unit) { onDispose { viewModel.cancelSession() } }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { WaitingCard(state.waitingMessage, state.isBusy) }
        item { ResultBanner(state.operationMessage, state.operationSuccessful) }
        state.lastRead?.let { result ->
            item {
                SectionCard {
                    Text("Tag Content", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (result.records.isEmpty()) {
                        Text(
                            "No standard NDEF records were found.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    } else {
                        result.records.forEachIndexed { index, record ->
                            if (index > 0) HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp))
                            Text(record.type, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            Text(record.content, modifier = Modifier.padding(top = 5.dp))
                        }
                    }
                }
            }
            item {
                SectionCard {
                    Text("Tag Information", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Column(modifier = Modifier.padding(top = 10.dp)) {
                        LabelValue("UID", result.tagInfo.uid)
                        LabelValue("Technologies", result.tagInfo.technologies.joinToString(", "))
                        LabelValue("NDEF supported", if (result.tagInfo.ndefSupported) "Yes" else "No")
                        LabelValue("Capacity", result.tagInfo.capacityBytes?.let { "$it bytes" } ?: "Unknown")
                        LabelValue("Used", "${result.tagInfo.usedBytes} bytes")
                        LabelValue("Available", result.tagInfo.availableBytes?.let { "$it bytes" } ?: "Unknown")
                        LabelValue("Writable", if (result.tagInfo.writable) "Yes" else "No")
                    }
                }
            }
        }
    }
}
