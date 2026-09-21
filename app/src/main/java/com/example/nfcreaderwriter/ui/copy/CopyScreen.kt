package com.example.nfcreaderwriter.ui.copy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.nfcreaderwriter.ui.components.ResultBanner
import com.example.nfcreaderwriter.ui.components.SectionCard
import com.example.nfcreaderwriter.ui.components.WaitingCard

@Composable
fun CopyScreen(state: AppUiState, viewModel: AppViewModel, modifier: Modifier = Modifier) {
    LaunchedEffect(Unit) { viewModel.beginCopy() }
    DisposableEffect(Unit) { onDispose { viewModel.cancelSession() } }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SectionCard {
                Text("Copy standard NDEF data", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "First scan the source tag, then scan a different writable destination tag. Secure data and UIDs are never copied.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
        state.copySourcePreview?.let { preview ->
            item {
                SectionCard {
                    Text("Source captured", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text(preview, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
        item { WaitingCard(state.waitingMessage, state.isBusy) }
        item { ResultBanner(state.operationMessage, state.operationSuccessful) }
    }
}
