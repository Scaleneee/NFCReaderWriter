package com.example.nfcreaderwriter.ui.qr

import android.Manifest
import android.content.pm.PackageManager
import android.view.View
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Nfc
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.nfcreaderwriter.AppUiState
import com.example.nfcreaderwriter.AppViewModel
import com.example.nfcreaderwriter.ui.components.PrimaryButton
import com.example.nfcreaderwriter.ui.components.ResultBanner
import com.example.nfcreaderwriter.ui.components.SecondaryButton
import com.example.nfcreaderwriter.ui.components.SectionCard
import com.example.nfcreaderwriter.ui.components.WaitingCard
import com.google.zxing.BarcodeFormat
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.journeyapps.barcodescanner.DefaultDecoderFactory

@Composable
fun QrToNfcScreen(state: AppUiState, viewModel: AppViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var qrContent by remember { mutableStateOf("") }
    var cameraAllowed by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionDenied by remember { mutableStateOf(false) }
    val currentQrContent by rememberUpdatedState(qrContent)
    val currentResultHandler by rememberUpdatedState<(String) -> Unit> { result ->
        if (qrContent.isBlank()) qrContent = result
    }
    val scannerView = remember(context) {
        DecoratedBarcodeView(context).apply {
            statusView.visibility = View.GONE
            barcodeView.decoderFactory = DefaultDecoderFactory(listOf(BarcodeFormat.QR_CODE))
            decodeContinuous(object : BarcodeCallback {
                override fun barcodeResult(result: BarcodeResult) {
                    val text = result.text.orEmpty()
                    if (text.isNotBlank()) {
                        currentResultHandler(text)
                        pause()
                    }
                }

                override fun possibleResultPoints(resultPoints: List<ResultPoint>) = Unit
            })
        }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        cameraAllowed = granted
        permissionDenied = !granted
        if (granted) scannerView.resume()
    }
    LaunchedEffect(Unit) {
        viewModel.clearOperationMessage()
        if (!cameraAllowed) permission.launch(Manifest.permission.CAMERA)
    }
    DisposableEffect(lifecycleOwner, scannerView, cameraAllowed) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> if (cameraAllowed && currentQrContent.isBlank()) scannerView.resume()
                Lifecycle.Event.ON_PAUSE -> scannerView.pause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (cameraAllowed && qrContent.isBlank()) scannerView.resume()
        onDispose {
            scannerView.pause()
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.cancelSession()
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillParentMaxHeight(if (qrContent.isBlank()) 0.85f else 0.55f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Place the QR code inside the square",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 22.dp)
                    )
                    if (cameraAllowed) {
                        Box(
                            modifier = Modifier
                                .size(292.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color.Black)
                                .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(18.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            AndroidView(
                                factory = { scannerView },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    if (permissionDenied) {
                        Text(
                            "Camera permission is needed to scan a QR code.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 14.dp)
                        )
                        PrimaryButton(
                            text = "Allow camera",
                            onClick = {
                                permissionDenied = false
                                permission.launch(Manifest.permission.CAMERA)
                            }
                        )
                    }
                }
            }
        }
        if (qrContent.isNotBlank()) {
            item {
                SectionCard {
                    Text("Detected content", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text(qrContent, modifier = Modifier.padding(top = 8.dp, bottom = 18.dp))
                    PrimaryButton(
                        text = "Write to NFC",
                        icon = Icons.Outlined.Nfc,
                        enabled = !state.isBusy,
                        onClick = { viewModel.beginQrWrite(qrContent) }
                    )
                    SecondaryButton(
                        text = "Scan another QR code",
                        onClick = {
                            qrContent = ""
                            scannerView.resume()
                        },
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }
            }
        }
        item { WaitingCard(state.waitingMessage, state.isBusy) }
        item { ResultBanner(state.operationMessage, state.operationSuccessful) }
    }
}

@Composable
fun NfcToQrScreen(state: AppUiState, viewModel: AppViewModel, modifier: Modifier = Modifier) {
    LaunchedEffect(Unit) { viewModel.beginRead(forQr = true) }
    DisposableEffect(Unit) { onDispose { viewModel.cancelSession() } }
    val content = state.lastRead?.mainContent.orEmpty()
    val qrImage = remember(content) {
        if (content.isNotBlank()) runCatching { QrCodeGenerator.create(content) }.getOrNull() else null
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { WaitingCard(state.waitingMessage, state.isBusy) }
        item { ResultBanner(state.operationMessage, state.operationSuccessful) }
        if (qrImage != null) {
            item {
                SectionCard {
                    Text("NFC content", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text(content, modifier = Modifier.padding(top = 6.dp, bottom = 18.dp))
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Image(
                            bitmap = qrImage,
                            contentDescription = "QR code containing NFC data",
                            modifier = Modifier.size(280.dp)
                        )
                    }
                    Text(
                        "Keep this screen visible for another device to scan.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }
        }
    }
}
