package top.phj233.magplay.ui.screens.work.music_player

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.util.LruCache
import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import top.phj233.magplay.repository.preferences.PlaylistMMKV
import top.phj233.magplay.service.MusicPlayerService
import java.io.ByteArrayOutputStream

/**
 * 音乐播放器视图模型
 *
 * 负责管理音乐播放器的状态和控制逻辑，包括：
 * - 音乐播放控制
 * - 播放列表管理
 * - 音乐元数据处理
 * - 专辑封面缓存
 * - 播放进度保存和恢复
 *
 * @author phj233
 */
@OptIn(UnstableApi::class)
class MusicPlayerViewModel : ViewModel(), KoinComponent {
    private val context: Context by inject()
    private val player: ExoPlayer by inject()
    private val playlistMMKV: PlaylistMMKV by inject()
    private val artworkCache = LruCache<String, ByteArray>((Runtime.getRuntime().maxMemory() / 1024 / 8).toInt())
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration = _duration.asStateFlow()

    private val _currentTrack = MutableStateFlow<MediaItem?>(null)
    val currentTrack = _currentTrack.asStateFlow()

    private val _playlist = MutableStateFlow<List<MediaItem>>(emptyList())
    val playlist = _playlist.asStateFlow()

    data class MusicMetadata(
        val bitrate: String = "",
        val sampleRate: String = "",
        val format: String = "",
        val duration: String = "",
        val fileSize: String = ""
    )

    private val _musicMetadata = MutableStateFlow<MusicMetadata?>(null)
    val musicMetadata = _musicMetadata.asStateFlow()

    private var positionUpdateJob: Job? = null

