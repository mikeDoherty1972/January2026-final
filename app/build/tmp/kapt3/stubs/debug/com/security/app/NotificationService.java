package com.security.app;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0086\u0001\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\t\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0006\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010!\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0010\u0007\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\b\n\u0002\b\u0004\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0002\b\u0004\u0018\u0000 72\u00020\u0001:\u00017B\u0005\u00a2\u0006\u0002\u0010\u0002J\u0018\u0010\u0017\u001a\u00020\u00182\u0006\u0010\u0019\u001a\u00020\f2\u0006\u0010\u001a\u001a\u00020\u001bH\u0002J\u000e\u0010\u001c\u001a\u00020\u0018H\u0082@\u00a2\u0006\u0002\u0010\u001dJ\b\u0010\u001e\u001a\u00020\u0018H\u0002J\b\u0010\u001f\u001a\u00020\u0018H\u0002J\u0010\u0010 \u001a\u00020\u00182\u0006\u0010!\u001a\u00020\"H\u0002J\u0010\u0010#\u001a\u0004\u0018\u00010$H\u0082@\u00a2\u0006\u0002\u0010\u001dJ\u0014\u0010%\u001a\u0004\u0018\u00010&2\b\u0010\'\u001a\u0004\u0018\u00010(H\u0016J\b\u0010)\u001a\u00020\u0018H\u0017J\b\u0010*\u001a\u00020\u0018H\u0016J\"\u0010+\u001a\u00020,2\b\u0010\'\u001a\u0004\u0018\u00010(2\u0006\u0010-\u001a\u00020,2\u0006\u0010.\u001a\u00020,H\u0016J,\u0010/\u001a\u00020\u00182\u0006\u00100\u001a\u0002012\u0006\u00102\u001a\u0002012\b\b\u0002\u00103\u001a\u0002042\b\b\u0002\u00105\u001a\u000204H\u0002J\b\u00106\u001a\u00020\u0018H\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0005\u001a\u0004\u0018\u00010\u0006X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0012\u0010\u000b\u001a\u0004\u0018\u00010\fX\u0082\u000e\u00a2\u0006\u0004\n\u0002\u0010\rR\u000e\u0010\u000e\u001a\u00020\bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000f\u001a\u00020\u0010X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0011\u001a\u00020\u0012X\u0082.\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0013\u001a\u0004\u0018\u00010\u0014X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0015\u001a\b\u0012\u0004\u0012\u00020\f0\u0016X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u00068"}, d2 = {"Lcom/security/app/NotificationService;", "Landroid/app/Service;", "()V", "dvrSheetsReader", "Lcom/security/app/DVRGoogleSheetsReader;", "dvrUpdateReceiver", "Landroid/content/BroadcastReceiver;", "highAmpsStartTime", "", "job", "Lkotlinx/coroutines/CompletableJob;", "lastDvrPulse", "", "Ljava/lang/Double;", "lastDvrPulseTime", "scope", "Lkotlinx/coroutines/CoroutineScope;", "sheetsReader", "Lcom/security/app/GoogleSheetsReader;", "timer", "Ljava/util/Timer;", "waterUsageHistory", "", "checkConstantWaterUsage", "", "latestWaterUsage", "varianceThreshold", "", "checkSensorData", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "createNotificationChannel", "ensureForeground", "ensureSecurityAlertChannel", "notificationManager", "Landroid/app/NotificationManager;", "loadLatestReading", "Lcom/security/app/GoogleSheetsReader$SensorReading;", "onBind", "Landroid/os/IBinder;", "intent", "Landroid/content/Intent;", "onCreate", "onDestroy", "onStartCommand", "", "flags", "startId", "sendNotification", "title", "", "message", "fullScreen", "", "audible", "startMonitoring", "Companion", "app_debug"})
public final class NotificationService extends android.app.Service {
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String ACTION_TEST_ALARMS = "com.security.app.ACTION_TEST_ALARMS";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String ACTION_SECURITY_ALARM = "com.security.app.ACTION_SECURITY_ALARM";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String ACTION_UPDATE_DVR_PULSE = "com.security.app.ACTION_UPDATE_DVR_PULSE";
    private static boolean isRunning = false;
    private static final double DEFAULT_LOW_WATER_PRESSURE = 1.0;
    private static final double DEFAULT_HIGH_AMPS = 6.0;
    private static final long DEFAULT_HIGH_AMPS_DURATION_MS = 3600000L;
    private static final float DEFAULT_WATER_VARIANCE = 0.1F;
    private static final long DEFAULT_DVR_STALE_MINUTES = 60L;
    private static final int FOREGROUND_NOTIF_ID = 1000;
    @org.jetbrains.annotations.Nullable()
    private android.content.BroadcastReceiver dvrUpdateReceiver;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.CompletableJob job = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.CoroutineScope scope = null;
    private com.security.app.GoogleSheetsReader sheetsReader;
    @org.jetbrains.annotations.NotNull()
    private final com.security.app.DVRGoogleSheetsReader dvrSheetsReader = null;
    @org.jetbrains.annotations.Nullable()
    private java.util.Timer timer;
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<java.lang.Double> waterUsageHistory = null;
    private long highAmpsStartTime = 0L;
    @org.jetbrains.annotations.Nullable()
    private java.lang.Double lastDvrPulse;
    private long lastDvrPulseTime = 0L;
    @org.jetbrains.annotations.NotNull()
    public static final com.security.app.NotificationService.Companion Companion = null;
    
