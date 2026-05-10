package com.easylocker.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max

private val timeFormatter = SimpleDateFormat("HH:mm", Locale.CHINA)
private val dateFormatter = SimpleDateFormat("MM-dd", Locale.CHINA)

fun formatClockRange(startMillis: Long, endMillis: Long): String =
    "${timeFormatter.format(Date(startMillis))} - ${timeFormatter.format(Date(endMillis))}"

fun formatDate(millis: Long): String = dateFormatter.format(Date(millis))

fun formatDuration(minutes: Int): String {
    val safeMinutes = max(0, minutes)
    val hours = safeMinutes / 60
    val mins = safeMinutes % 60
    return when {
        hours > 0 && mins > 0 -> "${hours}小时 ${mins}分钟"
        hours > 0 -> "${hours}小时"
        else -> "${mins}分钟"
    }
}

fun isToday(millis: Long): Boolean {
    val today = Calendar.getInstance()
    val target = Calendar.getInstance().apply { timeInMillis = millis }
    return today.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
        today.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)
}

fun isYesterday(millis: Long): Boolean {
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    val target = Calendar.getInstance().apply { timeInMillis = millis }
    return yesterday.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
        yesterday.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)
}
