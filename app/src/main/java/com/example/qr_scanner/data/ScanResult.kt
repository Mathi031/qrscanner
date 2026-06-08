package com.example.qr_scanner.data

import android.util.Base64
import android.util.Patterns
import androidx.core.text.util.LinkifyCompat
import android.text.util.Linkify
import android.text.Spannable
import android.text.SpannableString
import android.text.style.URLSpan
import com.google.mlkit.vision.barcode.common.Barcode
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

enum class ContentType { URL, EMAIL, PHONE, WIFI, GEO, TEXT }

@Serializable
data class ScanResult(
    val rawContent: String,
    val detectedType: ContentType,
    val primaryActionTarget: String? = null,
    // Id de fila en el historial; null si es un escaneo recién hecho aún no guardado.
    val id: Long? = null,
) {
    companion object {
        private val json = Json { ignoreUnknownKeys = true }
        private const val BASE64_FLAGS = Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING

        // Longitud máxima para considerar que un contenido "es" un teléfono;
        // evita clasificar como teléfono un texto largo que contenga dígitos.
        private const val MAX_PHONE_LENGTH = 25

        fun from(barcode: Barcode): ScanResult? {
            val rawContent = barcode.rawValue ?: return null
            return classify(rawContent)
        }

        /**
         * Clasifica el contenido crudo en un [ContentType] por orden de
         * prioridad, conservando siempre [rawContent] completo.
         */
        fun classify(rawContent: String): ScanResult {
            val trimmed = rawContent.trim()

            // 1. Wi-Fi (formato estándar de QR Wi-Fi).
            if (trimmed.startsWith("WIFI:", ignoreCase = true)) {
                return ScanResult(rawContent, ContentType.WIFI)
            }

            // 2. Geo.
            if (trimmed.startsWith("geo:", ignoreCase = true)) {
                return ScanResult(rawContent, ContentType.GEO)
            }

            // 3. URL — extrae la primera URL real con Linkify.
            val firstUrl = firstLinkifiedUrl(rawContent)
            if (firstUrl != null) {
                return ScanResult(rawContent, ContentType.URL, primaryActionTarget = firstUrl)
            }
            if (Patterns.WEB_URL.matcher(trimmed).matches()) {
                return ScanResult(rawContent, ContentType.URL, primaryActionTarget = trimmed)
            }

            // 4. Email.
            if (Patterns.EMAIL_ADDRESS.matcher(trimmed).matches()) {
                return ScanResult(rawContent, ContentType.EMAIL, primaryActionTarget = trimmed)
            }
            firstLinkifiedEmail(rawContent)?.let { email ->
                return ScanResult(rawContent, ContentType.EMAIL, primaryActionTarget = email)
            }

            // 5. Teléfono (sólo si el contenido es razonablemente corto).
            if (trimmed.length < MAX_PHONE_LENGTH && Patterns.PHONE.matcher(trimmed).matches()) {
                return ScanResult(rawContent, ContentType.PHONE, primaryActionTarget = trimmed)
            }

            // 6. Texto (fallback).
            return ScanResult(rawContent, ContentType.TEXT)
        }

        /** Devuelve la primera URL (http/https/www) detectada por Linkify, o null. */
        private fun firstLinkifiedUrl(content: String): String? =
            firstSpanUrl(content, Linkify.WEB_URLS)
                ?.let { if (it.startsWith("http", ignoreCase = true)) it else "https://$it" }

        /** Devuelve el primer email detectado por Linkify (sin el prefijo mailto:), o null. */
        private fun firstLinkifiedEmail(content: String): String? =
            firstSpanUrl(content, Linkify.EMAIL_ADDRESSES)?.removePrefix("mailto:")

        private fun firstSpanUrl(content: String, mask: Int): String? {
            val spannable: Spannable = SpannableString(content)
            LinkifyCompat.addLinks(spannable, mask)
            val spans = spannable.getSpans(0, spannable.length, URLSpan::class.java)
            return spans.firstOrNull()?.url
        }

        fun encode(result: ScanResult): String {
            val bytes = json.encodeToString(result).toByteArray(Charsets.UTF_8)
            return Base64.encodeToString(bytes, BASE64_FLAGS)
        }

        fun decode(encoded: String): ScanResult {
            val bytes = Base64.decode(encoded, BASE64_FLAGS)
            return json.decodeFromString(String(bytes, Charsets.UTF_8))
        }
    }
}
