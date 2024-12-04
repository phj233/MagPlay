package top.phj233.magplay.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaStyleNotificationHelper.MediaStyle
import org.koin.android.ext.android.inject
import top.phj233.magplay.MainActivity
import top.phj233.magplay.R

@UnstableApi
class MusicPlayerService : Service() {
    private val player: ExoPlayer by inject()
    private val notificationManager by lazy {
        getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    }
    private var notificationId = 1
    private val channelId = "music_playback_channel"
    private lateinit var mediaSession: MediaSession

    companion object {
        private const val ACTION_PLAY = "top.phj233.magplay.action.PLAY"
        private const val ACTION_PAUSE = "top.phj233.magplay.action.PAUSE"
        private const val ACTION_NEXT = "top.phj233.magplay.action.NEXT"
        private const val ACTION_PREVIOUS = "top.phj233.magplay.action.PREVIOUS"
    }

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }
        initializeMediaSession()

        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    startForeground(notificationId, createNotification())
                } else {
                    stopForeground(
                        STOP_FOREGROUND_DETACH
                    )
                    notificationManager.notify(notificationId, createNotification())
                }
            }

            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                if (player.isPlaying) {
                    notificationManager.notify(notificationId, createNotification())
                }
            }
        })
    }

    private fun initializeMediaSession() {
        mediaSession = MediaSession.Builder(this, player)
            .setId("MagPlaySession")
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> player.play()
            ACTION_PAUSE -> player.pause()
            ACTION_NEXT -> player.seekToNext()
            ACTION_PREVIOUS -> player.seekToPrevious()
            else -> {
                // 如果没有特定动作，但播放器正在播放，则显示通知
                if (player.isPlaying) {
                    startForeground(notificationId, createNotification())
                }
            }
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "音乐播放",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "音乐播放控制"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val currentItem = player.currentMediaItem
        val title = currentItem?.mediaMetadata?.title ?: "未知标题"
        val artist = currentItem?.mediaMetadata?.artist ?: "未知艺术家"
        val albumTitle = currentItem?.mediaMetadata?.albumTitle ?: "未知专辑"

        // 创建通知的点击意图
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 创建媒体控制按钮的意图
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

        // 获取专辑封面
        val artwork = currentItem?.mediaMetadata?.artworkData?.let { data ->
            BitmapFactory.decodeByteArray(data, 0, data.size)
        }

        // 构建通知
        return NotificationCompat.Builder(this, channelId)
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
                R.drawable.ic_launcher_foreground,
                "上一曲",
                previousIntent
            )
            .addAction(
                if (player.isPlaying) R.drawable.ic_launcher_foreground else R.drawable.ic_launcher_foreground,
                if (player.isPlaying) "暂停" else "播放",
                playPauseIntent
            )
            .addAction(
                R.drawable.ic_launcher_foreground,
                "下一曲",
                nextIntent
            )
            .setStyle(MediaStyle(mediaSession)
                .setShowActionsInCompactView(0, 1, 2)
            )
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        mediaSession.release()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }
}
