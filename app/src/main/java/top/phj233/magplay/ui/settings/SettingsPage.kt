package top.phj233.magplay.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.compose.koinInject
import top.phj233.magplay.repository.preferences.SettingsMMKV
import top.phj233.magplay.ui.components.MagPlayTopBar
import top.phj233.magplay.ui.components.ThemePreview

object SettingsPage {
    @Composable
    fun ThemeSetting() {
        val settingsMMKV: SettingsMMKV = koinInject()
        val themeState by settingsMMKV.themeState.collectAsState()

        Scaffold(
            topBar = {
                MagPlayTopBar("主题设置")
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                // 预设主题选择
                Text("预设主题", style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf("默认", "预设1", "预设2").forEach { theme ->
                        FilterChip(
                            selected = themeState.selectedTheme == theme,
                            onClick = {
                                settingsMMKV.updateTheme {
                                    copy(selectedTheme = theme)
                                }
                            },
                            label = { Text(theme) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 主题预览
                ThemePreview()
                Spacer(modifier = Modifier.height(16.dp))

                // 其他设置选项
                ListItem(
                    headlineContent = { Text("动态取色") },
                    trailingContent = {
                        Switch(
                            checked = themeState.dynamicColorEnabled,
                            onCheckedChange = { enabled ->
                                settingsMMKV.updateTheme {
                                    copy(dynamicColorEnabled = enabled)
                                }
                            }
                        )
                    }
                )

                ListItem(
                    headlineContent = { Text("深色模式") },
                    trailingContent = {
                        Switch(
                            checked = themeState.darkModeEnabled,
                            onCheckedChange = { enabled ->
                                settingsMMKV.updateTheme {
                                    copy(darkModeEnabled = enabled)
                                }
                            }
                        )
                    }
                )

                ListItem(
                    headlineContent = { Text("跟随系统主题") },
                    trailingContent = {
                        Switch(
                            checked = themeState.followSystemTheme,
                            onCheckedChange = { enabled ->
                                settingsMMKV.updateTheme {
                                    copy(followSystemTheme = enabled)
                                }
                            }
                        )
                    }
                )
            }
        }
    }
}