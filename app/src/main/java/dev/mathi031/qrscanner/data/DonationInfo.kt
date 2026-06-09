package dev.mathi031.qrscanner.data

/**
 * Datos fijos de donaciones del desarrollador. Una sola fuente de verdad.
 *
 * El QR de Yape es un sistema cerrado: no se puede generar desde fuera de la app de Yape,
 * por eso se incrusta la imagen oficial ([dev.mathi031.qrscanner.R.drawable.yape_qr]) en vez
 * de generarla en tiempo de ejecución.
 */
object DonationInfo {
    const val PAYPAL_URL = "https://paypal.me/mathi031"
    const val YAPE_NUMBER = "969266730"
    const val YAPE_HOLDER = "George Miguel Puma Salcedo"
}
