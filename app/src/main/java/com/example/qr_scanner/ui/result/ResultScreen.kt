package com.example.qr_scanner.ui.result

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.qr_scanner.data.ContentType
import com.example.qr_scanner.data.ScanResult
import com.example.qr_scanner.ui.copyToClipboard
import com.example.qr_scanner.ui.openUrl
import com.example.qr_scanner.ui.shareText
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ResultScreen(
    onBack: () -> Unit,
    onScanAgain: () -> Unit,
    encoded: String? = null,
    historyId: Long? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Modo histórico: carga por id desde el repositorio. Modo escaneo: decodifica el nav arg.
    val isHistorical = historyId != null
    val parsed: ScanResult?
    if (isHistorical) {
        val vm: ResultViewModel = androidx.lifecycle.viewmodel.compose.viewModel(factory = ResultViewModel.Factory)
        val loaded by vm.loaded.collectAsStateWithLifecycle()
        val loadedResult by vm.result.collectAsStateWithLifecycle()
        LaunchedEffect(historyId) { vm.load(historyId) }
        if (!loaded) {
            // Mientras carga, no renderizamos contenido (evita parpadeo de "no se pudo leer").
            LoadingScaffold(onBack = onBack)
            return
        }
        parsed = loadedResult
    } else {
        parsed = remember(encoded) {
            encoded?.let { runCatching { ScanResult.decode(it) }.getOrNull() }
        }
    }

    var pendingUrl by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Resultado") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver a escanear",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets.systemBars,
    ) { innerPadding ->
        if (parsed == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("No se pudo leer el código.", style = MaterialTheme.typography.bodyLarge)
            }
            return@Scaffold
        }

        val wifi = remember(parsed) {
            if (parsed.detectedType == ContentType.WIFI) parseWifi(parsed.rawContent) else null
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TypeChip(type = parsed.detectedType)

            ContentCard(result = parsed, wifi = wifi)

            ActionGrid(
                result = parsed,
                wifi = wifi,
                onCopy = { label, value ->
                    copyToClipboard(context, label, value)
                    scope.launch { snackbarHostState.showSnackbar("Copiado al portapapeles") }
                },
                onShare = { value -> shareText(context, value) },
                onOpenUrl = { url ->
                    val host = runCatching { Uri.parse(url).host.orEmpty() }.getOrDefault("")
                    if (urlSafetyReasons(host, url).isEmpty()) {
                        openUrl(context, url)
                    } else {
                        pendingUrl = url
                    }
                },
                onSendEmail = { email -> openEmail(context, email) },
                onCall = { phone -> openDial(context, phone) },
                onSms = { phone -> openSms(context, phone) },
                onOpenGeo = { geo -> openGeo(context, geo) },
                onConnectWifi = {
                    val message = connectWifi(context, wifi)
                    scope.launch { snackbarHostState.showSnackbar(message) }
                },
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = onScanAgain,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                // Texto contextual: desde el historial "Volver", desde un escaneo "Escanear otro".
                Text(if (isHistorical) "Volver al historial" else "Escanear otro")
            }
        }
    }

    val urlToConfirm = pendingUrl
    if (urlToConfirm != null) {
        UrlSafetyDialog(
            url = urlToConfirm,
            onConfirm = {
                pendingUrl = null
                openUrl(context, urlToConfirm)
            },
            onDismiss = { pendingUrl = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoadingScaffold(onBack: () -> Unit) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Resultado") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                        )
                    }
                },
            )
        },
        contentWindowInsets = WindowInsets.systemBars,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.material3.CircularProgressIndicator()
        }
    }
}

@Composable
private fun TypeChip(type: ContentType) {
    val (label, icon) = when (type) {
        ContentType.URL -> "Enlace" to Icons.AutoMirrored.Filled.OpenInNew
        ContentType.EMAIL -> "Email" to Icons.Filled.Email
        ContentType.PHONE -> "Teléfono" to Icons.Filled.Call
        ContentType.WIFI -> "Wi-Fi" to Icons.Filled.Wifi
        ContentType.GEO -> "Ubicación" to Icons.Filled.LocationOn
        ContentType.TEXT -> "Texto" to Icons.Filled.ContentCopy
    }
    AssistChip(
        onClick = {},
        label = { Text(label) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            leadingIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
        },
    )
}

