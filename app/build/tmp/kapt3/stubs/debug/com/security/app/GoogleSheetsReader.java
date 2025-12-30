package com.security.app;

/**
 * Utility class to read sensor data from Google Sheets
 * Uses CSV export format: /export?format=csv&gid=0
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000.\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u000e\n\u0002\u0010\u0007\n\u0002\b\u0005\u0018\u0000 \u001c2\u00020\u0001:\u0002\u001c\u001dB\u0005\u00a2\u0006\u0002\u0010\u0002J\u001e\u0010\u0003\u001a\b\u0012\u0004\u0012\u00020\u00050\u00042\b\b\u0002\u0010\u0006\u001a\u00020\u0007H\u0086@\u00a2\u0006\u0002\u0010\bJ\u0018\u0010\t\u001a\u0004\u0018\u00010\n2\u0006\u0010\u000b\u001a\u00020\u0007H\u0086@\u00a2\u0006\u0002\u0010\bJ&\u0010\f\u001a\b\u0012\u0004\u0012\u00020\u00050\u00042\u0006\u0010\r\u001a\u00020\n2\b\b\u0002\u0010\u0006\u001a\u00020\u0007H\u0086@\u00a2\u0006\u0002\u0010\u000eJ\u001e\u0010\u000f\u001a\b\u0012\u0004\u0012\u00020\u00050\u00042\b\b\u0002\u0010\u0010\u001a\u00020\u0007H\u0086@\u00a2\u0006\u0002\u0010\bJ0\u0010\u0011\u001a\b\u0012\u0004\u0012\u00020\u00050\u00042\u0006\u0010\u000b\u001a\u00020\u00072\b\b\u0002\u0010\u0010\u001a\u00020\u00072\b\b\u0002\u0010\u0006\u001a\u00020\u0007H\u0086@\u00a2\u0006\u0002\u0010\u0012J\u001e\u0010\u0013\u001a\b\u0012\u0004\u0012\u00020\u00050\u00042\u0006\u0010\u0014\u001a\u00020\n2\u0006\u0010\u0006\u001a\u00020\u0007H\u0002J\u0016\u0010\u0015\u001a\b\u0012\u0004\u0012\u00020\n0\u00042\u0006\u0010\u0016\u001a\u00020\nH\u0002J\u001e\u0010\u0017\u001a\b\u0012\u0004\u0012\u00020\u00050\u00042\u0006\u0010\u0014\u001a\u00020\n2\b\b\u0002\u0010\u0006\u001a\u00020\u0007J\u0010\u0010\u0018\u001a\u00020\u00192\u0006\u0010\u001a\u001a\u00020\nH\u0002J\u001e\u0010\u001b\u001a\b\u0012\u0004\u0012\u00020\u00050\u00042\u0006\u0010\u0014\u001a\u00020\n2\u0006\u0010\u0006\u001a\u00020\u0007H\u0002\u00a8\u0006\u001e"}, d2 = {"Lcom/security/app/GoogleSheetsReader;", "", "()V", "fetchLatestReadings", "", "Lcom/security/app/GoogleSheetsReader$SensorReading;", "maxRows", "", "(ILkotlin/coroutines/Continuation;)Ljava/lang/Object;", "fetchRawCsvForGid", "", "gid", "fetchReadingsForDate", "date", "(Ljava/lang/String;ILkotlin/coroutines/Continuation;)Ljava/lang/Object;", "fetchRecentReadings", "hours", "fetchRecentReadingsFromGid", "(IIILkotlin/coroutines/Continuation;)Ljava/lang/Object;", "parseCsvData", "csvData", "parseCsvLine", "line", "parseCsvStringToSensorReadings", "parseFloat", "", "value", "parseTwoColumnCsvData", "Companion", "SensorReading", "app_debug"})
public final class GoogleSheetsReader {
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String SHEET_ID = "1_GuECfJE0nFSGMS6-prrWAfLCR4gBPa3Qf_zzaKzAAA";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String CSV_URL = "https://docs.google.com/spreadsheets/d/1_GuECfJE0nFSGMS6-prrWAfLCR4gBPa3Qf_zzaKzAAA/export?format=csv&gid=0";
    @org.jetbrains.annotations.NotNull()
    public static final com.security.app.GoogleSheetsReader.Companion Companion = null;
    
    public GoogleSheetsReader() {
        super();
    }
    
    /**
     * Fetch the latest sensor data from Google Sheets
     * Returns the most recent readings for today
     */
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object fetchLatestReadings(int maxRows, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.security.app.GoogleSheetsReader.SensorReading>> $completion) {
        return null;
    }
    
    /**
     * Fetch sensor readings for a specific date
     */
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object fetchReadingsForDate(@org.jetbrains.annotations.NotNull()
    java.lang.String date, int maxRows, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.security.app.GoogleSheetsReader.SensorReading>> $completion) {
        return null;
    }
    
    /**
     * Parse CSV data into SensorReading objects - ALWAYS GET LAST ROW
     */
    private final java.util.List<com.security.app.GoogleSheetsReader.SensorReading> parseCsvData(java.lang.String csvData, int maxRows) {
        return null;
    }
    
    /**
     * Parse a CSV line handling quoted fields
     */
    private final java.util.List<java.lang.String> parseCsvLine(java.lang.String line) {
        return null;
    }
    
    /**
     * Safely parse float from string
     */
    private final float parseFloat(java.lang.String value) {
        return 0.0F;
    }
    
    /**
     * Get readings for the last N hours
     */
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object fetchRecentReadings(int hours, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.security.app.GoogleSheetsReader.SensorReading>> $completion) {
        return null;
    }
    
    /**
     * Get readings from a specific sheet gid (tab) - useful for alternate trend tabs
     */
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object fetchRecentReadingsFromGid(int gid, int hours, int maxRows, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.security.app.GoogleSheetsReader.SensorReading>> $completion) {
        return null;
    }
    
    /**
     * Parse CSVs that are in a simple two-column format: value, timestamp
     * Produces SensorReading objects with dvrTemp set to column0 and timestamp column1.
     */
    private final java.util.List<com.security.app.GoogleSheetsReader.SensorReading> parseTwoColumnCsvData(java.lang.String csvData, int maxRows) {
        return null;
    }
    
    /**
     * Fetch raw CSV data for a specific gid (tab) - used for previewing CSV content
     */
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object fetchRawCsvForGid(int gid, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.String> $completion) {
        return null;
    }
    
    /**
     * Public helper to parse a raw CSV string (from Sheets or Drive) into SensorReading objects.
     * This exposes the internal parser so other modules (e.g. Drive fetchers) can reuse it.
     */
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.security.app.GoogleSheetsReader.SensorReading> parseCsvStringToSensorReadings(@org.jetbrains.annotations.NotNull()
    java.lang.String csvData, int maxRows) {
        return null;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0002\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0006"}, d2 = {"Lcom/security/app/GoogleSheetsReader$Companion;", "", "()V", "CSV_URL", "", "SHEET_ID", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000(\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\u0007\n\u0002\b*\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0002\b\u0086\b\u0018\u00002\u00020\u0001Bm\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0005\u0012\u0006\u0010\u0007\u001a\u00020\u0005\u0012\u0006\u0010\b\u001a\u00020\u0005\u0012\u0006\u0010\t\u001a\u00020\u0005\u0012\u0006\u0010\n\u001a\u00020\u0005\u0012\u0006\u0010\u000b\u001a\u00020\u0005\u0012\u0006\u0010\f\u001a\u00020\u0005\u0012\u0006\u0010\r\u001a\u00020\u0005\u0012\u0006\u0010\u000e\u001a\u00020\u0005\u0012\u0006\u0010\u000f\u001a\u00020\u0005\u0012\u0006\u0010\u0010\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0011J\t\u0010!\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\"\u001a\u00020\u0005H\u00c6\u0003J\t\u0010#\u001a\u00020\u0005H\u00c6\u0003J\t\u0010$\u001a\u00020\u0005H\u00c6\u0003J\t\u0010%\u001a\u00020\u0005H\u00c6\u0003J\t\u0010&\u001a\u00020\u0005H\u00c6\u0003J\t\u0010\'\u001a\u00020\u0005H\u00c6\u0003J\t\u0010(\u001a\u00020\u0005H\u00c6\u0003J\t\u0010)\u001a\u00020\u0005H\u00c6\u0003J\t\u0010*\u001a\u00020\u0005H\u00c6\u0003J\t\u0010+\u001a\u00020\u0005H\u00c6\u0003J\t\u0010,\u001a\u00020\u0005H\u00c6\u0003J\t\u0010-\u001a\u00020\u0005H\u00c6\u0003J\u008b\u0001\u0010.\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00052\b\b\u0002\u0010\u0006\u001a\u00020\u00052\b\b\u0002\u0010\u0007\u001a\u00020\u00052\b\b\u0002\u0010\b\u001a\u00020\u00052\b\b\u0002\u0010\t\u001a\u00020\u00052\b\b\u0002\u0010\n\u001a\u00020\u00052\b\b\u0002\u0010\u000b\u001a\u00020\u00052\b\b\u0002\u0010\f\u001a\u00020\u00052\b\b\u0002\u0010\r\u001a\u00020\u00052\b\b\u0002\u0010\u000e\u001a\u00020\u00052\b\b\u0002\u0010\u000f\u001a\u00020\u00052\b\b\u0002\u0010\u0010\u001a\u00020\u0005H\u00c6\u0001J\u0013\u0010/\u001a\u0002002\b\u00101\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u00102\u001a\u000203H\u00d6\u0001J\t\u00104\u001a\u00020\u0003H\u00d6\u0001R\u0011\u0010\u000b\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0012\u0010\u0013R\u0011\u0010\n\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0014\u0010\u0013R\u0011\u0010\f\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0015\u0010\u0013R\u0011\u0010\u0010\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0016\u0010\u0013R\u0011\u0010\r\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0017\u0010\u0013R\u0011\u0010\u0007\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0018\u0010\u0013R\u0011\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0019\u0010\u0013R\u0011\u0010\u0006\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001a\u0010\u0013R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001b\u0010\u001cR\u0011\u0010\u000f\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001d\u0010\u0013R\u0011\u0010\u000e\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001e\u0010\u0013R\u0011\u0010\t\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001f\u0010\u0013R\u0011\u0010\b\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b \u0010\u0013\u00a8\u00065"}, d2 = {"Lcom/security/app/GoogleSheetsReader$SensorReading;", "", "timestamp", "", "indoorTemp", "", "outdoorTemp", "humidity", "windSpeed", "windDirection", "currentPower", "currentAmps", "dailyPower", "dvrTemp", "waterTemp", "waterPressure", "dailyWater", "(Ljava/lang/String;FFFFFFFFFFFF)V", "getCurrentAmps", "()F", "getCurrentPower", "getDailyPower", "getDailyWater", "getDvrTemp", "getHumidity", "getIndoorTemp", "getOutdoorTemp", "getTimestamp", "()Ljava/lang/String;", "getWaterPressure", "getWaterTemp", "getWindDirection", "getWindSpeed", "component1", "component10", "component11", "component12", "component13", "component2", "component3", "component4", "component5", "component6", "component7", "component8", "component9", "copy", "equals", "", "other", "hashCode", "", "toString", "app_debug"})
    public static final class SensorReading {
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String timestamp = null;
        private final float indoorTemp = 0.0F;
        private final float outdoorTemp = 0.0F;
        private final float humidity = 0.0F;
        private final float windSpeed = 0.0F;
        private final float windDirection = 0.0F;
        private final float currentPower = 0.0F;
        private final float currentAmps = 0.0F;
        private final float dailyPower = 0.0F;
        private final float dvrTemp = 0.0F;
        private final float waterTemp = 0.0F;
        private final float waterPressure = 0.0F;
        private final float dailyWater = 0.0F;
        
        public SensorReading(@org.jetbrains.annotations.NotNull()
        java.lang.String timestamp, float indoorTemp, float outdoorTemp, float humidity, float windSpeed, float windDirection, float currentPower, float currentAmps, float dailyPower, float dvrTemp, float waterTemp, float waterPressure, float dailyWater) {
            super();
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getTimestamp() {
            return null;
        }
        
        public final float getIndoorTemp() {
            return 0.0F;
        }
        
        public final float getOutdoorTemp() {
            return 0.0F;
        }
        
        public final float getHumidity() {
            return 0.0F;
        }
        
        public final float getWindSpeed() {
            return 0.0F;
        }
        
        public final float getWindDirection() {
            return 0.0F;
        }
        
        public final float getCurrentPower() {
            return 0.0F;
        }
        
        public final float getCurrentAmps() {
            return 0.0F;
        }
        
        public final float getDailyPower() {
            return 0.0F;
        }
        
        public final float getDvrTemp() {
            return 0.0F;
        }
        
        public final float getWaterTemp() {
            return 0.0F;
        }
        
        public final float getWaterPressure() {
            return 0.0F;
        }
        
        public final float getDailyWater() {
            return 0.0F;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String component1() {
            return null;
        }
        
        public final float component10() {
            return 0.0F;
        }
        
        public final float component11() {
            return 0.0F;
        }
        
        public final float component12() {
            return 0.0F;
        }
        
        public final float component13() {
            return 0.0F;
        }
        
        public final float component2() {
            return 0.0F;
        }
        
        public final float component3() {
            return 0.0F;
        }
        
        public final float component4() {
            return 0.0F;
        }
        
        public final float component5() {
            return 0.0F;
        }
        
        public final float component6() {
            return 0.0F;
        }
        
        public final float component7() {
            return 0.0F;
        }
        
        public final float component8() {
            return 0.0F;
        }
        
        public final float component9() {
            return 0.0F;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.security.app.GoogleSheetsReader.SensorReading copy(@org.jetbrains.annotations.NotNull()
        java.lang.String timestamp, float indoorTemp, float outdoorTemp, float humidity, float windSpeed, float windDirection, float currentPower, float currentAmps, float dailyPower, float dvrTemp, float waterTemp, float waterPressure, float dailyWater) {
            return null;
        }
        
        @java.lang.Override()
        public boolean equals(@org.jetbrains.annotations.Nullable()
        java.lang.Object other) {
            return false;
        }
        
        @java.lang.Override()
        public int hashCode() {
            return 0;
        }
        
        @java.lang.Override()
        @org.jetbrains.annotations.NotNull()
        public java.lang.String toString() {
            return null;
        }
    }
}