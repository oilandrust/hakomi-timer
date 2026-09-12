package com.hakomi.practicetimer.domain

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/** Candidate end times offered in "until" mode: roughly 60, 90 and 120 minutes out, snapped to the half hour. */
object EndTimePresets {

    data class Option(val label: String, val endMillis: Long) {
        fun minutesFrom(nowMillis: Long): Int = minutesBetween(nowMillis, endMillis)
    }

    private val offsets = listOf("60 min" to 60L, "90 min" to 90L, "2 hours" to 120L)

    fun options(nowMillis: Long, zone: ZoneId = ZoneId.systemDefault()): List<Option> {
        val now = Instant.ofEpochMilli(nowMillis).atZone(zone)
        return offsets.map { (label, minutes) ->
            Option(label, roundToNearestHalfHour(now.plusMinutes(minutes)).toInstant().toEpochMilli())
        }
    }

    /** Snaps to the closest :00 or :30, rounding ties (xx:15, xx:45) upward like `Math.round` does. */
    fun roundToNearestHalfHour(time: ZonedDateTime): ZonedDateTime {
        val halfHours = (time.minute + 15) / 30
        return time.truncatedTo(ChronoUnit.HOURS).plusMinutes(halfHours * 30L)
    }

    /** Whole minutes from [fromMillis] to [toMillis], rounded half-up and never negative. */
    fun minutesBetween(fromMillis: Long, toMillis: Long): Int {
        val minutes = Math.round((toMillis - fromMillis) / SessionPlan.MILLIS_PER_MINUTE.toDouble())
        return maxOf(0L, minutes).toInt()
    }

    /** Builds today's instant for a picked hour:minute; rolls to tomorrow if that moment has already passed. */
    fun endMillisFor(hour: Int, minute: Int, nowMillis: Long, zone: ZoneId = ZoneId.systemDefault()): Long {
        val now = Instant.ofEpochMilli(nowMillis).atZone(zone)
        var candidate = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if (candidate.toInstant().toEpochMilli() < nowMillis) candidate = candidate.plusDays(1)
        return candidate.toInstant().toEpochMilli()
    }
}
