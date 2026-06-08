package com.example.qr_scanner.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.qr_scanner.QrScannerApplication

/** Recupera la [QrScannerApplication] desde los [CreationExtras] de una factory de ViewModel. */
fun CreationExtras.app(): QrScannerApplication =
    (this[APPLICATION_KEY] as QrScannerApplication)
