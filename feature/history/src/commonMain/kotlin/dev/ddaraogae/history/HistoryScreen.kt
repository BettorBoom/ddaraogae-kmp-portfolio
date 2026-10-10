package dev.ddaraogae.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.ddaraogae.designsystem.*
import dev.ddaraogae.domain.*

fun durationLabel(millis: Long): String = "${millis / 60_000}분 ${(millis / 1000) % 60}초"
fun distanceLabel(meters: Double): String = "${meters.toInt()}m"

@Composable
fun HistoryList(records: List<WalkSession>, onBack: () -> Unit, onRecord: (String) -> Unit) {
    Page("산책 모아보기", "홈으로", onBack) {
        Text("최근 산책 그래프", style = MaterialTheme.typography.titleMedium)
        InfoCard("함께 걸은 거리", if (records.isEmpty()) "최근 산책 데이터가 없어요." else "최근 ${records.take(7).size}회 산책") {
            if (records.isNotEmpty()) WalkDistanceGraph(records)
        }
        Text("산책기록", style = MaterialTheme.typography.titleMedium)
        if (records.isEmpty()) {
            InfoCard("아직 기록이 없어요", "첫 산책을 마치면 여기에 차곡차곡 쌓여요.")
        } else records.forEachIndexed { index, record ->
            InfoCard("${index + 1}번째 산책 · ${record.dogName ?: "이름 없는 반려견"}",
                "산책 거리 ${distanceLabel(record.distanceMeters)}   산책 시간 ${durationLabel(record.activeDurationMillis)}") {
                OutlinedButton(onClick = { onRecord(record.id) }, modifier = Modifier.fillMaxWidth()) {
                    Text("${index + 1}번째 산책 자세히 보기")
                }
            }
        }
    }
}

@Composable
fun RecordContent(record: WalkSession?, status: ScreenStatus, longText: Boolean = false,
                  onRetry: (() -> Unit)? = null, onBack: () -> Unit, points: List<WalkPoint> = emptyList()) {
    Page(if (record?.status == WalkStatus.Completed) "산책 완료" else "산책 기록", "목록으로", onBack) {
        StateNotice(status, "기록을 찾지 못했어요. 산책 화면에서 저장 상태를 확인해 주세요.", onRetry)
        if (record != null && status == ScreenStatus.Ready) {
            if (points.isEmpty()) MapPlaceholder() else RouteSketch(points)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Column {
                    Text("산책 시간", color = MaterialTheme.colorScheme.primary)
                    Text(durationLabel(record.activeDurationMillis), style = MaterialTheme.typography.titleLarge)
                }
                Column {
                    Text("산책 거리", color = MaterialTheme.colorScheme.primary)
                    Text(if (points.isEmpty()) "위치 기록 없음" else distanceLabel(record.distanceMeters),
                        style = MaterialTheme.typography.titleLarge)
                }
            }
            HorizontalDivider()
            Text("오늘 산책을 함께한 반려견", style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DogAvatar(Modifier.size(48.dp))
                Text(record.dogName ?: "반려견 정보가 없는 이전 기록", style = MaterialTheme.typography.bodyLarge)
            }
            Text(when (record.status) {
                WalkStatus.Recording -> "아직 함께 걷는 중이에요."
                WalkStatus.Completed -> "함께한 시간을 저장했어요."
                WalkStatus.Interrupted -> "중단된 산책이에요."
            }, style = MaterialTheme.typography.bodyMedium)
            if (longText) InfoCard("다음에도 함께 걸어요", "오래 걸은 날도, 잠깐 바람을 쐰 날도 모두 소중한 기록이에요. 긴 안내 문구와 큰 글씨를 사용해도 모든 내용을 읽고 아래 행동으로 이동할 수 있어야 해요.")
        }
        PrimaryAction("산책 목록으로", onClick = onBack)
    }
}

@Composable
fun RouteSketch(points: List<WalkPoint>) {
    if (points.isEmpty()) {
        MapPlaceholder()
        return
    }
    val route = MaterialTheme.colorScheme.primary
    InfoCard("함께 걸은 길", "샘플 지도 위 상대 이동선 · 실제 위치와 다름") {
        Box(Modifier.fillMaxWidth().height(230.dp).clip(RoundedCornerShape(10.dp))) {
            MapBackdrop(Modifier.fillMaxSize())
            Canvas(Modifier.fillMaxSize()
                .semantics { contentDescription = "기록된 위치 ${points.size}개를 표시한 경로 도식" }) {
            val minX = points.minOf { it.sample.longitude }
            val maxX = points.maxOf { it.sample.longitude }
            val minY = points.minOf { it.sample.latitude }
            val maxY = points.maxOf { it.sample.latitude }
            fun position(point: WalkPoint): Offset {
                val x = if (maxX == minX) 0.5f else ((point.sample.longitude - minX) / (maxX - minX)).toFloat()
                val y = if (maxY == minY) 0.5f else ((point.sample.latitude - minY) / (maxY - minY)).toFloat()
                return Offset(size.width * (0.1f + 0.8f * x), size.height * (0.9f - 0.8f * y))
            }
            points.zipWithNext().forEach { (a, b) ->
                if (a.segment == b.segment) drawLine(route, position(a), position(b), 5.dp.toPx(), StrokeCap.Round)
            }
            drawCircle(Color(0xFF238563), 6.dp.toPx(), position(points.first()))
            drawCircle(route, 6.dp.toPx(), position(points.last()))
            }
        }
    }
}

@Composable
fun WalkDistanceGraph(records: List<WalkSession>) {
    val distances = records.take(7).reversed().map { it.distanceMeters.toFloat() }
    val maximum = distances.maxOrNull()?.coerceAtLeast(1f) ?: 1f
    val ink = MaterialTheme.colorScheme.primary
    Canvas(Modifier.fillMaxWidth().height(140.dp)) {
        for (row in 0..3) {
            val y = size.height * row / 3f
            drawLine(Color(0xFFD9D9D9), Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
        }
        val anchors = distances.mapIndexed { index, value ->
            Offset(size.width * (index + .5f) / distances.size,
                size.height * (.87f - .7f * (value / maximum)))
        }
        anchors.zipWithNext().forEach { (a, b) -> drawLine(ink, a, b, 3.dp.toPx(), StrokeCap.Round) }
        anchors.forEach { drawCircle(ink, 4.dp.toPx(), it) }
    }
}
