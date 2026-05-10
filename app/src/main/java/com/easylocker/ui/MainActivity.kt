package com.easylocker.ui

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.easylocker.R
import com.easylocker.service.LockTimerService
import com.easylocker.utils.DeviceAdminUtils

class MainActivity : ComponentActivity() {
    private var adminActive by mutableStateOf(false)
    private var pendingStart: Pair<Int, Int>? = null

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            pendingStart?.let { (duration, reminder) -> startTimer(duration, reminder) }
        }
        pendingStart = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EasyLockerTheme {
                EasyLockerApp(
                    adminActive = adminActive,
                    onEnableAdmin = ::requestDeviceAdmin,
                    onStartTimer = ::handleStartTimer,
                    onStopTimer = ::stopTimer
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        adminActive = DeviceAdminUtils.isAdminActive(this)
    }

    private fun handleStartTimer(durationMinutes: Int, reminderMinutes: Int) {
        if (!DeviceAdminUtils.isAdminActive(this)) {
            requestDeviceAdmin()
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            pendingStart = durationMinutes to reminderMinutes
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        startTimer(durationMinutes, reminderMinutes)
    }

    private fun startTimer(durationMinutes: Int, reminderMinutes: Int) {
        LockTimerService.start(this, durationMinutes, reminderMinutes)
        moveTaskToBack(true)
    }

    private fun stopTimer() {
        LockTimerService.stop(this)
    }

    private fun requestDeviceAdmin() {
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, DeviceAdminUtils.adminComponent(this))
            .putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                getString(R.string.device_admin_description)
            )
        startActivity(intent)
    }
}
