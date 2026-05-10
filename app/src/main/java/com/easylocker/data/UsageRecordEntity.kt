package com.easylocker.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.easylocker.model.UsageStatus

@Entity(tableName = "usage_records")
data class UsageRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "start_time")
    val startTime: Long,
    @ColumnInfo(name = "end_time")
    val endTime: Long,
    @ColumnInfo(name = "duration_minutes")
    val durationMinutes: Int,
    val status: UsageStatus,
    @ColumnInfo(name = "created_at")
    val createdAt: Long
)
