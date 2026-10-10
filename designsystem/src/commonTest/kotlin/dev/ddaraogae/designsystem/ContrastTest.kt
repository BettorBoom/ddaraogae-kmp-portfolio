package dev.ddaraogae.designsystem

import androidx.compose.ui.graphics.Color
import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertTrue

class ContrastTest {
    private fun luminance(color: Color): Double {
        fun channel(value: Float): Double = if (value <= 0.04045f) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
        return channel(color.red) * 0.2126 + channel(color.green) * 0.7152 + channel(color.blue) * 0.0722
    }

    @Test fun brandTextPairsMeetNormalTextContrast() {
        for (colors in listOf(lightColors, darkColors)) {
            for ((text, background) in listOf(colors.onPrimary to colors.primary, colors.onSurface to colors.surface,
                colors.onSurface to colors.surfaceContainer, colors.onSurfaceVariant to colors.surfaceContainer,
                colors.onBackground to colors.background, colors.error to colors.surface)) {
                val a = luminance(text); val b = luminance(background)
                val ratio = (maxOf(a, b) + 0.05) / (minOf(a, b) + 0.05)
                assertTrue(ratio >= 4.5, "Contrast $ratio: $text on $background")
            }
        }
    }
}
