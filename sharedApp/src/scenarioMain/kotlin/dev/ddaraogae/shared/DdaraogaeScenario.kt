package dev.ddaraogae.shared

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import dev.ddaraogae.designsystem.*
import dev.ddaraogae.domain.*
import dev.ddaraogae.walk.WalkContent
import dev.ddaraogae.history.RecordContent

// Compiled only with ddaraogae.scenarios=true. No SDK, DB or Koin fixture.
@Composable
fun DdaraogaeScenario(screen: String, stateName: String, dark: Boolean, fontScale: Float) {
    val density = LocalDensity.current
    val scale = fontScale.takeIf { it.isFinite() && it > 0 }?.coerceIn(1f, 2f) ?: density.fontScale
    var currentScreen by remember(screen) { mutableStateOf(screen) }
    var currentState by remember(screen, stateName) { mutableStateOf(stateName) }
    var recording by remember(screen, stateName) { mutableStateOf(true) }
    val status = when (currentState) {
        "loading" -> ScreenStatus.Loading
        "empty" -> ScreenStatus.Empty
        "error" -> ScreenStatus.Error
        "denied" -> ScreenStatus.PermissionDenied
        else -> ScreenStatus.Ready
    }
    val session = WalkSession("fixture-walk", "fixture-user", 1_000L,
        if (recording && currentScreen == "walk") WalkStatus.Recording else WalkStatus.Completed,
        if (recording && currentScreen == "walk") null else 2_521_000L,
        dogName = "보리", distanceMeters = 13.0, activeDurationMillis = 2_520_000L)
    val route = listOf(
        WalkPoint(1, 1, LocationSample(37.50000, 127.00000, 10_000L, 5.0)),
        WalkPoint(2, 1, LocationSample(37.50000, 127.00015, 12_000L, 5.0)),
    )
    CompositionLocalProvider(LocalDensity provides Density(density.density, scale)) {
        DdaraogaeTheme(dark) {
            Surface(Modifier.fillMaxSize()) {
                Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                    Text("개발용 예시 데이터 · $currentState", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp))
                    Box(Modifier.weight(1f)) {
                val retry = { currentState = "ready" }
                when (currentScreen) {
                    "home" -> HomeContent(status, currentState == "long", retry) { currentScreen = "walk" }
                    "walk" -> WalkContent(WalkSessionState(if (status == ScreenStatus.Empty) null else session,
                        loaded = status != ScreenStatus.Loading, failed = status == ScreenStatus.Error),
                        busy = false, error = if (currentState == "long") "아주 긴 산책 안내예요. 반려견의 속도에 맞춰 안전하게 걸으며 화면을 이동해도 기록이 이어지는지 확인해 주세요." else null,
                        onBack = { currentScreen = "gallery" }, onRecord = { currentScreen = "record" },
                        onAction = { recording = !recording }, status = status, onRetry = retry,
                        dogName = "보리", elapsedMillis = 2_520_000L,
                        pointsCount = if (status == ScreenStatus.Ready) route.size else 0)
                    "record" -> RecordContent(if (status == ScreenStatus.Empty) null else session,
                        status, currentState == "long", retry, onBack = { currentScreen = "gallery" }, points = route)
                    else -> Page("화면 갤러리") {
                        Text("개발용 예시 데이터 · 실제 계정·지도·권한을 사용하지 않아요.")
                        Text("${if (dark) "어두운" else "밝은"} 테마 · 글씨 ${scale}배")
                        for (name in listOf("home", "walk", "record")) {
                            OutlinedButton(onClick = { currentScreen = name }) { Text(name) }
                        }
                        Text("상태: $currentState")
                        for (name in listOf("ready", "loading", "empty", "error", "denied", "long")) {
                            TextButton(onClick = { currentState = name }) { Text(name) }
                        }
                    }
                }
                    }
                }
            }
        }
    }
}
