package com.example.qr_scanner.tile

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.example.qr_scanner.MainActivity
import com.example.qr_scanner.R

class QrScannerTileService : TileService() {

    override fun onTileAdded() {
        super.onTileAdded()
        renderTile()
    }

    override fun onStartListening() {
        super.onStartListening()
        renderTile()
    }

    override fun onClick() {
        super.onClick()
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
            putExtra(EXTRA_OPEN_SCANNER, true)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pi = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            startActivityAndCollapse(pi)
        } else {
            @Suppress("DEPRECATION", "StartActivityAndCollapseDeprecated")
            startActivityAndCollapse(intent)
        }
    }

    private fun renderTile() {
        qsTile?.apply {
            state = Tile.STATE_INACTIVE // tile de acción, no de toggle
            label = getString(R.string.tile_label)
            contentDescription = getString(R.string.tile_content_description)
            icon = Icon.createWithResource(this@QrScannerTileService, R.drawable.ic_qr_tile)
            updateTile()
        }
    }

    companion object {
        const val EXTRA_OPEN_SCANNER = "open_scanner"
    }
}
