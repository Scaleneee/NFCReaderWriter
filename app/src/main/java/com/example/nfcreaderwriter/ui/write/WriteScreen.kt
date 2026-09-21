package com.example.nfcreaderwriter.ui.write

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Nfc
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.example.nfcreaderwriter.AppUiState
import com.example.nfcreaderwriter.AppViewModel
import com.example.nfcreaderwriter.data.models.NfcContentType
import com.example.nfcreaderwriter.ui.components.PrimaryButton
import com.example.nfcreaderwriter.ui.components.ResultBanner
import com.example.nfcreaderwriter.ui.components.SectionCard
import com.example.nfcreaderwriter.ui.components.WaitingCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WriteScreen(state: AppUiState, viewModel: AppViewModel, modifier: Modifier = Modifier) {
    var selectedType by remember { mutableStateOf(NfcContentType.TEXT) }
    var content by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { viewModel.clearOperationMessage() }
    DisposableEffect(Unit) { onDispose { viewModel.cancelSession() } }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SectionCard {
                Text("What would you like to write?", style = MaterialTheme.typography.titleMedium)
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.padding(top = 14.dp)
                ) {
                    OutlinedTextField(
                        value = selectedType.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Data type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        NfcContentType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.label) },
                                onClick = {
                                    selectedType = type
                                    expanded = false
                                    content = ""
                                }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text(fieldLabel(selectedType)) },
                    placeholder = { Text(fieldPlaceholder(selectedType)) },
                    minLines = if (selectedType in listOf(NfcContentType.CONTACT, NfcContentType.WIFI, NfcContentType.CUSTOM)) 4 else 1,
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType(selectedType)),
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                )
                Text(
                    "Only standard NDEF data is written. Secure cards and tag UIDs cannot be changed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp, bottom = 16.dp)
                )
                PrimaryButton(
                    text = "Write to NFC",
                    onClick = { viewModel.beginWrite(selectedType, content) },
                    enabled = content.isNotBlank() && !state.isBusy,
                    icon = Icons.Outlined.Nfc
                )
            }
        }
        item { WaitingCard(state.waitingMessage, state.isBusy) }
        item { ResultBanner(state.operationMessage, state.operationSuccessful) }
    }
}

private fun fieldLabel(type: NfcContentType) = when (type) {
    NfcContentType.URL -> "Web address"
    NfcContentType.PHONE -> "Phone number"
    NfcContentType.EMAIL -> "Email address"
    NfcContentType.LOCATION -> "Coordinates"
    NfcContentType.CONTACT -> "vCard data"
    NfcContentType.WIFI -> "Wi-Fi QR payload"
    NfcContentType.CUSTOM -> "Plain data"
    NfcContentType.TEXT -> "Text"
}

private fun fieldPlaceholder(type: NfcContentType) = when (type) {
    NfcContentType.URL -> "https://example.com"
    NfcContentType.PHONE -> "+60 12 345 6789"
    NfcContentType.EMAIL -> "name@example.com"
    NfcContentType.LOCATION -> "3.1390,101.6869"
    NfcContentType.CONTACT -> "BEGIN:VCARD…"
    NfcContentType.WIFI -> "WIFI:T:WPA;S:Network;P:password;;"
    else -> "Enter content"
}

private fun keyboardType(type: NfcContentType) = when (type) {
    NfcContentType.URL -> KeyboardType.Uri
    NfcContentType.PHONE -> KeyboardType.Phone
    NfcContentType.EMAIL -> KeyboardType.Email
    else -> KeyboardType.Text
}
