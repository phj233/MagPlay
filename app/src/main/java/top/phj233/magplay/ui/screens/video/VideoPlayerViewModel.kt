package top.phj233.magplay.ui.screens.video

import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.mp4.Mp4Extractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import top.phj233.magplay.torrent.TorrentManager
import java.io.File

private const val TAG = "VideoPlayerViewModel"

class VideoPlayerViewModel : ViewModel(), KoinComponent {
    private val _videoState = MutableStateFlow<VideoState>(VideoState.Loading)
    val videoState: StateFlow<VideoState> = _videoState.asStateFlow()

    private val _downloadProgress = MutableStateFlow<Float>(0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress.asStateFlow()

    private val _downloadSpeed = MutableStateFlow<Long>(0)
    val downloadSpeed: StateFlow<Long> = _downloadSpeed.asStateFlow()

    private val context by inject<android.content.Context>()
    private var currentPath: String? = null
    private var player: ExoPlayer? = null
    private var isInitializing = false

    @OptIn(UnstableApi::class)
    private fun createPlayer(): ExoPlayer {
        val renderersFactory = DefaultRenderersFactory(context)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
            .setEnableDecoderFallback(true)
        
        val extractorsFactory = DefaultExtractorsFactory()
            .setConstantBitrateSeekingEnabled(true)
            .setMp4ExtractorFlags(Mp4Extractor.FLAG_WORKAROUND_IGNORE_EDIT_LISTS)
        
        return ExoPlayer.Builder(context)
            .setRenderersFactory(renderersFactory)
            .setMediaSourceFactory(DefaultMediaSourceFactory(context, extractorsFactory))
            .build()
            .apply {
                playWhenReady = true
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        Log.d(TAG, "Playback state changed to: $playbackState")
                        when (playbackState) {
                            Player.STATE_READY -> {
                                Log.d(TAG, "Player is ready")
                                play()
                            }
                            Player.STATE_BUFFERING -> {
                                Log.d(TAG, "Player is buffering")
                            }
                            Player.STATE_ENDED -> {
                                Log.d(TAG, "Playback ended")
                            }
                            Player.STATE_IDLE -> {
                                Log.d(TAG, "Player is idle")
                            }
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        Log.e(TAG, "Player error", error)
                        val errorMessage = when (error.errorCode) {
                            PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND -> "文件未找到"
                            PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE -> "无效的内容类型"
                            PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED -> "文件格式错误"
                            PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED -> "清单文件错误"
                            PlaybackException.ERROR_CODE_DECODER_INIT_FAILED -> "解码器初始化失败"
                            else -> error.message ?: "未知错误"
                        }
                        _videoState.value = VideoState.Error("播放错误: $errorMessage")
                        isInitializing = false
                    }
                })
            }
    }

    @OptIn(UnstableApi::class)
    private fun createMediaSource(file: File): MediaSource {
        val uri = Uri.fromFile(file)
        Log.d(TAG, "Creating MediaSource with URI: $uri")
        
        val dataSourceFactory = DefaultDataSource.Factory(context)
        val extractorsFactory = DefaultExtractorsFactory()
            .setConstantBitrateSeekingEnabled(true)
            .setMp4ExtractorFlags(Mp4Extractor.FLAG_WORKAROUND_IGNORE_EDIT_LISTS)
        
        return ProgressiveMediaSource.Factory(dataSourceFactory, extractorsFactory)
            .setLoadErrorHandlingPolicy(DefaultLoadErrorHandlingPolicy(/* minimumLoadableRetryCount= */ 3))
            .createMediaSource(MediaItem.fromUri(uri))
    }

    fun prepareVideo(magnetLink: String, fileIndex: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d(TAG, "Starting video preparation")
                _videoState.value = VideoState.Loading
                releasePlayer()
                currentPath = null
                isInitializing = false

                TorrentManager.streamVideo(
                    magnetUrl = magnetLink,
                    fileIndex = fileIndex,
                    onProgress = { progress ->
                        _downloadProgress.value = progress / 100f
                        // 检查是否已下载足够的数据来开始播放
                        if (progress >= 5 && !isInitializing && currentPath != null) {  // 增加到5%确保有足够的头部数据
                            Log.d(TAG, "Download progress reached 5%, attempting to initialize player")
                            tryInitializePlayer()
                        }
                    },
                    onSpeed = { downloadSpeed, _ ->
                        _downloadSpeed.value = downloadSpeed
                    },
                    onReady = { path ->
                        Log.d(TAG, "File ready at path: $path")
                        currentPath = path
                        // 在onReady时不立即初始化播放器，等待进度达到5%
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error preparing video", e)
                _videoState.value = VideoState.Error(e.message ?: "未知错误")
            }
        }
    }

    @OptIn(UnstableApi::class)
    private fun tryInitializePlayer() {
        if (isInitializing || currentPath == null) {
            Log.d(TAG, "Skipping player initialization: already initializing or no path available")
            return
        }

        val path = currentPath ?: return
        val file = File(path)
        
        if (!file.exists()) {
            Log.e(TAG, "File does not exist: $path")
            _videoState.value = VideoState.Error("文件不存在")
            return
        }

        if (file.length() < 1024 * 1024) { // 确保至少有1MB的数据
            Log.d(TAG, "File too small (${file.length()} bytes), waiting for more data")
            return
        }

        isInitializing = true
        
        // 在主线程上初始化播放器
        viewModelScope.launch(Dispatchers.Main) {
            try {
                Log.d(TAG, "Initializing player with path: $path")
                
                val player = createPlayer()
                val mediaSource = createMediaSource(file)
                
                player.setMediaSource(mediaSource)
                player.prepare()
                
                _videoState.value = VideoState.Ready(player)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing player", e)
                _videoState.value = VideoState.Error("初始化播放器失败: ${e.message}")
                isInitializing = false
            }
        }
    }

    fun releasePlayer() {
        Log.d(TAG, "Releasing player")
        try {
            player?.release()
            player = null
            currentPath = null
            isInitializing = false
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing player", e)
        }
        TorrentManager.stopStreaming()
    }

    override fun onCleared() {
        super.onCleared()
        releasePlayer()
    }
}
