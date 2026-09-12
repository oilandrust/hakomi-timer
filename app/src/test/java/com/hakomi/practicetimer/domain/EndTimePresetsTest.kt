package com.hakomi.practicetimer.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

class EndTimePresetsTest {

    private val zone: ZoneId = ZoneOffset.UTC

    private fun at(hour: Int, minute: Int, second: Int = 0): Long =
        LocalDateTime.of(2025, 9, 1, hour, minute, second).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun `presets snap to the nearest half hour`() {
        // 12:31 -> 13:31 -> 13:30, 14:01 -> 14:00, 14:31 -> 14:30
        val options = EndTimePresets.options(at(12, 31), zone)
        assertEquals(listOf(at(13, 30), at(14, 0), at(14, 30)), options.map { it.endMillis })
        assertEquals(listOf("60 min", "90 min", "2 hours"), options.map { it.label })
    }

    @Test
    fun `xx45 rounds up to the next hour like Math round`() {
        val options = EndTimePresets.options(at(9, 45), zone)
        // 10:45 -> 11:00, 11:15 -> 11:30, 11:45 -> 12:00
        assertEquals(listOf(at(11, 0), at(11, 30), at(12, 0)), options.map { it.endMillis })
    }

    @Test
    fun `xx14 rounds down and xx15 rounds up`() {
        val base = LocalDateTime.of(2025, 9, 1, 10, 0).atZone(zone)
        assertEquals(base, EndTimePresets.roundToNearestHalfHour(base.withMinute(14).withSecond(59)))
        assertEquals(base.withMinute(30), EndTimePresets.roundToNearestHalfHour(base.withMinute(15)))
    }

    @Test
    fun `minutes until an option are rounded half up and never negative`() {
        val now = at(12, 31, 40)
        val option = EndTimePresets.options(now, zone).first()
        // 13:30:00 - 12:31:40 = 58 min 20 s -> 58
        assertEquals(58, option.minutesFrom(now))
        assertEquals(0, EndTimePresets.minutesBetween(now, now - 5 * 60_000L))
        assertEquals(1, EndTimePresets.minutesBetween(now, now + 30_000L))
    }

    @Test
    fun `picked time earlier than now rolls to tomorrow`() {
        val now = at(15, 0)
        assertEquals(at(16, 30), EndTimePresets.endMillisFor(16, 30, now, zone))
        assertEquals(at(9, 0) + 24 * 3_600_000L, EndTimePresets.endMillisFor(9, 0, now, zone))
    }
}
