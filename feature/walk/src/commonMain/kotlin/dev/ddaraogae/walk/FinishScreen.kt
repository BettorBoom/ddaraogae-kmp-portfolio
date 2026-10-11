package dev.ddaraogae.walk

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.ddaraogae.designsystem.AppIcons
import dev.ddaraogae.designsystem.AppText
import dev.ddaraogae.designsystem.AppTheme
import dev.ddaraogae.designsystem.BrownButton
import dev.ddaraogae.designsystem.DogCircle
import dev.ddaraogae.designsystem.RouteMap
import dev.ddaraogae.designsystem.RoutePoint
import dev.ddaraogae.designsystem.ScreenStatus
import dev.ddaraogae.designsystem.StatusNotice
import dev.ddaraogae.designsystem.spToDp
import dev.ddaraogae.domain.StampType
import dev.ddaraogae.domain.WalkSession
import dev.ddaraogae.domain.formatDistance
import dev.ddaraogae.domain.formatWalkTimer
import dev.ddaraogae.domain.formatKoreanDate
import dev.ddaraogae.domain.localDateOf
import kotlinx.datetime.TimeZone

/**
 * The original FinishActivity ("산책 완료"): title, route map, time and distance, the dogs that
 * walked, the date and "산책 끝!". Stamps earned by this walk open the stamp dialog first.
 * The walk is already saved, so unlike the original there is no "not saved" exit warning.
 */
@Composable
fun FinishContent(
    walk: WalkSession?,
    route: List<RoutePoint>,
    stamps: List<StampType>,
    status: ScreenStatus,
    zone: TimeZone,
    onDone: () -> Unit,
    onRetry: (() -> Unit)? = null,
) {
    val colors = AppTheme.colors
    var stampsShown by rememberSaveable(walk?.id) { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(colors.white)) {
        Text("산책 완료", style = AppText.bodyBold, color = colors.text, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp).testTag("screen-title").semantics { heading() })
        Box(Modifier.padding(top = 16.dp).fillMaxWidth().height(1.dp).background(colors.grey))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            StatusNotice(status, "기록을 찾지 못했어요. 산책 화면에서 저장 상태를 확인해 주세요.", onRetry,
                Modifier.padding(top = 20.dp))
            if (walk != null && status == ScreenStatus.Ready) {
                Box(Modifier.padding(start = 20.dp, top = 20.dp, end = 20.dp).fillMaxWidth().height(230.spToDp())
                    .clip(RoundedCornerShape(15.dp))) {
                    RouteMap(route, Modifier.fillMaxSize())
                }
                if (route.size < 2) Text("저장된 위치가 없어요. 앱이 열린 동안 위치 권한을 허용하면 경로를 기록해요.",
                    style = AppText.mini, color = colors.darkGrey, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(start = 20.dp, top = 8.dp, end = 20.dp).testTag("no-route"))
                Row(Modifier.fillMaxWidth().padding(top = 30.dp)) {
                    Stat("산책 시간", formatWalkTimer(walk.activeDurationMillis / 1000),
                        Modifier.weight(1f).padding(end = 36.dp), Alignment.End)
                    Stat("산책 거리", if (route.isEmpty()) "위치 기록 없음" else formatDistance(walk.distanceMeters),
                        Modifier.weight(1f).padding(start = 36.dp), Alignment.Start)
                }
                Text("오늘 산책을 함께한 반려견", style = AppText.text, color = colors.text, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 30.dp))
                Row(Modifier.fillMaxWidth().padding(top = 20.dp), horizontalArrangement = Arrangement.Center) {
                    Column(Modifier.padding(horizontal = 10.dp).width(64.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        DogCircle(size = 48.dp, ring = colors.lightGrey, ringWidth = 1.dp)
                        Text(walk.dogName ?: "이름 없음", style = AppText.miniBold, color = colors.blackFix,
                            textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
                    }
                }
                val ended = walk.endedAtMillis ?: walk.startedAtMillis
                Text(formatKoreanDate(localDateOf(ended, zone)), style = AppText.caption, color = colors.text,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 20.dp))
            }
            Spacer(Modifier.height(20.dp))
            BrownButton("산책 끝!", onClick = { if (stamps.isEmpty() || stampsShown) onDone() else stampsShown = true },
                modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 20.dp).fillMaxWidth()
                    .height(55.spToDp()).testTag("primary-action"),
                shape = RoundedCornerShape(10.dp), textStyle = AppText.bodyBold)
        }
    }
    if (stampsShown && stamps.isNotEmpty()) StampDialog(stamps, onDone)
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier, align: Alignment.Horizontal) {
    Column(modifier, horizontalAlignment = align) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = AppText.caption, color = AppTheme.colors.brownTrans)
            Text(value, style = AppText.textBold, color = AppTheme.colors.text, modifier = Modifier.padding(top = 10.dp))
        }
    }
}

/** fragment_stamp_dialog_: "오늘 모은 산책 도장", swipeable stamps with dots and "확인했어요!". */
@Composable
private fun StampDialog(stamps: List<StampType>, onDone: () -> Unit) {
    val colors = AppTheme.colors
    val pager = rememberPagerState(pageCount = { stamps.size })
    // Width is 80% of the screen as in the original, so the platform default width is turned off.
    Dialog(onDismissRequest = {}, properties = DialogProperties(dismissOnBackPress = false,
        dismissOnClickOutside = false, usePlatformDefaultWidth = false)) {
      Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Surface(Modifier.fillMaxWidth(0.8f).testTag("stamp-dialog"), shape = RoundedCornerShape(16.dp), color = colors.white) {
            Column(Modifier.padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("오늘 모은 산책 도장", style = AppText.textBold, color = colors.text, textAlign = TextAlign.Center)
                HorizontalPager(pager, Modifier.padding(top = 8.dp).fillMaxWidth()) { page ->
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(AppIcons.Stamp, contentDescription = stamps[page].title, modifier = Modifier.size(120.spToDp()))
                        Text(stamps[page].title, style = AppText.caption, color = colors.text, textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp))
                    }
                }
                Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(stamps.size) { index ->
                        Box(Modifier.size(8.dp).clip(CircleShape)
                            .background(if (index == pager.currentPage) colors.banana else colors.darkGrey))
                    }
                }
                BrownButton("확인했어요!", onDone, Modifier.padding(top = 16.dp).fillMaxWidth().testTag("stamp-done"),
                    shape = RoundedCornerShape(8.dp), textStyle = AppText.textBold)
            }
        }
      }
    }
}
