package top.phj233.magplay.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Calculator(
    @PrimaryKey(autoGenerate = true)
    val uid: Int?,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "age")
    val sex: String,
    @ColumnInfo(name = "weight")
    val weight: Double,
    @ColumnInfo(name = "height")
    val height: Double,
    @ColumnInfo(name = "create_time")
    val createTime: String
    )
