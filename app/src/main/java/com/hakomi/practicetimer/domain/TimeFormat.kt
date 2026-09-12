package com.hakomi.practicetimer.domain

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

/** Human readable time formatting shared by every screen. Mirrors the helpers in the original web app. */
object TimeFormat {

    /** `45 min`, `1 hr`, `2 hrs`, `1 hr 30 min`. Negative values clamp to zero. */
    fun minutes(totalMinutes: Int): String {
        val rounded = max(0, totalMinutes)
        val hours = rounded / 60
        val mins = rounded % 60
        return when {
            hours == 0 -> "$mins min"
            mins == 0 -> "$hours ${hourWord(hours)}"
            else -> "$hours ${hourWord(hours)} $mins min"
        }
    }

    fun minutes(totalMinutes: Double): String = minutes(totalMinutes.roundToInt())

    private fun hourWord(hours: Int) = if (hours > 1) "hrs" else "hr"

    /** Countdown text: `MM:SS`, or `HH:MM:SS` once an hour or more remains. */
    fun countdown(totalSeconds: Long): String {
        val s = max(0L, totalSeconds)
        val hours = s / 3600
        val mins = (s % 3600) / 60
        val secs = s % 60
        return if (hours > 0) {
            "%02d:%02d:%02d".format(Locale.ROOT, hours, mins, secs)
        } else {
            "%02d:%02d".format(Locale.ROOT, mins, secs)
        }
    }

    private val twelveHour = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
    private val twentyFourHour = DateTimeFormatter.ofPattern("HH:mm", Locale.US)

    /** Wall-clock time such as `1:31 PM` or `13:31`. */
    fun timeOfDay(epochMillis: Long, use24Hour: Boolean = false, zone: ZoneId = ZoneId.systemDefault()): String {
        val time = Instant.ofEpochMilli(epochMillis).atZone(zone)
        return if (use24Hour) twentyFourHour.format(time) else twelveHour.format(time)
    }
}
