package com.security.app;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00002\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0005\n\u0002\u0010\u000e\n\u0000\u0018\u00002\u00020\u0001B!\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\b\u0010\u0004\u001a\u0004\u0018\u00010\u0005\u0012\b\u0010\u0006\u001a\u0004\u0018\u00010\u0007\u00a2\u0006\u0002\u0010\bJ\u0006\u0010\u000b\u001a\u00020\fJ\u0006\u0010\r\u001a\u00020\fJ\u0006\u0010\u000e\u001a\u00020\fJ\u0006\u0010\u000f\u001a\u00020\fJ\u0010\u0010\u0010\u001a\u00020\f2\u0006\u0010\u0011\u001a\u00020\u0012H\u0002R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0004\u001a\u0004\u0018\u00010\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0006\u001a\u0004\u0018\u00010\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0013"}, d2 = {"Lcom/security/app/LightController;", "", "context", "Landroid/content/Context;", "db", "Lcom/google/firebase/firestore/FirebaseFirestore;", "driveService", "Lcom/google/api/services/drive/Drive;", "(Landroid/content/Context;Lcom/google/firebase/firestore/FirebaseFirestore;Lcom/google/api/services/drive/Drive;)V", "fcmSender", "Lcom/security/app/FcmSender;", "turnOff", "", "turnOffDriveOnly", "turnOn", "turnOnDriveOnly", "updateDriveCommand", "cmd", "", "app_debug"})
public final class LightController {
    @org.jetbrains.annotations.NotNull()
    private final android.content.Context context = null;
    @org.jetbrains.annotations.Nullable()
    private final com.google.firebase.firestore.FirebaseFirestore db = null;
    @org.jetbrains.annotations.Nullable()
    private final com.google.api.services.drive.Drive driveService = null;
    @org.jetbrains.annotations.NotNull()
    private final com.security.app.FcmSender fcmSender = null;
    
    public LightController(@org.jetbrains.annotations.NotNull()
    android.content.Context context, @org.jetbrains.annotations.Nullable()
    com.google.firebase.firestore.FirebaseFirestore db, @org.jetbrains.annotations.Nullable()
    com.google.api.services.drive.Drive driveService) {
        super();
    }
    
    public final void turnOn() {
    }
    
    public final void turnOff() {
    }
    
    public final void turnOnDriveOnly() {
    }
    
    public final void turnOffDriveOnly() {
    }
    
    private final void updateDriveCommand(java.lang.String cmd) {
    }
}