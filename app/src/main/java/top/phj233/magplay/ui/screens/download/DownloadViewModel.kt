package top.phj233.magplay.ui.screens.download

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.libtorrent4j.TorrentHandle
import top.phj233.magplay.entity.DownloadData
import top.phj233.magplay.repository.DBUtil
import top.phj233.magplay.torrent.DownloadStatus
import java.util.*

data class DownloadState(
    val id: Int = 0,
    val torrentHandle: TorrentHandle,
    val title: String,
    val totalSize: Long,
    val downloadedSize: Long = 0L,
    val progress: Float = 0f,
    val downloadSpeed: Long = 0L,
    val uploadSpeed: Long = 0L,
    val isPaused: Boolean = false,
    val createTime: Date = Date()
)

class DownloadViewModel : ViewModel() {
    private val downloadDao = DBUtil.getDownloadDao()
    private val _downloads = MutableStateFlow<List<DownloadState>>(emptyList())
    val downloads: StateFlow<List<DownloadState>> = _downloads.asStateFlow()

    fun updateDownloadProgress(
        torrentHandle: TorrentHandle,
        progress: Float,
        downloadedSize: Long,
        downloadSpeed: Long,
        uploadSpeed: Long
    ) {
        viewModelScope.launch {
            val currentList = _downloads.value.toMutableList()
            val index = currentList.indexOfFirst { it.torrentHandle == torrentHandle }
            if (index != -1) {
                val currentState = currentList[index]
                currentList[index] = currentState.copy(
                    progress = progress,
                    downloadedSize = downloadedSize,
                    downloadSpeed = downloadSpeed,
                    uploadSpeed = uploadSpeed
                )
                _downloads.value = currentList

                // 更新数据库
                downloadDao.updateDownload(
                    DownloadData(
                        id = currentState.id,
                        magnetLink = currentState.torrentHandle.makeMagnetUri(),
                        title = currentState.title,
                        totalSize = currentState.totalSize,
                        downloadedSize = downloadedSize,
                        progress = progress,
                        downloadSpeed = downloadSpeed,
                        status = if (currentState.isPaused) DownloadStatus.PAUSED else DownloadStatus.DOWNLOADING,
                        createAt = currentState.createTime.toString(),
                        updateAt = Date().toString()
                    )
                )
            }
        }
    }

    fun togglePause(torrentHandle: TorrentHandle) {
        viewModelScope.launch {
            val currentList = _downloads.value.toMutableList()
            val index = currentList.indexOfFirst { it.torrentHandle == torrentHandle }
            if (index != -1) {
                val currentState = currentList[index]
                if (currentState.isPaused) {
                    torrentHandle.resume()
                } else {
                    torrentHandle.pause()
                }
                currentList[index] = currentState.copy(isPaused = !currentState.isPaused)
                _downloads.value = currentList

                // 更新数据库状态
                downloadDao.updateDownload(
                    DownloadData(
                        id = currentState.id,
                        title = currentState.title,
                        totalSize = currentState.totalSize,
                        downloadedSize = currentState.downloadedSize,
                        progress = currentState.progress,
                        downloadSpeed = currentState.downloadSpeed,
                        status = if (!currentState.isPaused) DownloadStatus.PAUSED else DownloadStatus.DOWNLOADING,
                        createAt = currentState.createTime.toString(),
                        updateAt = Date().toString(),
                        magnetLink = currentState.torrentHandle.makeMagnetUri()
                    )
                )
            }
        }
    }

    fun removeDownload(torrentHandle: TorrentHandle) {
        viewModelScope.launch {
            val currentList = _downloads.value.toMutableList()
            val downloadState = currentList.find { it.torrentHandle == torrentHandle }
            if (downloadState != null) {
                currentList.remove(downloadState)
                _downloads.value = currentList

                // 从数据库中删除
                downloadDao.deleteDownload(
                    DownloadData(
                        id = downloadState.id,
                        title = downloadState.title,
                        totalSize = downloadState.totalSize,
                        downloadedSize = downloadState.downloadedSize,
                        progress = downloadState.progress,
                        downloadSpeed = downloadState.downloadSpeed,
                        status = DownloadStatus.DELETED,
                        createAt = downloadState.createTime.toString(),
                        updateAt = Date().toString(),
                        magnetLink = downloadState.torrentHandle.makeMagnetUri()
                    )
                )
            }
        }
    }
}
