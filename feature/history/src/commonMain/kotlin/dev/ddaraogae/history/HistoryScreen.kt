package dev.ddaraogae.history

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.ddaraogae.designsystem.AppBar
import dev.ddaraogae.designsystem.AppIcons
import dev.ddaraogae.designsystem.AppText
import dev.ddaraogae.designsystem.AppTheme
import dev.ddaraogae.designsystem.ChartPoint
import dev.ddaraogae.designsystem.EmptyDataChip
import dev.ddaraogae.designsystem.LocalToast
import dev.ddaraogae.designsystem.RouteMap
import dev.ddaraogae.designsystem.RoutePoint
import dev.ddaraogae.designsystem.WalkLineChart
import dev.ddaraogae.designsystem.WhiteCard
import dev.ddaraogae.domain.DogProfile
import dev.ddaraogae.domain.WalkSession
import dev.ddaraogae.domain.formatDistance
import dev.ddaraogae.domain.formatHistoryDuration
import dev.ddaraogae.domain.formatMonthDay
import dev.ddaraogae.domain.formatStamp
import dev.ddaraogae.domain.localDateOf
import dev.ddaraogae.domain.monthDays
import dev.ddaraogae.domain.walksOfMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

private const val PAGE = 5

/**
 * The original "산책 모아보기" (activity_history.xml): a month picker, the month's distance chart and
 * the walk cards, five at a time. There is no dog or period filter in the original either;
 * the month is the only choice. A card's thumbnail opens the route popup.
 */
@Composable
fun HistoryScreen(
    dog: DogProfile?,
    history: List<WalkSession>,
    nowMillis: Long,
    zone: TimeZone,
    routeOf: (WalkSession) -> Flow<List<RoutePoint>>,
    onBack: () -> Unit,
) {
    val colors = AppTheme.colors
    val toast = LocalToast.current
    val today = localDateOf(nowMillis, zone)
    var year by rememberSaveable { mutableIntStateOf(today.year) }
    var month by rememberSaveable { mutableIntStateOf(today.monthNumber) }
    var picking by rememberSaveable { mutableStateOf(false) }
    var shown by remember(year, month) { mutableIntStateOf(PAGE) }
    var popup by remember { mutableStateOf<WalkSession?>(null) }
    val walks = remember(history, year, month, zone) { walksOfMonth(history, year, month, zone) }
    val days = remember(history, year, month, zone) { monthDays(history, year, month, zone) }
    val points = days.map { ChartPoint(formatMonthDay(it.date), (it.meters / 1000.0).toFloat(), it.date == today) }

    Column(Modifier.fillMaxSize().background(colors.white)) {
        AppBar("산책 모아보기", onBack = onBack, height = 56.dp, dividerHeight = 0.dp, backSize = 20.dp)
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                Column(Modifier.background(colors.white)) {
                    Row(Modifier.clickable(role = Role.Button, onClickLabel = "날짜 선택") { picking = true }
                        .testTag("primary-action"), verticalAlignment = Alignment.CenterVertically) {
                        Text("${year}년 ${month}월", style = AppText.caption, color = colors.text,
                            modifier = Modifier.padding(20.dp).testTag("history-month"))
                        Icon(AppIcons.CalendarSelect, contentDescription = null, tint = colors.black,
                            modifier = Modifier.rotate(90f).size(24.dp))
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(if (dog == null) "산책 그래프" else "${dog.name}의 산책 그래프", style = AppText.textBold,
                            color = colors.text, modifier = Modifier.weight(1f).padding(20.dp))
                        Icon(AppIcons.Information, contentDescription = "선택한 반려견의 최근 한 달간의 산책 그래프입니다.",
                            tint = colors.darkGrey, modifier = Modifier.padding(end = 20.dp).size(24.dp)
                                .clickable(role = Role.Button) { toast.show("선택한 반려견의 최근 한 달간의 산책 그래프입니다.") })
                    }
                    Box(Modifier.padding(20.dp).fillMaxWidth().height(200.dp)) {
                        WalkLineChart(points, walks.isNotEmpty(), Modifier.fillMaxSize(), scrollable = true,
                            description = "${month}월 산책 거리 그래프")
                        if (walks.isEmpty()) EmptyDataChip("최근 산책 데이터가 없어요.", Modifier.align(Alignment.Center),
                            elevation = 20.dp)
                    }
                }
            }
            item {
                Box(Modifier.fillMaxWidth().background(colors.bgGrey)) {
                    Text("산책기록", style = AppText.textBold, color = colors.text, modifier = Modifier.padding(20.dp))
                }
            }
            if (walks.isEmpty()) {
                item {
                    Box(Modifier.heightIn(min = 160.dp).fillMaxWidth().background(colors.bgGrey).padding(top = 20.dp),
                        contentAlignment = Alignment.TopCenter) {
                        EmptyDataChip("이번달 산책 기록이 없습니다.", textStyle = AppText.text, elevation = 8.dp)
                    }
                }
            } else {
                itemsIndexed(walks.take(shown)) { index, walk ->
                    Box(Modifier.fillMaxWidth().background(colors.bgGrey)) {
                        WalkCard(walk, zone, routeOf(walk)) { popup = walk }
                    }
                    if (index == shown - 1 && shown < walks.size) LaunchedEffect(shown) { shown += PAGE }
                }
                item { Box(Modifier.fillMaxWidth().height(16.dp).background(colors.bgGrey)) }
            }
        }
    }
    if (picking) CalendarDialog(today.year, { picking = false }) { pickedYear, pickedMonth ->
        year = pickedYear; month = pickedMonth; picking = false
    }
    popup?.let { walk -> WalkMapDialog(walk, dog, routeOf(walk), { popup = null }) }
}

