package dev.mathi031.qrscanner.tile

import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import dev.mathi031.qrscanner.R

private const val PREFS_NAME = "qr_scanner_prefs"
private const val KEY_HAS_PROMPTED_FOR_TILE = "has_prompted_for_tile"

/**
 * En Android 13+ (TIRAMISU) ofrece, una sola vez, agregar el tile de Ajustes
 * rápidos mediante el diálogo nativo del sistema (`requestAddTileService`).
 * En versiones anteriores no muestra nada (el usuario agrega el tile a mano).
 *
 * @param visible si la pantalla actual es la del scanner (sólo entonces se ofrece).
 */
@Composable
fun TileAdditionPrompt(visible: Boolean) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    var showDialog by remember {
        mutableStateOf(!prefs.getBoolean(KEY_HAS_PROMPTED_FOR_TILE, false))
    }

    if (!visible || !showDialog) return

    fun dismissForever() {
        prefs.edit().putBoolean(KEY_HAS_PROMPTED_FOR_TILE, true).apply()
        showDialog = false
    }

    AlertDialog(
        onDismissRequest = { dismissForever() },
        title = { Text(stringResource(R.string.add_tile_prompt_title)) },
        text = { Text(stringResource(R.string.add_tile_prompt_message)) },
        confirmButton = {
            TextButton(onClick = {
                requestAddTile(context)
                dismissForever()
            }) {
                Text(stringResource(R.string.add_tile_prompt_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = { dismissForever() }) {
                Text(stringResource(R.string.add_tile_prompt_dismiss))
            }
        },
    )
}

@androidx.annotation.RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun requestAddTile(context: Context) {
    val sbm = context.getSystemService(StatusBarManager::class.java) ?: return
    runCatching {
        sbm.requestAddTileService(
            ComponentName(context, QrScannerTileService::class.java),
            context.getString(R.string.tile_label),
            Icon.createWithResource(context, R.drawable.ic_qr_tile),
            context.mainExecutor,
            { /* El resultado no cambia nada: ya marcamos hasPrompted. */ },
        )
    }
}
