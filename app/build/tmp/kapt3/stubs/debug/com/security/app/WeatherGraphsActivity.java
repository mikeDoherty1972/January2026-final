package com.security.app;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000H\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0004\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\b\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\u0012\u0010\n\u001a\u00020\u000b2\b\u0010\f\u001a\u0004\u0018\u00010\rH\u0014J\u0010\u0010\u000e\u001a\u00020\u000f2\u0006\u0010\u0010\u001a\u00020\u0011H\u0002J\b\u0010\u0012\u001a\u00020\u000bH\u0002J\b\u0010\u0013\u001a\u00020\u000bH\u0002J\u0016\u0010\u0014\u001a\u00020\u000b2\f\u0010\u0015\u001a\b\u0012\u0004\u0012\u00020\u00170\u0016H\u0002J\u0016\u0010\u0018\u001a\u00020\u000b2\f\u0010\u0019\u001a\b\u0012\u0004\u0012\u00020\u001a0\u0016H\u0002J$\u0010\u001b\u001a\u00020\u000b2\f\u0010\u001c\u001a\b\u0012\u0004\u0012\u00020\u001a0\u00162\f\u0010\u001d\u001a\b\u0012\u0004\u0012\u00020\u001a0\u0016H\u0002J\u0016\u0010\u001e\u001a\u00020\u000b2\f\u0010\u001f\u001a\b\u0012\u0004\u0012\u00020\u001a0\u0016H\u0002J\u0016\u0010 \u001a\u00020\u000b2\f\u0010!\u001a\b\u0012\u0004\u0012\u00020\u001a0\u0016H\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\u0006X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\u0006X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\u0006X\u0082.\u00a2\u0006\u0002\n\u0000\u00a8\u0006\""}, d2 = {"Lcom/security/app/WeatherGraphsActivity;", "Lcom/security/app/BaseActivity;", "()V", "dateTitle", "Landroid/widget/TextView;", "humidityChart", "Lcom/github/mikephil/charting/charts/LineChart;", "temperatureChart", "windChart", "windDirectionChart", "onCreate", "", "savedInstanceState", "Landroid/os/Bundle;", "parseTimeToMinutes", "", "timestamp", "", "setupCharts", "showEmptyCharts", "updateCharts", "readings", "", "Lcom/security/app/GoogleSheetsReader$SensorReading;", "updateHumidityChart", "humidityEntries", "Lcom/github/mikephil/charting/data/Entry;", "updateTemperatureChart", "indoorEntries", "outdoorEntries", "updateWindChart", "windEntries", "updateWindDirectionChart", "dirEntries", "app_debug"})
public final class WeatherGraphsActivity extends com.security.app.BaseActivity {
    private com.github.mikephil.charting.charts.LineChart temperatureChart;
    private com.github.mikephil.charting.charts.LineChart humidityChart;
    private com.github.mikephil.charting.charts.LineChart windChart;
    private com.github.mikephil.charting.charts.LineChart windDirectionChart;
    private android.widget.TextView dateTitle;
    
    public WeatherGraphsActivity() {
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
    
    private final void updateTemperatureChart(java.util.List<? extends com.github.mikephil.charting.data.Entry> indoorEntries, java.util.List<? extends com.github.mikephil.charting.data.Entry> outdoorEntries) {
    }
    
    private final void updateHumidityChart(java.util.List<? extends com.github.mikephil.charting.data.Entry> humidityEntries) {
    }
    
    private final void updateWindChart(java.util.List<? extends com.github.mikephil.charting.data.Entry> windEntries) {
    }
    
    private final void updateWindDirectionChart(java.util.List<? extends com.github.mikephil.charting.data.Entry> dirEntries) {
    }
    
    private final void showEmptyCharts() {
    }
    
    private final int parseTimeToMinutes(java.lang.String timestamp) {
        return 0;
    }
}