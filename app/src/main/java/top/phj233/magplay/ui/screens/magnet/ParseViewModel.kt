package top.phj233.magplay.ui.screens.magnet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import top.phj233.magplay.torrent.TorrentManager
import top.phj233.magplay.torrent.TorrentState

class ParseViewModel : ViewModel() {
    private val _torrentState = MutableStateFlow<TorrentState>(TorrentState.Idle)
    val torrentState: StateFlow<TorrentState> = _torrentState

    private val _downloadProgress = MutableStateFlow<Map<Int, Float>>(emptyMap())
    val downloadProgress: StateFlow<Map<Int, Float>> = _downloadProgress

    private val _downloadSpeed = MutableStateFlow<Map<Int, Long>>(emptyMap())
    val downloadSpeed: StateFlow<Map<Int, Long>> = _downloadSpeed

    fun parseMagnet(magnetLink: String) {
        viewModelScope.launch {
            _torrentState.value = TorrentState.Parsing
            try {
                TorrentManager.magnetLinkParser(magnetLink).onSuccess {
                    _torrentState.value = TorrentState.Success(it)
                }.onFailure {
                    _torrentState.value = TorrentState.Error(it.message ?: "Unknown error")
                }
            } catch (e: Exception) {
                _torrentState.value = TorrentState.Error(e.message ?: "Unknown error")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
    }
}