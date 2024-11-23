package top.phj233.magplay.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

class MagPlayVideoPlayer(private val context: Context) {
    private val player: ExoPlayer = ExoPlayer.Builder(context).build()

    fun initializePlayer(playerView: PlayerView, videoUri: String) {
        playerView.player = player
        val mediaItem = MediaItem.fromUri(videoUri)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
    }

    // 释放播放器资源
    fun releasePlayer() {
        player.release()
    }
}