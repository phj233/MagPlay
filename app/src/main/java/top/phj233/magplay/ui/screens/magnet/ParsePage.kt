package top.phj233.magplay.ui.screens.magnet

import android.text.format.Formatter.formatFileSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import top.phj233.magplay.torrent.TorrentState
import top.phj233.magplay.torrent.MagPlayTorrentInfo
import top.phj233.magplay.torrent.TorrentFile
import top.phj233.magplay.ui.components.MagPlayTopBar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ParsePage(magnetLink: String) {
    val viewModel = viewModel<ParseViewModel>()
    val torrentState by viewModel.torrentState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(magnetLink) {
        viewModel.parseMagnet(magnetLink)
    }

    Scaffold(
        topBar = { MagPlayTopBar("文件信息") }
    ) { padding ->
        when (val state = torrentState) {
            is TorrentState.Parsing -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
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
                    items(state.info.files) {
                        TorrentFileCard(it)
                    }
                }
            }
            is TorrentState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = state.message)
                }
            }
            else -> {}
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
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = info.name,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "总大小: ${formatFileSize(LocalContext.current, info.totalSize)}",
                style = MaterialTheme.typography.bodyMedium
            )
            info.comment?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "备注: $it",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            info.creator?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "创建者: $it",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            info.creationDate?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "创建时间: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(
                        Date(it * 1000)
                    )}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun TorrentFileCard(file: TorrentFile) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = file.path,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "大小: ${formatFileSize(LocalContext.current, file.size)}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "路径: ${file.path}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}