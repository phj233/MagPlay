package top.phj233.magplay.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class MagnetHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Int?,
    val magnetLink: String,
    val createAt: String
)
