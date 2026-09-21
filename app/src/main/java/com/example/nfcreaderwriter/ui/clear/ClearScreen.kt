package com.example.nfcreaderwriter.ui.clear

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
import com.example.nfcreaderwriter.ui.components.DestructiveButton
import com.example.nfcreaderwriter.ui.components.ResultBanner
import com.example.nfcreaderwriter.ui.components.SectionCard
import com.example.nfcreaderwriter.ui.components.WaitingCard

@Composable
fun ClearScreen(state: AppUiState, viewModel: AppViewModel, modifier: Modifier = Modifier) {
    LaunchedEffect(Unit) { viewModel.clearOperationMessage() }
    DisposableEffect(Unit) { onDispose { viewModel.cancelSession() } }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SectionCard {
                Text("Clear NFC Tag", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    "This removes all writable NDEF records from the tag. It cannot clear a permanently read-only tag or protected card data.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)
                )
                DestructiveButton(
                    text = "Clear NFC Tag",
                    onClick = { viewModel.beginClear() }
                )
            }
        }
        item { WaitingCard(state.waitingMessage, state.isBusy) }
        item { ResultBanner(state.operationMessage, state.operationSuccessful) }
    }
}
