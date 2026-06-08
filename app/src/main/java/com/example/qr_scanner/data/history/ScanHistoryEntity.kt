package com.example.qr_scanner.data.history

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_history")
data class ScanHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rawContent: String,
    val detectedType: String, // ContentType.name
    val primaryActionTarget: String?,
    val scannedAt: Long, // System.currentTimeMillis()
    val source: String, // "CAMERA" o "GALLERY"
    val isFavorite: Boolean = false,
)
