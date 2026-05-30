package com.easylocker.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.easylocker.service.LockTimerService

/**
 * 设备重启后恢复计时服务的广播接收器
 */
class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED ||
            intent?.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            // 尝试恢复计时服务
            // 服务会在 onStartCommand 中检查是否有活跃的会话
            context.startService(Intent(context, LockTimerService::class.java))
        }
    }
}
