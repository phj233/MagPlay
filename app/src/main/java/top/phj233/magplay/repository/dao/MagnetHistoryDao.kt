package top.phj233.magplay.repository.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import top.phj233.magplay.entity.MagnetHistory

@Dao
interface MagnetHistoryDao {
    @Insert
    suspend fun insert(history: MagnetHistory)

    @Query("SELECT * FROM MagnetHistory ORDER BY createAt DESC")
    suspend fun getAll(): List<MagnetHistory>

    @Delete
    suspend fun delete(history: MagnetHistory)
}