package com.easylocker.data

import kotlinx.coroutines.flow.Flow

class UsageRepository(private val dao: UsageRecordDao) {
    fun observeRecords(): Flow<List<UsageRecordEntity>> = dao.observeAll()

    suspend fun addRecord(record: UsageRecordEntity) {
        dao.insert(record)
    }
}
