package com.security.app

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Small Firestore data availability monitor.
 * - Listens to a document and logs missing documents/fields to an internal file `data_watch.log`.
 * - Optionally calls back with human-readable status updates.
 *
 * Usage:
 *   val monitor = FirestoreDataMonitor(this) { status -> Log.d("UI", status) }
 *   monitor.start()
 *   // call monitor.stop() when no longer needed
 */
class FirestoreDataMonitor(
    private val context: Context,
    private val collection: String = "Flights_commands",
    private val document: String = "current_command",
    private val onStatus: ((String) -> Unit)? = null
) {
    private val tag = "FirestoreDataMonitor"
    private val firestore = FirebaseFirestore.getInstance()
    private var listener: ListenerRegistration? = null
    private val logFileName = "data_watch.log"
    private val isoFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)

    fun start() {
        stop()
        val docRef = firestore.collection(collection).document(document)
        listener = docRef.addSnapshotListener { snapshot, error ->
            val ts = isoFmt.format(Date())
            if (error != null) {
                val msg = "$ts ERROR: Firestore listen failed: ${error.message}"
                record(msg)
                notify(msg)
                return@addSnapshotListener
            }

            if (snapshot == null || !snapshot.exists()) {
                val msg = "$ts MISSING: document $collection/$document not found"
                record(msg)
                notify(msg)
                return@addSnapshotListener
            }

            val data = snapshot.data ?: emptyMap<String, Any>()
            val hasCommand = data.containsKey("command") && data["command"] != null
            // robust desired check: explicitly inspect the value so analyzer can't assume truthiness
            val desiredVal = data["desired"]
            val hasDesired = desiredVal != null && !(desiredVal is String && desiredVal.isBlank())

            if (!hasCommand && !hasDesired) {
                val msg = "$ts MISSING_FIELDS: no 'command' or 'desired' field in $collection/$document - data=${data.keys}"
                record(msg)
                notify(msg)
            } else {
                val resolved = when {
                    hasCommand -> "command=${data["command"]}"
                    hasDesired -> "desired=${data["desired"]}"
                    else -> "none"
                }
                val msg = "$ts OK: doc present, $resolved"
                record(msg)
                notify(msg)
            }
        }
        val startMsg = "${isoFmt.format(Date())} WATCH_STARTED: $collection/$document"
        record(startMsg)
        notify(startMsg)
    }

    fun stop() {
        listener?.remove()
        listener = null
        val stopMsg = "${isoFmt.format(Date())} WATCH_STOPPED"
        record(stopMsg)
        notify(stopMsg)
    }

    private fun notify(message: String) {
        Log.i(tag, message)
        onStatus?.invoke(message)
        // Removed Toast to avoid showing UI popups from background Firestore listeners.
        // The monitor still logs and invokes the optional callback for UI updates.
    }

    private fun shortMsg(full: String): String {
        val parts = full.split(":")
        return if (parts.size > 1) parts[1].trim() else full
    }

    private fun record(message: String) {
        try {
            context.openFileOutput(logFileName, Context.MODE_APPEND).use { fos ->
                OutputStreamWriter(fos).use { w ->
                    w.appendLine(message)
                }
            }
        } catch (_: Exception) {
            Log.w(tag, "Failed to write log")
        }
    }
}
