package com.example.qr_scanner.util

import android.content.Context
import android.os.Build
import android.telephony.TelephonyManager
import java.util.Locale

/**
 * Indica si el dispositivo parece estar en Perú. Se usa para mostrar la opción de Yape
 * (que solo existe en Perú) sin pedir ningún permiso nuevo.
 *
 * Combina varias señales y devuelve `true` si **cualquiera** apunta a Perú (`PE`):
 *  1. País de la red móvil ([TelephonyManager.getNetworkCountryIso]).
 *  2. País de la SIM ([TelephonyManager.getSimCountryIso]).
 *  3. País del locale del dispositivo (fallback para tablets/Wi-Fi sin SIM).
 *
 * Los getters de país de [TelephonyManager] no requieren permiso.
 */
fun isPeru(context: Context): Boolean {
    val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
    val candidates = buildList {
        tm?.let {
            runCatching { add(it.networkCountryIso) }
            runCatching { add(it.simCountryIso) }
        }
        add(deviceLocale(context).country)
    }
    return candidates.any { it?.equals("pe", ignoreCase = true) == true }
}

private fun deviceLocale(context: Context): Locale {
    val config = context.resources.configuration
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        config.locales[0]
    } else {
        @Suppress("DEPRECATION")
        config.locale
    } ?: Locale.getDefault()
}
