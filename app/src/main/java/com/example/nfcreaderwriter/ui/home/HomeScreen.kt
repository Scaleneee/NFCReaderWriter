package com.example.nfcreaderwriter.ui.home

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoDelete
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Nfc
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.nfcreaderwriter.nfc.NfcAvailability
import com.example.nfcreaderwriter.ui.components.NfcStatusCard

data class HomeFeature(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val route: String
)

@Composable
fun HomeScreen(
    nfcAvailability: NfcAvailability,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val features = listOf(
        HomeFeature("Read NFC", "Read tag data and details", Icons.Outlined.Nfc, "read"),
        HomeFeature("Write NFC", "Write text, links and more", Icons.Outlined.Edit, "write"),
        HomeFeature("QR → NFC", "Scan a QR code and write it", Icons.Outlined.QrCodeScanner, "qr_to_nfc"),
        HomeFeature("NFC → QR", "Turn tag content into a QR", Icons.Outlined.QrCode, "nfc_to_qr"),
        HomeFeature("Copy NFC", "Copy standard NDEF records", Icons.Outlined.ContentCopy, "copy"),
        HomeFeature("Clear NFC", "Remove writable NDEF data", Icons.Outlined.AutoDelete, "clear")
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(modifier = Modifier.padding(bottom = 8.dp)) {
                Text("NFC Utility", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Read, write and share standard NFC data.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 18.dp)
                )
                NfcStatusCard(nfcAvailability)
            }
        }
        items(features) { feature ->
            FeatureCard(feature = feature, onClick = { onNavigate(feature.route) })
        }
    }
}

@Composable
private fun FeatureCard(feature: HomeFeature, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 150.dp)
            .animateContentSize()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(feature.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(feature.title, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 18.dp))
            Text(
                feature.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 5.dp)
            )
        }
    }
}
