package com.hakomi.practicetimer.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerEngineTest {

    private val t0 = 5_000_000L
    private val roundTarget = TimerTarget(slot = Slot.Round(1), practiceMinutes = 21, feedbackMinutes = 7)
    private val breakTarget = TimerTarget(slot = Slot.Break, breakMinutes = 10)

    @Test
    fun `a round starts idle in practice with the full duration showing`() {
        val s = TimerEngine.initial(roundTarget)
        assertEquals(Phase.PRACTICE, s.phase)
        assertEquals(RunState.IDLE, s.runState)
        assertFalse(s.hasStarted)
        assertEquals(21 * 60L, TimerEngine.remainingSeconds(s, t0))
    }

    @Test
    fun `a break starts idle in the break phase`() {
        val s = TimerEngine.initial(breakTarget)
        assertEquals(Phase.BREAK, s.phase)
        assertEquals(10 * 60L, TimerEngine.remainingSeconds(s, t0))
    }

    @Test
    fun `running counts down against the wall clock`() {
        val s = TimerEngine.start(TimerEngine.initial(roundTarget), t0)
        assertTrue(s.isRunning)
        assertTrue(s.hasStarted)
        assertEquals(21 * 60L, TimerEngine.remainingSeconds(s, t0))
        assertEquals(21 * 60L - 90, TimerEngine.remainingSeconds(s, t0 + 90_000L))
    }

    @Test
    fun `remaining seconds round up so the last second reads one`() {
        val s = TimerEngine.start(TimerEngine.initial(roundTarget), t0)
        val end = s.endAtMillis
        assertEquals(1L, TimerEngine.remainingSeconds(s, end - 1))
        assertEquals(1L, TimerEngine.remainingSeconds(s, end - 999))
        assertEquals(0L, TimerEngine.remainingSeconds(s, end))
        assertEquals(0L, TimerEngine.remainingSeconds(s, end + 5_000))
    }

    @Test
    fun `pausing freezes the remainder and resuming extends the deadline`() {
        var s = TimerEngine.start(TimerEngine.initial(roundTarget), t0)
        s = TimerEngine.pause(s, t0 + 60_000L)
        assertTrue(s.isPaused)
        assertEquals(20 * 60L, TimerEngine.remainingSeconds(s, t0 + 60_000L))
        // Time passes while paused; nothing changes.
        assertEquals(20 * 60L, TimerEngine.remainingSeconds(s, t0 + 600_000L))
        s = TimerEngine.resume(s, t0 + 600_000L)
        assertTrue(s.isRunning)
        assertEquals(t0 + 600_000L + 20 * 60_000L, s.endAtMillis)
        assertEquals(20 * 60L - 5, TimerEngine.remainingSeconds(s, t0 + 605_000L))
    }

    @Test
    fun `toggle pause alternates`() {
        var s = TimerEngine.start(TimerEngine.initial(roundTarget), t0)
        s = TimerEngine.togglePause(s, t0 + 1_000)
        assertTrue(s.isPaused)
        s = TimerEngine.togglePause(s, t0 + 2_000)
        assertTrue(s.isRunning)
    }

    @Test
    fun `tick before the deadline changes nothing`() {
        val s = TimerEngine.start(TimerEngine.initial(roundTarget), t0)
        val tick = TimerEngine.tick(s, s.endAtMillis - 1)
        assertFalse(tick.phaseCompleted)
        assertEquals(s, tick.state)
    }

    @Test
    fun `practice completing hands over to an idle feedback phase`() {
        val s = TimerEngine.start(TimerEngine.initial(roundTarget), t0)
        val tick = TimerEngine.tick(s, s.endAtMillis)
        assertTrue(tick.phaseCompleted)
        assertEquals(Phase.FEEDBACK, tick.state.phase)
        assertEquals(RunState.IDLE, tick.state.runState)
        assertEquals(7 * 60L, TimerEngine.remainingSeconds(tick.state, s.endAtMillis + 30_000L))
    }

    @Test
    fun `feedback completing finishes the round`() {
        var s = TimerEngine.start(TimerEngine.initial(roundTarget), t0)
        s = TimerEngine.tick(s, s.endAtMillis).state
        s = TimerEngine.start(s, t0 + 30 * 60_000L)
        val tick = TimerEngine.tick(s, s.endAtMillis + 10)
        assertTrue(tick.phaseCompleted)
        assertEquals(Phase.FINISHED, tick.state.phase)
        assertEquals(0L, TimerEngine.remainingSeconds(tick.state, s.endAtMillis + 10))
    }

    @Test
    fun `a round without feedback finishes straight after practice`() {
        val target = roundTarget.copy(feedbackMinutes = 0)
        val s = TimerEngine.start(TimerEngine.initial(target), t0)
        assertEquals(Phase.FINISHED, TimerEngine.tick(s, s.endAtMillis).state.phase)
    }

    @Test
    fun `break completing finishes`() {
        val s = TimerEngine.start(TimerEngine.initial(breakTarget), t0)
        assertEquals(Phase.FINISHED, TimerEngine.tick(s, s.endAtMillis).state.phase)
    }

    @Test
    fun `skip ends the current phase early`() {
        var s = TimerEngine.start(TimerEngine.initial(roundTarget), t0)
        s = TimerEngine.skip(s)
        assertEquals(Phase.FEEDBACK, s.phase)
        assertEquals(RunState.IDLE, s.runState)
        s = TimerEngine.skip(s)
        assertEquals(Phase.FINISHED, s.phase)
        assertEquals(s, TimerEngine.skip(s))
    }

    @Test
    fun `start is ignored unless idle`() {
        val running = TimerEngine.start(TimerEngine.initial(roundTarget), t0)
        assertEquals(running, TimerEngine.start(running, t0 + 5_000))
        val finished = TimerEngine.skip(TimerEngine.skip(running))
        assertEquals(finished, TimerEngine.start(finished, t0 + 5_000))
    }
}
