package com.security.app;

/**
 * Simple in-memory cache for Google Sheets CSV exports by gid.
 * Fetches CSV once per gid and caches it for the app lifetime unless forceRefresh is true.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000,\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0004\n\u0002\u0010\u000b\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u0017\u0010\b\u001a\u00020\t2\n\b\u0002\u0010\n\u001a\u0004\u0018\u00010\u0007\u00a2\u0006\u0002\u0010\u000bJ$\u0010\f\u001a\u0004\u0018\u00010\u00042\b\b\u0002\u0010\n\u001a\u00020\u00072\b\b\u0002\u0010\r\u001a\u00020\u000eH\u0086@\u00a2\u0006\u0002\u0010\u000fR\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u001a\u0010\u0005\u001a\u000e\u0012\u0004\u0012\u00020\u0007\u0012\u0004\u0012\u00020\u00040\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0010"}, d2 = {"Lcom/security/app/SheetsCache;", "", "()V", "SHEET_ID", "", "cache", "Ljava/util/concurrent/ConcurrentHashMap;", "", "clearCache", "", "gid", "(Ljava/lang/Integer;)V", "fetchCsv", "forceRefresh", "", "(IZLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "app_debug"})
public final class SheetsCache {
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String SHEET_ID = "1_GuECfJE0nFSGMS6-prrWAfLCR4gBPa3Qf_zzaKzAAA";
    @org.jetbrains.annotations.NotNull()
    private static final java.util.concurrent.ConcurrentHashMap<java.lang.Integer, java.lang.String> cache = null;
    @org.jetbrains.annotations.NotNull()
    public static final com.security.app.SheetsCache INSTANCE = null;
    
    private SheetsCache() {
        super();
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object fetchCsv(int gid, boolean forceRefresh, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.String> $completion) {
        return null;
    }
    
    public final void clearCache(@org.jetbrains.annotations.Nullable()
    java.lang.Integer gid) {
    }
}