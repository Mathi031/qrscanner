package dev.mathi031.qrscanner.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import dev.mathi031.qrscanner.QrScannerApplication

/** Recupera la [QrScannerApplication] desde los [CreationExtras] de una factory de ViewModel. */
fun CreationExtras.app(): QrScannerApplication =
    (this[APPLICATION_KEY] as QrScannerApplication)
