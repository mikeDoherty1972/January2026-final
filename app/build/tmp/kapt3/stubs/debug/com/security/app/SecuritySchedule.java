package com.security.app;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000&\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\b\n\u0002\b\u0004\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0000\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J&\u0010\u0003\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\u00062\u0006\u0010\b\u001a\u00020\u00062\u0006\u0010\t\u001a\u00020\u0006J0\u0010\n\u001a\u00020\u000b2\u0006\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\u00062\u0006\u0010\b\u001a\u00020\u00062\u0006\u0010\t\u001a\u00020\u00062\b\b\u0002\u0010\f\u001a\u00020\r\u00a8\u0006\u000e"}, d2 = {"Lcom/security/app/SecuritySchedule;", "", "()V", "formatSchedule", "", "enableHour", "", "enableMin", "disableHour", "disableMin", "isNowInSchedule", "", "now", "Ljava/util/Calendar;", "app_debug"})
public final class SecuritySchedule {
    @org.jetbrains.annotations.NotNull()
    public static final com.security.app.SecuritySchedule INSTANCE = null;
    
    private SecuritySchedule() {
        super();
    }
    
    /**
     * Returns true if the given (enableHour, enableMin) to (disableHour, disableMin) schedule
     * includes the provided time (now).
     * Hours are 0..23, minutes 0..59. If enableHour or disableHour < 0, schedule is considered unset -> always true.
     */
    public final boolean isNowInSchedule(int enableHour, int enableMin, int disableHour, int disableMin, @org.jetbrains.annotations.NotNull()
    java.util.Calendar now) {
        return false;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String formatSchedule(int enableHour, int enableMin, int disableHour, int disableMin) {
        return null;
    }
}