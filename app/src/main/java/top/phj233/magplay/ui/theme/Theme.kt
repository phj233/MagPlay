package top.phj233.magplay.ui.theme
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import top.phj233.magplay.repository.preferences.ThemeState

@Immutable
data class ColorFamily(
    val color: Color,
    val onColor: Color,
    val colorContainer: Color,
    val onColorContainer: Color
)

@Composable
fun AppTheme(
    themeState: ThemeState,
    content: @Composable () -> Unit
) {
    val darkTheme = themeState.darkModeEnabled || 
        (themeState.followSystemTheme && isSystemInDarkTheme())
    val context = LocalContext.current
    
    val colorScheme = when {
        themeState.dynamicColorEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        else -> {
            when (themeState.selectedTheme) {
                "预设1" -> if (darkTheme) PreColorOne.dark.colorScheme else PreColorOne.light.colorScheme
                "预设2" -> if (darkTheme) PreColorTwo.dark.colorScheme else PreColorTwo.light.colorScheme
                else -> if (darkTheme) MagPlayColor.dark.colorScheme else MagPlayColor.light.colorScheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}

