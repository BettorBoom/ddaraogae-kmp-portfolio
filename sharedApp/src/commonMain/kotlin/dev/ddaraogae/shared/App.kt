package dev.ddaraogae.shared

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import dev.ddaraogae.designsystem.AppTheme
import dev.ddaraogae.designsystem.DdaraogaeTheme
import dev.ddaraogae.designsystem.LegacyBottomBar
import dev.ddaraogae.designsystem.LegacyTab
import dev.ddaraogae.designsystem.LocalToast
import dev.ddaraogae.designsystem.RoutePoint
import dev.ddaraogae.designsystem.ScreenStatus
import dev.ddaraogae.designsystem.ToastHost
import dev.ddaraogae.designsystem.ToastState
import dev.ddaraogae.domain.StampAward
import dev.ddaraogae.domain.StampType
import dev.ddaraogae.domain.WalkSession
import dev.ddaraogae.domain.earnedBy
import dev.ddaraogae.domain.stampAwards
import dev.ddaraogae.dogs.PetDetailScreen
import dev.ddaraogae.dogs.PetFormScreen
import dev.ddaraogae.dogs.PetListScreen
import dev.ddaraogae.history.HistoryScreen
import dev.ddaraogae.mypage.AlarmScreen
import dev.ddaraogae.mypage.MyPageContent
import dev.ddaraogae.stamp.StampDetailScreen
import dev.ddaraogae.stamp.StampListScreen
import dev.ddaraogae.walk.FinishContent
import dev.ddaraogae.walk.WalkFeature
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.datetime.TimeZone
import kotlin.time.Clock

@Composable
fun DdaraogaeApp(runtime: AppRuntime, requestLocationPermission: () -> Unit = {}) {
    DdaraogaeTheme {
        val toast = remember { ToastState() }
        CompositionLocalProvider(LocalToast provides toast) {
            Surface(Modifier.fillMaxSize(), color = AppTheme.colors.white) {
                Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                    if (runtime.demoData) Text("개발용 합성 경로 · 실제 GPS가 아니에요.",
                        color = androidx.compose.material3.MaterialTheme.colorScheme.error)
                    val backStack = rememberNavBackStack(routeConfiguration, Home)
                    fun back() { if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) }
                    val dog by runtime.controller.dog.collectAsState()
                    val history by runtime.controller.history.collectAsState()
                    val zone = remember { TimeZone.currentSystemDefault() }
                    val now by produceState(Clock.System.now().toEpochMilliseconds()) {
                        while (true) { delay(30_000); value = Clock.System.now().toEpochMilliseconds() }
                    }
                    val awards = remember(history, zone) { stampAwards(history, zone) }
                    val current = backStack.last()
                    val tab = when (current) { Home -> LegacyTab.Home; Walk -> LegacyTab.Walk; MyPage -> LegacyTab.MyPage; else -> null }
                    Box(Modifier.weight(1f)) {
                        NavDisplay(backStack = backStack, onBack = { back() },
                            entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator(),
                                rememberViewModelStoreNavEntryDecorator()),
                            entryProvider = entryProvider {
                                entry<Home> {
                                    HomeContent(dog, history, now, zone,
                                        onAddDog = { backStack.add(PetForm(editing = false)) },
                                        onHistory = { backStack.add(History) }, onStamps = { backStack.add(Stamps) })
                                }
                                entry<Walk> {
                                    val locationNotice by runtime.locationNotice.collectAsState()
                                    val writeFailed by runtime.writeFailed.collectAsState()
                                    WalkFeature(runtime.userScope, onFinished = { backStack.add(Finish(it)) },
                                        locationNotice = if (writeFailed) "기록 저장에 문제가 있어요. 안전한 곳에서 산책을 마치고 다시 확인해 주세요." else locationNotice)
                                }
                                entry<MyPage> { MyPageContent(onDogs = { backStack.add(PetList) }, onAlarm = { backStack.add(Alarm) }) }
                                entry<History> {
                                    HistoryScreen(dog, history, now, zone, routeOf = { routeFlow(runtime, it.id) }, onBack = { back() })
                                }
                                entry<Stamps> { StampListScreen(onBack = { back() }, onOpen = { backStack.add(StampDetail(it.number)) }) }
                                entry<StampDetail> { key ->
                                    StampDetailScreen(StampType.entries.first { it.number == key.number }, awards, zone, onBack = { back() })
                                }
                                entry<Finish> { key -> FinishRoute(key.walkId, runtime, awards, zone, onDone = { back() }) }
                                entry<PetList> {
                                    PetListScreen(dog, onBack = { back() }, onOpen = { backStack.add(PetDetail) }, onAdd = {
                                        if (dog == null) backStack.add(PetForm(editing = false))
                                        else toast.show("이 시연판은 반려견 1마리만 저장해요.")
                                    })
                                }
                                entry<PetDetail> {
                                    androidx.compose.runtime.LaunchedEffect(dog) { if (dog == null) back() }
                                    PetDetailScreen(dog, onBack = { back() }, onEdit = { backStack.add(PetForm(editing = true)) })
                                }
                                entry<PetForm> { key ->
                                    var busy by remember { mutableStateOf(false) }
                                    PetFormScreen(initial = if (key.editing) dog else null, busy = busy, onBack = { back() },
                                        onSubmit = { draft ->
                                            busy = true
                                            runtime.saveDog(draft.name, draft.gender, draft.age, draft.breed, draft.memo) { saved ->
                                                busy = false
                                                if (saved) {
                                                    toast.show(if (key.editing) "반려견 정보가 업데이트되었습니다." else "반려견 정보가 추가되었습니다.")
                                                    back()
                                                } else toast.show(if (key.editing) "수정사항 업데이트에 실패했습니다" else "반려견 정보 업로드에 실패했습니다.")
                                            }
                                        },
                                        onDelete = if (key.editing) ({
                                            runtime.deleteDog { deleted ->
                                                if (deleted) { toast.show("삭제되었습니다."); back() } else toast.show("반려견 삭제가 실패했습니다")
                                            }
                                        }) else null)
                                }
                                entry<Alarm> { AlarmScreen(onBack = { back() }) }
                            })
                        ToastHost(toast, Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp))
                    }
                    if (tab != null) LegacyBottomBar(tab, onSelect = { selected ->
                        if (selected == tab) return@LegacyBottomBar
                        while (backStack.size > 1) back()
                        when (selected) {
                            LegacyTab.Home -> Unit
                            LegacyTab.Walk -> { requestLocationPermission(); backStack.add(Walk) }
                            LegacyTab.MyPage -> backStack.add(MyPage)
                        }
                    })
                }
            }
        }
    }
}

private fun routeFlow(runtime: AppRuntime, walkId: String) = runtime.controller.points(walkId)
    .map { points -> points.map { RoutePoint(it.sample.latitude, it.sample.longitude, it.segment) } }

@Composable
private fun FinishRoute(walkId: String, runtime: AppRuntime, awards: List<StampAward>, zone: TimeZone, onDone: () -> Unit) {
    val loaded by produceState<Pair<Boolean, WalkSession?>>(false to null, walkId) {
        runtime.controller.record(walkId).collect { value = true to it }
    }
    val route by produceState(emptyList<RoutePoint>(), walkId) { routeFlow(runtime, walkId).collect { value = it } }
    FinishContent(loaded.second, route, awards.earnedBy(walkId), when {
        !loaded.first -> ScreenStatus.Loading
        loaded.second == null -> ScreenStatus.Empty
        else -> ScreenStatus.Ready
    }, zone, onDone)
}
