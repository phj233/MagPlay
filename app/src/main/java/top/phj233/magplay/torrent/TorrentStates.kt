package top.phj233.magplay.torrent

sealed class TorrentState {
    data object Idle : TorrentState()
    data object Parsing : TorrentState()
    data class Success(val info: MagPlayTorrentInfo) : TorrentState()
    data class Error(val message: String) : TorrentState()
}