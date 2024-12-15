package top.phj233.magplay.repository

import android.content.Context
import android.util.Log
import androidx.room.Room

object DBUtil {
    private const val DB_NAME = "magplay.db"
    lateinit var magPlayDB: MagPlayDB

    // 创建数据库
    fun initializeDB(applicationContent: Context): MagPlayDB {
        try {
            magPlayDB = Room.databaseBuilder(applicationContent,
                MagPlayDB::class.java, DB_NAME)
                .allowMainThreadQueries()
                .fallbackToDestructiveMigration()
                .build()
            return magPlayDB
        }catch (e: Exception) {
            Log.e("DBUtil", "createDB: $e")
        }
        return magPlayDB
    }

    fun getCalculatorDao() = magPlayDB.calculatorDao()
    fun getContactDao() = magPlayDB.contactDao()
    fun getDownloadDao() = magPlayDB.downloadDao()
}