package com.example.qr_scanner.data.history

import com.example.qr_scanner.data.ContentType
import com.example.qr_scanner.data.ScanResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ScanSource { CAMERA, GALLERY }

/**
 * Modelo de UI para una fila del historial: lleva el [ScanResult] (para abrir
 * el detalle / acciones) más los metadatos que sólo importan en la lista.
 */
data class HistoryItem(
    val id: Long,
    val scan: ScanResult,
    val scannedAt: Long,
    val source: ScanSource,
    val isFavorite: Boolean,
)

class ScanHistoryRepository(private val dao: ScanHistoryDao) {

    fun observeAll(): Flow<List<HistoryItem>> =
        dao.observeAll().map { list -> list.map { it.toHistoryItem() } }

    fun search(q: String): Flow<List<HistoryItem>> =
        dao.search(q).map { list -> list.map { it.toHistoryItem() } }

    suspend fun getById(id: Long): ScanResult? = dao.findById(id)?.toScanResult()

    suspend fun save(scan: ScanResult, source: ScanSource, now: Long): Long {
        val entity = ScanHistoryEntity(
            rawContent = scan.rawContent,
            detectedType = scan.detectedType.name,
            primaryActionTarget = scan.primaryActionTarget,
            scannedAt = now,
            source = source.name,
        )
        return dao.insert(entity)
    }

    /** Re-inserta una entrada (para "Deshacer" un borrado), conservando sus datos. */
    suspend fun restore(item: HistoryItem): Long {
        val entity = ScanHistoryEntity(
            rawContent = item.scan.rawContent,
            detectedType = item.scan.detectedType.name,
            primaryActionTarget = item.scan.primaryActionTarget,
            scannedAt = item.scannedAt,
            source = item.source.name,
            isFavorite = item.isFavorite,
        )
        return dao.insert(entity)
    }

    suspend fun delete(id: Long) = dao.delete(id)

    suspend fun setFavorite(id: Long, value: Boolean) = dao.setFavorite(id, value)

    suspend fun clearAll() = dao.deleteAll()

    private fun ScanHistoryEntity.toScanResult(): ScanResult = ScanResult(
        rawContent = rawContent,
        detectedType = parseType(detectedType),
        primaryActionTarget = primaryActionTarget,
        id = id,
    )

    private fun ScanHistoryEntity.toHistoryItem(): HistoryItem = HistoryItem(
        id = id,
        scan = toScanResult(),
        scannedAt = scannedAt,
        source = runCatching { ScanSource.valueOf(source) }.getOrDefault(ScanSource.CAMERA),
        isFavorite = isFavorite,
    )

    private fun parseType(name: String): ContentType =
        runCatching { ContentType.valueOf(name) }.getOrDefault(ContentType.TEXT)
}
