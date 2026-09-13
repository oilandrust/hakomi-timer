package com.hakomi.practicetimer.ui.plan

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hakomi.practicetimer.data.SessionRepository
import com.hakomi.practicetimer.domain.EndTimePresets
import com.hakomi.practicetimer.domain.Pacing
import com.hakomi.practicetimer.domain.SessionPlan
import com.hakomi.practicetimer.domain.TimingMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import kotlin.math.max

/** Holds the draft plan while the user shapes it. Values persist across rotation and from the previous session. */
class PlanViewModel(
    private val repository: SessionRepository,
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val seed = repository.lastPlan() ?: SessionPlan()

    var timingMode by mutableStateOf(TimingMode.DURING)
        private set

    /** Session length in "during" mode. */
    var durationMinutes by mutableStateOf(seed.totalMinutes)
        private set

    /** Chosen end of the session in "until" mode. */
    var targetEndMillis by mutableStateOf<Long?>(null)
        private set

    var rounds by mutableStateOf(seed.rounds)
        private set
    var breakMinutes by mutableStateOf(seed.breakMinutes)
        private set
    /** Not offered on the planning screen at the moment, so nothing is held back by default. */
    var landingMinutes by mutableStateOf(0)
        private set
    var includeLanding by mutableStateOf(seed.includeLanding)
        private set
    var pacing by mutableStateOf(seed.pacing)
        private set

    private val _now = MutableStateFlow(clock())
    /** Ticks once a second so "until" presets and the projected end time stay honest. */
    val now: StateFlow<Long> = _now

    init {
        viewModelScope.launch {
            while (true) {
                _now.value = clock()
                delay(1_000)
            }
        }
    }

    fun totalMinutes(nowMillis: Long): Int = when (timingMode) {
        TimingMode.DURING -> durationMinutes
        TimingMode.UNTIL -> targetEndMillis?.let { EndTimePresets.minutesBetween(nowMillis, it) } ?: 0
    }

    fun plan(nowMillis: Long): SessionPlan = SessionPlan(
        totalMinutes = totalMinutes(nowMillis),
        rounds = rounds,
        breakMinutes = breakMinutes,
        landingMinutes = landingMinutes,
        includeLanding = includeLanding,
        pacing = pacing,
    )

    fun endPresets(nowMillis: Long, zone: ZoneId = ZoneId.systemDefault()) = EndTimePresets.options(nowMillis, zone)

    fun chooseTimingMode(mode: TimingMode) {
        if (mode == timingMode) return
        timingMode = mode
        if (mode == TimingMode.UNTIL && targetEndMillis == null) {
            targetEndMillis = endPresets(clock()).first().endMillis
        }
    }

    fun chooseDuration(minutes: Int) {
        durationMinutes = minutes.coerceIn(MIN_DURATION, MAX_DURATION)
    }

    fun nudgeDuration(deltaMinutes: Int) = chooseDuration(durationMinutes + deltaMinutes)

    fun chooseDurationHours(hours: Int) = chooseDuration(hours * 60 + durationMinutes % 60)

    fun chooseDurationMinutes(minutes: Int) = chooseDuration((durationMinutes / 60) * 60 + minutes)

    fun selectEndTime(endMillis: Long) {
        targetEndMillis = endMillis
    }

    fun nudgeEndTime(deltaMinutes: Int) {
        val current = targetEndMillis ?: return
        val next = current + deltaMinutes * SessionPlan.MILLIS_PER_MINUTE
        if (next > clock()) targetEndMillis = next
    }

    fun pickEndTime(hour: Int, minute: Int, zone: ZoneId = ZoneId.systemDefault()) {
        targetEndMillis = EndTimePresets.endMillisFor(hour, minute, clock(), zone)
    }

    /** Moves the end time to the morning or the afternoon, keeping the hour and minute shown. */
    fun setMeridiem(pm: Boolean, zone: ZoneId = ZoneId.systemDefault()) {
        val time = targetEndLocalTime(zone) ?: return
        if ((time.hour >= 12) == pm) return
        pickEndTime(if (pm) time.hour + 12 else time.hour - 12, time.minute, zone)
    }

    fun chooseEndHour(hour24: Int, zone: ZoneId = ZoneId.systemDefault()) {
        val time = targetEndLocalTime(zone) ?: return
        pickEndTime(hour24, time.minute, zone)
    }

    /** A typed hour is read off the clock face, so 3 stays in the afternoon if the end time already was. */
    fun chooseEndHourOnClock(hour: Int, use24Hour: Boolean, zone: ZoneId = ZoneId.systemDefault()) {
        val time = targetEndLocalTime(zone) ?: return
        val hour24 = if (use24Hour) hour else hour % 12 + if (time.hour >= 12) 12 else 0
        pickEndTime(hour24, time.minute, zone)
    }

    fun chooseEndMinute(minute: Int, zone: ZoneId = ZoneId.systemDefault()) {
        val time = targetEndLocalTime(zone) ?: return
        pickEndTime(time.hour, minute, zone)
    }

    fun targetEndLocalTime(zone: ZoneId = ZoneId.systemDefault()) =
        targetEndMillis?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalTime() }

    fun chooseRounds(value: Int) {
        rounds = value.coerceIn(1, 12)
    }

    fun chooseBreak(minutes: Int) {
        breakMinutes = max(0, minutes)
    }

    fun chooseIncludeLanding(value: Boolean) {
        includeLanding = value
    }

    fun nudgeLanding(delta: Int) {
        landingMinutes = (landingMinutes + delta).coerceIn(0, 60)
    }

    fun choosePacing(value: Pacing) {
        pacing = value
    }

    /** Freezes the plan and starts the session clock. Returns false if the plan is not valid. */
    fun launchSession(): Boolean {
        val nowMillis = clock()
        val plan = plan(nowMillis)
        if (!plan.isValid) return false
        repository.startSession(plan, nowMillis)
        return true
    }

    companion object {
        const val MIN_DURATION = 5
        const val MAX_DURATION = 12 * 60

        fun factory(repository: SessionRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = PlanViewModel(repository) as T
        }
    }
}
