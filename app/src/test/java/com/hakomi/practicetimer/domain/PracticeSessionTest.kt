package com.hakomi.practicetimer.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PracticeSessionTest {

    private val start = 1_700_000_000_000L
    private fun minutesLater(minutes: Int) = start + minutes * 60_000L

    private val defaultSession = PracticeSession(plan = SessionPlan(), startMillis = start)

    @Test
    fun `landing time is spread across rounds rounding up`() {
        assertEquals(2, defaultSession.downtimePerRound) // ceil(3 / 2)
        val four = PracticeSession(SessionPlan(rounds = 4, landingMinutes = 5), start)
        assertEquals(2, four.downtimePerRound) // ceil(5 / 4)
        val off = PracticeSession(SessionPlan(includeLanding = false), start)
        assertEquals(0, off.downtimePerRound)
    }

    @Test
    fun `on time - at the start the round gets its planned share`() {
        // 60 total, 2 rounds, 3 landing -> remaining 60, floor(60/2) - 2 = 28
        val d = defaultSession.distribution(start)
        assertEquals(60, d.remainingMinutes)
        assertEquals(28, d.roundMinutes)
        assertEquals(minutesLater(60), d.sessionEndMillis)
    }

    @Test
    fun `on time - lost minutes shrink the remaining rounds`() {
        // 10 minutes late before the first round starts: floor(50/2) - 2 = 23
        val d = defaultSession.distribution(minutesLater(10))
        assertEquals(50, d.remainingMinutes)
        assertEquals(23, d.roundMinutes)
    }

    @Test
    fun `on time - after round one the rest of the time goes to round two`() {
        val afterRoundOne = defaultSession.markCompleted(Slot.Round(1))
        // 35 minutes elapsed: remaining 25, single round -> 25 - 2 = 23
        val d = afterRoundOne.distribution(minutesLater(35))
        assertEquals(23, d.roundMinutes)
        assertEquals(listOf(2), afterRoundOne.remainingRounds)
    }

    @Test
    fun `on time - a pending break is reserved out of the remaining time`() {
        val session = PracticeSession(SessionPlan(totalMinutes = 90, rounds = 3, breakMinutes = 10, landingMinutes = 3), start)
        // remaining 90 - break 10 = 80 -> floor(80/3) = 26 - ceil(3/3)=1 -> 25
        assertEquals(25, session.distribution(start).roundMinutes)
        val breakDone = session.markCompleted(Slot.Break)
        // remaining 90 - 0 = 90 -> 30 - 1 = 29
        assertEquals(29, breakDone.distribution(start).roundMinutes)
    }

    @Test
    fun `on time - round time never drops below one minute`() {
        val d = defaultSession.distribution(minutesLater(200))
        assertEquals(0, d.remainingMinutes)
        assertEquals(1, d.roundMinutes)
    }

    @Test
    fun `on time - elapsed time is measured in whole minutes`() {
        // 59 seconds late still counts as zero elapsed minutes
        assertEquals(28, defaultSession.distribution(start + 59_000L).roundMinutes)
        assertEquals(27, defaultSession.distribution(start + 60_000L).roundMinutes)
    }

    @Test
    fun `flexible - rounds keep their planned length and the end time slides`() {
        val session = PracticeSession(SessionPlan(pacing = Pacing.FLEXIBLE), start)
        val late = minutesLater(15)
        val d = session.distribution(late)
        assertEquals(28, d.roundMinutes)
        // 2 rounds * (28 + 2 downtime) + 0 break = 60 minutes from now
        assertEquals(60, d.remainingMinutes)
        assertEquals(late + 60 * 60_000L, d.sessionEndMillis)
    }

    @Test
    fun `flexible - remaining time shrinks as rounds and break complete`() {
        val session = PracticeSession(SessionPlan(breakMinutes = 10, pacing = Pacing.FLEXIBLE), start)
        // perRound = floor((60-10-3)/2) = 23, downtime 2 -> 2*25 + 10 = 60
        assertEquals(60, session.distribution(start).remainingMinutes)
        val one = session.markCompleted(Slot.Round(1))
        assertEquals(35, one.distribution(start).remainingMinutes)
        val done = one.markCompleted(Slot.Break)
        assertEquals(25, done.distribution(start).remainingMinutes)
    }

    @Test
    fun `next slot is the first open round then the break`() {
        val session = PracticeSession(SessionPlan(rounds = 2, breakMinutes = 5), start)
        assertEquals(Slot.Round(1), session.nextSlot)
        val one = session.markCompleted(Slot.Round(1))
        assertEquals(Slot.Round(2), one.nextSlot)
        val two = one.markCompleted(Slot.Round(2))
        assertEquals(Slot.Break, two.nextSlot)
        assertFalse(two.allDone)
        val finished = two.markCompleted(Slot.Break)
        assertNull(finished.nextSlot)
        assertTrue(finished.allDone)
    }

    @Test
    fun `sessions without a break are done once every round is complete`() {
        val session = defaultSession.markCompleted(Slot.Round(1)).markCompleted(Slot.Round(2))
        assertTrue(session.allDone)
    }

    @Test
    fun `round split honours the requested feedback and keeps a minute of practice`() {
        assertEquals(RoundSplit(21, 7), RoundSplit.of(28, 7))
        assertEquals(RoundSplit(28, 0), RoundSplit.of(28, 0))
        assertEquals(RoundSplit(1, 27), RoundSplit.of(28, 40))
        assertEquals(RoundSplit(1, 0), RoundSplit.of(1, 7))
        assertEquals(RoundSplit(5, 2), RoundSplit.of(7, 2))
        assertEquals(RoundSplit(28, 0), RoundSplit.of(28, -3))
    }
}
