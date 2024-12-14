package top.phj233.magplay.ui.screens.magnet

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.format.Formatter.formatFileSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import top.phj233.magplay.torrent.*
import top.phj233.magplay.ui.components.MagPlayTopBar
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

@Composable
fun ParsePage(
    magnetLink: String,
    onNavigateToPlayer: (String) -> Unit = {}
) {
    val viewModel: ParseViewModel = koinViewModel()
    val torrentState by viewModel.torrentState.collectAsState()
    val permissionNeeded by viewModel.permissionNeeded.collectAsState()
    val storagePermissionGranted by viewModel.storagePermissionGranted.collectAsState()
    val context = LocalContext.current
    val decodedMagnet = URLDecoder.decode(magnetLink, StandardCharsets.UTF_8.toString())

    LaunchedEffect(decodedMagnet) {
        viewModel.parseMagnet(decodedMagnet)
    }

    // 权限请求对话框
    if (permissionNeeded) {
        AlertDialog(
            onDismissRequest = { /* 不允许用户关闭对话框 */ },
            title = { Text("需要存储权限") },
            text = { Text("为了下载文件，我们需要存储权限。请在设置中授予权限。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            context.startActivity(
                                Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                            )
                        } else {
                            context.startActivity(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                            )
                        }
                    }
                ) {
                    Text("前往设置")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.checkStoragePermission()
                    }
                ) {
                    Text("检查权限")
                }
            }
        )
    }

    Scaffold(
        topBar = { MagPlayTopBar("文件信息") }
    ) { padding ->
        when (val state = torrentState) {
            is TorrentState.Idle -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "准备解析...")
                }
            }
            is TorrentState.Parsing -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "正在解析种子...\n这可能需要一些时间",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            is TorrentState.Success -> {
                LazyColumn(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                ) {
                    item {
                        TorrentInfoCard(state.info)
                    }
                    items(state.info.files) { file ->
                        TorrentFileCard(
                            file = file,
                            magnetLink = decodedMagnet,
                            onPlayClick = onNavigateToPlayer,
                            viewModel = viewModel
                        )
                    }
                }
            }
            is TorrentState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "解析失败",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TorrentInfoCard(info: MagPlayTorrentInfo) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = info.name,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "总大小: ${formatFileSize(LocalContext.current, info.totalSize)}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "文件数: ${info.numFiles}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun TorrentFileCard(
    file: TorrentFile,
    magnetLink: String,
    onPlayClick: (String) -> Unit,
    viewModel: ParseViewModel = koinViewModel()
) {
    val downloadProgress by viewModel.downloadProgress.collectAsState()
    val downloadSpeed by viewModel.downloadSpeed.collectAsState()
    val uploadSpeed by viewModel.uploadSpeed.collectAsState()
    val fileIndex = (viewModel.torrentState.value as? TorrentState.Success)?.info?.files?.indexOf(file) ?: -1
    val isDownloading = fileIndex != -1 && downloadProgress.containsKey(fileIndex)
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // 错误对话框
    if (showErrorDialog) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = false },
            title = { Text("下载错误") },
            text = { Text(errorMessage) },
            confirmButton = {
                TextButton(onClick = { showErrorDialog = false }) {
                    Text("确定")
                }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        val fileDir = TorrentManager.getDownloadDirectory()
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = file.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatFileSize(LocalContext.current, file.size),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    // 显示下载进度和速度
                    if (isDownloading) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = downloadProgress[fileIndex]!! / 100f,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "下载: ${formatFileSize(LocalContext.current, downloadSpeed[fileIndex]!!)}/s",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "上传: ${formatFileSize(LocalContext.current, uploadSpeed[fileIndex]!!)}/s",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                // 操作按钮
                Row {
                    // 下载按钮
                    IconButton(
                        onClick = {
                            try {
                                viewModel.startDownload(magnetLink, fileIndex)
                            } catch (e: Exception) {
                                errorMessage = e.message ?: "下载失败"
                                showErrorDialog = true
                            }
                        },
                        enabled = !isDownloading
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "下载",
                            tint = if (isDownloading) MaterialTheme.colorScheme.outline 
                                  else MaterialTheme.colorScheme.primary
                        )
                    }
        
                    // 视频播放按钮 - 仅对视频文件显示
                    if (file.name.endsWith(".mp4", ignoreCase = true) ||
                        file.name.endsWith(".mkv", ignoreCase = true) ||
                        file.name.endsWith(".avi", ignoreCase = true)) {
                        IconButton(
                            onClick = { onPlayClick("$magnetLink#$fileIndex") }
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "播放",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}