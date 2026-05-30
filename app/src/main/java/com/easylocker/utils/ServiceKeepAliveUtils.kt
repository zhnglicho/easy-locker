package com.easylocker.utils

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import com.easylocker.service.LockTimerService

/**
 * 服务保活和诊断工具
 */
object ServiceKeepAliveUtils {
    
    /**
     * 检查服务是否正在运行
     */
    fun isServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return manager.getRunningServices(Integer.MAX_VALUE)
            .any { it.service.className == serviceClass.name }
    }
    
    /**
     * 检查是否忽略电池优化（允许后台运行）
     */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }
    
    /**
     * 获取电池优化设置页面 Intent
     */
    fun getBatteryOptimizationIntent(context: Context): Intent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return null
        return Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = android.net.Uri.parse("package:${context.packageName}")
        }
    }
    
    /**
     * 检查服务状态并自动重启
     */
    fun checkAndRestartService(context: Context) {
        if (!isServiceRunning(context, LockTimerService::class.java)) {
            // 服务未运行，尝试从存储恢复
            val intent = Intent(context, LockTimerService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
    
    /**
     * 获取诊断信息
     */
    fun getDiagnostics(context: Context): String {
        val sb = StringBuilder()
        sb.appendLine("=== Easy Locker 诊断信息 ===")
        sb.appendLine()
        
        // 服务状态
        val serviceRunning = isServiceRunning(context, LockTimerService::class.java)
        sb.appendLine("计时服务运行中: $serviceRunning")
        
        // 电池优化
        val ignoringBattery = isIgnoringBatteryOptimizations(context)
        sb.appendLine("忽略电池优化: $ignoringBattery")
        
        // Android 版本
        sb.appendLine("Android 版本: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        
        // 设备厂商
        sb.appendLine("设备厂商: ${Build.MANUFACTURER}")
        sb.appendLine("设备型号: ${Build.MODEL}")
        
        return sb.toString()
    }
}
