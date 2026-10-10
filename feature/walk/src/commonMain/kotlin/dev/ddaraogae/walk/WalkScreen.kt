package dev.ddaraogae.walk

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.ddaraogae.domain.WalkStatus
import dev.ddaraogae.domain.WalkSessionState
import dev.ddaraogae.domain.WalkPoint
import dev.ddaraogae.designsystem.*
import kotlinx.coroutines.delay
import androidx.lifecycle.viewmodel.compose.viewModel
import org.koin.core.scope.Scope
import kotlin.time.Clock

@Composable
fun WalkFeature(scope: Scope, onBack: () -> Unit, onRecord: (String) -> Unit,
                locationNotice: String? = null) {
    val model = viewModel<WalkViewModel> { scope.get() }
    WalkScreen(model, onBack, onRecord, locationNotice)
}

@Composable
internal fun WalkScreen(model: WalkViewModel, onBack: () -> Unit, onRecord: (String) -> Unit,
                        locationNotice: String?) {
    val state by model.state.collectAsState()
    val busy by model.busy.collectAsState()
    val error by model.error.collectAsState()
    val dog by model.dog.collectAsState()
    val points by remember(state.session?.id) { state.session?.id?.let(model::points) }
        ?.collectAsState(initial = emptyList()) ?: remember { mutableStateOf(emptyList<WalkPoint>()) }
    var now by remember { mutableLongStateOf(Clock.System.now().toEpochMilliseconds()) }
    LaunchedEffect(state.session?.id, state.session?.status) {
        while (state.session?.status == WalkStatus.Recording) {
            now = Clock.System.now().toEpochMilliseconds()
            delay(1_000)
        }
    }
    WalkContent(state, busy, error, onBack, onRecord,
        onAction = { when (state.session?.status) {
            WalkStatus.Recording -> model.finish()
            WalkStatus.Interrupted -> model.resume()
            else -> model.start()
        } }, dogName = dog?.name, elapsedMillis = state.session?.elapsedMillis(now) ?: 0,
        pointsCount = points.size, onFinishInterrupted = { model.finish() }, locationNotice = locationNotice)
}

@Composable
fun WalkContent(state: WalkSessionState, busy: Boolean, error: String?, onBack: () -> Unit,
                onRecord: (String) -> Unit, onAction: () -> Unit,
                status: ScreenStatus = when { state.failed -> ScreenStatus.Error; !state.loaded -> ScreenStatus.Loading; else -> ScreenStatus.Ready },
                onRetry: (() -> Unit)? = null, dogName: String? = null,
                elapsedMillis: Long = 0, pointsCount: Int = 0,
                onFinishInterrupted: () -> Unit = {}, locationNotice: String? = null) {
    val session = state.session
    val recording = session?.status == WalkStatus.Recording
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).safeDrawingPadding()) {
        MapBackdrop(Modifier.fillMaxSize())
        Column(Modifier.align(Alignment.TopStart).fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            TextButton(onClick = onBack, colors = ButtonDefaults.textButtonColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Text("홈으로")
            }
            Text(if (recording) "함께 걷는 중이에요" else "오늘도 함께 걸어요",
                modifier = Modifier.testTag("screen-title").semantics { heading() },
                style = MaterialTheme.typography.titleLarge)
            Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DogAvatar(Modifier.size(30.dp))
                    Text(session?.dogName ?: dogName ?: "함께할 친구", style = MaterialTheme.typography.bodyMedium)
                }
            }
            Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surface) {
                Text("원본 앱 샘플 지도 · 현재 위치와 무관", Modifier.padding(8.dp),
                    style = MaterialTheme.typography.bodySmall)
            }
        }
        val actionLabel = if (!state.loaded) "기록 확인 중…" else if (busy) "저장 중…"
            else if (recording) "산책 마치기"
            else if (session?.status == WalkStatus.Interrupted) "산책 이어가기" else "산책 시작하기"
        val actionEnabled = !busy && state.loaded && !state.failed &&
            status != ScreenStatus.Loading && status != ScreenStatus.PermissionDenied
        if (session == null && status == ScreenStatus.Ready && error == null && locationNotice == null) {
            Button(onClick = onAction, enabled = actionEnabled,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp)
                    .width(220.dp).heightIn(min = 60.dp).testTag("primary-action"),
                shape = RoundedCornerShape(16.dp)) {
                Text(actionLabel)
            }
        } else {
            Surface(Modifier.align(Alignment.BottomCenter).fillMaxWidth().heightIn(max = 400.dp),
                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
                color = MaterialTheme.colorScheme.surface, shadowElevation = 6.dp) {
                Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    StateNotice(status, "첫 산책을 시작하면 함께한 시간이 여기에 쌓여요.", onRetry)
                    if (session != null) {
                        val shownTime = if (recording) elapsedMillis else session.activeDurationMillis
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(if (pointsCount == 0) "0m" else "${session.distanceMeters.toInt()}m",
                                    style = MaterialTheme.typography.titleLarge)
                                Text("거리", color = MaterialTheme.colorScheme.primary)
                            }
                            Button(onClick = onAction, enabled = actionEnabled,
                                modifier = Modifier.heightIn(min = 56.dp).testTag("primary-action"),
                                shape = RoundedCornerShape(16.dp)) {
                                Text(actionLabel)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${shownTime / 60_000}분 ${(shownTime / 1000) % 60}초",
                                    style = MaterialTheme.typography.titleLarge)
                                Text("시간", color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    } else {
                        PrimaryAction(actionLabel, enabled = actionEnabled, onClick = onAction)
                    }
                    if (pointsCount > 0) Text("위치 ${pointsCount}개 기록 중 · 종료 후 경로 도식에서 확인",
                        style = MaterialTheme.typography.bodySmall)
                    locationNotice?.let { Text(it, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    if (state.failed) Text("저장된 기록을 읽지 못했어요. 앱을 다시 열어 주세요.",
                        color = MaterialTheme.colorScheme.error)
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    if (session?.status == WalkStatus.Interrupted) {
                        OutlinedButton(onClick = onFinishInterrupted, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                            Text("중단된 산책 마치기")
                        }
                    }
                    session?.let { walk ->
                        OutlinedButton(onClick = { onRecord(walk.id) }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                            Text("산책 기록 보기")
                        }
                    }
                }
            }
        }
    }
}
