package dev.ddaraogae.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class ScreenStatus { Ready, Loading, Empty, Error, PermissionDenied }

/** The original lays several boxes out in `sp`, so they grow with the font size; do the same. */
@Composable
fun Int.spToDp(): Dp = with(LocalDensity.current) { this@spToDp.sp.toDp() }

/**
 * The original screens' top bar: a centered 20sp bold title, optional back arrow and trailing
 * action, and a thin divider underneath (fragment_mypage, fragment_pet_list, activity_alarm).
 * The title is centered between the two sides and wraps instead of running under a wide action.
 */
@Composable
fun AppBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    trailing: (@Composable BoxScope.() -> Unit)? = null,
    height: Dp = 64.dp,
    dividerHeight: Dp = 0.5.dp,
    titleStyle: TextStyle = AppText.bodyBold,
    backSize: Dp = 18.dp,
) {
    val colors = AppTheme.colors
    Column(modifier.fillMaxWidth().background(colors.white)) {
        Layout(content = {
            Box(Modifier.layoutId("start")) {
                if (onBack != null) {
                    Box(Modifier.padding(start = 8.dp).size(48.dp)
                        .clickable(role = Role.Button, onClickLabel = "뒤로", onClick = onBack)
                        .testTag("back"), contentAlignment = Alignment.Center) {
                        Icon(AppIcons.Back, contentDescription = "뒤로", tint = colors.black, modifier = Modifier.size(backSize))
                    }
                }
            }
            Box(Modifier.layoutId("end")) { trailing?.invoke(this) }
            Text(title, style = titleStyle, color = colors.text, textAlign = TextAlign.Center,
                modifier = Modifier.layoutId("title").padding(vertical = 12.dp).testTag("screen-title").semantics { heading() })
        }, Modifier.fillMaxWidth()) { measurables, constraints ->
            val loose = constraints.copy(minWidth = 0, minHeight = 0)
            val start = measurables.first { it.layoutId == "start" }.measure(loose)
            val end = measurables.first { it.layoutId == "end" }.measure(loose)
            val side = maxOf(start.width, end.width, 56.dp.roundToPx())
            val center = measurables.first { it.layoutId == "title" }
                .measure(loose.copy(maxWidth = (constraints.maxWidth - 2 * side).coerceAtLeast(0)))
            val bar = maxOf(height.roundToPx(), start.height, end.height, center.height)
            layout(constraints.maxWidth, bar) {
                start.placeRelative(0, (bar - start.height) / 2)
                end.placeRelative(constraints.maxWidth - end.width, (bar - end.height) / 2)
                center.placeRelative((constraints.maxWidth - center.width) / 2, (bar - center.height) / 2)
            }
        }
        if (dividerHeight > 0.dp) Box(Modifier.fillMaxWidth().height(dividerHeight).background(colors.grey))
    }
}

/** The original's solid brown rounded button (rec_walk_start / rec_finish_done / bg_btn_edit). */
@Composable
fun BrownButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(10.dp),
    textStyle: TextStyle = AppText.body,
    enabled: Boolean = true,
    color: Color = AppTheme.colors.brown,
    textColor: Color = Color.White,
) {
    Box(modifier.heightIn(min = 48.dp).clip(shape).background(color)
        .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center) {
        Text(text, style = textStyle, color = textColor, textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp))
    }
}

/** CardView with the original's 8dp corner and 1dp elevation. */
@Composable
fun WhiteCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(modifier, shape = RoundedCornerShape(8.dp), color = AppTheme.colors.card,
        shadowElevation = 1.dp, content = content)
}

/** bg_empty_data: the small white chip the original shows over empty charts and cards. */
@Composable
fun EmptyDataChip(text: String, modifier: Modifier = Modifier, textStyle: TextStyle = AppText.mini,
                  elevation: Dp = 4.dp) {
    Surface(modifier, shape = RoundedCornerShape(8.dp), color = AppTheme.colors.white, shadowElevation = elevation) {
        Text(text, style = textStyle, color = AppTheme.colors.text, modifier = Modifier.padding(16.dp))
    }
}

/** CircleImageView: the default thumbnail is the original's grey paw disc. */
@Composable
fun DogCircle(
    modifier: Modifier = Modifier,
    size: Dp = 100.dp,
    ring: Color? = null,
    ringWidth: Dp = 4.dp,
    inset: Dp = 0.dp,
    description: String? = null,
    image: ImageVector = AppIcons.DogDefault,
) {
    var frame = modifier.size(size).clip(CircleShape)
    if (ring != null) frame = frame.border(ringWidth, ring, CircleShape)
    Box(frame.padding(inset).semantics { if (description != null) contentDescription = description },
        contentAlignment = Alignment.Center) {
        Image(image, contentDescription = null, contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().clip(CircleShape))
    }
}

/** Two-line section header used across the original: bold 18sp title and an optional gray 14sp link. */
@Composable
fun Hint(text: String, modifier: Modifier = Modifier) {
    Text(text, style = AppText.caption, color = AppTheme.colors.darkGrey, modifier = modifier)
}

/** Centered hint card for a failed load or a denied permission, with the existing retry hook. */
@Composable
fun StatusNotice(status: ScreenStatus, emptyMessage: String, onRetry: (() -> Unit)?,
                 modifier: Modifier = Modifier) {
    if (status == ScreenStatus.Ready) return
    val message = when (status) {
        ScreenStatus.Loading -> "잠시 확인하고 있어요"
        ScreenStatus.Empty -> emptyMessage
        ScreenStatus.Error -> "불러오지 못했어요"
        ScreenStatus.PermissionDenied -> "위치 권한이 필요해요. 거절해도 이전 기록은 남아 있어요."
        ScreenStatus.Ready -> return
    }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        EmptyDataChip(message, textStyle = AppText.caption)
        if (onRetry != null && (status == ScreenStatus.Error || status == ScreenStatus.PermissionDenied)) {
            BrownButton(if (status == ScreenStatus.Error) "다시 시도" else "권한 안내 보기", onRetry,
                Modifier.padding(horizontal = 20.dp), shape = RoundedCornerShape(8.dp), textStyle = AppText.caption)
        }
    }
}

/** progress_bar_stamp: grey track, banana fill clipped at the progress level (the clip leaves a flat edge). */
@Composable
fun StampProgressBar(progress: Int, max: Int, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    val value = progress.coerceIn(0, max)
    Canvas(modifier.fillMaxWidth().height(20.dp)
        .semantics { progressBarRangeInfo = ProgressBarRangeInfo(value.toFloat(), 0f..max.toFloat()) }) {
        val corner = CornerRadius(size.height / 2f)
        drawRoundRect(colors.grey, cornerRadius = corner)
        val reach = if (max > 0) size.width * value / max else 0f
        clipRect(right = reach) { drawRoundRect(colors.banana, cornerRadius = corner) }
    }
}
