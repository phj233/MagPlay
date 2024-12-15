package top.phj233.magplay.repository.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import top.phj233.magplay.entity.DownloadData
import top.phj233.magplay.torrent.DownloadStatus

@Dao
interface DownloadDao {
    @Query("SELECT * FROM download_data ORDER BY createAt DESC")
    fun getAllDownloads(): Flow<List<DownloadData>>

    @Query("SELECT * FROM download_data WHERE status = :status ORDER BY createAt DESC")
    fun getDownloadsByStatus(status: DownloadStatus): Flow<List<DownloadData>>

    @Query("SELECT * FROM download_data WHERE id = :id")
    suspend fun getDownloadById(id: Int): DownloadData?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(download: DownloadData)

    @Update
    suspend fun updateDownload(download: DownloadData)

    @Delete
    suspend fun deleteDownload(download: DownloadData)

    @Query("DELETE FROM download_data WHERE status = :status")
    suspend fun deleteDownloadsByStatus(status: DownloadStatus)
}
