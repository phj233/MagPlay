package top.phj233.magplay.ui.screens.storage

import android.net.Uri
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import top.phj233.magplay.repository.preferences.SettingsMMKV

class StorageViewModel(
    private val settingsMMKV: SettingsMMKV // 添加 MMKV 依赖
) : ViewModel() {
    private val _selectedUri = MutableStateFlow<Uri?>(null)
    val selectedUri: StateFlow<Uri?> = _selectedUri
    
    // 添加检查是否已设置路径的状态
    private val _needSelectStorage = MutableStateFlow(true)
    val needSelectStorage: StateFlow<Boolean> = _needSelectStorage

    init {
        // 初始化时检查是否已经设置了存储路径
        val savedUri = settingsMMKV.getStoragePath()
        if (savedUri != null) {
            _selectedUri.value = Uri.parse(savedUri)
            _needSelectStorage.value = false
        }
    }

    fun setSelectedUri(uri: Uri) {
        _selectedUri.value = uri
        settingsMMKV.setStoragePath(uri.toString())
    }

    fun setNeedSelectStorage(need: Boolean) {
        _needSelectStorage.value = need
    }

    fun resetStorageState() {
        _selectedUri.value = null
        _needSelectStorage.value = true
        settingsMMKV.setStoragePath(null)
    }
}