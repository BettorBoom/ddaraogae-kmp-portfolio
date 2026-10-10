package dev.ddaraogae.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Own brand tokens; this is not an installation of Toss's restricted TDS.
internal val lightColors = lightColorScheme(
    primary = Color(0xFF6F4E37), onPrimary = Color.White,
    primaryContainer = Color(0xFFFED8B1), onPrimaryContainer = Color(0xFF3D2B1F),
    secondary = Color(0xFFA67B5B), background = Color(0xFFF6F6F6), onBackground = Color(0xFF171717),
    surface = Color.White, onSurface = Color(0xFF171717),
    surfaceVariant = Color(0xFFF6F6F6), onSurfaceVariant = Color(0xFF666666),
    surfaceContainer = Color.White, surfaceContainerLow = Color(0xFFF6F6F6),
    surfaceContainerHigh = Color(0xFFEEEEEE), surfaceContainerHighest = Color(0xFFE4E4E4),
    surfaceContainerLowest = Color.White, outlineVariant = Color(0xFFD9D9D9),
    error = Color(0xFFB3261E),
)

internal val darkColors = darkColorScheme(
    primary = Color(0xFFDEC3AA), onPrimary = Color(0xFF352416),
    primaryContainer = Color(0xFF6F4E37), onPrimaryContainer = Color.White,
    secondary = Color(0xFFDAB798), background = Color(0xFF151515), onBackground = Color(0xFFF5F5F5),
    surface = Color(0xFF222222), onSurface = Color(0xFFF5F5F5),
    surfaceVariant = Color(0xFF303030), onSurfaceVariant = Color(0xFFD4D4D4),
    surfaceContainer = Color(0xFF252525), surfaceContainerLow = Color(0xFF1D1D1D),
    surfaceContainerHigh = Color(0xFF303030), surfaceContainerHighest = Color(0xFF3A3A3A),
    surfaceContainerLowest = Color(0xFF171717), outlineVariant = Color(0xFF666666),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
)

@Composable
fun DdaraogaeTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) darkColors else lightColors,
        shapes = Shapes(medium = RoundedCornerShape(10.dp), large = RoundedCornerShape(14.dp)),
        content = content)
}
