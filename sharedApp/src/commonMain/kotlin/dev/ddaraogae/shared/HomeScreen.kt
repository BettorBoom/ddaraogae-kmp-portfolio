package dev.ddaraogae.shared

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.ddaraogae.designsystem.AppIcons
import dev.ddaraogae.designsystem.AppText
import dev.ddaraogae.designsystem.AppTheme
import dev.ddaraogae.designsystem.ChartPoint
import dev.ddaraogae.designsystem.DogCircle
import dev.ddaraogae.designsystem.EmptyDataChip
import dev.ddaraogae.designsystem.Hint
import dev.ddaraogae.designsystem.LocalToast
import dev.ddaraogae.designsystem.ScreenStatus
import dev.ddaraogae.designsystem.StampProgressBar
import dev.ddaraogae.designsystem.StatusNotice
import dev.ddaraogae.designsystem.WalkLineChart
import dev.ddaraogae.designsystem.WhiteCard
import dev.ddaraogae.domain.DogProfile
import dev.ddaraogae.domain.STAMPS_PER_WEEK
import dev.ddaraogae.domain.WalkSession
import dev.ddaraogae.domain.finished
import dev.ddaraogae.domain.formatMonthDay
import dev.ddaraogae.domain.lastSevenDays
import dev.ddaraogae.domain.sinceLastWalkLabel
import dev.ddaraogae.domain.stampAwards
import dev.ddaraogae.domain.startDate
import dev.ddaraogae.domain.thisWeekCount
import kotlinx.datetime.TimeZone

/**
 * The original home tab (fragment_home.xml): greeting, time since the last walk, the dog, the
 * seven-day graph, this week's stamp progress and the weather card. Everything except the
 * weather is computed from the local walk history; weather has no data source yet (ADR-013).
 */
@Composable
internal fun HomeContent(
    dog: DogProfile?,
    history: List<WalkSession>,
    nowMillis: Long,
    zone: TimeZone,
    status: ScreenStatus = ScreenStatus.Ready,
    onRetry: (() -> Unit)? = null,
    onAddDog: () -> Unit,
    onHistory: () -> Unit,
    onStamps: () -> Unit,
) {
    val colors = AppTheme.colors
    val toast = LocalToast.current
    val days = remember(history, nowMillis, zone) { lastSevenDays(history, nowMillis, zone) }
    val window = days.first().date..days.last().date
    val hasData = dog != null && history.finished().any { it.startDate(zone) in window }
    val points = days.map { ChartPoint(formatMonthDay(it.date), (it.meters / 1000.0).toFloat()) }
    val stampsThisWeek = remember(history, nowMillis, zone) { stampAwards(history, zone).thisWeekCount(nowMillis, zone) }
    Column(Modifier.fillMaxSize().background(colors.bgGrey).verticalScroll(rememberScrollState())) {
        Text("반가워요!", style = AppText.headline, color = colors.text,
            modifier = Modifier.padding(start = 20.dp, top = 50.dp, end = 20.dp).testTag("screen-title").semantics { heading() })
        Text(if (dog == null) "최근에 산책한 기록이 없어요!" else sinceLastWalkLabel(history, nowMillis, zone),
            style = AppText.textBold, color = colors.text, modifier = Modifier.padding(start = 20.dp, top = 22.dp, end = 20.dp)
                .testTag("since-last-walk"))
        StatusNotice(status, "아직 함께한 산책이 없어요. 작은 걸음부터 시작해 볼까요?", onRetry, Modifier.padding(top = 12.dp))
        // rv_dogArea / iv_dogAdd: a 100dp circle per dog, or the paw tile with a plus badge when none exists.
        Box(Modifier.padding(start = 20.dp, top = 20.dp)) {
            if (dog == null) {
                Box(Modifier.size(100.dp).clickable(role = Role.Button, onClickLabel = "반려견 추가", onClick = onAddDog)
                    .semantics { contentDescription = "반려견 추가" }.testTag("home-dog-add")) {
                    DogCircle(size = 100.dp, inset = 16.dp)
                    Icon(AppIcons.DogAdd, contentDescription = null, tint = colors.lightBrown,
                        modifier = Modifier.align(Alignment.BottomEnd).offset(x = (-12).dp, y = (-16).dp).size(24.dp))
                }
            } else {
                Column(Modifier.width(100.dp).testTag("home-dog").semantics { contentDescription = "반려견 ${dog.name}" },
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    DogCircle(size = 100.dp, ring = colors.banana, ringWidth = 4.dp, inset = 4.dp)
                    // item_home_dog_selection ellipsizes the name on one line by design.
                    Text(dog.name, style = AppText.caption, color = colors.text, maxLines = 1,
                        overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().testTag("ellipsized-by-design"))
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 42.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(if (dog == null) "금주 산책 이력" else "${dog.name}의 산책 그래프", style = AppText.textBold, color = colors.text,
                modifier = Modifier.weight(1f).padding(start = 20.dp))
            Icon(AppIcons.Information, contentDescription = "선택한 반려견의 최근 1주일 산책 그래프입니다.", tint = colors.darkGrey,
                modifier = Modifier.clickable(role = Role.Button) { toast.show("선택한 반려견의 최근 1주일 산책 그래프입니다.") }
                    .padding(16.dp).size(24.dp))
            Hint("상세 보기", Modifier.clickable(role = Role.Button, onClick = {
                if (dog == null) toast.show("반려견을 선택해 주세요.") else onHistory()
            }).padding(start = 4.dp, top = 12.dp, end = 20.dp, bottom = 12.dp).testTag("primary-action"))
        }
        WhiteCard(Modifier.padding(start = 20.dp, top = 8.dp, end = 20.dp).fillMaxWidth()) {
            Box(Modifier.padding(8.dp).fillMaxWidth().height(160.dp)) {
                WalkLineChart(points, hasData, Modifier.fillMaxSize(), description = "최근 7일 산책 거리 그래프")
                if (!hasData) EmptyDataChip("최근 산책 데이터가 없어요.", Modifier.align(Alignment.Center))
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 36.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("금주 산책 도장 달성률", style = AppText.textBold, color = colors.text, modifier = Modifier.weight(1f).padding(start = 20.dp))
            Hint("전체 보기", Modifier.clickable(role = Role.Button, onClick = onStamps)
                .padding(start = 12.dp, top = 12.dp, end = 20.dp, bottom = 12.dp).testTag("home-stamps"))
        }
        WhiteCard(Modifier.padding(start = 20.dp, top = 8.dp, end = 20.dp).fillMaxWidth()) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
                Text("${STAMPS_PER_WEEK}개 중 ${stampsThisWeek}개 획득", style = AppText.captionBold, color = colors.text,
                    modifier = Modifier.testTag("stamp-rate"))
                StampProgressBar(stampsThisWeek, STAMPS_PER_WEEK, Modifier.padding(top = 8.dp))
            }
        }
        Text("오늘의 날씨", style = AppText.textBold, color = colors.text,
            modifier = Modifier.padding(start = 20.dp, top = 36.dp, end = 20.dp))
        WhiteCard(Modifier.padding(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 20.dp).fillMaxWidth()) {
            Box(Modifier.fillMaxWidth().height(90.dp), contentAlignment = Alignment.Center) {
                // The original shows this chip when weather cannot be loaded; here it is never loaded.
                EmptyDataChip("날씨 정보는 아직 연결되지 않았어요.", elevation = 8.dp)
            }
        }
    }
}
