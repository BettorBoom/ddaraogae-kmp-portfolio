package dev.ddaraogae.shared

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.*
import androidx.navigation3.ui.NavDisplay
import dev.ddaraogae.designsystem.DdaraogaeTheme
import dev.ddaraogae.designsystem.*
import dev.ddaraogae.domain.WalkSession
import dev.ddaraogae.domain.WalkStatus
import dev.ddaraogae.walk.WalkFeature
import dev.ddaraogae.dogs.DogEditor
import dev.ddaraogae.history.HistoryList
import dev.ddaraogae.history.RecordContent
import dev.ddaraogae.history.WalkDistanceGraph

@Composable
fun DdaraogaeApp(runtime: AppRuntime, requestLocationPermission: () -> Unit = {}) {
    DdaraogaeTheme {
        Surface(Modifier.fillMaxSize()) {
            Column {
                if (runtime.demoData) Text("개발용 합성 경로 · 실제 GPS가 아니에요.",
                    color = MaterialTheme.colorScheme.error)
                val backStack = rememberNavBackStack(routeConfiguration, Home)
                fun back() { if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) }
                fun toHistory() {
                    while (backStack.size > 1 && backStack.last() != History) back()
                    if (backStack.last() != History) backStack.add(History)
                }
                NavDisplay(backStack = backStack, onBack = { back() }, modifier = Modifier.weight(1f),
                entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator()),
                entryProvider = entryProvider {
                    entry<Home> {
                        val dog by runtime.controller.dog.collectAsState()
                        val records by runtime.controller.history.collectAsState()
                        HomeContent(dogName = dog?.name,
                            records = records,
                            onDog = { backStack.add(Dog) }, onHistory = { backStack.add(History) },
                            onWalk = { requestLocationPermission(); backStack.add(Walk) })
                    }
                    entry<Dog> {
                        val dog by runtime.controller.dog.collectAsState()
                        var busy by remember { mutableStateOf(false) }
                        var error by remember { mutableStateOf<String?>(null) }
                        DogEditor(dog?.name, busy, error, onSave = { name ->
                            busy = true; error = null
                            runtime.saveDog(name) { saved ->
                                busy = false
                                if (saved) back() else error = "이름을 저장하지 못했어요. 다시 시도해 주세요."
                            }
                        }, onBack = { back() })
                    }
                    entry<Walk> {
                        val locationNotice by runtime.locationNotice.collectAsState()
                        val writeFailed by runtime.writeFailed.collectAsState()
                        WalkFeature(runtime.userScope, onBack = { back() },
                            onRecord = { backStack.add(Record(it)) },
                            locationNotice = if (writeFailed) "기록 저장에 문제가 있어요. 안전한 곳에서 산책을 마치고 다시 확인해 주세요." else locationNotice)
                    }
                    entry<History> {
                        val records by runtime.controller.history.collectAsState()
                        HistoryList(records, onBack = { back() }, onRecord = { backStack.add(Record(it)) })
                    }
                    entry<Record> { key ->
                        RecordScreen(key.walkId, runtime, onBack = { toHistory() })
                    }
                })
                if (backStack.last() == Home || backStack.last() == Walk || backStack.last() == Dog) {
                    Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).heightIn(min = 64.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { while (backStack.size > 1) back() }) { Text("홈") }
                        TextButton(onClick = {
                            while (backStack.size > 1) back()
                            requestLocationPermission(); backStack.add(Walk)
                        }) { Text("산책") }
                        TextButton(onClick = {
                            while (backStack.size > 1) back()
                            backStack.add(Dog)
                        }) { Text("반려견") }
                    }
                }
            }
        }
    }
}

@Composable
internal fun HomeContent(status: ScreenStatus = ScreenStatus.Ready, longText: Boolean = false,
                         onRetry: (() -> Unit)? = null, dogName: String? = "예시 반려견",
                         records: List<WalkSession> = emptyList(), onDog: () -> Unit = {},
                         onHistory: () -> Unit = {}, onWalk: () -> Unit) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).safeDrawingPadding()
        .verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text("반가워요!", style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.testTag("screen-title").semantics { heading() })
        Text(if (records.isEmpty()) "오늘의 산책을 시작해 볼까요?" else "함께한 산책 ${records.size}회가 쌓였어요.",
            style = MaterialTheme.typography.bodyMedium)
        StateNotice(status, "아직 함께한 산책이 없어요. 작은 걸음부터 시작해 볼까요?", onRetry)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            DogAvatar(Modifier.size(76.dp))
            Column {
                Text(dogName ?: "함께할 친구", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = onDog) {
                    Text(if (dogName == null) "반려견 이름 등록하기" else "반려견 이름 바꾸기")
                }
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            Text("최근 산책 그래프", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = onHistory) { Text("전체 보기") }
        }
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
            Box(Modifier.fillMaxWidth().height(160.dp).padding(16.dp), contentAlignment = Alignment.Center) {
                WalkDistanceGraph(records)
                if (records.isEmpty()) Text("최근 산책 데이터가 없어요.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text("금주 산책 도장 달성률", style = MaterialTheme.typography.titleMedium)
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("도장 집계 연결 전", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Box(Modifier.fillMaxWidth().height(8.dp)
                    .background(Color(0xFFE2E2E2)))
            }
        }
        Text("오늘의 날씨", style = MaterialTheme.typography.titleMedium)
        InfoCard("날씨 정보", "날씨 서비스 연결 전 · 실시간 정보 없음")
        PrimaryAction("산책하러 가기", enabled = dogName != null, onClick = onWalk)
        if (longText) InfoCard("작은 걸음부터", "익숙한 골목을 천천히 걸어도 좋아요. 반려견의 속도에 맞춰 쉬어 가며 오래 기억할 오늘의 추억을 함께 남겨 보세요. 아주 긴 안내도 글씨 크기와 화면 너비에 맞춰 이어서 읽을 수 있어야 해요.")
        Text("기록은 이 기기에 저장돼요.", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}


@Composable
private fun RecordScreen(walkId: String, runtime: AppRuntime, onBack: () -> Unit) {
    val loadedRecord by produceState<Pair<Boolean, WalkSession?>>(false to null, walkId) {
        runtime.controller.record(walkId).collect { value = true to it }
    }
    val points by runtime.controller.points(walkId).collectAsState(initial = emptyList())
    RecordContent(loadedRecord.second, when {
        !loadedRecord.first -> ScreenStatus.Loading
        loadedRecord.second == null -> ScreenStatus.Empty
        else -> ScreenStatus.Ready
    },
        onBack = onBack, points = points)
}
