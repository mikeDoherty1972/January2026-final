package com.security.app;

/**
 * IPERL-specific Google Sheets reader for water meter data
 * Reads from mike_data and mike_RSSI tabs
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000X\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0006\n\u0002\b\u0003\n\u0002\u0010\u0007\n\u0002\b\u000b\u0018\u0000 &2\u00020\u0001:\u0004&\'()B\u0005\u00a2\u0006\u0002\u0010\u0002J\u001a\u0010\u0003\u001a\u000e\u0012\u0004\u0012\u00020\u0005\u0012\u0004\u0012\u00020\u00050\u0004H\u0086@\u00a2\u0006\u0002\u0010\u0006J\u0010\u0010\u0007\u001a\u0004\u0018\u00010\bH\u0086@\u00a2\u0006\u0002\u0010\u0006J\u001e\u0010\t\u001a\b\u0012\u0004\u0012\u00020\u000b0\n2\b\b\u0002\u0010\f\u001a\u00020\rH\u0086@\u00a2\u0006\u0002\u0010\u000eJ\u001e\u0010\u000f\u001a\b\u0012\u0004\u0012\u00020\u00100\n2\b\b\u0002\u0010\f\u001a\u00020\rH\u0086@\u00a2\u0006\u0002\u0010\u000eJ\u000e\u0010\u0011\u001a\u00020\rH\u0086@\u00a2\u0006\u0002\u0010\u0006J\u000e\u0010\u0012\u001a\u00020\u0013H\u0086@\u00a2\u0006\u0002\u0010\u0006J\u0016\u0010\u0014\u001a\b\u0012\u0004\u0012\u00020\b0\n2\u0006\u0010\u0015\u001a\u00020\bH\u0002J \u0010\u0016\u001a\u0004\u0018\u00010\u00172\u0006\u0010\u0018\u001a\u00020\b2\f\u0010\u0019\u001a\b\u0012\u0004\u0012\u00020\b0\nH\u0002J\u0017\u0010\u001a\u001a\u0004\u0018\u00010\u001b2\u0006\u0010\u001c\u001a\u00020\bH\u0002\u00a2\u0006\u0002\u0010\u001dJ\u0010\u0010\u001e\u001a\u00020\u001f2\u0006\u0010\u001c\u001a\u00020\bH\u0002J\u0017\u0010 \u001a\u0004\u0018\u00010\u001f2\u0006\u0010\u001c\u001a\u00020\bH\u0002\u00a2\u0006\u0002\u0010!J\u001e\u0010\"\u001a\b\u0012\u0004\u0012\u00020\u000b0\n2\u0006\u0010#\u001a\u00020\b2\u0006\u0010\f\u001a\u00020\rH\u0002J\u001e\u0010$\u001a\b\u0012\u0004\u0012\u00020\u00100\n2\u0006\u0010#\u001a\u00020\b2\b\b\u0002\u0010\f\u001a\u00020\rJ\u001e\u0010%\u001a\b\u0012\u0004\u0012\u00020\u00100\n2\u0006\u0010#\u001a\u00020\b2\u0006\u0010\f\u001a\u00020\rH\u0002\u00a8\u0006*"}, d2 = {"Lcom/security/app/IPERLGoogleSheetsReader;", "", "()V", "dumpSheetsToSdcard", "Lkotlin/Pair;", "", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "fetchLatestColumnKValue", "", "fetchLatestRSSIReadings", "", "Lcom/security/app/IPERLGoogleSheetsReader$SignalReading;", "maxRows", "", "(ILkotlin/coroutines/Continuation;)Ljava/lang/Object;", "fetchLatestWaterReadings", "Lcom/security/app/IPERLGoogleSheetsReader$WaterMeterReading;", "fetchMunicipalityWaterMeterReading", "getUsageStatistics", "Lcom/security/app/IPERLGoogleSheetsReader$UsageStats;", "parseCsvLine", "line", "parseDateFlexible", "Ljava/util/Date;", "input", "formats", "parseDoubleFlexible", "", "value", "(Ljava/lang/String;)Ljava/lang/Double;", "parseFloatFlexible", "", "parseFloatNullable", "(Ljava/lang/String;)Ljava/lang/Float;", "parseSignalData", "csvData", "parseWaterMeterCsv", "parseWaterMeterDataColumnAandK", "Companion", "SignalReading", "UsageStats", "WaterMeterReading", "app_debug"})
public final class IPERLGoogleSheetsReader {
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String SHEET_ID = "1_GuECfJE0nFSGMS6-prrWAfLCR4gBPa3Qf_zzaKzAAA";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String MIKE_DATA_URL = "https://docs.google.com/spreadsheets/d/1_GuECfJE0nFSGMS6-prrWAfLCR4gBPa3Qf_zzaKzAAA/export?format=csv&gid=0";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String USAGE_STATS_URL = "https://docs.google.com/spreadsheets/d/1_GuECfJE0nFSGMS6-prrWAfLCR4gBPa3Qf_zzaKzAAA/export?format=csv&gid=1172995861";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String MIKE_RSSI_URL = "https://docs.google.com/spreadsheets/d/1_GuECfJE0nFSGMS6-prrWAfLCR4gBPa3Qf_zzaKzAAA/export?format=csv&gid=1935643323";
    @org.jetbrains.annotations.NotNull()
    public static final com.security.app.IPERLGoogleSheetsReader.Companion Companion = null;
    
    public IPERLGoogleSheetsReader() {
        super();
    }
    
    /**
     * Mike's Water Meter chart: fetches from column K (value) and column A (timestamp)
     */
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object fetchLatestWaterReadings(int maxRows, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.security.app.IPERLGoogleSheetsReader.WaterMeterReading>> $completion) {
        return null;
    }
    
    /**
     * Fetch the latest RSSI readings from mike_RSSI tab
     */
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object fetchLatestRSSIReadings(int maxRows, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.security.app.IPERLGoogleSheetsReader.SignalReading>> $completion) {
        return null;
    }
    
    /**
     * Water usage statistics card: reads from column A (timestamp) and column B (value)
     * Ensures all values are parsed as Double to avoid type mismatch errors.
     */
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object getUsageStatistics(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.security.app.IPERLGoogleSheetsReader.UsageStats> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.security.app.IPERLGoogleSheetsReader.WaterMeterReading> parseWaterMeterCsv(@org.jetbrains.annotations.NotNull()
    java.lang.String csvData, int maxRows) {
        return null;
    }
    
    /**
     * Parse CSV data for Water Meters Card (today's water usage)
     * Uses column K (index 10) for value and column A (index 0) for timestamp.
     * Skips header row robustly and supports multiple date formats.
     */
    private final java.util.List<com.security.app.IPERLGoogleSheetsReader.WaterMeterReading> parseWaterMeterDataColumnAandK(java.lang.String csvData, int maxRows) {
        return null;
    }
    
    /**
     * Parse CSV data into SignalReading objects (reads last N rows)
     */
    private final java.util.List<com.security.app.IPERLGoogleSheetsReader.SignalReading> parseSignalData(java.lang.String csvData, int maxRows) {
        return null;
    }
    
    /**
     * Parse a CSV line handling quoted fields and escaped quotes
     */
    private final java.util.List<java.lang.String> parseCsvLine(java.lang.String line) {
        return null;
    }
    
    /**
     * Safely parse float from string and accept comma decimal separators
     */
    private final float parseFloatFlexible(java.lang.String value) {
        return 0.0F;
    }
    
    private final java.lang.Float parseFloatNullable(java.lang.String value) {
        return null;
    }
    
    private final java.lang.Double parseDoubleFlexible(java.lang.String value) {
        return null;
    }
    
    /**
     * Try multiple common date formats to parse a timestamp string.
     */
    private final java.util.Date parseDateFlexible(java.lang.String input, java.util.List<java.lang.String> formats) {
        return null;
    }
    
    /**
     * Fetch the latest value from column K (11th column, zero-based index 10) of the first sheet
     */
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object fetchLatestColumnKValue(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.String> $completion) {
        return null;
    }
    
    /**
     * Municipality Water Meter Monitoring: fetches from column K (value) and column A (timestamp)
     */
    @kotlin.Suppress(names = {"unused"})
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object fetchMunicipalityWaterMeterReading(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Integer> $completion) {
        return null;
    }
    
    /**
     * Debug helper: download both CSV sheets and save to /sdcard for inspection.
     * Use adb pull /sdcard/iperl_mike.csv and /sdcard/iperl_usage.csv to retrieve.
     */
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object dumpSheetsToSdcard(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Pair<java.lang.Boolean, java.lang.Boolean>> $completion) {
        return null;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0004\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\b"}, d2 = {"Lcom/security/app/IPERLGoogleSheetsReader$Companion;", "", "()V", "MIKE_DATA_URL", "", "MIKE_RSSI_URL", "SHEET_ID", "USAGE_STATS_URL", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000.\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\u0007\n\u0000\n\u0002\u0018\u0002\n\u0002\b\f\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0002\b\u0086\b\u0018\u00002\u00020\u0001B\u001d\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\u0002\u0010\bJ\t\u0010\u000f\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u0010\u001a\u00020\u0005H\u00c6\u0003J\t\u0010\u0011\u001a\u00020\u0007H\u00c6\u0003J\'\u0010\u0012\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00052\b\b\u0002\u0010\u0006\u001a\u00020\u0007H\u00c6\u0001J\u0013\u0010\u0013\u001a\u00020\u00142\b\u0010\u0015\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010\u0016\u001a\u00020\u0017H\u00d6\u0001J\t\u0010\u0018\u001a\u00020\u0003H\u00d6\u0001R\u0011\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b\t\u0010\nR\u0011\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000b\u0010\fR\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\r\u0010\u000e\u00a8\u0006\u0019"}, d2 = {"Lcom/security/app/IPERLGoogleSheetsReader$SignalReading;", "", "timestamp", "", "rssi", "", "date", "Ljava/util/Date;", "(Ljava/lang/String;FLjava/util/Date;)V", "getDate", "()Ljava/util/Date;", "getRssi", "()F", "getTimestamp", "()Ljava/lang/String;", "component1", "component2", "component3", "copy", "equals", "", "other", "hashCode", "", "toString", "app_debug"})
    public static final class SignalReading {
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String timestamp = null;
        private final float rssi = 0.0F;
        @org.jetbrains.annotations.NotNull()
        private final java.util.Date date = null;
        
        public SignalReading(@org.jetbrains.annotations.NotNull()
        java.lang.String timestamp, float rssi, @org.jetbrains.annotations.NotNull()
        java.util.Date date) {
            super();
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getTimestamp() {
            return null;
        }
        
        public final float getRssi() {
            return 0.0F;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.util.Date getDate() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String component1() {
            return null;
        }
        
        public final float component2() {
            return 0.0F;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.util.Date component3() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.security.app.IPERLGoogleSheetsReader.SignalReading copy(@org.jetbrains.annotations.NotNull()
        java.lang.String timestamp, float rssi, @org.jetbrains.annotations.NotNull()
        java.util.Date date) {
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
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000*\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0006\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0002\b\r\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0002\b\u0086\b\u0018\u00002\u00020\u0001B%\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0003\u0012\u0006\u0010\u0005\u001a\u00020\u0003\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\u0002\u0010\bJ\t\u0010\u000f\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u0010\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u0011\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u0012\u001a\u00020\u0007H\u00c6\u0003J1\u0010\u0013\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00032\b\b\u0002\u0010\u0005\u001a\u00020\u00032\b\b\u0002\u0010\u0006\u001a\u00020\u0007H\u00c6\u0001J\u0013\u0010\u0014\u001a\u00020\u00152\b\u0010\u0016\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010\u0017\u001a\u00020\u0018H\u00d6\u0001J\t\u0010\u0019\u001a\u00020\u0007H\u00d6\u0001R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\t\u0010\nR\u0011\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000b\u0010\fR\u0011\u0010\u0004\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\r\u0010\nR\u0011\u0010\u0005\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000e\u0010\n\u00a8\u0006\u001a"}, d2 = {"Lcom/security/app/IPERLGoogleSheetsReader$UsageStats;", "", "dayUsage", "", "monthUsage", "totalUsage", "lastUpdate", "", "(DDDLjava/lang/String;)V", "getDayUsage", "()D", "getLastUpdate", "()Ljava/lang/String;", "getMonthUsage", "getTotalUsage", "component1", "component2", "component3", "component4", "copy", "equals", "", "other", "hashCode", "", "toString", "app_debug"})
    public static final class UsageStats {
        private final double dayUsage = 0.0;
        private final double monthUsage = 0.0;
        private final double totalUsage = 0.0;
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String lastUpdate = null;
        
        public UsageStats(double dayUsage, double monthUsage, double totalUsage, @org.jetbrains.annotations.NotNull()
        java.lang.String lastUpdate) {
            super();
        }
        
        public final double getDayUsage() {
            return 0.0;
        }
        
        public final double getMonthUsage() {
            return 0.0;
        }
        
        public final double getTotalUsage() {
            return 0.0;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getLastUpdate() {
            return null;
        }
        
        public final double component1() {
            return 0.0;
        }
        
        public final double component2() {
            return 0.0;
        }
        
        public final double component3() {
            return 0.0;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String component4() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.security.app.IPERLGoogleSheetsReader.UsageStats copy(double dayUsage, double monthUsage, double totalUsage, @org.jetbrains.annotations.NotNull()
        java.lang.String lastUpdate) {
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
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000.\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\u0007\n\u0000\n\u0002\u0018\u0002\n\u0002\b\f\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0002\b\u0086\b\u0018\u00002\u00020\u0001B\u001d\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\u0002\u0010\bJ\t\u0010\u000f\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u0010\u001a\u00020\u0005H\u00c6\u0003J\t\u0010\u0011\u001a\u00020\u0007H\u00c6\u0003J\'\u0010\u0012\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00052\b\b\u0002\u0010\u0006\u001a\u00020\u0007H\u00c6\u0001J\u0013\u0010\u0013\u001a\u00020\u00142\b\u0010\u0015\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010\u0016\u001a\u00020\u0017H\u00d6\u0001J\t\u0010\u0018\u001a\u00020\u0003H\u00d6\u0001R\u0011\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b\t\u0010\nR\u0011\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000b\u0010\fR\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\r\u0010\u000e\u00a8\u0006\u0019"}, d2 = {"Lcom/security/app/IPERLGoogleSheetsReader$WaterMeterReading;", "", "timestamp", "", "meterReading", "", "date", "Ljava/util/Date;", "(Ljava/lang/String;FLjava/util/Date;)V", "getDate", "()Ljava/util/Date;", "getMeterReading", "()F", "getTimestamp", "()Ljava/lang/String;", "component1", "component2", "component3", "copy", "equals", "", "other", "hashCode", "", "toString", "app_debug"})
    public static final class WaterMeterReading {
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String timestamp = null;
        private final float meterReading = 0.0F;
        @org.jetbrains.annotations.NotNull()
        private final java.util.Date date = null;
        
        public WaterMeterReading(@org.jetbrains.annotations.NotNull()
        java.lang.String timestamp, float meterReading, @org.jetbrains.annotations.NotNull()
        java.util.Date date) {
            super();
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getTimestamp() {
            return null;
        }
        
        public final float getMeterReading() {
            return 0.0F;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.util.Date getDate() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String component1() {
            return null;
        }
        
        public final float component2() {
            return 0.0F;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.util.Date component3() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.security.app.IPERLGoogleSheetsReader.WaterMeterReading copy(@org.jetbrains.annotations.NotNull()
        java.lang.String timestamp, float meterReading, @org.jetbrains.annotations.NotNull()
        java.util.Date date) {
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