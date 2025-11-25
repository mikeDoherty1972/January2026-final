package com.security.app

import java.util.Calendar
import java.util.Locale

object SecuritySchedule {
    /**
     * Returns true if the given (enableHour, enableMin) to (disableHour, disableMin) schedule
     * includes the provided time (now).
     * Hours are 0..23, minutes 0..59. If enableHour or disableHour < 0, schedule is considered unset -> always true.
     */
    fun isNowInSchedule(enableHour: Int, enableMin: Int, disableHour: Int, disableMin: Int, now: Calendar = Calendar.getInstance()): Boolean {
        if (enableHour < 0 || disableHour < 0) return true
        val nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val startMinutes = enableHour * 60 + enableMin
        val endMinutes = disableHour * 60 + disableMin
        return if (startMinutes <= endMinutes) {
            nowMinutes in startMinutes..endMinutes
        } else {
            // wraps past midnight
            nowMinutes >= startMinutes || nowMinutes <= endMinutes
        }
    }

    // Helper to format schedule to string e.g. "22:00–06:00" or "Always"
    fun formatSchedule(enableHour: Int, enableMin: Int, disableHour: Int, disableMin: Int): String {
        return if (enableHour < 0 || disableHour < 0) {
            "Always"
        } else String.format(Locale.US, "%02d:%02d–%02d:%02d", enableHour, enableMin, disableHour, disableMin)
    }
}
