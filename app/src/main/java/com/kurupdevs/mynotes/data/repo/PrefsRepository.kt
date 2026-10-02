package com.kurupdevs.mynotes.data.repo

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kurupdevs.mynotes.data.model.SortOrder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.prefs by preferencesDataStore("mynotes_prefs")

class PrefsRepository(private val context: Context) {
    private val store = context.prefs

    val theme: Flow<String> = store.data.map { it[stringPreferencesKey("theme")] ?: "dark" }
    val sortOrder: Flow<SortOrder> = store.data.map {
        runCatching { SortOrder.valueOf(it[stringPreferencesKey("sort")] ?: "EDITED") }.getOrDefault(SortOrder.EDITED)
    }
    val pinnedFirst: Flow<Boolean> = store.data.map { it[booleanPreferencesKey("pinned_first")] ?: true }
    val defaultColor: Flow<String> = store.data.map { it[stringPreferencesKey("default_color")] ?: "default" }
    val moveCheckedToBottom: Flow<Boolean> = store.data.map { it[booleanPreferencesKey("move_checked_bottom")] ?: true }
    val wordCountOn: Flow<Boolean> = store.data.map { it[booleanPreferencesKey("word_count")] ?: true }
    val syncWifiOnly: Flow<Boolean> = store.data.map { it[booleanPreferencesKey("wifi_only")] ?: false }
    val autoSync: Flow<Boolean> = store.data.map { it[booleanPreferencesKey("auto_sync")] ?: true }
    val onboardingDone: Flow<Boolean> = store.data.map { it[booleanPreferencesKey("onboarding")] ?: false }
    val backupAsks: Flow<Int> = store.data.map { it[intPreferencesKey("backup_asks")] ?: 0 }
    val lastSyncAt: Flow<Long> = store.data.map { it[longPreferencesKey("last_sync")] ?: 0L }
    val appLock: Flow<Boolean> = store.data.map { it[booleanPreferencesKey("app_lock")] ?: false }

    suspend fun setTheme(v: String) = edit { it[stringPreferencesKey("theme")] = v }
    suspend fun setSort(v: SortOrder) = edit { it[stringPreferencesKey("sort")] = v.name }
    suspend fun setPinnedFirst(v: Boolean) = edit { it[booleanPreferencesKey("pinned_first")] = v }
    suspend fun setDefaultColor(v: String) = edit { it[stringPreferencesKey("default_color")] = v }
    suspend fun setMoveCheckedToBottom(v: Boolean) = edit { it[booleanPreferencesKey("move_checked_bottom")] = v }
    suspend fun setWordCountOn(v: Boolean) = edit { it[booleanPreferencesKey("word_count")] = v }
    suspend fun setSyncWifiOnly(v: Boolean) = edit { it[booleanPreferencesKey("wifi_only")] = v }
    suspend fun setAutoSync(v: Boolean) = edit { it[booleanPreferencesKey("auto_sync")] = v }
    suspend fun setOnboardingDone() = edit { it[booleanPreferencesKey("onboarding")] = true }
    suspend fun bumpBackupAsks() = edit { it[intPreferencesKey("backup_asks")] = (it[intPreferencesKey("backup_asks")] ?: 0) + 1 }
    suspend fun setLastSyncAt(v: Long) = edit { it[longPreferencesKey("last_sync")] = v }
    suspend fun setAppLock(v: Boolean) = edit { it[booleanPreferencesKey("app_lock")] = v }

    private suspend fun edit(fn: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        store.edit(fn)
    }

    suspend fun themeNow(): String = theme.first()
}
