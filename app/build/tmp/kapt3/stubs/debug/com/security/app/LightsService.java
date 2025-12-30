package com.security.app;

/**
 * Centralized service for sending commands to the bridge for lights-related actions.
 * Keep the public surface tiny: one helper to write the outside lights command.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000&\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\"\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\u0006\u0010\u0006\u001a\u00020\u0007J\u0016\u0010\b\u001a\u00020\u00072\u0006\u0010\t\u001a\u00020\n2\u0006\u0010\u000b\u001a\u00020\u0007R\u0014\u0010\u0003\u001a\b\u0012\u0004\u0012\u00020\u00050\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\f"}, d2 = {"Lcom/security/app/LightsService;", "", "()V", "approvedUids", "", "", "isUserAuthorizedForLights", "", "writeOutsideLights", "context", "Landroid/content/Context;", "turnOn", "app_debug"})
public final class LightsService {
    
    /**
     * Approved UIDs mirror your Firestore rules; update as needed.
     */
    @org.jetbrains.annotations.NotNull()
    private final java.util.Set<java.lang.String> approvedUids = null;
    
    public LightsService() {
        super();
    }
    
    /**
     * Write the desired outside lights state to the bridge.
     * Returns true if the request was issued without local errors.
     */
    public final boolean writeOutsideLights(@org.jetbrains.annotations.NotNull()
    android.content.Context context, boolean turnOn) {
        return false;
    }
    
    public final boolean isUserAuthorizedForLights() {
        return false;
    }
}