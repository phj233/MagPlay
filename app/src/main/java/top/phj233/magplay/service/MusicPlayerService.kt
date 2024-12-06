package top.phj233.magplay.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.MediaStyleNotificationHelper.MediaStyle
import org.koin.android.ext.android.inject
import top.phj233.magplay.MainActivity
import top.phj233.magplay.R
import top.phj233.magplay.repository.preferences.PlaylistMMKV

/**
 * 音乐播放服务
 *
 * 该服务负责管理音乐播放的核心功能，包括：
 * - 音频播放控制
 * - 通知栏管理
 * - 媒体会话控制
 * - 前台服务维护
 * - 播放进度保存和恢复
 *
 * 主要组件：
 * - ExoPlayer：用于音频播放
 * - MediaSession：处理媒体会话
 * - NotificationManager：管理通知栏显示
 * - PlaylistMMKV：保存播放进度
 *
 * @author phj233
 */
@UnstableApi
class MusicPlayerService : MediaSessionService() {
    private val player: ExoPlayer by inject()
    private val mediaSession: MediaSession by inject()
    private val playlistMMKV: PlaylistMMKV by inject()
    private val notificationManager by lazy {
        getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    }

    companion object {
        private const val TAG = "MusicPlayerService"
        private const val CHANNEL_ID = "music_playback_channel"
        private const val NOTIFICATION_ID = 1
        /**
         * 播放控制动作常量
         */
        const val ACTION_PLAY = "top.phj233.magplay.action.PLAY"
        const val ACTION_PAUSE = "top.phj233.magplay.action.PAUSE"
        const val ACTION_NEXT = "top.phj233.magplay.action.NEXT"
        const val ACTION_PREVIOUS = "top.phj233.magplay.action.PREVIOUS"
    }

    /**
     * 处理服务命令
     * 处理播放控制动作
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> player.play()
            ACTION_PAUSE -> player.pause()
            ACTION_NEXT -> player.seekToNext()
            ACTION_PREVIOUS -> player.seekToPrevious()
            else -> {
                // 如果没有特定操作，尝试恢复播放状态
                if (player.playbackState != Player.STATE_READY) {
                    restorePlaybackState()
                }
            }
        }
        // 始终保持前台服务状态
        startForeground(NOTIFICATION_ID, createNotification())
        return START_STICKY
    }

    /**
     * 服务创建时的初始化
     * 创建通知渠道并设置播放器监听器
     */
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        setupPlayerListener()
        // 恢复上次播放位置
        restorePlaybackState()
        // 立即显示通知并启动前台服务
        startForeground(NOTIFICATION_ID, createNotification())
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    /**
     * 服务销毁时释放资源
     * 保存播放进度
     */
    override fun onDestroy() {
        // 保存播放状态
        savePlaybackState()
        // 停止前台服务
        stopForeground(STOP_FOREGROUND_REMOVE)
        // 清理资源
        player.removeListener(playerListener)
        mediaSession.release()
        player.release()
        super.onDestroy()
    }

    /**
     * 设置播放器状态监听
     * 监听播放状态变化和媒体项切换
     */
    private fun setupPlayerListener() {
        playerListener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updateNotificationState()
                // 播放状态改变时保存进度
                if (!isPlaying) {
                    savePlaybackState()
                }
            }

            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                updateNotificationState()
                // 切换曲目时保存索引
                savePlaybackState()
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                Log.e(TAG, "播放器错误: ${error.message}", error)
                // 发生错误时尝试恢复播放状态
                restorePlaybackState()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_IDLE -> {
                        // 播放器空闲时尝试恢复状态
                        restorePlaybackState()
                    }
                    Player.STATE_ENDED -> {
                        // 播放结束时保存状态
                        savePlaybackState()
                    }
                }
                updateNotificationState()
            }
        }
        player.addListener(playerListener)
    }

    /**
     * 创建通知渠道
     * 适配 Android O 及以上版本
     */
    @SuppressLint("ObsoleteSdkInt")
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "音乐播放",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "音乐播放控制"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * 更新通知状态
     * 根据播放状态更新前台服务通知
     */
    private fun updateNotificationState() {
        try {
            val notification = createNotification()
            // 始终保持前台服务状态
            startForeground(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating notification: ${e.message}", e)
        }
    }

    /**
     * 创建通知
     * 构建包含媒体控制的通知
     *
     * @return 包含媒体控制的通知对象
     */
    private fun createNotification(): Notification {
        val currentItem = player.currentMediaItem
        val title = currentItem?.mediaMetadata?.title ?: "未知标题"
        val artist = currentItem?.mediaMetadata?.artist ?: "未知艺术家"
        val albumTitle = currentItem?.mediaMetadata?.albumTitle ?: "未知专辑"

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseIntent = PendingIntent.getService(
            this, 0,
            Intent(this, MusicPlayerService::class.java).apply {
                action = if (player.isPlaying) ACTION_PAUSE else ACTION_PLAY
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nextIntent = PendingIntent.getService(
            this, 1,
            Intent(this, MusicPlayerService::class.java).apply {
                action = ACTION_NEXT
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val previousIntent = PendingIntent.getService(
            this, 2,
            Intent(this, MusicPlayerService::class.java).apply {
                action = ACTION_PREVIOUS
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val artwork = currentItem?.mediaMetadata?.artworkData?.let { data ->
            BitmapFactory.decodeByteArray(data, 0, data.size)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText("$artist - $albumTitle")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setLargeIcon(artwork)
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSilent(true)
            .setOngoing(player.isPlaying)
            .setAutoCancel(false)
            .addAction(
                androidx.media3.session.R.drawable.media3_notification_small_icon,
                "上一曲",
                previousIntent
            )
            .addAction(
                if (player.isPlaying)
                    androidx.media3.session.R.drawable.media3_icon_pause
                else 
                    androidx.media3.session.R.drawable.media3_icon_play,
                if (player.isPlaying) "暂停" else "播放",
                playPauseIntent
            )
            .addAction(
                androidx.media3.session.R.drawable.media3_notification_small_icon,
                "下一曲",
                nextIntent
            )
            .setStyle(
                MediaStyle(mediaSession)
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .build()
    }

    private lateinit var playerListener: Player.Listener

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
            if (player.mediaItemCount > 0) {
                val lastIndex = playlistMMKV.getLastTrackIndex()
                val lastPosition = playlistMMKV.getLastPosition()
                
                if (lastIndex in 0 until player.mediaItemCount) {
                    player.seekTo(lastIndex, lastPosition)
                    player.prepare()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "恢复播放状态失败: ${e.message}", e)
        }
    }
}
