package com.easylocker.utils

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import com.easylocker.receiver.EasyLockerDeviceAdminReceiver

object DeviceAdminUtils {
    fun adminComponent(context: Context): ComponentName =
        ComponentName(context, EasyLockerDeviceAdminReceiver::class.java)

    fun policyManager(context: Context): DevicePolicyManager =
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

    fun isAdminActive(context: Context): Boolean =
        policyManager(context).isAdminActive(adminComponent(context))
}
