package com.easylocker.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.easylocker.R
import com.easylocker.data.AppDatabase
import com.easylocker.data.SettingsStore
import com.easylocker.data.UsageRecordEntity
import com.easylocker.model.UsageStatus
import com.easylocker.ui.MainActivity
import com.easylocker.ui.ReminderActivity
import com.easylocker.utils.DeviceAdminUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.math.ceil
import kotlin.math.max

class LockTimerService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var timerJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var startTimeMillis: Long = 0
    private var durationMinutes: Int = 0
    private var reminderMinutes: Int = 0
    private var completedNormally = false
    private var recordWritten = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 处理停止指令
        if (intent?.action == ACTION_STOP) {
            finishSession(UsageStatus.EARLY_ENDED)
            return START_NOT_STICKY
        }
        
        // 处理系统重启后的恢复 (intent 为 null 时)
        if (intent == null) {
            // 尝试从 DataStore 恢复会话
            restoreSessionFromStorage()
            if (startTimeMillis > 0 && durationMinutes > 0) {
                // 检查是否已过期
                val endTimeMillis = startTimeMillis + durationMinutes * 60_000L
                if (System.currentTimeMillis() >= endTimeMillis) {
                    // 已过期，执行锁屏并停止
                    lockDevice()
                    scope.launch {
                        writeRecord(UsageStatus.NORMAL_LOCKED, endTimeMillis)
                        clearActiveSession()
                    }
                    stopSelf()
                    return START_NOT_STICKY
                }
                // 继续计时
                acquireWakeLock()
                startForeground(NOTIFICATION_ID, buildNotification((endTimeMillis - System.currentTimeMillis()) / 1000))
                startTimerForRemainingTime(endTimeMillis)
                return START_STICKY
            }
            stopSelf()
            return START_NOT_STICKY
        }

        durationMinutes = intent.getIntExtra(EXTRA_DURATION_MINUTES, 30)
        reminderMinutes = intent.getIntExtra(EXTRA_REMINDER_MINUTES, 1)
        startTimeMillis = intent.getLongExtra(EXTRA_START_TIME, System.currentTimeMillis())
        completedNormally = false
        recordWritten = false

        acquireWakeLock()
        scope.launch {
            SettingsStore(this@LockTimerService).saveActiveSession(
                startTimeMillis = startTimeMillis,
                endTimeMillis = startTimeMillis + durationMinutes * 60_000L,
                durationMinutes = durationMinutes,
                reminderMinutes = reminderMinutes
            )
        }
        startForeground(NOTIFICATION_ID, buildNotification(durationMinutes * 60L))
        startTimer(durationMinutes, reminderMinutes)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // 用户从最近任务列表划掉应用时触发
        if (!completedNormally && !recordWritten) {
            lockDevice()
            finishSession(UsageStatus.EARLY_ENDED)
        }
    }

    override fun onDestroy() {
        timerJob?.cancel()
        releaseWakeLock()
        if (!completedNormally && startTimeMillis > 0 && !recordWritten) {
            runBlocking {
                writeRecord(UsageStatus.EARLY_ENDED, System.currentTimeMillis())
                clearActiveSession()
            }
        }
        scope.cancel()
        super.onDestroy()
    }

    private fun restoreSessionFromStorage() {
        // 从 DataStore 同步读取保存的会话信息
        runCatching {
            val store = SettingsStore(this)
            val session = runBlocking { store.activeSession.first() }
            if (session != null) {
                startTimeMillis = session.startTimeMillis
                durationMinutes = session.durationMinutes
                reminderMinutes = session.reminderMinutes
            }
        }
    }

    private fun startTimerForRemainingTime(endTimeMillis: Long) {
        timerJob?.cancel()
        var reminderShown = false
        val reminderMillis = reminderMinutes * 60_000L

        timerJob = scope.launch {
            while (true) {
                val remainingMillis = max(0, endTimeMillis - System.currentTimeMillis())
                val remainingSeconds = ceil(remainingMillis / 1000.0).toLong()
                updateNotification(remainingSeconds)

                // 检查是否需要显示提醒
                if (!reminderShown && reminderMinutes > 0 && remainingMillis <= reminderMillis) {
                    reminderShown = true
                    showReminder(reminderMinutes)
                }

                if (remainingMillis <= 0) {
                    completedNormally = true
                    writeRecord(UsageStatus.NORMAL_LOCKED, endTimeMillis)
                    clearActiveSession()
                    lockDevice()
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    return@launch
                }
                delay(1_000L)
            }
        }
    }

    private fun startTimer(durationMinutes: Int, reminderMinutes: Int) {
        timerJob?.cancel()
        val totalMillis = durationMinutes * 60_000L
        val endTimeMillis = startTimeMillis + totalMillis
        var reminderShown = reminderMinutes == 0

        timerJob = scope.launch {
            while (true) {
                val remainingMillis = max(0, endTimeMillis - System.currentTimeMillis())
                val remainingSeconds = ceil(remainingMillis / 1000.0).toLong()
                updateNotification(remainingSeconds)

                if (!reminderShown && remainingSeconds <= reminderMinutes * 60L) {
                    reminderShown = true
                    showReminder(reminderMinutes)
                }

                if (remainingMillis <= 0) {
                    completedNormally = true
                    writeRecord(UsageStatus.NORMAL_LOCKED, endTimeMillis)
                    clearActiveSession()
                    lockDevice()
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    return@launch
                }
                delay(1_000L)
            }
        }
    }

    private fun showReminder(reminderMinutes: Int) {
        // 触发震动提醒
        vibrateReminder()
        
        val intent = Intent(this, ReminderActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(ReminderActivity.EXTRA_MINUTES, reminderMinutes)
        val pendingIntent = PendingIntent.getActivity(
            this,
            2,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(this, REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("还有 $reminderMinutes 分钟")
            .setContentText("即将锁屏，请提前保存重要内容")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVibrate(REMINDER_VIBRATION_PATTERN)
            .setFullScreenIntent(pendingIntent, true)
            .setAutoCancel(true)
            .build()
        getSystemService(NotificationManager::class.java).notify(REMINDER_NOTIFICATION_ID, notification)

        runCatching { startActivity(intent) }
    }

    /**
     * 触发倒计时提醒震动
     * 震动模式：短-停-短-停-长（模拟心跳节奏，引起注意但不突兀）
     */
    private fun vibrateReminder() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Android 12+ 使用 VibratorManager
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    ?: return
                val vibrator = vibratorManager.defaultVibrator
                if (vibrator.hasVibrator()) {
                    val vibrationEffect = VibrationEffect.createWaveform(REMINDER_VIBRATION_PATTERN, -1)
                    val attributes = VibrationAttributes.Builder()
                        .setUsage(VibrationAttributes.USAGE_ALARM)
                        .build()
                    vibrator.vibrate(vibrationEffect, attributes)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Android 8.0 - 11 使用 VibrationEffect
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
                if (vibrator.hasVibrator()) {
                    val vibrationEffect = VibrationEffect.createWaveform(REMINDER_VIBRATION_PATTERN, -1)
                    vibrator.vibrate(vibrationEffect)
                }
            } else {
                // Android 8.0 以下使用旧 API
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
                if (vibrator.hasVibrator()) {
                    vibrator.vibrate(REMINDER_VIBRATION_PATTERN, -1)
                }
            }
        } catch (e: Exception) {
            // 震动失败不阻断提醒流程
            e.printStackTrace()
        }
    }

    private fun lockDevice() {
        if (DeviceAdminUtils.isAdminActive(this)) {
            DeviceAdminUtils.policyManager(this).lockNow()
        }
    }

    private fun finishSession(status: UsageStatus) {
        timerJob?.cancel()
        lockDevice() // 手动结束也触发锁屏
        scope.launch {
            writeRecord(status, System.currentTimeMillis())
            clearActiveSession()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private suspend fun clearActiveSession() {
        SettingsStore(this).clearActiveSession()
    }

    private suspend fun writeRecord(status: UsageStatus, endMillis: Long) {
        if (recordWritten) return
        recordWritten = true
        AppDatabase.get(this).usageRecordDao().insert(
            UsageRecordEntity(
                startTime = startTimeMillis,
                endTime = endMillis,
                durationMinutes = durationMinutes,
                status = status,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "EasyLocker:TimerWakeLock"
        ).apply { 
            // 不设置超时，由服务生命周期控制，确保计时期间不会被系统杀死
            setReferenceCounted(false)
            acquire() 
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.takeIf { it.isHeld }?.release()
        wakeLock = null
    }

    private fun buildNotification(remainingSeconds: Long): Notification {
        val mainIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, LockTimerService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("儿童定时锁屏运行中")
            .setContentText("剩余 ${formatRemainingMinutes(remainingSeconds)}")
            .setContentIntent(mainIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(0, "提前结束", stopIntent)
            .build()
    }

    private fun updateNotification(remainingSeconds: Long) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(remainingSeconds))
    }

    private fun formatRemainingMinutes(remainingSeconds: Long): String {
        val minutes = ceil(remainingSeconds / 60.0).toInt().coerceAtLeast(0)
        return "${minutes} 分钟"
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "定时锁屏",
            NotificationManager.IMPORTANCE_LOW
        )
        
        // 提醒通知渠道：启用震动
        val reminderChannel = NotificationChannel(
            REMINDER_CHANNEL_ID,
            "锁屏提醒",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            // 启用震动
            enableVibration(true)
            // 设置震动模式：短-停-短-停-长
            vibrationPattern = REMINDER_VIBRATION_PATTERN
        }
        
        getSystemService(NotificationManager::class.java).apply {
            createNotificationChannel(channel)
            createNotificationChannel(reminderChannel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "lock_timer"
        private const val REMINDER_CHANNEL_ID = "lock_timer_reminder_v2"
        private val REMINDER_VIBRATION_PATTERN = longArrayOf(0, 200, 100, 200, 100, 500)
        private const val NOTIFICATION_ID = 1001
        private const val REMINDER_NOTIFICATION_ID = 1002
        private const val ACTION_STOP = "com.easylocker.action.STOP"
        private const val EXTRA_DURATION_MINUTES = "duration_minutes"
        private const val EXTRA_REMINDER_MINUTES = "reminder_minutes"
        private const val EXTRA_START_TIME = "start_time"

        fun start(context: Context, durationMinutes: Int, reminderMinutes: Int) {
            val intent = Intent(context, LockTimerService::class.java)
                .putExtra(EXTRA_DURATION_MINUTES, durationMinutes)
                .putExtra(EXTRA_REMINDER_MINUTES, reminderMinutes)
                .putExtra(EXTRA_START_TIME, System.currentTimeMillis())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, LockTimerService::class.java).setAction(ACTION_STOP)
            context.startService(intent)
        }
    }
}
