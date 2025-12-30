package com.security.app;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u009a\u0001\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010!\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010 \n\u0002\b\u0003\n\u0002\u0010\u000b\n\u0002\b\t\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0010\u0011\n\u0000\n\u0002\u0010\u0015\n\u0002\b\u000b\n\u0002\u0010\u0000\n\u0002\b\u000f\n\u0002\u0010$\n\u0002\b\u0002\u0018\u0000 `2\u00020\u0001:\u0001`B\u0005\u00a2\u0006\u0002\u0010\u0002J\b\u0010&\u001a\u00020\'H\u0002J\u0016\u0010(\u001a\u00020\'2\f\u0010)\u001a\b\u0012\u0004\u0012\u00020\'0*H\u0002J\u001a\u0010+\u001a\u00020\'2\u0010\b\u0002\u0010,\u001a\n\u0012\u0004\u0012\u00020\'\u0018\u00010*H\u0002J\u000e\u0010-\u001a\b\u0012\u0004\u0012\u00020\u00040.H\u0002J\n\u0010/\u001a\u0004\u0018\u00010\u0004H\u0002J\n\u00100\u001a\u0004\u0018\u00010\u0004H\u0002J\u0012\u00101\u001a\u0002022\b\u00103\u001a\u0004\u0018\u00010\u0004H\u0002J\b\u00104\u001a\u000202H\u0002J\b\u00105\u001a\u000202H\u0002J\b\u00106\u001a\u000202H\u0002J\u0012\u00107\u001a\u0002022\b\u00108\u001a\u0004\u0018\u00010\u0004H\u0002J\b\u00109\u001a\u00020\'H\u0002J\u0012\u0010:\u001a\u00020\'2\b\u0010;\u001a\u0004\u0018\u00010<H\u0014J\b\u0010=\u001a\u00020\'H\u0014J-\u0010>\u001a\u00020\'2\u0006\u0010?\u001a\u00020\u00062\u000e\u0010@\u001a\n\u0012\u0006\b\u0001\u0012\u00020\u00040A2\u0006\u0010B\u001a\u00020CH\u0016\u00a2\u0006\u0002\u0010DJ\b\u0010E\u001a\u00020\'H\u0014J\u001c\u0010F\u001a\u00020\'2\b\u0010G\u001a\u0004\u0018\u00010\u00042\b\b\u0002\u0010H\u001a\u000202H\u0002J\n\u0010I\u001a\u0004\u0018\u00010\u0004H\u0002J\b\u0010J\u001a\u00020\'H\u0002J\b\u0010K\u001a\u00020\'H\u0002J\b\u0010L\u001a\u00020\'H\u0002J\u0012\u0010M\u001a\u0002022\b\u0010N\u001a\u0004\u0018\u00010OH\u0002J\b\u0010P\u001a\u00020\'H\u0002J\b\u0010Q\u001a\u000202H\u0002J\b\u0010R\u001a\u00020\'H\u0002J\u001e\u0010S\u001a\u00020\'2\u0006\u0010T\u001a\u00020\u00062\f\u0010U\u001a\b\u0012\u0004\u0012\u00020\u00040.H\u0002J\b\u0010V\u001a\u00020\'H\u0002J\b\u0010W\u001a\u00020\'H\u0002J(\u0010X\u001a\u00020\'2\b\u0010Y\u001a\u0004\u0018\u00010\u00112\b\u0010Z\u001a\u0004\u0018\u00010\u00112\n\b\u0002\u0010[\u001a\u0004\u0018\u00010\u0011H\u0002J\b\u0010\\\u001a\u00020\'H\u0002J\u001c\u0010]\u001a\u00020\'2\u0012\u0010^\u001a\u000e\u0012\u0004\u0012\u00020\u0004\u0012\u0004\u0012\u00020O0_H\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082D\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082D\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\bX\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\nX\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000b\u001a\u00020\bX\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\f\u001a\u00020\bX\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\r\u001a\u00020\bX\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000e\u001a\u00020\bX\u0082.\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u000f\u001a\u0004\u0018\u00010\u0004X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0010\u001a\u0004\u0018\u00010\u0011X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0012\u001a\u0004\u0018\u00010\u0013X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0014\u001a\u0004\u0018\u00010\bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0015\u001a\u0004\u0018\u00010\u0011X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0016\u001a\u0004\u0018\u00010\u0011X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0017\u001a\u00020\bX\u0082.\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0018\u001a\b\u0012\u0004\u0012\u00020\u00040\u0019X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u001a\u001a\u00020\bX\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u001b\u001a\u00020\u001cX\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u001d\u001a\u00020\u001cX\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u001e\u001a\u00020\u001cX\u0082.\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u001f\u001a\u0004\u0018\u00010 X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010!\u001a\u00020\bX\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\"\u001a\u00020\bX\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010#\u001a\u00020$X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010%\u001a\u00020\bX\u0082.\u00a2\u0006\u0002\n\u0000\u00a8\u0006a"}, d2 = {"Lcom/security/app/SecurityActivity;", "Lcom/security/app/BaseActivity;", "()V", "CHANNEL_ID", "", "NOTIF_ID", "", "backStatus", "Landroid/widget/TextView;", "db", "Lcom/google/firebase/firestore/FirebaseFirestore;", "doorStatus", "frontStatus", "garageSideStatus", "garageStatus", "lastKnownSsid", "networkBridgeDot", "Landroid/widget/ImageView;", "networkCallback", "Landroid/net/ConnectivityManager$NetworkCallback;", "networkDebugText", "networkHomeDot", "networkHomeDotInline", "northStatus", "recentActivityCache", "", "recentActivityText", "securityAlarmsSwitch", "Landroidx/appcompat/widget/SwitchCompat;", "securityForceOffSwitch", "securityForceSwitch", "securityListener", "Lcom/google/firebase/firestore/ListenerRegistration;", "securityScheduleSummary", "southStatus", "systemStatusCard", "Landroidx/cardview/widget/CardView;", "systemStatusText", "createNotificationChannel", "", "ensureLocationPermissionForSsid", "onGranted", "Lkotlin/Function0;", "fetchAndCacheSsidOnce", "onComplete", "filteredSecurityActivityAll", "", "getCurrentSsid", "getGatewayIp", "is172PrivateRange", "", "ip", "isActiveNetworkWifi", "isNetworkAvailable", "isOnHomeNetwork", "isSecurityLine", "line", "listenForSecurityUpdates", "onCreate", "savedInstanceState", "Landroid/os/Bundle;", "onDestroy", "onRequestPermissionsResult", "requestCode", "permissions", "", "grantResults", "", "(I[Ljava/lang/String;[I)V", "onResume", "openImageView", "imageUrl", "forceReload", "readSsidFromWifiManager", "renderRecentActivityPreview", "requestNotificationPermissionIfNeeded", "saveCurrentSsidAsHome", "sensorValueToBoolean", "value", "", "setupCameraClickListeners", "shouldTriggerSecurityAlarms", "showPresenceDialog", "showSecurityNotification", "activeZones", "recent", "startSsidMonitoring", "stopSsidMonitoring", "updateNetworkIndicators", "homeDot", "bridgeDot", "inlineDot", "updateScheduleUi", "updateSecurityDisplay", "data", "", "Companion", "app_debug"})
public final class SecurityActivity extends com.security.app.BaseActivity {
    private com.google.firebase.firestore.FirebaseFirestore db;
    private android.widget.TextView systemStatusText;
    private android.widget.TextView garageStatus;
    private android.widget.TextView garageSideStatus;
    private android.widget.TextView southStatus;
    private android.widget.TextView backStatus;
    private android.widget.TextView northStatus;
    private android.widget.TextView frontStatus;
    private android.widget.TextView doorStatus;
    private androidx.appcompat.widget.SwitchCompat securityAlarmsSwitch;
    private android.widget.TextView securityScheduleSummary;
    private androidx.cardview.widget.CardView systemStatusCard;
    private androidx.appcompat.widget.SwitchCompat securityForceSwitch;
    private androidx.appcompat.widget.SwitchCompat securityForceOffSwitch;
    private android.widget.TextView recentActivityText;
    @org.jetbrains.annotations.Nullable()
    private android.widget.ImageView networkHomeDot;
    @org.jetbrains.annotations.Nullable()
    private android.widget.ImageView networkBridgeDot;
    @org.jetbrains.annotations.Nullable()
    private android.widget.ImageView networkHomeDotInline;
    @org.jetbrains.annotations.Nullable()
    private android.widget.TextView networkDebugText;
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<java.lang.String> recentActivityCache = null;
    @org.jetbrains.annotations.Nullable()
    private com.google.firebase.firestore.ListenerRegistration securityListener;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String CHANNEL_ID = "security_alerts";
    private final int NOTIF_ID = 1001;
    private static final int REQ_POST_NOTIF = 2001;
    private static final int REQ_FINE_LOCATION = 3001;
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String DEFAULT_CHANNEL_ID = "default_notifications";
    @kotlin.jvm.Volatile()
    @org.jetbrains.annotations.Nullable()
    private volatile java.lang.String lastKnownSsid;
    @org.jetbrains.annotations.Nullable()
    private android.net.ConnectivityManager.NetworkCallback networkCallback;
    @org.jetbrains.annotations.NotNull()
    public static final com.security.app.SecurityActivity.Companion Companion = null;
    
