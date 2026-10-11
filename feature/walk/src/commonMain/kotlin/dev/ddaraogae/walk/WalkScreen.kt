package dev.ddaraogae.walk

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.ddaraogae.designsystem.AppIcons
import dev.ddaraogae.designsystem.AppText
import dev.ddaraogae.designsystem.AppTheme
import dev.ddaraogae.designsystem.BrownButton
import dev.ddaraogae.designsystem.DogCircle
import dev.ddaraogae.designsystem.EmptyDataChip
import dev.ddaraogae.designsystem.LocalToast
import dev.ddaraogae.designsystem.MapBackdrop
import dev.ddaraogae.designsystem.ScreenStatus
import dev.ddaraogae.designsystem.StatusNotice
import dev.ddaraogae.designsystem.spToDp
import dev.ddaraogae.domain.WalkSessionState
import dev.ddaraogae.domain.WalkStatus
import dev.ddaraogae.domain.formatDistance
import dev.ddaraogae.domain.formatWalkTimer
import kotlinx.coroutines.delay
import org.koin.core.scope.Scope
import kotlin.time.Clock

@Composable
fun WalkFeature(scope: Scope, onFinished: (String) -> Unit, locationNotice: String? = null) {
    val model = viewModel<WalkViewModel> { scope.get() }
    WalkScreen(model, onFinished, locationNotice)
}

@Composable
internal fun WalkScreen(model: WalkViewModel, onFinished: (String) -> Unit, locationNotice: String?) {
    val state by model.state.collectAsState()
    val busy by model.busy.collectAsState()
    val error by model.error.collectAsState()
    val dog by model.dog.collectAsState()
    val toast = LocalToast.current
    var now by remember { mutableLongStateOf(Clock.System.now().toEpochMilliseconds()) }
    var dogSelected by remember { mutableStateOf(true) }
    LaunchedEffect(state.session?.id, state.session?.status) {
        while (state.session?.status == WalkStatus.Recording) {
            now = Clock.System.now().toEpochMilliseconds()
            delay(1_000)
        }
    }
    // The original reports a failed start as a toast; the wording is its own.
    LaunchedEffect(error) { error?.let { toast.show(it); model.clearError() } }
    WalkContent(state, busy, notice = locationNotice, dogName = dog?.name,
        elapsedMillis = state.session?.elapsedMillis(now) ?: 0,
        dogSelected = dogSelected && dog != null, onToggleDog = { dogSelected = !dogSelected },
        onStart = { if (dogSelected) model.start() else toast.show("선택된 반려견이 없습니다") },
        onStop = { model.finish(onFinished) }, onResume = model::resume,
        onFinishInterrupted = { model.finish(onFinished) })
}

/**
 * The original walk tab (fragment_walk.xml): a full-screen map, the dog picker at the top and a
 * brown "산책하기" pill; while walking, a white bar with distance, the stop button and time.
 * The map is the original's fixed sample image until a real map SDK is connected (ADR-013).
 */
@Composable
fun WalkContent(
    state: WalkSessionState,
    busy: Boolean,
    notice: String?,
    dogName: String?,
    elapsedMillis: Long,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onResume: () -> Unit,
    onFinishInterrupted: () -> Unit,
    status: ScreenStatus = when { state.failed -> ScreenStatus.Error; !state.loaded -> ScreenStatus.Loading; else -> ScreenStatus.Ready },
    onRetry: (() -> Unit)? = null,
    dogSelected: Boolean = true,
    onToggleDog: () -> Unit = {},
) {
    val colors = AppTheme.colors
    val session = state.session
    val recording = session?.status == WalkStatus.Recording
    val interrupted = session?.status == WalkStatus.Interrupted
    val startEnabled = !busy && state.loaded && !state.failed &&
        status != ScreenStatus.Loading && status != ScreenStatus.PermissionDenied
    Box(Modifier.fillMaxSize().background(colors.white)) {
        MapBackdrop(Modifier.fillMaxSize())
        Text("샘플 지도 · 실제 위치와 다름", style = AppText.mini,
            color = colors.blackFix,
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 124.dp, end = 16.dp).widthIn(max = 180.dp)
                .background(colors.whiteFix.copy(alpha = 0.85f), RoundedCornerShape(6.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp))
        if (!recording && !interrupted) {
            Row(Modifier.align(Alignment.TopStart).padding(start = 20.dp, end = 20.dp, top = 32.dp)) {
                DogChip(dogName ?: "반려견 없음", dogSelected && dogName != null, onToggleDog, enabled = dogName != null)
            }
        }
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            StatusNotice(status, "첫 산책을 시작하면 함께한 시간이 여기에 쌓여요.", onRetry)
            if (notice != null) EmptyDataChip(notice, Modifier.padding(horizontal = 20.dp), AppText.caption)
            when {
                recording -> WalkingBar(session?.distanceMeters ?: 0.0, elapsedMillis, onStop)
                interrupted -> InterruptedSheet(session?.distanceMeters ?: 0.0,
                    session?.activeDurationMillis ?: 0L, busy, onResume, onFinishInterrupted)
                else -> BrownButton("산책하기", onStart,
                    Modifier.padding(bottom = 20.dp).width(220.spToDp()).height(60.spToDp()).testTag("primary-action"),
                    shape = RoundedCornerShape(30.dp), textStyle = AppText.content.copy(fontWeight = FontWeight.Bold),
                    enabled = startEnabled)
            }
        }
    }
}

