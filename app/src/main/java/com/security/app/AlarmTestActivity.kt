package com.security.app

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch

class AlarmTestActivity : BaseActivity() {

    private lateinit var btnRequestNotif: Button
    private lateinit var btnOpenNotifSettings: Button
    private lateinit var btnOpenBatterySettings: Button
    private lateinit var btnStartTestAlarm: Button
    private lateinit var btnRefreshIperl: Button
    private lateinit var statusText: TextView
    private lateinit var iperlStatusText: TextView

    // move launcher initialization into onCreate to avoid referencing uninitialised views
    private lateinit var requestNotifLauncher: ActivityResultLauncher<String>

    // progress indicator for water (layout has this). Note: progressUsage removed because layout doesn't define it
    private lateinit var progressWater: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarm_test)

        btnRequestNotif = findViewById(R.id.btnRequestNotif)
        btnOpenNotifSettings = findViewById(R.id.btnOpenNotifSettings)
        btnOpenBatterySettings = findViewById(R.id.btnOpenBatterySettings)
        btnStartTestAlarm = findViewById(R.id.btnStartTestAlarm)
        btnRefreshIperl = findViewById(R.id.btnRefreshIperl)
        statusText = findViewById(R.id.statusText)
        iperlStatusText = findViewById(R.id.iperlStatusText)

        // initialize progress views that exist in layout
        progressWater = findViewById(R.id.progressWater)

        // initialize the permission launcher now that views are available
        requestNotifLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            statusText.text = if (granted) "Notifications permission: GRANTED" else "Notifications permission: DENIED"
        }

        btnRequestNotif.setOnClickListener { requestNotificationPermissionIfNeeded() }
        btnOpenNotifSettings.setOnClickListener { openAppNotificationSettings() }
        btnOpenBatterySettings.setOnClickListener { openBatteryOptimizationSettings() }
        btnStartTestAlarm.setOnClickListener { triggerTestAlarm() }
        btnRefreshIperl.setOnClickListener { refreshIperlData() }

        updateCurrentPermissionStatus()
        observeRepositoryStatus()
    }

    private fun updateCurrentPermissionStatus() {
        val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else true
        statusText.text = "Notifications permission: ${if (granted) "GRANTED" else "NOT GRANTED"}"
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                try {
                    requestNotifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } catch (e: Exception) {
                    Log.e("AlarmTest", "Request permission failed", e)
                }
            } else {
                statusText.text = "Notifications permission: GRANTED"
            }
        } else {
            statusText.text = "Notifications permission: N/A (SDK < 33)"
        }
    }

    private fun openAppNotificationSettings() {
        try {
            val intent = Intent()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                intent.action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            } else {
                intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                intent.data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("AlarmTest", "Failed to open notification settings", e)
        }
    }

    private fun openBatteryOptimizationSettings() {
        try {
            val intent = Intent()
            intent.action = Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("AlarmTest", "Failed to open battery settings", e)
            // fallback: open app details
            val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
            startActivity(fallback)
        }
    }

    private fun triggerTestAlarm() {
        try {
            val intent = Intent(this, NotificationService::class.java)
            intent.action = "com.security.app.ACTION_TEST_ALARMS"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            statusText.text = "Test alarm requested"
        } catch (e: Exception) {
            Log.e("AlarmTest", "Failed to start test alarm", e)
            statusText.text = "Failed to start test alarm: ${e.message}"
        }
    }

    private fun refreshIperlData() {
        // trigger repository preload again
        lifecycleScope.launch {
            GraphDataRepository.preLoadAllGraphData()
            statusText.text = "Requested repository refresh"
        }
    }

    private fun observeRepositoryStatus() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                GraphDataRepository.waterMeterGraphData.collect { readings ->
                    iperlStatusText.text = "Water meter rows: ${readings.size}"
                    progressWater.visibility = if (readings.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                GraphDataRepository.iperlUsageStats.collect { usage ->
                    if (usage != null) {
                        iperlStatusText.text = iperlStatusText.text.toString() + " | usage total=${usage.totalUsage}"
                        // R.id.progressUsage is not declared in the layout, so avoid referencing it here
                        // If you add a view with id 'progressUsage' to the layout, set its visibility here.
                    }
                }
            }
        }
    }
}
