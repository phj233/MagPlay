package top.phj233.magplay.repository.preferences

import com.tencent.mmkv.MMKV
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 用于存储设置的 MMKV
 */
class SettingsMMKV {
    private val mmkv = MMKV.mmkvWithID("settings")
    
    private val _themeState = MutableStateFlow(ThemeState(
        selectedTheme = mmkv.decodeString("selected_theme", "default") ?: "default",
        dynamicColorEnabled = mmkv.decodeBool("dynamic_color", false),
        darkModeEnabled = mmkv.decodeBool("dark_mode", false),
        followSystemTheme = mmkv.decodeBool("follow_system_theme", false)
    ))
    
    val themeState: StateFlow<ThemeState> = _themeState.asStateFlow()
    
    fun updateTheme(update: ThemeState.() -> ThemeState) {
        val newState = update(_themeState.value)
        _themeState.value = newState
        
        with(mmkv) {
            encode("selected_theme", newState.selectedTheme)
            encode("dynamic_color", newState.dynamicColorEnabled)
            encode("dark_mode", newState.darkModeEnabled)
            encode("follow_system_theme", newState.followSystemTheme)
        }
    }

        // 添加存储路径相关的方法
    fun setStoragePath(path: String?) {
        if (path == null) {
            mmkv.removeValueForKey("storage_path")
        } else {
            mmkv.encode("storage_path", path)
        }
    }

    fun getStoragePath(): String? {
        return mmkv.decodeString("storage_path")
    }

}

data class ThemeState(
    val selectedTheme: String,
    val dynamicColorEnabled: Boolean,
    val darkModeEnabled: Boolean,
    val followSystemTheme: Boolean
)