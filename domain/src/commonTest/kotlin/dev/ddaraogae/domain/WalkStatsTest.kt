package dev.ddaraogae.domain

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals

internal val seoul = TimeZone.of("Asia/Seoul")

internal fun millis(date: String, hour: Int = 9, minute: Int = 0): Long =
    LocalDateTime(LocalDate.parse(date), LocalTime(hour, minute)).toInstant(seoul).toEpochMilliseconds()

internal fun walk(id: String, date: String, hour: Int = 9, meters: Double = 500.0, minutes: Long = 30,
                  status: WalkStatus = WalkStatus.Completed): WalkSession {
    val start = millis(date, hour)
    return WalkSession(id, "u", start, status,
        endedAtMillis = if (status == WalkStatus.Completed) start + minutes * 60_000 else null,
        distanceMeters = meters, activeDurationMillis = minutes * 60_000)
}

class WalkStatsTest {
    @Test fun weekRunsMondayThroughSunday() {
        assertEquals(LocalDate(2026, 10, 5)..LocalDate(2026, 10, 11), weekOf(LocalDate(2026, 10, 5)))
        assertEquals(LocalDate(2026, 10, 5)..LocalDate(2026, 10, 11), weekOf(LocalDate(2026, 10, 11)))
        assertEquals(LocalDate(2026, 10, 12)..LocalDate(2026, 10, 18), weekOf(LocalDate(2026, 10, 12)))
    }

    @Test fun dailyTotalsAreZeroFilledAndBucketedByLocalStartDate() {
        // 00:30 in Seoul is still the previous day in UTC; the bucket must follow the zone.
        val history = listOf(
            walk("a", "2026-10-05", hour = 0, meters = 1_200.0),
            walk("b", "2026-10-05", hour = 18, meters = 300.0),
            walk("c", "2026-10-07", meters = 800.0),
            walk("open", "2026-10-06", meters = 9_999.0, status = WalkStatus.Interrupted),
        )
        val days = dailyDistances(history, LocalDate(2026, 10, 4), LocalDate(2026, 10, 8), seoul)
        assertEquals(listOf(0.0, 1_500.0, 0.0, 800.0, 0.0), days.map { it.meters })
        assertEquals(LocalDate(2026, 10, 4), days.first().date)
    }

    @Test fun homeGraphIsTheSevenDaysEndingToday() {
        val days = lastSevenDays(emptyList(), millis("2026-10-10"), seoul)
        assertEquals(7, days.size)
        assertEquals(LocalDate(2026, 10, 4), days.first().date)
        assertEquals(LocalDate(2026, 10, 10), days.last().date)
    }

    @Test fun monthGraphCoversEveryDayIncludingLeapFebruary() {
        assertEquals(31, monthDays(emptyList(), 2026, 10, seoul).size)
        assertEquals(29, monthDays(emptyList(), 2028, 2, seoul).size)
        assertEquals(28, monthDays(emptyList(), 2026, 2, seoul).size)
    }

    @Test fun monthListIsNewestFirstAndSkipsUnfinishedWalks() {
        val history = listOf(
            walk("old", "2026-10-01"), walk("new", "2026-10-09"), walk("other", "2026-09-30"),
            walk("open", "2026-10-10", status = WalkStatus.Recording),
        )
        assertEquals(listOf("new", "old"), walksOfMonth(history, 2026, 10, seoul).map { it.id })
    }

    @Test fun timeSinceLastWalkFollowsOriginalThresholds() {
        val now = millis("2026-10-10", hour = 12)
        fun label(vararg walks: WalkSession) = sinceLastWalkLabel(walks.toList(), now, seoul)
        assertEquals("최근에 산책한 기록이 없어요!", label())
        // Ended 20 minutes ago.
        assertEquals("금방 산책을 다녀왔어요!", label(walk("a", "2026-10-10", hour = 11, minutes = 40)))
        // Ended 5 hours ago (started 07:00, 30 minutes long).
        assertEquals("5 시간 전", label(walk("b", "2026-10-10", hour = 6, minutes = 30)))
        // Ended 23 hours ago -> still hours; exactly 24 hours -> over a day.
        assertEquals("23 시간 전", label(walk("c", "2026-10-09", hour = 13, minutes = 0)))
        assertEquals("산책을 안한지 하루가 넘었어요!", label(walk("d", "2026-10-09", hour = 12, minutes = 0)))
        // Older than the seven-day window counts as no recent record.
        assertEquals("최근에 산책한 기록이 없어요!", label(walk("e", "2026-10-03")))
        // Unfinished walks never count.
        assertEquals("최근에 산책한 기록이 없어요!", label(walk("f", "2026-10-10", status = WalkStatus.Recording)))
    }
}