    public NotificationService() {
        super();
    }
    
    @java.lang.Override()
    @android.annotation.SuppressLint(value = {"UnspecifiedRegisterReceiverFlag"})
    public void onCreate() {
    }
    
    private final java.lang.Object loadLatestReading(kotlin.coroutines.Continuation<? super com.security.app.GoogleSheetsReader.SensorReading> $completion) {
        return null;
    }
    
    @java.lang.Override()
    public int onStartCommand(@org.jetbrains.annotations.Nullable()
    android.content.Intent intent, int flags, int startId) {
        return 0;
    }
    
    private final void createNotificationChannel() {
    }
    
    private final void startMonitoring() {
    }
    
    private final void ensureForeground() {
    }
    
    private final java.lang.Object checkSensorData(kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    private final void checkConstantWaterUsage(double latestWaterUsage, float varianceThreshold) {
    }
    
    private final void ensureSecurityAlertChannel(android.app.NotificationManager notificationManager) {
    }
    
    /**
     * Send a notification.
     * - fullScreen=true: starts AlarmFullscreenActivity directly (keeps previous behavior)
     * - audible=true: use the security_alerts channel (with bundled mp3) for non-fullscreen alerts
     */
    private final void sendNotification(java.lang.String title, java.lang.String message, boolean fullScreen, boolean audible) {
    }
    
    @java.lang.Override()
    public void onDestroy() {
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public android.os.IBinder onBind(@org.jetbrains.annotations.Nullable()
    android.content.Intent intent) {
        return null;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00006\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0010\t\n\u0000\n\u0002\u0010\u0006\n\u0002\b\u0003\n\u0002\u0010\u0007\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0004\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\bX\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\nX\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000b\u001a\u00020\bX\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\f\u001a\u00020\nX\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\r\u001a\u00020\u000eX\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000f\u001a\u00020\u0010X\u0082T\u00a2\u0006\u0002\n\u0000R\u001a\u0010\u0011\u001a\u00020\u0012X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0011\u0010\u0013\"\u0004\b\u0014\u0010\u0015\u00a8\u0006\u0016"}, d2 = {"Lcom/security/app/NotificationService$Companion;", "", "()V", "ACTION_SECURITY_ALARM", "", "ACTION_TEST_ALARMS", "ACTION_UPDATE_DVR_PULSE", "DEFAULT_DVR_STALE_MINUTES", "", "DEFAULT_HIGH_AMPS", "", "DEFAULT_HIGH_AMPS_DURATION_MS", "DEFAULT_LOW_WATER_PRESSURE", "DEFAULT_WATER_VARIANCE", "", "FOREGROUND_NOTIF_ID", "", "isRunning", "", "()Z", "setRunning", "(Z)V", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
        
        public final boolean isRunning() {
            return false;
        }
        
        public final void setRunning(boolean p0) {
        }
    }
}