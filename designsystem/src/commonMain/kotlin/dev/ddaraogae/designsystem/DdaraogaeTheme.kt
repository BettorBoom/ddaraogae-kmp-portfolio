package dev.ddaraogae.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Own brand tokens from the original app's res/values(-night)/colors.xml and
// dimens.xml (ADR-011, ADR-013); this is not an installation of Toss's restricted TDS.

/** The original palette. Names follow colors.xml; night values follow values-night. */
@Immutable
class AppColors(
    val dark: Boolean,
    /** `white`: screen and bar background. */
    val white: Color,
    /** `black`: icons and chart text. */
    val black: Color,
    /** `black_white`: black by day, white by night (radio labels). */
    val blackWhite: Color,
    /** Default text: Material3 on-surface in day, forced white by the original night styles. */
    val text: Color,
    val darkGrey: Color,
    val bgGrey: Color,
    val brownTrans: Color,
    val softOrange: Color,
    val greyTrans: Color,
    /** Default CardView background. */
    val card: Color,
) {
    val grey = Color(0xFFD9D9D9)
    val lightGrey = Color(0xFFEEEEEE)
    val brown = Color(0xFF6F4E37)
    val lightBrown = Color(0xFFA67B5B)
    val verySoftOrange = Color(0xFFFED8B1)
    val orange = Color(0xFFF49D1A)
    val banana = Color(0xFFFFE15D)
    val red = Color(0xFFDC3535)
    val whiteFix = Color.White
    val blackFix = Color.Black
}

val LightAppColors = AppColors(
    dark = false, white = Color.White, black = Color.Black, blackWhite = Color.Black, text = Color(0xFF1D1B20),
    darkGrey = Color(0xFF7C7C7C), bgGrey = Color(0xFFF6F6F6), brownTrans = Color(0xFF6F4E37),
    softOrange = Color(0xFFECB176), greyTrans = Color(0xFFD9D9D9), card = Color.White,
)

val NightAppColors = AppColors(
    dark = true, white = Color(0xFF333333), black = Color(0xFFCAC4D0), blackWhite = Color.White, text = Color.White,
    darkGrey = Color(0xFFD9D9D9), bgGrey = Color(0xFF333333), brownTrans = Color(0xFFA67B5B),
    softOrange = Color(0xFFA67B5B), greyTrans = Color(0xFF7C7C7C), card = Color(0xFF424242),
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }

object AppTheme {
    val colors: AppColors
        @Composable @ReadOnlyComposable get() = LocalAppColors.current
}

/** The original dimens.xml text sizes; SF Pro is not bundled, so the system font renders them. */
object AppText {
    val headline = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold)
    val subtitle = TextStyle(fontSize = 28.sp)
    val content = TextStyle(fontSize = 24.sp)
    val body = TextStyle(fontSize = 20.sp)
    val bodyBold = body.copy(fontWeight = FontWeight.Bold)
    val text = TextStyle(fontSize = 18.sp)
    val textBold = text.copy(fontWeight = FontWeight.Bold)
    val caption = TextStyle(fontSize = 14.sp)
    val captionBold = caption.copy(fontWeight = FontWeight.Bold)
    val mini = TextStyle(fontSize = 10.sp)
    val miniBold = mini.copy(fontWeight = FontWeight.Bold)
}

internal val lightColors = lightColorScheme(
    primary = Color(0xFF6F4E37), onPrimary = Color.White,
    primaryContainer = Color(0xFFFED8B1), onPrimaryContainer = Color(0xFF3D2B1F),
    secondary = Color(0xFFA67B5B), background = Color(0xFFF6F6F6), onBackground = Color(0xFF1D1B20),
    surface = Color.White, onSurface = Color(0xFF1D1B20),
    surfaceVariant = Color(0xFFF6F6F6), onSurfaceVariant = Color(0xFF666666),
    surfaceContainer = Color.White, surfaceContainerLow = Color(0xFFF6F6F6),
    surfaceContainerHigh = Color(0xFFEEEEEE), surfaceContainerHighest = Color(0xFFE4E4E4),
    surfaceContainerLowest = Color.White, outlineVariant = Color(0xFFD9D9D9),
    error = Color(0xFFB3261E),
)

internal val darkColors = darkColorScheme(
    primary = Color(0xFFDEC3AA), onPrimary = Color(0xFF352416),
    primaryContainer = Color(0xFF6F4E37), onPrimaryContainer = Color.White,
    secondary = Color(0xFFDAB798), background = Color(0xFF333333), onBackground = Color.White,
    surface = Color(0xFF333333), onSurface = Color.White,
    surfaceVariant = Color(0xFF424242), onSurfaceVariant = Color(0xFFD9D9D9),
    surfaceContainer = Color(0xFF424242), surfaceContainerLow = Color(0xFF333333),
    surfaceContainerHigh = Color(0xFF4A4A4A), surfaceContainerHighest = Color(0xFF555555),
    surfaceContainerLowest = Color(0xFF2B2B2B), outlineVariant = Color(0xFF7C7C7C),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
)

@Composable
fun DdaraogaeTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalAppColors provides if (dark) NightAppColors else LightAppColors) {
        MaterialTheme(colorScheme = if (dark) darkColors else lightColors,
            shapes = Shapes(medium = RoundedCornerShape(8.dp), large = RoundedCornerShape(16.dp)),
            content = content)
    }
}
