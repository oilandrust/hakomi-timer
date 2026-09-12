package com.hakomi.practicetimer.domain

import kotlinx.serialization.Serializable
import kotlin.math.ceil
import kotlin.math.max

/** A slot the group can run next: a numbered round or the break. */
@Serializable
sealed interface Slot {
    @Serializable
    data class Round(val number: Int) : Slot

    @Serializable
    data object Break : Slot
}

/**
 * A running session: the plan that was launched, when it started, and what has been completed so far.
 * Everything derived from it is computed by [distribution] for a given wall-clock time.
 */
@Serializable
data class PracticeSession(
    val plan: SessionPlan,
    val startMillis: Long,
    val completedRounds: Set<Int> = emptySet(),
    val breakCompleted: Boolean = false,
    val feedbackMinutes: Int = DEFAULT_FEEDBACK_MINUTES,
) {
    val hasBreak: Boolean get() = plan.breakMinutes > 0

    val remainingRounds: List<Int> get() = (1..plan.rounds).filter { it !in completedRounds }

    val isBreakPending: Boolean get() = hasBreak && !breakCompleted

    val allDone: Boolean get() = remainingRounds.isEmpty() && !isBreakPending

    /** The slot that should be highlighted by default: the first open round, then the break. */
    val nextSlot: Slot?
        get() = remainingRounds.firstOrNull()?.let { Slot.Round(it) }
            ?: if (isBreakPending) Slot.Break else null

    fun isCompleted(slot: Slot): Boolean = when (slot) {
        is Slot.Round -> slot.number in completedRounds
        Slot.Break -> breakCompleted
    }

    fun markCompleted(slot: Slot): PracticeSession = when (slot) {
        is Slot.Round -> copy(completedRounds = completedRounds + slot.number)
        Slot.Break -> copy(breakCompleted = true)
    }

    /** Elapsed whole minutes since the session started; never negative. */
    fun elapsedMinutes(nowMillis: Long): Int =
        max(0L, (nowMillis - startMillis) / SessionPlan.MILLIS_PER_MINUTE).toInt()

    /**
     * Landing time is not a separate timer; it is spread over the rounds so each round
     * leaves a little room to land and wrap up.
     */
    val downtimePerRound: Int
        get() = if (plan.landingTime > 0 && plan.rounds > 0) ceil(plan.landingTime / plan.rounds.toDouble()).toInt() else 0

    /** Recomputes how much time each remaining round can have right now. */
    fun distribution(nowMillis: Long): Distribution {
        val remainingBreak = if (isBreakPending) plan.breakMinutes else 0
        val remaining = remainingRounds
        val downtime = downtimePerRound

        return when (plan.pacing) {
            Pacing.FLEXIBLE -> {
                val remainingMinutes = max(0, remaining.size * (plan.perRoundMinutes + downtime) + remainingBreak)
                Distribution(
                    remainingMinutes = remainingMinutes,
                    roundMinutes = max(1, plan.perRoundMinutes),
                    sessionEndMillis = nowMillis + remainingMinutes * SessionPlan.MILLIS_PER_MINUTE,
                    remainingBreakMinutes = remainingBreak,
                    downtimePerRound = downtime,
                )
            }

            Pacing.FINISH_ON_TIME -> {
                val remainingMinutes = max(0, plan.totalMinutes - elapsedMinutes(nowMillis))
                val roundMinutes = if (remaining.isNotEmpty()) {
                    max(1, (remainingMinutes - remainingBreak).floorDiv(remaining.size) - downtime)
                } else {
                    plan.perRoundMinutes
                }
                Distribution(
                    remainingMinutes = remainingMinutes,
                    roundMinutes = roundMinutes,
                    sessionEndMillis = plan.endMillis(startMillis),
                    remainingBreakMinutes = remainingBreak,
                    downtimePerRound = downtime,
                )
            }
        }
    }

    companion object {
        const val DEFAULT_FEEDBACK_MINUTES = 7
    }
}

/** Time budget for the next round, valid at the instant it was computed. */
data class Distribution(
    /** Minutes left in the whole session. */
    val remainingMinutes: Int,
    /** Minutes the next round can take, including its share of landing time. */
    val roundMinutes: Int,
    /** When the session is expected to end. */
    val sessionEndMillis: Long,
    val remainingBreakMinutes: Int,
    val downtimePerRound: Int,
) {
    /** Splits [roundMinutes] into practice and feedback, honouring the requested feedback length where possible. */
    fun split(requestedFeedbackMinutes: Int): RoundSplit = RoundSplit.of(roundMinutes, requestedFeedbackMinutes)
}

/** Practice and feedback minutes for one round. Practice always keeps at least one minute. */
data class RoundSplit(val practiceMinutes: Int, val feedbackMinutes: Int) {
    val totalMinutes: Int get() = practiceMinutes + feedbackMinutes

    companion object {
        fun of(roundMinutes: Int, requestedFeedbackMinutes: Int): RoundSplit {
            val total = max(1, roundMinutes)
            val feedback = requestedFeedbackMinutes.coerceIn(0, total - 1)
            return RoundSplit(practiceMinutes = total - feedback, feedbackMinutes = feedback)
        }
    }
}
