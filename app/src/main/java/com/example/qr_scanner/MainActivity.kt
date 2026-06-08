package com.example.qr_scanner

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.qr_scanner.tile.QrScannerTileService
import com.example.qr_scanner.tile.TileAdditionPrompt
import com.example.qr_scanner.ui.donations.DonationsScreen
import com.example.qr_scanner.ui.history.HistoryScreen
import com.example.qr_scanner.ui.result.ResultScreen
import com.example.qr_scanner.ui.scanner.ScannerScreen
import com.example.qr_scanner.ui.settings.SettingsScreen
import com.example.qr_scanner.ui.theme.QRScannerTheme
import kotlinx.coroutines.flow.MutableSharedFlow

class MainActivity : ComponentActivity() {

    // Evento de un solo disparo: "vuelve al scanner". Lo emite el intent del tile.
    private val navigateToScanner = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)
        setContent {
            QRScannerTheme {
                val navController = rememberNavController()

                LaunchedEffect(Unit) {
                    navigateToScanner.collect {
                        val current = navController.currentBackStackEntry?.destination?.route
                        if (current == "scanner") return@collect // ya estamos ahí
                        val popped = navController.popBackStack(route = "scanner", inclusive = false)
                        if (!popped) navController.navigate("scanner")
                    }
                }

                AppNavHost(navController)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(QrScannerTileService.EXTRA_OPEN_SCANNER, false) == true) {
            navigateToScanner.tryEmit(Unit)
        }
    }
}

@Composable
private fun AppNavHost(navController: NavHostController) {
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val onScannerScreen = currentBackStackEntry?.destination?.route == "scanner"

    NavHost(navController = navController, startDestination = "scanner") {
        composable("scanner") {
            ScannerScreen(
                onResult = { encoded ->
                    navController.navigate("result/$encoded")
                },
                onOpenHistory = { navController.navigate("history") },
                onOpenSettings = { navController.navigate("settings") },
            )
        }
        composable(
            route = "result/{encoded}",
            arguments = listOf(navArgument("encoded") { type = NavType.StringType }),
        ) { backStackEntry ->
            val encoded = backStackEntry.arguments?.getString("encoded").orEmpty()
            ResultScreen(
                encoded = encoded,
                onBack = { navController.popBackStack() },
                onScanAgain = {
                    navController.popBackStack(route = "scanner", inclusive = false)
                },
            )
        }
        composable(
            route = "result_history/{id}",
            arguments = listOf(navArgument("id") { type = NavType.LongType }),
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("id") ?: 0L
            ResultScreen(
                historyId = id,
                onBack = { navController.popBackStack() },
                // Desde el historial, "Volver al historial" simplemente vuelve atrás.
                onScanAgain = { navController.popBackStack() },
            )
        }
        composable("history") {
            HistoryScreen(
                onBack = { navController.popBackStack() },
                onOpenItem = { id -> navController.navigate("result_history/$id") },
                onOpenSettings = { navController.navigate("settings") },
            )
        }
        composable("settings") {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenDonations = { navController.navigate("donations") },
            )
        }
        composable("donations") {
            DonationsScreen(onBack = { navController.popBackStack() })
        }
    }

    // Ofrece agregar el tile (sólo Android 13+, una vez, y sólo en el scanner).
    TileAdditionPrompt(visible = onScannerScreen)
}
