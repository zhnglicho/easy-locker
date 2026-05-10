package com.easylocker.data

import androidx.room.TypeConverter
import com.easylocker.model.UsageStatus

class Converters {
    @TypeConverter
    fun toUsageStatus(value: String): UsageStatus = UsageStatus.valueOf(value)

    @TypeConverter
    fun fromUsageStatus(value: UsageStatus): String = value.name
}
