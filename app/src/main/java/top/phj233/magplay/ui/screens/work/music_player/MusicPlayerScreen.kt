package top.phj233.magplay.ui.screens.work.music_player

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import top.phj233.magplay.R
import top.phj233.magplay.nav.LocalNavController
import top.phj233.magplay.ui.components.MagPlayTopBar

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicPlayerScreen() {
    val viewModel: MusicPlayerViewModel = viewModel()
    val nav = LocalNavController.current
    val context = LocalContext.current
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentPosition by viewModel.currentPosition.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val currentTrack by viewModel.currentTrack.collectAsState()
    val playlist by viewModel.playlist.collectAsState()

    val musicPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.addMusic(it)
        }
    }

    val bottomSheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        Log.d("MusicPlayerScreen", "Loading music from directory")
        viewModel.loadMusicFromDirectory()
    }
    Log.d("MusicPlayerScreen", "Current playlist size: ${playlist.size}")

    Scaffold(
        topBar = {
            MagPlayTopBar(
                title = "音乐播放器",
                actions = {
                    IconButton(onClick = { musicPickerLauncher.launch("audio/*") }) {
                        Icon(
                            Icons.Default.MusicNote,
                            contentDescription = "Add Music"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // 专辑封面区域
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    contentAlignment = Alignment.Center
                ) {
                    currentTrack?.mediaMetadata?.artworkData?.let { artworkData ->
                        val bitmap = BitmapFactory.decodeByteArray(artworkData, 0, artworkData.size)
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "专辑封面",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } ?: Image(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = "默认封面",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }

                // 歌曲信息
                Text(
                    text = currentTrack?.mediaMetadata?.title?.toString() ?: "未选择歌曲",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = currentTrack?.mediaMetadata?.artist?.toString() ?: "未知艺术家",
                    style = MaterialTheme.typography.bodyMedium
                )

                // 进度条
                Slider(
                    value = currentPosition.toFloat(),
                    onValueChange = { viewModel.seekTo(it.toLong()) },
                    valueRange = 0f..duration.toFloat(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)

                )

                // 时间显示
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = formatTime(currentPosition))
                    Text(text = formatTime(duration))
                }

                // 播放控制按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.skipToPrevious() }) {
                        Icon(Icons.Default.SkipPrevious, "上一曲")
                    }
                    IconButton(onClick = { viewModel.playPause() }) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            if (isPlaying) "暂停" else "播放"
                        )
                    }
                    IconButton(onClick = { viewModel.skipToNext() }) {
                        Icon(Icons.Default.SkipNext, "下一曲")
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // 播放列表按钮
                FilledTonalButton(
                    onClick = { showBottomSheet = true }
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.QueueMusic,
                        contentDescription = "Playlist",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("播放列表")
                }
            }
        }

        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                sheetState = bottomSheetState
            ) {
                if (playlist.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "没有找到音乐文件",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 32.dp)
                    ) {
                        itemsIndexed(playlist) { index, item ->
                            PlaylistItem(
                                mediaItem = item,
                                isPlaying = currentTrack == item && isPlaying,
                                onClick = { viewModel.playTrack(index) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistItem(
    mediaItem: MediaItem,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(mediaItem.mediaMetadata.title?.toString() ?: "未知标题")
        },
        supportingContent = {
            Text(mediaItem.mediaMetadata.artist?.toString() ?: "未知艺术家")
        },
        leadingContent = {
            mediaItem.mediaMetadata.artworkData?.let { artworkData ->
                val bitmap = BitmapFactory.decodeByteArray(artworkData, 0, artworkData.size)
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "歌曲封面",
                    modifier = Modifier.size(40.dp),
                    contentScale = ContentScale.Crop
                )
            } ?: Icon(
                Icons.Default.MusicNote,
                contentDescription = "默认封面"
            )
        },
        trailingContent = {
            if (isPlaying) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "正在播放"
                )
            }
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

private fun formatTime(timeMs: Long): String {
    val totalSeconds = timeMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
