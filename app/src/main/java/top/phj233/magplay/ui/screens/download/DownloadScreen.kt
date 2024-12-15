package top.phj233.magplay.ui.screens.download

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import top.phj233.magplay.ui.components.MagPlayTopBar
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadScreen() {
    val viewModel: DownloadViewModel = koinViewModel()
    val downloads by viewModel.downloads.collectAsState()

    Scaffold(
        topBar = {
            MagPlayTopBar(
                title = "下载管理",
            )
        }
    ) { paddingValues ->
        if (downloads.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "暂无下载任务",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(downloads) { download ->
                    DownloadItem(
                        downloadState = download,
                        onPauseResume = { viewModel.togglePause(download.torrentHandle) },
                        onDelete = { viewModel.removeDownload(download.torrentHandle) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloadItem(
    downloadState: DownloadState,
    onPauseResume: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = downloadState.title,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "创建时间: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(downloadState.createTime)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row {
                    IconButton(onClick = onPauseResume) {
                        Icon(
                            imageVector = if (downloadState.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = if (downloadState.isPaused) "继续" else "暂停"
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "删除"
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LinearProgressIndicator(
                progress = { downloadState.progress },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${formatSize(downloadState.downloadedSize)} / ${formatSize(downloadState.totalSize)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "${(downloadState.progress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "↓ ${formatSpeed(downloadState.downloadSpeed)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "↑ ${formatSpeed(downloadState.uploadSpeed)}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

private fun formatSpeed(bytesPerSecond: Long): String {
    return when {
        bytesPerSecond < 1024 -> "${bytesPerSecond}B/s"
        bytesPerSecond < 1024 * 1024 -> (bytesPerSecond / 1024).toString() + "KB/s"
        bytesPerSecond < 1024 * 1024 * 1024 -> (bytesPerSecond / (1024 * 1024)).toString() + "MB/s"
        else -> (bytesPerSecond / (1024 * 1024 * 1024)).toString() + "GB/s"
    }
}

private fun formatSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "${bytes}B"
        bytes < 1024 * 1024 -> (bytes / 1024).toString() + "KB"
        bytes < 1024 * 1024 * 1024 -> (bytes / (1024 * 1024)).toString() + "MB"
        else -> (bytes / (1024 * 1024 * 1024)).toString() + "GB"
    }
}
