package top.phj233.magplay.ui.screens.video

import androidx.media3.exoplayer.ExoPlayer

sealed class VideoState {
    object Loading : VideoState()
    data class Ready(val player: ExoPlayer) : VideoState()
    data class Error(val message: String) : VideoState()
}
