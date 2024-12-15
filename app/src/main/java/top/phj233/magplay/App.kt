package top.phj233.magplay

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.documentfile.provider.DocumentFile
import androidx.navigation.compose.rememberNavController
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import top.phj233.magplay.nav.LocalNavController
import top.phj233.magplay.nav.MagPlayNavHost
import top.phj233.magplay.repository.preferences.SettingsMMKV
import top.phj233.magplay.ui.screens.start.StartScreen
import top.phj233.magplay.ui.screens.start.StartViewModel
import top.phj233.magplay.ui.theme.AppTheme

@Composable
fun App() {
    val settingsMMKV: SettingsMMKV = koinInject()
    val startViewModel: StartViewModel = koinViewModel()
    val context = LocalContext.current

    val themeState by settingsMMKV.themeState.collectAsState()
    val storagePermissionGranted by startViewModel.storagePermissionGranted.collectAsState()

    // 检查存储路径和首次启动
    LaunchedEffect(Unit) {
        startViewModel.checkStoragePermission()
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
                    // 存储路径有效，不需要重新选择
                    startViewModel.resetPermissionRequest()
                }
            } catch (e: SecurityException) {
                // 权限失效时不做任何操作，保持 permissionNeeded 为 true
                Toast.makeText(context, "存储路径权限失效，请重新选择", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    AppTheme(themeState = themeState) {
        val navController = rememberNavController()
        CompositionLocalProvider(LocalNavController provides navController) {
            if (!storagePermissionGranted || startViewModel.selectedUri.collectAsState().value == null) {
                StartScreen()
            } else {
                MagPlayNavHost()
            }
        }
    }
}