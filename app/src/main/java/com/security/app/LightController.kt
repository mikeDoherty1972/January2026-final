package com.security.app

import android.content.Context
import android.util.Log

class LightController(private val context: Context, private val db: com.google.firebase.firestore.FirebaseFirestore?, private val driveService: com.google.api.services.drive.Drive?) {

    private val fcmSender = FcmSender(db)

    fun turnOn() {
        Log.d("LightController", "turnOn called")
        // 1) send command to bridge via Firestore (used by bridge to command PLC)
        try { fcmSender.sendToBridgeCommand("on") } catch (e: Exception) { Log.w("LightController", "Failed to send bridge command", e) }
        // 2) persist the desired state in Drive if Drive available and Drive mode selected
        try { updateDriveCommand("on") } catch (e: Exception) { Log.w("LightController", "Failed to update Drive command", e) }
    }

    fun turnOff() {
        Log.d("LightController", "turnOff called")
        try { fcmSender.sendToBridgeCommand("off") } catch (e: Exception) { Log.w("LightController", "Failed to send bridge command", e) }
        try { updateDriveCommand("off") } catch (e: Exception) { Log.w("LightController", "Failed to update Drive command", e) }
    }

    // Drive-only methods: write the command to Drive but don't send the Firestore/FCM message.
    fun turnOnDriveOnly() {
        Log.d("LightController", "turnOnDriveOnly called")
        try { updateDriveCommand("on") } catch (e: Exception) { Log.w("LightController", "Failed to update Drive command (drive-only)", e) }
    }

    fun turnOffDriveOnly() {
        Log.d("LightController", "turnOffDriveOnly called")
        try { updateDriveCommand("off") } catch (e: Exception) { Log.w("LightController", "Failed to update Drive command (drive-only)", e) }
    }

    private fun updateDriveCommand(cmd: String) {
        if (driveService == null) {
            Log.d("LightController", "Drive service null, skipping Drive command write")
            return
        }
        // Write a small text file contents to the configured Drive file used by the bridge
        try {
            val fileId = "1rIlYuXnuITRT2Thm5o4yDBmCXktx05j6" // messages_received.txt id used historically
            val content = "command:$cmd\nsource:android_app\nts:${System.currentTimeMillis()}"
            val media = com.google.api.client.http.ByteArrayContent.fromString("text/plain", content)
            val file = com.google.api.services.drive.model.File()
            file.name = "messages_received.txt"
            driveService.files().update(fileId, file, media).execute()
            Log.d("LightController", "Wrote Drive file command: $cmd")
        } catch (e: Exception) {
            Log.w("LightController", "Drive write failed", e)
        }
    }
}