/** item_home_history_walk: date, distance, duration and the 80dp route thumbnail. */
@Composable
private fun WalkCard(walk: WalkSession, zone: TimeZone, route: Flow<List<RoutePoint>>, onMap: () -> Unit) {
    val colors = AppTheme.colors
    val points by route.collectAsState(emptyList())
    val started = Instant.fromEpochMilliseconds(walk.startedAtMillis).toLocalDateTime(zone)
    WhiteCard(Modifier.padding(16.dp).fillMaxWidth()) {
        Row(Modifier.padding(16.dp)) {
            Column(Modifier.weight(1f)) {
                Text(formatStamp(started), style = AppText.mini, color = colors.darkGrey)
                Row(Modifier.padding(start = 16.dp, top = 8.dp)) {
                    Metric(if (points.isEmpty()) "0 m" else formatDistance(walk.distanceMeters), "산책 거리")
                    Metric(formatHistoryDuration(walk.activeDurationMillis / 1000), "산책 시간", Modifier.padding(start = 36.dp))
                }
            }
            Box(Modifier.width(80.dp).height(72.dp).clickable(role = Role.Button, onClickLabel = "경로 보기", onClick = onMap)
                .testTag("history-map")) { RouteMap(points, Modifier.fillMaxSize(), padding = 12.dp, caption = false) }
        }
    }
}

@Composable
private fun Metric(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = AppText.bodyBold, color = AppTheme.colors.text)
        Text(label, style = AppText.mini, color = AppTheme.colors.darkGrey)
    }
}

/** dialog_calendar: year with arrows, then a 3x4 month grid; no selection highlight, as in the original. */
@Composable
private fun CalendarDialog(initialYear: Int, onDismiss: () -> Unit, onPick: (Int, Int) -> Unit) {
    val colors = AppTheme.colors
    var year by remember { mutableIntStateOf(initialYear) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(color = colors.white, shape = RoundedCornerShape(4.dp)) {
            Column(Modifier.padding(16.dp).testTag("calendar-dialog")) {
                Box(Modifier.fillMaxWidth()) {
                    Icon(AppIcons.Left, contentDescription = "이전 해", tint = colors.black,
                        modifier = Modifier.align(Alignment.CenterStart).size(24.dp).clickable(role = Role.Button) { year -= 1 })
                    Text("$year", style = AppText.text, color = colors.text, textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.Center))
                    Icon(AppIcons.Right, contentDescription = "다음 해", tint = colors.black,
                        modifier = Modifier.align(Alignment.CenterEnd).size(24.dp).clickable(role = Role.Button) { year += 1 })
                }
                (1..12).chunked(3).forEachIndexed { row, months ->
                    Row(Modifier.fillMaxWidth().padding(top = if (row == 0) 16.dp else 0.dp), horizontalArrangement = Arrangement.Center) {
                        months.forEach { month ->
                            val shape = RoundedCornerShape(8.dp)
                            Box(Modifier.padding(4.dp).width(80.dp).heightIn(min = 48.dp).background(colors.white, shape)
                                .border(1.dp, colors.black, shape).clip(shape)
                                .clickable(role = Role.Button) { onPick(year, month) }.testTag("month-$month"),
                                contentAlignment = Alignment.Center) {
                                Text("${month}월", style = AppText.caption, color = colors.text)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** dialog_walk_history_map: the route over the map with duration, distance and dog name, and a share icon. */
@Composable
private fun WalkMapDialog(walk: WalkSession, dog: DogProfile?, route: Flow<List<RoutePoint>>, onDismiss: () -> Unit) {
    val colors = AppTheme.colors
    val toast = LocalToast.current
    val points by route.collectAsState(emptyList())
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxWidth(0.92f).widthIn(max = 400.dp).height(250.dp).testTag("map-dialog")) {
            RouteMap(points, Modifier.fillMaxSize(), padding = 36.dp)
            Column(Modifier.align(Alignment.TopEnd).padding(top = 12.dp, end = 12.dp), horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(formatHistoryDuration(walk.activeDurationMillis / 1000),
                    if (points.isEmpty()) "0 m" else formatDistance(walk.distanceMeters), walk.dogName ?: dog?.name ?: "-").forEach { text ->
                    Text(text, style = AppText.captionBold, color = Color.White,
                        modifier = Modifier.background(colors.lightBrown).padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }
            Box(Modifier.align(Alignment.BottomEnd).padding(end = 24.dp, bottom = 16.dp).size(48.dp)
                .clickable(role = Role.Button, onClickLabel = "공유하기") { toast.show("공유하기는 아직 연결되지 않았어요.") }
                .testTag("history-share"), contentAlignment = Alignment.Center) {
                Image(AppIcons.Share, contentDescription = "공유하기", modifier = Modifier.size(30.dp))
            }
        }
    }
}
