package dev.ddaraogae.shared

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import dev.ddaraogae.auth.LoginContent
import dev.ddaraogae.auth.SignUpContent
import dev.ddaraogae.designsystem.AppTheme
import dev.ddaraogae.designsystem.DdaraogaeTheme
import dev.ddaraogae.designsystem.LocalToast
import dev.ddaraogae.designsystem.RoutePoint
import dev.ddaraogae.designsystem.ScreenStatus
import dev.ddaraogae.designsystem.ToastHost
import dev.ddaraogae.designsystem.ToastState
import dev.ddaraogae.designsystem.LegacyBottomBar
import dev.ddaraogae.designsystem.LegacyTab
import dev.ddaraogae.dogs.PetFormScreen
import dev.ddaraogae.dogs.PetListScreen
import dev.ddaraogae.domain.DogGender
import dev.ddaraogae.domain.DogProfile
import dev.ddaraogae.domain.StampType
import dev.ddaraogae.domain.WalkSession
import dev.ddaraogae.domain.WalkSessionState
import dev.ddaraogae.domain.WalkStatus
import dev.ddaraogae.domain.earnedBy
import dev.ddaraogae.domain.stampAwards
import dev.ddaraogae.history.HistoryScreen
import dev.ddaraogae.mypage.AlarmScreen
import dev.ddaraogae.mypage.MyPageContent
import dev.ddaraogae.stamp.StampDetailScreen
import dev.ddaraogae.stamp.StampListScreen
import dev.ddaraogae.walk.FinishContent
import dev.ddaraogae.walk.WalkContent
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant

// Compiled only with ddaraogae.scenarios=true. No SDK, DB or Koin fixture. Every screen is the
// real composable fed with fixed data, so the matrix checks the shipped layout (ADR-006).

private val zone = TimeZone.of("Asia/Seoul")
private val today = LocalDate(2026, 10, 10)
private val nowMillis = LocalDateTime(today, LocalTime(12, 0)).toInstant(zone).toEpochMilliseconds()

private val dog = DogProfile("fixture-user", "보리", DogGender.Female, 3, "말티즈", "산책을 좋아해요")
private const val longDogName = "아주아주긴이름을가진반려견"

private fun fixtureWalk(id: String, daysAgo: Int, hour: Int, meters: Double, minutes: Long, dogName: String = "보리"): WalkSession {
    val day = today.plus(-daysAgo, DateTimeUnit.DAY)
    val start = LocalDateTime(day, LocalTime(hour, 10)).toInstant(zone).toEpochMilliseconds()
    return WalkSession(id, "fixture-user", start, WalkStatus.Completed, start + minutes * 60_000, dogName,
        meters, minutes * 60_000)
}

private fun fixtureHistory(dogName: String = "보리") = listOf(
    fixtureWalk("w1", 0, 9, 2_300.0, 42, dogName), fixtureWalk("w2", 1, 18, 1_800.0, 35, dogName),
    fixtureWalk("w3", 2, 7, 5_200.0, 61, dogName), fixtureWalk("w4", 3, 20, 900.0, 18, dogName),
    fixtureWalk("w5", 4, 8, 8_900.0, 95, dogName), fixtureWalk("w6", 6, 19, 3_700.0, 52, dogName),
    fixtureWalk("w7", 9, 10, 1_300.0, 27, dogName),
)

private val route = (0..12).map { i ->
    RoutePoint(37.5000 + 0.0004 * (if (i < 7) i else 12 - i), 127.0000 + 0.0003 * i + (if (i % 2 == 0) 0.00005 else 0.0), 0)
}

