package com.hakomi.practicetimer.domain

import kotlinx.serialization.Serializable
import kotlin.math.max

enum class Phase { PRACTICE, FEEDBACK, BREAK, FINISHED }

enum class RunState {
    /** The current phase has not been started yet (or has just been queued up). */
    IDLE,
    RUNNING,
    PAUSED,
}

/** What the timer is counting: a numbered round (practice then feedback) or the break. */
@Serializable
data class TimerTarget(
    val slot: Slot,
    val practiceMinutes: Int = 0,
    val feedbackMinutes: Int = 0,
    val breakMinutes: Int = 0,
) {
    val isBreak: Boolean get() = slot is Slot.Break
}

/**
 * Immutable timer state. While running, only [endAtMillis] matters, so the countdown stays
 * correct through screen-off, process death, and clock drift of the ticking loop.
 */
@Serializable
data class TimerState(
    val target: TimerTarget,
    val phase: Phase,
    val runState: RunState,
    val phaseDurationMillis: Long,
    /** Absolute deadline while [RunState.RUNNING]. */
    val endAtMillis: Long = 0L,
    /** Frozen remainder while [RunState.IDLE] or [RunState.PAUSED]. */
    val remainingMillis: Long = phaseDurationMillis,
    /** True once any phase has started; used to decide whether leaving needs confirmation. */
    val hasStarted: Boolean = false,
) {
    val isRunning: Boolean get() = runState == RunState.RUNNING
    val isPaused: Boolean get() = runState == RunState.PAUSED
    val isFinished: Boolean get() = phase == Phase.FINISHED
}

/** Pure transitions for a single round or break timer. All times are epoch millis supplied by the caller. */
object TimerEngine {

    fun initial(target: TimerTarget): TimerState {
        val phase = if (target.isBreak) Phase.BREAK else Phase.PRACTICE
        return TimerState(
            target = target,
            phase = phase,
            runState = RunState.IDLE,
            phaseDurationMillis = durationFor(target, phase),
        )
    }

    fun durationFor(target: TimerTarget, phase: Phase): Long = when (phase) {
        Phase.PRACTICE -> target.practiceMinutes
        Phase.FEEDBACK -> target.feedbackMinutes
        Phase.BREAK -> target.breakMinutes
        Phase.FINISHED -> 0
    } * SessionPlan.MILLIS_PER_MINUTE

    /** Starts the current idle phase from its full duration. No-op if already running or finished. */
    fun start(state: TimerState, nowMillis: Long): TimerState {
        if (state.runState != RunState.IDLE || state.isFinished) return state
        return state.copy(
            runState = RunState.RUNNING,
            endAtMillis = nowMillis + state.phaseDurationMillis,
            remainingMillis = state.phaseDurationMillis,
            hasStarted = true,
        )
    }

    fun pause(state: TimerState, nowMillis: Long): TimerState {
        if (!state.isRunning) return state
        return state.copy(
            runState = RunState.PAUSED,
            remainingMillis = max(0L, state.endAtMillis - nowMillis),
        )
    }

    fun resume(state: TimerState, nowMillis: Long): TimerState {
        if (!state.isPaused) return state
        return state.copy(
            runState = RunState.RUNNING,
            endAtMillis = nowMillis + state.remainingMillis,
        )
    }

    fun togglePause(state: TimerState, nowMillis: Long): TimerState =
        if (state.isRunning) pause(state, nowMillis) else resume(state, nowMillis)

    fun remainingMillis(state: TimerState, nowMillis: Long): Long = when (state.runState) {
        RunState.RUNNING -> max(0L, state.endAtMillis - nowMillis)
        RunState.IDLE, RunState.PAUSED -> state.remainingMillis
    }

    /** Whole seconds left, rounded up so the display reads `00:01` right until the deadline passes. */
    fun remainingSeconds(state: TimerState, nowMillis: Long): Long {
        val millis = remainingMillis(state, nowMillis)
        return (millis + 999) / 1000
    }

    data class Tick(val state: TimerState, val phaseCompleted: Boolean)

    /** Advances the state if the running phase has reached its deadline. */
    fun tick(state: TimerState, nowMillis: Long): Tick {
        if (!state.isRunning || nowMillis < state.endAtMillis) return Tick(state, phaseCompleted = false)
        return Tick(advance(state), phaseCompleted = true)
    }

    /** The "Finish" action: ends the current phase early and moves on. */
    fun skip(state: TimerState): TimerState = if (state.isFinished) state else advance(state)

    /**
     * Practice hands over to an idle feedback phase (the group starts it when ready);
     * feedback and break both complete the timer. A round without feedback finishes immediately.
     */
    private fun advance(state: TimerState): TimerState {
        val next = when (state.phase) {
            Phase.PRACTICE -> if (state.target.feedbackMinutes > 0) Phase.FEEDBACK else Phase.FINISHED
            Phase.FEEDBACK, Phase.BREAK, Phase.FINISHED -> Phase.FINISHED
        }
        val duration = durationFor(state.target, next)
        return state.copy(
            phase = next,
            runState = RunState.IDLE,
            phaseDurationMillis = duration,
            endAtMillis = 0L,
            remainingMillis = duration,
            hasStarted = true,
        )
    }
}
