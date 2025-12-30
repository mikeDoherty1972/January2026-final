package com.security.app;

/**
 * Pure policy functions for security alarm enabling logic.
 * This is intentionally a pure Kotlin file so it can be unit-tested without Android framework.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u001c\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0002\b\u0006\n\u0002\u0010\b\n\u0002\b\u0003\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002JG\u0010\u0003\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u00042\u0006\u0010\u0006\u001a\u00020\u00042\u0006\u0010\u0007\u001a\u00020\u00042\u0006\u0010\b\u001a\u00020\u00042\u0006\u0010\t\u001a\u00020\u00042\b\u0010\n\u001a\u0004\u0018\u00010\u000b2\b\u0010\f\u001a\u0004\u0018\u00010\u000b\u00a2\u0006\u0002\u0010\r\u00a8\u0006\u000e"}, d2 = {"Lcom/security/app/SecurityPolicy;", "", "()V", "shouldTriggerSecurityAlarms", "", "enabled", "forceOn", "forceOff", "isAtHome", "enableWhenAway", "scheduleStartMinutes", "", "scheduleEndMinutes", "(ZZZZZLjava/lang/Integer;Ljava/lang/Integer;)Z", "app_debug"})
public final class SecurityPolicy {
    @org.jetbrains.annotations.NotNull()
    public static final com.security.app.SecurityPolicy INSTANCE = null;
    
    private SecurityPolicy() {
        super();
    }
    
    /**
     * Decide whether security alarms should trigger based on the following precedence:
     * 1) forceOff -> false
     * 2) forceOn -> true
     * 3) enabled flag + schedule -> true/false
     *
     * scheduleStartMinutes and scheduleEndMinutes are minutes-since-midnight values; if either is null, schedule is treated as always enabled.
     */
    public final boolean shouldTriggerSecurityAlarms(boolean enabled, boolean forceOn, boolean forceOff, boolean isAtHome, boolean enableWhenAway, @org.jetbrains.annotations.Nullable()
    java.lang.Integer scheduleStartMinutes, @org.jetbrains.annotations.Nullable()
    java.lang.Integer scheduleEndMinutes) {
        return false;
    }
}