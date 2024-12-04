package top.phj233.magplay.ui.screens.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import top.phj233.magplay.torrent.TorrentState
import top.phj233.magplay.torrent.TorrentManager


class SearchViewModel : ViewModel() {
    private val _torrentState = MutableStateFlow<TorrentState>(TorrentState.Idle)
    val torrentState: StateFlow<TorrentState> = _torrentState.asStateFlow()
    
    fun parseMagnet(magnetLink: String) {
        viewModelScope.launch {
            _torrentState.value = TorrentState.Parsing
            try {
                TorrentManager.magnetLinkParser(magnetLink)
                    .onSuccess { info ->
                        _torrentState.value = TorrentState.Success(info)
                    }
                    .onFailure { e ->
                        _torrentState.value = TorrentState.Error(e.message ?: "Unknown error")
                    }
            } catch (e: Exception) {
                _torrentState.value = TorrentState.Error(e.message ?: "Unknown error")
            }
        }
    }
}