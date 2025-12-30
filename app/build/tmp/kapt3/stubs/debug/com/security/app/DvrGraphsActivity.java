package com.security.app;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000:\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\u0012\u0010\u000b\u001a\u00020\f2\b\u0010\r\u001a\u0004\u0018\u00010\u000eH\u0014J\b\u0010\u000f\u001a\u00020\fH\u0002J\u0016\u0010\u0010\u001a\u00020\f2\f\u0010\u0011\u001a\b\u0012\u0004\u0012\u00020\u00130\u0012H\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\u0004X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\nX\u0082.\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0014"}, d2 = {"Lcom/security/app/DvrGraphsActivity;", "Lcom/security/app/BaseActivity;", "()V", "currentDvrStatus", "Landroid/widget/TextView;", "dateTitle", "dvrRawCsvCard", "Landroid/view/View;", "dvrRawCsvPreview", "dvrTempChart", "Lcom/github/mikephil/charting/charts/LineChart;", "onCreate", "", "savedInstanceState", "Landroid/os/Bundle;", "setupChart", "updateChart", "readings", "", "Lcom/security/app/GoogleSheetsReader$SensorReading;", "app_debug"})
public final class DvrGraphsActivity extends com.security.app.BaseActivity {
    private com.github.mikephil.charting.charts.LineChart dvrTempChart;
    private android.widget.TextView currentDvrStatus;
    private android.widget.TextView dateTitle;
    private android.widget.TextView dvrRawCsvPreview;
    private android.view.View dvrRawCsvCard;
    
    public DvrGraphsActivity() {
        super();
    }
    
    @java.lang.Override()
    protected void onCreate(@org.jetbrains.annotations.Nullable()
    android.os.Bundle savedInstanceState) {
    }
    
    private final void setupChart() {
    }
    
    private final void updateChart(java.util.List<com.security.app.GoogleSheetsReader.SensorReading> readings) {
    }
}