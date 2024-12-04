package top.phj233.magplay.ui.screens.work.music_player

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.util.LruCache
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.ByteArrayOutputStream

class MusicPlayerViewModel : ViewModel(), KoinComponent {
    private val context: Context by inject()
    private val player: ExoPlayer by inject()
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

    private var positionUpdateJob: Job? = null

    init {
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                _isPlaying.value = playbackState == Player.STATE_READY && player.isPlaying
                _duration.value = player.duration.coerceAtLeast(0L)
                updatePosition()
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                updatePosition()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                _currentTrack.value = mediaItem
            }
        })
        loadMusicFromDirectory()
    }

    private suspend fun getArtworkFromUri(uri: Uri, mediaId: String): ByteArray? = withContext(Dispatchers.IO) {
        try {
            // 先从缓存中查找
            artworkCache.get(mediaId)?.let { 
                Log.d("MusicPlayerViewModel", "从缓存获取到封面: $mediaId")
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
                        Log.d("MusicPlayerViewModel", "成功获取并缓存封面: $mediaId")
                        compressedArtwork
                    } else {
                        Log.d("MusicPlayerViewModel", "未找到封面: $mediaId")
                        null
                    }
                }
            } else {
                TODO("VERSION.SDK_INT < Q")
            }
        } catch (e: Exception) {
            Log.e("MusicPlayerViewModel", "获取封面失败: ${e.message}", e)
            null
        }
    }

    /**
     * 压缩图片
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

    fun addMusic(uri: Uri) {
        viewModelScope.launch {
            Log.d("MusicPlayerViewModel", "添加音乐: $uri")
            try {
                val mediaId = uri.toString()
                val rawFileName = uri.lastPathSegment ?: "未知文件"
                val fileName = when {
                    rawFileName.contains("/") -> rawFileName.substringAfterLast("/")
                    else -> rawFileName
                }
                val title = fileName.substringBeforeLast(".")
                
                // 异步获取封面
                val artwork = getArtworkFromUri(uri, mediaId)
                
                // 创建MediaItem
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
                Log.d("MusicPlayerViewModel", "成功添加音乐: $title")
            } catch (e: Exception) {
                Log.e("MusicPlayerViewModel", "添加音乐失败: ${e.message}", e)
            }
        }
    }

    fun loadMusicFromDirectory() {
        viewModelScope.launch {
            try {
                val mediaItems = mutableListOf<MediaItem>()
                val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                val projection = arrayOf(
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.DISPLAY_NAME,
                    MediaStore.Audio.Media.DATA,
                    MediaStore.Audio.Media.DURATION,
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Media.ALBUM
                )
                
                val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
                val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"
                
                Log.d("MusicPlayerViewModel", "开始扫描音乐")
                context.contentResolver.query(
                    collection,
                    projection,
                    selection,
                    null,
                    sortOrder
                )?.use { cursor ->
                    Log.d("MusicPlayerViewModel", "找到 ${cursor.count} 个音乐文件")
                    
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                    val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                    val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                    val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                    val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                    val displayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                    
                    while (cursor.moveToNext()) {
                        try {
                            val id = cursor.getLong(idColumn)
                            val path = cursor.getString(dataColumn)
                            val title = cursor.getString(titleColumn)
                            val artist = cursor.getString(artistColumn)
                            val album = cursor.getString(albumColumn)
                            val displayName = cursor.getString(displayNameColumn)
                            
                            if (isSupportedAudioFormat(displayName)) {
                                val contentUri = Uri.withAppendedPath(collection, id.toString())
                                val mediaId = id.toString()
                                
                                // 异步获取封面
                                val artwork = getArtworkFromUri(contentUri, mediaId)
                                
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
                                Log.d("MusicPlayerViewModel", "添加歌曲: ${title ?: displayName}")
                            }
                        } catch (e: Exception) {
                            Log.e("MusicPlayerViewModel", "处理音乐文件失败", e)
                            continue
                        }
                    }
                }

                if (mediaItems.isNotEmpty()) {
                    _playlist.value = mediaItems
                    player.setMediaItems(mediaItems)
                    player.prepare()
                    Log.d("MusicPlayerViewModel", "成功加载 ${mediaItems.size} 首歌曲")
                } else {
                    Log.w("MusicPlayerViewModel", "未找到音乐文件")
                }
            } catch (e: Exception) {
                Log.e("MusicPlayerViewModel", "加载音乐失败: ${e.message}", e)
            }
        }
    }

    private fun isSupportedAudioFormat(fileName: String): Boolean {
        val supportedFormats = setOf(".mp3", ".wav", ".m4a", ".flac", ".aac")
        return supportedFormats.any { fileName.lowercase().endsWith(it) }
    }

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


    fun playPause() {
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
    }

    fun skipToNext() {
        if (player.hasNextMediaItem()) {
            player.seekToNext()
        }
    }

    fun skipToPrevious() {
        if (player.hasPreviousMediaItem()) {
            player.seekToPrevious()
        }
    }

    fun seekTo(position: Long) {
        player.seekTo(position)
    }

    fun playTrack(index: Int) {
        if (index in 0 until player.mediaItemCount) {
            player.seekTo(index, 0)
            player.prepare()
            player.play()
            Log.d("MusicPlayerViewModel", "开始播放曲目: ${_playlist.value[index].mediaMetadata.title}")
        } else {
            Log.w("MusicPlayerViewModel", "无效的播放索引: $index")
        }
    }

    override fun onCleared() {
        super.onCleared()
        positionUpdateJob?.cancel()
        coroutineScope.cancel()
        player.release()
        artworkCache.evictAll()
    }
}
