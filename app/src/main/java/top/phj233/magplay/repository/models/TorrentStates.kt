sealed class TorrentState {
    object Idle : TorrentState()
    object Connecting : TorrentState()
    object Downloading : TorrentState()
    object Completed : TorrentState()
    data class Error(val message: String) : TorrentState()
}