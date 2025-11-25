package com.security.app

import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class SecurityScheduleTest {

    private fun makeCal(hour: Int, min: Int): Calendar {
        val c = Calendar.getInstance()
        c.set(Calendar.HOUR_OF_DAY, hour)
        c.set(Calendar.MINUTE, min)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c
    }

    @Test
    fun testAlwaysWhenUnset() {
        val now = makeCal(12, 0)
        assertTrue(SecuritySchedule.isNowInSchedule(-1, 0, -1, 0, now))
    }

    @Test
    fun testWithinSimpleRange() {
        val now = makeCal(14, 30)
        assertTrue(SecuritySchedule.isNowInSchedule(9, 0, 18, 0, now))
        val before = makeCal(8, 59)
        assertFalse(SecuritySchedule.isNowInSchedule(9, 0, 18, 0, before))
    }

    @Test
    fun testWrapAroundRange() {
        // schedule 22:00 -> 06:00 spans midnight
        val midnightPlus = makeCal(0, 30)
        assertTrue(SecuritySchedule.isNowInSchedule(22, 0, 6, 0, midnightPlus))
        val lateEvening = makeCal(23, 15)
        assertTrue(SecuritySchedule.isNowInSchedule(22, 0, 6, 0, lateEvening))
        val midday = makeCal(12, 0)
        assertFalse(SecuritySchedule.isNowInSchedule(22, 0, 6, 0, midday))
    }

    @Test
    fun testFormatSchedule() {
        assertEquals("Always", SecuritySchedule.formatSchedule(-1, 0, -1, 0))
        assertEquals("22:00–06:00", SecuritySchedule.formatSchedule(22, 0, 6, 0))
        assertEquals("09:05–17:30", SecuritySchedule.formatSchedule(9, 5, 17, 30))
    }
}
