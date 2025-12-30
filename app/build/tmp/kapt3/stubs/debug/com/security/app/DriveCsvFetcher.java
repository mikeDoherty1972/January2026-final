package com.security.app;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u001a\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\"\u0010\u0003\u001a\u0004\u0018\u00010\u00042\b\u0010\u0005\u001a\u0004\u0018\u00010\u00062\u0006\u0010\u0007\u001a\u00020\u0004H\u0086@\u00a2\u0006\u0002\u0010\b\u00a8\u0006\t"}, d2 = {"Lcom/security/app/DriveCsvFetcher;", "", "()V", "fetchCsv", "", "driveService", "Lcom/google/api/services/drive/Drive;", "fileId", "(Lcom/google/api/services/drive/Drive;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "app_debug"})
public final class DriveCsvFetcher {
    @org.jetbrains.annotations.NotNull()
    public static final com.security.app.DriveCsvFetcher INSTANCE = null;
    
    private DriveCsvFetcher() {
        super();
    }
    
    /**
     * Try to fetch file content via Drive API if 'driveService' is provided, otherwise try public download URL
     * fileId is the Drive file ID. Returns file content or null on error.
     */
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object fetchCsv(@org.jetbrains.annotations.Nullable()
    com.google.api.services.drive.Drive driveService, @org.jetbrains.annotations.NotNull()
    java.lang.String fileId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.String> $completion) {
        return null;
    }
}