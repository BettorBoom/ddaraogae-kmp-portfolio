@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

package dev.ddaraogae.domain

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlin.native.HiddenFromObjC

/** The original app's eight stamps; titles and descriptions are its own strings (StampInfo.kt). */
@HiddenFromObjC
enum class StampType(val number: Int, val title: String, val description: String) {
    DailyWalk(1, "오늘의 산책 완료", "매일 얻을 수 있으며 1.5km이상 산책을 하면 얻을 수 있습니다."),
    Streak3(2, "3일 연속 산책", "1주일 안에 3일 연속으로 산책을 하면 얻을 수 있습니다."),
    Streak5(3, "5일 연속 산책", "1주일 안에 5일 연속으로 산책을 하면 얻을 수 있습니다."),
    Streak7(4, "7일 연속 산책", "1주일 연속으로 산책을 하면 얻을 수 있습니다."),
    ManyWalks(5, "다다익선", "1주일안에 일 수에 상관없이 7번 산책을 하면 얻을 수 있습니다."),
    ShortRunner(6, "단거리 마라토너", "1주일 안에 10km이상 산책을하면 얻을 수 있습니다."),
    MiddleRunner(7, "중거리 마라토너", "1주일 안에 15km이상 산책을하면 얻을 수 있습니다."),
    LongRunner(8, "장거리 마라토너", "1주일 안에 20km이상 산책을하면 얻을 수 있습니다."),
}

/** One daily stamp per day (7) plus seven weekly stamps: the "14개 중 N개" denominator. */
@HiddenFromObjC
const val STAMPS_PER_WEEK = 14

/** [walkId] is the walk whose completion triggered the stamp. */
@HiddenFromObjC
data class StampAward(val type: StampType, val walkId: String, val awardedAtMillis: Long)

private const val DAILY_METERS = 1_500.0

/** Longest run of consecutive calendar days that have at least one walk. */
internal fun longestStreak(days: Collection<LocalDate>): Int {
    val sorted = days.distinct().sorted()
    var best = 0
    var run = 0
    var previous: LocalDate? = null
    for (day in sorted) {
        run = if (previous != null && previous.plus(1, DateTimeUnit.DAY) == day) run + 1 else 1
        best = maxOf(best, run)
        previous = day
    }
    return best
}

/**
 * Replays the history in finish order and grants stamps with the original rules
 * (FirebaseDataSourceImpl.checkStampCondition): the daily stamp once per day, every
 * other stamp once per Monday-Sunday week. Recomputing is idempotent, so no award is
 * stored and a reopened finish screen shows the same stamps (ADR-013).
 */
@HiddenFromObjC
fun stampAwards(history: List<WalkSession>, zone: TimeZone): List<StampAward> {
    val ordered = history.finished()
        .sortedWith(compareBy<WalkSession>({ it.endedAtMillis }, { it.startedAtMillis }, { it.id }))
    val seen = mutableListOf<WalkSession>()
    val given = HashSet<Pair<StampType, LocalDate>>()
    val awards = mutableListOf<StampAward>()
    for (walk in ordered) {
        seen += walk
        val at = walk.endedAtMillis ?: continue
        val today = localDateOf(at, zone)
        val week = weekOf(today)
        fun grant(type: StampType, key: LocalDate) {
            if (given.add(type to key)) awards += StampAward(type, walk.id, at)
        }
        val weekWalks = seen.filter { it.startDate(zone) in week }
        if (seen.filter { it.startDate(zone) == today }.sumOf { it.distanceMeters } >= DAILY_METERS)
            grant(StampType.DailyWalk, today)
        val streak = longestStreak(weekWalks.map { it.startDate(zone) })
        if (streak >= 3) grant(StampType.Streak3, week.start)
        if (streak >= 5) grant(StampType.Streak5, week.start)
        if (streak >= 7) grant(StampType.Streak7, week.start)
        if (weekWalks.size >= 7) grant(StampType.ManyWalks, week.start)
        val weekKm = weekWalks.sumOf { it.distanceMeters } / 1_000.0
        if (weekKm >= 10.0) grant(StampType.ShortRunner, week.start)
        if (weekKm >= 15.0) grant(StampType.MiddleRunner, week.start)
        if (weekKm >= 20.0) grant(StampType.LongRunner, week.start)
    }
    return awards
}

/** Stamps earned by finishing [walkId], in the original's award order. */
@HiddenFromObjC
fun List<StampAward>.earnedBy(walkId: String): List<StampType> = filter { it.walkId == walkId }.map { it.type }

/** The home progress numerator: stamps granted during the current Monday-Sunday week. */
@HiddenFromObjC
fun List<StampAward>.thisWeekCount(nowMillis: Long, zone: TimeZone): Int {
    val week = weekOf(localDateOf(nowMillis, zone))
    return count { localDateOf(it.awardedAtMillis, zone) in week }
}

/** All eight stamp types with how many times each was earned (zero when never). */
@HiddenFromObjC
fun List<StampAward>.countsByType(): Map<StampType, Int> =
    StampType.entries.associateWith { type -> count { it.type == type } }
