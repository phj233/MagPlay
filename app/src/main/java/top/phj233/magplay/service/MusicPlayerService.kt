package top.phj233.magplay.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
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

/**
 * 音乐播放服务
 *
 * 该服务负责管理音乐播放的核心功能，包括：
 * - 音频播放控制
 * - 通知栏管理
 * - 媒体会话控制
 * - 前台服务维护
 *
 * 主要组件：
 * - ExoPlayer：用于音频播放
 * - MediaSession：处理媒体会话
 * - NotificationManager：管理通知栏显示
 *
 * @author phj233
 */
@UnstableApi
class MusicPlayerService : MediaSessionService() {
    private val player: ExoPlayer by inject()
    private val mediaSession: MediaSession by inject()
    private val notificationManager by lazy {
        getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    }

    companion object {
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
     * 服务创建时的初始化
     * 创建通知渠道并设置播放器监听器
     */
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        setupPlayerListener()
        // 立即显示通知
        startForeground(NOTIFICATION_ID, createNotification())
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    /**
     * 设置播放器状态监听
     * 监听播放状态变化和媒体项切换
     */
    private fun setupPlayerListener() {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updateNotificationState()
            }

            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                updateNotificationState()
            }
        })
    }

    /**
     * 创建通知渠道
     * 适配 Android O 及以上版本
     */
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
        val notification = createNotification()
        if (player.isPlaying) {
            startForeground(NOTIFICATION_ID, notification)
        } else {
            stopForeground(STOP_FOREGROUND_DETACH)
            notificationManager.notify(NOTIFICATION_ID, notification)
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


    /**
     * 服务销毁时的清理工作
     */
    override fun onDestroy() {
        super.onDestroy()
        stopForeground(STOP_FOREGROUND_REMOVE)
        mediaSession.release()
        player.release()
    }
}
