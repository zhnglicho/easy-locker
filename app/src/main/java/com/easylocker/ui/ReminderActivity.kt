package com.easylocker.ui

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.max

class ReminderActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val reminderMinutes = intent.getIntExtra(EXTRA_MINUTES, 1).coerceIn(1, 3)
        
        // 触发震动提醒
        vibrateOnReminder()
        
        setContent {
            EasyLockerTheme {
                ReminderScreen(
                    reminderMinutes = reminderMinutes,
                    onDismiss = { finish() }
                )
            }
        }
    }

    companion object {
        const val EXTRA_MINUTES = "extra_minutes"
    }
    
    /**
     * 提醒界面显示时触发震动
     * 震动模式：短-停-短-停-长（与服务的震动节奏一致）
     */
    private fun vibrateOnReminder() {
        try {
            // 震动模式：震动时间(毫秒), 停止时间, 震动时间, 停止时间...
            val pattern = longArrayOf(0, 200, 100, 200, 100, 500)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Android 12+ 使用 VibratorManager
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                val vibrator = vibratorManager.defaultVibrator
                if (vibrator.hasVibrator()) {
                    val vibrationEffect = VibrationEffect.createWaveform(pattern, -1)
                    vibrator.vibrate(vibrationEffect)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Android 8.0 - 11 使用 VibrationEffect
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (vibrator.hasVibrator()) {
                    val vibrationEffect = VibrationEffect.createWaveform(pattern, -1)
                    vibrator.vibrate(vibrationEffect)
                }
            } else {
                // Android 8.0 以下使用旧 API
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (vibrator.hasVibrator()) {
                    vibrator.vibrate(pattern, -1)
                }
            }
        } catch (e: Exception) {
            // 震动失败不阻断提醒流程
            e.printStackTrace()
        }
    }
}

@Composable
private fun ReminderScreen(reminderMinutes: Int, onDismiss: () -> Unit) {
    val totalSeconds = reminderMinutes * 60L
    var remainingSeconds by remember { mutableLongStateOf(totalSeconds) }

    LaunchedEffect(Unit) {
        val start = System.currentTimeMillis()
        while (remainingSeconds > 0) {
            remainingSeconds = max(0, totalSeconds - ((System.currentTimeMillis() - start) / 1000L))
            delay(250L)
        }
    }

    LaunchedEffect(Unit) {
        delay(5_000L)
        onDismiss()
    }

    Surface(color = AppBackground, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 34.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(86.dp))
            Box(
                modifier = Modifier
                    .size(112.dp)
                    .background(Color(0xFFF0EDFF), RoundedCornerShape(56.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("🔔", fontSize = 46.sp)
            }
            Spacer(Modifier.height(56.dp))
            Text(
                buildAnnotatedString {
                    append("还有 ")
                    withStyle(SpanStyle(color = Color(0xFF7564F2), fontWeight = FontWeight.Bold)) {
                        append(reminderMinutes.toString())
                    }
                    append(" 分钟")
                },
                fontSize = 38.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))
            Text("即将锁屏", fontSize = 24.sp, color = TextPrimary)
            Spacer(Modifier.height(46.dp))
            CountdownRing(remainingSeconds = remainingSeconds, totalSeconds = totalSeconds)
            Spacer(Modifier.height(54.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(24.dp))
                    .padding(22.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🔔", fontSize = 34.sp)
                Column(
                    modifier = Modifier.padding(start = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("时间结束后将自动锁屏", fontSize = 18.sp, fontWeight = FontWeight.Medium)
                    Text("请提前保存重要内容", fontSize = 18.sp, color = TextPrimary)
                }
            }
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(62.dp),
                shape = RoundedCornerShape(31.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = ButtonDefaults.ContentPadding
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(listOf(Color(0xFF9C8CF7), Color(0xFF7D6BF2))),
                            RoundedCornerShape(31.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("知道了", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun CountdownRing(remainingSeconds: Long, totalSeconds: Long) {
    Box(modifier = Modifier.size(250.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 14.dp.toPx()
            drawCircle(
                color = Color(0xFFE8E1FF),
                radius = (size.minDimension - stroke) / 2f,
                center = Offset(size.width / 2f, size.height / 2f),
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
            drawArc(
                brush = Brush.sweepGradient(listOf(Color(0xFF9C8CF7), SoftPurple, Color(0xFF9C8CF7))),
                startAngle = -90f,
                sweepAngle = (remainingSeconds / totalSeconds.toFloat()) * 360f,
                useCenter = false,
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
        }
        Text(
            "%02d:%02d".format(remainingSeconds / 60, remainingSeconds % 60),
            fontSize = 46.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
