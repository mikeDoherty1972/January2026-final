package com.security.app;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000(\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0004\u0018\u0000 \u000e2\u00020\u0001:\u0001\u000eB\u0005\u00a2\u0006\u0002\u0010\u0002J\b\u0010\u0007\u001a\u00020\bH\u0016J\u0012\u0010\t\u001a\u00020\b2\b\u0010\n\u001a\u0004\u0018\u00010\u000bH\u0014J\b\u0010\f\u001a\u00020\bH\u0014J\b\u0010\r\u001a\u00020\bH\u0002R\u0010\u0010\u0003\u001a\u0004\u0018\u00010\u0004X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0005\u001a\u0004\u0018\u00010\u0006X\u0082\u000e\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u000f"}, d2 = {"Lcom/security/app/AlarmFullscreenActivity;", "Landroidx/appcompat/app/AppCompatActivity;", "()V", "mediaPlayer", "Landroid/media/MediaPlayer;", "vibrator", "Landroid/os/Vibrator;", "onBackPressed", "", "onCreate", "savedInstanceState", "Landroid/os/Bundle;", "onDestroy", "stopAlarmAndFinish", "Companion", "app_debug"})
public final class AlarmFullscreenActivity extends androidx.appcompat.app.AppCompatActivity {
    @org.jetbrains.annotations.Nullable()
    private android.media.MediaPlayer mediaPlayer;
    @org.jetbrains.annotations.Nullable()
    private android.os.Vibrator vibrator;
    @org.jetbrains.annotations.Nullable()
    private static java.lang.String lastAlarmZone;
    private static long lastAlarmTimeMs = 0L;
    public static final long MIN_RESTART_MS = 30000L;
    @org.jetbrains.annotations.NotNull()
    public static final com.security.app.AlarmFullscreenActivity.Companion Companion = null;
    
    public AlarmFullscreenActivity() {
        super();
    }
    
    @java.lang.Override()
    @kotlin.Suppress(names = {"DEPRECATION", "RedundantInitializer"})
    protected void onCreate(@org.jetbrains.annotations.Nullable()
    android.os.Bundle savedInstanceState) {
    }
    
    private final void stopAlarmAndFinish() {
    }
    
    @java.lang.Override()
    protected void onDestroy() {
    }
    
    @java.lang.Override()
    @kotlin.Suppress(names = {"MissingSuperCall", "DEPRECATION"})
    public void onBackPressed() {
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u001c\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\t\n\u0002\b\u0006\n\u0002\u0010\u000e\n\u0002\b\u0005\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u001a\u0010\u0005\u001a\u00020\u0004X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0006\u0010\u0007\"\u0004\b\b\u0010\tR\u001c\u0010\n\u001a\u0004\u0018\u00010\u000bX\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\f\u0010\r\"\u0004\b\u000e\u0010\u000f\u00a8\u0006\u0010"}, d2 = {"Lcom/security/app/AlarmFullscreenActivity$Companion;", "", "()V", "MIN_RESTART_MS", "", "lastAlarmTimeMs", "getLastAlarmTimeMs", "()J", "setLastAlarmTimeMs", "(J)V", "lastAlarmZone", "", "getLastAlarmZone", "()Ljava/lang/String;", "setLastAlarmZone", "(Ljava/lang/String;)V", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
        
        @org.jetbrains.annotations.Nullable()
        public final java.lang.String getLastAlarmZone() {
            return null;
        }
        
        public final void setLastAlarmZone(@org.jetbrains.annotations.Nullable()
        java.lang.String p0) {
        }
        
        public final long getLastAlarmTimeMs() {
            return 0L;
        }
        
        public final void setLastAlarmTimeMs(long p0) {
        }
    }
}