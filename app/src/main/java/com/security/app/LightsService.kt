package com.security.app

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

/**
 * Centralized service for sending commands to the bridge for lights-related actions.
 * Keep the public surface tiny: one helper to write the outside lights command.
 */
class LightsService(
    // ...existing code...
) {
    /**
     * Write the desired outside lights state to the bridge.
     * Returns true if the request was issued without local errors.
     */
    fun writeOutsideLights(context: Context, turnOn: Boolean): Boolean {
        return try {
            val user = Firebase.auth.currentUser
            val uid = user?.uid
            if (uid.isNullOrBlank()) {
                Log.e(
                    "LightsService",
                    "Not signed in; cannot write controls. Please sign in with Google so we have a UID allowed by Firestore rules."
                )
                return false
            }

            val db = FirebaseFirestore.getInstance()
            val desired = turnOn
            val cmd = if (turnOn) "on" else "off"
            val nonce = System.currentTimeMillis() // ensure every press produces a unique change

            // Primary app-facing controls doc (kept for UI/state reflection)
            val controlsDoc = db.collection("scada_controls").document("lights")
            val controlsPayload = hashMapOf<String, Any>(
                "desired" to desired,
                "command" to cmd,
                "source" to "android_app",
                "client_nonce" to nonce,
                "ts" to FieldValue.serverTimestamp(),
                "uid" to uid
            )

            // Legacy bridge command doc that the Unified Bridge V6 watches
            val bridgeDoc = db.collection("Flights_commands").document("current_command")
            val bridgePayload = hashMapOf<String, Any>(
                "desired" to desired,
                "command" to cmd,
                "command_source" to "android_app",
                "desired_ts" to FieldValue.serverTimestamp(),
                "client_nonce" to nonce,
                "uid" to uid
            )

            Log.d("LightsService", "Writing lights control: $cmd (nonce=$nonce) as UID=$uid")

            // 1) Write to scada_controls/lights
            controlsDoc
                .set(controlsPayload, SetOptions.merge())
                .addOnSuccessListener {
                    Log.d("LightsService", "Reflected controls/lights (turnOn=$turnOn) as UID=$uid")
                }
                .addOnFailureListener { e ->
                    Log.e(
                        "LightsService",
                        "Failed reflecting controls/lights. Check Firestore rules for UID=$uid",
                        e
                    )
                }

            // 2) Write to Flights_commands/current_command to trigger the bridge
            bridgeDoc
                .set(bridgePayload, SetOptions.merge())
                .addOnSuccessListener {
                    Log.d("LightsService", "Bridge command mirrored to Flights_commands/current_command (turnOn=$turnOn)")
                }
                .addOnFailureListener { e ->
                    Log.e(
                        "LightsService",
                        "Bridge write to Flights_commands/current_command failed",
                        e
                    )
                }

            true
        } catch (t: Throwable) {
            Log.w("LightsService", "Failed to write outside lights state", t)
            false
        }
    }

    /** Approved UIDs mirror your Firestore rules; update as needed. */
    private val approvedUids: Set<String> = setOf(
        "iIDlWbne1BdR4mAmntbJIkRMr032",
        "Kd0W4NrkjiUPmlJ7AiRiURPcLut2"
    )

    fun isUserAuthorizedForLights(): Boolean {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val authorized = uid != null && approvedUids.contains(uid)
        Log.d("LightsService", "Auth check for lights: uid=$uid authorized=$authorized")
        return authorized
    }
}

/**
 * Backwards-compatible helper so existing code can call a top-level function while routing through LightsService.
 */
fun toggleOutsideLights(context: Context, turnOn: Boolean): Boolean {
    return LightsService().writeOutsideLights(context, turnOn)
}

/**
 * Helper to expose currently signed-in Firebase UID (or null if not signed in).
 */
fun currentFirebaseUid(): String? = Firebase.auth.currentUser?.uid
