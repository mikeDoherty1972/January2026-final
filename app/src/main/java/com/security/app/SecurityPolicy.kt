package com.security.app

/**
 * Pure policy functions for security alarm enabling logic.
 * This is intentionally a pure Kotlin file so it can be unit-tested without Android framework.
 */
object SecurityPolicy {
    /**
     * Decide whether security alarms should trigger based on the following precedence:
     * 1) forceOff -> false
     * 2) forceOn -> true
     * 3) presence: if not at home -> true
     * 4) enabled flag + schedule -> true/false
     *
     * scheduleStartMinutes and scheduleEndMinutes are minutes-since-midnight values; if either is null, schedule is treated as always enabled.
     */
    fun shouldTriggerSecurityAlarms(
        enabled: Boolean,
        forceOn: Boolean,
        forceOff: Boolean,
        isAtHome: Boolean,
        scheduleStartMinutes: Int?,
        scheduleEndMinutes: Int?
    ): Boolean {
        if (forceOff) return false
        if (forceOn) return true
        if (!enabled) return false
        // presence-based override: not at home => enable
        if (!isAtHome) return true

        // schedule
        if (scheduleStartMinutes == null || scheduleEndMinutes == null) return true
        val now = java.util.Calendar.getInstance()
        val nowMinutes = now.get(java.util.Calendar.HOUR_OF_DAY) * 60 + now.get(java.util.Calendar.MINUTE)

        val start = scheduleStartMinutes
        val end = scheduleEndMinutes
        return if (start <= end) {
            nowMinutes in start..end
        } else {
            // wraps past midnight
            nowMinutes >= start || nowMinutes <= end
        }
    }
}

