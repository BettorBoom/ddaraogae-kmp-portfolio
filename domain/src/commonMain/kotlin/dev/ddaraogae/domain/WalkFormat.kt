@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

package dev.ddaraogae.domain

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlin.math.roundToInt
import kotlin.native.HiddenFromObjC

// Text rules copied from the original app's TextConverter so both platforms show
// the same strings. They are pure so the shared unit tests pin them (ADR-013).

/** 1 km or more as "3.7 km"; below that as whole meters, truncated like the original. */
@HiddenFromObjC
fun formatDistance(meters: Double): String {
    val safe = if (meters.isFinite() && meters > 0) meters else 0.0
    if (safe < 1_000.0) return "${safe.toInt()} m"
    val tenths = (safe / 100.0).roundToInt()
    return "${tenths / 10}.${tenths % 10} km"
}

private fun two(value: Int) = value.toString().padStart(2, '0')

/** Walk-screen timer: "07분 02초", or "01시간  05분" (two spaces, as in the original) from one hour. */
@HiddenFromObjC
fun formatWalkTimer(totalSeconds: Long): String {
    val seconds = totalSeconds.coerceAtLeast(0)
    val hours = (seconds / 3600).toInt()
    val minutes = ((seconds / 60) % 60).toInt()
    val rest = (seconds % 60).toInt()
    return if (hours >= 1) "${two(hours)}시간  ${two(minutes)}분" else "${two(minutes)}분 ${two(rest)}초"
}

/** History-card duration: "1시간 2분 3초", "32분 10초", "10초". */
@HiddenFromObjC
fun formatHistoryDuration(totalSeconds: Long): String {
    val seconds = totalSeconds.coerceAtLeast(0)
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val rest = seconds % 60
    return when {
        hours > 0 -> "${hours}시간 ${minutes}분 ${rest}초"
        minutes > 0 -> "${minutes}분 ${rest}초"
        else -> "${rest}초"
    }
}

private val koreanWeekday = mapOf(
    DayOfWeek.MONDAY to "월요일", DayOfWeek.TUESDAY to "화요일", DayOfWeek.WEDNESDAY to "수요일",
    DayOfWeek.THURSDAY to "목요일", DayOfWeek.FRIDAY to "금요일", DayOfWeek.SATURDAY to "토요일",
    DayOfWeek.SUNDAY to "일요일",
)

/** "2024년 07월 04일 목요일" (yyyy년 MM월 dd일 EEEE, zero padded like the original). */
@HiddenFromObjC
fun formatKoreanDate(date: LocalDate): String =
    "${date.year}년 ${two(date.monthNumber)}월 ${two(date.dayOfMonth)}일 ${koreanWeekday.getValue(date.dayOfWeek)}"

/** "2024.05.28 17:38" (yyyy.MM.dd HH:mm), used by the history cards. */
@HiddenFromObjC
fun formatStamp(dateTime: LocalDateTime): String =
    "${dateTime.year}.${two(dateTime.monthNumber)}.${two(dateTime.dayOfMonth)} ${two(dateTime.hour)}:${two(dateTime.minute)}"

/** Chart axis label "MM/dd". */
@HiddenFromObjC
fun formatMonthDay(date: LocalDate): String = "${two(date.monthNumber)}/${two(date.dayOfMonth)}"
