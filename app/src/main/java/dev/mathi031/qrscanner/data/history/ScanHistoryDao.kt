package dev.mathi031.qrscanner.data.history

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanHistoryDao {
    @Query("SELECT * FROM scan_history ORDER BY isFavorite DESC, scannedAt DESC")
    fun observeAll(): Flow<List<ScanHistoryEntity>>

    @Query(
        "SELECT * FROM scan_history WHERE rawContent LIKE '%' || :q || '%' " +
            "ORDER BY isFavorite DESC, scannedAt DESC"
    )
    fun search(q: String): Flow<List<ScanHistoryEntity>>

    @Query("SELECT * FROM scan_history WHERE id = :id")
    suspend fun findById(id: Long): ScanHistoryEntity?

    @Insert
    suspend fun insert(entry: ScanHistoryEntity): Long

    @Query("DELETE FROM scan_history WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE scan_history SET isFavorite = :fav WHERE id = :id")
    suspend fun setFavorite(id: Long, fav: Boolean)

    @Query("DELETE FROM scan_history")
    suspend fun deleteAll()
}
