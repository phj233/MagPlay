package top.phj233.magplay.ui.screens.work.music_player

import android.graphics.BitmapFactory
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import top.phj233.magplay.R
import top.phj233.magplay.ui.components.MagPlayTopBar

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicPlayerScreen() {
    val viewModel: MusicPlayerViewModel = koinViewModel()
    val context = LocalContext.current
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentPosition by viewModel.currentPosition.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val currentTrack by viewModel.currentTrack.collectAsState()
    val playlist by viewModel.playlist.collectAsState()
    val musicMetadata by viewModel.musicMetadata.collectAsState()
    var showMetadataDialog by remember { mutableStateOf(false) }
    var showPlaylist by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            MagPlayTopBar(
                title = "音乐播放器"
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 显示专辑封面区域
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                currentTrack?.mediaMetadata?.artworkData?.let { artworkData ->
                    val bitmap = BitmapFactory.decodeByteArray(artworkData, 0, artworkData.size)
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "专辑封面",
                        modifier = Modifier.size(400.dp),
                        contentScale = ContentScale.Fit
                    )
                } ?: Image(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = "默认专辑封面",
                    modifier = Modifier.size(400.dp)
                )
            }

            // 音乐信息和控制区域
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 显示歌曲标题和艺术家
                Text(
                    text = currentTrack?.mediaMetadata?.title?.toString() ?: "未选择音乐",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = currentTrack?.mediaMetadata?.artist?.toString() ?: "未知艺术家",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 进度条控制
                Slider(
                    value = currentPosition.toFloat(),
                    onValueChange = { viewModel.seekTo(it.toLong()) },
                    valueRange = 0f..duration.toFloat(),
                    modifier = Modifier.fillMaxWidth()
                )

                // 显示播放时间
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = formatDuration(currentPosition))
                    Text(text = formatDuration(duration))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 播放控制按钮组
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 音乐信息按钮
                    IconButton(
                        onClick = {
                            currentTrack?.localConfiguration?.uri?.let { uri ->
                                viewModel.viewModelScope.launch {
                                    viewModel.getMusicMetadata(uri)
                                    showMetadataDialog = true
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "音乐信息",
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // 播放控制按钮
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 上一曲按钮
                        IconButton(onClick = { viewModel.playPrevious() }) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "上一曲",
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        // 播放/暂停按钮
                        IconButton(
                            onClick = { viewModel.playPause() },
                            modifier = Modifier.size(64.dp)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                                contentDescription = if (isPlaying) "暂停" else "播放",
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // 下一曲按钮
                        IconButton(onClick = { viewModel.playNext() }) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "下一曲",
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    // 播放列表按钮
                    IconButton(onClick = { showPlaylist = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = "播放列表",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 添加音乐波形动画
                MusicWaveform(
                    isPlaying = isPlaying,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
        }

        // 音乐元数据对话框
        if (showMetadataDialog && musicMetadata != null) {
            AlertDialog(
                onDismissRequest = { showMetadataDialog = false },
                title = { Text("音乐信息") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("比特率: ${musicMetadata?.bitrate}")
                        Text("采样率: ${musicMetadata?.sampleRate}")
                        Text("格式: ${musicMetadata?.format}")
                        Text("时长: ${musicMetadata?.duration}")
                        Text("文件大小: ${musicMetadata?.fileSize}")
                    }
                },
                confirmButton = {
                    FilledTonalButton(onClick = { showMetadataDialog = false }) {
                        Text("确定")
                    }
                }
            )
        }

        // 播放列表底部弹窗
        if (showPlaylist) {
            ModalBottomSheet(
                onDismissRequest = { showPlaylist = false },
                sheetState = sheetState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "播放列表",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(playlist) { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        scope.launch {
                                            sheetState.hide()
                                            showPlaylist = false
                                        }
                                        viewModel.playTrack(playlist.indexOf(item))
                                    }
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.mediaMetadata.title?.toString() ?: "未知歌曲",
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f)
                                )
                                if (currentTrack == item) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.PlayCircle else Icons.Default.PauseCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MusicWaveform(
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .padding(horizontal = 32.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(20) { index ->
            val delay = index * 100
            val animation = infiniteTransition.animateFloat(
                initialValue = 0.2f,
                targetValue = 0.8f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 1000,
                        delayMillis = delay,
                        easing = FastOutSlowInEasing
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "wave$index"
            )
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(if (isPlaying) animation.value else 0.2f)
                    .padding(horizontal = 2.dp)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        RoundedCornerShape(8.dp)
                    )
            )
        }
    }
}

/**
 * 格式化播放时长
 * 
 * @param durationMs 时长（毫秒）
 * @return 格式化后的时间字符串（分:秒）
 */
private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
