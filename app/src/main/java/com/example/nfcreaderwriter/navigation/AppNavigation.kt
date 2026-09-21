package com.example.nfcreaderwriter.navigation

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.nfcreaderwriter.AppUiState
import com.example.nfcreaderwriter.AppViewModel
import com.example.nfcreaderwriter.ui.clear.ClearScreen
import com.example.nfcreaderwriter.ui.copy.CopyScreen
import com.example.nfcreaderwriter.ui.history.HistoryScreen
import com.example.nfcreaderwriter.ui.home.HomeScreen
import com.example.nfcreaderwriter.ui.qr.NfcToQrScreen
import com.example.nfcreaderwriter.ui.qr.QrToNfcScreen
import com.example.nfcreaderwriter.ui.read.ReadScreen
import com.example.nfcreaderwriter.ui.settings.SettingsScreen
import com.example.nfcreaderwriter.ui.write.WriteScreen

private data class TopDestination(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val topDestinations = listOf(
    TopDestination("home", "Home", Icons.Outlined.Home),
    TopDestination("history", "History", Icons.Outlined.History),
    TopDestination("settings", "Settings", Icons.Outlined.Settings)
)

private val routeTitles = mapOf(
    "home" to "NFC Reader Writer",
    "history" to "History",
    "settings" to "Settings",
    "read" to "Read NFC",
    "write" to "Write NFC",
    "qr_to_nfc" to "Scan QR Code",
    "nfc_to_qr" to "NFC to QR",
    "copy" to "Copy NFC",
    "clear" to "Clear NFC"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(state: AppUiState, viewModel: AppViewModel) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route ?: "home"
    val isTopDestination = route in topDestinations.map { it.route }
    val context = LocalContext.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(routeTitles[route].orEmpty(), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    if (!isTopDestination) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            if (isTopDestination) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    topDestinations.forEach { destination ->
                        val selected = backStackEntry?.destination?.hierarchy?.any { it.route == destination.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo("home") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(destination.icon, contentDescription = null) },
                            label = { Text(destination.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(padding),
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(240)) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(240)) },
            popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(240)) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(240)) }
        ) {
            composable("home") { HomeScreen(state.nfcAvailability, navController::navigate) }
            composable("history") { HistoryScreen(state.history, viewModel::clearHistory) }
            composable("settings") {
                SettingsScreen(
                    state = state,
                    onOpenNfcSettings = {
                        val intent = Intent(Settings.ACTION_NFC_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        runCatching { context.startActivity(intent) }.onFailure {
                            context.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        }
                    },
                    onVerifyChanged = viewModel::setAutomaticallyVerify,
                    onSaveHistoryChanged = viewModel::setSaveHistory
                )
            }
            composable("read") { ReadScreen(state, viewModel) }
            composable("write") { WriteScreen(state, viewModel) }
            composable("qr_to_nfc") { QrToNfcScreen(state, viewModel) }
            composable("nfc_to_qr") { NfcToQrScreen(state, viewModel) }
            composable("copy") { CopyScreen(state, viewModel) }
            composable("clear") { ClearScreen(state, viewModel) }
        }
    }
}
