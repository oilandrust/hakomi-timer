package com.hakomi.practicetimer.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionPlanTest {

    @Test
    fun `default plan matches the web app defaults`() {
        val plan = SessionPlan()
        assertEquals(60, plan.totalMinutes)
        assertEquals(2, plan.rounds)
        assertEquals(0, plan.breakMinutes)
        assertEquals(3, plan.landingTime)
        assertEquals(57, plan.availableMinutes)
        // 57 / 2 = 28.5 -> floor
        assertEquals(28, plan.perRoundMinutes)
        assertNull(plan.validationError)
    }

    @Test
    fun `landing time is ignored when the option is off`() {
        val plan = SessionPlan(totalMinutes = 60, rounds = 2, landingMinutes = 3, includeLanding = false)
        assertEquals(0, plan.landingTime)
        assertEquals(60, plan.availableMinutes)
        assertEquals(30, plan.perRoundMinutes)
    }

    @Test
    fun `break and landing are subtracted before dividing by rounds`() {
        val plan = SessionPlan(totalMinutes = 120, rounds = 3, breakMinutes = 10, landingMinutes = 5)
        assertEquals(105, plan.availableMinutes)
        assertEquals(35, plan.perRoundMinutes)
    }

    @Test
    fun `remainder minutes are left as slack rather than rounded up`() {
        val plan = SessionPlan(totalMinutes = 90, rounds = 4, breakMinutes = 5, landingMinutes = 3)
        // 90 - 5 - 3 = 82 -> 82 / 4 = 20.5
        assertEquals(20, plan.perRoundMinutes)
    }

    @Test
    fun `break plus landing longer than the session is an error`() {
        val plan = SessionPlan(totalMinutes = 10, rounds = 1, breakMinutes = 8, landingMinutes = 3)
        assertEquals(0, plan.availableMinutes)
        assertEquals("Break time and landing time cannot exceed total session time", plan.validationError)
    }

    @Test
    fun `zero rounds is an error`() {
        assertEquals("Rounds must be at least 1", SessionPlan(rounds = 0).validationError)
    }

    @Test
    fun `a plan that leaves no time for a round is invalid`() {
        val plan = SessionPlan(totalMinutes = 5, rounds = 4, breakMinutes = 0, landingMinutes = 3)
        assertEquals(2, plan.availableMinutes)
        assertEquals(0, plan.perRoundMinutes)
        assertTrue(plan.validationError != null)
    }

    @Test
    fun `end time is start plus total minutes`() {
        val plan = SessionPlan(totalMinutes = 90)
        assertEquals(1_000_000L + 90 * 60_000L, plan.endMillis(1_000_000L))
    }
}
