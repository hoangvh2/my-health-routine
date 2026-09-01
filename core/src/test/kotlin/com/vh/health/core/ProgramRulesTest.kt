package com.vh.health.core

import com.vh.health.core.program.ImpactLevel
import com.vh.health.core.program.ImpactPolicy
import com.vh.health.core.program.KneeLoadPolicy
import com.vh.health.core.program.KneeSignal
import com.vh.health.core.program.Phase
import com.vh.health.core.program.Progression
import com.vh.health.core.program.RunVolumeGuard
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProgramRulesTest {

    @Test
    fun `the four week block cycles adapt build peak deload`() {
        assertEquals(listOf(Phase.ADAPT, Phase.BUILD, Phase.PEAK, Phase.DELOAD), (1..4).map { Progression.phaseOf(it) })
        assertEquals(Phase.ADAPT, Progression.phaseOf(5))
        assertEquals(2, Progression.blockOf(5))
        assertEquals(1, Progression.weekWithinBlock(5))
    }

    @Test
    fun `the deload week really does take work off`() {
        assertEquals(4, Progression.scaleVolume(baseline = 4, week = 1))
        assertEquals(2, Progression.scaleVolume(baseline = 4, week = 4))
    }

    @Test
    fun `plyometrics stay out of the first two weeks and come back in week three`() {
        assertFalse(ImpactPolicy.allows(week = 1, exerciseImpact = ImpactLevel.MODERATE))
        assertFalse(ImpactPolicy.allows(week = 2, exerciseImpact = ImpactLevel.MODERATE))
        assertTrue(ImpactPolicy.allows(week = 3, exerciseImpact = ImpactLevel.MODERATE))
        assertTrue(ImpactPolicy.allows(week = 1, exerciseImpact = ImpactLevel.LOW))
    }

    @Test
    fun `a clear knee week earns the ten percent and no more`() {
        val decision = KneeLoadPolicy.decide(KneeSignal.CLEAR)
        assertEquals(1.10, decision.impactFactor, 1e-9)
        assertEquals(1.0, decision.strengthFactor, 1e-9)
    }

    @Test
    fun `an overloaded knee cuts impact but never cuts the strength work`() {
        val decision = KneeLoadPolicy.decide(KneeSignal.OVERLOADED)
        assertEquals(0.7, decision.impactFactor, 1e-9)
        assertEquals(1.0, decision.strengthFactor, 1e-9, "the weights are the treatment, not the overload")
    }

    @Test
    fun `a lingering ache holds volume steady`() {
        assertEquals(1.0, KneeLoadPolicy.decide(KneeSignal.LINGERING).impactFactor, 1e-9)
    }

    @Test
    fun `two red weeks in a row suggests seeing someone`() {
        assertTrue(KneeLoadPolicy.shouldSuggestClinician(listOf(KneeSignal.OVERLOADED, KneeSignal.OVERLOADED)))
        assertFalse(KneeLoadPolicy.shouldSuggestClinician(listOf(KneeSignal.OVERLOADED, KneeSignal.CLEAR)))
    }

    @Test
    fun `the ten percent cap holds even when the user feels strong`() {
        assertEquals(110, RunVolumeGuard.cap(previousWeekMinutes = 100, proposedMinutes = 160))
        assertEquals(90, RunVolumeGuard.cap(previousWeekMinutes = 100, proposedMinutes = 90))
        assertTrue(RunVolumeGuard.exceedsCap(previousWeekMinutes = 100, proposedMinutes = 130))
    }

    @Test
    fun `the first ever week has nothing to be capped against`() {
        assertEquals(60, RunVolumeGuard.cap(previousWeekMinutes = 0, proposedMinutes = 60))
    }

    @Test
    fun `an overloaded week comes back thirty percent lighter`() {
        val next = RunVolumeGuard.nextWeek(100, KneeLoadPolicy.decide(KneeSignal.OVERLOADED))
        assertEquals(70, next)
    }
}
