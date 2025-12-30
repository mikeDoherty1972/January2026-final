package com.security.app;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000b\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0010\u000b\n\u0002\b\u0007\n\u0002\u0010\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u000e\n\u0002\b\f\n\u0002\u0010$\n\u0002\u0010\u0000\n\u0002\b\b\u0018\u00002\u00020\u0001:\u0001>B\u0005\u00a2\u0006\u0002\u0010\u0002J\b\u0010\u001c\u001a\u00020\u001dH\u0002J\b\u0010\u001e\u001a\u00020\u001dH\u0002J\b\u0010\u001f\u001a\u00020\u001dH\u0002J\b\u0010 \u001a\u00020\u001dH\u0002J\u0012\u0010!\u001a\u00020\u001d2\b\u0010\"\u001a\u0004\u0018\u00010#H\u0014J\b\u0010$\u001a\u00020\u001dH\u0014J\b\u0010%\u001a\u00020\u001dH\u0014J\b\u0010&\u001a\u00020\u001dH\u0002J\u0010\u0010\'\u001a\u00020\u001d2\u0006\u0010(\u001a\u00020)H\u0002J\b\u0010*\u001a\u00020\u001dH\u0002J\u0018\u0010+\u001a\u00020\u001d2\u0006\u0010,\u001a\u00020)2\u0006\u0010-\u001a\u00020)H\u0002J\u0010\u0010.\u001a\u00020\u001d2\u0006\u0010-\u001a\u00020)H\u0002J\b\u0010/\u001a\u00020\u001dH\u0002J\b\u00100\u001a\u00020\u001dH\u0002J\b\u00101\u001a\u00020\u001dH\u0002J\b\u00102\u001a\u00020\u001dH\u0002J\b\u00103\u001a\u00020\u001dH\u0002J\u001e\u00104\u001a\u00020\u001d2\u0014\u00105\u001a\u0010\u0012\u0004\u0012\u00020)\u0012\u0004\u0012\u000207\u0018\u000106H\u0002J\u001c\u00108\u001a\u00020\u001d2\u0012\u00109\u001a\u000e\u0012\u0004\u0012\u00020)\u0012\u0004\u0012\u00020706H\u0002J\u001c\u0010:\u001a\u00020\u001d2\u0012\u00109\u001a\u000e\u0012\u0004\u0012\u00020)\u0012\u0004\u0012\u00020706H\u0002J\u001c\u0010;\u001a\u00020\u001d2\u0012\u00109\u001a\u000e\u0012\u0004\u0012\u00020)\u0012\u0004\u0012\u00020706H\u0002J\u001c\u0010<\u001a\u00020\u001d2\u0012\u00109\u001a\u000e\u0012\u0004\u0012\u00020)\u0012\u0004\u0012\u00020706H\u0002J\u001e\u0010=\u001a\u00020\u001d2\u0014\u00109\u001a\u0010\u0012\u0004\u0012\u00020)\u0012\u0004\u0012\u000207\u0018\u000106H\u0002R\u0010\u0010\u0003\u001a\u0004\u0018\u00010\u0004X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0005\u001a\u0004\u0018\u00010\u0006X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0007\u001a\u0004\u0018\u00010\u0006X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\b\u001a\u0004\u0018\u00010\tX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\n\u001a\u0004\u0018\u00010\u0004X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000b\u001a\u00020\fX\u0082.\u00a2\u0006\u0002\n\u0000R\u0010\u0010\r\u001a\u0004\u0018\u00010\u000eX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000f\u001a\u00020\u0010X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0011\u001a\u00020\u0004X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0012\u001a\u00020\u0010X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0013\u001a\u00020\u0004X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0014\u001a\u00020\u0015X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0016\u001a\u00020\u0010X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0017\u001a\u00020\u0004X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0018\u001a\u00020\u0010X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0019\u001a\u00020\u0004X\u0082.\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u001a\u001a\u0004\u0018\u00010\u000eX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u001b\u001a\u00020\u0004X\u0082.\u00a2\u0006\u0002\n\u0000\u00a8\u0006?"}, d2 = {"Lcom/security/app/MainActivity;", "Lcom/security/app/BaseActivity;", "()V", "dashboardLightsDetails", "Landroid/widget/TextView;", "dashboardLightsOffButton", "Landroid/widget/Button;", "dashboardLightsOnButton", "dashboardLightsStatusIcon", "Landroid/widget/ImageView;", "dashboardLightsStatusText", "db", "Lcom/google/firebase/firestore/FirebaseFirestore;", "gaugeListener", "Lcom/google/firebase/firestore/ListenerRegistration;", "idsCard", "Landroidx/cardview/widget/CardView;", "idsStatus", "iperlCard", "iperlStatus", "isUiInitialized", "", "scadaCard", "scadaStatus", "securityCard", "securityStatus", "sensorListener", "timestampText", "applyCardColors", "", "createNotificationChannel", "getFCMToken", "listenForSensorUpdates", "onCreate", "savedInstanceState", "Landroid/os/Bundle;", "onDestroy", "onResume", "refreshData", "saveTokenToFirestore", "token", "", "setupNavigation", "showColorPicker", "prefKey", "dialogTitle", "showColorPickerForAllCards", "showSettingsDialog", "testIDSAlarm", "testIPERLAlarm", "testSCADAAlarm", "testSecurityAlarm", "updateGaugeUI", "gaugeData", "", "", "updateIDSAlarms", "data", "updateIPERLAlarms", "updateSCADAAlarms", "updateStatusBlocks", "updateUI", "ColorAdapter", "app_debug"})
public final class MainActivity extends com.security.app.BaseActivity {
    private com.google.firebase.firestore.FirebaseFirestore db;
    private android.widget.TextView timestampText;
    private android.widget.TextView securityStatus;
    private android.widget.TextView scadaStatus;
    private android.widget.TextView idsStatus;
    private android.widget.TextView iperlStatus;
    private androidx.cardview.widget.CardView securityCard;
    private androidx.cardview.widget.CardView scadaCard;
    private androidx.cardview.widget.CardView idsCard;
    private androidx.cardview.widget.CardView iperlCard;
    @org.jetbrains.annotations.Nullable()
    private android.widget.Button dashboardLightsOnButton;
    @org.jetbrains.annotations.Nullable()
    private android.widget.Button dashboardLightsOffButton;
    @org.jetbrains.annotations.Nullable()
    private android.widget.TextView dashboardLightsStatusText;
    @org.jetbrains.annotations.Nullable()
    private android.widget.ImageView dashboardLightsStatusIcon;
    @org.jetbrains.annotations.Nullable()
    private android.widget.TextView dashboardLightsDetails;
    @org.jetbrains.annotations.Nullable()
    private com.google.firebase.firestore.ListenerRegistration sensorListener;
    @org.jetbrains.annotations.Nullable()
    private com.google.firebase.firestore.ListenerRegistration gaugeListener;
    private boolean isUiInitialized = false;
    
