package dev.ddaraogae.designsystem

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.compose.resources.painterResource
import dev.ddaraogae.designsystem.resources.Res
import dev.ddaraogae.designsystem.resources.legacy_sample_map

@Composable
fun MapBackdrop(modifier: Modifier = Modifier) {
    // shortcut: original app's fixed preview is not georeferenced; replace
    // with a licensed map adapter before claiming live map display.
    Image(painterResource(Res.drawable.legacy_sample_map),
        contentDescription = "원본 앱의 고정 샘플 지도, 실제 위치와 무관", modifier = modifier,
        contentScale = ContentScale.Crop)
}
