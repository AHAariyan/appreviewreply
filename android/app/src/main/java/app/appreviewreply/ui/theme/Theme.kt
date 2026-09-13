package app.appreviewreply.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Teal = Color(0xFF1F6F8B)
private val TealLight = Color(0xFF5FB0CC)
private val Amber = Color(0xFFD99A12)

private val LightColors = lightColorScheme(
    primary = Teal,
    secondary = Amber,
    tertiary = TealLight,
)

private val DarkColors = darkColorScheme(
    primary = TealLight,
    secondary = Amber,
    tertiary = Teal,
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
