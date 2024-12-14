package top.phj233.magplay.ui.screens.magnet

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext
import top.phj233.magplay.torrent.TorrentManager
import top.phj233.magplay.torrent.TorrentSession
import top.phj233.magplay.torrent.TorrentState

class ParseViewModel(
    private val torrentSession: TorrentSession
) : ViewModel() {
    private val context by lazy { GlobalContext.get().get<Context>() }
    private val _torrentState = MutableStateFlow<TorrentState>(TorrentState.Idle)
    val torrentState: StateFlow<TorrentState> = _torrentState

    private val _downloadProgress = MutableStateFlow<Map<Int, Float>>(emptyMap())
    val downloadProgress: StateFlow<Map<Int, Float>> = _downloadProgress

    private val _downloadSpeed = MutableStateFlow<Map<Int, Long>>(emptyMap())
    val downloadSpeed: StateFlow<Map<Int, Long>> = _downloadSpeed

    private val _uploadSpeed = MutableStateFlow<Map<Int, Long>>(emptyMap())
    val uploadSpeed: StateFlow<Map<Int, Long>> = _uploadSpeed

    private val _permissionNeeded = MutableStateFlow(false)
    val permissionNeeded: StateFlow<Boolean> = _permissionNeeded

    private val _storagePermissionGranted = MutableStateFlow(false)
    val storagePermissionGranted: StateFlow<Boolean> = _storagePermissionGranted

    init {
        checkStoragePermission()
    }

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
                Log.e("ParseViewModel", "Error parsing magnet link", e)
                _torrentState.value = TorrentState.Error(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * 检查是否有必要的存储权限
     */
    fun checkStoragePermission() {
        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
        _storagePermissionGranted.value = hasPermission
        if (!hasPermission) {
            _permissionNeeded.value = true
        }
    }

    /**
     * 开始下载文件
     * @param magnetLink 磁力链接
     * @param fileIndex 文件索引
     */
    fun startDownload(magnetLink: String, fileIndex: Int) {
        if (!_storagePermissionGranted.value) {
            _permissionNeeded.value = true
            return
        }

        viewModelScope.launch {
            try {
                Log.d("ParseViewModel", "Starting download for index: $fileIndex")
                // 初始化进度和速度
                _downloadProgress.value = _downloadProgress.value + (fileIndex to 0f)
                _downloadSpeed.value = _downloadSpeed.value + (fileIndex to 0L)
                _uploadSpeed.value = _uploadSpeed.value + (fileIndex to 0L)

                // 开始下载并监听进度
                TorrentManager.downloadTorrentFile(
                    magnetUrl = magnetLink,
                    fileIndex = fileIndex,
                    onProgress = { progress ->
                        Log.d("ParseViewModel", "Download progress: $progress")
                        _downloadProgress.value = _downloadProgress.value + (fileIndex to progress)
                    },
                    onSpeed = { downloadSpeed, uploadSpeed ->
                        Log.d("ParseViewModel", "Download speed: $downloadSpeed, Upload speed: $uploadSpeed")
                        _downloadSpeed.value = _downloadSpeed.value + (fileIndex to downloadSpeed)
                        _uploadSpeed.value = _uploadSpeed.value + (fileIndex to uploadSpeed)
                    }
                )
            } catch (e: Exception) {
                Log.e("ParseViewModel", "Error starting download", e)
                // 移除进度和速度状态
                _downloadProgress.value = _downloadProgress.value - fileIndex
                _downloadSpeed.value = _downloadSpeed.value - fileIndex
                _uploadSpeed.value = _uploadSpeed.value - fileIndex
                _torrentState.value = TorrentState.Error(e.message ?: "下载失败")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // 清理下载状态
        _downloadProgress.value = emptyMap()
        _downloadSpeed.value = emptyMap()
        _uploadSpeed.value = emptyMap()
        torrentSession.stop()
    }
}