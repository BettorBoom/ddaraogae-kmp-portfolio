@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

package dev.ddaraogae.domain

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.daysUntil
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.native.HiddenFromObjC
import kotlin.time.Instant

// History-derived numbers for the home, history and stamp screens. Nothing here is
// stored: it is recomputed from the local walk history so it is idempotent (ADR-013).

@HiddenFromObjC
data class DayDistance(val date: LocalDate, val meters: Double)

/** Walks that finished and can contribute to graphs, stamps and "time since". */
@HiddenFromObjC
fun List<WalkSession>.finished(): List<WalkSession> =
    filter { it.status == WalkStatus.Completed && it.endedAtMillis != null }

@HiddenFromObjC
fun WalkSession.startDate(zone: TimeZone): LocalDate =
    Instant.fromEpochMilliseconds(startedAtMillis).toLocalDateTime(zone).date

@HiddenFromObjC
fun localDateOf(epochMillis: Long, zone: TimeZone): LocalDate =
    Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(zone).date

/** Monday..Sunday of the week containing [date], as the original stamp rules define a week. */
@HiddenFromObjC
fun weekOf(date: LocalDate): ClosedRange<LocalDate> {
    val monday = date.minus(date.dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY)
    return monday..monday.plus(6, DateTimeUnit.DAY)
}

/** Inclusive, zero-filled per-day totals; walks are bucketed by their start date like the original. */
@HiddenFromObjC
fun dailyDistances(history: List<WalkSession>, from: LocalDate, to: LocalDate, zone: TimeZone): List<DayDistance> {
    if (to < from) return emptyList()
    val totals = HashMap<LocalDate, Double>()
    history.finished().forEach { walk ->
        val day = walk.startDate(zone)
        if (day in from..to) totals[day] = (totals[day] ?: 0.0) + walk.distanceMeters
    }
    return (0..from.daysUntil(to)).map { offset ->
        val day = from.plus(offset, DateTimeUnit.DAY)
        DayDistance(day, totals[day] ?: 0.0)
    }
}

/** The home graph: the seven days ending today. */
@HiddenFromObjC
fun lastSevenDays(history: List<WalkSession>, nowMillis: Long, zone: TimeZone): List<DayDistance> {
    val today = localDateOf(nowMillis, zone)
    return dailyDistances(history, today.minus(6, DateTimeUnit.DAY), today, zone)
}

/** The history graph: every day of one calendar month. */
@HiddenFromObjC
fun monthDays(history: List<WalkSession>, year: Int, month: Int, zone: TimeZone): List<DayDistance> {
    val first = LocalDate(year, month, 1)
    val last = first.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY)
    return dailyDistances(history, first, last, zone)
}

/** Finished walks that started inside [year]-[month], newest first (the history list order). */
@HiddenFromObjC
fun walksOfMonth(history: List<WalkSession>, year: Int, month: Int, zone: TimeZone): List<WalkSession> =
    history.finished().filter { val day = it.startDate(zone); day.year == year && day.monthNumber == month }
        .sortedWith(compareByDescending<WalkSession> { it.startedAtMillis }.thenByDescending { it.id })

/**
 * Home headline under the greeting. Like the original it looks only at walks from the
 * last seven days (today-6 00:00 until now) and applies its hour thresholds.
 */
@HiddenFromObjC
fun sinceLastWalkLabel(history: List<WalkSession>, nowMillis: Long, zone: TimeZone): String {
    val windowStart = localDateOf(nowMillis, zone).minus(6, DateTimeUnit.DAY)
        .atStartOfDayIn(zone).toEpochMilliseconds()
    val last = history.finished().filter { it.startedAtMillis in windowStart..nowMillis }
        .maxByOrNull { it.startedAtMillis } ?: return "최근에 산책한 기록이 없어요!"
    val hours = ((nowMillis - (last.endedAtMillis ?: nowMillis)).coerceAtLeast(0) / 3_600_000L).toInt()
    return when {
        hours == 0 -> "금방 산책을 다녀왔어요!"
        hours < 24 -> "$hours 시간 전"
        else -> "산책을 안한지 하루가 넘었어요!"
    }
}
