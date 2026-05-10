package com.easylocker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UsageRecordDao {
    @Query("SELECT * FROM usage_records ORDER BY start_time DESC")
    fun observeAll(): Flow<List<UsageRecordEntity>>

    @Insert
    suspend fun insert(record: UsageRecordEntity)
}
