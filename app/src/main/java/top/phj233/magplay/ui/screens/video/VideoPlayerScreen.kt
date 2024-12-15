package top.phj233.magplay.ui.screens.video

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import org.koin.compose.viewmodel.koinViewModel

private const val TAG = "VideoPlayerScreen"

@Composable
fun VideoPlayerScreen(
    magnetLink: String,
    fileIndex: Int,
    viewModel: VideoPlayerViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val videoState by viewModel.videoState.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()
    val downloadSpeed by viewModel.downloadSpeed.collectAsState()

    DisposableEffect(Unit) {
        Log.d(TAG, "Starting video preparation with magnet link: $magnetLink, fileIndex: $fileIndex")
        viewModel.prepareVideo(magnetLink, fileIndex)
        onDispose {
            Log.d(TAG, "Disposing video player")
            viewModel.releasePlayer()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (val state = videoState) {
            is VideoState.Loading -> {
                Log.d(TAG, "Video state: Loading, Progress: $downloadProgress, Speed: $downloadSpeed")
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = "下载中: ${(downloadProgress * 100).toInt()}%",
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Text(
                        text = "速度: ${downloadSpeed / 1024}KB/s",
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    LinearProgressIndicator(
                        progress = downloadProgress,
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .padding(top = 8.dp)
                    )
                }
            }
            is VideoState.Ready -> {
                Log.d(TAG, "Video state: Ready, setting up PlayerView")
                AndroidView(
                    factory = { context ->
                        PlayerView(context).apply {
                            player = state.player
                            useController = true
                            setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            is VideoState.Error -> {
                Log.e(TAG, "Video state: Error - ${state.message}")
                Text(
                    text = state.message,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
            }
        }
    }
}
