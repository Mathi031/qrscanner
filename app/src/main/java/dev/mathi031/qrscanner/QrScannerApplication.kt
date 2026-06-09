package dev.mathi031.qrscanner

import android.app.Application
import androidx.room.Room
import dev.mathi031.qrscanner.data.history.AppDatabase
import dev.mathi031.qrscanner.data.history.ScanHistoryRepository
import dev.mathi031.qrscanner.data.prefs.UserPreferencesRepository

class QrScannerApplication : Application() {
    val database by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, "qrscanner.db").build()
    }
    val historyRepository by lazy { ScanHistoryRepository(database.scanHistoryDao()) }
    val preferencesRepository by lazy { UserPreferencesRepository(this) }
}
