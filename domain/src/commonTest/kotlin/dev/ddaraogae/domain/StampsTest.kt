package dev.ddaraogae.domain

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StampsTest {
    private fun awardsOf(vararg walks: WalkSession) = stampAwards(walks.toList(), seoul)
    private fun typesFor(awards: List<StampAward>, id: String) = awards.earnedBy(id)

    @Test fun catalogMatchesTheOriginalEightStamps() {
        assertEquals(8, StampType.entries.size)
        assertEquals(listOf(1, 2, 3, 4, 5, 6, 7, 8), StampType.entries.map { it.number })
        assertEquals("오늘의 산책 완료", StampType.DailyWalk.title)
        assertEquals("장거리 마라토너", StampType.LongRunner.title)
        assertEquals(14, STAMPS_PER_WEEK)
    }

    @Test fun dailyStampNeedsOnePointFiveKilometersAndIsGivenOncePerDay() {
        val short = awardsOf(walk("a", "2026-10-06", meters = 1_499.0))
        assertTrue(short.isEmpty())
        val exact = awardsOf(walk("a", "2026-10-06", meters = 1_500.0))
        assertEquals(listOf(StampType.DailyWalk), typesFor(exact, "a"))
        // Two walks the same day sum up; only the walk that crosses the line earns it, once.
        val summed = awardsOf(walk("a", "2026-10-06", hour = 8, meters = 900.0),
            walk("b", "2026-10-06", hour = 18, meters = 700.0),
            walk("c", "2026-10-06", hour = 20, meters = 2_000.0))
        assertEquals(emptyList(), typesFor(summed, "a"))
        assertEquals(listOf(StampType.DailyWalk), typesFor(summed, "b"))
        assertEquals(emptyList(), typesFor(summed, "c"))
        // The next day is a fresh chance.
        val twoDays = awardsOf(walk("a", "2026-10-06", meters = 1_600.0), walk("b", "2026-10-07", meters = 1_600.0))
        assertEquals(2, twoDays.count { it.type == StampType.DailyWalk })
    }

    @Test fun streakStampsFollowConsecutiveDaysInsideOneWeek() {
        val days = listOf("2026-10-05", "2026-10-06", "2026-10-07", "2026-10-08", "2026-10-09", "2026-10-10", "2026-10-11")
        val awards = stampAwards(days.mapIndexed { i, d -> walk("w$i", d) }, seoul)
        assertEquals(listOf(StampType.Streak3), typesFor(awards, "w2"))
        assertEquals(listOf(StampType.Streak5), typesFor(awards, "w4"))
        // The seventh walk of the week also reaches the 7-walk stamp, in the original's order.
        assertEquals(listOf(StampType.Streak7, StampType.ManyWalks), typesFor(awards, "w6"))
        assertEquals(1, awards.count { it.type == StampType.Streak3 })
    }

    @Test fun aStreakCannotCrossTheSundayMondayBoundary() {
        val awards = awardsOf(walk("sat", "2026-10-10"), walk("sun", "2026-10-11"), walk("mon", "2026-10-12"))
        assertTrue(awards.none { it.type == StampType.Streak3 })
    }

    @Test fun sevenWalksInAWeekEarnTheManyWalksStampOnce() {
        val walks = (0 until 8).map { walk("w$it", "2026-10-06", hour = 6 + it) }
        val awards = stampAwards(walks, seoul)
        assertEquals(listOf(StampType.ManyWalks), typesFor(awards, "w6"))
        assertEquals(1, awards.count { it.type == StampType.ManyWalks })
        assertEquals(emptyList(), typesFor(awards, "w7"))
    }

    @Test fun weeklyDistanceStampsAreGivenWhenEachThresholdIsCrossed() {
        val awards = awardsOf(
            walk("a", "2026-10-05", meters = 9_000.0), walk("b", "2026-10-06", meters = 1_000.0),
            walk("c", "2026-10-07", meters = 5_000.0), walk("d", "2026-10-08", meters = 5_000.0),
            walk("e", "2026-10-09", meters = 1_000.0),
        )
        assertTrue(StampType.ShortRunner in typesFor(awards, "b"))
        assertTrue(StampType.MiddleRunner in typesFor(awards, "c"))
        assertTrue(StampType.LongRunner in typesFor(awards, "d"))
        assertTrue(awards.none { it.type == StampType.LongRunner && it.walkId == "e" })
    }

    @Test fun distanceStampsStartOverInTheNextWeek() {
        val awards = awardsOf(walk("a", "2026-10-06", meters = 10_000.0), walk("b", "2026-10-13", meters = 10_000.0))
        assertEquals(2, awards.count { it.type == StampType.ShortRunner })
    }

    @Test fun recomputingIsIdempotentAndIgnoresInputOrderAndUnfinishedWalks() {
        val history = listOf(walk("a", "2026-10-05", meters = 2_000.0), walk("b", "2026-10-06", meters = 2_000.0),
            walk("c", "2026-10-07", meters = 2_000.0), walk("open", "2026-10-08", meters = 50_000.0, status = WalkStatus.Interrupted))
        val first = stampAwards(history, seoul)
        assertEquals(first, stampAwards(history.shuffled(), seoul))
        assertEquals(first, stampAwards(history, seoul))
        assertTrue(first.none { it.walkId == "open" })
    }

    @Test fun weekCountAndPerTypeCountsFollowTheAwards() {
        val awards = awardsOf(walk("a", "2026-10-06", meters = 2_000.0), walk("b", "2026-10-13", meters = 2_000.0))
        val now = millis("2026-10-13", hour = 12)
        assertEquals(1, awards.thisWeekCount(now, seoul))
        val counts = awards.countsByType()
        assertEquals(8, counts.size)
        assertEquals(2, counts.getValue(StampType.DailyWalk))
        assertEquals(0, counts.getValue(StampType.LongRunner))
    }

    @Test fun longestStreakCountsDistinctConsecutiveDays() {
        val day = LocalDate(2026, 10, 5)
        assertEquals(0, longestStreak(emptyList()))
        assertEquals(1, longestStreak(listOf(day, day)))
        assertEquals(3, longestStreak(listOf(LocalDate(2026, 10, 7), day, LocalDate(2026, 10, 6), LocalDate(2026, 10, 9))))
    }
}
