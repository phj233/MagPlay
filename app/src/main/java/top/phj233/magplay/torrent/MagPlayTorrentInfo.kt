package top.phj233.magplay.torrent

data class MagPlayTorrentInfo(
    val name: String,
    val totalSize: Long,
    val numFiles: Int,
    val files: List<TorrentFile>,
    val infoHash: String,
    val creationDate: Long? = null,
    val comment: String? = null,
    val creator: String? = null
)

data class TorrentFile(
    val name: String,
    val path: String,
    val size: Long,
    val priority: Int? = 0
)