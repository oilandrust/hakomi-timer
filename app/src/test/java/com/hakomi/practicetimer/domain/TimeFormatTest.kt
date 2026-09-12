package com.hakomi.practicetimer.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneOffset

class TimeFormatTest {

    @Test
    fun `minutes formatting matches the web helper`() {
        assertEquals("0 min", TimeFormat.minutes(0))
        assertEquals("45 min", TimeFormat.minutes(45))
        assertEquals("1 hr", TimeFormat.minutes(60))
        assertEquals("2 hrs", TimeFormat.minutes(120))
        assertEquals("1 hr 30 min", TimeFormat.minutes(90))
        assertEquals("2 hrs 5 min", TimeFormat.minutes(125))
        assertEquals("0 min", TimeFormat.minutes(-10))
    }

    @Test
    fun `countdown shows hours only when needed`() {
        assertEquals("00:00", TimeFormat.countdown(0))
        assertEquals("00:07", TimeFormat.countdown(7))
        assertEquals("23:00", TimeFormat.countdown(23 * 60))
        assertEquals("01:00:00", TimeFormat.countdown(3600))
        assertEquals("01:02:03", TimeFormat.countdown(3723))
        assertEquals("00:00", TimeFormat.countdown(-4))
    }

    @Test
    fun `time of day in twelve and twenty four hour clocks`() {
        val millis = LocalDateTime.of(2025, 9, 1, 13, 31).toInstant(ZoneOffset.UTC).toEpochMilli()
        assertEquals("1:31 PM", TimeFormat.timeOfDay(millis, use24Hour = false, zone = ZoneOffset.UTC))
        assertEquals("13:31", TimeFormat.timeOfDay(millis, use24Hour = true, zone = ZoneOffset.UTC))
    }
}
