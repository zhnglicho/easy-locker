package com.easylocker.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.easylocker.R
import com.easylocker.data.ActiveTimerSession
import com.easylocker.data.UsageRecordEntity
import com.easylocker.utils.formatClockRange
import com.easylocker.utils.formatDate
import com.easylocker.utils.formatDuration
import com.easylocker.utils.isToday
import com.easylocker.utils.isYesterday
import com.easylocker.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun EasyLockerApp(
    adminActive: Boolean,
    onEnableAdmin: () -> Unit,
    onStartTimer: (durationMinutes: Int, reminderMinutes: Int) -> Unit,
    onStopTimer: () -> Unit,
    viewModel: MainViewModel = viewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val records by viewModel.records.collectAsState()
    val activeSession by viewModel.activeSession.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = AppBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (selectedTab == 0) {
                    HomeScreen(
                        durationMinutes = settings.durationMinutes,
                        reminderMinutes = settings.reminderMinutes,
                        adminActive = adminActive,
                        onDurationChange = viewModel::setDuration,
                        onReminderChange = viewModel::setReminder,
                        onEnableAdmin = onEnableAdmin,
                        onStartTimer = onStartTimer,
                        onStopTimer = onStopTimer,
                        activeSession = activeSession
                    )
                } else {
                    UsageScreen(records = records)
                }
            }
            BottomTabs(
                selectedTab = selectedTab,
                onSelected = { selectedTab = it }
            )
        }
    }
}

