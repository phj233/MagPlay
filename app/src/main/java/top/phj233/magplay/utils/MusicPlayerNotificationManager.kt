package top.phj233.magplay.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaStyleNotificationHelper.MediaStyle
import top.phj233.magplay.MainActivity
import top.phj233.magplay.R
import top.phj233.magplay.service.MusicPlayerService

@UnstableApi
class MusicPlayerNotificationManager(
    private val context: Context,
    private val exoPlayer: ExoPlayer,
    private val mediaSession: MediaSession
) {
    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val notificationId = 1
    private val channelId = "music_player_channel"


    companion object {
        const val ACTION_PLAY = "top.phj233.magplay.action.PLAY"
        const val ACTION_PAUSE = "top.phj233.magplay.action.PAUSE"
        const val ACTION_NEXT = "top.phj233.magplay.action.NEXT"
        const val ACTION_PREVIOUS = "top.phj233.magplay.action.PREVIOUS"
    }

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }
    }

    private fun createNotificationChannel() {
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

    fun getNotification(): Notification? {
        val currentItem = exoPlayer.currentMediaItem
        val title = currentItem?.mediaMetadata?.title ?: "未知标题"
        val artist = currentItem?.mediaMetadata?.artist ?: "未知艺术家"
        val albumTitle = currentItem?.mediaMetadata?.albumTitle ?: "未知专辑"

        // 创建通知的点击意图
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 创建媒体控制按钮的意图
        val playPauseIntent = PendingIntent.getService(
            context, 0,
            Intent(context, MusicPlayerService::class.java).apply {
                action = if (exoPlayer.isPlaying) ACTION_PAUSE else ACTION_PLAY
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nextIntent = PendingIntent.getService(
            context, 1,
            Intent(context, MusicPlayerService::class.java).apply {
                action = ACTION_NEXT
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val previousIntent = PendingIntent.getService(
            context, 2,
            Intent(context, MusicPlayerService::class.java).apply {
                action = ACTION_PREVIOUS
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 获取专辑封面
        val artwork = currentItem?.mediaMetadata?.artworkData?.let { data ->
            BitmapFactory.decodeByteArray(data, 0, data.size)
        }

        return NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setContentText("$artist - $albumTitle")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setLargeIcon(artwork)
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSilent(true)
            .setOngoing(exoPlayer.isPlaying)
            .setAutoCancel(false)
            .addAction(
                R.drawable.ic_launcher_foreground,
                "上一曲",
                previousIntent
            )
            .addAction(
                if (exoPlayer.isPlaying) R.drawable.ic_launcher_foreground else R.drawable.ic_launcher_foreground,
                if (exoPlayer.isPlaying) "暂停" else "播放",
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
}
