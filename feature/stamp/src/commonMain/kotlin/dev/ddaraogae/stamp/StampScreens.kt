package dev.ddaraogae.stamp

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.ddaraogae.designsystem.AppBar
import dev.ddaraogae.designsystem.AppIcons
import dev.ddaraogae.designsystem.AppText
import dev.ddaraogae.designsystem.AppTheme
import dev.ddaraogae.designsystem.EmptyDataChip
import dev.ddaraogae.designsystem.StampPaw
import dev.ddaraogae.designsystem.WhiteCard
import dev.ddaraogae.domain.StampAward
import dev.ddaraogae.domain.StampType
import dev.ddaraogae.domain.formatStamp
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/** fragment_all_stamp: the eight stamps in two columns; each card opens its detail. */
@Composable
fun StampListScreen(onBack: () -> Unit, onOpen: (StampType) -> Unit) {
    val colors = AppTheme.colors
    Column(Modifier.fillMaxSize().background(colors.white)) {
        AppBar("모든 산책 도장 목록", onBack = onBack)
        LazyVerticalGrid(GridCells.Fixed(2), Modifier.fillMaxSize().padding(start = 20.dp, top = 8.dp, end = 20.dp)) {
            items(StampType.entries) { type ->
                WhiteCard(Modifier.padding(16.dp)) {
                    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(AppIcons.Stamp, contentDescription = null,
                            modifier = Modifier.padding(horizontal = 36.dp, vertical = 8.dp).size(75.dp, 70.dp).clip(CircleShape))
                        Text(type.title, style = AppText.textBold, color = colors.text, textAlign = TextAlign.Center)
                        Text("내가 얻은 도장 보러가기", style = AppText.mini, color = colors.lightBrown,
                            modifier = Modifier.padding(top = 8.dp))
                        Box(Modifier.padding(8.dp).fillMaxWidth().height(30.dp)
                            .background(colors.white, RoundedCornerShape(8.dp))
                            .border(BorderStroke(0.5.dp, colors.verySoftOrange), RoundedCornerShape(8.dp))
                            .clip(RoundedCornerShape(8.dp)).clickable(role = Role.Button) { onOpen(type) }
                            .testTag("stamp-open-${type.number}"), contentAlignment = Alignment.Center) {
                            Text("CLICK", style = AppText.miniBold, color = colors.text)
                        }
                    }
                }
            }
        }
    }
}

/** fragment_stamp_detail: the stamp, its rule and one cell per time it was earned. */
@Composable
fun StampDetailScreen(type: StampType, awards: List<StampAward>, zone: TimeZone, onBack: () -> Unit) {
    val colors = AppTheme.colors
    val earned = awards.filter { it.type == type }.sortedBy { it.awardedAtMillis }
    Column(Modifier.fillMaxSize().background(colors.white)) {
        AppBar("산책 도장 정보", onBack = onBack)
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Image(AppIcons.Stamp, contentDescription = type.title, modifier = Modifier.size(100.dp).clip(CircleShape))
            Text(type.title, style = AppText.bodyBold, color = colors.text, modifier = Modifier.padding(top = 16.dp))
            Text(type.description, style = AppText.caption, color = colors.text, textAlign = TextAlign.Center,
                modifier = Modifier.padding(start = 20.dp, top = 4.dp, end = 20.dp))
        }
        Box(Modifier.weight(1f).padding(start = 20.dp, top = 36.dp, end = 20.dp).fillMaxWidth()
            .background(colors.white, RoundedCornerShape(8.dp)).border(1.dp, colors.brown, RoundedCornerShape(8.dp))) {
            if (earned.isEmpty()) {
                EmptyDataChip("획득한 산책 도장이 없습니다.", Modifier.align(Alignment.Center).testTag("stamp-empty"),
                    textStyle = AppText.caption, elevation = 8.dp)
            } else {
                LazyVerticalGrid(GridCells.Fixed(3), Modifier.fillMaxSize()) {
                    items(earned) { award ->
                        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            StampPaw(Modifier.size(80.dp))
                            Text(formatStamp(Instant.fromEpochMilliseconds(award.awardedAtMillis).toLocalDateTime(zone)),
                                style = AppText.mini, color = colors.text, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
    }
}
