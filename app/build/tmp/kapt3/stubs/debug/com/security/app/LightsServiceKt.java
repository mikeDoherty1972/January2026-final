package com.security.app;

@kotlin.Metadata(mv = {1, 9, 0}, k = 2, xi = 48, d1 = {"\u0000\u0016\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\u001a\b\u0010\u0000\u001a\u0004\u0018\u00010\u0001\u001a\u0016\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u00052\u0006\u0010\u0006\u001a\u00020\u0003\u00a8\u0006\u0007"}, d2 = {"currentFirebaseUid", "", "toggleOutsideLights", "", "context", "Landroid/content/Context;", "turnOn", "app_debug"})
public final class LightsServiceKt {
    
    /**
     * Backwards-compatible helper so existing code can call a top-level function while routing through LightsService.
     */
    public static final boolean toggleOutsideLights(@org.jetbrains.annotations.NotNull()
    android.content.Context context, boolean turnOn) {
        return false;
    }
    
    /**
     * Helper to expose currently signed-in Firebase UID (or null if not signed in).
     */
    @org.jetbrains.annotations.Nullable()
    public static final java.lang.String currentFirebaseUid() {
        return null;
    }
}