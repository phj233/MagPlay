package top.phj233.magplay.repository.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import top.phj233.magplay.entity.Calculator

@Dao
interface CalculatorDao {
    @Insert
    fun insertAll(vararg calculator: Calculator)

    @Delete
    fun delete(calculator: Calculator)

    @Query("SELECT * FROM calculator")
    fun getAll(): Flow<List<Calculator>>

    @Update
    fun update(calculator: Calculator)

}