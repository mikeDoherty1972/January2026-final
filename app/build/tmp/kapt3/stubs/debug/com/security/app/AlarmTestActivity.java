package com.security.app;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000<\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0007\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\b\u0010\u0011\u001a\u00020\u0012H\u0002J\u0012\u0010\u0013\u001a\u00020\u00122\b\u0010\u0014\u001a\u0004\u0018\u00010\u0015H\u0014J\b\u0010\u0016\u001a\u00020\u0012H\u0002J\b\u0010\u0017\u001a\u00020\u0012H\u0002J\b\u0010\u0018\u001a\u00020\u0012H\u0002J\b\u0010\u0019\u001a\u00020\u0012H\u0002J\b\u0010\u001a\u001a\u00020\u0012H\u0002J\b\u0010\u001b\u001a\u00020\u0012H\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0004X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\u0004X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\u0004X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\nX\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000b\u001a\u00020\fX\u0082.\u00a2\u0006\u0002\n\u0000R\u0014\u0010\r\u001a\b\u0012\u0004\u0012\u00020\u000f0\u000eX\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0010\u001a\u00020\nX\u0082.\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u001c"}, d2 = {"Lcom/security/app/AlarmTestActivity;", "Lcom/security/app/BaseActivity;", "()V", "btnOpenBatterySettings", "Landroid/widget/Button;", "btnOpenNotifSettings", "btnRefreshIperl", "btnRequestNotif", "btnStartTestAlarm", "iperlStatusText", "Landroid/widget/TextView;", "progressWater", "Landroid/view/View;", "requestNotifLauncher", "Landroidx/activity/result/ActivityResultLauncher;", "", "statusText", "observeRepositoryStatus", "", "onCreate", "savedInstanceState", "Landroid/os/Bundle;", "openAppNotificationSettings", "openBatteryOptimizationSettings", "refreshIperlData", "requestNotificationPermissionIfNeeded", "triggerTestAlarm", "updateCurrentPermissionStatus", "app_debug"})
public final class AlarmTestActivity extends com.security.app.BaseActivity {
    private android.widget.Button btnRequestNotif;
    private android.widget.Button btnOpenNotifSettings;
    private android.widget.Button btnOpenBatterySettings;
    private android.widget.Button btnStartTestAlarm;
    private android.widget.Button btnRefreshIperl;
    private android.widget.TextView statusText;
    private android.widget.TextView iperlStatusText;
    private androidx.activity.result.ActivityResultLauncher<java.lang.String> requestNotifLauncher;
    private android.view.View progressWater;
    
    public AlarmTestActivity() {
        super();
    }
    
    @java.lang.Override()
    protected void onCreate(@org.jetbrains.annotations.Nullable()
    android.os.Bundle savedInstanceState) {
    }
    
    private final void updateCurrentPermissionStatus() {
    }
    
    private final void requestNotificationPermissionIfNeeded() {
    }
    
    private final void openAppNotificationSettings() {
    }
    
    private final void openBatteryOptimizationSettings() {
    }
    
    private final void triggerTestAlarm() {
    }
    
    private final void refreshIperlData() {
    }
    
    private final void observeRepositoryStatus() {
    }
}