@Composable
private fun ContentCard(result: ScanResult, wifi: WifiInfo?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (wifi != null) {
                LabeledRow("Red (SSID)", wifi.ssid.ifEmpty { "(desconocida)" })
                LabeledRow("Seguridad", wifi.encryption.ifEmpty { "(abierta)" })
                LabeledRow(
                    "Contraseña",
                    if (wifi.password.isEmpty()) "(sin contraseña)" else "•".repeat(wifi.password.length.coerceAtMost(16)),
                )
            }
            Text(
                text = "Contenido completo",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SelectionContainer {
                Text(
                    text = result.rawContent,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun LabeledRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SelectionContainer {
            Text(text = value, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActionGrid(
    result: ScanResult,
    wifi: WifiInfo?,
    onCopy: (label: String, value: String) -> Unit,
    onShare: (String) -> Unit,
    onOpenUrl: (String) -> Unit,
    onSendEmail: (String) -> Unit,
    onCall: (String) -> Unit,
    onSms: (String) -> Unit,
    onOpenGeo: (String) -> Unit,
    onConnectWifi: () -> Unit,
) {
    val target = result.primaryActionTarget
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when (result.detectedType) {
            ContentType.URL -> {
                if (!target.isNullOrEmpty()) {
                    ActionButton("Abrir enlace", Icons.AutoMirrored.Filled.OpenInNew) {
                        onOpenUrl(target)
                    }
                }
            }
            ContentType.EMAIL -> {
                if (!target.isNullOrEmpty()) {
                    ActionButton("Enviar email", Icons.Filled.Email) { onSendEmail(target) }
                }
            }
            ContentType.PHONE -> {
                if (!target.isNullOrEmpty()) {
                    ActionButton("Llamar", Icons.Filled.Call) { onCall(target) }
                    ActionButton("Enviar SMS", Icons.Filled.Sms) { onSms(target) }
                }
            }
            ContentType.WIFI -> {
                val password = wifi?.password.orEmpty()
                if (password.isNotEmpty()) {
                    ActionButton("Copiar contraseña", Icons.Filled.Lock) {
                        onCopy("Contraseña Wi-Fi", password)
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    ActionButton("Conectar", Icons.Filled.Wifi) { onConnectWifi() }
                }
            }
            ContentType.GEO -> {
                ActionButton("Ver en mapa", Icons.Filled.LocationOn) { onOpenGeo(result.rawContent) }
            }
            ContentType.TEXT -> Unit
        }
        ActionButton("Copiar", Icons.Filled.ContentCopy) {
            onCopy("Contenido QR", result.rawContent)
        }
        ActionButton("Compartir", Icons.Filled.Share) { onShare(result.rawContent) }
    }
}

@Composable
private fun ActionButton(label: String, icon: ImageVector, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier.heightIn(min = 48.dp),
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.size(8.dp))
        Text(text = label)
    }
}

@Composable
private fun UrlSafetyDialog(
    url: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val host = remember(url) { runCatching { Uri.parse(url).host.orEmpty() }.getOrDefault("") }
    val reasons = remember(host, url) { urlSafetyReasons(host, url) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("¿Continuar al enlace?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Dominio: $host", style = MaterialTheme.typography.bodyMedium)
                reasons.forEach { reason ->
                    Text("• $reason", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Continuar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}

// -------- Wi-Fi parsing (formato estándar QR: WIFI:S:<ssid>;T:<enc>;P:<pwd>;H:<hidden>;;) --------

private data class WifiInfo(
    val ssid: String,
    val password: String,
    val encryption: String,
)

private fun parseWifi(raw: String): WifiInfo {
    // Quita el prefijo y separa por ';' respetando '\;' escapados.
    val body = raw.trim().removePrefix("WIFI:").removePrefix("wifi:")
    val fields = mutableMapOf<String, String>()
    val sb = StringBuilder()
    var key: String? = null
    var i = 0
    while (i < body.length) {
        val c = body[i]
        when {
            c == '\\' && i + 1 < body.length -> {
                sb.append(body[i + 1]); i += 2; continue
            }
            c == ':' && key == null -> {
                key = sb.toString(); sb.clear()
            }
            c == ';' -> {
                if (key != null) fields[key.uppercase()] = sb.toString()
                key = null; sb.clear()
            }
            else -> sb.append(c)
        }
        i++
    }
    if (key != null) fields[key.uppercase()] = sb.toString()
    return WifiInfo(
        ssid = fields["S"].orEmpty(),
        password = fields["P"].orEmpty(),
        encryption = fields["T"].orEmpty(),
    )
}

private val SHORTENER_HOSTS = setOf(
    "bit.ly", "tinyurl.com", "t.co", "goo.gl", "ow.ly", "is.gd", "buff.ly",
    "shorturl.at", "rebrand.ly", "cutt.ly",
)

private fun urlSafetyReasons(host: String, url: String): List<String> {
    val reasons = mutableListOf<String>()
    val normalized = host.lowercase()
    if (normalized in SHORTENER_HOSTS) {
        reasons += "Es un acortador de enlaces; el destino real está oculto."
    }
    if (host.startsWith("xn--") || host.contains(".xn--")) {
        reasons += "El dominio usa codificación punycode (puede imitar otro sitio)."
    }
    if (host.any { it.code > 127 }) {
        reasons += "El dominio contiene caracteres no ASCII (riesgo de homógrafo)."
    }
    if (url.any { it.code in 0x200B..0x200F || it.code == 0x202E }) {
        reasons += "La URL contiene caracteres invisibles o de control."
    }
    return reasons
}

private fun openEmail(context: Context, address: String) {
    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + Uri.encode(address))).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(intent) }
}

private fun openDial(context: Context, number: String) {
    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(number))).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(intent) }
}

private fun openSms(context: Context, number: String) {
    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + Uri.encode(number))).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(intent) }
}

private fun openGeo(context: Context, geoUri: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(geoUri)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(intent) }
}

private fun connectWifi(context: Context, wifi: WifiInfo?): String {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
        return "Conectar requiere Android 11 o superior."
    }
    if (wifi == null) return "No se pudo leer la red Wi-Fi."
    val ssid = wifi.ssid.takeIf { it.isNotEmpty() } ?: return "Falta el SSID."
    val builder = WifiNetworkSuggestion.Builder().setSsid(ssid)
    val pwd = wifi.password
    val encryption = wifi.encryption.uppercase()
    when {
        encryption.contains("WPA3") && pwd.isNotEmpty() -> builder.setWpa3Passphrase(pwd)
        encryption.contains("WPA") && pwd.isNotEmpty() -> builder.setWpa2Passphrase(pwd)
        // Red abierta: sin passphrase.
    }
    val suggestion = builder.build()
    val manager = context.getSystemService(Context.WIFI_SERVICE) as? android.net.wifi.WifiManager
        ?: return "No se pudo acceder al servicio Wi-Fi."
    val status = manager.addNetworkSuggestions(listOf(suggestion))
    return if (status == android.net.wifi.WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS) {
        "Red sugerida. Acepta la notificación del sistema para conectar."
    } else {
        "No se pudo añadir la sugerencia de red (código $status)."
    }
}
