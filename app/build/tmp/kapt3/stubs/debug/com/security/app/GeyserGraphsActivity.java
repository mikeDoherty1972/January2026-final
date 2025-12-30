package com.security.app;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000J\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0004\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\u0012\u0010\b\u001a\u00020\t2\b\u0010\n\u001a\u0004\u0018\u00010\u000bH\u0014J\b\u0010\f\u001a\u00020\tH\u0014J\u0010\u0010\r\u001a\u00020\u000e2\u0006\u0010\u000f\u001a\u00020\u0010H\u0002J\b\u0010\u0011\u001a\u00020\tH\u0002J\b\u0010\u0012\u001a\u00020\tH\u0002J\u0016\u0010\u0013\u001a\u00020\t2\f\u0010\u0014\u001a\b\u0012\u0004\u0012\u00020\u00160\u0015H\u0002J$\u0010\u0017\u001a\u00020\t2\f\u0010\u0018\u001a\b\u0012\u0004\u0012\u00020\u00190\u00152\f\u0010\u001a\u001a\b\u0012\u0004\u0012\u00020\u00190\u0015H\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\u0006X\u0082.\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u001b"}, d2 = {"Lcom/security/app/GeyserGraphsActivity;", "Lcom/security/app/BaseActivity;", "()V", "dateTitle", "Landroid/widget/TextView;", "geyserTempChart", "Lcom/github/mikephil/charting/charts/LineChart;", "pressureChart", "onCreate", "", "savedInstanceState", "Landroid/os/Bundle;", "onResume", "parseTimeToMinutes", "", "timestamp", "", "setupCharts", "showEmptyCharts", "updateCharts", "readings", "", "Lcom/security/app/GoogleSheetsReader$SensorReading;", "updateGeyserCharts", "tempEntries", "Lcom/github/mikephil/charting/data/Entry;", "pressureEntries", "app_debug"})
public final class GeyserGraphsActivity extends com.security.app.BaseActivity {
    private com.github.mikephil.charting.charts.LineChart geyserTempChart;
    private com.github.mikephil.charting.charts.LineChart pressureChart;
    private android.widget.TextView dateTitle;
    
    public GeyserGraphsActivity() {
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
    
    private final void updateGeyserCharts(java.util.List<? extends com.github.mikephil.charting.data.Entry> tempEntries, java.util.List<? extends com.github.mikephil.charting.data.Entry> pressureEntries) {
    }
    
    private final void showEmptyCharts() {
    }
    
    private final int parseTimeToMinutes(java.lang.String timestamp) {
        return 0;
    }
    
    @java.lang.Override()
    protected void onResume() {
    }
}