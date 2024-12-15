package top.phj233.magplay.ui.screens.start

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.context.GlobalContext
import top.phj233.magplay.repository.preferences.SettingsMMKV

class StartViewModel : ViewModel() {
    private val context by lazy { GlobalContext.get().get<Context>() }
    private val settingsMMKV by lazy { GlobalContext.get().get<SettingsMMKV>() }

    private val _storagePermissionGranted = MutableStateFlow(false)
    val storagePermissionGranted: StateFlow<Boolean> = _storagePermissionGranted

    private val _permissionNeeded = MutableStateFlow(false)
    val permissionNeeded: StateFlow<Boolean> = _permissionNeeded

    private val _selectedUri = MutableStateFlow<Uri?>(null)
    val selectedUri: StateFlow<Uri?> = _selectedUri

    init {
        checkStoragePermission()
        // 从 SettingsMMKV 恢复已保存的 URI
        settingsMMKV.getStoragePath()?.let {
            _selectedUri.value = Uri.parse(it)
        }
    }

    /**
     * 检查是否有必要的存储权限
     */
    fun checkStoragePermission() {
        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
        _storagePermissionGranted.value = hasPermission
    }

    /**
     * 设置选中的URI
     */
    fun setSelectedUri(uri: Uri) {
        _selectedUri.value = uri
        settingsMMKV.setStoragePath(uri.toString())
    }

    fun resetSelectedUri() {
        _selectedUri.value = null
        settingsMMKV.setStoragePath(null)
    }

    /**
     * 重置权限请求状态
     */
    fun resetPermissionRequest() {
        _permissionNeeded.value = false
    }
}