    init {
        // 启动前台服务
        val intent = Intent(context, MusicPlayerService::class.java)
        context.startForegroundService(intent)

        // 如果播放列表为空，则加载音乐
        if (player.mediaItemCount == 0) {
            loadMusicFromDirectory()
        } else {
            // 否则直接使用现有播放列表
            _playlist.value = List(player.mediaItemCount) { index -> player.getMediaItemAt(index) }
        }

        // 恢复播放状态
        viewModelScope.launch(Dispatchers.Main) {
            try {
                // 等待播放列表加载完成
                var attempts = 0
                while (player.mediaItemCount == 0 && attempts < 10) {
                    delay(100)
                    attempts++
                }

                if (player.mediaItemCount > 0) {
                    val lastIndex = playlistMMKV.getLastTrackIndex()
                    val lastPosition = playlistMMKV.getLastPosition()
                    
                    if (lastIndex in 0 until player.mediaItemCount) {
                        player.seekTo(lastIndex, lastPosition)
                        _currentTrack.value = player.getMediaItemAt(lastIndex)
                    }
                }

                // 同步当前播放状态
                _isPlaying.value = player.isPlaying
                _currentPosition.value = player.currentPosition
                _duration.value = player.duration.coerceAtLeast(0L)
                _currentTrack.value = player.currentMediaItem
            } catch (e: Exception) {
                Log.e(TAG, "恢复播放状态失败: ${e.message}", e)
            }
        }

        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                _isPlaying.value = playbackState == Player.STATE_READY && player.isPlaying
                _duration.value = player.duration.coerceAtLeast(0L)
                updatePosition()
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                updatePosition()
                // 暂停时保存播放进度
                if (!isPlaying) {
                    savePlaybackState()
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                _currentTrack.value = mediaItem
                // 切换曲目时保存进度
                savePlaybackState()
            }
        })
    }

    /**
     * 从 URI 获取音乐封面
     * 
     * @param uri 音乐文件的 URI
     * @param mediaId 媒体ID，用于缓存键
     * @return 封面图片的字节数组，如果没有封面则返回 null
     */
    private suspend fun getArtworkFromUri(uri: Uri, mediaId: String): ByteArray? = withContext(Dispatchers.IO) {
        try {
            // 先从缓存中查找
            artworkCache.get(mediaId)?.let { 
                Log.d(TAG, "从缓存获取到封面: $mediaId")
                return@withContext it 
            }

            // 从文件中获取
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaMetadataRetriever().use { retriever ->
                    retriever.setDataSource(context, uri)
                    val artwork = retriever.embeddedPicture

                    if (artwork != null) {
                        // 压缩图片
                        val bitmap = BitmapFactory.decodeByteArray(artwork, 0, artwork.size)
                        val compressedArtwork = compressArtwork(bitmap)

                        // 存入缓存
                        artworkCache.put(mediaId, compressedArtwork)
                        Log.d(TAG, "成功获取并缓存封面: $mediaId")
                        compressedArtwork
                    } else {
                        Log.d(TAG, "未找到封面: $mediaId")
                        null
                    }
                }
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取封面失败: ${e.message}", e)
            null
        }
    }

    /**
     * 压缩专辑封面图片
     * 
     * @param bitmap 原始图片
     * @return 压缩后的图片数据
     */
    private fun compressArtwork(bitmap: Bitmap): ByteArray {
        val maxSize = 500 // 最大尺寸
        val scale = kotlin.math.min(
            maxSize.toFloat() / bitmap.width,
            maxSize.toFloat() / bitmap.height
        )
        
        val scaledBitmap = if (scale < 1) {
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).toInt(),
                (bitmap.height * scale).toInt(),
                true
            )
        } else {
            bitmap
        }

        ByteArrayOutputStream().use { stream ->
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
            return stream.toByteArray()
        }
    }

    /**
     * 添加单个音乐文件到播放列表
     * 
     * @param uri 音乐文件的 URI
     */
    fun addMusic(uri: Uri) {
        viewModelScope.launch {
            Log.d(TAG, "添加音乐: $uri")
            try {
                val mediaId = uri.toString()
                val rawFileName = uri.lastPathSegment ?: "未知文件"
                val fileName = when {
                    rawFileName.contains("/") -> rawFileName.substringAfterLast("/")
                    else -> rawFileName
                }
                val title = fileName.substringBeforeLast(".")
                
                // 异步获取封面和元数据
                val artwork = getArtworkFromUri(uri, mediaId)
                getMusicMetadata(uri)  // 获取元数据
                
                // 创建 MediaItem
                val mediaItem = MediaItem.Builder()
                    .setUri(uri)
                    .setMediaId(mediaId)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(title)
                            .setDisplayTitle(title)
                            .setArtist("未知艺术家")
                            .setArtworkData(artwork, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                            .build()
                    )
                    .build()
                
                // 更新播放列表
                val currentList = _playlist.value.toMutableList()
                currentList.add(mediaItem)
                _playlist.value = currentList
                
                // 添加到播放器
                player.addMediaItem(mediaItem)
                if (!player.isPlaying) {
                    player.prepare()
                }
                Log.d(TAG, "成功添加音乐: $title")
            } catch (e: Exception) {
                Log.e(TAG, "添加音乐失败: ${e.message}", e)
            }
        }
    }

    /**
     * 从媒体库加载音乐文件
     * 
     * 使用 MediaStore API 扫描设备中的音乐文件，
     * 并将其添加到播放列表中
     */
    fun loadMusicFromDirectory() {
        viewModelScope.launch {
            try {
                val mediaItems = mutableListOf<MediaItem>()
                val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                val projection = arrayOf(
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.DISPLAY_NAME,
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Media.ALBUM
                )
                
                val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
                val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"
                
                Log.d(TAG, "开始扫描音乐")
                context.contentResolver.query(
                    collection,
                    projection,
                    selection,
                    null,
                    sortOrder
                )?.use { cursor -> 
                    Log.d(TAG, "找到 ${cursor.count} 个音乐文件")
                    
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                    val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                    val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                    val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                    val displayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                    
                    while (cursor.moveToNext()) {
                        try {
                            val id = cursor.getLong(idColumn)
                            val title = cursor.getString(titleColumn)
                            val artist = cursor.getString(artistColumn)
                            val album = cursor.getString(albumColumn)
                            val displayName = cursor.getString(displayNameColumn)
                            
                            val contentUri = Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id.toString())
                            
                            if (isSupportedAudioFormat(displayName)) {
                                val mediaId = id.toString()
                                
                                // 异步获取封面和元数据
                                val artwork = getArtworkFromUri(contentUri, mediaId)
                                getMusicMetadata(contentUri)  // 获取元数据
                                
                                val mediaItem = MediaItem.Builder()
                                    .setUri(contentUri)
                                    .setMediaId(mediaId)
                                    .setMediaMetadata(
                                        MediaMetadata.Builder()
                                            .setTitle(title ?: displayName.substringBeforeLast("."))
                                            .setDisplayTitle(displayName)
                                            .setArtist(artist ?: "未知艺术家")
                                            .setAlbumTitle(album ?: "未知专辑")
                                            .setArtworkData(artwork, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                                            .build()
                                    )
                                    .build()
                                mediaItems.add(mediaItem)
                                Log.d(TAG, "添加歌曲: ${title ?: displayName}")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "处理音乐文件失败", e)
                            continue
                        }
                    }
                }

                if (mediaItems.isNotEmpty()) {
                    _playlist.value = mediaItems
                    player.setMediaItems(mediaItems)
                    player.prepare()
                    // 恢复上次播放位置
                    restorePlaybackState()
                    Log.d(TAG, "成功加载 ${mediaItems.size} 首歌曲")
                } else {
                    Log.w(TAG, "未找到音乐文件")
                }
            } catch (e: Exception) {
                Log.e(TAG, "加载音乐失败: ${e.message}", e)
            }
        }
    }

    /**
     * 检查文件是否为支持的音频格式
     * 
     * @param fileName 文件名
     * @return 如果是支持的格式返回 true，否则返回 false
     */
    private fun isSupportedAudioFormat(fileName: String): Boolean {
        val supportedFormats = setOf(".mp3", ".wav", ".m4a", ".flac", ".aac")
        return supportedFormats.any { fileName.lowercase().endsWith(it) }
    }

    /**
     * 更新播放位置
     * 
     * 当音乐正在播放时，每秒更新一次当前播放位置
     */
    private fun updatePosition() {
        positionUpdateJob?.cancel()
        if (_isPlaying.value) {
            positionUpdateJob = viewModelScope.launch {
                while (true) {
                    _currentPosition.value = player.currentPosition
                    delay(1000)
                }
            }
        }
    }

    /**
     * 播放或暂停当前曲目
     */
    fun playPause() {
        viewModelScope.launch(Dispatchers.Main) {
            try {
                if (player.isPlaying) {
                    player.pause()
                } else {
                    player.play()
                }
                // 确保服务保持运行
                val intent = Intent(context, MusicPlayerService::class.java)
                context.startForegroundService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "播放控制失败: ${e.message}", e)
            }
        }
    }

    /**
     * 播放下一曲
     */
    fun playNext() {
        viewModelScope.launch(Dispatchers.Main) {
            try {
                if (player.hasNextMediaItem()) {
                    player.seekToNext()
                    // 确保服务保持运行
                    val intent = Intent(context, MusicPlayerService::class.java)
                    context.startForegroundService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "播放下一曲失败: ${e.message}", e)
            }
        }
    }

    /**
     * 播放上一曲
     */
    fun playPrevious() {
        viewModelScope.launch(Dispatchers.Main) {
            try {
                if (player.hasPreviousMediaItem()) {
                    player.seekToPrevious()
                    // 确保服务保持运行
                    val intent = Intent(context, MusicPlayerService::class.java)
                    context.startForegroundService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "播放上一曲失败: ${e.message}", e)
            }
        }
    }

    /**
     * 跳转到指定播放位置
     * 
     * @param position 目标位置（毫秒）
     */
    fun seekTo(position: Long) {
        viewModelScope.launch(Dispatchers.Main) {
            try {
                player.seekTo(position)
            } catch (e: Exception) {
                Log.e(TAG, "跳转播放位置失败: ${e.message}", e)
            }
        }
    }

    /**
     * 播放指定索引的曲目
     * 
     * @param index 播放列表中的曲目索引
     */
    fun playTrack(index: Int) {
        if (index in 0 until player.mediaItemCount) {
            viewModelScope.launch(Dispatchers.Main) {
                try {
                    player.seekTo(index, 0)
                    player.prepare()
                    player.play()
                    // 确保服务保持运行
                    val intent = Intent(context, MusicPlayerService::class.java)
                    context.startForegroundService(intent)
                    Log.d(TAG, "开始播放曲目: ${_playlist.value[index].mediaMetadata.title}")
                } catch (e: Exception) {
                    Log.e(TAG, "播放失败: ${e.message}", e)
                }
            }
        } else {
            Log.w(TAG, "无效的播放索引: $index")
        }
    }

    /**
     * 保存播放状态
     * 包括当前曲目索引和播放位置
     */
    private fun savePlaybackState() {
        try {
            val currentPosition = player.currentPosition
            val currentIndex = player.currentMediaItemIndex
            playlistMMKV.saveLastPosition(currentPosition)
            playlistMMKV.saveLastTrackIndex(currentIndex)
            Log.d(TAG, "保存播放状态：索引=$currentIndex, 位置=$currentPosition")
        } catch (e: Exception) {
            Log.e(TAG, "保存播放状态失败: ${e.message}", e)
        }
    }

    /**
     * 恢复播放状态
     * 从持久化存储中恢复上次的播放位置
     */
    private fun restorePlaybackState() {
        try {
            val lastIndex = playlistMMKV.getLastTrackIndex()
            val lastPosition = playlistMMKV.getLastPosition()
            
            if (lastIndex in 0 until player.mediaItemCount) {
                player.seekTo(lastIndex, lastPosition)
                Log.d(TAG, "恢复播放状态：索引=$lastIndex, 位置=$lastPosition")
            }
        } catch (e: Exception) {
            Log.e(TAG, "恢复播放状态失败: ${e.message}", e)
        }
    }

    override fun onCleared() {
        super.onCleared()
        // 保存最后的播放状态
        savePlaybackState()
        positionUpdateJob?.cancel()
        coroutineScope.cancel()
        player.release()
        artworkCache.evictAll()
    }

    /**
     * 获取音乐文件的元数据信息
     *
     * @param uri 音乐文件的 URI
     */
    suspend fun getMusicMetadata(uri: Uri) {
        try {
            MediaMetadataRetriever().use { retriever ->
                retriever.setDataSource(context, uri)
                
                val bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.let {
                    "${it.toInt() / 1000} kbps"
                } ?: "未知"
                
                val sampleRate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)?.let {
                    "$it Hz"
                } ?: "未知"
                
                val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.let {
                    val seconds = it.toLong() / 1000
                    val minutes = seconds / 60
                    val remainingSeconds = seconds % 60
                    "%02d:%02d".format(minutes, remainingSeconds)
                } ?: "未知"
                
                val format = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)?.let {
                    it.substringAfterLast("/").uppercase()
                } ?: "未知"
                
                val fileSize = context.contentResolver.openFileDescriptor(uri, "r")?.use { 
                    val size = it.statSize
                    when {
                        size < 1024 -> "$size B"
                        size < 1024 * 1024 -> "%.1f KB".format(size / 1024.0)
                        else -> "%.1f MB".format(size / (1024.0 * 1024.0))
                    }
                } ?: "未知"
                
                _musicMetadata.value = MusicMetadata(
                    bitrate = bitrate,
                    sampleRate = sampleRate,
                    format = format,
                    duration = duration,
                    fileSize = fileSize
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取音乐元数据失败: ${e.message}", e)
            _musicMetadata.value = null
        }
    }

    companion object {
        private const val TAG = "MusicPlayerViewModel"
    }
}
