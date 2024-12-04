package top.phj233.magplay

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.documentfile.provider.DocumentFile
import org.koin.compose.koinInject
import top.phj233.magplay.nav.MagPlayNavHost
import top.phj233.magplay.repository.preferences.SettingsMMKV
import top.phj233.magplay.ui.screens.storage.StorageScreen
import top.phj233.magplay.ui.screens.storage.StorageViewModel
import top.phj233.magplay.ui.theme.AppTheme

@Composable
fun App() {
    val settingsMMKV: SettingsMMKV = koinInject()
    val storageViewModel: StorageViewModel = koinInject()
    val context = LocalContext.current

    val themeState by settingsMMKV.themeState.collectAsState()
    val needSelectStorage by storageViewModel.needSelectStorage.collectAsState()

    // 检查存储路径和首次启动
    LaunchedEffect(Unit) {
        // 只有当 needSelectStorage 为 true 时才进行检查
        // 这样可以避免覆盖已经设置好的状态
        Log.d("App", "needSelectStorage: $needSelectStorage")
        Log.d("App", "storagePath: ${settingsMMKV.getStoragePath()}")
        if (needSelectStorage) {
            val storagePath = settingsMMKV.getStoragePath()
            if (storagePath != null) {
                val uri = Uri.parse(storagePath)
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                    val documentFile = DocumentFile.fromTreeUri(context, uri)
                    if (documentFile?.exists() == true && documentFile.canRead()) {
                        storageViewModel.setSelectedUri(uri)
                        storageViewModel.setNeedSelectStorage(false)
                    }
                } catch (e: SecurityException) {
                    // 权限失效时不做任何操作，保持 needSelectStorage 为 true
                    Toast.makeText(context, "存储路径权限失效，请重新选择", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    AppTheme(themeState = themeState) {
        if (needSelectStorage) {
            StorageScreen(
                viewModel = storageViewModel,
                onStorageSelected = {
                    storageViewModel.setNeedSelectStorage(false)
                }
            )
        } else {
            MagPlayNavHost()
        }
    }
}