package top.phj233.magplay.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import top.phj233.magplay.torrent.DownloadStatus

@Entity(tableName = "download_data")
data class DownloadData(
    @PrimaryKey(autoGenerate = true)
    val id: Int?,
    val magnetLink: String,
    val title: String,
    val totalSize: Long,
    val downloadedSize: Long,
    val downloadSpeed: Long,
    val progress: Float,
    val status: DownloadStatus,
    val createAt: String,
    val updateAt: String
)
