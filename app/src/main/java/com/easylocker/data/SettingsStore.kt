package com.easylocker.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

data class TimerSettings(
    val durationMinutes: Int = 15,
    val reminderMinutes: Int = 1
)

data class ActiveTimerSession(
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val durationMinutes: Int,
    val reminderMinutes: Int
)

class SettingsStore(private val context: Context) {
    private val durationKey = intPreferencesKey("duration_minutes")
    private val reminderKey = intPreferencesKey("reminder_minutes")
    private val activeStartKey = longPreferencesKey("active_start_time")
    private val activeEndKey = longPreferencesKey("active_end_time")
    private val activeDurationKey = intPreferencesKey("active_duration_minutes")
    private val activeReminderKey = intPreferencesKey("active_reminder_minutes")

    val settings: Flow<TimerSettings> = context.settingsDataStore.data.map { prefs ->
        TimerSettings(
            durationMinutes = prefs[durationKey] ?: 15,
            reminderMinutes = prefs[reminderKey] ?: 1
        )
    }

    val activeSession: Flow<ActiveTimerSession?> = context.settingsDataStore.data.map { prefs ->
        val start = prefs[activeStartKey] ?: return@map null
        val end = prefs[activeEndKey] ?: return@map null
        val duration = prefs[activeDurationKey] ?: return@map null
        val reminder = prefs[activeReminderKey] ?: 1
        if (end <= System.currentTimeMillis()) {
            null
        } else {
            ActiveTimerSession(
                startTimeMillis = start,
                endTimeMillis = end,
                durationMinutes = duration,
                reminderMinutes = reminder
            )
        }
    }

    suspend fun saveDuration(minutes: Int) {
        context.settingsDataStore.edit { prefs -> prefs[durationKey] = minutes }
    }

    suspend fun saveReminder(minutes: Int) {
        context.settingsDataStore.edit { prefs -> prefs[reminderKey] = minutes }
    }

    suspend fun saveActiveSession(
        startTimeMillis: Long,
        endTimeMillis: Long,
        durationMinutes: Int,
        reminderMinutes: Int
    ) {
        context.settingsDataStore.edit { prefs ->
            prefs[activeStartKey] = startTimeMillis
            prefs[activeEndKey] = endTimeMillis
            prefs[activeDurationKey] = durationMinutes
            prefs[activeReminderKey] = reminderMinutes
        }
    }

    suspend fun clearActiveSession() {
        context.settingsDataStore.edit { prefs ->
            prefs.remove(activeStartKey)
            prefs.remove(activeEndKey)
            prefs.remove(activeDurationKey)
            prefs.remove(activeReminderKey)
        }
    }
}