    public MainActivity() {
        super();
    }
    
    @java.lang.Override()
    protected void onCreate(@org.jetbrains.annotations.Nullable()
    android.os.Bundle savedInstanceState) {
    }
    
    @java.lang.Override()
    protected void onResume() {
    }
    
    private final void applyCardColors() {
    }
    
    private final void refreshData() {
    }
    
    private final void showColorPickerForAllCards(java.lang.String dialogTitle) {
    }
    
    private final void showSettingsDialog() {
    }
    
    private final void showColorPicker(java.lang.String prefKey, java.lang.String dialogTitle) {
    }
    
    private final void createNotificationChannel() {
    }
    
    private final void getFCMToken() {
    }
    
    private final void saveTokenToFirestore(java.lang.String token) {
    }
    
    private final void listenForSensorUpdates() {
    }
    
    private final void updateUI(java.util.Map<java.lang.String, ? extends java.lang.Object> data) {
    }
    
    private final void updateGaugeUI(java.util.Map<java.lang.String, ? extends java.lang.Object> gaugeData) {
    }
    
    private final void updateStatusBlocks(java.util.Map<java.lang.String, ? extends java.lang.Object> data) {
    }
    
    private final void updateSCADAAlarms(java.util.Map<java.lang.String, ? extends java.lang.Object> data) {
    }
    
    private final void updateIDSAlarms(java.util.Map<java.lang.String, ? extends java.lang.Object> data) {
    }
    
    private final void updateIPERLAlarms(java.util.Map<java.lang.String, ? extends java.lang.Object> data) {
    }
    
    private final void setupNavigation() {
    }
    
    private final void testSecurityAlarm() {
    }
    
    private final void testSCADAAlarm() {
    }
    
    private final void testIDSAlarm() {
    }
    
    private final void testIPERLAlarm() {
    }
    
    @java.lang.Override()
    protected void onDestroy() {
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0011\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\b\u0082\u0004\u0018\u00002\b\u0012\u0004\u0012\u00020\u00020\u0001B)\u0012\u0006\u0010\u0003\u001a\u00020\u0004\u0012\f\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00020\u0006\u0012\f\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\u00020\u0006\u00a2\u0006\u0002\u0010\bJ\"\u0010\n\u001a\u00020\u000b2\u0006\u0010\f\u001a\u00020\r2\b\u0010\u000e\u001a\u0004\u0018\u00010\u000b2\u0006\u0010\u000f\u001a\u00020\u0010H\u0016R\u0016\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\u00020\u0006X\u0082\u0004\u00a2\u0006\u0004\n\u0002\u0010\tR\u0016\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00020\u0006X\u0082\u0004\u00a2\u0006\u0004\n\u0002\u0010\t\u00a8\u0006\u0011"}, d2 = {"Lcom/security/app/MainActivity$ColorAdapter;", "Landroid/widget/ArrayAdapter;", "", "context", "Landroid/content/Context;", "colors", "", "colorNames", "(Lcom/security/app/MainActivity;Landroid/content/Context;[Ljava/lang/String;[Ljava/lang/String;)V", "[Ljava/lang/String;", "getView", "Landroid/view/View;", "position", "", "convertView", "parent", "Landroid/view/ViewGroup;", "app_debug"})
    final class ColorAdapter extends android.widget.ArrayAdapter<java.lang.String> {
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String[] colors = null;
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String[] colorNames = null;
        
        public ColorAdapter(@org.jetbrains.annotations.NotNull()
        android.content.Context context, @org.jetbrains.annotations.NotNull()
        java.lang.String[] colors, @org.jetbrains.annotations.NotNull()
        java.lang.String[] colorNames) {
            super(null, 0);
        }
        
        @java.lang.Override()
        @org.jetbrains.annotations.NotNull()
        public android.view.View getView(int position, @org.jetbrains.annotations.Nullable()
        android.view.View convertView, @org.jetbrains.annotations.NotNull()
        android.view.ViewGroup parent) {
            return null;
        }
    }
}