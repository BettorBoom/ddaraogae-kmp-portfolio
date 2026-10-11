package dev.ddaraogae.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.ddaraogae.designsystem.resources.Res
import dev.ddaraogae.designsystem.resources.legacy_sample_map
import org.jetbrains.compose.resources.painterResource
import kotlin.math.cos

/** A saved location of a walk; [segment] changes after each resume so gaps are not joined. */
class RoutePoint(val latitude: Double, val longitude: Double, val segment: Int = 0)

@Composable
fun MapBackdrop(modifier: Modifier = Modifier) {
    // shortcut: the original's fixed preview image is not georeferenced; replace this
    // one seam with a licensed map adapter before claiming live map display. (Its mini
    // preview has a route baked in, so every map surface crops this clean one instead.)
    Image(painterResource(Res.drawable.legacy_sample_map),
        contentDescription = "원본 앱의 고정 샘플 지도, 실제 위치와 무관", modifier = modifier,
        contentScale = ContentScale.Crop)
}

/**
 * The saved route over the sample map, in the original's red. The line keeps its real
 * proportions but not its position, so the caption says so (ADR-011, ADR-013).
 */
@Composable
fun RouteMap(route: List<RoutePoint>, modifier: Modifier = Modifier, padding: Dp = 32.dp, caption: Boolean = true) {
    val red = AppTheme.colors.red
    Box(modifier.semantics { contentDescription = "기록된 위치 ${route.size}개를 표시한 경로 도식" }) {
        MapBackdrop(Modifier.fillMaxSize())
        Canvas(Modifier.fillMaxSize()) {
            if (route.size < 2) return@Canvas
            val pad = padding.toPx()
            val minLat = route.minOf { it.latitude }
            val maxLat = route.maxOf { it.latitude }
            val minLon = route.minOf { it.longitude }
            val maxLon = route.maxOf { it.longitude }
            val lonScale = cos((minLat + maxLat) / 2.0 * kotlin.math.PI / 180.0)
            val spanX = ((maxLon - minLon) * lonScale).coerceAtLeast(1e-9)
            val spanY = (maxLat - minLat).coerceAtLeast(1e-9)
            val scale = minOf((size.width - 2 * pad) / spanX, (size.height - 2 * pad) / spanY).toFloat()
            val offsetX = (size.width - (maxLon - minLon).toFloat() * lonScale.toFloat() * scale) / 2f
            val offsetY = (size.height - (maxLat - minLat).toFloat() * scale) / 2f
            fun at(point: RoutePoint) = Offset(
                offsetX + ((point.longitude - minLon) * lonScale).toFloat() * scale,
                size.height - offsetY - (point.latitude - minLat).toFloat() * scale)
            route.zipWithNext().forEach { (a, b) ->
                if (a.segment == b.segment) drawLine(red, at(a), at(b), 4.dp.toPx(), StrokeCap.Round)
            }
        }
        if (caption) Text("샘플 지도 · 실제 위치와 다름", style = AppText.mini, color = Color.Black,
            modifier = Modifier.align(Alignment.BottomStart).padding(8.dp)
                .background(Color(0xCCFFFFFF), RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp))
    }
}
