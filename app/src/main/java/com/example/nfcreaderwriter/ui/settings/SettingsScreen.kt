package com.example.nfcreaderwriter.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.nfcreaderwriter.AppUiState
import com.example.nfcreaderwriter.nfc.NfcAvailability
import com.example.nfcreaderwriter.ui.components.NfcStatusCard
import com.example.nfcreaderwriter.ui.components.SecondaryButton
import com.example.nfcreaderwriter.ui.components.SectionCard

@Composable
fun SettingsScreen(
    state: AppUiState,
    onOpenNfcSettings: () -> Unit,
    onVerifyChanged: (Boolean) -> Unit,
    onSaveHistoryChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { NfcStatusCard(state.nfcAvailability) }
        if (state.nfcAvailability != NfcAvailability.UNAVAILABLE) {
            item { SecondaryButton("Open Android NFC settings", onClick = onOpenNfcSettings) }
        }
        item {
            SectionCard {
                Text("Preferences", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                SettingToggle(
                    title = "Automatically verify after writing",
                    description = "Read the NDEF message back and compare it after every write.",
                    checked = state.automaticallyVerify,
                    onCheckedChange = onVerifyChanged
                )
                SettingToggle(
                    title = "Save operation history",
                    description = "Keep recent operation results on this device only.",
                    checked = state.saveHistory,
                    onCheckedChange = onSaveHistoryChanged
                )
            }
        }
        item {
            SectionCard {
                Text("About", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "NFC Reader Writer is a personal utility for standard writable NDEF tags such as NTAG213, NTAG215 and NTAG216.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    "It does not clone secure access cards, payment cards, transit cards, protected sectors or tag UIDs.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun SettingToggle(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, fontWeight = FontWeight.Medium)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
