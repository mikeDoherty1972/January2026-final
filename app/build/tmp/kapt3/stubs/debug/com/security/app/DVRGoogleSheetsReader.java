package com.security.app;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000*\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010 \n\u0000\n\u0002\u0010\b\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0002\b\u0002\u0018\u0000 \u000e2\u00020\u0001:\u0001\u000eB\u0005\u00a2\u0006\u0002\u0010\u0002J\u0010\u0010\u0003\u001a\u0004\u0018\u00010\u0004H\u0086@\u00a2\u0006\u0002\u0010\u0005J\u001e\u0010\u0006\u001a\b\u0012\u0004\u0012\u00020\u00040\u00072\b\b\u0002\u0010\b\u001a\u00020\tH\u0086@\u00a2\u0006\u0002\u0010\nJ\u001e\u0010\u000b\u001a\b\u0012\u0004\u0012\u00020\u00040\u00072\u0006\u0010\f\u001a\u00020\r2\u0006\u0010\b\u001a\u00020\tH\u0002\u00a8\u0006\u000f"}, d2 = {"Lcom/security/app/DVRGoogleSheetsReader;", "", "()V", "fetchLatestDvrReading", "Lcom/security/app/GoogleSheetsReader$SensorReading;", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "fetchLatestDvrReadings", "", "maxRows", "", "(ILkotlin/coroutines/Continuation;)Ljava/lang/Object;", "parseDvrData", "csvData", "", "Companion", "app_debug"})
public final class DVRGoogleSheetsReader {
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String SHEET_ID = "1_GuECfJE0nFSGMS6-prrWAfLCR4gBPa3Qf_zzaKzAAA";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String DVR_DATA_URL = "https://docs.google.com/spreadsheets/d/1_GuECfJE0nFSGMS6-prrWAfLCR4gBPa3Qf_zzaKzAAA/export?format=csv&gid=2109322930";
    @org.jetbrains.annotations.NotNull()
    public static final com.security.app.DVRGoogleSheetsReader.Companion Companion = null;
    
    public DVRGoogleSheetsReader() {
        super();
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object fetchLatestDvrReadings(int maxRows, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.security.app.GoogleSheetsReader.SensorReading>> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object fetchLatestDvrReading(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.security.app.GoogleSheetsReader.SensorReading> $completion) {
        return null;
    }
    
    private final java.util.List<com.security.app.GoogleSheetsReader.SensorReading> parseDvrData(java.lang.String csvData, int maxRows) {
        return null;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0002\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0006"}, d2 = {"Lcom/security/app/DVRGoogleSheetsReader$Companion;", "", "()V", "DVR_DATA_URL", "", "SHEET_ID", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}