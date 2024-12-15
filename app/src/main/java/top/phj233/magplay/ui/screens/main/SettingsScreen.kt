package top.phj233.magplay.ui.screens.main

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.rounded.Create
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DriveFolderUpload
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import top.phj233.magplay.nav.LocalNavController
import top.phj233.magplay.nav.navSetting
import top.phj233.magplay.ui.screens.start.StartViewModel

@Composable
fun SettingsScreen() {
    val nav = LocalNavController.current
    val startViewModel = koinInject<StartViewModel>()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var showStorageDialog by remember { mutableStateOf(false) }
    val selectedUri by startViewModel.selectedUri.collectAsState()

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let { 
            startViewModel.setSelectedUri(it)
            scope.launch {
                snackbarHostState.showSnackbar("存储路径已更新")
            }
        }
    }
    Column(modifier = Modifier.padding(16.dp)) {
        // TODO MagPlay Big Logo
        HorizontalDivider()
        // 主题设置
        ListItem(
            headlineContent = { Text("主题设置") },
            leadingContent = { Icon(Icons.Rounded.Create, contentDescription = null) },
            modifier = Modifier.clickable(onClick = { nav.navSetting("theme") })
        )
        // 存储位置设置
        ListItem(
            headlineContent = { Text("存储设置") },
            leadingContent = { Icon(Icons.Rounded.DriveFolderUpload, contentDescription = null) },
            supportingContent = { 
                Text(
                    text = selectedUri?.lastPathSegment ?: "未设置",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
            trailingContent = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 更改位置按钮
                    IconButton(onClick = { launcher.launch(null) }) {
                        Icon(
                            imageVector = Icons.Filled.FileUpload,
                            contentDescription = "更改存储位置",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    // 重置位置按钮
                    IconButton(
                        onClick = { showStorageDialog = true }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = "重置存储位置",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        )
        ListItem(
            headlineContent = { Text("关于") },
            leadingContent = { Icon(Icons.Rounded.Info, contentDescription = null) },
            modifier = Modifier.clickable(onClick = { nav.navSetting("about") })
        )

    }
   if (showStorageDialog) {
        AlertDialog(
            onDismissRequest = { showStorageDialog = false },
            title = { Text("重置存储位置") },
            text = { Text("确定要重置存储位置吗？这将清除当前的存储路径设置。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        startViewModel.resetSelectedUri()
                        showStorageDialog = false

                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStorageDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}
