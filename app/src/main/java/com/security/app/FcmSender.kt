package com.security.app

import android.util.Log

// Lightweight local sender abstraction. In production, FCM messages are normally
// sent from a server. This class logs intents that would be sent and writes a
// Firestore command document for the bridge which consumes commands.
class FcmSender(private val db: com.google.firebase.firestore.FirebaseFirestore?) {

    fun sendToBridgeCommand(command: String) {
        try {
            if (db == null) {
                Log.w("FcmSender", "Firestore DB is null -- cannot send bridge command")
                return
            }
            val payload = hashMapOf<String, Any>(
                "command" to command,
                "command_ts" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                "command_source" to "android_app"
            )
            // Write to both scada_controls/lights and Flights_commands/current_command for compatibility
            db.collection("scada_controls").document("lights").set(payload, com.google.firebase.firestore.SetOptions.merge())
            db.collection("Flights_commands").document("current_command").set(payload, com.google.firebase.firestore.SetOptions.merge())
            Log.d("FcmSender", "Wrote bridge command via Firestore: $command")
        } catch (e: Exception) {
            Log.e("FcmSender", "Failed to write bridge command", e)
        }
    }
}

