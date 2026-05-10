package com.easylocker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.easylocker.data.ActiveTimerSession
import com.easylocker.data.AppDatabase
import com.easylocker.data.SettingsStore
import com.easylocker.data.TimerSettings
import com.easylocker.data.UsageRecordEntity
import com.easylocker.data.UsageRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsStore = SettingsStore(application)
    private val repository = UsageRepository(AppDatabase.get(application).usageRecordDao())

    val settings: StateFlow<TimerSettings> = settingsStore.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TimerSettings()
    )

    val records: StateFlow<List<UsageRecordEntity>> = repository.observeRecords().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val activeSession: StateFlow<ActiveTimerSession?> = settingsStore.activeSession.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null
    )

    fun setDuration(minutes: Int) {
        viewModelScope.launch { settingsStore.saveDuration(minutes) }
    }

    fun setReminder(minutes: Int) {
        viewModelScope.launch { settingsStore.saveReminder(minutes) }
    }
}
