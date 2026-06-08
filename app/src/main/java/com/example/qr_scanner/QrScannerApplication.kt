package com.example.qr_scanner

import android.app.Application
import androidx.room.Room
import com.example.qr_scanner.data.history.AppDatabase
import com.example.qr_scanner.data.history.ScanHistoryRepository
import com.example.qr_scanner.data.prefs.UserPreferencesRepository

class QrScannerApplication : Application() {
    val database by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, "qrscanner.db").build()
    }
    val historyRepository by lazy { ScanHistoryRepository(database.scanHistoryDao()) }
    val preferencesRepository by lazy { UserPreferencesRepository(this) }
}
