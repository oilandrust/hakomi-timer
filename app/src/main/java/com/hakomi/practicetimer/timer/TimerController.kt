package com.hakomi.practicetimer.timer

import com.hakomi.practicetimer.data.SessionRepository
import com.hakomi.practicetimer.domain.Phase
import com.hakomi.practicetimer.domain.TimerEngine
import com.hakomi.practicetimer.domain.TimerState
import com.hakomi.practicetimer.domain.TimerTarget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * The single source of truth for the round timer. Lives for the whole process so the UI,
 * the foreground service and the notification all observe the same state.
 */
class TimerController(
    private val repository: SessionRepository,
    private val scope: CoroutineScope,
    private val onPhaseCompleted: (Phase) -> Unit = {},
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val _state = MutableStateFlow(repository.readTimer())
    val state: StateFlow<TimerState?> = _state

    /** Wall clock, refreshed a few times per second while a phase is running. */
    private val _now = MutableStateFlow(clock())
    val now: StateFlow<Long> = _now

    private val _phaseCompleted = MutableSharedFlow<Phase>(extraBufferCapacity = 8)
    val phaseCompleted: SharedFlow<Phase> = _phaseCompleted

    init {
        scope.launch { runTicker() }
    }

    private suspend fun runTicker() {
        while (true) {
            val current = _state.value
            if (current == null || !current.isRunning) {
                _state.first { it?.isRunning == true }
                continue
            }
            val nowMillis = clock()
            _now.value = nowMillis
            val tick = TimerEngine.tick(current, nowMillis)
            if (tick.phaseCompleted) {
                set(tick.state)
                _phaseCompleted.tryEmit(current.phase)
                onPhaseCompleted(current.phase)
            }
            delay(TICK_MILLIS)
        }
    }

    /** Loads a new round or break. Breaks begin counting immediately, rounds wait for a tap. */
    fun begin(target: TimerTarget) {
        val initial = TimerEngine.initial(target)
        set(if (target.isBreak) TimerEngine.start(initial, clock()) else initial)
    }

    fun start() = mutate { TimerEngine.start(it, clock()) }

    fun togglePause() = mutate { TimerEngine.togglePause(it, clock()) }

    fun skip() = mutate { TimerEngine.skip(it) }

    fun clear() = set(null)

    fun refreshNow() {
        _now.value = clock()
    }

    private fun mutate(transform: (TimerState) -> TimerState) {
        val current = _state.value ?: return
        val next = transform(current)
        if (next != current) set(next)
        refreshNow()
    }

    private fun set(state: TimerState?) {
        _state.value = state
        repository.writeTimer(state)
    }

    private companion object {
        const val TICK_MILLIS = 250L
    }
}