    public SecurityActivity() {
        super();
    }
    
    private final boolean isSecurityLine(java.lang.String line) {
        return false;
    }
    
    private final java.util.List<java.lang.String> filteredSecurityActivityAll() {
        return null;
    }
    
    private final void renderRecentActivityPreview() {
    }
    
    private final boolean is172PrivateRange(java.lang.String ip) {
        return false;
    }
    
    private final void fetchAndCacheSsidOnce(kotlin.jvm.functions.Function0<kotlin.Unit> onComplete) {
    }
    
    private final void ensureLocationPermissionForSsid(kotlin.jvm.functions.Function0<kotlin.Unit> onGranted) {
    }
    
    private final void startSsidMonitoring() {
    }
    
    private final void stopSsidMonitoring() {
    }
    
    private final java.lang.String readSsidFromWifiManager() {
        return null;
    }
    
    private final java.lang.String getCurrentSsid() {
        return null;
    }
    
    private final void saveCurrentSsidAsHome() {
    }
    
    @java.lang.Override()
    protected void onCreate(@org.jetbrains.annotations.Nullable()
    android.os.Bundle savedInstanceState) {
    }
    
    private final void setupCameraClickListeners() {
    }
    
    private final void openImageView(java.lang.String imageUrl, boolean forceReload) {
    }
    