@Composable
fun DdaraogaeScenario(screen: String, stateName: String, dark: Boolean, fontScale: Float) {
    val density = LocalDensity.current
    val scale = fontScale.takeIf { it.isFinite() && it > 0 }?.coerceIn(1f, 2f) ?: density.fontScale
    var currentScreen by remember(screen) { mutableStateOf(screen) }
    var currentState by remember(screen, stateName) { mutableStateOf(stateName) }
    var recording by remember(screen, stateName) { mutableStateOf(true) }
    val toast = remember { ToastState() }
    val status = when (currentState) {
        "loading" -> ScreenStatus.Loading
        "empty" -> ScreenStatus.Empty
        "error" -> ScreenStatus.Error
        "denied" -> ScreenStatus.PermissionDenied
        else -> ScreenStatus.Ready
    }
    val long = currentState == "long"
    val name = if (long) longDogName else "보리"
    val profile = dog.copy(name = name)
    val history = fixtureHistory(name)
    val awards = stampAwards(history, zone)
    val finished = history.first()
    CompositionLocalProvider(LocalDensity provides Density(density.density, scale), LocalToast provides toast) {
        DdaraogaeTheme(dark) {
            Surface(Modifier.fillMaxSize(), color = AppTheme.colors.white) {
                Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                    Text("개발용 예시 데이터 · $currentState", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp))
                    Box(Modifier.weight(1f)) {
                        val retry = { currentState = "ready" }
                        val gallery = { currentScreen = "gallery" }
                        when (currentScreen) {
                            "home" -> HomeContent(if (status == ScreenStatus.Empty) null else profile,
                                if (status == ScreenStatus.Empty) emptyList() else history, nowMillis, zone, status, retry,
                                onAddDog = {}, onHistory = {}, onStamps = {})
                            "walk" -> WalkContent(
                                WalkSessionState(if (status != ScreenStatus.Ready || !recording) null else
                                    WalkSession("fixture-walk", "fixture-user", 1_000L, WalkStatus.Recording, dogName = name,
                                        distanceMeters = 1_380.0, activeDurationMillis = 2_520_000L),
                                    loaded = status != ScreenStatus.Loading, failed = status == ScreenStatus.Error),
                                busy = false, notice = if (long) "아주 긴 산책 안내예요. 반려견의 속도에 맞춰 안전하게 걸으며 화면을 이동해도 기록이 이어지는지 확인해 주세요." else null,
                                dogName = name, elapsedMillis = 2_520_000L, onStart = { recording = true },
                                onStop = { recording = false }, onResume = {}, onFinishInterrupted = {},
                                status = status, onRetry = retry)
                            "finish" -> FinishContent(if (status == ScreenStatus.Empty) null else finished, route,
                                awards.earnedBy(finished.id).ifEmpty { listOf(StampType.DailyWalk, StampType.Streak3) },
                                status, zone, onDone = gallery, onRetry = retry)
                            "history" -> HistoryScreen(profile, history, nowMillis, zone, { flowOf(route) }, gallery)
                            "stamps" -> StampListScreen(gallery, onOpen = {})
                            "stamp-detail" -> StampDetailScreen(StampType.DailyWalk, awards, zone, gallery)
                            "mypage" -> Column {
                                Box(Modifier.weight(1f)) { MyPageContent(onDogs = {}, onAlarm = {}) }
                                LegacyBottomBar(LegacyTab.MyPage, onSelect = {})
                            }
                            "pets" -> PetListScreen(profile, gallery, onAdd = {}, onOpen = {})
                            "pet-form" -> PetFormScreen(profile, busy = false, onBack = gallery, onSubmit = {}, onDelete = {})
                            "alarm" -> AlarmScreen(gallery)
                            "login" -> LoginContent()
                            "signup" -> SignUpContent(onBack = gallery)
                            else -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
                                Text("화면 갤러리", style = MaterialTheme.typography.titleLarge)
                                Text("개발용 예시 데이터 · 실제 계정·지도·권한을 사용하지 않아요.")
                                Text("${if (dark) "어두운" else "밝은"} 테마 · 글씨 ${scale}배")
                                for (screenName in scenarioScreens) {
                                    OutlinedButton(onClick = { currentScreen = screenName }) { Text(screenName) }
                                }
                                Text("상태: $currentState")
                                for (stateLabel in listOf("ready", "loading", "empty", "error", "denied", "long")) {
                                    TextButton(onClick = { currentState = stateLabel }) { Text(stateLabel) }
                                }
                            }
                        }
                        ToastHost(toast, Modifier.align(androidx.compose.ui.Alignment.BottomCenter).padding(bottom = 24.dp))
                    }
                }
            }
        }
    }
}

internal val scenarioScreens = listOf("home", "walk", "finish", "history", "stamps", "stamp-detail", "mypage",
    "pets", "pet-form", "alarm", "login", "signup")
