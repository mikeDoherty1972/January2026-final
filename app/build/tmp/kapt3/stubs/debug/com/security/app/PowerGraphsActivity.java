package com.security.app;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000H\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0004\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\u0012\u0010\t\u001a\u00020\n2\b\u0010\u000b\u001a\u0004\u0018\u00010\fH\u0014J\u0010\u0010\r\u001a\u00020\u000e2\u0006\u0010\u000f\u001a\u00020\u0010H\u0002J\b\u0010\u0011\u001a\u00020\nH\u0002J\b\u0010\u0012\u001a\u00020\nH\u0002J\u0016\u0010\u0013\u001a\u00020\n2\f\u0010\u0014\u001a\b\u0012\u0004\u0012\u00020\u00160\u0015H\u0002J2\u0010\u0017\u001a\u00020\n2\f\u0010\u0018\u001a\b\u0012\u0004\u0012\u00020\u00190\u00152\f\u0010\u001a\u001a\b\u0012\u0004\u0012\u00020\u00190\u00152\f\u0010\u001b\u001a\b\u0012\u0004\u0012\u00020\u00190\u0015H\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0004X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\bX\u0082.\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u001c"}, d2 = {"Lcom/security/app/PowerGraphsActivity;", "Lcom/security/app/BaseActivity;", "()V", "currentAmpsChart", "Lcom/github/mikephil/charting/charts/LineChart;", "currentPowerChart", "dailyTotalChart", "dateTitle", "Landroid/widget/TextView;", "onCreate", "", "savedInstanceState", "Landroid/os/Bundle;", "parseTimeToMinutes", "", "timestamp", "", "setupCharts", "showEmptyCharts", "updateCharts", "readings", "", "Lcom/security/app/GoogleSheetsReader$SensorReading;", "updatePowerCharts", "powerEntries", "Lcom/github/mikephil/charting/data/Entry;", "ampsEntries", "dailyEntries", "app_debug"})
public final class PowerGraphsActivity extends com.security.app.BaseActivity {
    private com.github.mikephil.charting.charts.LineChart currentPowerChart;
    private com.github.mikephil.charting.charts.LineChart currentAmpsChart;
    private com.github.mikephil.charting.charts.LineChart dailyTotalChart;
    private android.widget.TextView dateTitle;
    
    public PowerGraphsActivity() {
        super();
    }
    
    @java.lang.Override()
    protected void onCreate(@org.jetbrains.annotations.Nullable()
    android.os.Bundle savedInstanceState) {
    }
    
    private final void setupCharts() {
    }
    
    private final void updateCharts(java.util.List<com.security.app.GoogleSheetsReader.SensorReading> readings) {
    }
    
    private final void updatePowerCharts(java.util.List<? extends com.github.mikephil.charting.data.Entry> powerEntries, java.util.List<? extends com.github.mikephil.charting.data.Entry> ampsEntries, java.util.List<? extends com.github.mikephil.charting.data.Entry> dailyEntries) {
    }
    
    private final void showEmptyCharts() {
    }
    
    private final int parseTimeToMinutes(java.lang.String timestamp) {
        return 0;
    }
}