@Composable
private fun HomeScreen(
    durationMinutes: Int,
    reminderMinutes: Int,
    adminActive: Boolean,
    onDurationChange: (Int) -> Unit,
    onReminderChange: (Int) -> Unit,
    onEnableAdmin: () -> Unit,
    onStartTimer: (Int, Int) -> Unit,
    onStopTimer: () -> Unit,
    activeSession: ActiveTimerSession?
) {
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(activeSession) {
        while (activeSession != null) {
            nowMillis = System.currentTimeMillis()
            delay(1_000L)
        }
    }
    val activeRemainingSeconds = activeSession?.let {
        max(0L, ceil((it.endTimeMillis - nowMillis) / 1000.0).toLong())
    } ?: 0L
    val isTimerActive = activeSession != null && activeRemainingSeconds > 0L
    val displayMinutes = if (isTimerActive) {
        ceil(activeRemainingSeconds / 60.0).toInt().coerceIn(1, 60)
    } else {
        durationMinutes
    }

    var showStopConfirm by remember { mutableStateOf(false) }

    if (showStopConfirm) {
        AlertDialog(
            onDismissRequest = { showStopConfirm = false },
            title = { Text("确认提前结束？") },
            text = { Text("提前结束计时也会立即触发锁屏，以确保使用约定的有效性。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showStopConfirm = false
                        onStopTimer()
                    }
                ) {
                    Text("确定结束并锁屏", color = Color(0xFFE85050), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStopConfirm = false }) {
                    Text("取消", color = TextSecondary)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(20.dp))
        Text("定时锁屏", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            "设定使用时长，帮助孩子合理使用手机",
            color = TextSecondary,
            fontSize = 15.sp,
            textAlign = TextAlign.Center
        )
        
        Spacer(Modifier.weight(1f))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
                Text("设置使用时长", fontSize = 19.sp, fontWeight = FontWeight.Bold)
                
                CircularMinuteSlider(
                    value = displayMinutes,
                    onValueChange = onDurationChange,
                    enabled = !isTimerActive,
                    centerLabel = if (isTimerActive) "剩余" else "分钟",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                )
                
                activeSession?.takeIf { isTimerActive }?.let { session ->
                    TimerStatusCard(
                        session = session,
                        remainingSeconds = activeRemainingSeconds
                    )
                } ?: run {
                    HealthHint(minutes = durationMinutes)
                }
                
                Spacer(Modifier.height(10.dp))
                Text("倒计时提醒", fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Text("倒计时结束前提醒孩子", color = TextSecondary, fontSize = 13.sp)
                Spacer(Modifier.height(10.dp))
                ReminderOptions(
                    selected = reminderMinutes,
                    enabled = !isTimerActive,
                    onSelected = onReminderChange
                )
            }
        }

        Spacer(Modifier.weight(1.2f))

        Button(
            onClick = {
                if (isTimerActive) {
                    showStopConfirm = true
                } else {
                    onStartTimer(durationMinutes, reminderMinutes)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = RoundedCornerShape(32.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            contentPadding = ButtonDefaults.ContentPadding
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            if (isTimerActive) {
                                listOf(Color(0xFFFF9F6E), Color(0xFFE85050))
                            } else {
                                listOf(PrimaryBlue, Color(0xFF7B61F2))
                            }
                        ),
                        RoundedCornerShape(32.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (isTimerActive) "■  提前结束" else "▶  开始计时",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Spacer(Modifier.height(12.dp))
        
        InfoCard(
            text = if (isTimerActive) {
                "计时正在后台运行\n剩余 ${formatCountdown(activeRemainingSeconds)} 后将自动锁屏"
            } else if (adminActive) {
                "计时开始后，应用将进入后台运行\n时间结束后将自动锁屏"
            } else {
                "首次使用需要开启设备管理员权限\n否则无法自动锁屏"
            },
            onClick = if (adminActive) null else onEnableAdmin
        )

        Spacer(Modifier.height(20.dp))
    }

}

@Composable
private fun CircularMinuteSlider(
    value: Int,
    onValueChange: (Int) -> Unit,
    enabled: Boolean,
    centerLabel: String,
    modifier: Modifier = Modifier
) {
    val steps = (5..60 step 5).toList()
    val style = remember(value) { healthTimeStyle(value) }
    Box(
        modifier = modifier.then(
            if (enabled) {
                Modifier.pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            onValueChange(offsetToMinutes(offset, size.width, size.height))
                        },
                        onDrag = { change, _ ->
                            onValueChange(offsetToMinutes(change.position, size.width, size.height))
                        }
                    )
                }
            } else {
                Modifier
            }
        ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(10.dp)) {
            val stroke = 18.dp.toPx()
            val radius = (size.minDimension - stroke) / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            val topLeft = Offset(center.x - radius, center.y - radius)
            val arcSize = Size(radius * 2f, radius * 2f)
            drawArc(
                color = style.trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
            drawArc(
                brush = Brush.sweepGradient(listOf(style.startColor, style.endColor, style.startColor)),
                startAngle = -90f,
                sweepAngle = value / 60f * 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round)
            )

            steps.forEach { minute ->
                val angle = (-90f + minute / 60f * 360f) * PI / 180f
                val tickStart = Offset(
                    center.x + cos(angle).toFloat() * (radius - 28.dp.toPx()),
                    center.y + sin(angle).toFloat() * (radius - 28.dp.toPx())
                )
                val tickEnd = Offset(
                    center.x + cos(angle).toFloat() * (radius - 6.dp.toPx()),
                    center.y + sin(angle).toFloat() * (radius - 6.dp.toPx())
                )
                drawLine(
                    color = if (minute == value) style.endColor else style.trackColor,
                    start = tickStart,
                    end = tickEnd,
                    strokeWidth = if (minute == value) 4.dp.toPx() else 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            val thumbAngle = (-90f + value / 60f * 360f) * PI / 180f
            val thumb = Offset(
                center.x + cos(thumbAngle).toFloat() * radius,
                center.y + sin(thumbAngle).toFloat() * radius
            )
            drawCircle(style.endColor, 15.dp.toPx(), thumb)
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value.toString(), fontSize = 62.sp, fontWeight = FontWeight.Bold)
            Text(centerLabel, color = style.endColor, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

private data class HealthTimeStyle(
    val startColor: Color,
    val endColor: Color,
    val trackColor: Color,
    val hint: String
)

private fun healthTimeStyle(minutes: Int): HealthTimeStyle = when {
    minutes <= 25 -> HealthTimeStyle(
        startColor = Color(0xFF7CCBA2),
        endColor = Color(0xFF388E3C),
        trackColor = Color(0xFFE8F5E9),
        hint = "适合短时娱乐与轻度使用。"
    )
    minutes <= 45 -> HealthTimeStyle(
        startColor = Color(0xFFFFD54F),
        endColor = Color(0xFFF57C00),
        trackColor = Color(0xFFFFF3E0),
        hint = "适合学习、阅读等持续专注场景。"
    )
    else -> HealthTimeStyle(
        startColor = Color(0xFFFF8A65),
        endColor = Color(0xFFD32F2F),
        trackColor = Color(0xFFFFEBEE),
        hint = "使用时长较长，请结合孩子状态合理安排。"
    )
}

@Composable
private fun HealthHint(minutes: Int) {
    val style = healthTimeStyle(minutes)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(style.trackColor, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(
                    Brush.horizontalGradient(listOf(style.startColor, style.endColor)),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text("!", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
        }
        Spacer(Modifier.width(10.dp))
        Text(
            style.hint,
            color = style.endColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
private fun TimerStatusCard(session: ActiveTimerSession, remainingSeconds: Long) {
    val progress = (remainingSeconds / (session.durationMinutes * 60f)).coerceIn(0f, 1f)
    val style = healthTimeStyle(session.durationMinutes)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF8FAFF), RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("计时中", color = style.endColor, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text(formatCountdown(remainingSeconds), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(Color(0xFFE9EDF7), RoundedCornerShape(4.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(8.dp)
                    .background(
                        Brush.horizontalGradient(listOf(style.startColor, style.endColor)),
                        RoundedCornerShape(4.dp)
                    )
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "本次设置 ${session.durationMinutes} 分钟，结束后自动锁屏",
            color = TextSecondary,
            fontSize = 14.sp
        )
    }
}

private fun formatCountdown(totalSeconds: Long): String {
    val safeSeconds = totalSeconds.coerceAtLeast(0L)
    val minutes = safeSeconds / 60
    val seconds = safeSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

private fun offsetToMinutes(offset: Offset, width: Int, height: Int): Int {
    val centerX = width / 2f
    val centerY = height / 2f
    val degrees = Math.toDegrees(atan2(offset.y - centerY, offset.x - centerX).toDouble())
    val normalized = ((degrees + 90 + 360) % 360).toFloat()
    val step = ((normalized / 360f) * 12f).roundToInt()
    val minute = if (step == 0) 60 else step * 5
    return minute.coerceIn(5, 60)
}

@Composable
private fun ReminderOptions(selected: Int, enabled: Boolean, onSelected: (Int) -> Unit) {
    val options = listOf(0 to "关闭", 1 to "1 分钟", 2 to "2 分钟", 3 to "3 分钟")
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        options.forEach { (value, label) ->
            val isSelected = selected == value
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(58.dp)
                    .background(
                        if (isSelected) Color(0xFFEFF2FF) else Color(0xFFF5F7FC),
                        RoundedCornerShape(16.dp)
                    )
                    .border(
                        width = if (isSelected) 1.dp else 0.dp,
                        color = if (isSelected) PrimaryBlue else Color.Transparent,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .then(if (enabled) Modifier.clickable { onSelected(value) } else Modifier),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    color = when {
                        !enabled -> TextSecondary
                        isSelected -> PrimaryBlue
                        else -> TextPrimary
                    },
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun InfoCard(text: String, onClick: (() -> Unit)?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF0F6FF), RoundedCornerShape(18.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("ⓘ", color = PrimaryBlue, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(12.dp))
        Text(text, color = TextSecondary, fontSize = 15.sp, lineHeight = 22.sp)
    }
}

@Composable
private fun UsageScreen(records: List<UsageRecordEntity>) {
    val todayRecords = records.filter { isToday(it.startTime) }
    val totalTodayMinutes = todayRecords.sumOf { it.durationMinutes }
    
    // 状态：记录哪些日期是展开的。默认展开“今日”
    var expandedGroups by remember { mutableStateOf(setOf("今日")) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(24.dp))
        Text("使用记录", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("帮助您了解孩子近期的设备使用情况", color = TextSecondary, fontSize = 15.sp)
        Spacer(Modifier.height(20.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("今日共计", "${todayRecords.size} 次", PrimaryBlue, Modifier.weight(1f))
            StatCard("累计时长", formatDuration(totalTodayMinutes), SoftGreen, Modifier.weight(1f))
        }

        Spacer(Modifier.height(24.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val groups = records.groupBy {
                when {
                    isToday(it.startTime) -> "今日"
                    isYesterday(it.startTime) -> "昨日"
                    else -> formatDate(it.startTime)
                }
            }

            if (records.isEmpty()) {
                item {
                    Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        Text("暂无记录", color = TextSecondary, fontSize = 16.sp)
                    }
                }
            } else {
                groups.forEach { (dateTitle, items) ->
                    val isExpanded = expandedGroups.contains(dateTitle)
                    
                    item {
                        val dayDuration = items.sumOf { it.durationMinutes }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    expandedGroups = if (isExpanded) {
                                        expandedGroups - dateTitle
                                    } else {
                                        expandedGroups + dateTitle
                                    }
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(dateTitle, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "(${items.size} 次 · ${formatDuration(dayDuration)})",
                                color = TextSecondary,
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                painter = painterResource(
                                    id = if (isExpanded) R.drawable.ic_timer_24 else R.drawable.ic_history_24 
                                    // 这里暂时借用现有图标演示，实际建议用箭头图标 ic_expand_more/less
                                ),
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    
                    if (isExpanded) {
                        items(items, key = { it.id }) { record ->
                            UsageRecordCard(record)
                        }
                    }
                }
                item { Spacer(Modifier.height(20.dp)) }
            }
        }
    }
}

@Composable
private fun StatCard(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(96.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.Center) {
            Text(title, color = TextSecondary, fontSize = 14.sp)
            Spacer(Modifier.height(6.dp))
            Text(value, color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Box(
                Modifier
                    .padding(top = 8.dp)
                    .width(38.dp)
                    .height(4.dp)
                    .background(color, RoundedCornerShape(2.dp))
            )
        }
    }
}

@Composable
private fun UsageRecordCard(record: UsageRecordEntity) {
    val isNormal = record.status.name == "NORMAL_LOCKED"
    val statusColor = if (isNormal) SoftGreen else Color(0xFFFF9F6E)
    val statusBg = if (isNormal) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(statusBg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (isNormal) "✓" else "!",
                    color = statusColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    formatClockRange(record.startTime, record.endTime),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    record.status.label,
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
            Text(
                "${record.durationMinutes} 分钟",
                color = PrimaryBlue,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .background(Color(0xFFEFF4FF), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun BottomTabs(selectedTab: Int, onSelected: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(82.dp)
            .background(Color.White)
            .navigationBarsPadding(),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BottomTab(R.drawable.ic_timer_24, "定时锁屏", selectedTab == 0) { onSelected(0) }
        BottomTab(R.drawable.ic_history_24, "使用记录", selectedTab == 1) { onSelected(1) }
    }
}

@Composable
private fun BottomTab(iconRes: Int, label: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(110.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = label,
            tint = if (selected) PrimaryBlue else TextSecondary,
            modifier = Modifier.size(26.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            color = if (selected) PrimaryBlue else TextSecondary,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
