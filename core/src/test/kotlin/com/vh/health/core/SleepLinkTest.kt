package com.vh.health.core

import com.vh.health.core.schedule.SleepLink
import java.time.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals

class SleepLinkTest {

    @Test
    fun `the default pair gives exactly eight hours`() {
        assertEquals(480, SleepLink.sleepMinutes(LocalTime.of(20, 30), LocalTime.of(4, 30)))
    }

    @Test
    fun `waking at 0500 proposes a 2100 bedtime`() {
        assertEquals(LocalTime.of(21, 0), SleepLink.bedtimeFor(LocalTime.of(5, 0)))
    }

    @Test
    fun `keeping 2030 while waking at 0500 reports the extra half hour rather than moving anything`() {
        assertEquals(510, SleepLink.sleepMinutes(LocalTime.of(20, 30), LocalTime.of(5, 0)))
        assertEquals(30, SleepLink.deviationFromTarget(LocalTime.of(20, 30), LocalTime.of(5, 0)))
    }

    @Test
    fun `wake and bedtime are inverses of each other`() {
        val wake = LocalTime.of(4, 30)
        assertEquals(wake, SleepLink.wakeFor(SleepLink.bedtimeFor(wake)))
    }
}