    private final void requestNotificationPermissionIfNeeded() {
    }
    
    @java.lang.Override()
    public void onRequestPermissionsResult(int requestCode, @org.jetbrains.annotations.NotNull()
    java.lang.String[] permissions, @org.jetbrains.annotations.NotNull()
    int[] grantResults) {
    }
    
    private final void listenForSecurityUpdates() {
    }
    
    private final void updateSecurityDisplay(java.util.Map<java.lang.String, ? extends java.lang.Object> data) {
    }
    
    private final boolean sensorValueToBoolean(java.lang.Object value) {
        return false;
    }
    
    private final void createNotificationChannel() {
    }
    
    private final void showSecurityNotification(int activeZones, java.util.List<java.lang.String> recent) {
    }
    
    @java.lang.Override()
    protected void onDestroy() {
    }
    
    private final boolean shouldTriggerSecurityAlarms() {
        return false;
    }
    
    private final java.lang.String getGatewayIp() {
        return null;
    }
    
    private final boolean isActiveNetworkWifi() {
        return false;
    }
    
    private final boolean isOnHomeNetwork() {
        return false;
    }
    
    private final boolean isNetworkAvailable() {
        return false;
    }
    
    private final void updateNetworkIndicators(android.widget.ImageView homeDot, android.widget.ImageView bridgeDot, android.widget.ImageView inlineDot) {
    }
    
    private final void showPresenceDialog() {
    }
    
    private final void updateScheduleUi() {
    }
    
    @java.lang.Override()
    protected void onResume() {
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u001a\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\b\n\u0002\b\u0002\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\u0006X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\b"}, d2 = {"Lcom/security/app/SecurityActivity$Companion;", "", "()V", "DEFAULT_CHANNEL_ID", "", "REQ_FINE_LOCATION", "", "REQ_POST_NOTIF", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}