/** item_walk_dogs: 48dp circle, an orange checkbox hanging off its corner, a 10sp name below. */
@Composable
private fun DogChip(name: String, selected: Boolean, onToggle: () -> Unit, enabled: Boolean) {
    val colors = AppTheme.colors
    Column(Modifier.padding(start = 10.dp).width(64.dp).clickable(enabled = enabled, role = Role.Checkbox, onClick = onToggle)
        .semantics { contentDescription = "산책 진행 반려견 $name"; stateDescription = if (selected) "선택됨" else "선택 안 됨" }
        .testTag("walk-dog"), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(48.dp)) {
            DogCircle(size = 48.dp, ring = colors.lightGrey, ringWidth = 1.dp)
            Box(Modifier.align(Alignment.BottomEnd).offset(x = 4.dp, y = 8.dp)) {
                Box(Modifier.align(Alignment.Center).size(16.dp).background(colors.white))
                Icon(if (selected) AppIcons.CheckBox else AppIcons.CheckBoxBlank, contentDescription = null,
                    tint = colors.orange, modifier = Modifier.size(24.dp))
            }
        }
        Text(name, style = AppText.miniBold, color = colors.blackFix, textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp).fillMaxWidth())
    }
}

/** The white bar of the original while walking: distance | stop | time, over a 1dp light-grey line. */
@Composable
private fun WalkingBar(distanceMeters: Double, elapsedMillis: Long, onStop: () -> Unit) {
    val colors = AppTheme.colors
    Column(Modifier.fillMaxWidth().background(colors.whiteFix)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.lightGrey))
        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                Metric(formatDistance(distanceMeters), "거리", Modifier.padding(end = 56.dp))
            }
            Image(AppIcons.WalkStop, contentDescription = "산책 종료",
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(24.dp))
                    .clickable(role = Role.Button, onClickLabel = "산책 종료", onClick = onStop)
                    .padding(4.5.dp).testTag("primary-action"))
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                Metric(formatWalkTimer(elapsedMillis / 1000), "시간", Modifier.padding(start = 56.dp))
            }
        }
    }
}

@Composable
private fun Metric(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = AppText.bodyBold, color = AppTheme.colors.blackFix, textAlign = TextAlign.Center)
        Text(label, style = AppText.caption, color = AppTheme.colors.brown, modifier = Modifier.padding(top = 4.dp))
    }
}

/** Not in the original: a walk interrupted by a restart or the background needs resume/finish. */
@Composable
private fun InterruptedSheet(distanceMeters: Double, activeMillis: Long, busy: Boolean,
                             onResume: () -> Unit, onFinish: () -> Unit) {
    val colors = AppTheme.colors
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color = colors.whiteFix, shadowElevation = 6.dp) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("중단된 산책이 있어요", style = AppText.textBold, color = colors.blackFix)
            Text("${formatDistance(distanceMeters)} · ${formatWalkTimer(activeMillis / 1000)}",
                style = AppText.caption, color = colors.darkGrey)
            BrownButton("산책 이어가기", onResume, Modifier.fillMaxWidth().testTag("primary-action"),
                shape = RoundedCornerShape(10.dp), enabled = !busy)
            Text("산책 마치기", style = AppText.body.copy(fontWeight = FontWeight.Bold), color = colors.brown,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(10.dp))
                    .border(1.dp, colors.brown, RoundedCornerShape(10.dp))
                    .clickable(enabled = !busy, role = Role.Button, onClick = onFinish)
                    .padding(top = 10.dp).testTag("finish-interrupted"))
        }
    }
}
