package dev.ddaraogae.domain

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class WalkFormatTest {
    @Test fun distanceUsesMetersBelowOneKilometerAndTruncates() {
        assertEquals("0 m", formatDistance(0.0))
        assertEquals("0 m", formatDistance(-5.0))
        assertEquals("13 m", formatDistance(13.9))
        assertEquals("999 m", formatDistance(999.9))
    }

    @Test fun distanceUsesOneDecimalKilometersFromOneKilometer() {
        assertEquals("1.0 km", formatDistance(1_000.0))
        assertEquals("3.7 km", formatDistance(3_700.0))
        assertEquals("1.1 km", formatDistance(1_050.0))
        assertEquals("12.3 km", formatDistance(12_345.0))
    }

    @Test fun walkTimerSwitchesToHoursWithTwoSpaces() {
        assertEquals("00분 00초", formatWalkTimer(0))
        assertEquals("07분 02초", formatWalkTimer(422))
        assertEquals("59분 59초", formatWalkTimer(3_599))
        assertEquals("01시간  00분", formatWalkTimer(3_600))
        assertEquals("01시간  02분", formatWalkTimer(3_725))
    }

    @Test fun historyDurationDropsEmptyLeadingUnits() {
        assertEquals("8초", formatHistoryDuration(8))
        assertEquals("3분 8초", formatHistoryDuration(188))
        assertEquals("1시간 2분 3초", formatHistoryDuration(3_723))
    }

    @Test fun koreanDateAndStampAreZeroPadded() {
        assertEquals("2024년 07월 04일 목요일", formatKoreanDate(LocalDate(2024, 7, 4)))
        assertEquals("2026년 10월 11일 일요일", formatKoreanDate(LocalDate(2026, 10, 11)))
        assertEquals("2024.05.28 07:08", formatStamp(LocalDateTime(2024, 5, 28, 7, 8)))
        assertEquals("06/25", formatMonthDay(LocalDate(2024, 6, 25)))
    }
}
