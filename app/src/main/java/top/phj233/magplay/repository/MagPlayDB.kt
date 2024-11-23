package top.phj233.magplay.repository

import androidx.room.Database
import androidx.room.RoomDatabase
import top.phj233.magplay.entity.Calculator
import top.phj233.magplay.entity.Contact
import top.phj233.magplay.entity.MagnetHistory
import top.phj233.magplay.repository.dao.CalculatorDao
import top.phj233.magplay.repository.dao.ContactDao
import top.phj233.magplay.repository.dao.MagnetHistoryDao

@Database(entities = [
    Calculator::class,
    Contact::class,
    MagnetHistory::class], version = 2)
abstract class MagPlayDB : RoomDatabase() {
    abstract fun calculatorDao(): CalculatorDao
    abstract fun contactDao(): ContactDao
    abstract fun magnetHistoryDao(): MagnetHistoryDao
}