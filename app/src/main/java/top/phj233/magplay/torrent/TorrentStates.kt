package top.phj233.magplay.torrent

sealed class TorrentState {
    data object Idle : TorrentState()
    data object Parsing : TorrentState()
    data class Success(val info: MagPlayTorrentInfo) : TorrentState()
    data class Error(val message: String) : TorrentState()
}
enum class DownloadStatus {
    PENDING,    // 等待下载
    DOWNLOADING,// 下载中
    PAUSED,     // 已暂停
    COMPLETED,  // 已完成
    ERROR,       // 错误
    DELETED      // 已删除
}