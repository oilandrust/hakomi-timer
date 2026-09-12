package com.hakomi.practicetimer.domain

import kotlinx.serialization.Serializable
import kotlin.math.max

/** How the total session length is specified. */
enum class TimingMode { DURING, UNTIL }

/** How the app should respond when the group runs late. */
enum class Pacing {
    /** Shrink the remaining rounds so the session still ends at the planned time. */
    FINISH_ON_TIME,
    /** Keep every round at its planned length and let the end time slide. */
    FLEXIBLE,
}

/**
 * The parameters entered on the planning screen. All arithmetic that the planning screen
 * displays lives here so it can be unit tested independently of the UI.
 */
@Serializable
data class SessionPlan(
    val totalMinutes: Int = 60,
    val rounds: Int = 2,
    val breakMinutes: Int = 0,
    val landingMinutes: Int = 3,
    val includeLanding: Boolean = true,
    val pacing: Pacing = Pacing.FINISH_ON_TIME,
) {
    /** Minutes reserved for landing / wrap-up, or zero when the option is off. */
    val landingTime: Int get() = if (includeLanding) max(0, landingMinutes) else 0

    /** Minutes that remain for practice once the break and landing time are set aside. */
    val availableMinutes: Int get() = max(0, totalMinutes - max(0, breakMinutes) - landingTime)

    /** Whole minutes per round; any remainder is left as slack. */
    val perRoundMinutes: Int get() = if (rounds > 0) availableMinutes / rounds else 0

    /** Planned end of the session when it starts at [startMillis]. */
    fun endMillis(startMillis: Long): Long = startMillis + totalMinutes * MILLIS_PER_MINUTE

    val validationError: String?
        get() = when {
            rounds < 1 -> "Rounds must be at least 1"
            max(0, breakMinutes) + landingTime > totalMinutes ->
                "Break time and landing time cannot exceed total session time"
            perRoundMinutes < 1 -> "There is no time left for a round"
            else -> null
        }

    val isValid: Boolean get() = validationError == null

    companion object {
        const val MILLIS_PER_MINUTE = 60_000L
        val durationPresets = listOf(60, 90, 120)
        val roundPresets = listOf(1, 2, 3, 4)
        val breakPresets = listOf(0, 5, 10, 15)
    }
}
